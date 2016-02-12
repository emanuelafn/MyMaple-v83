/*
This file is part of the OdinMS Maple Story Server
Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
Matthias Butz <matze@odinms.de>
Jan Christian Meyer <vimes@odinms.de>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as
published by the Free Software Foundation version 3 as published by
the Free Software Foundation. You may not use, modify or distribute
this program under any other version of the GNU Affero General Public
License.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package client;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import tools.DatabaseConnection;

/**
 *
 * @author Anujan
 */
public class MapleFamily {

    private static Map<Integer, MapleFamily> families = new HashMap<Integer, MapleFamily>();
    private Map<Integer, MapleFamilyEntry> members = new HashMap<Integer, MapleFamilyEntry>();
    private int familyId;
    private String famName;

    public static MapleFamily getMapleFamily(MapleCharacter chr) {
        if (families.containsKey(chr.getFamilyId())) {
            return families.get(chr.getFamilyId());
        }
        return loadOrCreateFamily(chr);
    }

    public String getFamilyName() {
        return famName;
    }

    public void setFamilyName(String famName) {
        this.famName = famName;
    }

    public int getFamilyId() {
        return familyId;
    }

    public void setFamilyId(int id) {
        familyId = id;
    }

    public static MapleFamily loadOrCreateFamily(MapleCharacter chr) {
        MapleFamily ret = new MapleFamily();
        int famid = chr.getId();
        String name = chr.getName();
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps;
            if (chr.getFamilyId() != 0) {
                ps = con.prepareStatement("SELECT * FROM family_character WHERE cid = ?");
                ps.setInt(1, chr.getFamilyId());
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    MapleFamilyEntry wat = new MapleFamilyEntry();
                    wat.setRank(rs.getInt("rank"));
                    wat.setReputation(rs.getInt("reputation"));
                    wat.setCharacterId(rs.getInt("cid"));
                    name = rs.getString("name");
                    wat.setFamilyName(name);
                    famid = rs.getInt("familyId");
                    wat.setTodaysRep(rs.getInt("todaysrep"));
                    wat.setFamilyId(famid);
                    wat.setJuniors(new int[]{rs.getInt("junior1"), rs.getInt("junior2")});
                }
                rs.close();
                ps.close();
            } else { //Create Family
                ps = con.prepareStatement("INSERT INTO family_character (`cid`, `name`, `familyid`) VALUES (?, ?, ?)");
                ps.setInt(1, chr.getId());
                ps.setString(2, chr.getName());
                ps.setInt(3, chr.getId());
                ps.executeUpdate();
                MapleFamilyEntry wat = new MapleFamilyEntry();
                wat.setRank(1);
                wat.setReputation(0);
                wat.setCharacterId(chr.getId());
                wat.setFamilyId(famid);
                wat.setFamilyName(name);
                wat.setTodaysRep(0);
                wat.setPlayer(chr);
                wat.setSenior(0); //Leader
                ps.close();
            }
        } catch (SQLException sqle) {
        }
        ret.setFamilyName(name);
        ret.setFamilyId(famid);
        return ret;
    }

    private void addMember(MapleCharacter chr, MapleCharacter snr) {
        if (!members.containsKey(chr.getId())) {
            MapleFamilyEntry mem = new MapleFamilyEntry();
            PreparedStatement ps = null;
            try {
                ps = DatabaseConnection.getConnection().prepareStatement("INSERT INTO family_character (`cid`, `familyid`, `senior`, `name`) VALUES (?, ?, ?, ?)");
                ps.setInt(1, chr.getId());
                ps.setInt(2, getFamilyId());
                ps.setInt(3, snr.getId());
                ps.setString(4, getFamilyName());
                ps.executeUpdate();
                ps.close();
                ps = null;
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                if (ps != null) {
                    try {
                        ps.close();
                    } catch (Exception ee) {
                    }
                }
            }
            members.put(chr.getId(), mem);
        }
    }

    public MapleFamilyEntry getMember(int id) {
        if (members.containsKey(id)) {
            return members.get(id);
        }
        return null;
    }

    public static void saveAll() {
        synchronized (families) {
            for (MapleFamily family : families.values()) {
                synchronized (family.members) {
                    for (MapleFamilyEntry mem : family.members.values()) {
                        try {
                            mem.save();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }
}

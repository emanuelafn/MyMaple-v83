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
import java.sql.SQLException;
import tools.DatabaseConnection;

public class MapleFamilyEntry {

    private MapleCharacter chr;
    private int familyId, rank, reputation, totalReputation, todaysRep, characterId;
    private String familyName;
    private int[] juniors = new int[2];
    private int senior;

    public int getId() {
        return familyId;
    }

    public void setFamilyId(int familyId) {
        this.familyId = familyId;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public MapleCharacter getPlayer() {
        return chr;
    }

    public void setPlayer(MapleCharacter chr) {
        this.chr = chr;
    }

    public int getReputation() {
        return reputation;
    }

    public int getTodaysRep() {
        return todaysRep;
    }

    public void setReputation(int reputation) {
        this.reputation = reputation;
    }

    public void setTodaysRep(int today) {
        this.todaysRep = today;
    }

    public void gainReputation(int gain) {
        this.reputation += gain;
        this.totalReputation += gain;
    }

    public int getTotalJuniors() {
        int j = 0;
        for (int i : juniors) {
            if (i != 0) {
                j++;
            }
        }
        return j;
    }

    public int[] getJuniors() {
        return juniors;
    }

    public void setJuniors(int[] juniors) {
        this.juniors = juniors;
    }

    public int getFamilyId() {
        return familyId;
    }

    public void setFamilyName(String familyName) {
        this.familyName = familyName;
    }

    public String getFamilyName() {
        return familyName;
    }

    public int getTotalReputation() {
        return totalReputation;
    }

    public void setTotalReputation(int totalReputation) {
        this.totalReputation = totalReputation;
    }

    public int getSenior() {
        return senior;
    }

    public void setSenior(int id) {
        senior = id;
    }

    public int getCharacterId() {
        return characterId;
    }

    public void setCharacterId(int characterid) {
        this.characterId = characterid;
    }

    public void save() throws SQLException {
        Connection con = DatabaseConnection.getConnection();
        PreparedStatement ps = con.prepareStatement("UPDATE family_character SET reputation = ?, todaysrep = ?, name = ?, totalreputation = ?, familyid = ?, junior1 = ?, junior2 = ? WHERE cid = ?");
        ps.setInt(1, reputation);
        ps.setInt(2, todaysRep);
        ps.setString(3, familyName);
        ps.setInt(4, totalReputation);
        ps.setInt(5, familyId);
        ps.setInt(6, juniors[0]);
        ps.setInt(7, juniors[1]);
        ps.setInt(8, characterId);
        ps.executeUpdate();
        ps.close();
    }
}

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
package client.command;

import client.MapleCharacter;
import client.MapleClient;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.List;
import provider.MapleData;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;
import tools.Pair;
import java.util.ArrayList;
import java.util.LinkedList;
import java.io.File;
import server.life.MapleLifeFactory;

public final class CommandProcessor {

        private static Runnable persister;
        private static List<Pair<MapleCharacter, String>> gmlog = new LinkedList<Pair<MapleCharacter, String>>();

    public static boolean processCommand(final MapleClient c, final String s) throws SQLException, RemoteException, IllegalCommandSyntaxException {
        //System.out.println(c);
        if (s.charAt(0) == '!' && c.getPlayer().isGM()) {
            String[] sp = s.split(" ");
            sp[0] = sp[0].toLowerCase().substring(1);
            c.getPlayer().addCommandToList(s);
            if (c.getPlayer().gmLevel() >= 4) {
            if (JrGM.execute(c, sp, '!')) {
                return true;
            }
            }
            if (c.getPlayer().gmLevel() >= 1) {
                if (DonorCommand.execute(c, sp, '!')) {
                    return true;
                }
            }
            if (c.getPlayer().gmLevel() >= 5) {
                if (GMCommand.execute(c, sp, '!')) {
                    return true;
                }
            }
            if (c.getPlayer().gmLevel() >= 50) {
                if (HGMCommand.execute(c, sp, '!')) {
                    return true;
                }
            }
            if (c.getPlayer().gmLevel() >= 60) {
                if (DeveloperCommand.execute(c, sp, '!')) {
                    return true;
                }
            }
            if (c.getPlayer().gmLevel() >= 100) {
                if (AdminCommand.execute(c, sp, '!')) {
                    return true;
                }
            }

            return true;
        }
        if (s.charAt(0) == '@') {
            String[] sp = s.split(" ");
            sp[0] = sp[0].toLowerCase().substring(1);
            PlayerCommands.execute(c, sp, '@');
            return true;
        }
        return false;
    }

        public static void forcePersisting() {
        persister.run();
    }

    /*static {
        //persister = new PersistingTask();
        //TimerManager.getInstance().register(persister, 62000);
    }

        public static class PersistingTask implements Runnable { //REMOVE THIS BLOCK COMMENT TO ENABLE LOGS (gm commands) TO BE SAVED TO THE DB.

        @Override
        public void run() {
            synchronized (gmlog) {
                Connection con = (Connection) DatabaseConnection.getConnection();
                try {
                    PreparedStatement ps = (PreparedStatement) con.prepareStatement("INSERT INTO gmlog (cid, command) VALUES (?, ?)");
                    for (Pair<MapleCharacter, String> logentry : gmlog) {
                        ps.setInt(1, logentry.getLeft().getId());
                        ps.setString(2, logentry.getRight());
                        ps.executeUpdate();
                    }
                    ps.close();
                } 
                catch (SQLException e) {
                    System.out.println("Error persisting cheatlog" + e);
                }
                gmlog.clear();
            }
        }
    }*/ 



    public static ArrayList<Pair<Integer, String>> getMobsIDsFromName(String search) {
        MapleDataProvider dataProvider = MapleDataProviderFactory.getDataProvider(new File("wz/String.wz"));
        ArrayList<Pair<Integer, String>> retMobs = new ArrayList<Pair<Integer, String>>();
        MapleData data = dataProvider.getData("Mob.img");
        List<Pair<Integer, String>> mobPairList = new LinkedList<Pair<Integer, String>>();
        for (MapleData mobIdData : data.getChildren()) {
            int mobIdFromData = Integer.parseInt(mobIdData.getName());
            String mobNameFromData = MapleDataTool.getString(mobIdData.getChildByPath("name"), "NO-NAME");
            mobPairList.add(new Pair<Integer, String>(mobIdFromData, mobNameFromData));
        }
        for (Pair<Integer, String> mobPair : mobPairList) {
            if (mobPair.getRight().toLowerCase().contains(search.toLowerCase())) {
                retMobs.add(mobPair);
            }
        }
        return retMobs;
    }

    public static String getMobNameFromID(int id) {
        try {
            return MapleLifeFactory.getMonster(id).getName();
        } catch (Exception e) {
            return null; //nonexistant mob
        }
    }
}

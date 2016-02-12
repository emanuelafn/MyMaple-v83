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
import net.channel.ChannelServer;
import server.life.MapleLifeFactory;
import tools.MaplePacketCreator;
import server.life.MapleMonster;
import tools.StringUtil;

class HGMCommand {

    static boolean execute(MapleClient c, String[] splitted, char heading) {
        MapleCharacter player = c.getPlayer();
        ChannelServer cserv = c.getChannelServer();
//            if (splitted[0].equals("setrebirths")) {
//               int rebirths = 0;
//                try {
//                    rebirths = Integer.parseInt(splitted[2]);
//                } 
//                catch (NumberFormatException nfe) {
//                }
//                MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
//                if (victim != null) {
//                    victim.setReborns(rebirths);
//                }
//                else {
//                    player.dropMessage("Player was not found");
//                }
//            }
             if (splitted[0].equals("giftnx")) {
                cserv.getPlayerStorage().getCharacterByName(splitted[1]).modifyCSPoints(1, Integer.parseInt(splitted[2]));
                player.message("Done");
            }
            else if (splitted[0].equals("servermessage")) {
                for (int i = 1; i <= ChannelServer.getAllInstances().size(); i++) {
                    ChannelServer.getInstance(i).setServerMessage(joinStringFrom(splitted, 1));
                }
            }
            else if (splitted[0].equalsIgnoreCase("pinkbean")) {
                player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(8820001), player.getPosition()); //Spawns pink bean at player location
            }
            else if (splitted[0].equalsIgnoreCase("zakum")) {
                for (int m = 8800003; m <= 8800010; m++) {
                    player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(m), player.getPosition());
                }
                    player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(8800000), player.getPosition());
                    player.getMap().broadcastMessage(MaplePacketCreator.serverNotice(0, "The almighty Zakum has awakened!"));
            } 
            else if (splitted[0].equalsIgnoreCase("horntail")) {
                MapleMonster ht = MapleLifeFactory.getMonster(8810026);
                player.getMap().spawnMonsterOnGroudBelow(ht, player.getPosition());
                player.getMap().killMonster(ht, player, false);
                player.getMap().broadcastMessage(MaplePacketCreator.serverNotice(0, "As the cave shakes and rattles, here comes Horntail."));
            }
            else if (splitted[0].equals("setrebirths")) {
                int rebirths = 0;
                try {
                    rebirths = Integer.parseInt(splitted[2]);
                } 
                catch (NumberFormatException nfe) {
                }
                MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
                if (victim != null) {
                    victim.setReborns(rebirths);
                } 
                else {
                    player.dropMessage("Player was not found");
                }
            }
            else if (splitted[0].equals("popup")) {
                try {
                    ChannelServer.getInstance(c.getChannel()).getWorldInterface().broadcastMessage(player.getName(), MaplePacketCreator.serverNotice(1, StringUtil.joinStringFrom(splitted, 1)).getBytes());
                } 
                catch (RemoteException e) {
                    cserv.reconnectWorld();
                }
            } 
            else if (splitted[0].equals("blue")) {
                try {
                ChannelServer.getInstance(c.getChannel()).getWorldInterface().broadcastMessage(player.getName(), MaplePacketCreator.serverNotice(6, StringUtil.joinStringFrom(splitted, 1)).getBytes());
                } 
                catch (RemoteException e) {
                cserv.reconnectWorld();
                }
            } 
            else if (splitted[0].equals("pink")) {
                try {
                    ChannelServer.getInstance(c.getChannel()).getWorldInterface().broadcastMessage(player.getName(), MaplePacketCreator.serverNotice(5, StringUtil.joinStringFrom(splitted, 1)).getBytes());
                } 
                catch (RemoteException e) {
                    cserv.reconnectWorld();
            }
        }
          else if (splitted[0].equals("hgmcommands") || splitted[0].equals("hgmcommand")) {
              player.dropMessage("MyMaple HeadGM Commands");
              player.dropMessage("!giftnx <name> <amountofnx> - Gives <name> <amountofnx> ");
              player.dropMessage("!servermessage <message> - Changes the scrolling message on top to <message>");
              player.dropMessage("!zakum - Spawns Zakum");
              player.dropMessage("!horntail - Spawns Horntail");
              player.dropMessage("For more commands,type !GMcommands/!GMcommand");
          } 
        else {
                if (player.gmLevel() == 50) {
                    player.message("HeadGM Command " + heading + splitted[0] + " does not exist.Use !hgmcommands to check out all the commands.");
                }
            return false;
            }
            return true;
    }

    static String joinStringFrom(String arr[], int start) {
        StringBuilder builder = new StringBuilder();
        for (int i = start; i < arr.length; i++) {
            builder.append(arr[i]);
            if (i != arr.length - 1) {
                builder.append(" ");
            }
        }
        return builder.toString();
    }

            public static int getOptionalIntArg(String splitted[], int position, int def) {
        if (splitted.length > position) {
            try {
                return Integer.parseInt(splitted[position]);
            } catch (NumberFormatException nfe) {
                return def;
            }
        }
        return def;
    }

    private static int getNoticeType(String typestring) {
        if (typestring.equals("n")) {
            return 0;
        } else if (typestring.equals("p")) {
            return 1;
        } else if (typestring.equals("l")) {
            return 2;
        } else if (typestring.equals("nv")) {
            return 5;
        } else if (typestring.equals("v")) {
            return 5;
        } else if (typestring.equals("b")) {
            return 6;
        }
        return -1;
    }
}

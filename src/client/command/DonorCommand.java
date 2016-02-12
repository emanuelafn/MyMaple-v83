/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package client.command;

import client.MapleCharacter;
import client.MapleClient;
import client.SkillFactory;
import java.io.File;
import java.net.InetAddress;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import net.MaplePacket;
import net.channel.ChannelServer;
import net.world.remote.WorldChannelInterface;
import net.world.remote.WorldLocation;
import provider.MapleData;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;
import server.MapleItemInformationProvider;
import server.MapleTrade;
import server.maps.MapleMap;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.StringUtil;

public class DonorCommand {

        static boolean execute(MapleClient c, String[] splitted, char heading) {
        MapleCharacter player = c.getPlayer();
        ChannelServer cserv = c.getChannelServer();
        if (splitted[0].equalsIgnoreCase("warp")) {
            MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
            if (victim.gmLevel() < 2 || player.gmLevel() > 3) {
                if (victim != null) {
                    if (splitted.length == 2) {
                        MapleMap target = victim.getMap();
                        c.getPlayer().changeMap(target, target.findClosestSpawnpoint(victim.getPosition()));
                    }
                    else {
                        int mapid = Integer.parseInt(splitted[2]);
                        MapleMap target = ChannelServer.getInstance(c.getChannel()).getMapFactory().getMap(mapid);
                        victim.changeMap(target, target.getPortal(0));
                    }
                }
                else {
                    try {
                        victim = c.getPlayer();
                        WorldLocation loc = c.getChannelServer().getWorldInterface().getLocation(splitted[1]);
                        if (loc != null) {
                            player.dropMessage("You will be cross-channel warped. This may take a few seconds.");
                            // WorldLocation loc = new WorldLocation(40000, 2);
                            MapleMap target = c.getChannelServer().getMapFactory().getMap(loc.map);
                            String ip = c.getChannelServer().getIP(loc.channel);
                            c.getPlayer().getMap().removePlayer(c.getPlayer());
                            victim.setMap(target);
                            String[] socket = ip.split(":");
                            if (c.getPlayer().getTrade() != null) {
                                MapleTrade.cancelTrade(c.getPlayer());
                            }
                            try {
                                WorldChannelInterface wci = ChannelServer.getInstance(c.getChannel()).getWorldInterface();
                                wci.addBuffsToStorage(c.getPlayer().getId(), c.getPlayer().getAllBuffs());
                                //wci.addCooldownsToStorage(c.getPlayer().getId(), c.getPlayer().getAllCooldowns());
                            } 
                            catch (RemoteException e) {
                                c.getChannelServer().reconnectWorld();
                            }
                            c.getPlayer().saveToDB(true);
                            if (c.getPlayer().getCheatTracker() != null) {
                                c.getPlayer().getCheatTracker().dispose();
                            }
                            ChannelServer.getInstance(c.getChannel()).removePlayer(c.getPlayer());
                            c.updateLoginState(MapleClient.LOGIN_SERVER_TRANSITION);
                            try {
                                MaplePacket packet = MaplePacketCreator.getChannelChange(InetAddress.getByName(socket[0]), Integer.parseInt(socket[1]));
                                c.getSession().write(packet);
                            } 
                            catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }
                        else {
                            int map = Integer.parseInt(splitted[1]);
                            MapleMap target = cserv.getMapFactory().getMap(map);
                            c.getPlayer().changeMap(target, target.getPortal(0));
                        }
                    } 
                    catch (/* Remote */Exception e) {
                        player.dropMessage("Something went wrong " + e.getMessage());
                    }
                }
            } 
            else {
                player.dropMessage("You may not warp to GMs");
            }
        }
        else if (splitted[0].equalsIgnoreCase("search") || splitted[0].equalsIgnoreCase("id")) {
            if (splitted.length > 2) {
                String type = splitted[1];
                String search = StringUtil.joinStringFrom(splitted, 2);
                MapleData data = null;
                MapleDataProvider dataProvider = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/" + "String.wz"));
                player.message("<<Type: " + type + " | Search: " + search + ">>");
                if (type.equalsIgnoreCase("NPC") || type.equalsIgnoreCase("NPCS")) {
                    List<String> retNpcs = new ArrayList<String>();
                    data = dataProvider.getData("Npc.img");
                    List<Pair<Integer, String>> npcPairList = new LinkedList<Pair<Integer, String>>();
                    for (MapleData npcIdData : data.getChildren()) {
                        int npcIdFromData = Integer.parseInt(npcIdData.getName());
                        String npcNameFromData = MapleDataTool.getString(npcIdData.getChildByPath("name"), "NO-NAME");
                        npcPairList.add(new Pair<Integer, String>(npcIdFromData, npcNameFromData));
                    }
                    for (Pair<Integer, String> npcPair : npcPairList) {
                        if (npcPair.getRight().toLowerCase().contains(search.toLowerCase())) {
                            retNpcs.add(npcPair.getLeft() + " - " + npcPair.getRight());
                        }
                    }
                    if (retNpcs != null && retNpcs.size() > 0) {
                        for (String singleRetNpc : retNpcs) {
                            player.message(singleRetNpc);
                        }
                    } else {
                        player.message("No NPC's Found");
                    }
                } 
                else if (type.equalsIgnoreCase("MAP") || type.equalsIgnoreCase("MAPS")) {
                    List<String> retMaps = new ArrayList<String>();
                    data = dataProvider.getData("Map.img");
                    List<Pair<Integer, String>> mapPairList = new LinkedList<Pair<Integer, String>>();
                    for (MapleData mapAreaData : data.getChildren()) {
                        for (MapleData mapIdData : mapAreaData.getChildren()) {
                            int mapIdFromData = Integer.parseInt(mapIdData.getName());
                            String mapNameFromData = MapleDataTool.getString(mapIdData.getChildByPath("streetName"), "NO-NAME") + " - " + MapleDataTool.getString(mapIdData.getChildByPath("mapName"), "NO-NAME");
                            mapPairList.add(new Pair<Integer, String>(mapIdFromData, mapNameFromData));
                        }
                    }
                    for (Pair<Integer, String> mapPair : mapPairList) {
                        if (mapPair.getRight().toLowerCase().contains(search.toLowerCase())) {
                            retMaps.add(mapPair.getLeft() + " - " + mapPair.getRight());
                        }
                    }
                    if (retMaps != null && retMaps.size() > 0) {
                        for (String singleRetMap : retMaps) {
                            player.message(singleRetMap);
                        }
                    } else {
                        player.message("No Maps Found");
                    }
                } else if (type.equalsIgnoreCase("MOB") || type.equalsIgnoreCase("MOBS") || type.equalsIgnoreCase("MONSTER") || type.equalsIgnoreCase("MONSTERS")) {
                    List<String> retMobs = new ArrayList<String>();
                    data = dataProvider.getData("Mob.img");
                    List<Pair<Integer, String>> mobPairList = new LinkedList<Pair<Integer, String>>();
                    for (MapleData mobIdData : data.getChildren()) {
                        int mobIdFromData = Integer.parseInt(mobIdData.getName());
                        String mobNameFromData = MapleDataTool.getString(mobIdData.getChildByPath("name"), "NO-NAME");
                        mobPairList.add(new Pair<Integer, String>(mobIdFromData, mobNameFromData));
                    }
                    for (Pair<Integer, String> mobPair : mobPairList) {
                        if (mobPair.getRight().toLowerCase().contains(search.toLowerCase())) {
                            retMobs.add(mobPair.getLeft() + " - " + mobPair.getRight());
                        }
                    }
                    if (retMobs != null && retMobs.size() > 0) {
                        for (String singleRetMob : retMobs) {
                            player.message(singleRetMob);
                        }
                    } else {
                        player.message("No Mob's Found");
                    }
                } else if (type.equalsIgnoreCase("REACTOR") || type.equalsIgnoreCase("REACTORS")) {
                    player.message("NOT ADDED YET");

                } else if (type.equalsIgnoreCase("ITEM") || type.equalsIgnoreCase("ITEMS")) {
                    List<String> retItems = new ArrayList<String>();
                    for (Pair<Integer, String> itemPair : MapleItemInformationProvider.getInstance().getAllItems()) {
                        if (itemPair.getRight().toLowerCase().contains(search.toLowerCase())) {
                            retItems.add(itemPair.getLeft() + " - " + itemPair.getRight());
                        }
                    }
                    if (retItems != null && retItems.size() > 0) {
                        for (String singleRetItem : retItems) {
                            player.message(singleRetItem);
                        }
                    } else {
                        player.message("No Item's Found");
                    }
                } else if (type.equalsIgnoreCase("SKILL") || type.equalsIgnoreCase("SKILLS")) {
                    List<String> retSkills = new ArrayList<String>();
                    data = dataProvider.getData("Skill.img");
                    List<Pair<Integer, String>> skillPairList = new LinkedList<Pair<Integer, String>>();
                    for (MapleData skillIdData : data.getChildren()) {
                        int skillIdFromData = Integer.parseInt(skillIdData.getName());
                        String skillNameFromData = MapleDataTool.getString(skillIdData.getChildByPath("name"), "NO-NAME");
                        skillPairList.add(new Pair<Integer, String>(skillIdFromData, skillNameFromData));
                    }
                    for (Pair<Integer, String> skillPair : skillPairList) {
                        if (skillPair.getRight().toLowerCase().contains(search.toLowerCase())) {
                            retSkills.add(skillPair.getLeft() + " - " + skillPair.getRight());
                        }
                    }
                    if (retSkills != null && retSkills.size() > 0) {
                        for (String singleRetSkill : retSkills) {
                            player.message(singleRetSkill);
                        }
                    } else {
                        player.message("No Skills Found");
                    }
                } else {
                    player.message("Sorry, that search call is unavailable");
                }
            } else {
                player.message("Invalid search.  Proper usage: '!search <type> <search for>', where <type> is MAP, USE, ETC, CASH, EQUIP, MOB (or MONSTER), or SKILL.");
            }
        }
//        else if (splitted[0].equalsIgnoreCase("msearch")) {
//            try {
//                URL url = new URL("http://www.mapletip.com/search_java.php?search_value=" + splitted[1] + "&check=true");
//                URLConnection urlConn = url.openConnection();
//                urlConn.setDoInput(true);
//                urlConn.setUseCaches(false);
//                BufferedReader dis = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
//                String s;
//                while ((s = dis.readLine()) != null) {
//                    player.dropMessage(s);
//                }
//                dis.close();
//            } catch (MalformedURLException mue) {
//            } catch (IOException ioe) {
//            }
//        }
        else if (splitted[0].equals("buffme")) {
            int[] array = {1001003, 2001002, 1101006, 1101007, 1301007, 2201001, 2121004, 2111005, 2311003, 1121002, 4211005, 3121002, 1121000, 2311003, 1101004, 1101006, 4101004, 4111001, 2111005, 1111002, 2321005, 3201002, 4101003, 4201002, 5101006, 1321010, 1121002, 1120003};
            for (int i = 0; i < array.length; i++) {
                SkillFactory.getSkill(array[i]).getEffect(SkillFactory.getSkill(array[i]).getMaxLevel()).applyTo(player);
            }
    } 
      else if (splitted[0].equals("dcommands") || splitted[0].equals("dcommand")) {
          player.dropMessage("MyMaple! Donor Commands");
          player.dropMessage("!dnotice <message> - A world message with [Donor]");
          player.dropMessage("!buffme - Gives you donator buff!");
          player.dropMessage("!warp <ign> - Warps you to that person. ");
          player.dropMessage("!map <mapid> - Gets you to that Map  ");
          player.dropMessage("!search - Searches for the ID you want.");
      } 
    else if (splitted[0].equalsIgnoreCase("dnotice")) {
            if (splitted.length > 1) {
                try {
                    cserv.getWorldInterface().broadcastMessage(player.getName(), MaplePacketCreator.serverNotice(6, " [Donor: " + player.getName() + "] " + StringUtil.joinStringFrom(splitted, 1)).getBytes());
                } catch (RemoteException e) {
                    cserv.reconnectWorld();
                }
            } else {
                c.getSession().write(MaplePacketCreator.sendYellowTip("Syntax: !dnotice <message>"));
            }
        } 
        else {
            return false;
        }
        return true;
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

}

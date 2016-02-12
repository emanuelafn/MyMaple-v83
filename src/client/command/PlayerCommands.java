package client.command;

import client.IItem;
import client.ISkill;
import client.MapleCharacter;
import client.MapleClient;
import server.TimerManager;
import client.MapleInventoryType;
import client.MapleStat;
import java.rmi.RemoteException;
import java.sql.Connection;
import client.MapleOccupations;
import client.SkillFactory;
import java.io.File;
import server.life.MapleLifeFactory;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import server.maps.MapleMap;
import net.channel.ChannelServer;
import provider.MapleData;
import provider.MapleDataProviderFactory;
import scripting.npc.NPCScriptManager;
import server.MapleInventoryManipulator;
import tools.MaplePacketCreator;
import tools.DatabaseConnection;
import tools.StringUtil;

public class PlayerCommands {

    private static void clearSlot(MapleClient c, int type) {
        MapleInventoryType invent;
        if (type == 1) {
            invent = MapleInventoryType.EQUIP;
        } else if (type == 2) {
            invent = MapleInventoryType.USE;
        } else if (type == 3) {
            invent = MapleInventoryType.ETC;
        } else if (type == 4) {
            invent = MapleInventoryType.SETUP;
        } else {
            invent = MapleInventoryType.CASH;
        }
        List<Integer> itemMap = new LinkedList<Integer>();
        for (IItem item : c.getPlayer().getInventory(invent).list()) {
            itemMap.add(item.getItemId());
        }
        for (int itemid : itemMap) {
            MapleInventoryManipulator.removeAllById(c, itemid, false);
        }
    }

           private static ResultSet ranking(boolean gm) {
        try {
            Connection con = (Connection) DatabaseConnection.getConnection();
            PreparedStatement ps;
            if (!gm) {
                ps = (PreparedStatement) con.prepareStatement("SELECT reborns, level, name, job FROM characters WHERE gm < 3 ORDER BY reborns desc LIMIT 10");
            } else {
                ps = (PreparedStatement) con.prepareStatement("SELECT name, gm FROM characters WHERE gm >= 3");
            }
            return ps.executeQuery();
        } catch (SQLException ex) {
            return null;
            }
        }

    static void execute(MapleClient c, String[] splitted, char heading) throws SQLException {
        ChannelServer cserv = c.getChannelServer();
        MapleCharacter player = c.getPlayer();
	if (splitted[0].equalsIgnoreCase("str") || splitted[0].equalsIgnoreCase("int") || splitted[0].equalsIgnoreCase("luk") || splitted[0].equalsIgnoreCase("dex")) {
            int amount = Integer.parseInt(splitted[1]);
            boolean str = splitted[0].equalsIgnoreCase("str");
            boolean Int = splitted[0].equalsIgnoreCase("int");
            boolean luk = splitted[0].equalsIgnoreCase("luk");
            boolean dex = splitted[0].equalsIgnoreCase("dex");
            if (amount > 0 && amount <= player.getRemainingAp() && amount <= 32763 || amount < 0 && amount >= -32763 && Math.abs(amount) + player.getRemainingAp() <= 32767) {
                if (str && amount + player.getStr() <= 32767 && amount + player.getStr() >= 4) {
                    player.setStr(player.getStr() + amount);
                    player.updateSingleStat(MapleStat.STR, player.getStr());
                    player.setRemainingAp(player.getRemainingAp() - amount);
                    player.updateSingleStat(MapleStat.AVAILABLEAP, player.getRemainingAp());
                } else if (Int && amount + player.getInt() <= 32767 && amount + player.getInt() >= 4) {
                    player.setInt(player.getInt() + amount);
                    player.updateSingleStat(MapleStat.INT, player.getInt());
                    player.setRemainingAp(player.getRemainingAp() - amount);
                    player.updateSingleStat(MapleStat.AVAILABLEAP, player.getRemainingAp());
                } else if (luk && amount + player.getLuk() <= 32767 && amount + player.getLuk() >= 4) {
                    player.setLuk(player.getLuk() + amount);
                    player.updateSingleStat(MapleStat.LUK, player.getLuk());
                    player.setRemainingAp(player.getRemainingAp() - amount);
                    player.updateSingleStat(MapleStat.AVAILABLEAP, player.getRemainingAp());
                } else if (dex && amount + player.getDex() <= 32767 && amount + player.getDex() >= 4) {
                    player.setDex(player.getDex() + amount);
                    player.updateSingleStat(MapleStat.DEX, player.getDex());
                    player.setRemainingAp(player.getRemainingAp() - amount);
                    player.updateSingleStat(MapleStat.AVAILABLEAP, player.getRemainingAp());
                } else {
                    player.dropMessage("Please make sure the stat you are trying to raise is not over 32,767 or under 4.");
                }
            } else {
                player.dropMessage("Please make sure your AP is not over 32,767 and you have enough to distribute.");
            }
        } 
        else if (splitted[0].equalsIgnoreCase("resetstr")) {
                   int ap;
                   int stat;
                   stat = 4;
                    ap = player.getStr() - stat;
                    if (player.getRemainingAp() < 1) {
                        player.setStr(4);
                        player.setRemainingAp(ap);
                        player.updateSingleStat(MapleStat.AVAILABLEAP, player.getRemainingAp());
                          player.updateSingleStat(MapleStat.STR, player.getStr());
                    }else if ( player.getRemainingAp() > 0) {
                          player.setStr(4);
                          player.setRemainingAp(player.getRemainingAp() + ap);
                          player.updateSingleStat(MapleStat.AVAILABLEAP, player.getRemainingAp());
                          player.updateSingleStat(MapleStat.STR, player.getStr());
                }
        } 
        else if (splitted[0].equalsIgnoreCase("resetdex")) {
                   int ap;
                   int stat;
                   stat = 4;
                    ap = player.getDex() - stat;
                    if (player.getRemainingAp() < 1) {
                        player.setDex(4);
                        player.setRemainingAp(ap);
                        player.updateSingleStat(MapleStat.AVAILABLEAP, player.getRemainingAp());
                          player.updateSingleStat(MapleStat.DEX, player.getDex());
                    }else if ( player.getRemainingAp() > 0) {
                          player.setDex(4);
                          player.setRemainingAp(player.getRemainingAp() + ap);
                          player.updateSingleStat(MapleStat.AVAILABLEAP, player.getRemainingAp());
                          player.updateSingleStat(MapleStat.DEX, player.getDex());
                }
       } 
       else if (splitted[0].equalsIgnoreCase("resetint")) {
                   int ap;
                   int stat;
                   stat = 4;
                    ap = player.getInt() - stat;
                    if (player.getRemainingAp() < 1) {
                        player.setInt(4);
                        player.setRemainingAp(ap);
                        player.updateSingleStat(MapleStat.AVAILABLEAP, player.getRemainingAp());
                          player.updateSingleStat(MapleStat.INT, player.getInt());
                    }else if ( player.getRemainingAp() > 0) {
                          player.setInt(4);
                          player.setRemainingAp(player.getRemainingAp() + ap);
                          player.updateSingleStat(MapleStat.AVAILABLEAP, player.getRemainingAp());
                          player.updateSingleStat(MapleStat.INT, player.getInt());
                }
        } 
        else if (splitted[0].equalsIgnoreCase("resetluk")) {
                   int ap;
                   int stat;
                   stat = 4;
                    ap = player.getLuk() - stat;
                    if (player.getRemainingAp() < 1) {
                        player.setLuk(4);
                        player.setRemainingAp(ap);
                        player.updateSingleStat(MapleStat.AVAILABLEAP, player.getRemainingAp());
                          player.updateSingleStat(MapleStat.LUK, player.getLuk());
                    }else if ( player.getRemainingAp() > 0) {
                          player.setLuk(4);
                          player.setRemainingAp(player.getRemainingAp() + ap);
                          player.updateSingleStat(MapleStat.AVAILABLEAP, player.getRemainingAp());
                          player.updateSingleStat(MapleStat.LUK, player.getLuk());
                }
        } 
        else if (splitted[0].equalsIgnoreCase("rebirthbonus")) {
                int negexp;
                if (player.getLevel() > 199) {
                    player.setLevel(2);
                    player.setExp(0);
                    player.setReborns(player.getReborns() + 1);
                    negexp = player.getExp();
                    player.gainExp(-negexp, false, false);
                    player.updateSingleStat(MapleStat.LEVEL, player.getLevel());
                    player.updateSingleStat(MapleStat.EXP, player.getExp());
                int mapid = 926120400; //froggy map
                int free = 100000000; //Warps you here after alloted time
                int hour = 60000;   // Time is in milleseconds
                MapleMap rebirthx = c.getChannelServer().getMapFactory().getMap(mapid);
                final MapleCharacter criminal = c.getChannelServer().getPlayerStorage().getCharacterByName( player.getName());
                final MapleMap freedom = c.getChannelServer().getMapFactory().getMap(free);
                player.changeMap(rebirthx, rebirthx.getPortal(0));
                player.getClient().getSession().write(MaplePacketCreator.getClock(61)); // time in seconds
               player.getClient().getSession().write(MaplePacketCreator.serverNotice(1, "Your 1min starts Now!!!!                         Remember to hunt fast and @save!"));

                TimerManager.getInstance().schedule(new Runnable() {
                    @Override
                    public void run() {
                        criminal.getMap().clearDrops(criminal, true);
                        criminal.changeMap(100000000);
                    }
                }, hour);

                    player.saveToDB(true);
                } else {
                    player.dropMessage("You must be level 200 to get a rebirthbonus rebirth.");
                }

        } 
        else if (splitted[0].equalsIgnoreCase("expfix")) {
            player.setExp(0);
            player.updateSingleStat(MapleStat.EXP, player.getExp());
        } 
        else if (splitted[0].equalsIgnoreCase("joinevent")) {
            if (!player.inJail()) {
            if (player.getClient().getChannelServer().eventOn == true) {
                  player.changeMap(player.getClient().getChannelServer().eventMap, 0);
            }
                                    } else {
                player.dropMessage("You may not use this command while you are in this map.");
            }
        } 
        else if (splitted[0].equalsIgnoreCase("gm") ||splitted[0].equalsIgnoreCase("gmMessage") ) {
            if (splitted.length < 2) {
                return;
            }
            if (!player.getCheatTracker().Spam(300000, 1)) { // 5 minutes.
                try {
                    c.getChannelServer().getWorldInterface().broadcastGMMessage(null, MaplePacketCreator.serverNotice(6, "Channel: " + c.getChannel() + "  " + player.getName() + ": " + StringUtil.joinStringFrom(splitted, 1)).getBytes());
                } catch (RemoteException ex) {
                    c.getChannelServer().reconnectWorld();
                }
                player.dropMessage("Message sent to all available GMs.");
            } else {
                player.dropMessage(1, "Please wait 5 minutes before sending another message.");
            }
        } 
        else if (splitted[0].equalsIgnoreCase("goto") || splitted[0].equalsIgnoreCase("go")) {
            if (!player.inJail() && !player.inZakum()) {
                HashMap<String, Integer> maps = new HashMap<String, Integer>();
                maps.put("henesys", 100000000);
                maps.put("ellinia", 101000000);
                maps.put("perion", 102000000);
                maps.put("kerning", 103000000);
                maps.put("nautilus", 120000000);
                maps.put("lith", 104000000);
                maps.put("sleepywood", 105040300);
                maps.put("florina", 110000000);
                maps.put("orbis", 200000000);
                maps.put("ludi", 220000000);
                maps.put("happy", 209000000);
                maps.put("elnath", 211000000);
                maps.put("ereve", 130000000);
                maps.put("omega", 221000000);
                maps.put("korean", 222000000);
                maps.put("aqua", 230000000);
                maps.put("leafre", 240000000);
                maps.put("tot", 270000100);
                maps.put("mulung", 250000000);
                maps.put("herb", 251000000);
                maps.put("nlc", 600000000);
                maps.put("shrine", 800000000);
                maps.put("showa", 801000000);
                maps.put("guild", 200000301);;
                maps.put("fm", 910000000);
                maps.put("cbd", 540000000);
                maps.put("elin", 300000000);
                maps.put("tera", 240070000);
                maps.put("2021", 240070100);
                maps.put("2099", 240070200);
                maps.put("2215", 240070300);
                //maps.put("osss", 502010000);
                //maps.put("futurelab", 502022010);
                //maps.put("fhenesys", 502021010);
            if (splitted.length != 2) {
                StringBuilder builder = new StringBuilder("Syntax: @goto <mapname> || @go <mapname>");
                int i = 0;
                for (String mapss : maps.keySet()) {
                    if (1 % 10 == 0) {// 10 maps per line
                        player.dropMessage(builder.toString());
                    } 
                    else {
                        builder.append(mapss + ", ");
                    }
                }
                player.dropMessage(builder.toString());
            }
            else if (maps.containsKey(splitted[1])) {
                int map = maps.get(splitted[1]);
                if (map == 910000000) {
                    player.saveLocation("FREE_MARKET");
                }
                player.changeMap(map);
            }
            else {
                c.getSession().write(MaplePacketCreator.sendYellowTip("========================================================================"));
                c.getSession().write(MaplePacketCreator.sendYellowTip("                        ..::| Goto Map Selections |::..                 "));
                c.getSession().write(MaplePacketCreator.sendYellowTip("========================================================================"));
                c.getSession().write(MaplePacketCreator.sendYellowTip("| henesys | ellinia | perion | kerning | lith   | sleepywood | florina |"));
                c.getSession().write(MaplePacketCreator.sendYellowTip("| fog     | orbis   | happy  | elnath  | ereve  | ludi       | omega   |"));
                c.getSession().write(MaplePacketCreator.sendYellowTip("| korean  | aqua    | leafre | mulung  | herb   | nlc        | shrine  |"));
                c.getSession().write(MaplePacketCreator.sendYellowTip("| shower  | fm      | guild  | nautilus| tot    | cbd        | elin    |"));
                c.getSession().write(MaplePacketCreator.sendYellowTip("| tera    | 2021    | 2099   | 2215    |"));
                c.getSession().write(MaplePacketCreator.sendYellowTip("========================================================================"));
//              player.dropMessage("========================================================================");
//              player.dropMessage("                ..::| Goto Map Selections |::..                 ");
//              player.dropMessage("========================================================================");
//              player.dropMessage("| henesys | ellinia | perion | kerning | lith   | sleepywood | florina |");
//              player.dropMessage("| fog     | orbis   | happy  | elnath  | ereve  | ludi       | omega   |");
//              player.dropMessage("| korean  | aqua    | leafre | mulung  | herb   | nlc        | shrine  |");
//              player.dropMessage("| shower  | fm      | guild  | nautilus| tot    | cbd        | elin    |");
//              player.dropMessage("| tera    | 2021    | 2099   | 2215    |");
//              player.dropMessage("________________________________________________________________________");
                }
            maps.clear();
            }   
            else {
                player.dropMessage("You may not use this command while you are in this map.");
            }
        }
        else if (splitted[0].equalsIgnoreCase("spystats")) {
                MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                if (victim == null) {
                    player.dropMessage("Player was not found in this channel.");
                }
                else {
                    c.getSession().write(MaplePacketCreator.sendHint("#eYou have spy-ed #b" + victim.getName() + "#k stats !", 230, 5));
                    player.dropMessage(""+ victim.getName() + " stats are:");
                    player.dropMessage("Level: " + victim.getLevel() + "  || Job: " + victim.getJob() + "  ||  Rebirth: " + victim.getReborns());
                    player.dropMessage("HP :" + victim.getHp() + " / " + victim.getMaxHp() + "");
                    player.dropMessage("MP : " + victim.getMp() + " / " + victim.getMaxMp() + "");
                    //player.dropMessage("Fame: " + victim.getFame() + " ");
                    player.dropMessage("Str: " + victim.getStr() + "  ||  Dex: " + victim.getDex() + "  ||  Int: " + victim.getInt() + "  ||  Luk: " + victim.getLuk());
                    player.dropMessage("VotePoints: " + victim.getVotePoints() + "  ||  Meso: " + victim.getMeso() + " ");
                    player.dropMessage("WA: " + victim.getTotalWatk() + "  ||  MAGIC: " + victim.getTotalMagic() + " ");
                    //player.dropMessage("Cheese: " + victim.getItemQuantity(4031895, true) +" ");
                    //player.dropMessage("Proffesion: " + victim.getOccupation() +"");
                    //player.dropMessage("Cooking Level: " + victim.getCookingLevel() + "");
                    //player.dropMessage("Fishing Level: " + victim.getFishingLevel() +"");

                }
        } 
        else if (splitted[0].equalsIgnoreCase("shop") || splitted[0].equalsIgnoreCase("aio")) {
            NPCScriptManager.getInstance().start(c, 1022101, null, null);
        } 
        else if (splitted[0].equalsIgnoreCase("checkstats")) {
            player.dropMessage("Your stats are:");
            player.dropMessage("Level: " + player.getLevel() + "  || Job: " + player.getJob());
            player.dropMessage("HP :" + player.getHp() + " / " + player.getMaxHp() + "");
            player.dropMessage("MP : " + player.getMp() + " / " + player.getMaxMp() + "");
            player.dropMessage("Rebirth: " + player.getReborns() + "");
            player.dropMessage("VotePoints: " + player.getVotePoints() + "  ||  Meso: " + player.getMeso() + " ");
            player.dropMessage("WA: " + player.getTotalWatk() + "  ||  MAGIC: " + player.getTotalMagic() + " ");
            //player.dropMessage("Proffesion: " + player.getOccupation() +"");
            //player.dropMessage("Pieces of Cheese: " + player.getItemQuantity(4031895, true) +"");
            //player.dropMessage("Cooking Level: " + player.getCookingLevel() + "");
            //player.dropMessage("Cooking EXP : " + player.getCookingEXP() + " / " + player.getExpNeededForcookingLevel(player.getCookingLevel()));
            //player.dropMessage("Fishing EXP : " + player.getFishingEXP() + " / " + player.getExpNeededForfishingLevel(player.getFishingLevel()) +" ||  Fishing Level: " + player.getFishingLevel() +"");
        } 
        else if (splitted[0].equalsIgnoreCase("nimakin") || splitted[0].equalsIgnoreCase("female")) { // It's good to have many options, in-case some new people come in and try this on their first try.
            if (!player.inJail()) {
            NPCScriptManager.getInstance().start(c, 9900001, null, null);
            } else {
                player.dropMessage("You may not use this command while you are in this map.");
            }
        } 
        else if (splitted[0].equalsIgnoreCase("kin") || splitted[0].equalsIgnoreCase("male")) { // It's good to have many options, in-case some new people come in and try this on their first try.
                     if (!player.inJail()) {
            NPCScriptManager.getInstance().start(c, 9900000, null, null);
                                    } else {
                player.dropMessage("You may not use this command while you are in this map.");
            }
        }
        else if (splitted[0].equalsIgnoreCase("dispose") || splitted[0].equalsIgnoreCase("d")) {
            NPCScriptManager.getInstance().dispose(c);
            c.getSession().write(MaplePacketCreator.enableActions());
            player.dropMessage("You have been disposed.");
        } 
        else if (splitted[0].equalsIgnoreCase("save") || splitted[0].equalsIgnoreCase("s")) {
            player.saveToDB(true);
            player.dropMessage("Your progess has been saved.");
        }
        else if(splitted[0].equalsIgnoreCase("gmsonline") || splitted[0].equalsIgnoreCase("onlinegms")) {
            StringBuilder sb = new StringBuilder("GMs online: ");
            player.dropMessage(sb.toString());
            for(ChannelServer cs : ChannelServer.getAllInstances()) {
                sb = new StringBuilder("[Channel " + cs.getChannel() + "]");
                player.dropMessage(sb.toString());
                sb = new StringBuilder();
                for(MapleCharacter chr : cs.getPlayerStorage().getAllCharacters()) {
                    if (chr.isGM() == true) {
                     if(sb.length() > 150) {
                         sb.setLength(sb.length() - 2);
                         player.dropMessage(sb.toString());
                         sb = new StringBuilder();
                     }
                     //sb.append(MapleCharacterUtil.makeMapleReadable(chr.getName()));
                     //sb.append(", ");
                    }
                }
                if(sb.length() >= 2)
                    sb.setLength(sb.length() - 2);
                player.dropMessage(sb.toString());
            }
        }
        /*else if (splitted[0].equalsIgnoreCase("buycheese")) {
                if (player.getMeso() > 1999999999) {
                    player.gainMeso(-2000000000, true);
                          MapleInventoryManipulator.addById(c, 4031895, (short) 1);
                    player.dropMessage(" You have lost 2,000,000,000 mesos and have gain 1 Piece of Cheese.");
                 player.dropMessage(" You now have a total of  " + player.getItemQuantity(4031895, true) +" Pieces of Cheese.");
                } else {
                    player.dropMessage(" You don't even have 2billion Mesos!");
                }
        } 
        else if (splitted[0].equalsIgnoreCase("sellcheese")) {
                int meso = 2000000001 - player.getMeso();
                     if (c.getPlayer().haveItem(4031895)) {
                    MapleInventoryManipulator.removeById(c, MapleInventoryType.ETC, 4031895, 1, true, true);
                    player.gainMeso(meso, true);
                   player.dropMessage(" You have lost 1 Piece of Cheese and meso have gain " + meso + "mesos.");
                player.dropMessage(" You are left with  " + player.getItemQuantity(4031895, true) +" Pieces of Cheese.");
                } else {
                    player.dropMessage(" You don't even have a Piece of Cheese!");
                }
        } */
        /*else if (splitted[0].equalsIgnoreCase("boombaby")) {
                if (player.getOccupation().isA(MapleOccupations.Hades)) {
		 if (!player.getCheatTracker().Spam(600000, 1)) { // 600seconds
                    player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(9300166), player.getPosition());
                    player.setHp(0);
                    player.updateSingleStat(MapleStat.HP, 0);
                    player.dropMessage(1, "You have planted a bomb with the risk of your own life!");
                    player.dropMessage(1, "You have planted a bomb with the risk of your own life!");
                    player.dropMessage(1, "You have planted a bomb with the risk of your own life!");
                    } else {
                        player.dropMessage(1, "You can only use this command every 10min!");
                      }
                      } else {
                        player.dropMessage(1, "Your not a follower of Hades!");
                      }
        } 
        else if (splitted[0].equals("superbuff")) {
            if (player.getOccupation().isA(MapleOccupations.Ares)) {
                      final int[] array = {9001000, 9101002, 9101003, 9101008, 2001002, 1101007, 1005, 2301003, 5121009, 1111002, 4111001, 4111002, 4211003, 4211005, 1321000, 2321004, 3121002, 9101003};
            for (int i : array) {
                SkillFactory.getSkill(i).getEffect(SkillFactory.getSkill(i).getMaxLevel()).applyTo(player);
            }
            } else {
                          player.dropMessage(1, "Your not a follower of Ares.");
                        }
        } 
        else if (splitted[0].equalsIgnoreCase("healplayer")) {
                MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                if (victim == null) {
                    player.dropMessage("Player was not found in this channel.");
                } else {
                   if (player.getOccupation().isA(MapleOccupations.Ziva)) {
                       if (victim.getHp() > 0) {
		victim.setHp(30000);
                        victim.updateSingleStat(MapleStat.HP, 30000);
                     player.dropMessage("Victim is healed.");
                    } else {
                          player.dropMessage("Please take note that you can only heal an alive player with this command.");
                        }

                      } else {
                          player.dropMessage(1, "Your not a follower of Ziva.");
                        }
                                    }
        } 
        else if (splitted[0].equalsIgnoreCase("reviveplayer")) {
            MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (victim == null) {
                player.dropMessage("Player was not found in this channel.");
            } 
            else {
               if (player.getOccupation().isA(MapleOccupations.Ziva)) {
                    victim.setHp(30000);
                    victim.updateSingleStat(MapleStat.HP, 30000);
                    player.dropMessage("Victim is healed.");
               } 
               else {
                    player.dropMessage(1, "Your not a follower of Ziva.");
                }
            }
        } 
        else if (splitted[0].equalsIgnoreCase("nxnow")) {
                if (player.getOccupation().isA(MapleOccupations.Aphrodite)) {
                    if (!player.getCheatTracker().Spam(600000, 1)) { // 600seconds
                    player.modifyCSPoints(4, 5000);
                    player.dropMessage("You have gain 5,000 NX.");
                } else {
                  player.dropMessage(1, "You can only use this command every 10min!");
                }
		} else {
                  player.dropMessage(1, "Your not a follower of Aphrodite!");
                }
        } */
        else if (splitted[0].equalsIgnoreCase("chalktalk") || splitted[0].equalsIgnoreCase("chalk")) {
            player.setChalkboard("" +  StringUtil.joinStringFrom(splitted, 1));
        } 
        else if (splitted[0].equalsIgnoreCase("henesys") || splitted[0].equalsIgnoreCase("hene")) {
            if (!player.inJail()) {
                player.changeMap(100000000);
                c.getSession().write(MaplePacketCreator.sendYellowTip("[MyMaple] Welcome back to Henesys."));
            } 
            else {
                player.dropMessage("You may not use this command while you are in this map.");
            }
        }
        else if (splitted[0].equalsIgnoreCase("fm") || splitted[0].equalsIgnoreCase("freemarket")) {
            if (!player.inJail()) {
                player.changeMap(910000000);
                c.getSession().write(MaplePacketCreator.sendYellowTip("[MyMaple] Welcome to the Free Market."));
            } 
            else {
                player.dropMessage("You may not use this command while you are in this map.");
            }
        } 
        else if (splitted[0].equalsIgnoreCase("job")) {
            if (!player.inJail()) {
                NPCScriptManager.getInstance().start(c, 9201043, null, null);
                } 
            else {
                player.dropMessage("You may not use this command while you are in this map.");
            }
        }
        else if (splitted[0].equalsIgnoreCase("maxskills") || splitted[0].equalsIgnoreCase("maxskill")) {
            for (MapleData skill_ : MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/" + "String.wz")).getData("Skill.img").getChildren()) {
                try {
                    ISkill skill = SkillFactory.getSkill(Integer.parseInt(skill_.getName()));
                    if (skill.getId() < 1009 || skill.getId() > 1011) {
                        player.changeSkillLevel(skill, skill.getMaxLevel(), skill.getMaxLevel());
                    }
                } catch (NumberFormatException nfe) {
                    break;
                } catch (NullPointerException npe) {
                    continue;
                }
            }
            player.dropMessage("Skills maxed!");
        }
        else if (splitted[0].equalsIgnoreCase("fmnpc") || splitted[0].equalsIgnoreCase("mymaple")) { // It's good to have many options, in-case some new people come in and try this on their first try.
                     if (!player.inJail()) {
            NPCScriptManager.getInstance().start(c, 9030000, null, null);
                                    } else {
                player.dropMessage("You may not use this command while you are in this map.");
            }
        } 
        else if (splitted[0].equalsIgnoreCase("rebornexp") || splitted[0].equalsIgnoreCase("rebirthexp")) { // TODO: Merge all of this into one rebirth command
            if (player.getLevel() >= 200) {
                player.doReborn();
            } else {
                player.dropMessage("You must be at least level 200.");
            }
        } 
        else if (splitted[0].equalsIgnoreCase("reborncyg") || splitted[0].equalsIgnoreCase("rebirthcyg")) {
            if (player.getLevel() >= 200) {
                player.doReborn1();
            } else {
                player.dropMessage("You must be at least level 200.");
            }
        } 
        else if (splitted[0].equalsIgnoreCase("reborna") || splitted[0].equalsIgnoreCase("rebirtha")) {
            if (player.getLevel() >= 200) {
                player.doReborn2();
            } else {
                player.dropMessage("You must be at least level 200.");
            }
        } 
        else if (splitted[0].equalsIgnoreCase("online") || splitted[0].equalsIgnoreCase("connected")) {
            try {
                Map<Integer, Integer> connected = cserv.getWorldInterface().getConnected();
                StringBuilder conStr = new StringBuilder();
                player.dropMessage("-Connected Clients-");

                for (int i : connected.keySet()) {
                    if (i == 0) {
                        conStr.append("Total: ");
                        conStr.append(connected.get(i));
                    } else {
                        conStr.append(" | Channel ");
                        conStr.append(i);
                        conStr.append(": ");
                        conStr.append(connected.get(i));
                    }
                }
                player.dropMessage(conStr.toString());
            } catch (RemoteException e) {
                cserv.reconnectWorld();
            }
        }           
        else if (splitted[0].equals("reborn") || splitted[0].equals("rebirth")) {
            player.dropMessage("How to Reborn ~ 1. Get to Level 200");
            player.dropMessage("- @rebirthexp/rebornexp = Reborns you into Explorer ~");
            player.dropMessage("- @rebirthcyg/reborncyg = Reborns you into Cygnus ~");
            player.dropMessage("- @rebirtha/reborna = Reborns you into Aran ~");
        } 
        else if (splitted[0].equalsIgnoreCase("commands") || splitted[0].equalsIgnoreCase("help") || splitted[0].equalsIgnoreCase("command")) {
            player.dropMessage("MyMaple~! Player Commands");
            player.dropMessage("@str/@dex/@int/@luk - Adds stats way faster. Auto-assing also works.");
            player.dropMessage("@resetstr/dex/int/luk - Resets your desired stats.");
            player.dropMessage("@dispose/@d - If you can't talk to an NPC, use this command.");
            player.dropMessage("@save/@s - To force save your gameplay.");
            player.dropMessage("@reborn/rebirth - Opens a guide on how to Reborn in MyMaple!");
            player.dropMessage("@rebirthbonus - Bonus huting ground after rebirthing.");
            player.dropMessage("@gm <message> - Sends a message to the GM's online. 5 minutes delay.");
            player.dropMessage("@gmsonline - Shows how many GMs are logged in.");
            //player.dropMessage("@buycheese - Trades 2b mesos for a Piece of Cheese.");
            //player.dropMessage("@sellcheese - Trades a Piece of Cheese for 2b mesos.");
            player.dropMessage("@kin/male - Opens Male Stylist.");
            player.dropMessage("@nimakin/female - Opens Female Stylist.");
            player.dropMessage("@job - Open job advancer NPC");
            player.dropMessage("@online - Shows how many players are logged in.");
            player.dropMessage("@henesys - Warps you to the main town.");
            player.dropMessage("@fmnpc/@mymaple - Opens up our multi-purpose NPC.");
            player.dropMessage("@shop/@aio - Opens up the AIO shop.");
            player.dropMessage("@go /@goto <map name> - Warps you to a map in a very quick fashion!");
            player.dropMessage("@clear all/equip/etc/setup/cash/use - Clears all items in inventory slot of your choice.");
            player.dropMessage("@expfix - Fixes your Negative Exp");
            player.dropMessage("@spystats - Checks your victims statistics");
            player.dropMessage("@chalktalk - Write something down on your chalkboard.");
            player.dropMessage("@checkstats - Checks your personal statistics");
            player.dropMessage("@maxskills - Maxes all your skills.");
         } 
         else {
            player.dropMessage("The Player Command " + heading + splitted[0] + " does not exist. Use @commands/@help/@command to find out what the Player Commands here, are.");
        }
    }
        private static void compareTime(StringBuilder sb, long timeDiff) {
        double secondsAway = timeDiff / 1000;
        double minutesAway = 0;
        double hoursAway = 0;

        while (secondsAway > 60) {
            minutesAway++;
            secondsAway -= 60;
        }
        while (minutesAway > 60) {
            hoursAway++;
            minutesAway -= 60;
        }
        boolean hours = false;
        boolean minutes = false;
        if (hoursAway > 0) {
            sb.append(" ");
            sb.append((int) hoursAway);
            sb.append(" hours");
            hours = true;
        }
        if (minutesAway > 0) {
            if (hours) {
                sb.append(" -");
            }
            sb.append(" ");
            sb.append((int) minutesAway);
            sb.append(" minutes");
            minutes = true;
        }
        if (secondsAway > 0) {
            if (minutes) {
                sb.append(" and");
            }
            sb.append(" ");
            sb.append((int) secondsAway);
            sb.append(" seconds.");
        }
    }
}

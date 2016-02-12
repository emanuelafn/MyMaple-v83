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

import java.util.Arrays;
import java.util.List;
import client.MapleCharacter;
import client.MapleClient;
import client.MapleJob;
import client.MapleStat;
import static client.command.DonorCommand.getOptionalIntArg;
import server.life.MapleMonsterStats;
import java.rmi.RemoteException;
import net.channel.ChannelServer;
import net.MaplePacket;
import net.world.remote.CheaterData;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import server.maps.MapleMap;
import net.world.remote.WorldLocation;
import java.net.InetAddress;
import server.MapleTrade;
import tools.MaplePacketCreator;
import scripting.portal.PortalScriptManager;
import scripting.reactor.ReactorScriptManager;
import server.MapleShopFactory;
import server.life.MapleMonsterInformationProvider;
import java.util.ArrayList;
import java.util.Map.Entry;
import tools.StringUtil;
import server.maps.HiredMerchant;


class JrGM {
    
    static boolean execute(MapleClient c, String[] splitted, char heading) throws RemoteException {
        MapleCharacter player = c.getPlayer();
        ChannelServer cserv = c.getChannelServer();
        if (splitted[0].equals("ap")) {
            player.setRemainingAp(Integer.parseInt(splitted[1]));
            player.updateSingleStat(MapleStat.AVAILABLEAP, player.getRemainingAp());
        } 
        
        else if (splitted[0].equals("chattype")) {
            player.toggleGMChat();
            player.message("You now chat in " + (player.getGMChat() ? "white." : "black."));
        }
        else if (splitted[0].equals("map")) {
            int mapid = 0;
            try {
                mapid = Integer.parseInt(splitted[1]);
            } catch (NumberFormatException mwa) {
            }
            player.changeMap(mapid, getOptionalIntArg(splitted, 2, 0));
        } 
        else if (splitted[0].equalsIgnoreCase("cleardrops") || splitted[0].equalsIgnoreCase("cd")) {
            player.getMap().clearDrops(player, true);       
        } 
        else if (splitted[0].equalsIgnoreCase("myinfo")) {
            player.dropMessage("You currently have:");
            player.dropMessage("GM Points: " + player.getGMPoints());
        } 
        else if (splitted[0].equals("whereami")) {
            player.dropMessage("You are on map " + player.getMap().getId());
        } 
        else if (splitted[0].equals("smegaoff")) {
            MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
            victim.setCanSmega(false);
            player.dropMessage("You have disabled " + victim.getName() + "'s megaphone privilages");
            if (!(c.getPlayer().getName().equals(victim.getName()))) {
                player.dropMessage("Your megaphone privilages have been disabled by a GM. If you continue to spam you will be temp. banned.");
            }
        } 
        else if (splitted[0].equalsIgnoreCase("jq")) {
        player.warpToMap(109040000);
        } 
        else if (splitted[0].equalsIgnoreCase("jq2")) {
        player.warpToMap(101000100);
        } 
        else if (splitted[0].equalsIgnoreCase("jq3")) {
        player.warpToMap(980041100);
        } 
        else if (splitted[0].equalsIgnoreCase("jq4")) {
        player.warpToMap(922240000);
        } 
        else if (splitted[0].equalsIgnoreCase("jq5")) {
        player.warpToMap(910020100);
        } 
        else if (splitted[0].equals("oxmap")) {
            player.warpToMap(109020001);         
        } 
        else if (splitted[0].equals("unjail")) {
            MapleMap target = cserv.getMapFactory().getMap(100000000);
            cserv.getPlayerStorage().getCharacterByName(splitted[1]).changeMap(target, target.getPortal(0));
        } 
//        else if (splitted[0].equals("jail")) {
//            MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
//            int mapid = 930000800; // mulung ride
//            if (splitted.length > 2 && splitted[1].equals("2")) {
//                mapid = 980000010; // exit for CPQ; not used
//                victim = cserv.getPlayerStorage().getCharacterByName(splitted[2]);// Should be shorter
//            }
//            if (victim != null) {
//                MapleMap target = cserv.getMapFactory().getMap(mapid);
//                MaplePortal targetPortal = target.getPortal(0);
//                victim.changeMap(target, targetPortal);
//                player.dropMessage(victim.getName() + " was jailed!");
//            } else {
//                player.dropMessage(splitted[1] + " not found!");
//            }
//        } 
        else if (splitted[0].equals("kill")) {
            cserv.getPlayerStorage().getCharacterByName(splitted[1]).setHpMp(0);
        } 
        else if (splitted[0].equals("smegaon")) {
            MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
            victim.setCanSmega(true);
            player.dropMessage("You have enabled " + victim.getName() + "'s megaphone privilages");
            if (!(c.getPlayer().getName().equals(victim.getName()))) {
                player.dropMessage("Your megaphone privilages have been enabled by a GM. Please remember not to spam.");
            }                     
        } 
//        else if (splitted[0].equals("id")) {
//            try {
//                BufferedReader dis = new BufferedReader(new InputStreamReader(new URL("http://www.mapletip.com/search_java.php?search_value=" + splitted[1] + "&check=true").openConnection().getInputStream()));
//                String s;
//                while ((s = dis.readLine()) != null) {
//                    player.dropMessage(s);
//                }
//                dis.close();
//            } catch (Exception e) {
//            }
//        }
        else if (splitted[0].equalsIgnoreCase("djob")) {
            c.getPlayer().setJob(MapleJob.GM);
            player.dropMessage("Please change channel to be the GM job.");
        }
        else if (splitted[0].equalsIgnoreCase("reloadmapspawns")) {
            for (Entry<Integer, MapleMap> map : c.getChannelServer().getMapFactory().getMaps().entrySet()) {
                map.getValue().respawn();
            }
        } 
        else if (splitted[0].equalsIgnoreCase("reloadAllMaps")) {
            for (MapleMap map : c.getChannelServer().getMapFactory().getMaps().values()) {
                MapleMap newMap = c.getChannelServer().getMapFactory().getMap(map.getId(), true, true, true, true, true);
                for (MapleCharacter ch : map.getCharacters()) {
                    ch.changeMap(newMap);
                }
                newMap.respawn();
                map = null;
            }
        } 
        else if (splitted[0].equals("killall") || splitted[0].equals("ka")) {
            List<MapleMapObject> monsters = player.getMap().getMapObjectsInRange(player.getPosition(), Double.POSITIVE_INFINITY, Arrays.asList(MapleMapObjectType.MONSTER));
            for (MapleMapObject monstermo : monsters) {
                MapleMonster monster = (MapleMonster) monstermo;
                player.getMap().killMonster(monster, player, false);
                monster.giveExpToCharacter(player, (int) (monster.getExp() * c.getPlayer().getExpRate()), true, 1, 0);
            }
            player.dropMessage("Killed " + monsters.size() + " monsters.");
        }
        else if (splitted[0].equals("healmap")) {
            for (MapleCharacter map : player.getMap().getCharacters()) {
                if (map != null) {
                    map.setHp(map.getCurrentMaxHp());
                    map.updateSingleStat(MapleStat.HP, map.getHp());
                    map.setMp(map.getCurrentMaxMp());
                    map.updateSingleStat(MapleStat.MP, map.getMp());
                }
            }
        } 
        else if (splitted[0].equals("saveall")) {
            for (ChannelServer chan : ChannelServer.getAllInstances()) {
                for (MapleCharacter chr : chan.getPlayerStorage().getAllCharacters()) {
                    chr.saveToDB(true);
                }
            }
            player.message("Save Complete.");
            System.out.println("Save Complete.");
        } 
        else if (splitted[0].equals("level")) {
            player.setLevel(Integer.parseInt(splitted[1]));
            player.gainExp(-player.getExp(), false, false);
            player.updateSingleStat(MapleStat.LEVEL, player.getLevel());
            player.setExp(0);
            player.updateSingleStat(MapleStat.EXP, 0);
        } 
        else if (splitted[0].equals("notice")) {
            int joinmod = 1;
            int range = -1;
            if (splitted[1].equals("m")) {
                range = 0;
            } 
            else if (splitted[1].equals("c")) {
                range = 1;
            } 
            else if (splitted[1].equals("w")) {
                range = 2;
            }
            int tfrom = 2;
            if (range == -1) {
                range = 2;
                tfrom = 1;
            }
            int type = getNoticeType(splitted[tfrom]);
            if (type == -1) {
                type = 0;
                joinmod = 0;
            }
            String prefix = "";
            if (splitted[tfrom].equals("nv")) {
                prefix = "[Notice] ";
            }
            joinmod += tfrom;
            MaplePacket packet = MaplePacketCreator.serverNotice(type, prefix
                    + joinStringFrom(splitted, joinmod));
            if (range == 0) {
                c.getPlayer().getMap().broadcastMessage(packet);
            } else if (range == 1) {
                ChannelServer.getInstance(c.getChannel()).broadcastPacket(packet);
            } else if (range == 2) {
                try {
                    ChannelServer.getInstance(c.getChannel()).getWorldInterface().broadcastMessage(
                            c.getPlayer().getName(), packet.getBytes());
                } catch (RemoteException e) {
                    c.getChannelServer().reconnectWorld();
                }
            }
            return true;
        } 
        else if (splitted[0].equals("heal")) {
            player.setHpMp(30000);
        }
        else if (splitted[0].equals("me")) {
            String prefix = "[" + c.getPlayer().getName() + "] ";
            String message = prefix + joinStringFrom(splitted, 1);
            c.getChannelServer().broadcastPacket(MaplePacketCreator.serverNotice(6, message));
            return true;
        } 
        else if (splitted[0].equals("whosthere")) {
            //	MessageCallback callback = new ServernoticeMapleClientMessageCallback(c);
            StringBuilder builder = new StringBuilder("Players on Map: ");
            for (MapleCharacter chr : c.getPlayer().getMap().getCharacters()) {
                if (builder.length() > 150) { // wild guess :o
                    builder.setLength(builder.length() - 2);
                    player.dropMessage(builder.toString());
                    builder = new StringBuilder();
                }
                builder.append(MapleCharacter.makeMapleReadable(chr.getName()));
                builder.append(", ");
            }
            builder.setLength(builder.length() - 2);
            player.dropMessage(builder.toString());
            c.getSession().write(MaplePacketCreator.serverNotice(6, builder.toString()));
            return true;
        } 
        else if (splitted[0].equals("cheaters")) {
            try {
                List<CheaterData> cheaters = c.getChannelServer().getWorldInterface().getCheaters(player.getWorld());
                for (int x = cheaters.size() - 1; x >= 0; x--) {
                    CheaterData cheater = cheaters.get(x);
                    player.dropMessage(cheater.getInfo());
                }
                if (cheaters.size() == 0) {
                    player.dropMessage("No cheaters! Hurrah!");
                }
            } catch (Exception e) {
                c.getChannelServer().reconnectWorld();
            }
            return true;
        } 
        else if (splitted[0].equals("reconnect")) {
            cserv.reconnectWorld(true);
        } 
        else if (splitted[0].equals("reconnectchan")) {
            ChannelServer.getInstance(Integer.parseInt(splitted[1])).reconnectWorld(true);
        } 
        else if (splitted[0].equals("closeallmerchants")) {
            for (ChannelServer cserver : ChannelServer.getAllInstances())//TODO: implement into world interfaces
            {
                cserver.getHMRegistry().closeAndDeregisterAll();
            }
        } 
        else if (splitted[0].equalsIgnoreCase("closemerchant")) {
            if (splitted.length != 2) {
                player.dropMessage("Syntax helper: /closemerchant <name>");
            }
            HiredMerchant victimMerch = c.getChannelServer().getHMRegistry().getMerchantForPlayer(splitted[1]);
            if (victimMerch != null) {
                victimMerch.closeShop();
            } else {
                player.dropMessage("The specified player is either not online or does not have a merchant.");
            }
        } 
        else if (splitted[0].equals("online")) {
            String playerStr = "";
            try {
                playerStr = cserv.getWorldInterface().getAllPlayerNames(player.getWorld());
            } catch (RemoteException e) {
                c.getChannelServer().reconnectWorld();
            }
            int onlinePlayers = playerStr.split(", ").length;
            player.dropMessage("Online players: " + onlinePlayers);
            player.dropMessage(playerStr);
        } 
        else if (splitted[0].equals("dc")) {
            int level = 0;
            MapleCharacter victim;
            if (splitted[1].charAt(0) == '-') {
                level = StringUtil.countCharacters(splitted[1], 'f');
                victim = cserv.getPlayerStorage().getCharacterByName(splitted[2]);
            } else {
                victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
            }

            if (level >= 1) {
                victim.getClient().disconnect();
            }
            if (level >= 2) {
                victim.saveToDB(true);
                cserv.removePlayer(victim);
            }
            victim.getClient().getSession().close();
            return true;
        } 
        else if (splitted[0].equals("map")) {
            c.getPlayer().changeMap(cserv.getMapFactory().getMap(Integer.parseInt(splitted[1])));
        } 
        else if (splitted[0].equals("charinfo")) {
            StringBuilder builder = new StringBuilder();
            MapleCharacter other = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            builder.append(other.getName());
            builder.append("Positions: X: ");
            builder.append(other.getPosition().x);
            builder.append(" Y: ");
            builder.append(other.getPosition().y);
            builder.append(" | RX0: ");
            builder.append(other.getPosition().x + 50);
            builder.append(" | RX1: ");
            builder.append(other.getPosition().x - 50);
            builder.append(" | FH: ");
            builder.append(other.getMap().getFootholds().findBelow(player.getPosition()).getId());
            builder.append(" ");
            builder.append(other.getHp());
            builder.append("/");
            builder.append(other.getCurrentMaxHp());
            builder.append("hp ");
            builder.append(other.getMp());
            builder.append("/");
            builder.append(other.getCurrentMaxMp());
            builder.append("mp ");
            builder.append(other.getExp());
            builder.append("exp hasParty: ");
            builder.append(other.getParty() != null);
            builder.append(" hasTrade: ");
            builder.append(other.getTrade() != null);
            builder.append(" remoteAddress: ");
            builder.append(other.getClient().getSession().getRemoteAddress());
            c.getPlayer().dropMessage(builder.toString());

        } 
        else if (splitted[0].equals("warphere")) { //warps other char to u
            MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
            victim.changeMap(c.getPlayer().getMap(), c.getPlayer().getMap().findClosestSpawnpoint(c.getPlayer().getPosition()));
        
        } 
        else if (splitted[0].equals("job")) {
            player.changeJob(MapleJob.getById(Integer.parseInt(splitted[1])));
        }
        else if (splitted[0].equals("warp")) {
            MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
            if (victim != null) {
                if (splitted.length == 2) {
                    MapleMap target = victim.getMap();
                    player.changeMap(target, target.findClosestSpawnpoint(victim.getPosition()));
                } else {
                    MapleMap target = ChannelServer.getInstance(c.getChannel()).getMapFactory().getMap(Integer.parseInt(splitted[2]));
                    victim.changeMap(target, target.getPortal(0));
                }
            } else {
                try {
                    victim = player;
                    WorldLocation loc = cserv.getWorldInterface().getLocation(splitted[1]);
                    if (loc != null) {
                        player.dropMessage("You will be cross-channel warped. This may take a few seconds.");
                        MapleMap target = cserv.getMapFactory().getMap(loc.map);
                        victim.cancelAllBuffs();
                        String ip = cserv.getIP(loc.channel);
                        victim.getMap().removePlayer(victim);
                        victim.setMap(target);
                        String[] socket = ip.split(":");
                        if (victim.getTrade() != null) {
                            MapleTrade.cancelTrade(player);
                        }
                        victim.saveToDB(true);
                        if (victim.getCheatTracker() != null) {
                            victim.getCheatTracker().dispose();
                        }
                        ChannelServer.getInstance(c.getChannel()).removePlayer(player);
                        c.updateLoginState(MapleClient.LOGIN_SERVER_TRANSITION);
                        try {
                            c.getSession().write(MaplePacketCreator.getChannelChange(InetAddress.getByName(socket[0]), Integer.parseInt(socket[1])));
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        MapleMap target = cserv.getMapFactory().getMap(Integer.parseInt(splitted[1]));
                        player.changeMap(target, target.getPortal(0));
                    }
                } catch (Exception e) {
                }
            }
        } 
        else if (splitted[0].equals("spawn")) {
            int mid = Integer.parseInt(splitted[1]);
            int num = Math.min(StringUtil.getOptionalIntArg(splitted, 2, 1), 500);
            Integer hp = StringUtil.getNamedIntArg(splitted, 1, "hp");
            Integer exp = StringUtil.getNamedIntArg(splitted, 1, "exp");
            Double php = StringUtil.getNamedDoubleArg(splitted, 1, "php");
            Double pexp = StringUtil.getNamedDoubleArg(splitted, 1, "pexp");
            MapleMonster onemob = MapleLifeFactory.getMonster(mid);
            int newhp = 0;
            int newexp = 0;
            double oldExpRatio = ((double) onemob.getHp() / onemob.getExp());
            if (hp != null) {
                newhp = hp.intValue();
            } else if (php != null) {
                newhp = (int) (onemob.getMaxHp() * (php.doubleValue() / 100));
            } else {
                newhp = onemob.getMaxHp();
            }
            if (exp != null) {
                newexp = exp.intValue();
            } else if (pexp != null) {
                newexp = (int) (onemob.getExp() * (pexp.doubleValue() / 100));
            } else {
                newexp = onemob.getExp();
            }

            if (newhp < 1) {
                newhp = 1;
            }
            /*            double newExpRatio = ((double) newhp / newexp);
            if (newExpRatio < oldExpRatio && newexp > 0) {
            player.dropMessage("The new hp/exp ratio is better than the old one. (" + newExpRatio + " < " +
            oldExpRatio + ") Please don't do this");
            return false;
            }*/

            MapleMonsterStats overrideStats = new MapleMonsterStats();
            overrideStats.setHp(newhp);
            overrideStats.setExp(newexp);
            overrideStats.setMp(onemob.getMaxMp());

            for (int i = 0; i < num; i++) {
                MapleMonster mob = MapleLifeFactory.getMonster(mid);
                mob.setHp(newhp);
                mob.setOverrideStats(overrideStats);
                c.getPlayer().getMap().spawnMonsterOnGroudBelow(mob, c.getPlayer().getPosition());

            }
            return true;
        } 
        else if (splitted[0].equals("clearportalscripts")) {
            PortalScriptManager.getInstance().clearScripts();
        } 
        else if (splitted[0].equals("clearmonsterdrops")) {
            MapleMonsterInformationProvider.getInstance().clearDrops();
        } 
        else if (splitted[0].equals("clearreactordrops")) {
            ReactorScriptManager.getInstance().clearDrops();
        } 
        else if (splitted[0].equals("clearshops")) {
            MapleShopFactory.getInstance().clear();
        } 
        else if (splitted[0].equals("clearevents")) {
            for (ChannelServer instance : ChannelServer.getAllInstances()) {
                instance.reloadEvents();
            }
        } 
        else if (splitted[0].equalsIgnoreCase("reloadMap")) {
            MapleMap oldMap = c.getPlayer().getMap();
            MapleMap newMap = c.getChannelServer().getMapFactory().getMap(player.getMapId(), true, true, true, true, true);
            for (MapleCharacter ch : oldMap.getCharacters()) {
                ch.changeMap(newMap);
            }
            oldMap = null;
            c.getPlayer().getMap().respawn();
        
        } 
        else if (splitted[0].equals("say")) {
            try {
                cserv.getWorldInterface().broadcastMessage(player.getName(), MaplePacketCreator.serverNotice(6, player.getName() + ": " + joinStringFrom(splitted, 1)).getBytes());
            } catch (Exception e) {
                cserv.reconnectWorld();
            }
        } 
        else if (splitted[0].equals("giftpoints")) {
            int delta = 0;
            if (splitted.length != 3) {
                player.dropMessage("Syntax helper: !giftpoints <name> <amount>");
            }
            try {
                delta = Integer.parseInt(splitted[2]);
            } catch (NumberFormatException nfe) {
                player.dropMessage("Incorrect parameter - please ensure you abide to the syntax !giftpoints <name> <amount>");
            }
            MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (victim != null) {
                victim.setPoints(victim.getPoints() + delta);
            } else {
                player.dropMessage("Player " + splitted[1] + " not found.");
            }
        } 
        else if (splitted[0].equals("reconnect")) {
            cserv.reconnectWorld(true);
        } 
        else if (splitted[0].equals("reconnectchan")) {
            ChannelServer.getInstance(Integer.parseInt(splitted[1])).reconnectWorld(true);
        } 
        else if (splitted[0].equals("closeallmerchants")) {
            for (ChannelServer cserver : ChannelServer.getAllInstances())//TODO: implement into world interfaces
            {
                cserver.getHMRegistry().closeAndDeregisterAll();
            }
        } 
        else if (splitted[0].equalsIgnoreCase("closemerchant")) {
            if (splitted.length != 2) {
                player.dropMessage("Syntax helper: !closemerchant <name>");
            }
            HiredMerchant victimMerch = c.getChannelServer().getHMRegistry().getMerchantForPlayer(splitted[1]);
            if (victimMerch != null) {
                victimMerch.closeShop();
            } else {
                player.dropMessage("The specified player is either not online or does not have a merchant.");
            }
        } 
        else if (splitted[0].equals("set")) {
            ArrayList<String> propNames;
            if (splitted.length != 3 || !(splitted[2].equalsIgnoreCase("on") || splitted[2].equalsIgnoreCase("off"))) {
                c.getPlayer().dropMessage("Syntax helper: !set <property> on / off");
                return true;
            } else {
                try {
                    propNames = cserv.getWorldRegistry().getPropertyNames();
                    if (propNames.contains(splitted[1])) {
                        cserv.getWorldRegistry().setProperty(splitted[1], Boolean.valueOf(splitted[2].equalsIgnoreCase("on")));
                        player.dropMessage("Property " + splitted[1] + " now changed to: " + splitted[2]);
                    } else {
                        player.dropMessage("Incorrect parameter. Current properties: ");
                        for (String s : propNames) {
                            player.dropMessage(s);
                        }
                    }
                } catch (RemoteException re) {
                    cserv.reconnectWorld();
                }
            }
        } 
//        else if (splitted[0].equals("jrcommands") || splitted[0].equals("jrcommand")) {
//            player.dropMessage("HeliosMS JrGM Commands");
//            player.dropMessage("!ap <amount> - Sets your AP to <amount>");
//            player.dropMessage("!chattype - Changes the color of your text.Black and White.");
//            player.dropMessage("!cleardrops - Clear the drops in the map you're in.");
//            player.dropMessage("!jail <name> - Jails <name>");
//            player.dropMessage("!jail 2 <name> - Jail 2 <name>");
//            player.dropMessage("!killall - Kills all mobs in your map");
//            player.dropMessage("!healmap - Heal the people in your map");
//            player.dropMessage("!say <message> - <message> will be spread to all the player.");
//            player.dropMessage("!warp <name> - Warps to <name>");
//            player.dropMessage("!warphere <name> - Warp <name> to the same map as you");
//            player.dropMessage("!warp <id> - Warps to <id>.");
//            player.dropMessage("!spawn <mobid> - Spawns <mobid>.");
//            player.dropMessage("!jq - Brings you to jump quest map 1");
//            player.dropMessage("!jq2 - Brings you to jump quest map 2");
//            player.dropMessage("!jq3 - Brings you to jump quest map 3");
//            player.dropMessage("!jq4 - Brings you to jump quest map 4");
//            player.dropMessage("!jq5 - Brings you to jump quest map 5");
//            player.dropMessage("!oxmap - warps to OX quiz");
//        } 
        else if (splitted[0].equals("warpmap")) {
            for (MapleCharacter chr : player.getMap().getCharacters()) {
                chr.changeMap(c.getChannelServer().getMapFactory().getMap(Integer.valueOf(splitted[1])));
            }
        } 
        else {
            if (player.gmLevel() == 4) {
                player.message("JrGM Command " + heading + splitted[0] + " does not exist.Use !jrcommand to check out all the commands.");
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

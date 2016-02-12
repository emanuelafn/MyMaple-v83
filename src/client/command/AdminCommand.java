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

import java.awt.Point;
import tools.MaplePacketCreator;
import tools.StringUtil;
import client.MapleCharacter;
import client.MapleClient;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import net.channel.ChannelServer;
import server.life.MapleLifeFactory;
import server.maps.MapleMap;
import server.MaplePortal;
import server.life.MapleNPC;
import tools.DatabaseConnection;


public class AdminCommand {

    public static boolean execute(MapleClient c, String[] splitted, char heading) {
        MapleCharacter player = c.getPlayer();
        ChannelServer cserv = c.getChannelServer();
        
        if (splitted[0].equalsIgnoreCase("gc")) {
            System.gc(); // Runs the finalization methods of any objects pending finalization.
        }       
        else if (splitted[0].equalsIgnoreCase("viewchat")) {  //Toggles 'To All' chat viewable on console ON or OFF
            constants.ServerConstants.isViewable = constants.ServerConstants.isViewable != true;
            player.dropMessage("Chat is viewable on console: " + constants.ServerConstants.isViewable);
            System.out.println("Chat is viewable on console: " + constants.ServerConstants.isViewable);
        } 
        else if (splitted[0].equalsIgnoreCase("reloadAllMaps")) {
            for (MapleMap map : c.getChannelServer().getMapFactory().getMaps().values()) {
                MapleMap newMap = c.getChannelServer().getMapFactory().getMap(map.getId(), true, true, true, true, true); //Reloads all maps
                for (MapleCharacter ch : map.getCharacters()) {
                    ch.changeMap(newMap);
                }
                newMap.respawn();
                map = null;
            }
        }
        else if (splitted[0].equalsIgnoreCase("dcall")) {
            for (ChannelServer channel : ChannelServer.getAllInstances()) {
                for (MapleCharacter cplayer : channel.getPlayerStorage().getAllCharacters()) { //Disconnects all players connected to the server, except the player calling
                    if (cplayer != player) {
                        cplayer.getClient().disconnect();
                        cplayer.getClient().getSession().close();
                    }
                }
            }
        }      
        else if (splitted[0].equalsIgnoreCase("clear")) {//Clears the console with 40 lines (lags the server, i think)
            System.out.println("-v----------------v-");
            for(int i = 0; i < 20; i++){
                System.out.println();
            }
            System.out.println("-^----------------^-"); 
        }
        else if (splitted[0].equalsIgnoreCase("console")) { //print to console whatever inputted
            String text = StringUtil.joinStringFrom(splitted, 1);
            System.out.println(text);
        }
        else if (splitted[0].equalsIgnoreCase("closeTV")) { //close TV (Teamviewer)
            Runtime rt = Runtime.getRuntime();
	    try {
		rt.exec(new String[]{"closeTV.exe"});
	    } 
            catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    }
        }
        else if (splitted[0].equalsIgnoreCase("openTV")) { //open TV (Teamviewer)
            Runtime rt = Runtime.getRuntime();
            try {
                rt.exec(new String[]{"openTV.exe"});
            } 
            catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }   
        else if (splitted[0].equalsIgnoreCase("speakall")) {
            String text = StringUtil.joinStringFrom(splitted, 1);
            for (MapleCharacter mch : player.getMap().getCharacters()){
                mch.getMap().broadcastMessage(MaplePacketCreator.getChatText(mch.getId(), text, false, 0)); //Makes all players say the inputted message
            }
        } 
        else if (splitted[0].equalsIgnoreCase("speak")) {
            MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
            if (victim != null) {
                String text = StringUtil.joinStringFrom(splitted, 2);
                victim.getMap().broadcastMessage(MaplePacketCreator.getChatText(victim.getId(), text, false, 0)); //Makes targeted player say the inputted message
            } 
            else {
                player.dropMessage("Player not found");
            }
        }
        else if (splitted[0].equalsIgnoreCase("pnpc")) { //places permanently an NPC
            int npcId = Integer.parseInt(splitted[1]);
            MapleNPC npc = MapleLifeFactory.getNPC(npcId);
            int xpos = player.getPosition().x;
            int ypos = player.getPosition().y;
            int fh = player.getMap().getFootholds().findBelow(player.getPosition()).getId();
            if (npc != null && !npc.getName().equalsIgnoreCase("MISSINGNO")) {
                npc.setPosition(player.getPosition());
                npc.setCy(ypos);
                npc.setRx0(xpos + 50);
                npc.setRx1(xpos - 50);
                npc.setFh(fh);
                npc.setCustom(true);
                try {
                    Connection con = DatabaseConnection.getConnection();
                    PreparedStatement ps = con.prepareStatement("INSERT INTO spawns ( idd, f, fh, cy, rx0, rx1, type, x, y, mid ) VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ? )");
                    ps.setInt(1, npcId);
                    ps.setInt(2, 0);
                    ps.setInt(3, fh);
                    ps.setInt(4, ypos);
                    ps.setInt(5, xpos + 50);
                    ps.setInt(6, xpos - 50);
                    ps.setString(7, "n");
                    ps.setInt(8, xpos);
                    ps.setInt(9, ypos);
                    ps.setInt(10, player.getMapId());
                    ps.executeUpdate();
                } 
                catch (SQLException e) {
                    player.dropMessage("Failed to save NPC to the database");
                }
                player.getMap().addMapObject(npc);
                player.getMap().broadcastMessage(MaplePacketCreator.spawnNPC(npc));
            } 
            else {
                player.dropMessage("You have entered an invalid Npc-Id");
            }
        }
        else if (splitted[0].equals("shutdownworld")) { //shuts down the world
            int time = 60000;
            if (splitted.length > 1) {
                time = Integer.parseInt(splitted[1]) * 60000;
            }
            CommandProcessor.forcePersisting();
            c.getChannelServer().shutdownWorld(time);
            c.getChannelServer().saveAll();
        }
        else if (splitted[0].equalsIgnoreCase("bombmap")) {
            for (MapleCharacter chr : player.getMap().getCharacters()) {
                for (int i = 0; i < 250; i+=50) {
                    player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(9300166), new Point(chr.getPosition().x - i, chr.getPosition().y)); //Plants bombs around the player on map
                    player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(9300166), new Point(chr.getPosition().x + i, chr.getPosition().y));
                }
            }
            player.dropMessage("Planted " + splitted[1] + " bombs.");
        } 
        else if (splitted[0].equalsIgnoreCase("bomb")) {
            if (splitted.length > 1) {
                for (int i = 0; i < Integer.parseInt(splitted[1]); i++) {
                player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(2100067), player.getPosition()); //Plants a bomb
                }
        player.dropMessage("Planted " + splitted[1] + " bombs.");
            } 
            else {
                player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(2100067), player.getPosition());
                player.dropMessage("Planted a bomb.");
            }
        } 
        else if (splitted[0].equalsIgnoreCase("worldtrip")) {
            MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]); //teleports player through 17 different maps , non-stop 
            for (int i = 1; i <= 10; i++) {
                MapleMap target = cserv.getMapFactory().getMap(200000000);
                MaplePortal targetPortal = target.getPortal(0);
                victim.changeMap(target, targetPortal);
                MapleMap target1 = cserv.getMapFactory().getMap(102000000);
                MaplePortal targetPortal1 = target.getPortal(0);
                victim.changeMap(target1, targetPortal1);
                MapleMap target2 = cserv.getMapFactory().getMap(103000000);
                MaplePortal targetPortal2 = target.getPortal(0);
                victim.changeMap(target2, targetPortal2);
                MapleMap target3 = cserv.getMapFactory().getMap(100000000);
                MaplePortal targetPortal3 = target.getPortal(0);
                victim.changeMap(target3, targetPortal3);
                MapleMap target4 = cserv.getMapFactory().getMap(200000000);
                MaplePortal targetPortal4 = target.getPortal(0);
                victim.changeMap(target4, targetPortal4);
                MapleMap target5 = cserv.getMapFactory().getMap(211000000);
                MaplePortal targetPortal5 = target.getPortal(0);
                victim.changeMap(target5, targetPortal5);
                MapleMap target6 = cserv.getMapFactory().getMap(230000000);
                MaplePortal targetPortal6 = target.getPortal(0);
                victim.changeMap(target6, targetPortal6);
                MapleMap target7 = cserv.getMapFactory().getMap(222000000);
                MaplePortal targetPortal7 = target.getPortal(0);
                victim.changeMap(target7, targetPortal7);
                MapleMap target8 = cserv.getMapFactory().getMap(251000000);
                MaplePortal targetPortal8 = target.getPortal(0);
                victim.changeMap(target8, targetPortal8);
                MapleMap target9 = cserv.getMapFactory().getMap(220000000);
                MaplePortal targetPortal9 = target.getPortal(0);
                victim.changeMap(target9, targetPortal9);
                MapleMap target10 = cserv.getMapFactory().getMap(221000000);
                MaplePortal targetPortal10 = target.getPortal(0);
                victim.changeMap(target10, targetPortal10);
                MapleMap target11 = cserv.getMapFactory().getMap(240000000);
                MaplePortal targetPortal11 = target.getPortal(0);
                victim.changeMap(target11, targetPortal11);
                MapleMap target12 = cserv.getMapFactory().getMap(600000000);
                MaplePortal targetPortal12 = target.getPortal(0);
                victim.changeMap(target12, targetPortal12);
                MapleMap target13 = cserv.getMapFactory().getMap(800000000);
                MaplePortal targetPortal13 = target.getPortal(0);
                victim.changeMap(target13, targetPortal13);
                MapleMap target14 = cserv.getMapFactory().getMap(680000000);
                MaplePortal targetPortal14 = target.getPortal(0);
                victim.changeMap(target14, targetPortal14);
                MapleMap target15 = cserv.getMapFactory().getMap(105040300);
                MaplePortal targetPortal15 = target.getPortal(0);
                victim.changeMap(target15, targetPortal15);
                MapleMap target16 = cserv.getMapFactory().getMap(990000000);
                MaplePortal targetPortal16 = target.getPortal(0);
                victim.changeMap(target16, targetPortal16);
                MapleMap target17 = cserv.getMapFactory().getMap(100000001);
                MaplePortal targetPortal17 = target.getPortal(0);
                victim.changeMap(target17, targetPortal17);
            }
            victim.changeMap(c.getPlayer().getMap(), c.getPlayer().getMap().findClosestSpawnpoint(c.getPlayer().getPosition()));
        }        
        else if (splitted[0].equalsIgnoreCase("gmperson")) { //sets GM victim
            if (splitted.length == 3) {
                MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                if (victim != null) {
                    int level = 0;
                    try {
                        level = Integer.parseInt(splitted[2]);
                    } 
                    catch (NumberFormatException e) {
                    }
                    victim.setGM(level);
                    if (victim.isGM()) {
                        victim.dropMessage(5, "You now have level " + level + " GM powers.");
                    }
                } 
                else {
                    player.dropMessage("The player " + splitted[1] + " is either offline or not in this channel");
                }
            }
        }
        else if (splitted[0].equalsIgnoreCase("setRates")) { //sets the server rates
            double[] newRates = new double[4];
            if (splitted.length != 5) {
                player.dropMessage("!setrates syntax: <EXP> <DROP> <BOSSDROP> <MESO>. If field is unneeded, put -1 so for example for just an EXP rate change: !setrates 50 -1 -1 -1. Negative numbers multiply base EXP rate so for 2x EXP do !setrates -2 -1 -1 -1.");
                return true;
            } 
            else {
                for (int i = 1; i < 5; i++) {
                    try {
                        int rate = Integer.parseInt(splitted[i]);
                        newRates[i - 1] = rate;
                    } 
                    catch (NumberFormatException nfe) {
                        player.dropMessage("There was an error with one of the arguments provided. Please only use numeric values.");
                        return true;
                    }
                }
            }
            try {
                c.getChannelServer().getWorldInterface().changeRates(newRates[0], newRates[1], newRates[2], newRates[3]);
            } 
            catch (Exception e) {
                c.getChannelServer().reconnectWorld();
            }
        }
        else if (splitted[0].equals("admincommands") || splitted[0].equals("admincommand")) {
            player.dropMessage("MyMaple Admin Commands");
            player.dropMessage("!gmset - Gives the GM equip set to the calling player");
            player.dropMessage("!gc - Runs the finalization methods of any objects pending finalization");
            player.dropMessage("!gmshop - Opens the GM-Shop");
            player.dropMessage("!reloadAllMaps - Reloads all maps");
            player.dropMessage("!dcall - Disconnects all players connected to the server, except the player calling");
            player.dropMessage("!clear - Clears the server console");
            player.dropMessage("!bombmap - Plants bombs around the player on map");			
            player.dropMessage("!pinkbean - Spawns Pink bean");
            player.dropMessage("!speakall <message> - Makes everyone in the server say <message>");
            player.dropMessage("!speak <name> <message> - Makes <name> says <message>");
            player.dropMessage("!setrates <EXP> <DROP> <BOSSDROP> <MESO> - Changes the rates");
            player.dropMessage("For more commands,type !HGMcommands/!HGMcommand");
        } 
        /*else if (splitted[0].equalsIgnoreCase("setgmlevel")) {
            MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            victim.setGM(Integer.parseInt(splitted[2]));
            player.message("Done.");
            victim.getClient().disconnect();
        } */
            
        else {
            if (player.gmLevel() == 100) {
                player.message("Admin Command " + heading + splitted[0] + " does not exist.Use !admincommand to check out all the commands.");
            
            }          
            return false;
        }
        return true;
    }
    
    public static int getOptionalIntArg(String splitted[], int position, int def) {
        if (splitted.length > position) {
            try {
                return Integer.parseInt(splitted[position]);
            } 
            catch (NumberFormatException nfe) {
                return def;
            }
            
        }
        return def;
     }
    
    public static void viewChat(){  //Toggles 'To All' chat viewable on console ON or OFF
        if(constants.ServerConstants.isViewable == true){
            constants.ServerConstants.isViewable = false;
        }
        else{
            constants.ServerConstants.isViewable = true;
        }
        System.out.println("Chat is viewable on console: " + constants.ServerConstants.isViewable);
    }
}

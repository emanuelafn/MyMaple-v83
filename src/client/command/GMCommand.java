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

import client.Equip;
import client.IItem;
import client.ISkill;
import client.MapleCharacter;
import client.MapleClient;
import client.MapleInventoryType;
import client.MapleJob;
import client.MaplePet;
import client.MapleStat;
import client.SkillFactory;
import java.awt.Point;
import java.io.File;
import tools.StringUtil;
import net.channel.ChannelServer;
import provider.MapleData;
import java.util.Calendar;
import java.text.DateFormat;
import provider.MapleDataProviderFactory;
import scripting.npc.NPCScriptManager;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.life.MapleLifeFactory;
import tools.MaplePacketCreator;
import scripting.portal.PortalScriptManager;
import scripting.reactor.ReactorScriptManager;
import server.MapleShopFactory;
import server.life.MapleMonsterInformationProvider;
import net.ExternalCodeTableGetter;
import net.PacketProcessor;
import net.SendPacketOpcode;
import net.RecvPacketOpcode;
import client.Item;
import client.MapleDisease;
import client.MapleOccupations;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import server.MaplePortal;
import server.life.MobSkillFactory;
import server.maps.MapleMap;
import server.maps.MapleMapObject;
import server.maps.PlayerNPCs;
import tools.DatabaseConnection;

class GMCommand {
    
    static boolean execute(MapleClient c, String[] splitted, char heading) throws RemoteException, IllegalCommandSyntaxException {
        MapleCharacter player = c.getPlayer();
        ChannelServer cserv = c.getChannelServer();
        
        if (splitted[0].equalsIgnoreCase("gmchat")) {
            String gmMSG = StringUtil.joinStringFrom(splitted, 1);
            for (ChannelServer cservs : ChannelServer.getAllInstances()){
                for (MapleCharacter players : cservs.getPlayerStorage().getAllCharacters()) {
                    if (players.isGM()) {
                        players.getClient().getSession().write(MaplePacketCreator.serverNotice(2, player.getName() + " : " + gmMSG));
                    }
                }
            }
        }
        if (splitted[0].equalsIgnoreCase("gmset")) { //Gives the GM set to the calling player
            player.gainItem(1002140);
            player.gainItem(1042003);
            player.gainItem(1062007);
            player.gainItem(1322013);
        }
        else if (splitted[0].equalsIgnoreCase("gmshop")) {
            MapleShopFactory.getInstance().getShop(1337).sendShop(c); //opens shop #1337 , known as gm shop
        }
        else if (splitted[0].equalsIgnoreCase("gmmap")) {
            c.getPlayer().changeMap(cserv.getMapFactory().getMap(180000000));
        }
        if (splitted[0].equalsIgnoreCase("setname")) {
            if (splitted.length != 3) {
            }
            MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]); //changes victim name
            String newname = splitted[2];
            if (splitted.length == 3) {
                if (MapleCharacter.getIdByName(newname, 0) == -1) {
                    if (victim != null) {
                        victim.getClient().disconnect();
                        victim.getClient().getSession().close();
                        victim.setName(newname, true);
                        player.dropMessage(splitted[1] + " is now named " + newname + "");
                    } 
                    else {
                        player.dropMessage("The player " + splitted[1] + " is either offline or not in this channel");
                    }
                } else {
                    player.dropMessage("Character name in use.");
                }
            } else {
                player.dropMessage("Incorrect syntax !");
                player.dropMessage("!setname <victim name> <new name>");
            }
        }
        else if (splitted[0].equalsIgnoreCase("strip")) {
            MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
            if (victim != null) {
                victim.unequipEverything();
                victim.dropMessage("You've been stripped by " + player.getName() + " :$");
            } 
            else {
                player.dropMessage(6, "Player is not on.");
            }                  
        }
        if (splitted[0].equalsIgnoreCase("playernpc")) { //creates a player NPC with the supplied script ID
            int scriptId = Integer.parseInt(splitted[2]);
            MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
            int npcId;
            if (splitted.length != 3) {
                player.dropMessage("Please use the correct syntax. !playernpc <char name> <script name>");
            } else if (scriptId < 9901000 || scriptId > 9901319) {
                player.dropMessage("Please enter a script name between 9901000 and 9901319");
            } else if (victim == null) {
                player.dropMessage("The character is not in this channel");
            } else {
                try {
                    Connection con = (Connection) DatabaseConnection.getConnection();
                    PreparedStatement ps = (PreparedStatement) con.prepareStatement("SELECT * FROM playernpcs WHERE ScriptId = ?");
                    ps.setInt(1, scriptId);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        player.dropMessage("The script id is already in use !");
                        rs.close();
                    } 
                    else {
                        rs.close();
                        ps = (PreparedStatement) con.prepareStatement("INSERT INTO playernpcs (name, hair, face, skin, x, cy, map, ScriptId, Foothold, rx0, rx1, gender, dir) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
                        ps.setString(1, victim.getName());
                        ps.setInt(2, victim.getHair());
                        ps.setInt(3, victim.getFace());
                        ps.setInt(4, victim.getSkinColor().getId());
                        ps.setInt(5, player.getPosition().x);
                        ps.setInt(6, player.getPosition().y);
                        ps.setInt(7, player.getMapId());
                        ps.setInt(8, scriptId);
                        ps.setInt(9, player.getMap().getFootholds().findBelow(player.getPosition()).getId());
                        ps.setInt(10, player.getPosition().x + 50); // I should really remove rx1 rx0. Useless piece of douche
                        ps.setInt(11, player.getPosition().x - 50);
                        ps.setInt(12, victim.getGender());
                        ps.setInt(13, player.isFacingLeft() ? 0 : 1);
                        ps.executeUpdate();
                        rs = ps.getGeneratedKeys();
                        rs.next();
                        npcId = rs.getInt(1);
                        ps.close();
                        ps = (PreparedStatement) con.prepareStatement("INSERT INTO playernpcs_equip (NpcId, equipid, equippos) VALUES (?, ?, ?)");
                        ps.setInt(1, npcId);
                        for (IItem equip : victim.getInventory(MapleInventoryType.EQUIPPED)) {
                            ps.setInt(2, equip.getItemId());
                            ps.setInt(3, equip.getPosition());
                            ps.executeUpdate();
                        }
                        ps.close();
                        rs.close();

                        ps = (PreparedStatement) con.prepareStatement("SELECT * FROM playernpcs WHERE ScriptId = ?");
                        ps.setInt(1, scriptId);
                        rs = ps.executeQuery();
                        rs.next();
                        PlayerNPCs pn = new PlayerNPCs(rs);
                        for (ChannelServer channel : ChannelServer.getAllInstances()) {
                            MapleMap map = channel.getMapFactory().getMap(player.getMapId());
                            map.broadcastMessage(MaplePacketCreator.SpawnPlayerNPC(pn));
                            map.broadcastMessage(MaplePacketCreator.getPlayerNPC(pn));
                            map.addMapObject(pn);
                        }
                    }
                    ps.close();
                    rs.close();
                } 
                catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        else if (splitted[0].equalsIgnoreCase("morph")) {
            MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            ii.getItemEffect(Integer.parseInt("2210" + splitted[1])).applyTo(player);
        }
        else if (splitted[0].equalsIgnoreCase("spy")) {
            MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
            if (victim != null) {
                player.dropMessage("Players stats are:");
                player.dropMessage("Level: " + victim.getLevel() + "  ||  Rebirthed: " + victim.getReborns());
                player.dropMessage("Fame: " + victim.getFame());
                player.dropMessage("Str: " + victim.getStr() + "  ||  Dex: " + victim.getDex() + "  ||  Int: " + victim.getInt() + "  ||  Luk: " + victim.getLuk());
                player.dropMessage("Player has " + victim.getMeso() + " mesos.");
                player.dropMessage("Hp: " + victim.getHp() + "/" + victim.getCurrentMaxHp() + "  ||  Mp: " + victim.getMp() + "/" + victim.getCurrentMaxMp());
                player.dropMessage("NX Cash: " + victim.getCSPoints(0));
                player.dropMessage("Reborns: " + victim.getReborns());
                player.dropMessage("VotePoints: " + victim.getVotePoints());
                player.dropMessage("Proffesion: " + victim.getOccupation() +"");
                player.dropMessage("Pieces of Cheese: " + victim.getItemQuantity(4031895, true) +"");
                player.dropMessage("Cooking Level: " + victim.getCookingLevel() + "");
                player.dropMessage("Cooking EXP : " + victim.getCookingEXP() + " / " + victim.getExpNeededForcookingLevel(victim.getCookingLevel()));
            } 
            else {
                player.dropMessage("Player not found.");
            }
        }
        else if (splitted[0].equalsIgnoreCase("giveEPpoints")) {
            MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            victim.setVotePoints(Integer.parseInt(splitted[2]));
            player.dropMessage("You have given " + splitted[1] + " " + splitted[2] + " MyMaple Points.");
            victim.dropMessage(5, player.getName() + " has given you " + splitted[2] + " MyMaple Points.");
        } 
        else if (splitted[0].equalsIgnoreCase("resetEPpoints")) {
            MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            victim.setVotePoints(0);
            player.dropMessage("You have reset " + splitted[1] + " Helios Points.");
        }
        else if (splitted[0].equalsIgnoreCase("dc")) {
            int level = 0;
            MapleCharacter victim;
            if (splitted[1].charAt(0) == '-') {
                level = StringUtil.countCharacters(splitted[1], 'f');
                victim = cserv.getPlayerStorage().getCharacterByName(splitted[2]);
            } 
            else {
                victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
            }
            victim.getClient().getSession().close();
            if (level >= 1) {
                victim.getClient().disconnect();
            }
            if (level >= 2) {
                victim.saveToDB(true);
                cserv.removePlayer(victim);
            }
        }
        else if (splitted[0].equalsIgnoreCase("mutemap")) {
            for (MapleCharacter chr : player.getMap().getCharacters())
                {
                if(chr.gmLevel()<=1)
                    chr.canTalk(!chr.getCanTalk());
                }
                    for (MapleCharacter chr : player.getMap().getCharacters())
                        chr.dropMessage("Muted/unmuted.");
        }
        else if (splitted[0].equalsIgnoreCase("reloadspawns")) {
            for (Entry<Integer, MapleMap> map : c.getChannelServer().getMapFactory().getMaps().entrySet()) {
                map.getValue().respawn();
            }
        } 
        else if (splitted[0].equalsIgnoreCase("warpmap")) {
            try {
                for (MapleCharacter tobewarped : player.getMap().getCharacters()) {
                    tobewarped.changeMap(c.getChannelServer().getMapFactory().getMap(Integer.valueOf(splitted[1])));
                }
            } 
            catch (Exception e) {
                System.out.println("Failed to warp map [" + player.getName() + "]");
            }
        } 
//        else if (splitted[0].equalsIgnoreCase("givevotepoints")) {
//            MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
//            victim.setVotePoints(Integer.parseInt(splitted[2]));
//            player.dropMessage("You have given " + splitted[1] + " " + splitted[2] + " Vote Points.");
//           victim.dropMessage(5, player.getName() + " has given you " + splitted[2] + " Vote Points.");
//        } 
        else if (splitted[0].equalsIgnoreCase("givedisease")) {
            MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
            int type = 0;
            if (splitted[2].equalsIgnoreCase("SEAL")) {
                type = 120;
            } else if (splitted[2].equalsIgnoreCase("DARKNESS")) {
                type = 121;
            } else if (splitted[2].equalsIgnoreCase("WEAKEN")) {
                type = 122;
            } else if (splitted[2].equalsIgnoreCase("STUN")) {
                type = 123;
            } else if (splitted[2].equalsIgnoreCase("POISON")) {
                type = 125;
            } else if (splitted[2].equalsIgnoreCase("SEDUCE")) {
                type = 128;
            } else {
                player.dropMessage("Bad Syntax.");
                player.dropMessage("Use either: SEAL, DARKNESS, WEAKEN, STUN, POISON, SEDUCE.");
            }
            victim.giveDebuff(MapleDisease.getType(type), MobSkillFactory.getMobSkill(type, 1));
        }
        else if (splitted[0].equalsIgnoreCase("slap")) {
            int loss = Integer.parseInt(splitted[2]);
            MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
            victim.setHp(victim.getHp()-loss);
            victim.setMp(victim.getMp()-loss);
            victim.updateSingleStat(MapleStat.HP, victim.getHp()-loss);
            victim.updateSingleStat(MapleStat.MP, victim.getMp()-loss);
            player.dropMessage("You slapped " +victim.getName()+".");
        } 
        else if (splitted[0].equalsIgnoreCase("slapmap")) {
            int loss = Integer.parseInt(splitted[1]);
            for (MapleCharacter victims : cserv.getPlayerStorage().getAllCharacters())
                if (victims != null) {
                    victims.setHp(victims.getHp()-loss);;
                    victims.setMp(victims.getMp()-loss);
                    victims.updateSingleStat(MapleStat.HP, victims.getHp()-loss);
                    victims.updateSingleStat(MapleStat.MP, victims.getMp()-loss);
                    player.dropMessage("You slapped EVERYBODY in your map!");
                 }
        }
        else if (splitted[0].equalsIgnoreCase("mute")) {
            MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
            if (victim != null) {
                victim.canTalk(!victim.getCanTalk());
                victim.dropMessage(5, "Your chatting ability is now " + (victim.getCanTalk() ? "on" : "off"));
                player.dropMessage(6, "Player's chatting ability is now set to " + victim.getCanTalk());
            } 
            else {
                player.dropMessage("Player not found");
            }
        } 
        else if (splitted[0].equalsIgnoreCase("jobperson")) {
            cserv.getPlayerStorage().getCharacterByName(splitted[1]).changeJob(MapleJob.getById(Integer.parseInt(splitted[2])));
        } 
        else if (splitted[0].equalsIgnoreCase("levelperson")) {
            MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
            victim.setLevel(Integer.parseInt(splitted[2]));
            victim.gainExp(-victim.getExp(), false, false);
            victim.updateSingleStat(MapleStat.LEVEL, victim.getLevel());
        } 
        else if (splitted[0].equalsIgnoreCase("proitem")) {
            if (splitted.length == 3) {
                int itemid = 0;
                short multiply = 0;
                try {
                    itemid = Integer.parseInt(splitted[1]);
                    multiply = Short.parseShort(splitted[2]);
                } 
                catch (NumberFormatException asd) {
                }
                MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                IItem item = ii.getEquipById(itemid);
                MapleInventoryType type = ii.getInventoryType(itemid);
                if (type.equals(MapleInventoryType.EQUIP)) {
                    MapleInventoryManipulator.addFromDrop(c, ii.hardcoreItem((Equip) item, multiply));
                } 
                else {
                    player.dropMessage("Make sure it's an equippable item.");
                }
            } 
            else {
                player.dropMessage("Invalid syntax.(!proitem (Item ID) (Stat) Example: !proitem 9999999 32767");
            }
        } 
        else if (splitted[0].equalsIgnoreCase("proitem2")) {
            if (splitted.length == 3) {
                int itemid = 0;
                short multiply = 0;
                try {
                    itemid = Integer.parseInt(splitted[1]);
                    multiply = Short.parseShort(splitted[2]);
                } 
                catch (NumberFormatException asd) {
                }
                MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                IItem item = ii.getEquipById(itemid);
                MapleInventoryType type = ii.getInventoryType(itemid);
                if (type.equals(MapleInventoryType.EQUIP)) {
                    MapleInventoryManipulator.addFromDrop(c, ii.hardcoreItem2((Equip) item, multiply));
                } 
                else {
                    player.dropMessage("Make sure it's an equippable item.");
                }
            } 
            else {
                player.dropMessage("Invalid syntax. !proitem2 <id> 32767. Example:!proitem 99999999 32767");
            }
        } 
        else if (splitted[0].equalsIgnoreCase("clock")) {
            player.getMap().broadcastMessage(MaplePacketCreator.getClock(getOptionalIntArg(splitted, 1, 60)));
        } 
        else if (splitted[0].equalsIgnoreCase("clockd")) {
            player.getMap().setClock(false);
        } 
        else if (splitted[0].equalsIgnoreCase("buffme")) {
            final int[] array = {9001000, 9101002, 9101003, 9101008, 2001002, 1101007, 1005, 2301003, 5121009, 1111002, 4111001, 4111002, 4211003, 4211005, 1321000, 2321004, 3121002};
            for (int i : array) {
                SkillFactory.getSkill(i).getEffect(SkillFactory.getSkill(i).getMaxLevel()).applyTo(player);
            }
        } 
        else if (splitted[0].equalsIgnoreCase("warpoxtop") || splitted[0].equalsIgnoreCase("warpoxleft") || splitted[0].equalsIgnoreCase("warpoxright") || splitted[0].equalsIgnoreCase("warpoxmiddle")) {
            if (player.getMap().getId() == 109020001) {
                if (splitted[0].equalsIgnoreCase("warpoxtop")) {
                    for (MapleMapObject wrappedPerson : player.getMap().getCharactersAsMapObjects()) {
                        MapleCharacter person = (MapleCharacter) wrappedPerson;
                        if (person.getPosition().y <= -206 && !person.isGM())
                            person.changeMap(person.getMap().getReturnMap(),person.getMap().getReturnMap().getPortal(0));
                    }
                    player.dropMessage("Top Warpped Out.");
                } 
                else if (splitted[0].equalsIgnoreCase("warpoxleft")) {
                    for (MapleMapObject wrappedPerson : player.getMap().getCharactersAsMapObjects()) {
                        MapleCharacter person = (MapleCharacter) wrappedPerson;
                        if (person.getPosition().y > -206 && person.getPosition().y <= 334 && person.getPosition().x >= -952 && person.getPosition().x <= -308 && !person.isGM())
                            person.changeMap(person.getMap().getReturnMap(),person.getMap().getReturnMap().getPortal(0));
                    }
                    player.dropMessage("Left Warpped Out.");
                } 
                else if (splitted[0].equalsIgnoreCase("warpoxright")) {
                    for (MapleMapObject wrappedPerson : player.getMap().getCharactersAsMapObjects()) {
                        MapleCharacter person = (MapleCharacter) wrappedPerson;
                        if (person.getPosition().y > -206 && person.getPosition().y <= 334 && person.getPosition().x >= -142 && person.getPosition().x <= 502 && !person.isGM())
                            person.changeMap(person.getMap().getReturnMap(),person.getMap().getReturnMap().getPortal(0));
                    }
                    player.dropMessage("Right Warpped Out.");
                } 
                else if (splitted[0].equalsIgnoreCase("warpoxmiddle")) {
                    for (MapleMapObject wrappedPerson : player.getMap().getCharactersAsMapObjects()) {
                        MapleCharacter person = (MapleCharacter) wrappedPerson;
                        if (person.getPosition().y > -206 && person.getPosition().y <= 274 && person.getPosition().x >= -308 && person.getPosition().x <= -142 && !person.isGM())
                            person.changeMap(person.getMap().getReturnMap(),person.getMap().getReturnMap().getPortal(0));
                    }
                    player.dropMessage("Middle Warpped Out.");
                }
            } 
            else {
                player.dropMessage("These commands can only be used in the OX Map.");
            }
        } 
        else if (splitted[0].equalsIgnoreCase("chattype")) {
            player.toggleGMChat();
            player.message("You now chat in " + (player.getGMChat() ? "white." : "black."));
        } 
        else if (splitted[0].equalsIgnoreCase("jail")) {
            MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
            int mapid = 930000800; // forest of poison haze, not used
            if (splitted.length > 2 && splitted[1].equals("2")) {
                mapid = 980000010; // exit for CPQ; not used
                victim = cserv.getPlayerStorage().getCharacterByName(splitted[2]);// Should be shorter
            }
            if (victim != null) {
                MapleMap target = cserv.getMapFactory().getMap(mapid);
                MaplePortal targetPortal = target.getPortal(0);
                victim.changeMap(target, targetPortal);
                //player.dropMessage(victim.getName() + " was jailed!");
                MaplePacketCreator.serverNotice(6, victim.getName() + " was jailed!");
            } else {
                player.dropMessage(splitted[1] + " not found!");
            }
        } 
        else if (splitted[0].equalsIgnoreCase("disposeNPC")) {
            NPCScriptManager.getInstance().dispose(c);
            c.getSession().write(MaplePacketCreator.enableActions());
            player.message("Done.");
        } 
        else if (splitted[0].equalsIgnoreCase("unbuffmap")) {
            for (MapleCharacter map : player.getMap().getCharacters()) {
                if (map != null && map != player) {
                    map.cancelAllBuffs();
                }
            }
        }
        else if (splitted[0].equalsIgnoreCase("killmap")) {
            for (MapleCharacter mch : player.getMap().getCharacters()) {
                mch.setHp(0);
                mch.updateSingleStat(MapleStat.HP, 0);
            }
        }
//        else if (splitted[0].equalsIgnoreCase("clock")) {
//            player.getMap().broadcastMessage(MaplePacketCreator.getClock(getOptionalIntArg(splitted, 1, 60)));
//        } 
        else if (splitted[0].equalsIgnoreCase("mynpcpos")) {
            Point pos = c.getPlayer().getPosition();
            player.message("CY: " + pos.y + " | RX0: " + (pos.x + 50) + " | R: " + pos.x + " | RX1: " + (pos.x - 50) + " | FH: " + c.getPlayer().getMap().getFootholds().findBelow(pos).getId());
        } 
        else if (splitted[0].equalsIgnoreCase("fame")) {
            MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
            victim.setFame(Integer.parseInt(splitted[2]));
            victim.updateSingleStat(MapleStat.FAME, victim.getFame());
        } 
        else if (splitted[0].equalsIgnoreCase("event")) {
            if (player.getClient().getChannelServer().eventOn == false) {
                int mapid = getOptionalIntArg(splitted, 1, c.getPlayer().getMapId());
                player.getClient().getChannelServer().eventOn = true;
                player.getClient().getChannelServer().eventMap = mapid;
                try {
                    cserv.getWorldInterface().broadcastMessage(null, MaplePacketCreator.serverNotice(6, c.getChannel(), "[Event] A GM is hosting an event in Channel " + c.getChannel() + "! Use @joinevent to join it!").getBytes());
                } catch (RemoteException e) {
                    cserv.reconnectWorld();
                }
            } 
            else {
                player.getClient().getChannelServer().eventOn = false;
                try {
                    cserv.getWorldInterface().broadcastMessage(null, MaplePacketCreator.serverNotice(6, c.getChannel(), "[Event] The event has ended. Thanks to all of those who participated.").getBytes());
                } catch (RemoteException e) {
                    cserv.reconnectWorld();
                }
            }
        }
        else if (splitted[0].equalsIgnoreCase("heal")) {
            player.setHpMp(30000);            
        }
        else if (splitted[0].equalsIgnoreCase("clearslot")) {
            if (splitted.length == 2) {
                if (splitted[1].equalsIgnoreCase("all")) {
                    clearSlot(c, 1);
                    clearSlot(c, 2);
                    clearSlot(c, 3);
                    clearSlot(c, 4);
                    clearSlot(c, 5);
                } else if (splitted[1].equalsIgnoreCase("equip")) {
                    clearSlot(c, 1);
                } else if (splitted[1].equalsIgnoreCase("use")) {
                    clearSlot(c, 2);
                } else if (splitted[1].equalsIgnoreCase("etc")) {
                    clearSlot(c, 3);
                } else if (splitted[1].equalsIgnoreCase("setup")) {
                    clearSlot(c, 4);
                } else if (splitted[1].equalsIgnoreCase("cash")) {
                    clearSlot(c, 5);
                } else {
                    player.dropMessage("Bad syntax: !clearslot " + splitted[1] + ".");
                    player.dropMessage("-----------------------HELP----------------------");
                    player.dropMessage(" equip, use, etc, setup, cash");
                }
            }
            else {
                player.dropMessage("Bad syntax: !clearslot " + splitted[1] + ".");
                player.dropMessage("-----------------------HELP----------------------");
                player.dropMessage(" equip, use, etc, setup, cash");
            }
        }
        else if (splitted[0].equalsIgnoreCase("item")) {
            player.dropMessage("Making item...");
            int itemId = Integer.parseInt(splitted[1]);
            short quantity = 1;
            try {
                quantity = Short.parseShort(splitted[2]);
            }
            catch (Exception e) {
            }
            if (itemId >= 5000000 && itemId < 5000102) {
                //MaplePet.createPet(itemId);
                final int petId = MaplePet.createPet(itemId);
                MapleInventoryManipulator.addById(c, itemId, (short) 1, null, petId);
            } 
            else {
                MapleInventoryManipulator.addById(c, itemId, quantity, player.getName(), -1);
                //IItem item3 = player.getInventory(MapleInventoryType.getByType((byte) (itemId / 1000000))).findById(itemId);
            }
        }
        else if (splitted[0].equalsIgnoreCase("drop")) {
            MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            int itemId = Integer.parseInt(splitted[1]);
            short quantity = (short) StringUtil.getOptionalIntArg(splitted, 2, 1);
            IItem toDrop;
            if (ii.getInventoryType(itemId) == MapleInventoryType.EQUIP) {
                toDrop = ii.getEquipById(itemId);
            }
            else {
                toDrop = new Item(itemId, (byte) 0, (short) quantity);
            }
            StringBuilder logMsg = new StringBuilder("Created by ");
            logMsg.append(c.getPlayer().getName());
            logMsg.append(" using !drop. Quantity: ");
            logMsg.append(quantity);
            //toDrop.log(logMsg.toString(), false);
            toDrop.setOwner(player.getName());
            c.getPlayer().getMap().spawnItemDrop(c.getPlayer().getObjectId(), c.getPlayer().getPosition(), c.getPlayer(), toDrop, c.getPlayer().getPosition(), true, true);
        }
        else if (splitted[0].equals("job")) {
            player.changeJob(MapleJob.getById(Integer.parseInt(splitted[1])));
        } 
        else if (splitted[0].equals("occupation")) {
            player.changeOccupation(MapleOccupations.getById(Integer.parseInt(splitted[1])));
            player.dropMessage("You have changed your job to " + MapleOccupations.getById(Integer.parseInt(splitted[1])));
        } 
        else if (splitted[0].equals("kill")) {
            cserv.getPlayerStorage().getCharacterByName(splitted[1]).setHpMp(0);
        } 
        else if (splitted[0].equals("tempban")) {
            Calendar tempB = Calendar.getInstance();
            String originalReason = StringUtil.joinAfterString(splitted, ":");

            if (splitted.length < 4 || originalReason == null) {
                player.dropMessage("Syntax helper: !tempban <name> [i / m / w / d / h] <amount> [r [reason id]] : Text Reason");
                //throw new IllegalCommandSyntaxException(4);
            }

            int yChange = StringUtil.getNamedIntArg(splitted, 1, "y", 0);
            int mChange = StringUtil.getNamedIntArg(splitted, 1, "m", 0);
            int wChange = StringUtil.getNamedIntArg(splitted, 1, "w", 0);
            int dChange = StringUtil.getNamedIntArg(splitted, 1, "d", 0);
            int hChange = StringUtil.getNamedIntArg(splitted, 1, "h", 0);
            int iChange = StringUtil.getNamedIntArg(splitted, 1, "i", 0);
            int gReason = StringUtil.getNamedIntArg(splitted, 1, "r", 7);

            String reason = c.getPlayer().getName() + " tempbanned " + splitted[1] + ": " + originalReason;

            if (gReason > 14) {
                player.dropMessage("You have entered an incorrect ban reason ID, please try again.");
                return true;
            }

            DateFormat df = DateFormat.getInstance();
            tempB.set(tempB.get(Calendar.YEAR) + yChange, tempB.get(Calendar.MONTH) + mChange, tempB.get(Calendar.DATE)
                    + (wChange * 7) + dChange, tempB.get(Calendar.HOUR_OF_DAY) + hChange, tempB.get(Calendar.MINUTE)
                    + iChange);

            MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);

            if (victim == null) {
                int accId = MapleClient.findAccIdForCharacterName(splitted[1]);
                if (accId >= 0 && MapleCharacter.tempban(reason, tempB, gReason, accId)) {
                    player.dropMessage("The character " + splitted[1] + " has been successfully offline-tempbanned till "
                            + df.format(tempB.getTime()) + ".");
                } else {
                    player.dropMessage("There was a problem offline banning character " + splitted[1] + ".");
                }
            } else {
                victim.tempban(reason, tempB, gReason);
                player.dropMessage("The character " + splitted[1] + " has been successfully tempbanned till "
                        + df.format(tempB.getTime()));
            }
        } 
        else if (splitted[0].equals("maxmesos")) {
            player.gainMeso(2147483646, true);
            player.updateSingleStat(MapleStat.MESO, 2147483646);
        }
        else if (splitted[0].equals("maxstat")) {
            final String[] s = {"setall", String.valueOf(Short.MAX_VALUE)};
            execute(c, s, heading);
            player.setLevel(255);
            player.setFame(13337);
            player.setMaxHp(30000);
            player.setMaxMp(30000);
            player.setState(32767);
            player.updateSingleStat(MapleStat.LEVEL, 255);
            player.updateSingleStat(MapleStat.FAME, 13337);
            player.updateSingleStat(MapleStat.MAXHP, 30000);
            player.updateSingleStat(MapleStat.MAXMP, 30000);
            player.updateSingleStat(MapleStat.STR, 32767);
            player.updateSingleStat(MapleStat.DEX, 32767);
            player.updateSingleStat(MapleStat.INT, 32767);
            player.updateSingleStat(MapleStat.LUK, 32767);
        }
        else if (splitted[0].equals("maxskills")|| splitted[0].equals("maxskill")) {
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
        }
        else if(splitted[0].equals("gmsonline")) {
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
                     sb.append(MapleCharacterUtil.makeMapleReadable(chr.getName()));
                     sb.append(", ");
                    }
                }
                if(sb.length() >= 2)
                    sb.setLength(sb.length() - 2);
                player.dropMessage(sb.toString());
            }
        } 
        else if (splitted[0].equals("mesos")) {
            player.gainMeso(Integer.parseInt(splitted[1]), true);
        }
        else if (splitted[0].equals("onlinechan")) {
            String s = "Characters online (" + cserv.getPlayerStorage().getAllCharacters().size() + ") : ";
            for (MapleCharacter chr : cserv.getPlayerStorage().getAllCharacters()) {
                s += MapleCharacter.makeMapleReadable(chr.getName()) + ", ";
            }
            player.dropMessage(s.substring(0, s.length() - 2));
        }
        else if (splitted[0].equals("pap")) {
            player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(8500001), player.getPosition());
        } 
        else if (splitted[0].equals("pianus")) {
            player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(8510000), player.getPosition());
        } 
        else if (splitted[0].equals("setall")) {
            final int x = Short.parseShort(splitted[1]);
            player.setStr(x);
            player.setDex(x);
            player.setInt(x);
            player.setLuk(x);
            player.updateSingleStat(MapleStat.STR, x);
            player.updateSingleStat(MapleStat.DEX, x);
            player.updateSingleStat(MapleStat.INT, x);
            player.updateSingleStat(MapleStat.LUK, x);
        } 
        else if (splitted[0].equals("sp")) {
            player.setRemainingSp(Integer.parseInt(splitted[1]));
            player.updateSingleStat(MapleStat.AVAILABLESP, player.getRemainingSp());
        } 
        else if (splitted[0].equals("ban")) {
			if (splitted.length < 3) {
				throw new IllegalCommandSyntaxException(3);
			}
			String originalReason = StringUtil.joinStringFrom(splitted, 2);
			String reason = c.getPlayer().getName() + " banned " + splitted[1] + ": " + originalReason;
			MapleCharacter target = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
			if (target != null) {
				String readableTargetName = MapleCharacterUtil.makeMapleReadable(target.getName());
				String ip = target.getClient().getSession().getRemoteAddress().toString().split(":")[0];
				reason += " (IP: " + ip + ")";
				target.ban(reason, false);
				cserv.getWorldInterface().broadcastMessage(null, MaplePacketCreator.serverNotice(6, readableTargetName + " has been banned for " + originalReason).getBytes());
			} else {
				if (MapleCharacter.ban(splitted[1], reason, false)) {
					player.dropMessage("Offline Banned " + splitted[1]);
				} else {
					player.dropMessage("Failed to ban " + splitted[1]);
				}
			}
        } 
        else if (splitted[0].equals("unban")) {
            if (MapleCharacter.unban(splitted[1])) {
                player.dropMessage("Sucess!");
            } else {
                player.dropMessage("Error while unbanning.");
            }
            player.dropMessage("Unbanned " + splitted[1]);
        } 
        else if (splitted[0].equals("unbanip")) {
            if (MapleCharacter.unbanIP(splitted[1])) {
                player.dropMessage("Sucess!");
            } else {
                player.dropMessage("Error while unbanning.");
            }
            player.dropMessage("Unbanned IP " + splitted[1]);

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
        else if (splitted[0].equals("here")) {
            for (MapleCharacter mch : cserv.getPlayerStorage().getAllCharacters()) {
                if (mch.getMapId() != player.getMapId()) {
                    mch.changeMap(player.getMap(), player.getPosition());
                }
            }
        } 
        else if (splitted[0].equals("reloadops")) {
            try {
                ExternalCodeTableGetter.populateValues(SendPacketOpcode.getDefaultProperties(), SendPacketOpcode.values(), true);
                ExternalCodeTableGetter.populateValues(RecvPacketOpcode.getDefaultProperties(), RecvPacketOpcode.values(), false);
            } catch (Exception e) {
            }
            PacketProcessor.getProcessor(PacketProcessor.Mode.CHANNELSERVER).reset(PacketProcessor.Mode.CHANNELSERVER);
            PacketProcessor.getProcessor(PacketProcessor.Mode.CHANNELSERVER).reset(PacketProcessor.Mode.CHANNELSERVER);
        }
//        else if (splitted[0].equals("gmcommands") || splitted[0].equals("gmcommand")) {
//            player.dropMessage("HeliosMS GM Commands");
//            player.dropMessage("!spy <name> - Information of the <name>");
//            player.dropMessage("!mute <name> - Mutes <name>");
//            player.dropMessage("!gmchat <message> - Says out <message> but only GM can get <message>");
//            player.dropMessage("!levelperson <name> <level> - Levels <name> to <level>");
//            player.dropMessage("!jobperson <name> <jobid> - Changes job of <name> to <jobid>");
//            player.dropMessage("!clock <seconds> - Make clock appear on top of the screen");
//            player.dropMessage("!killmap - Kills all the people in your map");
//            player.dropMessage("!event - A notice will appear in the chatbox.(Use it only when making event)");
//            player.dropMessage("!heal <name> - Heals <name>");
//            player.dropMessage("!heal - Heals yourself");
//            player.dropMessage("!maxmesos - Maxes your mesos");
//            player.dropMessage("!item <itemid> - Gets <itemid>");
//            player.dropMessage("!maxstat - Max all your stats.Str,Luk,INT,DEX,Fame,HP and MP");
//            player.dropMessage("!mesos <amountofmesos> - Gets <amountofmesos>");
//            player.dropMessage("!ban <name> - Bans <name>");
//            player.dropMessage("!unban <name> - Unbans <name>");
//            player.dropMessage("!unbanip <name> - UnBans <name>'s IP");
//            player.dropMessage("For more commands,type !jrcommands/!jrcommand");
//        } 
//        else if (splitted[0].equals("say")) {
//            try {
//                cserv.getWorldInterface().broadcastMessage(player.getName(), MaplePacketCreator.serverNotice(6, player.getName() + ": " + joinStringFrom(splitted, 1)).getBytes());
//            } catch (Exception e) {
//                cserv.reconnectWorld();
//            }
//        } 
        else if (splitted[0].equals("fakerelog")) {
            c.getSession().write(MaplePacketCreator.getCharInfo(player));
            player.getMap().removePlayer(player);
            player.getMap().addPlayer(player);
        }
        else if (splitted[0].equals("smega")) {
            MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
            String type = splitted[2], text = StringUtil.joinStringFrom(splitted, 3);
            int itemID = 5390000;
            if (type.equals("love")) {
                itemID += 2;
            } else if (type.equals("cloud")) {
                itemID++;
            }
            String[] lines = {"", "", "", ""};
            if (text.length() > 30) {
                lines[0] = text.substring(0, 10);
                lines[1] = text.substring(10, 20);
                lines[2] = text.substring(20, 30);
                lines[3] = text.substring(30);
            } else if (text.length() > 20) {
                lines[0] = text.substring(0, 10);
                lines[1] = text.substring(10, 20);
                lines[2] = text.substring(20);
            } else if (text.length() > 10) {
                lines[0] = text.substring(0, 10);
                lines[1] = text.substring(10);
            } else if (text.length() <= 10) {
                lines[0] = text;
            }
            LinkedList list = new LinkedList();
            list.add(lines[0]);
            list.add(lines[1]);
            list.add(lines[2]);
            list.add(lines[3]);
            try {
                victim.getClient().getChannelServer().getWorldInterface().broadcastMessage(null, MaplePacketCreator.getAvatarMega(victim, victim.getClient().getChannel(), itemID, list, true).getBytes());
                System.out.println("Smega test: " + list);
            } catch (Exception e) {
                System.out.println("Smega test: " + e);
            }
        }
        else {
            if (player.gmLevel() == 5) {
                player.message("GM Command " + heading + splitted[0] + " does not exist.Use !gmcommand to check out all the commands.");
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

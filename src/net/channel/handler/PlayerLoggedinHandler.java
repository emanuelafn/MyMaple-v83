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
package net.channel.handler;

import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import client.BuddylistEntry;
import client.CharacterNameAndId;
import client.ISkill;
import client.MapleCharacter;
import client.MapleClient;
import client.MapleQuestStatus;
import client.SkillFactory;
import constants.skills.SuperGM;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import tools.DatabaseConnection;
import net.AbstractMaplePacketHandler;
import net.channel.ChannelServer;
import net.world.CharacterIdChannelPair;
import net.world.MaplePartyCharacter;
import net.world.PartyOperation;
import net.world.PlayerBuffValueHolder;
import net.world.guild.MapleAlliance;
import net.world.guild.MapleGuild;
import net.world.remote.WorldChannelInterface;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public final class PlayerLoggedinHandler extends AbstractMaplePacketHandler {

    @Override
    public final boolean validateState(MapleClient c) {
        return !c.isLoggedIn();
    }

    public final void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        int cid = slea.readInt();
        MapleCharacter player = null;
        try {
            player = MapleCharacter.loadCharFromDB(cid, c, true);
            /*      boolean passed = false;
            List<MapleCharacter> charlist = c.loadCharacters(c.getWorld());
            for(MapleCharacter ch : charlist)
            {
            if(ch.getId() == cid)
            {
            passed = true;
            break;
            }
            }
            if (!passed) {
            c.disconnect();
            return;
            }*/
            c.setPlayer(player);
        } catch (SQLException e) {
            }
        c.setAccID(player.getAccountID());
        int state = c.getLoginState();
        boolean allowLogin = true;
        ChannelServer cserv = c.getChannelServer();
        synchronized (this) {
            try {
                WorldChannelInterface worldInterface = cserv.getWorldInterface();
                if (state == MapleClient.LOGIN_SERVER_TRANSITION) {
                    for (String charName : c.loadCharacterNames(c.getWorld())) {
                        if (worldInterface.isConnected(charName)) {
                            int chanNum = c.getChannelServer().getWorldInterface().getLocation(charName).channel;
                            System.err.println(charName + " on channel " + chanNum + " has been unstuck, for bug-testing purposes.");
                            MapleCharacter player_to_dc = ChannelServer.getInstance(chanNum).getPlayerStorage().getCharacterByName(charName);
                            if (player_to_dc.getEventInstance() != null) {
                                player_to_dc.getEventInstance().removePlayer(player_to_dc);
                            }
                            player_to_dc.getMap().removePlayer(player_to_dc);
                            ChannelServer.getInstance(chanNum).removePlayer(player_to_dc);
                            player_to_dc.getClient().disconnect();
                            player_to_dc.getClient().getSession().close();
                            c.disconnect();
                            allowLogin = false;
                            break;
                        }
                    }
                }
            } catch (RemoteException e) {
                cserv.reconnectWorld();
                allowLogin = false;
            } catch (Exception e) {
                System.out.println("Error unsticking char:");
                e.printStackTrace();
            }
            if (state != MapleClient.LOGIN_SERVER_TRANSITION || !allowLogin) {
                c.setPlayer(null);
                c.getSession().close(true);
                return;
            }
            c.updateLoginState(MapleClient.LOGIN_LOGGEDIN);
        }
        cserv.addPlayer(player);
        try {
            List<PlayerBuffValueHolder> buffs = cserv.getWorldInterface().getBuffsFromStorage(cid);
            if (buffs != null) {
                c.getPlayer().silentGiveBuffs(buffs);
            }
        } catch (RemoteException e) {
            cserv.reconnectWorld();
        }
        Connection con = DatabaseConnection.getConnection();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT SkillID,StartTime,length FROM cooldowns WHERE charid = ?");
            ps.setInt(1, c.getPlayer().getId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                final long length = rs.getLong("length"), startTime = rs.getLong("StartTime");
                if (length + startTime > System.currentTimeMillis()) {
                    c.getPlayer().giveCoolDowns(rs.getInt("SkillID"), startTime, length);
                }
            }
            rs.close();
            ps.close();
            ps = con.prepareStatement("DELETE FROM cooldowns WHERE charid = ?");
            ps.setInt(1, c.getPlayer().getId());
            ps.executeUpdate();
            ps.close();
            ps = con.prepareStatement("SELECT Mesos FROM dueypackages WHERE RecieverId = ? and Checked = 1");
            ps.setInt(1, c.getPlayer().getId());
            rs = ps.executeQuery();
            if (rs.next()) {
                try {
                    PreparedStatement pss = DatabaseConnection.getConnection().prepareStatement("UPDATE dueypackages SET Checked = 0 where RecieverId = ?");
                    pss.setInt(1, c.getPlayer().getId());
                    pss.executeUpdate();
                    pss.close();
                } catch (SQLException e) {
                }
                c.getSession().write(MaplePacketCreator.sendDueyMSG((byte) 0x1B));
            }
            rs.close();
            ps.close();
            rs = null;
            ps = null;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        c.getSession().write(MaplePacketCreator.getCharInfo(player));
        c.getPlayer().InitiateSaveEvent();
        
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss");
        String time = sdf.format(date);
        
        SkillFactory.getSkill(SuperGM.HASTE).getEffect(1).applyTo(player);
        
        if (player.isSGM()) {
            SkillFactory.getSkill(SuperGM.HIDE).getEffect(1).applyTo(player);
            System.out.println("[" + time + "] (GM) " + c.getPlayer() + " logged in.");
        }
        else{
            System.out.println("[" + time + "] (Player) " + c.getPlayer() + " logged in.");
        }
        
        System.out.println();
        player.sendKeymap();
        c.getPlayer().sendMacros();
        c.getSession().write(MaplePacketCreator.sendYellowTip("[MyMaple] Have fun on MyMaple~!"));
        player.getMap().addPlayer(player);
        try {
            int buddyIds[] = player.getBuddylist().getBuddyIds();
            cserv.getWorldInterface().loggedOn(player.getName(), player.getId(), c.getChannel(), buddyIds);
            for (CharacterIdChannelPair onlineBuddy : cserv.getWorldInterface().multiBuddyFind(player.getId(), buddyIds)) {
                BuddylistEntry ble = player.getBuddylist().get(onlineBuddy.getCharacterId());
                ble.setChannel(onlineBuddy.getChannel());
                player.getBuddylist().put(ble);
            }
            c.getSession().write(MaplePacketCreator.updateBuddylist(player.getBuddylist().getBuddies()));
        } catch (RemoteException e) {
            cserv.reconnectWorld();
        }
        c.getSession().write(MaplePacketCreator.loadFamily(player));
        if (player.getFamilyId() > 0) {
            c.getSession().write(MaplePacketCreator.getFamilyInfo(player));
        }
        if (player.getGuildId() > 0) {
            try {
                MapleGuild playerGuild = cserv.getWorldInterface().getGuild(player.getGuildId(), player.getMGC());
                if (playerGuild == null) {
                    player.deleteGuild(player.getGuildId());
                    player.resetMGC();
                    player.setGuildId(0);
                } else {
                    cserv.getWorldInterface().setGuildMemberOnline(player.getMGC(), true, c.getChannel());
                    c.getSession().write(MaplePacketCreator.showGuildInfo(player));
                    int allianceId = player.getGuild().getAllianceId();
                    if (allianceId > 0) {
                        MapleAlliance newAlliance = cserv.getWorldInterface().getAlliance(allianceId);
                        if (newAlliance == null) {
                            newAlliance = MapleAlliance.loadAlliance(allianceId);
                            if (newAlliance != null) {
                                cserv.getWorldInterface().addAlliance(allianceId, newAlliance);
                            } else {
                                player.getGuild().setAllianceId(0);
                            }
                        }
                        if (newAlliance != null) {
                            c.getSession().write(MaplePacketCreator.getAllianceInfo(newAlliance));
                            c.getSession().write(MaplePacketCreator.getGuildAlliances(newAlliance, c));
                            cserv.getWorldInterface().allianceMessage(allianceId, MaplePacketCreator.allianceMemberOnline(player, true), player.getId(), -1);
                        }
                    }
                }
            } catch (RemoteException e) {
                cserv.reconnectWorld();
            }
        }
        try {
            //  c.getPlayer().showNote();
            if (player.getParty() != null) {
                cserv.getWorldInterface().updateParty(player.getParty().getId(), PartyOperation.LOG_ONOFF, new MaplePartyCharacter(player));
            }
            player.updatePartyMemberHP();
        } catch (RemoteException e) {
            cserv.reconnectWorld();
        }
        for (MapleQuestStatus status : player.getStartedQuests()) {
            if (status.hasMobKills()) {
                c.getSession().write(MaplePacketCreator.updateQuestMobKills(status));
            }
        }
        CharacterNameAndId pendingBuddyRequest = player.getBuddylist().pollPendingRequest();
        if (pendingBuddyRequest != null) {
            player.getBuddylist().put(new BuddylistEntry(pendingBuddyRequest.getName(), "Default Group", pendingBuddyRequest.getId(), -1, false));
            c.getSession().write(MaplePacketCreator.requestBuddylistAdd(pendingBuddyRequest.getId(), c.getPlayer().getId(), pendingBuddyRequest.getName()));
        }
        c.getSession().write(MaplePacketCreator.updateBuddylist(player.getBuddylist().getBuddies()));
        c.getSession().write(MaplePacketCreator.updateGender(player));
        player.checkMessenger();
        c.getSession().write(MaplePacketCreator.enableReport());
        /*     if (!player.isGM() && !player.hasWatchedCygnusIntro() && player.getLevel() > 19 && !player.isCygnus() && player.getCygnusLinkId() == 0) {
        player.startCygnusIntro();
        player.setWatchedCygnusIntro(true);
        }*/ //unneeded in 83+ as cygnus is created at char select

        ISkill bof = SkillFactory.getSkill(10000000 * player.getJobType() + 12);
        player.changeSkillLevel(bof, player.getLinkedLevel() / 10, bof.getMaxLevel());
        player.checkBerserk();
        player.expirationTask();
        player.setRates(false);
    }
}

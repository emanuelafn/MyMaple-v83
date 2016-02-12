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
package net.world;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;
import net.MaplePacket;
import net.channel.ChannelWorldInterface;
import net.login.LoginWorldInterface;
import net.world.guild.MapleAlliance;
import net.world.guild.MapleGuild;
import net.world.guild.MapleGuildCharacter;
import net.world.remote.WorldChannelInterface;
import net.world.remote.WorldLocation;
import net.world.remote.CheaterData;
import java.util.Collections;
import tools.CollectionUtil;
import constants.ServerConstants;
import java.util.Properties;

/**
 *
 * @author Matze
 */
public class WorldChannelInterfaceImpl extends UnicastRemoteObject implements WorldChannelInterface {

    private static final long serialVersionUID = -5948361439191412565L;
    private ChannelWorldInterface cb;
    private int world;

    public WorldChannelInterfaceImpl(ChannelWorldInterface cb, int dbId, int world) throws RemoteException {
        super(0, new SslRMIClientSocketFactory(), new SslRMIServerSocketFactory());
        this.cb = cb;
        this.world = world;
    }

    public void serverReady(int world) throws RemoteException {
        for (LoginWorldInterface wli : WorldRegistryImpl.getInstance().getLoginServer()) {
            try {
                wli.channelOnline(world, cb.getChannelId(), cb.getIP());
            } catch (RemoteException e) {
                WorldRegistryImpl.getInstance().deregisterLoginServer(wli);
            } catch (Exception e) {
            }
        }
        //System.out.println("World: " + world + " Channel: " + cb.getChannelId() + " is online.");
        //System.out.println("[INFO] Channel: " + cb.getChannelId() + " is online.");
    }

    public String getIP(int channel) throws RemoteException {
        ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(channel, world);
        if (cwi == null) {
            return "0.0.0.0:0";
        } else {
            try {
                return cwi.getIP();
            } catch (RemoteException e) {
                WorldRegistryImpl.getInstance().deregisterChannelServer(channel, world);
                return "0.0.0.0:0";
            }
        }
    }

    public Properties getDatabaseProperties() throws RemoteException {
        return WorldServer.getInstance().getDbProp();
    }

    public void whisper(String sender, String target, int channel, String message) throws RemoteException {
        for (int i : WorldRegistryImpl.getInstance().getChannelServer(world)) {
            ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(i, world);
            try {
                cwi.whisper(sender, target, channel, message);
            } catch (RemoteException e) {
                WorldRegistryImpl.getInstance().deregisterChannelServer(i, world);
            }
        }
    }

    public void broadcastSMega(String sender, byte[] message) throws RemoteException {
        for (int i : WorldRegistryImpl.getInstance().getChannelServer()) {
            ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(i);
            try {
                cwi.broadcastSMega(sender, message);
            } catch (RemoteException e) {
                WorldRegistryImpl.getInstance().deregisterChannelServer(i);
            }
        }
    }

    public String GetAllCharNames() throws RemoteException {
        ArrayList<String> allChars = new ArrayList<String>();
        StringBuilder sb = new StringBuilder();
        sb.append("Online players: ");
        for (int i : WorldRegistryImpl.getInstance().getChannelServer(world)) {
            ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(i, world);
            try {
                sb.append(cwi.getAllPlayerNames());
            } catch (RemoteException e) {
                WorldRegistryImpl.getInstance().deregisterChannelServer(i, world);
            }
        }
        int online = sb.toString().split(", ").length;
        sb.append(". That's ");
        sb.append(online);
        sb.append("players online.");

        return sb.toString();
    }

    public void changeRates(double exp, double drop, double bossdrop, double meso) throws RemoteException { //-1 for no change, negatives for multiples, positive for values
        double[] inputs = {exp, drop, bossdrop, meso};
        int[] newRates = getNewRates(inputs);
        for (int i : WorldRegistryImpl.getInstance().getChannelServer(world)) {
            ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(i, world);
            try {
                cwi.setRates(newRates);
            } catch (RemoteException e) {
                WorldRegistryImpl.getInstance().deregisterChannelServer(i, world);
            }
        }
    }

    private int[] getNewRates(double[] inputs) throws RemoteException {
        int[] outputs = new int[4];
        for (int i = 0; i < 4; i++) {
            if (i < 0) {
                if (i == 0) // exp
                {
                    outputs[i] = (int) Math.floor(ServerConstants.EXP_RATE * (inputs[i] * -1));
                }
                if (i == 2) // drop
                {
                    outputs[i] = (int) Math.floor(ServerConstants.DROP_RATE * (inputs[i] * -1));
                }
                if (i == 3) // bossdrop
                {
                    outputs[i] = (int) Math.floor(ServerConstants.BOSS_DROP_RATE * (inputs[i] * -1));
                }
                if (i == 3) // meso
                {
                    outputs[i] = (int) Math.floor(ServerConstants.MESO_RATE * (inputs[i] * -1));
                }
            } else {
                outputs[i] = (int) Math.floor(inputs[i]);
            }
        }
        return outputs;
    }

    public boolean isConnected(String charName) throws RemoteException {
        for (int i : WorldRegistryImpl.getInstance().getChannelServer(world)) {
            ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(i, world);
            try {
                if (cwi.isConnected(charName)) {
                    return true;
                }
            } catch (RemoteException e) {
                WorldRegistryImpl.getInstance().deregisterChannelServer(i, world);
            }
        }
        return false;
    }

    public void broadcastMessage(String sender, byte[] message) throws RemoteException {
        for (int i : WorldRegistryImpl.getInstance().getChannelServer(world)) {
            ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(i, world);
            try {
                cwi.broadcastMessage(sender, message);
            } catch (RemoteException e) {
                WorldRegistryImpl.getInstance().deregisterChannelServer(i, world);
            }
        }
    }

    public void broadcastGMMessage(String sender, byte[] message) throws RemoteException {
        for (int i : WorldRegistryImpl.getInstance().getChannelServer(world)) {
            ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(i, world);
            try {
                cwi.broadcastGMMessage(sender, message);
            } catch (RemoteException e) {
                WorldRegistryImpl.getInstance().deregisterChannelServer(i, world);
            }
        }
    }

    public int find(String charName) throws RemoteException {
        for (int i : WorldRegistryImpl.getInstance().getChannelServer(world)) {
            ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(i, world);
            try {
                if (cwi.isConnected(charName)) {
                    return cwi.getChannelId();
                }
            } catch (RemoteException e) {
                WorldRegistryImpl.getInstance().deregisterChannelServer(i, world);
            }
        }
        return -1;
    }

    // can we generify this
    @Override
    public int find(int characterId) throws RemoteException {
        for (int i : WorldRegistryImpl.getInstance().getChannelServer(world)) {
            ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(i, world);
            try {
                if (cwi.isConnected(characterId)) {
                    return cwi.getChannelId();
                }
            } catch (RemoteException e) {
                WorldRegistryImpl.getInstance().deregisterChannelServer(i, world);
            }
        }
        return -1;
    }

    public void shutdown(int time) throws RemoteException {
        for (LoginWorldInterface lwi : WorldRegistryImpl.getInstance().getLoginServer()) {
            try {
                lwi.shutdown();
            } catch (RemoteException e) {
                WorldRegistryImpl.getInstance().deregisterLoginServer(lwi);
            }
        }
        for (int i : WorldRegistryImpl.getInstance().getChannelServer(world)) {
            ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(i, world);
            try {
                cwi.shutdown(time);
            } catch (RemoteException e) {
                WorldRegistryImpl.getInstance().deregisterChannelServer(i, world);
            }
        }
    }

    public Map<Integer, Integer> getConnected() throws RemoteException {
        Map<Integer, Integer> ret = new HashMap<Integer, Integer>();
        int total = 0;
        for (int i : WorldRegistryImpl.getInstance().getChannelServer(world)) {
            ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(i, world);
            try {
                int curConnected = cwi.getConnected();
                ret.put(i, curConnected);
                total += curConnected;
            } catch (RemoteException e) {
                WorldRegistryImpl.getInstance().deregisterChannelServer(i, world);
            }
        }
        ret.put(0, total);
        return ret;
    }

    public void loggedOn(String name, int characterId, int channel, int[] buddies) throws RemoteException {
        for (int i : WorldRegistryImpl.getInstance().getChannelServer(world)) {
            ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(i, world);
            try {
                cwi.loggedOn(name, characterId, channel, buddies);
            } catch (RemoteException e) {
                WorldRegistryImpl.getInstance().deregisterChannelServer(i, world);
            }
        }
    }

    @Override
    public void loggedOff(String name, int characterId, int channel, int[] buddies) throws RemoteException {
        for (int i : WorldRegistryImpl.getInstance().getChannelServer(world)) {
            ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(i, world);
            try {
                cwi.loggedOff(name, characterId, channel, buddies);
            } catch (RemoteException e) {
                WorldRegistryImpl.getInstance().deregisterChannelServer(i, world);
            }
        }
    }

    //TODO only notify channels where partymembers are?
    public void updateParty(int partyid, PartyOperation operation, MaplePartyCharacter target) throws RemoteException {
        MapleParty party = WorldRegistryImpl.getInstance().getParty(partyid);
        if (party == null) {
            throw new IllegalArgumentException("no party with the specified partyid exists");
        }
        switch (operation) {
            case JOIN:
                party.addMember(target);
                break;
            case EXPEL:
            case LEAVE:
                party.removeMember(target);
                break;
            case DISBAND:
                WorldRegistryImpl.getInstance().disbandParty(partyid);
                break;
            case SILENT_UPDATE:
            case LOG_ONOFF:
                party.updateMember(target);
                break;
            case CHANGE_LEADER:
                party.setLeader(target);
                break;
            default:
                throw new RuntimeException("Unhandeled updateParty operation " + operation.name());
        }
        for (int i : WorldRegistryImpl.getInstance().getChannelServer(world)) {
            ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(i, world);
            try {
                cwi.updateParty(party, operation, target);
            } catch (RemoteException e) {
                WorldRegistryImpl.getInstance().deregisterChannelServer(i, world);
            }
        }
    }

    public MapleParty createParty(MaplePartyCharacter chrfor) throws RemoteException {
        return WorldRegistryImpl.getInstance().createParty(chrfor);
    }

    public MapleParty getParty(int partyid) throws RemoteException {
        return WorldRegistryImpl.getInstance().getParty(partyid);
    }

    @Override
    public void partyChat(int partyid, String chattext, String namefrom) throws RemoteException {
        MapleParty party = WorldRegistryImpl.getInstance().getParty(partyid);
        if (party == null) {
            throw new IllegalArgumentException("no party with the specified partyid exists");
        }
        for (int i : WorldRegistryImpl.getInstance().getChannelServer(world)) {
            ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(i, world);
            try {
                cwi.partyChat(party, chattext, namefrom);
            } catch (RemoteException e) {
                WorldRegistryImpl.getInstance().deregisterChannelServer(i, world);
            }
        }
    }

    public boolean isAvailable() throws RemoteException {
        return true;
    }

    public WorldLocation getLocation(String charName) throws RemoteException {
        for (int i : WorldRegistryImpl.getInstance().getChannelServer(world)) {
            ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(i, world);
            try {
                if (cwi.isConnected(charName)) {
                    return new WorldLocation(cwi.getLocation(charName), cwi.getChannelId());
                }
            } catch (RemoteException e) {
                WorldRegistryImpl.getInstance().deregisterChannelServer(i, world);
            }
        }
        return null;
    }

    @Override
    public ChannelWorldInterface getChannelInterface(int channel) {
        ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(channel, world);
        return cwi;
    }

    @Override
    public void buddyChat(int[] recipientCharacterIds, int cidFrom, String nameFrom, String chattext) throws RemoteException {
        for (ChannelWorldInterface cwi : WorldRegistryImpl.getInstance().getAllChannelServers(world)) {
            cwi.buddyChat(recipientCharacterIds, cidFrom, nameFrom, chattext);
        }
    }

    @Override
    public CharacterIdChannelPair[] multiBuddyFind(int charIdFrom, int[] characterIds) throws RemoteException {
        List<CharacterIdChannelPair> foundsChars = new ArrayList<CharacterIdChannelPair>(characterIds.length);
        for (int i : WorldRegistryImpl.getInstance().getChannelServer(world)) {
            ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(i, world);
            for (int charid : cwi.multiBuddyFind(charIdFrom, characterIds)) {
                foundsChars.add(new CharacterIdChannelPair(charid, i));
            }
        }
        return foundsChars.toArray(new CharacterIdChannelPair[foundsChars.size()]);
    }

    @Override
    public MapleGuild getGuild(int id, MapleGuildCharacter mgc) throws RemoteException {
        return WorldRegistryImpl.getInstance().getGuild(id, mgc, world);
    }

    @Override
    public void setGuildMemberOnline(MapleGuildCharacter mgc, boolean bOnline, int channel) throws RemoteException {
        WorldRegistryImpl.getInstance().setGuildMemberOnline(mgc, bOnline, channel, world);
    }

    @Override
    public int addGuildMember(MapleGuildCharacter mgc) throws RemoteException {
        return WorldRegistryImpl.getInstance().addGuildMember(mgc);
    }

    @Override
    public void guildChat(int gid, String name, int cid, String msg)
            throws RemoteException {
        WorldRegistryImpl.getInstance().guildChat(gid, name, cid, msg);
    }

    @Override
    public void leaveGuild(MapleGuildCharacter mgc) throws RemoteException {
        WorldRegistryImpl.getInstance().leaveGuild(mgc);
    }

    @Override
    public void changeRank(int gid, int cid, int newRank) throws RemoteException {
        WorldRegistryImpl.getInstance().changeRank(gid, cid, newRank);
    }

    @Override
    public void expelMember(MapleGuildCharacter initiator, String name, int cid) throws RemoteException {
        WorldRegistryImpl.getInstance().expelMember(initiator, name, cid);
    }

    @Override
    public void setGuildNotice(int gid, String notice) throws RemoteException {
        WorldRegistryImpl.getInstance().setGuildNotice(gid, notice);
    }

    @Override
    public void memberLevelJobUpdate(MapleGuildCharacter mgc) throws RemoteException {
        WorldRegistryImpl.getInstance().memberLevelJobUpdate(mgc);
    }

    @Override
    public void changeRankTitle(int gid, String[] ranks) throws RemoteException {
        WorldRegistryImpl.getInstance().changeRankTitle(gid, ranks);
    }

    @Override
    public int createGuild(int leaderId, String name) throws RemoteException {
        return WorldRegistryImpl.getInstance().createGuild(leaderId, name);
    }

    @Override
    public void setGuildEmblem(int gid, short bg, byte bgcolor, short logo, byte logocolor) throws RemoteException {
        WorldRegistryImpl.getInstance().setGuildEmblem(gid, bg, bgcolor, logo, logocolor);
    }

    @Override
    public void disbandGuild(int gid) throws RemoteException {
        WorldRegistryImpl.getInstance().disbandGuild(gid);
    }

    @Override
    public boolean increaseGuildCapacity(int gid) throws RemoteException {
        return WorldRegistryImpl.getInstance().increaseGuildCapacity(gid);
    }

    @Override
    public void gainGP(int gid, int amount) throws RemoteException {
        WorldRegistryImpl.getInstance().gainGP(gid, amount);
    }

    public MapleMessenger createMessenger(MapleMessengerCharacter chrfor) throws RemoteException {
        return WorldRegistryImpl.getInstance().createMessenger(chrfor);
    }

    public MapleMessenger getMessenger(int messengerid) throws RemoteException {
        return WorldRegistryImpl.getInstance().getMessenger(messengerid);
    }

    public void messengerInvite(String sender, int messengerid, String target, int fromchannel) throws RemoteException {
        for (int i : WorldRegistryImpl.getInstance().getChannelServer(world)) {
            ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(i, world);
            try {
                cwi.messengerInvite(sender, messengerid, target, fromchannel);
            } catch (RemoteException e) {
                WorldRegistryImpl.getInstance().deregisterChannelServer(i, world);
            }
        }
    }

    public void leaveMessenger(int messengerid, MapleMessengerCharacter target) throws RemoteException {
        MapleMessenger messenger = WorldRegistryImpl.getInstance().getMessenger(messengerid);
        if (messenger == null) {
            throw new IllegalArgumentException("No messenger with the specified messengerid exists");
        }
        int position = messenger.getPositionByName(target.getName());
        messenger.removeMember(target);
        for (int i : WorldRegistryImpl.getInstance().getChannelServer(world)) {
            ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(i, world);
            try {
                cwi.removeMessengerPlayer(messenger, position);
            } catch (RemoteException e) {
                WorldRegistryImpl.getInstance().deregisterChannelServer(i, world);
            }
        }
    }

    public void joinMessenger(int messengerid, MapleMessengerCharacter target, String from, int fromchannel) throws RemoteException {
        MapleMessenger messenger = WorldRegistryImpl.getInstance().getMessenger(messengerid);
        if (messenger == null) {
            throw new IllegalArgumentException("No messenger with the specified messengerid exists");
        }
        messenger.addMember(target);

        for (int i : WorldRegistryImpl.getInstance().getChannelServer(world)) {
            ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(i, world);
            try {
                cwi.addMessengerPlayer(messenger, from, fromchannel, target.getPosition());
            } catch (RemoteException e) {
                WorldRegistryImpl.getInstance().deregisterChannelServer(i, world);
            }
        }
    }

    public void messengerChat(int messengerid, String chattext, String namefrom) throws RemoteException {
        MapleMessenger messenger = WorldRegistryImpl.getInstance().getMessenger(messengerid);
        if (messenger == null) {
            throw new IllegalArgumentException("No messenger with the specified messengerid exists");
        }
        for (int i : WorldRegistryImpl.getInstance().getChannelServer(world)) {
            ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(i, world);
            try {
                cwi.messengerChat(messenger, chattext, namefrom);
            } catch (RemoteException e) {
                WorldRegistryImpl.getInstance().deregisterChannelServer(i, world);
            }
        }
    }

    public void declineChat(String target, String namefrom) throws RemoteException {
        for (int i : WorldRegistryImpl.getInstance().getChannelServer(world)) {
            ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(i, world);
            try {
                cwi.declineChat(target, namefrom);
            } catch (RemoteException e) {
                WorldRegistryImpl.getInstance().deregisterChannelServer(i, world);
            }
        }
    }

    public void updateMessenger(int messengerid, String namefrom, int fromchannel) throws RemoteException {
        MapleMessenger messenger = WorldRegistryImpl.getInstance().getMessenger(messengerid);
        int position = messenger.getPositionByName(namefrom);
        for (int i : WorldRegistryImpl.getInstance().getChannelServer(world)) {
            ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(i, world);
            try {
                cwi.updateMessenger(messenger, namefrom, position, fromchannel);
            } catch (RemoteException e) {
                WorldRegistryImpl.getInstance().deregisterChannelServer(i, world);
            }
        }
    }

    public void silentLeaveMessenger(int messengerid, MapleMessengerCharacter target) throws RemoteException {
        MapleMessenger messenger = WorldRegistryImpl.getInstance().getMessenger(messengerid);
        if (messenger == null) {
            throw new IllegalArgumentException("No messenger with the specified messengerid exists");
        }
        messenger.silentRemoveMember(target);
    }

    public void silentJoinMessenger(int messengerid, MapleMessengerCharacter target, int position) throws RemoteException {
        MapleMessenger messenger = WorldRegistryImpl.getInstance().getMessenger(messengerid);
        if (messenger == null) {
            throw new IllegalArgumentException("No messenger with the specified messengerid exists");
        }
        messenger.silentAddMember(target, position);
    }

    public void addBuffsToStorage(int chrid, List<PlayerBuffValueHolder> toStore) throws RemoteException {
        WorldRegistryImpl.getInstance().getPlayerBuffStorage().addBuffsToStorage(chrid, toStore);
    }

    public void sendSpouseChat(String sender, String target, String message) throws RemoteException {
        for (int i : WorldRegistryImpl.getInstance().getChannelServer(world)) {
            ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(i, world);
            try {
                cwi.sendSpouseChat(sender, target, message);
            } catch (Exception e) {
                WorldRegistryImpl.getInstance().deregisterChannelServer(i, world);
            }
        }
    }

    @Override
    public MapleAlliance getAlliance(int id) throws RemoteException {
        return WorldRegistryImpl.getInstance().getAlliance(id);
    }

    @Override
    public void addAlliance(int id, MapleAlliance alliance) throws RemoteException {
        WorldRegistryImpl.getInstance().addAlliance(id, alliance);
    }

    @Override
    public void disbandAlliance(int id) throws RemoteException {
        WorldRegistryImpl.getInstance().disbandAlliance(id);
    }

    @Override
    public void allianceMessage(int id, MaplePacket packet, int exception, int guildex) throws RemoteException {
        WorldRegistryImpl.getInstance().allianceMessage(id, packet, exception, guildex);
    }

    @Override
    public boolean setAllianceNotice(int aId, String notice) throws RemoteException {
        return WorldRegistryImpl.getInstance().setAllianceNotice(aId, notice);
    }

    @Override
    public boolean setAllianceRanks(int aId, String[] ranks) throws RemoteException {
        return WorldRegistryImpl.getInstance().setAllianceRanks(aId, ranks);
    }

    @Override
    public boolean removeGuildFromAlliance(int aId, int guildId) throws RemoteException {
        return WorldRegistryImpl.getInstance().removeGuildFromAlliance(aId, guildId);
    }

    @Override
    public boolean addGuildtoAlliance(int aId, int guildId) throws RemoteException {
        return WorldRegistryImpl.getInstance().addGuildtoAlliance(aId, guildId);
    }

    @Override
    public boolean setGuildAllianceId(int gId, int aId) throws RemoteException {
        return WorldRegistryImpl.getInstance().setGuildAllianceId(gId, aId);
    }

    @Override
    public boolean increaseAllianceCapacity(int aId, int inc) throws RemoteException {
        return WorldRegistryImpl.getInstance().increaseAllianceCapacity(aId, inc);
    }

    public List<PlayerBuffValueHolder> getBuffsFromStorage(int chrid) throws RemoteException {
        PlayerBuffStorage buffStorage = WorldRegistryImpl.getInstance().getPlayerBuffStorage();
        return buffStorage.getBuffsFromStorage(chrid);
    }

    public List<CheaterData> getCheaters(int world) throws RemoteException {
        List<CheaterData> allCheaters = new ArrayList<CheaterData>();
        for (int i : WorldRegistryImpl.getInstance().getChannelServer(world)) {
            try {
                ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(i, world);

                allCheaters.addAll(cwi.getCheaters());
            } catch (Exception e) {
                WorldRegistryImpl.getInstance().deregisterChannelServer(i, world);
            }
        }
        Collections.sort(allCheaters);
        return CollectionUtil.copyFirst(allCheaters, 10);
    }

    public String getAllPlayerNames(int world) throws RemoteException {
        StringBuilder sb = new StringBuilder();
        for (int i : WorldRegistryImpl.getInstance().getChannelServer(world)) {
            try {
                ChannelWorldInterface cwi = WorldRegistryImpl.getInstance().getChannel(i, world);

                sb.append(cwi.getAllPlayerNames());
            } catch (Exception e) {
                WorldRegistryImpl.getInstance().deregisterChannelServer(i, world);
            }
        }
        return sb.toString();
    }

    public void setProperty(String propertyName, Object value) {
        WorldRegistryImpl.getInstance().setProperty(propertyName, value);
    }

    public Object getProperty(String propertyName) {
        return WorldRegistryImpl.getInstance().getProperty(propertyName);
    }
}

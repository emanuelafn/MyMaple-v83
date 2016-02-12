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

import constants.ServerConstants;
import java.rmi.RemoteException;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import javax.script.ScriptEngine;
import tools.DatabaseConnection;
import net.channel.ChannelServer;
import net.login.LoginServer;
import net.world.MapleMessengerCharacter;
import net.world.MaplePartyCharacter;
import net.world.PartyOperation;
import net.world.guild.MapleGuildCharacter;
import net.world.remote.WorldChannelInterface;
import scripting.npc.NPCConversationManager;
import scripting.npc.NPCScriptManager;
import scripting.quest.QuestActionManager;
import scripting.quest.QuestScriptManager;
import server.MapleTrade;
import server.TimerManager;
import server.maps.HiredMerchant;
import tools.MapleAESOFB;
import tools.MaplePacketCreator;
import tools.HexTool;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.DummySession;
import server.MapleMiniGame;

public class MapleClient {

    public static final int LOGIN_NOTLOGGEDIN = 0;
    public static final int LOGIN_SERVER_TRANSITION = 1;
    public static final int LOGIN_LOGGEDIN = 2;
    public static final String CLIENT_KEY = "CLIENT";
    private MapleAESOFB send;
    private MapleAESOFB receive;
    private IoSession session;
    public MapleCharacter player;
    private int channel = 1;
    private int accId = 1;
    private boolean loggedIn = false;
    private boolean serverTransition = false;
    private Birthday birthday = null;
    private String accountName;
    private int world;
    private long lastPong;
    private int gmlevel;
    private Set<String> macs = new HashSet<String>();
    private Map<String, ScriptEngine> engines = new HashMap<String, ScriptEngine>();
    private ScheduledFuture<?> idleTask = null;
    private int characterSlots = 5;
    private byte loginattempt = 0;
    private String pin = null;
    private Calendar tempban;
    private byte greason = 0;
    private String pic = null;

    public MapleClient(MapleAESOFB send, MapleAESOFB receive, IoSession session) {
        this.send = send;
        this.receive = receive;
        this.session = session;
    }
    
    public MapleClient()//fake client
    {
        this.session = new DummySession();
        this.send = null;
        this.receive = null;
    }

    public synchronized MapleAESOFB getReceiveCrypto() {
        return receive;
    }

    public synchronized MapleAESOFB getSendCrypto() {
        return send;
    }

    public synchronized IoSession getSession() {
        return session;
    }

    public MapleCharacter getPlayer() {
        return player;
    }

    public void setPlayer(MapleCharacter player) {
        this.player = player;
    }

    public void sendCharList(int server) {
        this.session.write(MaplePacketCreator.getCharList(this, server));
    }

    public List<MapleCharacter> loadCharacters(int serverId) {
        List<MapleCharacter> chars = new ArrayList<MapleCharacter>(6);
        try {
            for (CharNameAndId cni : loadCharactersInternal(serverId)) {
                chars.add(MapleCharacter.loadCharFromDB(cni.id, this, false));
            }
        } catch (Exception e) {
        }
        return chars;
    }

    public List<String> loadCharacterNames(int serverId) {
        List<String> chars = new ArrayList<String>(6);
        for (CharNameAndId cni : loadCharactersInternal(serverId)) {
            chars.add(cni.name);
        }
        return chars;
    }

    private List<CharNameAndId> loadCharactersInternal(int serverId) {
        PreparedStatement ps;
        List<CharNameAndId> chars = new ArrayList<CharNameAndId>(6);
        try {
            ps = DatabaseConnection.getConnection().prepareStatement("SELECT id, name FROM characters WHERE accountid = ? AND world = ? AND deleted = 0");
            ps.setInt(1, this.getAccID());
            ps.setInt(2, serverId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                chars.add(new CharNameAndId(rs.getString("name"), rs.getInt("id")));
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return chars;
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public boolean hasBannedIP() {
		boolean ret = false;
		try {
			Connection con = DatabaseConnection.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM ipbans WHERE ? LIKE CONCAT(ip, '%')");
			ps.setString(1, session.getRemoteAddress().toString());
			ResultSet rs = ps.executeQuery();
			rs.next();
			if (rs.getInt(1) > 0) {
				ret = true;
			}
			rs.close();
			ps.close();
		} catch (SQLException ex) {
		}
		return ret;
	}

    public boolean hasBannedMac() {
		if (macs.isEmpty())
			return false;
		boolean ret = false;
		int i = 0;
		try {
			Connection con = DatabaseConnection.getConnection();
			StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM macbans WHERE mac IN (");
			for (i = 0; i < macs.size(); i++) {
				sql.append("?");
				if (i != macs.size() - 1)
					sql.append(", ");
			}
			sql.append(")");
			PreparedStatement ps = con.prepareStatement(sql.toString());
			i = 0;
			for (String mac : macs) {
				i++;
				ps.setString(i, mac);
			}
			ResultSet rs = ps.executeQuery();
			rs.next();
			if (rs.getInt(1) > 0) {
				ret = true;
			}
			rs.close();
			ps.close();
		} catch (SQLException ex) {
		}
		return ret;
	}

    private void loadMacsIfNescessary() throws SQLException {
        if (macs.isEmpty()) {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT macs FROM accounts WHERE id = ?");
            ps.setInt(1, accId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                for (String mac : rs.getString("macs").split(", ")) {
                    if (!mac.equals("")) {
                        macs.add(mac);
                    }
                }
            }
            rs.close();
            ps.close();
        }
    }

    public void banMacs() {
		Connection con = DatabaseConnection.getConnection();
		try {
			loadMacsIfNescessary();
			List<String> filtered = new LinkedList<String>();
			PreparedStatement ps = con.prepareStatement("SELECT filter FROM macfilters");
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				filtered.add(rs.getString("filter"));
			}
			rs.close();
			ps.close();
			ps = con.prepareStatement("INSERT INTO macbans (mac) VALUES (?)");
			for (String mac : macs) {
				boolean matched = false;
				for (String filter : filtered) {
					if (mac.matches(filter)) {
						matched = true;
						break;
					}
				}
				if (!matched) {
					ps.setString(1, mac);
					try {
						ps.executeUpdate();
					} catch (SQLException e) {
						// can fail because of UNIQUE key, we dont care
					}
				}
			}
			ps.close();
		} catch (SQLException e) {
		}
	}
    public int finishLogin() {
        synchronized (MapleClient.class) {
            if (getLoginState() > LOGIN_NOTLOGGEDIN) {
                loggedIn = false;
                return 7;
            }
            updateLoginState(LOGIN_LOGGEDIN);
        }
        return 0;
    }

    public void setPin(String pin) {
        this.pin = pin;
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("UPDATE accounts SET pin = ? WHERE id = ?");
            ps.setString(1, pin);
            ps.setInt(2, accId);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
        }
    }

    public String getPin() {
        return pin;
    }

    public int login(String login, String pwd, boolean ipMacBanned) {
        loginattempt++;
        if (loginattempt > 6) {
            getSession().close(true);
        }
        int loginok = 5;
        Connection con = DatabaseConnection.getConnection();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT id, password, salt, banned, gm, pin, greason, tempban FROM accounts WHERE name = ?");
            ps.setString(1, login);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int banned = rs.getInt("banned");
                this.accId = rs.getInt("id");
                setAccID(rs.getInt("id"));
                this.gmlevel = rs.getInt("gm");
                pin = rs.getString("pin");
                String passhash = rs.getString("password");
                String salt = rs.getString("salt");
                greason = rs.getByte("greason");
                tempban = getTempBanCalendar(rs);
                ps.close();
                if (banned > 0) {
                    loginok = 3;
                } else {
                    if (banned == -1) { // unban
                        int i;
                        try {
                            loadMacsIfNescessary();
                            //StringBuilder sql = new StringBuilder("DELETE FROM macbans WHERE mac IN (");
                            for (i = 0; i < macs.size(); i++) {
                                //sql.append("?");
                                if (i != macs.size() - 1) {
                                    //sql.append(", ");
                                }
                            }
                            //sql.append(")");
                           // ps = con.prepareStatement(sql.toString());
                            i = 0;
                            for (String mac : macs) {
                                ps.setString(++i, mac);
                            }
                            ps.executeUpdate();
                            ps.close();
                            ps = con.prepareStatement("DELETE FROM ipbans WHERE ip LIKE CONCAT(?, '%')");
                            ps.setString(1, getSession().getRemoteAddress().toString().split(":")[0]);
                            ps.executeUpdate();
                            ps.close();
                            ps = con.prepareStatement("UPDATE accounts SET banned = 0, norankupdate = 0 WHERE id = ?");
                            ps.setInt(1, accId);
                            ps.executeUpdate();
                            ps.close();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                    if (banned == 1) {
		    loginok = 3;
                    }
                    if (getLoginState() > LOGIN_NOTLOGGEDIN) { // already loggedin
                        loggedIn = false;
                        loginok = 7;
                    } else if (pwd.equals(passhash) || checkHash(passhash, "SHA-1", pwd) || checkHash(passhash, "SHA-512", pwd + salt)) {
                        loginok = 0;
                    } else {
                        loggedIn = false;
                        loginok = 4;
                    }
                }
            }
            rs.close();
            ps.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (loginok == 0) {
            loginattempt = 0;
        }
        return loginok;
    }

    private static long dottedQuadToLong(String dottedQuad) throws RuntimeException {
        String[] quads = dottedQuad.split("\\.");
        if (quads.length != 4) {
            throw new RuntimeException("Invalid IP Address format.");
        }
        long ipAddress = 0;
        for (int i = 0; i < 4; i++) {
            int quad = Integer.parseInt(quads[i]);
            ipAddress += (long) (quad % 256) * (long) Math.pow(256, (double) (4 - i));
        }
        return ipAddress;
    }

    public static String getChannelServerIPFromSubnet(String clientIPAddress, int channel) {
        long ipAddress = dottedQuadToLong(clientIPAddress);
        Properties subnetInfo = LoginServer.getInstance().getSubnetInfo();
        if (subnetInfo.contains("net.login.subnetcount")) {
            int subnetCount = Integer.parseInt(subnetInfo.getProperty("net.login.subnetcount"));
            for (int i = 0; i < subnetCount; i++) {
                String[] connectionInfo = subnetInfo.getProperty("net.login.subnet." + i).split(":");
                long subnet = dottedQuadToLong(connectionInfo[0]);
                long channelIP = dottedQuadToLong(connectionInfo[1]);
                int channelNumber = Integer.parseInt(connectionInfo[2]);
                if (((ipAddress & subnet) == (channelIP & subnet)) && (channel == channelNumber)) {
                    return connectionInfo[1];
                }
            }
        }
        return "0.0.0.0";
    }

        public String getPic() {
        return pic;
    }

    public void updateMacs(String macData) {
        for (String mac : macData.split(", ")) {
            macs.add(mac);
        }
        StringBuilder newMacData = new StringBuilder();
        Iterator<String> iter = macs.iterator();
        while (iter.hasNext()) {
            String cur = iter.next();
            newMacData.append(cur);
            if (iter.hasNext()) {
                newMacData.append(", ");
            }
        }
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("UPDATE accounts SET macs = ? WHERE id = ?");
            ps.setString(1, newMacData.toString());
            ps.setInt(2, accId);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setAccID(int id) {
        this.accId = id;
    }

    public int getAccID() {
        return accId;
    }

    public void updateLoginState(int newstate) {
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("UPDATE accounts SET loggedin = ?, lastlogin = CURRENT_TIMESTAMP() WHERE id = ?");
            ps.setInt(1, newstate);
            ps.setInt(2, getAccID());
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (newstate == LOGIN_NOTLOGGEDIN) {
            loggedIn = false;
            serverTransition = false;
            Date date = new Date();
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss");
            String time = sdf.format(date);
            if(player.isGM()){
            System.out.println("[" + time + "] (GM) " + getPlayer() + " logged off.");
            }
            else{
            System.out.println("[" + time + "] (Player) " + getPlayer() + " logged off.");   
            }
            System.out.println();
        } else {
            serverTransition = (newstate == LOGIN_SERVER_TRANSITION);
            loggedIn = !serverTransition;
        }
    }

    /*public int getLoginState() {
    try {
    Connection con = DatabaseConnection.getConnection();
    PreparedStatement ps = con.prepareStatement("SELECT loggedin, lastlogin, UNIX_TIMESTAMP(birthday) as birthday FROM accounts WHERE id = ?");
    ps.setInt(1, getAccID());
    ResultSet rs = ps.executeQuery();
    if (!rs.next()) {
    rs.close();
    ps.close();
    throw new RuntimeException("getLoginState - MapleClient");
    }
    birthday = Calendar.getInstance();
    long blubb = rs.getLong("birthday");
    if (blubb >= 0) {
    birthday.setTimeInMillis(blubb * 1000);
    }
    int state = rs.getInt("loggedin");
    if (state == LOGIN_SERVER_TRANSITION) {
    if (rs.getTimestamp("lastlogin").getTime() + 30000 < System.currentTimeMillis()) {
    state = LOGIN_NOTLOGGEDIN;
    updateLoginState(LOGIN_NOTLOGGEDIN);
    }
    }
    rs.close();
    ps.close();
    if (state == LOGIN_LOGGEDIN) {
    loggedIn = true;
    } else if (state == LOGIN_SERVER_TRANSITION) {
    ps = con.prepareStatement("UPDATE accounts SET loggedin = 0 WHERE id = ?");
    ps.setInt(1, getAccID());
    ps.executeUpdate();
    ps.close();
    } else {
    loggedIn = false;
    }
    return state;
    } catch (SQLException e) {
    loggedIn = false;
    e.printStackTrace();
    throw new RuntimeException("login state");
    }
    }*/
    public int getLoginState() { // TODO hide?
        Connection con = DatabaseConnection.getConnection();
        try {
            PreparedStatement ps;
            //"CAST(birthday AS CHAR) as birthday" instead of just "birthday" gives us a workaround for
            //a java.sql.Date limitation for null/undefined (0000-00-00) date, since we need the null date for some checks.
            ps = con.prepareStatement("SELECT loggedin, lastlogin, CAST(birthday AS CHAR) as birthday FROM accounts WHERE id = ?");
            ps.setInt(1, getAccID());
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                ps.close();
                throw new RuntimeException("Error with getLoginState");
            }
            birthday = new Birthday(rs.getString("birthday"));
            int state = rs.getInt("loggedin");
            if (state == MapleClient.LOGIN_SERVER_TRANSITION) {
                java.sql.Timestamp ts = rs.getTimestamp("lastlogin");
                long t = ts.getTime();
                long now = System.currentTimeMillis();
                if (t + 30000 < now) { // connecting to chanserver timeout
                    state = MapleClient.LOGIN_NOTLOGGEDIN;
                    updateLoginState(MapleClient.LOGIN_NOTLOGGEDIN);
                }
            }
            rs.close();
            ps.close();
            loggedIn = (state == MapleClient.LOGIN_LOGGEDIN);
            return state;
        } catch (SQLException e) {
            loggedIn = false;
            e.printStackTrace();
            throw new RuntimeException("Error with getLoginState", e);
        }
    }

    /**
     * Slight optimization. Why do we need to convert idate to year, month, day and
     * put it in a calendar so that we can take out the year, month, and day data from
     * the calendar again when checking the birthday? All birthday information is sent
     * in the same format, so just use this as a common method to reduce redundancy.
     */
    public boolean checkBirthDate(int idate) {
        if (birthday == null) {
            getLoginState();
        }
        return birthday.equals(idate);
    }

    public String getBirthday() {
        if (birthday == null) {
            getLoginState();
        }
        return birthday.getDateString(false);
    }

    public void setBirthday(String birthday) {
        this.birthday.setBirthday(birthday);
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("UPDATE accounts SET birthday = ? WHERE id = ?");
            ps.setString(1, this.birthday.getDateString(true));
            ps.setInt(2, accId);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        if (player != null && isLoggedIn()) {
            if (player.getTrade() != null) {
                MapleTrade.cancelTrade(player);
            }
            player.saveCooldowns();
//            player.getFamily().setPlayer(null);
            MapleMiniGame game = player.getMiniGame();
            if (game != null) {
                player.setMiniGame(null);
                if (game.isOwner(player)) {
                    player.getMap().broadcastMessage(MaplePacketCreator.removeCharBox(player));
                    game.broadcastToVisitor(MaplePacketCreator.getMiniGameClose((byte) 0));
                } else {
                    game.removeVisitor(player);
                }
            }
            player.cancelAllBuffs();
            if (player.getEventInstance() != null) {
                player.getEventInstance().playerDisconnected(player);
            }
            HiredMerchant merchant = player.getHiredMerchant();
            if (merchant != null) {
                if (merchant.isOwner(player)) {
                    merchant.setOpen(true);
                } else {
                    merchant.removeVisitor(player);
                }
            }
            player.unequipAllPets();
            try {
                WorldChannelInterface wci = getChannelServer().getWorldInterface();
                if (player.getMessenger() != null) {
                    MapleMessengerCharacter messengerplayer = new MapleMessengerCharacter(player);
                    wci.leaveMessenger(player.getMessenger().getId(), messengerplayer);
                    player.setMessenger(null);
                }
            } catch (RemoteException e) {
                getChannelServer().reconnectWorld();
                e.printStackTrace();
            }
            NPCScriptManager npcsm = NPCScriptManager.getInstance();
            if (npcsm != null) {
                npcsm.dispose(this);
            }
            if (!player.isAlive()) {
                player.setHp(50, true);
            }
            player.setMessenger(null);
            player.saveToDB(true);
            player.getMap().removePlayer(player);
            try {
                WorldChannelInterface wci = getChannelServer().getWorldInterface();
                if (player.getParty() != null) {
                    MaplePartyCharacter chrp = new MaplePartyCharacter(player);
                    chrp.setOnline(false);
                    wci.updateParty(player.getParty().getId(), PartyOperation.LOG_ONOFF, chrp);
                }
            } catch (RemoteException e) {
                getChannelServer().reconnectWorld();
                e.printStackTrace();
            }
            try {
                WorldChannelInterface wci = getChannelServer().getWorldInterface();
                if (!this.serverTransition && isLoggedIn()) {
                    wci.loggedOff(player.getName(), player.getId(), channel, player.getBuddylist().getBuddyIds());
                } else {
                    wci.loggedOn(player.getName(), player.getId(), channel, player.getBuddylist().getBuddyIds());
                }
                if (player.getGuildId() > 0) {
                    wci.setGuildMemberOnline(player.getMGC(), false, -1);
                    int allianceId = player.getGuild().getAllianceId();
                    if (allianceId > 0) {
                        wci.allianceMessage(allianceId, MaplePacketCreator.allianceMemberOnline(player, false), player.getId(), -1);
                    }
                }
            } catch (RemoteException e) {
                getChannelServer().reconnectWorld();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (getChannelServer() != null) {
                    getChannelServer().removePlayer(player);
                }
                this.getSession().close();
            }
        }
        if (!this.serverTransition && isLoggedIn()) {
            this.updateLoginState(LOGIN_NOTLOGGEDIN);
        }
    }

    public int getChannel() {
        return channel;
    }

    public ChannelServer getChannelServer() {
        return ChannelServer.getInstance(getChannel());
    }

    public byte deleteCharacter(int cid) {
        Connection con = DatabaseConnection.getConnection();
        String charname = "";
        try {
            PreparedStatement ps = con.prepareStatement("SELECT id, guildid, guildrank, name, allianceRank FROM characters WHERE id = ? AND accountid = ?");
            ps.setInt(1, cid);
            ps.setInt(2, accId);
            ResultSet rs = ps.executeQuery();
            rs.next();
            charname = rs.getString("name");
            if (rs.getInt("guildid") > 0) {
                if (rs.getInt("guildrank") == 1) {
                    rs.close();
                    ps.close();
                    return 22;
                }
                try {
                    LoginServer.getInstance().getWorldInterface().deleteGuildCharacter(new MapleGuildCharacter(cid, 0, charname, -1, 0, rs.getInt("guildrank"), rs.getInt("guildid"), false, rs.getInt("allianceRank")), world);
                } catch (RemoteException re) {
                    rs.close();
                    ps.close();
                    return 1;
                }
            }
            rs.close();
            /*    ps = con.prepareStatement("UPDATE characters set `deleted` = 1, `name` = ? WHERE id = ?");
            ps.setString(1, "___" + charname);
            ps.setInt(2, cid);
            ps.executeUpdate();
            ps.close();*/
            ps = con.prepareStatement("DELETE FROM characters WHERE id = ?");
            ps.setInt(1, cid);
            ps.executeUpdate();
            ps.close();
            String[] toDel = {"famelog", "keymap", "queststatus", "savedlocations", "skillmacros", "skills", "eventstats"};
            for (String s : toDel) {
                ps = con.prepareStatement("DELETE FROM `" + s + "` WHERE characterid = ?");
                ps.setInt(1, cid);
                ps.executeUpdate();
                ps.close();
            }
            ps = con.prepareStatement("DELETE FROM koccharacters WHERE knightId = ?");
            ps.setInt(1, cid);
            ps.executeUpdate();
            ps.close();
            ps = con.prepareStatement("DELETE FROM koccharacters WHERE linkedId = ?");
            ps.setInt(1, cid);
            ps.executeUpdate();
            ps.close();
            return 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return 1;
        }
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String a) {
        this.accountName = a;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    public int getWorld() {
        return world;
    }

    public void setWorld(int world) {
        this.world = world;
    }

    public void pongReceived() {
        lastPong = System.currentTimeMillis();
    }

    public void sendPing() {
        final long then = System.currentTimeMillis();
        getSession().write(MaplePacketCreator.getPing());
        TimerManager.getInstance().schedule(new Runnable() {

            @Override
            public void run() {
                if (lastPong < then) {
                    if (getSession().isConnected() && !ServerConstants.DEBUG) {
                        getSession().close(true);
                    }
                    if (player != null) {
                        ChannelServer.getInstance(channel).removePlayer(player);
                        player.setStuck(true);
                    }
                }
            }
        }, 15000);
    }

    public Set<String> getMacs() {
        return Collections.unmodifiableSet(macs);
    }

    public int gmLevel() {
        return this.gmlevel;
    }

    public void setScriptEngine(String name, ScriptEngine e) {
        engines.put(name, e);
    }

    public ScriptEngine getScriptEngine(String name) {
        return engines.get(name);
    }

    public void removeScriptEngine(String name) {
        engines.remove(name);
    }

    public ScheduledFuture<?> getIdleTask() {
        return idleTask;
    }

    public void setIdleTask(ScheduledFuture<?> idleTask) {
        this.idleTask = idleTask;
    }

    public NPCConversationManager getCM() {
        return NPCScriptManager.getInstance().getCM(this);
    }

    public QuestActionManager getQM() {
        return QuestScriptManager.getInstance().getQM(this);
    }

    private static class CharNameAndId {

        public String name;
        public int id;

        public CharNameAndId(String name, int id) {
            super();
            this.name = name;
            this.id = id;
        }
    }

    private static boolean checkHash(String hash, String type, String password) {
        try {
            MessageDigest digester = MessageDigest.getInstance(type);
            digester.update(password.getBytes("UTF-8"), 0, password.length());
            return HexTool.toString(digester.digest()).replace(" ", "").toLowerCase().equals(hash);
        } catch (Exception e) {
            throw new RuntimeException("Encoding the string failed", e);
        }
    }

    public int getCharacterSlots() {
        return characterSlots;
    }

    public void setCharacterSlots(int amount) {
        this.characterSlots = amount;
    }

    public static int findAccIdForCharacterName(String charName) {
        Connection con = DatabaseConnection.getConnection();

        try {
            PreparedStatement ps = con.prepareStatement("SELECT accountid FROM characters WHERE name = ?");
            ps.setString(1, charName);
            ResultSet rs = ps.executeQuery();

            int ret = -1;
            if (rs.next()) {
                ret = rs.getInt("accountid");
            }
            rs.close();
            ps.close();
            return ret;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    private Calendar getTempBanCalendar(ResultSet rs) throws SQLException {
        Calendar lTempban = Calendar.getInstance();
        long blubb = rs.getLong("tempban");
        if (blubb == 0) { // basically if timestamp in db is 0000-00-00
            lTempban.setTimeInMillis(0);
            return lTempban;
        }
        Calendar today = Calendar.getInstance();
        lTempban.setTimeInMillis(rs.getTimestamp("tempban").getTime());
        if (today.getTimeInMillis() < lTempban.getTimeInMillis()) {
            return lTempban;
        }

        lTempban.setTimeInMillis(0);
        return lTempban;
    }

    public Calendar getTempBanCalendar() {
        return tempban;
    }

    public byte getBanReason() {
        return greason;
    }

    public void empty() {
        if (this.player != null) {
            if (this.player.getMount() != null) {
                this.player.getMount().empty();
            }
            this.player.empty();

        }
        this.session = null;
        this.engines.clear();
        this.engines = null;
        this.send = null;
        this.receive = null;
        this.channel = -1;
    }

    /**
     * Simple instance to hold a user's birthdate with seperate fields for year, month and date,
     * without dealing with time, because that's not needed for a birthday, and allows the server
     * to hold birthdays before the Unix Epoch (January 1, 1970).
     *
     * @author GoldenKevin
     */
    private static class Birthday {

        private int year;
        private int month;
        private int day;

        /**
         * Creates a birthdate instance with the year, month, and day values set to 0.
         */
        public Birthday() {
        }

        /**
         * Create a new birthday instance using the 8-digit integer-based date format
         * that the MapleStory client sends. Be careful to cast any numbers as
         * integers so you don't accidentally use the Timestamp-based constructor instead.
         * @param idate birthday in 8-digit integer-based YYYYMMDD format
         */
        public Birthday(int idate) {
            setBirthday(idate);
        }

        /**
         * Create a new birthday instance using the year, the month, and the day of the
         * month of the birthday, based on the Gregorian Calendar.
         * @param year year of birthday
         * @param month month of birthday
         * @param day day of the month of birthday
         */
        public Birthday(int year, int month, int day) {
            setBirthday(year, month, day);
        }

        /**
         * Create a new birthday instance by passing a date string in YYYY-MM-DD format
         * or in YYYYMMDD format.
         * @param date date in string representation
         * @throws RuntimeException if the string is not in YYYY-MM-DD or YYYYMMDD format.
         */
        public Birthday(String date) throws RuntimeException {
            setBirthday(date);
        }

        /**
         * Change the current birthday using the 8-digit integer-based date format
         * that the MapleStory client sends. Be careful to cast any numbers as
         * integers so you don't accidentally use the Timestamp-based constructor instead.
         * @param idate birthday in 8-digit integer-based YYYYMMDD format
         */
        public void setBirthday(int idate) {
            year = idate / 10000;
            month = (idate % 10000) / 100; //(idate - year * 10000) / 100
            day = idate % 100; //idate - year * 10000 - month * 100
        }

        /**
         * Change the current birthday using the year, the month, and the day of the
         * month of the birthday, based on the Gregorian Calendar.
         * @param year year of birthday
         * @param month month of birthday
         * @param day day of the month of birthday
         */
        public void setBirthday(int year, int month, int day) {
            this.year = year;
            this.month = month;
            this.day = day;
        }

        /**
         * Change the current birthday by passing a date string in YYYY-MM-DD format
         * or in YYYYMMDD format.
         * @param date date in string representation
         * @throws RuntimeException if the string is not in YYYY-MM-DD or YYYYMMDD format.
         */
        public void setBirthday(String date) throws RuntimeException {
            if (date.length() == 8) { //YYYYMMDD
                try {
                    //Perhaps we can use setBirthday(Integer.parseInt(date)); since an 8-digit birthday integer is an idate?
                    year = Integer.parseInt(date.substring(0, 4));
                    month = Integer.parseInt(date.substring(4, 6));
                    day = Integer.parseInt(date.substring(6));
                } catch (NumberFormatException e) {
                    throw new RuntimeException("Invalid Birthday Date String format : " + date + " could not be resolved to YYYYMMDD.");
                }
            } else if (date.length() == 10) { //YYYY-MM-DD
                String[] splitted = date.split("-");
                try {
                    year = Integer.parseInt(splitted[0]);
                    month = Integer.parseInt(splitted[1]);
                    day = Integer.parseInt(splitted[2]);
                } catch (NumberFormatException e) {
                    throw new RuntimeException("Invalid Birthday Date String format : " + date + " could not be resolved to YYYY-MM-DD.");
                }
            } else {
                throw new RuntimeException("Invalid Birthday Date String format : " + date + " could not be resolved to YYYY-MM-DD or YYYYMMDD.");
            }
        }

        /**
         * Set the year that the birthday is in.
         * @param year the year to set the birthday to.
         */
        public void setYear(int year) {
            this.year = year;
        }

        /**
         * Set the month that the birthday is in.
         * @param month the month to set the birthday to.
         */
        public void setMonth(int month) {
            this.month = month;
        }

        /**
         * Set the day of the month that the birthday is in.
         * @param day the day of the month to set the birthday to.
         */
        public void setDay(int day) {
            this.day = day;
        }

        /**
         * Get the birthday in a string format with either YYYYMMDD or YYYY-MM-DD depending on
         * whether you use the parameter withDashes or not.
         * @param withDashes return a string in YYYY-MM-DD format if true, otherwise, will
         * return a string in YYYYMMDD format.
         * @return a string of the birthday in YYYY-MM-DD or YYYYMMDD format.
         */
        public String getDateString(boolean withDashes) {
            NumberFormat fmt = new DecimalFormat("00");
            StringBuilder builder = new StringBuilder(withDashes ? 10 : 8);
            builder.append(new DecimalFormat("0000").format(year));
            if (withDashes) {
                builder.append("-");
            }
            builder.append(fmt.format(month));
            if (withDashes) {
                builder.append("-");
            }
            builder.append(fmt.format(day));
            return builder.toString();
            //Perhaps we can use an idate and concatenate it to a String if we only
            //need an 8-digit birthday and withDashes is false.
        }

        /**
         * Get the birthday in an 8-digit integer-based date format. (YYYYMMDD)
         * @return The birthdate in idate format.
         */
        public int getIntegerDate() {
            int idate = 0;
            idate += year * 10000;
            idate += month * 100;
            idate += day;
            return idate;
        }

        /**
         * Get the year of the birthday
         * @return The year of the birthday.
         */
        public int getYear() {
            return year;
        }

        /**
         * Get the month of the birthday
         * @return The month of the birthday.
         */
        public int getMonth() {
            return month;
        }

        /**
         * Get the day of the month of the birthday
         * @return The day of the month of the birthday.
         */
        public int getDay() {
            return day;
        }

        /**
         * Check if the user's real birthday matches with the given year, month, and day of the month
         * @param year the year to compare the birthday to
         * @param month the month to compare the birthday to
         * @param day the day to compare the birthday to
         * @return
         */
        public boolean equals(int year, int month, int day) {
            return ((this.year == 0 && this.month == 0 && this.day == 0)
                    || (this.year == year && this.month == month && this.day == day));
        }

        /**
         * Check if the user's real birthday matches with the given idate
         * @param idate birthday in YYYYMMDD format
         * @return
         */
        public boolean equals(int idate) {
            return equals((idate / 10000), ((idate - year * 10000) / 100), (idate - year * 10000 - month * 100));
        }

        /**
         * Check if the user's real birthday matches with another Birthday object
         * @param birthday Birthday that holds the year, month, and day of the values to be compared.
         * @return
         */
        public boolean equals(Birthday birthday) {
            return this == birthday || equals(birthday.getYear(), birthday.getMonth(), birthday.getDay());
        }

        /**
         * Check if the user's real birthday matches with the given date string in YYYY-MM-DD
         * or in YYYYMMDD format.
         * @param date date in string representation
         * @throws RuntimeException if the string is not in YYYY-MM-DD or YYYYMMDD format.
         */
        public boolean equals(String date) {
            //Maybe just "return getDateString(false).equals(date) || getDateString(true).equals(date);"?
            if (date.length() == 8) {
                try { //YYYYMMDD
                    //Perhaps we can use "equals(Integer.parseInt(date));" since an 8-digit birthday integer is an idate?
                    return equals(Integer.parseInt(date.substring(0, 4)), //year
                            Integer.parseInt(date.substring(4, 6)), //month
                            Integer.parseInt(date.substring(6))); //day
                } catch (NumberFormatException e) {
                    throw new RuntimeException("Invalid Birthday Date String format : " + date + " could not be resolved to YYYYMMDD.");
                }
            } else if (date.length() == 10) { //YYYY-MM-DD
                String[] splitted = date.split("-");
                try {
                    return equals(Integer.parseInt(splitted[0]), //year
                            Integer.parseInt(splitted[1]), //month
                            Integer.parseInt(splitted[2])); //day
                } catch (NumberFormatException e) {
                    throw new RuntimeException("Invalid Birthday Date String format : " + date + " could not be resolved to YYYY-MM-DD.");
                }
            } else {
                throw new RuntimeException("Invalid Birthday Date String format : " + date + " could not be resolved to YYYY-MM-DD or YYYYMMDD.");
            }
        }
    }
}

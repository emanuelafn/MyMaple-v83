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
package net.channel;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MalformedObjectNameException;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import client.MapleCharacter;
import constants.ServerConstants;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.Properties;
import net.world.MapleParty;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import tools.DatabaseConnection;
import net.MaplePacket;
import net.MapleServerHandler;
import net.PacketProcessor;
import net.ServerMode;
import net.ServerMode.Mode;
import net.mina.MapleCodecFactory;
import net.world.MaplePartyCharacter;
import net.world.guild.MapleGuild;
import net.world.guild.MapleGuildCharacter;
import net.world.guild.MapleGuildSummary;
import net.world.remote.WorldChannelInterface;
import net.world.remote.WorldRegistry;
import provider.MapleDataProviderFactory;
import scripting.event.EventScriptManager;
import server.ShutdownServer;
import server.TimerManager;
import server.maps.MapleMapFactory;
import tools.MaplePacketCreator;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.buffer.SimpleBufferAllocator;
import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.SocketSessionConfig;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import server.MapleSquad;
import server.MapleSquadType;
import net.world.WorldServer;
import server.maps.MapleMap;

public class ChannelServer implements Runnable {

    private int port = 7575;
    private static WorldRegistry worldRegistry;
    private PlayerStorage players = new PlayerStorage();
    private int channel;
    private String key;
    private ChannelWorldInterface cwi;
    private WorldChannelInterface wci = null;
    private IoAcceptor acceptor;
    private String ip;
    private boolean shutdown = false;
    private boolean finishedShutdown = false;
    private MapleMapFactory mapFactory;
    private EventScriptManager eventSM;
    private static Map<Integer, ChannelServer> instances = new HashMap<Integer, ChannelServer>();
    private static Map<String, ChannelServer> pendingInstances = new HashMap<String, ChannelServer>();
    private Map<Integer, MapleGuildSummary> gsStore = new HashMap<Integer, MapleGuildSummary>();
    private Boolean worldReady = true;
    private int instanceId = 0;
    private Map<MapleSquadType, MapleSquad> mapleSquads = new HashMap<MapleSquadType, MapleSquad>();
    private static int world = 0;
    private int EXP_RATE = ServerConstants.EXP_RATE;
    private int DROP_RATE = ServerConstants.DROP_RATE;
    private int BOSS_DROP_RATE = ServerConstants.BOSS_DROP_RATE;
    private int MESO_RATE = ServerConstants.MESO_RATE;
    private static int RespawnRate = ServerConstants.RESPAWN_RATE;
    public boolean eventOn = false;
    public int eventMap = 0;
    private HiredMerchantRegistry HMRegistry = new HiredMerchantRegistry(channel);

    private ChannelServer(String key) {
        mapFactory = new MapleMapFactory(MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/Map.wz")), MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/String.wz")));
        this.key = key;
    }

    public static WorldRegistry getWorldRegistry() {
        return worldRegistry;
    }

    public int getInstanceId() {
        return instanceId;
    }

        public static int RespawnRate() {
        return RespawnRate;
    }
public void allocateSkills(){
        if(!WorldServer.getInstance().skillsLoaded){
            client.SkillFactory.cacheSkills();
            WorldServer.getInstance().skillsLoaded = true;
        }
    }


    public void addInstanceId() {
        instanceId++;
    }

    public void reconnectWorld() {
        reconnectWorld(false);
    }
     
    public void reconnectWorld(boolean force) {
        if (!force) {
            try {
                wci.isAvailable();
            } catch (RemoteException ex) {
                synchronized (worldReady) {
                    worldReady = false;
                }
            }
            synchronized (cwi) {
                synchronized (worldReady) {
                    if (worldReady && !force) {
                        return;
                    }
                }
                System.out.println("Reconnecting to world server");
                synchronized (wci) {
                    try {
                        Registry registry = LocateRegistry.getRegistry(ServerConstants.HOST, Registry.REGISTRY_PORT, new SslRMIClientSocketFactory());
                        worldRegistry = (WorldRegistry) registry.lookup("WorldRegistry");
                        cwi = new ChannelWorldInterfaceImpl(this, world);
                        wci = worldRegistry.registerChannelServer(key, cwi, world);
                        Properties dbProp = new Properties();
                        FileReader fr = new FileReader("db.properties");
                        dbProp.load(fr);
                        fr.close();
                        DatabaseConnection.setProps(dbProp);
                        DatabaseConnection.getConnection();
                        wci.serverReady(world);
                    } catch (Exception e) {
                    }
                    worldReady = true;
                }
            }
            synchronized (worldReady) {
                worldReady.notifyAll();
            }
        }
    }

    @Override
    public void run() {
        try {
            cwi = new ChannelWorldInterfaceImpl(this, world);
            wci = worldRegistry.registerChannelServer(key, cwi, world);
            eventSM = new EventScriptManager(this, ServerConstants.EVENTS.split(" "));
            Properties dbProp = new Properties();
            FileReader fileReader = new FileReader("db.properties");
            dbProp.load(fileReader);
            fileReader.close();
            allocateSkills();
            DatabaseConnection.setProps(dbProp);
            Connection c = DatabaseConnection.getConnection();
            PreparedStatement ps = c.prepareStatement("UPDATE accounts SET loggedin = 0");
            ps.executeUpdate();
            ps.close();
            ps = c.prepareStatement("UPDATE characters SET HasMerchant = 0");
            ps.executeUpdate();
            ps.close();
            port = 7575 + this.channel - 1;
            ip = ServerConstants.HOST + ":" + port;
            IoBuffer.setUseDirectBuffer(false);
            IoBuffer.setAllocator(new SimpleBufferAllocator());
            acceptor = new NioSocketAcceptor();
            TimerManager tMan = TimerManager.getInstance();
            tMan.start();
            //tMan.register(new respawnMaps(), 10000);
            tMan.register(new Runnable() {

                @Override
                public void run() {
                    for(MapleCharacter chr : getPlayerStorage().getAllCharacters()){
                        if(chr.isFishing && chr.getMapId() == 970020001){
                            server.Fishing.doFishing(chr);
                        }
                    }
                    TimerManager.getInstance().purgeTM();
                }
            }, 30000); //cleans all ca
            
            acceptor.setHandler((IoHandler) new MapleServerHandler(PacketProcessor.getProcessor(PacketProcessor.Mode.CHANNELSERVER), channel));
            acceptor.getSessionConfig().setIdleTime(IdleStatus.BOTH_IDLE, 30);
            acceptor.getFilterChain().addLast("codec", (IoFilter) new ProtocolCodecFilter((ProtocolCodecFactory) new MapleCodecFactory()));
            acceptor.bind(new InetSocketAddress(port));
            ((SocketSessionConfig) acceptor.getSessionConfig()).setTcpNoDelay(true);
            System.out.println("[INFO] Channel " + getChannel() + ": Listening on port " + port);
            wci.serverReady(world);
            eventSM.init();
        } 
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void shutdown() {
        shutdown = true;
        boolean error = true;
        while (error) {
            try {
                for (MapleCharacter chr : players.getAllCharacters()) {
                    synchronized (chr) {
                        if (chr.getHiredMerchant().isOpen()) {
                            chr.getHiredMerchant().saveItems();
                        }
                        chr.getClient().disconnect();
                    }
                    error = false;
                }
            } catch (Exception e) {
                error = true;
            }
        }
        finishedShutdown = true;
        wci = null;
        cwi = null;
    }
    
    public void shutdownWorld(int time) {
        try {
            getWorldInterface().shutdown(time);
        } catch (RemoteException e) {
            reconnectWorld();
        }
    }

    public void unbind() {
        acceptor.unbind();
    }

    public void broadcastSMega(MaplePacket data) {
        for (MapleCharacter chr : players.getAllCharacters()) {
                chr.getClient().getSession().write(data);
        }
    }

    public boolean hasFinishedShutdown() {
        return finishedShutdown;
    }

    public MapleMapFactory getMapFactory() {
        return mapFactory;
    }

    private static ChannelServer newInstance(String key) throws InstanceAlreadyExistsException, MalformedObjectNameException {
        ChannelServer instance = new ChannelServer(key);
        pendingInstances.put(key, instance);
        return instance;
    }

    public static ChannelServer getInstance(int channel) {
        return instances.get(channel);
    }

    public void addPlayer(MapleCharacter chr) {
        players.registerPlayer(chr);
        chr.getClient().getSession().write(MaplePacketCreator.serverMessage(ServerConstants.SERVER_MESSAGE));
    }

    public IPlayerStorage getPlayerStorage() {
        return players;
    }

    public void removePlayer(MapleCharacter chr) {
        chr.cancelPeriodicSaveTask();
        players.deregisterPlayer(chr);
    }

    public int getConnectedClients() {
        return players.getAllCharacters().size();
    }

    public void setServerMessage(String newMessage) {
        ServerConstants.SERVER_MESSAGE = newMessage;
        broadcastPacket(MaplePacketCreator.serverMessage(ServerConstants.SERVER_MESSAGE));
    }

    public void broadcastPacket(MaplePacket data) {
        for (MapleCharacter chr : players.getAllCharacters()) {
            chr.getClient().getSession().write(data);
        }
    }

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        if (pendingInstances.containsKey(key)) {
            pendingInstances.remove(key);
        }
        if (instances.containsKey(channel)) {
            instances.remove(channel);
        }
        instances.put(channel, this);
        this.channel = channel;
        this.mapFactory.setChannel(channel);
    }

    public static Collection<ChannelServer> getAllInstances() {
        return Collections.unmodifiableCollection(instances.values());
    }

    public String getIP() {
        return ip;
    }

    public String getIP(int channel) {
        try {
            return getWorldInterface().getIP(channel);
        } catch (RemoteException e) {
            System.out.println("Lost connection to world server " + e);
            reconnectWorld();
            throw new RuntimeException("Lost connection to world server");
        }
    }

    public WorldChannelInterface getWorldInterface() {
        synchronized (worldReady) {
            while (!worldReady) {
                try {
                    worldReady.wait();
                } catch (InterruptedException e) {
                }
            }
        }
        return wci;
    }

    public boolean isShutdown() {
        return shutdown;
    }

    public void shutdown(int time) {
        TimerManager.getInstance().schedule(new ShutdownServer(getChannel(), getWorld()), time);
    }

    public EventScriptManager getEventSM() {
        return eventSM;
    }

    public MapleGuild getGuild(MapleGuildCharacter mgc) {
        int gid = mgc.getGuildId();
        MapleGuild g = null;
        try {
            g = this.getWorldInterface().getGuild(gid, mgc);
        } catch (RemoteException re) {
            System.out.println("RemoteException while fetching MapleGuild. " + re);
            reconnectWorld();
            re.printStackTrace();
            return null;
        }
        if (gsStore.get(gid) == null) {
            gsStore.put(gid, new MapleGuildSummary(g));
        }
        return g;
    }

                public void saveAll() {
        for (MapleCharacter chr : players.getAllCharacters()) {
            chr.saveToDB(true);
        }
    }

    public MapleGuildSummary getGuildSummary(int gid) {
        if (gsStore.containsKey(gid)) {
            return gsStore.get(gid);
        } else {
            try {
                MapleGuild g = this.getWorldInterface().getGuild(gid, null);
                if (g != null) {
                    gsStore.put(gid, new MapleGuildSummary(g));
                }
                return gsStore.get(gid);
            } catch (RemoteException re) {
                System.out.println("RemoteException while fetching GuildSummary. " + re);
                return null;
            }
        }
    }

    public void updateGuildSummary(int gid, MapleGuildSummary mgs) {
        gsStore.put(gid, mgs);
    }

    public void reloadGuildSummary() {
        try {
            MapleGuild g;
            for (int i : gsStore.keySet()) {
                g = this.getWorldInterface().getGuild(i, null);
                if (g != null) {
                    gsStore.put(i, new MapleGuildSummary(g));
                } else {
                    gsStore.remove(i);
                }
            }
        } catch (RemoteException re) {
            System.out.println("RemoteException while reloading GuildSummary." + re);
            this.reconnectWorld();
        }
    }

    public static void main(String args[]) throws FileNotFoundException, IOException, NotBoundException, InstanceAlreadyExistsException, MalformedObjectNameException {
        ServerMode.setServerMode(Mode.CHANNEL);
        Registry registry = LocateRegistry.getRegistry("localhost", Registry.REGISTRY_PORT, new SslRMIClientSocketFactory());
        worldRegistry = (WorldRegistry) registry.lookup("WorldRegistry");
        for (int i = 0; i < 2; i++) { //channel amount -emanuel
            newInstance("release" + (i + 1)).run();
        }
        System.out.println("[INFO] Server loading complete!");
        DatabaseConnection.getConnection();
        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                for (ChannelServer channel : getAllInstances()) {
                    for (MapleCharacter mc : channel.getPlayerStorage().getAllCharacters()) {
                        mc.saveToDB(true);
                        if (mc.getHiredMerchant() != null) {
                            if (mc.getHiredMerchant().isOpen()) {
                                try {
                                    mc.getHiredMerchant().saveItems();
                                } catch (SQLException e) {
                                    System.out.println(mc.getName() + "'s merchant failed to save.");
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }
            }
        });
    }
       public void broadcastGMPacket(MaplePacket data) {
        for (MapleCharacter chr : players.getAllCharacters()) {
            if (chr.isGM()) {
                chr.getClient().getSession().write(data);
            }
        }
    }

    public void yellowWorldMessage(String msg) {
        for (MapleCharacter mc : getPlayerStorage().getAllCharacters()) {
            mc.getClient().getSession().write(MaplePacketCreator.sendYellowTip(msg));
        }
    }

    public void worldMessage(String msg) {
        for (MapleCharacter mc : getPlayerStorage().getAllCharacters()) {
            mc.dropMessage(msg);
        }
    }

    public List<MapleCharacter> getPartyMembers(MapleParty party, int map) {
        List<MapleCharacter> partym = new ArrayList<MapleCharacter>(6);
        for (MaplePartyCharacter partychar : party.getMembers()) {
            if (partychar.getChannel() == getChannel()) {
                if (map > 0 && partychar.getMapid() != map) {
                    continue;
                }
                MapleCharacter chr = getPlayerStorage().getCharacterByName(partychar.getName());
                if (chr != null) {
                    partym.add(chr);
                }
            }
        }
        return partym;
    }

    public class respawnMaps implements Runnable {

        @Override
        public void run() {
            for (Entry<Integer, MapleMap> map : mapFactory.getMaps().entrySet()) {
                map.getValue().respawn();
            }
        }
    }

    public static MapleCharacter getCharacterFromAllServers(int id) {
        for (ChannelServer cserv_ : ChannelServer.getAllInstances()) {
            MapleCharacter ret = cserv_.getPlayerStorage().getCharacterById(id);
            if (ret != null) {
                return ret;
            }
        }
        return null;
    }

    public MapleSquad getMapleSquad(MapleSquadType type) {
        return mapleSquads.get(type);
    }

    public boolean addMapleSquad(MapleSquad squad, MapleSquadType type) {
        if (mapleSquads.get(type) == null) {
            mapleSquads.remove(type);
            mapleSquads.put(type, squad);
            return true;
        } else {
            return false;
        }
    }

    public int getWorld() {
        return world;
    }

    public void reloadEvents() {
        eventSM.cancel();
        eventSM = new EventScriptManager(this, ServerConstants.EVENTS.split(" "));
        eventSM.init();
    }

    public void changeRates(int[] newRates) {
        if (newRates.length != 4) {
            return;
        } else {
            for (int i = 0; i < 4; i++) {
                if (i == 0) {
                    this.EXP_RATE = newRates[i];
                } else if (i == 1) {
                    this.DROP_RATE = newRates[i];
                } else if (i == 2) {
                    this.BOSS_DROP_RATE = newRates[i];
                } else if (i == 3) {
                    this.MESO_RATE = newRates[i];
                }
            }
        }

        for (MapleCharacter c : this.getPlayerStorage().getAllCharacters()) {
            c.setRates(false); // updates the rates
        }

        for (MapleMap m : this.getMapFactory().getMaps().values()) {
            m.updateRates();
        }
    }

    public int getEXPRate() {
        return this.EXP_RATE;
    }

    public int getDropRate() {
        return this.DROP_RATE;
    }

    public int getBossDropRate() {
        return this.BOSS_DROP_RATE;
    }

    public int getMesoRate() {
        return this.MESO_RATE;
    }

    public HiredMerchantRegistry getHMRegistry() {
        return this.HMRegistry;
    }
    
   
//
//    public void weddingMessage(String husband, String wife) {
//        for (ChannelServer cserv : ChannelServer.getAllInstances()) {
//            for (MapleCharacter mc : cserv.getPlayerStorage().getAllCharacters())
//                mc.message(husband+"'s and " + wife + "'s marriage will start in "+ channel +" at the Cathedral.");
//        }
//    }
}

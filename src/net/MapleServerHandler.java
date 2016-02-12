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
package net;

import client.MapleClient;
import constants.ServerConstants;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import net.channel.ChannelServer;
import tools.MapleAESOFB;
import tools.MaplePacketCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.data.input.ByteArrayByteStream;
import tools.data.input.GenericSeekableLittleEndianAccessor;
import tools.data.input.SeekableLittleEndianAccessor;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.write.WriteToClosedSessionException;
import java.util.Date;

public class MapleServerHandler extends IoHandlerAdapter {

    private final static Logger LOG = LoggerFactory.getLogger(MapleServerHandler.class);
    private PacketProcessor processor;
    private int channel = -1;

    public MapleServerHandler(PacketProcessor processor) {
        this.processor = processor;
    }

    public MapleServerHandler(PacketProcessor processor, int channel) {
        this.processor = processor;
        this.channel = channel;
    }

    @Override
    public void messageSent(IoSession session, Object message) throws Exception {
        Runnable r = ((MaplePacket) message).getOnSend();
        if (r != null) {
            r.run();
        }
        super.messageSent(session, message);
    }

    @Override
    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
        if (!(cause instanceof WriteToClosedSessionException) && (session != null)) {
            MapleClient c = (MapleClient) session.getAttribute(MapleClient.CLIENT_KEY);
            if (c != null) {
                try{
                    if(!c.getAccountName().equalsIgnoreCase("null")){
                        System.out.println(c.getAccountName() + " caught an exception: " + cause.toString());
                        //cause.printStackTrace();                enable for more info
                    }
                }
                catch(Exception e){
                    //added by Emanuel
                    System.out.println("Webpage Loaded.\n");
                    System.out.println();
                }
            }
            
        }
        
    }

    @Override
    public void sessionOpened(IoSession session) throws Exception { 
        
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss");
        String time = sdf.format(date);
        //added by emanuel
        System.out.println("\n[" + time + "] IoSession with " + session.getRemoteAddress() + " opened.");
        
        //print online players on console
//        MapleClient c = (MapleClient) session.getAttribute(MapleClient.CLIENT_KEY);
//        String playerStr = "";
//           try {
//               playerStr = c.getChannelServer().getWorldInterface().getAllPlayerNames(c.player.getWorld());
//           } catch (RemoteException e) {
//               c.getChannelServer().reconnectWorld();
//           }
//           int onlinePlayers = playerStr.split(", ").length;
//           System.out.println("Online players: " + onlinePlayers);
//           System.out.println(playerStr);
        
        if (channel > -1) {
            if (ChannelServer.getInstance(channel).isShutdown()) {
                session.close(true);
                return;
            }
        }
        byte key[] = {0x13, 0x00, 0x00, 0x00, 0x08, 0x00, 0x00, 0x00, 0x06, 0x00, 0x00, 0x00, (byte) 0xB4, 0x00, 0x00, 0x00, 0x1B, 0x00, 0x00, 0x00, 0x0F, 0x00, 0x00, 0x00, 0x33, 0x00, 0x00, 0x00, 0x52, 0x00, 0x00, 0x00};
        byte ivRecv[] = {70, 114, 122, 82};
        byte ivSend[] = {82, 48, 120, 115};
        ivRecv[3] = (byte) (Math.random() * 255);
        ivSend[3] = (byte) (Math.random() * 255);
        MapleAESOFB sendCypher = new MapleAESOFB(key, ivSend, (short) (0xFFFF - 83));
        MapleAESOFB recvCypher = new MapleAESOFB(key, ivRecv, (short) 83);
        MapleClient client = new MapleClient(sendCypher, recvCypher, session);
        client.setChannel(channel);
        if (client.hasBannedIP() || client.hasBannedMac())
        session.close();
        session.write(MaplePacketCreator.getHello((short) 83, ivSend, ivRecv, ServerConstants.IS_TEST));
        session.setAttribute(MapleClient.CLIENT_KEY, client);
    }

    @Override
    public void sessionClosed(IoSession session) throws Exception {
        synchronized (session) {
            MapleClient client = (MapleClient) session.getAttribute(MapleClient.CLIENT_KEY);
            if (client != null) {
                client.disconnect();
                session.removeAttribute(MapleClient.CLIENT_KEY);
                client.empty();
            }
        }
        super.sessionClosed(session);
    }

    @Override
    public void messageReceived(IoSession session, Object message) throws Exception {
        byte[] content = (byte[]) message;
        SeekableLittleEndianAccessor slea = new GenericSeekableLittleEndianAccessor(new ByteArrayByteStream(content));
        short packetId = slea.readShort();
        MapleClient client = (MapleClient) session.getAttribute(MapleClient.CLIENT_KEY);
        MaplePacketHandler packetHandler = processor.getHandler(packetId);
        if (packetHandler != null && packetHandler.validateState(client)) {
            try {
                packetHandler.handlePacket(slea, client);
            } catch (Throwable t) {
            }
        } else if (packetHandler == null && ServerConstants.DEBUG) {
            System.out.println("--ERROR--");
            System.out.println(slea);
        }
    }

    /*@Override
    public void sessionIdle(final IoSession session, final IdleStatus status) throws Exception {
    MapleClient client = (MapleClient) session.getAttribute(MapleClient.CLIENT_KEY);
    if ((client != null) && (client.getLoginState() == MapleClient.LOGIN_LOGGEDIN)) {
    client.sendPing();
    }
    super.sessionIdle(session, status);
    }*/
    @Override
    public void sessionIdle(final IoSession session, final IdleStatus status) throws Exception {
        MapleClient client = (MapleClient) session.getAttribute(MapleClient.CLIENT_KEY);

        if (client != null && client.getPlayer() != null && LOG.isTraceEnabled()) {
            LOG.trace("Player {} went idle", client.getPlayer().getName());
        }

        if (client != null) {
            client.sendPing();
        }
        super.sessionIdle(session, status);
    }
}

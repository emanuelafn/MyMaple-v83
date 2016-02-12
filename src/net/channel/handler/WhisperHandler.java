package net.channel.handler;

import client.MapleCharacter;
import client.MapleClient;
import client.command.CommandProcessor;
import client.command.IllegalCommandSyntaxException;
import java.rmi.RemoteException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.AbstractMaplePacketHandler;
import tools.DatabaseConnection;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

/**
 *
 * @author Matze
 */
public final class WhisperHandler extends AbstractMaplePacketHandler {

    public final void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        c.getPlayer().resetAfkTime();
        byte mode = slea.readByte();
        if (mode == 6) { // whisper
            String recipient = slea.readMapleAsciiString();
            String text = slea.readMapleAsciiString();
            try {
                try {
                    if (!CommandProcessor.processCommand(c, text)) {
                        if (!c.getPlayer().getCheatTracker().Spam(500, 4)) {
                            MapleCharacter player = c.getChannelServer().getPlayerStorage().getCharacterByName(recipient);
                            if (player != null) {
                                player.getClient().getSession().write(MaplePacketCreator.getWhisper(c.getPlayer().getName(), c.getChannel(), text));
                                c.getSession().write(MaplePacketCreator.getWhisperReply(recipient, (byte) 1));
                            } else // not found
                            {
                                try {
                                    if (c.getChannelServer().getWorldInterface().isConnected(recipient)) {
                                        c.getChannelServer().getWorldInterface().whisper(c.getPlayer().getName(), recipient, c.getChannel(), text);
                                        c.getSession().write(MaplePacketCreator.getWhisperReply(recipient, (byte) 1));
                                    } else {
                                        c.getSession().write(MaplePacketCreator.getWhisperReply(recipient, (byte) 0));
                                    }
                                } catch (RemoteException e) {
                                    c.getSession().write(MaplePacketCreator.getWhisperReply(recipient, (byte) 0));
                                    c.getChannelServer().reconnectWorld();
                                }
                            }
                        }
                    }
                } catch (RemoteException ex) {
                    Logger.getLogger(WhisperHandler.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IllegalCommandSyntaxException ex) {
                    Logger.getLogger(WhisperHandler.class.getName()).log(Level.SEVERE, null, ex);
                }
            } catch (SQLException ex) {
                Logger.getLogger(WhisperHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else if (mode == 5) { // - /find
            String recipient = slea.readMapleAsciiString();
            MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(recipient);
            if (victim != null && c.getPlayer().gmLevel() >= victim.gmLevel()) {
                if (victim.inCS()) {
                    c.getSession().write(MaplePacketCreator.getFindReplyWithCSorMTS(victim.getName(), true));
                } else if (victim.inMTS()) {
                    c.getSession().write(MaplePacketCreator.getFindReplyWithCSorMTS(victim.getName(), false));
                } else {
                    c.getSession().write(MaplePacketCreator.getFindReplyWithMap(victim.getName(), victim.getMap().getId()));
                }
            } else // not found
            {
                try {
                    PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT gm FROM characters WHERE name = ?");
                    ps.setString(1, recipient);
                    ResultSet rs = ps.executeQuery();
                    int gm = 0;
                    if (rs.next()) {
                        gm = rs.getInt("gm");
                    }
                    rs.close();
                    ps.close();
                    int channel = c.getChannelServer().getWorldInterface().find(recipient);
                    if (channel > -1 && gm == 0) {
                        c.getSession().write(MaplePacketCreator.getFindReply(recipient, channel));
                    } else {
                        c.getSession().write(MaplePacketCreator.getWhisperReply(recipient, (byte) 0));
                    }
                } catch (Exception e) {
                    c.getChannelServer().reconnectWorld();
                }
            }
        }
    }
}
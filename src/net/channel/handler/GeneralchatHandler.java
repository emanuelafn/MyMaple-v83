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

import client.command.CommandProcessor;
import client.command.IllegalCommandSyntaxException;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;
import client.MapleClient;
import java.text.SimpleDateFormat;
import java.util.Date;

public final class GeneralchatHandler extends net.AbstractMaplePacketHandler {

    public final void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {

        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss");
        String time = sdf.format(date);
        
        String text = slea.readMapleAsciiString();
        
        if(constants.ServerConstants.isViewable == true){
        System.out.println("[" + time + "] " + c.getPlayer() +": " + text); //[10:53:24] Emanuel: Hello World!
        }
        
        try {
            try {
                if (!CommandProcessor.processCommand(c, text) && c.getPlayer().getCanTalk()) {
                    if (c.getPlayer().isDonor()) {
                        c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.sendYellowTip("[Donor]" + c.getPlayer().getName() + ": " + text));
                        c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.getChatText(c.getPlayer().getId(), text, false, 1));
                    } else {
                        c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.getChatText(c.getPlayer().getId(), text, c.getPlayer().getGMChat(), slea.readByte()));
                    }
                }
            } catch (RemoteException ex) {
                Logger.getLogger(GeneralchatHandler.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalCommandSyntaxException ex) {
                Logger.getLogger(GeneralchatHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (SQLException ex) {
            Logger.getLogger(GeneralchatHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
        }
    }
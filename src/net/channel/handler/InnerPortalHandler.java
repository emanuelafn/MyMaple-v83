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

import client.MapleClient;
import java.awt.Point;
import net.AbstractMaplePacketHandler;
import server.MaplePortal;
import tools.data.input.SeekableLittleEndianAccessor;

/**
 *
 * @author BubblesDev
 */
public final class InnerPortalHandler extends AbstractMaplePacketHandler {

    public final void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        slea.readByte();
        MaplePortal portal = c.getPlayer().getMap().getPortal(slea.readMapleAsciiString());
        if (portal == null || portal.getPosition().distanceSq(c.getPlayer().getPosition()) > 22500) {
            return;
        }
        c.getPlayer().getMap().movePlayer(c.getPlayer(), new Point(slea.readShort(), slea.readShort()));
    }
}

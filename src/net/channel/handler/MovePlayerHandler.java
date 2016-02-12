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

import java.util.List;
import client.MapleClient;
import server.maps.MapleMap;
import server.movement.LifeMovementFragment;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public final class MovePlayerHandler extends AbstractMovementPacketHandler {

    public final void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        c.getPlayer().resetAfkTime();
        slea.skip(9);
        final List<LifeMovementFragment> res = parseMovement(slea);
        if (res != null && res.size() > 0) {
            if (c.getPlayer().isHidden()) {
                c.getPlayer().getMap().broadcastGMMessage(c.getPlayer(), MaplePacketCreator.movePlayer(c.getPlayer().getId(), res), false);
            } else {
                c.getPlayer().getMap().broadcastMessage(c.getPlayer(), MaplePacketCreator.movePlayer(c.getPlayer().getId(), res), false);
            }
            final MapleMap map = c.getPlayer().getMap();
            updatePosition(res, c.getPlayer(), 0);
            map.movePlayer(c.getPlayer(), c.getPlayer().getPosition());
            int count = c.getPlayer().getFallCounter(); // from vana
            if (map.getFootholds().findBelow(c.getPlayer().getPosition()) == null) {
                if (count > 3) {
                    c.getPlayer().changeMap(map, map.getPortal(0));
                } else {
                    c.getPlayer().setFallCounter(++count);
                }
            } else if (count > 0) {
                c.getPlayer().setFallCounter(0);
            }
        }
    }
}

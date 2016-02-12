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
import net.AbstractMaplePacketHandler;
import server.maps.FieldLimit;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

/**
 *
 * @author Matze
 */
public final class TrockAddMapHandler extends AbstractMaplePacketHandler {

    public final void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        byte action = slea.readByte();
        byte rocktype = slea.readByte();
        if (rocktype != 0 && rocktype != 1) {
            return;
        }
        int mapId;
        if (action == 0) {
            mapId = slea.readInt();
            c.getPlayer().deleteTeleportRockMap(Integer.valueOf(mapId), rocktype);
        } else if (action == 1) {
            mapId = c.getPlayer().getMapId();
            if (FieldLimit.CANNOTVIPROCK.check(c.getPlayer().getMap().getFieldLimit())) {
                c.getPlayer().addTeleportRockMap(Integer.valueOf(mapId), rocktype);
            } else {
                c.getPlayer().dropMessage(1, "You may not save this map.");
            }
        }
        c.getSession().write(MaplePacketCreator.refreshTeleportRockMapList(c.getPlayer(), rocktype));
    }
}

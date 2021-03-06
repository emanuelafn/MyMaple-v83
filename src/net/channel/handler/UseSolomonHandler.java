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

import client.IItem;
import client.MapleClient;
import client.MapleInventoryType;
import net.AbstractMaplePacketHandler;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

/**
 *
 * @author XoticStory
 */
public final class UseSolomonHandler extends AbstractMaplePacketHandler {

    public final void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        slea.skip(4);
        byte slot = (byte) slea.readShort();
        int itemId = slea.readInt();
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        IItem slotItem = c.getPlayer().getInventory(MapleInventoryType.USE).getItem(slot);
        if (slotItem.getQuantity() < 1 || slotItem.getItemId() != itemId || c.getPlayer().getLevel() > ii.getMaxLevelById(itemId)) {
            return;
        }
        c.getPlayer().gainExp(ii.getExpById(itemId), true, true);
        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
        c.getSession().write(MaplePacketCreator.enableActions());
    }
}

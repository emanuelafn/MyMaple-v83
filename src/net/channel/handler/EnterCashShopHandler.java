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

import java.rmi.RemoteException;
import client.MapleClient;
import net.AbstractMaplePacketHandler;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

/**
 *
 * @author Acrylic
 */
public final class EnterCashShopHandler extends AbstractMaplePacketHandler {

    public final void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        if (!c.getPlayer().isAlive()) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        if (c.getPlayer().getNoPets() > 0) {
            c.getPlayer().unequipAllPets();
        }
        try {
            c.getChannelServer().getWorldInterface().addBuffsToStorage(c.getPlayer().getId(), c.getPlayer().getAllBuffs());
        } catch (RemoteException e) {
            c.getChannelServer().reconnectWorld();
        }
        c.getSession().write(MaplePacketCreator.updateGender(c.getPlayer()));
        c.getPlayer().cancelAllBuffs();
        if (c.getPlayer().getTrade() != null) {
            c.getPlayer().getTrade().cancelTrade(c.getPlayer());
        }
        c.getPlayer().getMap().removePlayer(c.getPlayer());
        //c.getSession().write(MaplePacketCreator.warpCS(c, false));
        c.getSession().write(MaplePacketCreator.openCashShop(c));
        c.getPlayer().setInCS(true);
        c.getSession().write(MaplePacketCreator.enableCSUse0());
        c.getSession().write(MaplePacketCreator.enableCSUse1());
        c.getSession().write(MaplePacketCreator.enableCSUse2());
        c.getSession().write(MaplePacketCreator.enableCSUse3());
        c.getSession().write(MaplePacketCreator.showNXMapleTokens(c.getPlayer()));
        c.getSession().write(MaplePacketCreator.showCashInventoryDummy(c));
        c.getSession().write(MaplePacketCreator.sendWishList(c.getPlayer(), false));
        c.getPlayer().saveToDB(true);
    }
}

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

import client.MapleCharacter;
import net.world.MaplePartyCharacter;
import client.MapleClient;
import java.util.Arrays;
import net.AbstractMaplePacketHandler;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.maps.MapleMapItem;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;
import client.anticheat.CheatingOffense;

/**
 *
 * @author Matze
 */
public final class ItemPickupHandler extends AbstractMaplePacketHandler {

    public final void handlePacket(final SeekableLittleEndianAccessor slea, final MapleClient c) {
        c.getPlayer().resetAfkTime();
        slea.skip(9);
        int oid = slea.readInt();
        MapleMapObject ob = c.getPlayer().getMap().getMapObject(oid);
        if (c.getPlayer().getInventory(MapleItemInformationProvider.getInstance().getInventoryType(ob.getObjectId())).getNextFreeSlot() > -1) {
            if (c.getPlayer().getMapId() > 209000000 && c.getPlayer().getMapId() < 209000016) {//happyville trees
                MapleMapItem mapitem = (MapleMapItem) ob;
                if (mapitem.getDropperId() == c.getPlayer().getObjectId()) {
                    if (MapleInventoryManipulator.addFromDrop(c, mapitem.getItem(), false)) {
                        c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.removeItemFromMap(mapitem.getObjectId(), 2, c.getPlayer().getId()), mapitem.getPosition());
                        c.getPlayer().getMap().removeMapObject(ob);
                    } else {
                        return;
                    }
                    mapitem.setPickedUp(true);
                } else {
                    c.getSession().write(MaplePacketCreator.getInventoryFull());
                    c.getSession().write(MaplePacketCreator.getShowInventoryFull());
                    return;
                }
                return;
            }
            if (ob == null) {
                c.getSession().write(MaplePacketCreator.getInventoryFull());
                c.getSession().write(MaplePacketCreator.getShowInventoryFull());
                return;
            }
            if (ob instanceof MapleMapItem) {
                MapleMapItem mapitem = (MapleMapItem) ob;
                double distance = c.getPlayer().getPosition().distanceSq(mapitem.getPosition());
                //c.getPlayer().getCheatTracker().checkPickupAgain();
                if (distance > 90000.0) { // 300^2, 550 is approximatly the range of ultis
                    // AutobanManager.getInstance().addPoints(c, 100, 300000, "Itemvac");
                    c.getPlayer().getCheatTracker().registerOffense(CheatingOffense.ITEMVAC);
                    // Double.valueOf(Math.sqrt(distance))
                } else if (distance > 30000.0) {
                    // log.warn("[h4x] Player {} is picking up an item that's fairly far away: {}", c.getPlayer().getName(), Double.valueOf(Math.sqrt(distance)));
                    c.getPlayer().getCheatTracker().registerOffense(CheatingOffense.SHORT_ITEMVAC);
                }
                synchronized (mapitem) {
                    if (mapitem.isPickedUp()) {
                        c.getSession().write(MaplePacketCreator.getInventoryFull());
                        c.getSession().write(MaplePacketCreator.getShowInventoryFull());
                        return;
                    }
                    if (mapitem.getMeso() > 0) {
                        if (c.getPlayer().getParty() != null) {
                            int mesosamm = mapitem.getMeso();
                            if (mesosamm > 50000 * c.getPlayer().getMesoRate()) {
                                return;
                            }
                            int partynum = 0;
                            for (MaplePartyCharacter partymem : c.getPlayer().getParty().getMembers()) {
                                if (partymem.isOnline() && partymem.getMapid() == c.getPlayer().getMap().getId() && partymem.getChannel() == c.getChannel()) {
                                    partynum++;
                                }
                            }
                            for (MaplePartyCharacter partymem : c.getPlayer().getParty().getMembers()) {
                                if (partymem.isOnline() && partymem.getMapid() == c.getPlayer().getMap().getId()) {
                                    MapleCharacter somecharacter = c.getChannelServer().getPlayerStorage().getCharacterById(partymem.getId());
                                    if (somecharacter != null) {
                                        somecharacter.gainMeso(mesosamm / partynum, true, true, false);
                                    }
                                }
                            }
                        } else {
                            c.getPlayer().gainMeso(mapitem.getMeso(), true, true, false);
                        }
                        c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.removeItemFromMap(mapitem.getObjectId(), 2, c.getPlayer().getId()), mapitem.getPosition());
                        c.getPlayer().getMap().removeMapObject(ob);
                    } else if (useItem(c, mapitem.getItem().getItemId())) {
                        if (mapitem.getItem().getItemId() / 10000 == 238) {
                            c.getPlayer().getMonsterBook().addCard(c, mapitem.getItem().getItemId());
                        }
                        mapitem.setPickedUp(true);
                        c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.removeItemFromMap(mapitem.getObjectId(), 2, c.getPlayer().getId()), mapitem.getPosition());
                        c.getPlayer().getMap().removeMapObject(ob);
                    } else if (MapleInventoryManipulator.addFromDrop(c, mapitem.getItem(), true)) {
                        c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.removeItemFromMap(mapitem.getObjectId(), 2, c.getPlayer().getId()), mapitem.getPosition());
                        c.getPlayer().getMap().removeMapObject(ob);
                    } else if (mapitem.getItem().getItemId() == 4031868) {
                        c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.updateAriantPQRanking(c.getPlayer().getName(), c.getPlayer().getItemQuantity(4031868, false), false));
                        c.getPlayer().getMap().removeMapObject(ob);
                    } else {
                        return;
                    }
                    mapitem.setPickedUp(true);
                }
            }
            c.getSession().write(MaplePacketCreator.enableActions());
        }
    }

    static final boolean useItem(final MapleClient c, final int id) {
        if (id / 1000000 == 2) {
            MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            if (ii.isConsumeOnPickup(id)) {
                if (id >= 2022430 && id < 2022434) {
                    if (c.getPlayer().getParty() != null) {
                        for (MapleCharacter mc : c.getChannelServer().getPartyMembers(c.getPlayer().getParty(), -1)) {
                            if (mc.getMap().getId() >= 925020000 && mc.getMap().getId() <= 925030000) {
                                ii.getItemEffect(id).applyTo(mc);
                            }
                        }
                    } else {
                        ii.getItemEffect(id).applyTo(c.getPlayer());
                    }
                } else {
                    if (c.getPlayer().getMap().getId() >= 925020000 && c.getPlayer().getMap().getId() < 925030000) {
                        MapleCharacter player = c.getPlayer();
                        for (MapleMapObject i : player.getMap().getMapObjectsInRange(player.getPosition(), Double.POSITIVE_INFINITY, Arrays.asList(MapleMapObjectType.ITEM))) {
                            MapleMapItem item = (MapleMapItem) i;
                            if (!(item.getItem().getItemId() >= 2022430 && item.getItem().getItemId() < 2022434)) {
                                player.getMap().removeMapObject(i);
                                player.getMap().broadcastMessage(MaplePacketCreator.removeItemFromMap(i.getObjectId(), 0, player.getId()));
                            }
                        }
                    }
                    ii.getItemEffect(id).applyTo(c.getPlayer());
                }
                return true;
            }
        }
        return false;
    }
}

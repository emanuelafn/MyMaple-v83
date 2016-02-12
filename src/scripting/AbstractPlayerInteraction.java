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
package scripting;

import java.util.Arrays;
import java.util.List;
import client.Equip;
import client.IItem;
import client.MapleCharacter;
import client.MapleClient;
import client.MapleInventory;
import client.MapleInventoryType;
import client.MaplePet;
import client.MapleQuestStatus;
import constants.InventoryConstants;
import java.rmi.RemoteException;
import net.world.MapleParty;
import net.world.MaplePartyCharacter;
import net.world.guild.MapleGuild;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.maps.MapleMap;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import server.quest.MapleQuest;
import tools.MaplePacketCreator;

public class AbstractPlayerInteraction {

    public MapleClient c;

    public AbstractPlayerInteraction(MapleClient c) {
        this.c = c;
    }

    public MapleClient getClient() {
        return c;
    }

    public MapleCharacter getPlayer() {
        return c.getPlayer();
    }

    public void warp(int map) {
        getPlayer().changeMap(getWarpMap(map), getWarpMap(map).getPortal(0));
    }

    public void warp(int map, int portal) {
        getPlayer().changeMap(getWarpMap(map), getWarpMap(map).getPortal(portal));
    }

    public void warp(int map, String portal) {
        getPlayer().changeMap(getWarpMap(map), getWarpMap(map).getPortal(portal));
    }

    public void warpMap(int map) {
        for (MapleCharacter mc : getPlayer().getMap().getCharacters()) {
            mc.changeMap(getWarpMap(map), getWarpMap(map).getPortal(0));
        }
    }

    protected MapleMap getWarpMap(int map) {
        MapleMap target;
        if (getPlayer().getEventInstance() == null) {
            target = c.getChannelServer().getMapFactory().getMap(map);
        } else {
            target = getPlayer().getEventInstance().getMapInstance(map);
        }
        return target;
    }

    public MapleMap getMap(int map) {
        return getWarpMap(map);
    }

    public boolean haveItem(int itemid) {
        return haveItem(itemid, 1);
    }

    public boolean haveItem(int itemid, int quantity) {
        return c.getPlayer().getItemQuantity(itemid, false) >= quantity;
    }

    public boolean canHold(int itemid) {
        return c.getPlayer().getInventory(MapleItemInformationProvider.getInstance().getInventoryType(itemid)).getNextFreeSlot() > -1;
    }

    public MapleQuestStatus.Status getQuestStatus(int id) {
        return c.getPlayer().getQuest(MapleQuest.getInstance(id)).getStatus();
    }

    public void gainItem(int id, short quantity) {
        gainItem(id, quantity, false);
    }

    public void gainItem(int id) {
        gainItem(id, (short) 1, false);
    }

    public void gainItem(int id, short quantity, boolean randomStats) {
        if (id >= 5000000 && id <= 5000100) {
            MapleInventoryManipulator.addById(c, id, (short) 1, null, MaplePet.createPet(id));
        }
        if (quantity >= 0) {
            MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            IItem item = ii.getEquipById(id);
            if (!MapleInventoryManipulator.checkSpace(c, id, quantity, "")) {
                c.getPlayer().dropMessage(1, "Your inventory is full. Please remove an item from your " + ii.getInventoryType(id).name() + " inventory.");
                return;
            }
            if (ii.getInventoryType(id).equals(MapleInventoryType.EQUIP) && !InventoryConstants.isRechargable(item.getItemId())) {
                if (randomStats) {
                    MapleInventoryManipulator.addFromDrop(c, ii.randomizeStats((Equip) item), false);
                } else {
                    MapleInventoryManipulator.addFromDrop(c, (Equip) item, false);
                }
            } else {
                MapleInventoryManipulator.addById(c, id, quantity);
            }
        } else {
            MapleInventoryManipulator.removeById(c, MapleItemInformationProvider.getInstance().getInventoryType(id), id, -quantity, true, false);
        }
        c.getSession().write(MaplePacketCreator.getShowItemGain(id, quantity, true));
    }


    public void changeMusic(String songName) {
        getPlayer().getMap().broadcastMessage(MaplePacketCreator.musicChange(songName));
    }

    public void playerMessage(int type, String message) {
        c.getSession().write(MaplePacketCreator.serverNotice(type, message));
    }

    public void mapMessage(int type, String message) {
        getPlayer().getMap().broadcastMessage(MaplePacketCreator.serverNotice(type, message));
    }

    public void guildMessage(int type, String message) {
        if (getGuild() != null) {
            getGuild().guildMessage(MaplePacketCreator.serverNotice(type, message));
        }
    }

    public MapleGuild getGuild() {
        try {
            return c.getChannelServer().getWorldInterface().getGuild(getPlayer().getGuildId(), null);
        } catch (RemoteException e) {
            c.getChannelServer().reconnectWorld();
        }
        return null;
    }

    public MapleParty getParty() {
        return getPlayer().getParty();
    }

    public boolean isLeader() {
        if (c.getPlayer().getParty() != null) {
            return getParty().getLeader().equals(new MaplePartyCharacter(c.getPlayer()));
        }
        return false;
    }

    public void givePartyItems(int id, short quantity, List<MapleCharacter> party) {
        for (MapleCharacter chr : party) {
            MapleClient cl = chr.getClient();
            if (quantity >= 0) {
                MapleInventoryManipulator.addById(cl, id, quantity);
            } else {
                MapleInventoryManipulator.removeById(cl, MapleItemInformationProvider.getInstance().getInventoryType(id), id, -quantity, true, false);
            }
            cl.getSession().write(MaplePacketCreator.getShowItemGain(id, quantity, true));
        }
    }

    public void givePartyExp(int amount, List<MapleCharacter> party) {
        for (MapleCharacter chr : party) {
            chr.gainExp((int) (amount * chr.getExpRate()), true, true);
        }
    }

    public void removeFromParty(int id, List<MapleCharacter> party) {
        for (MapleCharacter chr : party) {
            MapleClient cl = chr.getClient();
            MapleInventoryType type = MapleItemInformationProvider.getInstance().getInventoryType(id);
            MapleInventory iv = cl.getPlayer().getInventory(type);
            int possesed = iv.countById(id);
            if (possesed > 0) {
                MapleInventoryManipulator.removeById(c, MapleItemInformationProvider.getInstance().getInventoryType(id), id, possesed, true, false);
                cl.getSession().write(MaplePacketCreator.getShowItemGain(id, (short) -possesed, true));
            }
        }
    }

    public void removeAll(int id) {
        removeAll(id, c);
    }

    public void removeAll(int id, MapleClient cl) {
        int possessed = cl.getPlayer().getInventory(MapleItemInformationProvider.getInstance().getInventoryType(id)).countById(id);
        if (possessed > 0) {
            MapleInventoryManipulator.removeById(cl, MapleItemInformationProvider.getInstance().getInventoryType(id), id, possessed, true, false);
            cl.getSession().write(MaplePacketCreator.getShowItemGain(id, (short) -possessed, true));
        }
    }

    public int getMapId() {
        return c.getPlayer().getMap().getId();
    }

    public MapleMap getMap() {
        return c.getPlayer().getMap();
    }

    public int getPlayerCount(int mapid) {
        return c.getChannelServer().getMapFactory().getMap(mapid).getCharacters().size();
    }

    public void showInstruction(String msg, int width, int height) {
        c.getSession().write(MaplePacketCreator.sendHint(msg, width, height));
        c.getSession().write(MaplePacketCreator.enableActions());
    }

    public void resetMap(int mapid) {
        getMap(mapid).resetReactors();
        getMap(mapid).killAllMonsters();
        for (MapleMapObject i : getMap(mapid).getMapObjectsInRange(c.getPlayer().getPosition(), Double.POSITIVE_INFINITY, Arrays.asList(MapleMapObjectType.ITEM))) {
            getMap(mapid).removeMapObject(i);
            getMap(mapid).broadcastMessage(MaplePacketCreator.removeItemFromMap(i.getObjectId(), 0, c.getPlayer().getId()));
        }
    }

    public void sendClock(MapleClient d, int time) {
        d.getSession().write(MaplePacketCreator.getClock((int) (time - System.currentTimeMillis()) / 1000));
    }

    public void useItem(int id) {
        MapleItemInformationProvider.getInstance().getItemEffect(id).applyTo(c.getPlayer());
        c.getSession().write(MaplePacketCreator.getStatusMsg(id));
    }
}

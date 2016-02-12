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
package server.maps;

import client.MapleCharacter;
import java.awt.Point;
import client.MapleClient;
import net.world.MaplePartyCharacter;
import scripting.portal.PortalScriptManager;
import server.MaplePortal;
import tools.MaplePacketCreator;
import client.anticheat.CheatingOffense;

public class MapleGenericPortal implements MaplePortal {

    private String name;
    private String target;
    private Point position;
    private int targetmap;
    private int type;
    private boolean status = true;
    private int id;
    private String scriptName;
    private boolean portalState;

    public MapleGenericPortal(int type) {
        this.type = type;
    }

    @Override
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Point getPosition() {
        return position;
    }

    @Override
    public String getTarget() {
        return target;
    }

    @Override
    public void setPortalStatus(boolean newStatus) {
        this.status = newStatus;
    }

    @Override
    public boolean getPortalStatus() {
        return status;
    }

    @Override
    public int getTargetMapId() {
        return targetmap;
    }

    @Override
    public int getType() {
        return type;
    }

    @Override
    public String getScriptName() {
        return scriptName;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPosition(Point position) {
        this.position = position;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public void setTargetMapId(int targetmapid) {
        this.targetmap = targetmapid;
    }

    @Override
    public void setScriptName(String scriptName) {
        this.scriptName = scriptName;
    }

    @Override
    public void enterPortal(MapleClient c) {
        boolean changed = false;
        MapleCharacter player = c.getPlayer();
        double distanceSq = getPosition().distanceSq(player.getPosition());
        if (distanceSq > 22500) {
            player.getCheatTracker().registerOffense(CheatingOffense.USING_FARAWAY_PORTAL, "D" + Math.sqrt(distanceSq));
        }
        if (getScriptName() != null) {
            if (!handlePortal(getScriptName(), c.getPlayer())) {
                changed = PortalScriptManager.getInstance().executePortalScript(this, c);
            }
        } else if (getTargetMapId() != 999999999) {
            MapleMap to = c.getPlayer().getEventInstance() == null ? c.getChannelServer().getMapFactory().getMap(getTargetMapId()) : c.getPlayer().getEventInstance().getMapInstance(getTargetMapId());
            MaplePortal pto = to.getPortal(getTarget());
            if (pto == null) {// fallback for missing portals - no real life case anymore - intresting for not implemented areas
                pto = to.getPortal(0);
            }
            c.getPlayer().changeMap(to, pto); //late resolving makes this harder but prevents us from loading the whole world at once
            changed = true;
        }
        if (!changed) {
            c.getSession().write(MaplePacketCreator.enableActions());
        }
    }

    public void setPortalState(boolean state) {
        this.portalState = state;
    }

    public boolean getPortalState() {
        return portalState;
    }

    public static boolean handlePortal(String name, MapleCharacter c) {
        boolean partyCheck = true;
        if (c.getParty() != null) {
            for (MaplePartyCharacter mpc : c.getParty().getMembers()) {
                if (mpc.getJobId() % 100 != 2 || mpc.getJobId() / 100 != 1) {
                    partyCheck = false;
                    break;
                }
            }
        } else {
            return false;
        }
        if (name.equals("s4rush")) {
            if (c.getParty().getLeader().getId() != c.getId()) {
                if (!partyCheck) {
                    c.dropMessage("You step into the portal, but it swiftly kicks you out.");
                } else {
                    c.dropMessage("You're not the party leader.");
                }
                c.getClient().getSession().write(MaplePacketCreator.enableActions());
                return true;
            }
            if (!partyCheck) {
                c.dropMessage("Someone in your party is not a 4th Job warrior.");
                c.getClient().getSession().write(MaplePacketCreator.enableActions());
                return true;
            }
            c.getClient().getChannelServer().getEventSM().getEventManager("4jrush").startInstance(c.getParty(), c.getMap());
            return true;
        } else if (name.equals("s4berserk")) {
            if (!c.haveItem(4031475)) {
                c.dropMessage("The portal to the Forgotten Shrine is locked");
                c.getClient().getSession().write(MaplePacketCreator.enableActions());
                return true;
            }
            c.getClient().getChannelServer().getEventSM().getEventManager("4jberserk").startInstance(c.getParty(), c.getMap());
            return true;
        }
        return false;
    }
}

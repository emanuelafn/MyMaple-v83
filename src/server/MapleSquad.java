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
package server;

import client.MapleCharacter;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import net.channel.ChannelServer;
import server.maps.MapleMap;

public final class MapleSquad {

    private MapleMap map;
    private String leader;
    private List<MapleCharacter> members = new LinkedList<MapleCharacter>();
    private List<MapleCharacter> bannedMembers = new LinkedList<MapleCharacter>();
    private boolean closed = false;
    private ScheduledFuture<?> timer;

    public MapleSquad(MapleMap mm, String name) {
        map = mm;
        leader = name;
        long timeOut = 5 * 60 * 1000;

        timer = TimerManager.getInstance().register(new Runnable() {

            public void run() {
                timer.cancel(false);
                MapleCharacter target = findLeader();
                if (leader.equals(map.getMapleSquad().getLeader()) && map != target.getMap()) {
                    map.deleteMapleSquad();

                    if (target != null) {
                        target.dropMessage(5, "Your squad has timed out.");
                    }
                }
            }
        }, timeOut, timeOut);
    }

    public String getLeader() {
        return leader;
    }

    public MapleCharacter findLeader() {
        MapleCharacter target = null;
        for (ChannelServer cs : ChannelServer.getAllInstances()) {
            target = cs.getPlayerStorage().getCharacterByName(leader);
            if (target != null) {
                return target;
            }
        }
        return null;
    }

    public List<MapleCharacter> getMembers() {
        return members;
    }

    public boolean addMember(MapleCharacter mc) {
        if (bannedMembers.contains(mc)) {
            return false;
        }
        if (!members.contains(mc)) {
            members.add(mc);
        }
        return true;
    }

    public void removeMember(MapleCharacter mc) {
        members.remove(mc);
    }

    public List<MapleCharacter> getBannedMembers() {
        return bannedMembers;
    }

    public void banMember(MapleCharacter mc) {
        if (!bannedMembers.contains(mc)) {
            bannedMembers.add(mc);
        }
        members.remove(mc);
    }

    public boolean isClosed() {
        return closed;
    }

    public void close(boolean b) {
        closed = b;
    }

    public ScheduledFuture<?> getTimer() {
        return timer;
    }
}

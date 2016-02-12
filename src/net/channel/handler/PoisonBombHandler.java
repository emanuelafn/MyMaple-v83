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
import client.SkillFactory;
import constants.skills.NightWalker;
import java.awt.Point;
import java.awt.Rectangle;
import net.AbstractMaplePacketHandler;
import server.maps.MapleMist;
import tools.data.input.SeekableLittleEndianAccessor;

/**
 *
 * @author BubblesDev v75 (Moogra)
 */
public final class PoisonBombHandler extends AbstractMaplePacketHandler {

    public final void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        int x = slea.readInt();
        int y = slea.readInt();
        int chargetime = slea.readShort(); // time in millis, you can actually read int
        slea.skip(2); // 0
        if (slea.readInt() != NightWalker.POISON_BOMB) {// this is also for pirate grenade or something, but that doesn't have mist?
            return;
        }
        int left = c.getPlayer().isFacingLeft() ? -1 : 1;
        int level = c.getPlayer().getSkillLevel(SkillFactory.getSkill(NightWalker.POISON_BOMB));
        Point newp = null;
        // equation is something like -x^2/360+x for fully charged projectile, but i'm not doing rectline thing again.
        try {
            newp = c.getPlayer().getMap().getGroundBelow(new Point(x + left * chargetime / 3, y - 30));
        } catch (NullPointerException e) {
            newp = c.getPlayer().getPosition(); // if it's a wall, too lazy to do the wall checks
        }
        c.getPlayer().getMap().spawnMist(new MapleMist(calculateBoundingBox(newp), c.getPlayer(), SkillFactory.getSkill(NightWalker.POISON_BOMB).getEffect(level)), (int) (4 * (Math.ceil(level / 3))) * 1000, true, false);
    }

    private Rectangle calculateBoundingBox(Point posFrom) {
        Point mylt = new Point(-100 + posFrom.x, -82 + posFrom.y);
        Point myrb = new Point(100 + posFrom.x, 83 + posFrom.y);
        return new Rectangle(mylt.x, mylt.y, myrb.x - mylt.x, myrb.y - mylt.y);
    }
}

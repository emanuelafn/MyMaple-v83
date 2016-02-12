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
import constants.skills.Magician;
import net.AbstractMaplePacketHandler;
import tools.data.input.SeekableLittleEndianAccessor;
import client.anticheat.CheatingOffense;

public final class HealOvertimeHandler extends AbstractMaplePacketHandler {

    public final void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        if (c.getPlayer().getLastHealed() < 2000) {
            return;
        }
        c.getPlayer().setLastHealed(System.currentTimeMillis());
        slea.readInt();
        short healHP = slea.readShort();
        if (healHP != 0) {
            if (healHP > 140) {
                c.getPlayer().getCheatTracker().registerOffense(CheatingOffense.REGEN_HIGH_HP, String.valueOf(healHP));

            }
            c.getPlayer().addHP(healHP);
            c.getPlayer().checkBerserk();
        }
        short healMP = slea.readShort();
        int skill = c.getPlayer().getSkillLevel(SkillFactory.getSkill(Magician.IMPROVED_MP_RECOVERY));
        if (healMP > Math.floor(c.getPlayer().getMaxMp() * 0.02)) {
            c.getPlayer().getCheatTracker().registerOffense(CheatingOffense.REGEN_HIGH_MP, String.valueOf(healMP));
        }
        if (healMP != 0 && 2 * healMP <= ((skill != 0 ? skill : 16) * c.getPlayer().getLevel() / 10 + 3) * 3 + 50) {
            c.getPlayer().addMP(healMP);
        }
    }
}

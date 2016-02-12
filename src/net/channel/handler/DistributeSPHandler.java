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

import client.ISkill;
import client.MapleCharacter;
import client.MapleClient;
import client.MapleStat;
import client.SkillFactory;
import net.AbstractMaplePacketHandler;
import tools.data.input.SeekableLittleEndianAccessor;
import tools.MaplePacketCreator;

public final class DistributeSPHandler extends AbstractMaplePacketHandler {

    public final void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        c.getPlayer().resetAfkTime();
        slea.skip(4);
        int skillid = slea.readInt();
        MapleCharacter player = c.getPlayer();
        boolean isBeginnerSkill = false;
        if (skillid % 10000000 > 999 && skillid % 10000000 < 1003) {
            isBeginnerSkill = true;
        }
        ISkill skill = SkillFactory.getSkill(skillid);
        int curLevel = player.getSkillLevel(skill);
        if (((player.getRemainingSp() > 0 || isBeginnerSkill) && curLevel + 1 <= (skill.isFourthJob() ? player.getMasterLevel(skill) : skill.getMaxLevel()))) {
            if (!isBeginnerSkill) {
                player.setRemainingSp(player.getRemainingSp() - 1);
            }

            if (skill.getId() == 21120002) //overswing or something?
            {
                player.changeSkillLevel(SkillFactory.getSkill(21120009), curLevel + 1, 0); //mastery level 0 == invisible
                player.changeSkillLevel(SkillFactory.getSkill(21120010), curLevel + 1, 0); //mastery level 0 == invisible
            }

            if (skill.getId() == 21110002) //full swing
            {
                player.changeSkillLevel(SkillFactory.getSkill(21110007), curLevel + 1, 0); //mastery level 0 == invisible
                player.changeSkillLevel(SkillFactory.getSkill(21110008), curLevel + 1, 0); //mastery level 0 == invisible
            }

            if (skillid == 1121001) {
            player.dropMessage(1, "You may not add SP into Monster Magnet");
            return;
        }
                        if (skillid == 1221001) {
            player.dropMessage(1, "You may not add SP into Monster Magnet");
            return;
        }
                        if (skillid == 1321001) {
            player.dropMessage(1, "You may not add SP into Monster Magnet");
            return;
        }

            if ((skill.getId() == 21110007) || (skill.getId() == 21110008) || (skill.getId() == 21120009) || (skill.getId() == 21120010)) {
                player.dropMessage(1, "This skill is linked to another skill; please don't put points in it.");
                player.getClient().getSession().write(MaplePacketCreator.enableActions());
                return;
            }


            player.updateSingleStat(MapleStat.AVAILABLESP, player.getRemainingSp());
            player.changeSkillLevel(skill, curLevel + 1, player.getMasterLevel(skill));
        }
    }
}

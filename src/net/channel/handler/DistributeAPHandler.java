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
import client.MapleClient;
import client.MapleJob;
import client.MapleStat;
import client.SkillFactory;
import net.AbstractMaplePacketHandler;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public final class DistributeAPHandler extends AbstractMaplePacketHandler {

    private static final int max = 999;

    public final void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        c.getPlayer().resetAfkTime();
        slea.skip(4);
        if (c.getPlayer().getRemainingAp() > 0) {
            if (addStat(c, slea.readInt())) {
                c.getPlayer().setRemainingAp(c.getPlayer().getRemainingAp() - 1);
                c.getPlayer().updateSingleStat(MapleStat.AVAILABLEAP, c.getPlayer().getRemainingAp());
            }
        }
        c.getSession().write(MaplePacketCreator.enableActions());
    }

    static boolean addStat(MapleClient c, int id) {
        MapleCharacter player = c.getPlayer();
        switch (id) {
            case 0x40: // Str
                if (c.getPlayer().getStr() >= max) {
                    return false;
                }
                c.getPlayer().addStat(1, 1);
                break;
            case 0x80: // Dex
                if (c.getPlayer().getDex() >= max) {
                    return false;
                }
                c.getPlayer().addStat(2, 1);
                break;
            case 0x100: // Int
                if (c.getPlayer().getInt() >= max) {
                    return false;
                }
                c.getPlayer().addStat(3, 1);
                break;
            case 0x200: // Luk
                if (c.getPlayer().getLuk() >= max) {
                    return false;
                }
                c.getPlayer().addStat(4, 1);
                break;
            case 0x800: // HP
                int MaxHP = player.getMaxHp();
                if (player.getHpMpApUsed() > 9999 || MaxHP >= 30000) {
                    return false;
                }
                int jobnum = player.getJob().getId() / 100 % 10;
                if (jobnum == 1 && player.getSkillLevel(player.isCygnus() ? SkillFactory.getSkill(10000000) : SkillFactory.getSkill(1000001)) > 0) {
                    MaxHP += 20;
                } else if (jobnum == 5 && player.getSkillLevel(player.isCygnus() ? SkillFactory.getSkill(15100000) : SkillFactory.getSkill(5100000)) > 0) {
                    MaxHP += 18;
                } else if (jobnum == 2) {
                    MaxHP += 6;
                } else {
                    MaxHP += 8;
                }
                MaxHP = Math.min(30000, MaxHP);
                player.setHpMpApUsed(player.getHpMpApUsed() + 1);
                player.setMaxHp(MaxHP);
                player.updateSingleStat(MapleStat.MAXHP, MaxHP);
                break;
            case 0x2000: // MP
                int MaxMP = player.getMaxMp();
                if (player.getHpMpApUsed() > 9999 || player.getMaxMp() >= 30000) {
                    return false;
                }
                if (player.getJob().isA(MapleJob.BEGINNER) || player.isCygnus()) {
                    MaxMP += 6;
                } else if (player.getJob().isA(MapleJob.WARRIOR) || player.getJob().isA(MapleJob.DAWNWARRIOR1)) {
                    MaxMP += 2;
                } else if (player.getJob().isA(MapleJob.MAGICIAN) || player.getJob().isA(MapleJob.BLAZEWIZARD1)) {
                    if (player.getSkillLevel(player.isCygnus() ? SkillFactory.getSkill(12000000) : SkillFactory.getSkill(2000001)) > 0) {
                        MaxMP += 18;
                    } else {
                        MaxMP += 14;
                    }
                } else if (player.getJob().isA(MapleJob.BOWMAN) || player.getJob().isA(MapleJob.THIEF)) {
                    MaxMP += 10;
                } else if (player.getJob().isA(MapleJob.PIRATE)) {
                    MaxMP += 14;
                }
                MaxMP = Math.min(30000, MaxMP);
                player.setHpMpApUsed(player.getHpMpApUsed() + 1);
                player.setMaxMp(MaxMP);
                player.updateSingleStat(MapleStat.MAXMP, MaxMP);
                break;
            default:
                c.getSession().write(MaplePacketCreator.updatePlayerStats(MaplePacketCreator.EMPTY_STATUPDATE, true));
                return false;
        }
        return true;
    }
}

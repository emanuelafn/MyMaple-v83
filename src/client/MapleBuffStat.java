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
package client;

import java.io.Serializable;
import net.LongValueHolder;

public enum MapleBuffStat implements LongValueHolder, Serializable {

    MORPH(0x2), // correct
    RECOVERY(0x4),
    MAPLE_WARRIOR(0x8), // correct
    STANCE(0x10),
    SHARP_EYES(0x20),
    MANA_REFLECTION(0x40),
    SHADOW_CLAW(0x100),
    INFINITY(0x200), // correct
    HOLY_SHIELD(0x400), // correct
    HAMSTRING(0x800),
    BLIND(0x1000),
    CONCENTRATE(0x2000),
    ECHO_OF_HERO(0x8000),
    GHOST_MORPH(0x20000),
    AURA(0x40000),
    CONFUSE(0x80000),
    BERSERK_FURY(0x8000000),
    DIVINE_BODY(0x10000000),
    SPARK(0x20000000),
    FINALATTACK(0x80000000L),
    WATK(0x100000000L),
    WDEF(0x200000000L), // correct
    MATK(0x400000000L), // correct
    MDEF(0x800000000L), // correct
    ACC(0x1000000000L),
    AVOID(0x2000000000L), // correct
    HANDS(0x4000000000L),
    SPEED(0x8000000000L), // correct
    BATTLESHIP(0xA00000040L),
    JUMP(0x10000000000L), // correct
    MAGIC_GUARD(0x20000000000L), // correct
    DARKSIGHT(0x40000000000L), // correct
    BOOSTER(0x80000000000L), // correct
    POWERGUARD(0x100000000000L),
    HYPERBODYHP(0x200000000000L), // correct
    HYPERBODYMP(0x400000000000L), // correct
    INVINCIBLE(0x800000000000L), // correct
    SOULARROW(0x1000000000000L), // correct
    COMBO(0x20000000000000L),
    SUMMON(0x20000000000000L),
    WK_CHARGE(0x40000000000000L), // correct
    DRAGONBLOOD(0x80000000000000L),
    HOLY_SYMBOL(0x100000000000000L), // correct
    MESOUP(0x200000000000000L),
    SHADOWPARTNER(0x400000000000000L), // correct
    PICKPOCKET(0x800000000000000L), // correct
    PUPPET(0x800000000000000L),
    SPEED_INFUSION(0x100000000000000L, true),
    MESOGUARD(0x1000000000000000L), // correct
    ELEMENTAL_RESET(0x200000000L, true), // correct
    ARAN_COMBO(0x1000000000L, true), // correct
    COMBO_DRAIN(0x2000000000L, true), // correct
    COMBO_BARRIER(0x4000000000L, true),
    BODY_PRESSURE(0x8000000000L, true), // correct
    SMART_KNOCKBACK(0x10000000000L, true), // correct
    PYRAMID_PQ(0x20000000000L, true), // correct
    DASH2(0x8000000000000L, true), // correct (speed)
    DASH(0x10000000000000L, true), // correct (jump)
    ENERGY_CHARGE(0x4000000000000L, true), // correct
    MONSTER_RIDING(0x20000000000000L, true), // correct
    HOMING_BEACON(0x80000000000000L, true); // correct  v85: 0x200000000000000L
    static final long serialVersionUID = 0L;
    private final long i;
    private final boolean isFirst;

    private MapleBuffStat(long i, boolean isFirst) {
        this.i = i;
        this.isFirst = isFirst;
    }

    private MapleBuffStat(long i) {
        this.i = i;
        this.isFirst = false;
    }

    @Override
    public long getValue() {
        return i;
    }

    public boolean isFirst() {
        return isFirst;
    }
}

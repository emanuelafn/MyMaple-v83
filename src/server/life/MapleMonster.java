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
package server.life;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import client.MapleBuffStat;
import client.MapleCharacter;
import client.MapleClient;
import client.MapleJob;
import client.SkillFactory;
import client.status.MonsterStatus;
import client.status.MonsterStatusEffect;
import constants.skills.FPMage;
import constants.skills.Hermit;
import constants.skills.ILMage;
import constants.skills.NightLord;
import constants.skills.NightWalker;
import constants.skills.Shadower;
import tools.Randomizer;
import net.MaplePacket;
import net.channel.ChannelServer;
import net.world.MapleParty;
import net.world.MaplePartyCharacter;
import scripting.event.EventInstanceManager;
import server.TimerManager;
import server.life.MapleLifeFactory.BanishInfo;
import server.life.MapleMonsterInformationProvider.DropEntry;
import server.maps.MapleMap;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import tools.MaplePacketCreator;
import tools.Pair;
import java.util.concurrent.locks.ReentrantLock;

public class MapleMonster extends AbstractLoadedMapleLife {

    private MapleMonsterStats stats;
    private MapleMonsterStats overrideStats;
    private int hp;
    private int mp;
    private WeakReference<MapleCharacter> controller = new WeakReference<MapleCharacter>(null);
    private boolean controllerHasAggro, controllerKnowsAboutAggro;
    private Collection<AttackerEntry> attackers = new LinkedList<AttackerEntry>();
    private EventInstanceManager eventInstance = null;
    private Collection<MonsterListener> listeners = new LinkedList<MonsterListener>();
    private MapleCharacter highestDamageChar;
    private Map<MonsterStatus, MonsterStatusEffect> stati = new LinkedHashMap<MonsterStatus, MonsterStatusEffect>();
    private List<MonsterStatusEffect> activeEffects = new ArrayList<MonsterStatusEffect>();
    private MapleMap map;
    private int VenomMultiplier = 0;
    private boolean fake = false;
    private boolean dropsDisabled = false;
    private List<Pair<Integer, Integer>> usedSkills = new ArrayList<Pair<Integer, Integer>>();
    private Map<Pair<Integer, Integer>, Integer> skillsUsed = new HashMap<Pair<Integer, Integer>, Integer>();
    private List<MonsterStatus> monsterBuffs = new ArrayList<MonsterStatus>();
    private List<Integer> stolenItems = new ArrayList<Integer>();
    public ReentrantLock monsterLock = new ReentrantLock();
    private int belongsTo = -1;
    private long endBelong;

    public MapleMonster(int id, MapleMonsterStats stats) {
        super(id);
        initWithStats(stats);
    }

    public MapleMonster(MapleMonster monster) {
        super(monster);
        initWithStats(monster.stats);
    }

    private void initWithStats(MapleMonsterStats stats) {
        setStance(5);
        this.stats = stats;
        hp = stats.getHp();
        mp = stats.getMp();
    }

    public void disableDrops() {
        this.dropsDisabled = true;
    }

    public boolean dropsDisabled() {
        return dropsDisabled;
    }

    public void setMap(MapleMap map) {
        this.map = map;
    }

    public int getDrop() {
        MapleMonsterInformationProvider mi = MapleMonsterInformationProvider.getInstance();
        int lastAssigned = -1;
        int minChance = 1;
        List<DropEntry> dl = mi.retrieveDropChances(getId());
        for (DropEntry d : dl) {
            if (d.chance > minChance) {
                minChance = d.chance;
            }
        }
        for (DropEntry d : dl) {
            d.assignedRangeStart = lastAssigned + 1;
            d.assignedRangeLength = (int) Math.ceil(((double) 1 / (double) d.chance) * minChance);
            lastAssigned += d.assignedRangeLength;
        }
        int c = Randomizer.getInstance().nextInt(minChance);
        for (DropEntry d : dl) {
            if (c >= d.assignedRangeStart && c < (d.assignedRangeStart + d.assignedRangeLength)) {
                if (stolenItems.contains(d.itemId)) {
                    return -1;
                }
                return d.itemId;
            }
        }
        return -1;
    }

    public int getHp() {
        return hp;
    }

    public void setHp(int hp) {
        this.hp = hp;
    }

    public int getMaxHp() {
        if (overrideStats != null) {
            return overrideStats.getHp();
        }
        return stats.getHp();
    }

    public boolean getBelongsToSomeone() {
        return belongsTo != -1 && endBelong > System.currentTimeMillis();
    }

    public int getBelongsTo() {
        return belongsTo;
    }

    public long getBelongTimeLeft() {
        if (getBelongsToSomeone()) {
            return endBelong - System.currentTimeMillis();
        } else {
            return 0;
        }
    }

    public boolean getCanBelong() {
        return belongsTo == -1;
    }

    public void expireAntiKS() {
        belongsTo = -2;
        //endBelong = System.currentTimeMillis();
    }

    public void setBelongsTo(MapleCharacter chr) { //Only let it be controlled once.
        if (belongsTo != -1) {
            return;
        }
        belongsTo = chr.getId();
        endBelong = System.currentTimeMillis() + 30000; //30 seconds for the person to kill it.
    }

    public int getMp() {
        return mp;
    }

    public void setMp(int mp) {
        if (mp < 0) {
            mp = 0;
        }
        this.mp = mp;
    }

    public int getMaxMp() {
        if (overrideStats != null) {
            return overrideStats.getMp();
        }
        return stats.getMp();
    }

    public int getExp() {
        if (overrideStats != null) {
            return overrideStats.getExp();
        }
        return stats.getExp();
    }

    int getLevel() {
        return stats.getLevel();
    }

    public int getVenomMulti() {
        return this.VenomMultiplier;
    }

    public void setVenomMulti(int multiplier) {
        this.VenomMultiplier = multiplier;
    }

    public boolean isBoss() {
        return stats.isBoss() || isHT();
    }

    public int getAnimationTime(String name) {
        return stats.getAnimationTime(name);
    }

    private List<Integer> getRevives() {
        return stats.getRevives();
    }

    private byte getTagColor() {
        return stats.getTagColor();
    }

    private byte getTagBgColor() {
        return stats.getTagBgColor();
    }

    /**
     *
     * @param from the player that dealt the damage
     * @param damage
     */
    public void damage(MapleCharacter from, int damage, boolean updateAttackTime) { //todo: get overridestats working here
        AttackerEntry attacker = null;
        if (from.getParty() != null) {
            attacker = new PartyAttackerEntry(from.getParty().getId(), from.getClient().getChannelServer());
        } else {
            attacker = new SingleAttackerEntry(from, from.getClient().getChannelServer());
        }
        boolean replaced = false;
        for (AttackerEntry aentry : attackers) {
            if (aentry.equals(attacker)) {
                attacker = aentry;
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            attackers.add(attacker);
        }
        int rDamage = Math.max(0, Math.min(damage, this.hp));
        attacker.addDamage(from, rDamage);
        this.hp -= rDamage;
        int remhppercentage = (int) Math.ceil((this.hp * 100.0) / getMaxHp());
        if (remhppercentage < 1) {
            remhppercentage = 1;
        }
        if (hasBossHPBar()) {
            from.getMap().broadcastMessage(makeBossHPBarPacket(), getPosition());
        } else if (!isBoss()) {
            for (AttackerEntry mattacker : attackers) {
                for (AttackingMapleCharacter cattacker : mattacker.getAttackers()) {
                    if (cattacker.getAttacker().getMap() == from.getMap()) {
                        cattacker.getAttacker().getClient().getSession().write(MaplePacketCreator.showMonsterHP(getObjectId(), remhppercentage));
                    }
                }
            }
        }
    }

    public void heal(int hp, int mp) {
        int hp2Heal = getHp() + hp;
        int mp2Heal = getMp() + mp;
        if (hp2Heal >= getMaxHp()) {
            hp2Heal = getMaxHp();
        }
        if (mp2Heal >= getMaxMp()) {
            mp2Heal = getMaxMp();
        }
        setHp(hp2Heal);
        setMp(mp2Heal);
        getMap().broadcastMessage(MaplePacketCreator.healMonster(getObjectId(), hp));
    }

    public boolean isAttackedBy(MapleCharacter chr) {
        for (AttackerEntry aentry : attackers) {
            if (aentry.contains(chr)) {
                return true;
            }
        }
        return false;
    }

    public void giveExpToCharacter(MapleCharacter attacker, int exp, boolean highestDamage, int numExpSharers, int party) {
        if (highestDamage) {
            if (eventInstance != null) {
                eventInstance.monsterKilled(attacker, this);
            }
            highestDamageChar = attacker;
        }
        if (attacker.getHp() > 0) {
            if (exp > 0) {
                Integer holySymbol = attacker.getBuffedValue(MapleBuffStat.HOLY_SYMBOL);
                if (holySymbol != null) {
                    exp *= 1.0 + (holySymbol.doubleValue() / (numExpSharers == 1 ? 500.0 : 100.0));
                }
            }
            attacker.gainExp(exp, true, false, highestDamage, party);
            attacker.mobKilled(this.getId());
            if ((int) (Math.random() * 100) < 3) {
                attacker.modifyCSPoints(4, 35);
                attacker.dropMessage("You have gained 35 NX");
            }
        }
    }

    public MapleCharacter killBy(MapleCharacter killer) {
        if (map.getId() > 980000000 && map.getId() < 980000604) {
            if (killer.getParty() != null) {
                killer.increaseCp(this.stats.getCP());
            }
        }
        long totalBaseExpL = (long) (this.getExp() * killer.getClient().getPlayer().getExpRate());
        int totalBaseExp = (int) (Math.min(Integer.MAX_VALUE, totalBaseExpL));
        AttackerEntry highest = null;
        int highdamage = 0;
        for (AttackerEntry attackEntry : attackers) {
            if (attackEntry.getDamage() > highdamage) {
                highest = attackEntry;
                highdamage = attackEntry.getDamage();
            }
        }
        for (AttackerEntry attackEntry : attackers) {
            attackEntry.killedMob(killer.getMap(), (int) Math.ceil(totalBaseExp * ((double) attackEntry.getDamage() / getMaxHp())), attackEntry == highest);
        }
        if (this.getController() != null) { // this can/should only happen when a hidden gm attacks the monster
            getController().getClient().getSession().write(MaplePacketCreator.stopControllingMonster(this.getObjectId()));
            getController().stopControllingMonster(this);
        }
        final List<Integer> toSpawn = this.getRevives();
        if (toSpawn != null) {
            final MapleMap reviveMap = killer.getMap();
            if (toSpawn.contains(9300216) && reviveMap.getId() > 925000000 && reviveMap.getId() < 926000000) {
                reviveMap.broadcastMessage(MaplePacketCreator.playSound("Dojang/clear"));
                reviveMap.broadcastMessage(MaplePacketCreator.showEffect("dojang/end/clear"));
            }
            if (toSpawn.contains(reviveMap.getTimeMobId())) {
                reviveMap.broadcastMessage(MaplePacketCreator.serverNotice(6, reviveMap.getTimeMobMessage()));
            }
            for (Integer mid : toSpawn) {
                MapleMonster mob = MapleLifeFactory.getMonster(mid);
                if (eventInstance != null) {
                    eventInstance.registerMonster(mob);
                }
                mob.setPosition(getPosition());
                if (dropsDisabled()) {
                    mob.disableDrops();
                }
                reviveMap.spawnMonster(mob);
            }
        }
        if (eventInstance != null) {
            eventInstance.unregisterMonster(this);
        }
        for (MonsterListener listener : listeners.toArray(new MonsterListener[listeners.size()])) {
            listener.monsterKilled(this, highestDamageChar);
        }
        MapleCharacter ret = highestDamageChar;
        highestDamageChar = null; // may not keep hard references to chars outside of PlayerStorage or MapleMap
        return ret;
    }

    public boolean isAlive() {
        return this.hp > 0;
    }

    public MapleCharacter getController() {
        return controller.get();
    }

    public void setController(MapleCharacter controller) {
        this.controller = new WeakReference<MapleCharacter>(controller);
    }

    public void switchController(MapleCharacter newController, boolean immediateAggro) {
        MapleCharacter controllers = getController();
        if (controllers == newController) {
            return;
        }
        if (controllers != null) {
            controllers.stopControllingMonster(this);
            controllers.getClient().getSession().write(MaplePacketCreator.stopControllingMonster(getObjectId()));
        }
        newController.controlMonster(this, immediateAggro);
        setController(newController);
        if (immediateAggro) {
            setControllerHasAggro(true);
        }
        setControllerKnowsAboutAggro(false);
    }

    public void addListener(MonsterListener listener) {
        listeners.add(listener);
    }

    public boolean isControllerHasAggro() {
        return fake ? false : controllerHasAggro;
    }

    public void setControllerHasAggro(boolean controllerHasAggro) {
        if (fake) {
            return;
        }
        this.controllerHasAggro = controllerHasAggro;
    }

    public boolean isControllerKnowsAboutAggro() {
        return fake ? false : controllerKnowsAboutAggro;
    }

    public void setControllerKnowsAboutAggro(boolean controllerKnowsAboutAggro) {
        if (fake) {
            return;
        }
        this.controllerKnowsAboutAggro = controllerKnowsAboutAggro;
    }

    public MaplePacket makeBossHPBarPacket() {
        return MaplePacketCreator.showBossHP(getId(), getHp(), getMaxHp(), getTagColor(), getTagBgColor());
    }

    public boolean hasBossHPBar() {
        return (isBoss() && getTagColor() > 0) || isHT();
    }

    private boolean isHT() {
        return getId() == 8810018 || getId() == 8810026;
    }

    @Override
    public void sendSpawnData(MapleClient c) {
        if (!isAlive()) {
            return;
        }
        if (isFake()) {
            c.getSession().write(MaplePacketCreator.spawnFakeMonster(this, 0));
        } else {
            c.getSession().write(MaplePacketCreator.spawnMonster(this, false));
        }
        if (stati.size() > 0) {
            for (MonsterStatusEffect mse : activeEffects) {
                MaplePacket packet = MaplePacketCreator.applyMonsterStatus(getObjectId(), mse.getStati(), mse.getSkill().getId(), false, 0);
                c.getSession().write(packet);
            }
        }
        if (hasBossHPBar()) {
            c.getSession().write(makeBossHPBarPacket());
        }
    }

    @Override
    public void sendDestroyData(MapleClient client) {
        client.getSession().write(MaplePacketCreator.killMonster(getObjectId(), false));
    }

    @Override
    public MapleMapObjectType getType() {
        return MapleMapObjectType.MONSTER;
    }

    public void setEventInstance(EventInstanceManager eventInstance) {
        this.eventInstance = eventInstance;
    }

    public boolean isMobile() {
        return stats.isMobile();
    }

    public ElementalEffectiveness getEffectiveness(Element e) {
        if (activeEffects.size() > 0 && stati.get(MonsterStatus.DOOM) != null) {
            return ElementalEffectiveness.NORMAL;
        }
        return stats.getEffectiveness(e);
    }

    public boolean applyStatus(MapleCharacter from, final MonsterStatusEffect status, boolean poison, long duration) {
        return applyStatus(from, status, poison, duration, false);
    }

    public boolean applyStatus(MapleCharacter from, final MonsterStatusEffect status, boolean poison, long duration, boolean venom) {
        switch (stats.getEffectiveness(status.getSkill().getElement())) {
            case IMMUNE:
            case STRONG:
            case NEUTRAL:
                return false;
            case NORMAL:
            case WEAK:
                break;
            default: {
                System.out.println("Unknown elemental effectiveness: " + stats.getEffectiveness(status.getSkill().getElement()));
                return false;
            }
        }
        if (status.getSkill().getId() == FPMage.ELEMENT_COMPOSITION) {
            ElementalEffectiveness effectiveness = stats.getEffectiveness(Element.POISON);
            if (effectiveness == ElementalEffectiveness.IMMUNE || effectiveness == ElementalEffectiveness.STRONG) {
                return false;
            }
        } else if (status.getSkill().getId() == ILMage.ELEMENT_COMPOSITION) {
            ElementalEffectiveness effectiveness = stats.getEffectiveness(Element.ICE);
            if (effectiveness == ElementalEffectiveness.IMMUNE || effectiveness == ElementalEffectiveness.STRONG) {
                return false;
            }
        } else if (status.getSkill().getId() == NightLord.VENOMOUS_STAR || status.getSkill().getId() == Shadower.VENOMOUS_STAB || status.getSkill().getId() == NightWalker.VENOM) {// venom
            ElementalEffectiveness effectiveness = stats.getEffectiveness(Element.POISON);
            if (effectiveness == ElementalEffectiveness.IMMUNE || effectiveness == ElementalEffectiveness.STRONG) {
                return false;
            }
        }
        if (poison && getHp() <= 1) {
            return false;
        }
        if (isBoss() && !status.getStati().containsKey(MonsterStatus.SPEED)) {
            return false;
        }
        for (MonsterStatus stat : status.getStati().keySet()) {
            MonsterStatusEffect oldEffect = stati.get(stat);
            if (oldEffect != null) {
                oldEffect.removeActiveStatus(stat);
                if (oldEffect.getStati().size() == 0) {
                    oldEffect.getCancelTask().cancel(false);
                    oldEffect.cancelPoisonSchedule();
                    activeEffects.remove(oldEffect);
                    oldEffect = null;
                }
            }
        }
        TimerManager timerManager = TimerManager.getInstance();
        final Runnable cancelTask = new Runnable() {

            @Override
            public void run() {
                if (isAlive()) {
                    MaplePacket packet = MaplePacketCreator.cancelMonsterStatus(getObjectId(), status.getStati());
                    map.broadcastMessage(packet, getPosition());
                    if (getController() != null && !getController().isMapObjectVisible(MapleMonster.this)) {
                        getController().getClient().getSession().write(packet);
                    }
                }
                activeEffects.remove(status);
                for (MonsterStatus stat : status.getStati().keySet()) {
                    stati.remove(stat);
                    stat = null;
                }
                setVenomMulti(0);
                status.cancelPoisonSchedule();
            }
        };
        if (poison) {
            int poisonLevel = from.getSkillLevel(status.getSkill());
            int poisonDamage = Math.min(Short.MAX_VALUE, (int) (getMaxHp() / (70.0 - poisonLevel) + 0.999));
            status.setValue(MonsterStatus.POISON, Integer.valueOf(poisonDamage));
            status.setPoisonSchedule(timerManager.register(new PoisonTask(poisonDamage, from, status, cancelTask, false), 1000, 1000));
        } else if (venom) {
            if (from.getJob() == MapleJob.NIGHTLORD || from.getJob() == MapleJob.SHADOWER || from.getJob().isA(MapleJob.NIGHTWALKER3)) {
                int poisonLevel = 0;
                int matk = 0;
                int id = from.getJob().getId();
                int skill = (id == 412 ? 4120005 : (id == 422 ? 4220005 : 14110004));
                poisonLevel = from.getSkillLevel(SkillFactory.getSkill(skill));
                if (poisonLevel <= 0) {
                    return false;
                }
                matk = SkillFactory.getSkill(skill).getEffect(poisonLevel).getMatk();
                int luk = from.getLuk();
                int maxDmg = (int) Math.ceil(Math.min(Short.MAX_VALUE, 0.2 * luk * matk));
                int minDmg = (int) Math.ceil(Math.min(Short.MAX_VALUE, 0.1 * luk * matk));
                int gap = maxDmg - minDmg;
                if (gap == 0) {
                    gap = 1;
                }
                int poisonDamage = 0;
                for (int i = 0; i < getVenomMulti(); i++) {
                    poisonDamage = poisonDamage + (Randomizer.getInstance().nextInt(gap) + minDmg);
                }
                poisonDamage = Math.min(Short.MAX_VALUE, poisonDamage);
                status.setValue(MonsterStatus.POISON, Integer.valueOf(poisonDamage));
                status.setPoisonSchedule(timerManager.register(new PoisonTask(poisonDamage, from, status, cancelTask, false), 1000, 1000));
            } else {
                return false;
            }
        } else if (status.getSkill().getId() == Hermit.SHADOW_WEB || status.getSkill().getId() == NightWalker.SHADOW_WEB) {
            status.setPoisonSchedule(timerManager.schedule(new PoisonTask((int) (getMaxHp() / 50.0 + 0.999), from, status, cancelTask, true), 3500));
        }
        for (MonsterStatus stat : status.getStati().keySet()) {
            stati.put(stat, status);
        }
        activeEffects.add(status);
        int animationTime = status.getSkill().getAnimationTime();
        MaplePacket packet = MaplePacketCreator.applyMonsterStatus(getObjectId(), status.getStati(), status.getSkill().getId(), false, 0);
        map.broadcastMessage(packet, getPosition());
        if (getController() != null && !getController().isMapObjectVisible(this)) {
            getController().getClient().getSession().write(packet);
        }
        status.setCancelTask(timerManager.schedule(cancelTask, duration + animationTime));
        return true;
    }

    public void applyMonsterBuff(final MonsterStatus status, final int x, int skillId, long duration, MobSkill skill) {
        TimerManager timerManager = TimerManager.getInstance();
        final Runnable cancelTask = new Runnable() {

            @Override
            public void run() {
                if (isAlive()) {
                    MaplePacket packet = MaplePacketCreator.cancelMonsterStatus(getObjectId(), Collections.singletonMap(status, Integer.valueOf(x)));
                    map.broadcastMessage(packet, getPosition());
                    if (getController() != null && !getController().isMapObjectVisible(MapleMonster.this)) {
                        getController().getClient().getSession().write(packet);
                    }
                    removeMonsterBuff(status);
                }
            }
        };
        MaplePacket packet = MaplePacketCreator.applyMonsterStatus(getObjectId(), Collections.singletonMap(status, x), skillId, true, 0, skill);
        map.broadcastMessage(packet, getPosition());
        if (getController() != null && !getController().isMapObjectVisible(this)) {
            getController().getClient().getSession().write(packet);
        }
        timerManager.schedule(cancelTask, duration);
        addMonsterBuff(status);
    }

    public void addMonsterBuff(MonsterStatus status) {
        this.monsterBuffs.add(status);
    }

    public void removeMonsterBuff(MonsterStatus status) {
        this.monsterBuffs.remove(status);
    }

    public boolean isBuffed(MonsterStatus status) {
        return this.monsterBuffs.contains(status);
    }

    public void setFake(boolean fake) {
        this.fake = fake;
    }

    public boolean isFake() {
        return fake;
    }

    public MapleMap getMap() {
        return map;
    }

    public List<Pair<Integer, Integer>> getSkills() {
        return stats.getSkills();
    }

    public boolean hasSkill(int skillId, int level) {
        return stats.hasSkill(skillId, level);
    }

    public boolean canUseSkill(MobSkill toUse) {
        if (toUse == null) {
            return false;
        }
        for (Pair<Integer, Integer> skill : usedSkills) {
            if (skill.getLeft() == toUse.getSkillId() && skill.getRight() == toUse.getSkillLevel()) {
                return false;
            }
        }
        if (toUse.getLimit() > 0) {
            if (this.skillsUsed.containsKey(new Pair<Integer, Integer>(toUse.getSkillId(), toUse.getSkillLevel()))) {
                int times = this.skillsUsed.get(new Pair<Integer, Integer>(toUse.getSkillId(), toUse.getSkillLevel()));
                if (times >= toUse.getLimit()) {
                    return false;
                }
            }
        }
        if (toUse.getSkillId() == 200) {
            Collection<MapleMapObject> mmo = getMap().getMapObjects();
            int i = 0;
            for (MapleMapObject mo : mmo) {
                if (mo.getType() == MapleMapObjectType.MONSTER) {
                    i++;
                }
            }
            if (i > 100) {
                return false;
            }
        }
        return true;
    }

    public void usedSkill(final int skillId, final int level, long cooltime) {
        this.usedSkills.add(new Pair<Integer, Integer>(skillId, level));
        if (this.skillsUsed.containsKey(new Pair<Integer, Integer>(skillId, level))) {
            int times = this.skillsUsed.get(new Pair<Integer, Integer>(skillId, level)) + 1;
            this.skillsUsed.remove(new Pair<Integer, Integer>(skillId, level));
            this.skillsUsed.put(new Pair<Integer, Integer>(skillId, level), times);
        } else {
            this.skillsUsed.put(new Pair<Integer, Integer>(skillId, level), 1);
        }
        final MapleMonster mons = this;
        TimerManager tMan = TimerManager.getInstance();
        tMan.schedule(
                new Runnable() {

                    @Override
                    public void run() {
                        mons.clearSkill(skillId, level);
                    }
                }, cooltime);
    }

    public void clearSkill(int skillId, int level) {
        int index = -1;
        for (Pair<Integer, Integer> skill : usedSkills) {
            if (skill.getLeft() == skillId && skill.getRight() == level) {
                index = usedSkills.indexOf(skill);
                break;
            }
        }
        if (index != -1) {
            usedSkills.remove(index);
        }
    }

    public int getNoSkills() {
        return this.stats.getNoSkills();
    }

    public boolean isFirstAttack() {
        return this.stats.isFirstAttack();
    }

    public int getBuffToGive() {
        return this.stats.getBuffToGive();
    }

    private final class PoisonTask implements Runnable {

        private final int poisonDamage;
        private final MapleCharacter chr;
        private final MonsterStatusEffect status;
        private final Runnable cancelTask;
        private final boolean shadowWeb;
        private final MapleMap map;

        private PoisonTask(int poisonDamage, MapleCharacter chr, MonsterStatusEffect status, Runnable cancelTask, boolean shadowWeb) {
            this.poisonDamage = poisonDamage;
            this.chr = chr;
            this.status = status;
            this.cancelTask = cancelTask;
            this.shadowWeb = shadowWeb;
            this.map = chr.getMap();
        }

        @Override
        public void run() {
            int damage = poisonDamage;
            if (damage >= hp) {
                damage = hp - 1;
                if (!shadowWeb) {
                    cancelTask.run();
                    status.getCancelTask().cancel(false);
                }
            }
            if (hp > 1 && damage > 0) {
                damage(chr, damage, false);
                if (shadowWeb) {
                    map.broadcastMessage(MaplePacketCreator.damageMonster(getObjectId(), damage), getPosition());
                }
            }
        }
    }

    public String getName() {
        return stats.getName();
    }

    private class AttackingMapleCharacter {

        private MapleCharacter attacker;

        public AttackingMapleCharacter(MapleCharacter attacker) {
            super();
            this.attacker = attacker;
        }

        public MapleCharacter getAttacker() {
            return attacker;
        }
    }

    private interface AttackerEntry {

        List<AttackingMapleCharacter> getAttackers();

        public void addDamage(MapleCharacter from, int damage);

        public int getDamage();

        public boolean contains(MapleCharacter chr);

        public void killedMob(MapleMap map, int baseExp, boolean mostDamage);
    }

    private class SingleAttackerEntry implements AttackerEntry {

        private int damage;
        private int chrid;
        private ChannelServer cserv;

        public SingleAttackerEntry(MapleCharacter from, ChannelServer cserv) {
            this.chrid = from.getId();
            this.cserv = cserv;
        }

        @Override
        public void addDamage(MapleCharacter from, int damage) {
            if (chrid == from.getId()) {
                this.damage += damage;
            } else {
                throw new IllegalArgumentException("Not the attacker of this entry");
            }
        }

        @Override
        public List<AttackingMapleCharacter> getAttackers() {
            MapleCharacter chr = cserv.getPlayerStorage().getCharacterById(chrid);
            if (chr != null) {
                return Collections.singletonList(new AttackingMapleCharacter(chr));
            } else {
                return Collections.emptyList();
            }
        }

        @Override
        public boolean contains(MapleCharacter chr) {
            return chrid == chr.getId();
        }

        @Override
        public int getDamage() {
            return damage;
        }

        @Override
        public void killedMob(MapleMap map, int baseExp, boolean mostDamage) {
            MapleCharacter chr = cserv.getPlayerStorage().getCharacterById(chrid);
            if (chr != null && chr.getMap() == map) {
                giveExpToCharacter(chr, baseExp, mostDamage, 1, 0);
            }
        }

        @Override
        public int hashCode() {
            return chrid;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final SingleAttackerEntry other = (SingleAttackerEntry) obj;
            return chrid == other.chrid;
        }
    }

    private static class OnePartyAttacker {

        public MapleParty lastKnownParty;
        public int damage;

        public OnePartyAttacker(MapleParty lastKnownParty, int damage) {
            this.lastKnownParty = lastKnownParty;
            this.damage = damage;
        }
    }

    private class PartyAttackerEntry implements AttackerEntry {

        private int totDamage;
        private Map<Integer, OnePartyAttacker> attackers;
        private ChannelServer cserv;
        private int partyid;

        public PartyAttackerEntry(int partyid, ChannelServer cserv) {
            this.partyid = partyid;
            this.cserv = cserv;
            attackers = new HashMap<Integer, OnePartyAttacker>(6);
        }

        public List<AttackingMapleCharacter> getAttackers() {
            List<AttackingMapleCharacter> ret = new ArrayList<AttackingMapleCharacter>(attackers.size());
            for (Entry<Integer, OnePartyAttacker> entry : attackers.entrySet()) {
                MapleCharacter chr = cserv.getPlayerStorage().getCharacterById(entry.getKey());
                if (chr != null) {
                    ret.add(new AttackingMapleCharacter(chr));
                }
            }
            return ret;
        }

        private Map<MapleCharacter, OnePartyAttacker> resolveAttackers() {
            Map<MapleCharacter, OnePartyAttacker> ret = new HashMap<MapleCharacter, OnePartyAttacker>(attackers.size());
            for (Entry<Integer, OnePartyAttacker> aentry : attackers.entrySet()) {
                MapleCharacter chr = cserv.getPlayerStorage().getCharacterById(aentry.getKey());
                if (chr != null) {
                    ret.put(chr, aentry.getValue());
                }
            }
            return ret;
        }

        @Override
        public boolean contains(MapleCharacter chr) {
            return attackers.containsKey(chr.getId());
        }

        @Override
        public int getDamage() {
            return totDamage;
        }

        public void addDamage(MapleCharacter from, int damage) {
            OnePartyAttacker oldPartyAttacker = attackers.get(from.getId());
            if (oldPartyAttacker != null) {
                oldPartyAttacker.damage += damage;
                oldPartyAttacker.lastKnownParty = from.getParty();
            } else {
                // TODO actually this causes wrong behaviour when the party changes between attacks
                // only the last setup will get exp - but otherwise we'd have to store the full party
                // constellation for every attack/everytime it changes, might be wanted/needed in the
                // future but not now
                OnePartyAttacker onePartyAttacker = new OnePartyAttacker(from.getParty(), damage);
                attackers.put(from.getId(), onePartyAttacker);
            }
            totDamage += damage;
        }

        @Override
        public void killedMob(MapleMap map, int baseExp, boolean mostDamage) {
            Map<MapleCharacter, OnePartyAttacker> attackers_ = resolveAttackers();
            MapleCharacter pchr, highest = null;
            int iDamage, iexp, ptysize = 0, highestDamage = 0;
            MapleParty party;
            double averagePartyLevel, expBonus, expWeight, levelMod, innerBaseExp, expFraction;
            List<MapleCharacter> expApplicable;
            Map<MapleCharacter, ExpMap> expMap = new HashMap<MapleCharacter, ExpMap>(6);
            for (Entry<MapleCharacter, OnePartyAttacker> attacker : attackers_.entrySet()) {
                party = attacker.getValue().lastKnownParty;
                averagePartyLevel = 0;
                ptysize = 0;
                expApplicable = new ArrayList<MapleCharacter>();
                for (MaplePartyCharacter partychar : party.getMembers()) {
                    if (attacker.getKey().getLevel() - partychar.getLevel() <= 5 || getLevel() - partychar.getLevel() <= 5) {
                        pchr = cserv.getPlayerStorage().getCharacterByName(partychar.getName());
                        if (pchr != null) {
                            if (pchr.isAlive() && pchr.getMap() == map) {
                                expApplicable.add(pchr);
                                averagePartyLevel += pchr.getLevel();
                                ptysize++;
                            }
                        }
                    }
                }
                expBonus = 1.0;
                if (expApplicable.size() > 1) {
                    expBonus = 1.10 + 0.05 * expApplicable.size();
                    averagePartyLevel /= expApplicable.size();
                }
                iDamage = attacker.getValue().damage;
                if (iDamage > highestDamage) {
                    highest = attacker.getKey();
                    highestDamage = iDamage;
                }
                innerBaseExp = baseExp * ((double) iDamage / totDamage);
                expFraction = (innerBaseExp * expBonus) / (expApplicable.size() + 1);
                for (MapleCharacter expReceiver : expApplicable) {
                    iexp = expMap.get(expReceiver) == null ? 0 : expMap.get(expReceiver).exp;
                    expWeight = (expReceiver == attacker.getKey() ? 2.0 : 1.0);
                    levelMod = expReceiver.getLevel() / averagePartyLevel;
                    if (levelMod > 1.0 || this.attackers.containsKey(expReceiver.getId())) {
                        levelMod = 1.0;
                    }
                    iexp += (int) Math.round(expFraction * expWeight * levelMod);
                    expMap.put(expReceiver, new ExpMap(iexp, (short) ptysize));
                }
            }
                for (Entry<MapleCharacter, ExpMap> expReceiver : expMap.entrySet()) {
                    int exp = expReceiver.getValue().exp;
                    if (expReceiver.getKey().getReborns() == 0) {
                        exp *= 2;
                    }
                    giveExpToCharacter(expReceiver.getKey(), exp, mostDamage ? expReceiver.getKey() == highest : false, expMap.size(), expReceiver.getValue().ptysize);
                }
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + partyid;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final PartyAttackerEntry other = (PartyAttackerEntry) obj;
            if (partyid != other.partyid) {
                return false;
            }
            return true;
        }
    }

    public void addStolen(int itemId) {
        stolenItems.add(itemId);
    }

    public void setTempEffectiveness(Element e, ElementalEffectiveness ee, long milli) {
        final Element fE = e;
        final ElementalEffectiveness fEE = stats.getEffectiveness(e);
        if (!stats.getEffectiveness(e).equals(ElementalEffectiveness.WEAK)) {
            stats.setEffectiveness(e, ee);
            TimerManager.getInstance().schedule(new Runnable() {

                public void run() {
                    stats.removeEffectiveness(fE);
                    stats.setEffectiveness(fE, fEE);
                }
            }, milli);
        }
    }

    public BanishInfo getBanish() {
        return stats.getBanishInfo();
    }

    public void setBoss(boolean boss) {
        this.stats.setBoss(boss);
    }

    public int getDropPeriodTime() {
        return stats.getDropPeriod();
    }

    public int getPADamage() {
        return stats.getPADamage();
    }

    public void setOverrideStats(MapleMonsterStats override) {
        this.overrideStats = override;
        this.hp = override.getHp();
        this.mp = override.getMp();
    }

    public void empty() {
        try {
            this.monsterLock.unlock();
        } catch (Exception e) {
        } //who cares
        this.monsterLock = null;
        for (MonsterStatus ms : stati.keySet()) {
            ms = null;
        }
        this.stati = null;
        this.usedSkills = null;
        this.listeners = null;
        this.skillsUsed = null;
        for (MonsterStatusEffect mse : activeEffects) {
            mse.getCancelTask().cancel(false);
            mse.cancelPoisonSchedule();
            //activeEffects.remove(mse);
            mse = null;
        }
        this.activeEffects = null;
        this.monsterBuffs = null;
        this.stats = null;
        this.highestDamageChar = null;
        this.map = null;
        this.eventInstance = null;
        this.attackers = null;
        this.stolenItems = null;
        this.controller = null;
    }

    private static class ExpMap {

        public int exp;
        public short ptysize;

        public ExpMap(int exp, short ptysize) {
            this.exp = exp;
            this.ptysize = ptysize;
        }
    }
}

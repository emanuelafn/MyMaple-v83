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

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Calendar;
import client.Equip;
import client.IItem;
import client.Item;
import client.MapleBuffStat;
import client.MapleCharacter;
import client.MapleClient;
import client.MapleInventoryType;
import client.MapleOccupations;
import client.MaplePet;
import client.SkillFactory;
import client.status.MonsterStatus;
import client.status.MonsterStatusEffect;
import constants.InventoryConstants;
import tools.Randomizer;
import net.MaplePacket;
import net.channel.ChannelServer;
import net.world.MaplePartyCharacter;
import scripting.map.MapScriptManager;
import server.MapleItemInformationProvider;
import server.MaplePortal;
import server.MapleStatEffect;
import server.TimerManager;
import server.life.MapleMonster;
import server.life.MapleNPC;
import server.life.SpawnPoint;
import tools.MaplePacketCreator;
import server.MapleOxQuiz;
import server.MapleSquad;
import server.life.MapleLifeFactory;
import java.util.concurrent.locks.ReentrantReadWriteLock; //better at deadlock prevention

public class MapleMap {

    private static final List<MapleMapObjectType> rangedMapobjectTypes = Arrays.asList(MapleMapObjectType.SHOP, MapleMapObjectType.ITEM, MapleMapObjectType.NPC, MapleMapObjectType.MONSTER, MapleMapObjectType.DOOR, MapleMapObjectType.SUMMON, MapleMapObjectType.REACTOR);
    private Map<Integer, MapleMapObject> mapobjects = new LinkedHashMap<Integer, MapleMapObject>();
    private Collection<SpawnPoint> monsterSpawn = new LinkedList<SpawnPoint>();
    private AtomicInteger spawnedMonstersOnMap = new AtomicInteger(0);
    private Collection<MapleCharacter> characters = new LinkedHashSet<MapleCharacter>();
    private Map<Integer, MaplePortal> portals = new HashMap<Integer, MaplePortal>();
    private List<Rectangle> areas = new ArrayList<Rectangle>();
    private MapleFootholdTree footholds = null;
    private int mapid;
    private int runningOid = 30000;
    private int returnMapId;
    private int channel;
    private float monsterRate;
    private boolean clock;
    private boolean boat;
    private boolean docked;
    private String mapName;
    private String streetName;
    private MapleMapEffect mapEffect = null;
    private boolean everlast = false;
    private int forcedReturnMap = 999999999;
    private int timeLimit;
    private int dropLife = 180000;
    private int decHP = 0;
    private int protectItem = 0;
    private boolean town;
    private MapleOxQuiz ox;
    private boolean isOxQuiz = false;
    private boolean dropsOn = true;
    private String onFirstUserEnter;
    private String onUserEnter;
    private int dropRate;
    private int bossDropRate;
    private int fieldType;
    private int timeMobId;
    private String timeMobMessage = "";
    private int fieldLimit = 0;
    private MapleSquad mapleSquad = null;
    public ScheduledFuture respawnTask;
    // Threading - allows for better performance (and deadlock prevention) than synchronized methods
    private ReentrantReadWriteLock objectlock = new ReentrantReadWriteLock(true);
    private ReentrantReadWriteLock characterlock = new ReentrantReadWriteLock(true);
    // HPQ
    private int riceCakeNum = 0; // bad place to put this
    int randhum = MapleCharacter.rand(1,5);

    public MapleMap(int mapid, int channel, int returnMapId, float monsterRate) {
        this.mapid = mapid;
        this.channel = (short) channel;
        this.returnMapId = returnMapId;
        this.monsterRate = monsterRate;
        this.dropRate = ChannelServer.getInstance(channel).getDropRate();
        this.bossDropRate = ChannelServer.getInstance(channel).getBossDropRate();
        InitiateRespawnTask();

    }
    
     public void addBotPlayer(MapleCharacter chr) {
        synchronized (characters) {
            this.characters.add(chr);
        }
        synchronized (this.mapobjects) {
            if (!chr.isHidden()) {
                broadcastMessage(chr, (MaplePacketCreator.spawnPlayerMapobject(chr)), false);
            }
            this.mapobjects.put(Integer.valueOf(chr.getObjectId()), chr);
        }
    }

    public void broadcastMessage(MapleCharacter source, MaplePacket packet) {
        characterlock.readLock().lock();
        try {
            for (MapleCharacter chr : characters) {
                if (chr != source) {
                    chr.getClient().getSession().write(packet);
                }
            }
        } finally {
            characterlock.readLock().unlock();
        }
    }

    public void toggleDrops() {
        this.dropsOn = !dropsOn;
    }

    public List<MapleMapObject> getMapObjectsInRect(Rectangle box, List<MapleMapObjectType> types) {
        List<MapleMapObject> ret = new LinkedList<MapleMapObject>();
        objectlock.readLock().lock();
        try {
            for (MapleMapObject l : mapobjects.values()) {
                if (types.contains(l.getType())) {
                    if (box.contains(l.getPosition())) {
                        ret.add(l);
                    }
                }
            }
        } finally {
            objectlock.readLock().unlock();
        }
        return ret;
    }

    public int getId() {
        return mapid;
    }

    public MapleMap getReturnMap() {
        return ChannelServer.getInstance(channel).getMapFactory().getMap(returnMapId);
    }

    public int getReturnMapId() {
        return returnMapId;
    }

    public void setReactorState() {
        objectlock.readLock().lock();
        try {
            for (MapleMapObject o : mapobjects.values()) {
                if (o.getType() == MapleMapObjectType.REACTOR) {
                    ((MapleReactor) o).setState((byte) 1);
                    broadcastMessage(MaplePacketCreator.triggerReactor((MapleReactor) o, 1));
                }
            }
        } finally {
            objectlock.readLock().unlock();
        }
    }

    public int getForcedReturnId() {
        return forcedReturnMap;
    }

    public MapleMap getForcedReturnMap() {
        return ChannelServer.getInstance(channel).getMapFactory().getMap(forcedReturnMap);
    }

    public void setForcedReturnMap(int map) {
        this.forcedReturnMap = map;
    }

    public int getTimeLimit() {
        return timeLimit;
    }

    public void setTimeLimit(int timeLimit) {
        this.timeLimit = timeLimit;
    }

    public int getCurrentPartyId() {
        for (MapleCharacter chr : this.getCharacters()) {
            if (chr.getPartyId() != -1) {
                return chr.getPartyId();
            }
        }
        return -1;
    }

    public void addMapObject(MapleMapObject mapobject) {
        objectlock.writeLock().lock();
        try {
            mapobject.setObjectId(runningOid);
            this.mapobjects.put(Integer.valueOf(runningOid), mapobject);
            incrementRunningOid();
        } finally {
            objectlock.writeLock().unlock();
        }
    }

    private void spawnAndAddRangedMapObject(MapleMapObject mapobject, DelayedPacketCreation packetbakery) {
        spawnAndAddRangedMapObject(mapobject, packetbakery, null);
    }

    private void spawnAndAddRangedMapObject(MapleMapObject mapobject, DelayedPacketCreation packetbakery, SpawnCondition condition) {
        objectlock.writeLock().lock();
        characterlock.readLock().lock();
        try {
            mapobject.setObjectId(runningOid);
            for (MapleCharacter chr : characters) {
                if (condition == null || condition.canSpawn(chr)) {
                    if (chr.getPosition().distanceSq(mapobject.getPosition()) <= 722500) {
                        packetbakery.sendPackets(chr.getClient());
                        chr.addVisibleMapObject(mapobject);
                    }
                }
            }
            this.mapobjects.put(Integer.valueOf(runningOid), mapobject);
            incrementRunningOid();
        } finally {
            characterlock.readLock().unlock();
            objectlock.writeLock().unlock();
        }

    }

    private void incrementRunningOid() {
        runningOid++;
        /*   if (!this.mapobjects.containsKey(Integer.valueOf(runningOid))) {
        return;
        }
        for (int numIncrements = 1; numIncrements < 30000; numIncrements++) {
        if (runningOid > 30000) {
        runningOid = 100;
        }
        if (this.mapobjects.containsKey(Integer.valueOf(runningOid))) {
        runningOid++;
        } else {
        return;
        }
        }

        throw new RuntimeException("Out of OIDs on map " + mapid + " (channel: " + channel + ")");*/
        //unneeded and costly
    }

    public void removeMapObject(int num) {
        objectlock.writeLock().lock();
        try {
            this.mapobjects.remove(Integer.valueOf(num));
        } finally {
            objectlock.writeLock().unlock();
        }
    }

    public void removeMapObject(MapleMapObject obj) {
        removeMapObject(obj.getObjectId());
    }

    private Point calcPointBelow(Point initial) {
        MapleFoothold fh = footholds.findBelow(initial);
        if (fh == null) {
            return null;
        }
        int dropY = fh.getY1();
        if (!fh.isWall() && fh.getY1() != fh.getY2()) {
            double s1 = Math.abs(fh.getY2() - fh.getY1());
            double s2 = Math.abs(fh.getX2() - fh.getX1());
            double s5 = Math.cos(Math.atan(s2 / s1)) * (Math.abs(initial.x - fh.getX1()) / Math.cos(Math.atan(s1 / s2)));
            if (fh.getY2() < fh.getY1()) {
                dropY = fh.getY1() - (int) s5;
            } else {
                dropY = fh.getY1() + (int) s5;
            }
        }
        return new Point(initial.x, dropY);
    }

    private Point calcDropPos(Point initial, Point fallback) {
        Point ret = calcPointBelow(new Point(initial.x, initial.y - 99));
        if (ret == null) {
            return fallback;
        }
        return ret;
    }

    protected final void dropFromMonster(MapleCharacter dropOwner, MapleMonster monster) {
        if (monster.dropsDisabled() || dropRate < 1 || !dropsOn) {
            return;
        }
        final boolean isBoss = monster.isBoss();
        int maxDrops = isBoss ? 10 * bossDropRate : 5 * dropRate * dropOwner.getDropRate();
        if (monster.getId() > 9300183 && monster.getId() < 9300216) {
            maxDrops = 1;
        } else if (monster.getId() > 9300215 && monster.getId() < 9300271) {
            maxDrops = 2;
        }
        List<Integer> toDrop = new ArrayList<Integer>();
        for (int i = 0; i < maxDrops; i++) {
            toDrop.add(monster.getDrop());
        }
        Set<Integer> alreadyDropped = new HashSet<Integer>();
        int htpendants = 0;
        int htstones = 0;
        int mesos = 0;
        for (int i = 0; i < toDrop.size(); i++) {
            if (toDrop.get(i) == -1) {
                if (alreadyDropped.contains(-1)) {
                    if (!isBoss) {
                        toDrop.remove(i);
                        i--;
                    } else if (mesos < 7) {
                        mesos++;
                    } else {
                        toDrop.remove(i);
                        i--;
                    }
                } else {
                    alreadyDropped.add(-1);
                }
            } else if (alreadyDropped.contains(toDrop.get(i)) && !(monster.getId() > 8810000 && monster.getId() < 8810018) && monster.getId() != 8810018 && monster.getId() != 8810026) {
                toDrop.remove(i);
                i--;
            } else {
                if (toDrop.get(i) == 2041200 || toDrop.get(i) == 1122000) {// stone, pendant
                    if (htstones > 2) {
                        toDrop.remove(i);
                        toDrop.add(monster.getDrop());
                        i--;
                        continue;
                    } else {
                        if (toDrop.get(i) == 2041200) {
                            htstones++;
                        } else {
                            htpendants++;
                        }
                    }
                }
                alreadyDropped.add(toDrop.get(i));
            }
        }
        if (toDrop.size() > maxDrops) {
            toDrop = toDrop.subList(0, maxDrops);
        }
        if (mesos < 7 && isBoss) {
            for (int i = mesos; i < 7; i++) {
                toDrop.add(-1);
            }
        }
        Point[] toPoint = new Point[toDrop.size()];
        int shiftDirection = 0;
        int shiftCount = 0;
        int curX = Math.min(Math.max(monster.getPosition().x - 25 * (toDrop.size() / 2), footholds.getMinDropX() + 25), footholds.getMaxDropX() - toDrop.size() * 25);
        int curY = Math.max(monster.getPosition().y, footholds.getY1());
        while (shiftDirection < 3 && shiftCount < 1000) {
            if (shiftDirection == 1) {
                curX += 25;
            } else if (shiftDirection == 2) {
                curX -= 25;
            }
            for (int i = 0; i < toDrop.size(); i++) {
                MapleFoothold wall = footholds.findWall(new Point(curX, curY), new Point(curX + toDrop.size() * 25, curY));
                if (wall != null) {
                    if (wall.getX1() < curX) {
                        shiftDirection = 1;
                        shiftCount++;
                        break;
                    } else if (wall.getX1() == curX) {
                        if (shiftDirection == 0) {
                            shiftDirection = 1;
                        }
                        shiftCount++;
                        break;
                    } else {
                        shiftDirection = 2;
                        shiftCount++;
                        break;
                    }
                } else if (i == toDrop.size() - 1) {
                    shiftDirection = 3;
                }
                final Point dropPos = calcDropPos(new Point(curX + i * 25, curY), new Point(monster.getPosition()));
                toPoint[i] = new Point(curX + i * 25, curY);
                final int drop = toDrop.get(i);
                if (drop == -1) { // meso
                    double mesoDecrease = Math.pow(0.98, monster.getExp() / 300.0);
                    if (mesoDecrease > 1.0) {
                        mesoDecrease = 1.0;
                    }
                    int tempmeso = Math.min(50000, (int) (mesoDecrease * (monster.getExp()) * (1.0 + new Random().nextInt(20)) / 10.0));
                    if (dropOwner.getBuffedValue(MapleBuffStat.MESOUP) != null) {
                        tempmeso = (int) (tempmeso * dropOwner.getBuffedValue(MapleBuffStat.MESOUP).doubleValue() / 100.0);
                    }
                    final int meso = tempmeso * dropOwner.getMesoRate();
                    if (meso > 0 && (mapid < 925020000 || mapid > 925030000) && mapid != 910010000) { // Dojo Maps, HPQ Map
                        final MapleCharacter dropChar = dropOwner;
                        final Point dropperPos = monster.getPosition();
                        final int dropperId = monster.getObjectId();
                        TimerManager.getInstance().schedule(new Runnable() {

                            public void run() {
                                spawnMesoDrop(meso, meso, dropPos, dropperId, dropperPos, dropChar, isBoss);
                            }
                        }, monster.getAnimationTime("die1"));
                    }
                } else {
                    IItem idrop;
                    MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                    if (ii.getInventoryType(drop).equals(MapleInventoryType.EQUIP)) {
                        idrop = ii.randomizeStats((Equip) ii.getEquipById(drop));
                    } else {
                        idrop = new Item(drop, (byte) 0, (short) 1);
                        if (InventoryConstants.isArrowForBow(drop) || InventoryConstants.isArrowForCrossBow(drop)) {
                            idrop.setQuantity((short) (1 + Randomizer.getInstance().nextInt(101)));
                        } else if (InventoryConstants.isRechargable(drop)) {
                            idrop.setQuantity((short) (1));
                        }
                    }

                    final MapleCharacter dropChar = dropOwner;
                    final TimerManager tMan = TimerManager.getInstance();
                    final int monsterId = monster.getObjectId();
                    final Point dropPosition = monster.getPosition();
                    final MapleMapItem mdrop = new MapleMapItem(idrop, dropPos, monsterId, dropPosition, dropOwner);
                    tMan.schedule(new Runnable() {

                        public void run() {
                            spawnAndAddRangedMapObject(mdrop, new DelayedPacketCreation() {

                                public void sendPackets(MapleClient c) {
                                    c.getSession().write(MaplePacketCreator.dropItemFromMapObject(drop, mdrop.getObjectId(), monsterId, isBoss ? 0 : dropChar.getId(), dropPosition, dropPos, (byte) 1));
                                }
                            });
                            tMan.schedule(new ExpireMapItemJob(mdrop), dropLife);
                        }
                    }, monster.getAnimationTime("die1"));
                }
            }
        }
    }
public int playerCount() {
        List<MapleMapObject> players = getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Arrays.asList(MapleMapObjectType.PLAYER));
        return players.size();
    }

    public int mobCount() {
        List<MapleMapObject> mobsCount = getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Arrays.asList(MapleMapObjectType.MONSTER));
        return mobsCount.size();
    }
    public int getMonsterCount() {
                List<MapleMapObject> monsters = getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Arrays.asList(MapleMapObjectType.MONSTER));
                return monsters.size();
        }

    public MapleMonster getMonsterById(int id) {
        objectlock.readLock().lock();
        try {
            for (MapleMapObject obj : mapobjects.values()) {
                if (obj.getType() == MapleMapObjectType.MONSTER) {
                    if (((MapleMonster) obj).getId() == id) {
                        return (MapleMonster) obj;
                    }
                }
            }
        } finally {
            objectlock.readLock().unlock();
        }
        return null;
    }

    public int countMonster(int id) {
        int count = 0;
        for (MapleMapObject m : getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Arrays.asList(MapleMapObjectType.MONSTER))) {
            if (((MapleMonster) m).getId() == id) {
                count++;
            }
        }
        return count;
    }

    public boolean damageMonster(MapleCharacter chr, MapleMonster monster, int damage) {
        if (monster.getId() == 8800000) {
            for (MapleMapObject object : chr.getMap().getMapObjects()) {
                MapleMonster mons = chr.getMap().getMonsterByOid(object.getObjectId());
                if (mons != null) {
                    if (mons.getId() >= 8800003 && mons.getId() <= 8800010) {
                        return true;
                    }
                }
            }
        }
        if (monster.isAlive()) {
            boolean killMonster_bool = false;
            monster.monsterLock.lock();
            try {
                if (!monster.isAlive()) {
                    return false;
                }
                if (damage > 0) {
                    int monsterhp = monster.getHp();
                    monster.damage(chr, damage, true);
                    if (!monster.isAlive()) { // monster just died
                        //    killMonster(monster, chr, true);
                        killMonster_bool = true;
                        if (monster.getId() >= 8810002 && monster.getId() <= 8810009) {
                            for (MapleMapObject object : chr.getMap().getMapObjects()) {
                                MapleMonster mons = chr.getMap().getMonsterByOid(object.getObjectId());
                                if (mons != null) {
                                    if (mons.getId() == 8810018 || mons.getId() == 8810026) {
                                        damageMonster(chr, mons, monsterhp);
                                    }
                                }
                            }
                        }
                    } else if (monster.getId() >= 8810002 && monster.getId() <= 8810009) {//zakum
                        for (MapleMapObject object : chr.getMap().getMapObjects()) {
                            MapleMonster mons = chr.getMap().getMonsterByOid(object.getObjectId());
                            if (mons != null) {
                                if (mons.getId() == 8810018 || mons.getId() == 8810026) {
                                    damageMonster(chr, mons, damage);
                                }
                            }
                        }
                    }
                } //todo: horntail exception here
            } finally {
                monster.monsterLock.unlock();
            }

            if (killMonster_bool && monster != null) {
                killMonster(monster, chr, true);
                removeFromAllCharsVisible(monster);
                monster.empty();
                nullifyObject(monster);
            }
            return true;
        }
        return false;
    }

    public void killMonster(final MapleMonster monster, final MapleCharacter chr, final boolean withDrops) {
        killMonster(monster, chr, withDrops, false, 1);
    }

    public void killMonster(final MapleMonster monster, final MapleCharacter chr, final boolean withDrops, final boolean secondTime, int animation) {
        MapleItemInformationProvider mii = MapleItemInformationProvider.getInstance();
        if (monster.getId() == 8810018 && !secondTime) {
            TimerManager.getInstance().schedule(new Runnable() {

                @Override
                public void run() {
                    killMonster(monster, chr, withDrops, true, 1);
                    killAllMonsters();
                }
            }, 3000);
            return;
        }
        if (chr.getOccupation().isA(MapleOccupations.Aphrodite)) {
            chr.gainNX(randhum);
        }
        if (monster.getBuffToGive() > -1) {
            for (MapleMapObject mmo : this.getAllPlayer()) {
                MapleCharacter character = (MapleCharacter) mmo;
                if (character.isAlive()) {
                    mii.getItemEffect(monster.getBuffToGive()).applyTo(character);
                }
            }
        }
        spawnedMonstersOnMap.decrementAndGet();
        monster.setHp(0);
        broadcastMessage(MaplePacketCreator.killMonster(monster.getObjectId(), animation), monster.getPosition());
        removeMapObject(monster);
        if (monster.getId() >= 8800003 && monster.getId() <= 8800010) {
            boolean makeZakReal = true;
            Collection<MapleMapObject> objects = getMapObjects();
            for (MapleMapObject object : objects) {
                MapleMonster mons = getMonsterByOid(object.getObjectId());
                if (mons != null) {
                    if (mons.getId() >= 8800003 && mons.getId() <= 8800010) {
                        makeZakReal = false;
                        break;
                    }
                }
            }
            if (makeZakReal) {
                for (MapleMapObject object : objects) {
                    MapleMonster mons = chr.getMap().getMonsterByOid(object.getObjectId());
                    if (mons != null) {
                        if (mons.getId() == 8800000) {
                            makeMonsterReal(mons);
                            updateMonsterController(mons);
                            break;
                        }
                    }
                }
            }
            /*} else if (monster.getId() == 8810018 || monster.getId() == 8810026) {
            for (ChannelServer cserv : ChannelServer.getAllInstances()) {
            for (MapleCharacter player : cserv.getPlayerStorage().getAllCharacters()) {
            if (player.getMapId() == 240000000) {
            player.message("Mysterious power arose as I heard the powerful cry of the Nine Spirit Baby Dragon.");
            player.getClient().getSession().write(MaplePacketCreator.showOwnBuffEffect(2022109, 13)); // The Breath of Nine Spirit
            player.getMap().broadcastMessage(player, MaplePacketCreator.showBuffeffect(player.getId(), 2022109, 13), false); // The Breath of Nine Spirit
            mii.getItemEffect(2022109).applyTo(player);
            } else {
            player.dropMessage("To the crew that have finally conquered Horned Tail after numerous attempts, I salute thee! You are the true heroes of Leafre!!");
            if (player.isGM()) {
            player.message("[GM-Message] Horntail was killed by : " + chr.getName());
            }
            }
            }
            }*/
        } else if (monster.getId() == 8820001) {
            chr.getClient().getSession().write(MaplePacketCreator.showOwnBuffEffect(2022449, 11));
            broadcastMessage(chr, MaplePacketCreator.showBuffeffect(chr.getId(), 2022449, 11), false);
        }
        MapleCharacter dropOwner = monster.killBy(chr);
        if (withDrops && !monster.dropsDisabled()) {
            if (dropOwner == null) {
                dropOwner = chr;
            }
            dropFromMonster(dropOwner, monster);
        }
    }

    public void killMonster(int monsId) {
        for (MapleMapObject mmo : getMapObjects()) {
            if (mmo instanceof MapleMonster) {
                if (((MapleMonster) mmo).getId() == monsId) {
                    this.killMonster((MapleMonster) mmo, (MapleCharacter) getAllPlayer().get(0), false);
                }
            }
        }
    }
    public final List<MapleMapObject> getAllMonster() {
        return getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Arrays.asList(MapleMapObjectType.MONSTER));
    } 

    public void killAllMonsters() {
        for (MapleMapObject monstermo : getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Arrays.asList(MapleMapObjectType.MONSTER))) {
            MapleMonster monster = (MapleMonster) monstermo;
            spawnedMonstersOnMap.decrementAndGet();
            monster.setHp(0);
            broadcastMessage(MaplePacketCreator.killMonster(monster.getObjectId(), true), monster.getPosition());
            removeMapObject(monster);
            monster.empty();
            nullifyObject(monster);
        }
    }
public     List<MapleMapObject> getCharactersAsMapObjects() {
                  return getMapObjectsInRange(new Point(0,0), Double.POSITIVE_INFINITY, Arrays.asList(MapleMapObjectType.PLAYER));
        } 

    public void killAllMonstersSpecialMode() {
        for (MapleMapObject monstermo : getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Arrays.asList(MapleMapObjectType.MONSTER))) {
            MapleMonster monster = (MapleMonster) monstermo;
            spawnedMonstersOnMap.decrementAndGet();
            monster.setHp(0);
            broadcastMessage(MaplePacketCreator.killMonster(monster.getObjectId(), true), monster.getPosition());
            monster.empty();
            nullifyObject(monster);
        }
    }

    public List<MapleMapObject> getAllPlayer() {
        return getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Arrays.asList(MapleMapObjectType.PLAYER));
    }

    public void destroyReactor(int oid) {
        final MapleReactor reactor = getReactorByOid(oid);
        TimerManager tMan = TimerManager.getInstance();
        broadcastMessage(MaplePacketCreator.destroyReactor(reactor));
        reactor.setAlive(false);
        removeMapObject(reactor);
        reactor.setTimerActive(false);
        if (reactor.getDelay() > 0) {
            tMan.schedule(new Runnable() {

                @Override
                public void run() {
                    respawnReactor(reactor);
                }
            }, reactor.getDelay());
        }
    }

    public void resetReactors() {
        objectlock.readLock().lock();
        try {
            for (MapleMapObject o : mapobjects.values()) {
                if (o.getType() == MapleMapObjectType.REACTOR) {
                    ((MapleReactor) o).setState((byte) 0);
                    ((MapleReactor) o).setTimerActive(false);
                    broadcastMessage(MaplePacketCreator.triggerReactor((MapleReactor) o, 0));
                }
            }
        } finally {
            objectlock.readLock().unlock();
        }
    }

    public void shuffleReactors() {
        List<Point> points = new ArrayList<Point>();
        objectlock.readLock().lock();
        try {
            for (MapleMapObject o : mapobjects.values()) {
                if (o.getType() == MapleMapObjectType.REACTOR) {
                    points.add(((MapleReactor) o).getPosition());
                }
            }
            Collections.shuffle(points);
            for (MapleMapObject o : mapobjects.values()) {
                if (o.getType() == MapleMapObjectType.REACTOR) {
                    ((MapleReactor) o).setPosition(points.remove(points.size() - 1));
                }
            }
        } finally {
            objectlock.readLock().unlock();
        }
    }

    public MapleReactor getReactorById(int Id) {
        objectlock.readLock().lock();
        try {
            for (MapleMapObject obj : mapobjects.values()) {
                if (obj.getType() == MapleMapObjectType.REACTOR) {
                    if (((MapleReactor) obj).getId() == Id) {
                        return (MapleReactor) obj;
                    }
                }
            }
            return null;
        } finally {
            objectlock.readLock().unlock();
        }
    }

    /**
     * Automagically finds a new controller for the given monster from the chars on the map...
     *
     * @param monster
     */
    public void updateMonsterController(MapleMonster monster) {
        monster.monsterLock.lock();
        try {
            if (!monster.isAlive()) {
                return;
            }
            if (monster.getController() != null) {
                if (monster.getController().getMap() != this) {
                    monster.getController().stopControllingMonster(monster);
                } else {
                    return;
                }
            }
            int mincontrolled = -1;
            MapleCharacter newController = null;
            characterlock.readLock().lock();
            try {
                for (MapleCharacter chr : characters) {
                    if (!chr.isHidden() && (chr.getControlledMonsters().size() < mincontrolled || mincontrolled == -1)) {
                        mincontrolled = chr.getControlledMonsters().size();
                        newController = chr;
                    }
                }
            } finally {
                characterlock.readLock().unlock();
            }
            if (newController != null) {// was a new controller found? (if not no one is on the map)
                if (monster.isFirstAttack()) {
                    newController.controlMonster(monster, true);
                    monster.setControllerHasAggro(true);
                    monster.setControllerKnowsAboutAggro(true);
                } else {
                    newController.controlMonster(monster, false);
                }
            }
        } finally {
            monster.monsterLock.unlock();
        }
    }

    public Collection<MapleMapObject> getMapObjects() {
        return Collections.unmodifiableCollection(mapobjects.values());
    }

    public boolean containsNPC(int npcid) {
        objectlock.readLock().lock();
        try {
            for (MapleMapObject obj : mapobjects.values()) {
                if (obj.getType() == MapleMapObjectType.NPC) {
                    if (((MapleNPC) obj).getId() == npcid) {
                        return true;
                    }
                }
            }
        } finally {
            objectlock.readLock().unlock();
        }
        return false;
    }

    public MapleMapObject getMapObject(int oid) {
        return mapobjects.get(oid);
    }

    /**
     * returns a monster with the given oid, if no such monster exists returns null
     *
     * @param oid
     * @return
     */
    public MapleMonster getMonsterByOid(int oid) {
        MapleMapObject mmo = getMapObject(oid);
        if (mmo == null) {
            return null;
        }
        if (mmo.getType() == MapleMapObjectType.MONSTER) {
            return (MapleMonster) mmo;
        }
        return null;
    }

    public MapleReactor getReactorByOid(int oid) {
        MapleMapObject mmo = getMapObject(oid);
        if (mmo == null) {
            return null;
        }
        return mmo.getType() == MapleMapObjectType.REACTOR ? (MapleReactor) mmo : null;
    }

    public MapleReactor getReactorByName(String name) {
        objectlock.readLock().lock();
        try {
            for (MapleMapObject obj : mapobjects.values()) {
                if (obj.getType() == MapleMapObjectType.REACTOR) {
                    if (((MapleReactor) obj).getName().equals(name)) {
                        return (MapleReactor) obj;
                    }
                }
            }
        } finally {
            objectlock.readLock().unlock();
        }
        return null;
    }

    public void spawnMonsterOnGroudBelow(MapleMonster mob, Point pos) {
        spawnMonsterOnGroundBelow(mob, pos);
    }

    public void spawnMonsterOnGroundBelow(MapleMonster mob, Point pos) {
        Point spos = new Point(pos.x, pos.y - 1);
        spos = calcPointBelow(spos);
        spos.y--;
        mob.setPosition(spos);
        spawnMonster(mob);
    }
//  public void spawnItemDrop(final int dropperId, final Point dropperPos, final MapleCharacter owner, final IItem item, Point pos, final boolean ffaDrop, final boolean expire) {

    private void monsterItemDrop(final MapleMonster m, final IItem item, long delay) {
        if (item.getItemId() == 4001101) { // moonbunny makes one instant it is spawned
            MapleMap.this.broadcastMessage(MaplePacketCreator.serverNotice(6, "The Moon Bunny made rice cake number " + (++MapleMap.this.riceCakeNum)));
            spawnItemDrop(m.getObjectId(), m.getPosition(), null, item, m.getPosition(), true, true);
        }
        final ScheduledFuture<?> monsterItemDrop = TimerManager.getInstance().register(new Runnable() {

            @Override
            public void run() {
                if (MapleMap.this.getMonsterById(m.getId()) != null) {
                    if (item.getItemId() == 4001101) {
                        MapleMap.this.broadcastMessage(MaplePacketCreator.serverNotice(6, "The Moon Bunny made rice cake number " + (++MapleMap.this.riceCakeNum)));
                    }
                    spawnItemDrop(m.getObjectId(), m.getPosition(), null, item, m.getPosition(), true, true);
                }
            }
        }, delay, delay);
        if (getMonsterById(m.getId()) == null) {
            monsterItemDrop.cancel(false);
        }
    }

    public void spawnFakeMonsterOnGroundBelow(MapleMonster mob, Point pos) {
        Point spos = getGroundBelow(pos);
        mob.setPosition(spos);
        spawnFakeMonster(mob);
    }

    public Point getGroundBelow(Point pos) {
        Point spos = new Point(pos.x, pos.y - 1);
        spos = calcPointBelow(spos);
        spos.y--;
        return spos;
    }

    public void spawnRevives(final MapleMonster monster) {
        monster.setMap(this);
        //no need for synchronization here as it is handled within spawnandaddrangedmapobject - taken out
        // synchronized (this.mapobjects) {
        spawnAndAddRangedMapObject(monster, new DelayedPacketCreation() {

            public void sendPackets(MapleClient c) {
                c.getSession().write(MaplePacketCreator.spawnMonster(monster, false));
            }
        });
        updateMonsterController(monster);
        //}
        spawnedMonstersOnMap.incrementAndGet();
    }

    public void spawnMonster(final MapleMonster monster) {
        monster.setMap(this);
        //same here
        //   synchronized (this.mapobjects) {
        spawnAndAddRangedMapObject(monster, new DelayedPacketCreation() {

            public void sendPackets(MapleClient c) {
                c.getSession().write(MaplePacketCreator.spawnMonster(monster, true));
                if (monster.getId() == 9300166 || monster.getId() == 8810026) {
                    TimerManager.getInstance().schedule(new Runnable() {

                        @Override
                        public void run() {
                            killMonster(monster, (MapleCharacter) getAllPlayer().get(0), false, false, 4);
                        }
                    }, 4500 + Randomizer.getInstance().nextInt(500));
                }
            }
        }, null);
        updateMonsterController(monster);
        // }
        if (monster.getDropPeriodTime() > 0) { //9300102 - Watchhog, 9300061 - Moon Bunny (HPQ)
            if (monster.getId() == 9300102) {
                monsterItemDrop(monster, new Item(4031507, (byte) 0, (short) 1), monster.getDropPeriodTime());
            } else if (monster.getId() == 9300061) {
                monsterItemDrop(monster, new Item(4001101, (byte) 0, (short) 1), monster.getDropPeriodTime() / 3);
            } else {
                System.out.println("UNCODED TIMED MOB DETECTED: " + monster.getId());
            }
        }
        spawnedMonstersOnMap.incrementAndGet();
    }

    public void spawnDojoMonster(final MapleMonster monster) {
        TimerManager.getInstance().schedule(new Runnable() {

            public void run() {
                Point[] pts = {new Point(140, 0), new Point(190, 7), new Point(187, 7)};
                spawnMonsterWithEffect(monster, 15, pts[Randomizer.getInstance().nextInt(3)]);
            }
        }, 3000);
    }

    public void spawnMonsterWithEffect(final MapleMonster monster, final int effect, Point pos) {
        monster.setMap(this);
        Point spos = new Point(pos.x, pos.y - 1);
        spos = calcPointBelow(spos);
        spos.y--;
        monster.setPosition(spos);
        if (mapid < 925020000 || mapid > 925030000) {
            monster.disableDrops();
        }
        //  synchronized (this.mapobjects) {
        spawnAndAddRangedMapObject(monster, new DelayedPacketCreation() {

            public void sendPackets(MapleClient c) {
                c.getSession().write(MaplePacketCreator.spawnMonster(monster, true, effect));
            }
        });
        if (monster.hasBossHPBar()) {
            broadcastMessage(monster.makeBossHPBarPacket(), monster.getPosition());
        }
        updateMonsterController(monster);
        // }
        spawnedMonstersOnMap.incrementAndGet();
    }

    public void spawnFakeMonster(final MapleMonster monster) {
        monster.setMap(this);
        monster.setFake(true);
        //    synchronized (this.mapobjects) {
        spawnAndAddRangedMapObject(monster, new DelayedPacketCreation() {

            public void sendPackets(MapleClient c) {
                c.getSession().write(MaplePacketCreator.spawnFakeMonster(monster, 0));
            }
        });
        //     }
        spawnedMonstersOnMap.incrementAndGet();
    }

    public void makeMonsterReal(final MapleMonster monster) {
        monster.setFake(false);
        broadcastMessage(MaplePacketCreator.makeMonsterReal(monster));
        updateMonsterController(monster);
    }

    public void spawnReactor(final MapleReactor reactor) {
        reactor.setMap(this);
        //     synchronized (this.mapobjects) {
        spawnAndAddRangedMapObject(reactor, new DelayedPacketCreation() {

            public void sendPackets(MapleClient c) {
                c.getSession().write(reactor.makeSpawnData());
            }
        });
        //   }
    }

    private void respawnReactor(final MapleReactor reactor) {
        reactor.setState((byte) 0);
        reactor.setAlive(true);
        spawnReactor(reactor);
    }

    public void spawnDoor(final MapleDoor door) {
        //     synchronized (this.mapobjects) {
        spawnAndAddRangedMapObject(door, new DelayedPacketCreation() {

            public void sendPackets(MapleClient c) {
                c.getSession().write(MaplePacketCreator.spawnDoor(door.getOwner().getId(), door.getTargetPosition(), false));
                if (door.getOwner().getParty() != null && (door.getOwner() == c.getPlayer() || door.getOwner().getParty().containsMembers(new MaplePartyCharacter(c.getPlayer())))) {
                    c.getSession().write(MaplePacketCreator.partyPortal(door.getTown().getId(), door.getTarget().getId(), door.getTargetPosition()));
                }
                c.getSession().write(MaplePacketCreator.spawnPortal(door.getTown().getId(), door.getTarget().getId(), door.getTargetPosition()));
                c.getSession().write(MaplePacketCreator.enableActions());
            }
        }, new SpawnCondition() {

            public boolean canSpawn(MapleCharacter chr) {
                return chr.getMapId() == door.getTarget().getId() || chr == door.getOwner() && chr.getParty() == null;
            }
        });
        //      }
    }

    public List<MapleCharacter> getPlayersInRange(Rectangle box, List<MapleCharacter> chr) { //what was whoever who wrote this THINKING!? needs synch
        List<MapleCharacter> character = new LinkedList<MapleCharacter>();
        characterlock.readLock().lock();
        try {
            for (MapleCharacter a : characters) {
                if (chr.contains(a.getClient().getPlayer())) {
                    if (box.contains(a.getPosition())) {
                        character.add(a);
                    }
                }
            }
            return character;
        } finally {
            characterlock.readLock().unlock();
        }
    }

    public void spawnSummon(final MapleSummon summon) {
        spawnAndAddRangedMapObject(summon, new DelayedPacketCreation() {

            public void sendPackets(MapleClient c) {
                if (summon != null) {
                    c.getSession().write(MaplePacketCreator.spawnSpecialMapObject(summon, summon.getOwner().getSkillLevel(SkillFactory.getSkill(summon.getSkill())), true));
                }
            }
        }, null);
    }

    public void spawnMist(final MapleMist mist, final int duration, boolean poison, boolean fake) {
        addMapObject(mist);
        broadcastMessage(fake ? mist.makeFakeSpawnData(30) : mist.makeSpawnData());
        TimerManager tMan = TimerManager.getInstance();
        final ScheduledFuture<?> poisonSchedule;
        if (poison) {
            Runnable poisonTask = new Runnable() {

                @Override
                public void run() {
                    for (MapleMapObject mo : getMapObjectsInBox(mist.getBox(), Collections.singletonList(MapleMapObjectType.MONSTER))) {
                        if (mist.makeChanceResult()) {
                            ((MapleMonster) mo).applyStatus(mist.getOwner(), new MonsterStatusEffect(Collections.singletonMap(MonsterStatus.POISON, 1), mist.getSourceSkill(), false), true, duration);
                        }
                    }
                }
            };
            poisonSchedule = tMan.register(poisonTask, 2000, 2500);
        } else {
            poisonSchedule = null;
        }
        tMan.schedule(new Runnable() {

            @Override
            public void run() {
                removeMapObject(mist);
                if (poisonSchedule != null) {
                    poisonSchedule.cancel(false);
                }
                broadcastMessage(mist.makeDestroyData());
            }
        }, duration);
    }

    public void disappearingItemDrop(final int dropperId, final Point dropperPosition, final MapleCharacter owner, final IItem item, Point pos) {
        final Point droppos = calcDropPos(pos, pos);
        final MapleMapItem drop = new MapleMapItem(item, droppos, dropperId, dropperPosition, owner);
        broadcastMessage(MaplePacketCreator.dropItemFromMapObject(item.getItemId(), drop.getObjectId(), 0, 0, dropperPosition, droppos, (byte) 3), drop.getPosition());
    }

    public void spawnItemDrop(final int dropperId, final Point dropperPos, final MapleCharacter owner, final IItem item, Point pos, final boolean ffaDrop, final boolean expire) {
        final Point droppos = calcDropPos(pos, pos);
        final MapleMapItem drop = new MapleMapItem(item, droppos, dropperId, dropperPos, owner);
        spawnAndAddRangedMapObject(drop, new DelayedPacketCreation() {

            public void sendPackets(MapleClient c) {
                c.getSession().write(MaplePacketCreator.dropItemFromMapObject(item.getItemId(), drop.getObjectId(), 0, ffaDrop ? 0 : owner.getId(), dropperPos, droppos, (byte) 1));
            }
        });
        broadcastMessage(MaplePacketCreator.dropItemFromMapObject(item.getItemId(), drop.getObjectId(), 0, ffaDrop ? 0 : owner.getId(), dropperPos, droppos, (byte) 0), drop.getPosition());
        if (expire) {
            TimerManager.getInstance().schedule(new ExpireMapItemJob(drop), dropLife);
        }
        activateItemReactors(drop);
    }

    private void activateItemReactors(MapleMapItem drop) {
        IItem item = drop.getItem();
        final TimerManager tMan = TimerManager.getInstance();
        for (MapleMapObject o : mapobjects.values()) {
            if (o.getType() == MapleMapObjectType.REACTOR) {
                if (((MapleReactor) o).getReactorType() == 100) {
                    if (((MapleReactor) o).getReactItem().getLeft() == item.getItemId() && ((MapleReactor) o).getReactItem().getRight() <= item.getQuantity()) {
                        Rectangle area = ((MapleReactor) o).getArea();
                        if (area.contains(drop.getPosition())) {
                            MapleClient ownerClient = null;
                            if (drop.getOwner() != null) {
                                ownerClient = drop.getOwner().getClient();
                            }
                            MapleReactor reactor = (MapleReactor) o;
                            if (!reactor.isTimerActive()) {
                                tMan.schedule(new ActivateItemReactor(drop, reactor, ownerClient), 5000);
                                reactor.setTimerActive(true);
                            }
                        }
                    }
                }
            }
        }
    }

    public void spawnMesoDrop(final int meso, final int displayMeso, Point position, final int dropperId, final Point dropperPos, final MapleCharacter owner, final boolean ffaLoot) {
        final Point droppos = calcDropPos(position, position);
        final MapleMapItem mdrop = new MapleMapItem(meso, displayMeso, droppos, dropperId, dropperPos, owner);
        spawnAndAddRangedMapObject(mdrop, new DelayedPacketCreation() {

            public void sendPackets(MapleClient c) {
                c.getSession().write(MaplePacketCreator.dropMesoFromMapObject(displayMeso, mdrop.getObjectId(), dropperId, ffaLoot ? 0 : owner.getId(), dropperPos, droppos, (byte) 1));
            }
        });
        TimerManager.getInstance().schedule(new ExpireMapItemJob(mdrop), dropLife);
    }

    public void startMapEffect(String msg, int itemId) {
        startMapEffect(msg, itemId, 30000);
    }

    public void startMapEffect(String msg, int itemId, long time) {
        if (mapEffect != null) {
            return;
        }
        mapEffect = new MapleMapEffect(msg, itemId);
        broadcastMessage(mapEffect.makeStartData());
        TimerManager.getInstance().schedule(new Runnable() {

            @Override
            public void run() {
                broadcastMessage(mapEffect.makeDestroyData());
                mapEffect = null;
            }
        }, time);
    }

    private void handlePets(MapleCharacter chr, boolean hide) {
        if (hide) {
            broadcastGMMessage(chr, MaplePacketCreator.spawnPlayerMapobject(chr), false);//NPE here for changing maps??
        } else {
            broadcastMessage(chr, MaplePacketCreator.spawnPlayerMapobject(chr), false);
        }
        MaplePet[] pets = chr.getPets();
        for (int i = 0; i < chr.getPets().length; i++) {
            if (pets[i] != null && chr.getPosition() != null) {
                pets[i].setPos(getGroundBelow(chr.getPosition()));
                if (hide) {
                    broadcastGMMessage(chr, MaplePacketCreator.showPet(chr, pets[i], false, false), false);
                } else {
                    broadcastMessage(chr, MaplePacketCreator.showPet(chr, pets[i], false, false), false);
                }
            } else {
                break;
            }
        }
    }

    public void addPlayer(final MapleCharacter chr) {
        characterlock.writeLock().lock();
        try {
            this.characters.add(chr);
        } finally {
            characterlock.writeLock().unlock();
        }
        //  synchronized (this.mapobjects) { wrong place for this
        if (!onFirstUserEnter.equals("") && !chr.hasEntered(onFirstUserEnter, mapid) && MapScriptManager.getInstance().scriptExists(onFirstUserEnter, true)) {
            if (getAllPlayer().size() <= 1) {
                chr.enteredScript(onFirstUserEnter, mapid);
                MapScriptManager.getInstance().getMapScript(chr.getClient(), onFirstUserEnter, true);
            }
        }
        if (!onUserEnter.equals("")) {
            if (onUserEnter.equals("cygnusTest") && (mapid < 913040000 || mapid > 913040006)) {
                chr.saveLocation("CYGNUSINTRO");
            }
            MapScriptManager.getInstance().getMapScript(chr.getClient(), onUserEnter, false);
        }
        if (FieldLimit.CANNOTUSEMOUNTS.check(fieldLimit) && chr.getBuffedValue(MapleBuffStat.MONSTER_RIDING) != null) {
            chr.cancelEffectFromBuffStat(MapleBuffStat.MONSTER_RIDING);
            chr.cancelBuffStats(MapleBuffStat.MONSTER_RIDING);
        }
        if (mapid == 923010000 && getMonsterById(9300102) == null) { // Kenta's Mount Quest
            spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(9300102), new Point(77, 426)); // hog has to be here
        } else if (mapid == 910010000) { // Henesys Party Quest
            startMapEffect("Plant the Primrose Seed around the moon! The Moon Bunnies will start pounding the mill.", 5120016, 7000);
            chr.getClient().getSession().write(MaplePacketCreator.getClock(10 * 60)); // 10 minutes
            TimerManager.getInstance().schedule(new Runnable() {

                @Override
                public void run() {
                    if (chr.getMapId() == 910010000) {
                        chr.getClient().getPlayer().changeMap(chr.getClient().getChannelServer().getMapFactory().getMap(910010200));
                    }
                }
            }, 10 * 60 * 1000 + 3000);
        }
        handlePets(chr, chr.isHidden());
        sendObjectPlacement(chr.getClient());
        if (chr.isHidden()) {
            chr.getClient().getSession().write(MaplePacketCreator.getGMEffect(16, (byte) 1));
        }
        MaplePet[] pets = chr.getPets();
        for (int i = 0; i < 3; i++) {
            if (pets[i] != null) {
                pets[i].setPos(getGroundBelow(chr.getPosition()));
                chr.getClient().getSession().write(MaplePacketCreator.showPet(chr, pets[i], false, false));
            } else {
                break;
            }
        }
        if (hasForcedEquip()) {
            chr.getClient().getSession().write(MaplePacketCreator.showForcedEquip());
        }
        this.objectlock.writeLock().lock();
        try {
            this.mapobjects.put(Integer.valueOf(chr.getObjectId()), chr);
        } finally {
            this.objectlock.writeLock().unlock();
        }
        if (chr.getPlayerShop() != null) {
            addMapObject(chr.getPlayerShop());
        }
        MapleStatEffect summonStat = chr.getStatForBuff(MapleBuffStat.SUMMON);
        if (summonStat != null) {
            MapleSummon summon = chr.getSummons().get(summonStat.getSourceId());
            summon.setPosition(chr.getPosition());
            chr.getMap().spawnSummon(summon);
            updateMapObjectVisibility(chr, summon);
        }
        if (mapEffect != null) {
            mapEffect.sendStartData(chr.getClient());
        }
        if (chr.getEnergyBar() >= 10000) {
            broadcastMessage(chr, (MaplePacketCreator.giveForeignEnergyCharge(chr.getId(), 10000)));
        }
        if (getTimeLimit() > 0 && getForcedReturnMap() != null) {
            chr.getClient().getSession().write(MaplePacketCreator.getClock(getTimeLimit()));
            chr.startMapTimeLimitTask(this, this.getForcedReturnMap());
        }
        if (chr.getEventInstance() != null && chr.getEventInstance().isTimerStarted()) {
            chr.getClient().getSession().write(MaplePacketCreator.getClock((int) (chr.getEventInstance().getTimeLeft() / 1000)));
        }
        if (hasClock()) {
            Calendar cal = Calendar.getInstance();
            chr.getClient().getSession().write((MaplePacketCreator.getClockTime(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND))));
        }
        if (hasBoat() == 2) {
            chr.getClient().getSession().write((MaplePacketCreator.boatPacket(true)));
        } else if (hasBoat() == 1 && (chr.getMapId() != 200090000 || chr.getMapId() != 200090010)) {
            chr.getClient().getSession().write(MaplePacketCreator.boatPacket(false));
        }
        chr.receivePartyMemberHP();
    }

    public MaplePortal findClosestPortal(Point from) {
        MaplePortal closest = null;
        double shortestDistance = Double.POSITIVE_INFINITY;
        for (MaplePortal portal : portals.values()) {
            double distance = portal.getPosition().distanceSq(from);
            if (distance < shortestDistance) {
                closest = portal;
                shortestDistance = distance;
            }
        }
        return closest;
    }

    public MaplePortal getRandomSpawnpoint() {
        List<MaplePortal> spawnPoints = new ArrayList<MaplePortal>();
        for (MaplePortal portal : portals.values()) {
            if (portal.getType() >= 0 && portal.getType() <= 2) {
                spawnPoints.add(portal);
            }
        }
        MaplePortal portal = spawnPoints.get(new Random().nextInt(spawnPoints.size()));
        return portal != null ? portal : getPortal(0);
    }

    public void removePlayer(MapleCharacter chr) {
        characterlock.writeLock().lock();
        try {
            characters.remove(chr);
        } finally {
            characterlock.writeLock().unlock();
        }
        removeMapObject(Integer.valueOf(chr.getObjectId()));
        broadcastMessage(MaplePacketCreator.removePlayerFromMap(chr.getId()));
        for (MapleMonster monster : chr.getControlledMonsters()) {
            monster.setController(null);
            monster.setControllerHasAggro(false);
            monster.setControllerKnowsAboutAggro(false);
            updateMonsterController(monster);
        }
        chr.leaveMap();
        chr.cancelMapTimeLimitTask();
        for (MapleSummon summon : chr.getSummons().values()) {
            if (summon.isStationary()) {
                chr.cancelBuffStats(MapleBuffStat.PUPPET);
            } else {
                removeMapObject(summon);
            }
        }
    }

    public void broadcastMessage(MaplePacket packet) {
        broadcastMessage(null, packet, Double.POSITIVE_INFINITY, null);
    }

    /**
     * Nonranged. Repeat to source according to parameter.
     *
     * @param source
     * @param packet
     * @param repeatToSource
     */
    public void broadcastMessage(MapleCharacter source, MaplePacket packet, boolean repeatToSource) {
        broadcastMessage(repeatToSource ? null : source, packet, Double.POSITIVE_INFINITY, source.getPosition());
    }

    /**
     * Ranged and repeat according to parameters.
     *
     * @param source
     * @param packet
     * @param repeatToSource
     * @param ranged
     */
    public void broadcastMessage(MapleCharacter source, MaplePacket packet, boolean repeatToSource, boolean ranged) {
        broadcastMessage(repeatToSource ? null : source, packet, ranged ? 722500 : Double.POSITIVE_INFINITY, source.getPosition());
    }

    /**
     * Always ranged from Point.
     *
     * @param packet
     * @param rangedFrom
     */
    public void broadcastMessage(MaplePacket packet, Point rangedFrom) {
        broadcastMessage(null, packet, 722500, rangedFrom);
    }

    /**
     * Always ranged from point. Does not repeat to source.
     *
     * @param source
     * @param packet
     * @param rangedFrom
     */
    public void broadcastMessage(MapleCharacter source, MaplePacket packet, Point rangedFrom) {
        broadcastMessage(source, packet, 722500, rangedFrom);
    }

    private void broadcastMessage(MapleCharacter source, MaplePacket packet, double rangeSq, Point rangedFrom) {
        //synchronized (characters) {
        this.characterlock.readLock().lock();
        try {
            for (MapleCharacter chr : characters) {
                if (chr != source) {
                    if (chr == null || chr.getClient() == null || chr.getClient().getSession() == null) {
                        this.characterlock.readLock().unlock(); //removeplayer needs a writelock
                        removePlayer(chr);
                        return;
                    }
                    if (rangeSq < Double.POSITIVE_INFINITY) {
                        if (rangedFrom.distanceSq(chr.getPosition()) <= rangeSq) {
                            chr.getClient().getSession().write(packet);
                        }
                    } else {
                        chr.getClient().getSession().write(packet);
                    }
                }
            }
        } finally {
            this.characterlock.readLock().unlock();
        }
    }

    private boolean isNonRangedType(MapleMapObjectType type) {
        switch (type) {
            case NPC:
            case PLAYER:
            case HIRED_MERCHANT:
            case PLAYER_NPC:
            case MIST:
                return true;
        }
        return false;
    }

    private void sendObjectPlacement(MapleClient mapleClient) { //once again this really should be synchronised
        objectlock.readLock().lock();
        try {
            for (MapleMapObject o : mapobjects.values()) {
                if (o == null) {
                    continue;
                }
                if (isNonRangedType(o.getType())) {
                    o.sendSpawnData(mapleClient);
                } else if (o.getType() == MapleMapObjectType.MONSTER) {
                    updateMonsterController((MapleMonster) o);
                }
            }
        } finally {
            objectlock.readLock().unlock();
        }
        MapleCharacter chr = mapleClient.getPlayer();
        if (chr != null) {
            for (MapleMapObject o : getMapObjectsInRange(chr.getPosition(), 722500, rangedMapobjectTypes)) {
                if (o == null) {
                    continue;
                }
                try {
                    if (o.getType() == MapleMapObjectType.REACTOR) {
                        if (((MapleReactor) o).isAlive()) {
                            o.sendSpawnData(chr.getClient());
                            chr.addVisibleMapObject(o);
                        }
                    } else {
                        o.sendSpawnData(chr.getClient());
                        chr.addVisibleMapObject(o);
                    }
                } catch (NullPointerException npe) {
                    this.removeMapObject(o);
                }
            }
        }
    }

    public List<MapleMapObject> getMapObjectsInRange(Point from, double rangeSq, List<MapleMapObjectType> types) {
        List<MapleMapObject> ret = new LinkedList<MapleMapObject>();
        objectlock.writeLock().lock();
        try {
            for (MapleMapObject l : mapobjects.values()) {
                if (types.contains(l.getType())) {
                    if ((l.getPosition() == null)) {
                        this.removeMapObject(l);
                        l = null;
                        continue;
                    }
                    if (from.distanceSq(l.getPosition()) <= rangeSq) {//todo: bug (NPE) here; fixme
                        ret.add(l);
                    }
                }
            }
        } finally {
            objectlock.writeLock().unlock();
        }
        return ret;
    }

    public List<MapleMapObject> getMapObjectsInBox(Rectangle box, List<MapleMapObjectType> types) {
        List<MapleMapObject> ret = new LinkedList<MapleMapObject>();
        objectlock.readLock().lock();
        try {
            for (MapleMapObject l : mapobjects.values()) {
                if (types.contains(l.getType())) {
                    if (box.contains(l.getPosition())) {
                        ret.add(l);
                    }
                }
            }
        } finally {
            objectlock.readLock().unlock();
        }
        return ret;
    }

    public void addPortal(MaplePortal myPortal) {
        portals.put(myPortal.getId(), myPortal);
    }

    public MaplePortal getPortal(String portalname) {
        for (MaplePortal port : portals.values()) {
            if (port.getName().equals(portalname)) {
                return port;
            }
        }
        return null;
    }

    public MaplePortal getPortal(int portalid) {
        return portals.get(portalid);
    }

    public void addMapleArea(Rectangle rec) {
        areas.add(rec);
    }

    public List<Rectangle> getAreas() {
        return new ArrayList<Rectangle>(areas);
    }

    public Rectangle getArea(int index) {
        return areas.get(index);
    }

    public void setFootholds(MapleFootholdTree footholds) {
        this.footholds = footholds;
    }

    public MapleFootholdTree getFootholds() {
        return footholds;
    }

    public void addMonsterSpawn(MapleMonster monster, int mobTime) {
        Point newpos = calcPointBelow(monster.getPosition());
        newpos.y -= 1;
        SpawnPoint sp = new SpawnPoint(monster, newpos, mobTime);
        monsterSpawn.add(sp);
        if (sp.shouldSpawn() || mobTime == -1) {// -1 does not respawn and should not either but force ONE spawn
            sp.spawnMonster(this);
        }
    }

    public float getMonsterRate() {
        return monsterRate;
    }

    public Collection<MapleCharacter> getCharacters() {
        return Collections.unmodifiableCollection(this.characters);
    }

    public MapleCharacter getCharacterById(int id) {
        for (MapleCharacter c : this.characters) {
            if (c.getId() == id) {
                return c;
            }
        }
        return null;
    }

    private void updateMapObjectVisibility(MapleCharacter chr, MapleMapObject mo) {
        if (mo == null) {
            return;
        }
        if (mo.getPosition() == null) {
            chr.removeVisibleMapObject(mo);
            mo = null;
            return;
        }
        if (!chr.isMapObjectVisible(mo)) { // monster entered view range
            if (mo.getType() == MapleMapObjectType.SUMMON || mo.getPosition().distanceSq(chr.getPosition()) <= 722500) {
                chr.addVisibleMapObject(mo);
                mo.sendSpawnData(chr.getClient());
            }
        } else if (mo.getType() != MapleMapObjectType.SUMMON && mo.getPosition().distanceSq(chr.getPosition()) > 722500) {
            chr.removeVisibleMapObject(mo);
            mo.sendDestroyData(chr.getClient());
        }
    }

    public void moveMonster(MapleMonster monster, Point reportedPos) {
        monster.setPosition(reportedPos);
        characterlock.readLock().lock();
        try {
            for (MapleCharacter chr : characters) {
                updateMapObjectVisibility(chr, monster);
            }
        } finally {
            characterlock.readLock().unlock();
        }
    }

    public void movePlayer(MapleCharacter player, Point newPosition) {
        player.setPosition(newPosition);
        Collection<MapleMapObject> visibleObjects = player.getVisibleMapObjects();
        MapleMapObject[] visibleObjectsNow = visibleObjects.toArray(new MapleMapObject[visibleObjects.size()]);
        try {
            for (MapleMapObject mo : visibleObjectsNow) {
                if (mo != null) {
                    if (mapobjects.get(mo.getObjectId()) == mo) {
                        updateMapObjectVisibility(player, mo);
                    } else {
                        player.removeVisibleMapObject(mo);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        for (MapleMapObject mo : getMapObjectsInRange(player.getPosition(), 722500, rangedMapobjectTypes)) {
            if (!player.isMapObjectVisible(mo)) {
                mo.sendSpawnData(player.getClient());
                player.addVisibleMapObject(mo);
            }
        }
    }

    public MaplePortal findClosestSpawnpoint(Point from) {
        MaplePortal closest = null;
        double shortestDistance = Double.POSITIVE_INFINITY;
        for (MaplePortal portal : portals.values()) {
            double distance = portal.getPosition().distanceSq(from);
            if (portal.getType() >= 0 && portal.getType() <= 2 && distance < shortestDistance && portal.getTargetMapId() == 999999999) {
                closest = portal;
                shortestDistance = distance;
            }
        }
        return closest;
    }

    public Collection<MaplePortal> getPortals() {
        return Collections.unmodifiableCollection(portals.values());
    }

    public String getMapName() {
        return mapName;
    }

    public void setMapName(String mapName) {
        this.mapName = mapName;
    }

    public String getStreetName() {
        return streetName;
    }

    public void setClock(boolean hasClock) {
        this.clock = hasClock;
    }

    public boolean hasClock() {
        return clock;
    }

    public void setTown(boolean isTown) {
        this.town = isTown;
    }

    public boolean isTown() {
        return town;
    }

    public void setStreetName(String streetName) {
        this.streetName = streetName;
    }

    public void setEverlast(boolean everlast) {
        this.everlast = everlast;
    }

    public boolean getEverlast() {
        return everlast;
    }

    public int getSpawnedMonstersOnMap() {
        return spawnedMonstersOnMap.get();
    }

	public void killAllMonsters(boolean drop) {
		List<MapleMapObject> players = null;
		if (drop) {
			players = getAllPlayer();
		}
		List<MapleMapObject> monsters = getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Arrays.asList(MapleMapObjectType.MONSTER));
		for (MapleMapObject monstermo : monsters) {
			MapleMonster monster = (MapleMonster) monstermo;
			spawnedMonstersOnMap.decrementAndGet();
			monster.setHp(0);
			broadcastMessage(MaplePacketCreator.killMonster(monster.getObjectId(), true), monster.getPosition());
			removeMapObject(monster);
			if (drop) {
				int random = (int) Math.random() * (players.size());
				dropFromMonster((MapleCharacter) players.get(random), monster);
			}
		}
	}

    public void addMonsterSpawn(int id, int i, Point spawnPos, boolean b) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private class ExpireMapItemJob implements Runnable {

        private MapleMapItem mapitem;

        public ExpireMapItemJob(MapleMapItem mapitem) {
            this.mapitem = mapitem;
        }

        @Override
        public void run() {
            if (mapitem != null && mapitem == getMapObject(mapitem.getObjectId())) {
                mapitem.itemLock.lock();
                try {
                    if (mapitem.isPickedUp()) {
                        return;
                    }
                    MapleMap.this.broadcastMessage(MaplePacketCreator.removeItemFromMap(mapitem.getObjectId(), 0, 0), mapitem.getPosition());
                    mapitem.setPickedUp(true);
                } finally {
                    mapitem.itemLock.unlock();
                    MapleMap.this.removeMapObject(mapitem); //need it to close lock before destroying totally
                    removeFromAllCharsVisible(mapitem);
                    nullifyObject(mapitem);
                }
            }
        }
    }

    private class ActivateItemReactor implements Runnable {

        private MapleMapItem mapitem;
        private MapleReactor reactor;
        private MapleClient c;

        public ActivateItemReactor(MapleMapItem mapitem, MapleReactor reactor, MapleClient c) {
            this.mapitem = mapitem;
            this.reactor = reactor;
            this.c = c;
        }

        @Override
        public void run() {
            if (mapitem != null && mapitem == getMapObject(mapitem.getObjectId())) {
                mapitem.itemLock.lock();
                try {
                    TimerManager tMan = TimerManager.getInstance();
                    if (mapitem.isPickedUp()) {
                        return;
                    }
                    MapleMap.this.broadcastMessage(MaplePacketCreator.removeItemFromMap(mapitem.getObjectId(), 0, 0), mapitem.getPosition());
                    MapleMap.this.removeMapObject(mapitem);
                    reactor.hitReactor(c);
                    reactor.setTimerActive(false);
                    if (reactor.getDelay() > 0) {
                        tMan.schedule(new Runnable() {

                            @Override
                            public void run() {
                                reactor.setState((byte) 0);
                                broadcastMessage(MaplePacketCreator.triggerReactor(reactor, 0));
                            }
                        }, reactor.getDelay());
                    }
                } finally {
                    mapitem.itemLock.unlock();
                }
            }
        }
    }

    public void respawn() {
        if (characters.size() == 0) {
            return;
        }
        int numShouldSpawn = (monsterSpawn.size() - spawnedMonstersOnMap.get()) * Math.round(monsterRate);
        if (numShouldSpawn > 0) {
            List<SpawnPoint> randomSpawn = new ArrayList<SpawnPoint>(monsterSpawn);
            Collections.shuffle(randomSpawn);
            int spawned = 0;
            for (SpawnPoint spawnPoint : randomSpawn) {
                if (spawnPoint.shouldSpawn()) {
                    spawnPoint.spawnMonster(MapleMap.this);
                    spawned++;
                }
                if (spawned >= numShouldSpawn) {
                    break;
                }
            }
        }
    }

    private static interface DelayedPacketCreation {

        void sendPackets(MapleClient c);
    }

    private static interface SpawnCondition {

        boolean canSpawn(MapleCharacter chr);
    }

    public int getHPDec() {
        return decHP;
    }

    public void setHPDec(int delta) {
        decHP = delta;
    }

    public int getHPDecProtect() {
        return protectItem;
    }

    public void setHPDecProtect(int delta) {
        this.protectItem = delta;
    }

    private int hasBoat() {
        return docked ? 2 : (boat ? 1 : 0);
    }

    public void setBoat(boolean hasBoat) {
        this.boat = hasBoat;
    }

    public void setDocked(boolean isDocked) {
        this.docked = isDocked;
    }

    public void broadcastGMMessage(MapleCharacter source, MaplePacket packet, boolean repeatToSource) {
        broadcastGMMessage(repeatToSource ? null : source, packet, Double.POSITIVE_INFINITY, source.getPosition());
    }

    private void broadcastGMMessage(MapleCharacter source, MaplePacket packet, double rangeSq, Point rangedFrom) {
        characterlock.readLock().lock();
        try {
            for (MapleCharacter chr : characters) {
                if (chr != source && chr.isGM()) {
                    if (rangeSq < Double.POSITIVE_INFINITY) {
                        if (rangedFrom.distanceSq(chr.getPosition()) <= rangeSq) {
                            chr.getClient().getSession().write(packet);
                        }
                    } else {
                        chr.getClient().getSession().write(packet);
                    }
                }
            }
        } finally {
            characterlock.readLock().unlock();
        }
    }

    public void broadcastNONGMMessage(MapleCharacter source, MaplePacket packet, boolean repeatToSource) {
        characterlock.readLock().lock();
        try {
            for (MapleCharacter chr : characters) {
                if (chr != source && !chr.isGM()) {
                    chr.getClient().getSession().write(packet);
                }
            }
        } finally {
            characterlock.readLock().unlock();
        }
    }

    public MapleOxQuiz getOx() {
        return ox;
    }

    public void setOx(MapleOxQuiz set) {
        this.ox = set;
    }

    public void setOxQuiz(boolean b) {
        this.isOxQuiz = b;
    }

    public boolean isOxQuiz() {
        return isOxQuiz;
    }

    public void setOnUserEnter(String onUserEnter) {
        this.onUserEnter = onUserEnter;
    }

    public String getOnUserEnter() {
        return onUserEnter;
    }

    public void setOnFirstUserEnter(String onFirstUserEnter) {
        this.onFirstUserEnter = onFirstUserEnter;
    }

    public String getOnFirstUserEnter() {
        return onFirstUserEnter;
    }

    private boolean hasForcedEquip() {
        return fieldType == 81 || fieldType == 82 || fieldType == 1001;
    }

    public void setFieldType(int fieldType) {
        this.fieldType = fieldType;
    }

    public void setTimeMobId(int id) {
        this.timeMobId = id;
    }

    public void setTimeMobMessage(String message) {
        this.timeMobMessage = message;
    }

    public int getTimeMobId() {
        return timeMobId;
    }

    public String getTimeMobMessage() {
        return timeMobMessage;
    }

    public void clearDrops(MapleCharacter player, boolean command) {
        List<MapleMapObject> items = player.getMap().getMapObjectsInRange(player.getPosition(), Double.POSITIVE_INFINITY, Arrays.asList(MapleMapObjectType.ITEM));
        for (MapleMapObject i : items) {
            player.getMap().removeMapObject(i);
            player.getMap().broadcastMessage(MaplePacketCreator.removeItemFromMap(i.getObjectId(), 0, player.getId()));
        }
        if (command) {
            player.message("Items Destroyed: " + items.size());
        }
    }

    public void setFieldLimit(int fieldLimit) {
        this.fieldLimit = fieldLimit;
    }

    public int getFieldLimit() {
        return fieldLimit;
    }

    public void resetRiceCakes() {
        this.riceCakeNum = 0;
    }

    public MapleSquad getMapleSquad() {
        if (mapleSquad != null && mapleSquad.getTimer().isCancelled() && characters.size() == 0) {
            mapleSquad = null;
        }
        return mapleSquad;
    }

    public void createMapleSquad(String name) {
        mapleSquad = new MapleSquad(this, name);
    }

    public void deleteMapleSquad() {
        mapleSquad.getTimer().cancel(false);
        mapleSquad = null;
    }

    public MapleCharacter getRandomOpposingPlayer(MapleCharacter original) { //deary deary me...
        characterlock.readLock().lock();
        try {
            for (MapleCharacter mc : characters) {
                if (mc.getParty() != null) {
                    if (mc.getParty().getOpponent().getId() == original.getParty().getId()) {
                        List<MapleCharacter> plist = mc.getClient().getChannelServer().getPartyMembers(mc.getParty(), original.getMapId());
                        return plist.get(Randomizer.getInstance().nextInt(plist.size()));
                    }
                }
            }
            return null;
        } finally {
            characterlock.readLock().unlock();
        }
    }

    public void InitiateRespawnTask() {
        respawnTask = TimerManager.getInstance().register(new Runnable() {

            @Override
            public void run() {
                ChannelServer.getInstance(channel).getMapFactory().getMap(mapid).respawn();
            }
        }, 10000);
    }

    public void updateRates() {
        this.bossDropRate = ChannelServer.getInstance(channel).getBossDropRate();
        this.dropRate = ChannelServer.getInstance(channel).getDropRate();
    }

    private void nullifyObject(MapleMapObject mmobj) {
        mmobj.nullifyPosition();
        mmobj = null;
    }

    private void removeFromAllCharsVisible(MapleMapObject mmo) {
        this.characterlock.readLock().lock();
        try {
            for (MapleCharacter ch : this.characters) {
                try {
                    ch.removeVisibleMapObject(mmo);
                } catch (Exception e) {
                } //we don't care
            }
        } finally {
            this.characterlock.readLock().unlock();
        }
    }

    private void cancelRespawnTask() {
        if (this.respawnTask != null) {
            respawnTask.cancel(false);
        }
    }

    public void empty() {
        this.areas.clear();
        this.characters.clear();
        this.mapobjects.clear();
        this.cancelRespawnTask();
        this.footholds = null;
    }

    public ChannelServer getChannel() {
        return ChannelServer.getInstance(channel);
    }
}

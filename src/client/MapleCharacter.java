/*
This file is part of the OdinMS Maple Story Server
Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
Matthias Butz <matze@odinms.de>
Jan Christian Meyer <vimes@odinms.de>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License zas
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

import constants.ExpTable;
import constants.skills.Bishop;
import constants.skills.BlazeWizard;
import constants.skills.Corsair;
import constants.skills.Crusader;
import constants.skills.DarkKnight;
import constants.skills.DawnWarrior;
import constants.skills.FPArchMage;
import constants.skills.GM;
import constants.skills.Hermit;
import constants.skills.ILArchMage;
import constants.skills.Magician;
import constants.skills.Marauder;
import constants.skills.NightWalker;
import constants.skills.Priest;
import constants.skills.Ranger;
import constants.skills.Sniper;
import constants.skills.Spearman;
import constants.skills.SuperGM;
import constants.skills.Swordsman;
import constants.skills.ThunderBreaker;
import constants.skills.WindArcher;
import java.awt.Point;
import java.lang.ref.WeakReference;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Formatter;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import net.MaplePacket;
import net.channel.ChannelServer;
import net.world.MapleMessenger;
import net.world.MapleMessengerCharacter;
import net.world.MapleParty;
import net.world.MaplePartyCharacter;
import net.world.PartyOperation;
import net.world.PlayerBuffValueHolder;
import net.world.PlayerCoolDownValueHolder;
import net.world.guild.MapleGuild;
import net.world.guild.MapleGuildCharacter;
import net.world.remote.WorldChannelInterface;
import scripting.event.EventInstanceManager;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.MapleMiniGame;
import server.MaplePlayerShop;
import server.MaplePortal;
import server.MapleShop;
import server.MapleStatEffect;
import server.MapleStorage;
import server.MapleTrade;
import server.TimerManager;
import server.life.MapleMonster;
import server.life.MobSkill;
import server.maps.AbstractAnimatedMapleMapObject;
import server.maps.FieldLimit;
import server.maps.HiredMerchant;
import server.maps.MapleDoor;
import server.maps.MapleMap;
import server.maps.MapleMapFactory;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import server.maps.MapleSummon;
import server.maps.SavedLocation;
import server.maps.SavedLocationType;
import server.quest.MapleQuest;
import tools.DatabaseConnection;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.Randomizer;
import client.anticheat.CheatTracker;
import constants.InventoryConstants;
import constants.ServerConstants;
import java.sql.Timestamp;
import java.util.Random;

public class MapleCharacter extends AbstractAnimatedMapleMapObject {

    private int world;
    private int accountid;
    private int rank;
    private int reborns;
    public int FishingExp;
    public int FishingLevel;
    private int ESP;
    private int rankMove;
    private int jobRank;
    private int jobRankMove;
    private int id;
    public int CookingLevel;
    public int CookingExp;
    private int level;
    private int str;
    private int dex;
    private int luk;
    private int int_;
    public boolean isfake = false;
    private int hp;
    private int maxhp;
    private int mp;
    private int maxmp;
    private int hpMpApUsed;
    private int hair;
    private int face;
    private int remainingAp;
    private int remainingSp;
    private int fame;
    private int initialSpawnPoint;
    private int mapid;
    private int gender;
    private int currentPage;
    private transient int wdef, mdef;
    private int currentType = 0;
    private int currentTab = 1;
    private int chair;
    private int itemEffect;
    private int paypalnx;
    private int maplepoints;
    private int cardnx;
    private int guildid;
    private int guildrank;
    private int allianceRank;
    private int messengerposition = 4;
    private int energybar;
    private int gmLevel;
    private int ci = 0;
    private int votepoints;
    private int familyId;
    private int bookCover;
    private String linkedName;
    private int linkedLevel;
    private int battleshipHp = 0;
    private int mesosTraded = 0;
    private int possibleReports = 10;
    private int dojoPoints;
    private int vanquisherStage;
    private int dojoStage;
    private int dojoEnergy;
    private int vanquisherKills;
    private int warpToId = -1;
    private boolean antiKS = false;
    private int mesoRate = 1;
    private int dropRate = 1;
    private int omokwins;
    private int omokties;
    private int omoklosses;
    private int DonatorPoints;
    private int matchcardwins;
    private int matchcardties;
    private double sword;
    private double blunt;
    private double axe;
    private double spear;
    private double polearm;
    private double claw;
    private double dagger;
    private int matchcardlosses;
    private int married;
    private int fallcounter;
    private int givenRiceCakes;
    private int points = 0;
    private int beaconOid = -1;
    private int cp;
    private int totalCp;
    private double expRate = 1;
    private long dojoFinish;
    private long lastfametime;
    private long lastUsedCashItem;
    private long lastHealed;
    private long megaLimit = 0;
    private transient int localmaxhp;
    private transient int localmaxmp;
    private int fmmorphId = 0;
    private transient int localstr;
    private transient int localdex;
    private transient int localluk;
    private transient int localint_;
    private transient int magic;
    private transient int watk;
    private boolean hasBeacon = false;
    private boolean hidden;
    private boolean canDoor = true;
    private boolean incs;
    private boolean inmts;
    private boolean whitechat = true;
    private long afkTime;
    private boolean Berserk;
    private boolean hasMerchant;
    private boolean watchedCygnusIntro;
    private boolean finishedDojoTutorial;
    private boolean dojoParty;
    private double staffwand = 0.1;
    private double crossbow;
    private int maxDis;
    private double bow;
    public boolean isFishing = false;
    private boolean gottenRiceHat;
    private boolean allowMapChange = true;
    private boolean stuck = false;
    public int state;
    private String name;
    private String chalktext;
    private String search = null;
    private String partyquestitems = "";
    private AtomicInteger exp = new AtomicInteger();
    private AtomicInteger meso = new AtomicInteger();
    private BuddyList buddylist;
    private EventInstanceManager eventInstance = null;
    private HiredMerchant hiredMerchant = null;
    private MapleClient client;
    private MapleGuildCharacter mgc = null;
    private MapleInventory[] inventory;
    private MapleOccupations occupation = MapleOccupations.Atheist;
    private MapleJob job = MapleJob.BEGINNER;
    private MapleMap map;
    private MapleMap dojoMap;
    private MapleMessenger messenger = null;
    private MapleMiniGame miniGame;
    private MapleMount maplemount;
    private MapleParty party;
    private MaplePet[] pets = new MaplePet[3];
    private MaplePlayerShop playerShop = null;
    private MapleShop shop = null;
    private MapleSkinColor skinColor = MapleSkinColor.NORMAL;
    private MapleStorage storage = null;
    private MapleTrade trade = null;
    private boolean canTalk = true;
    private SavedLocation savedLocations[];
    private SkillMacro[] skillMacros = new SkillMacro[5];
    private List<Integer> lastmonthfameids;
    private Map<MapleQuest, MapleQuestStatus> quests;
    private Set<MapleMonster> controlled = new LinkedHashSet<MapleMonster>();
    private Map<Integer, String> entered = new LinkedHashMap<Integer, String>();
    private Set<MapleMapObject> visibleMapObjects = new LinkedHashSet<MapleMapObject>();
    private Map<ISkill, SkillEntry> skills = new LinkedHashMap<ISkill, SkillEntry>();
    private Map<MapleBuffStat, MapleBuffStatValueHolder> effects = Collections.synchronizedMap(new LinkedHashMap<MapleBuffStat, MapleBuffStatValueHolder>());
    private Map<Integer, MapleKeyBinding> keymap = new LinkedHashMap<Integer, MapleKeyBinding>();
    private Map<Integer, MapleSummon> summons = new LinkedHashMap<Integer, MapleSummon>();
    private Map<Integer, MapleCoolDownValueHolder> coolDowns = new LinkedHashMap<Integer, MapleCoolDownValueHolder>();
    private final List<MapleDisease> diseases = new ArrayList<MapleDisease>();
    private List<MapleDoor> doors = new ArrayList<MapleDoor>();
    public List<Pair<IItem, MapleInventoryType>> tempMerchantItems;
    private ScheduledFuture<?> dragonBloodSchedule;
    private ScheduledFuture<?> mapTimeLimitTask = null;
    private ScheduledFuture<?> periodicSaveTask = null;
    private ScheduledFuture<?> hpDecreaseTask;
    private ScheduledFuture<?> beholderHealingSchedule;
    private ScheduledFuture<?> beholderBuffSchedule;
    private ScheduledFuture<?> BerserkSchedule;
    private ScheduledFuture<?>[] fullness = new ScheduledFuture<?>[3];
    private NumberFormat nf = new DecimalFormat("#,###,###,###");
    private static List<Pair<Byte, Integer>> inventorySlots = new ArrayList<Pair<Byte, Integer>>();
    private ArrayList<String> commands = new ArrayList<String>();
    private ArrayList<Integer> excluded = new ArrayList<Integer>();
    private MonsterBook monsterbook;
    private List<Integer> wishList = new ArrayList<Integer>();
    private List<MapleRing> crushRings = new ArrayList<MapleRing>();
    private List<MapleRing> friendshipRings = new ArrayList<MapleRing>();
    private List<MapleRing> marriageRings = new ArrayList<MapleRing>();
    private List<Integer> vipRockMaps = new LinkedList<Integer>();
    private List<Integer> rockMaps = new LinkedList<Integer>();
    private transient int localmaxbasedamage;
    private static boolean multilevel = true;
    private CheatTracker anticheat;
    private boolean receivedMOTB;
    private boolean canSmega;
    public long latestUse = 0;
    private int slot;
    // PQs
    private static String[] ariantroomleader = new String[3];
    private static int[] ariantroomslot = new int[3];
    private ISkill skil;
    private int skill = 0;
    private MapleClient player;
    private int rebirth;
    private int GMPoints;

    public MapleCharacter() {
        canSmega = true;
        setStance(0);
        inventory = new MapleInventory[MapleInventoryType.values().length];
        savedLocations = new SavedLocation[SavedLocationType.values().length];
        for (MapleInventoryType type : MapleInventoryType.values()) {
            inventory[type.ordinal()] = new MapleInventory(type);
        }
        for (int i = 0; i < SavedLocationType.values().length; i++) {
            savedLocations[i] = null;
        }
        quests = new LinkedHashMap<MapleQuest, MapleQuestStatus>();
        afkTime = System.currentTimeMillis();
        setPosition(new Point(0, 0));
        this.anticheat = new CheatTracker(this);
    }
    
     public void setID(int id){
     this.id = id;
    }
     
       public void setJob(int job){
        this.job = MapleJob.getById(job);
    }
       
    public static MapleCharacter getDefault(MapleClient c) {
        MapleCharacter ret = new MapleCharacter();
        ret.client = c;
        ret.gmLevel = c.gmLevel();
        ret.hp = 50;
        ret.maxhp = 50;
        ret.mp = 5;
        ret.maxmp = 5;
        ret.str = 4;
        ret.dex = 4;
        ret.int_ = 4;
        ret.luk = 4;
        ret.reborns = 0;
        ret.FishingExp = 0;
        ret.FishingLevel = 1;
        ret.ESP = 0;
        ret.CookingExp = 0;
        ret.CookingLevel = 1;
        ret.map = null;
        ret.mapid = -1;
        ret.job = MapleJob.BEGINNER;
        ret.occupation = MapleOccupations.Atheist;
        ret.level = 1;
        ret.accountid = c.getAccID();
        ret.buddylist = new BuddyList(100);

        ret.getInventory(MapleInventoryType.EQUIP).setSlotLimit(96);
        ret.getInventory(MapleInventoryType.USE).setSlotLimit(96);
        ret.getInventory(MapleInventoryType.SETUP).setSlotLimit(96);
        ret.getInventory(MapleInventoryType.ETC).setSlotLimit(96);

        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT name, paypalNX, mPoints, cardNX, votepoints FROM accounts WHERE id = ?");
            ps.setInt(1, ret.accountid);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                ret.client.setAccountName(rs.getString("name"));
                ret.paypalnx = rs.getInt("paypalNX");
                ret.maplepoints = rs.getInt("mPoints");
                ret.cardnx = rs.getInt("cardNX");
                ret.votepoints = rs.getInt("votepoints");
            }
            rs.close();
            ps.close();
        } catch (Exception e) {
        }
        ret.maplemount = null;
        int[] key = {18, 65, 2, 23, 3, 4, 5, 6, 16, 17, 19, 25, 26, 27, 31, 34, 35, 37, 38, 40, 43, 44, 45, 46, 50, 56, 59, 60, 61, 62, 63, 64, 57, 48, 29, 7, 24, 33, 41};
        int[] type = {4, 6, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 4, 4, 5, 6, 6, 6, 6, 6, 6, 5, 4, 5, 4, 4, 4, 4};
        int[] action = {0, 106, 10, 1, 12, 13, 18, 24, 8, 5, 4, 19, 14, 15, 2, 17, 11, 3, 20, 16, 9, 50, 51, 6, 7, 53, 100, 101, 102, 103, 104, 105, 54, 22, 52, 21, 25, 26, 23};
        for (int i = 0; i < key.length; i++) {
            ret.keymap.put(key[i], new MapleKeyBinding(type[i], action[i]));
        }
        return ret;
    }
    
    public void addCooldown(int skillId, long startTime, long length, ScheduledFuture<?> timer) {
        if (this.coolDowns.containsKey(Integer.valueOf(skillId))) {
            this.coolDowns.remove(skillId);
        }
        this.coolDowns.put(Integer.valueOf(skillId), new MapleCoolDownValueHolder(skillId, startTime, length, timer));
    }

    public void addCommandToList(String command) {
        commands.add(command);
    }

    public void addCrushRing(MapleRing r) {
        crushRings.add(r);
    }
    public int getExpNeededForfishingLevel(int level) {
        return ExpTable.getFishingNeededForLevel(level);
    }
    public int getFishingLevel(){
        return this.FishingLevel;
    }
    public int getFishingEXP(){
        return this.FishingExp;
    }
    public void setFishingLevel(int x){
        this.FishingLevel = x;
    }
    public void FishingLevelUp(){
        getMap().broadcastMessage(getClient().getPlayer(), MaplePacketCreator.showSpecialEffect(8), false);
        getClient().getSession().write(MaplePacketCreator.showSpecialEffect(0));
        this.FishingLevel += 1;
        this.FishingExp = 0;
        this.remainingAp += 250;
        this.updateSingleStat(MapleStat.AVAILABLEAP, remainingAp);
        getClient().getSession().write(MaplePacketCreator.serverNotice(5, "Congrats! Your Fishing Level has leveled up! You are now a Lv." + this.getFishingLevel() + " Fisherman!"));
getClient().getSession().write(MaplePacketCreator.serverNotice(1, "You are now a Lv." + this.getFishingLevel() + " Fisherman!"));

    }
    public void gainFishingEXP(int amount){
        int totoexp = this.FishingExp + amount;
        if(totoexp >= ExpTable.getFishingNeededForLevel(FishingLevel) && this.getFishingLevel() < 150){
            FishingLevelUp();
        }
        else {
            FishingExp += amount;
            getClient().getSession().write(MaplePacketCreator.showSpecialEffect(9));
            getClient().getSession().write(MaplePacketCreator.serverNotice(5, "You have gain " + amount + " Fishing EXP! This is your Fishing EXP Table : " + this.getFishingEXP() + " / " + this.getExpNeededForfishingLevel(this.getFishingLevel()) +" "));
        }
    }
            public boolean inEventMap() {
        return getMapId() == 280030000 || getMapId() == 910000000 || getMapId() == 3;
    }

    public boolean gmChat = true;

    public int addDojoPointsByMap() {
        int pts = 0;
        if (dojoPoints < 17000) {
            pts = 1 + ((getMap().getId() - 1) / 100 % 100) / 6;
            if (!dojoParty) {
                pts++;
            }
            this.dojoPoints += pts;
        }
        return pts;
    }

    public int[] jailmaps = {
        930000800,//carnival pq
        980000010,//carnifal pq2
        200090300,//??????
        926100700,//Starter map
        926100000,//Starter mobs
    };

    public boolean inZakum() {
        return getMapId() == 280030000 || getMapId() == 677000009;
    }

    public boolean inJail() {
        boolean injail = false;
        for (int i = 0; i < jailmaps.length; i++) {
            if (getMapId() == jailmaps[i]) {
                injail = true;
            }
        }
        return injail;
    }
    public void warpToMap(int map) {
        changeMap(getClient().getChannelServer().getMapFactory().getMap(map));
    }
    public void warpToRandomJailMap() {
        Random rnd = new Random();
        changeMap(getClient().getChannelServer().getMapFactory().getMap(jailmaps[rnd.nextInt(jailmaps.length)]));
    }
    public void warpToDefinedJailMap(int mapto) {
        changeMap(getClient().getChannelServer().getMapFactory().getMap(jailmaps[mapto]));
    }

    public void addDoor(MapleDoor door) {
        doors.add(door);
    }

    public void addExcluded(int x) {
        excluded.add(x);
    }

    public void addFame(int famechange) {
        this.fame += famechange;
    }

    public void addFriendshipRing(MapleRing r) {
        friendshipRings.add(r);
    }

    public void addHP(int delta) {
        int newHP = hp + delta;
        setHp(newHP);
        updateSingleStat(MapleStat.HP, newHP);
    }

    public boolean getCanSmega() {
        return canSmega;
    }

    public void setCanSmega(boolean setTo) {
        canSmega = setTo;
    }

    public void addMarriageRing(MapleRing r) {
        marriageRings.add(r);
    }

    public void addMesosTraded(int gain) {
        this.mesosTraded += gain;
    }


    public void addMP(int delta) {
        int newMP = mp + delta;
        setMp(newMP);
        updateSingleStat(MapleStat.MP, newMP);
    }

    public void addMPHP(int hpDiff, int mpDiff) {
        int newHP = hp + hpDiff;
        int newMP = mp + mpDiff;
        setHp(newHP);
        setMp(newMP);
        updateSingleStat(MapleStat.HP, newHP);
        updateSingleStat(MapleStat.MP, newMP);
    }

    public void addPet(MaplePet pet) {
        for (int i = 0; i < 3; i++) {
            if (pets[i] == null) {
                pets[i] = pet;
                return;
            }
        }
    }

       public int quantityItem(int itemid, boolean checkStorage) {
        int item_amount = inventory[MapleItemInformationProvider.getInstance().getInventoryType(itemid).ordinal()].countById(itemid);
        if (checkStorage) {
            List<IItem> storageitems = getStorage().getItems();
            for (IItem item : storageitems)
                if (item.getItemId() == itemid)
                    item_amount += item.getQuantity();
        }
        return item_amount;
    }


    public void addStat(int type, int up) {
        if (type == 1) {
            this.str += up;
            updateSingleStat(MapleStat.STR, str);
        } else if (type == 2) {
            this.dex += up;
            updateSingleStat(MapleStat.DEX, dex);
        } else if (type == 3) {
            this.int_ += up;
            updateSingleStat(MapleStat.INT, int_);
        } else if (type == 4) {
            this.luk += up;
            updateSingleStat(MapleStat.LUK, luk);
        }
    }

    public void addSummon(int id, MapleSummon summon) {
        summons.put(id, summon);
    }

    public void addTeleportRockMap(Integer mapId, int type) {
        if (type == 0 && rockMaps.size() < 5 && !rockMaps.contains(mapId)) {
            rockMaps.add(mapId);
        } else if (vipRockMaps.size() < 10 && !vipRockMaps.contains(mapId)) {
            vipRockMaps.add(mapId);
        }
    }

    public void addToWishList(int sn) {
        wishList.add(sn);
    }

    public void addVisibleMapObject(MapleMapObject mo) {
        visibleMapObjects.add(mo);
    }

    public static boolean unban(String name) {
        try {
            int accountid = -1;
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT accountid FROM characters WHERE name = ?");
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                accountid = rs.getInt("accountid");
            }
            ps.close();
            rs.close();
            if (accountid == -1) {
                return false;
            }
            ps = con.prepareStatement("UPDATE accounts SET banned = -1 WHERE id = ?");
            ps.setInt(1, accountid);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            return false;
        }
        return true;
    }

    public static boolean unbanIP(String id) {
        String banString = "";
        PreparedStatement ps;
        Connection con = DatabaseConnection.getConnection();
        boolean ret = false;
        try {
            ps = con.prepareStatement("SELECT banreason FROM accounts WHERE name = ?");
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                ps.close();
            }
            banString = rs.getString("banreason");
            rs.close();
            ps.close();
        } catch (SQLException e) {
        }
        if (banString.indexOf("IP: /") != -1) {
            String ip = banString.substring(banString.indexOf("IP: /") + 5, banString.length() - 1);
            try {
                ps = con.prepareStatement("DELETE FROM ipbans WHERE ip = ?");
                ps.setString(1, ip);
                ps.executeUpdate();
                ps.close();
                ret = true;
            } catch (SQLException exe) {
            }
        }
        return ret;
    }

    public void ban(String reason, boolean permBan) {
		if (lastmonthfameids == null) {
			throw new RuntimeException("Trying to ban a non-loaded character (testhack)");
		}
		try {
			getClient().banMacs();
			Connection con = DatabaseConnection.getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE accounts SET banned = ?, banreason = ? WHERE id = ?");
			ps.setInt(1, 1);
			ps.setString(2, reason);
			ps.setInt(3, accountid);
			ps.executeUpdate();
			ps.close();
			ps = con.prepareStatement("INSERT INTO ipbans VALUES (DEFAULT, ?)");
			String[] ipSplit = client.getSession().getRemoteAddress().toString().split(":");
			ps.setString(1, ipSplit[0]);
			ps.executeUpdate();
			ps.close();
		} catch (SQLException ex) {
		}
		client.getSession().close();
	}

	public static boolean ban(String id, String reason, boolean accountId) {
		try {
			Connection con = DatabaseConnection.getConnection();
			PreparedStatement ps;
			if (id.matches("/[0-9]{1,3}\\..*")) {
				ps = con.prepareStatement("INSERT INTO ipbans VALUES (DEFAULT, ?)");
				ps.setString(1, id);
				ps.executeUpdate();
				ps.close();
				return true;
			}
			if (accountId) {
				ps = con.prepareStatement("SELECT id FROM accounts WHERE name = ?");
			} else {
				ps = con.prepareStatement("SELECT accountid FROM characters WHERE name = ?");
			}
			boolean ret = false;
			ps.setString(1, id);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				PreparedStatement psb = con.prepareStatement("UPDATE accounts SET banned = 1, banreason = ? WHERE id = ?");
				psb.setString(1, reason);
				psb.setInt(2, rs.getInt(1));
				psb.executeUpdate();
				psb.close();
				ret = true;
			}
			rs.close();
			ps.close();
			return ret;
		} catch (SQLException ex) {
		}
		return false;
	}

    public void morphById(int toMorph, MapleCharacter target) {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                ii.getItemEffect(Integer.parseInt("22100" + toMorph)).applyTo(target);
    }

    public void morphRandom() {
                 Random rnd = new Random();
                    int asdf = rnd.nextInt(getFMMorphIds().length);
                    setFMMorph(getFMMorphIds()[asdf]);
                    MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                    ii.getItemEffect(Integer.parseInt("22100" + getFMMorph())).applyTo(this);

    }
    public void unmorph(MapleCharacter target) {

            if (target.getBuffedValue(MapleBuffStat.MORPH) != null) {
                      target.cancelEffectFromBuffStat(MapleBuffStat.MORPH);
        }
    }

    public boolean isMorphed(MapleCharacter target) {

            if (target.getBuffedValue(MapleBuffStat.MORPH) != null) {
                return true;
            }
    return false;
    }


    public int[] getFMMorphIds() {
        int[] asdf = {35, 36, 37, 38, 39};
        return asdf;
    }
    public boolean isFMMorph() {
            for (int i = 0; i < getFMMorphIds().length; i++) {
                if (getFMMorph() == getFMMorphIds()[i]) {
                    return true;
                }
            }
        return false;
    }
    public int getFMMorph() {
        return fmmorphId;
    }

    public void setFMMorph(int asdf) {
        fmmorphId = asdf;
    }

    public void tempban(String reason, Calendar duration, int greason) {
        if (lastmonthfameids == null) {
            throw new RuntimeException("Trying to ban a non-loaded character (testhack)");
        }
        tempban(reason, duration, greason, client.getAccID());
        client.getSession().close();
    }

    /*
     * 			DateFormat df = DateFormat.getInstance();
    tempB.set(tempB.get(Calendar.YEAR) + yChange, tempB.get(Calendar.MONTH) + mChange, tempB.get(Calendar.DATE) +
    (wChange * 7) + dChange, tempB.get(Calendar.HOUR_OF_DAY) + hChange, tempB.get(Calendar.MINUTE) +
    iChange);
     */
    public static boolean tempban(String reason, Calendar duration, int greason, int accountid) {
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("UPDATE accounts SET tempban = ?, banreason = ?, greason = ? WHERE id = ?");
            Timestamp TS = new Timestamp(duration.getTimeInMillis());
            ps.setTimestamp(1, TS);
            ps.setString(2, reason);
            ps.setInt(3, greason);
            ps.setInt(4, accountid);
            ps.executeUpdate();
            ps.close();
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            //log.error("Error while tempbanning", ex);
        }
        return false;
    }

    public int calculateMaxBaseDamageForHH(int watk) {
        if (watk == 0) {
            return 1;
        } else {
            IItem weapon_item = getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -11);
            if (weapon_item != null) {
                return (int) (((MapleItemInformationProvider.getInstance().getWeaponType(weapon_item.getItemId()).getMaxDamageMultiplier() * localstr + localdex) / 100.0) * watk);
            } else {
                return 0;
            }
        }
    }

    public void cancelAllBuffs() {
        for (MapleBuffStatValueHolder mbsvh : new ArrayList<MapleBuffStatValueHolder>(effects.values())) {
            cancelEffect(mbsvh.effect, false, mbsvh.startTime);
        }
    }

    public void cancelBuffStats(MapleBuffStat stat) {
        List<MapleBuffStat> buffStatList = Arrays.asList(stat);
        deregisterBuffStats(buffStatList);
        cancelPlayerBuffs(buffStatList);
    }

    public void changeMap(int map) {
        changeMap(map, 0);
    }
        public void changeMap(int map, int portal) {
        MapleMap warpMap = client.getChannelServer().getMapFactory().getMap(map);
        changeMap(warpMap, warpMap.getPortal(portal));
    }

public void removeAll(int id) {
        MapleInventoryManipulator.removeAllById(getClient(), id, true);
    }

    public int calculateMaxBaseDamage(int watk) {
        int maxbasedamage;
        if (watk == 0) {
            maxbasedamage = 1;
        } else {
            IItem weapon_item = getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -11);
            if (weapon_item != null) {
                MapleWeaponType weapon = MapleItemInformationProvider.getInstance().getWeaponType(weapon_item.getItemId());
                int mainstat;
                int secondarystat;
                if (weapon == MapleWeaponType.BOW || weapon == MapleWeaponType.CROSSBOW) {
                    mainstat = localdex;
                    secondarystat = localstr;
                } else if (getJob().isA(MapleJob.THIEF) && (weapon == MapleWeaponType.CLAW || weapon == MapleWeaponType.DAGGER)) {
                    mainstat = localluk;
                    secondarystat = localdex + localstr;
                } else {
                    mainstat = localstr;
                    secondarystat = localdex;
                }
                maxbasedamage = (int) (((weapon.getMaxDamageMultiplier() * mainstat + secondarystat) / 100.0) * watk);
                maxbasedamage += 10;
            } else {
                maxbasedamage = 0;
            }
        }
        return maxbasedamage;
    }

    public int calculateMinBaseDamage(MapleCharacter player) {
        int minbasedamage = 0;
        int atk = player.getTotalWatk();
        if (atk == 0) {
            minbasedamage = 1;
        } else {
            IItem weapon_item = getInventory(MapleInventoryType.EQUIPPED).getItem((byte) - 11);
            if (weapon_item != null) {
                MapleWeaponType weapon = MapleItemInformationProvider.getInstance().getWeaponType(weapon_item.getItemId());
                if (player.getJob().isA(MapleJob.FIGHTER)) {
                    skil = SkillFactory.getSkill(1100000);
                    skill = player.getSkillLevel(skil);
                    if (skill > 0) {
                        sword = ((skil.getEffect(player.getSkillLevel(skil)).getMastery() * 5 + 10) / 100);
                    } else {
                        sword = 0.1;
                    }
                } else {
                    skil = SkillFactory.getSkill(1200000);
                    skill = player.getSkillLevel(skil);
                    if (skill > 0) {
                        sword = ((skil.getEffect(player.getSkillLevel(skil)).getMastery() * 5 + 10) / 100);
                    } else {
                        sword = 0.1;
                    }
                }
                skil = SkillFactory.getSkill(1100001);
                skill = player.getSkillLevel(skil);
                if (skill > 0) {
                    axe = ((skil.getEffect(player.getSkillLevel(skil)).getMastery() * 5 + 10) / 100);
                } else {
                    axe = 0.1;
                }
                skil = SkillFactory.getSkill(1200001);
                skill = player.getSkillLevel(skil);
                if (skill > 0) {
                    blunt = ((skil.getEffect(player.getSkillLevel(skil)).getMastery() * 5 + 10) / 100);
                } else {
                    blunt = 0.1;
                }
                skil = SkillFactory.getSkill(1300000);
                skill = player.getSkillLevel(skil);
                if (skill > 0) {
                    spear = ((skil.getEffect(player.getSkillLevel(skil)).getMastery() * 5 + 10) / 100);
                } else {
                    spear = 0.1;
                }
                skil = SkillFactory.getSkill(1300001);
                skill = player.getSkillLevel(skil);
                if (skill > 0) {
                    polearm = ((skil.getEffect(player.getSkillLevel(skil)).getMastery() * 5 + 10) / 100);
                } else {
                    polearm = 0.1;
                }
                skil = SkillFactory.getSkill(3200000);
                skill = player.getSkillLevel(skil);
                if (skill > 0) {
                    crossbow = ((skil.getEffect(player.getSkillLevel(skil)).getMastery() * 5 + 10) / 100);
                } else {
                    crossbow = 0.1;
                }
                skil = SkillFactory.getSkill(3100000);
                skill = player.getSkillLevel(skil);
                if (skill > 0) {
                    bow = ((skil.getEffect(player.getSkillLevel(skil)).getMastery() * 5 + 10) / 100);
                } else {
                    bow = 0.1;
                }
                if (weapon == MapleWeaponType.CROSSBOW) {
                    minbasedamage = (int) (localdex * 0.9 * 3.6 * crossbow + localstr) / 100 * (atk + 15);
                }
                if (weapon == MapleWeaponType.BOW) {
                    minbasedamage = (int) (localdex * 0.9 * 3.4 * bow + localstr) / 100 * (atk + 15);
                }
                if (getJob().isA(MapleJob.THIEF) && (weapon == MapleWeaponType.DAGGER)) {
                    minbasedamage = (int) (localluk * 0.9 * 3.6 * dagger + localstr + localdex) / 100 * atk;
                }
                if (!getJob().isA(MapleJob.THIEF) && (weapon == MapleWeaponType.DAGGER)) {
                    minbasedamage = (int) (localstr * 0.9 * 4.0 * dagger + localdex) / 100 * atk;
                }
                if (getJob().isA(MapleJob.THIEF) && (weapon == MapleWeaponType.CLAW)) {
                    minbasedamage = (int) (localluk * 0.9 * 3.6 * claw + localstr + localdex) / 100 * (atk + 15);
                }
                if (weapon == MapleWeaponType.SPEAR) {
                    minbasedamage = (int) (localstr * 0.9 * 3.0 * spear + localdex) / 100 * atk;
                }
                if (weapon == MapleWeaponType.POLE_ARM) {
                    minbasedamage = (int) (localstr * 0.9 * 3.0 * polearm + localdex) / 100 * atk;
                }
                if (weapon == MapleWeaponType.SWORD1H) {
                    minbasedamage = (int) (localstr * 0.9 * 4.0 * sword + localdex) / 100 * atk;
                }
                if (weapon == MapleWeaponType.SWORD2H) {
                    minbasedamage = (int) (localstr * 0.9 * 4.6 * sword + localdex) / 100 * atk;
                }
                if (weapon == MapleWeaponType.AXE1H) {
                    minbasedamage = (int) (localstr * 0.9 * 3.2 * axe + localdex) / 100 * atk;
                }
                if (weapon == MapleWeaponType.BLUNT1H) {
                    minbasedamage = (int) (localstr * 0.9 * 3.2 * blunt + localdex) / 100 * atk;
                }
                if (weapon == MapleWeaponType.AXE2H) {
                    minbasedamage = (int) (localstr * 0.9 * 3.4 * axe + localdex) / 100 * atk;
                }
                if (weapon == MapleWeaponType.BLUNT2H) {
                    minbasedamage = (int) (localstr * 0.9 * 3.4 * blunt + localdex) / 100 * atk;
                }
                if (weapon == MapleWeaponType.STAFF || weapon == MapleWeaponType.WAND) {
                    minbasedamage = (int) (localstr * 0.9 * 3.0 * staffwand + localdex) / 100 * atk;
                }
            }
        }
        return minbasedamage;
    }

    public int getMaxDis(MapleCharacter player) {
        IItem weapon_item = player.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -11);
        if (weapon_item != null) {
            MapleWeaponType weapon = MapleItemInformationProvider.getInstance().getWeaponType(weapon_item.getItemId());
            if (weapon == MapleWeaponType.SPEAR || weapon == MapleWeaponType.POLE_ARM) {
                maxDis = 106;
            }
            if (weapon == MapleWeaponType.DAGGER || weapon == MapleWeaponType.SWORD1H || weapon == MapleWeaponType.AXE1H || weapon == MapleWeaponType.BLUNT1H) {
                maxDis = 63;
            }
            if (weapon == MapleWeaponType.SWORD2H || weapon == MapleWeaponType.AXE1H || weapon == MapleWeaponType.BLUNT1H) {
                maxDis = 73;
            }
            if (weapon == MapleWeaponType.STAFF || weapon == MapleWeaponType.WAND) {
                maxDis = 51;
            }
            if (weapon == MapleWeaponType.CLAW) {
                skil = SkillFactory.getSkill(4000001);
                skill = player.getSkillLevel(skil);
                if (skill > 0) {
                    maxDis = (skil.getEffect(player.getSkillLevel(skil)).getRange()) + 205;
                } else {
                    maxDis = 205;
                }
            }
            if (weapon == MapleWeaponType.BOW || weapon == MapleWeaponType.CROSSBOW) {
                skil = SkillFactory.getSkill(3000002);
                skill = player.getSkillLevel(skil);
                if (skill > 0) {
                    maxDis = (skil.getEffect(player.getSkillLevel(skil)).getRange()) + 270;
                } else {
                    maxDis = 270;
                }
            }
        }
        return maxDis;
    }

    public int getCurrentMaxBaseDamage() {
		return localmaxbasedamage;
	}

    public int getTotalMdef() {
        return mdef;
    }

    public int getTotalWdef() {
        return wdef;
    }

    public void gainMeso(int gain, boolean show, boolean enableActions) {
        gainMeso(gain, show, enableActions, false);
    }

    public boolean isDead() {
        return this.hp <= 0;
    }

  public boolean getAntiKS() {
  return antiKS;
  }

  public void setAntiKS(boolean b) {
      antiKS = b;
  }
  private byte numAntiKSMonsters = 0;

  public byte getNumAntiKSMonsters() {
      return numAntiKSMonsters;
  }
  public void incAntiKSNum() {
      numAntiKSMonsters++;
  }

  public void decAntiKSNum() {
      if (numAntiKSMonsters > 0) {
          numAntiKSMonsters--;
      }
    }

    public void unequipEverything() {
        MapleInventory equipped = this.getInventory(MapleInventoryType.EQUIPPED);
        List<Byte> position = new ArrayList<Byte>();
        for (IItem item : equipped.list()) {
            position.add(item.getPosition());
        }
        for (byte pos : position) {
            MapleInventoryManipulator.unequip(client, pos, getInventory(MapleInventoryType.EQUIP).getNextFreeSlot());
        }
    }

    public static class CancelCooldownAction implements Runnable {

        private int skillId;
        private WeakReference<MapleCharacter> target;

        public CancelCooldownAction(MapleCharacter target, int skillId) {
            this.target = new WeakReference<MapleCharacter>(target);
            this.skillId = skillId;
        }

        @Override
        public void run() {
            MapleCharacter realTarget = target.get();
            if (realTarget != null) {
                realTarget.removeCooldown(skillId);
                realTarget.client.getSession().write(MaplePacketCreator.skillCooldown(skillId, 0));
            }
        }
    }

    public void cancelEffect(MapleStatEffect effect, boolean overwrite, long startTime) {
        List<MapleBuffStat> buffstats;
        if (!overwrite) {
            buffstats = getBuffStats(effect, startTime);
        } else {
            List<Pair<MapleBuffStat, Integer>> statups = effect.getStatups();
            buffstats = new ArrayList<MapleBuffStat>(statups.size());
            for (Pair<MapleBuffStat, Integer> statup : statups) {
                buffstats.add(statup.getLeft());
            }
        }
        deregisterBuffStats(buffstats);
        if (effect.isMagicDoor()) {
            if (!getDoors().isEmpty()) {
                MapleDoor door = getDoors().iterator().next();
                for (MapleCharacter chr : door.getTarget().getCharacters()) {
                    door.sendDestroyData(chr.client);
                }
                for (MapleCharacter chr : door.getTown().getCharacters()) {
                    door.sendDestroyData(chr.client);
                }
                for (MapleDoor destroyDoor : getDoors()) {
                    door.getTarget().removeMapObject(destroyDoor);
                    door.getTown().removeMapObject(destroyDoor);
                }
                clearDoors();
                silentPartyUpdate();
            }
        }
        if (effect.getSourceId() == Spearman.HYPER_BODY || effect.getSourceId() == GM.HYPER_BODY || effect.getSourceId() == SuperGM.HYPER_BODY) {
            List<Pair<MapleStat, Integer>> statup = new ArrayList<Pair<MapleStat, Integer>>(4);
            statup.add(new Pair<MapleStat, Integer>(MapleStat.HP, Math.min(hp, maxhp)));
            statup.add(new Pair<MapleStat, Integer>(MapleStat.MP, Math.min(mp, maxhp)));
            statup.add(new Pair<MapleStat, Integer>(MapleStat.MAXHP, maxhp));
            statup.add(new Pair<MapleStat, Integer>(MapleStat.MAXMP, maxmp));
            client.getSession().write(MaplePacketCreator.updatePlayerStats(statup));
        }
        if (effect.isMonsterRiding()) {
            if (effect.getSourceId() != Corsair.BATTLE_SHIP) {
                maplemount.cancelSchedule();
                maplemount.setActive(false);
            }
        }
        final int id_ = effect.getSourceId();
        if (id_ == DawnWarrior.SOUL || id_ == BlazeWizard.FLAME || id_ == WindArcher.STORM || id_ == NightWalker.DARKNESS || id_ == ThunderBreaker.LIGHTNING) {
            message(new String[]{"Soul", "Flame", "Storm", "Darkness", "Lightning"}[id / 100000 % 10] + "'s time has run out has disappeared.");
        }
        if (!overwrite) {
            cancelPlayerBuffs(buffstats);
            if (effect.isHide() && (MapleCharacter) getMap().getMapObject(getObjectId()) != null) {
                this.hidden = false;
                this.getClient().getSession().write(MaplePacketCreator.getGMEffect(16, (byte) 0));
                getMap().broadcastNONGMMessage(this, MaplePacketCreator.spawnPlayerMapobject(this), false);
                for (int i = 0; pets[i] != null; i++) {
                    getMap().broadcastNONGMMessage(this, MaplePacketCreator.showPet(this, pets[i], false, false), false);
                }
            }
        }
    }

    public void cancelEffectFromBuffStat(MapleBuffStat stat) {
        cancelEffect(effects.get(stat).effect, false, -1);
    }

    public void cancelMagicDoor() {
        for (MapleBuffStatValueHolder mbsvh : new ArrayList<MapleBuffStatValueHolder>(effects.values())) {
            if (mbsvh.effect.isMagicDoor()) {
                cancelEffect(mbsvh.effect, false, mbsvh.startTime);
            }
        }
    }

    public void cancelMapTimeLimitTask() {
        if (mapTimeLimitTask != null) {
            mapTimeLimitTask.cancel(false);
        }
    }

    public void cancelPeriodicSaveTask() {
        if (periodicSaveTask != null) {
            periodicSaveTask.cancel(false);
        }
    }

    private void cancelPlayerBuffs(List<MapleBuffStat> buffstats) {
        if (client.getChannelServer().getPlayerStorage().getCharacterById(getId()) != null) {
            recalcLocalStats();
            enforceMaxHpMp();
            client.getSession().write(MaplePacketCreator.cancelBuff(buffstats));
            if (buffstats.size() > 0 && !buffstats.get(0).equals(MapleBuffStat.HOMING_BEACON)) {
                getMap().broadcastMessage(this, MaplePacketCreator.cancelForeignBuff(getId(), buffstats), false);
            }
        }
    }

    public static boolean canCreateChar(String name) {
        if (name.length() < 4 || name.length() > 12) {
            return false;
        }
        return getIdByName(name) < 0 && Pattern.compile("[a-zA-Z0-9_-]{3,12}").matcher(name).matches();
    }

    public boolean canDoor() {
        return canDoor;
    }

    public FameStatus canGiveFame(MapleCharacter from) {
        if (gmLevel > 0) {
            return FameStatus.OK;
        } else if (lastfametime >= System.currentTimeMillis() - 3600000 * 24) {
            return FameStatus.NOT_TODAY;
        } else if (lastmonthfameids.contains(Integer.valueOf(from.getId()))) {
            return FameStatus.NOT_THIS_MONTH;
        } else {
            return FameStatus.OK;
        }
    }

    public void changeCI(int type) {
        this.ci = type;
    }

    public void changeJob(MapleJob newJob) {
        if (newJob == null) {
            return;
        }
        this.job = newJob;
        this.remainingSp++;
        if (newJob.getId() % 10 == 2) {
            this.remainingSp += 5;
        }
        if (newJob.getId() % 10 > 1) {
            this.remainingAp += 5;
        }
        int job_ = job.getId() % 1000; // lame temp "fix"
        if (job_ == 100) {
            maxhp += rand(200, 250);
        } else if (job_ == 200) {
            if (job.getId() == 200 && level > 8) {
                remainingSp += 3 * (level - 8);
            }
            maxmp += rand(100, 150);
        } else if (job_ % 100 == 0) {
            maxhp += rand(100, 150);
            maxhp += rand(25, 50);
        } else if (job_ > 0 && job_ < 200) {
            maxhp += rand(300, 350);
        } else if (job_ < 300) {
            maxmp += rand(250, 300);
        } else if (job_ > 0 && job_ != 1000) {
            maxhp += rand(300, 350);
            maxmp += rand(150, 200);
        }
        if (maxhp >= 30000) {
            maxhp = 30000;
        }
        if (maxmp >= 30000) {
            maxmp = 30000;
        }
        if (gmLevel < 1) {
            for (int i = (job_ == 200 ? 2 : 1); i < (job_ != 100 && job_ != 200 ? 3 : 5); i++) {
                getInventory(MapleInventoryType.getByType((byte) i)).increaseSlotLimit(4);
            }
        }
        List<Pair<MapleStat, Integer>> statup = new ArrayList<Pair<MapleStat, Integer>>(5);
        statup.add(new Pair<MapleStat, Integer>(MapleStat.MAXHP, Integer.valueOf(maxhp)));
        statup.add(new Pair<MapleStat, Integer>(MapleStat.MAXMP, Integer.valueOf(maxmp)));
        statup.add(new Pair<MapleStat, Integer>(MapleStat.AVAILABLEAP, remainingAp));
        statup.add(new Pair<MapleStat, Integer>(MapleStat.AVAILABLESP, remainingSp));
        statup.add(new Pair<MapleStat, Integer>(MapleStat.JOB, Integer.valueOf(job.getId())));
        recalcLocalStats();
        client.getSession().write(MaplePacketCreator.updatePlayerStats(statup));
        silentPartyUpdate();
        guildUpdate();
        getMap().broadcastMessage(this, MaplePacketCreator.showForeignEffect(getId(), 8), false);
    }

    public void changeKeybinding(int key, MapleKeyBinding keybinding) {
        if (keybinding.getType() != 0) {
            keymap.put(Integer.valueOf(key), keybinding);
        } else {
            keymap.remove(Integer.valueOf(key));
        }
    }

    public void changeMap(MapleMap to) {
        changeMap(to, to.getPortal(0));
    }

    public void changeMap(final MapleMap to, final MaplePortal pto) {
        if (to.getId() == 100000200 || to.getId() == 211000100 || to.getId() == 220000300) {
            changeMapInternal(to, pto.getPosition(), MaplePacketCreator.getWarpToMap(to, pto.getId() - 2, this));
        } else {
            changeMapInternal(to, pto.getPosition(), MaplePacketCreator.getWarpToMap(to, pto.getId(), this));
        }
    }

    public void changeMap(final MapleMap to, final Point pos) {
        changeMapInternal(to, pos, MaplePacketCreator.getWarpToMap(to, 0x80, this));
    }

    public void changeMapBanish(int mapid, String portal, String msg) {
        dropMessage(6, msg);
        MapleMap map_ = ChannelServer.getInstance(client.getChannel()).getMapFactory().getMap(mapid);
        changeMap(map_, map_.getPortal(portal));
    }

    private void changeMapInternal(final MapleMap to, final Point pos, MaplePacket warpPacket) {
        if ((this.getEventInstance() != null) && (!this.getEventInstance().mapChanged(this))) {
            this.dropMessage(6, "Map changes are not allowed in this event. Please leave the event before changing your map.");
            return;
        }
        warpPacket.setOnSend(new Runnable() {

            @Override
            public void run() {
                map.removePlayer(MapleCharacter.this);
                try {
                    List<MapleMapObject> monsters = map.getAllMonster();
                    for (MapleMapObject mmo : monsters) {
                        MapleMonster m = (MapleMonster) mmo;
                        if (m.getBelongsTo() == getId()) {
                            decAntiKSNum();
                            m.expireAntiKS();
                        }
                    }
                } catch (Exception e) {
                }
                if (client.getChannelServer().getPlayerStorage().getCharacterById(getId()) != null) {
                    map = to;
                    setPosition(pos);
                    map.addPlayer(MapleCharacter.this);
                    if (party != null) {
                        silentPartyUpdate();
                        client.getSession().write(MaplePacketCreator.updateParty(client.getChannel(), party, PartyOperation.SILENT_UPDATE, null));
                        updatePartyMemberHP();
                    }
                    if (getMap().getHPDec() > 0) {
                        hpDecreaseTask = TimerManager.getInstance().schedule(new Runnable() {

                            @Override
                            public void run() {
                                doHurtHp();
                            }
                        }, 10000);
                    }
                }
            }
        });
        client.getSession().write(warpPacket);
    }

    public void changePage(int page) {
        this.currentPage = page;
    }

    public void changeSkillLevel(ISkill skill, int newLevel, int newMasterlevel) {
        skills.put(skill, new SkillEntry(newLevel, newMasterlevel));
        this.client.getSession().write(MaplePacketCreator.updateSkill(skill.getId(), newLevel, newMasterlevel));
    }

    public void changeTab(int tab) {
        this.currentTab = tab;
    }

    public void changeType(int type) {
        this.currentType = type;
    }

    public void checkBerserk() {
        if (BerserkSchedule != null) {
            BerserkSchedule.cancel(false);
        }
        final MapleCharacter chr = this;
        if (job.equals(MapleJob.DARKKNIGHT)) {
            ISkill BerserkX = SkillFactory.getSkill(DarkKnight.BERSERK);
            final int skilllevel = getSkillLevel(BerserkX);
            if (skilllevel > 0) {
                Berserk = chr.getHp() * 100 / chr.getMaxHp() < BerserkX.getEffect(skilllevel).getX();
                BerserkSchedule = TimerManager.getInstance().register(new Runnable() {

                    @Override
                    public void run() {
                        client.getSession().write(MaplePacketCreator.showOwnBerserk(skilllevel, Berserk));
                        getMap().broadcastMessage(MapleCharacter.this, MaplePacketCreator.showBerserk(getId(), skilllevel, Berserk), false);
                    }
                }, 5000, 3000);
            }
        }
    }

    public void checkMessenger() {
        if (messenger != null && messengerposition < 4 && messengerposition > -1) {
            try {
                WorldChannelInterface wci = ChannelServer.getInstance(client.getChannel()).getWorldInterface();
                wci.silentJoinMessenger(messenger.getId(), new MapleMessengerCharacter(this, messengerposition), messengerposition);
                wci.updateMessenger(getMessenger().getId(), name, client.getChannel());
            } catch (Exception e) {
                client.getChannelServer().reconnectWorld();
            }
        }
    }

    public void checkMonsterAggro(MapleMonster monster) {
        if (!monster.isControllerHasAggro()) {
            if (monster.getController() == this) {
                monster.setControllerHasAggro(true);
            } else {
                monster.switchController(this, true);
            }
        }
    }

    public void clearDoors() {
        doors.clear();
    }

    public void clearSavedLocation(SavedLocationType type) {
        savedLocations[type.ordinal()] = null;
    }

    public void clearWishList() {
        wishList.clear();
    }

    public void controlMonster(MapleMonster monster, boolean aggro) {
        monster.setController(this);
        controlled.add(monster);
        client.getSession().write(MaplePacketCreator.controlMonster(monster, false, aggro));
    }

    public int countItem(int itemid) {
        return inventory[MapleItemInformationProvider.getInstance().getInventoryType(itemid).ordinal()].countById(itemid);
    }

    public void decreaseBattleshipHp(int decrease) {
        this.battleshipHp -= decrease;
        if (battleshipHp <= 0) {
            this.battleshipHp = 0;
            ISkill battleship = SkillFactory.getSkill(Corsair.BATTLE_SHIP);
            int cooldown = battleship.getEffect(getSkillLevel(battleship)).getCooldown();
            getClient().getSession().write(MaplePacketCreator.skillCooldown(Corsair.BATTLE_SHIP, cooldown));
            addCooldown(Corsair.BATTLE_SHIP, System.currentTimeMillis(), cooldown * 1000, TimerManager.getInstance().schedule(new CancelCooldownAction(this, Corsair.BATTLE_SHIP), cooldown * 1000));
            cancelEffectFromBuffStat(MapleBuffStat.MONSTER_RIDING);
            resetBattleshipHp();
        }
    }

    public void decreaseReports() {
        this.possibleReports--;
    }

    public void deleteGuild(int guildId) {
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("UPDATE characters SET guildid = 0, guildrank = 5 WHERE guildid = ?");
            ps.setInt(1, guildId);
            ps.executeUpdate();
            ps.close();
            ps = con.prepareStatement("DELETE FROM guilds WHERE guildid = ?");
            ps.setInt(1, id);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException ex) {
            System.out.print("Error deleting guild: " + ex);
        }
    }

    public void deleteTeleportRockMap(Integer mapId, int type) {
        if (type == 0) {
            rockMaps.remove(mapId);
        } else {
            vipRockMaps.remove(mapId);
        }
    }

    private void deleteWhereCharacterId(Connection con, String sql) throws SQLException {
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
        ps.close();
    }

    private void deregisterBuffStats(List<MapleBuffStat> stats) {
        synchronized (stats) {
            List<MapleBuffStatValueHolder> effectsToCancel = new ArrayList<MapleBuffStatValueHolder>(stats.size());
            for (MapleBuffStat stat : stats) {
                MapleBuffStatValueHolder mbsvh = effects.get(stat);
                if (mbsvh != null) {
                    effects.remove(stat);
                    boolean addMbsvh = true;
                    for (MapleBuffStatValueHolder contained : effectsToCancel) {
                        if (mbsvh.startTime == contained.startTime && contained.effect == mbsvh.effect) {
                            addMbsvh = false;
                        }
                    }
                    if (addMbsvh) {
                        effectsToCancel.add(mbsvh);
                    }
                    if (stat == MapleBuffStat.SUMMON || stat == MapleBuffStat.PUPPET) {
                        int summonId = mbsvh.effect.getSourceId();
                        MapleSummon summon = summons.get(summonId);
                        if (summon != null) {
                            getMap().broadcastMessage(MaplePacketCreator.removeSpecialMapObject(summon, true), summon.getPosition());
                            getMap().removeMapObject(summon);
                            removeVisibleMapObject(summon);
                            summons.remove(summonId);
                        }
                        if (summon.getSkill() == DarkKnight.BEHOLDER) {
                            if (beholderHealingSchedule != null) {
                                beholderHealingSchedule.cancel(false);
                                beholderHealingSchedule = null;
                            }
                            if (beholderBuffSchedule != null) {
                                beholderBuffSchedule.cancel(false);
                                beholderBuffSchedule = null;
                            }
                        }
                    } else if (stat == MapleBuffStat.DRAGONBLOOD) {
                        dragonBloodSchedule.cancel(false);
                        dragonBloodSchedule = null;
                    }
                }
                stat = null;
            }
            for (MapleBuffStatValueHolder cancelEffectCancelTasks : effectsToCancel) {
                if (getBuffStats(cancelEffectCancelTasks.effect, cancelEffectCancelTasks.startTime).size() == 0) {
                    if (!cancelEffectCancelTasks.effect.isHomingBeacon()) {
                        cancelEffectCancelTasks.schedule.cancel(false);
                    }
                }
            }
        }
    }

    public void disableDoor() {
        canDoor = false;
        TimerManager.getInstance().schedule(new Runnable() {

            @Override
            public void run() {
                canDoor = true;
            }
        }, 5000);
    }

    public void disbandGuild() {
        if (guildid < 1 || guildrank != 1) {
            return;
        }
        try {
            client.getChannelServer().getWorldInterface().disbandGuild(guildid);
        } catch (Exception e) {
        }
    }


    public void dispel() {
        for (MapleBuffStatValueHolder mbsvh : new ArrayList<MapleBuffStatValueHolder>(effects.values())) {
            if (mbsvh.effect.isSkill()) {
                cancelEffect(mbsvh.effect, false, mbsvh.startTime);
            }
        }
    }

    public void dispelDebuffs() {
        List<MapleDisease> disease_ = new ArrayList<MapleDisease>();
        for (MapleDisease disease : diseases) {
            if (disease == MapleDisease.WEAKEN || disease != MapleDisease.DARKNESS || disease != MapleDisease.SEAL || disease != MapleDisease.POISON) {
                disease_.add(disease);
                client.getSession().write(MaplePacketCreator.cancelDebuff(disease_));
                getMap().broadcastMessage(this, MaplePacketCreator.cancelForeignDebuff(this.id, disease_), false);
                disease_.clear();
            } else {
                return;
            }
        }
        this.diseases.clear();
    }


    public void dispelSeduce() {
        List<MapleDisease> disease_ = new ArrayList<MapleDisease>();
        for (MapleDisease disease : diseases) {
            if (disease == MapleDisease.SEDUCE) {
                disease_.add(disease);
                client.getSession().write(MaplePacketCreator.cancelDebuff(disease_));
                getMap().broadcastMessage(this, MaplePacketCreator.cancelForeignDebuff(this.id, disease_), false);
                disease_.clear();
            }
        }
        this.diseases.clear();
    }

    public void dispelSkill(int skillid) {
        LinkedList<MapleBuffStatValueHolder> allBuffs = new LinkedList<MapleBuffStatValueHolder>(effects.values());
        for (MapleBuffStatValueHolder mbsvh : allBuffs) {
            if (skillid == 0) {
                if (mbsvh.effect.isSkill() && (mbsvh.effect.getSourceId() % 10000000 == 1004 || dispelSkills(mbsvh.effect.getSourceId()))) {
                    cancelEffect(mbsvh.effect, false, mbsvh.startTime);
                }
            } else if (mbsvh.effect.isSkill() && mbsvh.effect.getSourceId() == skillid) {
                cancelEffect(mbsvh.effect, false, mbsvh.startTime);
            }
        }
    }

    private boolean dispelSkills(int skillid) {
        switch (skillid) {
            case DarkKnight.BEHOLDER:
            case FPArchMage.ELQUINES:
            case ILArchMage.IFRIT:
            case Priest.SUMMON_DRAGON:
            case Bishop.BAHAMUT:
            case Ranger.PUPPET:
            case Ranger.SILVER_HAWK:
            case Sniper.PUPPET:
            case Sniper.GOLDEN_EAGLE:
            case Hermit.SHADOW_PARTNER:
            case DawnWarrior.SOUL:
            case BlazeWizard.FLAME:
            case BlazeWizard.IFRIT:
            case WindArcher.STORM:
            case WindArcher.PUPPET:
            case NightWalker.DARKNESS:
            case NightWalker.SHADOW_PARTNER:
            case ThunderBreaker.LIGHTNING:
                return true;
            default:
                return false;
        }
    }

    public void dispelSeal() {
        List<MapleDisease> disease_ = new ArrayList<MapleDisease>();
        for (MapleDisease disease : diseases) {
            if (disease == MapleDisease.SEAL) {
                disease_.add(disease);
                client.getSession().write(MaplePacketCreator.cancelDebuff(disease_));
                getMap().broadcastMessage(this, MaplePacketCreator.cancelForeignDebuff(this.id, disease_), false);
                disease_.clear();
            }
        }
        this.diseases.clear();
    }

    public void doHurtHp() {
        if (this.getInventory(MapleInventoryType.EQUIPPED).findById(getMap().getHPDecProtect()) != null) {
            return;
        }
        addHP(-getMap().getHPDec());
        hpDecreaseTask = TimerManager.getInstance().schedule(new Runnable() {

            @Override
            public void run() {
                doHurtHp();
            }
        }, 10000);
    }

    public void dropMessage(String message) {
        dropMessage(6, "[MyMaple]: " + message);
    }

    public void dropMessage(int type, String message) {
        client.getSession().write(MaplePacketCreator.serverNotice(type, message));
    }

    public String emblemCost() {
        return nf.format(MapleGuild.CHANGE_EMBLEM_COST);
    }

    private void enforceMaxHpMp() {
        List<Pair<MapleStat, Integer>> stats = new ArrayList<Pair<MapleStat, Integer>>(2);
        if (getMp() > getCurrentMaxMp()) {
            setMp(getMp());
            stats.add(new Pair<MapleStat, Integer>(MapleStat.MP, Integer.valueOf(getMp())));
        }
        if (getHp() > getCurrentMaxHp()) {
            setHp(getHp());
            stats.add(new Pair<MapleStat, Integer>(MapleStat.HP, Integer.valueOf(getHp())));
        }
        if (stats.size() > 0) {
            client.getSession().write(MaplePacketCreator.updatePlayerStats(stats));
        }
    }

    public void enteredScript(String script, int mapid) {
        if (!entered.containsKey(mapid)) {
            entered.put(mapid, script);
        }
    }

    public void equipChanged() {
        getMap().broadcastMessage(this, MaplePacketCreator.updateCharLook(this), false);
        recalcLocalStats();
        enforceMaxHpMp();
        if (getMessenger() != null) {
            WorldChannelInterface wci = client.getChannelServer().getWorldInterface();
            try {
                wci.updateMessenger(getMessenger().getId(), getName(), client.getChannel());
            } catch (Exception e) {
                client.getChannelServer().reconnectWorld();
            }
        }
    }

    public void expirationTask() {
        long expiration, currenttime = System.currentTimeMillis();
        List<IItem> toberemove = new ArrayList<IItem>(); // This is here to prevent deadlock.
        for (MapleInventory inv : inventory) {
            for (IItem item : inv.list()) {
                expiration = item.getExpiration();
                if (expiration != -1) {
                    if (currenttime < expiration) {
                        client.getSession().write(MaplePacketCreator.itemExpired(item.getItemId()));
                        toberemove.add(item);
                    }
                }
            }
            for (IItem item : toberemove) {
                MapleInventoryManipulator.removeFromSlot(client, inv.getType(), item.getPosition(), item.getQuantity(), true);
            }
            toberemove.clear();
        }
    }

    public enum FameStatus {

        OK, NOT_TODAY, NOT_THIS_MONTH
    }

    public void gainExp(int gain, boolean show, boolean inChat) {
        gainExp(gain, show, inChat, true);
    }

    public void gainExp(int gain, boolean show, boolean inChat, boolean white) {
        gainExp(gain, show, inChat, white, 0);
    }

    public void gainExp(int gain, boolean show, boolean inChat, boolean white, int party) {
        if (level < getMaxLevel()) {
            int total = gain;
            if (party > 1 && client.getPlayer().getReborns() == 0) {
            total += party * gain * 2;
            }
            if (party > 1) {
            total += party * gain / 20;
            }
            if ((long) this.exp.get() + (long) gain > (long) Integer.MAX_VALUE) {
                int gainFirst = ExpTable.getExpNeededForLevel(level) - this.exp.get();
                gain -= gainFirst + 1;
                this.gainExp(gainFirst + 1, false, inChat, white);
            }
            updateSingleStat(MapleStat.EXP, this.exp.addAndGet(gain));
            if (show && gain != 0) {
                client.getSession().write(MaplePacketCreator.getShowExpGain(gain, inChat, white, (byte) (total != gain ? party - 1 : 0)));
            }
        if (MapleCharacter.getMultiLevel()) {
            while (level < getMaxLevel() && exp.get() >= ExpTable.getExpNeededForLevel(level)) {
                levelUp(true);
            }
            } else if (exp.get() >= ExpTable.getExpNeededForLevel(level)) {
                levelUp(true);
                int need = ExpTable.getExpNeededForLevel(level);
                if (exp.get() >= need) {
                    setExp(need - 1);
                    updateSingleStat(MapleStat.EXP, need);
                }
            }
        }
    }

    public void gainFame(int delta) {
        this.addFame(delta);
        this.updateSingleStat(MapleStat.FAME, this.fame);
    }

                public static boolean getMultiLevel() {
        return multilevel;
    }

    public void gainMeso(int gain, boolean show) {
        gainMeso(gain, show, false, false);
    }

    public void gainMeso(int gain, boolean show, boolean enableActions, boolean inChat) {
        boolean noOp = false;
        if (((long) (meso.get()) + gain) >= 2147483647L) { //no-op; they've reached max mesos
            noOp = true;
            client.getSession().write(MaplePacketCreator.enableActions());
            return;
            //       updateSingleStat(MapleStat.MESO, meso.addAndGet(2147483647 - meso.get()), enableActions);
        } else {
            updateSingleStat(MapleStat.MESO, meso.addAndGet(gain), enableActions);
        }
        if (show && !noOp) {
            client.getSession().write(MaplePacketCreator.getShowMesoGain(gain, inChat));
        }
    }

    public void clearSlots() {
        inventorySlots.clear();
    }

    public void genericGuildMessage(int code) {
        this.client.getSession().write(MaplePacketCreator.genericGuildMessage((byte) code));
    }

    public int getAccountID() {
        return accountid;
    }

    public List<PlayerBuffValueHolder> getAllBuffs() {
        List<PlayerBuffValueHolder> ret = new ArrayList<PlayerBuffValueHolder>();
        for (MapleBuffStatValueHolder mbsvh : effects.values()) {
            ret.add(new PlayerBuffValueHolder(mbsvh.startTime, mbsvh.effect));
        }
        return ret;
    }

    public List<PlayerCoolDownValueHolder> getAllCooldowns() {
        List<PlayerCoolDownValueHolder> ret = new ArrayList<PlayerCoolDownValueHolder>();
        for (MapleCoolDownValueHolder mcdvh : coolDowns.values()) {
            ret.add(new PlayerCoolDownValueHolder(mcdvh.skillId, mcdvh.startTime, mcdvh.length));
        }
        return ret;
    }

    public int getAllianceRank() {
        return this.allianceRank;
    }

    public int getAllowWarpToId() {
        return warpToId;
    }

    public static String getAriantRoomLeaderName(int room) {
        return ariantroomleader[room];
    }

    public static int getAriantSlotsRoom(int room) {
        return ariantroomslot[room];
    }

    public int getBattleshipHp() {
        return battleshipHp;
    }

    public BuddyList getBuddylist() {
        return buddylist;
    }

    public Long getBuffedStarttime(MapleBuffStat effect) {
        MapleBuffStatValueHolder mbsvh = effects.get(effect);
        if (mbsvh == null) {
            return null;
        }
        return Long.valueOf(mbsvh.startTime);
    }

    public Integer getBuffedValue(MapleBuffStat effect) {
        MapleBuffStatValueHolder mbsvh = effects.get(effect);
        if (mbsvh == null) {
            return null;
        }
        return Integer.valueOf(mbsvh.value);
    }

    public int getBuffSource(MapleBuffStat stat) {
        MapleBuffStatValueHolder mbsvh = effects.get(stat);
        if (mbsvh == null) {
            return -1;
        }
        return mbsvh.effect.getSourceId();
    }

    private List<MapleBuffStat> getBuffStats(MapleStatEffect effect, long startTime) {
        List<MapleBuffStat> stats = new ArrayList<MapleBuffStat>();
        for (Entry<MapleBuffStat, MapleBuffStatValueHolder> stateffect : effects.entrySet()) {
            if (stateffect.getValue().effect.sameSource(effect) && (startTime == -1 || startTime == stateffect.getValue().startTime)) {
                stats.add(stateffect.getKey());
            }
        }
        return stats;
    }

    public int getChair() {
        return chair;
    }

    public void setChalkboard(String text) {
        this.chalktext = text;
        if (chalktext == null) {
            getMap().broadcastMessage(MaplePacketCreator.useChalkboard(this, true));
        } else {
            getMap().broadcastMessage(MaplePacketCreator.useChalkboard(this, false));
        }
    }

   public String getChalkboard() {
        return this.chalktext;
    }

    public MapleClient getClient() {
        return client;
    }

    public final List<MapleQuestStatus> getCompletedQuests() {
        List<MapleQuestStatus> ret = new LinkedList<MapleQuestStatus>();
        for (MapleQuestStatus q : quests.values()) {
            if (q.getStatus().equals(MapleQuestStatus.Status.COMPLETED)) {
                ret.add(q);
            }
        }
        return Collections.unmodifiableList(ret);
    }

    public Collection<MapleMonster> getControlledMonsters() {
        return Collections.unmodifiableCollection(controlled);
    }

    public int getCp() {
        return cp;
    }

    public List<MapleRing> getCrushRings() {
        Collections.sort(crushRings);
        return crushRings;
    }

    public int getCSPoints(int type) {
        switch (type) {
            case 1:
                return paypalnx;
            case 2:
                return maplepoints;
            default:
                return cardnx;
        }
    }

    public int getCurrentCI() {
        return ci;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public int getCurrentMaxHp() {
        return localmaxhp;
    }

    public int getCurrentMaxMp() {
        return localmaxmp;
    }

    public int getCurrentTab() {
        return currentTab;
    }

    public int getCurrentType() {
        return currentType;
    }

    public int getDex() {
        return dex;
    }

    public List<MapleDisease> getDiseases() {
        synchronized (diseases) {
            return Collections.unmodifiableList(diseases);
        }
    }

    public int getDojoEnergy() {
        return dojoEnergy;
    }

    public boolean getDojoParty() {
        return dojoParty;
    }

    public int getDojoPoints() {
        return dojoPoints;
    }

    public int getDojoStage() {
        return dojoStage;
    }

    public List<MapleDoor> getDoors() {
        return new ArrayList<MapleDoor>(doors);
    }

    public int getEnergyBar() {
        return energybar;
    }

    public EventInstanceManager getEventInstance() {
        return eventInstance;
    }

    public ArrayList<Integer> getExcluded() {
        return excluded;
    }

    public int getExp() {
        return exp.get();
    }

    public int getFace() {
        return face;
    }

    public int getFallCounter() {
        return fallcounter;
    }

    public int getFame() {
        return fame;
    }

    public MapleFamilyEntry getFamily() {
        return MapleFamily.getMapleFamily(this).getMember(getId());
    }

    public int getFamilyId() {
        return familyId;
    }

    public boolean getFinishedDojoTutorial() {
        return finishedDojoTutorial;
    }

    public List<MapleRing> getFriendshipRings() {
        Collections.sort(friendshipRings);
        return friendshipRings;
    }

    public int getGender() {
        return gender;
    }

    public int getGivenRiceCakes() {
        return givenRiceCakes;
    }

    public boolean getGMChat() {
        return whitechat;
    }

    public boolean getGottenRiceHat() {
        return gottenRiceHat;
    }

    public MapleGuild getGuild() {
        try {
            return client.getChannelServer().getWorldInterface().getGuild(getGuildId(), null);
        } catch (Exception ex) {
            return null;
        }
    }

    public int getGuildId() {
        return guildid;
    }

    public int getGuildRank() {
        return guildrank;
    }

    public int getHair() {
        return hair;
    }

    public HiredMerchant getHiredMerchant() {
        return hiredMerchant;
    }

    public int getHp() {
        return hp;
    }

    public int getHpMpApUsed() {
        return hpMpApUsed;
    }

    public int getId() {
        return id;
    }

    public static int getIdByName(String name) {
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT id FROM characters WHERE name = ?");
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            int id = -1;
            if (rs.next()) {
                id = rs.getInt("id");
            }
            rs.close();
            ps.close();
            return id;
        } catch (Exception e) {
        }
        return -1;
    }

    public int getInitialSpawnpoint() {
        return initialSpawnPoint;
    }

    public int getInt() {
        return int_;
    }

    public MapleInventory getInventory(MapleInventoryType type) {
        return inventory[type.ordinal()];
    }

    public int getItemEffect() {
        return itemEffect;
    }

    public int getItemQuantity(int itemid, boolean checkEquipped) {
        int possesed = inventory[MapleItemInformationProvider.getInstance().getInventoryType(itemid).ordinal()].countById(itemid);
        if (checkEquipped) {
            possesed += inventory[MapleInventoryType.EQUIPPED.ordinal()].countById(itemid);
        }
        return possesed;
    }

    public MapleJob getJob() {
        return job;
    }

    public int getJobRank() {
        return jobRank;
    }

    public int getJobRankMove() {
        return jobRankMove;
    }

    public int getJobType() {
        return job.getId() / 1000;
    }

    public Map<Integer, MapleKeyBinding> getKeymap() {
        return keymap;
    }

    public long getLastHealed() {
        return lastHealed;
    }

    public long getLastUsedCashItem() {
        return lastUsedCashItem;
    }

    public int getLevel() {
        return level;
    }

    public String getLinkedName() {
        return linkedName;
    }

    public int getLinkedLevel() {
        return linkedLevel;
    }

    public boolean isLinked() {
        return linkedName != null;
    }

    public int getLuk() {
        return luk;
    }

    public MapleMap getMap() {
        return map;
    }

    public int getMapId() {
        if (map != null) {
            return map.getId();
        }
        return mapid;
    }

    public List<MapleRing> getMarriageRings() {
        Collections.sort(marriageRings);
        return marriageRings;
    }

    public int getMarried() {
        return married;
    }

         public void gainItem(int id, short quantity) {
        gainItem(id, quantity, false);
    }

    public void gainItem(int id) {
        gainItem(id, (short) 1, false);
    }

    public void gainItem(int id, short quantity, boolean randomStats) {
        if (id >= 5000000 && id <= 5000100) {
            MapleInventoryManipulator.addById(player, id, (short) 1, null, MaplePet.createPet(id));
        }
        if (quantity >= 0) {
            MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            IItem item = ii.getEquipById(id);
            if (!MapleInventoryManipulator.checkSpace(player, id, quantity, "")) {
                player.getPlayer().dropMessage(1, "Your inventory is full. Please remove an item from your " + ii.getInventoryType(id).name() + " inventory.");
                return;
            }
            if (ii.getInventoryType(id).equals(MapleInventoryType.EQUIP) && !InventoryConstants.isRechargable(item.getItemId())) {
                if (randomStats) {
                    MapleInventoryManipulator.addFromDrop(player, ii.randomizeStats((Equip) item), false);
                } else {
                    MapleInventoryManipulator.addFromDrop(player, (Equip) item, false);
                }
            } else {
                MapleInventoryManipulator.addById(player, id, quantity);
            }
        } else {
            MapleInventoryManipulator.removeById(player, MapleItemInformationProvider.getInstance().getInventoryType(id), id, -quantity, true, false);
        }
        player.getSession().write(MaplePacketCreator.getShowItemGain(id, quantity, true));
    }

    public int getMasterLevel(ISkill skill) {
        if (skills.get(skill) == null) {
            return 0;
        }
        return skills.get(skill).masterlevel;
    }

    public int getMaxHp() {
        return maxhp;
    }

    public int getMaxLevel() {
        return isCygnus() ? 200 : 200;
    }

    public void BEGINNER() {
        int[] skillId = {1001, 1002, 1004, 1005
        };
        for (int skillzors_ : skillId) {
            maxSkill(skillzors_);
        }
    }
    public void WARRIOR() {
        int[] skillId = {1000000, 1000001, 1000002, 1001003, 1001004, 1001005
        };
        for (int skillzors_ : skillId) {
            maxSkill(skillzors_);
        }
    }
    public void FIGHTER() {
        int[] skillId = {1100000, 1100001, 1100002, 1100003, 1101004, 1101005, 1101006, 1101007
        };
        for (int skillzors_ : skillId) {
            maxSkill(skillzors_);
        }
    }
    public void CRUSADER() {
        int[] skillId = {1110000, 1110001, 1111002, 1111003, 1111004, 1111005, 1111006, 1111007, 1111008
        };
        for (int skillzors_ : skillId) {
            maxSkill(skillzors_);
        }
    }
    public void HERO() {
        int[] skillId = {1121000, 1121002, 1120003, 1120004, 1120005, 1121006, 1121008, 1121010, 1121011
        };
        for (int skillzors_ : skillId) {
            maxSkill(skillzors_);
        }
    }
    public void PAGE() {
        int[] skillId = {1300000, 1300001, 1300002, 1300003, 1301004, 1301005, 1301006, 1301007
        };
        for (int skillzors_ : skillId) {
            maxSkill(skillzors_);
        }
    }
    public void WHITE_KNIGHT() {
        int[] skillId = {1210000, 1210001, 1211002, 1211003, 1211004, 1211005, 1211006, 1211007, 1211008, 1211009
        };
        for (int skillzors_ : skillId) {
            maxSkill(skillzors_);
        }
    }
    public void PALADIN() {
        int[] skillId = {1221000, 1221001, 1221002, 1221003, 1221004, 1220005, 1220006, 1221007, 1221009, 1220010, 1221011, 1221012
        };
        for (int skillzors_ : skillId) {
            maxSkill(skillzors_);
        }
    }
    public void SPEARMAN() {
        int[] skillId = {1300000, 1300001, 1300002, 1300003, 1301004, 1301005, 1301006, 1301007
        };
        for (int skillzors_ : skillId) {
            maxSkill(skillzors_);
        }
    }
    public void DRAGON_KNIGHT() {
        int[] skillId = {1310000, 1311001, 1311002, 1311003, 1311004, 1311005, 1311006, 1311007, 1311008
        };
        for (int skillzors_ : skillId) {
            maxSkill(skillzors_);
        }
    }
    public void DARK_KNIGHT() {
        int[] skillId = {1321000, 1321001, 1321002, 1321003, 1320005, 1320006, 1321007, 1320008, 1320009, 1321010
        };
        for (int skillzors_ : skillId) {
            maxSkill(skillzors_);
        }
    }

    public void MAGICIAN() {
        int[] skillId = {2000000, 2000001, 2001002, 2001003, 2001004, 2001005
        };
        for (int skillzors_ : skillId) {
            maxSkill(skillzors_);
        }
    }
    public void FP_WIZARD() {
        int[] skillId = {2100000, 2101001, 2101002, 2101003, 2101004, 2101005
        };
        for (int skillzors_ : skillId) {
            maxSkill(skillzors_);
        }
    }
    public void FP_MAGE() {
        int[] skillId = {2110000, 2110001, 2111002, 2111003, 2111004, 2111005, 2111006
        };
        for (int skillzors_ : skillId) {
            maxSkill(skillzors_);
        }
    }
    public void FP_ARCH_MAGE() {
        int[] skillId = {2121000, 2121001, 2121002, 2121003, 2121004, 2121005, 2121006, 2121007, 2121008
        };
        for (int skillzors_ : skillId) {
            maxSkill(skillzors_);
        }
    }
    public void IL_WIZARD() {
        int[] skillId = {2200000, 2201001, 2201002, 2201003, 2201004, 2201005
        };
        for (int skillzors_ : skillId) {
            maxSkill(skillzors_);
        }
    }
    public void IL_MAGE() {
        int[] skillId = {2210000, 2210001, 2211002, 2211003, 2211004, 2211005, 2211006
        };
        for (int skillzors_ : skillId) {
            maxSkill(skillzors_);
        }
    }
    public void IL_ARCH_MAGE() {
        int[] skillId = {2221000, 2221001, 2221002, 2221003, 2221004, 2221005, 2221006, 2221007, 2221008
        };
        for (int skillzors_ : skillId) {
            maxSkill(skillzors_);
        }
    }
    public void CLERIC() {
        int[] skillId = {2300000, 2301001, 2301002, 2301003, 2301004, 2301005
        };
        for (int skillzors_ : skillId) {
            maxSkill(skillzors_);
        }
    }
    public void PRIEST() {
        int[] skillId = {2310000, 2311001, 2311002, 2311003, 2311004, 2311005, 2311006
        };
        for (int skillzors_ : skillId) {
            maxSkill(skillzors_);
        }
    }
    public void BISHOP() {
        int[] skillId = {2321000, 2321001, 2321002, 2321003, 2321004, 2321005, 2321006, 2321007, 2321008, 2321009
        };
        for (int skillzors_ : skillId) {
            maxSkill(skillzors_);
        }
    }

    public void BOWMAN() {
        int[] skillId = {3000000, 3000001, 3000002, 3001003, 3001004, 3001005
        };
        for (int skillzors_ : skillId) {
            maxSkill(skillzors_);
        }
    }
    public void HUNTER() {
        int[] skillId = {3100000, 3100001, 3101002, 3101003, 3101004, 3101005
        };
        for (int skillzors_ : skillId) {
            maxSkill(skillzors_);
        }
    }
    public void RANGER() {
        int[] skillId = {3110000, 3110001, 3111002, 3111003, 3111004, 3111005, 3111006
        };
        for (int skillzors_ : skillId) {
            maxSkill(skillzors_);
        }
    }
    public void BOW_MASTER() {
        int[] skillId = {3121000, 3121002, 3121003, 3121004, 3120005, 3121006, 3121007, 3121008, 3121009
        };
        for (int skillzors_ : skillId) {
            maxSkill(skillzors_);
        }
    }
    public void CROSS_BOWMAN() {
        int[] skillId = {3200000, 3200001, 3201002, 3201003, 3201004, 3201005
        };
        for (int skillzors_ : skillId) {
            maxSkill(skillzors_);
        }
    }
    public void SNIPER() {
        int[] skillId = {3210000, 3210001, 3211002, 3211003, 3211004, 3211005, 3211006
        };
        for (int skillzors_ : skillId) {
            maxSkill(skillzors_);
        }
    }
    public void MARKSMAN() {
        int[] skillId = {3221000, 3221001, 3221002, 3221003, 3220004, 3221005, 3221006, 3221007, 3221008
        };
        for (int skillzors_ : skillId) {
            maxSkill(skillzors_);
        }
    }

    public void THIEF() {
        int[] skillId = {4000000, 4000001, 4001002, 4001003, 4001334, 4001344
        };
        for (int skillzors_ : skillId) {
            maxSkill(skillzors_);
        }
    }
    public void ASSASSIN() {
        int[] skillId = {4100000, 4100001, 4100002, 4101003, 4101004, 4101005
        };
        for (int skillzors_ : skillId) {
            maxSkill(skillzors_);
        }
    }
    public void HERMIT() {
        int[] skillId = {4110000, 4111001, 4111002, 4111003, 4111004, 4111005, 4111006
        };
        for (int skillzors_ : skillId) {
            maxSkill(skillzors_);
        }
    }
    public void NIGHT_LORD() {
        int[] skillId = {4121000, 4120002, 4121003, 4121004, 4120005, 4121006, 4121007, 4121008, 4121009
        };
        for (int skillzors_ : skillId) {
            maxSkill(skillzors_);
        }
    }
    public void BANDIT() {
        int[] skillId = {4200000, 4200001, 4201002, 4201003, 4201004, 4201005
        };
        for (int skillzors_ : skillId) {
            maxSkill(skillzors_);
        }
    }
    public void CHIEF_BANDIT() {
        int[] skillId = {4210000, 4211001, 4211002, 4211003, 4211004, 4211005, 4211006
        };
        for (int skillzors_ : skillId) {
            maxSkill(skillzors_);
        }
    }
    public void SHADOWER() {
        int[] skillId = {4221000, 4220002, 4221003, 4221004, 4220005, 4221006, 4221007, 4221001, 4221008
        };
        for (int skillzors_ : skillId) {
            maxSkill(skillzors_);
        }
    }

    public void PIRATE() {
        int[] skillId = {5000000, 5001001, 5001002, 5001003, 5001005
        };
        for (int skillzors_ : skillId) {
            maxSkill(skillzors_);
        }
    }
    public void BRAWLER() {
        int[] skillId = {5100000, 5100001, 5101002, 5101003, 5101004, 5101005, 5101006, 5101007
        };
        for (int skillzors_ : skillId) {
            maxSkill(skillzors_);
        }
    }
    public void MARAUDER() {
        int[] skillId = {5110000, 5110001, 5111002, 5111004, 5111005, 5111006
        };
        for (int skillzors_ : skillId) {
            maxSkill(skillzors_);
        }
    }
    public void BUCCANEER() {
        int[] skillId = {5121000, 5121001, 5121002, 5121003, 5121004, 5121005, 5121007, 5121008, 5121009, 5121010
        };
        for (int skillzors_ : skillId) {
            maxSkill(skillzors_);
        }
    }
    public void GUNSLINGER() {
        int[] skillId = {5200000, 5201001, 5201002, 5201003, 5201004, 5201005, 5201006
        };
        for (int skillzors_ : skillId) {
            maxSkill(skillzors_);
        }
    }
    public void OUTLAW() {
        int[] skillId = {5210000, 5211001, 5211002, 5211004, 5211005, 5211006
        };
        for (int skillzors_ : skillId) {
            maxSkill(skillzors_);
        }
    }
    public void CORSAIR() {
        int[] skillId = {5220001, 5220002, 5220011, 5221000, 5221003, 5221004, 5221006, 5221007, 5221008, 5221009, 5221010
        };
        for (int skillzors_ : skillId) {
            maxSkill(skillzors_);
        }
    }

    public void MAPLE_LEAF_WATCH_BRIGADER() {
        int[] skillId = {
        };
        for (int skillzors_ : skillId) {
            maxSkill(skillzors_);
        }
    }
    public void GM() {
        int[] skillId = {
        };
        for (int skillzors_ : skillId) {
            maxSkill(skillzors_);
        }
    }
    public void SUPER_GM() {
        int[] skillId = {
        };
        for (int skillzors_ : skillId) {
            maxSkill(skillzors_);
        }
    }

    public void NOBLESSE() {
        int[] skillId = {10001001, 10001002, 10001000, 10001004, 10001005
        };
        for (int skillzors_ : skillId) {
            maxSkill(skillzors_);
        }
    }

    public void DAWN_WARRIOR_1() {
        int[] skillId = {11000000, 11001001, 11001002, 11001003, 11001004
        };
        for (int skillzors_ : skillId) {
            maxSkill(skillzors_);
        }
    }
    public void DAWN_WARRIOR_2() {
        int[] skillId = {11100000, 11101001, 11101002, 11101003, 11101004, 11101005
        };
        for (int skillzors_ : skillId) {
            maxSkill(skillzors_);
        }
    }
    public void DAWN_WARRIOR_3() {
        int[] skillId = {11110000, 11111001, 11111002, 11111003, 11111004, 11110005, 11111006, 11111007
        };
        for (int skillzors_ : skillId) {
            maxSkill(skillzors_);
        }
    }

    public void BLAZE_WIZARD_1() {
        int[] skillId = {12000000, 12001001, 12001002, 12001003, 12001004
        };
        for (int skillzors_ : skillId) {
            maxSkill(skillzors_);
        }
    }
    public void BLAZE_WIZARD_2() {
        int[] skillId = {12101000, 12101001, 12101002, 12101003, 12101004, 12101005, 12101006
        };
        for (int skillzors_ : skillId) {
            maxSkill(skillzors_);
        }
    }
    public void BLAZE_WIZARD_3() {
        int[] skillId = {12110000, 12110001, 12111002, 12111003, 12111004, 12111005, 12111006
        };
        for (int skillzors_ : skillId) {
            maxSkill(skillzors_);
        }
    }

    public void WIND_ARCHER_1() {
        int[] skillId = {13000001, 13001002, 13001003, 13000000, 13001004
        };
        for (int skillzors_ : skillId) {
            maxSkill(skillzors_);
        }
    }
    public void WIND_ARCHER_2() {
        int[] skillId = {13100000, 13101001, 13101002, 13101003, 13101005, 13101006
        };
        for (int skillzors_ : skillId) {
            maxSkill(skillzors_);
        }
    }
    public void WIND_ARCHER_3() {
        int[] skillId = {13111000, 13111001, 13110003, 13111002, 13111004, 13111005, 13111006, 13111007
        };
        for (int skillzors_ : skillId) {
            maxSkill(skillzors_);
        }
    }

    public void NIGHT_WALKER_1() {
        int[] skillId = {14000000, 14000001, 14001002, 14001003, 14001004, 14001005
        };
        for (int skillzors_ : skillId) {
            maxSkill(skillzors_);
        }
    }
    public void NIGHT_WALKER_2() {
        int[] skillId = {14100000, 14100001, 14101002, 14101003, 14101004, 14100005, 14101006
        };
        for (int skillzors_ : skillId) {
            maxSkill(skillzors_);
        }
    }
    public void NIGHT_WALKER_3() {
        int[] skillId = {14111000, 14111001, 14111002, 14110003, 14110004, 14111005, 14111006
        };
        for (int skillzors_ : skillId) {
            maxSkill(skillzors_);
        }
    }

    public void THUNDER_BREAKER_1() {
        int[] skillId = {5000000, 15001001, 15001002, 15001003, 15001004
        };
        for (int skillzors_ : skillId) {
            maxSkill(skillzors_);
        }
    }
    public void THUNDER_BREAKER_2() {
        int[] skillId = {15100000, 15100001, 15100004, 15101002, 15101003, 15101005, 15101006
        };
        for (int skillzors_ : skillId) {
            maxSkill(skillzors_);
        }
    }
    public void THUNDER_BREAKER_3() {
        int[] skillId = {15110000, 15111001, 15111002, 15111003, 15111004, 15111005, 15111006, 15111007
        };
        for (int skillzors_ : skillId) {
            maxSkill(skillzors_);
        }
    }

    public void LEGEND() {
        int[] skillId = {20001001, 20001002, 20001000, 20001004, 20001005
        };
        for (int skillzors_ : skillId) {
            maxSkill(skillzors_);
        }
    }

    public void ARAN_1() {
        int[] skillId = {21000000, 21000002, 21001003, 21001001
        };
        for (int skillzors_ : skillId) {
            maxSkill(skillzors_);
        }
    }
    public void ARAN_2() {
        int[] skillId = {21100000, 21100001, 21100002, 21100003, 21100004, 21100005
        };
        for (int skillzors_ : skillId) {
            maxSkill(skillzors_);
        }
    }
    public void ARAN_3() {
        int[] skillId = {21110000, 21110003, 21110004, 21110006, 21110002, 21111003, 21110005
        };
        for (int skillzors_ : skillId) {
            maxSkill(skillzors_);
        }
    }
    public void ARAN_4() {
        int[] skillId = {21120001, 21120004, 21120005, 21120006, 21120007, 21120002, 21121000, 21121003, 21121008
        };
        for (int skillzors_ : skillId) {
            maxSkill(skillzors_);
        }
    }

    public int getMaxMp() {
        return maxmp;
    }

    public int getMeso() {
        return meso.get();
    }

    public int getMesosTraded() {
        return mesosTraded;
    }

    public int getMessengerPosition() {
        return messengerposition;
    }

    public MapleGuildCharacter getMGC() {
        return mgc;
    }

    public MapleMiniGame getMiniGame() {
        return miniGame;
    }

    public int getMiniGamePoints(String type, boolean omok) {
        if (omok) {
            if (type.equals("wins")) {
                return omokwins;
            } else if (type.equals("losses")) {
                return omoklosses;
            } else {
                return omokties;
            }
        } else {
            if (type.equals("wins")) {
                return matchcardwins;
            } else if (type.equals("losses")) {
                return matchcardlosses;
            } else {
                return matchcardties;
            }
        }
    }

    public MonsterBook getMonsterBook() {
        return monsterbook;
    }

    public int getMonsterBookCover() {
        return bookCover;
    }

    public MapleMount getMount() {
        return maplemount;
    }

    public int getMp() {
        return mp;
    }

    public MapleMessenger getMessenger() {
        return messenger;
    }

    public String getName() {
        return name;
    }

    public int getNextEmptyPetIndex() {
        for (int i = 0; i < 3; i++) {
            if (pets[i] == null) {
                return i;
            }
        }
        return 3;
    }

    public int getNoPets() {
        int ret = 0;
        for (int i = 0; i < 3; i++) {
            if (pets[i] != null) {
                ret++;
            }
        }
        return ret;
    }

    public int getNumControlledMonsters() {
        return controlled.size();
    }

    public MapleParty getParty() {
        return party;
    }

    public int getPartyId() {
        return (party != null ? party.getId() : -1);
    }

    public MaplePet getPet(int index) {
        return pets[index];
    }

    public int getPetIndex(int petId) {
        for (int i = 0; i < 3; i++) {
            if (pets[i] != null) {
                if (pets[i].getUniqueId() == petId) {
                    return i;
                }
            }
        }
        return -1;
    }

    public int getPetIndex(MaplePet pet) {
        for (int i = 0; i < 3; i++) {
            if (pets[i] != null) {
                if (pets[i].getUniqueId() == pet.getUniqueId()) {
                    return i;
                }
            }
        }
        return -1;
    }

    public MaplePlayerShop getPlayerShop() {
        return playerShop;
    }

    public MaplePet[] getPets() {
        return pets;
    }

    public int getPossibleReports() {
        return possibleReports;
    }

    public MapleQuestStatus getQuest(MapleQuest quest) {
        if (!quests.containsKey(quest)) {
            return new MapleQuestStatus(quest, MapleQuestStatus.Status.NOT_STARTED);
        }
        return quests.get(quest);
    }

    public int getRank() {
        return rank;
    }

    public int getRankMove() {
        return rankMove;
    }

    public int getRemainingAp() {
        return remainingAp;
    }

    public int getRemainingSp() {
        return remainingSp;
    }

    public int getSavedLocation(String type) {
        int m = savedLocations[SavedLocationType.fromString(type).ordinal()].getMapId();
        if (!SavedLocationType.fromString(type).equals(SavedLocationType.WORLDTOUR)) {
            clearSavedLocation(SavedLocationType.fromString(type));
        }
        return m;
    }

    public String getSearch() {
        return search;
    }

    public void changeOccupation(MapleOccupations newoccupation) {
        this.occupation = newoccupation;
    }

    public void Setoccupation(int occ) {
        changeOccupation(MapleOccupations.getById(occ));
    }

    public MapleOccupations getOccupation() {
        return occupation;
    }
    private static ChannelServer cserv;

    public int[] getOccupationRates() { 
        int exprate = ServerConstants.EXP_RATE;
        int mesorate = ServerConstants.MESO_RATE;
        int droprate = ServerConstants.DROP_RATE;
        if (getOccupation().isA(MapleOccupations.Ziva)) {
            exprate = ServerConstants.EXP_RATE ;
            mesorate = ServerConstants.MESO_RATE;
            droprate = ServerConstants.DROP_RATE;
        } else if (getOccupation().isA(MapleOccupations.Hades)) {
            exprate = ServerConstants.EXP_RATE ;
            mesorate = ServerConstants.MESO_RATE ;
            droprate = ServerConstants.DROP_RATE;
                    } else if (getOccupation().isA(MapleOccupations.Ares)) {
            exprate = ServerConstants.EXP_RATE ;
            mesorate = ServerConstants.MESO_RATE ;
            droprate = ServerConstants.DROP_RATE;
                    } else if (getOccupation().isA(MapleOccupations.Aphrodite)) {
            exprate = ServerConstants.EXP_RATE ;
            mesorate = ServerConstants.MESO_RATE ;
            droprate = ServerConstants.DROP_RATE;
        } else if (getOccupation().isA(MapleOccupations.NOJOB)) {
            exprate = ServerConstants.EXP_RATE;
            mesorate = ServerConstants.MESO_RATE;
            droprate = ServerConstants.DROP_RATE;
        } else {
            exprate = ServerConstants.EXP_RATE;
            mesorate = ServerConstants.MESO_RATE;
            droprate = ServerConstants.DROP_RATE;
        }
        return new int[]{exprate, mesorate, droprate};
    }

    public int getExpRate() {
        return getOccupationRates()[0];
    }

    public int getMesoRate() {
        return getOccupationRates()[1];
    }

    public int getDropRate() {
        return getOccupationRates()[2];
    }

    public MapleShop getShop() {
        return shop;
    }

    public Map<ISkill, SkillEntry> getSkills() {
        return Collections.unmodifiableMap(skills);
    }

    public int getSkillLevel(int skill) {
        SkillEntry ret = skills.get(SkillFactory.getSkill(skill));
        if (ret == null) {
            return 0;
        }
        return ret.skillevel;
    }

    public int getSkillLevel(ISkill skill) {
        if (skills.get(skill) == null) {
            return 0;
        }
        return skills.get(skill).skillevel;
    }

    public MapleSkinColor getSkinColor() {
        return skinColor;
    }

    public final List<MapleQuestStatus> getStartedQuests() {
        List<MapleQuestStatus> ret = new LinkedList<MapleQuestStatus>();
        for (MapleQuestStatus q : quests.values()) {
            if (q.getStatus().equals(MapleQuestStatus.Status.STARTED)) {
                ret.add(q);
            }
        }
        return Collections.unmodifiableList(ret);
    }

    public MapleStatEffect getStatForBuff(MapleBuffStat effect) {
        MapleBuffStatValueHolder mbsvh = effects.get(effect);
        if (mbsvh == null) {
            return null;
        }
        return mbsvh.effect;
    }

    public MapleStorage getStorage() {
        return storage;
    }

    public int getStr() {
        return str;
    }

    public Map<Integer, MapleSummon> getSummons() {
        return summons;
    }

    public List<Integer> getTeleportRockMaps(int type) {
        if (type == 0) {
            return rockMaps;
        } else {
            return vipRockMaps;
        }
    }

    public int getTotalCp() {
        return totalCp;
    }

    public int getTotalLuk() {
        return localluk;
    }

    public int getTotalMagic() {
        return magic;
    }

    public int getTotalWatk() {
        return watk;
    }

    public int getState() {
        return state;
    }

    public void setState(int s) {
        state = s;
    }

    public MapleTrade getTrade() {
        return trade;
    }

    public int getVanquisherKills() {
        return vanquisherKills;
    }

    public int getVanquisherStage() {
        return vanquisherStage;
    }

    public Collection<MapleMapObject> getVisibleMapObjects() {
        return Collections.unmodifiableCollection(visibleMapObjects);
    }

    public List<Integer> getWishList() {
        return wishList;
    }

    public int getWorld() {
        return world;
    }

    public void giveCoolDowns(final int skillid, long starttime, long length) {
        int time = (int) ((length + starttime) - System.currentTimeMillis());
        addCooldown(skillid, System.currentTimeMillis(), time, TimerManager.getInstance().schedule(new CancelCooldownAction(this, skillid), time));
    }

    public void giveDebuff(MapleDisease disease, MobSkill skill) {
        if (diseases.size() < 4) {
            List<Pair<MapleDisease, Integer>> disease_ = new ArrayList<Pair<MapleDisease, Integer>>();
            disease_.add(new Pair<MapleDisease, Integer>(disease, Integer.valueOf(skill.getX())));
            this.diseases.add(disease);
            client.getSession().write(MaplePacketCreator.giveDebuff(disease_, skill));
            getMap().broadcastMessage(this, MaplePacketCreator.giveForeignDebuff(this.id, disease_, skill), false);
        }
    }

    public int gmLevel() {
        return gmLevel;
    }

    public int getESP() {
        return ESP;
    }

        public void doReborn() {
        setReborns(getReborns() + 1);
        List<Pair<MapleStat, Integer>> reborn = new ArrayList<Pair<MapleStat, Integer>>(4);
        setLevel(1);
        setExp(0);
        setJob(MapleJob.BEGINNER);
        updateSingleStat(MapleStat.LEVEL, 1);
        updateSingleStat(MapleStat.JOB, 0);
        updateSingleStat(MapleStat.EXP, 0);
    }
        public void doReborn1() {
        setReborns(getReborns() + 1);
        List<Pair<MapleStat, Integer>> reborn = new ArrayList<Pair<MapleStat, Integer>>(4);
        setLevel(1);
        setExp(0);
        setJob(MapleJob.NOBLESSE);
        updateSingleStat(MapleStat.LEVEL, 1);
        updateSingleStat(MapleStat.JOB, 1000);
        updateSingleStat(MapleStat.EXP, 0);
    }
public void doReborn2() {
        setReborns(getReborns() + 1);
        List<Pair<MapleStat, Integer>> reborn = new ArrayList<Pair<MapleStat, Integer>>(4);
        setLevel(1);
        setExp(0);
        setJob(MapleJob.LEGEND);
        updateSingleStat(MapleStat.LEVEL, 1);
        updateSingleStat(MapleStat.JOB, 2000);
        updateSingleStat(MapleStat.EXP, 0);
    }

    public boolean gotPartyQuestItem(String partyquestchar) {
        return partyquestitems.contains(partyquestchar);
    }

    public String guildCost() {
        return nf.format(MapleGuild.CREATE_GUILD_COST);
    }

    private void guildUpdate() {
        if (this.guildid < 1) {
            return;
        }
        mgc.setLevel(level);
        mgc.setJobId(job.getId());
        try {
            this.client.getChannelServer().getWorldInterface().memberLevelJobUpdate(this.mgc);
            int allianceId = getGuild().getAllianceId();
            if (allianceId > 0) {
                client.getChannelServer().getWorldInterface().allianceMessage(allianceId, MaplePacketCreator.updateAllianceJobLevel(this), getId(), -1);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void handleEnergyChargeGain() { // to get here energychargelevel has to be > 0
        ISkill energycharge = isCygnus() ? SkillFactory.getSkill(ThunderBreaker.ENERGY_CHARGE) : SkillFactory.getSkill(Marauder.ENERGY_CHARGE);
        MapleStatEffect ceffect = null;
        ceffect = energycharge.getEffect(getSkillLevel(energycharge));
        TimerManager tMan = TimerManager.getInstance();
        if (energybar < 10000) {
            energybar += 102;
            if (energybar > 10000) {
                energybar = 10000;
            }
            client.getSession().write(MaplePacketCreator.giveEnergyCharge(energybar));
            client.getSession().write(MaplePacketCreator.showOwnBuffEffect(energycharge.getId(), 2));
            getMap().broadcastMessage(this, MaplePacketCreator.showBuffeffect(id, energycharge.getId(), 2));
            if (energybar == 10000) {
                getMap().broadcastMessage(this, MaplePacketCreator.giveForeignEnergyCharge(id, energybar));
            }
        }
        if (energybar >= 10000) {
            energybar = 15000;
            final MapleCharacter chr = this;
            tMan.schedule(new Runnable() {

                @Override
                public void run() {
                    client.getSession().write(MaplePacketCreator.giveEnergyCharge(0));
                    getMap().broadcastMessage(chr, MaplePacketCreator.giveForeignEnergyCharge(id, energybar));
                    energybar = 0;
                }
            }, ceffect.getDuration());
        }
    }
    public void gainESP(int amt) {
        ESP += amt;
    }

public int getCookingEXP(){
        return this.CookingExp;

    }
public void setCookingLevel(int x){
        this.CookingLevel = x;
    }
public int getCookingLevel(){
        return this.CookingLevel;
    }
    public void handleOrbconsume() {
        int skillid = isCygnus() ? DawnWarrior.COMBO : Crusader.COMBO;
        ISkill combo = SkillFactory.getSkill(skillid);
        List<Pair<MapleBuffStat, Integer>> stat = Collections.singletonList(new Pair<MapleBuffStat, Integer>(MapleBuffStat.COMBO, 1));
        setBuffedValue(MapleBuffStat.COMBO, 1);
        client.getSession().write(MaplePacketCreator.giveBuff(skillid, combo.getEffect(getSkillLevel(combo)).getDuration() + (int) ((getBuffedStarttime(MapleBuffStat.COMBO) - System.currentTimeMillis())), stat, false, false, getMount()));
        getMap().broadcastMessage(this, MaplePacketCreator.giveForeignBuff(getId(), stat, false), false);
    }
public int getExpNeededForcookingLevel(int level) {
        return ExpTable.getCookingNeededForLevel(level);
    }

public void gainCookingEXP(int amount){
        int totoexp = this.CookingExp + amount;
        if(totoexp >= ExpTable.getCookingNeededForLevel(CookingLevel) && this.getCookingLevel() < 150){
            CookingLevelUp();
        }
        else {
            CookingExp += amount;
                    getClient().getSession().write(MaplePacketCreator.showSpecialEffect(9));
            getClient().getSession().write(MaplePacketCreator.serverNotice(5, "You have gain " + amount + " Cooking EXP! This is your Cooking EXP Table : " + this.getCookingEXP() + " / " + this.getExpNeededForcookingLevel(this.getCookingLevel()) +" "));
}

    }
public void CookingLevelUp(){
        getMap().broadcastMessage(getClient().getPlayer(), MaplePacketCreator.showSpecialEffect(8), false);
        getClient().getSession().write(MaplePacketCreator.showSpecialEffect(0));
        this.CookingLevel += 1;
        this.CookingExp = 0;
        this.remainingAp += 250;
        this.updateSingleStat(MapleStat.MAXHP, maxhp);
        this.updateSingleStat(MapleStat.MAXMP, maxmp);
        this.updateSingleStat(MapleStat.AVAILABLEAP, remainingAp);
        getClient().getSession().write(MaplePacketCreator.serverNotice(5, "Congrats! Your Cooking Level has leveled up! You are now a Lv." + this.getCookingLevel() + " Chef!"));
getClient().getSession().write(MaplePacketCreator.serverNotice(1, "You are now a Lv." + this.getCookingLevel() + " Chef!"));

    }
    public boolean hasEntered(String script) {
        for (int mapId : entered.keySet()) {
            if (entered.get(mapId).equals(script)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasEntered(String script, int mapId) {
        if (entered.containsKey(mapId)) {
            if (entered.get(mapId).equals(script)) {
                return true;
            }
        }
        return false;
    }

    public void hasGivenFame(MapleCharacter to) {
        lastfametime = System.currentTimeMillis();
        lastmonthfameids.add(Integer.valueOf(to.getId()));
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("INSERT INTO famelog (characterid, characterid_to) VALUES (?, ?)");
            ps.setInt(1, getId());
            ps.setInt(2, to.getId());
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
        }
    }

    public boolean hasMerchant() {
        return hasMerchant;
    }

    public boolean hasWatchedCygnusIntro() {
        return watchedCygnusIntro;
    }

    public boolean haveItem(int itemid) {
        return getItemQuantity(itemid, false) > 0;
    }

    public boolean haveItemEquipped(int itemid) {
        if (getInventory(MapleInventoryType.EQUIPPED).findById(itemid) != null) {
            return true;
        }
        return false;
    }

    public void increaseCp(int amount) {
        this.cp += amount;
    }

    public void increaseTotalCp(int amount) {
        this.totalCp += amount;
    }

    public void increaseGivenRiceCakes(int amount) {
        this.givenRiceCakes += amount;
    }

    public void increaseGuildCapacity() { //hopefully nothing is null
        if (getMeso() < getGuild().getIncreaseGuildCost(getGuild().getCapacity())) {
            dropMessage(1, "You don't have enough mesos.");
            return;
        }
        try {
            client.getChannelServer().getWorldInterface().increaseGuildCapacity(guildid);
        } catch (RemoteException e) {
            client.getChannelServer().reconnectWorld();
            return;
        }
        gainMeso(-getGuild().getIncreaseGuildCost(getGuild().getCapacity()), true, false, false);
    }

    public boolean inCS() {
        return incs;
    }

    public boolean inMTS() {
        return inmts;
    }

    public boolean isActiveBuffedValue(int skillid) {
        LinkedList<MapleBuffStatValueHolder> allBuffs = new LinkedList<MapleBuffStatValueHolder>(effects.values());
        for (MapleBuffStatValueHolder mbsvh : allBuffs) {
            if (mbsvh.effect.isSkill() && mbsvh.effect.getSourceId() == skillid) {
                return true;
            }
        }
        return false;
    }

    public boolean isAlive() {
        return hp > 0;
    }

    public boolean isBuffFrom(MapleBuffStat stat, ISkill skill) {
        MapleBuffStatValueHolder mbsvh = effects.get(stat);
        if (mbsvh == null) {
            return false;
        }
        return mbsvh.effect.isSkill() && mbsvh.effect.getSourceId() == skill.getId();
    }

    public void maxSkill(int skillid) {
        if (Math.floor(skillid / 10000) == getJob().getId() || isGM() || skillid < 2000) { // lmao im lazy
            ISkill skill_ = SkillFactory.getSkill(skillid);
            int maxlevel = skill_.getMaxLevel(); // TODO - Find a less laggy way.. our xml style skill maxer was fine T____T
            changeSkillLevel(skill_, maxlevel, maxlevel);
        }
    }

    public boolean isCygnus() {
        return getJobType() == 1;
    }

    public boolean isAran() {
        return getJobType() == 2;
    }

    public boolean isGM() {
        return gmLevel >= 1;
    }

public boolean isSGM() {
        return gmLevel >= 2;
    }
    public boolean isHidden() {
        return hidden;
    }

    public boolean isMapObjectVisible(MapleMapObject mo) {
        return visibleMapObjects.contains(mo);
    }

    public boolean isPartyLeader() {
        return party.getLeader() == party.getMemberById(getId());
    }

    public void leaveMap() {
        controlled.clear();
        visibleMapObjects.clear();
        if (chair != 0) {
            chair = 0;
        }
        if (hpDecreaseTask != null) {
            hpDecreaseTask.cancel(false);
        }
    }

    public void levelUp(boolean takeexp) {
        ISkill improvingMaxHP = null;
        ISkill improvingMaxMP = null;
        int improvingMaxHPLevel = 0;
        int improvingMaxMPLevel = 0;
        if (isCygnus() && level < 70) {
            remainingAp++;
        }
        remainingAp += 5;
        int jobtype = job.getId() / 100 % 10;
        if (jobtype == 0) {
            maxhp += rand(12, 16);
            maxmp += rand(10, 12);
        } else if (jobtype == 1) {
            if (isAran()) {
                maxhp += rand(44, 48);
                maxmp += rand(9, 10);
            } else {
                improvingMaxHP = isCygnus() ? SkillFactory.getSkill(DawnWarrior.MAX_HP_INCREASE) : SkillFactory.getSkill(Swordsman.IMPROVED_MAX_HP_INCREASE);
                if (job.isA(MapleJob.CRUSADER)) {
                    improvingMaxMP = SkillFactory.getSkill(1210000);
                } else if (job.isA(MapleJob.DAWNWARRIOR2)) {
                    improvingMaxMP = SkillFactory.getSkill(11110000);
                }
                improvingMaxHPLevel = getSkillLevel(improvingMaxHP);
                maxhp += rand(24, 28);
                maxmp += rand(4, 6);
            }
        } else if (jobtype == 2) {
            improvingMaxMP = isCygnus() ? SkillFactory.getSkill(BlazeWizard.INCREASING_MAX_MP) : SkillFactory.getSkill(Magician.IMPROVED_MAX_MP_INCREASE);
            improvingMaxMPLevel = getSkillLevel(improvingMaxMP);
            maxhp += rand(10, 14);
            maxmp += rand(22, 24);
        } else if (jobtype <= 4) {
            maxhp += rand(20, 24);
            maxmp += rand(14, 16);
        } else if (jobtype == 5) {
            improvingMaxHP = isCygnus() ? SkillFactory.getSkill(ThunderBreaker.IMPROVE_MAX_HP) : SkillFactory.getSkill(5100000);
            improvingMaxHPLevel = getSkillLevel(improvingMaxHP);
            maxhp += rand(22, 28);
            maxmp += rand(18, 23);
        } else if (jobtype == 9) {
            maxhp = 30000;
            maxmp = 30000;
        }
        if (improvingMaxHPLevel > 0 && (jobtype == 1 || job.isA(MapleJob.PIRATE))) {
            maxhp += improvingMaxHP.getEffect(improvingMaxHPLevel).getX();
        }
        if (improvingMaxMPLevel > 0 && (jobtype == 2 || job.isA(MapleJob.CRUSADER))) {
            maxmp += improvingMaxMP.getEffect(improvingMaxMPLevel).getX();
        }
        maxmp += localint_ / 10;
        if (takeexp) {
            exp.addAndGet(-ExpTable.getExpNeededForLevel(level));
            if (exp.get() < 0) {
                exp.set(0);
            }
        }
        level++;
        if (level >= getMaxLevel()) {
            exp.set(0);
        }
        maxhp = Math.min(30000, maxhp);
        maxmp = Math.min(30000, maxmp);
        if (level == 200) {
            exp.set(0);
        }
        hp = maxhp;
        mp = maxmp;
        recalcLocalStats();
        List<Pair<MapleStat, Integer>> statup = new ArrayList<Pair<MapleStat, Integer>>(8);
        statup.add(new Pair<MapleStat, Integer>(MapleStat.AVAILABLEAP, remainingAp));
        statup.add(new Pair<MapleStat, Integer>(MapleStat.HP, localmaxhp));
        statup.add(new Pair<MapleStat, Integer>(MapleStat.MP, localmaxmp));
        statup.add(new Pair<MapleStat, Integer>(MapleStat.EXP, exp.get()));
        statup.add(new Pair<MapleStat, Integer>(MapleStat.LEVEL, level));
        statup.add(new Pair<MapleStat, Integer>(MapleStat.MAXHP, maxhp));
        statup.add(new Pair<MapleStat, Integer>(MapleStat.MAXMP, maxmp));
        if (jobtype > 0) {
            remainingSp += 3;
            statup.add(new Pair<MapleStat, Integer>(MapleStat.AVAILABLESP, remainingSp));
        }
        client.getSession().write(MaplePacketCreator.updatePlayerStats(statup));
        getMap().broadcastMessage(this, MaplePacketCreator.showForeignEffect(getId(), 0), false);
        recalcLocalStats();
        silentPartyUpdate();
        guildUpdate();
        if (this.guildid > 0) { // to do, make it not show to self
            this.getGuild().broadcast(MaplePacketCreator.serverNotice(5, "[Guild] " + name + " has reached Lv. " + level + "."));
        }
	if (rebirth == 200)	{
        client.getChannelServer().broadcastPacket(MaplePacketCreator.serverNotice(6, "[Congrats] " + name + " has reached 200 Rebirth!" + name + ", that's quite alot already!"));
        }
        if (rebirth == 100)	{
        client.getChannelServer().broadcastPacket(MaplePacketCreator.serverNotice(6, "[Congrats] " + name + " has reached 100 Rebirth!" + name + " is such a hard working trainer!"));
        }
        if (rebirth == 500)	{
        client.getChannelServer().broadcastPacket(MaplePacketCreator.serverNotice(6, "[Congrats] " + name + " has reached 500 Rebirth!" + name + " come on this insane!"));
        }
        if (rebirth == 1000)	{
        client.getChannelServer().broadcastPacket(MaplePacketCreator.serverNotice(6, "[Congrats] " + name + " has reached 1000 Rebirth! You are unbelievable!!"));
        }
    }

    public static MapleCharacter loadCharFromDB(int charid, MapleClient client, boolean channelserver) throws SQLException {
        try {
            MapleCharacter ret = new MapleCharacter();
            ret.client = client;
            ret.id = charid;
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT * FROM characters WHERE id = ?");
            ps.setInt(1, charid);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                rs.close();
                ps.close();
                throw new RuntimeException("Loading char failed (not found)");
            }
            ret.name = rs.getString("name");
            ret.level = rs.getInt("level");
            ret.fame = rs.getInt("fame");
            ret.str = rs.getInt("str");
            ret.dex = rs.getInt("dex");
            ret.int_ = rs.getInt("int");
            ret.luk = rs.getInt("luk");
            ret.exp.set(rs.getInt("exp"));
            ret.hp = rs.getInt("hp");
            ret.reborns = rs.getInt("reborns");
            ret.FishingExp = rs.getInt("fishingexp");
            ret.FishingLevel = rs.getInt("fishinglevel");
            ret.ESP = rs.getInt("ESP");
            ret.CookingExp = rs.getInt("cookingexp");
            ret.CookingLevel = rs.getInt("cookinglevel");
            ret.maxhp = rs.getInt("maxhp");
            ret.mp = rs.getInt("mp");
            ret.maxmp = rs.getInt("maxmp");
            ret.hpMpApUsed = rs.getInt("hpMpUsed");
            ret.hasMerchant = rs.getInt("HasMerchant") == 1;
            ret.remainingSp = rs.getInt("sp");
            ret.remainingAp = rs.getInt("ap");
            ret.meso.set(rs.getInt("meso"));
            ret.gmLevel = rs.getInt("gm");
            ret.skinColor = MapleSkinColor.getById(rs.getInt("skincolor"));
            ret.gender = rs.getInt("gender");
            ret.job = MapleJob.getById(rs.getInt("job"));
            ret.occupation = MapleOccupations.getById(rs.getInt("occupation"));
            ret.finishedDojoTutorial = rs.getInt("finishedDojoTutorial") == 1;
            ret.vanquisherKills = rs.getInt("vanquisherKills");
            ret.omokwins = rs.getInt("omokwins");
            ret.omoklosses = rs.getInt("omoklosses");
            ret.omokties = rs.getInt("omokties");
            ret.matchcardwins = rs.getInt("matchcardwins");
            ret.matchcardlosses = rs.getInt("matchcardlosses");
            ret.matchcardties = rs.getInt("matchcardties");
            ret.receivedMOTB = rs.getInt("receivedMOTB") == 1;
            ret.hair = rs.getInt("hair");
            ret.face = rs.getInt("face");
            ret.accountid = rs.getInt("accountid");
            ret.mapid = rs.getInt("map");
            ret.initialSpawnPoint = rs.getInt("spawnpoint");
            ret.world = rs.getInt("world");
            ret.rank = rs.getInt("rank");
            ret.rankMove = rs.getInt("rankMove");
            ret.jobRank = rs.getInt("jobRank");
            ret.jobRankMove = rs.getInt("jobRankMove");
            int mountexp = rs.getInt("mountexp");
            int mountlevel = rs.getInt("mountlevel");
            int mounttiredness = rs.getInt("mounttiredness");
            ret.guildid = rs.getInt("guildid");
            ret.guildrank = rs.getInt("guildrank");
            ret.allianceRank = rs.getInt("allianceRank");
            ret.familyId = rs.getInt("familyId");
            ret.bookCover = rs.getInt("monsterbookcover");
            ret.monsterbook = new MonsterBook();
            ret.monsterbook.loadCards(charid);
            ret.watchedCygnusIntro = rs.getInt("watchedcygnusintro") == 1;
            ret.vanquisherStage = rs.getInt("vanquisherStage");
            ret.dojoPoints = rs.getInt("dojoPoints");
            ret.dojoStage = rs.getInt("lastDojoStage");
            ret.whitechat = ret.gmLevel > 0;
            ret.givenRiceCakes = rs.getInt("givenRiceCakes");
            ret.partyquestitems = rs.getString("partyquestitems");
            if (ret.guildid > 0) {
                ret.mgc = new MapleGuildCharacter(ret);
            }
            int buddyCapacity = rs.getInt("buddyCapacity");
            ret.buddylist = new BuddyList(buddyCapacity);

            for (byte i = 1; i <= 4; i++) {
                MapleInventoryType type = MapleInventoryType.getByType(i);
                ret.getInventory(type).setSlotLimit(rs.getInt(type.name().toLowerCase() + "slots"));
            }

            for (Pair<IItem, MapleInventoryType> item : ItemFactory.INVENTORY.loadItems(ret.id, !channelserver)) {
                ret.getInventory(item.getRight()).addFromDB(item.getLeft());
            }

            if (channelserver) {
                MapleMapFactory mapFactory = client.getChannelServer().getMapFactory();
                ret.map = mapFactory.getMap(ret.mapid);
                if (ret.map == null) {
                    ret.map = mapFactory.getMap(0);
                }
                MaplePortal portal = ret.map.getPortal(ret.initialSpawnPoint);
                if (portal == null) {
                    portal = ret.map.getPortal(0);
                    ret.initialSpawnPoint = 0;
                }
                ret.setPosition(portal.getPosition());
                int partyid = rs.getInt("party");
                try {
                    MapleParty party = client.getChannelServer().getWorldInterface().getParty(partyid);
                    if (party != null) {
                        ret.party = party;
                    }
                } catch (RemoteException ex) {
                    client.getChannelServer().reconnectWorld();
                }
                int messengerid = rs.getInt("messengerid");
                int position = rs.getInt("messengerposition");
                if (messengerid > 0 && position < 4 && position > -1) {
                    try {
                        WorldChannelInterface wci = client.getChannelServer().getWorldInterface();
                        MapleMessenger messenger = wci.getMessenger(messengerid);
                        if (messenger != null) {
                            ret.messenger = messenger;
                            ret.messengerposition = position;
                        }
                    } catch (RemoteException ez) {
                        client.getChannelServer().reconnectWorld();
                    }
                }
            }
            rs.close();
            ps.close();
            ps = con.prepareStatement("SELECT `name`, `level` FROM `characters` WHERE `accountid` = ? AND `id` <> ? ORDER BY `level` DESC LIMIT 1");
            ps.setInt(1, ret.accountid);
            ps.setInt(2, ret.id);
            rs = ps.executeQuery();

            if (rs.next()) {
                ret.linkedName = rs.getString("name");
                ret.linkedLevel = rs.getInt("level");
            }

            rs.close();
            ps.close();
            ps = con.prepareStatement("SELECT name, paypalNX, mPoints, cardNX, points, votepoints FROM accounts WHERE id = ?");
            ps.setInt(1, ret.accountid);
            rs = ps.executeQuery();
            if (rs.next()) {
                ret.client.setAccountName(rs.getString("name"));
                ret.paypalnx = rs.getInt("paypalNX");
                ret.maplepoints = rs.getInt("mPoints");
                ret.cardnx = rs.getInt("cardNX");
                ret.points = rs.getInt("points");
                ret.votepoints = rs.getInt("votepoints");
            }
            rs.close();
            ps.close();
            if (channelserver) {
                ps = con.prepareStatement("SELECT * FROM queststatus WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                PreparedStatement pse = con.prepareStatement("SELECT * FROM queststatusmobs WHERE queststatusid = ?");
                while (rs.next()) {
                    MapleQuest q = MapleQuest.getInstance(rs.getInt("quest"));
                    MapleQuestStatus status = new MapleQuestStatus(q, MapleQuestStatus.Status.getById(rs.getInt("status")));
                    long cTime = rs.getLong("time");
                    if (cTime > -1) {
                        status.setCompletionTime(cTime * 1000);
                    }
                    status.setForfeited(rs.getInt("forfeited"));
                    ret.quests.put(q, status);
                    pse.setInt(1, rs.getInt("queststatusid"));
                    ResultSet rsMobs = pse.executeQuery();
                    while (rsMobs.next()) {
                        status.setMobKills(rsMobs.getInt("mob"), rsMobs.getInt("count"));
                    }
                    rsMobs.close();
                }
                rs.close();
                ps.close();
                pse.close();
                ps = con.prepareStatement("SELECT skillid,skilllevel,masterlevel FROM skills WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                while (rs.next()) {
                    ret.skills.put(SkillFactory.getSkill(rs.getInt("skillid")), new SkillEntry(rs.getInt("skilllevel"), rs.getInt("masterlevel")));
                }
                rs.close();
                ps.close();
                ps = con.prepareStatement("SELECT * FROM skillmacros WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                while (rs.next()) {
                    int position = rs.getInt("position");
                    SkillMacro macro = new SkillMacro(rs.getInt("skill1"), rs.getInt("skill2"), rs.getInt("skill3"), rs.getString("name"), rs.getInt("shout"), position);
                    ret.skillMacros[position] = macro;
                }
                rs.close();
                ps.close();
                ps = con.prepareStatement("SELECT `key`,`type`,`action` FROM keymap WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                while (rs.next()) {
                    int key = rs.getInt("key");
                    int type = rs.getInt("type");
                    int action = rs.getInt("action");
                    ret.keymap.put(Integer.valueOf(key), new MapleKeyBinding(type, action));
                }
                rs.close();
                ps.close();
                ps = con.prepareStatement("SELECT `locationtype`,`map`,`portal` FROM savedlocations WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                while (rs.next()) {
                    ret.savedLocations[SavedLocationType.valueOf(rs.getString("locationtype")).ordinal()] = new SavedLocation(rs.getInt("map"), rs.getInt("portal"));
                }
                rs.close();
                ps.close();
                ps = con.prepareStatement("SELECT mapId, type FROM telerockmaps WHERE characterId = ? ORDER BY type");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                while (rs.next()) {
                    if (rs.getInt("type") == 0) {
                        ret.rockMaps.add(Integer.valueOf(rs.getInt("mapid")));
                    } else {
                        ret.vipRockMaps.add(Integer.valueOf(rs.getInt("mapid")));
                    }
                }
                rs.close();
                ps.close();
                ps = con.prepareStatement("SELECT `characterid_to`,`when` FROM famelog WHERE characterid = ? AND DATEDIFF(NOW(),`when`) < 30");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                ret.lastfametime = 0;
                ret.lastmonthfameids = new ArrayList<Integer>(31);
                while (rs.next()) {
                    ret.lastfametime = Math.max(ret.lastfametime, rs.getTimestamp("when").getTime());
                    ret.lastmonthfameids.add(Integer.valueOf(rs.getInt("characterid_to")));
                }
                rs.close();
                ps.close();
                ps = con.prepareStatement("SELECT `sn` FROM wishlist WHERE `charid` = ?");
                ps.setInt(1, ret.id);
                rs = ps.executeQuery();
                while (rs.next()) {
                    ret.wishList.add(rs.getInt("sn"));
                }
                rs.close();
                ps.close();
                ret.buddylist.loadFromDb(charid);
                ret.storage = MapleStorage.loadOrCreateFromDB(ret.accountid);
                ret.recalcLocalStats();
                ret.resetBattleshipHp();
                ret.silentEnforceMaxHpMp();
            }
            int mountid = ret.getJobType() * 10000000 + 1004;
            if (ret.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -18) != null) {
                ret.maplemount = new MapleMount(ret, ret.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -18).getItemId(), mountid);
            } else {
                ret.maplemount = new MapleMount(ret, 0, mountid);
            }
            ret.maplemount.setExp(mountexp);
            ret.maplemount.setLevel(mountlevel);
            ret.maplemount.setTiredness(mounttiredness);
            ret.maplemount.setActive(false);
            return ret;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String makeMapleReadable(String in) {
        String i = in.replace('I', 'i');
        i = i.replace('l', 'L');
        i = i.replace("rn", "Rn");
        i = i.replace("vv", "Vv");
        i = i.replace("VV", "Vv");
        return i;
    }

    private static class MapleBuffStatValueHolder {

        public MapleStatEffect effect;
        public long startTime;
        public int value;
        public ScheduledFuture<?> schedule;

        public MapleBuffStatValueHolder(MapleStatEffect effect, long startTime, ScheduledFuture<?> schedule, int value) {
            super();
            this.effect = effect;
            this.startTime = startTime;
            this.schedule = schedule;
            this.value = value;
        }
    }

    public static class MapleCoolDownValueHolder {

        public int skillId;
        public long startTime, length;
        public ScheduledFuture<?> timer;

        public MapleCoolDownValueHolder(int skillId, long startTime, long length, ScheduledFuture<?> timer) {
            super();
            this.skillId = skillId;
            this.startTime = startTime;
            this.length = length;
            this.timer = timer;
        }
    }

    public void message(String m) {
        dropMessage(5, m);
    }

    public void mobKilled(int id) {
        for (MapleQuestStatus q : quests.values()) {
            if (q.getStatus() == MapleQuestStatus.Status.COMPLETED || q.getQuest().canComplete(this, null)) {
                continue;
            }
            if (q.mobKilled(id)) {
                client.getSession().write(MaplePacketCreator.updateQuestMobKills(q));
                if (q.getQuest().canComplete(this, null)) {
                    client.getSession().write(MaplePacketCreator.getShowQuestCompletion(q.getQuest().getId()));
                }
            }
        }
    }

    public void modifyCSPoints(int type, int dx) {
        if (type == 1) {
            this.paypalnx += dx;
        } else if (type == 2) {
            this.maplepoints += dx;
        } else if (type == 4) {
            this.cardnx += dx;
        }
    }

    public void gainNX(int dx) {
        this.paypalnx += dx;
    }

    public void mount(int id, int skillid) {
        maplemount = new MapleMount(this, id, skillid);
    }

    public void offBeacon(boolean bf) {
        hasBeacon = false;
        beaconOid = -1;
        if (bf) {
            cancelEffectFromBuffStat(MapleBuffStat.HOMING_BEACON);
        }
    }

    /*
    private static int playerMap(int playerjob) {
    return playerjob > 1000 ? 13000100 : (playerjob > 500 ? 120000101 : (playerjob > 400 ? 103000003 : (playerjob > 300 ? 100000201 : (playerjob > 200 ? 102000003 : (playerjob > 0 ? 101000003 : 0)))));
    }*/

    private void playerDead() {
        cancelAllBuffs();
        dispelDebuffs();
        if (getEventInstance() != null) {
            getEventInstance().playerKilled(this);
        }
        int[] charmID = {5130000, 4031283, 4140903};
        int possesed = 0;
        int i;
        for (i = 0; i < charmID.length; i++) {
            int quantity = getItemQuantity(charmID[i], false);
            if (possesed == 0 && quantity > 0) {
                possesed = quantity;
                break;
            }
        }
        if (possesed > 0) {
            message("You have used a safety charm, so your EXP points have not been decreased.");
            MapleInventoryManipulator.removeById(client, MapleItemInformationProvider.getInstance().getInventoryType(charmID[i]), charmID[i], 1, true, false);
        } else if (mapid > 925020000 && mapid < 925030000) {
            this.dojoStage = 0;
        } else if (mapid > 980000000 && mapid < 980000604) {
            if (cp > 10) {
                cp -= 10;
            } else {
                cp = 0;
            }
        } else if (FieldLimit.CANNOTREGULAREXPLOSS.check(map.getId())) {
        } else if (getJob() != MapleJob.BEGINNER) {
            int XPdummy = ExpTable.getExpNeededForLevel(getLevel());
            if (getMap().isTown()) {
                XPdummy /= 100;
            }
            if (XPdummy == ExpTable.getExpNeededForLevel(getLevel())) {
                if (getLuk() <= 100 && getLuk() > 8) {
                    XPdummy *= (200 - getLuk()) / 2000;
                } else if (getLuk() <= 8) {
                    XPdummy /= 10;
                } else {
                    XPdummy /= 20;
                }
            }
            //        gainExp(getExp() > XPdummy ? -XPdummy : -getExp(), false, false);
        }
        if (getBuffedValue(MapleBuffStat.MORPH) != null) {
            cancelEffectFromBuffStat(MapleBuffStat.MORPH);
        }
        if (getBuffedValue(MapleBuffStat.MONSTER_RIDING) != null) {
            cancelEffectFromBuffStat(MapleBuffStat.MONSTER_RIDING);
        }
        if (getChair() == -1) {
            setChair(0);
            client.getSession().write(MaplePacketCreator.cancelChair(-1));
            getMap().broadcastMessage(this, MaplePacketCreator.showChair(getId(), 0), false);
        }
        client.getSession().write(MaplePacketCreator.enableActions());
    }

    private void prepareDragonBlood(final MapleStatEffect bloodEffect) {
        if (dragonBloodSchedule != null) {
            dragonBloodSchedule.cancel(false);
        }
        dragonBloodSchedule = TimerManager.getInstance().register(new Runnable() {

            @Override
            public void run() {
                addHP(-bloodEffect.getX());
                client.getSession().write(MaplePacketCreator.showOwnBuffEffect(bloodEffect.getSourceId(), 5));
                getMap().broadcastMessage(MapleCharacter.this, MaplePacketCreator.showBuffeffect(getId(), bloodEffect.getSourceId(), 5), false);
                checkBerserk();
            }
        }, 4000, 4000);
    }

    public static int rand(int l, int u) {
        return Randomizer.getInstance().nextInt(u - l + 1) + l;
    }

    private void recalcLocalStats() {
        int oldmaxhp = localmaxhp;
        localmaxhp = getMaxHp();
        localmaxmp = getMaxMp();
        localdex = getDex();
        localint_ = getInt();
        localstr = getStr();
        localluk = getLuk();
        magic = localint_;
        watk = 0;
        for (IItem item : getInventory(MapleInventoryType.EQUIPPED)) {
            IEquip equip = (IEquip) item;
            localmaxhp += equip.getHp();
            localmaxmp += equip.getMp();
            localdex += equip.getDex();
            localint_ += equip.getInt();
            localstr += equip.getStr();
            localluk += equip.getLuk();
            magic += equip.getMatk() + equip.getInt();
            watk += equip.getWatk();
        }
        magic = Math.min(magic, 2000);
        Integer hbhp = getBuffedValue(MapleBuffStat.HYPERBODYHP);
        if (hbhp != null) {
            localmaxhp += (hbhp.doubleValue() / 100) * localmaxhp;
        }
        Integer hbmp = getBuffedValue(MapleBuffStat.HYPERBODYMP);
        if (hbmp != null) {
            localmaxmp += (hbmp.doubleValue() / 100) * localmaxmp;
        }
        localmaxhp = Math.min(30000, localmaxhp);
        localmaxmp = Math.min(30000, localmaxmp);
        Integer watkbuff = getBuffedValue(MapleBuffStat.WATK);
        if (watkbuff != null) {
            watk += watkbuff.intValue();
        }
        if (job.isA(MapleJob.BOWMAN)) {
            ISkill expert = null;
            if (job.isA(MapleJob.MARKSMAN)) {
                expert = SkillFactory.getSkill(3220004);
            } else if (job.isA(MapleJob.BOWMASTER)) {
                expert = SkillFactory.getSkill(3120005);
            }
            if (expert != null) {
                int boostLevel = getSkillLevel(expert);
                if (boostLevel > 0) {
                    watk += expert.getEffect(boostLevel).getX();
                }
            }
        }
        Integer matkbuff = getBuffedValue(MapleBuffStat.MATK);
        if (matkbuff != null) {
            magic += matkbuff.intValue();
        }
        if (oldmaxhp != 0 && oldmaxhp != localmaxhp) {
            updatePartyMemberHP();
        }
    }

    public void receivePartyMemberHP() {
        if (party != null) {
            int channel = client.getChannel();
            for (MaplePartyCharacter partychar : party.getMembers()) {
                if (partychar.getMapid() == getMapId() && partychar.getChannel() == channel) {
                    MapleCharacter other = ChannelServer.getInstance(channel).getPlayerStorage().getCharacterByName(partychar.getName());
                    if (other != null) {
                        client.getSession().write(MaplePacketCreator.updatePartyMemberHP(other.getId(), other.getHp(), other.getCurrentMaxHp()));
                    }
                }
            }
        }
    }

    public void registerEffect(MapleStatEffect effect, long starttime, ScheduledFuture<?> schedule) {
        if (effect.isHide() && gmLevel > 0) {
            this.hidden = true;
            this.getClient().getSession().write(MaplePacketCreator.getGMEffect(16, (byte) 1));
            getMap().broadcastNONGMMessage(this, MaplePacketCreator.removePlayerFromMap(id), false);
        } else if (effect.isDragonBlood()) {
            prepareDragonBlood(effect);
        } else if (effect.isBerserk()) {
            checkBerserk();
        } else if (effect.isBeholder()) {
            final int beholder = DarkKnight.BEHOLDER;
            if (beholderHealingSchedule != null) {
                beholderHealingSchedule.cancel(false);
            }
            if (beholderBuffSchedule != null) {
                beholderBuffSchedule.cancel(false);
            }
            ISkill bHealing = SkillFactory.getSkill(DarkKnight.AURA_OF_BEHOLDER);
            int bHealingLvl = getSkillLevel(bHealing);
            if (bHealingLvl > 0) {
                final MapleStatEffect healEffect = bHealing.getEffect(bHealingLvl);
                int healInterval = healEffect.getX() * 1000;
                beholderHealingSchedule = TimerManager.getInstance().register(new Runnable() {

                    @Override
                    public void run() {
                        addHP(healEffect.getHp());
                        client.getSession().write(MaplePacketCreator.showOwnBuffEffect(beholder, 2));
                        getMap().broadcastMessage(MapleCharacter.this, MaplePacketCreator.summonSkill(getId(), beholder, 5), true);
                        getMap().broadcastMessage(MapleCharacter.this, MaplePacketCreator.showOwnBuffEffect(beholder, 2), false);
                    }
                }, healInterval, healInterval);
            }
            ISkill bBuff = SkillFactory.getSkill(DarkKnight.HEX_OF_BEHOLDER);
            if (getSkillLevel(bBuff) > 0) {
                final MapleStatEffect buffEffect = bBuff.getEffect(getSkillLevel(bBuff));
                int buffInterval = buffEffect.getX() * 1000;
                beholderBuffSchedule = TimerManager.getInstance().register(new Runnable() {

                    @Override
                    public void run() {
                        buffEffect.applyTo(MapleCharacter.this);
                        client.getSession().write(MaplePacketCreator.showOwnBuffEffect(beholder, 2));
                        getMap().broadcastMessage(MapleCharacter.this, MaplePacketCreator.summonSkill(getId(), beholder, (int) (Math.random() * 3) + 6), true);
                        getMap().broadcastMessage(MapleCharacter.this, MaplePacketCreator.showBuffeffect(getId(), beholder, 2), false);
                    }
                }, buffInterval, buffInterval);
            }
        }
        for (Pair<MapleBuffStat, Integer> statup : effect.getStatups()) {
            effects.put(statup.getLeft(), new MapleBuffStatValueHolder(effect, starttime, schedule, statup.getRight().intValue()));
        }
        recalcLocalStats();
    }

    public void removeAllCooldownsExcept(int id) {
        for (MapleCoolDownValueHolder mcvh : coolDowns.values()) {
            if (mcvh.skillId != id) {
                coolDowns.remove(mcvh.skillId);
            }
        }
    }

    public static void removeAriantRoom(int room) {
        ariantroomleader[room] = "";
        ariantroomslot[room] = 0;
    }

    public void removeBuffStat(MapleBuffStat effect) {
        effects.remove(effect);
    }

    public boolean isDonor() {
		return gmLevel == 1;
	}

    public void removeCooldown(int skillId) {
        if (this.coolDowns.containsKey(skillId)) {
            this.coolDowns.remove(skillId);
        }
    }

    public void removeDisease(MapleDisease disease) {
        synchronized (diseases) {
            if (diseases.contains(disease)) {
                diseases.remove(disease);
            }
        }
    }

    public void removePartyQuestItem(String letter) {
        partyquestitems = partyquestitems.substring(0, partyquestitems.indexOf(letter)) + partyquestitems.substring(partyquestitems.indexOf(letter) + 1);
    }

    public void removePet(MaplePet pet, boolean shift_left) {
        int sl0t = -1;
        for (int i = 0; i < 3; i++) {
            if (pets[i] != null) {
                if (pets[i].getUniqueId() == pet.getUniqueId()) {
                    pets[i] = null;
                    sl0t = i;
                    break;
                }
            }
        }
        if (shift_left) {
            if (sl0t > -1) {
                for (int i = sl0t; i < 3; i++) {
                    if (i != 2) {
                        pets[i] = pets[i + 1];
                    } else {
                        pets[i] = null;
                    }
                }
            }
        }
    }

    public void removeVisibleMapObject(MapleMapObject mo) {
        visibleMapObjects.remove(mo);
    }

    public void resetBattleshipHp() {
        this.battleshipHp = 4000 * getSkillLevel(SkillFactory.getSkill(Corsair.BATTLE_SHIP)) + ((getLevel() - 120) * 2000);
    }

    public void resetEnteredScript() {
        if (entered.containsKey(map.getId())) {
            entered.remove(map.getId());
        }
    }

    public void resetEnteredScript(int mapId) {
        if (entered.containsKey(mapId)) {
            entered.remove(mapId);
        }
    }

        public boolean getCanTalk() {
        return canTalk;
    }

    public boolean canTalk(boolean yn) {
        return canTalk = yn;
    }

    public void resetEnteredScript(String script) {
        for (int mapId : entered.keySet()) {
            if (entered.get(mapId).equals(script)) {
                entered.remove(mapId);
            }
        }
    }

    public void resetMGC() {
        this.mgc = null;
    }

    public void saveCooldowns() {
        if (getAllCooldowns().size() > 0) {
            try {
                PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("INSERT INTO cooldowns (charid, SkillID, StartTime, length) VALUES (?, ?, ?, ?)");
                ps.setInt(1, getId());
                for (PlayerCoolDownValueHolder cooling : getAllCooldowns()) {
                    ps.setInt(2, cooling.skillId);
                    ps.setLong(3, cooling.startTime);
                    ps.setLong(4, cooling.length);
                    ps.addBatch();
                }
                ps.executeBatch();
                ps.close();
            } catch (SQLException se) {
            }
        }
    }

    public void saveGuildStatus() {
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("UPDATE characters SET guildid = ?, guildrank = ?, allianceRank = ? WHERE id = ?");
            ps.setInt(1, guildid);
            ps.setInt(2, guildrank);
            ps.setInt(3, allianceRank);
            ps.setInt(4, id);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException se) {
        }
    }

    public void saveLocation(String type) {
        MaplePortal closest = map.findClosestPortal(getPosition());
        savedLocations[SavedLocationType.fromString(type).ordinal()] = new SavedLocation(getMapId(), closest != null ? closest.getId() : 0);
    }

    public void saveToDB(boolean update) {
        if ((update) && this.trade != null) {
            return;
        }
        Connection con = DatabaseConnection.getConnection();
        try {
            con.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
            con.setAutoCommit(false);
            PreparedStatement ps;
            if (update) {
                ps = con.prepareStatement("UPDATE characters SET level = ?, fame = ?, str = ?, dex = ?, luk = ?, `int` = ?, exp = ?, hp = ?, mp = ?, maxhp = ?, maxmp = ?, sp = ?, ap = ?, gm = ?, skincolor = ?, gender = ?, job = ?, hair = ?, face = ?, map = ?, meso = ?, hpMpUsed = ?, spawnpoint = ?, party = ?, buddyCapacity = ?, messengerid = ?, messengerposition = ?, reborns = ?, fishingexp = ?,  fishinglevel = ?, esp = ?, cookingexp = ?, cookinglevel = ?, mountlevel = ?, mountexp = ?, mounttiredness= ?, equipslots = ?, useslots = ?, setupslots = ?, etcslots = ?,  monsterbookcover = ?, watchedcygnusintro = ?, vanquisherStage = ?, dojoPoints = ?, lastDojoStage = ?, finishedDojoTutorial = ?, vanquisherKills = ?, matchcardwins = ?, matchcardlosses = ?, matchcardties = ?, omokwins = ?, omoklosses = ?, omokties = ?, occupation = ?, givenRiceCakes = ?, partyquestitems = ?, receivedMOTB = ? WHERE id = ?");
            } else {
                ps = con.prepareStatement("INSERT INTO characters (level, fame, str, dex, luk, `int`, exp, hp, mp, maxhp, maxmp, sp, ap, gm, skincolor, gender, job, hair, face, map, meso, hpMpUsed, spawnpoint, party, buddyCapacity, messengerid, messengerposition, reborns, fishingexp, fishinglevel, esp, cookingexp, cookinglevel, mountlevel, mounttiredness, mountexp, equipslots, useslots, setupslots, etcslots, monsterbookcover, watchedcygnusintro, vanquisherStage, dojopoints, lastDojoStage, finishedDojoTutorial, vanquisherKills, matchcardwins, matchcardlosses, matchcardties, omokwins, omoklosses, omokties, occupation, givenRiceCakes, partyquestitems, accountid, name, world) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            }
            if (gmLevel < 1 && level > 199) {
                ps.setInt(1, isCygnus() ? 200 : 200);
            } else {
                ps.setInt(1, level);
            }
            ps.setInt(2, fame);
            ps.setInt(3, str);
            ps.setInt(4, dex);
            ps.setInt(5, luk);
            ps.setInt(6, int_);
            ps.setInt(7, exp.get());
            ps.setInt(8, hp);
            ps.setInt(9, mp);
            ps.setInt(10, maxhp);
            ps.setInt(11, maxmp);
            ps.setInt(12, remainingSp);
            ps.setInt(13, remainingAp);
            ps.setInt(14, gmLevel);
            ps.setInt(15, skinColor.getId());
            ps.setInt(16, gender);
            ps.setInt(17, job.getId());
            ps.setInt(18, hair);
            ps.setInt(19, face);
            if ((map == null) && (mapid == -1)) { //ie no data for map (shouldn't happen)
                ps.setInt(20, 109060001);
            } else if (map == null)//ie mapid set at createchar
            {
                ps.setInt(20, mapid);
            } else if (map.getForcedReturnId() != 999999999) {
                ps.setInt(20, map.getForcedReturnId());
            } else {
                ps.setInt(20, map.getId());
            }
            ps.setInt(21, meso.get());
            ps.setInt(22, hpMpApUsed);
            if (map == null || map.getId() == 610020000 || map.getId() == 610020001) {
                ps.setInt(23, 0);
            } else {
                MaplePortal closest = map.findClosestSpawnpoint(getPosition());
                if (closest != null) {
                    ps.setInt(23, closest.getId());
                } else {
                    ps.setInt(23, 0);
                }
            }
            ps.setInt(24, party != null ? party.getId() : -1);
            ps.setInt(25, buddylist.getCapacity());
            if (messenger != null) {
                ps.setInt(26, messenger.getId());
                ps.setInt(27, messengerposition);
                ps.setInt(28, reborns);
            } else {
                ps.setInt(26, 0);
                ps.setInt(27, 4);
                ps.setInt(28, reborns);
            ps.setInt(29, FishingExp);
            ps.setInt(30, FishingLevel);
                ps.setInt(31, ESP);
            ps.setInt(32, CookingExp);
            ps.setInt(33, CookingLevel);
            }
            if (maplemount != null) {
                ps.setInt(34, maplemount.getLevel());
                ps.setInt(35, maplemount.getExp());
                ps.setInt(36, maplemount.getTiredness());
            } else {
                ps.setInt(34, 1);
                ps.setInt(35, 0);
                ps.setInt(36, 0);
            }
            for (int i = 37; i < 41; i++) {
                ps.setInt(i, getInventory(MapleInventoryType.getByType((byte) (i - 35))).getSlotLimit());
            }
            if (update) {
                monsterbook.saveCards(getId());
                try {
                    getFamily().save();
                } catch (NullPointerException npe) {
                }
            }
            ps.setInt(41, bookCover);
            ps.setInt(42, watchedCygnusIntro ? 1 : 0);
            ps.setInt(43, vanquisherStage);
            ps.setInt(44, dojoPoints);
            ps.setInt(45, dojoStage);
            ps.setInt(46, finishedDojoTutorial ? 1 : 0);
            ps.setInt(47, vanquisherKills);
            ps.setInt(48, matchcardwins);
            ps.setInt(49, matchcardlosses);
            ps.setInt(50, matchcardties);
            ps.setInt(51, omokwins);
            ps.setInt(52, omoklosses);
            ps.setInt(53, omokties);
            ps.setInt(54, occupation.getId());
            ps.setInt(55, givenRiceCakes);
            ps.setString(56, partyquestitems);
            if (update) {
                ps.setInt(57, receivedMOTB ? 1 : 0);
                ps.setInt(58, id);
            } else {
                ps.setInt(57, accountid);
                ps.setString(58, name);
                ps.setInt(59, world);
            }
            int updateRows = ps.executeUpdate();
            if (!update) {
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    this.id = rs.getInt(1);
                } else {
                    throw new RuntimeException("Inserting char failed.");
                }
                rs.close();
            } else if (updateRows < 1) {
                throw new RuntimeException("Character not in database (" + id + ")");
            }
            for (int i = 0; i < 3; i++) {
                if (pets[i] != null) {
                    pets[i].saveToDb();
                }
            }
            ps.close();
            deleteWhereCharacterId(con, "DELETE FROM keymap WHERE characterid = ?");
            /*            ps = con.prepaffreStatement("INSERT INTO keymap (characterid, `key`, `type`, `action`) VALUES (?, ?, ?, ?)");
            ps.setInt(1, id);
            for (Entry<Integer, MapleKeyBinding> keybinding : keymap.entrySet()) {
            ps.setInt(2, keybinding.getKey().intValue());
            ps.setInt(3, keybinding.getValue().getType());
            ps.setInt(4, keybinding.getValue().getAction());
            ps.addBatch();
            }
            ps.executeBatch();*/
            if (!keymap.isEmpty()) {
                ps = con.prepareStatement(prepareKeymapQuery());
                ps.executeUpdate();
                ps.close();
            }

            deleteWhereCharacterId(con, "DELETE FROM skillmacros WHERE characterid = ?");
            ps = con.prepareStatement("INSERT INTO skillmacros (characterid, skill1, skill2, skill3, name, shout, position) VALUES (?, ?, ?, ?, ?, ?, ?)");
            ps.setInt(1, getId());
            for (int i = 0; i < 5; i++) {
                SkillMacro macro = skillMacros[i];
                if (macro != null) {
                    ps.setInt(2, macro.getSkill1());
                    ps.setInt(3, macro.getSkill2());
                    ps.setInt(4, macro.getSkill3());
                    ps.setString(5, macro.getName());
                    ps.setInt(6, macro.getShout());
                    ps.setInt(7, i);
                    ps.addBatch();
                }
            }
            ps.executeBatch();
            ps.close();
            deleteWhereCharacterId(con, "DELETE FROM telerockmaps WHERE characterId = ?");
            ps = con.prepareStatement("INSERT into telerockmaps (characterId, mapId, type) VALUES (?, ?, ?)");
            ps.setInt(1, id);
            for (int mapId : rockMaps) {
                ps.setInt(2, mapId);
                ps.setInt(3, 0);
                ps.addBatch();
            }
            for (int mapId : vipRockMaps) {
                ps.setInt(2, mapId);
                ps.setInt(3, 1);
                ps.addBatch();
            }
            ps.executeBatch();
            ps.close();
            List<Pair<IItem, MapleInventoryType>> itemsWithType = new ArrayList<Pair<IItem, MapleInventoryType>>();

            for (MapleInventory iv : inventory) {
                for (IItem item : iv.list()) {
                    itemsWithType.add(new Pair<IItem, MapleInventoryType>(item, iv.getType()));
                }
            }

            ItemFactory.INVENTORY.saveItems(itemsWithType, id);
            deleteWhereCharacterId(con, "DELETE FROM skills WHERE characterid = ?");
            //    ps = con.prepareStatement("INSERT INTO skills (characterid, skillid, skilllevel, masterlevel) VALUES (?, ?, ?, ?)");
         /*   ps.setInt(1, id);
            for (Entry<ISkill, SkillEntry> skill : skills.entrySet()) {
            ps.setInt(2, skill.getKey().getId());
            ps.setInt(3, skill.getValue().skillevel);
            ps.setInt(4, skill.getValue().masterlevel);
            ps.addBatch();
            }
            ps.executeBatch();*/
            if (!skills.isEmpty()) {
                ps = con.prepareStatement(prepareSkillQuery());
                ps.executeUpdate();
                ps.close();
            }
            deleteWhereCharacterId(con, "DELETE FROM savedlocations WHERE characterid = ?");
            ps = con.prepareStatement("INSERT INTO savedlocations (characterid, `locationtype`, `map`, `portal`) VALUES (?, ?, ?, ?)");
            ps.setInt(1, id);
            for (SavedLocationType savedLocationType : SavedLocationType.values()) {
                if (savedLocations[savedLocationType.ordinal()] != null) {
                    ps.setString(2, savedLocationType.name());
                    ps.setInt(3, savedLocations[savedLocationType.ordinal()].getMapId());
                    ps.setInt(4, savedLocations[savedLocationType.ordinal()].getPortal());
                    ps.addBatch();
                }
            }
            ps.executeBatch();
            ps.close();
            deleteWhereCharacterId(con, "DELETE FROM buddies WHERE characterid = ? AND pending = 0");
            ps = con.prepareStatement("INSERT INTO buddies (characterid, `buddyid`, `pending`, `group`) VALUES (?, ?, 0, ?)");
            ps.setInt(1, id);
            for (BuddylistEntry entry : buddylist.getBuddies()) {
                if (entry.isVisible()) {
                    ps.setInt(2, entry.getCharacterId());
                    ps.setString(3, entry.getGroup());
                    ps.addBatch();
                }
            }
            ps.executeBatch();
            ps.close();
            deleteWhereCharacterId(con, "DELETE FROM queststatus WHERE characterid = ?");
            ps = con.prepareStatement("INSERT INTO queststatus (`queststatusid`, `characterid`, `quest`, `status`, `time`, `forfeited`) VALUES (DEFAULT, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            PreparedStatement pse = con.prepareStatement("INSERT INTO queststatusmobs VALUES (DEFAULT, ?, ?, ?)");
            ps.setInt(1, id);
            for (MapleQuestStatus q : quests.values()) {
                ps.setInt(2, q.getQuest().getId());
                ps.setInt(3, q.getStatus().getId());
                ps.setInt(4, (int) (q.getCompletionTime() / 1000));
                ps.setInt(5, q.getForfeited());
                ps.executeUpdate();
                ResultSet rs = ps.getGeneratedKeys();
                rs.next();
                for (int mob : q.getMobKills().keySet()) {
                    pse.setInt(1, rs.getInt(1));
                    pse.setInt(2, mob);
                    pse.setInt(3, q.getMobKills(mob));
                    pse.addBatch();
                }
                pse.executeBatch();
                rs.close();
            }
            pse.close();
            ps.close();
            ps = con.prepareStatement("UPDATE accounts SET `paypalNX` = ?, `mPoints` = ?, `cardNX` = ?, gm = ?, points = ?, `votepoints` = ? WHERE id = ?");
            ps.setInt(1, paypalnx);
            ps.setInt(2, maplepoints);
            ps.setInt(3, cardnx);
            ps.setInt(4, gmLevel);
            ps.setInt(5, points);
            ps.setInt(6, votepoints);
            ps.setInt(7, client.getAccID());
            ps.executeUpdate();
            ps.close();
            if (storage != null) {
                storage.saveToDB();
            }
            ps = con.prepareStatement("DELETE FROM wishlist WHERE `charid` = ?");
            ps.setInt(1, id);
            ps.executeUpdate();
            ps.close();
            ps = con.prepareStatement("INSERT INTO wishlist(`sn`, `charid`) VALUES(?, ?)");
            for (int sn : wishList) {
                ps.setInt(1, sn);
                ps.setInt(2, id);
                ps.addBatch();
            }
            ps.executeBatch();
            ps.close();
            if (gmLevel > 0) {
                ps = con.prepareStatement("INSERT INTO gmlog (`cid`, `command`) VALUES (?, ?)");
                ps.setInt(1, id);
                for (String com : commands) {
                    ps.setString(2, com);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            ps.close();
            con.commit();
            ps = null;
        } catch (Exception e) {
            e.printStackTrace();
            try {
                con.rollback();
            } catch (SQLException se) {
            }
        } finally {
            try {
                con.setAutoCommit(true);
                con.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            } catch (Exception e) {
            }
        }
    }

    public void sendKeymap() {
        client.getSession().write(MaplePacketCreator.getKeymap(keymap));
    }

        public int getReborns() {
        return reborns;
    }

        public int getGMPoints() {
        return GMPoints;
    }

    public void setReborns(int reborn) {
        reborns = reborn;
    }

    public void sendMacros() {
        boolean macros = false;
        for (int i = 0; i < 5; i++) {
            if (skillMacros[i] != null) {
                macros = true;
            }
        }
        if (macros) {
            client.getSession().write(MaplePacketCreator.getMacros(skillMacros));
        }
    }

    public void sendNote(String to, String msg) throws SQLException {
        PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("INSERT INTO notes (`to`, `from`, `message`, `timestamp`) VALUES (?, ?, ?, ?)");
        ps.setString(1, to);
        ps.setString(2, this.getName());
        ps.setString(3, msg);
        ps.setLong(4, System.currentTimeMillis());
        ps.executeUpdate();
        ps.close();
    }

    public void setAllianceRank(int rank) {
        allianceRank = rank;
        if (mgc != null) {
            mgc.setAllianceRank(rank);
        }
    }

    public void setAllowWarpToId(int id) {
        this.warpToId = id;
    }

    public static void setAriantRoomLeader(int room, String charname) {
        ariantroomleader[room] = charname;
    }

    public static void setAriantSlotRoom(int room, int slot) {
        ariantroomslot[room] = slot;
    }

    public void setBattleshipHp(int battleshipHp) {
        this.battleshipHp = battleshipHp;
    }

    public void setBeacon(int oid) {
        beaconOid = oid;
    }

    public void setBuddyCapacity(int capacity) {
        buddylist.setCapacity(capacity);
        client.getSession().write(MaplePacketCreator.updateBuddyCapacity(capacity));
    }

    public void setBuffedValue(MapleBuffStat effect, int value) {
        MapleBuffStatValueHolder mbsvh = effects.get(effect);
        if (mbsvh == null) {
            return;
        }
        mbsvh.value = value;
    }

    public void setChair(int chair) {
        this.chair = chair;
    }

                public int getVotePoints() {
        return votepoints;
    }

    public void setVotePoints(int howmany) {
        votepoints = howmany;
    }
        public void gainVotePoints(int hi) {
        votepoints = votepoints + hi;
    }

    public void addVotepoint(int lol) {
        votepoints = votepoints + lol;
    }

    public void setDex(int dex) {
        this.dex = dex;
        recalcLocalStats();
    }

    public void setDojoEnergy(int x) {
        this.dojoEnergy = x;
    }

    public void setDojoParty(boolean b) {
        this.dojoParty = b;
    }

    public void setDojoPoints(int x) {
        this.dojoPoints = x;
    }

    public void setDojoStage(int x) {
        this.dojoStage = x;
    }
    
    public void setFake() {
        isfake = true;
    }
    
    public void setClient(MapleClient c) {
        client = c;
    }

    public void setDojoStart() {
        this.dojoMap = map;
        int stage = (map.getId() / 100) % 100;
        this.dojoFinish = System.currentTimeMillis() + (stage > 36 ? 15 : stage / 6 + 5) * 60000;
    }

    public void setRates(boolean dispel) {
        this.dropRate = client.getChannelServer().getDropRate();
        this.mesoRate = client.getChannelServer().getMesoRate();
        this.expRate = client.getChannelServer().getEXPRate();
        if (diseases.contains(MapleDisease.CURSE) && (!dispel)) {
            this.expRate *= 0.5;
            return;
        }
        if ((dispel) && (diseases.contains(MapleDisease.CURSE))) { //only double exp when curse is active
            this.expRate *= 2;
        }

    }

    public void setEnergyBar(int set) {
        energybar = set;
    }

    public void setEventInstance(EventInstanceManager eventInstance) {
        this.eventInstance = eventInstance;
    }

    public void setExp(int amount) {
        this.exp.set(amount);
    }

    public void setFace(int face) {
        this.face = face;
    }

    public void setFallCounter(int fallcounter) {
        this.fallcounter = fallcounter;
    }

    public void setFame(int fame) {
        this.fame = fame;
    }

    public void setFamilyId(int familyId) {
        this.familyId = familyId;
    }

    public void setFinishedDojoTutorial() {
        this.finishedDojoTutorial = true;
    }

    public void setGender(int gender) {
        this.gender = gender;
    }

    public void setGM(int level) {
        this.gmLevel = level;
    }

    public void setGottenRiceHat(boolean b) {
        this.gottenRiceHat = b;
    }

    public void setGuildId(int _id) {
        guildid = _id;
        if (guildid > 0) {
            if (mgc == null) {
                mgc = new MapleGuildCharacter(this);
            } else {
                mgc.setGuildId(guildid);
            }
        } else {
            mgc = null;
        }
    }

    public void setGuildRank(int _rank) {
        guildrank = _rank;
        if (mgc != null) {
            mgc.setGuildRank(_rank);
        }
    }

    public void setHair(int hair) {
        this.hair = hair;
    }

    public void setHasMerchant(boolean set) {
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("UPDATE characters SET HasMerchant = ? WHERE id = ?");
            ps.setInt(1, set ? 1 : 0);
            ps.setInt(2, id);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            return;
        }
        hasMerchant = set;
    }

    public void setHiredMerchant(HiredMerchant merchant) {
        this.hiredMerchant = merchant;
    }

    public void setHp(int newhp) {
        setHp(newhp, false);
    }

    public void setHp(int newhp, boolean silent) {
        int oldHp = hp;
        int thp = newhp;
        if (thp < 0) {
            thp = 0;
        }
        if (thp > localmaxhp) {
            thp = localmaxhp;
        }
        this.hp = thp;
        if (!silent) {
            updatePartyMemberHP();
        }
        if (oldHp > hp && !isAlive()) {
            playerDead();
        }
    }

    public void setHpMpApUsed(int mpApUsed) {
        this.hpMpApUsed = mpApUsed;
    }

    public void setHpMp(int x) {
        setHp(x);
        setMp(x);
        updateSingleStat(MapleStat.HP, hp);
        updateSingleStat(MapleStat.MP, mp);
    }

    public void setInCS(boolean b) {
        this.incs = b;
    }

    public void setInMTS(boolean b) {
        this.inmts = b;
    }

    public void setInt(int int_) {
        this.int_ = int_;
        recalcLocalStats();
    }

    public void setInventory(MapleInventoryType type, MapleInventory inv) {
        inventory[type.ordinal()] = inv;
    }

    public void setItemEffect(int itemEffect) {
        this.itemEffect = itemEffect;
    }

    public void setJob(MapleJob job) {
        this.job = job;
    }

    public void setLastHealed(long time) {
        this.lastHealed = time;
    }

    public void setLastUsedCashItem(long time) {
        this.lastUsedCashItem = time;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public void setLuk(int luk) {
        this.luk = luk;
        recalcLocalStats();
    }

    public void setMap(int PmapId) {
        this.mapid = PmapId;
    }

    public void setMap(MapleMap newmap) {
        this.map = newmap;
    }

    public String getMapName(int mapId) {
        return client.getChannelServer().getMapFactory().getMap(mapId).getMapName();
    }

    public void setMaxHp(int hp) {
        this.maxhp = hp;
        recalcLocalStats();
    }

    public void setMaxMp(int mp) {
        this.maxmp = mp;
        recalcLocalStats();
    }

    public void setMessenger(MapleMessenger messenger) {
        this.messenger = messenger;
    }

    public void setMessengerPosition(int position) {
        this.messengerposition = position;
    }

    public void setMiniGame(MapleMiniGame miniGame) {
        this.miniGame = miniGame;
    }

    public void setMiniGamePoints(MapleCharacter visitor, int winnerslot, boolean omok) {
        if (omok) {
            if (winnerslot == 1) {
                this.omokwins++;
                visitor.omoklosses++;
            } else if (winnerslot == 2) {
                visitor.omokwins++;
                this.omoklosses++;
            } else {
                this.omokties++;
                visitor.omokties++;
            }
        } else {
            if (winnerslot == 1) {
                this.matchcardwins++;
                visitor.matchcardlosses++;
            } else if (winnerslot == 2) {
                visitor.matchcardwins++;
                this.matchcardlosses++;
            } else {
                this.matchcardties++;
                visitor.matchcardties++;
            }
        }
    }

    public void setMonsterBookCover(int bookCover) {
        this.bookCover = bookCover;
    }

    public void setMp(int newmp) {
        int tmp = newmp;
        if (tmp < 0) {
            tmp = 0;
        }
        if (tmp > localmaxmp) {
            tmp = localmaxmp;
        }
        this.mp = tmp;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    	public static int getIdByName(String name, int world) {
		Connection con = DatabaseConnection.getConnection();
		PreparedStatement ps;
		try {
			ps = con.prepareStatement("SELECT id FROM characters WHERE name = ? AND world = ?");
			ps.setString(1, name);
			ps.setInt(2, world);
			ResultSet rs = ps.executeQuery();
			if (!rs.next()) {
				rs.close();
				ps.close();
				return -1;
			}
			int id = rs.getInt("id");
			rs.close();
			ps.close();
			return id;
		} catch (SQLException e) {
			System.out.print("ERROR" + e);
		}
		return -1;
	}

            public void setName(String name, boolean changeName) {
        if (!changeName) {
            this.name = name;
        } else {
            Connection con = DatabaseConnection.getConnection();
            try {
                con.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
                con.setAutoCommit(false);
                PreparedStatement sn = con.prepareStatement("UPDATE characters SET name = ? WHERE id = ?");
                sn.setString(1, name);
                sn.setInt(2, id);
                sn.execute();
                con.commit();
                sn.close();
                this.name = name;
            } catch (SQLException e) {
            }
        }
    }

    public void setParty(MapleParty party) {
        this.party = party;
    }

    public void setPartyQuestItemObtained(String partyquestchar) {
        this.partyquestitems += partyquestchar;
    }

    public void setPlayerShop(MaplePlayerShop playerShop) {
        this.playerShop = playerShop;
    }

    public void setRemainingAp(int remainingAp) {
        this.remainingAp = remainingAp;
    }

    public void setRemainingSp(int remainingSp) {
        this.remainingSp = remainingSp;
    }

    public void setSearch(String find) {
        search = find;
    }

    public void setSkinColor(MapleSkinColor skinColor) {
        this.skinColor = skinColor;
    }

    public void setShop(MapleShop shop) {
        this.shop = shop;
    }

    public int getSlot() {
        return slot;
    }

    public void setSlot(int slot) {
        this.slot = slot;
    }

    public void setStr(int str) {
        this.str = str;
        recalcLocalStats();
    }

    public void setTrade(MapleTrade trade) {
        this.trade = trade;
    }

    public void setVanquisherKills(int x) {
        this.vanquisherKills = x;
    }

    public void setVanquisherStage(int x) {
        this.vanquisherStage = x;
    }

    public void setWatchedCygnusIntro(boolean set) {
        this.watchedCygnusIntro = set;
    }

    public void setWorld(int world) {
        this.world = world;
    }

    public void shiftPetsRight() {
        if (pets[2] == null) {
            pets[2] = pets[1];
            pets[1] = pets[0];
            pets[0] = null;
        }
    }

    public void showDojoClock() {
        int stage = (map.getId() / 100) % 100;
        long time;
        if (stage % 6 == 1) {
            time = (stage > 36 ? 15 : stage / 6 + 5) * 60;
        } else {
            time = (dojoFinish - System.currentTimeMillis()) / 1000;
        }
        if (stage % 6 > 0) {
            client.getSession().write(MaplePacketCreator.getClock((int) time));
        }
        TimerManager.getInstance().schedule(new Runnable() {

            @Override
            public void run() {
                int clockid = (dojoMap.getId() / 100) % 100;
                if (dojoMap.getId() > clockid / 6 * 6 + 6 || dojoMap.getId() < clockid / 6 * 6) {
                    return;
                }
                client.getPlayer().changeMap(client.getChannelServer().getMapFactory().getMap(925020000));
            }
        }, time * 1000 + 100); // let the TIMES UP display for .1 seconds like in GMS
    }

    public void showNote() {
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT * FROM notes WHERE `to`=?", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            rs.last();
            rs.first();
            client.getSession().write(MaplePacketCreator.showNotes(rs, rs.getRow()));
            rs.close();
            ps.close();
        } catch (SQLException e) {
        }
    }

    private void silentEnforceMaxHpMp() {
        setMp(getMp());
        setHp(getHp(), true);
    }

    public void silentGiveBuffs(List<PlayerBuffValueHolder> buffs) {
        for (PlayerBuffValueHolder mbsvh : buffs) {
            mbsvh.effect.silentApplyBuff(this, mbsvh.startTime);
        }
    }

    public void silentPartyUpdate() {
        if (party != null) {
            try {
                client.getChannelServer().getWorldInterface().updateParty(party.getId(), PartyOperation.SILENT_UPDATE, new MaplePartyCharacter(this));
            } catch (RemoteException e) {
                e.printStackTrace();
                client.getChannelServer().reconnectWorld();
            }
        }
    }

    public static class SkillEntry {

        public int skillevel, masterlevel;

        public SkillEntry(int skillevel, int masterlevel) {
            this.skillevel = skillevel;
            this.masterlevel = masterlevel;
        }
    }

    public boolean skillisCooling(int skillId) {
        return coolDowns.containsKey(Integer.valueOf(skillId));
    }

    public void startCygnusIntro() {
        client.getSession().write(MaplePacketCreator.cygnusIntroDisableUI(true));
        client.getSession().write(MaplePacketCreator.cygnusIntroLock(true));
        saveLocation("CYGNUSINTRO");
        MapleMap introMap = client.getChannelServer().getMapFactory().getMap(913040000);
        changeMap(introMap, introMap.getPortal(0));
        TimerManager.getInstance().schedule(new Runnable() {

            @Override
            public void run() {
                client.getSession().write(MaplePacketCreator.cygnusIntroDisableUI(false));
                client.getSession().write(MaplePacketCreator.cygnusIntroLock(false));
            }
        }, 54 * 1000);
        savedLocations[SavedLocationType.CYGNUSINTRO.ordinal()] = null;
    }

    public void startFullnessSchedule(final int decrease, final MaplePet pet, int petSlot) {
        ScheduledFuture<?> schedule = TimerManager.getInstance().register(new Runnable() {

            @Override
            public void run() {
                int newFullness = pet.getFullness() - decrease;
                if (newFullness <= 5) {
                    pet.setFullness(15);
                    pet.saveToDb();
                    unequipPet(pet, true, true);
                    dropMessage(pet.getName() + " starved and went back home.");
                } else {
                    pet.setFullness(newFullness);
                    client.getSession().write(MaplePacketCreator.updatePet(pet));
                }
            }
        }, 120000, 120000);
        fullness[petSlot] = schedule;
    }

    public void startMapTimeLimitTask(final MapleMap from, final MapleMap to) {
        if (to.getTimeLimit() > 0 && from != null) {
            mapTimeLimitTask = TimerManager.getInstance().register(new Runnable() {

                @Override
                public void run() {
                    MaplePortal pfrom = null;
                    switch (from.getId()) {
                        case 100020000: // pig
                        case 105040304: // golem
                        case 105050100: // mushroom
                        case 221023400: // rabbit
                        case 240020500: // kentasaurus
                        case 240040511: // skelegons
                        case 240040520: // newties
                        case 260020600: // sand rats
                        case 261020300: // magatia
                            pfrom = from.getPortal("MD00");
                            break;
                        default:
                            pfrom = from.getPortal(0);
                    }
                    if (pfrom != null) {
                        MapleCharacter.this.changeMap(from, pfrom);
                    }
                }
            }, from.getTimeLimit() * 1000, from.getTimeLimit() * 1000);
        }
    }

    public void stopControllingMonster(MapleMonster monster) {
        controlled.remove(monster);
    }

    public void toggleGMChat() {
        whitechat = !whitechat;
    }

    public void unequipAllPets() {
        for (int i = 0; i < 3; i++) {
            if (pets[i] != null) {
                unequipPet(pets[i], true);
            }
        }
    }

    public void unequipPet(MaplePet pet, boolean shift_left) {
        unequipPet(pet, shift_left, false);
    }

    public void unequipPet(MaplePet pet, boolean shift_left, boolean hunger) {
        if (this.getPet(this.getPetIndex(pet)) != null) {
            this.getPet(this.getPetIndex(pet)).saveToDb();
        }
        int petSlot = getPetIndex(pet);
        if (fullness[petSlot] != null) {
            fullness[petSlot].cancel(false);
            fullness[petSlot] = null;
        }
        getMap().broadcastMessage(this, MaplePacketCreator.showPet(this, pet, true, hunger), true);
        //updateSingleStat(MapleStat.PET, 0);
        client.getSession().write(MaplePacketCreator.petStatUpdate(this));
        client.getSession().write(MaplePacketCreator.enableActions());
        removePet(pet, shift_left);
    }

    public void updateMacros(int position, SkillMacro updateMacro) {
        skillMacros[position] = updateMacro;
    }

    public void updatePartyMemberHP() {
        if (party != null) {
            int channel = client.getChannel();
            for (MaplePartyCharacter partychar : party.getMembers()) {
                if (partychar.getMapid() == getMapId() && partychar.getChannel() == channel) {
                    MapleCharacter other = ChannelServer.getInstance(channel).getPlayerStorage().getCharacterByName(partychar.getName());
                    if (other != null) {
                        other.client.getSession().write(MaplePacketCreator.updatePartyMemberHP(getId(), this.hp, localmaxhp));
                    }
                }
            }
        }
    }

    public void updateQuest(MapleQuestStatus quest) {
        quests.put(quest.getQuest(), quest);
        if (quest.getStatus().equals(MapleQuestStatus.Status.STARTED)) {
            client.getSession().write(MaplePacketCreator.startQuest(this, (short) quest.getQuest().getId()));
            client.getSession().write(MaplePacketCreator.updateQuestInfo(this, (short) quest.getQuest().getId(), quest.getNpc(), (byte) 8));
        } else if (quest.getStatus().equals(MapleQuestStatus.Status.COMPLETED)) {
            client.getSession().write(MaplePacketCreator.completeQuest(this, (short) quest.getQuest().getId()));
        } else if (quest.getStatus().equals(MapleQuestStatus.Status.NOT_STARTED)) {
            client.getSession().write(MaplePacketCreator.forfeitQuest(this, (short) quest.getQuest().getId()));
        }
    }

    public void updateSingleStat(MapleStat stat, int newval) {
        updateSingleStat(stat, newval, false);
    }

    private void updateSingleStat(MapleStat stat, int newval, boolean itemReaction) {
        client.getSession().write(MaplePacketCreator.updatePlayerStats(Collections.singletonList(new Pair<MapleStat, Integer>(stat, Integer.valueOf(newval))), itemReaction));
    }

    @Override
    public int getObjectId() {
        return getId();
    }

    public MapleMapObjectType getType() {
        return MapleMapObjectType.PLAYER;
    }

    public void sendDestroyData(MapleClient client) {
        client.getSession().write(MaplePacketCreator.removePlayerFromMap(this.getObjectId()));
    }

    @Override
    public void sendSpawnData(MapleClient client) {
        if ((this.isHidden() && client.getPlayer().isSGM()) || !this.isHidden()) {
            client.getSession().write(MaplePacketCreator.spawnPlayerMapobject(this));
            for (int i = 0; pets[i] != null; i++) {
                client.getSession().write(MaplePacketCreator.showPet(this, pets[i], false, false));
            }
        }
    }

    @Override
    public void setObjectId(int id) {
    }

    @Override
    public String toString() {
        return name;
    }

    public CheatTracker getCheatTracker() {
        return anticheat;
    }

    public void InitiateSaveEvent() {
        periodicSaveTask = TimerManager.getInstance().register(new Runnable() {

            @Override
            public void run() {
                client.getPlayer().saveToDB(true);
            }
        }, 300000); // 5 mins
    }

    public boolean hasreceivedMOTB() {
        return this.receivedMOTB;
    }

    public void setreceivedMOTB(boolean received) {
        this.receivedMOTB = received;
        this.saveToDB(true);
    }

    public boolean allowedMapChange() {
        return this.allowMapChange;
    }

    public void setallowedMapChange(boolean allowed) {
        this.allowMapChange = allowed;
    }
    public int aranCombo;

    public int getCombo() {
        return aranCombo;
    }

    public int setCombo(int _new) {
        if (aranCombo % 10 == 0) {
            client.getSession().write(MaplePacketCreator.addComboBuff(_new));
        }
        return aranCombo = _new;
    }

    	public void addCSPoints(int type, int quantity) {
		if (type == 1) {
			this.paypalnx += quantity;
		} else if (type == 2) {
			this.maplepoints += quantity;
		} else if (type == 4) {
			this.cardnx += quantity;
		}
	}

    private String prepareKeymapQuery() {
        StringBuilder query = new StringBuilder("INSERT INTO keymap (characterid, `key`, `type`, `action`) VALUES ");

        for (Iterator<Entry<Integer, MapleKeyBinding>> i = this.keymap.entrySet().iterator(); i.hasNext();) {
            String entry = "";
            Formatter itemEntry = new Formatter();
            Entry<Integer, MapleKeyBinding> e = i.next();
            itemEntry.format("(%d, %d, %d, %d)",
                    id, e.getKey().intValue(), e.getValue().getType(), e.getValue().getAction());
            if (i.hasNext()) {
                entry = itemEntry.toString() + ", ";
            } else {
                entry = itemEntry.toString();
            }

            query.append(entry);

        }
        //      System.out.println(query);
        return query.toString();
    }

    private String prepareSkillQuery() {
        StringBuilder query = new StringBuilder("INSERT INTO skills (characterid, skillid, skilllevel, masterlevel) VALUES ");

        for (Iterator<Entry<ISkill, SkillEntry>> i = this.skills.entrySet().iterator(); i.hasNext();) {
            String entry = "";
            Formatter itemEntry = new Formatter();
            Entry<ISkill, SkillEntry> e = i.next();
            itemEntry.format("(%d, %d, %d, %d)",
                    id, e.getKey().getId(), e.getValue().skillevel, e.getValue().masterlevel);
            if (i.hasNext()) {
                entry = itemEntry.toString() + ", ";
            } else {
                entry = itemEntry.toString();
            }

            query.append(entry);

        }
        //   ouSystem.t.println(query);
        return query.toString();
    }

    public void setStuck(boolean isStuck) {
        this.stuck = isStuck;
    }

    public boolean isStuck() {
        return stuck;
    }

    public void empty()//scheduled tasks need to be cancelled, otherwise strong refs remain to this, and we all know what that means
    {
        this.cancelMapTimeLimitTask();
        this.cancelPeriodicSaveTask();
        this.cancelAllBuffs();
        this.anticheat.killInvalidationTask();
        this.anticheat = null;

        if (dragonBloodSchedule != null) {
            dragonBloodSchedule.cancel(false);
        }
        if (hpDecreaseTask != null) {
            hpDecreaseTask.cancel(false);
        }
        if (beholderHealingSchedule != null) {
            beholderHealingSchedule.cancel(false);
        }
        if (beholderBuffSchedule != null) {
            beholderBuffSchedule.cancel(false);
        }
        if (BerserkSchedule != null) {
            BerserkSchedule.cancel(false);
        }

        if (fullness != null) {
            for (ScheduledFuture<?> f : fullness) {
                if (f != null) {
                    f.cancel(false);
                }
            }
        }
        this.maplemount = null;
        if (this.mgc != null) {
            this.mgc.setOnline(false);
        }
        this.mgc = null;
        this.client = null; //refs need to be nulled from char -> client AND client -> char
    }

    public void setMegaLimit(long limit) {
        this.megaLimit = limit;
    }

    public long getMegaLimit() {
        return this.megaLimit;
    }

    public int updateMesosGetOverflow(int gain) {
        int origMesos = meso.get();
        int overflow = 0;
        if (((long) (origMesos) + gain) >= 2147483647L) { //no-op; they've reached max mesos
            overflow = ((origMesos + gain) - 2147483647);
            updateSingleStat(MapleStat.MESO, meso.addAndGet(2147483647), true);
        } else {
            updateSingleStat(MapleStat.MESO, meso.addAndGet(gain), true);
        }
        client.getSession().write(MaplePacketCreator.getShowMesoGain(gain, false));
        return overflow;
    }

    public long getAfkTime() {
        return afkTime;
    }

    public void resetAfkTime() {
        if (this.chalktext != null && this.chalktext.equals("I'm currently AFK. Do not disturb or talk to me:)")) {
            setChalkboard(null);
        }
        afkTime = System.currentTimeMillis();
    }

    public int getPoints() {
        return this.points;
    }

    public void setPoints(int newPoints) {
        this.points = newPoints;
    }
}



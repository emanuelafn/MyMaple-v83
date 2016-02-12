/*
This file is part of the OdinMS Maple Story Server
Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
Matthias Butz <matze@odinms.de>
Jan Christian Meyer <vimes@odinms.de>

This prgogram is free software: you can redistribute it and/or modify
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
package scripting.npc;

import client.Equip;
import client.IItem;
import client.ISkill;
import client.Item;
import client.ItemFactory;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import constants.ExpTable;
import client.MapleCharacter;
import client.MapleClient;
import client.MapleInventory;
import client.MapleInventoryType;
import client.MapleJob;
import client.MapleOccupations;
import client.MaplePet;
import client.MapleQuestStatus;
import client.MapleSkinColor;
import client.MapleStat;
import client.SkillFactory;
import java.awt.Point;
import tools.Randomizer;
import java.io.File;
import java.util.Map;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import net.channel.ChannelServer;
import net.world.MapleParty;
import net.world.MaplePartyCharacter;
import tools.DatabaseConnection;
import net.world.guild.MapleAlliance;
import net.world.guild.MapleGuild;
import net.world.remote.WorldChannelInterface;
import provider.MapleData;
import provider.MapleDataProviderFactory;
import scripting.AbstractPlayerInteraction;
import scripting.event.EventManager;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.MapleShopFactory;
import server.MapleSquad;
import server.MapleSquadType;
import server.MapleStatEffect;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.life.MapleMonsterStats;
import server.maps.MapleMap;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import server.quest.MapleQuest;
import tools.MaplePacketCreator;
import tools.Pair;

/**
 *
 * @author Matze
 */
public class NPCConversationManager extends AbstractPlayerInteraction {

    private int npc;
    private String getText;

    public NPCConversationManager(MapleClient c, int npc) {
        super(c);
        this.npc = npc;
    }
    public int getNpc() {
        return npc;
    }
    public void clearDrops() {
        MapleMap map = getPlayer().getMap();
        List<MapleMapObject> items = map.getMapObjectsInRange(getPlayer().getPosition(), Double.POSITIVE_INFINITY, Arrays.asList(MapleMapObjectType.ITEM));
        for (MapleMapObject i : items) {
            map.removeMapObject(i);
            map.broadcastMessage(MaplePacketCreator.removeItemFromMap(i.getObjectId(), 0, getPlayer().getId()));
        }
    }
    public void killAllMonsters() {
        MapleMap map = getPlayer().getMap();
        map.killAllMonsters(false); // No drop.
    }   
    public void consolePrint(String input) {
        System.out.println(input);
    }
    public void summonMobAtPosition(int mobid, int amount, int posx, int posy) {
        if (amount <= 1) {
            MapleMonster npcmob = MapleLifeFactory.getMonster(mobid);
            npcmob.setHp(npcmob.getMaxHp());
            getPlayer().getMap().spawnMonsterOnGroudBelow(npcmob, new Point(posx, posy));
        } 
        else {
            for (int i = 0; i < amount; i++) {
                MapleMonster npcmob = MapleLifeFactory.getMonster(mobid);
                npcmob.setHp(npcmob.getMaxHp());
                getPlayer().getMap().spawnMonsterOnGroudBelow(npcmob, new Point(posx, posy));
            }
        }
    }
    public void morph(String id){
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        ii.getItemEffect(Integer.parseInt("2210" + id)).applyTo(getPlayer());
    }
    public void spawnMob(int mapid, int mid, int xpos, int ypos) {
        getClient().getChannelServer().getMapFactory().getMap(mapid).spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(mid), new Point(xpos, ypos));
    }
    public void summonMob(int mobid, int customHP, int customEXP, int amount) {
        spawnMonster(mobid, customHP, -1, -1, customEXP, 0, 0, amount, getPlayer().getPosition().x, getPlayer().getPosition().y);
    }
    public void spawnMonster(int mobid, int HP, int MP, int level, int EXP, int boss, int undead, int amount, int x, int y) {
        MapleMonsterStats newStats = new MapleMonsterStats();
        if (HP >= 0) {
            newStats.setHp(HP);
        }
        if (MP >= 0) {
            newStats.setMp(MP);
        }
        if (level >= 0) {
            newStats.setLevel(level);
        }
        if (EXP >= 0) {
            newStats.setExp(EXP);
        }
        newStats.setBoss(boss == 1);
        newStats.setUndead(undead == 1);
        for (int i = 0; i < amount; i++) {
            MapleMonster npcmob = MapleLifeFactory.getMonster(mobid);
            npcmob.setOverrideStats(newStats);
            npcmob.setHp(npcmob.getMaxHp());
            npcmob.setMp(npcmob.getMaxMp());
            getPlayer().getMap().spawnMonsterOnGroundBelow(npcmob, new Point(x, y));
        }
    }
    public void dispose() {
        NPCScriptManager.getInstance().dispose(this);
    }
    public void sendNext(String text) {
        getClient().getSession().write(MaplePacketCreator.getNPCTalk(npc, (byte) 0, text, "00 01"));
    }
    public void sendPrev(String text) {
        getClient().getSession().write(MaplePacketCreator.getNPCTalk(npc, (byte) 0, text, "01 00"));
    }
    public void sendNextPrev(String text) {
        getClient().getSession().write(MaplePacketCreator.getNPCTalk(npc, (byte) 0, text, "01 01"));
    }
    public void sendOk(String text) {
        getClient().getSession().write(MaplePacketCreator.getNPCTalk(npc, (byte) 0, text, "00 00"));
    }
    public void sendYesNo(String text) {
        getClient().getSession().write(MaplePacketCreator.getNPCTalk(npc, (byte) 1, text, ""));
    }
    public void resetState() {
        getPlayer().setState(0);
    }
    public void sendAcceptDecline(String text) {
        getClient().getSession().write(MaplePacketCreator.getNPCTalk(npc, (byte) 0x0C, text, ""));
    }
    public void openShop(int id) {
        MapleShopFactory.getInstance().getShop(id).sendShop(getClient());
	}
    public void sendSimple(String text) {
        getClient().getSession().write(MaplePacketCreator.getNPCTalk(npc, (byte) 4, text, ""));
    }
    public void teachSkill(int id, int level, int masterlevel) {
        getPlayer().changeSkillLevel(SkillFactory.getSkill(id), level, masterlevel);
    }
    public void sendStyle(String text, int styles[]) {
        getClient().getSession().write(MaplePacketCreator.getNPCTalkStyle(npc, text, styles));
    }
    public void sendGetNumber(String text, int def, int min, int max) {
        getClient().getSession().write(MaplePacketCreator.getNPCTalkNum(npc, text, def, min, max));
    }
    public void sendGetText(String text) {
        getClient().getSession().write(MaplePacketCreator.getNPCTalkText(npc, text));
    }
    public void setGetText(String text) {
        this.getText = text;
    }
    public List<Pair<Integer, IItem>> getStoredMerchantItems() {
        Connection con = DatabaseConnection.getConnection();
        List<Pair<Integer, IItem>> items = new ArrayList<Pair<Integer, IItem>>();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT * FROM hiredmerchant WHERE ownerid = ? AND onSale = false");
            ps.setInt(1, getPlayer().getId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                if (rs.getInt("type") == 1) {
                    Equip eq = new Equip(rs.getInt("itemid"), (byte) 0, -1);
                    eq.setUpgradeSlots((byte) rs.getInt("upgradeslots"));
                    eq.setLevel((byte) rs.getInt("level"));
                    eq.setStr((short) rs.getInt("str"));
                    eq.setDex((short) rs.getInt("dex"));
                    eq.setInt((short) rs.getInt("int"));
                    eq.setLuk((short) rs.getInt("luk"));
                    eq.setHp((short) rs.getInt("hp"));
                    eq.setMp((short) rs.getInt("mp"));
                    eq.setWatk((short) rs.getInt("watk"));
                    eq.setMatk((short) rs.getInt("matk"));
                    eq.setWdef((short) rs.getInt("wdef"));
                    eq.setMdef((short) rs.getInt("mdef"));
                    eq.setAcc((short) rs.getInt("acc"));
                    eq.setAvoid((short) rs.getInt("avoid"));
                    eq.setHands((short) rs.getInt("hands"));
                    eq.setSpeed((short) rs.getInt("speed"));
                    eq.setJump((short) rs.getInt("jump"));
                    eq.setOwner(rs.getString("owner"));
                    items.add(new Pair<Integer, IItem>(rs.getInt("id"), eq));                           
                } 
                else if (rs.getInt("type") == 2) {
                    Item newItem = new Item(rs.getInt("itemid"), (byte) 0, (short) rs.getInt("quantity"));
                    newItem.setOwner(rs.getString("owner"));
                    items.add(new Pair<Integer, IItem>(rs.getInt("id"), newItem));
                }
            }
            ps.close();
            rs.close();
        } 
        catch (SQLException se) {
            se.printStackTrace();
            return null;
        }
        return items;
    }
    public void removeHiredMerchantItem(int id) {
        try {
            List<Pair<IItem, MapleInventoryType>> workingList = getHiredMerchantItems();
            for (Pair<IItem, MapleInventoryType> p : workingList) {
                if (p.getLeft().getDBID() == id) {
                    workingList.remove(p);
                    break;
                }
            }
            ItemFactory.MERCHANT.saveItems(workingList, c.getPlayer().getId());
            c.getPlayer().saveToDB(true);
        } 
        catch (Exception e) {
        }
    }
    public List<Pair<IItem, MapleInventoryType>> getHiredMerchantItems() {
        try {
            return ItemFactory.MERCHANT.loadItems(c.getPlayer().getId(), false);
        } catch (Exception e) {
            System.out.println("Error loading merchant items:");
            e.printStackTrace();
        }
        return null;
    }
    public void resetStats() {
        List<Pair<MapleStat, Integer>> statup = new ArrayList<Pair<MapleStat, Integer>>(5);
        int totAp = getPlayer().getRemainingAp() + getPlayer().getStr() + getPlayer().getDex() + getPlayer().getInt() + getPlayer().getLuk();
        getPlayer().setStr(4);
        getPlayer().setDex(4);
        getPlayer().setInt(4);
        getPlayer().setLuk(4);
        getPlayer().setRemainingAp(totAp - 16);
        statup.add(new Pair<MapleStat, Integer>(MapleStat.STR, Integer.valueOf(4)));
        statup.add(new Pair<MapleStat, Integer>(MapleStat.DEX, Integer.valueOf(4)));
        statup.add(new Pair<MapleStat, Integer>(MapleStat.LUK, Integer.valueOf(4)));
        statup.add(new Pair<MapleStat, Integer>(MapleStat.INT, Integer.valueOf(4)));
        statup.add(new Pair<MapleStat, Integer>(MapleStat.AVAILABLEAP, Integer.valueOf(getPlayer().getRemainingAp())));
        getClient().getSession().write(MaplePacketCreator.updatePlayerStats(statup));
    }
    public String getText() {
        return this.getText;
    }
    public int getJobId() {
        return getPlayer().getJob().getId();
    }
    public void startQuest(int id) {
        try {
            MapleQuest.getInstance(id).forceStart(getPlayer(), npc);
        } 
        catch (NullPointerException ex) {
        }
    }
    public void resetReactors() {
        c.getPlayer().getMap().resetReactors();
    }
    public void completeQuest(int id) {
        try {
            MapleQuest.getInstance(id).forceComplete(getPlayer(), npc);
        } 
        catch (NullPointerException ex) {
        }
    }
    public int getMeso() {
        return getPlayer().getMeso();
    }
    public void gainReborns(int reborns) {
        getPlayer().setReborns(reborns + getPlayer().getReborns());
    }
    public void reloadChar() {
        getPlayer().getClient().getSession().write(MaplePacketCreator.getCharInfo(getPlayer()));
        getPlayer().getMap().removePlayer(getPlayer());
        getPlayer().getMap().addPlayer(getPlayer());
    }
    public int getPlayersInMap(int mapId){
        return( getClient().getChannelServer().getMapFactory().getMap(mapId).getAllPlayer().size() );
    }
    public void connected(){
        try {
            Map<Integer, Integer> connected = c.getChannelServer().getWorldInterface().getConnected();
            StringBuilder conStr = new StringBuilder("Connected players : ");
            boolean first = true;
            for (int i : connected.keySet()) {
                if (!first) {
                    conStr.append("");
                    } 
                else {
                    first = false;
                    }
                    if (i == 0) {
                        conStr.append("Total : #b");
                        conStr.append(connected.get(i) + "#k\r\n");
                    } 
                    else {
                        conStr.append("Channel ");
                        conStr.append(i);
                        conStr.append(" : #b");
                        conStr.append(connected.get(i)+ "#k\r\n");
                    }
            }
              sendOk(conStr.toString());
            } 
            catch (RemoteException e) {
                c.getChannelServer().reconnectWorld();
            }
			//dispose();let npc script do this
    }
    public String ranking(String job) {
        try {
          int int1 = 0;
          int int2 = 0;
          Connection con = DatabaseConnection.getConnection();
          PreparedStatement ps = con.prepareStatement("SELECT reborns, level, world, characters.name name, job, guilds.name guildname,COUNT(eventstats.characterid) wins FROM accounts, characters LEFT JOIN guilds ON guilds.guildid = characters.guildid LEFT JOIN eventstats ON characters.id=eventstats.characterid WHERE characters.gm < 3 AND characters.job >= ? AND characters.job <= ? AND characters.world = "+getPlayer().getWorld()+" AND accountid = accounts.id AND banned = 0  GROUP BY characters.id DESC ORDER BY reborns desc, level DESC, exp DESC LIMIT 50");
               if (job.equals("warrior")) {
                    int1 = 100;
                   int2 = 132;
                 } else if (job.equals("magician")) {
                    int1 = 200;
                    int2 = 232;
                 }  else if (job.equals("bowman")) {
                    int1 = 300;
                     int2 = 322;
                 } else if (job.equals("thief")) {
                     int1 = 400;
                     int2 = 422;
                 } else if (job.equals("pirate")) {
                     int1 = 500;
                     int2 = 522;
                 } else if (job.equals("beginner")) {
                     int1 = 0;
                     int2 = 0;
                 } else if (job.equals("nobless")) {
                     int1 = 1000;
                     int2 = 1000;
                 } else if (job.equals("cyg1")) {
                     int1 = 1100;
                     int2 = 1199;
                 } else if (job.equals("cyg2")) {
                     int1 = 1200;
                     int2 = 1299;
                 } else if (job.equals("cyg3")) {
                     int1 = 1300;
                     int2 = 1399;
                 } else if (job.equals("cyg4")) {
                     int1 = 1400;
                     int2 = 1499;
                 } else if (job.equals("cyg5")) {
                     int1 = 1500;
                     int2 = 1599;
                 } else if (job.equals("aran")) {
                     int1 = 2000;
                     int2 = 2199;
                 } else if (job.equals("total")) {
                     int1 = 0;
                     int2 = 9999;

                 } else if (job.equals("me")) {
             ps = con.prepareStatement("SELECT name FROM characters WHERE world = "+getPlayer().getWorld()+" AND gm = 0 ORDER BY reborns, level DESC");
             ResultSet rs = ps.executeQuery();
                 int i = 0;
                 int lol = 0;
                 while (rs.next()) {
            i++;
            if (rs.getString("name").equals(c.getPlayer().getName())) {
                lol = i;
            }
                }
                ps.close();
                rs.close();
                if (getPlayer().isDead()){
                return ("Dude! View this when your alife kay?");
                }else{
                return ("Your current rank is number " + lol +" in this world.");
                }
            } else {
                return "";
            }
            ps.setInt(1, int1);
            ps.setInt(2, int2);
            ResultSet rs = ps.executeQuery();
            int i = 0;
            String lulul = "";
            while (rs.next()) {
                i++;
                lulul += (i + ") #e" + rs.getString("name") + "#n  ,  Reborns :  #b" + rs.getInt("reborns") +"#k  ,  Level : #r" + rs.getInt("level") + "#k \r\n");
            }
            ps.close();
            rs.close();
            return lulul;
        } catch (SQLException e) {
            return "Unknown error, please report this to the admin";
        }
    }
    public void gainMeso(int gain) {
        getPlayer().gainMeso(gain, true, false, true);
    }
    public void gainCookingExp(int gain) {
        getPlayer().gainCookingEXP(gain);
    }
    public void openNpc(int npcid){
        NPCScriptManager npc = NPCScriptManager.getInstance();
        npc.start(c, npcid, null, null);
    }
    public void gainExp(int gain) {
        getPlayer().gainExp(gain, true, true);
    }
    public int getLevel() {
        return getPlayer().getLevel();
    }
    public MapleCharacter getP() {
        return getPlayer();
    }
    public void giveRandItem(int itemid) {
        Equip e = (Equip) MapleItemInformationProvider.getInstance().getEquipById(itemid);
        e.setStr((short) (Math.random() * 50 + 100));
        e.setDex((short) (Math.random() * 50 + 100));
        e.setInt((short) (Math.random() * 50 + 100));
        e.setLuk((short) (Math.random() * 50 + 100));
        MapleInventoryManipulator.addFromDrop(c, e);
    }

    public void giveRandItem1(int itemid) {
        Equip e = (Equip) MapleItemInformationProvider.getInstance().getEquipById(itemid);
        e.setStr((short) (Math.random() * 50 + 250));
        e.setDex((short) (Math.random() * 50 + 250));
        e.setInt((short) (Math.random() * 50 + 250));
        e.setLuk((short) (Math.random() * 50 + 250));
        MapleInventoryManipulator.addFromDrop(c, e);
    }
    public void giveRandItem2(int itemid) {
        Equip e = (Equip) MapleItemInformationProvider.getInstance().getEquipById(itemid);
        e.setStr((short) (Math.random() * 50 + 450));
        e.setDex((short) (Math.random() * 50 + 450));
        e.setInt((short) (Math.random() * 50 + 450));
        e.setLuk((short) (Math.random() * 50 + 450));
        MapleInventoryManipulator.addFromDrop(c, e);
    }
    public String EquipList(MapleClient c) {
        StringBuilder str = new StringBuilder();
        MapleInventory equip = c.getPlayer().getInventory(MapleInventoryType.EQUIP);
        List<String> stra = new LinkedList<String>();
        for (IItem item : equip.list()) {
            stra.add("#L"+item.getPosition()+"##v"+item.getItemId()+"##l");
        }
        for (String strb : stra) {
            str.append(strb);
        }
        return str.toString();
    }
    public void maxAranskills() {
        for (MapleData skill_ : MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/" + "String.wz")).getData("Skill.img").getChildren()) {
            try {
                ISkill skill = SkillFactory.getSkill(Integer.parseInt(skill_.getName()));
                if (skill.getId() < 1009 || skill.getId() > 1011) {
                    c.getPlayer().changeSkillLevel(skill, skill.getMaxLevel(), skill.getMaxLevel());
                }
                } 
            catch (NumberFormatException nfe) {
                break;
            } 
            catch (NullPointerException npe) {
                continue;
            }
        }
    }
    public EventManager getEventManager(String event) {
        return getClient().getChannelServer().getEventSM().getEventManager(event);
    }
    public void changeOccupationById(int occ) {
        getPlayer().changeOccupation(MapleOccupations.getById(occ));
    }
    public boolean HasOccupation() {
        return (getPlayer().getOccupation().getId() % 100 == 0);
    }
    public boolean HasOccupation0() {
        return (getPlayer().getOccupation().getId() == 1);
    }
    public boolean HasOccupation1() {
        return (getPlayer().getOccupation().getId() == 100);
    }
    public boolean HasOccupation2() {
        return (getPlayer().getOccupation().getId() == 110);
    }
    public boolean HasOccupation3() {
        return (getPlayer().getOccupation().getId() == 120);
    }
    public boolean HasOccupation4() {
        return (getPlayer().getOccupation().getId() == 130);
    }
    public boolean HasOccupation5() {
        return (getPlayer().getOccupation().getId() == 140);
    }
    public boolean HasOccupation6() {
        return (getPlayer().getOccupation().getId() == 150);
    }
    public boolean HasOccupation7() {
        return (getPlayer().getOccupation().getId() == 160);
    }
    public boolean HasOccupation8() {
        return (getPlayer().getOccupation().getId() == 170);
    }
    public boolean HasOccupation9() {
        return (getPlayer().getOccupation().getId() == 180);
    }
    public boolean HasOccupation10() {
        return (getPlayer().getOccupation().getId() == 190);
    }
     public int getVotePoints() {
        return getPlayer().getVotePoints();
    }
    public void setVotePoints(int x) {
        getPlayer().setVotePoints(getPlayer().getVotePoints() + x);
    }
    public void showEffect(String effect) {
        getPlayer().getMap().broadcastMessage(MaplePacketCreator.environmentChange(effect, 3));
    }
    public void playSound(String sound) {
        getPlayer().getMap().broadcastMessage(MaplePacketCreator.environmentChange(sound, 4));
    }
    public void setHair(int hair) {
        getPlayer().setHair(hair);
        getPlayer().updateSingleStat(MapleStat.HAIR, hair);
        getPlayer().equipChanged();
    }
    public void setFace(int face) {
        getPlayer().setFace(face);
        getPlayer().updateSingleStat(MapleStat.FACE, face);
        getPlayer().equipChanged();
    }
    public void setSkin(int color) {
        getPlayer().setSkinColor(MapleSkinColor.getById(color));
        getPlayer().updateSingleStat(MapleStat.SKIN, color);
        getPlayer().equipChanged();
    }
    public void setLevelz(int level) {
        getPlayer().setLevel(10);
    }
    public void setLevelx(int level) {
        getPlayer().setLevel(8);
    }
    public int itemQuantity(int itemid) {
        return getPlayer().getInventory(MapleItemInformationProvider.getInstance().getInventoryType(itemid)).countById(itemid);
    }
    public void displayGuildRanks() {
        MapleGuild.displayGuildRanks(getClient(), npc);
    }
    public void environmentChange(String env, int mode) {
        getPlayer().getMap().broadcastMessage(MaplePacketCreator.environmentChange(env, mode));
    }
    public void gainCloseness(int closeness) {
        for (MaplePet pet : getPlayer().getPets()) {
            if (pet.getCloseness() > 30000) {
                pet.setCloseness(30000);
                return;
            }
            pet.gainCloseness(closeness);
            while (pet.getCloseness() > ExpTable.getClosenessNeededForLevel(pet.getLevel())) {
                pet.setLevel(pet.getLevel() + 1);
                getClient().getSession().write(MaplePacketCreator.showOwnPetLevelUp(getPlayer().getPetIndex(pet)));
            }
            getPlayer().getClient().getSession().write(MaplePacketCreator.updatePet(pet));
        }
    }
    public String getName() {
        return getPlayer().getName();
    }
    public int getGender() {
        return getPlayer().getGender();
    }
    public void modifyNX(int amount, int type) {
        getPlayer().addCSPoints(type, amount);
        if (amount > 0) {
            getClient().getSession().write(MaplePacketCreator. serverNotice(5, "You have gained NX Cash (+" + amount +")."));
        } 
        else {
            getClient().getSession().write(MaplePacketCreator. serverNotice(5, "You have lost NX Cash (" + (amount) +")."));
        }
    }
    public void changeJobById(int a) {
        getPlayer().changeJob(MapleJob.getById(a));
    }
    public void addRandomItem(int id) {
        MapleItemInformationProvider i = MapleItemInformationProvider.getInstance();
        MapleInventoryManipulator.addFromDrop(getClient(), i.randomizeStats((Equip) i.getEquipById(id)), true);
    }
    public MapleJob getJobName(int id) {
        return MapleJob.getById(id);
    }
    public boolean isQuestCompleted(int quest) {
        try {
            return getQuestStatus(quest) == MapleQuestStatus.Status.COMPLETED;
        } 
        catch (NullPointerException e) {
            return false;
        }
    }
    public boolean isQuestStarted(int quest) {
        try {
            return getQuestStatus(quest) == MapleQuestStatus.Status.STARTED;
        } 
        catch (NullPointerException e) {
            return false;
        }
    }
    public void sendCygnusCharCreate() {
        c.getSession().write(MaplePacketCreator.sendCygnusCreateChar());
    }
    public MapleStatEffect getItemEffect(int itemId) {
        return MapleItemInformationProvider.getInstance().getItemEffect(itemId);
    }
    public void maxMastery() {
        for (MapleData skill_ : MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/" + "String.wz")).getData("Skill.img").getChildren()) {
            try {
                ISkill skill = SkillFactory.getSkill(Integer.parseInt(skill_.getName()));
                if ((skill.getId() / 10000 % 10 == 2 || (getClient().getPlayer().isCygnus() && skill.getId() / 10000 % 10 == 1)) && getPlayer().getSkillLevel(skill) < 1) {
                    getPlayer().changeSkillLevel(skill, 0, skill.getMaxLevel());
                }
            } 
            catch (NumberFormatException nfe) {
                break;
            } 
            catch (NullPointerException npe) {
                continue;
            }
        }
    }
    public void unmute() {
        getPlayer().canTalk(true);
    }
    public void worldMessage(int type, String message) {
        try {
            getPlayer().getClient().getChannelServer().getWorldInterface().broadcastMessage(null, MaplePacketCreator.serverNotice(type, message).getBytes());
        } catch (RemoteException e) {
            c.getChannelServer().reconnectWorld();
        }
    }
    public void processGachapon(int[] id, boolean remote) {
        int[] gacMap = {100000000, 101000000, 102000000, 103000000, 105040300, 800000000, 809000101, 809000201, 600000000, 120000000};
        int itemid = id[Randomizer.getInstance().nextInt(id.length)];
        addRandomItem(itemid);
        if (!remote) {
            gainItem(5220000, (short) -1);
        }
        sendNext("You have obtained a #b#t" + itemid + "##k.");
        //getClient().getChannelServer().broadcastPacket(MaplePacketCreator.gachaponMessage(getPlayer().getInventory(MapleInventoryType.getByType((byte) (itemid / 1000000))).findById(itemid), c.getPlayer().getMapName(gacMap[(getNpc() != 9100117 && getNpc() != 9100109) ? (getNpc() - 9100100) : getNpc() == 9100109 ? 8 : 9]), getPlayer()));
    }
    public void disbandAlliance(MapleClient c, int allianceId) {
        PreparedStatement ps = null;
        try {
            ps = DatabaseConnection.getConnection().prepareStatement("DELETE FROM `alliance` WHERE id = ?");
            ps.setInt(1, allianceId);
            ps.executeUpdate();
            ps.close();
            c.getChannelServer().getWorldInterface().allianceMessage(c.getPlayer().getGuild().getAllianceId(), MaplePacketCreator.disbandAlliance(allianceId), -1, -1);
            c.getChannelServer().getWorldInterface().disbandAlliance(allianceId);
        } catch (RemoteException r) {
            c.getChannelServer().reconnectWorld();
        } catch (SQLException sqle) {
            sqle.printStackTrace();
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException ex) {
            }
        }
    }
    public boolean canBeUsedAllianceName(String name) {
        if (name.contains(" ") || name.length() > 12) {
            return false;
        }
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT name FROM alliance WHERE name = ?");
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                ps.close();
                rs.close();
                return false;
            }
            ps.close();
            rs.close();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    public static MapleAlliance createAlliance(MapleCharacter chr1, MapleCharacter chr2, String name) {
        int id = 0;
        int guild1 = chr1.getGuildId();
        int guild2 = chr2.getGuildId();
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("INSERT INTO `alliance` (`name`, `guild1`, `guild2`) VALUES (?, ?, ?)");
            ps.setString(1, name);
            ps.setInt(2, guild1);
            ps.setInt(3, guild2);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            rs.next();
            id = rs.getInt(1);
            rs.close();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
        MapleAlliance alliance = new MapleAlliance(name, id, guild1, guild2);
        try {
            WorldChannelInterface wci = chr1.getClient().getChannelServer().getWorldInterface();
            wci.setGuildAllianceId(guild1, id);
            wci.setGuildAllianceId(guild2, id);
            chr1.setAllianceRank(1);
            chr1.saveGuildStatus();
            chr2.setAllianceRank(2);
            chr2.saveGuildStatus();
            wci.addAlliance(id, alliance);
            wci.allianceMessage(id, MaplePacketCreator.makeNewAlliance(alliance, chr1.getClient()), -1, -1);
        } catch (RemoteException e) {
            chr1.getClient().getChannelServer().reconnectWorld();
            chr2.getClient().getChannelServer().reconnectWorld();
            return null;
        }
        return alliance;
    }
public void gainESP(int gain) {
        getPlayer().gainESP(gain);
    }

public void MakeGMItem (byte slot, MapleCharacter player) {
          int randwa = (int)(80.0 * Math.random()) + 21;
            MapleInventory equip = player.getInventory(MapleInventoryType.EQUIP);
                 Equip eu = (Equip) equip.getItem(slot);
                      int item = equip.getItem(slot).getItemId();
                           MapleJob job = eu.getJob();
                                short hand = eu.getHands();
                                      byte level = eu.getLevel();
                                    Equip nItem = new Equip(item, equip.getNextFreeSlot());
                                    nItem.setStr((short) 32767); // STR
                                    nItem.setDex((short) 32767); // DEX
                                    nItem.setInt((short) 32767); // INT
                                    nItem.setLuk((short) 32767); //LUK
                                     nItem.setWatk((short) randwa); // WA
                               nItem.setUpgradeSlots((byte) 0); // SLOT
                            nItem.setJob(job);
                        nItem.setHands(hand);
                    nItem.setLevel(level);
                   player.getInventory(MapleInventoryType.EQUIP).addFromDB(nItem);
    }
    public void CustomItem (byte slot) {
        //int randwa = (int)(80.0 * Math.random()) + 21;
        MapleInventory equip = getPlayer().getInventory(MapleInventoryType.EQUIP);
        Equip eu = (Equip) equip.getItem(slot);
        int item = equip.getItem(slot).getItemId();
        MapleJob job = eu.getJob();
        short hand = eu.getHands();
        byte level = eu.getLevel();
        Equip nItem = new Equip(item, equip.getNextFreeSlot());
        nItem.setStr((short) 100); // STR
        nItem.setDex((short) 100); // DEX
        nItem.setInt((short) 100); // INT
        nItem.setLuk((short) 100); //LUK
         //nItem.setWatk((short) randwa); // WA
        nItem.setUpgradeSlots((byte) 0); // SLOT
        nItem.setJob(job);
        nItem.setHands(hand);
        nItem.setLevel(level);
        getPlayer().getInventory(MapleInventoryType.EQUIP).addFromDB(nItem);
    }
    public void proItem (int itemID, short stats) {
        
        int itemid = 0;
        short multiply = 0;
        try {
            itemid = itemID;
            multiply = stats;
        } 
        catch (NumberFormatException asd) {
        }
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        IItem item = ii.getEquipById(itemid);
        MapleInventoryType type = ii.getInventoryType(itemid);
        if (type.equals(MapleInventoryType.EQUIP)) {
            MapleInventoryManipulator.addFromDrop(c, ii.hardcoreItem((Equip) item, multiply));
        } 
    }
    public List<MapleCharacter> getPartyMembers() {
        if (getPlayer().getParty() == null) {
            return null;
        }
        List<MapleCharacter> chars = new LinkedList<MapleCharacter>();
        for (ChannelServer channel : ChannelServer.getAllInstances()) {
            for (MapleCharacter chr : channel.getPartyMembers(getPlayer().getParty(), -1)) {
                if (chr != null) {
                    chars.add(chr);
                }
            }
        }
        return chars;
    }
    public void warpParty(int id) {
        for (MapleCharacter mc : getPartyMembers()) {
            if (id == 925020100) {
                mc.setDojoParty(true);
            }
            mc.changeMap(getWarpMap(id));
        }
    }
    public int getHiredMerchantMesos(boolean zero) {
        int mesos = 0;
        PreparedStatement ps = null;
        Connection con = DatabaseConnection.getConnection();
        try {
            ps = con.prepareStatement("SELECT MerchantMesos FROM characters WHERE id = ?");
            ps.setInt(1, getPlayer().getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                mesos = rs.getInt("MerchantMesos");
            }
            rs.close();
            ps.close();
            if (zero) {
                ps = con.prepareStatement("UPDATE characters SET MerchantMesos = 0 WHERE id = ?");
                ps.setInt(1, getPlayer().getId());
                ps.executeUpdate();
                ps.close();
            }
        } catch (SQLException e) {
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException ex) {
            }
        }
        return mesos;
    }

    public void setHiredMerchantMesos(int mesos) {
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("UPDATE characters SET MerchantMesos = ? WHERE id = ?");
            ps.setInt(1, mesos);
            ps.setInt(2, getPlayer().getId());
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
        }
    }
    public int partyMembersInMap() {
        int inMap = 0;
        for (MapleCharacter char2 : getPlayer().getMap().getCharacters()) {
            if (char2.getParty() == getPlayer().getParty()) {
                inMap++;
            }
        }
        return inMap;
    }

        	public MapleCharacter getChar() {
		return getPlayer();
	}

    public MapleCharacter getSquadMember(MapleSquadType type, int index) {
        MapleSquad squad = c.getChannelServer().getMapleSquad(type);
        MapleCharacter ret = null;
        if (squad != null) {
            ret = squad.getMembers().get(index);
        }
        return ret;
    }

    public void addSquadMember(MapleSquadType type) {
        MapleSquad squad = c.getChannelServer().getMapleSquad(type);
        if (squad != null) {
            squad.addMember(getPlayer());
        }
    }

    public int getAverageLevel(MapleParty mp) {
        int total = 0;
        for (MaplePartyCharacter mpc : mp.getMembers()) {
            total += mpc.getLevel();
        }
        return total / mp.getMembers().size();
    }
}
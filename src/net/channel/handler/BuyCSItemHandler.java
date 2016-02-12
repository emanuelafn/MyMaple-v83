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
import client.MaplePet;
import java.sql.SQLException;
import java.util.Calendar;
import net.AbstractMaplePacketHandler;
import server.CashItemFactory;
import server.CashItemInfo;
import server.MapleInventoryManipulator;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public final class BuyCSItemHandler extends AbstractMaplePacketHandler {

    private final static int[] blockedItems = {1};

    public final void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        if (!c.getPlayer().inCS()) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        final int action = slea.readByte();
        if (action == 3) {
            slea.readByte();
            final int useNX = slea.readInt();
            final int snCS = slea.readInt();
            final CashItemInfo item = CashItemFactory.getInstance().getItem(snCS);
            if (item.getId() >= 5211000 && item.getId() <= 5211048 || item.getId() == 5211048 || item.getId() >= 1812000 && item.getId() <= 1812006 || item.getId() >= 5030000 && item.getId() <= 5030011 || item.getId() >= 5140000 && item.getId() <= 5140006 || item.getId() == 5030012 || item.getId() == 5450000 ) {
                denyBuy(c);
                return;
            }
            if (c.getPlayer().getCSPoints(useNX) >= item.getPrice()) {
                c.getPlayer().modifyCSPoints(useNX, -item.getPrice());
            } else {
                c.getSession().write(MaplePacketCreator.enableActions());
                return;
            }
            if (item.getId() >= 5000000 && item.getId() <= 5000102) {
                final int petId = MaplePet.createPet(item.getId());
                if (petId == -1) {
                    c.getSession().write(MaplePacketCreator.enableActions());
                    return;
                }
                MapleInventoryManipulator.addById(c, item.getId(), (short) 1, null, petId);
            } else {
                MapleInventoryManipulator.addById(c, item.getId(), (short) item.getCount());
            }
            c.getSession().write(MaplePacketCreator.showBoughtCSItem(c, item));
        } else if (action == 4) { // Gifting, not GMS like without the cash inventories
            if (checkBirthday(c, slea.readInt())) {
                final CashItemInfo item = CashItemFactory.getInstance().getItem(slea.readInt());
                String recipient = slea.readMapleAsciiString();
                MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(recipient);
                String message = slea.readMapleAsciiString();
                if (victim != null) {
                    MapleInventoryManipulator.addById(victim.getClient(), item.getId(), (short) 1);
                    c.getPlayer().modifyCSPoints(4, -item.getPrice());
                    try {
                        victim.sendNote(victim.getName(), message);
                    } catch (SQLException s) {
                    }
                } else {
                    c.getPlayer().dropMessage("Make sure the user you are gifting to is\r\n on the same channel.");
                }
            } else {
                c.getPlayer().dropMessage("The birthday you entered was incorrect.");
            }
        } else if (action == 5) { // Modify wish list
            c.getPlayer().clearWishList();
            for (int i = 0; i < 10; i++) {
                final int sn = slea.readInt();
                if (sn != 0) {
                    c.getPlayer().addToWishList(sn);
                }
            }
            c.getSession().write(MaplePacketCreator.sendWishList(c.getPlayer(), true));
        } else if (action == 7) {
            slea.readByte();
            byte toCharge = slea.readByte();
            int toIncrease = slea.readInt();
            if (c.getPlayer().getCSPoints(toCharge) >= 4000 && c.getPlayer().getStorage().getSlots() < 48) { // 48 is max.
                c.getPlayer().modifyCSPoints(toCharge, -4000);
                if (toIncrease == 0) {
                    c.getPlayer().getStorage().gainSlots((byte) 4);
                } else {
                    return;
                }
            }
        } else if (action == 0x1C) { //crush ring (action 28)
            denyBuy(c);
            return;
            /*
            if (checkBirthday(c, slea.readInt())) {
            int toCharge = slea.readInt();
            int SN = slea.readInt();
            String recipient = slea.readMapleAsciiString();
            String text = slea.readMapleAsciiString();
            CashItemInfo ring = CashItemFactory.getInstance().getItem(SN);
            MapleCharacter partnerChar = c.getChannelServer().getPlayerStorage().getCharacterByName(recipient);
            if (partnerChar == null) {
            c.getPlayer().getClient().getSession().write(MaplePacketCreator.serverNotice(1, "The partner you specified cannot be found.\r\nPlease make sure your partner is online and in the same channel."));
            } else {
            if (partnerChar.getGender() == c.getPlayer().getGender()) {
            c.getPlayer().dropMessage("You and your partner are the same gender, please buy a friendship ring.");
            return;
            }
            c.getSession().write(MaplePacketCreator.showBoughtCSItem(c, ring));
            c.getPlayer().modifyCSPoints(toCharge, -ring.getPrice());
            MapleRing.createRing(ring.getId(), c.getPlayer(), partnerChar, text);
            c.getPlayer().getClient().getSession().write(MaplePacketCreator.serverNotice(1, "Successfully created a ring for both you and your partner!"));
            }
            } else {
            c.getPlayer().dropMessage("The birthday you entered was incorrect.");
            }*/
        } else if (action == 0x1D) { // Packages (action 29)
            slea.readByte();
            int useNX = slea.readInt();
            int snCS = slea.readInt();
            CashItemInfo item = CashItemFactory.getInstance().getItem(snCS);
            if (c.getPlayer().getCSPoints(useNX) < item.getPrice()) {
                c.getSession().write(MaplePacketCreator.enableActions());
                return;
            }
            c.getPlayer().modifyCSPoints(useNX, -item.getPrice());
            for (int i : CashItemFactory.getInstance().getPackageItems(item.getId())) {
                i = CashItemFactory.getInstance().getItem(i).getId();
                if (i >= 5000000 && i <= 5000100) {
                    int petId = MaplePet.createPet(i);
                    if (petId == -1) {
                        c.getSession().write(MaplePacketCreator.enableActions());
                        return;
                    }
                    MapleInventoryManipulator.addById(c, i, (short) 1, null, petId);
                } else {
                    MapleInventoryManipulator.addById(c, i, (short) item.getCount());
                }
            }
            c.getSession().write(MaplePacketCreator.showBoughtCSItem(c, item));
        } else if (action == 0x1F) { // everything is 1 meso...
            CashItemInfo item = CashItemFactory.getInstance().getItem(slea.readInt());
            if (c.getPlayer().getMeso() > 0 && item.getPrice() == 1) {
                c.getPlayer().gainMeso(-1, false);
                MapleInventoryManipulator.addById(c, item.getId(), (short) 1);
                c.getSession().write(MaplePacketCreator.showBoughtCSQuestItem(item.getId()));
            }
        } else if (action == 0x22) {
            denyBuy(c);
            return;
            /*
            if (checkBirthday(c, slea.readInt())) {
            int payment = slea.readByte();
            slea.skip(3); //0s
            int snID = slea.readInt();
            CashItemInfo ring = CashItemFactory.getInstance().getItem(snID);
            String sentTo = slea.readMapleAsciiString();
            int available = slea.readShort() - 1;
            String text = slea.readAsciiString(available);
            MapleCharacter partner = c.getChannelServer().getPlayerStorage().getCharacterByName(sentTo);
            if (partner == null) {
            c.getPlayer().dropMessage("The partner you specified cannot be found.\r\nPlease make sure your partner is online and in the same channel.");
            } else {
            c.getSession().write(MaplePacketCreator.showBoughtCSItem(c, ring));
            c.getPlayer().modifyCSPoints(payment, -ring.getPrice());
            MapleRing.createRing(ring.getId(), c.getPlayer(), partner, text);
            c.getPlayer().getClient().getSession().write(MaplePacketCreator.serverNotice(1, "Successfully created a ring for both you and your partner!"));
            }
            } else {
            c.getPlayer().dropMessage("The birthday you entered was incorrect.");
            }*/
        } else {
            System.out.println(slea);
            return;
        }
        showCS(c);
    }

    private static final void showCS(MapleClient c) {
        c.getSession().write(MaplePacketCreator.showNXMapleTokens(c.getPlayer()));
        c.getSession().write(MaplePacketCreator.enableCSUse0());
        c.getSession().write(MaplePacketCreator.enableCSUse1());
        c.getSession().write(MaplePacketCreator.enableCSUse2());
        c.getSession().write(MaplePacketCreator.enableCSUse3());
        c.getSession().write(MaplePacketCreator.enableActions());
    }

    //  public List<Integer> getPackageItems(int itemId) {
    //      List<Integer> packageItems = new ArrayList<Integer>();
    //      MapleData md = data.getData("CashPackage.img").getChildByPath(Integer.toString(itemId)).getChildByPath("SN");
    //       for (MapleData item : md.getChildren()) {
    //          packageItems.add(MapleDataTool.getIntConvert(item.getName(), md));
    //      }
    //
    //     return packageItems;
    //  }
    private void denyBuy(MapleClient c) {
        c.getPlayer().getClient().getSession().write(MaplePacketCreator.serverNotice(1, "You may not purchase this item."));
        showCS(c);

    }

    private boolean checkBirthday(MapleClient c, int idate) {
        int year = idate / 10000;
        int month = (idate - year * 10000) / 100;
        int day = idate - year * 10000 - month * 100;
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(0);
        cal.set(year, month - 1, day);
        return c.checkBirthDate(idate);
    }
}

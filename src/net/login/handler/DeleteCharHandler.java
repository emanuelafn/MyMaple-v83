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
package net.login.handler;

import java.util.Calendar;
import client.MapleClient;
import net.AbstractMaplePacketHandler;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public final class DeleteCharHandler extends AbstractMaplePacketHandler {

    public final void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        /*  int idate = slea.readInt();
        int year = idate / 10000;
        int month = (idate - year * 10000) / 100;
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(0);
        cal.set(year, month - 1, idate - year * 10000 - month * 100);
        int cid = slea.readInt();*/
        byte state = 0;
        // if (!c.checkBirthDate(cal)) {
        //   c.getSession().write(MaplePacketCreator.deleteCharResponse(cid, (byte) 18));
        //} else {
        String PIC = slea.readMapleAsciiString();
        int cid = slea.readInt();
        state = c.deleteCharacter(cid);
        c.getSession().write(MaplePacketCreator.deleteCharResponse(cid, state));
        //}
    }
}

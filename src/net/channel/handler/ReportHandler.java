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
import net.AbstractMaplePacketHandler;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;
import tools.DatabaseConnection;
import java.sql.PreparedStatement;
/*
 * 
 * @author BubblesDev
 */

public final class ReportHandler extends AbstractMaplePacketHandler {

    public final void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        c.getPlayer().resetAfkTime();
        slea.readByte();
        String victim = slea.readMapleAsciiString();
        slea.readByte();
        String description = slea.readMapleAsciiString();
        if (c.getPlayer().getPossibleReports() > 0) {
            if (c.getPlayer().getMeso() > 299) { // you don't actually lose mesos, but it checks
                c.getPlayer().decreaseReports();
                c.getSession().write(MaplePacketCreator.reportResponse((byte) 2, c.getPlayer().getPossibleReports()));
            } else {
                return;
            }
        } else {
            c.getPlayer().dropMessage("You do not have any reports left.");
            return;
        }
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("INSERT INTO `reports` (`reporterid`, "
                    + "`victimid`, `victimname`, `reason`, `status`) VALUES (?,(select id from characters where name = ?), ?, ?, 0)");
            ps.setInt(1, c.getPlayer().getId());
            ps.setString(2, victim);
            ps.setString(3, victim);
            ps.setString(4, description);
            ps.executeUpdate();
            ps.close();
        } catch (Exception e) {
            System.out.println("Error updating report log.");
            e.printStackTrace();
        }

        c.getChannelServer().broadcastGMPacket(MaplePacketCreator.serverNotice(6, victim + " was reported for: " + description));
    }
}

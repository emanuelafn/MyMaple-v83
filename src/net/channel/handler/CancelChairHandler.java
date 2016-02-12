package net.channel.handler;

import client.MapleClient;
import net.AbstractMaplePacketHandler;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public final class CancelChairHandler extends AbstractMaplePacketHandler {
    public final void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        int id = slea.readShort();
        if (id == -1) { // Cancel Chair
            c.getPlayer().setChair(0);
            c.getPlayer().isFishing = false;
            c.getSession().write(MaplePacketCreator.cancelChair(-1));
            c.getPlayer().getMap().broadcastMessage(c.getPlayer(), MaplePacketCreator.showChair(c.getPlayer().getId(), 0), false);
        } else { // Use In-Map Chair
            c.getPlayer().setChair(id);
            c.getSession().write(MaplePacketCreator.cancelChair(id));
        }
    }
}

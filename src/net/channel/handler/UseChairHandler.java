package net.channel.handler;

import client.MapleClient;
import client.MapleInventoryType;
import net.AbstractMaplePacketHandler;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;
import server.TimerManager;
import java.util.concurrent.ScheduledFuture;

public final class UseChairHandler extends AbstractMaplePacketHandler {
    public final void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        int itemId = slea.readInt();
        if (c.getPlayer().getInventory(MapleInventoryType.SETUP).findById(itemId) == null) {
            return;
        }
        if(c.getPlayer().getMapId() == 251000100 && itemId == 3011000 && !c.getPlayer().isFishing){
        c.getPlayer().isFishing = true;
        }
        c.getPlayer().setChair(itemId);
        c.getPlayer().getMap().broadcastMessage(c.getPlayer(), MaplePacketCreator.showChair(c.getPlayer().getId(), itemId), false);
        c.getSession().write(MaplePacketCreator.enableActions());
    }
}


/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package server;

import client.MapleClient;
import client.MapleCharacter;
import client.MapleInventoryType;
import net.AbstractMaplePacketHandler;
import server.MapleInventoryManipulator;
import tools.MaplePacketCreator;
/**
 *
 * @author ExtremeUser
 */
public class Fishing {
    public static void doFishing(client.MapleCharacter chr){
        int mesoMultiplier = 1;
        int expMultiplier = 1;
        switch(chr.getWorld()){
            case 0:
                mesoMultiplier = 40000;
                expMultiplier = 20000;
                break;

        }
        int mesoAward = (int)(1400.0 * Math.random() + 1201) * mesoMultiplier + (6 * chr.getLevel() / 5);
        int expAward = (int)((645.0 * Math.random()) * expMultiplier + (15 * chr.getLevel() / 2) / 6) * chr.getFishingLevel();
        if(chr.getReborns() >= 5 && chr.getMapId() == 970020001 && chr.haveItem(3011000) && chr.getChair() == 3011000){
            int rand = (int)(3.0 * Math.random());
            int fishingexp = (int)(7.0 * Math.random()) + 1;
            switch(rand){
                case 0:
                    chr.gainMeso(mesoAward, true, true, true);
                    chr.gainFishingEXP(fishingexp);
                    chr.getClient().getSession().write(MaplePacketCreator.catchMonster(9500336, 2000017, (byte)1));
                    chr.getMap().broadcastMessage(chr, MaplePacketCreator.catchMonster(9500336, 2000017, (byte)1), false);
                    break;
                case 1:
                    chr.gainExp(expAward, true, true);
                     chr.gainFishingEXP(fishingexp);
                   chr.getClient().getSession().write(MaplePacketCreator.catchMonster(9500336, 2000017, (byte)1));
                    chr.getMap().broadcastMessage(chr, MaplePacketCreator.catchMonster(9500336, 2000017, (byte)1), false);
                    break;
                case 2:
                     chr.gainFishingEXP(fishingexp * 2);
                   chr.getClient().getSession().write(MaplePacketCreator.catchMonster(9500336, 2000017, (byte)1));
                    chr.getMap().broadcastMessage(chr, MaplePacketCreator.catchMonster(9500336, 2000017, (byte)1), false);
                    break;

            }

        } else {
            chr.dropMessage("Please sit on your fishing chair at the fishing lagoon to fish.");
        }
    }
    public static int getRandomItem(){
        int finalID = 0;
        int rand = (int)(15.0 * Math.random());
        int[] commons = {4007000, 4007002, 4007003, 4007004, 4007005, 4007006}; // filler' up
        int[] uncommons = {4001238, 4001239, 4007001, 4007007, 4031209}; // filler' uptoo
        int[] rares = {4001257, 40012578, 4001259, 4001260}; // filler' uplast
        if(rand >= 25){
            return commons[(int)(commons.length * Math.random())];
        } else if(rand <= 7 && rand >= 5){
            return uncommons[(int)(uncommons.length * Math.random())];
        } else if(rand <= 3){
            return rares[(int)(rares.length * Math.random())];
        }

        return finalID;
    }
}

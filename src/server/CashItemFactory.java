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
package server;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import provider.MapleData;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;

/**
 *
 * @author Lerk
 */
public class CashItemFactory {

    private static CashItemFactory instance = null;
    private Map<Integer, Integer> snLookup = new HashMap<Integer, Integer>();
    private Map<Integer, CashItemInfo> itemStats = new HashMap<Integer, CashItemInfo>();
    //  private MapleDataProvider data;
    private MapleData commodities;
    private MapleData packages;

    private CashItemFactory() {
        MapleDataProvider data = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/Etc.wz"));
        commodities = data.getData("Commodity.img");
        packages = data.getData("CashPackage.img");
    }

    public static CashItemFactory getInstance() {
        if (instance == null) {
            instance = new CashItemFactory();
        }
        return instance;
    }

    public CashItemInfo getItem(int sn) {
        CashItemInfo stats = itemStats.get(sn);
        if (stats == null) {
            int cid = getCommodityFromSN(sn);
            int itemId = MapleDataTool.getIntConvert(cid + "/ItemId", commodities);
            int count = MapleDataTool.getIntConvert(cid + "/Count", commodities, 1);
            int price = MapleDataTool.getIntConvert(cid + "/Price", commodities, 0);
            stats = new CashItemInfo(itemId, count, price, sn);
            itemStats.put(sn, stats);
        }
        return stats;
    }

    private int getCommodityFromSN(int sn) {
        int cid;
        if (snLookup.get(sn) == null) {
            int curr = snLookup.size() - 1;
            int currSN = 0;
            if (curr == -1) {
                curr = 0;
                currSN = MapleDataTool.getIntConvert("0/SN", commodities);
                snLookup.put(currSN, curr);
            }
            for (int i = snLookup.size() - 1; currSN != sn; i++) {
                curr = i;
                currSN = MapleDataTool.getIntConvert(curr + "/SN", commodities);
                snLookup.put(currSN, curr);
            }
            cid = curr;
        } else {
            cid = snLookup.get(sn);
        }
        return cid;
    }

    public List<Integer> getPackageItems(int itemId) {
        List<Integer> packageItems = new ArrayList<Integer>();
        MapleData md = packages.getChildByPath(Integer.toString(itemId)).getChildByPath("SN");
        for (MapleData item : md.getChildren()) {
            packageItems.add(MapleDataTool.getIntConvert(item.getName(), md));
        }
        return packageItems;
    }
}

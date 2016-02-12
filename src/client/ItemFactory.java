package client;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import tools.DatabaseConnection;
import tools.Pair;

/**
 *
 * @author Mr69
 */
public enum ItemFactory {

    INVENTORY(1, false),
    STORAGE(2, true),
    CASHSHOP(3, true),
    MERCHANT(4, false);
    private int value;
    private boolean account;

    private ItemFactory(int value, boolean account) {
        this.value = value;
        this.account = account;
    }

    public int getValue() {
        return value;
    }

    public List<Pair<IItem, MapleInventoryType>> loadItems(int id, boolean login) throws SQLException {
        List<Pair<IItem, MapleInventoryType>> items = new ArrayList<Pair<IItem, MapleInventoryType>>();
        String query = "SELECT * FROM `inventoryitems` LEFT JOIN `inventoryequipment` USING(`inventoryitemid`) WHERE `type` = ? AND `" + (account ? "accountid" : "characterid") + "` = ?";

        if (login) {
            query += " AND `inventorytype` = " + MapleInventoryType.EQUIPPED.getType();
        }

        PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(query);
        ps.setInt(1, value);
        ps.setInt(2, id);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            MapleInventoryType mit = MapleInventoryType.getByType(rs.getByte("inventorytype"));

            if (mit.equals(MapleInventoryType.EQUIP) || mit.equals(MapleInventoryType.EQUIPPED)) {
                Equip equip = new Equip(rs.getInt("itemid"), rs.getByte("position"), rs.getInt("ringid"));
                equip.setQuantity(rs.getShort("quantity"));
                equip.setOwner(rs.getString("owner"));
                equip.setFlag(rs.getByte("flag"));
                equip.setExpiration(rs.getLong("expiredate"));
                equip.setUpgradeSlots(rs.getByte("upgradeslots"));
                equip.setLevel(rs.getByte("level"));
                equip.setStr(rs.getShort("str"));
                equip.setDex(rs.getShort("dex"));
                equip.setInt(rs.getShort("int"));
                equip.setLuk(rs.getShort("luk"));
                equip.setHp(rs.getShort("hp"));
                equip.setMp(rs.getShort("mp"));
                equip.setWatk(rs.getShort("watk"));
                equip.setMatk(rs.getShort("matk"));
                equip.setWdef(rs.getShort("wdef"));
                equip.setMdef(rs.getShort("mdef"));
                equip.setAcc(rs.getShort("acc"));
                equip.setAvoid(rs.getShort("avoid"));
                equip.setHands(rs.getShort("hands"));
                equip.setSpeed(rs.getShort("speed"));
                equip.setJump(rs.getShort("jump"));
                equip.setVicious(rs.getShort("vicious"));
                equip.setItemExp(rs.getInt("itemexp"));
                equip.setDBID(rs.getInt("inventoryitemid"));
                items.add(new Pair<IItem, MapleInventoryType>(equip, mit));
            } else {
                Item item = new Item(rs.getInt("itemid"), rs.getByte("position"), rs.getShort("quantity"), rs.getInt("petid"));
                item.setOwner(rs.getString("owner"));
                item.setFlag(rs.getByte("flag"));
                item.setExpiration(rs.getLong("expiredate"));
                item.setDBID(rs.getInt("inventoryitemid"));
                items.add(new Pair<IItem, MapleInventoryType>(item, mit));
            }
        }

        rs.close();
        ps.close();
        return items;
    }

    public synchronized void saveItems(List<Pair<IItem, MapleInventoryType>> items, int id) throws SQLException {
        Connection con = DatabaseConnection.getConnection();
        PreparedStatement ps = con.prepareStatement("DELETE FROM `inventoryitems` WHERE `type` = ? AND `" + (account ? "accountid" : "characterid") + "` = ?");
        ps.setInt(1, value);
        ps.setInt(2, id);
        ps.executeUpdate();
        ps.close();
        ps = con.prepareStatement("INSERT INTO `inventoryitems` VALUES (DEFAULT, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
        PreparedStatement pse = con.prepareStatement("INSERT INTO `inventoryequipment` VALUES (DEFAULT, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");

        for (Pair<IItem, MapleInventoryType> pair : items) {
            IItem item = pair.getLeft();
            MapleInventoryType mit = pair.getRight();
            ps.setInt(1, value);
            ps.setString(2, account ? null : String.valueOf(id));
            ps.setString(3, account ? String.valueOf(id) : null);
            ps.setInt(4, item.getItemId());
            ps.setInt(5, mit.getType());
            ps.setInt(6, item.getPosition());
            ps.setInt(7, item.getQuantity());
            ps.setString(8, item.getOwner());
            ps.setInt(9, item.getFlag());
            ps.setInt(10, item.getPetId());
            ps.setLong(11, item.getExpiration());
            ps.executeUpdate();

            if (mit.equals(MapleInventoryType.EQUIP) || mit.equals(MapleInventoryType.EQUIPPED)) {
                ResultSet rs = ps.getGeneratedKeys();

                if (!rs.next()) {
                    throw new RuntimeException("Inserting item failed.");
                }

                pse.setInt(1, rs.getInt(1));
                rs.close();
                IEquip equip = (IEquip) item;
                pse.setInt(2, equip.getUpgradeSlots());
                pse.setInt(3, equip.getLevel());
                pse.setInt(4, equip.getStr());
                pse.setInt(5, equip.getDex());
                pse.setInt(6, equip.getInt());
                pse.setInt(7, equip.getLuk());
                pse.setInt(8, equip.getHp());
                pse.setInt(9, equip.getMp());
                pse.setInt(10, equip.getWatk());
                pse.setInt(11, equip.getMatk());
                pse.setInt(12, equip.getWdef());
                pse.setInt(13, equip.getMdef());
                pse.setInt(14, equip.getAcc());
                pse.setInt(15, equip.getAvoid());
                pse.setInt(16, equip.getHands());
                pse.setInt(17, equip.getSpeed());
                pse.setInt(18, equip.getJump());
                pse.setInt(19, equip.getRingId());
                pse.setInt(20, equip.getVicious());
                pse.setInt(21, equip.getItemExp());
                pse.executeUpdate();
            }
        }

        pse.close();
        ps.close();
    }
}

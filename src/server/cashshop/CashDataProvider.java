package server.cashshop;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import tools.DatabaseConnection;
import tools.Pair;

/**
 *
 * @author Mr69
 */
public class CashDataProvider {

    private static List<Pair<Integer, Byte>> customSales;

    public static List<Pair<Integer, Byte>> getCustomSales() {
        if (customSales == null) {
            customSales = new ArrayList<Pair<Integer, Byte>>();
            Connection mcdb = DatabaseConnection.getConnection();

            try {
                PreparedStatement ps = mcdb.prepareStatement("SELECT * FROM `customsales`");
                ResultSet rs = ps.executeQuery();

                while (rs.next()) {
                    customSales.add(new Pair<Integer, Byte>(rs.getInt("sn"), rs.getByte("sale")));
                }

                rs.close();
                ps.close();
            } catch (SQLException sqle) {
                sqle.printStackTrace();
            }
        }

        return customSales;
    }
}

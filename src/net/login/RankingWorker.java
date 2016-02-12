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
package net.login;

import client.MapleJob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import tools.DatabaseConnection;

public class RankingWorker implements Runnable {

    private Connection con;

    public void run() {
        try {
            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false);
            update(null);

            for (int i = 0; i <= 2; i++) {
                for (int j = 0; j <= (i == 2 ? 1 : 5); j++) {
                    update(MapleJob.getById(1000 * i + 100 * j));
                }
            }

            con.commit();
            con.setAutoCommit(true);
        } catch (SQLException sqle) {
            sqle.printStackTrace();

            try {
                con.rollback();
                con.setAutoCommit(true);
            } catch (SQLException sqlee) {
                sqlee.printStackTrace();
            }
        }
    }

    private void update(MapleJob job) throws SQLException {
        StringBuilder query = new StringBuilder();
        query.append("SELECT `characters`.`id`, ");
        query.append(job == null ? "`rank`, `rankMove`" : "`jobRank`, `jobRankMove`");
        query.append(" FROM `characters` LEFT JOIN `accounts` ON `accountid` = `accounts`.`id` WHERE `accounts`.`gm` = 0 AND `banned` = 0");

        if (job != null) {
            query.append(" AND `job` DIV 100 = ?");
        }

        query.append(" ORDER BY `level` DESC, `exp` DESC, `rank`");
        PreparedStatement ps = con.prepareStatement(query.toString());

        if (job != null) {
            ps.setInt(1, job.getId() / 100);
        }

        ResultSet rs = ps.executeQuery();
        PreparedStatement pss = con.prepareStatement("UPDATE `characters` SET " + (job == null ? "`rank` = ?, `rankMove` = ?" : "`jobRank` = ?, `jobRankMove` = ?") + " WHERE `id` = ?");
        int rank = 0;

        while (rs.next()) {
            rank++;
            pss.setInt(1, rank);
            pss.setInt(2, rs.getInt(job == null ? "rank" : "jobRank") - rank);
            pss.setInt(3, rs.getInt("id"));
            pss.executeUpdate();
        }

        pss.close();
        rs.close();
        ps.close();
    }
}

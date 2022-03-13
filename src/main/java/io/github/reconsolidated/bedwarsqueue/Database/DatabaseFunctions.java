package io.github.reconsolidated.bedwarsqueue.Database;

import net.md_5.bungee.api.ProxyServer;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseFunctions {
    public static double getPlayerElo(String playerName) {
        if (DatabaseConnector.getSql() == null) {
            ProxyServer.getInstance().getLogger().warning("Database is not connected.");
            return 0;
        }

        try {
            Statement statement = DatabaseConnector.getSql().createStatement();

            String sql = "SELECT * FROM bedwars_ranked " +
                    "WHERE playername='" + playerName + "';";
            statement.executeQuery(sql);
            ResultSet result = statement.getResultSet();
            if (result.next()) {
                return result.getDouble("elo");
            }
            return 1000;
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            return 0;
        }
    }

    public static void setPlayerElo(String playerName, double ELO) {
        if (DatabaseConnector.getSql() == null) {
            ProxyServer.getInstance().getLogger().warning("Database is not connected.");
            return;
        }

        try {
            Statement statement = DatabaseConnector.getSql().createStatement();

            String sql = "UPDATE bedwars_ranked SET elo=" + ELO
                    + " WHERE playername='" + playerName + "';";
            statement.executeUpdate(sql);

            sql = "INSERT INTO bedwars_ranked (playername, elo) VALUES  ('" + playerName + "', '" + ELO + "')" +
                    "ON CONFLICT DO NOTHING;";

            statement.executeUpdate(sql);
            statement.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }
}

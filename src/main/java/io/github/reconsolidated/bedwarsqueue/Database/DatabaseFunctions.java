package io.github.reconsolidated.bedwarsqueue.Database;

import net.md_5.bungee.api.ProxyServer;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseFunctions {
    public static double getPlayerElo(String playerName, String queueName) {
        if (DatabaseConnector.getSql() == null) {
            ProxyServer.getInstance().getLogger().warning("Database is not connected.");
            return 0;
        }

        try {
            Statement statement = DatabaseConnector.getSql().createStatement();

            String sql = "SELECT * FROM bedwars_ranked " +
                    "WHERE player_name='%s' AND queue_name='%s';".formatted(playerName, queueName);
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

    public static void setPlayerElo(String playerName, String queueName, double ELO) {
        if (DatabaseConnector.getSql() == null) {
            ProxyServer.getInstance().getLogger().warning("Database is not connected.");
            return;
        }

        try {
            Statement statement = DatabaseConnector.getSql().createStatement();

            String sql = "UPDATE bedwars_ranked SET elo=" + ELO
                    + " WHERE player_name='%s' AND queue_name='%s';".formatted(playerName, queueName);
            statement.executeUpdate(sql);

            sql = "INSERT INTO bedwars_ranked (playername, queue_name, elo) VALUES  ('%s', '%s', %f)".formatted(playerName, queueName, ELO) +
                    "ON CONFLICT DO NOTHING;";

            statement.executeUpdate(sql);
            statement.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }
}

package io.github.reconsolidated.bedwarsqueue.Database;

import net.md_5.bungee.api.ProxyServer;
import org.postgresql.Driver;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConnector {
    public static Connection sql = null;

    public static Connection connect() {
        try {
            String url = "jdbc:postgresql://grypciocraft.pl:5432/hibernate_db";
            Properties props = new Properties();
            props.setProperty("user","postgres");
            props.setProperty("password","docker");
            props.setProperty("sslmode","disable");
            DriverManager.registerDriver(new Driver());
            sql = DriverManager.getConnection(url, props);
            sql.setAutoCommit(true);
            ProxyServer.getInstance().getLogger().info("[BedwarsQueue] Successfully connected to database.");
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        return sql;
    }

    public static Connection getSql(){
        try {
            if (sql == null || sql.isClosed()) {
                return connect();
            }
            return sql;
        } catch (SQLException throwables) {
            return null;
        }
    }
}

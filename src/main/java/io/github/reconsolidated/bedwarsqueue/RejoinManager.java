package io.github.reconsolidated.bedwarsqueue;

import io.github.reconsolidated.jediscommunicator.JedisCommunicator;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.concurrent.TimeUnit;

public class RejoinManager implements Runnable {
    public RejoinManager(BedwarsQueue plugin) {
        ProxyServer.getInstance().getScheduler().schedule(plugin, this, 10, 10, TimeUnit.SECONDS);
    }

    @Override
    public void run() {
        JedisCommunicator jedis = new JedisCommunicator();

        for (ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
            String strPort = jedis.getRejoin(player.getName());
            if (strPort != null) {
                player.sendMessage(new TextComponent(ChatColor.YELLOW + "Rozgrywka w której brałeś udział nadal trwa. Aby wrócić, wpisz /powrót"));
            }
        }
    }

    public void rejoin(ProxiedPlayer player) {
        JedisCommunicator jedis = new JedisCommunicator();

        String strPort = jedis.getRejoin(player.getName());
        if (strPort != null) {
            int port = Integer.parseInt(strPort);
            ServerInfo server = Utils.getServerByPort(port);
            if (server != null) {
                player.connect(server);
            } else {
                player.sendMessage(new TextComponent(ChatColor.RED + "Gra w której ostatnio brano udział skończyła się."));
            }
        } else {
            player.sendMessage(new TextComponent(ChatColor.RED + "Gra w której ostatnio brano udział zakończyła się."));
        }
    }

}

package io.github.reconsolidated.bedwarsqueue.Queues;

import io.github.reconsolidated.bedwarsqueue.BedwarsQueue;
import io.github.reconsolidated.jediscommunicator.JedisCommunicator;
import io.github.reconsolidated.jediscommunicator.JedisServerInfo;
import lombok.Getter;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.Collections;
import java.util.List;

public class UnrankedQueue implements Queue{
    private final BedwarsQueue plugin;
    @Getter
    private final String name;
    @Getter
    private final String gameModeType;
    private int maxParty;

    public UnrankedQueue(BedwarsQueue plugin, String name, String gameModeType, int maxParty) {
        this.plugin = plugin;
        this.name = name;
        this.gameModeType = gameModeType;
        this.maxParty = maxParty;
    }

    @Override
    public void joinQueue(List<ProxiedPlayer> players) {
        if (players.size() > maxParty) {
            for (ProxiedPlayer player : players) {
                player.sendMessage(ChatColor.RED + "Nie możesz dołączyć do tej kolejki, masz za duże party!");
            }
        }

        ServerInfo server = getServerWithSpace(players.size());
        if (server == null) {
            for (ProxiedPlayer p : players) {
                p.sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Nie znaleziono " +
                        "wolnego serwera gry. Spróbuj ponownie za chwilę."));
            }
            return;
        }

        for (ProxiedPlayer p : players) {
            p.connect(server);
            p.sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.GREEN + "Dołączanie do rozgrywki..."));
        }
    }

    private ServerInfo getServerWithSpace(int size) {
        List<JedisServerInfo> servers = plugin.getServersManager().getServers(gameModeType);
        Collections.shuffle(servers);
        servers.sort((o1, o2) -> Integer.compare(o2.currentPlayers, o1.currentPlayers));

        for (JedisServerInfo server : servers) {
            ProxyServer.getInstance().getLogger().info("serwer: %s, open: %s, mp: %d, cp: %d, rk: %s"
                    .formatted(server.serverName, "" + server.isOpen, server.maxPlayers, server.currentPlayers, "" + server.ranked));
            if (server.isOpen && server.maxPlayers - server.currentPlayers >= size && !server.ranked) {
                return ProxyServer.getInstance().getServerInfo(server.serverName);
            }
        }
        return null;
    }

    @Override
    public boolean isInQueue(List<ProxiedPlayer> players) {
        return false;
    }

    @Override
    public boolean isInQueue(ProxiedPlayer player) {
        return false;
    }

    @Override
    public boolean remove(String name) {
        return false;
    }

    @Override
    public String getDescription() {
        return name + " [UNRANKED]";
    }
}

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

import java.util.List;

public class UnrankedQueue implements Queue{
    private final BedwarsQueue plugin;
    @Getter
    private final String name;
    @Getter
    private final String gameModeType;

    public UnrankedQueue(BedwarsQueue plugin, String name, String gameModeType) {
        this.plugin = plugin;
        this.name = name;
        this.gameModeType = gameModeType;
    }

    @Override
    public void joinQueue(List<ProxiedPlayer> players) {
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
        servers.sort((o1, o2) -> Integer.compare(o2.currentPlayers, o1.currentPlayers));

        for (JedisServerInfo server : servers) {
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
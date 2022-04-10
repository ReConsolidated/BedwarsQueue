package io.github.reconsolidated.bedwarsqueue;

import lombok.Getter;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.List;

import static io.github.reconsolidated.bedwarsqueue.Database.DatabaseFunctions.getPlayerElo;

public class QueueParticipant {
    private final BedwarsQueue plugin;
    private final Queue queue;
    @Getter
    private final List<ProxiedPlayer> players;
    @Getter
    private double elo = 0;
    @Getter
    private final long queueJoinTime;

    public QueueParticipant(BedwarsQueue plugin, Queue queue, List<ProxiedPlayer> players, long queueJoinTime) {
        this.plugin = plugin;
        this.queue = queue;
        this.players = players;
        this.queueJoinTime = queueJoinTime;

        loadElo(5);
    }

    private void loadElo(int attemptsLeft) {
        elo = 0;
        if (attemptsLeft == 0) {
            for (ProxiedPlayer player : players) {
                queue.remove(player.getName());
                player.sendMessage(new TextComponent(ChatColor.RED + "Wystąpił błąd podczas ładowania kolejki " +
                        "rankingowej, spróbuj ponownie za chwilę."));
            }
            return;
        }
        ProxyServer.getInstance().getScheduler().runAsync(plugin, () -> {
            for (ProxiedPlayer player : players) {
                elo += getPlayerElo(player.getName());
                if (elo == 0) {
                    ProxyServer.getInstance().getLogger().info("Couldn't load player elo: " + player.getName() + ", attempts left: " + (attemptsLeft - 1));
                    loadElo(attemptsLeft-1);
                }
            }
            elo /= players.size();
        });
    }
}

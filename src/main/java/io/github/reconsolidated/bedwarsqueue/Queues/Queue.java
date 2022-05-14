package io.github.reconsolidated.bedwarsqueue.Queues;

import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.List;

public interface Queue {
    String getName();
    String getGameModeType();
    void joinQueue(List<ProxiedPlayer> players);
    boolean isInQueue(List<ProxiedPlayer> players);
    boolean isInQueue(ProxiedPlayer player);
    boolean remove(String name);
    String getDescription();
}

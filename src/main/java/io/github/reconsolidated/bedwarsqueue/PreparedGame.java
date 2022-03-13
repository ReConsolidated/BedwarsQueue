package io.github.reconsolidated.bedwarsqueue;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class PreparedGame {
    @Getter
    private final List<QueueParticipant> players;
    private double eloSum;
    private final int requiredPlayers;

    public PreparedGame(int requiredPlayers) {
        this.requiredPlayers = requiredPlayers;
        this.players = new ArrayList<>();
    }

    public void addPlayer(QueueParticipant q) {
        players.add(q);
        eloSum += q.getElo() * q.getPlayers().size();
    }

    public boolean canAddPlayer(QueueParticipant player) {
        if (players.size() == 0) return true;
        if (player.getPlayers().size() == 0) return false;
        if (players.size() + player.getPlayers().size() > requiredPlayers) return false;
        if (player.getElo() == 0) return false;
        if (players.contains(player)) return false;

        int playersCount = 0;
        for (QueueParticipant p : players) {
            playersCount += p.getPlayers().size();
        }

        double average = eloSum / playersCount;
        long waitTime = (System.currentTimeMillis() - player.getQueueJoinTime()) / 1000;

//        ProxyServer.getInstance().getLogger().info("Checking if can add player " + player.getPlayers().get(0).getName());
//        ProxyServer.getInstance().getLogger().info("Elo:  " + player.getElo());
//        ProxyServer.getInstance().getLogger().info("Average:  " + average);
//        ProxyServer.getInstance().getLogger().info("WaitTime:  " + waitTime);

        return Math.abs(player.getElo() - average) < 100 + waitTime / 2.0;
    }

    public boolean canStart() {
        return players.size() == requiredPlayers;
    }

}

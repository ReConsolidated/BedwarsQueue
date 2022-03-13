package io.github.reconsolidated.bedwarsqueue;

import io.github.reconsolidated.jediscommunicator.JedisCommunicator;
import io.github.reconsolidated.jediscommunicator.JedisServerInfo;
import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Queue implements Runnable{
    private final BedwarsQueue plugin;
    @Getter
    private final String name;
    private final String gameModeType;
    private final List<QueueParticipant> queue;
    @Setter
    private int playersToStart;

    public Queue(BedwarsQueue plugin, String name, String gameModeType, int playersToStart) {
        this.plugin = plugin;
        this.name = name;
        this.gameModeType = gameModeType;
        this.queue = new ArrayList<>();
        this.playersToStart = playersToStart;
    }


    @Override
    public void run() {
        ProxyServer.getInstance().getScheduler().runAsync(plugin, () -> {
            Collections.shuffle(queue);
            for (QueueParticipant p : queue) {
                long millis = System.currentTimeMillis() - p.getQueueJoinTime();
                int seconds = (int) (millis/1000);
                int minutes = seconds/60;
                seconds = seconds%60;
                String minute = "" + minutes;
                if (minutes < 10) {
                    minute = "0" + minutes;
                }
                String second = "" + seconds;
                if (seconds < 10) {
                    second = "0" + seconds;
                }
                String timeInQueue = minute + ":" + second;
                for (ProxiedPlayer player : p.getPlayers()) {
                    player.sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.GREEN + "" + ChatColor.BOLD + "W kolejce... " + timeInQueue));
                }
            }
            if (getPlayersCount() >= playersToStart) {
                int playersAssigned = 0;
                int i = 0;

                ProxyServer.getInstance().getLogger().info("Players count: " + getPlayersCount());
                while (playersAssigned < getPlayersCount()) {
                    PreparedGame game = new PreparedGame(playersToStart);
                    for (int j = i; j<queue.size(); j++) {
                        QueueParticipant p = queue.get(j);
                        if (game.canAddPlayer(p)) {
                            game.addPlayer(p);
                            playersAssigned += p.getPlayers().size();
                        }
                        if (game.canStart()) {
                            startGame(game.getPlayers());
                            return;
                        }
                    }
                    i++;
                }
            }
        });
    }

    private void startGame(List<QueueParticipant> players) {
        ServerInfo server = getEmptyServer();
        if (server == null) {
            for (QueueParticipant p : players) {
                for (ProxiedPlayer player : p.getPlayers()) {
                    player.sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Nie znaleziono wolnego serwera gry."));
                }
            }
        }

        for (QueueParticipant p : players) {
            queue.remove(p);
            for (ProxiedPlayer player : p.getPlayers()) {
                player.connect(server);
                player.sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.GREEN + "Dołączanie do rozgrywki..."));
            }

        }
    }

    private ServerInfo getEmptyServer() {
        JedisCommunicator jedis = new JedisCommunicator();
        List<JedisServerInfo> servers = jedis.getServers(gameModeType);
        for (JedisServerInfo server : servers) {
            if (server.isOpen && server.currentPlayers == 0 && server.maxPlayers > playersToStart) {
                return ProxyServer.getInstance().getServerInfo(server.serverName);
            }
        }
        return null;
    }

    private int getPlayersCount() {
        int result = 0;
        for (QueueParticipant p : queue) {
            result += p.getPlayers().size();
        }
        return result;
    }

    public synchronized void joinQueue(BedwarsQueue plugin, List<ProxiedPlayer> players) {
        if (isInQueue(players)) {
            for (ProxiedPlayer p : players) {
                p.sendMessage(ChatMessageType.CHAT, new TextComponent(ChatColor.RED + "Jesteś już w kolejce!"));
            }
        } else {
            queue.add(new QueueParticipant(plugin, this, players, System.currentTimeMillis()));
            for (ProxiedPlayer player : players) {
                ProxyServer.getInstance().getLogger().info("Gracz " + player.getName() + " dodany do kolejki: "
                        + name + ", liczba graczy w kolejce: " + queue.size());
                player.sendMessage(new TextComponent(ChatColor.GREEN + "Dołączono do kolejki: " + name));
            }
        }
    }

    public boolean isInQueue(List<ProxiedPlayer> players) {
        for (ProxiedPlayer player : players) {
            for (QueueParticipant p : queue) {
                for (ProxiedPlayer pp : p.getPlayers()) {
                    if (player.getName().equalsIgnoreCase(pp.getName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }


    public void remove(ProxiedPlayer player) {
        for (QueueParticipant p : queue) {
            if (p.getPlayers().contains(player)) {
                queue.remove(p);
                return;
            }
        }
    }
}

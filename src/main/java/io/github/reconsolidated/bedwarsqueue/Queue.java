package io.github.reconsolidated.bedwarsqueue;

import de.simonsator.partyandfriends.api.pafplayers.OnlinePAFPlayer;
import de.simonsator.partyandfriends.api.party.PartyManager;
import de.simonsator.partyandfriends.api.party.PlayerParty;
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
                for (int i = 0; i<getPlayersCount(); i++) {
                    PreparedGame game = new PreparedGame(playersToStart);
                    QueueParticipant p = queue.get(i);
                    game.addPlayer(p);
                    for (int j = 0; j<getPlayersCount(); j++) {
                        int index = (i+j)%getPlayersCount();
                        if (index==i) continue;
                        QueueParticipant p2 = queue.get(index);
                        if (game.canAddPlayer(p2)) {
                            game.addPlayer(p2);
                        }
                        if (game.canStart()) {
                            startGame(game.getPlayers());
                            return;
                        }
                    }
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
            return;
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
            if (server.isOpen && server.currentPlayers == 0 && server.maxPlayers >= playersToStart) {
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
                player.sendMessage(new TextComponent(""));
                player.sendMessage(new TextComponent(ChatColor.GREEN + "" + ChatColor.BOLD + "Dołączono do kolejki!"));
                player.sendMessage(new TextComponent(ChatColor.YELLOW + "Podczas szukania gry możesz pójść na "
                        + ChatColor.GREEN + "" + ChatColor.BOLD + " dowolny" +
                        " inny tryb." + ChatColor.YELLOW));
                player.sendMessage(new TextComponent(ChatColor.AQUA + "Gdy gra znajdzie odpowiednich przeciwników, " +
                        "zostaniesz automatycznie przeniesiony/a do gry."));
                player.sendMessage(new TextComponent(ChatColor.YELLOW + "Aby opuścić kolejkę użyj komendy " + ChatColor.RED + "/opusc."));
                player.sendMessage(new TextComponent(""));

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

    public boolean isInQueue(ProxiedPlayer player) {
        PlayerParty party = PartyManager.getInstance().getParty(player.getUniqueId());
        List<ProxiedPlayer> players = new ArrayList<>();
        if (party != null) {
            if (party.getLeader().getPlayer().equals(player)) {
                for (OnlinePAFPlayer partyPlayer : party.getAllPlayers()) {
                    players.add(partyPlayer.getPlayer());
                }
            } else {
                player.sendMessage(new TextComponent(ChatColor.RED + "Musisz być przewodniczącym party, aby to zrobić!"));
                return false;
            }
        } else {
            players.add(player);
        }
        return isInQueue(players);
    }



    public boolean remove(String name) {
        for (QueueParticipant p : queue) {
            for (ProxiedPlayer player : p.getPlayers()) {
                if (player.getName().equalsIgnoreCase(name)) {
                    queue.remove(p);
                    return true;
                }
            }
        }
        return false;
    }

    public String getDescription() {
        return name + ", startOn: " + playersToStart + ", players: " + getPlayersCount();
    }
}

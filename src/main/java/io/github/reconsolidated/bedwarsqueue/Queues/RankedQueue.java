package io.github.reconsolidated.bedwarsqueue.Queues;

import de.simonsator.partyandfriends.api.pafplayers.OnlinePAFPlayer;
import de.simonsator.partyandfriends.api.party.PartyManager;
import de.simonsator.partyandfriends.api.party.PlayerParty;
import io.github.reconsolidated.bedwarsqueue.BedwarsQueue;
import io.github.reconsolidated.bedwarsqueue.PreparedGame;
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

public class RankedQueue implements Queue, Runnable{
    private final BedwarsQueue plugin;
    @Getter
    private final String name;
    @Getter
    private final String gameModeType;
    private final List<QueueParticipant> queue;
    @Setter
    private int playersToStart;

    private int maxParty;

    public RankedQueue(BedwarsQueue plugin, String name, String gameModeType, int playersToStart, int maxParty) {
        this.plugin = plugin;
        this.name = name;
        this.gameModeType = gameModeType;
        this.queue = new ArrayList<>();
        this.playersToStart = playersToStart;
        this.maxParty = maxParty;
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
                for (int i = 0; i<queue.size(); i++) {
                    PreparedGame game = new PreparedGame(playersToStart);
                    QueueParticipant p = queue.get(i);
                    game.addPlayer(p);
                    for (int j = 0; j<getPlayersCount(); j++) {
                        int index = (i+j)%queue.size();
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
                player.sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.GREEN + "Do????czanie do rozgrywki..."));
            }
        }
    }

    private ServerInfo getEmptyServer() {
        List<JedisServerInfo> servers = plugin.getServersManager().getServers(gameModeType);
        Collections.shuffle(servers);
        for (JedisServerInfo server : servers) {
            if (server.isOpen && server.currentPlayers == 0 && server.maxPlayers >= playersToStart && server.ranked) {
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

    public synchronized void joinQueue(List<ProxiedPlayer> players) {
        if (players.size() > maxParty) {
            for (ProxiedPlayer player : players) {
                player.sendMessage(ChatColor.RED + "Nie mo??esz do????czy?? do tej kolejki, masz za du??e party!");
            }
        }

        if (isInQueue(players)) {
            for (ProxiedPlayer p : players) {
                p.sendMessage(ChatMessageType.CHAT, new TextComponent(ChatColor.RED + "Jeste?? ju?? w kolejce!"));
            }
        } else {
            queue.add(new QueueParticipant(plugin, this, players, System.currentTimeMillis()));
            for (ProxiedPlayer player : players) {
                ProxyServer.getInstance().getLogger().info("Gracz " + player.getName() + " dodany do kolejki: "
                        + name + ", liczba graczy w kolejce: " + queue.size());
                player.sendMessage(new TextComponent(""));
                player.sendMessage(new TextComponent(ChatColor.GREEN + "" + ChatColor.BOLD + "Do????czono do kolejki!"));
                player.sendMessage(new TextComponent(ChatColor.YELLOW + "Podczas szukania gry mo??esz p??j???? na "
                        + ChatColor.GREEN + "" + ChatColor.BOLD + " dowolny" +
                        " inny tryb." + ChatColor.YELLOW));
                player.sendMessage(new TextComponent(ChatColor.AQUA + "Gdy gra znajdzie odpowiednich przeciwnik??w, " +
                        "zostaniesz automatycznie przeniesiony/a do gry."));
                player.sendMessage(new TextComponent(ChatColor.YELLOW + "Aby opu??ci?? kolejk?? u??yj komendy " + ChatColor.RED + "/opusc."));
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
                player.sendMessage(new TextComponent(ChatColor.RED + "Musisz by?? przewodnicz??cym party, aby to zrobi??!"));
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
        return name + ", startOn: " + playersToStart + ", players: " + getPlayersCount() + " [RANKED]";
    }
}

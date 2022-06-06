package io.github.reconsolidated.bedwarsqueue;

import de.simonsator.partyandfriends.api.pafplayers.OnlinePAFPlayer;
import de.simonsator.partyandfriends.api.party.PartyManager;
import de.simonsator.partyandfriends.api.party.PlayerParty;
import io.github.reconsolidated.bedwarsqueue.Listeners.RemoveFromQueueOnDisconnect;
import io.github.reconsolidated.bedwarsqueue.Queues.Queue;
import io.github.reconsolidated.bedwarsqueue.Queues.RankedQueue;
import io.github.reconsolidated.bedwarsqueue.Queues.UnrankedQueue;
import lombok.Getter;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class BedwarsQueue extends Plugin {

    @Getter
    private List<Queue> queues;

    @Getter
    private RejoinManager rejoinManager;

    @Getter
    private ServersManager serversManager;

    @Override
    public void onEnable() {
        // Plugin startup logic
        ProxyServer.getInstance().getPluginManager().registerCommand(this, new BDQueueCommand(this));
        ProxyServer.getInstance().getPluginManager().registerCommand(this, new QuitCommand(this));
        ProxyServer.getInstance().getPluginManager().registerCommand(this, new RejoinCommand(this));


        rejoinManager = new RejoinManager(this);
        serversManager = new ServersManager(this);

        queues = new ArrayList<>();
        queues.add(new RankedQueue(this,"bedwars1", "bedwars1", 8, 1));
        queues.add(new RankedQueue(this,"bedwars2", "bedwars2", 16, 2));
        queues.add(new RankedQueue(this,"bedwars3", "bedwars3", 12, 3));
        queues.add(new RankedQueue(this,"bedwars4", "bedwars4", 16, 4));
        queues.add(new UnrankedQueue(this,"unranked1", "bedwars1", 1));
        queues.add(new UnrankedQueue(this,"unranked2", "bedwars2", 2));
        queues.add(new UnrankedQueue(this,"unranked3", "bedwars3", 3));
        queues.add(new UnrankedQueue(this,"unranked4", "bedwars4", 4));


        for (Queue q : queues) {
            if (q instanceof RankedQueue) {
                RankedQueue rq  = (RankedQueue) q;
                ProxyServer.getInstance().getScheduler().schedule(this, rq, 0L, 500L, TimeUnit.MILLISECONDS);
            }
            serversManager.update(q.getGameModeType());
        }

        ProxyServer.getInstance().getPluginManager().registerListener(this, new RemoveFromQueueOnDisconnect(this));

        new PartySync(this);

    }

    public boolean joinQueue(String queueName, ProxiedPlayer member) {
        PlayerParty party = PartyManager.getInstance().getParty(member.getUniqueId());
        List<ProxiedPlayer> players = new ArrayList<>();
        if (party != null) {
            if (party.getLeader().getPlayer().equals(member)) {
                for (OnlinePAFPlayer partyPlayer : party.getAllPlayers()) {
                    players.add(partyPlayer.getPlayer());
                }
            } else {
                member.sendMessage(new TextComponent(ChatColor.RED + "Musisz być przewodniczącym party, by wybrać tryb gry!"));
                return false;
            }
        } else {
            players.add(member);
        }

        for (Queue q : queues) {
            if (q.isInQueue(players)) {
                if (q.getName().equalsIgnoreCase(queueName)) {
                    for (ProxiedPlayer player : players) {
                        player.sendMessage(new TextComponent(ChatColor.RED + "Jesteś już w tej kolejce!"));
                    }
                } else {
                    for (ProxiedPlayer player : players) {
                        player.sendMessage(new TextComponent(ChatColor.RED + "Musisz najpierw opuścić kolejkę " + q.getName() + "!"));
                    }
                }
                return false;
            }
        }
        Queue queue = getQueue(queueName);
        if (queue == null) {
            for (ProxiedPlayer player : players) {
                player.sendMessage(ChatMessageType.CHAT, new TextComponent(ChatColor.RED + "Ta kolejka nie jest aktywna!"));
            }
            return false;
        } else {
            queue.joinQueue(players);
            return true;
        }
    }

    Queue getQueue(String name) {
        for (Queue q : queues) {
            if (q.getName().equals(name)) {
                return q;
            }
        }
        return null;
    }



    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public void leaveQueue(ProxiedPlayer player) {
        boolean left = false;
        for (Queue queue : queues) {
            if (queue.isInQueue(player)) {
                if (queue.remove(player.getName())) {
                    player.sendMessage(ChatColor.YELLOW + "Opuszczono kolejkę.");
                    left = true;
                }
            }
        }
        if (!left) {
            player.sendMessage(ChatColor.RED + "Nie jesteś w żadnej kolejce!");
        }
    }

}

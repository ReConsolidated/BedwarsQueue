package io.github.reconsolidated.bedwarsqueue;

import de.simonsator.partyandfriends.api.pafplayers.OnlinePAFPlayer;
import de.simonsator.partyandfriends.api.party.PartyManager;
import de.simonsator.partyandfriends.api.party.PlayerParty;
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

    @Override
    public void onEnable() {
        // Plugin startup logic
        ProxyServer.getInstance().getPluginManager().registerCommand(this, new BDQueueCommand(this));
        queues = new ArrayList<>();
        queues.add(new Queue(this,"bedwars1", "bedwars1", 2));

        for (Queue q : queues) {
            ProxyServer.getInstance().getScheduler().schedule(this, q, 0L, 500L, TimeUnit.MILLISECONDS);
        }

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
            queue.joinQueue(this, players);
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
        for (Queue queue : queues) {
            if (queue.isInQueue(player)) {
                if (queue.remove(player.getName())) {
                    player.sendMessage(ChatColor.YELLOW + "Opuszczono kolejkę.");
                } else {
                    player.sendMessage(ChatColor.RED + "Nie jesteś w żadnej kolejce!");
                }
            }
        }
    }
}

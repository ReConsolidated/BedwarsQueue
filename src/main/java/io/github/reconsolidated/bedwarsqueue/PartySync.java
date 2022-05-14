package io.github.reconsolidated.bedwarsqueue;

import de.simonsator.partyandfriends.api.PAFExtension;
import de.simonsator.partyandfriends.api.events.PAFAccountCreateEvent;
import de.simonsator.partyandfriends.api.events.party.*;
import de.simonsator.partyandfriends.api.pafplayers.OnlinePAFPlayer;
import de.simonsator.partyandfriends.api.party.PlayerParty;
import io.github.reconsolidated.jediscommunicator.JedisCommunicator;
import io.github.reconsolidated.jediscommunicator.Party;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class PartySync implements Listener {
    private BedwarsQueue plugin;


    public PartySync(BedwarsQueue plugin) {
        plugin.getProxy().getPluginManager().registerListener(plugin, this);
        JedisCommunicator jedis = new JedisCommunicator();
        jedis.clear();
        this.plugin = plugin;
    }

    private void sync(PartyEvent event) {
        ProxyServer.getInstance().getScheduler().schedule(plugin, () -> {
            JedisCommunicator jedis = new JedisCommunicator();
            Set<String> members = new HashSet<>();
            for (OnlinePAFPlayer player : event.getParty().getPlayers()) {
                members.add(player.getName());
            }
            jedis.saveParty(new Party(event.getParty().getLeader().getName(), members));
        }, 1L, TimeUnit.SECONDS);


    }

    @EventHandler
    public void onPartyEvent(LeftPartyEvent event) {
        ProxyServer.getInstance().getLogger().info("PartyLeftEvent event fired!");
        sync(event);
    }

    @EventHandler
    public void onPartyJoinEvent(PartyJoinEvent event) {
        ProxyServer.getInstance().getLogger().info("PartyJoinEvent event fired!");
        sync(event);
    }

    @EventHandler
    public void onPartyCreateEvent(PartyCreatedEvent event) {
        ProxyServer.getInstance().getLogger().info("PartyCreatedEvent event fired!");
        sync(event);
    }

    @EventHandler
    public void onPartyCreateEvent(PartyLeaderChangedEvent event) {
        ProxyServer.getInstance().getLogger().info("PartyLeaderChangedEvent event fired!");
        sync(event);
    }
}

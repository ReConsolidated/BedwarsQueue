package io.github.reconsolidated.bedwarsqueue.Listeners;

import io.github.reconsolidated.bedwarsqueue.BedwarsQueue;
import io.github.reconsolidated.bedwarsqueue.Queue;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class RemoveFromQueueOnDisconnect implements Listener {

    private final BedwarsQueue plugin;

    public RemoveFromQueueOnDisconnect(BedwarsQueue plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerQuit(PlayerDisconnectEvent event) {
        plugin.leaveQueue(event.getPlayer());
    }
}

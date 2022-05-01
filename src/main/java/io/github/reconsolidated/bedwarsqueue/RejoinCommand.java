package io.github.reconsolidated.bedwarsqueue;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Listener;

public class RejoinCommand extends Command implements Listener {
    private final BedwarsQueue plugin;

    public RejoinCommand(BedwarsQueue plugin) {
        super("powrót", "", "powrot");
        this.plugin = plugin;
        plugin.getProxy().getPluginManager().registerListener(plugin, this);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        plugin.getRejoinManager().rejoin((ProxiedPlayer) sender);
    }


}

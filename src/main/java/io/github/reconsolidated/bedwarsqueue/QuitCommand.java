package io.github.reconsolidated.bedwarsqueue;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Listener;

public class QuitCommand extends Command implements Listener {
    private final BedwarsQueue plugin;

    public QuitCommand(BedwarsQueue plugin) {
        super("opusc", "", "opusckolejke");
        this.plugin = plugin;
        plugin.getProxy().getPluginManager().registerListener(plugin, this);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        plugin.leaveQueue((ProxiedPlayer) sender);
    }


}

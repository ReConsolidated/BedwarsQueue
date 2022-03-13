package io.github.reconsolidated.bedwarsqueue;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static io.github.reconsolidated.bedwarsqueue.Database.DatabaseFunctions.setPlayerElo;

public class BDQueueCommand extends Command implements Listener {
    private final BedwarsQueue plugin;
    private final String channelName = "bedwars:channel";
    private final Map<ProxiedPlayer, Long> queueJoinCooldowns;

    public BDQueueCommand(BedwarsQueue plugin) {
        super("bdqueue", "bdqueue.admin", "bdq");
        this.plugin = plugin;
        plugin.getProxy().registerChannel(channelName);
        plugin.getProxy().getPluginManager().registerListener(plugin, this);

        queueJoinCooldowns = new HashMap<>();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            onHelp(sender);
        } else {
            if (args[0].equalsIgnoreCase("join")) {
                if (args.length != 3) {
                    sender.sendMessage(ChatColor.RED + "Correct usage:");
                    sender.sendMessage(ChatColor.AQUA + "/bdqueue join <player> <queue> - adds player to queue");
                } else {
                    String playerName = args[1];
                    String queueName = args[2];
                    ProxiedPlayer player = plugin.getProxy().getPlayer(playerName);

                    // Check if player has tried to join queue in last 1000 ms
                    long lastJoin = queueJoinCooldowns.getOrDefault(player, 0L);
                    long diff = System.currentTimeMillis() - lastJoin;
                    ProxyServer.getInstance().getLogger().info("Diff: " + diff);
                    if (diff < 1000) {
                        return;
                    }
                    if (player != null) {
                        queueJoinCooldowns.put(player, System.currentTimeMillis());
                    }

                    if (player == null) {
                        sender.sendMessage(ChatColor.RED + "This player is not online!");
                    } else {
                        if (plugin.joinQueue(queueName, player)) {
                            sender.sendMessage(ChatColor.GREEN + "Added player " + playerName + " to queue " + queueName);
                        } else {
                            sender.sendMessage(ChatColor.RED + "Couldn't add player " + playerName + " to queue " + queueName);
                        }
                    }
                }
            }
            if (args[0].equalsIgnoreCase("setelo")) {
                if (args.length != 3) {
                    sender.sendMessage(ChatColor.RED + "Correct usage:");
                    sender.sendMessage(ChatColor.AQUA + "/bdqueue setelo <player> <elo> - sets player elo to given amount");
                } else {
                    String playerName = args[1];
                    String eloString = args[2];
                    double elo;
                    try {
                        elo = Double.parseDouble(eloString);
                    } catch (NumberFormatException exception) {
                        sender.sendMessage(ChatColor.RED + "Incorrect number: " + eloString);
                        return;
                    }

                    ProxiedPlayer player = plugin.getProxy().getPlayer(playerName);

                    if (player == null) {
                        sender.sendMessage(ChatColor.RED + "This player is not online!");
                    } else {
                        setPlayerElo(player.getName(), elo);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onMessageReceive(PluginMessageEvent event) {
        if (!event.getTag().equalsIgnoreCase(channelName)) {
            return;
        }

        ByteArrayDataInput in = ByteStreams.newDataInput( event.getData() );
        List<String> args = new ArrayList<>();
        try {
            while (true) {
                args.add(in.readUTF());
            }
        } catch (Exception ignored) {
        }

        ProxyServer.getInstance().getScheduler().schedule(plugin, () -> {
            execute(plugin.getProxy().getConsole(), args.toArray(new String[0]));
        }, 0L, TimeUnit.SECONDS);

    }

    private void onHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.AQUA + "BDQueue commands:");
        sender.sendMessage(ChatColor.AQUA + "/bdqueue join <player> <queue> - adds player to queue");
        sender.sendMessage(ChatColor.AQUA + "/bdqueue leave <player> - leaves active queue");
        sender.sendMessage(ChatColor.AQUA + "/bdqueue setelo <player> <elo> - sets player elo to given amount");
    }
}

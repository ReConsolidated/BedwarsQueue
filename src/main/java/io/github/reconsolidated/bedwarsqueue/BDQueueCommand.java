package io.github.reconsolidated.bedwarsqueue;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import io.github.reconsolidated.bedwarsqueue.Queues.Queue;
import io.github.reconsolidated.bedwarsqueue.Queues.RankedQueue;
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
                if (args.length != 4) {
                    sender.sendMessage(ChatColor.RED + "Correct usage:");
                    sender.sendMessage(ChatColor.AQUA + "/bdqueue setelo <player> <queue> <elo> - sets player elo to given amount");
                } else {
                    String playerName = args[1];
                    String eloString = args[3];
                    String queueName = args[2];
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
                        setPlayerElo(player.getName(), queueName, elo);
                        sender.sendMessage(ChatColor.GREEN + "Player elo set.");
                    }
                }
            }
            if (args[0].equalsIgnoreCase("leave")) {
                if (args.length != 2) {
                    sender.sendMessage(ChatColor.RED + "Correct usage:");
                    sender.sendMessage(ChatColor.AQUA + "/bdqueue leave <player> - leave active queue");
                } else {
                    String playerName = args[1];
                    ProxiedPlayer player = plugin.getProxy().getPlayer(playerName);
                    if (player == null) {
                        sender.sendMessage(ChatColor.RED + "This player is not online!");
                    } else {
                        plugin.leaveQueue(player);
                    }
                }
            }

            if (args[0].equalsIgnoreCase("addqueue")) {
                if (args.length != 3) {
                    sender.sendMessage(ChatColor.RED + "Correct usage:");
                    sender.sendMessage(ChatColor.AQUA + "/bdqueue addqueue <name> <gamemode> - creates new queue");
                } else {
                    RankedQueue queue = new RankedQueue(plugin, args[1], args[2], 16);
                    plugin.getQueues().add(queue);
                    ProxyServer.getInstance().getScheduler().schedule(plugin, queue, 0L, 500L, TimeUnit.MILLISECONDS);
                    sender.sendMessage(ChatColor.GREEN + "Queue added!");
                }
            }

            if (args[0].equalsIgnoreCase("removequeue")) {
                if (args.length != 2) {
                    sender.sendMessage(ChatColor.RED + "Correct usage:");
                    sender.sendMessage(ChatColor.AQUA + "/bdqueue removequeue <name> - removes queue");
                } else {
                    for (Queue q : plugin.getQueues()) {
                        if (q.getName().equalsIgnoreCase(args[1])) {
                            plugin.getQueues().remove(q);
                            sender.sendMessage(ChatColor.GREEN + "Queue deleted.");
                            return;
                        }
                    }
                    sender.sendMessage(ChatColor.RED + "Didn't find queue with that name.");
                }
            }

            if (args[0].equalsIgnoreCase("listqueues")) {
                if (args.length != 1) {
                    sender.sendMessage(ChatColor.RED + "Correct usage:");
                    sender.sendMessage(ChatColor.AQUA + "/bdqueue listqueues - list queues");
                } else {
                    sender.sendMessage(ChatColor.GREEN + "ACTIVE QUEUES: ");
                    for (Queue q : plugin.getQueues()) {
                        sender.sendMessage(" - " + q.getDescription());
                    }
                }
            }

            if (args[0].equalsIgnoreCase("setstartamount")) {
                if (args.length != 3) {
                    sender.sendMessage(ChatColor.RED + "Correct usage:");
                    sender.sendMessage(ChatColor.AQUA + "/bdqueue setstartamount <queuename> <newstartamount> - set queue required players to start.");
                } else {
                    Queue queue = plugin.getQueue(args[1]);
                    if (queue == null) {
                        sender.sendMessage(ChatColor.RED + "Nie ma takiego queue");
                    } else {
                        if (queue instanceof RankedQueue) {
                            try {
                                ((RankedQueue)queue).setPlayersToStart(Integer.parseInt(args[2]));
                                sender.sendMessage(ChatColor.GREEN + "Ustawiono rozmiar kolejki");
                            } catch (NumberFormatException exception) {
                                sender.sendMessage(ChatColor.RED + "Podaj prawidłową liczbę.");
                            }
                        } else {
                            sender.sendMessage(ChatColor.RED + "Ta komenda dotyczy tylko kolejek rankingowych!");
                        }

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
        sender.sendMessage(ChatColor.AQUA + "/bdqueue setelo <player> <queue> <elo> - sets player elo to given amount");
        sender.sendMessage(ChatColor.AQUA + "/bdqueue addqueue <name> <playersToStart> - creates new queue");
        sender.sendMessage(ChatColor.AQUA + "/bdqueue removequeue <name> - removes queue");
        sender.sendMessage(ChatColor.AQUA + "/bdqueue listqueues - list queues");
        sender.sendMessage(ChatColor.AQUA + "/bdqueue setstartamount <queuename> <newstartamount> - set queue required players to start.");
    }
}

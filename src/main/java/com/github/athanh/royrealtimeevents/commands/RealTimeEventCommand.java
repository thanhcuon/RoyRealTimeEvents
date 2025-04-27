package com.github.athanh.royrealtimeevents.commands;

import com.github.athanh.royrealtimeevents.RoyRealTimeEvents;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class RealTimeEventCommand implements CommandExecutor, TabCompleter {
    private final RoyRealTimeEvents plugin;

    public RealTimeEventCommand(RoyRealTimeEvents plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("royrealtimeevents.admin")) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("messages.no-permission")));
            return true;
        }

        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                plugin.reloadPlugin();
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        plugin.getConfig().getString("messages.reload")));
                break;

            case "forcespawn":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /rte forcespawn <dayboss|nightboss|dayguardian|nightdemon>");
                    return true;
                }
                handleForceSpawn(sender, args[1]);
                break;

            case "bloodmoon":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /rte bloodmoon <start|stop>");
                    return true;
                }
                handleBloodMoon(sender, args[1]);
                break;

            case "debug":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
                    return true;
                }
                toggleDebugMode((Player) sender);
                break;

            case "help":
                sendHelpMessage(sender);
                break;

            default:
                sender.sendMessage(ChatColor.RED + "Unknown command. Use /rte help for command list.");
                break;
        }

        return true;
    }

    private void handleForceSpawn(CommandSender sender, String type) {
        World world;
        Location spawnLoc;

        if (sender instanceof Player) {
            world = ((Player) sender).getWorld();
            spawnLoc = ((Player) sender).getLocation();
        } else {
            world = Bukkit.getWorlds().get(0);
            spawnLoc = world.getSpawnLocation();
        }

        switch (type.toLowerCase()) {
            case "dayboss":
                if (plugin.getConfig().getBoolean("mobs.day-boss.enabled")) {
                    String mobType = plugin.getConfig().getString("mobs.day-boss.mythic-mob");
                    spawnMob(mobType, spawnLoc);
                    sender.sendMessage(ChatColor.GREEN + "Day Boss spawned!");
                }
                break;
            case "nightboss":
                if (plugin.getConfig().getBoolean("mobs.night-boss.enabled")) {
                    String mobType = plugin.getConfig().getString("mobs.night-boss.mythic-mob");
                    spawnMob(mobType, spawnLoc);
                    sender.sendMessage(ChatColor.GREEN + "Night Boss spawned!");
                }
                break;
            case "dayguardian":
                if (plugin.getConfig().getBoolean("mobs.day-guardian-event.enabled")) {
                    List<String> mobTypes = plugin.getConfig().getStringList("mobs.day-guardian-event.mythic-mobs");
                    for (String mobType : mobTypes) {
                        spawnMob(mobType, spawnLoc);
                    }
                    sender.sendMessage(ChatColor.GREEN + "Day Guardians spawned!");
                }
                break;
            case "nightdemon":
                if (plugin.getConfig().getBoolean("mobs.night-demon-event.enabled")) {
                    List<String> mobTypes = plugin.getConfig().getStringList("mobs.night-demon-event.mythic-mobs");
                    for (String mobType : mobTypes) {
                        spawnMob(mobType, spawnLoc);
                    }
                    sender.sendMessage(ChatColor.GREEN + "Night Demons spawned!");
                }
                break;
            default:
                sender.sendMessage(ChatColor.RED + "Invalid mob type!");
                break;
        }
    }

    private void handleBloodMoon(CommandSender sender, String action) {
        if (!plugin.getConfig().getBoolean("blood-moon.enabled")) {
            sender.sendMessage(ChatColor.RED + "Blood Moon feature is disabled in config!");
            return;
        }

        switch (action.toLowerCase()) {
            case "start":
                plugin.getRealTimeTask().startBloodMoon();
                sender.sendMessage(ChatColor.GREEN + "Blood Moon event started!");
                break;
            case "stop":
                plugin.getRealTimeTask().stopBloodMoon();
                sender.sendMessage(ChatColor.GREEN + "Blood Moon event stopped!");
                break;
            default:
                sender.sendMessage(ChatColor.RED + "Invalid action! Use start or stop.");
                break;
        }
    }

    private void toggleDebugMode(Player player) {
        boolean debugMode = !plugin.getDebugPlayers().contains(player.getUniqueId());
        if (debugMode) {
            plugin.getDebugPlayers().add(player.getUniqueId());
            player.sendMessage(ChatColor.GREEN + "Debug mode enabled!");
        } else {
            plugin.getDebugPlayers().remove(player.getUniqueId());
            player.sendMessage(ChatColor.GREEN + "Debug mode disabled!");
        }
    }

    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("help-messages.header")));

        for (String command : plugin.getConfig().getConfigurationSection("help-messages.commands").getKeys(false)) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("help-messages.commands." + command)));
        }
    }

    private void spawnMob(String mobType, Location location) {
        if (mobType != null && location != null) {
            plugin.getMythicMobsAPI().getMobManager().spawnMob(mobType, location);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("royrealtimeevents.admin")) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            return Arrays.asList("reload", "forcespawn", "bloodmoon", "debug", "help")
                    .stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "forcespawn":
                    return Arrays.asList("dayboss", "nightboss", "dayguardian", "nightdemon")
                            .stream()
                            .filter(s -> s.startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
                case "bloodmoon":
                    return Arrays.asList("start", "stop")
                            .stream()
                            .filter(s -> s.startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
            }
        }

        return new ArrayList<>();
    }
}

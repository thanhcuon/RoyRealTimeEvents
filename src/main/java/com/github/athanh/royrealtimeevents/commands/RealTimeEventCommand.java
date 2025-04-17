package com.github.athanh.royrealtimeevents.commands;

import com.github.athanh.royrealtimeevents.RoyRealTimeEvents;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class RealTimeEventCommand implements CommandExecutor {
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
            sender.sendMessage(ChatColor.YELLOW + "usage: /rte reload");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            plugin.reloadPlugin();
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getConfig().getString("messages.reload")));
            return true;
        }

        return false;
    }
}
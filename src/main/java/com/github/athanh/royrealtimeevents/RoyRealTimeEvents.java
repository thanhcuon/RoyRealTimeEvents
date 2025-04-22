package com.github.athanh.royrealtimeevents;

import com.github.athanh.royrealtimeevents.commands.RealTimeEventCommand;
import com.github.athanh.royrealtimeevents.listeners.EventListener;
import com.github.athanh.royrealtimeevents.listeners.PlayerListener;
import com.github.athanh.royrealtimeevents.task.RealTimeTask;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public class RoyRealTimeEvents extends JavaPlugin {
    private RealTimeTask realTimeTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        if (getConfig().getBoolean("metrics.enabled", true)) {
            new Metrics(this, 25548);
            getLogger().info("Metrics enabled!");
        } else {
            getLogger().info("Metrics Disable!");
        }
        getCommand("realtimeevent").setExecutor(new RealTimeEventCommand(this));

        getServer().getPluginManager().registerEvents(new EventListener(this), this);


        startRealTimeTask();
        Bukkit.getConsoleSender().sendMessage(ChatColor.LIGHT_PURPLE +"--------------------------------------------------------------");
        Bukkit.getConsoleSender().sendMessage( "");
        Bukkit.getConsoleSender().sendMessage(ChatColor.LIGHT_PURPLE + "______           ______           _ _____ _                _____                _       ");
        Bukkit.getConsoleSender().sendMessage(ChatColor.LIGHT_PURPLE + "| ___ \\          | ___ \\         | |_   _(_)              |  ___|              | |      ");
        Bukkit.getConsoleSender().sendMessage(ChatColor.LIGHT_PURPLE + "| |_/ /___  _   _| |_/ /___  __ _| | | |  _ _ __ ___   ___| |____   _____ _ __ | |_ ___ ");
        Bukkit.getConsoleSender().sendMessage(ChatColor.LIGHT_PURPLE + "|    // _ \\| | | |    // _ \\/ _` | | | | | | '_ ` _ \\ / _ \\  __\\ \\ / / _ \\ '_ \\| __/ __|");
        Bukkit.getConsoleSender().sendMessage(ChatColor.LIGHT_PURPLE + "| |\\ \\ (_) | |_| | |\\ \\  __/ (_| | | | | | | | | | | |  __/ |___\\ V /  __/ | | | |_\\__ \\");
        Bukkit.getConsoleSender().sendMessage(ChatColor.LIGHT_PURPLE + "\\_| \\_\\___/ \\__, \\_| \\_\\___|\\__,_|_| \\_/ |_|_| |_| |_|\\___\\____/ \\_/ \\___|_| |_|\\__|___/");
        Bukkit.getConsoleSender().sendMessage(ChatColor.LIGHT_PURPLE + "             __/ |                                                                      ");
        Bukkit.getConsoleSender().sendMessage(ChatColor.LIGHT_PURPLE + "            |___/                                                                       ");
        Bukkit.getConsoleSender().sendMessage("");
        String var10000 = this.getDescription().getVersion();
        Bukkit.getConsoleSender().sendMessage(ChatColor.YELLOW +"Version: " + ChatColor.YELLOW + var10000);
        Bukkit.getConsoleSender().sendMessage(ChatColor.YELLOW +"Author: athanh");
        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage(ChatColor.YELLOW +"RoyRealTimeEvents Enable!");
        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage(ChatColor.LIGHT_PURPLE +"--------------------------------------------------------------");
    }

    @Override
    public void onDisable() {
        if (realTimeTask != null) {
            realTimeTask.cancel();
        }
        Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_RED +"--------------------------------------------------------------");
        Bukkit.getConsoleSender().sendMessage( "");
        Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_RED + "______           ______           _ _____ _                _____                _       ");
        Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_RED + "| ___ \\          | ___ \\         | |_   _(_)              |  ___|              | |      ");
        Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_RED + "| |_/ /___  _   _| |_/ /___  __ _| | | |  _ _ __ ___   ___| |____   _____ _ __ | |_ ___ ");
        Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_RED + "|    // _ \\| | | |    // _ \\/ _` | | | | | | '_ ` _ \\ / _ \\  __\\ \\ / / _ \\ '_ \\| __/ __|");
        Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_RED + "| |\\ \\ (_) | |_| | |\\ \\  __/ (_| | | | | | | | | | | |  __/ |___\\ V /  __/ | | | |_\\__ \\");
        Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_RED + "\\_| \\_\\___/ \\__, \\_| \\_\\___|\\__,_|_| \\_/ |_|_| |_| |_|\\___\\____/ \\_/ \\___|_| |_|\\__|___/");
        Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_RED + "             __/ |                                                                      ");
        Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_RED + "            |___/                                                                       ");
        Bukkit.getConsoleSender().sendMessage("");
        String var10000 = this.getDescription().getVersion();
        Bukkit.getConsoleSender().sendMessage(ChatColor.YELLOW +"Version: " + ChatColor.YELLOW + var10000);
        Bukkit.getConsoleSender().sendMessage(ChatColor.YELLOW +"Author: athanh");
        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage(ChatColor.YELLOW +"RoyRealTimeEvents Disable!");
        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_RED +"--------------------------------------------------------------");
    }

    public void startRealTimeTask() {
        if (realTimeTask != null) {
            realTimeTask.cancel();
        }
        realTimeTask = new RealTimeTask(this);

        getServer().getPluginManager().registerEvents(new PlayerListener(realTimeTask), this);
        realTimeTask.runTaskTimer(this, 0L, 20L * 60);
    }

    public void reloadPlugin() {
        reloadConfig();
        startRealTimeTask();
    }
}

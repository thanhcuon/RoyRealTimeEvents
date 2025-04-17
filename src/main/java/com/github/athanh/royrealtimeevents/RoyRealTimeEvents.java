package com.github.athanh.royrealtimeevents;

import com.github.athanh.royrealtimeevents.commands.RealTimeEventCommand;
import com.github.athanh.royrealtimeevents.listeners.EventListener;
import com.github.athanh.royrealtimeevents.listeners.PlayerListener;
import com.github.athanh.royrealtimeevents.task.RealTimeTask;
import org.bukkit.plugin.java.JavaPlugin;

public class RoyRealTimeEvents extends JavaPlugin {
    private RealTimeTask realTimeTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        getCommand("realtimeevent").setExecutor(new RealTimeEventCommand(this));

        getServer().getPluginManager().registerEvents(new EventListener(this), this);


        startRealTimeTask();

        getLogger().info("RoyRealTimeEvents Enable!");
    }

    @Override
    public void onDisable() {
        if (realTimeTask != null) {
            realTimeTask.cancel();
        }
        getLogger().info("RoyRealTimeEvents Disable!");
    }

    public void startRealTimeTask() {
        if (realTimeTask != null) {
            realTimeTask.cancel();
        }
        realTimeTask = new RealTimeTask(this);

        getServer().getPluginManager().registerEvents(new PlayerListener(realTimeTask), this);
        realTimeTask.runTaskTimer(this, 0L, 20L * 60); // Vẫn chạy mỗi phút nhưng có kiểm tra điều kiện
    }

    public void reloadPlugin() {
        reloadConfig();
        startRealTimeTask();
    }
}
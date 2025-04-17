package com.github.athanh.royrealtimeevents.listeners;

import com.github.athanh.royrealtimeevents.RoyRealTimeEvents;
import com.github.athanh.royrealtimeevents.task.RealTimeTask;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {
    private final RealTimeTask realTimeTask;

    public PlayerListener(RealTimeTask realTimeTask) {
        this.realTimeTask = realTimeTask;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        realTimeTask.notifyNewPlayer(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        realTimeTask.removePlayer(event.getPlayer());
    }
}
package com.github.athanh.royrealtimeevents.listeners;

import com.github.athanh.royrealtimeevents.RoyRealTimeEvents;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.TimeSkipEvent;

public class EventListener implements Listener {
    private final RoyRealTimeEvents plugin;

    public EventListener(RoyRealTimeEvents plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onTimeSkip(TimeSkipEvent event) {
        if (event.getSkipReason() == TimeSkipEvent.SkipReason.COMMAND) {
            event.setCancelled(true);
        }
    }
}

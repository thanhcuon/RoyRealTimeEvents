package com.github.athanh.royrealtimeevents.listeners;

import com.github.athanh.royrealtimeevents.RoyRealTimeEvents;
import com.github.athanh.royrealtimeevents.task.RealTimeTask;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class PlayerListener implements Listener {
    private final RealTimeTask realTimeTask;

    public PlayerListener(RealTimeTask realTimeTask) {
        this.realTimeTask = realTimeTask;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        realTimeTask.notifyNewPlayer(event.getPlayer());
        Player player = event.getPlayer();
        Location loc = player.getLocation();
        World world = player.getWorld();

        new BukkitRunnable() {
            int count = 0;
            @Override
            public void run() {
                if (count >= 20) {
                    this.cancel();
                    return;
                }

                for (double i = 0; i < Math.PI * 2; i += Math.PI / 8) {
                    double x = Math.cos(i) * 1;
                    double z = Math.sin(i) * 1;
                    Location particleLoc = loc.clone().add(x, 1, z);
                    world.spawnParticle(Particle.END_ROD, particleLoc, 1, 0, 0, 0, 0);
                }

                count++;
            }
        }.runTaskTimer(realTimeTask.getPlugin(), 0L, 1L);

    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        realTimeTask.removePlayer(event.getPlayer());
    }
}

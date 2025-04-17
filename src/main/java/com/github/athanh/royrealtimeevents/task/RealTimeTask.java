package com.github.athanh.royrealtimeevents.task;

import com.github.athanh.royrealtimeevents.RoyRealTimeEvents;
import io.lumine.mythic.bukkit.MythicBukkit;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class RealTimeTask extends BukkitRunnable {
    private final RoyRealTimeEvents plugin;
    private boolean isNightTime = false;
    private int lastNightEventHour = -1;
    private int lastDayEventHour = -1;
    private int lastNightBossSpawnHour = -1;
    private int lastDayBossSpawnHour = -1;
    private final Set<String> notifiedPlayers = new HashSet<>();
    private final Set<UUID> buffedMobs = new HashSet<>();
    private final Map<Location, Long> activeSpawnLocations = new HashMap<>();
    private BukkitRunnable particleTask;



    public RealTimeTask(RoyRealTimeEvents plugin) {
        this.plugin = plugin;
        startParticleTask();

    }

    @Override
    public void run() {
        LocalTime now = LocalTime.now();
        int hour = now.getHour();
        int minute = now.getMinute();
        int nightStart = plugin.getConfig().getInt("settings.night-time.start");
        int nightEnd = plugin.getConfig().getInt("settings.night-time.end");


        for (World world : Bukkit.getWorlds()) {

            long minecraftTime = (hour * 1000) % 24000;
            world.setTime(minecraftTime);


            boolean isNightNow = (hour >= nightStart || hour < nightEnd);
            if (isNightNow != isNightTime && minute == 0) {
                isNightTime = isNightNow;
                if (isNightTime) {
                    announceNightTime();
                    buffedMobs.clear();
                } else {
                    announceDayTime();
                }
            }


            if (isNightTime) {
                if (plugin.getConfig().getBoolean("mob-buffs.enabled")) {
                    strengthenMobs(world);
                }

                if (hour != lastNightBossSpawnHour && minute == 0 &&
                        plugin.getConfig().getBoolean("mobs.night-boss.enabled")) {
                    spawnNightBoss(world);
                    lastNightBossSpawnHour = hour;
                }

                int nightEventHour = plugin.getConfig().getInt("settings.night-event.hour");
                if (hour == nightEventHour && lastNightEventHour != hour && minute == 0) {
                    if (plugin.getConfig().getBoolean("mobs.night-demon-event.enabled")) {
                        triggerNightDemonEvent(world);
                        lastNightEventHour = hour;
                    }
                }
            }

            else {
                if (hour != lastDayBossSpawnHour && minute == 0 &&
                        plugin.getConfig().getBoolean("mobs.day-boss.enabled")) {
                    spawnDayBoss(world);
                    lastDayBossSpawnHour = hour;
                }

                int dayEventHour = plugin.getConfig().getInt("settings.day-event.hour");
                if (hour == dayEventHour && lastDayEventHour != hour && minute == 0) {
                    if (plugin.getConfig().getBoolean("mobs.day-guardian-event.enabled")) {
                        triggerDayGuardianEvent(world);
                        lastDayEventHour = hour;
                    }
                }
            }
        }


        if (hour == 0 && minute == 0) {
            lastNightEventHour = -1;
            lastDayEventHour = -1;
            lastNightBossSpawnHour = -1;
            lastDayBossSpawnHour = -1;
            buffedMobs.clear();
        }
    }



    private void strengthenMobs(World world) {
        List<Map<?, ?>> effects = plugin.getConfig().getMapList("mob-buffs.effects");
        for (Entity entity : world.getEntities()) {
            if (entity instanceof Monster && !buffedMobs.contains(entity.getUniqueId())) {
                for (Map<?, ?> effect : effects) {
                    PotionEffectType type = PotionEffectType.getByName(effect.get("effect").toString());
                    if (type != null) {
                        ((Monster) entity).addPotionEffect(new PotionEffect(
                                type,
                                ((Number) effect.get("duration")).intValue(),
                                ((Number) effect.get("amplifier")).intValue()
                        ));
                    }
                }
                buffedMobs.add(entity.getUniqueId());
            }
        }
    }
    private void startParticleTask() {
        particleTask = new BukkitRunnable() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();
                Iterator<Map.Entry<Location, Long>> iterator = activeSpawnLocations.entrySet().iterator();

                while (iterator.hasNext()) {
                    Map.Entry<Location, Long> entry = iterator.next();
                    Location loc = entry.getKey();
                    long spawnTime = entry.getValue();

                    if (currentTime - spawnTime > 300000) {
                        iterator.remove();
                        continue;
                    }


                    for (double y = 0; y < 50; y += 0.5) {
                        Location particleLoc = loc.clone().add(0, y, 0);
                        loc.getWorld().spawnParticle(Particle.FLAME, particleLoc, 1, 0, 0, 0, 0);
                    }

                    drawParticleCircle(loc);
                }
            }
        };
        particleTask.runTaskTimer(plugin, 0L, 5L);
    }

    private void drawParticleCircle(Location center) {
        double radius = 2;
        for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 16) {
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            Location particleLoc = center.clone().add(x, 0.1, z);
            center.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, particleLoc, 1, 0, 0, 0, 0);
        }
    }

    private void markSpawnLocation(Location location, String mobType, String eventType) {
        activeSpawnLocations.put(location, System.currentTimeMillis());

        String coords = String.format("&e[X: %d, Y: %d, Z: %d]",
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ()
        );

        String message = ChatColor.translateAlternateColorCodes('&',
                "&c" + mobType + " &7(&f" + eventType + "&7) &fđã xuất hiện tại: " + coords);
        Bukkit.broadcastMessage(message);


        location.getWorld().strikeLightningEffect(location);
        location.getWorld().playSound(location, Sound.BLOCK_BELL_USE, 3.0f, 1.0f);
        spawnFireworkEffect(location);
    }

    private void spawnFireworkEffect(Location location) {
        World world = location.getWorld();
        if (world != null) {
            world.spawnParticle(Particle.EXPLOSION_HUGE, location, 1);
            world.playSound(location, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 1.0f);
        }
    }

    public void notifyNewPlayer(Player player) {
        if (!notifiedPlayers.contains(player.getName())) {
            LocalTime now = LocalTime.now();
            int hour = now.getHour();
            int nightStart = plugin.getConfig().getInt("settings.night-time.start");
            int nightEnd = plugin.getConfig().getInt("settings.night-time.end");

            boolean isNightNow = (hour >= nightStart || hour < nightEnd);
            if (isNightNow) {
                String message = plugin.getConfig().getString("messages.nighttime-start");
                if (message != null) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
                }
            }
            notifiedPlayers.add(player.getName());
        }
    }
    private void spawnDayBoss(World world) {

        if (!isWorldAllowed(world.getName())) {
            return;
        }

        double chance = plugin.getConfig().getDouble("mobs.day-boss.spawn-chance");
        if (Math.random() < chance) {
            Location loc = getRandomLocation(world);
            String mobType = plugin.getConfig().getString("mobs.day-boss.mythic-mob");
            spawnMythicMob(mobType, loc);
            markSpawnLocation(loc, "Day Boss", "Day Event");
            broadcastBossSpawn("day-boss");
        }
    }

    private void spawnNightBoss(World world) {

        if (!isWorldAllowed(world.getName())) {
            return;
        }

        double chance = plugin.getConfig().getDouble("mobs.night-boss.spawn-chance");
        if (Math.random() < chance) {
            Location loc = getRandomLocation(world);
            String mobType = plugin.getConfig().getString("mobs.night-boss.mythic-mob");
            spawnMythicMob(mobType, loc);
            markSpawnLocation(loc, "Night Boss", "Night Event");
            broadcastBossSpawn("night-boss");
        }
    }

    private void triggerDayGuardianEvent(World world) {

        if (!isWorldAllowed(world.getName())) {
            return;
        }

        broadcastEventStart("day-guardian-event");
        List<String> mobTypes = plugin.getConfig().getStringList("mobs.day-guardian-event.mythic-mobs");
        int spawnCount = plugin.getConfig().getInt("mobs.day-guardian-event.spawn-count");

        for (int i = 0; i < spawnCount; i++) {
            Location loc = getRandomLocation(world);
            String randomMob = mobTypes.get(ThreadLocalRandom.current().nextInt(mobTypes.size()));
            spawnMythicMob(randomMob, loc);
            markSpawnLocation(loc, "Guardian", "Day Guardian Event");
        }
    }

    private void triggerNightDemonEvent(World world) {

        if (!isWorldAllowed(world.getName())) {
            return;
        }

        broadcastEventStart("night-demon-event");
        List<String> mobTypes = plugin.getConfig().getStringList("mobs.night-demon-event.mythic-mobs");
        int spawnCount = plugin.getConfig().getInt("mobs.night-demon-event.spawn-count");

        for (int i = 0; i < spawnCount; i++) {
            Location loc = getRandomLocation(world);
            String randomMob = mobTypes.get(ThreadLocalRandom.current().nextInt(mobTypes.size()));
            spawnMythicMob(randomMob, loc);
            markSpawnLocation(loc, "Night Demon", "Night Demon Event");
        }
    }

    private void broadcastEventStart(String eventKey) {
        String announcement = plugin.getConfig().getString("mobs." + eventKey + ".announcement");
        if (announcement != null) {
            Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', announcement));
        }

        String title = plugin.getConfig().getString("mobs." + eventKey + ".title.main");
        String subtitle = plugin.getConfig().getString("mobs." + eventKey + ".title.subtitle");
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendTitle(
                    ChatColor.translateAlternateColorCodes('&', title),
                    ChatColor.translateAlternateColorCodes('&', subtitle),
                    10, 70, 20
            );
        }
    }

    private void broadcastBossSpawn(String bossKey) {
        String sound = plugin.getConfig().getString("mobs." + bossKey + ".effects.sound");
        if (sound != null) {
            float volume = (float) plugin.getConfig().getDouble("mobs." + bossKey + ".effects.volume", 1.0);
            float pitch = (float) plugin.getConfig().getDouble("mobs." + bossKey + ".effects.pitch", 1.0);
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.playSound(player.getLocation(), Sound.valueOf(sound), volume, pitch);
            }
        }
    }


    public void removePlayer(Player player) {
        notifiedPlayers.remove(player.getName());
    }

    @Override
    public void cancel() {
        if (particleTask != null) {
            particleTask.cancel();
        }
        super.cancel();
    }


    private boolean isWorldAllowed(String worldName) {
        List<String> enabledWorlds = plugin.getConfig().getStringList("worlds.enabled-worlds");
        List<String> disabledWorlds = plugin.getConfig().getStringList("worlds.disabled-worlds");

        if (disabledWorlds.contains(worldName)) {
            return false;
        }
        

        return enabledWorlds.contains(worldName);
    }

    private Location getRandomLocation(World world) {
        int minRadius = plugin.getConfig().getInt("settings.spawn-radius.min", -200);
        int maxRadius = plugin.getConfig().getInt("settings.spawn-radius.max", 200);

        int x = ThreadLocalRandom.current().nextInt(minRadius, maxRadius);
        int z = ThreadLocalRandom.current().nextInt(minRadius, maxRadius);
        int y = world.getHighestBlockYAt(x, z);

        return new Location(world, x, y + 1, z);
    }

    private void spawnMythicMob(String mobType, Location location) {
        if (mobType != null && location != null) {
            MythicBukkit.inst().getMobManager().spawnMob(mobType, location);
        }
    }

    private void announceNightTime() {
        String message = plugin.getConfig().getString("messages.nighttime-start");
        if (message != null) {
            Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', message));
        }
    }

    private void announceDayTime() {
        String message = plugin.getConfig().getString("messages.daytime-start");
        if (message != null) {
            Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', message));
        }
    }
}
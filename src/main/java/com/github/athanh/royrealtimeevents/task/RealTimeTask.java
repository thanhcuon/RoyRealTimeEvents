package com.github.athanh.royrealtimeevents.task;

import com.github.athanh.royrealtimeevents.RoyRealTimeEvents;
import io.lumine.mythic.bukkit.MythicBukkit;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.LocalDate;
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
    private Map<String, Integer> activeMobsCount = new HashMap<>();
    private boolean isBloodMoon = false;
    private LocalDate lastBloodMoon = null;

    private void checkBloodMoon() {
        if (!plugin.getConfig().getBoolean("blood-moon.enabled")) {
            return;
        }

        LocalTime now = LocalTime.now();
        LocalDate today = LocalDate.now();

        // Chỉ check blood moon một lần mỗi đêm
        if (lastBloodMoon != null && lastBloodMoon.equals(today)) {
            return;
        }

        // Check khi bắt đầu đêm
        if (now.getHour() == plugin.getConfig().getInt("settings.night-time.start") && now.getMinute() == 0) {
            double chance = plugin.getConfig().getDouble("blood-moon.chance");
            if (Math.random() < chance) {
                startBloodMoon();
                lastBloodMoon = today;
            }
        }
    }

    public void startBloodMoon() {
        isBloodMoon = true;

        String title = plugin.getConfig().getString("blood-moon.announcement.title");
        String subtitle = plugin.getConfig().getString("blood-moon.announcement.subtitle");
        String message = plugin.getConfig().getString("blood-moon.announcement.message");

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendTitle(
                ChatColor.translateAlternateColorCodes('&', title),
                ChatColor.translateAlternateColorCodes('&', subtitle),
                10, 70, 20
            );

            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.5f);
            player.playSound(player.getLocation(), Sound.AMBIENT_CAVE, 1.0f, 0.5f);

            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 1));
        }

        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', message));

        for (World world : Bukkit.getWorlds()) {
            startBloodMoonEffects(world);
        }
    }
    public void stopBloodMoon() {
        isBloodMoon = false;

        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Monster) {
                    Monster monster = (Monster) entity;

                    double defaultHealth = 20.0; 
                    monster.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(defaultHealth);
                    monster.setHealth(defaultHealth);

                    if (monster.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE) != null) {
                        monster.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(
                                monster.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).getDefaultValue()
                        );
                    }

                    for (PotionEffect effect : monster.getActivePotionEffects()) {
                        monster.removePotionEffect(effect.getType());
                    }
                }
            }
        }

        String endTitle = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("blood-moon.end-announcement.title"));
        String endSubtitle = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("blood-moon.end-announcement.subtitle"));

        for (Player player : Bukkit.getOnlinePlayers()) {

            player.sendTitle(endTitle, endSubtitle, 10, 70, 20);

            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 1.0f);
            player.playSound(player.getLocation(), Sound.BLOCK_BELL_USE, 0.5f, 1.0f);

            Location loc = player.getLocation();
            player.spawnParticle(Particle.END_ROD, loc.add(0, 1, 0), 50, 2, 2, 2, 0.1);
            player.spawnParticle(Particle.PORTAL, loc, 100, 2, 2, 2, 0.1);
        }

        for (String message : plugin.getConfig().getStringList("blood-moon.end-announcement.messages")) {
            Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', message));
        }

        if (plugin.getConfig().getBoolean("blood-moon.special-mobs.remove-on-end", true)) {
            List<String> specialMobs = plugin.getConfig().getStringList("blood-moon.special-mobs");
            for (World world : Bukkit.getWorlds()) {
                for (Entity entity : world.getEntities()) {
                    if (entity.hasMetadata("BloodMoonMob")) {
                        // Hiệu ứng khi xóa mob
                        Location loc = entity.getLocation();
                        world.spawnParticle(Particle.CLOUD, loc, 20, 0.5, 0.5, 0.5, 0.1);
                        world.playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                        entity.remove();
                    }
                }
            }
        }

        for (World world : Bukkit.getWorlds()) {
            world.setStorm(false);
            world.setThundering(false);
        }

        new BukkitRunnable() {
            float intensity = 1.0f;
            @Override
            public void run() {
                if (intensity <= 0) {
                    this.cancel();
                    return;
                }

                intensity -= 0.1f;
                for (World world : Bukkit.getWorlds()) {
                    for (Player player : world.getPlayers()) {
                        Location loc = player.getLocation().add(0, 50, 0);
                        world.spawnParticle(
                                Particle.CRIMSON_SPORE,
                                loc,
                                10,
                                20, 10, 20,
                                intensity
                        );
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 5L);

        buffedMobs.clear();
        lastBloodMoon = LocalDate.now(); 
    }
    
    private void startBloodMoonEffects(World world) {
        new BukkitRunnable() {
            int duration = 0;
            @Override
            public void run() {
                if (!isBloodMoon || duration > 100) {
                    this.cancel();
                    return;
                }

                for (Player player : world.getPlayers()) {
                    Location loc = player.getLocation();
                    for (int i = 0; i < 5; i++) {
                        double x = loc.getX() + (Math.random() - 0.5) * 40;
                        double y = loc.getY() + 20 + (Math.random() - 0.5) * 10;
                        double z = loc.getZ() + (Math.random() - 0.5) * 40;
                        Location particleLoc = new Location(world, x, y, z);
                        world.spawnParticle(Particle.CRIMSON_SPORE, particleLoc, 1, 0, 0, 0, 0);
                        world.spawnParticle(Particle.DRIPPING_OBSIDIAN_TEAR, particleLoc, 1, 0, 0, 0, 0);
                    }
                    duration++;
                }
                duration++;
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

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
        checkBloodMoon();

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
                checkBloodMoon();
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

    // Thêm vào phương thức strengthenMobs
    private void strengthenMobs(World world) {
        List<Map<?, ?>> effects = plugin.getConfig().getMapList("mob-buffs.effects");
        double multiplier = isBloodMoon ?
            plugin.getConfig().getDouble("blood-moon.effects.mob-multiplier") : 1.0;

        for (Entity entity : world.getEntities()) {
            if (entity instanceof Monster && !buffedMobs.contains(entity.getUniqueId())) {
                Monster monster = (Monster) entity;

                // Tăng máu và sát thương cho quái trong Blood Moon
                if (isBloodMoon) {
                    monster.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(
                        monster.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue() * multiplier
                    );
                    monster.setHealth(monster.getMaxHealth());

                    if (monster.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE) != null) {
                        monster.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(
                            monster.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).getBaseValue() * multiplier
                        );
                    }
                }

                // Thêm các effect từ config
                for (Map<?, ?> effect : effects) {
                    PotionEffectType type = PotionEffectType.getByName(effect.get("effect").toString());
                    if (type != null) {
                        int amplifier = ((Number) effect.get("amplifier")).intValue();
                        if (isBloodMoon) {
                            amplifier++; // Tăng level effect trong Blood Moon
                        }
                        monster.addPotionEffect(new PotionEffect(
                            type,
                            ((Number) effect.get("duration")).intValue(),
                            amplifier
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

        // Format tọa độ với thêm hướng (N,S,E,W)
        String direction = getCardinalDirection(location);
        String coords = String.format("&e[X: %d, Y: %d, Z: %d | %s]",
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ(),
                direction
        );

        // Thông báo chính
        String message = ChatColor.translateAlternateColorCodes('&',
                "&c" + mobType + " &7(&f" + eventType + "&7) &fhas appeared at: " + coords);
        Bukkit.broadcastMessage(message);

        // Thông báo khoảng cách tới người chơi
        for (Player player : Bukkit.getOnlinePlayers()) {
            int distance = (int) player.getLocation().distance(location);
            String distanceMsg = ChatColor.translateAlternateColorCodes('&',
                    "&7Distance from you: &e" + distance + "m");
            player.sendMessage(distanceMsg);

            // Chỉ hướng với hạt effect
            showDirectionParticles(player.getLocation(), location);
        }

        location.getWorld().strikeLightningEffect(location);
        location.getWorld().playSound(location, Sound.BLOCK_BELL_USE, 3.0f, 1.0f);
        spawnFireworkEffect(location);
    }

    private String getCardinalDirection(Location loc) {
        double rotation = (loc.getYaw() - 90) % 360;
        if (rotation < 0) {
            rotation += 360.0;
        }
        if (0 <= rotation && rotation < 45) {
            return "W";
        } else if (45 <= rotation && rotation < 135) {
            return "N";
        } else if (135 <= rotation && rotation < 225) {
            return "E";
        } else if (225 <= rotation && rotation < 315) {
            return "S";
        } else if (315 <= rotation && rotation < 360) {
            return "W";
        } else {
            return "?";
        }
    }

    private void showDirectionParticles(Location start, Location end) {
        double distance = start.distance(end);
        org.bukkit.util.Vector direction = end.toVector().subtract(start.toVector()).normalize();

        // Chỉ hiển thị 20 particle để tránh spam
        for (double i = 0; i < Math.min(distance, 20); i++) {
            Location particleLoc = start.clone().add(direction.multiply(i));
            particleLoc.getWorld().spawnParticle(
                    Particle.END_ROD,
                    particleLoc,
                    1,
                    0, 0, 0,
                    0
            );
        }
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
    private void updateActiveMobsCount(String mobType) {
        activeMobsCount.merge(mobType, 1, Integer::sum);
        String message = ChatColor.translateAlternateColorCodes('&',
                "&7Currently there are &e" + activeMobsCount.get(mobType) + " &7" + mobType + " &7active");
        Bukkit.broadcastMessage(message);
    }


    private void spawnMythicMob(String mobType, Location location) {
        if (mobType != null && location != null) {
            MythicBukkit.inst().getMobManager().spawnMob(mobType, location);
            updateActiveMobsCount(mobType);
        }
    }


    private void announceNightTime() {
        String message = plugin.getConfig().getString("messages.nighttime-start");
        if (message != null) {
            Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', message));

            for (Player player : Bukkit.getOnlinePlayers()) {
                player.playSound(player.getLocation(), Sound.ENTITY_WOLF_HOWL, 1.0f, 1.0f);
                player.playSound(player.getLocation(), Sound.AMBIENT_CAVE, 0.5f, 1.0f);
            }
        }
    }


    private void announceDayTime() {
        String message = plugin.getConfig().getString("messages.daytime-start");
        if (message != null) {
            Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', message));

            for (Player player : Bukkit.getOnlinePlayers()) {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 2.0f);
            }
        }
    }
    public RoyRealTimeEvents getPlugin() {
        return this.plugin;
    }


}

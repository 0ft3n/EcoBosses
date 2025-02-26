package com.willfp.ecobosses.bosses.listeners;

import com.willfp.eco.util.NumberUtils;
import com.willfp.ecobosses.bosses.EcoBoss;
import com.willfp.ecobosses.bosses.EcoBosses;
import com.willfp.ecobosses.events.EcoBossSpawnTimerEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AutoSpawnTimer implements Runnable {
    private int tick = 0;

    @Override
    public void run() {
        tick++;

        for (EcoBoss boss : EcoBosses.values()) {
            if (boss.getAutoSpawnInterval() < 0) {
                continue;
            }

            if (boss.getAutoSpawnLocations().isEmpty()) {
                continue;
            }

            Set<World> worlds = new HashSet<>();

            for (Entity entity : boss.getLivingBosses().keySet()) {
                if (entity == null) {
                    continue;
                }

                worlds.add(entity.getWorld());
            }

            List<Location> locations = new ArrayList<>(boss.getAutoSpawnLocations());
            locations.removeIf(location -> worlds.contains(location.getWorld()));

            if (locations.isEmpty()) {
                continue;
            }

            boss.setTimeUntilSpawn(boss.getTimeUntilSpawn()-1);

            if (tick % boss.getAutoSpawnInterval() == 0) {
                Location location = locations.get(NumberUtils.randInt(0, locations.size() - 1));
                EcoBossSpawnTimerEvent event = new EcoBossSpawnTimerEvent(boss, location);
                Bukkit.getServer().getPluginManager().callEvent(event);
                if (!event.isCancelled()) {
                    boss.spawn(location);
                    boss.setTimeUntilSpawn(boss.getAutoSpawnInterval());
                }
            }
        }
    }
}

package com.willfp.illusioner.illusioner.listeners;
import com.willfp.eco.util.internal.PluginDependent;
import com.willfp.eco.util.plugin.AbstractEcoPlugin;
import com.willfp.illusioner.illusioner.BlockStructure;
import com.willfp.illusioner.illusioner.IllusionerManager;
import com.willfp.illusioner.proxy.proxies.EntityIllusionerProxy;
import com.willfp.illusioner.proxy.proxies.IllusionerHelperProxy;
import com.willfp.illusioner.util.ProxyUtils;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Illusioner;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public class SpawnListeners extends PluginDependent implements Listener {
    /**
     * Create new spawn listeners and link them to a plugin.
     *
     * @param plugin The plugin to link to.
     */
    public SpawnListeners(@NotNull final AbstractEcoPlugin plugin) {
        super(plugin);
    }

    /**
     * Called on block place.
     *
     * @param event The event to listen for.
     */
    @EventHandler
    public void onSpawn(@NotNull final BlockPlaceEvent event) {
        boolean matches = false;
        Set<Block> match = new HashSet<>();
        for (int i = 0; i < 3; i++) {
            Block block1;
            Block block2;
            Block block3;

            if (i == 0) {
                block3 = event.getBlock();
                block2 = event.getBlock().getRelative(0, -1, 0);
                block1 = event.getBlock().getRelative(0, -2, 0);
            } else if (i == 1) {
                block1 = event.getBlock();
                block2 = event.getBlock().getRelative(0, 1, 0);
                block3 = event.getBlock().getRelative(0, 2, 0);
            } else {
                block2 = event.getBlock();
                block1 = event.getBlock().getRelative(0, -1, 0);
                block3 = event.getBlock().getRelative(0, 1, 0);
            }

            matches = BlockStructure.matches(new BlockStructure(block1.getType(), block2.getType(), block3.getType()));
            if (matches) {
                match.add(block1);
                match.add(block2);
                match.add(block3);
                break;
            }
        }

        if (!matches) {
            return;
        }

        match.forEach(block -> block.setType(Material.AIR));
        IllusionerManager.OPTIONS.getSpawnSounds().forEach(optionedSound -> {
            if (optionedSound.isBroadcast()) {
                event.getBlock().getWorld().playSound(event.getBlock().getLocation(), optionedSound.getSound(), optionedSound.getVolume(), optionedSound.getPitch());
            } else {
                event.getPlayer().playSound(event.getBlock().getLocation(), optionedSound.getSound(), optionedSound.getVolume(), optionedSound.getPitch());
            }
        });

        EntityIllusionerProxy illusioner = ProxyUtils.getProxy(IllusionerHelperProxy.class).spawn(
                event.getBlock().getLocation(),
                IllusionerManager.OPTIONS.getMaxHealth(),
                IllusionerManager.OPTIONS.getAttackDamage(),
                IllusionerManager.OPTIONS.getName()
        );
        illusioner.createBossbar(this.getPlugin(), IllusionerManager.OPTIONS.getColor(), IllusionerManager.OPTIONS.getStyle());
    }

    /**
     * Called on vanilla illusioner spawn.
     *
     * @param event The event to listen for.
     */
    @EventHandler
    public void onExternalSpawn(@NotNull final EntitySpawnEvent event) {
        if (!(event.getEntity() instanceof Illusioner)) {
            return;
        }

        if (!IllusionerManager.OPTIONS.isOverride()) {
            return;
        }

        Illusioner illusioner = (Illusioner) event.getEntity();

        EntityIllusionerProxy internalIllusioner = ProxyUtils.getProxy(IllusionerHelperProxy.class).adapt(
                illusioner,
                illusioner.getLocation(),
                IllusionerManager.OPTIONS.getMaxHealth(),
                IllusionerManager.OPTIONS.getAttackDamage(),
                IllusionerManager.OPTIONS.getName()
        );

        if (internalIllusioner == null) {
            return;
        }
        internalIllusioner.createBossbar(this.getPlugin(), IllusionerManager.OPTIONS.getColor(), IllusionerManager.OPTIONS.getStyle());
    }
}

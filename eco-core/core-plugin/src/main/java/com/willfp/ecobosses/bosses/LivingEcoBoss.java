package com.willfp.ecobosses.bosses;

import com.willfp.eco.core.EcoPlugin;
import com.willfp.eco.core.PluginDependent;
import com.willfp.eco.core.scheduling.RunnableTask;
import com.willfp.eco.util.StringUtils;
import com.willfp.ecobosses.bosses.effects.Effect;
import com.willfp.ecobosses.bosses.tick.BossTicker;
import com.willfp.ecobosses.bosses.tick.tickers.BossBarTicker;
import com.willfp.ecobosses.bosses.tick.tickers.DeathTimeTicker;
import com.willfp.ecobosses.bosses.tick.tickers.NamePlaceholderTicker;
import com.willfp.ecobosses.bosses.tick.tickers.TargetTicker;
import com.willfp.ecobosses.bosses.util.obj.EquipmentPiece;
import com.willfp.ecobosses.bosses.util.obj.OptionedSound;
import lombok.Getter;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class LivingEcoBoss extends PluginDependent<EcoPlugin> {
    /**
     * The entity.
     */
    @Getter
    private LivingEntity entity;

    /**
     * The boss.
     */
    private final EcoBoss boss;

    /**
     * The boss tickers.
     */
    private final List<BossTicker> tickers;

    /**
     * The effects.
     */
    private final List<Effect> effects;

    /**
     * Create new living EcoBoss.
     *
     * @param plugin Instance of EcoBosses.
     * @param entity The entity.
     * @param boss   The boss.
     */
    public LivingEcoBoss(@NotNull final EcoPlugin plugin,
                         @NotNull final LivingEntity entity,
                         @NotNull final EcoBoss boss) {
        super(plugin);
        this.entity = entity;
        this.boss = boss;

        this.onSpawn();

        // Tickers
        this.tickers = new ArrayList<>();
        this.tickers.add(new NamePlaceholderTicker());
        this.tickers.add(new DeathTimeTicker());
        this.tickers.add(new TargetTicker(boss.getTargetMode(), boss.getTargetDistance()));
        if (boss.isBossbarEnabled()) {
            this.tickers.add(
                    new BossBarTicker(
                            BossBar.bossBar(
                                    StringUtils.toComponent(entity.getCustomName()),
                                    1,
                                    boss.getBossbarProperties().color(),
                                    boss.getBossbarProperties().style()
                            ),
                            this.getPlugin().getConfigYml().getInt("bossbar-radius")
                    )
            );
        }

        // Effects
        this.effects = new ArrayList<>();
        this.effects.addAll(boss.createEffects());

        AtomicLong currentTick = new AtomicLong(0);
        this.getPlugin().getRunnableFactory().create(runnable -> this.tick(currentTick.getAndAdd(1), runnable)).runTaskTimer(0, 1);
    }

    private void onSpawn() {
        entity.getPersistentDataContainer().set(this.getPlugin().getNamespacedKeyFactory().create("boss"), PersistentDataType.STRING, boss.getId());
        entity.setPersistent(true);
        entity.setRemoveWhenFarAway(false);

        if (boss.isGlowing()) entity.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 1, false, false, false));

        if (entity instanceof Ageable ageable) {
            if (boss.isBaby()) ageable.setBaby();
            else ageable.setAdult();
        }

        if (boss.getTimeToLive() > 0) {
            entity.setMetadata("death-time", this.getPlugin().getMetadataValueFactory().create(System.currentTimeMillis() + (boss.getTimeToLive() * 1000L)));
        }

        entity.setCustomName(boss.getDisplayName());
        entity.setCustomNameVisible(true);

        EntityEquipment equipment = entity.getEquipment();
        if (equipment != null) {
            EquipmentPiece head = boss.getEquipment().get(EquipmentSlot.HEAD);
            EquipmentPiece chest = boss.getEquipment().get(EquipmentSlot.CHEST);
            EquipmentPiece legs = boss.getEquipment().get(EquipmentSlot.LEGS);
            EquipmentPiece boots = boss.getEquipment().get(EquipmentSlot.FEET);
            EquipmentPiece hand = boss.getEquipment().get(EquipmentSlot.HAND);
            if (head != null) {
                equipment.setHelmet(head.itemStack(), true);
                equipment.setHelmetDropChance((float) head.chance());
            }
            if (chest != null) {
                equipment.setChestplate(chest.itemStack(), true);
                equipment.setChestplateDropChance((float) chest.chance());
            }
            if (legs != null) {
                equipment.setLeggings(legs.itemStack(), true);
                equipment.setLeggingsDropChance((float) legs.chance());
            }
            if (boots != null) {
                equipment.setBoots(boots.itemStack(), true);
                equipment.setBootsDropChance((float) boots.chance());
            }
            if (hand != null) {
                equipment.setItemInMainHand(hand.itemStack(), true);
                equipment.setItemInMainHandDropChance((float) hand.chance());
            }
        }

        AttributeInstance movementSpeed = entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        assert movementSpeed != null;
        movementSpeed.addModifier(new AttributeModifier(entity.getUniqueId(), "ecobosses-movement-multiplier", boss.getMovementSpeedMultiplier() - 1, AttributeModifier.Operation.MULTIPLY_SCALAR_1));

        AttributeInstance maxHealth = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.getModifiers().clear();
            maxHealth.setBaseValue(boss.getMaxHealth());
            entity.setHealth(maxHealth.getValue());
        }

        AttributeInstance followRange = entity.getAttribute(Attribute.GENERIC_FOLLOW_RANGE);
        if (followRange != null) {
            followRange.getModifiers().clear();
            followRange.setBaseValue(boss.getFollowRange());
        }

        AttributeInstance attackDamage = entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        if (attackDamage != null) {
            attackDamage.getModifiers().clear();
            attackDamage.setBaseValue(boss.getAttackDamage());
        }

        for (OptionedSound sound : boss.getSpawnSounds()) {
            entity.getWorld().playSound(entity.getLocation(), sound.sound(), sound.volume(), sound.pitch());
        }

        for (String spawnMessage : boss.getSpawnMessages()) {
            Bukkit.broadcastMessage(spawnMessage
                    .replace("%x%", StringUtils.internalToString(entity.getLocation().getBlockX()))
                    .replace("%y%", StringUtils.internalToString(entity.getLocation().getBlockY()))
                    .replace("%z%", StringUtils.internalToString(entity.getLocation().getBlockZ()))
            );
        }
    }

    private void tick(final long tick,
                      @NotNull final RunnableTask runnable) {
        if (entity == null || entity.isDead()) {
            for (BossTicker ticker : tickers) {
                ticker.onDeath(boss, entity, tick);
            }
            for (Effect effect : effects) {
                effect.onDeath(boss, entity, tick);
            }
            boss.removeLivingBoss(entity);
            runnable.cancel();
            return;
        }

        for (BossTicker ticker : tickers) {
            ticker.tick(boss, entity, tick);
        }
        for (Effect effect : effects) {
            effect.tick(boss, entity, tick);
        }
    }

    /**
     * Handle an attack to the player.
     *
     * @param player The player.
     */
    public void handleAttack(@NotNull final Player player) {
        for (OptionedSound sound : boss.getInjureSounds()) {
            player.getWorld().playSound(entity.getLocation(), sound.sound(), sound.volume(), sound.pitch());
        }

        for (Effect effect : effects) {
            effect.onAttack(boss, entity, player);
        }
    }
}

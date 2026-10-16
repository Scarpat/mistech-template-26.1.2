package com.mistech.mistechmod.item.mithril;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.registry.MistechTags;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Bônus de conjunto dos níveis acima do mitril (somam com o do mitril e entre si; vale o nível MAIS BAIXO das 4 peças,
 * todas carregadas — ex.: 3 de oricalco + 1 de adamantina = conjunto de Adamantina).
 * <ul>
 * <li><b>Adamantina — Baluarte:</b> +4 de vida, −15% de dano físico, sem repulsão agachado.</li>
 * <li><b>Oricalco — Astral:</b> vida +6 no total; à noite +15% de dano e Visão Noturna.</li>
 * <li><b>Oricalco Puro — Solar:</b> vida +8 no total; de dia Regeneração I; escudo de 4 corações que volta
 * depois de 30 s sem tomar dano; +50% de dano em mortos-vivos.</li>
 * </ul>
 * (As runas valendo ×1,25/×1,5 ficam por peça, em {@link GearTier#runeMultiplier()}.)
 */
@EventBusSubscriber(modid = Mistech.MODID)
public final class GearSetBonus {
    public static final Identifier SET_HEALTH = id("gear_set_health");
    public static final Identifier SET_ABSORPTION = id("gear_set_absorption");

    public static final double ADAMANTINE_HEALTH = 4.0;
    public static final double ORICHALCUM_HEALTH = 6.0;
    public static final double PURE_HEALTH = 8.0;
    public static final float BULWARK_MULTIPLIER = 0.85F;
    public static final float ASTRAL_NIGHT_DAMAGE = 1.15F;
    public static final float SOLAR_UNDEAD_DAMAGE = 1.5F;
    public static final float SOLAR_SHIELD = 8.0F;
    public static final int SOLAR_SHIELD_DELAY = 30 * 20;

    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    /** Último tick (gameTime) em que o jogador tomou dano — para o escudo Solar. */
    private static final Map<UUID, Long> LAST_HURT = new HashMap<>();

    private GearSetBonus() {}

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Mistech.MODID, path);
    }

    /** Nível do conjunto vestido (o mais baixo das 4 peças), ou null se não for conjunto completo carregado. */
    public static @Nullable GearTier setTier(LivingEntity entity) {
        if (!MithrilEvents.hasFullSet(entity)) {
            return null;
        }
        GearTier lowest = GearTier.PURE_ORICHALCUM;
        for (EquipmentSlot slot : ARMOR) {
            GearTier tier = MithrilGear.tier(entity.getItemBySlot(slot));
            if (tier.ordinal() < lowest.ordinal()) {
                lowest = tier;
            }
        }
        return lowest;
    }

    public static boolean hasSet(LivingEntity entity, GearTier atLeast) {
        GearTier tier = setTier(entity);
        return tier != null && tier.atLeast(atLeast);
    }

    private static double healthFor(@Nullable GearTier tier) {
        if (tier == null) {
            return 0.0;
        }
        return switch (tier) {
            case MITHRIL -> 0.0;
            case ADAMANTINE -> ADAMANTINE_HEALTH;
            case ORICHALCUM -> ORICHALCUM_HEALTH;
            case PURE_ORICHALCUM -> PURE_HEALTH;
        };
    }

    // ------------------------------------------------------------------ contínuos

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel level) || player.tickCount % 10 != 3) {
            return;
        }
        GearTier tier = setTier(player);
        setModifier(player, Attributes.MAX_HEALTH, SET_HEALTH, healthFor(tier));
        boolean solar = tier == GearTier.PURE_ORICHALCUM;
        setModifier(player, Attributes.MAX_ABSORPTION, SET_ABSORPTION, solar ? SOLAR_SHIELD : 0.0);
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
        if (tier == null) {
            return;
        }
        boolean night = level.isDarkOutside();
        if (tier.atLeast(GearTier.ORICHALCUM) && night) {
            refresh(player, MobEffects.NIGHT_VISION, 0, 320, 220);
        }
        if (solar) {
            if (!night) {
                refresh(player, MobEffects.REGENERATION, 0, 100, 50);
            }
            long last = LAST_HURT.getOrDefault(player.getUUID(), 0L);
            if (level.getGameTime() - last >= SOLAR_SHIELD_DELAY && player.getAbsorptionAmount() < SOLAR_SHIELD) {
                boolean wasEmpty = player.getAbsorptionAmount() <= 0.0F;
                player.setAbsorptionAmount(SOLAR_SHIELD);
                if (wasEmpty) {
                    level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0, player.getZ(), 14, 0.4, 0.6, 0.4, 0.03);
                    level.playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.4F, 1.8F);
                }
            }
        }
    }

    private static void setModifier(Player player, Holder<Attribute> attribute, Identifier id, double value) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        AttributeModifier current = instance.getModifier(id);
        if (value <= 0.0) {
            if (current != null) {
                instance.removeModifier(id);
            }
        } else if (current == null || current.amount() != value) {
            instance.removeModifier(id);
            instance.addTransientModifier(new AttributeModifier(id, value, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    private static void refresh(Player player, Holder<MobEffect> effect, int amplifier, int duration, int below) {
        MobEffectInstance current = player.getEffect(effect);
        if (current == null || current.getDuration() < below && current.getAmplifier() <= amplifier) {
            player.addEffect(new MobEffectInstance(effect, duration, amplifier, true, false, true));
        }
    }

    @SubscribeEvent
    static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_HURT.remove(event.getEntity().getUUID());
    }

    // ------------------------------------------------------------------ dano

    private static @Nullable Player attackingPlayer(DamageSource source) {
        Entity direct = source.getDirectEntity();
        Entity owner = source.getEntity();
        return owner instanceof Player player && (direct == owner || direct instanceof net.minecraft.world.entity.projectile.Projectile) ? player : null;
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (!(target.level() instanceof ServerLevel level)) {
            return;
        }
        DamageSource source = event.getSource();
        float amount = event.getAmount();

        // defesa: Baluarte
        if (target instanceof Player && hasSet(target, GearTier.ADAMANTINE) && !source.is(MistechTags.MAGIC_DAMAGE)
                && !source.is(DamageTypeTags.BYPASSES_ARMOR) && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            amount *= BULWARK_MULTIPLIER;
        }

        // ataque
        Player attacker = attackingPlayer(source);
        if (attacker != null && attacker != target) {
            GearTier tier = setTier(attacker);
            if (tier != null) {
                if (tier.atLeast(GearTier.ORICHALCUM) && level.isDarkOutside()) {
                    amount *= ASTRAL_NIGHT_DAMAGE;
                }
                if (tier == GearTier.PURE_ORICHALCUM && target.is(EntityTypeTags.UNDEAD)) {
                    amount *= SOLAR_UNDEAD_DAMAGE;
                    level.sendParticles(ParticleTypes.END_ROD, target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(),
                            6, 0.25, 0.3, 0.25, 0.04);
                }
            }
        }
        event.setAmount(amount);
    }

    @SubscribeEvent
    static void onDamaged(LivingDamageEvent.Post event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide() && event.getInflictedDamage() > 0.0F) {
            LAST_HURT.put(player.getUUID(), player.level().getGameTime());
        }
    }

    /** Baluarte: agachado, nada te empurra. */
    @SubscribeEvent
    static void onKnockBack(LivingKnockBackEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player && entity.isCrouching() && hasSet(entity, GearTier.ADAMANTINE)) {
            event.setCanceled(true);
        }
    }
}

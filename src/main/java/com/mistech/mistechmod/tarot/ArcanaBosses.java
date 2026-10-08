package com.mistech.mistechmod.tarot;

import java.util.EnumMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

/**
 * Quem aparece no Plano Arcano para cada arcano. Os chefes de verdade se registram com {@link #register};
 * enquanto um arcano não tem chefe, entra um substituto provisório (Ilusionista reforçado, com o nome do arcano).
 * O {@link ArcanePlane} cuida do resto: barra de vida, manter na arena, vitória e derrota.
 */
public final class ArcanaBosses {
    /**
     * Cria (sem adicionar ao mundo) o chefe do arcano. {@code spawn} = {@link ArenaLayout#bossSpawn}; a luta dá o centro
     * da arena, o arcano e o grupo; {@code challenger} = um jogador do grupo.
     */
    @FunctionalInterface
    public interface Factory {
        @Nullable LivingEntity create(ServerLevel level, ArcanePlane.Fight fight, Vec3 spawn, ServerPlayer challenger);
    }

    private static final Map<TarotArcana, Factory> FACTORIES = new EnumMap<>(TarotArcana.class);
    private static final Map<TarotArcana, ArenaLayout> ARENAS = new EnumMap<>(TarotArcana.class);

    /** Vida do substituto provisório: 100 + 10 por número do arcano. */
    private static final double PLACEHOLDER_BASE_HEALTH = 100.0;

    private ArcanaBosses() {}

    public static void register(TarotArcana arcana, Factory factory) {
        FACTORIES.put(arcana, factory);
    }

    /** Arena própria do arcano (senão vale a {@link CircleArena}). */
    public static void registerArena(TarotArcana arcana, ArenaLayout layout) {
        ARENAS.put(arcana, layout);
    }

    public static ArenaLayout arena(TarotArcana arcana) {
        return ARENAS.getOrDefault(arcana, CircleArena.INSTANCE);
    }

    public static boolean hasRealBoss(TarotArcana arcana) {
        return FACTORIES.containsKey(arcana);
    }

    /** Cria o chefe (o de verdade ou o provisório). */
    static @Nullable LivingEntity create(ServerLevel level, ArcanePlane.Fight fight, Vec3 spawn, ServerPlayer challenger) {
        TarotArcana arcana = fight.arcana();
        Factory factory = FACTORIES.get(arcana);
        LivingEntity boss = factory != null ? factory.create(level, fight, spawn, challenger) : placeholder(level, spawn, arcana);
        if (boss != null) {
            boss.addTag(ArcanePlane.BOSS_TAG);
            if (boss instanceof Mob mob) {
                mob.setPersistenceRequired();
                mob.setTarget(challenger);
            }
        }
        return boss;
    }

    private static @Nullable LivingEntity placeholder(ServerLevel level, Vec3 center, TarotArcana arcana) {
        Mob mob = EntityType.ILLUSIONER.create(level, EntitySpawnReason.EVENT);
        if (mob == null) {
            return null;
        }
        mob.snapTo(center.x, center.y, center.z, 0.0F, 0.0F);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(center)), EntitySpawnReason.EVENT, null);
        AttributeInstance health = mob.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.setBaseValue(PLACEHOLDER_BASE_HEALTH + arcana.number() * 10.0);
        }
        mob.setHealth(mob.getMaxHealth());
        mob.setCustomName(arcana.displayName().copy().withStyle(ChatFormatting.LIGHT_PURPLE)
                .append(Component.literal(" ").append(Component.translatable("entity.mistech.arcana_placeholder"))
                        .withStyle(ChatFormatting.DARK_GRAY)));
        mob.setCustomNameVisible(true);
        return mob;
    }
}

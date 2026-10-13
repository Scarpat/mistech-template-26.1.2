package com.mistech.mistechmod.folklore;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

/** Ações dos rituais do folclore (chamadas por CelestialRituals quando a ação não é celeste). */
public final class FolkloreRituals {
    public static final double CURE_RADIUS = 3.5;

    private FolkloreRituals() {}

    public static @Nullable Component check(String action, ServerLevel level, BlockPos pos, @Nullable Player player) {
        switch (action) {
            case "summon_raven", "summon_curupira", "summon_headless_mule" -> {
                if (player == null) {
                    return Component.translatable("message.mistech.ritual.no_player");
                }
                UUID id = player.getUUID();
                int owned = switch (action) {
                    case "summon_raven" -> RavenEntity.countOwned(level, id);
                    case "summon_curupira" -> FolkFamiliar.countOwned(level, FolkloreContent.CURUPIRA.get(), id);
                    default -> level.getEntities(FolkloreContent.HEADLESS_MULE.get(), m -> {
                        EntityReference<LivingEntity> ref = m.getOwnerReference();
                        return ref != null && id.equals(ref.getUUID());
                    }).size();
                };
                if (owned > 0) {
                    String key = switch (action) {
                        case "summon_raven" -> "entity.mistech.raven";
                        case "summon_curupira" -> "entity.mistech.curupira";
                        default -> "entity.mistech.headless_mule";
                    };
                    return Component.translatable("message.mistech.ritual.familiar_exists", Component.translatable(key));
                }
            }
            case "awaken_boitata" -> {
                if (!level.getEntities(FolkloreContent.BOITATA.get(), b -> b.distanceToSqr(pos.getCenter()) < 160 * 160).isEmpty()) {
                    return Component.translatable("message.mistech.boitata.already");
                }
            }
            default -> {
                if (com.mistech.mistechmod.lore.LoreRituals.handles(action)) {
                    return com.mistech.mistechmod.lore.LoreRituals.check(action, level, pos, player);
                }
                return com.mistech.mistechmod.beyond.BeyondRituals.check(action, level, pos, player);
            }
        }
        return null;
    }

    public static boolean run(String action, ServerLevel level, BlockPos pos, @Nullable Player player) {
        switch (action) {
            case "summon_raven" -> {
                RavenEntity raven = FolkloreContent.RAVEN.get().create(level, EntitySpawnReason.MOB_SUMMONED);
                if (raven != null && player != null) {
                    raven.snapTo(pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, level.getRandom().nextFloat() * 360.0F, 0.0F);
                    raven.bindTo(player);
                    level.addFreshEntity(raven);
                    poof(level, pos);
                }
            }
            case "summon_curupira" -> {
                CurupiraEntity curupira = FolkloreContent.CURUPIRA.get().create(level, EntitySpawnReason.MOB_SUMMONED);
                if (curupira != null && player != null) {
                    curupira.snapTo(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, level.getRandom().nextFloat() * 360.0F, 0.0F);
                    curupira.bindTo(player);
                    level.addFreshEntity(curupira);
                    level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, 30, 0.5, 0.6, 0.5, 0.0);
                    level.playSound(null, pos, SoundEvents.NOTE_BLOCK_FLUTE.value(), SoundSource.NEUTRAL, 1.0F, 1.6F);
                }
            }
            case "summon_headless_mule" -> {
                HeadlessMuleEntity mule = FolkloreContent.HEADLESS_MULE.get().create(level, EntitySpawnReason.MOB_SUMMONED);
                if (mule != null && player != null) {
                    mule.snapTo(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, player.getYRot(), 0.0F);
                    mule.bindTo(player);
                    level.addFreshEntity(mule);
                    level.sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, 60, 0.8, 1.0, 0.8, 0.05);
                    level.playSound(null, pos, SoundEvents.SKELETON_HORSE_AMBIENT, SoundSource.NEUTRAL, 1.5F, 0.6F);
                    player.sendSystemMessage(Component.translatable("message.mistech.headless_mule.summoned").withStyle(ChatFormatting.GOLD));
                }
            }
            case "awaken_boitata" -> {
                BoitataEntity boss = FolkloreContent.BOITATA.get().create(level, EntitySpawnReason.MOB_SUMMONED);
                if (boss != null) {
                    boss.snapTo(pos.getX() + 0.5, pos.getY() + 8.0, pos.getZ() + 0.5, 0.0F, 0.0F);
                    boss.setHome(pos);
                    level.addFreshEntity(boss);
                    level.sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 4.0, pos.getZ() + 0.5, 150, 1.5, 4.0, 1.5, 0.1);
                    level.playSound(null, pos, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 3.0F, 0.6F);
                    for (Player p : level.getEntitiesOfClass(Player.class, new AABB(pos).inflate(96))) {
                        p.sendSystemMessage(Component.translatable("message.mistech.boitata.awaken").withStyle(ChatFormatting.GOLD));
                    }
                }
            }
            case "cure_lycanthropy" -> {
                List<Player> players = new ArrayList<>(level.getEntitiesOfClass(Player.class, new AABB(pos).inflate(CURE_RADIUS, 2.0, CURE_RADIUS)));
                if (player != null && !players.contains(player)) {
                    players.add(player);
                }
                for (Player p : players) {
                    Lycanthropy.cure(p);
                    level.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1.0, p.getZ(), 30, 0.3, 0.8, 0.3, 0.05);
                }
            }
            default -> {
                if (com.mistech.mistechmod.lore.LoreRituals.handles(action)) {
                    return com.mistech.mistechmod.lore.LoreRituals.run(action, level, pos, player);
                }
                return com.mistech.mistechmod.beyond.BeyondRituals.run(action, level, pos, player);
            }
        }
        return true;
    }

    private static void poof(ServerLevel level, BlockPos pos) {
        level.sendParticles(ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, 20, 0.3, 0.3, 0.3, 0.02);
        level.sendParticles(ParticleTypes.SQUID_INK, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, 20, 0.4, 0.4, 0.4, 0.05);
    }
}

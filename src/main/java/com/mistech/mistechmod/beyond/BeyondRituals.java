package com.mistech.mistechmod.beyond;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

/** Ações dos rituais do Deep Dark e dos Quatro Corações (chamadas por FolkloreRituals quando a ação não é dele). */
public final class BeyondRituals {
    public static final long PIGLIN_PACT_TICKS = 3L * 24000L;

    private BeyondRituals() {}

    public static @Nullable Component check(String action, ServerLevel level, BlockPos pos, @Nullable Player player) {
        switch (action) {
            case "summon_echoling" -> {
                if (player == null) {
                    return Component.translatable("message.mistech.ritual.no_player");
                }
                if (EcholingEntity.countOwned(level, player.getUUID()) > 0) {
                    return Component.translatable("message.mistech.ritual.familiar_exists", Component.translatable("entity.mistech.echoling"));
                }
            }
            case "summon_guardian" -> {
                if (!level.getEntitiesOfClass(Warden.class, new AABB(pos).inflate(64), EchoEvents::isGuardian).isEmpty()) {
                    return Component.translatable("message.mistech.guardian.already");
                }
            }
            case "summon_runic_strider" -> {
                if (player == null) {
                    return Component.translatable("message.mistech.ritual.no_player");
                }
                if (RunicStriderEntity.countOwned(level, player.getUUID()) > 0) {
                    return Component.translatable("message.mistech.ritual.familiar_exists", Component.translatable("entity.mistech.runic_strider"));
                }
            }
            case "piglin_pact" -> {
                if (player == null) {
                    return Component.translatable("message.mistech.ritual.no_player");
                }
            }
            default -> {
            }
        }
        return null;
    }

    public static boolean run(String action, ServerLevel level, BlockPos pos, @Nullable Player player) {
        switch (action) {
            case "summon_echoling" -> {
                EcholingEntity echoling = BeyondContent.ECHOLING.get().create(level, EntitySpawnReason.MOB_SUMMONED);
                if (echoling != null && player != null) {
                    echoling.snapTo(pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, level.getRandom().nextFloat() * 360.0F, 0.0F);
                    echoling.bindTo(player);
                    level.addFreshEntity(echoling);
                    level.sendParticles(ParticleTypes.SCULK_SOUL, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, 30, 0.4, 0.4, 0.4, 0.05);
                    level.playSound(null, pos, SoundEvents.SCULK_CATALYST_BLOOM, SoundSource.NEUTRAL, 1.5F, 1.2F);
                }
            }
            case "summon_guardian" -> summonGuardian(level, pos, player);
            case "summon_runic_strider" -> {
                RunicStriderEntity strider = PaleNetherContent.RUNIC_STRIDER.get().create(level, EntitySpawnReason.MOB_SUMMONED);
                if (strider != null && player != null) {
                    strider.snapTo(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, player.getYRot(), 0.0F);
                    strider.bindTo(player);
                    level.addFreshEntity(strider);
                    level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, 40, 0.5, 0.6, 0.5, 0.03);
                    level.playSound(null, pos, SoundEvents.STRIDER_HAPPY, SoundSource.NEUTRAL, 1.5F, 0.8F);
                }
            }
            case "piglin_pact" -> {
                // 3 dias para quem fez o ritual e quem estiver a 8 blocos
                long until = level.getGameTime() + PIGLIN_PACT_TICKS;
                for (Player p : level.getEntitiesOfClass(Player.class, new AABB(pos).inflate(8.0))) {
                    p.setData(PaleNetherContent.PIGLIN_PACT, until);
                    p.sendSystemMessage(Component.translatable("message.mistech.piglin_pact.sealed").withStyle(ChatFormatting.GOLD));
                }
                if (player != null && player.distanceToSqr(pos.getCenter()) > 64.0) {
                    player.setData(PaleNetherContent.PIGLIN_PACT, until);
                }
                level.sendParticles(ParticleTypes.WAX_ON, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, 40, 0.6, 0.6, 0.6, 0.1);
                level.playSound(null, pos, SoundEvents.PIGLIN_ADMIRING_ITEM, SoundSource.NEUTRAL, 1.5F, 1.0F);
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    /** O Guardião sai da terra a 4 blocos do altar, já furioso com quem fez o ritual. */
    private static void summonGuardian(ServerLevel level, BlockPos altar, @Nullable Player player) {
        Warden warden = EntityType.WARDEN.create(level, EntitySpawnReason.TRIGGERED);
        if (warden == null) {
            return;
        }
        BlockPos at = altar;
        if (player != null) {
            // do lado oposto a quem invocou, para dar tempo de reagir
            double dx = altar.getX() + 0.5 - player.getX();
            double dz = altar.getZ() + 0.5 - player.getZ();
            double len = Math.max(0.01, Math.sqrt(dx * dx + dz * dz));
            at = altar.offset((int) Math.round(dx / len * 4.0), 0, (int) Math.round(dz / len * 4.0));
        }
        warden.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, level.getRandom().nextFloat() * 360.0F, 0.0F);
        warden.finalizeSpawn(level, level.getCurrentDifficultyAt(at), EntitySpawnReason.TRIGGERED, null);
        warden.getPersistentData().putBoolean(EchoEvents.GUARDIAN_TAG, true);
        warden.setPersistenceRequired();
        warden.setCustomName(Component.translatable("entity.mistech.guardian"));
        if (player != null) {
            warden.increaseAngerAt(player, 150, false);
        }
        level.addFreshEntity(warden);
        Warden.applyDarknessAround(level, altar.getCenter(), null, 40);
        level.playSound(null, altar, SoundEvents.WARDEN_EMERGE, SoundSource.HOSTILE, 3.0F, 0.8F);
        for (Player p : level.getEntitiesOfClass(Player.class, new AABB(altar).inflate(48))) {
            p.sendSystemMessage(Component.translatable("message.mistech.guardian.summoned").withStyle(ChatFormatting.DARK_AQUA));
        }
    }
}

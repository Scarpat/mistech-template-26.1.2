package com.mistech.mistechmod.lore;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

/** Rituais do folclore novo (leva 2): despertar a Cuca e o Mapinguari; e a Cuca que vem buscar quem não dorme. */
public final class LoreRituals {
    /** 3 noites (dias de jogo) sem dormir. */
    public static final int SLEEPLESS_TICKS = 72000;

    private LoreRituals() {}

    public static boolean handles(String action) {
        return action.equals("awaken_cuca") || action.equals("awaken_mapinguari");
    }

    public static @Nullable Component check(String action, ServerLevel level, BlockPos pos, @Nullable Player player) {
        if (action.equals("awaken_cuca") && !level.getEntitiesOfClass(CucaEntity.class, new AABB(pos).inflate(128)).isEmpty()) {
            return Component.translatable("message.mistech.cuca.already");
        }
        if (action.equals("awaken_mapinguari") && !level.getEntitiesOfClass(MapinguariEntity.class, new AABB(pos).inflate(128)).isEmpty()) {
            return Component.translatable("message.mistech.mapinguari.already");
        }
        return null;
    }

    public static boolean run(String action, ServerLevel level, BlockPos pos, @Nullable Player player) {
        if (action.equals("awaken_cuca")) {
            Mob cuca = spawn(level, LoreContent.CUCA.get(), pos.offset(0, 1, 3));
            if (cuca == null) {
                return false;
            }
            level.sendParticles(ParticleTypes.WITCH, pos.getX() + 0.5, pos.getY() + 2, pos.getZ() + 3.5, 120, 1.0, 1.5, 1.0, 0.2);
            level.playSound(null, pos, SoundEvents.WITCH_CELEBRATE, SoundSource.HOSTILE, 3.0F, 0.5F);
            announce(level, pos, "message.mistech.cuca.awaken", ChatFormatting.DARK_PURPLE);
            return true;
        }
        if (action.equals("awaken_mapinguari")) {
            Mob boss = spawn(level, LoreContent.MAPINGUARI.get(), pos.offset(0, 1, 5));
            if (boss == null) {
                return false;
            }
            level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.getX() + 0.5, pos.getY() + 2, pos.getZ() + 5.5, 60, 1.5, 1.5, 1.5, 0.02);
            level.playSound(null, pos, SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 4.0F, 0.5F);
            announce(level, pos, "message.mistech.mapinguari.awaken", ChatFormatting.DARK_GREEN);
            return true;
        }
        return false;
    }

    private static <T extends Mob> @Nullable T spawn(ServerLevel level, EntityType<T> type, BlockPos at) {
        T mob = type.create(level, EntitySpawnReason.MOB_SUMMONED);
        if (mob == null) {
            return null;
        }
        mob.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, level.getRandom().nextFloat() * 360, 0);
        level.addFreshEntity(mob);
        return mob;
    }

    private static void announce(ServerLevel level, BlockPos pos, String key, ChatFormatting color) {
        for (Player p : level.getEntitiesOfClass(Player.class, new AABB(pos).inflate(96))) {
            p.sendSystemMessage(Component.translatable(key).withStyle(color));
        }
    }

    /**
     * "Dorme neném que a Cuca vem pegar": quem passa 3 noites sem dormir e está na Mata Ancestral à noite tem uma chance
     * (checada a cada ~10 s) de a Cuca aparecer a ~20 blocos.
     */
    static void sleeplessTick(ServerPlayer player) {
        ServerLevel level = player.level();
        if (player.isCreative() || player.isSpectator() || !level.isDarkOutside()
                || !level.getBiome(player.blockPosition()).is(LoreContent.ANCESTRAL_FOREST)
                || player.getStats().getValue(Stats.CUSTOM.get(Stats.TIME_SINCE_REST)) < SLEEPLESS_TICKS
                || player.getRandom().nextInt(30) != 0
                || !level.getEntitiesOfClass(CucaEntity.class, player.getBoundingBox().inflate(96)).isEmpty()) {
            return;
        }
        for (int i = 0; i < 12; i++) {
            double a = player.getRandom().nextDouble() * Math.PI * 2;
            double d = 18 + player.getRandom().nextDouble() * 6;
            BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    BlockPos.containing(player.getX() + Math.cos(a) * d, 0, player.getZ() + Math.sin(a) * d));
            if (Math.abs(top.getY() - player.getY()) > 10) {
                continue;
            }
            CucaEntity cuca = LoreContent.CUCA.get().create(level, EntitySpawnReason.EVENT);
            if (cuca == null) {
                return;
            }
            cuca.snapTo(top.getX() + 0.5, top.getY(), top.getZ() + 0.5, 0, 0);
            cuca.setTarget(player);
            level.addFreshEntity(cuca);
            level.playSound(null, player.blockPosition(), SoundEvents.WITCH_AMBIENT, SoundSource.HOSTILE, 3.0F, 0.5F);
            player.sendSystemMessage(Component.translatable("message.mistech.cuca.sleepless").withStyle(ChatFormatting.DARK_PURPLE));
            return;
        }
    }
}

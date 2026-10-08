package com.mistech.mistechmod.ritual;

import java.util.HashMap;
import java.util.Map;

import com.mistech.mistechmod.Mistech;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;

/** Selo de Proteção: altares com selo ativo impedem que mobs hostis nasçam naturalmente em volta. */
@EventBusSubscriber(modid = Mistech.MODID)
public final class RitualWards {
    public static final int RADIUS = 48;

    /** dimensão → (altar → gameTime em que o selo acaba). */
    private static final Map<ResourceKey<Level>, Map<BlockPos, Long>> WARDS = new HashMap<>();

    private RitualWards() {}

    public static void set(Level level, BlockPos altar, long until) {
        if (until <= level.getGameTime()) {
            remove(level, altar);
            return;
        }
        WARDS.computeIfAbsent(level.dimension(), k -> new HashMap<>()).put(altar.immutable(), until);
    }

    public static void remove(Level level, BlockPos altar) {
        Map<BlockPos, Long> map = WARDS.get(level.dimension());
        if (map != null) {
            map.remove(altar);
        }
    }

    public static boolean isWarded(Level level, double x, double z) {
        Map<BlockPos, Long> map = WARDS.get(level.dimension());
        if (map == null || map.isEmpty()) {
            return false;
        }
        long now = level.getGameTime();
        map.values().removeIf(until -> until <= now);
        for (BlockPos pos : map.keySet()) {
            double dx = pos.getX() + 0.5 - x;
            double dz = pos.getZ() + 0.5 - z;
            if (dx * dx + dz * dz <= RADIUS * RADIUS) {
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent
    static void onSpawnCheck(MobSpawnEvent.PositionCheck event) {
        EntitySpawnReason reason = event.getSpawnType();
        if (!(event.getEntity() instanceof Enemy)
                || (reason != EntitySpawnReason.NATURAL && reason != EntitySpawnReason.CHUNK_GENERATION && reason != EntitySpawnReason.PATROL)) {
            return;
        }
        if (isWarded(event.getEntity().level(), event.getX(), event.getZ())) {
            event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
        }
    }
}

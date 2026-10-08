package com.mistech.mistechmod.tarot;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Utilidades para construir arenas no Plano Arcano. */
public final class ArenaTools {
    /**
     * Raio que toda arena limpa ao ser construída. Igual para todas, porque o mesmo "espaço" (slot) pode receber
     * arcanos diferentes em lutas seguidas e a arena nova precisa apagar a anterior inteira.
     */
    public static final int MAX_CLEAR_RADIUS = 36;
    public static final int CLEAR_BELOW = 24;
    public static final int CLEAR_ABOVE = 32;

    private ArenaTools() {}

    /** Apaga tudo em volta do centro (sobras de outra arena). */
    public static void clear(ServerLevel level, BlockPos center, ArenaLayout layout) {
        int r = Math.max(layout.clearRadius(), MAX_CLEAR_RADIUS);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                for (int dy = -CLEAR_BELOW; dy <= CLEAR_ABOVE; dy++) {
                    pos.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    if (!level.getBlockState(pos).isAir()) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
    }

    /** Ruído determinístico 0..1 a partir de coordenadas (para ilhas irregulares sempre iguais). */
    public static double noise(int x, int z, int seed) {
        long h = x * 341873128712L + z * 132897987541L + seed * 9137L;
        h = (h ^ (h >>> 13)) * 0x5DEECE66DL;
        h ^= h >>> 17;
        return (h & 0xFFFF) / 65535.0;
    }
}

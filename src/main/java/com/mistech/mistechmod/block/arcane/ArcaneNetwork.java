package com.mistech.mistechmod.block.arcane;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Registro (só no servidor) dos Receptores Arcanos carregados em cada mundo.
 * Os Pontos de Rede consultam essa lista em vez de varrer os blocos em volta todo tick.
 */
public final class ArcaneNetwork {
    /** Alcance entre Ponto e Receptor (distância em linha reta, em blocos). */
    public static final double RANGE = 10.0;

    private static final Map<Level, Set<BlockPos>> RECEIVERS = new WeakHashMap<>();

    private ArcaneNetwork() {}

    public static synchronized void addReceiver(Level level, BlockPos pos) {
        RECEIVERS.computeIfAbsent(level, l -> new HashSet<>()).add(pos.immutable());
    }

    public static synchronized void removeReceiver(Level level, BlockPos pos) {
        Set<BlockPos> set = RECEIVERS.get(level);
        if (set != null) {
            set.remove(pos);
        }
    }

    /** Receptores ao alcance do ponto (cópia, pode ser modificada à vontade). */
    public static synchronized Set<BlockPos> receiversNear(Level level, BlockPos node) {
        Set<BlockPos> result = new HashSet<>();
        Set<BlockPos> set = RECEIVERS.get(level);
        if (set != null) {
            double rangeSqr = RANGE * RANGE;
            for (BlockPos pos : set) {
                if (pos.distSqr(node) <= rangeSqr) {
                    result.add(pos);
                }
            }
        }
        return result;
    }
}

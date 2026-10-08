package com.mistech.mistechmod.ether;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import com.mistech.mistechmod.block.arcane.ArcaneNetwork;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Malha de Pilares de Transmissão (só no servidor). Um Ponto Arcano a até {@link ArcaneNetwork#RANGE} de um pilar
 * alcança todos os Receptores Arcanos perto de qualquer pilar ligado a ele (direta ou indiretamente).
 */
public final class PylonMesh {
    /** Distância máxima entre dois pilares ligados. */
    public static final double LINK_RANGE = 48.0;
    public static final int MAX_LINKS = 4;
    /** Teto por Receptor alcançado pela malha (por Ponto, por tick). */
    public static final int TRANSFER_PER_RECEIVER = 10_000;
    private static final int MAX_PYLONS = 256;

    private static final Map<Level, Set<BlockPos>> PYLONS = new WeakHashMap<>();

    private PylonMesh() {}

    public static synchronized void add(Level level, BlockPos pos) {
        PYLONS.computeIfAbsent(level, l -> new HashSet<>()).add(pos.immutable());
    }

    public static synchronized void remove(Level level, BlockPos pos) {
        Set<BlockPos> set = PYLONS.get(level);
        if (set != null) {
            set.remove(pos);
        }
    }

    /** Receptores Arcanos alcançados por um Ponto em {@code node} através da malha. */
    public static synchronized Set<BlockPos> receiversVia(Level level, BlockPos node) {
        Set<BlockPos> result = new HashSet<>();
        Set<BlockPos> all = PYLONS.get(level);
        if (all == null || all.isEmpty()) {
            return result;
        }
        double range = ArcaneNetwork.RANGE * ArcaneNetwork.RANGE;
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> seen = new HashSet<>();
        for (BlockPos pylon : all) {
            if (pylon.distSqr(node) <= range && seen.add(pylon)) {
                queue.add(pylon);
            }
        }
        while (!queue.isEmpty() && seen.size() <= MAX_PYLONS) {
            BlockPos pylon = queue.poll();
            if (!(level.getBlockEntity(pylon) instanceof TransmissionPylonBlockEntity be)) {
                continue;
            }
            result.addAll(ArcaneNetwork.receiversNear(level, pylon));
            for (BlockPos next : be.links()) {
                if (all.contains(next) && seen.add(next)) {
                    queue.add(next);
                }
            }
        }
        return result;
    }
}

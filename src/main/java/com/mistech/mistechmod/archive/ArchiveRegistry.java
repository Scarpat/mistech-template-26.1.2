package com.mistech.mistechmod.archive;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Onde estão os Núcleos, Relés e Selos do Arquivo carregados (só no servidor). Sem cabos: um bloco faz parte do
 * Arquivo de um Núcleo se estiver a até {@link #RANGE} blocos do Núcleo ou de um Relé ligado a ele (Relés encadeiam).
 */
public final class ArchiveRegistry {
    public static final double RANGE = 16.0;

    public enum Kind { CORE, RELAY, SEAL }

    private static final Map<Level, Map<Kind, Set<BlockPos>>> MAP = new WeakHashMap<>();

    private ArchiveRegistry() {}

    private static Set<BlockPos> set(Level level, Kind kind) {
        return MAP.computeIfAbsent(level, l -> new java.util.EnumMap<>(Kind.class)).computeIfAbsent(kind, k -> new HashSet<>());
    }

    public static synchronized void add(Level level, Kind kind, BlockPos pos) {
        set(level, kind).add(pos.immutable());
    }

    public static synchronized void remove(Level level, Kind kind, BlockPos pos) {
        set(level, kind).remove(pos);
    }

    /** Núcleo + Relés alcançados a partir dele. */
    public static synchronized Set<BlockPos> reach(Level level, BlockPos core) {
        Set<BlockPos> nodes = new HashSet<>();
        nodes.add(core);
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(core);
        double r = RANGE * RANGE;
        Set<BlockPos> relays = set(level, Kind.RELAY);
        while (!queue.isEmpty() && nodes.size() < 128) {
            BlockPos from = queue.poll();
            for (BlockPos relay : relays) {
                if (!nodes.contains(relay) && relay.distSqr(from) <= r) {
                    nodes.add(relay);
                    queue.add(relay);
                }
            }
        }
        return nodes;
    }

    public static boolean inReach(Set<BlockPos> nodes, BlockPos pos) {
        double r = RANGE * RANGE;
        for (BlockPos node : nodes) {
            if (node.distSqr(pos) <= r) {
                return true;
            }
        }
        return false;
    }

    /** O Núcleo cujo Arquivo alcança {@code pos} (o mais perto, se houver mais de um). */
    public static synchronized @Nullable ArchiveCoreBlockEntity coreFor(Level level, BlockPos pos) {
        ArchiveCoreBlockEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos core : Set.copyOf(set(level, Kind.CORE))) {
            if (level.getBlockEntity(core) instanceof ArchiveCoreBlockEntity be && inReach(reach(level, core), pos)) {
                double d = core.distSqr(pos);
                if (d < bestDist) {
                    best = be;
                    bestDist = d;
                }
            }
        }
        return best;
    }

    /** Selos dentro do alcance do Núcleo. */
    public static synchronized Set<BlockPos> sealsOf(Level level, BlockPos core) {
        Set<BlockPos> nodes = reach(level, core);
        Set<BlockPos> result = new HashSet<>();
        for (BlockPos seal : set(level, Kind.SEAL)) {
            if (inReach(nodes, seal)) {
                result.add(seal);
            }
        }
        return result;
    }
}

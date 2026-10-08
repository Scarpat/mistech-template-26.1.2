package com.mistech.mistechmod.machine;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Impulso de Gnomo: registro (só no servidor) dos Gnomos Engarrafados ALIMENTADOS em cada mundo.
 * As máquinas perguntam aqui se há um gnomo alimentado num cubo de raio {@link #RADIUS} em volta delas.
 * Vários gnomos não somam: o impulso é sempre {@link #SPEED_BONUS}.
 */
public final class GnomeBoost {
    public static final int RADIUS = 3;
    /** +50% de velocidade. */
    public static final float SPEED_BONUS = 0.5F;

    private static final Map<Level, Set<BlockPos>> ACTIVE = new WeakHashMap<>();

    private GnomeBoost() {}

    public static synchronized void setActive(Level level, BlockPos pos, boolean active) {
        if (active) {
            ACTIVE.computeIfAbsent(level, l -> new HashSet<>()).add(pos.immutable());
        } else {
            Set<BlockPos> set = ACTIVE.get(level);
            if (set != null) {
                set.remove(pos);
            }
        }
    }

    public static synchronized boolean isBoosted(Level level, BlockPos machine) {
        Set<BlockPos> set = ACTIVE.get(level);
        if (set == null || set.isEmpty()) {
            return false;
        }
        for (BlockPos gnome : set) {
            if (Math.abs(gnome.getX() - machine.getX()) <= RADIUS
                    && Math.abs(gnome.getY() - machine.getY()) <= RADIUS
                    && Math.abs(gnome.getZ() - machine.getZ()) <= RADIUS) {
                return true;
            }
        }
        return false;
    }

    /**
     * Para máquinas que andam 1 por tick: true em ticks alternados quando há gnomo alimentado por perto
     * (a máquina anda 1 a mais nesses ticks = +50%).
     */
    public static boolean extraStep(Level level, BlockPos machine) {
        return (level.getGameTime() & 1L) == 0 && isBoosted(level, machine);
    }

    /** Máquinas próximas de um gnomo (para desenhar as partículas indo até elas). */
    public static boolean inRange(BlockPos gnome, BlockPos machine) {
        return Math.abs(gnome.getX() - machine.getX()) <= RADIUS
                && Math.abs(gnome.getY() - machine.getY()) <= RADIUS
                && Math.abs(gnome.getZ() - machine.getZ()) <= RADIUS;
    }
}

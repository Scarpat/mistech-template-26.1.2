package com.mistech.mistechmod.ranged;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.mistech.mistechmod.Mistech;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Blocos temporários da Besta Mágica (muralha de Terra, poça de Magma): voltam ao bloco original depois de um tempo,
 * só se ninguém mexeu neles. Ficam só na memória — se o servidor fechar antes, o bloco fica.
 */
@EventBusSubscriber(modid = Mistech.MODID)
public final class TempBlocks {
    private record Temp(ResourceKey<Level> dimension, BlockPos pos, BlockState placed, BlockState original, long until) {}

    private static final List<Temp> TEMPS = new ArrayList<>();

    private TempBlocks() {}

    /** Troca o bloco por {@code ticks}; não mexe em blocos com block entity, inquebráveis ou já temporários. */
    public static boolean place(ServerLevel level, BlockPos pos, BlockState state, int ticks) {
        BlockState original = level.getBlockState(pos);
        if (level.getBlockEntity(pos) != null || original.getDestroySpeed(level, pos) < 0 || original == state) {
            return false;
        }
        for (Temp t : TEMPS) {
            if (t.pos().equals(pos) && t.dimension() == level.dimension()) {
                return false;
            }
        }
        if (!level.setBlock(pos, state, 3)) {
            return false;
        }
        TEMPS.add(new Temp(level.dimension(), pos.immutable(), state, original, level.getGameTime() + ticks));
        return true;
    }

    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        if (TEMPS.isEmpty()) {
            return;
        }
        Iterator<Temp> it = TEMPS.iterator();
        while (it.hasNext()) {
            Temp t = it.next();
            ServerLevel level = event.getServer().getLevel(t.dimension());
            if (level == null) {
                it.remove();
                continue;
            }
            if (level.getGameTime() < t.until()) {
                continue;
            }
            it.remove();
            if (level.isLoaded(t.pos()) && level.getBlockState(t.pos()) == t.placed()) {
                level.setBlock(t.pos(), t.original(), 3);
                level.sendParticles(ParticleTypes.POOF, t.pos().getX() + 0.5, t.pos().getY() + 0.5, t.pos().getZ() + 0.5, 2, 0.3, 0.3, 0.3, 0.01);
            }
        }
    }
}

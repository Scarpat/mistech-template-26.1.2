package com.mistech.mistechmod.machine;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.registry.MachineContent;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import com.mistech.mistechmod.block.SpatialBarrelBlock;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Multibloco da Mineradora.
 * <pre>
 * Base (y-1)      Em cima (y)
 *  0 T 0           0 0 0
 *  T B T           0 M 0
 *  0 T 0           0 0 0
 * </pre>
 * T = Tuneladora (qualquer direção), B = Barril Espacial, M = Bloco de Magia Impura, 0 = qualquer coisa.
 * Botão direito com a Pedra Guia no Bloco de Magia Impura → vira o núcleo da Mineradora (a pedra não é gasta).
 * As outras peças continuam sendo blocos normais: o barril recebe o que é minerado; se alguma peça sair,
 * a Mineradora para (e volta sozinha quando a peça é recolocada).
 */
@EventBusSubscriber(modid = Mistech.MODID)
public final class MinerStructure {
    private MinerStructure() {}

    /** Primeira peça que falta embaixo do núcleo em {@code top}, ou null se a base está completa. */
    public static @Nullable BlockPos findMissing(Level level, BlockPos top) {
        BlockPos center = top.below();
        if (!level.getBlockState(center).is(Mistech.SPATIAL_BARREL.get())) {
            return center;
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos side = center.relative(direction);
            if (!level.getBlockState(side).is(MachineContent.TUNNELER.get())) {
                return side;
            }
        }
        return null;
    }

    public static boolean isComplete(Level level, BlockPos top) {
        return findMissing(level, top) == null;
    }

    /**
     * Liga/desliga o visual de máquina montada nas peças que estiverem no lugar: o barril vira a Câmara Coletora e as
     * tuneladoras viram Pernas de Perfuração viradas para fora (com o braço subindo até o núcleo).
     */
    public static void setFormed(Level level, BlockPos top, boolean formed) {
        BlockPos center = top.below();
        BlockState barrel = level.getBlockState(center);
        if (barrel.is(Mistech.SPATIAL_BARREL.get()) && barrel.getValue(SpatialBarrelBlock.FORMED) != formed) {
            level.setBlock(center, barrel.setValue(SpatialBarrelBlock.FORMED, formed), Block.UPDATE_ALL);
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos side = center.relative(direction);
            BlockState leg = level.getBlockState(side);
            if (!leg.is(MachineContent.TUNNELER.get())) {
                continue;
            }
            BlockState target = formed
                    ? leg.setValue(TunnelerBlock.FORMED, true).setValue(TunnelerBlock.FACING, direction).setValue(TunnelerBlock.TRIGGERED, false)
                    : leg.setValue(TunnelerBlock.FORMED, false).setValue(TunnelerBlock.TRIGGERED, false);
            if (target != leg) {
                level.setBlock(side, target, Block.UPDATE_ALL);
            }
        }
        refreshCapabilities(level, top);
    }

    /** A capability de itens do núcleo depende de estar montada: avisa funis/canos. */
    private static void refreshCapabilities(Level level, BlockPos top) {
        level.invalidateCapabilities(top);
    }

    /** Brocas das pernas girando (TRIGGERED) enquanto a Mineradora trabalha. */
    public static void setWorking(Level level, BlockPos top, boolean working) {
        BlockPos center = top.below();
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos side = center.relative(direction);
            BlockState leg = level.getBlockState(side);
            if (leg.is(MachineContent.TUNNELER.get()) && leg.getValue(TunnelerBlock.FORMED)
                    && leg.getValue(TunnelerBlock.TRIGGERED) != working) {
                level.setBlock(side, leg.setValue(TunnelerBlock.TRIGGERED, working), Block.UPDATE_CLIENTS);
            }
        }
    }

    /** Pedra Guia no Bloco de Magia Impura com Barril Espacial embaixo → monta a Mineradora. */
    @SubscribeEvent
    static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getItemStack().is(Mistech.GUIDE_STONE.get())) {
            return;
        }
        Level level = event.getLevel();
        BlockPos top = event.getPos();
        if (!level.getBlockState(top).is(Mistech.IMPURE_MAGIC_BLOCK.get())
                || !level.getBlockState(top.below()).is(Mistech.SPATIAL_BARREL.get())) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        BlockPos missing = findMissing(level, top);
        if (missing != null) {
            event.getEntity().sendOverlayMessage(Component.translatable("message.mistech.miner_incomplete",
                    missing.getX(), missing.getY(), missing.getZ()));
            server.sendParticles(ParticleTypes.ANGRY_VILLAGER, missing.getX() + 0.5, missing.getY() + 1.1, missing.getZ() + 0.5,
                    3, 0.2, 0.1, 0.2, 0.0);
            return;
        }
        Direction facing = event.getEntity().getDirection().getOpposite();
        server.setBlock(top, MachineContent.MINER.get().defaultBlockState().setValue(MachineBlock.FACING, facing), Block.UPDATE_ALL);
        setFormed(server, top, true);
        event.getEntity().sendOverlayMessage(Component.translatable("message.mistech.miner_assembled"));

        BlockPos center = top.below();
        server.sendParticles(ParticleTypes.REVERSE_PORTAL, center.getX() + 0.5, center.getY() + 0.8, center.getZ() + 0.5,
                120, 1.2, 0.8, 1.2, 0.05);
        server.sendParticles(ParticleTypes.WITCH, top.getX() + 0.5, top.getY() + 1.0, top.getZ() + 0.5, 30, 0.3, 0.3, 0.3, 0.05);
        server.playSound(null, top, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0F, 0.8F);
        server.playSound(null, top, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.2F, 0.6F);
    }
}

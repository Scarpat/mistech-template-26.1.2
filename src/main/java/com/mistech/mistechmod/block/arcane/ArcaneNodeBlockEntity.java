package com.mistech.mistechmod.block.arcane;

import com.mistech.mistechmod.registry.MistechBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandlerUtil;

/**
 * Todo tick: puxa energia do bloco onde o Ponto está preso e empurra para o bloco preso atrás de cada
 * Receptor ao alcance. Como cada Ponto faz isso, um Receptor na área de vários Pontos recebe de todos.
 */
public class ArcaneNodeBlockEntity extends BlockEntity {
    /** Máximo que um Ponto envia para cada Receptor por tick. */
    public static final int TRANSFER_PER_RECEIVER = 1000;

    /** Receptores alcançados pela malha de Pilares de Transmissão (refeito a cada 1 s). */
    private java.util.Set<BlockPos> meshReceivers = java.util.Set.of();

    public ArcaneNodeBlockEntity(BlockPos pos, BlockState state) {
        super(MistechBlockEntities.ARCANE_NODE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ArcaneNodeBlockEntity node) {
        Direction facing = state.getValue(ArcaneAttachedBlock.FACING);
        EnergyHandler source = level.getCapability(Capabilities.Energy.BLOCK, ArcaneAttachedBlock.attachedPos(pos, state), facing);
        if (source == null || source.getAmountAsLong() <= 0) {
            return;
        }

        java.util.Set<BlockPos> direct = ArcaneNetwork.receiversNear(level, pos);
        if (level.getGameTime() % 20 == (pos.asLong() & 15)) {
            java.util.Set<BlockPos> mesh = com.mistech.mistechmod.ether.PylonMesh.receiversVia(level, pos);
            mesh.removeAll(direct);
            node.meshReceivers = mesh;
        }
        java.util.List<BlockPos> targets = new java.util.ArrayList<>(direct);
        targets.addAll(node.meshReceivers);
        int sentTotal = 0;
        for (BlockPos receiverPos : targets) {
            BlockState receiverState = level.getBlockState(receiverPos);
            if (!(receiverState.getBlock() instanceof ArcaneReceiverBlock)) {
                ArcaneNetwork.removeReceiver(level, receiverPos); // registro velho
                continue;
            }
            Direction receiverFacing = receiverState.getValue(ArcaneAttachedBlock.FACING);
            BlockPos targetPos = ArcaneAttachedBlock.attachedPos(receiverPos, receiverState);
            EnergyHandler target = level.getCapability(Capabilities.Energy.BLOCK, targetPos, receiverFacing);
            if (target == null || target == source) {
                continue;
            }
            int limit = direct.contains(receiverPos) ? TRANSFER_PER_RECEIVER : com.mistech.mistechmod.ether.PylonMesh.TRANSFER_PER_RECEIVER;
            int sent = EnergyHandlerUtil.move(source, target, limit, null);
            sentTotal += sent;
            if (sent > 0 && level.getGameTime() % 20 == 0 && level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        receiverPos.getX() + 0.5, receiverPos.getY() + 0.5, receiverPos.getZ() + 0.5, 3, 0.15, 0.15, 0.15, 0.05);
            }
        }
        if (sentTotal > 0 && level.getGameTime() % 20 == 0 && level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    3, 0.15, 0.15, 0.15, 0.05);
        }
    }
}

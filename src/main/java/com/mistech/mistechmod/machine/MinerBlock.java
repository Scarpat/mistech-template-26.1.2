package com.mistech.mistechmod.machine;

import com.mistech.mistechmod.registry.MistechBlockEntities;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Núcleo da Mineradora: nasce do Bloco de Magia Impura (Pedra Guia + estrutura, ver {@link MinerStructure}).
 * Quebrado, devolve o Bloco de Magia Impura (loot table) e as runas.
 */
public class MinerBlock extends MachineBlock {
    public static final MapCodec<MinerBlock> CODEC = simpleCodec(MinerBlock::new);

    public MinerBlock(BlockBehaviour.Properties properties) {
        super(properties, MistechBlockEntities.MINER);
    }

    @Override
    public MapCodec<MinerBlock> codec() {
        return CODEC;
    }

    /** Clique agachado (mão vazia) abre direto a Câmara Coletora; clique normal abre a Mineradora. */
    @Override
    protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            net.minecraft.world.entity.player.Player player, net.minecraft.world.phys.BlockHitResult hitResult) {
        if (player.isSecondaryUseActive()) {
            if (!level.isClientSide() && level.getBlockEntity(pos) instanceof MinerBlockEntity miner) {
                miner.openChamber(player);
            }
            return net.minecraft.world.InteractionResult.SUCCESS;
        }
        return super.useWithoutItem(state, level, pos, player, hitResult);
    }

    /** Núcleo saiu: as peças voltam a ser Tuneladoras e Barril Espacial comuns. */
    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos,
            boolean movedByPiston) {
        MinerStructure.setFormed(level, pos, false);
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }

    /** Trabalhando: partículas de portal sendo sugadas para o núcleo. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            return;
        }
        for (int i = 0; i < 2; i++) {
            level.addParticle(ParticleTypes.PORTAL, pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 1.5,
                    pos.getY() + random.nextDouble() * 1.2, pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 1.5,
                    (random.nextDouble() - 0.5) * 0.5, -random.nextDouble() * 0.6, (random.nextDouble() - 0.5) * 0.5);
        }
    }
}

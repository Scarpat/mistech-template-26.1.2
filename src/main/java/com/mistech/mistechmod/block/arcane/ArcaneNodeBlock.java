package com.mistech.mistechmod.block.arcane;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.registry.MistechBlockEntities;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** Ponto de Rede Arcana: puxa energia do bloco onde está preso e envia aos Receptores a até 10 blocos. */
public class ArcaneNodeBlock extends ArcaneAttachedBlock {
    public static final MapCodec<ArcaneNodeBlock> CODEC = simpleCodec(ArcaneNodeBlock::new);

    public ArcaneNodeBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<ArcaneNodeBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ArcaneNodeBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, MistechBlockEntities.ARCANE_NODE.get(), ArcaneNodeBlockEntity::serverTick);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(6) == 0) {
            Direction f = state.getValue(FACING);
            level.addParticle(ParticleTypes.END_ROD,
                    pos.getX() + 0.5 - f.getStepX() * 0.2, pos.getY() + 0.5 - f.getStepY() * 0.2, pos.getZ() + 0.5 - f.getStepZ() * 0.2,
                    f.getStepX() * 0.02, f.getStepY() * 0.02, f.getStepZ() * 0.02);
        }
    }
}

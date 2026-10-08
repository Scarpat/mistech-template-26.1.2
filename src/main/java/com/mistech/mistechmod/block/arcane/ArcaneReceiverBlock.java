package com.mistech.mistechmod.block.arcane;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** Receptor Arcano: recebe energia de todos os Pontos ao alcance e coloca no bloco onde está preso. */
public class ArcaneReceiverBlock extends ArcaneAttachedBlock {
    public static final MapCodec<ArcaneReceiverBlock> CODEC = simpleCodec(ArcaneReceiverBlock::new);

    public ArcaneReceiverBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<ArcaneReceiverBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ArcaneReceiverBlockEntity(pos, state);
    }
}

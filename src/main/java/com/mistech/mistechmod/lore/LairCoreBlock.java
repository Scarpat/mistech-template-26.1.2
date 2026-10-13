package com.mistech.mistechmod.lore;

import org.jspecify.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Coração da Toca: bloco invisível e inquebrável enterrado embaixo da Toca da Cuca e da Caverna do Mapinguari.
 * Acorda o chefe quando um jogador chega perto (ver {@link LairCoreBlockEntity}). Não tem item.
 */
public class LairCoreBlock extends BaseEntityBlock {
    public static final MapCodec<LairCoreBlock> CODEC = simpleCodec(LairCoreBlock::new);

    public LairCoreBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LairCoreBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, LoreContent.LAIR_CORE_BE.get(), LairCoreBlockEntity::serverTick);
    }
}

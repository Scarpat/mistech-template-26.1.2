package com.mistech.mistechmod.realm.dream.structure;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.registry.DreamStructures;
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
 * Âncora Onírica: bloco escondido nas paredes das estruturas do Pesadelo que vigia uma área e faz a "mágica" do sonho —
 * devolver o jogador para trás (corredor/escada sem fim) ou soltar o Bicho-Papão. Ver {@link DreamAnchorBlockEntity}.
 */
public class DreamAnchorBlock extends BaseEntityBlock {
    public static final MapCodec<DreamAnchorBlock> CODEC = simpleCodec(DreamAnchorBlock::new);

    public DreamAnchorBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DreamAnchorBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, DreamStructures.ANCHOR_BE.get(), DreamAnchorBlockEntity::serverTick);
    }
}

package com.mistech.mistechmod.purity.block;

import com.mistech.mistechmod.purity.PurityContent;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Arruda: erva de limpeza. Planta em terra arada e cresce em 8 estágios, como o trigo. */
public class RueCropBlock extends CropBlock {
    public static final MapCodec<RueCropBlock> CODEC = simpleCodec(RueCropBlock::new);
    /** Contorno de selecao por estagio (altura em pixels). Touceira baixa de raminhos. */
    private static final int[] HEIGHTS = {3, 5, 7, 9, 11, 13, 15, 16};
    private static final VoxelShape[] SHAPES = Block.boxes(7, age -> Block.column(13.0, 0.0, HEIGHTS[age]));

    public RueCropBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<RueCropBlock> codec() {
        return CODEC;
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return PurityContent.RUE_SEEDS.get();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[this.getAge(state)];
    }
}

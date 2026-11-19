package com.mistech.mistechmod.block;

import com.mistech.mistechmod.Mistech;
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

/** Plantação de Tabaco: planta em terra arada e cresce em 8 estágios, como o trigo. */
public class TobaccoCropBlock extends CropBlock {
    public static final MapCodec<TobaccoCropBlock> CODEC = simpleCodec(TobaccoCropBlock::new);
    /** Contorno de selecao por estagio (altura em pixels). Folhas largas embaixo e pendao de flores no alto. */
    private static final int[] HEIGHTS = {2, 4, 6, 9, 12, 15, 19, 26};
    private static final VoxelShape[] SHAPES = Block.boxes(7, age -> Block.column(14.0, 0.0, HEIGHTS[age]));

    public TobaccoCropBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<TobaccoCropBlock> codec() {
        return CODEC;
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return Mistech.TOBACCO_SEEDS.get();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[this.getAge(state)];
    }
}

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

/** Plantação de Cânhamo: planta em terra arada e cresce em 8 estágios, como o trigo. */
public class HempCropBlock extends CropBlock {
    public static final MapCodec<HempCropBlock> CODEC = simpleCodec(HempCropBlock::new);
    /** Contorno de selecao por estagio (altura em pixels). Alto: madura, a planta passa de um bloco de altura. */
    private static final int[] HEIGHTS = {3, 6, 9, 12, 16, 21, 26, 31};
    private static final VoxelShape[] SHAPES = Block.boxes(7, age -> Block.column(10.0, 0.0, HEIGHTS[age]));

    public HempCropBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<HempCropBlock> codec() {
        return CODEC;
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return Mistech.HEMP_SEEDS.get();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[this.getAge(state)];
    }
}

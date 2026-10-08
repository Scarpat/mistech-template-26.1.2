package com.mistech.mistechmod.block;

import com.mistech.mistechmod.Mistech;
import com.mojang.serialization.MapCodec;

import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Plantação de Cânhamo: planta em terra arada e cresce em 8 estágios, como o trigo. */
public class HempCropBlock extends CropBlock {
    public static final MapCodec<HempCropBlock> CODEC = simpleCodec(HempCropBlock::new);

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
}

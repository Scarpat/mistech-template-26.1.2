package com.mistech.mistechmod.block;

import com.mistech.mistechmod.Mistech;
import com.mojang.serialization.MapCodec;

import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Plantação de Tabaco: planta em terra arada e cresce em 8 estágios, como o trigo. */
public class TobaccoCropBlock extends CropBlock {
    public static final MapCodec<TobaccoCropBlock> CODEC = simpleCodec(TobaccoCropBlock::new);

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
}

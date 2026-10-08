package com.mistech.mistechmod.block;

import com.mistech.mistechmod.Mistech;
import com.mojang.serialization.MapCodec;

import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Plantação de Café: planta em terra arada e cresce em 8 estágios, como o trigo. */
public class CoffeeCropBlock extends CropBlock {
    public static final MapCodec<CoffeeCropBlock> CODEC = simpleCodec(CoffeeCropBlock::new);

    public CoffeeCropBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<CoffeeCropBlock> codec() {
        return CODEC;
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return Mistech.COFFEE_SEEDS.get();
    }
}

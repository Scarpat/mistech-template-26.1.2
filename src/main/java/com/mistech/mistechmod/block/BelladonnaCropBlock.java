package com.mistech.mistechmod.block;

import com.mistech.mistechmod.Mistech;
import com.mojang.serialization.MapCodec;

import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Plantação de Beladona: planta em terra arada e cresce em 8 estágios, como o trigo. */
public class BelladonnaCropBlock extends CropBlock {
    public static final MapCodec<BelladonnaCropBlock> CODEC = simpleCodec(BelladonnaCropBlock::new);

    public BelladonnaCropBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<BelladonnaCropBlock> codec() {
        return CODEC;
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return Mistech.BELLADONNA_SEEDS.get();
    }
}

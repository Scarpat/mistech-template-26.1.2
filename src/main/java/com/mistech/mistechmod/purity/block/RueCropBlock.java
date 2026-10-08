package com.mistech.mistechmod.purity.block;

import com.mistech.mistechmod.purity.PurityContent;
import com.mojang.serialization.MapCodec;

import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Arruda: erva de limpeza. Planta em terra arada e cresce em 8 estágios, como o trigo. */
public class RueCropBlock extends CropBlock {
    public static final MapCodec<RueCropBlock> CODEC = simpleCodec(RueCropBlock::new);

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
}

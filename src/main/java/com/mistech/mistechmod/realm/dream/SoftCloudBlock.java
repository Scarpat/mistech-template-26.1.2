package com.mistech.mistechmod.realm.dream;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** Lã-de-Nuvem: bloco macio das Colinas de Travesseiro — cair nela não machuca. */
public class SoftCloudBlock extends Block {
    public static final MapCodec<SoftCloudBlock> CODEC = simpleCodec(SoftCloudBlock::new);

    public SoftCloudBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
        entity.causeFallDamage(fallDistance, 0.0F, level.damageSources().fall());
    }
}

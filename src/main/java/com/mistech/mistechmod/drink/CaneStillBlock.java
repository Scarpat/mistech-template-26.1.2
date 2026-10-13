package com.mistech.mistechmod.drink;

import com.mistech.mistechmod.machine.MachineBlock;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** Alambique de cobre: vapor saindo do capelo enquanto destila. */
public class CaneStillBlock extends MachineBlock {
    public static final MapCodec<CaneStillBlock> CODEC = simpleCodec(CaneStillBlock::new);

    public CaneStillBlock(BlockBehaviour.Properties properties) {
        super(properties, DrinkContent.CANE_STILL_BE);
    }

    @Override
    public MapCodec<CaneStillBlock> codec() {
        return CODEC;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            return;
        }
        if (random.nextInt(2) == 0) {
            level.addParticle(ParticleTypes.CLOUD, pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.2, pos.getY() + 1.05,
                    pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.2, 0.0, 0.04, 0.0);
        }
        if (random.nextInt(5) == 0) {
            level.addParticle(ParticleTypes.DRIPPING_HONEY, pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.7, pos.getY() + 0.3,
                    pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.7, 0.0, 0.0, 0.0);
        }
    }
}

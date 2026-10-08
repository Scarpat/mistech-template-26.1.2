package com.mistech.mistechmod.machine;

import com.mistech.mistechmod.registry.MistechBlockEntities;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** Gerador de Cinzas: brasas e fumaça saindo por cima enquanto queima. */
public class AshGeneratorBlock extends MachineBlock {
    public static final MapCodec<AshGeneratorBlock> CODEC = simpleCodec(AshGeneratorBlock::new);

    public AshGeneratorBlock(BlockBehaviour.Properties properties) {
        super(properties, MistechBlockEntities.ASH_GENERATOR);
    }

    @Override
    public MapCodec<AshGeneratorBlock> codec() {
        return CODEC;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            return;
        }
        double x = pos.getX() + 0.5, y = pos.getY() + 1.0, z = pos.getZ() + 0.5;
        if (random.nextInt(2) == 0) {
            level.addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE, x + (random.nextDouble() - 0.5) * 0.4, y, z + (random.nextDouble() - 0.5) * 0.4, 0.0, 0.04, 0.0);
        }
        if (random.nextInt(5) == 0) {
            level.addParticle(net.minecraft.core.particles.ParticleTypes.SMALL_FLAME, x + (random.nextDouble() - 0.5) * 0.3, y, z + (random.nextDouble() - 0.5) * 0.3, 0.0, 0.02, 0.0);
        }
        if (random.nextInt(40) == 0) {
            level.playLocalSound(x, y, z, net.minecraft.sounds.SoundEvents.FURNACE_FIRE_CRACKLE, net.minecraft.sounds.SoundSource.BLOCKS, 0.6F, 1.0F, false);
        }
    }
}

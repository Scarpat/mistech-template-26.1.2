package com.mistech.mistechmod.beyond;

import com.mistech.mistechmod.machine.MachineBlock;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** Gerador de Blaze: chamas e fumaça saindo por cima enquanto queima. */
public class BlazeGeneratorBlock extends MachineBlock {
    public static final MapCodec<BlazeGeneratorBlock> CODEC = simpleCodec(BlazeGeneratorBlock::new);

    public BlazeGeneratorBlock(BlockBehaviour.Properties properties) {
        super(properties, PaleNetherContent.BLAZE_GENERATOR_BE);
    }

    @Override
    public MapCodec<BlazeGeneratorBlock> codec() {
        return CODEC;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            return;
        }
        double x = pos.getX() + 0.5, y = pos.getY() + 1.0, z = pos.getZ() + 0.5;
        if (random.nextInt(2) == 0) {
            level.addParticle(ParticleTypes.FLAME, x + (random.nextDouble() - 0.5) * 0.4, y, z + (random.nextDouble() - 0.5) * 0.4, 0.0, 0.04, 0.0);
        }
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.LARGE_SMOKE, x, y + 0.1, z, 0.0, 0.05, 0.0);
        }
        if (random.nextInt(30) == 0) {
            level.playLocalSound(x, y, z, SoundEvents.BLAZE_BURN, SoundSource.BLOCKS, 0.5F, 1.0F, false);
        }
    }
}

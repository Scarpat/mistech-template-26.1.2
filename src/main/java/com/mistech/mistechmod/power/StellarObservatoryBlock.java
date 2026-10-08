package com.mistech.mistechmod.power;

import com.mistech.mistechmod.machine.MachineBlock;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** Observatório Estelar: poeira de estrela descendo sobre a cúpula enquanto gera. */
public class StellarObservatoryBlock extends MachineBlock {
    public static final MapCodec<StellarObservatoryBlock> CODEC = simpleCodec(StellarObservatoryBlock::new);

    public StellarObservatoryBlock(BlockBehaviour.Properties properties) {
        super(properties, PowerContent.STELLAR_OBSERVATORY_BE);
    }

    @Override
    public MapCodec<StellarObservatoryBlock> codec() {
        return CODEC;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            return;
        }
        if (random.nextInt(2) == 0) {
            level.addParticle(ParticleTypes.END_ROD, pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.5, pos.getY() + 2.2,
                    pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.5, 0.0, -0.06, 0.0);
        }
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5,
                    (random.nextDouble() - 0.5) * 0.2, 0.05, (random.nextDouble() - 0.5) * 0.2);
        }
    }
}

package com.mistech.mistechmod.machine;

import com.mistech.mistechmod.registry.MistechBlockEntities;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** Plantadeira Rúnica: gotinhas e brilhos verdes enquanto trabalha. */
public class RunicPlanterBlock extends MachineBlock {
    public static final MapCodec<RunicPlanterBlock> CODEC = simpleCodec(RunicPlanterBlock::new);

    public RunicPlanterBlock(BlockBehaviour.Properties properties) {
        super(properties, MistechBlockEntities.RUNIC_PLANTER);
    }

    @Override
    public MapCodec<RunicPlanterBlock> codec() {
        return CODEC;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT) || random.nextInt(4) != 0) {
            return;
        }
        level.addParticle(net.minecraft.core.particles.ParticleTypes.SPLASH, pos.getX() + random.nextDouble(),
                pos.getY() + 1.05, pos.getZ() + random.nextDouble(), 0.0, 0.05, 0.0);
    }
}

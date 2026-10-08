package com.mistech.mistechmod.machine;

import com.mistech.mistechmod.registry.MistechBlockEntities;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** Triturador Rúnico: faíscas e poeira de pedra enquanto tritura. */
public class RunicCrusherBlock extends MachineBlock {
    public static final MapCodec<RunicCrusherBlock> CODEC = simpleCodec(RunicCrusherBlock::new);

    public RunicCrusherBlock(BlockBehaviour.Properties properties) {
        super(properties, MistechBlockEntities.RUNIC_CRUSHER);
    }

    @Override
    public MapCodec<RunicCrusherBlock> codec() {
        return CODEC;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            return;
        }
        double x = pos.getX() + 0.5, y = pos.getY() + 1.0, z = pos.getZ() + 0.5;
        if (random.nextInt(3) == 0) {
            level.addParticle(new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK,
                    net.minecraft.world.level.block.Blocks.GRAVEL.defaultBlockState()), x + (random.nextDouble() - 0.5) * 0.6, y, z + (random.nextDouble() - 0.5) * 0.6, 0.0, 0.05, 0.0);
        }
        if (random.nextInt(6) == 0) {
            level.addParticle(net.minecraft.core.particles.ParticleTypes.CRIT, x, y, z, (random.nextDouble() - 0.5) * 0.3, 0.2, (random.nextDouble() - 0.5) * 0.3);
        }
    }
}

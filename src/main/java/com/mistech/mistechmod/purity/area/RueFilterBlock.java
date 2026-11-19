package com.mistech.mistechmod.purity.area;

import com.mistech.mistechmod.machine.arcane.ArcaneMachineBlock;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** Filtro de Arruda: chaminé com folhas de arruda que solta fumaça verde enquanto limpa. */
public class RueFilterBlock extends ArcaneMachineBlock {
    public static final MapCodec<RueFilterBlock> CODEC = simpleCodec(RueFilterBlock::new);

    public RueFilterBlock(BlockBehaviour.Properties properties) {
        super(properties, AreaTaintContent.RUE_FILTER_BE);
    }

    @Override
    public MapCodec<RueFilterBlock> codec() {
        return CODEC;
    }

    @Override
    protected ParticleOptions particle() {
        return ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0xFF6FCF5A);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            return;
        }
        double x = pos.getX() + 0.5, y = pos.getY() + 1.15, z = pos.getZ() + 0.5;
        if (random.nextInt(2) == 0) {
            level.addParticle(ParticleTypes.SMOKE, x + (random.nextDouble() - 0.5) * 0.2, y, z + (random.nextDouble() - 0.5) * 0.2, 0.0, 0.04, 0.0);
        }
        level.addParticle(this.particle(), x + (random.nextDouble() - 0.5) * 0.25, y + 0.1, z + (random.nextDouble() - 0.5) * 0.25, 0.0, 0.05, 0.0);
        if (random.nextInt(8) == 0) {
            level.addParticle(ParticleTypes.HAPPY_VILLAGER, x + (random.nextDouble() - 0.5) * 0.6, y + 0.3, z + (random.nextDouble() - 0.5) * 0.6, 0.0, 0.02, 0.0);
        }
    }
}

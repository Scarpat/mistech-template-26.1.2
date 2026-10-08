package com.mistech.mistechmod.block.ritual;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** Marca de Giz Impuro: igual à Marca de Giz, desenhada com o Giz Impuro; usada nos círculos Tier II/III. */
public class ImpureChalkMarkBlock extends ChalkMarkBlock {
    public static final MapCodec<ImpureChalkMarkBlock> CODEC = simpleCodec(ImpureChalkMarkBlock::new);

    public ImpureChalkMarkBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(12) == 0) {
            level.addParticle(new DustParticleOptions(0x9B4FE0, 0.7F), pos.getX() + random.nextDouble(), pos.getY() + 0.05,
                    pos.getZ() + random.nextDouble(), 0.0, 0.02, 0.0);
        }
        if (random.nextInt(40) == 0) {
            level.addParticle(ParticleTypes.PORTAL, pos.getX() + 0.5, pos.getY() + 0.1, pos.getZ() + 0.5, 0.0, 0.1, 0.0);
        }
    }
}

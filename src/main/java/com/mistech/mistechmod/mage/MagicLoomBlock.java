package com.mistech.mistechmod.mage;

import com.mistech.mistechmod.machine.MachineBlock;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** Tear Mágico: brilhos lilás enquanto tece. */
public class MagicLoomBlock extends MachineBlock {
    public static final MapCodec<MagicLoomBlock> CODEC = simpleCodec(MagicLoomBlock::new);

    public MagicLoomBlock(BlockBehaviour.Properties properties) {
        super(properties, MageContent.MAGIC_LOOM_BE);
    }

    @Override
    public MapCodec<MagicLoomBlock> codec() {
        return CODEC;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(LIT) && random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.ENCHANT, pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.8, pos.getY() + 1.1,
                    pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.8, 0.0, 0.3, 0.0);
        }
    }
}

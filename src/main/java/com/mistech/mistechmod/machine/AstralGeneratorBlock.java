package com.mistech.mistechmod.machine;

import com.mistech.mistechmod.registry.MistechBlockEntities;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** Gerador Astral: estrelinhas subindo da cúpula enquanto gera. */
public class AstralGeneratorBlock extends MachineBlock {
    public static final MapCodec<AstralGeneratorBlock> CODEC = simpleCodec(AstralGeneratorBlock::new);

    public AstralGeneratorBlock(BlockBehaviour.Properties properties) {
        super(properties, MistechBlockEntities.ASTRAL_GENERATOR);
    }

    @Override
    public MapCodec<AstralGeneratorBlock> codec() {
        return CODEC;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            return;
        }
        if (random.nextInt(3) == 0) {
            level.addParticle(net.minecraft.core.particles.ParticleTypes.END_ROD, pos.getX() + 0.2 + random.nextDouble() * 0.6,
                    pos.getY() + 1.05, pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0.0, 0.03, 0.0);
        }
    }
}

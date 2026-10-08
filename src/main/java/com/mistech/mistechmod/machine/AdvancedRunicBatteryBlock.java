package com.mistech.mistechmod.machine;

import com.mistech.mistechmod.registry.MistechBlockEntities;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** Bateria Rúnica Aprimorada: o núcleo do vazio no topo solta partículas enquanto tem energia. */
public class AdvancedRunicBatteryBlock extends RunicBatteryBlock {
    public static final MapCodec<AdvancedRunicBatteryBlock> CODEC = simpleCodec(AdvancedRunicBatteryBlock::new);

    public AdvancedRunicBatteryBlock(BlockBehaviour.Properties properties) {
        super(properties, MistechBlockEntities.ADVANCED_RUNIC_BATTERY);
    }

    @Override
    public MapCodec<AdvancedRunicBatteryBlock> codec() {
        return CODEC;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            return;
        }
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.REVERSE_PORTAL, pos.getX() + 0.35 + random.nextDouble() * 0.3, pos.getY() + 1.0,
                    pos.getZ() + 0.35 + random.nextDouble() * 0.3, 0.0, 0.02, 0.0);
        }
        if (random.nextInt(10) == 0) {
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, pos.getX() + random.nextDouble(), pos.getY() + 0.2 + random.nextDouble() * 0.6,
                    pos.getZ() + random.nextDouble(), 0.0, 0.0, 0.0);
        }
    }
}

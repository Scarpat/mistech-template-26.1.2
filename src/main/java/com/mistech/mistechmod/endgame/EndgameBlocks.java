package com.mistech.mistechmod.endgame;

import com.mistech.mistechmod.machine.MachineBlock;
import com.mistech.mistechmod.machine.RunicBatteryBlock;
import com.mistech.mistechmod.machine.arcane.ArcaneMachineBlock;
import com.mistech.mistechmod.spellcraft.conjurer.RunicConjurerBlock;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** Blocos da Fase 9 (endgame). */
public final class EndgameBlocks {
    private EndgameBlocks() {}

    /** Coração da Forja Estelar. */
    public static final class StarForge extends ArcaneMachineBlock {
        public static final MapCodec<StarForge> CODEC = simpleCodec(StarForge::new);

        public StarForge(BlockBehaviour.Properties properties) {
            super(properties, EndgameContent.STAR_FORGE_BE);
        }

        @Override
        public MapCodec<StarForge> codec() {
            return CODEC;
        }

        @Override
        protected ParticleOptions particle() {
            return ParticleTypes.END_ROD;
        }
    }

    /** Núcleo do Reator Onírico. */
    public static final class DreamReactor extends MachineBlock {
        public static final MapCodec<DreamReactor> CODEC = simpleCodec(DreamReactor::new);

        public DreamReactor(BlockBehaviour.Properties properties) {
            super(properties, EndgameContent.DREAM_REACTOR_BE);
        }

        @Override
        public MapCodec<DreamReactor> codec() {
            return CODEC;
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            if (state.getValue(LIT) && random.nextInt(3) == 0) {
                level.addParticle(ParticleTypes.WITCH, pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(),
                        pos.getZ() + random.nextDouble(), 0.0, 0.02, 0.0);
            }
        }
    }

    /** Bateria Celeste: o mesmo bloco da Aprimorada, com outra entidade. */
    public static final class CelestialBattery extends RunicBatteryBlock {
        public static final MapCodec<CelestialBattery> CODEC = simpleCodec(CelestialBattery::new);

        public CelestialBattery(BlockBehaviour.Properties properties) {
            super(properties, EndgameContent.CELESTIAL_BATTERY_BE);
        }

        @Override
        public MapCodec<CelestialBattery> codec() {
            return CODEC;
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            if (state.getValue(LIT) && random.nextInt(3) == 0) {
                level.addParticle(ParticleTypes.END_ROD, pos.getX() + 0.35 + random.nextDouble() * 0.3, pos.getY() + 1.0,
                        pos.getZ() + 0.35 + random.nextDouble() * 0.3, 0.0, 0.03, 0.0);
            }
        }
    }

    public static final class CelestialConjurer extends RunicConjurerBlock {
        public static final MapCodec<CelestialConjurer> CODEC = simpleCodec(CelestialConjurer::new);

        public CelestialConjurer(BlockBehaviour.Properties properties) {
            super(properties, EndgameContent.CELESTIAL_CONJURER_BE);
        }

        @Override
        public MapCodec<CelestialConjurer> codec() {
            return CODEC;
        }

        @Override
        protected ParticleOptions particle() {
            return ParticleTypes.END_ROD;
        }
    }
}

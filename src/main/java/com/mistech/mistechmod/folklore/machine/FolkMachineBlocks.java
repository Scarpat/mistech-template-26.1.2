package com.mistech.mistechmod.folklore.machine;

import com.mistech.mistechmod.machine.MachineBlock;
import com.mistech.mistechmod.machine.arcane.ArcaneMachineBlock;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** Blocos da Fase 7 (máquinas de folclore). */
public final class FolkMachineBlocks {
    private FolkMachineBlocks() {}

    public static final class Windmill extends MachineBlock {
        public static final MapCodec<Windmill> CODEC = simpleCodec(Windmill::new);

        public Windmill(BlockBehaviour.Properties properties) {
            super(properties, FolkMachines.SACI_WINDMILL_BE);
        }

        @Override
        public MapCodec<Windmill> codec() {
            return CODEC;
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            if (state.getValue(LIT) && random.nextInt(6) == 0) {
                level.addParticle(ParticleTypes.CLOUD, pos.getX() + random.nextDouble(), pos.getY() + 1.1, pos.getZ() + random.nextDouble(),
                        (random.nextDouble() - 0.5) * 0.1, 0.05, (random.nextDouble() - 0.5) * 0.1);
            }
        }
    }

    public static final class Pump extends ArcaneMachineBlock {
        public static final MapCodec<Pump> CODEC = simpleCodec(Pump::new);

        public Pump(BlockBehaviour.Properties properties) {
            super(properties, FolkMachines.IARA_PUMP_BE);
        }

        @Override
        public MapCodec<Pump> codec() {
            return CODEC;
        }

        @Override
        protected ParticleOptions particle() {
            return ParticleTypes.SPLASH;
        }
    }

    public static final class Stump extends MachineBlock {
        public static final MapCodec<Stump> CODEC = simpleCodec(Stump::new);

        public Stump(BlockBehaviour.Properties properties) {
            super(properties, FolkMachines.CURUPIRA_STUMP_BE);
        }

        @Override
        public MapCodec<Stump> codec() {
            return CODEC;
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            if (state.getValue(LIT) && random.nextInt(5) == 0) {
                level.addParticle(ParticleTypes.FLAME, pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 1.0,
                        pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0.0, 0.02, 0.0);
            }
        }
    }

    public static final class Still extends ArcaneMachineBlock {
        public static final MapCodec<Still> CODEC = simpleCodec(Still::new);

        public Still(BlockBehaviour.Properties properties) {
            super(properties, FolkMachines.POTION_STILL_BE);
        }

        @Override
        public MapCodec<Still> codec() {
            return CODEC;
        }

        @Override
        protected ParticleOptions particle() {
            return ParticleTypes.BUBBLE_POP;
        }
    }

    public static final class Printer extends ArcaneMachineBlock {
        public static final MapCodec<Printer> CODEC = simpleCodec(Printer::new);

        public Printer(BlockBehaviour.Properties properties) {
            super(properties, FolkMachines.SCROLL_PRINTER_BE);
        }

        @Override
        public MapCodec<Printer> codec() {
            return CODEC;
        }

        @Override
        protected ParticleOptions particle() {
            return ParticleTypes.ENCHANT;
        }
    }

    public static final class LavaPump extends ArcaneMachineBlock {
        public static final MapCodec<LavaPump> CODEC = simpleCodec(LavaPump::new);

        public LavaPump(BlockBehaviour.Properties properties) {
            super(properties, FolkMachines.BOITATA_PUMP_BE);
        }

        @Override
        public MapCodec<LavaPump> codec() {
            return CODEC;
        }

        @Override
        protected ParticleOptions particle() {
            return ParticleTypes.FLAME;
        }
    }
}

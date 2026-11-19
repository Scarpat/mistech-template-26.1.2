package com.mistech.mistechmod.refining;

import com.mistech.mistechmod.machine.arcane.ArcaneMachineBlock;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Blocos da cadeia de minério (Fase 3): Lavadora de Essência, Cristalizador Rúnico e Núcleo do Prisma Astral. */
public final class RefiningBlocks {
    private RefiningBlocks() {}

    public static final class Washer extends ArcaneMachineBlock {
        public static final MapCodec<Washer> CODEC = simpleCodec(Washer::new);

        public Washer(BlockBehaviour.Properties properties) {
            super(properties, RefiningContent.ESSENCE_WASHER_BE);
        }

        @Override
        public MapCodec<Washer> codec() {
            return CODEC;
        }

        @Override
        protected ParticleOptions particle() {
            return ParticleTypes.WITCH;
        }
    }

    public static final class Crystallizer extends ArcaneMachineBlock {
        public static final MapCodec<Crystallizer> CODEC = simpleCodec(Crystallizer::new);

        public Crystallizer(BlockBehaviour.Properties properties) {
            super(properties, RefiningContent.RUNIC_CRYSTALLIZER_BE);
        }

        @Override
        public MapCodec<Crystallizer> codec() {
            return CODEC;
        }

        @Override
        protected ParticleOptions particle() {
            return ParticleTypes.END_ROD;
        }
    }

    public static final class Prism extends ArcaneMachineBlock {
        public static final MapCodec<Prism> CODEC = simpleCodec(Prism::new);

        public Prism(BlockBehaviour.Properties properties) {
            super(properties, RefiningContent.ASTRAL_PRISM_BE);
        }

        @Override
        public MapCodec<Prism> codec() {
            return CODEC;
        }

        @Override
        protected ParticleOptions particle() {
            return ParticleTypes.END_ROD;
        }
    }
}

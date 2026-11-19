package com.mistech.mistechmod.spellcraft.fx;

import com.mistech.mistechmod.Mistech;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Efeitos visuais das magias: partículas próprias (coloridas pela cor do elemento) e itens só de exibição, usados por
 * display entities (círculo mágico, onda de choque, esfera de luz). Os itens não aparecem no criativo.
 */
public final class VfxContent {
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, Mistech.MODID);

    private static ParticleType<ColorParticleOption> colored() {
        return new ParticleType<ColorParticleOption>(false) {
            @Override
            public MapCodec<ColorParticleOption> codec() {
                return ColorParticleOption.codec(this);
            }

            @Override
            public StreamCodec<? super RegistryFriendlyByteBuf, ColorParticleOption> streamCodec() {
                return ColorParticleOption.streamCodec(this);
            }
        };
    }

    /** Ponto de luz suave que encolhe. */
    public static final DeferredHolder<ParticleType<?>, ParticleType<ColorParticleOption>> MOTE = PARTICLES.register("mote", VfxContent::colored);
    /** Runa flutuante (8 desenhos). */
    public static final DeferredHolder<ParticleType<?>, ParticleType<ColorParticleOption>> GLYPH = PARTICLES.register("glyph", VfxContent::colored);
    /** Faísca em estrela, rápida, com atrito. */
    public static final DeferredHolder<ParticleType<?>, ParticleType<ColorParticleOption>> SPARK = PARTICLES.register("spark", VfxContent::colored);
    /** Brasa que sobe piscando. */
    public static final DeferredHolder<ParticleType<?>, ParticleType<ColorParticleOption>> EMBER = PARTICLES.register("ember", VfxContent::colored);
    /** Estilhaço que cai girando e quica no chão. */
    public static final DeferredHolder<ParticleType<?>, ParticleType<ColorParticleOption>> SHARD = PARTICLES.register("shard", VfxContent::colored);
    /** Onda de choque deitada no chão: abre até o raio dado na velocidade x. */
    public static final DeferredHolder<ParticleType<?>, ParticleType<ColorParticleOption>> WAVE = PARTICLES.register("wave", VfxContent::colored);

    /** Só carrega as texturas dos efeitos oníricos (dream_*); as partículas são criadas direto no cliente. */
    public static final DeferredHolder<ParticleType<?>, net.minecraft.core.particles.SimpleParticleType> DREAM =
            PARTICLES.register("dream", () -> new net.minecraft.core.particles.SimpleParticleType(false));

    /** Itens de exibição (tingidos pela cor do elemento via custom_model_data). */
    public static final DeferredItem<Item> VFX_CIRCLE = Mistech.ITEMS.registerSimpleItem("vfx_circle");
    public static final DeferredItem<Item> VFX_RING = Mistech.ITEMS.registerSimpleItem("vfx_ring");
    public static final DeferredItem<Item> VFX_ORB = Mistech.ITEMS.registerSimpleItem("vfx_orb");
    public static final DeferredItem<Item> VFX_CLOCK = Mistech.ITEMS.registerSimpleItem("vfx_clock");

    private VfxContent() {}

    public static void register(IEventBus bus) {
        PARTICLES.register(bus);
    }

    public static ColorParticleOption mote(int rgb) {
        return ColorParticleOption.create(MOTE.get(), 0xFF000000 | rgb);
    }

    public static ColorParticleOption glyph(int rgb) {
        return ColorParticleOption.create(GLYPH.get(), 0xFF000000 | rgb);
    }

    public static ColorParticleOption spark(int rgb) {
        return ColorParticleOption.create(SPARK.get(), 0xFF000000 | rgb);
    }

    public static ColorParticleOption ember(int rgb) {
        return ColorParticleOption.create(EMBER.get(), 0xFF000000 | rgb);
    }

    public static ColorParticleOption shard(int rgb) {
        return ColorParticleOption.create(SHARD.get(), 0xFF000000 | rgb);
    }

    public static ColorParticleOption wave(int rgb) {
        return ColorParticleOption.create(WAVE.get(), 0xFF000000 | rgb);
    }
}

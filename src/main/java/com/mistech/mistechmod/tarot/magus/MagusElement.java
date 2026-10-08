package com.mistech.mistechmod.tarot.magus;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.BossEvent;

/**
 * As formas do Mago. As quatro primeiras são os modos monstruosos (cada uma com a ferramenta do naipe:
 * Pentáculo, Espada, Taça e Bastão); a última é a forma humanoide do ápice ("assim em cima, como embaixo").
 */
public enum MagusElement {
    EARTH("earth", 0x8FB84A, ChatFormatting.GREEN, BossEvent.BossBarColor.GREEN),
    AIR("air", 0xCFEFFF, ChatFormatting.WHITE, BossEvent.BossBarColor.WHITE),
    WATER("water", 0x3AB8F0, ChatFormatting.AQUA, BossEvent.BossBarColor.BLUE),
    FIRE("fire", 0xFF7A1A, ChatFormatting.RED, BossEvent.BossBarColor.RED),
    QUINTESSENCE("quintessence", 0xFFE08A, ChatFormatting.GOLD, BossEvent.BossBarColor.YELLOW);

    public static final MagusElement[] CLASSIC = {EARTH, AIR, WATER, FIRE};

    public final String id;
    public final int color;
    public final ChatFormatting text;
    public final BossEvent.BossBarColor bar;

    MagusElement(String id, int color, ChatFormatting text, BossEvent.BossBarColor bar) {
        this.id = id;
        this.color = color;
        this.text = text;
        this.bar = bar;
    }

    public static MagusElement byId(int id) {
        MagusElement[] values = values();
        return id >= 0 && id < values.length ? values[id] : EARTH;
    }

    public static @Nullable MagusElement byIdOrNull(int id) {
        MagusElement[] values = values();
        return id >= 0 && id < values.length ? values[id] : null;
    }

    public Component displayName() {
        return Component.translatable("magus.mistech.element." + this.id).withStyle(this.text);
    }

    public ParticleOptions dust(float scale) {
        return new DustParticleOptions(this.color, scale);
    }

    /** Partícula "da cara" do elemento. */
    public ParticleOptions particle() {
        return switch (this) {
            case EARTH -> ParticleTypes.COMPOSTER;
            case AIR -> ParticleTypes.CLOUD;
            case WATER -> ParticleTypes.SPLASH;
            case FIRE -> ParticleTypes.FLAME;
            case QUINTESSENCE -> ParticleTypes.END_ROD;
        };
    }

    /** As misturas da Fase II (Alquimia). A forma do corpo é a do primeiro elemento; o segundo vem como aura. */
    public enum Mixture {
        MAGMA("magma", EARTH, FIRE),
        VAPOR("vapor", WATER, FIRE),
        STORM("storm", WATER, AIR),
        LIFE("life", EARTH, WATER),
        SAND("sand", AIR, EARTH),
        LIVING_FLAME("living_flame", AIR, FIRE);

        public final String id;
        public final MagusElement primary;
        public final MagusElement secondary;

        Mixture(String id, MagusElement primary, MagusElement secondary) {
            this.id = id;
            this.primary = primary;
            this.secondary = secondary;
        }

        public boolean has(MagusElement element) {
            return this.primary == element || this.secondary == element;
        }

        public static @Nullable Mixture of(MagusElement a, @Nullable MagusElement b) {
            if (b == null) {
                return null;
            }
            for (Mixture m : values()) {
                if (m.primary == a && m.secondary == b || m.primary == b && m.secondary == a) {
                    return m;
                }
            }
            return null;
        }

        public Component displayName() {
            return Component.translatable("magus.mistech.mixture." + this.id).withStyle(this.primary.text);
        }
    }
}

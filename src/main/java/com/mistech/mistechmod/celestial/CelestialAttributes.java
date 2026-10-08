package com.mistech.mistechmod.celestial;

import java.util.Optional;

import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.MoonPhase;

/**
 * Camadas extras do sistema de atributos do ambiente (cor do céu, da névoa, da luz, estrelas, fase da lua, sono,
 * monstros queimarem...). Lidas tanto no servidor (jogabilidade) quanto no cliente (visual), cada um com a sua
 * intensidade (0–1) de cada evento, que sobe e desce devagar.
 */
public final class CelestialAttributes {
    /** Intensidade atual (0–1) de cada evento. */
    @FunctionalInterface
    public interface Intensity {
        float of(CelestialEvent event);
    }

    public static final BedRule NO_SLEEP_BLOOD = new BedRule(BedRule.Rule.NEVER, BedRule.Rule.ALWAYS, false,
            Optional.of(Component.translatable("message.mistech.celestial.no_sleep_blood")));

    private CelestialAttributes() {}

    public static EnvironmentAttributeSystem.Builder addLayers(EnvironmentAttributeSystem.Builder builder, Intensity in) {
        color(builder, EnvironmentAttributes.SKY_COLOR, in, new Tint[] {
                tint(CelestialEvent.LONG_NIGHT, 0x05050F, 0.4F),
                tint(CelestialEvent.SOLSTICE, 0xFFD27A, 0.2F),
                tint(CelestialEvent.BLOOD_MOON, 0x7A0C0C, 0.8F),
                tint(CelestialEvent.SILVER_MOON, 0x8FA6CC, 0.45F),
                tint(CelestialEvent.ECLIPSE, 0x0B0B16, 0.9F),
                tint(CelestialEvent.AURORA, 0x1E7A62, 0.45F),
                tint(CelestialEvent.STARFALL, 0x1A1440, 0.3F),
                tint(CelestialEvent.BLESSED_RAIN, 0xD8C890, 0.25F)});
        color(builder, EnvironmentAttributes.FOG_COLOR, in, new Tint[] {
                tint(CelestialEvent.BLOOD_MOON, 0x5A0808, 0.7F),
                tint(CelestialEvent.SILVER_MOON, 0xA8B8D0, 0.4F),
                tint(CelestialEvent.ECLIPSE, 0x101018, 0.8F),
                tint(CelestialEvent.AURORA, 0x2A7A6A, 0.3F),
                tint(CelestialEvent.SOLSTICE, 0xFFE0A0, 0.15F)});
        color(builder, EnvironmentAttributes.CLOUD_COLOR, in, new Tint[] {
                tint(CelestialEvent.BLOOD_MOON, 0x8A2020, 0.55F),
                tint(CelestialEvent.AURORA, 0x50C8A0, 0.35F),
                tint(CelestialEvent.BLESSED_RAIN, 0xF0E0B0, 0.3F)});
        color(builder, EnvironmentAttributes.SKY_LIGHT_COLOR, in, new Tint[] {
                tint(CelestialEvent.BLOOD_MOON, 0xFF4030, 0.6F),
                tint(CelestialEvent.SILVER_MOON, 0xC8DCFF, 0.6F),
                tint(CelestialEvent.AURORA, 0x60FFC0, 0.4F),
                tint(CelestialEvent.ECLIPSE, 0x404060, 0.5F),
                tint(CelestialEvent.SOLSTICE, 0xFFE8B0, 0.25F)});
        builder.addTimeBasedLayer(EnvironmentAttributes.SKY_LIGHT_FACTOR, (base, cache) -> {
            float v = base;
            v = Mth.lerp(in.of(CelestialEvent.SILVER_MOON), v, Math.max(v, 0.5F));
            v = Mth.lerp(in.of(CelestialEvent.BLOOD_MOON), v, Math.max(v, 0.35F));
            v = Mth.lerp(in.of(CelestialEvent.AURORA), v, Math.max(v, 0.4F));
            v = Mth.lerp(in.of(CelestialEvent.ECLIPSE), v, 0.15F);
            return v;
        });
        // jogabilidade: luz do céu (o eclipse escurece de verdade: monstros nascem de dia)
        builder.addTimeBasedLayer(EnvironmentAttributes.SKY_LIGHT_LEVEL, (base, cache) -> Mth.lerp(in.of(CelestialEvent.ECLIPSE), base, Math.min(base, 4.0F)));
        builder.addTimeBasedLayer(EnvironmentAttributes.STAR_BRIGHTNESS, (base, cache) -> {
            float v = base;
            v = Mth.lerp(in.of(CelestialEvent.LONG_NIGHT), v, 1.0F);
            v = Mth.lerp(in.of(CelestialEvent.STARFALL), v, 1.0F);
            v = Mth.lerp(in.of(CelestialEvent.AURORA), v, 0.9F);
            v = Mth.lerp(in.of(CelestialEvent.ECLIPSE), v, 0.7F);
            v = Mth.lerp(in.of(CelestialEvent.BLOOD_MOON), v, 0.15F);
            return v;
        });
        builder.addTimeBasedLayer(EnvironmentAttributes.MOON_PHASE, (base, cache) ->
                in.of(CelestialEvent.BLOOD_MOON) > 0.5F || in.of(CelestialEvent.SILVER_MOON) > 0.5F ? MoonPhase.FULL_MOON : base);
        builder.addTimeBasedLayer(EnvironmentAttributes.MONSTERS_BURN, (base, cache) -> in.of(CelestialEvent.ECLIPSE) > 0.5F ? Boolean.FALSE : base);
        builder.addTimeBasedLayer(EnvironmentAttributes.BED_RULE, (base, cache) -> in.of(CelestialEvent.BLOOD_MOON) > 0.5F ? NO_SLEEP_BLOOD : base);
        return builder;
    }

    private record Tint(CelestialEvent event, int rgb, float strength) {}

    private static Tint tint(CelestialEvent event, int rgb, float strength) {
        return new Tint(event, 0xFF000000 | rgb, strength);
    }

    /** Mistura a cor de cada evento por cima da cor original, na força × intensidade do evento. */
    private static void color(EnvironmentAttributeSystem.Builder builder, EnvironmentAttribute<Integer> attribute, Intensity in, Tint[] tints) {
        builder.addTimeBasedLayer(attribute, (base, cache) -> {
            int c = base;
            for (Tint t : tints) {
                float k = in.of(t.event()) * t.strength();
                if (k > 0.001F) {
                    c = ARGB.srgbLerp(k, c, (c & 0xFF000000) | (t.rgb() & 0xFFFFFF));
                }
            }
            return c;
        });
    }
}

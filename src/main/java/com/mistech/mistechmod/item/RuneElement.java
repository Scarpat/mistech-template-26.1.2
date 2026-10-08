package com.mistech.mistechmod.item;

import java.util.List;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.registry.MistechEffects;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * As runas (10 básicas + 3 avançadas + 2 complexas + Eco) e o que cada uma faz num Charuto Mágico (tabaco: defesa) e num Baseado Mágico (cânhamo: poder do elemento).
 * Cada efeito dura {@link #DURATION_TICKS} e é renovado a cada tragada.
 */
public enum RuneElement {
    FIRE("fire", 0xFF6A1F, () -> Mistech.FIRE_RUNE.get(),
            List.of(e(MobEffects.FIRE_RESISTANCE, 0)),
            List.of(e(MobEffects.STRENGTH, 0), e(MistechEffects.FLAME_TOUCH, 0))),
    WATER("water", 0x3F8CFF, () -> Mistech.WATER_RUNE.get(),
            List.of(e(MobEffects.WATER_BREATHING, 0)),
            List.of(e(MobEffects.DOLPHINS_GRACE, 0), e(MobEffects.CONDUIT_POWER, 0))),
    EARTH("earth", 0x8A6A3A, () -> Mistech.EARTH_RUNE.get(),
            List.of(e(MobEffects.RESISTANCE, 0)),
            List.of(e(MobEffects.HASTE, 1))),
    AIR("air", 0xDDF4FF, () -> Mistech.AIR_RUNE.get(),
            List.of(e(MobEffects.SLOW_FALLING, 0)),
            List.of(e(MobEffects.SPEED, 1), e(MobEffects.JUMP_BOOST, 1))),
    ICE("ice", 0x9FE3FF, () -> Mistech.ICE_RUNE.get(),
            List.of(e(MistechEffects.FROST_WARD, 0)),
            List.of(e(MistechEffects.FROST_TOUCH, 0))),
    ENERGY("energy", 0xFFE14A, () -> Mistech.ENERGY_RUNE.get(),
            List.of(e(MobEffects.ABSORPTION, 1)),
            List.of(e(MobEffects.HASTE, 0), e(MobEffects.STRENGTH, 0))),
    LIGHT("light", 0xFFF6C0, () -> Mistech.LIGHT_RUNE.get(),
            List.of(e(MobEffects.HEALTH_BOOST, 1)),
            List.of(e(MobEffects.NIGHT_VISION, 0), e(MobEffects.LUCK, 0))),
    DARKNESS("darkness", 0x5A2E78, () -> Mistech.DARKNESS_RUNE.get(),
            List.of(e(MobEffects.INVISIBILITY, 0)),
            List.of(e(MistechEffects.SHADOW_TOUCH, 0))),
    SPACE("space", 0x8A4FD8, () -> Mistech.SPACE_RUNE.get(),
            List.of(e(MistechEffects.SPACE_DODGE, 0)),
            List.of(e(MistechEffects.EXTENDED_REACH, 0))),
    TIME("time", 0x5FD6B0, () -> Mistech.TIME_RUNE.get(),
            List.of(e(MobEffects.REGENERATION, 0)),
            List.of(e(MobEffects.SPEED, 0), e(MobEffects.HASTE, 1))),

    // ----- runas avançadas (combinam elementos básicos; a afinidade vai para os componentes) -----
    MAGMA("magma", 0xE0521B, () -> Mistech.MAGMA_RUNE.get(),
            List.of(e(MobEffects.FIRE_RESISTANCE, 0), e(MobEffects.RESISTANCE, 0)),
            List.of(e(MistechEffects.FLAME_TOUCH, 0), e(MobEffects.STRENGTH, 0), e(MobEffects.HASTE, 1)),
            List.of("fire", "earth")),
    VAPOR("vapor", 0xC9D8E8, () -> Mistech.VAPOR_RUNE.get(),
            List.of(e(MobEffects.FIRE_RESISTANCE, 0), e(MobEffects.WATER_BREATHING, 0), e(MobEffects.SLOW_FALLING, 0)),
            List.of(e(MobEffects.SPEED, 1), e(MobEffects.JUMP_BOOST, 1), e(MobEffects.DOLPHINS_GRACE, 0)),
            List.of("fire", "water")),
    VOID("void", 0x3B1E66, () -> Mistech.VOID_RUNE.get(),
            List.of(e(MistechEffects.SPACE_DODGE, 0), e(MobEffects.RESISTANCE, 0)),
            List.of(e(MistechEffects.EXTENDED_REACH, 0), e(MistechEffects.SHADOW_TOUCH, 0), e(MobEffects.HASTE, 0)),
            List.of("space", "darkness", "earth")),

    // ----- runas complexas (feitas em ritual; a afinidade vai para a runa-catalisador) -----
    LUCK("luck", 0x4FC95B, () -> Mistech.LUCK_RUNE.get(),
            List.of(e(MobEffects.LUCK, 0), e(MobEffects.HERO_OF_THE_VILLAGE, 0), e(MobEffects.REGENERATION, 0)),
            List.of(e(MobEffects.LUCK, 1), e(MistechEffects.FORTUNE, 0), e(MobEffects.HASTE, 0)),
            List.of("time")),
    ARCANE("arcane", 0xB05CFF, () -> Mistech.ARCANE_RUNE.get(),
            List.of(e(MistechEffects.ARCANE_WARD, 0), e(MobEffects.ABSORPTION, 1)),
            List.of(e(MistechEffects.ARCANE_SURGE, 0), e(MobEffects.STRENGTH, 0), e(MobEffects.SPEED, 0)),
            List.of("energy")),

    // ----- runa do Deep Dark (ritual Tier I; a afinidade vai para Ar + Escuridão) -----
    ECHO("echo", 0x0F5E5E, () -> com.mistech.mistechmod.beyond.BeyondContent.ECHO_RUNE.get(),
            List.of(e(MistechEffects.SILENT_STEP, 0), e(MobEffects.RESISTANCE, 0)),
            List.of(e(MistechEffects.ECHOLOCATION, 0), e(MobEffects.NIGHT_VISION, 0)),
            List.of("air", "darkness")),

    // ----- runas do Jardim Pálido e do Nether (Mesa de Escrita, como as avançadas) -----
    VIGIL("vigil", 0xE8E2D6, () -> com.mistech.mistechmod.beyond.PaleNetherContent.VIGIL_RUNE.get(),
            List.of(e(MistechEffects.VIGIL_WARD, 0), e(MobEffects.NIGHT_VISION, 0)),
            List.of(e(MistechEffects.HEAVY_GAZE, 0), e(MobEffects.STRENGTH, 0)),
            List.of("darkness", "earth")),
    SOUL("soul", 0x4FD7E6, () -> com.mistech.mistechmod.beyond.PaleNetherContent.SOUL_RUNE.get(),
            List.of(e(MobEffects.FIRE_RESISTANCE, 0), e(MistechEffects.SOUL_VEIL, 0)),
            List.of(e(MistechEffects.SOUL_TOUCH, 0), e(MobEffects.SPEED, 0)),
            List.of("fire", "darkness")),

    // ----- runa do céu (ritual Tier II: Vapor + Ar; a afinidade vai para Ar + Vapor = Ar, Fogo, Água) -----
    CLOUD("cloud", 0xE6F0FF, () -> com.mistech.mistechmod.sky.SkyContent.CLOUD_RUNE.get(),
            List.of(e(MobEffects.SLOW_FALLING, 0), e(MobEffects.JUMP_BOOST, 0)),
            List.of(e(MobEffects.SPEED, 0), e(MobEffects.JUMP_BOOST, 2), e(MobEffects.SLOW_FALLING, 0)),
            List.of("air", "vapor")),

    // ----- runa da vida (ritual Tier II a partir da Runa do Tempo; a afinidade vai para Água + Terra) -----
    LIFE("life", 0x5CD65C, () -> com.mistech.mistechmod.life.LifeContent.LIFE_RUNE.get(),
            List.of(e(MobEffects.REGENERATION, 0), e(MobEffects.ABSORPTION, 0)),
            List.of(e(MobEffects.SATURATION, 0), e(MobEffects.HEALTH_BOOST, 0)),
            List.of("water", "earth"));

    /** 90 segundos. */
    public static final int DURATION_TICKS = 90 * 20;

    public record Effect(Holder<MobEffect> effect, int amplifier) {}

    private static Effect e(Holder<MobEffect> effect, int amplifier) {
        return new Effect(effect, amplifier);
    }

    private final String id;
    private final int color;
    private final Supplier<Item> rune;
    private final List<Effect> cigarEffects;
    private final List<Effect> jointEffects;

    /** Ids dos elementos básicos que formam uma runa avançada (vazio nas básicas). */
    private final List<String> componentIds;

    RuneElement(String id, int color, Supplier<Item> rune, List<Effect> cigarEffects, List<Effect> jointEffects) {
        this(id, color, rune, cigarEffects, jointEffects, List.of());
    }

    RuneElement(String id, int color, Supplier<Item> rune, List<Effect> cigarEffects, List<Effect> jointEffects, List<String> componentIds) {
        this.componentIds = componentIds;
        this.id = id;
        this.color = color;
        this.rune = rune;
        this.cigarEffects = cigarEffects;
        this.jointEffects = jointEffects;
    }

    public String id() {
        return this.id;
    }

    /** Runa avançada (Magma, Vapor, Vazio)? Não tem afinidade própria: vai para os componentes. */
    public boolean isAdvanced() {
        return !this.componentIds.isEmpty();
    }

    /** Elementos básicos que formam esta runa (só ela mesma, se for básica). */
    public List<RuneElement> components() {
        if (this.componentIds.isEmpty()) {
            return List.of(this);
        }
        return this.componentIds.stream().map(RuneElement::byId).toList();
    }

    public int color() {
        return this.color;
    }

    public Item rune() {
        return this.rune.get();
    }

    public List<Effect> effects(boolean tobacco) {
        return tobacco ? this.cigarEffects : this.jointEffects;
    }

    public String translationKey() {
        return "rune_element.mistech." + this.id;
    }

    public static @Nullable RuneElement byId(@Nullable String id) {
        for (RuneElement element : values()) {
            if (element.id.equals(id)) {
                return element;
            }
        }
        return null;
    }

    /** Qual elemento é esta runa (null se não for runa). */
    public static @Nullable RuneElement ofRune(ItemStack stack) {
        for (RuneElement element : values()) {
            if (stack.is(element.rune())) {
                return element;
            }
        }
        return null;
    }
}

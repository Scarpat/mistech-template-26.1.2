package com.mistech.mistechmod.ritual;

import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.MoonPhase;

/** Quando o ritual pode ser feito. */
public enum RitualCondition implements StringRepresentable {
    NONE("none"),
    NIGHT("night"),
    DAY("day"),
    RAIN("rain"),
    FULL_MOON("full_moon"),
    THUNDER("thunder"),
    NEW_MOON("new_moon"),
    /** Durante a Lua de Sangue. */
    BLOOD_MOON("blood_moon"),
    /** Durante a Lua de Prata. */
    SILVER_MOON("silver_moon"),
    /** Durante um Eclipse. */
    ECLIPSE("eclipse"),
    /** De dia, num dia de lua nova (quando um eclipse pode acontecer). */
    NEW_MOON_DAY("new_moon_day"),
    /** À noite, num bioma frio (onde neva). */
    COLD_NIGHT("cold_night"),
    /** À noite, sem chuva. */
    CLEAR_NIGHT("clear_night"),
    /** Sexta-feira à noite (o dia do mundo % 7 == 5). */
    FRIDAY_NIGHT("friday_night"),
    /** Altar no Nether. */
    NETHER("nether"),
    /** Altar num bioma Deep Dark (Escuro Profundo). */
    DEEP_DARK("deep_dark"),
    /** À noite, com o altar no Jardim Pálido (a hora em que os Rangedores acordam). */
    PALE_GARDEN("pale_garden"),
    /** No amanhecer (a hora em que o Ascendente nasce no horizonte; mesma janela da Mesa de Astrologia). */
    DAWN("dawn");

    public static final Codec<RitualCondition> CODEC = StringRepresentable.fromEnum(RitualCondition::values);

    private final String name;

    RitualCondition(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public boolean test(Level level, BlockPos pos) {
        boolean night = level.isDarkOutside();
        return switch (this) {
            case NONE -> true;
            case NIGHT -> night;
            case DAY -> !night;
            case RAIN -> level.isRaining();
            case FULL_MOON -> night && level.environmentAttributes().getValue(EnvironmentAttributes.MOON_PHASE, pos) == MoonPhase.FULL_MOON;
            case THUNDER -> level.isThundering();
            case NEW_MOON -> night && level.environmentAttributes().getValue(EnvironmentAttributes.MOON_PHASE, pos) == MoonPhase.NEW_MOON;
            case BLOOD_MOON -> com.mistech.mistechmod.celestial.Celestial.isActive(level, com.mistech.mistechmod.celestial.CelestialEvent.BLOOD_MOON);
            case SILVER_MOON -> com.mistech.mistechmod.celestial.Celestial.isActive(level, com.mistech.mistechmod.celestial.CelestialEvent.SILVER_MOON);
            case ECLIPSE -> com.mistech.mistechmod.celestial.Celestial.isActive(level, com.mistech.mistechmod.celestial.CelestialEvent.ECLIPSE);
            case NEW_MOON_DAY -> !night && level.environmentAttributes().getValue(EnvironmentAttributes.MOON_PHASE, pos) == MoonPhase.NEW_MOON;
            case COLD_NIGHT -> night && level.getBiome(pos).value().coldEnoughToSnow(pos, level.getSeaLevel());
            case CLEAR_NIGHT -> night && !level.isRaining();
            case FRIDAY_NIGHT -> night && Math.floorMod(Math.floorDiv(level.getDefaultClockTime(), 24000L), 7L) == 5L;
            case NETHER -> level.dimension() == Level.NETHER;
            case DEEP_DARK -> level.getBiome(pos).is(net.minecraft.world.level.biome.Biomes.DEEP_DARK);
            case PALE_GARDEN -> night && level.getBiome(pos).is(net.minecraft.world.level.biome.Biomes.PALE_GARDEN);
            case DAWN -> com.mistech.mistechmod.astrology.NatalService.isDawn(level);
        };
    }

    public String translationKey() {
        return "ritual.mistech.condition." + this.name;
    }
}

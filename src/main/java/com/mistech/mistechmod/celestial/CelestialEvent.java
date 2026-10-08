package com.mistech.mistechmod.celestial;

import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;

/**
 * Eventos do céu chamados pelos rituais (ou que acontecem sozinhos). Cada um vale para o overworld inteiro
 * dentro de uma janela do relógio do mundo (ver {@link CelestialData}).
 */
public enum CelestialEvent implements StringRepresentable {
    /** Véu da Noite Longa: as noites duram o dobro. */
    LONG_NIGHT("long_night"),
    /** Solstício: os dias duram o dobro, o sol limpa mais Mácula e as plantas crescem mais. */
    SOLSTICE("solstice"),
    /** Lua de Sangue: céu vermelho, monstros mais fortes e em maior número, ninguém dorme, Sangue Lunar. */
    BLOOD_MOON("blood_moon"),
    /** Lua de Prata: noite calma, mortos-vivos queimam sob a lua, sem Pisadeira, Cristalizador a 100%. */
    SILVER_MOON("silver_moon"),
    /** Eclipse: o sol escurece durante um dia; monstros nascem e não queimam. */
    ECLIPSE("eclipse"),
    /** Noite Eterna (só na Lua de Sangue): o relógio quase para. */
    ETERNAL_NIGHT("eternal_night"),
    /** Chuva de Estrelas: meteoros caem perto dos jogadores com Fragmentos Estelares e minérios raros. */
    STARFALL("starfall"),
    /** Aurora: céu esverdeado; Sorte e Regeneração para quem está a céu aberto. */
    AURORA("aurora"),
    /** Chuva Bendita: quem fica na chuva perde Mácula e o Solo Maculado vai se limpando. */
    BLESSED_RAIN("blessed_rain");

    public static final Codec<CelestialEvent> CODEC = StringRepresentable.fromEnum(CelestialEvent::values);

    private final String name;

    CelestialEvent(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public int bit() {
        return 1 << this.ordinal();
    }

    public String translationKey() {
        return "celestial.mistech." + this.name;
    }
}

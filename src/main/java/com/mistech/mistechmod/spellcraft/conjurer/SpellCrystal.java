package com.mistech.mistechmod.spellcraft.conjurer;

import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.spellcraft.Spell;
import com.mistech.mistechmod.spellcraft.Spells;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * O que um Cristal de Feitiço gravado guarda: a magia, o poder fixado no momento da gravação (afinidade do jogador ×
 * {@code Config.conjurerPower()}) e quem gravou (o dono do dano, do loot e da XP quando o Conjurador lança).
 * Valor imutável (record) porque vira componente de item.
 */
public record SpellCrystal(String spell, float power, UUID owner, String ownerName) {
    public static final Codec<SpellCrystal> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("spell").forGetter(SpellCrystal::spell),
            Codec.FLOAT.fieldOf("power").forGetter(SpellCrystal::power),
            UUIDUtil.CODEC.fieldOf("owner").forGetter(SpellCrystal::owner),
            Codec.STRING.fieldOf("owner_name").forGetter(SpellCrystal::ownerName)).apply(i, SpellCrystal::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SpellCrystal> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SpellCrystal::spell,
            ByteBufCodecs.FLOAT, SpellCrystal::power,
            UUIDUtil.STREAM_CODEC, SpellCrystal::owner,
            ByteBufCodecs.STRING_UTF8, SpellCrystal::ownerName,
            SpellCrystal::new);

    public @Nullable Spell resolve() {
        return Spells.byId(this.spell);
    }
}

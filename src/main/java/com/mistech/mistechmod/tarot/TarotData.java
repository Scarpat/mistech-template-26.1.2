package com.mistech.mistechmod.tarot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Arcanos que o jogador já derrotou no Plano Arcano (ids de {@link TarotArcana}). Imutável. */
public record TarotData(Set<String> defeated) {
    public static final TarotData EMPTY = new TarotData(Set.of());

    public static final MapCodec<TarotData> CODEC = Codec.STRING.listOf()
            .<TarotData>xmap(list -> new TarotData(Set.copyOf(list)), data -> new ArrayList<>(data.defeated()))
            .fieldOf("defeated");

    public static final StreamCodec<RegistryFriendlyByteBuf, TarotData> STREAM_CODEC = ByteBufCodecs
            .<ByteBuf, String, HashSet<String>>collection(HashSet::new, ByteBufCodecs.STRING_UTF8)
            .<RegistryFriendlyByteBuf>cast()
            .map(set -> new TarotData(Set.copyOf(set)), data -> new HashSet<>(data.defeated()));

    public boolean hasDefeated(TarotArcana arcana) {
        return this.defeated.contains(arcana.id());
    }

    public int count() {
        return this.defeated.size();
    }

    public TarotData with(TarotArcana arcana) {
        if (this.hasDefeated(arcana)) {
            return this;
        }
        Set<String> copy = new HashSet<>(this.defeated);
        copy.add(arcana.id());
        return new TarotData(Set.copyOf(copy));
    }
}

package com.mistech.mistechmod.spellcraft;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;

/** Magias que o jogador já aprendeu (ids de {@link Spell}, na ordem em que aprendeu). Imutável. */
public record SpellBook(List<String> learned) {
    public static final SpellBook EMPTY = new SpellBook(List.of());

    public static final Codec<SpellBook> CODEC = Codec.STRING.listOf().xmap(SpellBook::new, SpellBook::learned);
    public static final StreamCodec<ByteBuf, SpellBook> STREAM_CODEC =
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).map(SpellBook::new, SpellBook::learned);

    public SpellBook {
        learned = List.copyOf(learned);
    }

    public static SpellBook of(Player player) {
        return player.getData(SpellcraftContent.SPELL_BOOK);
    }

    public boolean knows(String id) {
        return this.learned.contains(id);
    }

    public SpellBook with(String id) {
        if (this.learned.contains(id)) {
            return this;
        }
        List<String> copy = new ArrayList<>(this.learned);
        copy.add(id);
        return new SpellBook(copy);
    }
}

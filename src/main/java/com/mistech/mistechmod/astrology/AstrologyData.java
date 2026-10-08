package com.mistech.mistechmod.astrology;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Estrelas desbloqueadas por um jogador ("constelacao:estrela"). Imutável: cada mudança cria um novo objeto (e sincroniza). */
public record AstrologyData(Set<String> unlocked) {
    public static final AstrologyData EMPTY = new AstrologyData(Set.of());

    public static final MapCodec<AstrologyData> CODEC = Codec.STRING.listOf()
            .<AstrologyData>xmap(list -> new AstrologyData(Set.copyOf(list)), data -> new ArrayList<>(data.unlocked()))
            .fieldOf("unlocked");

    public static final StreamCodec<RegistryFriendlyByteBuf, AstrologyData> STREAM_CODEC = ByteBufCodecs
            .<ByteBuf, String, HashSet<String>>collection(HashSet::new, ByteBufCodecs.STRING_UTF8)
            .<RegistryFriendlyByteBuf>cast()
            .map(set -> new AstrologyData(Set.copyOf(set)), data -> new HashSet<>(data.unlocked()));

    private static String key(int constellation, int star) {
        return Constellations.get(constellation).id() + ":" + star;
    }

    public boolean isUnlocked(int constellation, int star) {
        return unlocked.contains(key(constellation, star));
    }

    /** A estrela pode ser desbloqueada agora? (a anterior já foi, e ela ainda não) */
    public boolean isAvailable(int constellation, int star) {
        if (isUnlocked(constellation, star)) {
            return false;
        }
        int parent = Constellations.get(constellation).stars().get(star).parent();
        return parent < 0 || isUnlocked(constellation, parent);
    }

    public int countIn(int constellation) {
        int count = 0;
        for (int i = 0; i < Constellations.get(constellation).stars().size(); i++) {
            if (isUnlocked(constellation, i)) {
                count++;
            }
        }
        return count;
    }

    public AstrologyData with(int constellation, int star) {
        Set<String> copy = new HashSet<>(unlocked);
        copy.add(key(constellation, star));
        return new AstrologyData(Set.copyOf(copy));
    }
}

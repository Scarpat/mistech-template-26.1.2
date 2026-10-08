package com.mistech.mistechmod.affinity;

import java.util.HashMap;
import java.util.Map;

import com.mistech.mistechmod.item.RuneElement;
import com.mojang.serialization.Codec;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Pontos de afinidade elemental do jogador (id do elemento → pontos). Imutável. */
public record AffinityData(Map<String, Integer> points) {
    public static final AffinityData EMPTY = new AffinityData(Map.of());

    public static final Codec<AffinityData> CODEC = Codec.unboundedMap(Codec.STRING, Codec.INT)
            .xmap(AffinityData::new, AffinityData::points);
    public static final StreamCodec<ByteBuf, AffinityData> STREAM_CODEC =
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_INT)
                    .map(AffinityData::new, data -> new HashMap<>(data.points));

    public AffinityData {
        points = Map.copyOf(points);
    }

    public int get(RuneElement element) {
        return this.points.getOrDefault(element.id(), 0);
    }

    public AffinityData add(RuneElement element, int amount) {
        Map<String, Integer> copy = new HashMap<>(this.points);
        copy.merge(element.id(), amount, Integer::sum);
        return new AffinityData(copy);
    }

    public int total() {
        int sum = 0;
        for (int value : this.points.values()) {
            sum += value;
        }
        return sum;
    }
}

package com.mistech.mistechmod.astrology;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** O que está escrito num item Mapa Astral: de quem é e os signos (-1 = ainda não revelado quando foi copiado). */
public record NatalChartCopy(String owner, int sun, int moon, int rising) {
    public static final Codec<NatalChartCopy> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("owner").forGetter(NatalChartCopy::owner),
            Codec.INT.fieldOf("sun").forGetter(NatalChartCopy::sun),
            Codec.INT.fieldOf("moon").forGetter(NatalChartCopy::moon),
            Codec.INT.fieldOf("rising").forGetter(NatalChartCopy::rising)
    ).apply(i, NatalChartCopy::new));

    public static final StreamCodec<ByteBuf, NatalChartCopy> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, NatalChartCopy::owner,
            ByteBufCodecs.VAR_INT, NatalChartCopy::sun,
            ByteBufCodecs.VAR_INT, NatalChartCopy::moon,
            ByteBufCodecs.VAR_INT, NatalChartCopy::rising,
            NatalChartCopy::new);

    public int sign(NatalChart.Part part) {
        return switch (part) {
            case SUN -> this.sun;
            case MOON -> this.moon;
            case RISING -> this.rising;
        };
    }
}

package com.mistech.mistechmod.seeking;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Alvo do Pêndulo Localizador: o que procura e onde achou (vazio = não achou nada por perto). */
public record SeekTarget(String kind, Optional<GlobalPos> pos) {
    public static final Codec<SeekTarget> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("kind").forGetter(SeekTarget::kind),
            GlobalPos.CODEC.optionalFieldOf("pos").forGetter(SeekTarget::pos)
    ).apply(i, SeekTarget::new));
    public static final StreamCodec<ByteBuf, SeekTarget> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SeekTarget::kind,
            ByteBufCodecs.optional(GlobalPos.STREAM_CODEC), SeekTarget::pos,
            SeekTarget::new);
}

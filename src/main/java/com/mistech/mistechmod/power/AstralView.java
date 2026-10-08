package com.mistech.mistechmod.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** O que a Bateria de Bolso Astral mostra da rede (energia, teto, nome). Atualizado pelo servidor. */
public record AstralView(long energy, long capacity, String name) {
    public static final AstralView NONE = new AstralView(0L, 0L, "");

    public static final Codec<AstralView> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.LONG.fieldOf("energy").forGetter(AstralView::energy),
            Codec.LONG.fieldOf("capacity").forGetter(AstralView::capacity),
            Codec.STRING.fieldOf("name").forGetter(AstralView::name)
    ).apply(i, AstralView::new));

    public static final StreamCodec<ByteBuf, AstralView> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, AstralView::energy,
            ByteBufCodecs.VAR_LONG, AstralView::capacity,
            ByteBufCodecs.STRING_UTF8, AstralView::name,
            AstralView::new);

    public float fill() {
        return this.capacity <= 0 ? 0.0F : Math.min(1.0F, this.energy / (float) this.capacity);
    }

    /** Muda pouco (menos de 0,5% do teto, mesmo nome e teto)? Evita mandar o item ao cliente toda hora. */
    public boolean similar(AstralView other) {
        if (this.capacity != other.capacity || !this.name.equals(other.name)) {
            return false;
        }
        long step = Math.max(1L, this.capacity / 200L);
        return Math.abs(this.energy - other.energy) < step && (this.energy == 0) == (other.energy == 0);
    }
}

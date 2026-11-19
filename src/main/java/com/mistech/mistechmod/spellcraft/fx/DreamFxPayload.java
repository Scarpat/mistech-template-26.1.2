package com.mistech.mistechmod.spellcraft.fx;

import com.mistech.mistechmod.Mistech;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Servidor -> cliente: toca um efeito onírico de magia (ver {@link DreamFx}). O efeito inteiro é coreografado no
 * cliente, quadro a quadro; o servidor só diz qual ({@code kind}), onde ({@code a}), para onde/velocidade ({@code b}),
 * qual entidade seguir ({@code entityId}, -1 = nenhuma), um número livre ({@code power}) e o {@code id} do disparo.
 */
public record DreamFxPayload(int kind, int id, int entityId, float power, double ax, double ay, double az, double bx, double by, double bz)
        implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<DreamFxPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Mistech.MODID, "dream_fx"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DreamFxPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeVarInt(p.kind);
                buf.writeVarInt(p.id);
                buf.writeInt(p.entityId);
                buf.writeFloat(p.power);
                buf.writeDouble(p.ax);
                buf.writeDouble(p.ay);
                buf.writeDouble(p.az);
                buf.writeDouble(p.bx);
                buf.writeDouble(p.by);
                buf.writeDouble(p.bz);
            },
            buf -> new DreamFxPayload(buf.readVarInt(), buf.readVarInt(), buf.readInt(), buf.readFloat(),
                    buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble()));

    @Override
    public CustomPacketPayload.Type<DreamFxPayload> type() {
        return TYPE;
    }
}

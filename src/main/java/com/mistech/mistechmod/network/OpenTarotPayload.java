package com.mistech.mistechmod.network;

import com.mistech.mistechmod.Mistech;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Servidor -> cliente: abre a tela da Mesa de Tarologia. */
public record OpenTarotPayload(BlockPos tablePos) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<OpenTarotPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Mistech.MODID, "open_tarot"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenTarotPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, OpenTarotPayload::tablePos,
            OpenTarotPayload::new);

    @Override
    public CustomPacketPayload.Type<OpenTarotPayload> type() {
        return TYPE;
    }
}

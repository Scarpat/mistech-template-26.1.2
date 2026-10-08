package com.mistech.mistechmod.network;

import com.mistech.mistechmod.Mistech;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Servidor -> cliente: abre o Grimório Mistech. */
public record OpenGuidePayload() implements CustomPacketPayload {
    public static final OpenGuidePayload INSTANCE = new OpenGuidePayload();
    public static final CustomPacketPayload.Type<OpenGuidePayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Mistech.MODID, "open_guide"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenGuidePayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public CustomPacketPayload.Type<OpenGuidePayload> type() {
        return TYPE;
    }
}

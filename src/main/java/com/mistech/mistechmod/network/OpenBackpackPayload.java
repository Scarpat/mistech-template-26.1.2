package com.mistech.mistechmod.network;

import com.mistech.mistechmod.Mistech;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Cliente -> servidor: tecla B, abrir a mochila. */
public record OpenBackpackPayload() implements CustomPacketPayload {
    public static final OpenBackpackPayload INSTANCE = new OpenBackpackPayload();
    public static final CustomPacketPayload.Type<OpenBackpackPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Mistech.MODID, "open_backpack"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenBackpackPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public CustomPacketPayload.Type<OpenBackpackPayload> type() {
        return TYPE;
    }
}

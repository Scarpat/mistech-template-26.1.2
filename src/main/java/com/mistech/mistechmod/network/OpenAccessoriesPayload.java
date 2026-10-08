package com.mistech.mistechmod.network;

import com.mistech.mistechmod.Mistech;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Cliente -> servidor: tecla J, abrir os acessórios. */
public record OpenAccessoriesPayload() implements CustomPacketPayload {
    public static final OpenAccessoriesPayload INSTANCE = new OpenAccessoriesPayload();
    public static final CustomPacketPayload.Type<OpenAccessoriesPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Mistech.MODID, "open_accessories"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenAccessoriesPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public CustomPacketPayload.Type<OpenAccessoriesPayload> type() {
        return TYPE;
    }
}

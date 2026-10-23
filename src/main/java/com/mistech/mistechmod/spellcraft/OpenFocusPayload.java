package com.mistech.mistechmod.spellcraft;

import com.mistech.mistechmod.Mistech;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Servidor -> cliente: abre a tela dos espaços do item da mão (0 = principal, 1 = secundária). */
public record OpenFocusPayload(int hand) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<OpenFocusPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Mistech.MODID, "open_focus"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenFocusPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, OpenFocusPayload::hand, OpenFocusPayload::new);

    @Override
    public CustomPacketPayload.Type<OpenFocusPayload> type() {
        return TYPE;
    }
}

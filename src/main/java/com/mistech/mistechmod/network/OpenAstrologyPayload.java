package com.mistech.mistechmod.network;

import com.mistech.mistechmod.Mistech;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Servidor -> cliente: abre a tela do céu da Mesa de Astrologia. */
public record OpenAstrologyPayload(BlockPos tablePos) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<OpenAstrologyPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Mistech.MODID, "open_astrology"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenAstrologyPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, OpenAstrologyPayload::tablePos,
            OpenAstrologyPayload::new);

    @Override
    public CustomPacketPayload.Type<OpenAstrologyPayload> type() {
        return TYPE;
    }
}

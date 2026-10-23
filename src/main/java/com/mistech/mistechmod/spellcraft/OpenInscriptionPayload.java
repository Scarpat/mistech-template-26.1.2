package com.mistech.mistechmod.spellcraft;

import com.mistech.mistechmod.Mistech;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Servidor -> cliente: abre a tela da Mesa de Inscrição. */
public record OpenInscriptionPayload(BlockPos tablePos) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<OpenInscriptionPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Mistech.MODID, "open_inscription"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenInscriptionPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, OpenInscriptionPayload::tablePos, OpenInscriptionPayload::new);

    @Override
    public CustomPacketPayload.Type<OpenInscriptionPayload> type() {
        return TYPE;
    }
}

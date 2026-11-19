package com.mistech.mistechmod.spellcraft;

import com.mistech.mistechmod.Mistech;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Cliente -> servidor: inscrever um pergaminho da magia. O servidor confere tudo de novo. */
/** {@code crystal}: grava um Cristal de Feitiço em vez de inscrever um pergaminho. */
public record InscribePayload(BlockPos tablePos, String spellId, boolean crystal) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<InscribePayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Mistech.MODID, "inscribe_spell"));
    public static final StreamCodec<RegistryFriendlyByteBuf, InscribePayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, InscribePayload::tablePos,
            ByteBufCodecs.STRING_UTF8, InscribePayload::spellId,
            ByteBufCodecs.BOOL, InscribePayload::crystal,
            InscribePayload::new);

    @Override
    public CustomPacketPayload.Type<InscribePayload> type() {
        return TYPE;
    }
}

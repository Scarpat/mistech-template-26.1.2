package com.mistech.mistechmod.network;

import com.mistech.mistechmod.Mistech;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Cliente -> servidor, na tela do Mapa Astral: 0 = revelar Sol, 1 = Lua, 2 = Ascendente, 3 = copiar no papel. */
public record NatalActionPayload(BlockPos tablePos, int action) implements CustomPacketPayload {
    public static final int COPY = 3;

    public static final CustomPacketPayload.Type<NatalActionPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Mistech.MODID, "natal_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, NatalActionPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, NatalActionPayload::tablePos,
            ByteBufCodecs.VAR_INT, NatalActionPayload::action,
            NatalActionPayload::new);

    @Override
    public CustomPacketPayload.Type<NatalActionPayload> type() {
        return TYPE;
    }
}

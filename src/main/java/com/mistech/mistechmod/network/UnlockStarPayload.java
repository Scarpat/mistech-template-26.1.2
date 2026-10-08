package com.mistech.mistechmod.network;

import com.mistech.mistechmod.Mistech;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Cliente -> servidor: "quero desbloquear esta estrela" (o servidor confere tudo). */
public record UnlockStarPayload(BlockPos tablePos, int constellation, int star) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<UnlockStarPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Mistech.MODID, "unlock_star"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UnlockStarPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, UnlockStarPayload::tablePos,
            ByteBufCodecs.VAR_INT, UnlockStarPayload::constellation,
            ByteBufCodecs.VAR_INT, UnlockStarPayload::star,
            UnlockStarPayload::new);

    @Override
    public CustomPacketPayload.Type<UnlockStarPayload> type() {
        return TYPE;
    }
}

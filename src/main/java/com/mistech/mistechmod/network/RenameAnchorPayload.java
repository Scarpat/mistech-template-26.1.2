package com.mistech.mistechmod.network;

import com.mistech.mistechmod.Mistech;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Cliente -> servidor: dar um nome à Âncora Espacial {@code key} (o jogador precisa estar perto dela). */
public record RenameAnchorPayload(String key, String name) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<RenameAnchorPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Mistech.MODID, "rename_anchor"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RenameAnchorPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(64), RenameAnchorPayload::key,
            ByteBufCodecs.stringUtf8(64), RenameAnchorPayload::name,
            RenameAnchorPayload::new);

    @Override
    public CustomPacketPayload.Type<RenameAnchorPayload> type() {
        return TYPE;
    }
}

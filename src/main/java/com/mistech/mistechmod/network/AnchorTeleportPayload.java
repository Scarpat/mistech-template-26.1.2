package com.mistech.mistechmod.network;

import com.mistech.mistechmod.Mistech;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Cliente -> servidor: ir até a âncora {@code target}, saindo da âncora {@code source} (vazio = Orbe). */
public record AnchorTeleportPayload(String target, String source) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<AnchorTeleportPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Mistech.MODID, "anchor_teleport"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AnchorTeleportPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, AnchorTeleportPayload::target,
            ByteBufCodecs.STRING_UTF8, AnchorTeleportPayload::source,
            AnchorTeleportPayload::new);

    @Override
    public CustomPacketPayload.Type<AnchorTeleportPayload> type() {
        return TYPE;
    }
}

package com.mistech.mistechmod.network;

import java.util.List;

import com.mistech.mistechmod.Mistech;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Servidor -> cliente: abre a lista de Âncoras Espaciais. {@code source} vazio = aberta pelo Orbe de Translocação. */
public record OpenAnchorsPayload(String source, List<Entry> entries) implements CustomPacketPayload {
    public record Entry(String key, String name, Identifier dimension, BlockPos pos, int cost) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Entry::key,
                ByteBufCodecs.STRING_UTF8, Entry::name,
                Identifier.STREAM_CODEC, Entry::dimension,
                BlockPos.STREAM_CODEC, Entry::pos,
                ByteBufCodecs.VAR_INT, Entry::cost,
                Entry::new);
    }

    public static final CustomPacketPayload.Type<OpenAnchorsPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Mistech.MODID, "open_anchors"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenAnchorsPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, OpenAnchorsPayload::source,
            Entry.STREAM_CODEC.apply(ByteBufCodecs.list(256)), OpenAnchorsPayload::entries,
            OpenAnchorsPayload::new);

    @Override
    public CustomPacketPayload.Type<OpenAnchorsPayload> type() {
        return TYPE;
    }
}

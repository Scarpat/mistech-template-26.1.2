package com.mistech.mistechmod.network;

import com.mistech.mistechmod.Mistech;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Cliente -> servidor: ação na Mesa de Tarologia (o servidor confere tudo).
 * @param action 0 = invocar a Carta Invertida, 1 = forjar ({@link com.mistech.mistechmod.tarot.TarotService})
 * @param option opção da forja (0 ou 1)
 */
public record TarotActionPayload(BlockPos tablePos, int action, int arcana, int option) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<TarotActionPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Mistech.MODID, "tarot_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TarotActionPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, TarotActionPayload::tablePos,
            ByteBufCodecs.VAR_INT, TarotActionPayload::action,
            ByteBufCodecs.VAR_INT, TarotActionPayload::arcana,
            ByteBufCodecs.VAR_INT, TarotActionPayload::option,
            TarotActionPayload::new);

    @Override
    public CustomPacketPayload.Type<TarotActionPayload> type() {
        return TYPE;
    }
}

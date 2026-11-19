package com.mistech.mistechmod.purity.area;

import com.mistech.mistechmod.Mistech;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Servidor → cliente, a cada 5 s: Mácula do chunk do jogador ({@code here}) e a maior dos 3×3 chunks em volta
 * ({@code around}), em décimos. O cliente usa para a névoa, as partículas e o HUD do Pêndulo.
 */
public record AreaTaintPayload(int here, int around) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<AreaTaintPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Mistech.MODID, "area_taint"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AreaTaintPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, AreaTaintPayload::here,
            ByteBufCodecs.VAR_INT, AreaTaintPayload::around,
            AreaTaintPayload::new);

    @Override
    public CustomPacketPayload.Type<AreaTaintPayload> type() {
        return TYPE;
    }
}

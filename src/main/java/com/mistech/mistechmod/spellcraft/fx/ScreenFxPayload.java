package com.mistech.mistechmod.spellcraft.fx;

import com.mistech.mistechmod.Mistech;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Servidor -> cliente: tremor de câmera ({@code shake}, 0–1) e clarão colorido na tela ({@code flash} = cor, {@code flashTicks}). */
public record ScreenFxPayload(float shake, int shakeTicks, int flash, int flashTicks) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ScreenFxPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Mistech.MODID, "screen_fx"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ScreenFxPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, ScreenFxPayload::shake,
            ByteBufCodecs.VAR_INT, ScreenFxPayload::shakeTicks,
            ByteBufCodecs.INT, ScreenFxPayload::flash,
            ByteBufCodecs.VAR_INT, ScreenFxPayload::flashTicks,
            ScreenFxPayload::new);

    @Override
    public CustomPacketPayload.Type<ScreenFxPayload> type() {
        return TYPE;
    }
}

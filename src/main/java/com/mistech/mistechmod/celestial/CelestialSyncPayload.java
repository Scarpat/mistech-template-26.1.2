package com.mistech.mistechmod.celestial;

import com.mistech.mistechmod.Mistech;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Servidor → cliente: quais Eventos Celestes estão "acesos" agora (bit = ordinal do {@link CelestialEvent}). */
public record CelestialSyncPayload(int mask) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<CelestialSyncPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Mistech.MODID, "celestial_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CelestialSyncPayload> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.<RegistryFriendlyByteBuf>cast().map(CelestialSyncPayload::new, CelestialSyncPayload::mask);

    @Override
    public CustomPacketPayload.Type<CelestialSyncPayload> type() {
        return TYPE;
    }
}

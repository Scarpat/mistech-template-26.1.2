package com.mistech.mistechmod.network;

import com.mistech.mistechmod.Mistech;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Cliente -> servidor: Shift + roda do mouse com o Cajado do Gnomo na mão (direction = +1 ou -1). */
public record StaffScrollPayload(int direction) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<StaffScrollPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Mistech.MODID, "staff_scroll"));
    public static final StreamCodec<RegistryFriendlyByteBuf, StaffScrollPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, StaffScrollPayload::direction, StaffScrollPayload::new);

    @Override
    public CustomPacketPayload.Type<StaffScrollPayload> type() {
        return TYPE;
    }
}

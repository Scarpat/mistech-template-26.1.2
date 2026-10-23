package com.mistech.mistechmod.spellcraft;

import com.mistech.mistechmod.Mistech;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Cliente -> servidor: põe uma magia (ou limpa, com id vazio) num espaço do item da mão. O servidor confere. */
public record FocusSlotPayload(int hand, int slot, String spellId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<FocusSlotPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Mistech.MODID, "focus_slot"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FocusSlotPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, FocusSlotPayload::hand,
            ByteBufCodecs.VAR_INT, FocusSlotPayload::slot,
            ByteBufCodecs.STRING_UTF8, FocusSlotPayload::spellId,
            FocusSlotPayload::new);

    @Override
    public CustomPacketPayload.Type<FocusSlotPayload> type() {
        return TYPE;
    }
}

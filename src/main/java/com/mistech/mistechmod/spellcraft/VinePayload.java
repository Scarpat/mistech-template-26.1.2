package com.mistech.mistechmod.spellcraft;

import com.mistech.mistechmod.Mistech;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Servidor -> cliente: o Cipó prendeu (ou soltou). O movimento do jogador é calculado no próprio cliente (é ele quem
 * manda na posição do jogador), por isso a física da corda roda lá; o servidor só valida e desenha o fio.
 * {@code entityId} = criatura presa (-1 = bloco); {@code level} = nível de Vida (corda encolhe mais rápido); {@code kind} = variante.
 */
public record VinePayload(boolean active, double x, double y, double z, int entityId, int level, int kind) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<VinePayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Mistech.MODID, "vine"));
    public static final StreamCodec<RegistryFriendlyByteBuf, VinePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, VinePayload::active,
            ByteBufCodecs.DOUBLE, VinePayload::x,
            ByteBufCodecs.DOUBLE, VinePayload::y,
            ByteBufCodecs.DOUBLE, VinePayload::z,
            ByteBufCodecs.VAR_INT, VinePayload::entityId,
            ByteBufCodecs.VAR_INT, VinePayload::level,
            ByteBufCodecs.VAR_INT, VinePayload::kind,
            VinePayload::new);

    /** {@code kind}: 0 = Cipó, 1 = Cipó Rápido, 2 = Teia Viva (ao soltar, dá um impulso). */
    public static VinePayload off(int kind) {
        return new VinePayload(false, 0, 0, 0, -1, 0, kind);
    }

    @Override
    public CustomPacketPayload.Type<VinePayload> type() {
        return TYPE;
    }
}

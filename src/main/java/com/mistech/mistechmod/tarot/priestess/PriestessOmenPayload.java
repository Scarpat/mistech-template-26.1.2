package com.mistech.mistechmod.tarot.priestess;

import com.mistech.mistechmod.Mistech;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * Servidor → cliente: um presságio da Sacerdotisa. Com Cegueira o jogador quase não vê nada, então cada golpe do
 * modo Boaz também aparece como um brilho na borda da tela (do lado de onde vem) — desenhado por
 * {@code client.PriestessClient}.
 *
 * @param kind     {@link #GUST}, {@link #POMEGRANATE}, {@link #TIDE}, {@link #SILENCE}, {@link #BOAZ}, {@link #JACHIN}
 * @param yaw      direção (graus, como o yaw de uma entidade) de ONDE o golpe vem (só a Rajada usa)
 * @param duration ticks até o golpe
 * @param value    extra (Jachin: acúmulos de Intuição do jogador)
 */
public record PriestessOmenPayload(int kind, float yaw, int duration, int value) implements CustomPacketPayload {
    public static final int GUST = 0, POMEGRANATE = 1, TIDE = 2, SILENCE = 3, BOAZ = 4, JACHIN = 5;

    public static final CustomPacketPayload.Type<PriestessOmenPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Mistech.MODID, "priestess_omen"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PriestessOmenPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PriestessOmenPayload::kind,
            ByteBufCodecs.FLOAT, PriestessOmenPayload::yaw,
            ByteBufCodecs.VAR_INT, PriestessOmenPayload::duration,
            ByteBufCodecs.VAR_INT, PriestessOmenPayload::value,
            PriestessOmenPayload::new);

    @Override
    public CustomPacketPayload.Type<PriestessOmenPayload> type() {
        return TYPE;
    }

    public static void send(ServerPlayer player, int kind, float yaw, int duration, int value) {
        PacketDistributor.sendToPlayer(player, new PriestessOmenPayload(kind, yaw, duration, value));
    }

    @EventBusSubscriber(modid = Mistech.MODID)
    public static final class Registration {
        private Registration() {}

        @SubscribeEvent
        static void register(RegisterPayloadHandlersEvent event) {
            event.registrar("1").playToClient(TYPE, STREAM_CODEC);
        }
    }
}

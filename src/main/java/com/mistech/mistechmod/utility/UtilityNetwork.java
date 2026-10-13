package com.mistech.mistechmod.utility;

import com.mistech.mistechmod.Mistech;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Pacotes dos utilitários: Elevador Rúnico (pulo/agachar) e tragada do Narguilé. */
@EventBusSubscriber(modid = Mistech.MODID)
public final class UtilityNetwork {
    private UtilityNetwork() {}

    /** Cliente -> servidor: pulou ({@code up}) ou agachou em cima de um elevador. */
    public record ElevatorPayload(boolean up) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ElevatorPayload> TYPE =
                new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Mistech.MODID, "runic_elevator"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ElevatorPayload> STREAM_CODEC =
                StreamCodec.composite(ByteBufCodecs.BOOL, ElevatorPayload::up, ElevatorPayload::new);

        @Override
        public CustomPacketPayload.Type<ElevatorPayload> type() {
            return TYPE;
        }
    }

    @SubscribeEvent
    static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(ElevatorPayload.TYPE, ElevatorPayload.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                RunicElevatorBlock.travel(player, payload.up());
            }
        });
    }
}

package com.mistech.mistechmod.artifact;

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
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Pacotes dos artefatos: pulo duplo (Botas do Vento) e tecla do Anel Espacial. */
@EventBusSubscriber(modid = Mistech.MODID)
public final class ArtifactNetwork {
    private ArtifactNetwork() {}

    /** Cliente -> servidor: pulo extra no ar ({@code index} = 1 no segundo pulo, 2 no terceiro). */
    public record DoubleJumpPayload(int index) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<DoubleJumpPayload> TYPE =
                new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Mistech.MODID, "double_jump"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DoubleJumpPayload> STREAM_CODEC =
                StreamCodec.composite(ByteBufCodecs.VAR_INT, DoubleJumpPayload::index, DoubleJumpPayload::new);

        @Override
        public CustomPacketPayload.Type<DoubleJumpPayload> type() {
            return TYPE;
        }
    }

    /** Cliente -> servidor: tecla do Anel Espacial (abre a lista de Âncoras). */
    public record SpatialRingPayload() implements CustomPacketPayload {
        public static final SpatialRingPayload INSTANCE = new SpatialRingPayload();
        public static final CustomPacketPayload.Type<SpatialRingPayload> TYPE =
                new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Mistech.MODID, "spatial_ring"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SpatialRingPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

        @Override
        public CustomPacketPayload.Type<SpatialRingPayload> type() {
            return TYPE;
        }
    }

    @SubscribeEvent
    static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(DoubleJumpPayload.TYPE, DoubleJumpPayload.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                ArtifactEvents.doubleJump(player, payload.index());
            }
        });
        registrar.playToServer(SpatialRingPayload.TYPE, SpatialRingPayload.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                ArtifactEvents.openSpatialRing(player);
            }
        });
    }
}

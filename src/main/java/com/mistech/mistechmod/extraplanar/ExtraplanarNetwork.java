package com.mistech.mistechmod.extraplanar;

import com.mistech.mistechmod.Mistech;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Cliente -> servidor: o código digitado na tela do Barril/Tanque Extraplanar. */
@EventBusSubscriber(modid = Mistech.MODID)
public final class ExtraplanarNetwork {
    private ExtraplanarNetwork() {}

    public record SetCodePayload(BlockPos pos, String code) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<SetCodePayload> TYPE =
                new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Mistech.MODID, "extraplanar_code"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SetCodePayload> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, SetCodePayload::pos,
                ByteBufCodecs.stringUtf8(ExtraplanarData.MAX_CODE_LENGTH * 4), SetCodePayload::code,
                SetCodePayload::new);

        @Override
        public CustomPacketPayload.Type<SetCodePayload> type() {
            return TYPE;
        }
    }

    @SubscribeEvent
    static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(SetCodePayload.TYPE, SetCodePayload.STREAM_CODEC, (payload, context) -> {
            if (!(context.player() instanceof ServerPlayer player) || !player.level().isLoaded(payload.pos())
                    || player.distanceToSqr(payload.pos().getCenter()) > 100.0) {
                return;
            }
            if (player.level().getBlockEntity(payload.pos()) instanceof CodedBlockEntity coded) {
                coded.setCode(payload.code());
                String now = coded.getCode();
                player.sendOverlayMessage(now.isEmpty() ? Component.translatable("message.mistech.extraplanar.code_cleared")
                        : Component.translatable("message.mistech.extraplanar.code_set", now));
            }
        });
    }
}

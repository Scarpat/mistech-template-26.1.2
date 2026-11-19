package com.mistech.mistechmod.spellcraft;

import com.mistech.mistechmod.Mistech;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Pacotes do sistema de magias (o handler do cliente fica em {@link com.mistech.mistechmod.spellcraft.client.SpellClient}). */
@EventBusSubscriber(modid = Mistech.MODID)
public final class SpellNetwork {
    private SpellNetwork() {}

    @SubscribeEvent
    static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToClient(OpenInscriptionPayload.TYPE, OpenInscriptionPayload.STREAM_CODEC);
        registrar.playToClient(OpenFocusPayload.TYPE, OpenFocusPayload.STREAM_CODEC);
        registrar.playToClient(VinePayload.TYPE, VinePayload.STREAM_CODEC);
        registrar.playToClient(com.mistech.mistechmod.spellcraft.fx.ScreenFxPayload.TYPE, com.mistech.mistechmod.spellcraft.fx.ScreenFxPayload.STREAM_CODEC);
        registrar.playToClient(com.mistech.mistechmod.spellcraft.fx.DreamFxPayload.TYPE, com.mistech.mistechmod.spellcraft.fx.DreamFxPayload.STREAM_CODEC);
        registrar.playToServer(FocusSlotPayload.TYPE, FocusSlotPayload.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                SpellFocusItem.assign(player, payload.hand(), payload.slot(), payload.spellId());
            }
        });
        registrar.playToServer(InscribePayload.TYPE, InscribePayload.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                if (payload.crystal()) {
                    InscriptionService.tryEngrave(player, payload.tablePos(), payload.spellId());
                } else {
                    InscriptionService.tryInscribe(player, payload.tablePos(), payload.spellId());
                }
            }
        });
    }
}

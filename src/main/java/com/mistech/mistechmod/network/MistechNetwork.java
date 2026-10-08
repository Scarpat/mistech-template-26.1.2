package com.mistech.mistechmod.network;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.astrology.AstrologyService;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Registro dos pacotes do mod (o handler do cliente fica em MistechClient). */
@EventBusSubscriber(modid = Mistech.MODID)
public final class MistechNetwork {
    private MistechNetwork() {}

    @SubscribeEvent
    static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(UnlockStarPayload.TYPE, UnlockStarPayload.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                AstrologyService.tryUnlock(player, payload.tablePos(), payload.constellation(), payload.star());
            }
        });
        registrar.playToClient(OpenAstrologyPayload.TYPE, OpenAstrologyPayload.STREAM_CODEC);
        registrar.playToServer(NatalActionPayload.TYPE, NatalActionPayload.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                if (payload.action() == NatalActionPayload.COPY) {
                    com.mistech.mistechmod.astrology.NatalService.tryCopy(player, payload.tablePos());
                } else if (payload.action() >= 0 && payload.action() < 3) {
                    com.mistech.mistechmod.astrology.NatalService.tryReveal(player, payload.tablePos(),
                            com.mistech.mistechmod.astrology.NatalChart.Part.byIndex(payload.action()));
                }
            }
        });
        registrar.playToClient(OpenAnchorsPayload.TYPE, OpenAnchorsPayload.STREAM_CODEC);
        registrar.playToServer(AnchorTeleportPayload.TYPE, AnchorTeleportPayload.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                com.mistech.mistechmod.spatial.SpatialService.tryTeleport(player, payload.target(), payload.source());
            }
        });
        registrar.playToServer(RenameAnchorPayload.TYPE, RenameAnchorPayload.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                com.mistech.mistechmod.spatial.SpatialService.tryRename(player, payload.key(), payload.name());
            }
        });
        registrar.playToClient(OpenGuidePayload.TYPE, OpenGuidePayload.STREAM_CODEC);
        registrar.playToClient(com.mistech.mistechmod.celestial.CelestialSyncPayload.TYPE, com.mistech.mistechmod.celestial.CelestialSyncPayload.STREAM_CODEC);
        registrar.playToClient(ConfectorRecipesPayload.TYPE, ConfectorRecipesPayload.STREAM_CODEC);
        registrar.playToServer(StaffScrollPayload.TYPE, StaffScrollPayload.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                if (com.mistech.mistechmod.ranged.ArcaneCrossbowItem.is(player.getMainHandItem())
                        || com.mistech.mistechmod.ranged.ArcaneCrossbowItem.is(player.getOffhandItem())
                        && !com.mistech.mistechmod.item.staff.GnomeStaffItem.holdsRunes(player.getMainHandItem())
                        && !com.mistech.mistechmod.tarot.magus.MagusStaffItem.is(player.getMainHandItem())) {
                    com.mistech.mistechmod.ranged.ArcaneCrossbowItem.scroll(player, payload.direction());
                } else if (com.mistech.mistechmod.tarot.magus.MagusStaffItem.is(player.getMainHandItem())
                        || com.mistech.mistechmod.tarot.magus.MagusStaffItem.is(player.getOffhandItem())
                        && !com.mistech.mistechmod.item.staff.GnomeStaffItem.holdsRunes(player.getMainHandItem())) {
                    com.mistech.mistechmod.tarot.magus.MagusStaffItem.scroll(player, payload.direction());
                } else {
                    com.mistech.mistechmod.item.staff.GnomeStaffItem.scroll(player, payload.direction());
                }
            }
        });
        registrar.playToServer(OpenAccessoriesPayload.TYPE, OpenAccessoriesPayload.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                player.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inventory, p) ->
                        new com.mistech.mistechmod.accessory.AccessoriesMenu(id, inventory,
                                new com.mistech.mistechmod.accessory.AccessoriesMenu.PlayerAccessories(p)),
                        net.minecraft.network.chat.Component.translatable("container.mistech.accessories")));
            }
        });
        // Tarô
        registrar.playToClient(OpenTarotPayload.TYPE, OpenTarotPayload.STREAM_CODEC);
        registrar.playToServer(TarotActionPayload.TYPE, TarotActionPayload.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player
                    && com.mistech.mistechmod.tarot.TarotArcana.isValid(payload.arcana())) {
                var arcana = com.mistech.mistechmod.tarot.TarotArcana.byIndex(payload.arcana());
                if (payload.action() == com.mistech.mistechmod.tarot.TarotService.ACTION_INVOKE) {
                    com.mistech.mistechmod.tarot.TarotService.tryInvoke(player, payload.tablePos(), arcana);
                } else if (payload.action() == com.mistech.mistechmod.tarot.TarotService.ACTION_FORGE) {
                    com.mistech.mistechmod.tarot.TarotService.tryForge(player, payload.tablePos(), arcana, payload.option());
                }
            }
        });
        registrar.playToServer(OpenBackpackPayload.TYPE, OpenBackpackPayload.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                com.mistech.mistechmod.item.BackpackItem.openFirst(player);
            }
        });
    }
}

package com.mistech.mistechmod.archive.client;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.archive.ArchiveContent;
import com.mistech.mistechmod.archive.ArchiveNetwork;
import com.mistech.mistechmod.archive.ArchiveTerminalMenu;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = Mistech.MODID, value = Dist.CLIENT)
public final class ArchiveClient {
    private ArchiveClient() {}

    @SubscribeEvent
    static void onScreens(RegisterMenuScreensEvent event) {
        event.register(ArchiveContent.TERMINAL_MENU.get(), ArchiveTerminalScreen::new);
    }

    @SubscribeEvent
    static void onPayloads(RegisterClientPayloadHandlersEvent event) {
        event.register(ArchiveNetwork.SyncPayload.TYPE, (payload, context) -> {
            var player = Minecraft.getInstance().player;
            if (player != null && player.containerMenu instanceof ArchiveTerminalMenu menu && menu.containerId == payload.containerId()) {
                menu.entries = payload.entries();
                menu.powered = payload.powered();
            }
        });
    }
}

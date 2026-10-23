package com.mistech.mistechmod.client;

import org.lwjgl.glfw.GLFW;

import com.mistech.mistechmod.Config;
import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.MistechClient;
import com.mistech.mistechmod.item.scroll.ScrollCaseContent;
import com.mistech.mistechmod.item.scroll.ScrollCaseItem;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** Tecla Z (trocar a runa do Porta-Pergaminhos; agachado = anterior) e agachado + roda do mouse. */
@EventBusSubscriber(modid = Mistech.MODID, value = Dist.CLIENT)
public final class ScrollCaseClient {
    public static final KeyMapping CYCLE = new KeyMapping("key.mistech.scroll_case_cycle",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Z, MistechClient.KEY_CATEGORY);

    private ScrollCaseClient() {}

    private static boolean holdingCase(LocalPlayer player) {
        return player.getMainHandItem().getItem() instanceof ScrollCaseItem || player.getOffhandItem().getItem() instanceof ScrollCaseItem;
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        while (CYCLE.consumeClick()) {
            if (mc.player != null && mc.screen == null && holdingCase(mc.player)) {
                ClientPacketDistributor.sendToServer(new ScrollCaseContent.CyclePayload(mc.player.isShiftKeyDown() ? -1 : 1));
            }
        }
    }

    @SubscribeEvent
    static void onScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null || !Config.sneakScrollCycles() || !mc.player.isShiftKeyDown()
                || !holdingCase(mc.player) || event.getScrollDeltaY() == 0) {
            return;
        }
        event.setCanceled(true);
        ClientPacketDistributor.sendToServer(new ScrollCaseContent.CyclePayload(event.getScrollDeltaY() > 0 ? -1 : 1));
    }
}

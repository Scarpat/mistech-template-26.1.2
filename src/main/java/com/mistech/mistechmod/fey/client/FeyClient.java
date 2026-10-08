package com.mistech.mistechmod.fey.client;

import org.lwjgl.glfw.GLFW;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.MistechClient;
import com.mistech.mistechmod.fey.CrownCall;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** Tecla do Chamado da Rainha (G). Registrada em {@link MistechClient}. */
@EventBusSubscriber(modid = Mistech.MODID, value = Dist.CLIENT)
public final class FeyClient {
    public static final KeyMapping CROWN_CALL = new KeyMapping("key.mistech.crown_call",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, MistechClient.KEY_CATEGORY);

    private FeyClient() {}

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        while (CROWN_CALL.consumeClick()) {
            if (minecraft.player != null && minecraft.screen == null) {
                ClientPacketDistributor.sendToServer(CrownCall.CrownCallPayload.INSTANCE);
            }
        }
    }
}

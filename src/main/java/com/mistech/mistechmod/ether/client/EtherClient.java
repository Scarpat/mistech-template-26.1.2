package com.mistech.mistechmod.ether.client;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.ether.EtherContent;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = Mistech.MODID, value = Dist.CLIENT)
public final class EtherClient {
    private EtherClient() {}

    @SubscribeEvent
    static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(EtherContent.PYLON_BE.get(), PylonRenderer::new);
    }
}

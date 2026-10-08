package com.mistech.mistechmod.golem.client;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.golem.GolemRegistry;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = Mistech.MODID, value = Dist.CLIENT)
public final class GolemClient {
    private GolemClient() {}

    @SubscribeEvent
    static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(GolemRegistry.WORK_GOLEM.get(), WorkGolemRenderer::new);
    }
}

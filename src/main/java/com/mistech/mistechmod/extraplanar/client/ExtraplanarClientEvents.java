package com.mistech.mistechmod.extraplanar.client;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.extraplanar.ExtraplanarContent;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = Mistech.MODID, value = Dist.CLIENT)
public final class ExtraplanarClientEvents {
    private ExtraplanarClientEvents() {}

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ExtraplanarContent.TANK_BE.get(), ExtraplanarTankRenderer::new);
    }
}

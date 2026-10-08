package com.mistech.mistechmod.ranged.client;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.ranged.RangedContent;

import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Cliente das armas de longe: o virote da Besta Mágica é desenhado como item (brilhando). */
@EventBusSubscriber(modid = Mistech.MODID, value = Dist.CLIENT)
public final class RangedClient {
    private RangedClient() {}

    @SubscribeEvent
    static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(RangedContent.ARCANE_BOLT_ENTITY.get(), ctx -> new ThrownItemRenderer<>(ctx, 1.2F, true));
    }
}

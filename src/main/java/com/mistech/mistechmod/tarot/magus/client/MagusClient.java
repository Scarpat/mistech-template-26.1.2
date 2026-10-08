package com.mistech.mistechmod.tarot.magus.client;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.tarot.magus.MagusContent;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Cliente do Mago: desenho do chefe, Aspectos, Pedras Rúnicas e magias. */
@EventBusSubscriber(modid = Mistech.MODID, value = Dist.CLIENT)
public final class MagusClient {
    private MagusClient() {}

    @SubscribeEvent
    static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(MagusContent.MAGUS.get(), MagusRenderer::new);
        event.registerEntityRenderer(MagusContent.ASPECT.get(), MagusAspectRenderer::new);
        event.registerEntityRenderer(MagusContent.RUNESTONE.get(), MagusRunestoneRenderer::new);
        event.registerEntityRenderer(MagusContent.SPELL.get(), MagusSpellRenderer::new);
    }

    @SubscribeEvent
    static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(MagusGeometry.BODY_LAYER, MagusGeometry::bodyLayer);
        event.registerLayerDefinition(MagusGeometry.FX_LAYER, MagusGeometry::fxLayer);
    }
}

package com.mistech.mistechmod.spellcraft.client;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.spellcraft.SpellcraftContent;

import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Renderers e modelos das invocações. */
@EventBusSubscriber(modid = Mistech.MODID, value = Dist.CLIENT)
public final class SummonClient {
    private SummonClient() {}

    private static Identifier tex(String name) {
        return Identifier.fromNamespaceAndPath(Mistech.MODID, "textures/entity/" + name + ".png");
    }

    @SubscribeEvent
    public static void onLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(SummonModels.STONE_GOLEM, SummonModels::stoneGolem);
        event.registerLayerDefinition(SummonModels.WOOD_GUARDIAN, SummonModels::woodGuardian);
    }

    @SubscribeEvent
    public static void onRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(SpellcraftContent.STONE_GOLEM.get(),
                ctx -> new SummonGolemRenderer(ctx, SummonModels.STONE_GOLEM, tex("stone_golem"), 0.9F));
        event.registerEntityRenderer(SpellcraftContent.WOOD_GUARDIAN.get(),
                ctx -> new SummonGolemRenderer(ctx, SummonModels.WOOD_GUARDIAN, tex("wood_guardian"), 0.7F));
        event.registerEntityRenderer(SpellcraftContent.SHADOW_WOLF.get(), ShadowWolfRenderer::new);
        event.registerEntityRenderer(SpellcraftContent.SPIRIT_GUARDIAN.get(), SpiritGuardianRenderer::new);
        event.registerEntityRenderer(SpellcraftContent.PHOENIX.get(), PhoenixRenderer::new);
    }
}

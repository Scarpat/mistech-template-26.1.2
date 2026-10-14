package com.mistech.mistechmod.golem.client;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.golem.GolemRegistry;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = Mistech.MODID, value = Dist.CLIENT)
public final class GolemClient {
    private GolemClient() {}

    @SubscribeEvent
    static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(GolemRegistry.WORK_GOLEM.get(), WorkGolemRenderer::new);
    }

    /** Segurando a Vara de Comando Rúnica: brilhos verdes nas origens e dourados nos destinos marcados. */
    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.isPaused() || mc.level.getGameTime() % 8 != 0) {
            return;
        }
        net.minecraft.world.item.ItemStack held = mc.player.getMainHandItem();
        if (!held.is(com.mistech.mistechmod.registry.GolemContent.RUNIC_GOLEM_WAND.get())) {
            held = mc.player.getOffhandItem();
            if (!held.is(com.mistech.mistechmod.registry.GolemContent.RUNIC_GOLEM_WAND.get())) {
                return;
            }
        }
        com.mistech.mistechmod.golem.GolemRoute route = com.mistech.mistechmod.item.RunicGolemWandItem.routeOf(held);
        net.minecraft.util.RandomSource random = mc.level.getRandom();
        for (net.minecraft.core.BlockPos p : route.sources()) {
            mc.level.addParticle(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                    p.getX() + random.nextDouble(), p.getY() + 1.05, p.getZ() + random.nextDouble(), 0.0, 0.0, 0.0);
        }
        for (net.minecraft.core.BlockPos p : route.targets()) {
            mc.level.addParticle(net.minecraft.core.particles.ParticleTypes.WAX_ON,
                    p.getX() + random.nextDouble(), p.getY() + 1.05, p.getZ() + random.nextDouble(), 0.0, 0.0, 0.0);
        }
    }
}

package com.mistech.mistechmod.client;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.registry.MistechAttachments;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

/** Forma de Névoa no cliente: não desenha o corpo (nem as mãos em primeira pessoa) de quem virou névoa. */
@EventBusSubscriber(modid = Mistech.MODID, value = Dist.CLIENT)
public final class MistFormClient {
    private MistFormClient() {}

    private static boolean isMist(Entity entity) {
        return entity instanceof Player player && player.getData(MistechAttachments.MIST_FORM);
    }

    @SubscribeEvent
    static void onRenderPlayer(RenderPlayerEvent.Pre<?> event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && isMist(mc.level.getEntity(event.getRenderState().id))) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onRenderHand(RenderHandEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && isMist(mc.player)) {
            event.setCanceled(true);
        }
    }
}

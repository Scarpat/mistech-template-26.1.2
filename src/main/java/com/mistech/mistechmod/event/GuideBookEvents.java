package com.mistech.mistechmod.event;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.registry.MistechAttachments;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Dá o Grimório Mistech na primeira vez que o jogador entra no mundo. */
@EventBusSubscriber(modid = Mistech.MODID)
public final class GuideBookEvents {
    private GuideBookEvents() {}

    @SubscribeEvent
    static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (!player.getData(MistechAttachments.RECEIVED_GUIDE)) {
            player.getInventory().placeItemBackInInventory(new ItemStack(Mistech.GUIDE_BOOK.get()));
            player.setData(MistechAttachments.RECEIVED_GUIDE, true);
        }
    }
}

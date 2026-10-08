package com.mistech.mistechmod.event;

import com.mistech.mistechmod.Mistech;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = Mistech.MODID)
public final class CraftingEvents {
    private CraftingEvents() {}

    /** A garrafa de água não tem "sobra" de receita no vanilla, então devolvemos o vidro vazio ao fazer o Giz de Cinzas ou o Giz Impuro. */
    @SubscribeEvent
    static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || !(event.getCrafting().is(Mistech.ASH_CHALK.get()) || event.getCrafting().is(Mistech.IMPURE_CHALK.get()))) {
            return;
        }
        if (!usedWaterBottle(event.getInventory())) {
            return;
        }
        ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
        if (!player.getInventory().add(bottle)) {
            player.drop(bottle, false);
        }
    }

    private static boolean usedWaterBottle(Container grid) {
        for (int i = 0; i < grid.getContainerSize(); i++) {
            ItemStack stack = grid.getItem(i);
            if (stack.is(Items.POTION)
                    && stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).is(Potions.WATER)) {
                return true;
            }
        }
        return false;
    }
}

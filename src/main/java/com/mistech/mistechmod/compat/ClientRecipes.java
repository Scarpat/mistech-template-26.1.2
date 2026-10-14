package com.mistech.mistechmod.compat;

import com.mistech.mistechmod.Mistech;

import net.minecraft.world.item.crafting.RecipeMap;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RecipesReceivedEvent;

/** Receitas que o servidor mandou para este cliente (ver {@link RecipeSync}). Usado pela integração com o JEI. */
@EventBusSubscriber(modid = Mistech.MODID, value = Dist.CLIENT)
public final class ClientRecipes {
    private static RecipeMap recipes = RecipeMap.EMPTY;

    private ClientRecipes() {}

    public static RecipeMap get() {
        return recipes;
    }

    @SubscribeEvent
    static void onRecipesReceived(RecipesReceivedEvent event) {
        recipes = event.getRecipeMap();
    }

    @SubscribeEvent
    static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        recipes = RecipeMap.EMPTY;
    }
}

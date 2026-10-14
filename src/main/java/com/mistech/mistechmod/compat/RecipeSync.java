package com.mistech.mistechmod.compat;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.registry.MistechRecipes;
import com.mistech.mistechmod.registry.RitualContent;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;

/**
 * Desde o 1.21.2 as receitas ficam só no servidor. Para o JEI (e outros visualizadores) mostrarem as receitas do Mistech,
 * o servidor manda as receitas de Mesa de Escrita, Triturador e Rituais para os clientes quando eles entram / no /reload.
 * O cliente guarda o que chegou em {@link ClientRecipes}.
 */
@EventBusSubscriber(modid = Mistech.MODID)
public final class RecipeSync {
    private RecipeSync() {}

    @SubscribeEvent
    static void onDatapackSync(OnDatapackSyncEvent event) {
        event.sendRecipes(MistechRecipes.WRITING_TYPE.get(), MistechRecipes.CRUSHING_TYPE.get(), RitualContent.RITUAL_TYPE.get());
    }
}

package com.mistech.mistechmod.drink.client;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.client.MachineScreen;
import com.mistech.mistechmod.drink.DrinkContent;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** Tela do Alambique (tela genérica das máquinas). */
@EventBusSubscriber(modid = Mistech.MODID, value = Dist.CLIENT)
public final class DrinkClient {
    private DrinkClient() {}

    @SubscribeEvent
    static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(DrinkContent.CANE_STILL_MENU.get(), MachineScreen::new);
    }
}

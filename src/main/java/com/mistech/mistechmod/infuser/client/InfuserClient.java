package com.mistech.mistechmod.infuser.client;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.infuser.InfuserContent;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = Mistech.MODID, value = Dist.CLIENT)
public final class InfuserClient {
    private InfuserClient() {}

    @SubscribeEvent
    static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(InfuserContent.ARCANE_INFUSER_MENU.get(), ArcaneInfuserScreen::new);
    }
}

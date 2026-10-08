package com.mistech.mistechmod.mage.client;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.client.MachineScreen;
import com.mistech.mistechmod.mage.MageContent;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** Tela do Tear Mágico (a mesma tela genérica das máquinas). */
@EventBusSubscriber(modid = Mistech.MODID, value = Dist.CLIENT)
public final class MageClient {
    private MageClient() {}

    @SubscribeEvent
    static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(MageContent.MAGIC_LOOM_MENU.get(), MachineScreen::new);
    }
}

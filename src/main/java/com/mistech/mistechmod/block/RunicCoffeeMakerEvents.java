package com.mistech.mistechmod.block;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.registry.MistechBlockEntities;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper;

/** Funis e canos na Cafeteira Rúnica: itens por qualquer lado (só tira as bebidas) e os 4 tanques de fluido. */
@EventBusSubscriber(modid = Mistech.MODID)
public final class RunicCoffeeMakerEvents {
    private RunicCoffeeMakerEvents() {}

    @SubscribeEvent
    static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Item.BLOCK, MistechBlockEntities.RUNIC_COFFEE_MAKER.get(),
                (maker, side) -> new WorldlyContainerWrapper(maker, side));
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, MistechBlockEntities.RUNIC_COFFEE_MAKER.get(),
                (maker, side) -> maker.getTanks());
    }
}

package com.mistech.mistechmod.extraplanar;

import com.mistech.mistechmod.Mistech;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;

/** Canos e funis: itens no Barril Extraplanar e fluido no Tanque Extraplanar (só quando têm código). */
@EventBusSubscriber(modid = Mistech.MODID)
public final class ExtraplanarEvents {
    private ExtraplanarEvents() {}

    @SubscribeEvent
    static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Item.BLOCK, ExtraplanarContent.BARREL_BE.get(),
                (barrel, side) -> barrel.isLinked() ? VanillaContainerWrapper.of(barrel) : null);
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, ExtraplanarContent.TANK_BE.get(),
                (tank, side) -> tank.linkedTank());
    }
}

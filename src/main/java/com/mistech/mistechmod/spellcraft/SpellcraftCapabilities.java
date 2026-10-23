package com.mistech.mistechmod.spellcraft;

import com.mistech.mistechmod.Mistech;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/** Expõe a energia do Carregador Arcano a cabos e geradores de qualquer mod. */
@EventBusSubscriber(modid = Mistech.MODID)
public final class SpellcraftCapabilities {
    private SpellcraftCapabilities() {}

    @SubscribeEvent
    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Energy.BLOCK, SpellcraftContent.ARCANE_CHARGER_BE.get(),
                (charger, side) -> charger.getEnergyHandler());
    }
}

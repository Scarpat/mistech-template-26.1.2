package com.mistech.mistechmod.machine;

import com.mistech.mistechmod.Mistech;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper;

/**
 * Rede de segurança para canos e funis de outros mods: todo bloco do Mistech cuja entidade é um {@link WorldlyContainer}
 * ganha a capability de itens, e toda máquina ({@link MachineBlockEntity}) a de energia — mesmo que ninguém tenha
 * registrado à mão (Fornalha Rúnica, Condensador, Incensário, Confector, Caldeira Ígnea, Reator Impuro...).
 * Roda por último: os registros específicos vêm antes e ganham.
 */
public final class CapabilityFallback {
    private CapabilityFallback() {}

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(EventPriority.LOWEST, CapabilityFallback::onCapabilities);
    }

    private static void onCapabilities(RegisterCapabilitiesEvent event) {
        Block[] blocks = BuiltInRegistries.BLOCK.stream()
                .filter(b -> Mistech.MODID.equals(BuiltInRegistries.BLOCK.getKey(b).getNamespace()))
                .toArray(Block[]::new);
        event.registerBlock(Capabilities.Item.BLOCK, (level, pos, state, be, side) ->
                be instanceof WorldlyContainer container ? new WorldlyContainerWrapper(container, side) : null, blocks);
        event.registerBlock(Capabilities.Energy.BLOCK, (level, pos, state, be, side) ->
                be instanceof MachineBlockEntity machine ? machine.getEnergyHandler() : null, blocks);
    }
}

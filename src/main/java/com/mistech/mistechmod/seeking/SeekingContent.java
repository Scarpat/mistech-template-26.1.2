package com.mistech.mistechmod.seeking;

import java.util.List;

import com.mistech.mistechmod.Mistech;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Pêndulo Localizador. */
public final class SeekingContent {
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Mistech.MODID);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SeekTarget>> TARGET =
            COMPONENTS.registerComponentType("seek_target", b -> b.persistent(SeekTarget.CODEC).networkSynchronized(SeekTarget.STREAM_CODEC));

    public static final DeferredItem<SeekingPendulumItem> SEEKING_PENDULUM = Mistech.ITEMS.registerItem("seeking_pendulum",
            SeekingPendulumItem::new, p -> p.durability(SeekingPendulumItem.USES).rarity(Rarity.UNCOMMON));

    private SeekingContent() {}

    public static List<DeferredItem<? extends Item>> creativeItems() {
        return List.of(SEEKING_PENDULUM);
    }

    public static void register(IEventBus modEventBus) {
        COMPONENTS.register(modEventBus);
    }
}

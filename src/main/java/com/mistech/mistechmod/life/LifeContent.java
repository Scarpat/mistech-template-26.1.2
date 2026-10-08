package com.mistech.mistechmod.life;

import java.util.List;

import com.mistech.mistechmod.Mistech;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;

/** Runa da Vida (ritual Tier II a partir da Runa do Tempo; Água + Terra). */
public final class LifeContent {
    public static final int LIFE_RUNE_USES = 16;

    public static final DeferredItem<LifeRuneItem> LIFE_RUNE = Mistech.ITEMS.registerItem("life_rune", LifeRuneItem::new,
            p -> p.durability(LIFE_RUNE_USES).rarity(Rarity.UNCOMMON));

    private LifeContent() {}

    public static List<DeferredItem<? extends Item>> creativeItems() {
        return List.of(LIFE_RUNE);
    }

    public static void register(IEventBus modEventBus) {
        // tudo nos registros do Mistech
    }
}

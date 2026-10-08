package com.mistech.mistechmod.registry;

import java.util.List;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.accessory.Accessory;
import com.mistech.mistechmod.accessory.AccessoryItem;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;

/** Anéis e amuletos. Bases: Aro de Ouro e Corrente com Engaste; gravação na Mesa de Escrita. */
public final class AccessoryContent {
    public static final DeferredItem<Item> GOLD_RING_BLANK = Mistech.ITEMS.registerSimpleItem("gold_ring_blank");
    public static final DeferredItem<Item> AMULET_BLANK = Mistech.ITEMS.registerSimpleItem("amulet_blank");

    public static final DeferredItem<AccessoryItem> VEIL_RING = accessory(Accessory.VEIL_RING);
    public static final DeferredItem<AccessoryItem> EMBER_RING = accessory(Accessory.EMBER_RING);
    public static final DeferredItem<AccessoryItem> BREEZE_RING = accessory(Accessory.BREEZE_RING);
    public static final DeferredItem<AccessoryItem> STONE_RING = accessory(Accessory.STONE_RING);
    public static final DeferredItem<AccessoryItem> TIDE_RING = accessory(Accessory.TIDE_RING);
    public static final DeferredItem<AccessoryItem> FROST_RING = accessory(Accessory.FROST_RING);
    public static final DeferredItem<AccessoryItem> CONSTELLATION_AMULET = accessory(Accessory.CONSTELLATION_AMULET);
    public static final DeferredItem<AccessoryItem> GNOME_AMULET = accessory(Accessory.GNOME_AMULET);
    public static final DeferredItem<AccessoryItem> ESSENCE_AMULET = accessory(Accessory.ESSENCE_AMULET);

    private AccessoryContent() {}

    private static DeferredItem<AccessoryItem> accessory(Accessory accessory) {
        return Mistech.ITEMS.registerItem(accessory.id, p -> new AccessoryItem(p, accessory),
                p -> p.stacksTo(1).rarity(accessory.ring ? Rarity.UNCOMMON : Rarity.RARE));
    }

    public static List<DeferredItem<? extends Item>> creativeItems() {
        return List.of(GOLD_RING_BLANK, AMULET_BLANK, VEIL_RING, EMBER_RING, BREEZE_RING, STONE_RING, TIDE_RING, FROST_RING,
                CONSTELLATION_AMULET, GNOME_AMULET, ESSENCE_AMULET);
    }

    /** Força o carregamento da classe (registra os itens). */
    public static void init() {}
}

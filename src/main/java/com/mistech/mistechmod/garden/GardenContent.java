package com.mistech.mistechmod.garden;

import java.util.List;
import java.util.function.Supplier;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.registry.MistechBlockEntities;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Mandrágora: Sementes (às vezes, ao colher plantações maduras à noite) → planta → Mandrágora (grita ao ser arrancada)
 * → no Pote de Vidro vira a Mandrágora Engarrafada, que faz as plantações em volta crescerem mais rápido.
 */
public final class GardenContent {
    public static final DeferredBlock<MandrakeCropBlock> MANDRAKE_CROP = Mistech.BLOCKS.registerBlock("mandrake", MandrakeCropBlock::new,
            p -> p.mapColor(MapColor.PLANT).noCollision().randomTicks().instabreak().sound(SoundType.CROP).pushReaction(PushReaction.DESTROY));
    public static final DeferredItem<BlockItem> MANDRAKE_SEEDS = Mistech.ITEMS.registerItem("mandrake_seeds",
            p -> new BlockItem(MANDRAKE_CROP.get(), p.useItemDescriptionPrefix()));
    /** A raiz-criatura arrancada. Colocada num Pote de Vidro, vira a Mandrágora Engarrafada. */
    public static final DeferredItem<Item> MANDRAKE = Mistech.ITEMS.registerItem("mandrake_root",
            com.mistech.mistechmod.herbal.MandrakeRootItem::new, p -> p.rarity(Rarity.UNCOMMON));
    /** Raiz de Mandrágora triturada: adubo forte em 3×3. */
    public static final DeferredItem<Item> MANDRAKE_POWDER = Mistech.ITEMS.registerItem("mandrake_powder",
            com.mistech.mistechmod.herbal.MandrakePowderItem::new, p -> p.rarity(Rarity.UNCOMMON));

    public static final DeferredBlock<BottledMandrakeBlock> BOTTLED_MANDRAKE = Mistech.BLOCKS.registerBlock("bottled_mandrake",
            BottledMandrakeBlock::new, p -> p.mapColor(MapColor.NONE).strength(0.4F).sound(SoundType.GLASS).noOcclusion());
    public static final DeferredItem<BlockItem> BOTTLED_MANDRAKE_ITEM = Mistech.ITEMS.registerSimpleBlockItem("bottled_mandrake", BOTTLED_MANDRAKE,
            p -> p.stacksTo(16).rarity(Rarity.UNCOMMON));
    public static final Supplier<BlockEntityType<BottledMandrakeBlockEntity>> BOTTLED_MANDRAKE_BE = MistechBlockEntities.BLOCK_ENTITY_TYPES.register(
            "bottled_mandrake", () -> new BlockEntityType<>(BottledMandrakeBlockEntity::new, BOTTLED_MANDRAKE.get()));

    private GardenContent() {}

    public static List<DeferredItem<? extends Item>> creativeItems() {
        return List.of(MANDRAKE_SEEDS, MANDRAKE, MANDRAKE_POWDER, BOTTLED_MANDRAKE_ITEM);
    }

    public static void register(IEventBus modEventBus) {
        // registros nos DeferredRegister do Mistech; eventos em GardenEvents
    }
}

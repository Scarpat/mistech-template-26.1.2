package com.mistech.mistechmod.archive.craft;

import java.util.List;
import java.util.function.Supplier;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.archive.ArchiveContent;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

/** Fase 8: autocrafting no Arquivo. Pergaminho de Padrão e Estante de Padrões (a fila fica no Núcleo). */
public final class CraftContent {
    private CraftContent() {}

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Pattern>> PATTERN =
            ArchiveContent.COMPONENTS.registerComponentType("pattern", b -> b.persistent(Pattern.CODEC).networkSynchronized(Pattern.STREAM_CODEC));

    public static final DeferredItem<PatternScrollItem> PATTERN_SCROLL = Mistech.ITEMS.registerItem("pattern_scroll", PatternScrollItem::new,
            p -> p.stacksTo(16).rarity(Rarity.UNCOMMON));

    public static final DeferredBlock<PatternShelfBlock> PATTERN_SHELF = Mistech.BLOCKS.registerBlock("pattern_shelf", PatternShelfBlock::new,
            p -> p.mapColor(MapColor.WOOD).strength(2.0F, 4.0F).sound(SoundType.WOOD).lightLevel(s -> 5));
    public static final DeferredItem<BlockItem> PATTERN_SHELF_ITEM = Mistech.ITEMS.registerSimpleBlockItem("pattern_shelf", PATTERN_SHELF,
            p -> p.rarity(Rarity.UNCOMMON));

    public static final Supplier<BlockEntityType<PatternShelfBlockEntity>> SHELF_BE = ArchiveContent.BLOCK_ENTITIES.register("pattern_shelf",
            () -> new BlockEntityType<>(PatternShelfBlockEntity::new, PATTERN_SHELF.get()));

    /** Força o carregamento da classe (os registros acontecem nos campos estáticos). */
    public static void init() {}

    public static List<DeferredItem<? extends Item>> creativeItems() {
        return List.of(PATTERN_SCROLL, PATTERN_SHELF_ITEM);
    }
}

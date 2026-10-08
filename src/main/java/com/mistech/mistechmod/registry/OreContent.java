package com.mistech.mistechmod.registry;

import java.util.List;

import com.mistech.mistechmod.Mistech;

import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Minérios raros das Profundezas dos Gnomos (material para craftings futuros).
 * Do menos para o mais raro:
 * - Mithril: pedra e ardósia, mais raro que diamante. Picareta de ferro.
 * - Adamantina: só na ardósia, tão raro quanto netherite. Picareta de diamante.
 * - Oricalco: só na ardósia funda, mais raro que netherite. Picareta de netherite.
 * Minério → bruto (Fortuna funciona) → lingote na fornalha (Adamantina e Oricalco só no alto-forno).
 * A geração fica nos placed_feature {@code realm_ore_mithril/adamantine/orichalcum} do bioma Profundezas dos Gnomos.
 */
public final class OreContent {
    // ----- Mithril -----
    public static final DeferredBlock<DropExperienceBlock> MITHRIL_ORE = Mistech.BLOCKS.registerBlock("mithril_ore",
            p -> new DropExperienceBlock(UniformInt.of(3, 7), p),
            p -> p.mapColor(MapColor.STONE).requiresCorrectToolForDrops().strength(3.0F, 3.0F).sound(SoundType.STONE));
    public static final DeferredBlock<DropExperienceBlock> DEEPSLATE_MITHRIL_ORE = Mistech.BLOCKS.registerBlock("deepslate_mithril_ore",
            p -> new DropExperienceBlock(UniformInt.of(3, 7), p),
            p -> p.mapColor(MapColor.DEEPSLATE).requiresCorrectToolForDrops().strength(4.5F, 3.0F).sound(SoundType.DEEPSLATE));

    // ----- Adamantina -----
    public static final DeferredBlock<DropExperienceBlock> DEEPSLATE_ADAMANTINE_ORE = Mistech.BLOCKS.registerBlock("deepslate_adamantine_ore",
            p -> new DropExperienceBlock(UniformInt.of(5, 10), p),
            p -> p.mapColor(MapColor.COLOR_RED).requiresCorrectToolForDrops().strength(30.0F, 1200.0F).sound(SoundType.ANCIENT_DEBRIS));

    // ----- Oricalco (brilha um pouquinho) -----
    public static final DeferredBlock<DropExperienceBlock> DEEPSLATE_ORICHALCUM_ORE = Mistech.BLOCKS.registerBlock("deepslate_orichalcum_ore",
            p -> new DropExperienceBlock(UniformInt.of(8, 14), p),
            p -> p.mapColor(MapColor.COLOR_ORANGE).requiresCorrectToolForDrops().strength(50.0F, 1200.0F).sound(SoundType.ANCIENT_DEBRIS)
                    .lightLevel(state -> 3));

    public static final DeferredItem<BlockItem> MITHRIL_ORE_ITEM = Mistech.ITEMS.registerSimpleBlockItem("mithril_ore", MITHRIL_ORE,
            p -> p.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<BlockItem> DEEPSLATE_MITHRIL_ORE_ITEM = Mistech.ITEMS.registerSimpleBlockItem("deepslate_mithril_ore",
            DEEPSLATE_MITHRIL_ORE, p -> p.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<BlockItem> DEEPSLATE_ADAMANTINE_ORE_ITEM = Mistech.ITEMS.registerSimpleBlockItem("deepslate_adamantine_ore",
            DEEPSLATE_ADAMANTINE_ORE, p -> p.rarity(Rarity.RARE).fireResistant());
    public static final DeferredItem<BlockItem> DEEPSLATE_ORICHALCUM_ORE_ITEM = Mistech.ITEMS.registerSimpleBlockItem("deepslate_orichalcum_ore",
            DEEPSLATE_ORICHALCUM_ORE, p -> p.rarity(Rarity.EPIC).fireResistant());

    public static final DeferredItem<Item> RAW_MITHRIL = Mistech.ITEMS.registerSimpleItem("raw_mithril", p -> p.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> MITHRIL_INGOT = Mistech.ITEMS.registerSimpleItem("mithril_ingot", p -> p.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> RAW_ADAMANTINE = Mistech.ITEMS.registerSimpleItem("raw_adamantine",
            p -> p.rarity(Rarity.RARE).fireResistant());
    public static final DeferredItem<Item> ADAMANTINE_INGOT = Mistech.ITEMS.registerSimpleItem("adamantine_ingot",
            p -> p.rarity(Rarity.RARE).fireResistant());
    public static final DeferredItem<Item> RAW_ORICHALCUM = Mistech.ITEMS.registerSimpleItem("raw_orichalcum",
            p -> p.rarity(Rarity.EPIC).fireResistant());
    public static final DeferredItem<Item> ORICHALCUM_INGOT = Mistech.ITEMS.registerSimpleItem("orichalcum_ingot",
            p -> p.rarity(Rarity.EPIC).fireResistant());

    // ----- Blocos de metal (9 lingotes; servem de base de sinalizador) -----
    public static final DeferredBlock<Block> MITHRIL_BLOCK = Mistech.BLOCKS.registerSimpleBlock("mithril_block",
            p -> p.mapColor(MapColor.COLOR_LIGHT_BLUE).instrument(NoteBlockInstrument.IRON_XYLOPHONE).requiresCorrectToolForDrops()
                    .strength(6.0F, 8.0F).sound(SoundType.METAL));
    public static final DeferredBlock<Block> ADAMANTINE_BLOCK = Mistech.BLOCKS.registerSimpleBlock("adamantine_block",
            p -> p.mapColor(MapColor.COLOR_RED).requiresCorrectToolForDrops().strength(50.0F, 1200.0F).sound(SoundType.NETHERITE_BLOCK));
    public static final DeferredBlock<Block> ORICHALCUM_BLOCK = Mistech.BLOCKS.registerSimpleBlock("orichalcum_block",
            p -> p.mapColor(MapColor.GOLD).requiresCorrectToolForDrops().strength(50.0F, 1200.0F).sound(SoundType.NETHERITE_BLOCK)
                    .lightLevel(state -> 5));
    public static final DeferredItem<BlockItem> MITHRIL_BLOCK_ITEM = Mistech.ITEMS.registerSimpleBlockItem("mithril_block", MITHRIL_BLOCK,
            p -> p.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<BlockItem> ADAMANTINE_BLOCK_ITEM = Mistech.ITEMS.registerSimpleBlockItem("adamantine_block",
            ADAMANTINE_BLOCK, p -> p.rarity(Rarity.RARE).fireResistant());
    public static final DeferredItem<BlockItem> ORICHALCUM_BLOCK_ITEM = Mistech.ITEMS.registerSimpleBlockItem("orichalcum_block",
            ORICHALCUM_BLOCK, p -> p.rarity(Rarity.EPIC).fireResistant());

    // ----- Isqueiros (sílex + minério): iguais ao vanilla (64 usos), só duram mais -----
    public static final DeferredItem<FlintAndSteelItem> MITHRIL_FLINT_AND_STEEL = Mistech.ITEMS.registerItem("mithril_flint_and_steel",
            FlintAndSteelItem::new, p -> p.durability(256).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<FlintAndSteelItem> ADAMANTINE_FLINT_AND_STEEL = Mistech.ITEMS.registerItem("adamantine_flint_and_steel",
            FlintAndSteelItem::new, p -> p.durability(640).rarity(Rarity.RARE).fireResistant());
    public static final DeferredItem<FlintAndSteelItem> ORICHALCUM_FLINT_AND_STEEL = Mistech.ITEMS.registerItem("orichalcum_flint_and_steel",
            FlintAndSteelItem::new, p -> p.durability(1280).rarity(Rarity.EPIC).fireResistant());

    private OreContent() {}

    /** Só para carregar a classe (os registros acontecem nos campos estáticos). */
    public static void init() {}

    public static List<DeferredItem<? extends Item>> creativeItems() {
        return List.of(MITHRIL_ORE_ITEM, DEEPSLATE_MITHRIL_ORE_ITEM, DEEPSLATE_ADAMANTINE_ORE_ITEM, DEEPSLATE_ORICHALCUM_ORE_ITEM,
                RAW_MITHRIL, MITHRIL_INGOT, RAW_ADAMANTINE, ADAMANTINE_INGOT, RAW_ORICHALCUM, ORICHALCUM_INGOT,
                MITHRIL_BLOCK_ITEM, ADAMANTINE_BLOCK_ITEM, ORICHALCUM_BLOCK_ITEM,
                MITHRIL_FLINT_AND_STEEL, ADAMANTINE_FLINT_AND_STEEL, ORICHALCUM_FLINT_AND_STEEL);
    }
}

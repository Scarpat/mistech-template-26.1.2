package com.mistech.mistechmod.machine.arcane;

import java.util.List;
import java.util.function.Supplier;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.machine.MachineBlock;
import com.mistech.mistechmod.machine.MachineLayout;
import com.mistech.mistechmod.machine.MachineMenu;
import com.mistech.mistechmod.registry.MistechBlockEntities;
import com.mistech.mistechmod.registry.MistechMenus;

import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper;

import static com.mistech.mistechmod.machine.MachineLayout.Kind.*;

/**
 * Máquinas Arcanas: versões avançadas das máquinas iniciais, no sistema das máquinas genéricas
 * (energia, Melhorias Rúnicas, redstone, lados e entrada/saída automática).
 * <ul>
 * <li>Fornalha Arcana ← Fornalha Rúnica: 3 linhas em paralelo, metade do tempo.</li>
 * <li>Extrator Arcano ← Extrator de Essência: 250 mB a cada 15 s num tanque de 8.000 mB; enche frascos/baldes.</li>
 * <li>Compressor Arcano ← Compressor de Magia: sem incenso, 1 fragmento a cada 6 s, aceita canos.</li>
 * </ul>
 */
public final class ArcaneMachines {
    private ArcaneMachines() {}

    private static BlockBehaviour.Properties arcane(BlockBehaviour.Properties p, int light) {
        return p.mapColor(MapColor.COLOR_PURPLE).requiresCorrectToolForDrops().strength(4.0F, 8.0F).sound(SoundType.DEEPSLATE_BRICKS)
                .noOcclusion().lightLevel(state -> state.getValue(MachineBlock.LIT) ? light : 2);
    }

    // ----- blocos -----
    public static final DeferredBlock<ArcaneMachineBlock.Furnace> ARCANE_FURNACE = Mistech.BLOCKS.registerBlock("arcane_furnace",
            ArcaneMachineBlock.Furnace::new, p -> arcane(p, 13));
    public static final DeferredItem<BlockItem> ARCANE_FURNACE_ITEM = Mistech.ITEMS.registerSimpleBlockItem("arcane_furnace", ARCANE_FURNACE,
            p -> p.rarity(Rarity.UNCOMMON));

    public static final DeferredBlock<ArcaneMachineBlock.Extractor> ARCANE_EXTRACTOR = Mistech.BLOCKS.registerBlock("arcane_extractor",
            ArcaneMachineBlock.Extractor::new, p -> arcane(p, 9));
    public static final DeferredItem<BlockItem> ARCANE_EXTRACTOR_ITEM = Mistech.ITEMS.registerSimpleBlockItem("arcane_extractor", ARCANE_EXTRACTOR,
            p -> p.rarity(Rarity.UNCOMMON));

    public static final DeferredBlock<ArcaneMachineBlock.Compressor> ARCANE_COMPRESSOR = Mistech.BLOCKS.registerBlock("arcane_compressor",
            ArcaneMachineBlock.Compressor::new, p -> arcane(p, 9));
    public static final DeferredItem<BlockItem> ARCANE_COMPRESSOR_ITEM = Mistech.ITEMS.registerSimpleBlockItem("arcane_compressor", ARCANE_COMPRESSOR,
            p -> p.rarity(Rarity.UNCOMMON));

    public static final DeferredBlock<ArcaneMachineBlock.Crusher> ARCANE_CRUSHER = Mistech.BLOCKS.registerBlock("arcane_crusher",
            ArcaneMachineBlock.Crusher::new, p -> arcane(p, 7));
    public static final DeferredItem<BlockItem> ARCANE_CRUSHER_ITEM = Mistech.ITEMS.registerSimpleBlockItem("arcane_crusher", ARCANE_CRUSHER,
            p -> p.rarity(Rarity.UNCOMMON));

    /** Liquefator Rúnico (máquina inicial): magia sólida → Essência Impura. */
    public static final DeferredBlock<ArcaneMachineBlock.Liquefier> LIQUEFIER = Mistech.BLOCKS.registerBlock("liquefier",
            ArcaneMachineBlock.Liquefier::new, p -> arcane(p, 8));
    public static final DeferredItem<BlockItem> LIQUEFIER_ITEM = Mistech.ITEMS.registerSimpleBlockItem("liquefier", LIQUEFIER);

    // ----- block entities -----
    public static final Supplier<BlockEntityType<ArcaneFurnaceBlockEntity>> ARCANE_FURNACE_BE = MistechBlockEntities.BLOCK_ENTITY_TYPES.register(
            "arcane_furnace", () -> new BlockEntityType<>(ArcaneFurnaceBlockEntity::new, ARCANE_FURNACE.get()));
    public static final Supplier<BlockEntityType<ArcaneExtractorBlockEntity>> ARCANE_EXTRACTOR_BE = MistechBlockEntities.BLOCK_ENTITY_TYPES.register(
            "arcane_extractor", () -> new BlockEntityType<>(ArcaneExtractorBlockEntity::new, ARCANE_EXTRACTOR.get()));
    public static final Supplier<BlockEntityType<ArcaneCompressorBlockEntity>> ARCANE_COMPRESSOR_BE = MistechBlockEntities.BLOCK_ENTITY_TYPES.register(
            "arcane_compressor", () -> new BlockEntityType<>(ArcaneCompressorBlockEntity::new, ARCANE_COMPRESSOR.get()));

    public static final Supplier<BlockEntityType<ArcaneCrusherBlockEntity>> ARCANE_CRUSHER_BE = MistechBlockEntities.BLOCK_ENTITY_TYPES.register(
            "arcane_crusher", () -> new BlockEntityType<>(ArcaneCrusherBlockEntity::new, ARCANE_CRUSHER.get()));

    public static final Supplier<BlockEntityType<LiquefierBlockEntity>> LIQUEFIER_BE = MistechBlockEntities.BLOCK_ENTITY_TYPES.register(
            "liquefier", () -> new BlockEntityType<>(LiquefierBlockEntity::new, LIQUEFIER.get()));

    // ----- telas -----
    private static Supplier<MenuType<MachineMenu>> menu(String name, Supplier<MachineLayout> layout) {
        java.util.concurrent.atomic.AtomicReference<MenuType<MachineMenu>> ref = new java.util.concurrent.atomic.AtomicReference<>();
        return MistechMenus.MENUS.register(name, () -> {
            MenuType<MachineMenu> type = new MenuType<>((id, inv) -> new MachineMenu(ref.get(), layout.get(), id, inv), FeatureFlags.VANILLA_SET);
            ref.set(type);
            return type;
        });
    }

    public static final Supplier<MenuType<MachineMenu>> FURNACE_MENU = menu("arcane_furnace", () -> ArcaneMachines.FURNACE_LAYOUT);
    public static final Supplier<MenuType<MachineMenu>> EXTRACTOR_MENU = menu("arcane_extractor", () -> ArcaneMachines.EXTRACTOR_LAYOUT);
    public static final Supplier<MenuType<MachineMenu>> COMPRESSOR_MENU = menu("arcane_compressor", () -> ArcaneMachines.COMPRESSOR_LAYOUT);
    public static final Supplier<MenuType<MachineMenu>> CRUSHER_MENU = menu("arcane_crusher", () -> ArcaneMachines.CRUSHER_LAYOUT);
    public static final Supplier<MenuType<MachineMenu>> LIQUEFIER_MENU = menu("liquefier", () -> ArcaneMachines.LIQUEFIER_LAYOUT);

    /** Magia sólida em cima, recipientes vazios embaixo, 2 saídas; tanque em x = 124. */
    public static final MachineLayout LIQUEFIER_LAYOUT = MachineLayout.builder("liquefier", LIQUEFIER_MENU::get)
            .slot(40, 25, INPUT)
            .slot(40, 53, FUEL)
            .grid(98, 26, 1, 2, OUTPUT)
            .capacity(20_000)
            .input(s -> LiquefierBlockEntity.essenceOf(s) > 0)
            .fuel(LiquefierBlockEntity::isContainer)
            .arrow(66, 34)
            .build();

    /** 2 entradas lado a lado, runa (Terra) embaixo delas, 6 saídas. */
    public static final MachineLayout CRUSHER_LAYOUT = MachineLayout.builder("arcane_crusher", CRUSHER_MENU::get)
            .slot(22, 25, INPUT).slot(40, 25, INPUT)
            .slot(31, 53, RUNE)
            .grid(98, 17, 2, 3, OUTPUT)
            .capacity(30_000)
            .rune(s -> s.is(Mistech.EARTH_RUNE.get()))
            .arrow(66, 34)
            .build();

    /** 3 entradas (coluna da esquerda) → 3 saídas (coluna da direita). */
    public static final MachineLayout FURNACE_LAYOUT = MachineLayout.builder("arcane_furnace", FURNACE_MENU::get)
            .slot(40, 17, INPUT).slot(40, 35, INPUT).slot(40, 53, INPUT)
            .slot(98, 17, OUTPUT).slot(98, 35, OUTPUT).slot(98, 53, OUTPUT)
            .capacity(30_000)
            .arrow(66, 34)
            .build();

    /** Recipientes vazios → 2 saídas; tanque desenhado em x = 124. */
    public static final MachineLayout EXTRACTOR_LAYOUT = MachineLayout.builder("arcane_extractor", EXTRACTOR_MENU::get)
            .slot(40, 35, INPUT)
            .grid(98, 26, 1, 2, OUTPUT)
            .capacity(30_000)
            .input(s -> s.is(Items.GLASS_BOTTLE) || s.is(Items.BUCKET))
            .arrow(66, 34)
            .build();

    /** Frascos/baldes de essência → 3 saídas; tanque desenhado em x = 124. */
    public static final MachineLayout COMPRESSOR_LAYOUT = MachineLayout.builder("arcane_compressor", COMPRESSOR_MENU::get)
            .slot(40, 35, INPUT)
            .grid(98, 17, 1, 3, OUTPUT)
            .capacity(30_000)
            .input(ArcaneCompressorBlockEntity::acceptsInput)
            .arrow(66, 34)
            .build();

    public static List<DeferredItem<? extends Item>> creativeItems() {
        return List.of(LIQUEFIER_ITEM, ARCANE_FURNACE_ITEM, ARCANE_CRUSHER_ITEM, ARCANE_EXTRACTOR_ITEM, ARCANE_COMPRESSOR_ITEM);
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ArcaneMachines::onCapabilities);
    }

    private static void onCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ARCANE_FURNACE_BE.get(), (be, side) -> be.getEnergyHandler());
        event.registerBlockEntity(Capabilities.Item.BLOCK, ARCANE_FURNACE_BE.get(), (be, side) -> new WorldlyContainerWrapper(be, side));
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ARCANE_CRUSHER_BE.get(), (be, side) -> be.getEnergyHandler());
        event.registerBlockEntity(Capabilities.Item.BLOCK, ARCANE_CRUSHER_BE.get(), (be, side) -> new WorldlyContainerWrapper(be, side));
        event.registerBlockEntity(Capabilities.Energy.BLOCK, LIQUEFIER_BE.get(), (be, side) -> be.getEnergyHandler());
        event.registerBlockEntity(Capabilities.Item.BLOCK, LIQUEFIER_BE.get(), (be, side) -> new WorldlyContainerWrapper(be, side));
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, LIQUEFIER_BE.get(), (be, side) -> be.getTank());
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ARCANE_EXTRACTOR_BE.get(), (be, side) -> be.getEnergyHandler());
        event.registerBlockEntity(Capabilities.Item.BLOCK, ARCANE_EXTRACTOR_BE.get(), (be, side) -> new WorldlyContainerWrapper(be, side));
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, ARCANE_EXTRACTOR_BE.get(), (be, side) -> be.getTank());
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ARCANE_COMPRESSOR_BE.get(), (be, side) -> be.getEnergyHandler());
        event.registerBlockEntity(Capabilities.Item.BLOCK, ARCANE_COMPRESSOR_BE.get(), (be, side) -> new WorldlyContainerWrapper(be, side));
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, ARCANE_COMPRESSOR_BE.get(), (be, side) -> be.getTank());
    }
}

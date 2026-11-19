package com.mistech.mistechmod.folklore.machine;

import static com.mistech.mistechmod.machine.MachineLayout.Kind.FUEL;
import static com.mistech.mistechmod.machine.MachineLayout.Kind.INPUT;
import static com.mistech.mistechmod.machine.MachineLayout.Kind.OUTPUT;
import static com.mistech.mistechmod.machine.MachineLayout.Kind.RUNE;

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

/**
 * Fase 7 do plano magia × tech: máquinas de folclore. Cada uma presa a uma criatura do mod, com uma "vontade" a atender.
 * Moinho do Saci, Bomba da Iara, Toco do Curupira, Alambique de Poções e Impressora de Pergaminhos.
 * A Brasa do Boitatá (catalisador da Caldeira Ígnea) fica em {@code FolkloreContent}.
 */
public final class FolkMachines {
    private FolkMachines() {}

    private static BlockBehaviour.Properties machine(BlockBehaviour.Properties p, MapColor color, SoundType sound, int light) {
        return p.mapColor(color).requiresCorrectToolForDrops().strength(3.0F, 6.0F).sound(sound).noOcclusion()
                .lightLevel(state -> state.getValue(MachineBlock.LIT) ? light : 0);
    }

    public static final DeferredBlock<FolkMachineBlocks.Windmill> SACI_WINDMILL = Mistech.BLOCKS.registerBlock("saci_windmill",
            FolkMachineBlocks.Windmill::new, p -> machine(p, MapColor.WOOD, SoundType.WOOD, 0));
    public static final DeferredItem<BlockItem> SACI_WINDMILL_ITEM = Mistech.ITEMS.registerSimpleBlockItem("saci_windmill", SACI_WINDMILL,
            p -> p.rarity(Rarity.UNCOMMON));
    public static final DeferredBlock<FolkMachineBlocks.Pump> IARA_PUMP = Mistech.BLOCKS.registerBlock("iara_pump",
            FolkMachineBlocks.Pump::new, p -> machine(p, MapColor.COLOR_CYAN, SoundType.COPPER, 6));
    public static final DeferredItem<BlockItem> IARA_PUMP_ITEM = Mistech.ITEMS.registerSimpleBlockItem("iara_pump", IARA_PUMP,
            p -> p.rarity(Rarity.UNCOMMON));
    public static final DeferredBlock<FolkMachineBlocks.LavaPump> BOITATA_PUMP = Mistech.BLOCKS.registerBlock("boitata_pump",
            FolkMachineBlocks.LavaPump::new, p -> machine(p, MapColor.NETHER, SoundType.NETHER_BRICKS, 10));
    public static final DeferredItem<BlockItem> BOITATA_PUMP_ITEM = Mistech.ITEMS.registerSimpleBlockItem("boitata_pump", BOITATA_PUMP,
            p -> p.rarity(Rarity.RARE).fireResistant());
    public static final DeferredBlock<FolkMachineBlocks.Stump> CURUPIRA_STUMP = Mistech.BLOCKS.registerBlock("curupira_stump",
            FolkMachineBlocks.Stump::new, p -> machine(p, MapColor.WOOD, SoundType.WOOD, 7));
    public static final DeferredItem<BlockItem> CURUPIRA_STUMP_ITEM = Mistech.ITEMS.registerSimpleBlockItem("curupira_stump", CURUPIRA_STUMP,
            p -> p.rarity(Rarity.UNCOMMON));
    public static final DeferredBlock<FolkMachineBlocks.Still> POTION_STILL = Mistech.BLOCKS.registerBlock("potion_still",
            FolkMachineBlocks.Still::new, p -> machine(p, MapColor.COLOR_ORANGE, SoundType.COPPER, 8));
    public static final DeferredItem<BlockItem> POTION_STILL_ITEM = Mistech.ITEMS.registerSimpleBlockItem("potion_still", POTION_STILL,
            p -> p.rarity(Rarity.UNCOMMON));
    public static final DeferredBlock<FolkMachineBlocks.Printer> SCROLL_PRINTER = Mistech.BLOCKS.registerBlock("scroll_printer",
            FolkMachineBlocks.Printer::new, p -> machine(p, MapColor.WOOD, SoundType.WOOD, 6));
    public static final DeferredItem<BlockItem> SCROLL_PRINTER_ITEM = Mistech.ITEMS.registerSimpleBlockItem("scroll_printer", SCROLL_PRINTER,
            p -> p.rarity(Rarity.RARE));

    public static final Supplier<BlockEntityType<SaciWindmillBlockEntity>> SACI_WINDMILL_BE = MistechBlockEntities.BLOCK_ENTITY_TYPES.register(
            "saci_windmill", () -> new BlockEntityType<>(SaciWindmillBlockEntity::new, SACI_WINDMILL.get()));
    public static final Supplier<BlockEntityType<IaraPumpBlockEntity>> IARA_PUMP_BE = MistechBlockEntities.BLOCK_ENTITY_TYPES.register(
            "iara_pump", () -> new BlockEntityType<>(IaraPumpBlockEntity::new, IARA_PUMP.get()));
    public static final Supplier<BlockEntityType<BoitataPumpBlockEntity>> BOITATA_PUMP_BE = MistechBlockEntities.BLOCK_ENTITY_TYPES.register(
            "boitata_pump", () -> new BlockEntityType<>(BoitataPumpBlockEntity::new, BOITATA_PUMP.get()));
    public static final Supplier<BlockEntityType<CurupiraStumpBlockEntity>> CURUPIRA_STUMP_BE = MistechBlockEntities.BLOCK_ENTITY_TYPES.register(
            "curupira_stump", () -> new BlockEntityType<>(CurupiraStumpBlockEntity::new, CURUPIRA_STUMP.get()));
    public static final Supplier<BlockEntityType<PotionStillBlockEntity>> POTION_STILL_BE = MistechBlockEntities.BLOCK_ENTITY_TYPES.register(
            "potion_still", () -> new BlockEntityType<>(PotionStillBlockEntity::new, POTION_STILL.get()));
    public static final Supplier<BlockEntityType<ScrollPrinterBlockEntity>> SCROLL_PRINTER_BE = MistechBlockEntities.BLOCK_ENTITY_TYPES.register(
            "scroll_printer", () -> new BlockEntityType<>(ScrollPrinterBlockEntity::new, SCROLL_PRINTER.get()));

    private static Supplier<MenuType<MachineMenu>> menu(String name, Supplier<MachineLayout> layout) {
        java.util.concurrent.atomic.AtomicReference<MenuType<MachineMenu>> ref = new java.util.concurrent.atomic.AtomicReference<>();
        return MistechMenus.MENUS.register(name, () -> {
            MenuType<MachineMenu> type = new MenuType<>((id, inv) -> new MachineMenu(ref.get(), layout.get(), id, inv), FeatureFlags.VANILLA_SET);
            ref.set(type);
            return type;
        });
    }

    public static final Supplier<MenuType<MachineMenu>> WINDMILL_MENU = menu("saci_windmill", () -> FolkMachines.WINDMILL_LAYOUT);
    public static final Supplier<MenuType<MachineMenu>> PUMP_MENU = menu("iara_pump", () -> FolkMachines.PUMP_LAYOUT);
    public static final Supplier<MenuType<MachineMenu>> LAVA_PUMP_MENU = menu("boitata_pump", () -> FolkMachines.LAVA_PUMP_LAYOUT);
    public static final Supplier<MenuType<MachineMenu>> STUMP_MENU = menu("curupira_stump", () -> FolkMachines.STUMP_LAYOUT);
    public static final Supplier<MenuType<MachineMenu>> STILL_MENU = menu("potion_still", () -> FolkMachines.STILL_LAYOUT);
    public static final Supplier<MenuType<MachineMenu>> PRINTER_MENU = menu("scroll_printer", () -> FolkMachines.PRINTER_LAYOUT);

    /** Tabaco seco embaixo (a chama mostra quanto falta), Saci Engarrafado à direita. */
    public static final MachineLayout WINDMILL_LAYOUT = MachineLayout.builder("saci_windmill", WINDMILL_MENU::get)
            .slot(30, 50, FUEL).slot(118, 35, INPUT)
            .capacity(30_000)
            .fuel(SaciWindmillBlockEntity::isTobacco).input(SaciWindmillBlockEntity::isSaci)
            .hint(0, Mistech.DRIED_TOBACCO_LEAF, "hint.mistech.saci_windmill.tobacco")
            .hint(1, com.mistech.mistechmod.folklore.FolkloreContent.SACI_JAR, "hint.mistech.saci_windmill.saci")
            .flame(31, 32)
            .info(50, 18, 64, 42, menu -> SaciWindmillBlockEntity.infoLines(menu.info(0), menu.info(1)))
            .build();
    /** Balde vazio, balde cheio e a Escama da Iara; tanque à direita (x = 124). */
    public static final MachineLayout PUMP_LAYOUT = MachineLayout.builder("iara_pump", PUMP_MENU::get)
            .slot(30, 50, FUEL).slot(58, 50, OUTPUT).slot(76, 50, INPUT)
            .capacity(20_000)
            .fuel(IaraPumpBlockEntity::isBucket).input(IaraPumpBlockEntity::isScale)
            .hint(0, () -> net.minecraft.world.item.Items.BUCKET, "hint.mistech.pump.bucket")
            .hint(2, com.mistech.mistechmod.lore.LoreContent.IARA_SCALE, "hint.mistech.iara_pump.scale")
            .info(56, 18, 62, 30, menu -> IaraPumpBlockEntity.infoLines(menu.info(0)))
            .build();
    /** Igual à Bomba da Iara, com a Escama do Boitatá. */
    public static final MachineLayout LAVA_PUMP_LAYOUT = MachineLayout.builder("boitata_pump", LAVA_PUMP_MENU::get)
            .slot(30, 50, FUEL).slot(58, 50, OUTPUT).slot(76, 50, INPUT)
            .capacity(20_000)
            .fuel(IaraPumpBlockEntity::isBucket).input(BoitataPumpBlockEntity::isBoitataScale)
            .hint(0, () -> net.minecraft.world.item.Items.BUCKET, "hint.mistech.pump.bucket")
            .hint(2, com.mistech.mistechmod.folklore.FolkloreContent.BOITATA_SCALE, "hint.mistech.boitata_pump.scale")
            .info(56, 18, 62, 30, menu -> BoitataPumpBlockEntity.boitataLines(menu.info(0)))
            .build();
    /** Cachaça (oferenda) e mudas à esquerda, 3×3 de saída. */
    public static final MachineLayout STUMP_LAYOUT = MachineLayout.builder("curupira_stump", STUMP_MENU::get)
            .slot(26, 17, FUEL).slot(26, 39, INPUT).grid(62, 17, 3, 3, OUTPUT)
            .capacity(20_000)
            .fuel(CurupiraStumpBlockEntity::isOffering).input(CurupiraStumpBlockEntity::isSapling)
            .hint(0, com.mistech.mistechmod.drink.DrinkContent.CACHACA, "hint.mistech.curupira_stump.offering")
            .hint(1, () -> net.minecraft.world.item.Items.OAK_SAPLING, "hint.mistech.curupira_stump.sapling")
            .info(8, 58, 52, 12, menu -> CurupiraStumpBlockEntity.infoLines(menu.info(0), menu.info(1), menu.info(2)))
            .build();
    /** Frascos vazios, 3 ingredientes (em ordem), 4×2 de saída; tanque de água à direita. */
    public static final MachineLayout STILL_LAYOUT = MachineLayout.builder("potion_still", STILL_MENU::get)
            .slot(20, 17, FUEL).grid(40, 17, 3, 1, INPUT).grid(40, 36, 4, 2, OUTPUT)
            .capacity(20_000)
            .fuel(PotionStillBlockEntity::isBottle).input(PotionStillBlockEntity::isIngredient)
            .hint(0, () -> net.minecraft.world.item.Items.GLASS_BOTTLE, "hint.mistech.potion_still.bottles")
            .hint(1, () -> net.minecraft.world.item.Items.NETHER_WART, "hint.mistech.potion_still.first")
            .hint(2, () -> net.minecraft.world.item.Items.SUGAR, "hint.mistech.potion_still.second")
            .hint(3, () -> net.minecraft.world.item.Items.REDSTONE, "hint.mistech.potion_still.third")
            .build();
    /** Matriz em cima, papel embaixo, lápis-lazúli à esquerda, 2 saídas; tanque de Essência Purificada à direita. */
    public static final MachineLayout PRINTER_LAYOUT = MachineLayout.builder("scroll_printer", PRINTER_MENU::get)
            .slot(40, 25, RUNE).slot(40, 53, FUEL).slot(18, 53, INPUT).grid(98, 26, 1, 2, OUTPUT)
            .capacity(20_000)
            .rune(ScrollPrinterBlockEntity::isScroll).fuel(ScrollPrinterBlockEntity::isPaper).input(ScrollPrinterBlockEntity::isLapis)
            .hint(0, com.mistech.mistechmod.spellcraft.SpellcraftContent.SPELL_SCROLL, "hint.mistech.scroll_printer.template")
            .hint(1, () -> net.minecraft.world.item.Items.PAPER, "hint.mistech.scroll_printer.paper")
            .hint(2, () -> net.minecraft.world.item.Items.LAPIS_LAZULI, "hint.mistech.scroll_printer.lapis")
            .arrow(66, 34)
            .build();

    public static List<DeferredItem<? extends Item>> creativeItems() {
        return List.of(SACI_WINDMILL_ITEM, IARA_PUMP_ITEM, BOITATA_PUMP_ITEM, CURUPIRA_STUMP_ITEM, POTION_STILL_ITEM, SCROLL_PRINTER_ITEM);
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(FolkMachines::onCapabilities);
    }

    private static void onCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Energy.BLOCK, SACI_WINDMILL_BE.get(), (be, side) -> be.getEnergyHandler());
        event.registerBlockEntity(Capabilities.Item.BLOCK, SACI_WINDMILL_BE.get(), (be, side) -> new WorldlyContainerWrapper(be, side));
        event.registerBlockEntity(Capabilities.Energy.BLOCK, IARA_PUMP_BE.get(), (be, side) -> be.getEnergyHandler());
        event.registerBlockEntity(Capabilities.Item.BLOCK, IARA_PUMP_BE.get(), (be, side) -> new WorldlyContainerWrapper(be, side));
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, IARA_PUMP_BE.get(), (be, side) -> be.getTank());
        event.registerBlockEntity(Capabilities.Energy.BLOCK, BOITATA_PUMP_BE.get(), (be, side) -> be.getEnergyHandler());
        event.registerBlockEntity(Capabilities.Item.BLOCK, BOITATA_PUMP_BE.get(), (be, side) -> new WorldlyContainerWrapper(be, side));
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, BOITATA_PUMP_BE.get(), (be, side) -> be.getTank());
        event.registerBlockEntity(Capabilities.Energy.BLOCK, CURUPIRA_STUMP_BE.get(), (be, side) -> be.getEnergyHandler());
        event.registerBlockEntity(Capabilities.Item.BLOCK, CURUPIRA_STUMP_BE.get(), (be, side) -> new WorldlyContainerWrapper(be, side));
        event.registerBlockEntity(Capabilities.Energy.BLOCK, POTION_STILL_BE.get(), (be, side) -> be.getEnergyHandler());
        event.registerBlockEntity(Capabilities.Item.BLOCK, POTION_STILL_BE.get(), (be, side) -> new WorldlyContainerWrapper(be, side));
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, POTION_STILL_BE.get(), (be, side) -> be.getTank());
        event.registerBlockEntity(Capabilities.Energy.BLOCK, SCROLL_PRINTER_BE.get(), (be, side) -> be.getEnergyHandler());
        event.registerBlockEntity(Capabilities.Item.BLOCK, SCROLL_PRINTER_BE.get(), (be, side) -> new WorldlyContainerWrapper(be, side));
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, SCROLL_PRINTER_BE.get(), (be, side) -> be.getTank());
    }
}

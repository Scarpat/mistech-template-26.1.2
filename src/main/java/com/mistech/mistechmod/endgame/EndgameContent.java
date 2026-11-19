package com.mistech.mistechmod.endgame;

import static com.mistech.mistechmod.machine.MachineLayout.Kind.FUEL;
import static com.mistech.mistechmod.machine.MachineLayout.Kind.INPUT;
import static com.mistech.mistechmod.machine.MachineLayout.Kind.OUTPUT;
import static com.mistech.mistechmod.machine.MachineLayout.Kind.RUNE;

import java.util.List;
import java.util.function.Supplier;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.machine.GnomeBicycleBlockEntity;
import com.mistech.mistechmod.machine.MachineBlock;
import com.mistech.mistechmod.machine.MachineLayout;
import com.mistech.mistechmod.machine.MachineMenu;
import com.mistech.mistechmod.registry.MistechBlockEntities;
import com.mistech.mistechmod.registry.MistechMenus;
import com.mistech.mistechmod.spellcraft.conjurer.RunicConjurerBlockEntity;

import net.minecraft.network.chat.Component;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper;

/**
 * Fase 9 do plano magia × tech: o endgame que junta as linhas.
 * <ul>
 * <li>Lingote e Bloco Celeste (só a Forja Estelar faz o lingote).</li>
 * <li>Núcleo Celeste: instalado numa máquina, os engastes param de se gastar (o Prisma Astral faz 6 pós).</li>
 * <li>Forja Estelar (multibloco 5×5), Reator Onírico (multibloco 3×3×3), Bateria Celeste (10M FE).</li>
 * <li>Conjurador Celeste (2 cristais que se alternam). O equipamento Celeste fica em {@code MithrilContent.CELESTIAL}.</li>
 * </ul>
 */
public final class EndgameContent {
    private EndgameContent() {}

    // ----- materiais -----
    public static final DeferredItem<Item> CELESTIAL_INGOT = Mistech.ITEMS.registerSimpleItem("celestial_ingot",
            p -> p.rarity(Rarity.EPIC).fireResistant());
    public static final DeferredBlock<Block> CELESTIAL_BLOCK = Mistech.BLOCKS.registerSimpleBlock("celestial_block",
            p -> p.mapColor(MapColor.COLOR_LIGHT_BLUE).requiresCorrectToolForDrops().strength(6.0F, 1200.0F).sound(SoundType.NETHERITE_BLOCK)
                    .lightLevel(state -> 6));
    public static final DeferredItem<BlockItem> CELESTIAL_BLOCK_ITEM = Mistech.ITEMS.registerSimpleBlockItem("celestial_block", CELESTIAL_BLOCK,
            p -> p.rarity(Rarity.EPIC).fireResistant());
    public static final DeferredItem<Item> CELESTIAL_CORE = Mistech.ITEMS.registerItem("celestial_core", CoreItem::new,
            p -> p.rarity(Rarity.EPIC).stacksTo(16).fireResistant());

    /** Núcleo Celeste: só a dica (a instalação é o {@link CelestialCoreEvents}). */
    public static final class CoreItem extends Item {
        public CoreItem(Item.Properties properties) {
            super(properties);
        }

        @Override
        public void appendHoverText(net.minecraft.world.item.ItemStack stack, Item.TooltipContext context,
                net.minecraft.world.item.component.TooltipDisplay display, java.util.function.Consumer<Component> tooltip,
                net.minecraft.world.item.TooltipFlag flag) {
            tooltip.accept(Component.translatable("item.mistech.celestial_core.tooltip").withStyle(net.minecraft.ChatFormatting.GRAY));
        }
    }

    // ----- blocos -----
    public static final DeferredBlock<EndgameBlocks.StarForge> STAR_FORGE = Mistech.BLOCKS.registerBlock("star_forge", EndgameBlocks.StarForge::new,
            p -> p.mapColor(MapColor.COLOR_LIGHT_BLUE).requiresCorrectToolForDrops().strength(6.0F, 1200.0F).sound(SoundType.NETHERITE_BLOCK)
                    .noOcclusion().lightLevel(state -> state.getValue(MachineBlock.LIT) ? 15 : 6));
    public static final DeferredItem<BlockItem> STAR_FORGE_ITEM = Mistech.ITEMS.registerSimpleBlockItem("star_forge", STAR_FORGE,
            p -> p.rarity(Rarity.EPIC));
    public static final DeferredBlock<EndgameBlocks.DreamReactor> DREAM_REACTOR = Mistech.BLOCKS.registerBlock("dream_reactor",
            EndgameBlocks.DreamReactor::new,
            p -> p.mapColor(MapColor.COLOR_PINK).requiresCorrectToolForDrops().strength(5.0F, 1200.0F).sound(SoundType.AMETHYST)
                    .noOcclusion().lightLevel(state -> state.getValue(MachineBlock.LIT) ? 13 : 4));
    public static final DeferredItem<BlockItem> DREAM_REACTOR_ITEM = Mistech.ITEMS.registerSimpleBlockItem("dream_reactor", DREAM_REACTOR,
            p -> p.rarity(Rarity.EPIC));
    public static final DeferredBlock<EndgameBlocks.CelestialBattery> CELESTIAL_BATTERY = Mistech.BLOCKS.registerBlock("celestial_battery",
            EndgameBlocks.CelestialBattery::new,
            p -> p.mapColor(MapColor.COLOR_LIGHT_BLUE).requiresCorrectToolForDrops().strength(5.0F, 1200.0F).sound(SoundType.AMETHYST)
                    .noOcclusion().lightLevel(state -> state.getValue(MachineBlock.LIT) ? 12 : 4));
    public static final DeferredItem<com.mistech.mistechmod.item.EnergyBlockItem> CELESTIAL_BATTERY_ITEM = Mistech.ITEMS.registerItem("celestial_battery",
            p -> new com.mistech.mistechmod.item.EnergyBlockItem(CELESTIAL_BATTERY.get(), p, CelestialBatteryBlockEntity.CAPACITY),
            p -> p.useBlockDescriptionPrefix().rarity(Rarity.EPIC));
    public static final DeferredBlock<EndgameBlocks.CelestialConjurer> CELESTIAL_CONJURER = Mistech.BLOCKS.registerBlock("celestial_conjurer",
            EndgameBlocks.CelestialConjurer::new,
            p -> p.mapColor(MapColor.COLOR_LIGHT_BLUE).requiresCorrectToolForDrops().strength(5.0F, 1200.0F).sound(SoundType.AMETHYST)
                    .noOcclusion().lightLevel(state -> state.getValue(MachineBlock.LIT) ? 13 : 5));
    public static final DeferredItem<BlockItem> CELESTIAL_CONJURER_ITEM = Mistech.ITEMS.registerSimpleBlockItem("celestial_conjurer",
            CELESTIAL_CONJURER, p -> p.rarity(Rarity.EPIC));

    public static final Supplier<BlockEntityType<StarForgeBlockEntity>> STAR_FORGE_BE = MistechBlockEntities.BLOCK_ENTITY_TYPES.register(
            "star_forge", () -> new BlockEntityType<>(StarForgeBlockEntity::new, STAR_FORGE.get()));
    public static final Supplier<BlockEntityType<DreamReactorBlockEntity>> DREAM_REACTOR_BE = MistechBlockEntities.BLOCK_ENTITY_TYPES.register(
            "dream_reactor", () -> new BlockEntityType<>(DreamReactorBlockEntity::new, DREAM_REACTOR.get()));
    public static final Supplier<BlockEntityType<CelestialBatteryBlockEntity>> CELESTIAL_BATTERY_BE = MistechBlockEntities.BLOCK_ENTITY_TYPES.register(
            "celestial_battery", () -> new BlockEntityType<>(CelestialBatteryBlockEntity::new, CELESTIAL_BATTERY.get()));
    public static final Supplier<BlockEntityType<CelestialConjurerBlockEntity>> CELESTIAL_CONJURER_BE = MistechBlockEntities.BLOCK_ENTITY_TYPES.register(
            "celestial_conjurer", () -> new BlockEntityType<>(CelestialConjurerBlockEntity::new, CELESTIAL_CONJURER.get()));

    // ----- telas -----
    private static Supplier<MenuType<MachineMenu>> menu(String name, Supplier<MachineLayout> layout) {
        java.util.concurrent.atomic.AtomicReference<MenuType<MachineMenu>> ref = new java.util.concurrent.atomic.AtomicReference<>();
        return MistechMenus.MENUS.register(name, () -> {
            MenuType<MachineMenu> type = new MenuType<>((id, inv) -> new MachineMenu(ref.get(), layout.get(), id, inv), FeatureFlags.VANILLA_SET);
            ref.set(type);
            return type;
        });
    }

    public static final Supplier<MenuType<MachineMenu>> STAR_FORGE_MENU = menu("star_forge", () -> EndgameContent.STAR_FORGE_LAYOUT);
    public static final Supplier<MenuType<MachineMenu>> DREAM_REACTOR_MENU = menu("dream_reactor", () -> EndgameContent.DREAM_REACTOR_LAYOUT);
    public static final Supplier<MenuType<MachineMenu>> CELESTIAL_BATTERY_MENU = menu("celestial_battery", () -> EndgameContent.CELESTIAL_BATTERY_LAYOUT);
    public static final Supplier<MenuType<MachineMenu>> CELESTIAL_CONJURER_MENU = menu("celestial_conjurer", () -> EndgameContent.CELESTIAL_CONJURER_LAYOUT);

    /** Medidores da Forja (x): mitril, adamantina, oricalco e Essência Purificada. */
    public static final int FORGE_GAUGE_MITHRIL = 8, FORGE_GAUGE_ADAMANTINE = 26, FORGE_GAUGE_ORICHALCUM = 44, FORGE_GAUGE_ESSENCE = 134;

    /** Metal (lingote, bloco ou balde) em cima, Fragmento de Estrela embaixo, seta, Lingote Celeste. info0..3 = tanques. */
    public static final MachineLayout STAR_FORGE_LAYOUT = MachineLayout.builder("star_forge", STAR_FORGE_MENU::get)
            .slot(64, 17, INPUT).slot(64, 51, RUNE).slot(112, 34, OUTPUT)
            .capacity(StarForgeBlockEntity.CAPACITY)
            .input(StarForgeBlockEntity::isMetal).rune(StarForgeBlockEntity::isFragment)
            .hint(0, com.mistech.mistechmod.registry.OreContent.MITHRIL_INGOT, "hint.mistech.star_forge.metal")
            .hint(1, com.mistech.mistechmod.celestial.CelestialContent.STAR_FRAGMENT, "hint.mistech.star_forge.fragment")
            .arrow(84, 34)
            .build();
    /** Fragmentos/Pó de Sonho/Estilhaço de Memória embaixo (a chama mostra quanto falta). */
    public static final MachineLayout DREAM_REACTOR_LAYOUT = MachineLayout.builder("dream_reactor", DREAM_REACTOR_MENU::get)
            .slot(30, 50, FUEL)
            .capacity(DreamReactorBlockEntity.CAPACITY)
            .fuel(DreamReactorBlockEntity::isFuel)
            .hint(0, com.mistech.mistechmod.registry.DreamDeepContent.MEMORY_SHARD, "hint.mistech.dream_reactor.fuel")
            .flame(31, 32)
            .info(56, 20, menu -> DreamReactorBlockEntity.infoLines(menu.info(0), menu.info(1), menu.info(2)))
            .build();
    /** Igual à Bateria Aprimorada: espaço de carga. info0/1 = FE/t entrando/saindo, info2 = geradores, info3 = máquinas. */
    public static final MachineLayout CELESTIAL_BATTERY_LAYOUT = MachineLayout.builder("celestial_battery", CELESTIAL_BATTERY_MENU::get)
            .slot(126, 53, RUNE)
            .capacity(CelestialBatteryBlockEntity.CAPACITY)
            .rune(s -> GnomeBicycleBlockEntity.batteryHandler(s) != null)
            .info(10, 22, menu -> List.of(
                    Component.translatable("gui.mistech.battery.in", menu.info(0)),
                    Component.translatable("gui.mistech.battery.out", menu.info(1)),
                    Component.translatable("gui.mistech.battery.generators", menu.info(2)),
                    Component.translatable("gui.mistech.battery.machines", menu.info(3))))
            .build();
    /** Como o Conjurador Rúnico, com um segundo cristal (espaço 4): cristal A em cima, B no meio, essência embaixo. */
    public static final MachineLayout CELESTIAL_CONJURER_LAYOUT = MachineLayout.builder("celestial_conjurer", CELESTIAL_CONJURER_MENU::get)
            .slot(40, 17, INPUT)
            .slot(40, 53, FUEL)
            .grid(98, 26, 1, 2, OUTPUT)
            .slot(40, 35, INPUT)
            .capacity(RunicConjurerBlockEntity.CAPACITY)
            .input(RunicConjurerBlockEntity::isCrystal)
            .fuel(RunicConjurerBlockEntity::isEssenceContainer)
            .hint(0, com.mistech.mistechmod.spellcraft.conjurer.ConjurerContent.SPELL_CRYSTAL, "hint.mistech.conjurer.crystal_a")
            .hint(4, com.mistech.mistechmod.spellcraft.conjurer.ConjurerContent.SPELL_CRYSTAL, "hint.mistech.conjurer.crystal_b")
            .hint(1, com.mistech.mistechmod.purity.PurityContent.PURIFIED_ESSENCE_BOTTLE, "hint.mistech.conjurer.essence")
            .arrow(66, 34)
            .build();

    public static List<DeferredItem<? extends Item>> creativeItems() {
        return List.of(CELESTIAL_INGOT, CELESTIAL_BLOCK_ITEM, CELESTIAL_CORE, STAR_FORGE_ITEM, DREAM_REACTOR_ITEM, CELESTIAL_BATTERY_ITEM,
                CELESTIAL_CONJURER_ITEM);
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(EndgameContent::onCapabilities);
    }

    private static void onCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Energy.BLOCK, STAR_FORGE_BE.get(), (be, side) -> be.getEnergyHandler());
        event.registerBlockEntity(Capabilities.Item.BLOCK, STAR_FORGE_BE.get(), (be, side) -> new WorldlyContainerWrapper(be, side));
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, STAR_FORGE_BE.get(), (be, side) -> be.getTanks());
        event.registerBlockEntity(Capabilities.Energy.BLOCK, DREAM_REACTOR_BE.get(), (be, side) -> be.getEnergyHandler());
        event.registerBlockEntity(Capabilities.Item.BLOCK, DREAM_REACTOR_BE.get(), (be, side) -> new WorldlyContainerWrapper(be, side));
        event.registerBlockEntity(Capabilities.Energy.BLOCK, CELESTIAL_BATTERY_BE.get(), (be, side) -> be.handlerFor(side));
        event.registerBlockEntity(Capabilities.Energy.BLOCK, CELESTIAL_CONJURER_BE.get(), (be, side) -> be.getEnergyHandler());
        event.registerBlockEntity(Capabilities.Item.BLOCK, CELESTIAL_CONJURER_BE.get(), (be, side) -> new WorldlyContainerWrapper(be, side));
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, CELESTIAL_CONJURER_BE.get(), (be, side) -> be.getTanks());
    }
}

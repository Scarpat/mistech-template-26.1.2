package com.mistech.mistechmod.refining;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.machine.MachineBlock;
import com.mistech.mistechmod.machine.MachineLayout;
import com.mistech.mistechmod.machine.MachineMenu;
import com.mistech.mistechmod.registry.MistechBlockEntities;
import com.mistech.mistechmod.registry.MistechMenus;
import com.mistech.mistechmod.registry.MistechRecipes;

import net.minecraft.resources.Identifier;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
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
 * Fase 3 do plano magia × tech: cadeia de minério em 4 tiers.
 * 2× Triturador → 3× Lavadora de Essência (Torrões) → 4× Cristalizador Rúnico (Cristais) → 5× Prisma Astral (pó direto).
 * Receitas em {@code data/mistech/recipe/refining/} (tipo {@code mistech:ore_refining}); torrões e cristais viram pó no triturador.
 */
public final class RefiningContent {
    private RefiningContent() {}

    /** Metais da cadeia (cada um com pó no mod). */
    public static final String[] METALS = {"iron", "gold", "copper", "mithril", "adamantine", "orichalcum"};

    // ----- receitas -----
    public static final Supplier<RecipeType<OreRefiningRecipe>> TYPE = MistechRecipes.RECIPE_TYPES.register("ore_refining",
            () -> RecipeType.simple(Identifier.fromNamespaceAndPath(Mistech.MODID, "ore_refining")));
    public static final Supplier<RecipeSerializer<OreRefiningRecipe>> SERIALIZER = MistechRecipes.RECIPE_SERIALIZERS.register("ore_refining",
            () -> new RecipeSerializer<>(OreRefiningRecipe.CODEC, OreRefiningRecipe.STREAM_CODEC));

    // ----- itens intermediários (torrão e cristal de cada metal) -----
    public static final List<DeferredItem<Item>> CLUMPS = new ArrayList<>();
    public static final List<DeferredItem<Item>> CRYSTALS = new ArrayList<>();

    static {
        for (String metal : METALS) {
            CLUMPS.add(Mistech.ITEMS.registerSimpleItem(metal + "_clump"));
            CRYSTALS.add(Mistech.ITEMS.registerSimpleItem(metal + "_ore_crystal", p -> p.rarity(Rarity.UNCOMMON)));
        }
    }

    private static BlockBehaviour.Properties machine(BlockBehaviour.Properties p, MapColor color, SoundType sound, int light) {
        return p.mapColor(color).requiresCorrectToolForDrops().strength(4.0F, 8.0F).sound(sound).noOcclusion()
                .lightLevel(state -> state.getValue(MachineBlock.LIT) ? light : 2);
    }

    // ----- blocos -----
    public static final DeferredBlock<RefiningBlocks.Washer> ESSENCE_WASHER = Mistech.BLOCKS.registerBlock("essence_washer",
            RefiningBlocks.Washer::new, p -> machine(p, MapColor.COLOR_PURPLE, SoundType.COPPER, 7));
    public static final DeferredItem<BlockItem> ESSENCE_WASHER_ITEM = Mistech.ITEMS.registerSimpleBlockItem("essence_washer", ESSENCE_WASHER,
            p -> p.rarity(Rarity.UNCOMMON));
    public static final DeferredBlock<RefiningBlocks.Crystallizer> RUNIC_CRYSTALLIZER = Mistech.BLOCKS.registerBlock("runic_crystallizer",
            RefiningBlocks.Crystallizer::new, p -> machine(p, MapColor.QUARTZ, SoundType.AMETHYST, 10));
    public static final DeferredItem<BlockItem> RUNIC_CRYSTALLIZER_ITEM = Mistech.ITEMS.registerSimpleBlockItem("runic_crystallizer", RUNIC_CRYSTALLIZER,
            p -> p.rarity(Rarity.RARE));
    public static final DeferredBlock<RefiningBlocks.Prism> ASTRAL_PRISM = Mistech.BLOCKS.registerBlock("astral_prism",
            RefiningBlocks.Prism::new, p -> machine(p, MapColor.LAPIS, SoundType.AMETHYST, 14));
    public static final DeferredItem<BlockItem> ASTRAL_PRISM_ITEM = Mistech.ITEMS.registerSimpleBlockItem("astral_prism", ASTRAL_PRISM,
            p -> p.rarity(Rarity.EPIC));

    public static final Supplier<BlockEntityType<EssenceWasherBlockEntity>> ESSENCE_WASHER_BE = MistechBlockEntities.BLOCK_ENTITY_TYPES.register(
            "essence_washer", () -> new BlockEntityType<>(EssenceWasherBlockEntity::new, ESSENCE_WASHER.get()));
    public static final Supplier<BlockEntityType<RunicCrystallizerBlockEntity>> RUNIC_CRYSTALLIZER_BE = MistechBlockEntities.BLOCK_ENTITY_TYPES.register(
            "runic_crystallizer", () -> new BlockEntityType<>(RunicCrystallizerBlockEntity::new, RUNIC_CRYSTALLIZER.get()));
    public static final Supplier<BlockEntityType<AstralPrismBlockEntity>> ASTRAL_PRISM_BE = MistechBlockEntities.BLOCK_ENTITY_TYPES.register(
            "astral_prism", () -> new BlockEntityType<>(AstralPrismBlockEntity::new, ASTRAL_PRISM.get()));

    // ----- telas -----
    private static Supplier<MenuType<MachineMenu>> menu(String name, Supplier<MachineLayout> layout) {
        java.util.concurrent.atomic.AtomicReference<MenuType<MachineMenu>> ref = new java.util.concurrent.atomic.AtomicReference<>();
        return MistechMenus.MENUS.register(name, () -> {
            MenuType<MachineMenu> type = new MenuType<>((id, inv) -> new MachineMenu(ref.get(), layout.get(), id, inv), FeatureFlags.VANILLA_SET);
            ref.set(type);
            return type;
        });
    }

    public static final Supplier<MenuType<MachineMenu>> WASHER_MENU = menu("essence_washer", () -> RefiningContent.WASHER_LAYOUT);
    public static final Supplier<MenuType<MachineMenu>> CRYSTALLIZER_MENU = menu("runic_crystallizer", () -> RefiningContent.CRYSTALLIZER_LAYOUT);
    public static final Supplier<MenuType<MachineMenu>> PRISM_MENU = menu("astral_prism", () -> RefiningContent.PRISM_LAYOUT);

    /** Minério em cima, frascos/baldes de essência embaixo, 2 saídas; tanque à direita (x = 124). */
    public static final MachineLayout WASHER_LAYOUT = MachineLayout.builder("essence_washer", WASHER_MENU::get)
            .slot(40, 25, INPUT).slot(40, 53, FUEL).grid(98, 26, 1, 2, OUTPUT)
            .capacity(30_000).input(RefineryBlockEntity::isOre).fuel(s -> RefineryBlockEntity.isEssence(s, false))
            .arrow(66, 34).build();
    public static final MachineLayout CRYSTALLIZER_LAYOUT = MachineLayout.builder("runic_crystallizer", CRYSTALLIZER_MENU::get)
            .slot(40, 25, INPUT).slot(40, 53, FUEL).grid(98, 26, 1, 2, OUTPUT)
            .capacity(40_000).input(RefineryBlockEntity::isOre).fuel(s -> RefineryBlockEntity.isEssence(s, true))
            .arrow(66, 34).build();
    /** Minérios em cima, Fragmentos Estelares embaixo, 2 saídas; tanque de Essência Purificada à esquerda (x = 18). */
    public static final MachineLayout PRISM_LAYOUT = MachineLayout.builder("astral_prism", PRISM_MENU::get)
            .slot(40, 25, INPUT).slot(40, 53, FUEL).grid(98, 26, 1, 2, OUTPUT)
            .capacity(200_000).input(RefineryBlockEntity::isOre).fuel(AstralPrismBlockEntity::isFragment)
            .arrow(66, 34).build();

    public static List<DeferredItem<? extends Item>> creativeItems() {
        List<DeferredItem<? extends Item>> list = new ArrayList<>(List.of(ESSENCE_WASHER_ITEM, RUNIC_CRYSTALLIZER_ITEM, ASTRAL_PRISM_ITEM));
        list.addAll(CLUMPS);
        list.addAll(CRYSTALS);
        return list;
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(RefiningContent::onCapabilities);
    }

    private static void onCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ESSENCE_WASHER_BE.get(), (be, side) -> be.getEnergyHandler());
        event.registerBlockEntity(Capabilities.Item.BLOCK, ESSENCE_WASHER_BE.get(), (be, side) -> new WorldlyContainerWrapper(be, side));
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, ESSENCE_WASHER_BE.get(), (be, side) -> be.getTank());
        event.registerBlockEntity(Capabilities.Energy.BLOCK, RUNIC_CRYSTALLIZER_BE.get(), (be, side) -> be.getEnergyHandler());
        event.registerBlockEntity(Capabilities.Item.BLOCK, RUNIC_CRYSTALLIZER_BE.get(), (be, side) -> new WorldlyContainerWrapper(be, side));
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, RUNIC_CRYSTALLIZER_BE.get(), (be, side) -> be.getTank());
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ASTRAL_PRISM_BE.get(), (be, side) -> be.getEnergyHandler());
        event.registerBlockEntity(Capabilities.Item.BLOCK, ASTRAL_PRISM_BE.get(), (be, side) -> new WorldlyContainerWrapper(be, side));
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, ASTRAL_PRISM_BE.get(), (be, side) -> be.getTank());
    }
}

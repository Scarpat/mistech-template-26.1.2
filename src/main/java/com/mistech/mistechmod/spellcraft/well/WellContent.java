package com.mistech.mistechmod.spellcraft.well;

import static com.mistech.mistechmod.machine.MachineLayout.Kind.FUEL;
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
 * Fase 5 do plano magia × tech: fecha o triângulo FE ↔ Essência ↔ mana.
 * Poço de Mana (FE + Essência Purificada → mana do jogador) e Sangria Arcana (mana do jogador → FE).
 */
public final class WellContent {
    private WellContent() {}

    private static BlockBehaviour.Properties machine(BlockBehaviour.Properties p, MapColor color, SoundType sound, int light) {
        return p.mapColor(color).requiresCorrectToolForDrops().strength(3.5F, 6.0F).sound(sound).noOcclusion()
                .lightLevel(state -> state.getValue(MachineBlock.LIT) ? light : 4);
    }

    public static final DeferredBlock<WellBlocks.Well> MANA_WELL = Mistech.BLOCKS.registerBlock("mana_well", WellBlocks.Well::new,
            p -> machine(p, MapColor.QUARTZ, SoundType.CALCITE, 12));
    public static final DeferredItem<BlockItem> MANA_WELL_ITEM = Mistech.ITEMS.registerSimpleBlockItem("mana_well", MANA_WELL,
            p -> p.rarity(Rarity.RARE));
    public static final DeferredBlock<WellBlocks.Siphon> MANA_SIPHON = Mistech.BLOCKS.registerBlock("mana_siphon", WellBlocks.Siphon::new,
            p -> machine(p, MapColor.COLOR_BLACK, SoundType.POLISHED_DEEPSLATE, 9));
    public static final DeferredItem<BlockItem> MANA_SIPHON_ITEM = Mistech.ITEMS.registerSimpleBlockItem("mana_siphon", MANA_SIPHON,
            p -> p.rarity(Rarity.UNCOMMON));

    /** Poção de Mana: Pote de Vidro com a mana líquida do Poço. */
    public static final DeferredItem<ManaPotionItem> MANA_POTION = Mistech.ITEMS.registerItem("mana_potion", ManaPotionItem::new,
            p -> p.stacksTo(16).rarity(Rarity.UNCOMMON).usingConvertsTo(net.minecraft.world.item.Items.GLASS_BOTTLE)
                    .component(net.minecraft.core.component.DataComponents.CONSUMABLE,
                            net.minecraft.world.item.component.Consumables.defaultDrink().build()));

    public static final Supplier<BlockEntityType<ManaWellBlockEntity>> MANA_WELL_BE = MistechBlockEntities.BLOCK_ENTITY_TYPES.register(
            "mana_well", () -> new BlockEntityType<>(ManaWellBlockEntity::new, MANA_WELL.get()));
    public static final Supplier<BlockEntityType<ManaSiphonBlockEntity>> MANA_SIPHON_BE = MistechBlockEntities.BLOCK_ENTITY_TYPES.register(
            "mana_siphon", () -> new BlockEntityType<>(ManaSiphonBlockEntity::new, MANA_SIPHON.get()));

    private static Supplier<MenuType<MachineMenu>> menu(String name, Supplier<MachineLayout> layout) {
        java.util.concurrent.atomic.AtomicReference<MenuType<MachineMenu>> ref = new java.util.concurrent.atomic.AtomicReference<>();
        return MistechMenus.MENUS.register(name, () -> {
            MenuType<MachineMenu> type = new MenuType<>((id, inv) -> new MachineMenu(ref.get(), layout.get(), id, inv), FeatureFlags.VANILLA_SET);
            ref.set(type);
            return type;
        });
    }

    public static final Supplier<MenuType<MachineMenu>> WELL_MENU = menu("mana_well", () -> WellContent.WELL_LAYOUT);
    public static final Supplier<MenuType<MachineMenu>> SIPHON_MENU = menu("mana_siphon", () -> WellContent.SIPHON_LAYOUT);

    /** Frascos/baldes de Essência Purificada, saída dos vazios e a runa de afinidade; tanque à direita (x = 124). */
    public static final MachineLayout WELL_LAYOUT = MachineLayout.builder("mana_well", WELL_MENU::get)
            .slot(30, 50, FUEL).slot(58, 50, OUTPUT).slot(76, 50, RUNE)
            .capacity(ManaWellBlockEntity.CAPACITY)
            .fuel(ManaWellBlockEntity::isEssence).rune(ManaWellBlockEntity::isRune)
            .hint(0, com.mistech.mistechmod.purity.PurityContent.PURIFIED_ESSENCE_BOTTLE, "hint.mistech.mana_well.essence")
            .hint(2, com.mistech.mistechmod.Mistech.ARCANE_RUNE, "hint.mistech.mana_well.rune")
            .info(52, 18, 66, 28, menu -> ManaWellBlockEntity.infoLines(menu.info(0), menu.info(2)))
            .build();
    /** Só a barra de energia. */
    public static final MachineLayout SIPHON_LAYOUT = MachineLayout.builder("mana_siphon", SIPHON_MENU::get)
            .capacity(ManaSiphonBlockEntity.CAPACITY)
            .info(12, 22, menu -> ManaSiphonBlockEntity.infoLines(menu.info(0), menu.info(1)))
            .build();

    public static List<DeferredItem<? extends Item>> creativeItems() {
        return List.of(MANA_WELL_ITEM, MANA_SIPHON_ITEM, MANA_POTION);
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(WellContent::onCapabilities);
    }

    private static void onCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Energy.BLOCK, MANA_WELL_BE.get(), (be, side) -> be.getEnergyHandler());
        event.registerBlockEntity(Capabilities.Item.BLOCK, MANA_WELL_BE.get(), (be, side) -> new WorldlyContainerWrapper(be, side));
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, MANA_WELL_BE.get(), (be, side) -> be.getTank());
        event.registerBlockEntity(Capabilities.Energy.BLOCK, MANA_SIPHON_BE.get(), (be, side) -> be.getEnergyHandler());
    }
}

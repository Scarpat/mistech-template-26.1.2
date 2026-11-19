package com.mistech.mistechmod.purity.area;

import static com.mistech.mistechmod.machine.MachineLayout.Kind.FUEL;

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
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper;

/** Fase 4 do plano magia × tech: Mácula do lugar. Filtro de Arruda (a Grama Murcha é a do Reino dos Gnomos). */
public final class AreaTaintContent {
    private AreaTaintContent() {}

    public static final DeferredBlock<RueFilterBlock> RUE_FILTER = Mistech.BLOCKS.registerBlock("rue_filter", RueFilterBlock::new,
            p -> p.mapColor(MapColor.COLOR_GREEN).requiresCorrectToolForDrops().strength(3.5F, 6.0F).sound(SoundType.COPPER).noOcclusion()
                    .lightLevel(state -> state.getValue(MachineBlock.LIT) ? 6 : 0));
    public static final DeferredItem<BlockItem> RUE_FILTER_ITEM = Mistech.ITEMS.registerSimpleBlockItem("rue_filter", RUE_FILTER,
            p -> p.rarity(Rarity.UNCOMMON));

    public static final Supplier<BlockEntityType<RueFilterBlockEntity>> RUE_FILTER_BE = MistechBlockEntities.BLOCK_ENTITY_TYPES.register(
            "rue_filter", () -> new BlockEntityType<>(RueFilterBlockEntity::new, RUE_FILTER.get()));

    private static Supplier<MenuType<MachineMenu>> menu(String name, Supplier<MachineLayout> layout) {
        java.util.concurrent.atomic.AtomicReference<MenuType<MachineMenu>> ref = new java.util.concurrent.atomic.AtomicReference<>();
        return MistechMenus.MENUS.register(name, () -> {
            MenuType<MachineMenu> type = new MenuType<>((id, inv) -> new MachineMenu(ref.get(), layout.get(), id, inv), FeatureFlags.VANILLA_SET);
            ref.set(type);
            return type;
        });
    }

    public static final Supplier<MenuType<MachineMenu>> RUE_FILTER_MENU = menu("rue_filter", () -> AreaTaintContent.RUE_FILTER_LAYOUT);

    /** Folha de Arruda no espaço de combustível; a chama mostra quanto falta da folha atual. */
    public static final MachineLayout RUE_FILTER_LAYOUT = MachineLayout.builder("rue_filter", RUE_FILTER_MENU::get)
            .slot(30, 50, FUEL)
            .capacity(10_000)
            .fuel(RueFilterBlockEntity::isLeaf)
            .flame(31, 32)
            .info(56, 24, menu -> RueFilterBlockEntity.infoLines(menu.info(0), menu.info(1), menu.info(2)))
            .build();

    public static List<DeferredItem<? extends Item>> creativeItems() {
        return List.of(RUE_FILTER_ITEM);
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(AreaTaintContent::onCapabilities);
    }

    private static void onCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Energy.BLOCK, RUE_FILTER_BE.get(), (be, side) -> be.getEnergyHandler());
        event.registerBlockEntity(Capabilities.Item.BLOCK, RUE_FILTER_BE.get(), (be, side) -> new WorldlyContainerWrapper(be, side));
    }
}

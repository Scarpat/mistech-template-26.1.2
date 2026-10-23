package com.mistech.mistechmod.machine.forge;

import java.util.List;
import java.util.function.Supplier;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.machine.MachineBlock;
import com.mistech.mistechmod.machine.MachineLayout;
import com.mistech.mistechmod.machine.MachineMenu;
import com.mistech.mistechmod.registry.MistechBlockEntities;
import com.mistech.mistechmod.registry.MistechMenus;

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

import static com.mistech.mistechmod.machine.MachineLayout.Kind.*;

/** Forja Trituradora (multibloco 2×2×2). */
public final class ForgeContent {
    private ForgeContent() {}

    public static final DeferredBlock<CrusherForgeBlock> CRUSHER_FORGE = Mistech.BLOCKS.registerBlock("crusher_forge", CrusherForgeBlock::new,
            p -> p.mapColor(MapColor.COLOR_PURPLE).requiresCorrectToolForDrops().strength(4.5F, 9.0F).sound(SoundType.DEEPSLATE_TILES)
                    .noOcclusion().lightLevel(s -> s.getValue(MachineBlock.LIT) ? 13 : 3));
    public static final DeferredItem<BlockItem> CRUSHER_FORGE_ITEM = Mistech.ITEMS.registerSimpleBlockItem("crusher_forge", CRUSHER_FORGE,
            p -> p.rarity(Rarity.RARE));

    public static final Supplier<BlockEntityType<CrusherForgeBlockEntity>> CRUSHER_FORGE_BE = MistechBlockEntities.BLOCK_ENTITY_TYPES.register(
            "crusher_forge", () -> new BlockEntityType<>(CrusherForgeBlockEntity::new, CRUSHER_FORGE.get()));

    private static final java.util.concurrent.atomic.AtomicReference<MenuType<MachineMenu>> MENU_REF = new java.util.concurrent.atomic.AtomicReference<>();
    public static final Supplier<MenuType<MachineMenu>> FORGE_MENU = MistechMenus.MENUS.register("crusher_forge", () -> {
        MenuType<MachineMenu> type = new MenuType<>((id, inv) -> new MachineMenu(MENU_REF.get(), ForgeContent.FORGE_LAYOUT, id, inv),
                FeatureFlags.VANILLA_SET);
        MENU_REF.set(type);
        return type;
    });

    /** 3 entradas (coluna), 4 saídas; aviso de estrutura embaixo da seta. */
    public static final MachineLayout FORGE_LAYOUT = MachineLayout.builder("crusher_forge", FORGE_MENU::get)
            .slot(40, 17, INPUT).slot(40, 35, INPUT).slot(40, 53, INPUT)
            .grid(98, 26, 2, 2, OUTPUT)
            .capacity(60_000)
            .arrow(66, 34)
            .info(60, 64, menu -> menu.info(0) == 0 ? List.of(Component.translatable("gui.mistech.forge.incomplete")) : List.of())
            .build();

    public static List<DeferredItem<? extends Item>> creativeItems() {
        return List.of(CRUSHER_FORGE_ITEM);
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ForgeContent::onCapabilities);
    }

    private static void onCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Energy.BLOCK, CRUSHER_FORGE_BE.get(), (be, side) -> be.getEnergyHandler());
        event.registerBlockEntity(Capabilities.Item.BLOCK, CRUSHER_FORGE_BE.get(), (be, side) -> new WorldlyContainerWrapper(be, side));
    }
}

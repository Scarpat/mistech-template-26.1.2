package com.mistech.mistechmod.infuser;

import java.util.List;
import java.util.function.Supplier;

import com.mistech.mistechmod.Mistech;
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

/** Infusor Arcano (I, Maior e Astral): escolher o encanto e o nível em vez de sortear. */
public final class InfuserContent {
    public static final DeferredBlock<ArcaneInfuserBlock> ARCANE_INFUSER = block("arcane_infuser", 1);
    public static final DeferredBlock<ArcaneInfuserBlock> GREATER_ARCANE_INFUSER = block("greater_arcane_infuser", 2);
    public static final DeferredBlock<ArcaneInfuserBlock> ASTRAL_ARCANE_INFUSER = block("astral_arcane_infuser", 3);
    public static final DeferredItem<BlockItem> ARCANE_INFUSER_ITEM = Mistech.ITEMS.registerSimpleBlockItem("arcane_infuser", ARCANE_INFUSER,
            p -> p.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<BlockItem> GREATER_ARCANE_INFUSER_ITEM = Mistech.ITEMS.registerSimpleBlockItem("greater_arcane_infuser",
            GREATER_ARCANE_INFUSER, p -> p.rarity(Rarity.RARE));
    public static final DeferredItem<BlockItem> ASTRAL_ARCANE_INFUSER_ITEM = Mistech.ITEMS.registerSimpleBlockItem("astral_arcane_infuser",
            ASTRAL_ARCANE_INFUSER, p -> p.rarity(Rarity.EPIC));

    public static final Supplier<BlockEntityType<ArcaneInfuserBlockEntity>> ARCANE_INFUSER_BE = MistechBlockEntities.BLOCK_ENTITY_TYPES.register(
            "arcane_infuser", () -> new BlockEntityType<>(ArcaneInfuserBlockEntity::new, ARCANE_INFUSER.get(), GREATER_ARCANE_INFUSER.get(),
                    ASTRAL_ARCANE_INFUSER.get()));
    public static final Supplier<MenuType<ArcaneInfuserMenu>> ARCANE_INFUSER_MENU = MistechMenus.MENUS.register("arcane_infuser",
            () -> new MenuType<>(ArcaneInfuserMenu::new, FeatureFlags.VANILLA_SET));

    private static DeferredBlock<ArcaneInfuserBlock> block(String name, int tier) {
        return Mistech.BLOCKS.registerBlock(name, p -> new ArcaneInfuserBlock(tier, p), p -> p.mapColor(tier == 3 ? MapColor.COLOR_LIGHT_BLUE
                : MapColor.COLOR_PURPLE).requiresCorrectToolForDrops().strength(5.0F, 1200.0F).sound(SoundType.AMETHYST).noOcclusion()
                .lightLevel(s -> 4 + tier * 2));
    }

    private InfuserContent() {}

    public static List<DeferredItem<? extends Item>> creativeItems() {
        return List.of(ARCANE_INFUSER_ITEM, GREATER_ARCANE_INFUSER_ITEM, ASTRAL_ARCANE_INFUSER_ITEM);
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(InfuserContent::onCapabilities);
    }

    private static void onCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ARCANE_INFUSER_BE.get(), (be, side) -> be.getEnergyHandler());
    }
}

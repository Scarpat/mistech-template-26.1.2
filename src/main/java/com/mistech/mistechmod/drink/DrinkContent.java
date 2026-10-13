package com.mistech.mistechmod.drink;

import java.util.List;
import java.util.function.Supplier;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.machine.MachineBlock;
import com.mistech.mistechmod.machine.MachineLayout;
import com.mistech.mistechmod.machine.MachineMenu;
import com.mistech.mistechmod.registry.MistechBlockEntities;
import com.mistech.mistechmod.registry.MistechMenus;

import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper;

/** Cachaça: cana → Garapa → (Barril) Garapa Fermentada → (Alambique) Cachaça → (Barril) Cachaça Envelhecida. */
public final class DrinkContent {
    // ------------------------------------------------------------------ bebidas

    /** Garapa: caldo de cana. Bebe = um pouco de fome e Velocidade 30 s. */
    public static final DeferredItem<Item> GARAPA = Mistech.ITEMS.registerSimpleItem("garapa", p -> p.stacksTo(16)
            .usingConvertsTo(Items.GLASS_BOTTLE)
            .food(new FoodProperties.Builder().nutrition(3).saturationModifier(0.3F).alwaysEdible().build(), Consumables.defaultDrink()
                    .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.SPEED, 600, 0))).build()));
    public static final DeferredItem<Item> FERMENTED_GARAPA = Mistech.ITEMS.registerSimpleItem("fermented_garapa", p -> p.stacksTo(16));

    public static final DeferredItem<CachacaItem> CACHACA = cachaca("cachaca", CachacaItem.Kind.PLAIN, Rarity.COMMON);
    public static final DeferredItem<CachacaItem> AGED_CACHACA = cachaca("aged_cachaca", CachacaItem.Kind.AGED, Rarity.UNCOMMON);
    public static final DeferredItem<CachacaItem> RUE_CACHACA = cachaca("rue_cachaca", CachacaItem.Kind.RUE, Rarity.UNCOMMON);
    public static final DeferredItem<CachacaItem> HONEY_CACHACA = cachaca("honey_cachaca", CachacaItem.Kind.HONEY, Rarity.UNCOMMON);

    private static DeferredItem<CachacaItem> cachaca(String name, CachacaItem.Kind kind, Rarity rarity) {
        return Mistech.ITEMS.registerItem(name, p -> new CachacaItem(kind, p), p -> p.stacksTo(16).rarity(rarity)
                .usingConvertsTo(Items.GLASS_BOTTLE)
                .component(net.minecraft.core.component.DataComponents.CONSUMABLE, Consumables.defaultDrink().build()));
    }

    // ------------------------------------------------------------------ barril

    public static final DeferredBlock<FermentationBarrelBlock> FERMENTATION_BARREL = Mistech.BLOCKS.registerBlock("fermentation_barrel",
            FermentationBarrelBlock::new, p -> p.mapColor(MapColor.COLOR_GRAY).strength(2.5F).sound(SoundType.WOOD).ignitedByLava());
    public static final DeferredItem<BlockItem> FERMENTATION_BARREL_ITEM = Mistech.ITEMS.registerSimpleBlockItem("fermentation_barrel",
            FERMENTATION_BARREL);
    public static final Supplier<BlockEntityType<FermentationBarrelBlockEntity>> FERMENTATION_BARREL_BE = MistechBlockEntities.BLOCK_ENTITY_TYPES
            .register("fermentation_barrel", () -> new BlockEntityType<>(FermentationBarrelBlockEntity::new, FERMENTATION_BARREL.get()));

    // ------------------------------------------------------------------ alambique

    public static final DeferredBlock<CaneStillBlock> CANE_STILL = Mistech.BLOCKS.registerBlock("cane_still", CaneStillBlock::new,
            p -> p.mapColor(MapColor.COLOR_ORANGE).requiresCorrectToolForDrops().strength(3.0F, 6.0F).sound(SoundType.COPPER).noOcclusion()
                    .lightLevel(s -> s.getValue(MachineBlock.LIT) ? 6 : 0));
    public static final DeferredItem<BlockItem> CANE_STILL_ITEM = Mistech.ITEMS.registerSimpleBlockItem("cane_still", CANE_STILL);
    public static final Supplier<BlockEntityType<CaneStillBlockEntity>> CANE_STILL_BE = MistechBlockEntities.BLOCK_ENTITY_TYPES.register("cane_still",
            () -> new BlockEntityType<>(CaneStillBlockEntity::new, CANE_STILL.get()));

    private static final java.util.concurrent.atomic.AtomicReference<MenuType<MachineMenu>> STILL_MENU_REF = new java.util.concurrent.atomic.AtomicReference<>();
    public static final Supplier<MenuType<MachineMenu>> CANE_STILL_MENU = MistechMenus.MENUS.register("cane_still", () -> {
        MenuType<MachineMenu> type = new MenuType<>((id, inv) -> new MachineMenu(STILL_MENU_REF.get(), DrinkContent.STILL_LAYOUT, id, inv),
                FeatureFlags.VANILLA_SET);
        STILL_MENU_REF.set(type);
        return type;
    });

    /** Mesmo desenho do Triturador: entrada, runa (Fogo) e 4 saídas; a linha de baixo diz de onde vem o calor. */
    public static final MachineLayout STILL_LAYOUT = MachineLayout.builder("cane_still", CANE_STILL_MENU::get)
            .slot(40, 25, MachineLayout.Kind.INPUT)
            .slot(40, 53, MachineLayout.Kind.RUNE)
            .grid(98, 26, 2, 2, MachineLayout.Kind.OUTPUT)
            .capacity(10_000)
            .input(s -> s.is(FERMENTED_GARAPA.get()))
            .rune(s -> s.is(Mistech.FIRE_RUNE.get()))
            .arrow(66, 34)
            .info(64, 64, menu -> List.of(Component.translatable("gui.mistech.cane_still.heat." + menu.info(0))))
            .build();

    private DrinkContent() {}

    public static List<DeferredItem<? extends Item>> creativeItems() {
        return List.of(GARAPA, FERMENTED_GARAPA, CACHACA, AGED_CACHACA, RUE_CACHACA, HONEY_CACHACA, FERMENTATION_BARREL_ITEM, CANE_STILL_ITEM);
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(DrinkContent::onCapabilities);
    }

    private static void onCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Energy.BLOCK, CANE_STILL_BE.get(), (machine, side) -> machine.getEnergyHandler());
        event.registerBlockEntity(Capabilities.Item.BLOCK, CANE_STILL_BE.get(), (machine, side) -> new WorldlyContainerWrapper(machine, side));
    }
}

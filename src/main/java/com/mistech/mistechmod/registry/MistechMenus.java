package com.mistech.mistechmod.registry;

import java.util.function.Supplier;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.menu.CondenserMenu;
import com.mistech.mistechmod.menu.IncenseHolderMenu;
import com.mistech.mistechmod.menu.RunicFurnaceMenu;
import com.mistech.mistechmod.menu.WritingTableMenu;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class MistechMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Mistech.MODID);

    public static final Supplier<MenuType<IncenseHolderMenu>> INCENSE_HOLDER =
            MENUS.register("incense_holder", () -> new MenuType<>(IncenseHolderMenu::new, FeatureFlags.VANILLA_SET));

    public static final Supplier<MenuType<WritingTableMenu>> WRITING_TABLE =
            MENUS.register("writing_table", () -> new MenuType<>(WritingTableMenu::new, FeatureFlags.VANILLA_SET));

    public static final Supplier<MenuType<RunicFurnaceMenu>> RUNIC_FURNACE =
            MENUS.register("runic_furnace", () -> new MenuType<>(RunicFurnaceMenu::new, FeatureFlags.VANILLA_SET));

    public static final Supplier<MenuType<CondenserMenu>> CONDENSER =
            MENUS.register("condenser", () -> new MenuType<>(CondenserMenu::new, FeatureFlags.VANILLA_SET));

    public static final Supplier<MenuType<com.mistech.mistechmod.menu.SpatialStorageMenu>> SPATIAL_STORAGE =
            MENUS.register("spatial_storage", () -> new MenuType<>(com.mistech.mistechmod.menu.SpatialStorageMenu::new, FeatureFlags.VANILLA_SET));

    public static final Supplier<MenuType<com.mistech.mistechmod.menu.RuneConfectorMenu>> RUNE_CONFECTOR =
            MENUS.register("rune_confector", () -> new MenuType<>(com.mistech.mistechmod.menu.RuneConfectorMenu::new, FeatureFlags.VANILLA_SET));

    public static final Supplier<MenuType<com.mistech.mistechmod.menu.RuneRollerMenu>> RUNE_ROLLER =
            MENUS.register("rune_roller", () -> new MenuType<>(com.mistech.mistechmod.menu.RuneRollerMenu::new, FeatureFlags.VANILLA_SET));

    public static final Supplier<MenuType<com.mistech.mistechmod.accessory.AccessoriesMenu>> ACCESSORIES =
            MENUS.register("accessories", () -> new MenuType<>(com.mistech.mistechmod.accessory.AccessoriesMenu::new, FeatureFlags.VANILLA_SET));

    public static final Supplier<MenuType<com.mistech.mistechmod.menu.RuneAnvilMenu>> RUNE_ANVIL =
            MENUS.register("rune_anvil", () -> new MenuType<>(com.mistech.mistechmod.menu.RuneAnvilMenu::new, FeatureFlags.VANILLA_SET));

    // ----- máquinas genéricas (MachineMenu + MachineLayout) -----
    public static final Supplier<MenuType<com.mistech.mistechmod.machine.MachineMenu>> RUNIC_CRUSHER = machine("runic_crusher",
            () -> com.mistech.mistechmod.machine.MachineLayouts.CRUSHER);
    public static final Supplier<MenuType<com.mistech.mistechmod.machine.MachineMenu>> ASH_GENERATOR = machine("ash_generator",
            () -> com.mistech.mistechmod.machine.MachineLayouts.ASH_GENERATOR);
    public static final Supplier<MenuType<com.mistech.mistechmod.machine.MachineMenu>> GNOME_BICYCLE = machine("gnome_bicycle",
            () -> com.mistech.mistechmod.machine.MachineLayouts.GNOME_BICYCLE);
    public static final Supplier<MenuType<com.mistech.mistechmod.machine.MachineMenu>> ASTRAL_GENERATOR = machine("astral_generator",
            () -> com.mistech.mistechmod.machine.MachineLayouts.ASTRAL_GENERATOR);
    public static final Supplier<MenuType<com.mistech.mistechmod.machine.MachineMenu>> RUNIC_HARVESTER = machine("runic_harvester",
            () -> com.mistech.mistechmod.machine.MachineLayouts.HARVESTER);
    public static final Supplier<MenuType<com.mistech.mistechmod.machine.MachineMenu>> RUNIC_PLANTER = machine("runic_planter",
            () -> com.mistech.mistechmod.machine.MachineLayouts.PLANTER);

    public static final Supplier<MenuType<com.mistech.mistechmod.machine.MachineMenu>> MINER = machine("miner",
            () -> com.mistech.mistechmod.machine.MachineLayouts.MINER);

    public static final Supplier<MenuType<com.mistech.mistechmod.machine.MachineMenu>> RUNIC_BATTERY = machine("runic_battery",
            () -> com.mistech.mistechmod.machine.MachineLayouts.RUNIC_BATTERY);
    public static final Supplier<MenuType<com.mistech.mistechmod.machine.MachineMenu>> ADVANCED_RUNIC_BATTERY = machine("advanced_runic_battery",
            () -> com.mistech.mistechmod.machine.MachineLayouts.ADVANCED_RUNIC_BATTERY);

    // ----- Magia Purificada -----
    public static final Supplier<MenuType<com.mistech.mistechmod.machine.MachineMenu>> DISTILLER = machine("distiller",
            () -> com.mistech.mistechmod.purity.PurityLayouts.DISTILLER);
    public static final Supplier<MenuType<com.mistech.mistechmod.machine.MachineMenu>> PURE_GENERATOR = machine("pure_generator",
            () -> com.mistech.mistechmod.purity.PurityLayouts.PURE_GENERATOR);

    public static final Supplier<MenuType<com.mistech.mistechmod.machine.MachineMenu>> BLAZE_GENERATOR = machine("blaze_generator",
            () -> com.mistech.mistechmod.beyond.BeyondLayouts.BLAZE_GENERATOR);

    // ----- Geradores avançados -----
    public static final Supplier<MenuType<com.mistech.mistechmod.machine.MachineMenu>> IMPURE_REACTOR = machine("impure_reactor",
            () -> com.mistech.mistechmod.power.PowerLayouts.IMPURE_REACTOR);
    public static final Supplier<MenuType<com.mistech.mistechmod.machine.MachineMenu>> STELLAR_OBSERVATORY = machine("stellar_observatory",
            () -> com.mistech.mistechmod.power.PowerLayouts.STELLAR_OBSERVATORY);
    public static final Supplier<MenuType<com.mistech.mistechmod.machine.MachineMenu>> IGNEOUS_BOILER = machine("igneous_boiler",
            () -> com.mistech.mistechmod.power.PowerLayouts.IGNEOUS_BOILER);
    public static final Supplier<MenuType<com.mistech.mistechmod.machine.MachineMenu>> EXPERIENCE_GENERATOR = machine("experience_generator",
            () -> com.mistech.mistechmod.power.PowerLayouts.EXPERIENCE_GENERATOR);

    private static Supplier<MenuType<com.mistech.mistechmod.machine.MachineMenu>> machine(String name,
            Supplier<com.mistech.mistechmod.machine.MachineLayout> layout) {
        java.util.concurrent.atomic.AtomicReference<MenuType<com.mistech.mistechmod.machine.MachineMenu>> ref =
                new java.util.concurrent.atomic.AtomicReference<>();
        return MENUS.register(name, () -> {
            MenuType<com.mistech.mistechmod.machine.MachineMenu> type = new MenuType<>(
                    (id, inv) -> new com.mistech.mistechmod.machine.MachineMenu(ref.get(), layout.get(), id, inv),
                    FeatureFlags.VANILLA_SET);
            ref.set(type);
            return type;
        });
    }

    private MistechMenus() {}

    public static void register(IEventBus modEventBus) {
        MENUS.register(modEventBus);
    }
}

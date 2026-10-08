package com.mistech.mistechmod.mage;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.machine.MachineLayout;
import com.mistech.mistechmod.machine.MachineMenu;
import com.mistech.mistechmod.registry.MistechBlockEntities;
import com.mistech.mistechmod.registry.MistechComponents;
import com.mistech.mistechmod.registry.MistechMenus;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.transfer.energy.ItemAccessEnergyHandler;
import net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper;

/** Equipamento de mago: Tear Mágico, Tecido Mágico e as Vestes de Mago (Cânhamo → Iniciado → Adepto → Arquimago). */
public final class MageContent {
    /** Conserta as vestes (e base das receitas). */
    public static final TagKey<Item> MAGIC_CLOTH_TAG = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Mistech.MODID, "magic_cloth"));

    public static final DeferredItem<Item> MAGIC_CLOTH = Mistech.ITEMS.registerSimpleItem("magic_cloth");

    // ------------------------------------------------------------------ Tear Mágico

    public static final DeferredBlock<MagicLoomBlock> MAGIC_LOOM = Mistech.BLOCKS.registerBlock("magic_loom", MagicLoomBlock::new,
            p -> p.mapColor(MapColor.COLOR_PURPLE).strength(2.5F).sound(SoundType.WOOD).noOcclusion()
                    .lightLevel(s -> s.getValue(com.mistech.mistechmod.machine.MachineBlock.LIT) ? 7 : 0));
    public static final DeferredItem<BlockItem> MAGIC_LOOM_ITEM = Mistech.ITEMS.registerSimpleBlockItem("magic_loom", MAGIC_LOOM);
    public static final Supplier<BlockEntityType<MagicLoomBlockEntity>> MAGIC_LOOM_BE = MistechBlockEntities.BLOCK_ENTITY_TYPES.register("magic_loom",
            () -> new BlockEntityType<>(MagicLoomBlockEntity::new, MAGIC_LOOM.get()));

    private static final java.util.concurrent.atomic.AtomicReference<MenuType<MachineMenu>> LOOM_MENU_REF = new java.util.concurrent.atomic.AtomicReference<>();
    public static final Supplier<MenuType<MachineMenu>> MAGIC_LOOM_MENU = MistechMenus.MENUS.register("magic_loom", () -> {
        MenuType<MachineMenu> type = new MenuType<>((id, inv) -> new MachineMenu(LOOM_MENU_REF.get(), MageContent.LOOM_LAYOUT, id, inv),
                FeatureFlags.VANILLA_SET);
        LOOM_MENU_REF.set(type);
        return type;
    });

    /** Mesmo desenho do Triturador: entrada, runa (Ar) e 4 saídas. */
    public static final MachineLayout LOOM_LAYOUT = MachineLayout.builder("magic_loom", MAGIC_LOOM_MENU::get)
            .slot(40, 25, MachineLayout.Kind.INPUT)
            .slot(40, 53, MachineLayout.Kind.RUNE)
            .grid(98, 26, 2, 2, MachineLayout.Kind.OUTPUT)
            .capacity(10_000)
            .rune(s -> s.is(Mistech.AIR_RUNE.get()))
            .arrow(66, 34)
            .build();

    // ------------------------------------------------------------------ vestes

    private static ResourceKey<EquipmentAsset> asset(MageRobeTier tier) {
        return ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(Mistech.MODID, "mage_" + tier.id));
    }

    private static ArmorMaterial material(MageRobeTier tier) {
        return new ArmorMaterial(37, Map.of(ArmorType.BOOTS, tier.defense[0], ArmorType.LEGGINGS, tier.defense[1],
                ArmorType.CHESTPLATE, tier.defense[2], ArmorType.HELMET, tier.defense[3], ArmorType.BODY, tier.defense[2]),
                18 + tier.ordinal() * 2, SoundEvents.ARMOR_EQUIP_LEATHER, tier.toughness, 0.0F, MAGIC_CLOTH_TAG, asset(tier));
    }

    public static final String[] PIECES = {"hood", "robe", "pants", "shoes"};
    private static final ArmorType[] TYPES = {ArmorType.HELMET, ArmorType.CHESTPLATE, ArmorType.LEGGINGS, ArmorType.BOOTS};

    /** [nível][peça] */
    public static final Map<MageRobeTier, List<DeferredItem<MageRobeItem>>> ROBES = new EnumMap<>(MageRobeTier.class);

    static {
        for (MageRobeTier tier : MageRobeTier.values()) {
            ArmorMaterial material = material(tier);
            List<DeferredItem<MageRobeItem>> list = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                ArmorType type = TYPES[i];
                Rarity rarity = tier.ordinal() >= 2 ? Rarity.EPIC : tier.ordinal() == 1 ? Rarity.RARE : Rarity.UNCOMMON;
                list.add(Mistech.ITEMS.registerItem(tier.id + "_mage_" + PIECES[i], p -> new MageRobeItem(tier, p),
                        p -> p.humanoidArmor(material, type).rarity(rarity)));
            }
            ROBES.put(tier, list);
        }
    }

    private MageContent() {}

    public static List<DeferredItem<? extends Item>> robeItems() {
        List<DeferredItem<? extends Item>> out = new ArrayList<>();
        for (MageRobeTier tier : MageRobeTier.values()) {
            out.addAll(ROBES.get(tier));
        }
        return out;
    }

    public static List<DeferredItem<? extends Item>> creativeItems() {
        List<DeferredItem<? extends Item>> out = new ArrayList<>(List.of(MAGIC_LOOM_ITEM, MAGIC_CLOTH));
        out.addAll(robeItems());
        return out;
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(MageContent::onCapabilities);
    }

    private static void onCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Energy.BLOCK, MAGIC_LOOM_BE.get(), (machine, side) -> machine.getEnergyHandler());
        event.registerBlockEntity(Capabilities.Item.BLOCK, MAGIC_LOOM_BE.get(), (machine, side) -> new WorldlyContainerWrapper(machine, side));
        for (DeferredItem<? extends Item> item : robeItems()) {
            var tier = ((MageRobeItem) item.get()).tier();
            event.registerItem(Capabilities.Energy.ITEM,
                    (stack, access) -> new ItemAccessEnergyHandler(access, MistechComponents.GEAR_ENERGY.get(), tier.capacity(), tier.chargeRate(), 0),
                    item.get());
        }
    }
}

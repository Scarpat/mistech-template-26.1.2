package com.mistech.mistechmod.boitata;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.registry.GnomeContent;
import com.mistech.mistechmod.registry.MistechBlockEntities;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

/** Armadura do Boitatá (só escamas; o peitoral leva o Olho) e o Ovo do Boitatá, que choca uma montaria. */
public final class BoitataGear {
    /** Conserta a armadura. */
    public static final TagKey<Item> SCALE_TAG = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Mistech.MODID, "boitata_scales"));
    public static final ResourceKey<EquipmentAsset> ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID,
            Identifier.fromNamespaceAndPath(Mistech.MODID, "boitata"));
    /** Netherite + 20% de durabilidade (37 → 44), mesma proteção, imune a fogo. */
    public static final ArmorMaterial MATERIAL = new ArmorMaterial(44, Map.of(ArmorType.BOOTS, 3, ArmorType.LEGGINGS, 6,
            ArmorType.CHESTPLATE, 8, ArmorType.HELMET, 3, ArmorType.BODY, 11), 15, SoundEvents.ARMOR_EQUIP_NETHERITE, 3.0F, 0.1F, SCALE_TAG, ASSET);

    public static final DeferredItem<BoitataArmorItem> HELMET = armor("boitata_helmet", ArmorType.HELMET);
    public static final DeferredItem<BoitataArmorItem> CHESTPLATE = armor("boitata_chestplate", ArmorType.CHESTPLATE);
    public static final DeferredItem<BoitataArmorItem> LEGGINGS = armor("boitata_leggings", ArmorType.LEGGINGS);
    public static final DeferredItem<BoitataArmorItem> BOOTS = armor("boitata_boots", ArmorType.BOOTS);

    private static DeferredItem<BoitataArmorItem> armor(String name, ArmorType type) {
        return Mistech.ITEMS.registerItem(name, p -> new BoitataArmorItem(type, p),
                p -> p.humanoidArmor(MATERIAL, type).rarity(Rarity.EPIC).fireResistant());
    }

    // ------------------------------------------------------------------ ovo e montaria

    public static final DeferredBlock<BoitataEggBlock> EGG = Mistech.BLOCKS.registerBlock("boitata_egg", BoitataEggBlock::new,
            p -> p.mapColor(MapColor.COLOR_ORANGE).strength(3.0F, 1200.0F).sound(SoundType.METAL).noOcclusion().lightLevel(s -> 7));
    public static final DeferredItem<BlockItem> EGG_ITEM = Mistech.ITEMS.registerSimpleBlockItem("boitata_egg", EGG,
            p -> p.rarity(Rarity.EPIC).fireResistant().stacksTo(1));
    public static final Supplier<BlockEntityType<BoitataEggBlockEntity>> EGG_BE = MistechBlockEntities.BLOCK_ENTITY_TYPES.register("boitata_egg",
            () -> new BlockEntityType<>(BoitataEggBlockEntity::new, EGG.get()));

    public static final DeferredHolder<EntityType<?>, EntityType<BoitataMountEntity>> MOUNT = GnomeContent.ENTITIES.registerEntityType(
            "boitata_mount", BoitataMountEntity::new, MobCategory.CREATURE,
            b -> b.sized(1.5F, 0.9F).eyeHeight(0.7F).fireImmune().clientTrackingRange(10));

    public static final DeferredItem<BoitataMountArmorItem> MOUNT_ARMOR_SCALE = Mistech.ITEMS.registerItem("boitata_mount_armor_scale",
            p -> new BoitataMountArmorItem(BoitataMountArmorItem.Kind.SCALE, p), p -> p.stacksTo(1).rarity(Rarity.RARE).fireResistant());
    public static final DeferredItem<BoitataMountArmorItem> MOUNT_ARMOR_ORICHALCUM = Mistech.ITEMS.registerItem("boitata_mount_armor_orichalcum",
            p -> new BoitataMountArmorItem(BoitataMountArmorItem.Kind.ORICHALCUM, p), p -> p.stacksTo(1).rarity(Rarity.EPIC).fireResistant());
    public static final DeferredItem<BoitataWhistleItem> WHISTLE = Mistech.ITEMS.registerItem("boitata_whistle", BoitataWhistleItem::new,
            p -> p.stacksTo(1).rarity(Rarity.RARE));

    private BoitataGear() {}

    public static boolean wearing(Player player, EquipmentSlot slot) {
        ItemStack stack = player.getItemBySlot(slot);
        return stack.getItem() instanceof BoitataArmorItem;
    }

    public static boolean fullSet(Player player) {
        return wearing(player, EquipmentSlot.HEAD) && wearing(player, EquipmentSlot.CHEST) && wearing(player, EquipmentSlot.LEGS)
                && wearing(player, EquipmentSlot.FEET);
    }

    public static List<DeferredItem<? extends Item>> creativeItems() {
        return List.of(HELMET, CHESTPLATE, LEGGINGS, BOOTS, EGG_ITEM, WHISTLE, MOUNT_ARMOR_SCALE, MOUNT_ARMOR_ORICHALCUM);
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(BoitataGear::onAttributes);
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(MOUNT.get(), BoitataMountEntity.createAttributes().build());
    }
}

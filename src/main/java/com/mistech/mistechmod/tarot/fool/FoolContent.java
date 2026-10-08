package com.mistech.mistechmod.tarot.fool;

import java.util.List;
import java.util.Map;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.tarot.ArcanaBosses;
import com.mistech.mistechmod.tarot.TarotArcana;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * O Louco (arcano 0): chefe {@link FoolEntity}, tornados, arena de ilhas ({@link FoolArena}) e os dois equipamentos
 * da Forja — Chapéu do Bobo ({@link FoolCapItem}) e Cajado do Louco ({@link FoolStaffItem}).
 */
public final class FoolContent {
    public static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(Mistech.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<FoolEntity>> FOOL = ENTITIES.registerEntityType("the_fool",
            FoolEntity::new, MobCategory.MONSTER, b -> b.sized(0.9F, 2.8F).eyeHeight(2.35F).clientTrackingRange(10).fireImmune());
    public static final DeferredHolder<EntityType<?>, EntityType<FoolTornadoEntity>> TORNADO = ENTITIES.registerEntityType("fool_tornado",
            FoolTornadoEntity::new, MobCategory.MISC, b -> b.sized(1.4F, 3.0F).clientTrackingRange(8).updateInterval(2).noSave());

    // ----- Chapéu do Bobo -----
    public static final ResourceKey<EquipmentAsset> CAP_ASSET =
            ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(Mistech.MODID, "fool_cap"));
    /** Defesa de capacete de diamante (3, resistência 2), conserta com lã. */
    public static final ArmorMaterial CAP_MATERIAL = new ArmorMaterial(33, Map.of(ArmorType.HELMET, 3, ArmorType.CHESTPLATE, 8,
            ArmorType.LEGGINGS, 6, ArmorType.BOOTS, 3, ArmorType.BODY, 11), 15, SoundEvents.ARMOR_EQUIP_LEATHER, 2.0F, 0.0F,
            ItemTags.WOOL, CAP_ASSET);

    private static AttributeModifier mod(String id, double amount, AttributeModifier.Operation operation) {
        return new AttributeModifier(Identifier.fromNamespaceAndPath(Mistech.MODID, "fool." + id), amount, operation);
    }

    private static ItemAttributeModifiers capAttributes() {
        ItemAttributeModifiers modifiers = CAP_MATERIAL.createAttributes(ArmorType.HELMET);
        EquipmentSlotGroup head = EquipmentSlotGroup.HEAD;
        modifiers = modifiers.withModifierAdded(Attributes.FALL_DAMAGE_MULTIPLIER,
                mod("cap_fall", -0.5, AttributeModifier.Operation.ADD_MULTIPLIED_BASE), head);
        modifiers = modifiers.withModifierAdded(Attributes.SAFE_FALL_DISTANCE, mod("cap_safe_fall", 2.0, AttributeModifier.Operation.ADD_VALUE), head);
        modifiers = modifiers.withModifierAdded(Attributes.MOVEMENT_SPEED,
                mod("cap_speed", 0.08, AttributeModifier.Operation.ADD_MULTIPLIED_BASE), head);
        return modifiers;
    }

    public static final DeferredItem<FoolCapItem> FOOL_CAP = Mistech.ITEMS.registerItem("fool_cap", FoolCapItem::new,
            p -> p.humanoidArmor(CAP_MATERIAL, ArmorType.HELMET).attributes(capAttributes()).rarity(Rarity.EPIC));

    // ----- Cajado do Louco -----
    public static final float STAFF_TORNADO_DAMAGE = 5.0F;

    private static ItemAttributeModifiers staffAttributes() {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, 6.0, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, -2.3, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_KNOCKBACK, mod("staff_knockback", 1.5, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .build();
    }

    public static final DeferredItem<FoolStaffItem> FOOL_STAFF = Mistech.ITEMS.registerItem("fool_staff", FoolStaffItem::new,
            p -> p.durability(900).repairable(Items.BREEZE_ROD).attributes(staffAttributes()).enchantable(15).rarity(Rarity.EPIC));

    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> FOOL_SPAWN_EGG = Mistech.ITEMS.registerItem("the_fool_spawn_egg",
            net.minecraft.world.item.SpawnEggItem::new, p -> p.spawnEgg(FOOL.get()));

    private FoolContent() {}

    public static List<DeferredItem<? extends Item>> creativeItems() {
        return List.of(FOOL_CAP, FOOL_STAFF, FOOL_SPAWN_EGG);
    }

    public static void register(IEventBus modEventBus) {
        ENTITIES.register(modEventBus);
        modEventBus.addListener(FoolContent::onAttributes);

        ArcanaBosses.registerArena(TarotArcana.THE_FOOL, FoolArena.INSTANCE);
        ArcanaBosses.register(TarotArcana.THE_FOOL, (level, fight, spawn, challenger) -> {
            FoolEntity fool = FOOL.get().create(level, EntitySpawnReason.EVENT);
            if (fool != null) {
                fool.snapTo(spawn.x, spawn.y, spawn.z, 0.0F, 0.0F);
                fool.setArena(fight.center());
            }
            return fool;
        });
        // Forja: Carta do Louco + materiais nos pedestais (provisórios)
        TarotArcana.THE_FOOL.setOptions(
                new TarotArcana.Option("cap", () -> new ItemStack(FOOL_CAP.get()),
                        List.of(() -> Mistech.AIR_RUNE.get(), () -> Items.LEATHER, () -> Items.YELLOW_WOOL, () -> Items.WHITE_WOOL,
                                () -> Items.GOLD_NUGGET)),
                new TarotArcana.Option("staff", () -> new ItemStack(FOOL_STAFF.get()),
                        List.of(() -> Mistech.AIR_RUNE.get(), () -> Items.BREEZE_ROD, () -> Items.BREEZE_ROD, () -> Items.FEATHER,
                                () -> Items.DIAMOND)));
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(FOOL.get(), FoolEntity.createAttributes().build());
    }
}

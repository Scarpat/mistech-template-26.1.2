package com.mistech.mistechmod.registry;

import java.util.Map;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.item.MysticWearItem;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.neoforged.neoforge.registries.DeferredItem;

/** Vestes místicas: lã, defesa baixa, tingíveis como couro, cada uma com um bônus. */
public final class MistechWear {
    /** Cores padrão (sem tinta). */
    public static final int HAT_COLOR = 0x4E7A3A, ROBE_COLOR = 0x4B2A78, PANTS_COLOR = 0x6C8FD0, SLIPPERS_COLOR = 0xC9A2D8;

    private static ResourceKey<EquipmentAsset> asset(String name) {
        return ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(Mistech.MODID, name));
    }

    /** Material de lã (defesa: botas 1, calças 1, peito 2, cabeça 1), conserta com lã. */
    private static ArmorMaterial wool(String assetName) {
        return new ArmorMaterial(5, Map.of(ArmorType.BOOTS, 1, ArmorType.LEGGINGS, 1, ArmorType.CHESTPLATE, 2, ArmorType.HELMET, 1, ArmorType.BODY, 2),
                15, SoundEvents.ARMOR_EQUIP_LEATHER, 0.0F, 0.0F, ItemTags.WOOL, asset(assetName));
    }

    public static final ArmorMaterial HAT_MATERIAL = wool("gnome_hat");
    public static final ArmorMaterial ROBE_MATERIAL = wool("mystic_robe");
    public static final ArmorMaterial PANTS_MATERIAL = wool("comfy_pants");
    public static final ArmorMaterial SLIPPERS_MATERIAL = wool("slippers");

    private static AttributeModifier mod(String id, double amount, AttributeModifier.Operation operation) {
        return new AttributeModifier(Identifier.fromNamespaceAndPath(Mistech.MODID, "wear." + id), amount, operation);
    }

    private static ItemAttributeModifiers with(ArmorMaterial material, ArmorType type, Object... pairs) {
        ItemAttributeModifiers modifiers = material.createAttributes(type);
        EquipmentSlotGroup group = EquipmentSlotGroup.bySlot(type.getSlot());
        for (int i = 0; i < pairs.length; i += 2) {
            @SuppressWarnings("unchecked")
            Holder<Attribute> attribute = (Holder<Attribute>) pairs[i];
            modifiers = modifiers.withModifierAdded(attribute, (AttributeModifier) pairs[i + 1], group);
        }
        return modifiers;
    }

    // Chapéu de Gnomo: +1 sorte, +10% velocidade, 10% menor
    public static final DeferredItem<MysticWearItem> GNOME_HAT = Mistech.ITEMS.registerItem("gnome_hat",
            p -> new MysticWearItem(p, false), p -> p.humanoidArmor(HAT_MATERIAL, ArmorType.HELMET)
                    .attributes(with(HAT_MATERIAL, ArmorType.HELMET,
                            Attributes.LUCK, mod("gnome_hat_luck", 1.0, AttributeModifier.Operation.ADD_VALUE),
                            Attributes.MOVEMENT_SPEED, mod("gnome_hat_speed", 0.10, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
                            Attributes.SCALE, mod("gnome_hat_scale", -0.10, AttributeModifier.Operation.ADD_MULTIPLIED_BASE))));

    // Roupão Místico: gravidade 30% menor (dano de frio pela metade fica no evento)
    public static final DeferredItem<MysticWearItem> MYSTIC_ROBE = Mistech.ITEMS.registerItem("mystic_robe",
            p -> new MysticWearItem(p, false), p -> p.humanoidArmor(ROBE_MATERIAL, ArmorType.CHESTPLATE)
                    .attributes(with(ROBE_MATERIAL, ArmorType.CHESTPLATE,
                            Attributes.GRAVITY, mod("mystic_robe_gravity", -0.30, AttributeModifier.Operation.ADD_MULTIPLIED_BASE))));

    // Calças Magicamente Confortáveis: metade do dano de queda e +1 bloco seguro (projéteis -30% no evento)
    public static final DeferredItem<MysticWearItem> COMFY_PANTS = Mistech.ITEMS.registerItem("comfy_pants",
            p -> new MysticWearItem(p, false), p -> p.humanoidArmor(PANTS_MATERIAL, ArmorType.LEGGINGS)
                    .attributes(with(PANTS_MATERIAL, ArmorType.LEGGINGS,
                            Attributes.FALL_DAMAGE_MULTIPLIER, mod("comfy_pants_fall", -0.50, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
                            Attributes.SAFE_FALL_DISTANCE, mod("comfy_pants_safe_fall", 1.0, AttributeModifier.Operation.ADD_VALUE))));

    // Pantufas: sobe 1 bloco sem pular e anda na neve fofa
    public static final DeferredItem<MysticWearItem> SLIPPERS = Mistech.ITEMS.registerItem("slippers",
            p -> new MysticWearItem(p, true), p -> p.humanoidArmor(SLIPPERS_MATERIAL, ArmorType.BOOTS)
                    .attributes(with(SLIPPERS_MATERIAL, ArmorType.BOOTS,
                            Attributes.STEP_HEIGHT, mod("slippers_step", 0.5, AttributeModifier.Operation.ADD_VALUE))));

    private MistechWear() {}

    /** Força o carregamento da classe (os itens se registram no DeferredRegister do Mistech). */
    public static void init() {
    }
}

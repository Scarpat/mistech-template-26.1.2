package com.mistech.mistechmod.registry;

import java.util.List;
import java.util.Map;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.item.mithril.MithrilAxeItem;
import com.mistech.mistechmod.block.socket.RuneAnvilBlock;
import com.mistech.mistechmod.item.mithril.GearTier;
import com.mistech.mistechmod.item.mithril.MithrilGear;
import com.mistech.mistechmod.item.mithril.MithrilHoeItem;
import com.mistech.mistechmod.item.mithril.MithrilItem;
import com.mistech.mistechmod.item.mithril.MithrilShovelItem;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.transfer.energy.ItemAccessEnergyHandler;

/**
 * Equipamento revestido de mitril (ritual Tier I sobre o equipamento de netherite): um pouco melhor que netherite,
 * gasta energia no lugar da durabilidade (ver {@link MithrilGear}). Armadura completa: velocidade + resistência a magia.
 */
public final class MithrilContent {
    /** Ferramentas: tier netherite, velocidade 12, +6 de dano, encantabilidade 22. A durabilidade não é usada. */
    public static final ToolMaterial MITHRIL = new ToolMaterial(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 2031, 12.0F, 6.0F, 22,
            MistechTags.MITHRIL_TOOL_MATERIALS);

    public static final ResourceKey<EquipmentAsset> MITHRIL_ASSET =
            ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(Mistech.MODID, "mithril"));

    /** Armadura: 4/8/6/4, resistência 4, repulsão 0,2. */
    public static final ArmorMaterial MITHRIL_ARMOR = new ArmorMaterial(40,
            Map.of(ArmorType.BOOTS, 4, ArmorType.LEGGINGS, 6, ArmorType.CHESTPLATE, 8, ArmorType.HELMET, 4, ArmorType.BODY, 20),
            22, SoundEvents.ARMOR_EQUIP_NETHERITE, 4.0F, 0.2F, MistechTags.MITHRIL_TOOL_MATERIALS, MITHRIL_ASSET);

    public static final DeferredItem<MithrilItem> MITHRIL_SWORD = Mistech.ITEMS.registerItem("mithril_sword", MithrilItem::new,
            p -> p.sword(MITHRIL, 3.0F, -2.4F).fireResistant().rarity(Rarity.RARE));
    public static final DeferredItem<MithrilItem> MITHRIL_SPEAR = Mistech.ITEMS.registerItem("mithril_spear", MithrilItem::new,
            p -> p.spear(MITHRIL, 1.1F, 1.25F, 0.4F, 2.5F, 9.0F, 5.5F, 5.1F, 8.75F, 4.6F).fireResistant().rarity(Rarity.RARE));
    public static final DeferredItem<MithrilItem> MITHRIL_PICKAXE = Mistech.ITEMS.registerItem("mithril_pickaxe", MithrilItem::new,
            p -> p.pickaxe(MITHRIL, 1.0F, -2.8F).fireResistant().rarity(Rarity.RARE));
    public static final DeferredItem<MithrilAxeItem> MITHRIL_AXE = Mistech.ITEMS.registerItem("mithril_axe",
            p -> new MithrilAxeItem(MITHRIL, 5.0F, -3.0F, p), p -> p.fireResistant().rarity(Rarity.RARE));
    public static final DeferredItem<MithrilShovelItem> MITHRIL_SHOVEL = Mistech.ITEMS.registerItem("mithril_shovel",
            p -> new MithrilShovelItem(MITHRIL, 1.5F, -3.0F, p), p -> p.fireResistant().rarity(Rarity.RARE));
    public static final DeferredItem<MithrilHoeItem> MITHRIL_HOE = Mistech.ITEMS.registerItem("mithril_hoe",
            p -> new MithrilHoeItem(MITHRIL, -5.0F, 0.0F, p), p -> p.fireResistant().rarity(Rarity.RARE));

    public static final DeferredItem<MithrilItem> MITHRIL_HELMET = armor("mithril_helmet", ArmorType.HELMET);
    public static final DeferredItem<MithrilItem> MITHRIL_CHESTPLATE = armor("mithril_chestplate", ArmorType.CHESTPLATE);
    public static final DeferredItem<MithrilItem> MITHRIL_LEGGINGS = armor("mithril_leggings", ArmorType.LEGGINGS);
    public static final DeferredItem<MithrilItem> MITHRIL_BOOTS = armor("mithril_boots", ArmorType.BOOTS);

    private static DeferredItem<MithrilItem> armor(String name, ArmorType type) {
        return Mistech.ITEMS.registerItem(name, MithrilItem::new, p -> p.humanoidArmor(MITHRIL_ARMOR, type).fireResistant().rarity(Rarity.RARE));
    }

    // ------------------------------------------------------------------ níveis acima do mitril

    /**
     * Um conjunto completo (4 peças de armadura + 6 ferramentas) de um nível de {@link GearTier}.
     * Ferramentas sempre no nível de netherite (o que muda é velocidade, dano e encantabilidade).
     */
    public static final class GearSet {
        public final GearTier tier;
        public final ToolMaterial tool;
        public final ArmorMaterial armor;
        public final DeferredItem<MithrilItem> helmet, chestplate, leggings, boots, sword, spear, pickaxe;
        public final DeferredItem<MithrilAxeItem> axe;
        public final DeferredItem<MithrilShovelItem> shovel;
        public final DeferredItem<MithrilHoeItem> hoe;

        GearSet(GearTier tier, float speed, float damage, int enchant, Map<ArmorType, Integer> defense, float toughness, float knockback,
                Rarity rarity) {
            this.tier = tier;
            String n = tier.id();
            this.tool = new ToolMaterial(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 2031, speed, damage, enchant, MistechTags.MITHRIL_TOOL_MATERIALS);
            ResourceKey<EquipmentAsset> asset = ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(Mistech.MODID, n));
            this.armor = new ArmorMaterial(40, defense, enchant, SoundEvents.ARMOR_EQUIP_NETHERITE, toughness, knockback,
                    MistechTags.MITHRIL_TOOL_MATERIALS, asset);
            ToolMaterial t = this.tool;
            this.sword = Mistech.ITEMS.registerItem(n + "_sword", p -> new MithrilItem(tier, p),
                    p -> p.sword(t, 3.0F, -2.4F).fireResistant().rarity(rarity));
            this.spear = Mistech.ITEMS.registerItem(n + "_spear", p -> new MithrilItem(tier, p),
                    p -> p.spear(t, 1.1F, 1.25F, 0.4F, 2.5F, 9.0F, 5.5F, 5.1F, 8.75F, 4.6F).fireResistant().rarity(rarity));
            this.pickaxe = Mistech.ITEMS.registerItem(n + "_pickaxe", p -> new MithrilItem(tier, p),
                    p -> p.pickaxe(t, 1.0F, -2.8F).fireResistant().rarity(rarity));
            this.axe = Mistech.ITEMS.registerItem(n + "_axe", p -> new MithrilAxeItem(tier, t, 5.0F, -3.0F, p),
                    p -> p.fireResistant().rarity(rarity));
            this.shovel = Mistech.ITEMS.registerItem(n + "_shovel", p -> new MithrilShovelItem(tier, t, 1.5F, -3.0F, p),
                    p -> p.fireResistant().rarity(rarity));
            this.hoe = Mistech.ITEMS.registerItem(n + "_hoe", p -> new MithrilHoeItem(tier, t, -5.0F, 0.0F, p),
                    p -> p.fireResistant().rarity(rarity));
            this.helmet = armorPiece(n + "_helmet", tier, this.armor, ArmorType.HELMET, rarity);
            this.chestplate = armorPiece(n + "_chestplate", tier, this.armor, ArmorType.CHESTPLATE, rarity);
            this.leggings = armorPiece(n + "_leggings", tier, this.armor, ArmorType.LEGGINGS, rarity);
            this.boots = armorPiece(n + "_boots", tier, this.armor, ArmorType.BOOTS, rarity);
        }

        public List<DeferredItem<? extends Item>> items() {
            return List.of(this.helmet, this.chestplate, this.leggings, this.boots,
                    this.sword, this.spear, this.pickaxe, this.axe, this.shovel, this.hoe);
        }
    }

    private static DeferredItem<MithrilItem> armorPiece(String name, GearTier tier, ArmorMaterial material, ArmorType type, Rarity rarity) {
        return Mistech.ITEMS.registerItem(name, p -> new MithrilItem(tier, p), p -> p.humanoidArmor(material, type).fireResistant().rarity(rarity));
    }

    /** Adamantina (ritual Tier II sobre o mitril): 3 engastes de runa. */
    public static final GearSet ADAMANTINE = new GearSet(GearTier.ADAMANTINE, 14.0F, 8.0F, 24,
            Map.of(ArmorType.BOOTS, 4, ArmorType.LEGGINGS, 7, ArmorType.CHESTPLATE, 9, ArmorType.HELMET, 4, ArmorType.BODY, 22),
            6.0F, 0.3F, Rarity.EPIC);
    /** Oricalco (ritual Tier III sobre a adamantina): 6 engastes, constelação, reforça Sol/Lua/Ascendente. */
    public static final GearSet ORICHALCUM = new GearSet(GearTier.ORICHALCUM, 16.0F, 10.0F, 26,
            Map.of(ArmorType.BOOTS, 5, ArmorType.LEGGINGS, 8, ArmorType.CHESTPLATE, 10, ArmorType.HELMET, 5, ArmorType.BODY, 24),
            8.0F, 0.35F, Rarity.EPIC);
    /** Oricalco Puro (ritual do Santuário sobre o oricalco): 8 engastes, Sol muito mais forte, Tarô (futuro). */
    public static final GearSet PURE_ORICHALCUM = new GearSet(GearTier.PURE_ORICHALCUM, 20.0F, 12.0F, 30,
            Map.of(ArmorType.BOOTS, 5, ArmorType.LEGGINGS, 9, ArmorType.CHESTPLATE, 11, ArmorType.HELMET, 5, ArmorType.BODY, 26),
            10.0F, 0.4F, Rarity.EPIC);

    /** Bigorna Rúnica: engasta runas no equipamento. */
    public static final DeferredBlock<RuneAnvilBlock> RUNE_ANVIL = Mistech.BLOCKS.registerBlock("rune_anvil", RuneAnvilBlock::new,
            p -> p.mapColor(MapColor.COLOR_PURPLE).requiresCorrectToolForDrops().strength(5.0F, 1200.0F).sound(SoundType.ANVIL)
                    .noOcclusion().lightLevel(state -> 4).pushReaction(PushReaction.BLOCK));
    public static final DeferredItem<BlockItem> RUNE_ANVIL_ITEM = Mistech.ITEMS.registerSimpleBlockItem("rune_anvil", RUNE_ANVIL,
            p -> p.rarity(Rarity.RARE));

    private MithrilContent() {}

    public static List<DeferredItem<? extends Item>> gear() {
        List<DeferredItem<? extends Item>> list = new java.util.ArrayList<>(List.of(MITHRIL_HELMET, MITHRIL_CHESTPLATE, MITHRIL_LEGGINGS,
                MITHRIL_BOOTS, MITHRIL_SWORD, MITHRIL_SPEAR, MITHRIL_PICKAXE, MITHRIL_AXE, MITHRIL_SHOVEL, MITHRIL_HOE));
        list.addAll(ADAMANTINE.items());
        list.addAll(ORICHALCUM.items());
        list.addAll(PURE_ORICHALCUM.items());
        return list;
    }

    public static List<DeferredItem<? extends Item>> creativeItems() {
        List<DeferredItem<? extends Item>> list = new java.util.ArrayList<>(gear());
        list.add(RUNE_ANVIL_ITEM);
        return list;
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(MithrilContent::onCapabilities);
    }

    /** Energia: recebe de qualquer carregador (taxa por nível, ver {@link GearTier}), não deixa tirar. */
    private static void onCapabilities(RegisterCapabilitiesEvent event) {
        for (DeferredItem<? extends Item> item : gear()) {
            GearTier tier = ((MithrilGear.Gear) item.get()).tier();
            event.registerItem(Capabilities.Energy.ITEM,
                    (stack, access) -> new ItemAccessEnergyHandler(access, MistechComponents.GEAR_ENERGY.get(),
                            tier.capacity(), tier.chargeRate(), 0),
                    item.get());
        }
    }
}

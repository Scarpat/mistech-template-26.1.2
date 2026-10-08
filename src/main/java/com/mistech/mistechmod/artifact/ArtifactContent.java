package com.mistech.mistechmod.artifact;

import java.util.List;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.accessory.Accessory;
import com.mistech.mistechmod.item.mithril.GearTier;
import com.mistech.mistechmod.item.mithril.MithrilGear;
import com.mistech.mistechmod.item.mithril.MithrilItem;
import com.mistech.mistechmod.registry.GnomeContent;
import com.mistech.mistechmod.registry.MistechComponents;
import com.mistech.mistechmod.registry.MithrilContent;
import com.mojang.serialization.Codec;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.transfer.energy.ItemAccessEnergyHandler;

/**
 * Artefatos Rúnicos: equipamento ligado a uma runa. A base varia:
 * <ul>
 * <li><b>Mitril</b> (energia, sem engastes): Botas do Vento, Patins Gélidos, Manopla da Tempestade.</li>
 * <li><b>Adamantina</b> (3 engastes): Marreta Vulcânica, Máscara das Marés, Coroa do Farol.</li>
 * <li><b>Oricalco</b> (6 engastes): Vassoura das Nuvens, Manto das Sombras, Lança das Marés.</li>
 * <li><b>Acessórios</b> (energia própria): Ímã do Vazio, Ampulheta de Bolso, Anel Espacial.</li>
 * </ul>
 * Todos gastam energia (FE) como o equipamento revestido ({@link MithrilGear}).
 */
public final class ArtifactContent {
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Mistech.MODID);

    /** Botas do Vento melhoradas com a Runa das Nuvens (pulo triplo). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> CLOUD_STEP =
            COMPONENTS.registerComponentType("cloud_step", b -> b.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));
    /** Marreta Vulcânica: derreter os drops (padrão: ligado). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> AUTOSMELT =
            COMPONENTS.registerComponentType("autosmelt", b -> b.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));
    /** Ímã do Vazio / Ampulheta de Bolso ligados (padrão: ligado). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> ARTIFACT_ACTIVE =
            COMPONENTS.registerComponentType("artifact_active", b -> b.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));
    /** Ímã do Vazio: itens que ele apaga em vez de puxar. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ItemContainerContents>> VOID_FILTER =
            COMPONENTS.registerComponentType("void_filter", b -> b.persistent(ItemContainerContents.CODEC)
                    .networkSynchronized(ItemContainerContents.STREAM_CODEC).cacheEncoding());

    // ------------------------------------------------------------------ armaduras com visual próprio

    private static ArmorMaterial look(ArmorMaterial base, String asset) {
        ResourceKey<EquipmentAsset> key = ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(Mistech.MODID, asset));
        return new ArmorMaterial(base.durability(), base.defense(), base.enchantmentValue(), base.equipSound(), base.toughness(),
                base.knockbackResistance(), base.repairIngredient(), key);
    }

    private static DeferredItem<MithrilItem> armor(String name, GearTier tier, ArmorMaterial base, ArmorType type, Rarity rarity) {
        ArmorMaterial material = look(base, name);
        return Mistech.ITEMS.registerItem(name, p -> new MithrilItem(tier, p), p -> p.humanoidArmor(material, type).fireResistant().rarity(rarity));
    }

    // ----- mitril (energia, sem engastes)
    public static final DeferredItem<MithrilItem> WIND_BOOTS = armor("wind_boots", GearTier.MITHRIL, MithrilContent.MITHRIL_ARMOR,
            ArmorType.BOOTS, Rarity.EPIC);
    public static final DeferredItem<MithrilItem> FROST_SKATES = armor("frost_skates", GearTier.MITHRIL, MithrilContent.MITHRIL_ARMOR,
            ArmorType.BOOTS, Rarity.EPIC);
    public static final DeferredItem<MithrilItem> STORM_GAUNTLET = Mistech.ITEMS.registerItem("storm_gauntlet", p -> new MithrilItem(GearTier.MITHRIL, p),
            p -> p.sword(MithrilContent.MITHRIL, 2.0F, -2.0F).fireResistant().rarity(Rarity.EPIC));

    // ----- adamantina (3 engastes)
    public static final DeferredItem<VolcanicHammerItem> VOLCANIC_HAMMER = Mistech.ITEMS.registerItem("volcanic_hammer",
            p -> new VolcanicHammerItem(GearTier.ADAMANTINE, p),
            p -> p.pickaxe(MithrilContent.ADAMANTINE.tool, 3.0F, -3.2F).fireResistant().rarity(Rarity.EPIC));
    public static final DeferredItem<MithrilItem> TIDE_MASK = armor("tide_mask", GearTier.ADAMANTINE, MithrilContent.ADAMANTINE.armor,
            ArmorType.HELMET, Rarity.EPIC);
    public static final DeferredItem<MithrilItem> BEACON_CROWN = armor("beacon_crown", GearTier.ADAMANTINE, MithrilContent.ADAMANTINE.armor,
            ArmorType.HELMET, Rarity.EPIC);

    // ----- oricalco (6 engastes)
    /** Vassoura comum (craft): catalisador da Vassoura das Nuvens. */
    public static final DeferredItem<Item> BROOM = Mistech.ITEMS.registerSimpleItem("broom", p -> p.stacksTo(1));
    public static final DeferredItem<CloudBroomItem> CLOUD_BROOM = Mistech.ITEMS.registerItem("cloud_broom",
            p -> new CloudBroomItem(GearTier.ORICHALCUM, p), p -> p.stacksTo(1).fireResistant().rarity(Rarity.EPIC));
    public static final DeferredItem<MithrilItem> SHADOW_CLOAK = armor("shadow_cloak", GearTier.ORICHALCUM, MithrilContent.ORICHALCUM.armor,
            ArmorType.CHESTPLATE, Rarity.EPIC);
    public static final DeferredItem<TideSpearItem> TIDE_SPEAR = Mistech.ITEMS.registerItem("tide_spear", TideSpearItem::new,
            p -> p.durability(2031).attributes(TridentItem.createAttributes())
                    .component(DataComponents.TOOL, TridentItem.createToolProperties()).enchantable(24).fireResistant().rarity(Rarity.EPIC));

    // ----- acessórios
    public static final DeferredItem<ArtifactAccessoryItem> VOID_MAGNET = accessory(Accessory.VOID_MAGNET);
    public static final DeferredItem<ArtifactAccessoryItem> POCKET_HOURGLASS = accessory(Accessory.POCKET_HOURGLASS);
    public static final DeferredItem<ArtifactAccessoryItem> SPATIAL_RING = accessory(Accessory.SPATIAL_RING);

    private static DeferredItem<ArtifactAccessoryItem> accessory(Accessory accessory) {
        return Mistech.ITEMS.registerItem(accessory.id, p -> new ArtifactAccessoryItem(p, accessory), p -> p.stacksTo(1).rarity(Rarity.EPIC));
    }

    // ------------------------------------------------------------------ entidade

    public static final DeferredHolder<EntityType<?>, EntityType<BroomEntity>> BROOM_ENTITY = GnomeContent.ENTITIES.registerEntityType("cloud_broom",
            BroomEntity::new, MobCategory.MISC, b -> b.sized(1.2F, 0.4F).clientTrackingRange(10));

    private ArtifactContent() {}

    /** Itens com energia (capability + carga inicial do ritual). */
    public static List<DeferredItem<? extends Item>> energyItems() {
        return List.of(WIND_BOOTS, FROST_SKATES, STORM_GAUNTLET, VOLCANIC_HAMMER, TIDE_MASK, BEACON_CROWN, CLOUD_BROOM, SHADOW_CLOAK,
                TIDE_SPEAR, VOID_MAGNET, POCKET_HOURGLASS, SPATIAL_RING);
    }

    public static List<DeferredItem<? extends Item>> creativeItems() {
        List<DeferredItem<? extends Item>> list = new java.util.ArrayList<>(energyItems());
        list.add(6, BROOM);
        return list;
    }

    public static void register(IEventBus modEventBus) {
        COMPONENTS.register(modEventBus);
        modEventBus.addListener(ArtifactContent::onCapabilities);
    }

    /** Energia: recebe de qualquer carregador (Bateria de Bolso, baterias...), não deixa tirar. */
    private static void onCapabilities(RegisterCapabilitiesEvent event) {
        for (DeferredItem<? extends Item> item : energyItems()) {
            GearTier tier = ((MithrilGear.Gear) item.get()).tier();
            event.registerItem(Capabilities.Energy.ITEM,
                    (stack, access) -> new ItemAccessEnergyHandler(access, MistechComponents.GEAR_ENERGY.get(), tier.capacity(), tier.chargeRate(), 0),
                    item.get());
        }
    }
}

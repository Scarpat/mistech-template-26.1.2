package com.mistech.mistechmod.ranged;

import java.util.List;
import java.util.function.Supplier;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.item.mithril.GearTier;
import com.mistech.mistechmod.registry.GnomeContent;
import com.mistech.mistechmod.registry.MistechAttachments;
import com.mistech.mistechmod.registry.MistechComponents;
import com.mistech.mistechmod.registry.MistechRecipes;
import com.mistech.mistechmod.registry.MistechTags;
import com.mojang.serialization.Codec;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.transfer.energy.ItemAccessEnergyHandler;

/**
 * Armas de longe e de prata: Adaga de Prata, Arco Turvo, Arcos de Mitril → Oricalco Puro, Flechas Rúnicas e Besta Mágica.
 */
public final class RangedContent {
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Mistech.MODID);

    /** Flecha Rúnica: id do elemento ({@link com.mistech.mistechmod.item.RuneElement}). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> ARROW_RUNE =
            COMPONENTS.registerComponentType("arrow_rune", b -> b.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));
    /** Besta Mágica: as 3 runas do tambor. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ItemContainerContents>> CROSSBOW_DRUM =
            COMPONENTS.registerComponentType("crossbow_drum", b -> b.persistent(ItemContainerContents.CODEC)
                    .networkSynchronized(ItemContainerContents.STREAM_CODEC).cacheEncoding());

    /** Magia das flechas (na entidade da flecha). */
    public static final Supplier<AttachmentType<ArrowMagic>> ARROW_MAGIC = MistechAttachments.ATTACHMENT_TYPES.register("arrow_magic",
            () -> AttachmentType.builder(() -> ArrowMagic.EMPTY).serialize(ArrowMagic.MAP_CODEC).build());

    // ------------------------------------------------------------------ Adaga de Prata

    /** Prata (mitril com presa de lobisomem): leve e rápida. */
    public static final ToolMaterial SILVER = new ToolMaterial(BlockTags.INCORRECT_FOR_IRON_TOOL, 900, 7.0F, 1.0F, 18,
            MistechTags.MITHRIL_TOOL_MATERIALS);
    /** Dano 4, velocidade 2,4. */
    public static final DeferredItem<SilverDaggerItem> SILVER_DAGGER = Mistech.ITEMS.registerItem("silver_dagger", SilverDaggerItem::new,
            p -> p.sword(SILVER, 2.0F, -1.6F).rarity(Rarity.UNCOMMON));

    // ------------------------------------------------------------------ arcos

    public static final DeferredItem<MurkyBowItem> MURKY_BOW = Mistech.ITEMS.registerItem("murky_bow", MurkyBowItem::new,
            p -> p.durability(576).enchantable(1).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<GearBowItem> MITHRIL_BOW = bow("mithril_bow", GearTier.MITHRIL, Rarity.RARE);
    public static final DeferredItem<GearBowItem> ADAMANTINE_BOW = bow("adamantine_bow", GearTier.ADAMANTINE, Rarity.EPIC);
    public static final DeferredItem<GearBowItem> ORICHALCUM_BOW = bow("orichalcum_bow", GearTier.ORICHALCUM, Rarity.EPIC);
    public static final DeferredItem<GearBowItem> PURE_ORICHALCUM_BOW = bow("pure_orichalcum_bow", GearTier.PURE_ORICHALCUM, Rarity.EPIC);

    private static DeferredItem<GearBowItem> bow(String name, GearTier tier, Rarity rarity) {
        return Mistech.ITEMS.registerItem(name, p -> new GearBowItem(tier, p),
                p -> p.durability(2031).enchantable(20).fireResistant().rarity(rarity));
    }

    public static final DeferredItem<RuneArrowItem> RUNE_ARROW = Mistech.ITEMS.registerItem("rune_arrow", RuneArrowItem::new);

    // ------------------------------------------------------------------ Besta Mágica

    public static final DeferredItem<ArcaneCrossbowItem> ARCANE_CROSSBOW = Mistech.ITEMS.registerItem("arcane_crossbow", ArcaneCrossbowItem::new,
            p -> p.stacksTo(1).durability(2031).component(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY)
                    .fireResistant().rarity(Rarity.EPIC));
    /** Só para desenhar o virote (projétil e besta carregada); não aparece no criativo. */
    public static final DeferredItem<Item> ARCANE_BOLT = Mistech.ITEMS.registerSimpleItem("arcane_bolt");

    public static final DeferredHolder<EntityType<?>, EntityType<ArcaneBoltEntity>> ARCANE_BOLT_ENTITY = GnomeContent.ENTITIES.registerEntityType(
            "arcane_bolt", ArcaneBoltEntity::new, MobCategory.MISC, b -> b.sized(0.3F, 0.3F).clientTrackingRange(6).updateInterval(5));

    public static final Supplier<RecipeSerializer<RuneArrowRecipe>> RUNE_ARROW_SERIALIZER = MistechRecipes.RECIPE_SERIALIZERS.register("rune_arrow",
            () -> new RecipeSerializer<>(RuneArrowRecipe.CODEC, RuneArrowRecipe.STREAM_CODEC));

    private RangedContent() {}

    public static List<DeferredItem<? extends Item>> gearItems() {
        return List.of(MITHRIL_BOW, ADAMANTINE_BOW, ORICHALCUM_BOW, PURE_ORICHALCUM_BOW, ARCANE_CROSSBOW);
    }

    public static List<DeferredItem<? extends Item>> creativeItems() {
        return List.of(SILVER_DAGGER, MURKY_BOW, MITHRIL_BOW, ADAMANTINE_BOW, ORICHALCUM_BOW, PURE_ORICHALCUM_BOW, ARCANE_CROSSBOW);
    }

    public static void register(IEventBus modEventBus) {
        COMPONENTS.register(modEventBus);
        modEventBus.addListener(RangedContent::onCapabilities);
    }

    /** Energia: recebe de qualquer carregador, não deixa tirar (igual aos artefatos). */
    private static void onCapabilities(RegisterCapabilitiesEvent event) {
        for (DeferredItem<? extends Item> item : gearItems()) {
            GearTier tier = ((com.mistech.mistechmod.item.mithril.MithrilGear.Gear) item.get()).tier();
            event.registerItem(Capabilities.Energy.ITEM,
                    (stack, access) -> new ItemAccessEnergyHandler(access, MistechComponents.GEAR_ENERGY.get(), tier.capacity(), tier.chargeRate(), 0),
                    item.get());
        }
    }
}

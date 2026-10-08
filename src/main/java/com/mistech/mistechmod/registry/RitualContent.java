package com.mistech.mistechmod.registry;

import java.util.List;
import java.util.function.Supplier;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.affinity.AffinityData;
import com.mistech.mistechmod.block.ritual.ChalkMarkBlock;
import com.mistech.mistechmod.block.ritual.ImpureAltarBlock;
import com.mistech.mistechmod.block.ritual.ImpureAltarBlockEntity;
import com.mistech.mistechmod.block.ritual.RitualPedestalBlock;
import com.mistech.mistechmod.block.ritual.RitualPedestalBlockEntity;
import com.mistech.mistechmod.entity.BlackCatEntity;
import com.mistech.mistechmod.entity.OwlEntity;
import com.mistech.mistechmod.item.ritual.ImpureHoeItem;
import com.mistech.mistechmod.item.ritual.RitualDaggerItem;
import com.mistech.mistechmod.ritual.RitualRecipe;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Rituais Tier I: Altar Impuro (multibloco), pedestais, Marca de Giz, ferramentas impuras, Mesa de Cartas e familiares básicos. */
public final class RitualContent {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Mistech.MODID);
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, Mistech.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, Mistech.MODID);
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Mistech.MODID);

    public static final TagKey<Item> IMPURE_TOOL_MATERIALS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Mistech.MODID, "impure_tool_materials"));
    /** Material das ferramentas impuras: 1800 usos, velocidade 8,5, +3,5 de dano, conserta com Fragmento de Magia Impura. */
    public static final ToolMaterial IMPURE = new ToolMaterial(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 1800, 8.5F, 3.5F, 18, IMPURE_TOOL_MATERIALS);

    // ----- blocos -----
    public static final DeferredBlock<ImpureAltarBlock> IMPURE_ALTAR = altar("impure_altar", 1);
    public static final DeferredItem<BlockItem> IMPURE_ALTAR_ITEM = Mistech.ITEMS.registerSimpleBlockItem("impure_altar", IMPURE_ALTAR,
            p -> p.rarity(Rarity.UNCOMMON));
    /** Altares Tier II e III: blocos próprios, só nascem do Ritual de Ascensão (não têm receita). */
    public static final DeferredBlock<ImpureAltarBlock> IMPURE_ALTAR_2 = altar("impure_altar_2", 2);
    public static final DeferredItem<BlockItem> IMPURE_ALTAR_2_ITEM = Mistech.ITEMS.registerSimpleBlockItem("impure_altar_2", IMPURE_ALTAR_2,
            p -> p.rarity(Rarity.RARE));
    public static final DeferredBlock<ImpureAltarBlock> IMPURE_ALTAR_3 = altar("impure_altar_3", 3);
    public static final DeferredItem<BlockItem> IMPURE_ALTAR_3_ITEM = Mistech.ITEMS.registerSimpleBlockItem("impure_altar_3", IMPURE_ALTAR_3,
            p -> p.rarity(Rarity.EPIC));

    private static DeferredBlock<ImpureAltarBlock> altar(String name, int tier) {
        return Mistech.BLOCKS.registerBlock(name, p -> new ImpureAltarBlock(p, tier),
                p -> p.mapColor(MapColor.COLOR_BLACK).requiresCorrectToolForDrops().strength(4.0F, 1200.0F).sound(SoundType.DEEPSLATE)
                        .noOcclusion().lightLevel(state -> state.getValue(ImpureAltarBlock.RUNNING) ? 12 : (state.getValue(ImpureAltarBlock.FILLED) ? 3 : 0) + (tier - 1) * 3)
                        .pushReaction(PushReaction.BLOCK));
    }

    public static final DeferredBlock<RitualPedestalBlock> RITUAL_PEDESTAL = Mistech.BLOCKS.registerBlock("ritual_pedestal", RitualPedestalBlock::new,
            p -> p.mapColor(MapColor.COLOR_BLACK).requiresCorrectToolForDrops().strength(2.5F, 6.0F).sound(SoundType.DEEPSLATE)
                    .noOcclusion().pushReaction(PushReaction.BLOCK));
    public static final DeferredItem<BlockItem> RITUAL_PEDESTAL_ITEM = Mistech.ITEMS.registerSimpleBlockItem("ritual_pedestal", RITUAL_PEDESTAL);

    public static final DeferredBlock<ChalkMarkBlock> CHALK_MARK = Mistech.BLOCKS.registerBlock("chalk_mark", ChalkMarkBlock::new,
            p -> p.mapColor(MapColor.SNOW).noCollision().instabreak().sound(SoundType.CALCITE).noOcclusion().replaceable().noLootTable()
                    .pushReaction(PushReaction.DESTROY));

    public static final DeferredBlock<com.mistech.mistechmod.block.ritual.ImpureChalkMarkBlock> IMPURE_CHALK_MARK = Mistech.BLOCKS.registerBlock("impure_chalk_mark",
            com.mistech.mistechmod.block.ritual.ImpureChalkMarkBlock::new,
            p -> p.mapColor(MapColor.COLOR_PURPLE).noCollision().instabreak().sound(SoundType.CALCITE).noOcclusion().replaceable().noLootTable()
                    .lightLevel(state -> 2).pushReaction(PushReaction.DESTROY));

    // ----- itens -----
    /** Coração Impuro: resultado da Consagração do Coração, chave do altar Tier II. */
    public static final DeferredItem<Item> IMPURE_HEART = Mistech.ITEMS.registerSimpleItem("impure_heart", p -> p.rarity(Rarity.EPIC).stacksTo(16));

    public static final DeferredItem<RitualDaggerItem> RITUAL_DAGGER = Mistech.ITEMS.registerItem("ritual_dagger", RitualDaggerItem::new,
            p -> p.sword(IMPURE, 2.0F, -2.0F).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> IMPURE_PICKAXE = Mistech.ITEMS.registerItem("impure_pickaxe", Item::new,
            p -> p.pickaxe(IMPURE, 1.0F, -2.8F).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<AxeItem> IMPURE_AXE = Mistech.ITEMS.registerItem("impure_axe",
            p -> new AxeItem(IMPURE, 5.0F, -3.0F, p.rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<ImpureHoeItem> IMPURE_HOE = Mistech.ITEMS.registerItem("impure_hoe",
            p -> new ImpureHoeItem(IMPURE, -3.5F, 0.0F, p.rarity(Rarity.UNCOMMON)));

    // ----- familiares -----
    public static final DeferredHolder<EntityType<?>, EntityType<OwlEntity>> OWL = GnomeContent.ENTITIES.registerEntityType("owl",
            OwlEntity::new, MobCategory.CREATURE, b -> b.sized(0.5F, 0.9F).eyeHeight(0.54F).clientTrackingRange(8));
    public static final DeferredHolder<EntityType<?>, EntityType<BlackCatEntity>> BLACK_CAT = GnomeContent.ENTITIES.registerEntityType("black_cat",
            BlackCatEntity::new, MobCategory.CREATURE, b -> b.sized(0.6F, 0.7F).eyeHeight(0.35F).clientTrackingRange(8));

    // ----- block entities -----
    public static final Supplier<BlockEntityType<ImpureAltarBlockEntity>> IMPURE_ALTAR_BE = BLOCK_ENTITIES.register("impure_altar",
            () -> new BlockEntityType<>(ImpureAltarBlockEntity::new, IMPURE_ALTAR.get(), IMPURE_ALTAR_2.get(), IMPURE_ALTAR_3.get()));
    public static final Supplier<BlockEntityType<RitualPedestalBlockEntity>> RITUAL_PEDESTAL_BE = BLOCK_ENTITIES.register("ritual_pedestal",
            () -> new BlockEntityType<>(RitualPedestalBlockEntity::new, RITUAL_PEDESTAL.get()));

    // ----- receita -----
    public static final Supplier<RecipeType<RitualRecipe>> RITUAL_TYPE = RECIPE_TYPES.register("ritual",
            () -> RecipeType.simple(Identifier.fromNamespaceAndPath(Mistech.MODID, "ritual")));
    public static final Supplier<RecipeSerializer<RitualRecipe>> RITUAL_SERIALIZER = RECIPE_SERIALIZERS.register("ritual",
            () -> new RecipeSerializer<>(RitualRecipe.CODEC, RitualRecipe.STREAM_CODEC));

    // ----- afinidade elemental (por jogador; mantém ao morrer; sincroniza com o dono) -----
    public static final Supplier<AttachmentType<AffinityData>> AFFINITY = ATTACHMENTS.register("affinity",
            () -> AttachmentType.builder(() -> AffinityData.EMPTY)
                    .serialize(AffinityData.CODEC.fieldOf("points"))
                    .copyOnDeath()
                    .sync((holder, to) -> holder == to, AffinityData.STREAM_CODEC)
                    .build());

    private RitualContent() {}

    public static List<DeferredItem<? extends Item>> creativeItems() {
        return List.of(IMPURE_ALTAR_ITEM, IMPURE_ALTAR_2_ITEM, IMPURE_ALTAR_3_ITEM, RITUAL_PEDESTAL_ITEM, IMPURE_HEART,
                RITUAL_DAGGER, IMPURE_PICKAXE, IMPURE_AXE, IMPURE_HOE);
    }

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITIES.register(modEventBus);
        RECIPE_TYPES.register(modEventBus);
        RECIPE_SERIALIZERS.register(modEventBus);
        ATTACHMENTS.register(modEventBus);
        modEventBus.addListener(RitualContent::onAttributes);
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(OWL.get(), Parrot.createAttributes().build());
        event.put(BLACK_CAT.get(), Cat.createAttributes().build());
    }
}

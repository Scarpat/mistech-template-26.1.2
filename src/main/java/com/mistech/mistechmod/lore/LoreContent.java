package com.mistech.mistechmod.lore;

import java.util.List;
import java.util.function.Supplier;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.accessory.Accessory;
import com.mistech.mistechmod.accessory.AccessoryItem;
import com.mistech.mistechmod.registry.MistechBlockEntities;
import com.mistech.mistechmod.registry.GnomeContent;
import com.mistech.mistechmod.registry.MistechEffects;
import com.mojang.serialization.Codec;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Folclore novo: bioma Mata Ancestral (árvores gigantes), Matinta Pereira e Caipora (leva 1);
 * Iara, Cuca e Mapinguari vêm na leva 2.
 */
public final class LoreContent {
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, Mistech.MODID);
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Mistech.MODID);

    public static final ResourceKey<Biome> ANCESTRAL_FOREST = ResourceKey.create(Registries.BIOME,
            Identifier.fromNamespaceAndPath(Mistech.MODID, "ancestral_forest"));
    /** Biomas onde os bichos do folclore também aparecem (mais raros que na Mata Ancestral). */
    public static final TagKey<Biome> FOLKLORE_WILDS = TagKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath(Mistech.MODID, "folklore_wilds"));

    public static final DeferredHolder<Feature<?>, AncestralTreeFeature> ANCESTRAL_TREE = FEATURES.register("ancestral_tree",
            () -> new AncestralTreeFeature(NoneFeatureConfiguration.CODEC));

    // ----- arenas dos chefes (Toca da Cuca, Caverna do Mapinguari) -----
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, Mistech.MODID);
    public static final DeferredRegister<StructurePieceType> PIECE_TYPES = DeferredRegister.create(Registries.STRUCTURE_PIECE, Mistech.MODID);
    public static final DeferredHolder<StructureType<?>, StructureType<LoreLairStructure>> LAIR_TYPE =
            STRUCTURE_TYPES.register("lore_lair", () -> () -> LoreLairStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> LAIR_PIECE =
            PIECE_TYPES.register("lore_lair", () -> (StructurePieceType) LoreLairPiece::new);
    public static final DeferredBlock<LairCoreBlock> LAIR_CORE = Mistech.BLOCKS.registerBlock("lair_core", LairCoreBlock::new,
            p -> p.mapColor(MapColor.STONE).strength(-1.0F, 3600000.0F).noLootTable().noOcclusion());
    public static final Supplier<BlockEntityType<LairCoreBlockEntity>> LAIR_CORE_BE = MistechBlockEntities.BLOCK_ENTITY_TYPES
            .register("lair_core", () -> new BlockEntityType<>(LairCoreBlockEntity::new, LAIR_CORE.get()));

    // ----- criaturas -----
    public static final DeferredHolder<EntityType<?>, EntityType<MatintaEntity>> MATINTA = GnomeContent.ENTITIES.registerEntityType("matinta_pereira",
            MatintaEntity::new, MobCategory.CREATURE, b -> b.sized(0.5F, 0.7F).eyeHeight(0.5F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<CaiporaEntity>> CAIPORA = GnomeContent.ENTITIES.registerEntityType("caipora",
            CaiporaEntity::new, MobCategory.CREATURE, b -> b.sized(1.2F, 2.4F).eyeHeight(2.0F).clientTrackingRange(10));

    public static final DeferredHolder<EntityType<?>, EntityType<IaraEntity>> IARA = GnomeContent.ENTITIES.registerEntityType("iara",
            IaraEntity::new, MobCategory.MONSTER, b -> b.sized(0.7F, 1.8F).eyeHeight(1.55F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<CucaEntity>> CUCA = GnomeContent.ENTITIES.registerEntityType("cuca",
            CucaEntity::new, MobCategory.MONSTER, b -> b.sized(1.2F, 2.8F).eyeHeight(2.4F).clientTrackingRange(12));
    public static final DeferredHolder<EntityType<?>, EntityType<MapinguariEntity>> MAPINGUARI = GnomeContent.ENTITIES.registerEntityType(
            "mapinguari", MapinguariEntity::new, MobCategory.MONSTER, b -> b.sized(1.8F, 3.6F).eyeHeight(3.1F).clientTrackingRange(12));

    // ----- caldeirão da Cuca -----
    public static final DeferredBlock<CucaCauldronBlock> CUCA_CAULDRON = Mistech.BLOCKS.registerBlock("cuca_cauldron", CucaCauldronBlock::new,
            p -> p.mapColor(MapColor.COLOR_BLACK).requiresCorrectToolForDrops().strength(4.0F, 8.0F).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(s -> s.getValue(CucaCauldronBlock.FILL) > 0 ? 5 : 0));
    public static final DeferredItem<BlockItem> CUCA_CAULDRON_ITEM = Mistech.ITEMS.registerItem("cuca_cauldron",
            p -> new BlockItem(CUCA_CAULDRON.get(), p.useBlockDescriptionPrefix().rarity(Rarity.EPIC)));
    public static final Supplier<BlockEntityType<CucaCauldronBlockEntity>> CUCA_CAULDRON_BE = MistechBlockEntities.BLOCK_ENTITY_TYPES
            .register("cuca_cauldron", () -> new BlockEntityType<>(CucaCauldronBlockEntity::new, CUCA_CAULDRON.get()));

    /** Garras do Mapinguari: machado lenhador (derruba a árvore inteira). */
    public static final TagKey<Item> MAPINGUARI_REPAIR = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Mistech.MODID, "mapinguari_repair"));
    public static final ToolMaterial MAPINGUARI_MATERIAL = new ToolMaterial(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 2000, 9.0F, 4.0F, 15,
            MAPINGUARI_REPAIR);

    // ----- itens -----
    public static final DeferredItem<Item> IARA_SCALE = Mistech.ITEMS.registerSimpleItem("iara_scale", p -> p.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> MAPINGUARI_CLAW = Mistech.ITEMS.registerSimpleItem("mapinguari_claw", p -> p.rarity(Rarity.RARE));
    public static final DeferredItem<MapinguariAxeItem> MAPINGUARI_AXE = Mistech.ITEMS.registerItem("mapinguari_axe",
            p -> new MapinguariAxeItem(MAPINGUARI_MATERIAL, 5.0F, -3.1F, p.rarity(Rarity.EPIC)));
    public static final DeferredItem<AccessoryItem> IARA_RING = Mistech.ITEMS.registerItem("iara_ring",
            p -> new AccessoryItem(p, Accessory.IARA_RING), p -> p.stacksTo(1).rarity(Rarity.RARE));
    public static final DeferredItem<SpawnEggItem> IARA_SPAWN_EGG = Mistech.ITEMS.registerItem("iara_spawn_egg", SpawnEggItem::new,
            p -> p.spawnEgg(IARA.get()));
    public static final DeferredItem<SpawnEggItem> CUCA_SPAWN_EGG = Mistech.ITEMS.registerItem("cuca_spawn_egg", SpawnEggItem::new,
            p -> p.spawnEgg(CUCA.get()));
    public static final DeferredItem<SpawnEggItem> MAPINGUARI_SPAWN_EGG = Mistech.ITEMS.registerItem("mapinguari_spawn_egg", SpawnEggItem::new,
            p -> p.spawnEgg(MAPINGUARI.get()));
    public static final DeferredItem<Item> MATINTA_FEATHER = Mistech.ITEMS.registerSimpleItem("matinta_feather", p -> p.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<SpawnEggItem> MATINTA_SPAWN_EGG = Mistech.ITEMS.registerItem("matinta_pereira_spawn_egg", SpawnEggItem::new,
            p -> p.spawnEgg(MATINTA.get()));
    public static final DeferredItem<SpawnEggItem> CAIPORA_SPAWN_EGG = Mistech.ITEMS.registerItem("caipora_spawn_egg", SpawnEggItem::new,
            p -> p.spawnEgg(CAIPORA.get()));

    // ----- efeitos -----
    /** Bênção da Caça (Caipora): animais caçados soltam o dobro. */
    public static final DeferredHolder<MobEffect, MobEffect> HUNT_BLESSING = MistechEffects.MOB_EFFECTS.register("hunt_blessing",
            () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0x6AB04C) {});
    /** Maldição da Caipora: animais caçados não soltam nada. */
    public static final DeferredHolder<MobEffect, MobEffect> CAIPORA_CURSE = MistechEffects.MOB_EFFECTS.register("caipora_curse",
            () -> new MobEffect(MobEffectCategory.HARMFUL, 0x8A2A10) {});

    // ----- pecado de caçador (animais mortos; desce com o tempo) -----
    public static final Supplier<AttachmentType<Integer>> HUNT_SIN = ATTACHMENTS.register("hunt_sin",
            () -> AttachmentType.builder(() -> 0).serialize(Codec.INT.fieldOf("sin")).build());

    private LoreContent() {}

    public static Holder<MobEffect> blessing() {
        return HUNT_BLESSING;
    }

    public static List<DeferredItem<? extends Item>> creativeItems() {
        return List.of(MATINTA_FEATHER, IARA_SCALE, IARA_RING, MAPINGUARI_CLAW, MAPINGUARI_AXE, CUCA_CAULDRON_ITEM, MATINTA_SPAWN_EGG,
                CAIPORA_SPAWN_EGG, IARA_SPAWN_EGG, CUCA_SPAWN_EGG, MAPINGUARI_SPAWN_EGG);
    }

    public static void register(IEventBus modEventBus) {
        FEATURES.register(modEventBus);
        STRUCTURE_TYPES.register(modEventBus);
        PIECE_TYPES.register(modEventBus);
        ATTACHMENTS.register(modEventBus);
        modEventBus.addListener(LoreContent::onAttributes);
        modEventBus.addListener(LoreContent::onSpawnPlacements);
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(MATINTA.get(), MatintaEntity.createAttributes().build());
        event.put(CAIPORA.get(), CaiporaEntity.createAttributes().build());
        event.put(IARA.get(), IaraEntity.createAttributes().build());
        event.put(CUCA.get(), CucaEntity.createAttributes().build());
        event.put(MAPINGUARI.get(), MapinguariEntity.createAttributes().build());
    }

    private static void onSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(MATINTA.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> level.getBlockState(pos.below()).isSolid(), RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(CAIPORA.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> level.getBlockState(pos.below()).isSolid(), RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(IARA.get(), SpawnPlacementTypes.IN_WATER, Heightmap.Types.OCEAN_FLOOR, IaraEntity::checkSpawn,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(MAPINGUARI.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, MapinguariEntity::checkSpawn,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
}

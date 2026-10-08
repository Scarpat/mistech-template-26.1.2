package com.mistech.mistechmod.registry;

import java.util.List;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.entity.EvilEyeEntity;
import com.mistech.mistechmod.entity.GnomeKingEntity;
import com.mistech.mistechmod.realm.GnomeRealmBiomeSource;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Dimensão dos Gnomos: um sonho distorcido. Prado verde com árvores enormes e a Casa do Rei no centro,
 * cercado pelo Ermo Turvo (chão cinza, Cogumelos Amarelados e Árvores Turvas, sem vida - só o Mal Olhado).
 * Embaixo, cavernas e a mina dos gnomos. Sai dormindo, pedindo ao Rei ou morrendo (sem perder itens).
 */
public final class RealmContent {
    public static final ResourceKey<Level> REALM = ResourceKey.create(Registries.DIMENSION, Identifier.fromNamespaceAndPath(Mistech.MODID, "gnome_realm"));

    public static final DeferredRegister<MapCodec<? extends BiomeSource>> BIOME_SOURCES =
            DeferredRegister.create(Registries.BIOME_SOURCE, Mistech.MODID);
    public static final DeferredHolder<MapCodec<? extends BiomeSource>, MapCodec<GnomeRealmBiomeSource>> REALM_BIOME_SOURCE =
            BIOME_SOURCES.register("gnome_realm", () -> GnomeRealmBiomeSource.CODEC);

    // Grama Murcha: o chão cinzento do Ermo Turvo
    public static final DeferredBlock<Block> WITHERED_GRASS = Mistech.BLOCKS.registerSimpleBlock("withered_grass",
            p -> p.mapColor(MapColor.COLOR_GRAY).strength(0.6F).sound(SoundType.GRASS));
    public static final DeferredItem<BlockItem> WITHERED_GRASS_ITEM = Mistech.ITEMS.registerSimpleBlockItem("withered_grass", WITHERED_GRASS);

    public static final DeferredHolder<EntityType<?>, EntityType<GnomeKingEntity>> GNOME_KING = GnomeContent.ENTITIES.registerEntityType("gnome_king",
            GnomeKingEntity::new, MobCategory.MISC, b -> b.sized(0.8F, 1.4F).eyeHeight(1.1F).clientTrackingRange(10).fireImmune());
    public static final DeferredHolder<EntityType<?>, EntityType<EvilEyeEntity>> EVIL_EYE = GnomeContent.ENTITIES.registerEntityType("evil_eye",
            EvilEyeEntity::new, MobCategory.MISC, b -> b.sized(0.9F, 3.2F).eyeHeight(2.9F).clientTrackingRange(16).noSave().fireImmune());

    private RealmContent() {}

    public static List<DeferredItem<? extends Item>> creativeItems() {
        return List.of(WITHERED_GRASS_ITEM);
    }

    public static void register(IEventBus modEventBus) {
        BIOME_SOURCES.register(modEventBus);
        modEventBus.addListener(RealmContent::onAttributes);
        modEventBus.addListener(RealmContent::onSpawnPlacements);
    }

    /** Gnomos nascem sozinhos na grama (o Prado dos Gnomos tem eles na lista de criaturas). */
    private static void onSpawnPlacements(net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent event) {
        event.register(GnomeContent.GNOME.get(), net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> level.getBlockState(pos.below()).is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK),
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(GNOME_KING.get(), GnomeKingEntity.createAttributes().build());
        event.put(EVIL_EYE.get(), EvilEyeEntity.createAttributes().build());
    }
}

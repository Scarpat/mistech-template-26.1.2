package com.mistech.mistechmod.registry;

import java.util.List;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.realm.dream.mob.BogeymanEntity;
import com.mistech.mistechmod.realm.dream.mob.CorruptedGnomeEntity;
import com.mistech.mistechmod.realm.dream.mob.CountingSheepEntity;
import com.mistech.mistechmod.realm.dream.mob.GnomeMerchantEntity;
import com.mistech.mistechmod.realm.dream.mob.GnomeMinerEntity;
import com.mistech.mistechmod.realm.dream.mob.ShadeEntity;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

/** Criaturas do Sonho (anéis 1 e 2): Ovelha-de-Contar, Gnomo Mercador, Gnomo Corrompido, Bicho-Papão e Sombra. */
public final class DreamMobs {
    public static final DeferredHolder<EntityType<?>, EntityType<CountingSheepEntity>> COUNTING_SHEEP = GnomeContent.ENTITIES.registerEntityType(
            "counting_sheep", CountingSheepEntity::new, MobCategory.CREATURE, b -> b.sized(1.0F, 1.25F).eyeHeight(0.6F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<GnomeMerchantEntity>> GNOME_MERCHANT = GnomeContent.ENTITIES.registerEntityType(
            "gnome_merchant", GnomeMerchantEntity::new, MobCategory.CREATURE, b -> b.sized(0.5F, 0.9F).eyeHeight(0.7F).clientTrackingRange(8));
    public static final DeferredHolder<EntityType<?>, EntityType<GnomeMinerEntity>> GNOME_MINER = GnomeContent.ENTITIES.registerEntityType(
            "gnome_miner", GnomeMinerEntity::new, MobCategory.CREATURE, b -> b.sized(0.5F, 0.9F).eyeHeight(0.7F).clientTrackingRange(8));
    public static final DeferredHolder<EntityType<?>, EntityType<CorruptedGnomeEntity>> CORRUPTED_GNOME = GnomeContent.ENTITIES.registerEntityType(
            "corrupted_gnome", CorruptedGnomeEntity::new, MobCategory.MONSTER, b -> b.sized(0.5F, 0.9F).eyeHeight(0.7F).clientTrackingRange(8));
    public static final DeferredHolder<EntityType<?>, EntityType<BogeymanEntity>> BOGEYMAN = GnomeContent.ENTITIES.registerEntityType(
            "bogeyman", BogeymanEntity::new, MobCategory.MONSTER, b -> b.sized(0.9F, 2.2F).eyeHeight(1.8F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<ShadeEntity>> SHADE = GnomeContent.ENTITIES.registerEntityType(
            "shade", ShadeEntity::new, MobCategory.MONSTER, b -> b.sized(0.6F, 2.1F).eyeHeight(1.85F).clientTrackingRange(10));

    public static final DeferredItem<SpawnEggItem> COUNTING_SHEEP_EGG = Mistech.ITEMS.registerItem("counting_sheep_spawn_egg", SpawnEggItem::new,
            p -> p.spawnEgg(COUNTING_SHEEP.get()));
    public static final DeferredItem<SpawnEggItem> GNOME_MERCHANT_EGG = Mistech.ITEMS.registerItem("gnome_merchant_spawn_egg", SpawnEggItem::new,
            p -> p.spawnEgg(GNOME_MERCHANT.get()));
    public static final DeferredItem<SpawnEggItem> GNOME_MINER_EGG = Mistech.ITEMS.registerItem("gnome_miner_spawn_egg", SpawnEggItem::new,
            p -> p.spawnEgg(GNOME_MINER.get()));
    public static final DeferredItem<SpawnEggItem> CORRUPTED_GNOME_EGG = Mistech.ITEMS.registerItem("corrupted_gnome_spawn_egg", SpawnEggItem::new,
            p -> p.spawnEgg(CORRUPTED_GNOME.get()));
    public static final DeferredItem<SpawnEggItem> BOGEYMAN_EGG = Mistech.ITEMS.registerItem("bogeyman_spawn_egg", SpawnEggItem::new,
            p -> p.spawnEgg(BOGEYMAN.get()));
    public static final DeferredItem<SpawnEggItem> SHADE_EGG = Mistech.ITEMS.registerItem("shade_spawn_egg", SpawnEggItem::new,
            p -> p.spawnEgg(SHADE.get()));

    private DreamMobs() {}

    public static List<DeferredItem<? extends Item>> creativeItems() {
        return List.of(COUNTING_SHEEP_EGG, GNOME_MERCHANT_EGG, GNOME_MINER_EGG, CORRUPTED_GNOME_EGG, BOGEYMAN_EGG, SHADE_EGG);
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(DreamMobs::onAttributes);
        modEventBus.addListener(DreamMobs::onSpawnPlacements);
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(COUNTING_SHEEP.get(), CountingSheepEntity.createAttributes().build());
        event.put(GNOME_MERCHANT.get(), GnomeMerchantEntity.createAttributes().build());
        event.put(GNOME_MINER.get(), GnomeMinerEntity.createAttributes().build());
        event.put(CORRUPTED_GNOME.get(), CorruptedGnomeEntity.createAttributes().build());
        event.put(BOGEYMAN.get(), BogeymanEntity.createAttributes().build());
        event.put(SHADE.get(), ShadeEntity.createAttributes().build());
    }

    private static void onSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(COUNTING_SHEEP.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> level.getBlockState(pos.below()).is(BlockTags.SUPPORTS_VEGETATION),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        // minerador: nas Profundezas (no escuro, em cima de pedra)
        event.register(GNOME_MINER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> pos.getY() < 60 && level.getBlockState(pos.below()).isSolid(),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(CORRUPTED_GNOME.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkAnyLightMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(SHADE.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkAnyLightMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(BOGEYMAN.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
}

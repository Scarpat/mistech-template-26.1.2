package com.mistech.mistechmod.registry;

import java.util.List;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.realm.dream.item.AwakeningBellItem;
import com.mistech.mistechmod.realm.dream.item.LucidDreamcatcherItem;
import com.mistech.mistechmod.realm.dream.item.SpatialDreamcatcherItem;
import com.mistech.mistechmod.realm.dream.mob.CloudWhaleEntity;
import com.mistech.mistechmod.realm.dream.mob.EchoEntity;
import com.mistech.mistechmod.realm.dream.mob.MemoryEntity;
import com.mistech.mistechmod.realm.dream.mob.PrismMothEntity;
import com.mistech.mistechmod.realm.dream.mob.RemWatcherEntity;
import com.mistech.mistechmod.realm.dream.mob.SkyFishEntity;
import com.mistech.mistechmod.realm.dream.mob.TheDreamerEntity;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

/** Fase 3 do Sonho (anéis 3 e 4): criaturas, chefe, materiais e a viagem entre camadas. */
public final class DreamDeepContent {
    // ----- criaturas
    public static final DeferredHolder<EntityType<?>, EntityType<CloudWhaleEntity>> CLOUD_WHALE = GnomeContent.ENTITIES.registerEntityType(
            "cloud_whale", CloudWhaleEntity::new, MobCategory.CREATURE, b -> b.sized(4.0F, 4.2F).eyeHeight(2.6F).clientTrackingRange(16));
    public static final DeferredHolder<EntityType<?>, EntityType<SkyFishEntity>> SKY_FISH = GnomeContent.ENTITIES.registerEntityType(
            "sky_fish", SkyFishEntity::new, MobCategory.AMBIENT, b -> b.sized(0.5F, 0.4F).eyeHeight(0.2F).clientTrackingRange(8));
    public static final DeferredHolder<EntityType<?>, EntityType<PrismMothEntity>> PRISM_MOTH = GnomeContent.ENTITIES.registerEntityType(
            "prism_moth", PrismMothEntity::new, MobCategory.AMBIENT, b -> b.sized(0.6F, 0.4F).eyeHeight(0.2F).clientTrackingRange(8));
    public static final DeferredHolder<EntityType<?>, EntityType<EchoEntity>> ECHO = GnomeContent.ENTITIES.registerEntityType(
            "echo", EchoEntity::new, MobCategory.MONSTER, b -> b.sized(0.6F, 2.0F).eyeHeight(1.75F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<RemWatcherEntity>> REM_WATCHER = GnomeContent.ENTITIES.registerEntityType(
            "rem_watcher", RemWatcherEntity::new, MobCategory.MONSTER, b -> b.sized(1.0F, 1.3F).eyeHeight(0.4F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<MemoryEntity>> MEMORY = GnomeContent.ENTITIES.registerEntityType(
            "memory", MemoryEntity::new, MobCategory.AMBIENT, b -> b.sized(0.6F, 1.6F).eyeHeight(1.3F).clientTrackingRange(8));
    public static final DeferredHolder<EntityType<?>, EntityType<TheDreamerEntity>> THE_DREAMER = GnomeContent.ENTITIES.registerEntityType(
            "the_dreamer", TheDreamerEntity::new, MobCategory.MONSTER, b -> b.sized(2.5F, 5.5F).eyeHeight(4.6F).clientTrackingRange(16).fireImmune());

    // ----- materiais
    public static final DeferredItem<Item> DREAM_DUST = Mistech.ITEMS.registerSimpleItem("dream_dust");
    public static final DeferredItem<Item> DREAM_AMBER = Mistech.ITEMS.registerSimpleItem("dream_amber", p -> p.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> SKY_FISH_ITEM = Mistech.ITEMS.registerSimpleItem("sky_fish_food", p -> p.food(
            new FoodProperties.Builder().nutrition(3).saturationModifier(0.4F).build(),
            Consumables.defaultFood().onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 20 * 15, 0))).build()));
    public static final DeferredItem<Item> MEMORY_SHARD = Mistech.ITEMS.registerSimpleItem("memory_shard", p -> p.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> WATCHER_EYE = Mistech.ITEMS.registerSimpleItem("watcher_eye", p -> p.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> DREAMER_ESSENCE = Mistech.ITEMS.registerSimpleItem("dreamer_essence", p -> p.rarity(Rarity.EPIC).fireResistant());
    public static final DeferredItem<AwakeningBellItem> AWAKENING_BELL = Mistech.ITEMS.registerItem("awakening_bell", AwakeningBellItem::new,
            p -> p.rarity(Rarity.RARE).stacksTo(1));
    public static final DeferredItem<LucidDreamcatcherItem> LUCID_DREAMCATCHER = Mistech.ITEMS.registerItem("lucid_dreamcatcher", LucidDreamcatcherItem::new,
            p -> p.rarity(Rarity.RARE).durability(32));
    public static final DeferredItem<SpatialDreamcatcherItem> SPATIAL_DREAMCATCHER = Mistech.ITEMS.registerItem("spatial_dreamcatcher",
            SpatialDreamcatcherItem::new, p -> p.rarity(Rarity.RARE).durability(48));

    // ----- ovos
    public static final DeferredItem<SpawnEggItem> CLOUD_WHALE_EGG = egg("cloud_whale", CLOUD_WHALE);
    public static final DeferredItem<SpawnEggItem> SKY_FISH_EGG = egg("sky_fish", SKY_FISH);
    public static final DeferredItem<SpawnEggItem> PRISM_MOTH_EGG = egg("prism_moth", PRISM_MOTH);
    public static final DeferredItem<SpawnEggItem> ECHO_EGG = egg("echo", ECHO);
    public static final DeferredItem<SpawnEggItem> REM_WATCHER_EGG = egg("rem_watcher", REM_WATCHER);
    public static final DeferredItem<SpawnEggItem> MEMORY_EGG = egg("memory", MEMORY);
    public static final DeferredItem<SpawnEggItem> THE_DREAMER_EGG = egg("the_dreamer", THE_DREAMER);

    private DreamDeepContent() {}

    private static <T extends net.minecraft.world.entity.Mob> DeferredItem<SpawnEggItem> egg(String name, DeferredHolder<EntityType<?>, EntityType<T>> type) {
        return Mistech.ITEMS.registerItem(name + "_spawn_egg", SpawnEggItem::new, p -> p.spawnEgg(type.get()));
    }

    public static List<DeferredItem<? extends Item>> creativeItems() {
        return List.of(DREAM_DUST, DREAM_AMBER, SKY_FISH_ITEM, MEMORY_SHARD, WATCHER_EYE, DREAMER_ESSENCE, AWAKENING_BELL, LUCID_DREAMCATCHER, SPATIAL_DREAMCATCHER,
                CLOUD_WHALE_EGG, SKY_FISH_EGG, PRISM_MOTH_EGG, ECHO_EGG, REM_WATCHER_EGG, MEMORY_EGG, THE_DREAMER_EGG);
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(DreamDeepContent::onAttributes);
        modEventBus.addListener(DreamDeepContent::onSpawnPlacements);
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(CLOUD_WHALE.get(), CloudWhaleEntity.createAttributes().build());
        event.put(SKY_FISH.get(), SkyFishEntity.createAttributes().build());
        event.put(PRISM_MOTH.get(), PrismMothEntity.createAttributes().build());
        event.put(ECHO.get(), EchoEntity.createAttributes().build());
        event.put(REM_WATCHER.get(), RemWatcherEntity.createAttributes().build());
        event.put(MEMORY.get(), MemoryEntity.createAttributes().build());
        event.put(THE_DREAMER.get(), TheDreamerEntity.createAttributes().build());
    }

    private static void onSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        RegisterSpawnPlacementsEvent.Operation op = RegisterSpawnPlacementsEvent.Operation.REPLACE;
        // baleias: raras, nunca duas perto (não somem sozinhas)
        event.register(CLOUD_WHALE.get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> random.nextInt(3) == 0
                        && level.getEntitiesOfClass(CloudWhaleEntity.class, new AABB(pos).inflate(160.0)).isEmpty(), op);
        event.register(SKY_FISH.get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> pos.getY() > 60 && level.getBlockState(pos).isAir(), op);
        event.register(PRISM_MOTH.get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> pos.getY() > 60 && level.getBlockState(pos).isAir(), op);
        event.register(MEMORY.get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> pos.getY() > 40 && level.getBlockState(pos).isAir(), op);
        event.register(ECHO.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkAnyLightMonsterSpawnRules, op);
        event.register(REM_WATCHER.get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkAnyLightMonsterSpawnRules, op);
    }
}

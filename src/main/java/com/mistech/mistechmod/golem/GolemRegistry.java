package com.mistech.mistechmod.golem;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.registry.GnomeContent;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

/** Golems de trabalho: uma entidade ({@link WorkGolemEntity}, o tipo vem da estátua) e cinco estátuas. */
public final class GolemRegistry {
    public static final DeferredHolder<EntityType<?>, EntityType<WorkGolemEntity>> WORK_GOLEM = GnomeContent.ENTITIES.registerEntityType("work_golem",
            WorkGolemEntity::new, MobCategory.MISC, b -> b.sized(0.5F, 0.8F).eyeHeight(0.65F).clientTrackingRange(8));

    public static final Map<GolemKind, DeferredBlock<RunicStatueBlock>> STATUES = new EnumMap<>(GolemKind.class);
    public static final Map<GolemKind, DeferredItem<BlockItem>> STATUE_ITEMS = new EnumMap<>(GolemKind.class);

    static {
        for (GolemKind kind : GolemKind.values()) {
            String name = kind.id + "_statue";
            MapColor color = switch (kind) {
                case SORTER -> MapColor.TERRACOTTA_ORANGE;
                case COLLECTOR -> MapColor.COLOR_PURPLE;
                case MESSENGER -> MapColor.COLOR_LIGHT_BLUE;
                case CRAFTER -> MapColor.COLOR_BLACK;
                case KEEPER -> MapColor.GOLD;
            };
            DeferredBlock<RunicStatueBlock> block = Mistech.BLOCKS.registerBlock(name, p -> new RunicStatueBlock(p, kind),
                    p -> p.mapColor(color).strength(1.5F).sound(SoundType.STONE).noOcclusion());
            STATUES.put(kind, block);
            STATUE_ITEMS.put(kind, Mistech.ITEMS.registerSimpleBlockItem(name, block, p -> p.rarity(Rarity.UNCOMMON)));
        }
    }

    private GolemRegistry() {}

    public static List<DeferredItem<? extends Item>> creativeItems() {
        return new ArrayList<>(STATUE_ITEMS.values());
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(GolemRegistry::onAttributes);
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(WORK_GOLEM.get(), WorkGolemEntity.createAttributes().build());
    }
}

package com.mistech.mistechmod.registry;

import java.util.List;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.block.ClayStatueBlock;
import com.mistech.mistechmod.entity.ClayGolemEntity;
import com.mistech.mistechmod.item.GolemWandItem;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

/** Golem de Barro (transporte de itens): Estátua de Barro + Runa do Tempo, rota pela Vara de Comando. */
public final class GolemContent {
    public static final DeferredHolder<EntityType<?>, EntityType<ClayGolemEntity>> CLAY_GOLEM = GnomeContent.ENTITIES.registerEntityType("clay_golem",
            ClayGolemEntity::new, MobCategory.MISC, b -> b.sized(0.5F, 0.8F).eyeHeight(0.65F).clientTrackingRange(8));

    public static final DeferredBlock<ClayStatueBlock> CLAY_STATUE = Mistech.BLOCKS.registerBlock("clay_statue",
            ClayStatueBlock::new, p -> p.mapColor(MapColor.CLAY).strength(0.8F).sound(SoundType.MUD_BRICKS).noOcclusion());
    public static final DeferredItem<BlockItem> CLAY_STATUE_ITEM = Mistech.ITEMS.registerSimpleBlockItem("clay_statue", CLAY_STATUE);

    public static final DeferredItem<GolemWandItem> GOLEM_WAND = Mistech.ITEMS.registerItem("golem_wand", GolemWandItem::new,
            p -> p.stacksTo(1));

    /** Vara de Comando Rúnica: várias origens/destinos e modos de coleta/entrega (Vara de Comando + Runa do Espaço). */
    public static final DeferredItem<com.mistech.mistechmod.item.RunicGolemWandItem> RUNIC_GOLEM_WAND = Mistech.ITEMS.registerItem("runic_golem_wand",
            com.mistech.mistechmod.item.RunicGolemWandItem::new, p -> p.stacksTo(1).rarity(net.minecraft.world.item.Rarity.UNCOMMON));

    private GolemContent() {}

    public static List<DeferredItem<? extends Item>> creativeItems() {
        return List.of(CLAY_STATUE_ITEM, GOLEM_WAND, RUNIC_GOLEM_WAND);
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(GolemContent::onAttributes);
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(CLAY_GOLEM.get(), ClayGolemEntity.createAttributes().build());
    }
}

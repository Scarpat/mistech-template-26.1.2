package com.mistech.mistechmod.husbandry;

import java.util.List;
import java.util.function.Supplier;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.registry.MistechBlockEntities;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper;

/**
 * Criação (early game):
 * - Vaso de Cultivo: planta uma cana, flor ou cogumelo e ele vai "brotando" cópias com o tempo.
 * - Pote com Criatura: Shift + clique com um Pote de Vidro numa galinha/vaca/ovelha/lula para prender ela;
 *   o pote vai gerando ovo, leite, lã ou tinta.
 */
public final class HusbandryContent {
    /** Itens que o Vaso de Cultivo aceita (data/mistech/tags/item/pot_plants.json). */
    public static final TagKey<Item> POT_PLANTS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Mistech.MODID, "pot_plants"));

    public static final DeferredBlock<GrowingPotBlock> GROWING_POT = Mistech.BLOCKS.registerBlock("growing_pot", GrowingPotBlock::new,
            p -> p.mapColor(MapColor.TERRACOTTA_ORANGE).strength(1.0F).sound(SoundType.DECORATED_POT).noOcclusion());
    public static final DeferredItem<BlockItem> GROWING_POT_ITEM = Mistech.ITEMS.registerSimpleBlockItem("growing_pot", GROWING_POT);
    public static final Supplier<BlockEntityType<GrowingPotBlockEntity>> GROWING_POT_BE = MistechBlockEntities.BLOCK_ENTITY_TYPES.register(
            "growing_pot", () -> new BlockEntityType<>(GrowingPotBlockEntity::new, GROWING_POT.get()));

    public static final DeferredBlock<CreatureJarBlock> CREATURE_JAR = Mistech.BLOCKS.registerBlock("creature_jar", CreatureJarBlock::new,
            p -> p.mapColor(MapColor.NONE).strength(0.4F).sound(SoundType.GLASS).noOcclusion());
    public static final DeferredItem<CreatureJarItem> CREATURE_JAR_ITEM = Mistech.ITEMS.registerItem("creature_jar",
            p -> new CreatureJarItem(CREATURE_JAR.get(), p.useBlockDescriptionPrefix().stacksTo(1)));
    public static final Supplier<BlockEntityType<CreatureJarBlockEntity>> CREATURE_JAR_BE = MistechBlockEntities.BLOCK_ENTITY_TYPES.register(
            "creature_jar", () -> new BlockEntityType<>(CreatureJarBlockEntity::new, CREATURE_JAR.get()));

    private HusbandryContent() {}

    /** O Pote com Criatura não entra no criativo vazio (só existe com um bicho dentro). */
    public static List<DeferredItem<? extends Item>> creativeItems() {
        return List.of(GROWING_POT_ITEM);
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(HusbandryContent::onCapabilities);
    }

    /** Funis e canos de outros mods: entra planta/balde, sai produto. */
    private static void onCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Item.BLOCK, GROWING_POT_BE.get(), (pot, side) -> new WorldlyContainerWrapper(pot, side));
        event.registerBlockEntity(Capabilities.Item.BLOCK, CREATURE_JAR_BE.get(), (jar, side) -> new WorldlyContainerWrapper(jar, side));
    }
}

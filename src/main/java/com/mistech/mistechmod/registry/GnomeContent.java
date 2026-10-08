package com.mistech.mistechmod.registry;

import java.util.List;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.block.BottledGnomeBlock;
import com.mistech.mistechmod.block.YellowMushroomBlock;
import com.mistech.mistechmod.entity.GnomeEntity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Gnomos: Cogumelo Amarelado → Isca de Gnomo → Gnomo (fogueira) ou Gnomo Engarrafado (Pote de Vidro). */
public final class GnomeContent {
    public static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(Mistech.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<GnomeEntity>> GNOME = ENTITIES.registerEntityType("gnome",
            GnomeEntity::new, MobCategory.CREATURE, b -> b.sized(0.5F, 0.9F).eyeHeight(0.7F).clientTrackingRange(8));

    // Cogumelo Amarelado (nasce perto da Árvore Turva quando ela cresce)
    public static final DeferredBlock<YellowMushroomBlock> YELLOW_MUSHROOM = Mistech.BLOCKS.registerBlock("yellow_mushroom",
            YellowMushroomBlock::new,
            p -> p.mapColor(MapColor.COLOR_YELLOW).noCollision().instabreak().sound(SoundType.GRASS).lightLevel(state -> 2)
                    .pushReaction(PushReaction.DESTROY));
    public static final DeferredItem<BlockItem> YELLOW_MUSHROOM_ITEM = Mistech.ITEMS.registerSimpleBlockItem("yellow_mushroom", YELLOW_MUSHROOM);

    // Isca de Gnomo: cogumelo amarelado + tabaco seco + cânhamo seco
    public static final DeferredItem<Item> GNOME_BAIT = Mistech.ITEMS.registerSimpleItem("gnome_bait");

    // Cajado do Gnomo (cai do gnomo). O que ele faz ainda vai ser definido.
    public static final DeferredItem<com.mistech.mistechmod.item.staff.GnomeStaffItem> GNOME_STAFF = Mistech.ITEMS.registerItem("gnome_staff",
            com.mistech.mistechmod.item.staff.GnomeStaffItem::new, p -> p.stacksTo(1).rarity(Rarity.UNCOMMON));

    // Gnomo Engarrafado: Pote de Vidro com Isca de Gnomo, depois de um tempo
    public static final DeferredBlock<BottledGnomeBlock> BOTTLED_GNOME = Mistech.BLOCKS.registerBlock("bottled_gnome",
            BottledGnomeBlock::new,
            p -> p.mapColor(MapColor.NONE).strength(0.4F).sound(SoundType.GLASS).noOcclusion());
    public static final DeferredItem<BlockItem> BOTTLED_GNOME_ITEM = Mistech.ITEMS.registerSimpleBlockItem("bottled_gnome", BOTTLED_GNOME,
            p -> p.stacksTo(16).rarity(Rarity.UNCOMMON));

    // Bicicleta do Gnomo: um Gnomo Engarrafado pedala e gera energia (troca o antigo Gerador Rúnico)
    public static final DeferredBlock<com.mistech.mistechmod.machine.GnomeBicycleBlock> GNOME_BICYCLE = Mistech.BLOCKS.registerBlock("gnome_bicycle",
            com.mistech.mistechmod.machine.GnomeBicycleBlock::new,
            p -> p.mapColor(MapColor.COLOR_RED).strength(2.0F).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(com.mistech.mistechmod.machine.MachineBlock.LIT) ? 4 : 0));
    public static final DeferredItem<BlockItem> GNOME_BICYCLE_ITEM = Mistech.ITEMS.registerSimpleBlockItem("gnome_bicycle", GNOME_BICYCLE);

    // Ovo do Gnomo (criativo)
    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> GNOME_SPAWN_EGG = Mistech.ITEMS.registerItem("gnome_spawn_egg",
            net.minecraft.world.item.SpawnEggItem::new, p -> p.spawnEgg(GNOME.get()));

    private GnomeContent() {}

    public static List<DeferredItem<? extends Item>> creativeItems() {
        return List.of(YELLOW_MUSHROOM_ITEM, GNOME_BAIT, GNOME_STAFF, BOTTLED_GNOME_ITEM, GNOME_BICYCLE_ITEM, GNOME_SPAWN_EGG);
    }

    public static void register(IEventBus modEventBus) {
        ENTITIES.register(modEventBus);
        modEventBus.addListener(GnomeContent::onAttributes);
        modEventBus.addListener(GnomeContent::onCapabilities);
    }

    /** A bateria do cajado recebe energia de qualquer carregador (não deixa tirar) + capabilities da Bicicleta do Gnomo. */
    private static void onCapabilities(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event) {
        event.registerItem(net.neoforged.neoforge.capabilities.Capabilities.Energy.ITEM,
                (stack, access) -> new net.neoforged.neoforge.transfer.energy.ItemAccessEnergyHandler(access,
                        MistechComponents.STAFF_ENERGY.get(), com.mistech.mistechmod.item.staff.GnomeStaffItem.CAPACITY,
                        com.mistech.mistechmod.item.staff.GnomeStaffItem.MAX_CHARGE_PER_TICK, 0),
                GNOME_STAFF.get());
        // Bicicleta do Gnomo: energia para cabos/máquinas de qualquer mod (só saída) e funis nas maçãs
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.Energy.BLOCK, MistechBlockEntities.GNOME_BICYCLE.get(),
                (bicycle, side) -> bicycle.getEnergyHandler());
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK, MistechBlockEntities.GNOME_BICYCLE.get(),
                (bicycle, side) -> new net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper(bicycle, side));
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(GNOME.get(), GnomeEntity.createAttributes().build());
    }
}

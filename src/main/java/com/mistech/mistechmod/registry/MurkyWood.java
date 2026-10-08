package com.mistech.mistechmod.registry;

import java.util.List;
import java.util.Optional;

import com.mistech.mistechmod.Mistech;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.entity.vehicle.boat.ChestBoat;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.HangingSignItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SignItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CeilingHangingSignBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.ShelfBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallHangingSignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Árvore Turva: conjunto completo de madeira (igual às árvores do jogo) em preto e cinza com detalhes dourados.
 * A árvore não tem folhas — só tronco e galhos, no formato da cerejeira. A muda só aparece quebrando folhas
 * sob a Visão Mística (poção de Beladona) ou no Botany Pots.
 */
public final class MurkyWood {
    public static final String NAME = "murky";

    /** Sons e comportamento de madeira comum (portas/alçapões abrem com a mão). */
    public static final BlockSetType BLOCK_SET = BlockSetType.register(new BlockSetType(Mistech.MODID + ":" + NAME));
    /** Nome "mistech:murky" → texturas das placas em assets/mistech/textures/entity/signs/murky.png etc. */
    public static final WoodType WOOD_TYPE = WoodType.register(new WoodType(Mistech.MODID + ":" + NAME, BLOCK_SET));

    /** Árvore que cresce da muda (data/mistech/worldgen/configured_feature/murky_tree.json). */
    public static final ResourceKey<ConfiguredFeature<?, ?>> TREE_FEATURE = ResourceKey.create(Registries.CONFIGURED_FEATURE,
            Identifier.fromNamespaceAndPath(Mistech.MODID, "murky_tree"));
    public static final TreeGrower TREE_GROWER = new TreeGrower(Mistech.MODID + ":" + NAME,
            Optional.empty(), Optional.of(TREE_FEATURE), Optional.empty());

    private static final MapColor PLANK_COLOR = MapColor.COLOR_BLACK;
    private static final MapColor BARK_COLOR = MapColor.COLOR_GRAY;

    private static final DeferredRegister.Blocks BLOCKS = Mistech.BLOCKS;
    private static final DeferredRegister.Items ITEMS = Mistech.ITEMS;
    public static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(Mistech.MODID);

    // ----- Troncos -----
    public static final DeferredBlock<RotatedPillarBlock> LOG = BLOCKS.registerBlock("murky_log", RotatedPillarBlock::new,
            p -> logProperties(p, PLANK_COLOR, BARK_COLOR));
    public static final DeferredBlock<RotatedPillarBlock> WOOD = BLOCKS.registerBlock("murky_wood", RotatedPillarBlock::new,
            p -> logProperties(p, BARK_COLOR, BARK_COLOR));
    public static final DeferredBlock<RotatedPillarBlock> STRIPPED_LOG = BLOCKS.registerBlock("stripped_murky_log", RotatedPillarBlock::new,
            p -> logProperties(p, PLANK_COLOR, PLANK_COLOR));
    public static final DeferredBlock<RotatedPillarBlock> STRIPPED_WOOD = BLOCKS.registerBlock("stripped_murky_wood", RotatedPillarBlock::new,
            p -> logProperties(p, PLANK_COLOR, PLANK_COLOR));

    // ----- Tábuas e derivados -----
    public static final DeferredBlock<Block> PLANKS = BLOCKS.registerSimpleBlock("murky_planks",
            p -> p.mapColor(PLANK_COLOR).instrument(NoteBlockInstrument.BASS).strength(2.0F, 3.0F).sound(SoundType.WOOD).ignitedByLava());
    public static final DeferredBlock<StairBlock> STAIRS = BLOCKS.registerBlock("murky_stairs",
            p -> new StairBlock(PLANKS.get().defaultBlockState(), p),
            () -> BlockBehaviour.Properties.ofLegacyCopy(PLANKS.get()));
    public static final DeferredBlock<SlabBlock> SLAB = BLOCKS.registerBlock("murky_slab", SlabBlock::new,
            p -> p.mapColor(PLANK_COLOR).instrument(NoteBlockInstrument.BASS).strength(2.0F, 3.0F).sound(SoundType.WOOD).ignitedByLava());
    public static final DeferredBlock<FenceBlock> FENCE = BLOCKS.registerBlock("murky_fence", FenceBlock::new,
            p -> p.mapColor(PLANK_COLOR).instrument(NoteBlockInstrument.BASS).strength(2.0F, 3.0F).sound(SoundType.WOOD).ignitedByLava());
    public static final DeferredBlock<FenceGateBlock> FENCE_GATE = BLOCKS.registerBlock("murky_fence_gate",
            p -> new FenceGateBlock(WOOD_TYPE, p),
            p -> p.mapColor(PLANK_COLOR).forceSolidOn().instrument(NoteBlockInstrument.BASS).strength(2.0F, 3.0F).ignitedByLava());
    public static final DeferredBlock<DoorBlock> DOOR = BLOCKS.registerBlock("murky_door",
            p -> new DoorBlock(BLOCK_SET, p),
            p -> p.mapColor(PLANK_COLOR).instrument(NoteBlockInstrument.BASS).strength(3.0F).noOcclusion().ignitedByLava()
                    .pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<TrapDoorBlock> TRAPDOOR = BLOCKS.registerBlock("murky_trapdoor",
            p -> new TrapDoorBlock(BLOCK_SET, p),
            p -> p.mapColor(PLANK_COLOR).instrument(NoteBlockInstrument.BASS).strength(3.0F).noOcclusion()
                    .isValidSpawn((state, level, pos, type) -> false).ignitedByLava());
    public static final DeferredBlock<PressurePlateBlock> PRESSURE_PLATE = BLOCKS.registerBlock("murky_pressure_plate",
            p -> new PressurePlateBlock(BLOCK_SET, p),
            p -> p.mapColor(PLANK_COLOR).forceSolidOn().instrument(NoteBlockInstrument.BASS).noCollision().strength(0.5F)
                    .ignitedByLava().pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<ButtonBlock> BUTTON = BLOCKS.registerBlock("murky_button",
            p -> new ButtonBlock(BLOCK_SET, 30, p),
            p -> p.noCollision().strength(0.5F).pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<ShelfBlock> SHELF = BLOCKS.registerBlock("murky_shelf", ShelfBlock::new,
            p -> p.mapColor(PLANK_COLOR).instrument(NoteBlockInstrument.BASS).sound(SoundType.SHELF).ignitedByLava().strength(2.0F, 3.0F));

    // ----- Placas -----
    public static final DeferredBlock<StandingSignBlock> SIGN = BLOCKS.registerBlock("murky_sign",
            p -> new StandingSignBlock(WOOD_TYPE, p),
            p -> p.mapColor(PLANK_COLOR).forceSolidOn().instrument(NoteBlockInstrument.BASS).noCollision().strength(1.0F).ignitedByLava());
    public static final DeferredBlock<WallSignBlock> WALL_SIGN = BLOCKS.registerBlock("murky_wall_sign",
            p -> new WallSignBlock(WOOD_TYPE, p),
            () -> wallVariant(SIGN.get()).mapColor(PLANK_COLOR).forceSolidOn().instrument(NoteBlockInstrument.BASS)
                    .noCollision().strength(1.0F).ignitedByLava());
    public static final DeferredBlock<CeilingHangingSignBlock> HANGING_SIGN = BLOCKS.registerBlock("murky_hanging_sign",
            p -> new CeilingHangingSignBlock(WOOD_TYPE, p),
            p -> p.mapColor(PLANK_COLOR).forceSolidOn().instrument(NoteBlockInstrument.BASS).noCollision().strength(1.0F).ignitedByLava());
    public static final DeferredBlock<WallHangingSignBlock> WALL_HANGING_SIGN = BLOCKS.registerBlock("murky_wall_hanging_sign",
            p -> new WallHangingSignBlock(WOOD_TYPE, p),
            () -> wallVariant(HANGING_SIGN.get()).mapColor(PLANK_COLOR).forceSolidOn().instrument(NoteBlockInstrument.BASS)
                    .noCollision().strength(1.0F).ignitedByLava());

    // ----- Muda -----
    public static final DeferredBlock<SaplingBlock> SAPLING = BLOCKS.registerBlock("murky_sapling",
            p -> new com.mistech.mistechmod.block.MurkySaplingBlock(TREE_GROWER, p),
            p -> p.mapColor(MapColor.COLOR_BLACK).noCollision().randomTicks().instabreak().sound(SoundType.GRASS)
                    .pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<FlowerPotBlock> POTTED_SAPLING = BLOCKS.registerBlock("potted_murky_sapling",
            p -> new FlowerPotBlock(() -> (FlowerPotBlock) Blocks.FLOWER_POT, SAPLING, p),
            p -> p.instabreak().noOcclusion().pushReaction(PushReaction.DESTROY));

    // ----- Barcos -----
    public static final DeferredHolder<EntityType<?>, EntityType<Boat>> BOAT_ENTITY = ENTITIES.registerEntityType("murky_boat",
            (type, level) -> new Boat(type, level, MurkyWood::boatItem), MobCategory.MISC,
            b -> b.noLootTable().sized(1.375F, 0.5625F).eyeHeight(0.5625F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<ChestBoat>> CHEST_BOAT_ENTITY = ENTITIES.registerEntityType("murky_chest_boat",
            (type, level) -> new ChestBoat(type, level, MurkyWood::chestBoatItem), MobCategory.MISC,
            b -> b.noLootTable().sized(1.375F, 0.5625F).eyeHeight(0.5625F).clientTrackingRange(10));

    // ----- Itens -----
    public static final DeferredItem<BlockItem> LOG_ITEM = ITEMS.registerSimpleBlockItem("murky_log", LOG);
    public static final DeferredItem<BlockItem> WOOD_ITEM = ITEMS.registerSimpleBlockItem("murky_wood", WOOD);
    public static final DeferredItem<BlockItem> STRIPPED_LOG_ITEM = ITEMS.registerSimpleBlockItem("stripped_murky_log", STRIPPED_LOG);
    public static final DeferredItem<BlockItem> STRIPPED_WOOD_ITEM = ITEMS.registerSimpleBlockItem("stripped_murky_wood", STRIPPED_WOOD);
    public static final DeferredItem<BlockItem> PLANKS_ITEM = ITEMS.registerSimpleBlockItem("murky_planks", PLANKS);
    public static final DeferredItem<BlockItem> STAIRS_ITEM = ITEMS.registerSimpleBlockItem("murky_stairs", STAIRS);
    public static final DeferredItem<BlockItem> SLAB_ITEM = ITEMS.registerSimpleBlockItem("murky_slab", SLAB);
    public static final DeferredItem<BlockItem> FENCE_ITEM = ITEMS.registerSimpleBlockItem("murky_fence", FENCE);
    public static final DeferredItem<BlockItem> FENCE_GATE_ITEM = ITEMS.registerSimpleBlockItem("murky_fence_gate", FENCE_GATE);
    public static final DeferredItem<DoubleHighBlockItem> DOOR_ITEM = ITEMS.registerItem("murky_door",
            p -> new DoubleHighBlockItem(DOOR.get(), p), p -> p.useBlockDescriptionPrefix());
    public static final DeferredItem<BlockItem> TRAPDOOR_ITEM = ITEMS.registerSimpleBlockItem("murky_trapdoor", TRAPDOOR);
    public static final DeferredItem<BlockItem> PRESSURE_PLATE_ITEM = ITEMS.registerSimpleBlockItem("murky_pressure_plate", PRESSURE_PLATE);
    public static final DeferredItem<BlockItem> BUTTON_ITEM = ITEMS.registerSimpleBlockItem("murky_button", BUTTON);
    public static final DeferredItem<BlockItem> SHELF_ITEM = ITEMS.registerSimpleBlockItem("murky_shelf", SHELF);
    public static final DeferredItem<SignItem> SIGN_ITEM = ITEMS.registerItem("murky_sign",
            p -> new SignItem(SIGN.get(), WALL_SIGN.get(), p), p -> p.stacksTo(16).useBlockDescriptionPrefix());
    public static final DeferredItem<HangingSignItem> HANGING_SIGN_ITEM = ITEMS.registerItem("murky_hanging_sign",
            p -> new HangingSignItem(HANGING_SIGN.get(), WALL_HANGING_SIGN.get(), p), p -> p.stacksTo(16).useBlockDescriptionPrefix());
    public static final DeferredItem<BlockItem> SAPLING_ITEM = ITEMS.registerSimpleBlockItem("murky_sapling", SAPLING);
    public static final DeferredItem<BoatItem> BOAT = ITEMS.registerItem("murky_boat",
            p -> new BoatItem(BOAT_ENTITY.get(), p), p -> p.stacksTo(1));
    public static final DeferredItem<BoatItem> CHEST_BOAT = ITEMS.registerItem("murky_chest_boat",
            p -> new BoatItem(CHEST_BOAT_ENTITY.get(), p), p -> p.stacksTo(1));

    /** Ordem da aba criativa. */
    public static List<DeferredItem<? extends Item>> creativeItems() {
        return List.of(SAPLING_ITEM, LOG_ITEM, WOOD_ITEM, STRIPPED_LOG_ITEM, STRIPPED_WOOD_ITEM, PLANKS_ITEM, STAIRS_ITEM, SLAB_ITEM,
                FENCE_ITEM, FENCE_GATE_ITEM, DOOR_ITEM, TRAPDOOR_ITEM, PRESSURE_PLATE_ITEM, BUTTON_ITEM, SHELF_ITEM,
                SIGN_ITEM, HANGING_SIGN_ITEM, BOAT, CHEST_BOAT);
    }

    private MurkyWood() {}

    private static Item boatItem() {
        return BOAT.get();
    }

    private static Item chestBoatItem() {
        return CHEST_BOAT.get();
    }

    private static BlockBehaviour.Properties logProperties(BlockBehaviour.Properties p, MapColor top, MapColor side) {
        return p.mapColor(state -> state.getValue(RotatedPillarBlock.AXIS) == net.minecraft.core.Direction.Axis.Y ? top : side)
                .instrument(NoteBlockInstrument.BASS).strength(2.0F).sound(SoundType.WOOD).ignitedByLava();
    }

    /** Placa de parede: mesma loot table e mesmo nome da placa em pé. */
    private static BlockBehaviour.Properties wallVariant(Block standing) {
        return BlockBehaviour.Properties.of().overrideLootTable(standing.getLootTable()).overrideDescription(standing.getDescriptionId());
    }

    public static void register(IEventBus modEventBus) {
        ENTITIES.register(modEventBus);
        modEventBus.addListener(MurkyWood::onAddBlockEntityBlocks);
    }

    /** Placas e prateleira usam os block entities do próprio jogo. */
    private static void onAddBlockEntityBlocks(BlockEntityTypeAddBlocksEvent event) {
        event.modify(BlockEntityType.SIGN, SIGN.get(), WALL_SIGN.get());
        event.modify(BlockEntityType.HANGING_SIGN, HANGING_SIGN.get(), WALL_HANGING_SIGN.get());
        event.modify(BlockEntityType.SHELF, SHELF.get());
    }

    /** Chamado no FMLCommonSetupEvent (enqueueWork): muda no vaso de flor. */
    public static void setup() {
        ((FlowerPotBlock) Blocks.FLOWER_POT).addPlant(SAPLING.getId(), POTTED_SAPLING);
    }
}

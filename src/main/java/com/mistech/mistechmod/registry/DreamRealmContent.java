package com.mistech.mistechmod.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.realm.DreamMaskFunction;
import com.mistech.mistechmod.realm.DreamPillowFunction;
import com.mistech.mistechmod.realm.dream.BlackThornBlock;
import com.mistech.mistechmod.realm.dream.BouncyCloudBlock;
import com.mistech.mistechmod.realm.dream.DreamFeatures;
import com.mistech.mistechmod.realm.dream.DreamFlowerBlock;
import com.mistech.mistechmod.realm.dream.DreamPlantBlock;
import com.mistech.mistechmod.realm.dream.DreamSeaBlock;
import com.mistech.mistechmod.realm.dream.EyeFlowerBlock;
import com.mistech.mistechmod.realm.dream.FractalTreeFeature;
import com.mistech.mistechmod.realm.dream.HangingVineBlock;
import com.mistech.mistechmod.realm.dream.PuffballBlock;
import com.mistech.mistechmod.realm.dream.SoftCloudBlock;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.UntintedParticleLeavesBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import java.util.Optional;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Blocos, plantas e geração do Sonho em anéis (Dimensão dos Gnomos) — ver {@link com.mistech.mistechmod.realm.DreamLayout}.
 */
public final class DreamRealmContent {
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, Mistech.MODID);
    public static final DeferredRegister<MapCodec<? extends DensityFunction>> DENSITY_TYPES =
            DeferredRegister.create(Registries.DENSITY_FUNCTION_TYPE, Mistech.MODID);

    private static final List<DeferredItem<? extends Item>> ITEMS = new ArrayList<>();

    /** Plantas em cruz: sem colisão, quebram na hora. */
    private static UnaryOperator<BlockBehaviour.Properties> plant(MapColor color, int light) {
        return p -> p.mapColor(color).noCollision().instabreak().sound(SoundType.GRASS).offsetType(BlockBehaviour.OffsetType.XZ)
                .pushReaction(PushReaction.DESTROY).lightLevel(s -> light);
    }

    // ----- Sonho Comum
    public static final DeferredBlock<DreamFlowerBlock> DREAM_POPPY = block("dream_poppy",
            p -> new DreamFlowerBlock(MobEffects.SLOWNESS, 8.0F, p, null, 0), plant(MapColor.COLOR_PURPLE, 0));
    public static final DeferredBlock<DreamFlowerBlock> DREAMFLOWER = block("dreamflower",
            p -> new DreamFlowerBlock(MobEffects.NIGHT_VISION, 8.0F, p, ParticleTypes.END_ROD, 30), plant(MapColor.COLOR_LIGHT_BLUE, 9));
    public static final DeferredBlock<PuffballBlock> PUFFBALL = block("puffball", PuffballBlock::new, plant(MapColor.SNOW, 0));
    public static final DeferredBlock<DreamPlantBlock> CLOUD_COTTON = block("cloud_cotton",
            p -> new DreamPlantBlock(p, 12.0, ParticleTypes.WHITE_ASH, 20), plant(MapColor.SNOW, 0));
    // ----- Pesadelo
    public static final DeferredBlock<BlackThornBlock> BLACK_THORN = block("black_thorn", BlackThornBlock::new,
            p -> p.mapColor(MapColor.COLOR_BLACK).noCollision().strength(0.2F).sound(SoundType.SWEET_BERRY_BUSH).pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<EyeFlowerBlock> EYE_FLOWER = block("eye_flower", EyeFlowerBlock::new, plant(MapColor.COLOR_RED, 2));
    public static final DeferredBlock<DreamPlantBlock> SLEEPLESS_IVY = block("sleepless_ivy",
            p -> new DreamPlantBlock(p, 13.0, null, 1), plant(MapColor.COLOR_GRAY, 0));
    // ----- Sonho Profundo
    public static final DeferredBlock<DreamFlowerBlock> KALEIDO_FLOWER = block("kaleido_flower",
            p -> new DreamFlowerBlock(MobEffects.NAUSEA, 6.0F, p, ParticleTypes.GLOW, 18), plant(MapColor.COLOR_MAGENTA, 6));
    public static final DeferredBlock<DreamPlantBlock> PRISM_MUSHROOM = block("prism_mushroom",
            p -> new DreamPlantBlock(p, 9.0, ParticleTypes.ENCHANT, 10), plant(MapColor.COLOR_PINK, 7));
    public static final DeferredBlock<HangingVineBlock> SPIRAL_VINE = block("spiral_vine", HangingVineBlock::new,
            p -> p.mapColor(MapColor.COLOR_CYAN).noCollision().instabreak().sound(SoundType.CAVE_VINES).pushReaction(PushReaction.DESTROY)
                    .lightLevel(s -> 5));
    // ----- Sono REM
    public static final DeferredBlock<DreamFlowerBlock> MEMORY_LILY = block("memory_lily",
            p -> new DreamFlowerBlock(MobEffects.REGENERATION, 6.0F, p, ParticleTypes.WAX_OFF, 25), plant(MapColor.QUARTZ, 8));

    // ----- chão e blocos
    public static final DeferredBlock<Block> SWEET_MYCELIUM = block("sweet_mycelium", Block::new,
            p -> p.mapColor(MapColor.COLOR_PINK).strength(0.6F).sound(SoundType.GRASS));
    public static final DeferredBlock<SoftCloudBlock> CLOUD_WOOL = block("cloud_wool", SoftCloudBlock::new,
            p -> p.mapColor(MapColor.SNOW).strength(0.8F).sound(SoundType.WOOL).instrument(NoteBlockInstrument.GUITAR));
    public static final DeferredBlock<Block> PRISMATIC_GRASS = block("prismatic_grass", Block::new,
            p -> p.mapColor(MapColor.COLOR_MAGENTA).strength(0.6F).sound(SoundType.GRASS).lightLevel(s -> 3));
    public static final DeferredBlock<Block> STAR_STONE = block("star_stone", Block::new,
            p -> p.mapColor(MapColor.COLOR_BLUE).requiresCorrectToolForDrops().strength(1.5F, 6.0F).sound(SoundType.DEEPSLATE));
    public static final DeferredBlock<Block> STAR_MOSS = block("star_moss", Block::new,
            p -> p.mapColor(MapColor.COLOR_BLUE).strength(0.6F).sound(SoundType.MOSS).lightLevel(s -> 4));
    public static final DeferredBlock<DreamSeaBlock> DENSE_CLOUD = block("dense_cloud", DreamSeaBlock::new,
            p -> p.mapColor(MapColor.SNOW).strength(0.3F).sound(SoundType.WOOL).isSuffocating((s, l, pos) -> false)
                    .isViewBlocking((s, l, pos) -> false).dynamicShape().noOcclusion()
                    .isRedstoneConductor((s, l, pos) -> false).isValidSpawn((s, l, pos, t) -> false));
    public static final DeferredBlock<DreamSeaBlock> STAR_MIST = block("star_mist", DreamSeaBlock::new,
            p -> p.mapColor(MapColor.COLOR_BLACK).strength(0.3F).sound(SoundType.WOOL).lightLevel(s -> 2).isSuffocating((s, l, pos) -> false)
                    .isViewBlocking((s, l, pos) -> false).dynamicShape().noOcclusion()
                    .isRedstoneConductor((s, l, pos) -> false).isValidSpawn((s, l, pos, t) -> false));
    public static final DeferredBlock<BouncyCloudBlock> BOUNCY_CLOUD = block("bouncy_cloud", BouncyCloudBlock::new,
            p -> p.mapColor(MapColor.COLOR_LIGHT_BLUE).strength(0.4F).sound(SoundType.WOOL).lightLevel(s -> 6));
    public static final DeferredBlock<HugeMushroomBlock> PINK_DREAM_CAP = block("pink_dream_mushroom_block", HugeMushroomBlock::new,
            p -> p.mapColor(MapColor.COLOR_PINK).strength(0.2F).sound(SoundType.WOOD).lightLevel(s -> 4));
    public static final DeferredBlock<HugeMushroomBlock> CYAN_DREAM_CAP = block("cyan_dream_mushroom_block", HugeMushroomBlock::new,
            p -> p.mapColor(MapColor.COLOR_CYAN).strength(0.2F).sound(SoundType.WOOD).lightLevel(s -> 4));

    // ----- Árvore Fractal (Jardim Fractal)
    public static final ResourceKey<ConfiguredFeature<?, ?>> FRACTAL_TREE_KEY = ResourceKey.create(Registries.CONFIGURED_FEATURE,
            Identifier.fromNamespaceAndPath(Mistech.MODID, "fractal_tree"));
    public static final TreeGrower FRACTAL_GROWER = new TreeGrower(Mistech.MODID + ":fractal", Optional.empty(), Optional.of(FRACTAL_TREE_KEY),
            Optional.empty());
    public static final DeferredBlock<RotatedPillarBlock> FRACTAL_LOG = block("fractal_log", RotatedPillarBlock::new,
            p -> p.mapColor(MapColor.QUARTZ).strength(2.0F).sound(SoundType.CHERRY_WOOD).ignitedByLava());
    public static final DeferredBlock<RotatedPillarBlock> FRACTAL_WOOD = block("fractal_wood", RotatedPillarBlock::new,
            p -> p.mapColor(MapColor.QUARTZ).strength(2.0F).sound(SoundType.CHERRY_WOOD).ignitedByLava());
    public static final DeferredBlock<Block> FRACTAL_PLANKS = block("fractal_planks", Block::new,
            p -> p.mapColor(MapColor.COLOR_LIGHT_GRAY).strength(2.0F, 3.0F).sound(SoundType.CHERRY_WOOD).ignitedByLava());
    public static final DeferredBlock<UntintedParticleLeavesBlock> FRACTAL_LEAVES = block("fractal_leaves",
            p -> new UntintedParticleLeavesBlock(0.02F, ParticleTypes.GLOW, p),
            p -> p.mapColor(MapColor.COLOR_MAGENTA).strength(0.2F).randomTicks().sound(SoundType.AZALEA_LEAVES).noOcclusion()
                    .isValidSpawn((s, l, pos, t) -> false).isSuffocating((s, l, pos) -> false).isViewBlocking((s, l, pos) -> false)
                    .ignitedByLava().pushReaction(PushReaction.DESTROY).isRedstoneConductor((s, l, pos) -> false).lightLevel(s -> 5));
    public static final DeferredBlock<SaplingBlock> FRACTAL_SAPLING = block("fractal_sapling", p -> new SaplingBlock(FRACTAL_GROWER, p),
            p -> p.mapColor(MapColor.COLOR_MAGENTA).noCollision().randomTicks().instabreak().sound(SoundType.CHERRY_SAPLING)
                    .pushReaction(PushReaction.DESTROY));

    // ----- geração
    public static final DeferredHolder<Feature<?>, DreamFeatures.HandTree> HAND_TREE = FEATURES.register("hand_tree", DreamFeatures.HandTree::new);
    public static final DeferredHolder<Feature<?>, FractalTreeFeature> FRACTAL_TREE = FEATURES.register("fractal_tree", FractalTreeFeature::new);
    public static final DeferredHolder<Feature<?>, DreamFeatures.InvertedTree> INVERTED_TREE = FEATURES.register("inverted_tree",
            DreamFeatures.InvertedTree::new);
    public static final DeferredHolder<Feature<?>, DreamFeatures.HangingVines> HANGING_VINES = FEATURES.register("hanging_vines",
            DreamFeatures.HangingVines::new);
    public static final DeferredHolder<Feature<?>, DreamFeatures.BouncyPad> BOUNCY_PAD = FEATURES.register("bouncy_pad", DreamFeatures.BouncyPad::new);
    public static final DeferredHolder<Feature<?>, DreamFeatures.LampPost> LAMP_POST = FEATURES.register("lamp_post", DreamFeatures.LampPost::new);

    public static final DeferredHolder<MapCodec<? extends DensityFunction>, MapCodec<DreamMaskFunction>> DREAM_MASK =
            DENSITY_TYPES.register("dream_mask", () -> DreamMaskFunction.MAP_CODEC);
    public static final DeferredHolder<MapCodec<? extends DensityFunction>, MapCodec<DreamPillowFunction>> DREAM_PILLOW =
            DENSITY_TYPES.register("dream_pillow", () -> DreamPillowFunction.MAP_CODEC);

    private DreamRealmContent() {}

    private static <B extends Block> DeferredBlock<B> block(String name, Function<BlockBehaviour.Properties, B> factory,
            UnaryOperator<BlockBehaviour.Properties> properties) {
        DeferredBlock<B> block = Mistech.BLOCKS.registerBlock(name, factory, properties);
        DeferredItem<BlockItem> item = Mistech.ITEMS.registerSimpleBlockItem(name, block);
        ITEMS.add(item);
        return block;
    }

    public static List<DeferredItem<? extends Item>> creativeItems() {
        return ITEMS;
    }

    public static void register(IEventBus modEventBus) {
        FEATURES.register(modEventBus);
        DENSITY_TYPES.register(modEventBus);
    }
}

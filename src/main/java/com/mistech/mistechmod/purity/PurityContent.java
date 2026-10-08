package com.mistech.mistechmod.purity;

import java.util.List;
import java.util.function.Supplier;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.item.ritual.ImpureHoeItem;
import com.mistech.mistechmod.purity.block.CrystallizerBlock;
import com.mistech.mistechmod.purity.block.CrystallizerBlockEntity;
import com.mistech.mistechmod.purity.block.DistillerBlock;
import com.mistech.mistechmod.purity.block.DistillerBlockEntity;
import com.mistech.mistechmod.purity.block.EvaporationTrayBlock;
import com.mistech.mistechmod.purity.block.PureGeneratorBlock;
import com.mistech.mistechmod.purity.block.PureGeneratorBlockEntity;
import com.mistech.mistechmod.purity.block.PurifiedEssenceBlock;
import com.mistech.mistechmod.purity.block.RueCropBlock;
import com.mistech.mistechmod.purity.block.SaltBathBlock;
import com.mistech.mistechmod.purity.block.SanctuaryFontBlock;
import com.mistech.mistechmod.purity.block.SanctuaryFontBlockEntity;
import com.mistech.mistechmod.purity.block.TaintedSoilBlock;
import com.mistech.mistechmod.purity.item.DowsingPendulumItem;
import com.mistech.mistechmod.purity.item.PureToolEvents;
import com.mistech.mistechmod.purity.item.PurifiedEssenceBottleItem;
import com.mistech.mistechmod.purity.item.RueCigarItem;
import com.mistech.mistechmod.purity.item.RueIncenseItem;
import com.mistech.mistechmod.purity.item.TaintedResidueItem;
import com.mistech.mistechmod.registry.MistechFluids;
import com.mojang.serialization.Codec;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper;

/**
 * Magia Purificada (mid game): Mácula, Pêndulo, Arruda, Sal Grosso, Destilador, Essência Purificada, Borra Impura,
 * Solo Maculado, Cristalizador, Magia Pura, Gerador Purificado, Santuário e ferramentas puras.
 */
public final class PurityContent {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Mistech.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Mistech.MODID);
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Mistech.MODID);

    // ----- tags -----
    /** Itens que maculam quem os carrega. */
    public static final TagKey<Item> TAINTING = TagKey.create(Registries.ITEM, id("tainting"));
    /** Ferramentas que maculam a cada uso. */
    public static final TagKey<Item> TAINTING_TOOLS = TagKey.create(Registries.ITEM, id("tainting_tools"));
    /** Blocos impuros que atrapalham o Santuário. */
    public static final TagKey<Block> IMPURE_BLOCKS = TagKey.create(Registries.BLOCK, id("impure_blocks"));
    public static final TagKey<Item> PURE_TOOL_MATERIALS = TagKey.create(Registries.ITEM, id("pure_tool_materials"));

    // ----- dados do jogador -----
    /** Mácula (0–100). Só no servidor; salva e mantida ao morrer. */
    public static final Supplier<AttachmentType<Float>> TAINT = ATTACHMENTS.register("taint",
            () -> AttachmentType.builder(() -> 0.0F)
                    .serialize(Codec.FLOAT.fieldOf("taint"))
                    .copyOnDeath()
                    .build());
    /** Cópia da Mácula em décimos para o cliente do próprio jogador (HUD do Pêndulo). Não é salva. */
    public static final Supplier<AttachmentType<Integer>> TAINT_CLIENT = ATTACHMENTS.register("taint_client",
            () -> AttachmentType.builder(() -> 0)
                    .sync((holder, to) -> holder == to, ByteBufCodecs.VAR_INT)
                    .build());
    /** Dia (do mundo) do último Banho de Sal. */
    public static final Supplier<AttachmentType<Long>> LAST_SALT_BATH = ATTACHMENTS.register("last_salt_bath",
            () -> AttachmentType.builder(() -> -1L)
                    .serialize(Codec.LONG.fieldOf("day"))
                    .copyOnDeath()
                    .build());

    // ----- componentes -----
    /** Runa selada no Santuário: não gasta usos, mas só funciona para quem não está Corrompido. */
    public static final DeferredHolder<net.minecraft.core.component.DataComponentType<?>, net.minecraft.core.component.DataComponentType<Boolean>> SEALED =
            COMPONENTS.registerComponentType("sealed", b -> b.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

    // ----- Pêndulo -----
    public static final DeferredItem<DowsingPendulumItem> DOWSING_PENDULUM = Mistech.ITEMS.registerItem("dowsing_pendulum",
            DowsingPendulumItem::new, p -> p.stacksTo(1));

    // ----- Arruda -----
    public static final DeferredBlock<RueCropBlock> RUE = Mistech.BLOCKS.registerBlock("rue", RueCropBlock::new,
            p -> p.mapColor(MapColor.PLANT).noCollision().randomTicks().instabreak().sound(SoundType.CROP).pushReaction(PushReaction.DESTROY));
    public static final DeferredItem<BlockItem> RUE_SEEDS = Mistech.ITEMS.registerItem("rue_seeds",
            p -> new BlockItem(RUE.get(), p.useItemDescriptionPrefix()));
    public static final DeferredItem<Item> RUE_LEAF = Mistech.ITEMS.registerSimpleItem("rue_leaf");
    public static final DeferredItem<RueIncenseItem> RUE_INCENSE = Mistech.ITEMS.registerItem("rue_incense", RueIncenseItem::new);
    public static final DeferredItem<RueCigarItem> RUE_CIGAR = Mistech.ITEMS.registerItem("rue_cigar", RueCigarItem::new);

    // ----- Sal -----
    public static final DeferredItem<Item> COARSE_SALT = Mistech.ITEMS.registerSimpleItem("coarse_salt");
    public static final DeferredBlock<EvaporationTrayBlock> EVAPORATION_TRAY = Mistech.BLOCKS.registerBlock("evaporation_tray",
            EvaporationTrayBlock::new, p -> p.mapColor(MapColor.STONE).strength(1.5F).sound(SoundType.STONE).noOcclusion().randomTicks());
    public static final DeferredItem<BlockItem> EVAPORATION_TRAY_ITEM = Mistech.ITEMS.registerSimpleBlockItem("evaporation_tray", EVAPORATION_TRAY);
    /** Banho de Sal: caldeirão com água + Sal Grosso. Quebrado, devolve o caldeirão. */
    public static final DeferredBlock<SaltBathBlock> SALT_BATH = Mistech.BLOCKS.registerBlock("salt_bath", SaltBathBlock::new,
            p -> p.mapColor(MapColor.STONE).requiresCorrectToolForDrops().strength(2.0F).noOcclusion());

    // ----- Essência Purificada -----
    public static final DeferredBlock<PurifiedEssenceBlock> PURIFIED_ESSENCE_BLOCK = Mistech.BLOCKS.registerBlock("purified_essence",
            p -> new PurifiedEssenceBlock(MistechFluids.PURIFIED_ESSENCE.get(), p),
            p -> p.mapColor(MapColor.GOLD).replaceable().noCollision().strength(100.0F)
                    .pushReaction(PushReaction.DESTROY).noLootTable().liquid().sound(SoundType.EMPTY).lightLevel(state -> 10));
    public static final DeferredItem<BucketItem> PURIFIED_ESSENCE_BUCKET = Mistech.ITEMS.registerItem("purified_essence_bucket",
            p -> new BucketItem(MistechFluids.PURIFIED_ESSENCE.get(), p.craftRemainder(Items.BUCKET).stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<PurifiedEssenceBottleItem> PURIFIED_ESSENCE_BOTTLE = Mistech.ITEMS.registerItem("purified_essence_bottle",
            PurifiedEssenceBottleItem::new, p -> p.stacksTo(16).craftRemainder(Items.GLASS_BOTTLE).rarity(Rarity.UNCOMMON));

    // ----- Destilador -----
    public static final DeferredItem<Item> MITHRIL_FILTER = Mistech.ITEMS.registerSimpleItem("mithril_filter", p -> p.durability(64));
    public static final DeferredItem<TaintedResidueItem> TAINTED_RESIDUE = Mistech.ITEMS.registerItem("tainted_residue", TaintedResidueItem::new);
    public static final DeferredBlock<DistillerBlock> DISTILLER = Mistech.BLOCKS.registerBlock("distiller", DistillerBlock::new,
            p -> p.mapColor(MapColor.COLOR_ORANGE).requiresCorrectToolForDrops().strength(3.5F).sound(SoundType.COPPER).noOcclusion()
                    .lightLevel(state -> state.getValue(DistillerBlock.LIT) ? 8 : 2));
    public static final DeferredItem<BlockItem> DISTILLER_ITEM = Mistech.ITEMS.registerSimpleBlockItem("distiller", DISTILLER);

    // ----- Solo Maculado -----
    public static final DeferredBlock<TaintedSoilBlock> TAINTED_SOIL = Mistech.BLOCKS.registerBlock("tainted_soil", TaintedSoilBlock::new,
            p -> p.mapColor(MapColor.COLOR_PURPLE).strength(0.6F).sound(SoundType.GRAVEL).randomTicks());
    public static final DeferredItem<BlockItem> TAINTED_SOIL_ITEM = Mistech.ITEMS.registerSimpleBlockItem("tainted_soil", TAINTED_SOIL);

    // ----- Cristalizador + Magia Pura -----
    public static final DeferredBlock<CrystallizerBlock> CRYSTALLIZER = Mistech.BLOCKS.registerBlock("crystallizer", CrystallizerBlock::new,
            p -> p.mapColor(MapColor.QUARTZ).requiresCorrectToolForDrops().strength(2.5F).sound(SoundType.CALCITE).noOcclusion());
    public static final DeferredItem<BlockItem> CRYSTALLIZER_ITEM = Mistech.ITEMS.registerSimpleBlockItem("crystallizer", CRYSTALLIZER);
    public static final DeferredItem<Item> PURE_MAGIC_SHARD = Mistech.ITEMS.registerSimpleItem("pure_magic_shard", p -> p.rarity(Rarity.UNCOMMON));
    public static final DeferredBlock<Block> PURE_MAGIC_BLOCK = Mistech.BLOCKS.registerSimpleBlock("pure_magic_block",
            p -> p.mapColor(MapColor.GOLD).instrument(net.minecraft.world.level.block.state.properties.NoteBlockInstrument.CHIME)
                    .requiresCorrectToolForDrops().strength(5.0F, 6.0F).sound(SoundType.AMETHYST).lightLevel(state -> 9));
    public static final DeferredItem<BlockItem> PURE_MAGIC_BLOCK_ITEM = Mistech.ITEMS.registerSimpleBlockItem("pure_magic_block", PURE_MAGIC_BLOCK,
            p -> p.rarity(Rarity.UNCOMMON));

    // ----- Gerador Purificado -----
    public static final DeferredBlock<PureGeneratorBlock> PURE_GENERATOR = Mistech.BLOCKS.registerBlock("pure_generator", PureGeneratorBlock::new,
            p -> p.mapColor(MapColor.QUARTZ).requiresCorrectToolForDrops().strength(3.5F).sound(SoundType.AMETHYST).noOcclusion()
                    .lightLevel(state -> state.getValue(PureGeneratorBlock.LIT) ? 12 : 3));
    public static final DeferredItem<BlockItem> PURE_GENERATOR_ITEM = Mistech.ITEMS.registerSimpleBlockItem("pure_generator", PURE_GENERATOR);

    // ----- Santuário -----
    public static final DeferredBlock<SanctuaryFontBlock> SANCTUARY_FONT = Mistech.BLOCKS.registerBlock("sanctuary_font", SanctuaryFontBlock::new,
            p -> p.mapColor(MapColor.QUARTZ).requiresCorrectToolForDrops().strength(4.0F, 1200.0F).sound(SoundType.CALCITE).noOcclusion()
                    .lightLevel(state -> state.getValue(SanctuaryFontBlock.RUNNING) ? 15 : (state.getValue(SanctuaryFontBlock.FILLED) ? 8 : 3))
                    .pushReaction(PushReaction.BLOCK));
    public static final DeferredItem<BlockItem> SANCTUARY_FONT_ITEM = Mistech.ITEMS.registerSimpleBlockItem("sanctuary_font", SANCTUARY_FONT,
            p -> p.rarity(Rarity.RARE));

    // ----- ferramentas puras -----
    /** Material puro: 2400 usos, velocidade 9,5, +4 de dano, encantabilidade 22. Conserta com Fragmento de Magia Pura. */
    public static final ToolMaterial PURE = new ToolMaterial(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 2400, 9.5F, 4.0F, 22, PURE_TOOL_MATERIALS);
    public static final DeferredItem<Item> PURE_PICKAXE = Mistech.ITEMS.registerItem("pure_pickaxe", Item::new,
            p -> p.pickaxe(PURE, 1.0F, -2.8F).rarity(Rarity.RARE));
    public static final DeferredItem<AxeItem> PURE_AXE = Mistech.ITEMS.registerItem("pure_axe",
            p -> new AxeItem(PURE, 5.0F, -3.0F, p.rarity(Rarity.RARE)));
    public static final DeferredItem<ImpureHoeItem> PURE_HOE = Mistech.ITEMS.registerItem("pure_hoe",
            p -> new ImpureHoeItem(PURE, -4.0F, 0.0F, p.rarity(Rarity.RARE)));

    // ----- block entities -----
    public static final Supplier<BlockEntityType<DistillerBlockEntity>> DISTILLER_BE = BLOCK_ENTITIES.register("distiller",
            () -> new BlockEntityType<>(DistillerBlockEntity::new, DISTILLER.get()));
    public static final Supplier<BlockEntityType<PureGeneratorBlockEntity>> PURE_GENERATOR_BE = BLOCK_ENTITIES.register("pure_generator",
            () -> new BlockEntityType<>(PureGeneratorBlockEntity::new, PURE_GENERATOR.get()));
    public static final Supplier<BlockEntityType<CrystallizerBlockEntity>> CRYSTALLIZER_BE = BLOCK_ENTITIES.register("crystallizer",
            () -> new BlockEntityType<>(CrystallizerBlockEntity::new, CRYSTALLIZER.get()));
    public static final Supplier<BlockEntityType<SanctuaryFontBlockEntity>> SANCTUARY_FONT_BE = BLOCK_ENTITIES.register("sanctuary_font",
            () -> new BlockEntityType<>(SanctuaryFontBlockEntity::new, SANCTUARY_FONT.get()));

    private PurityContent() {}

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Mistech.MODID, path);
    }

    public static List<DeferredItem<? extends Item>> creativeItems() {
        return List.of(DOWSING_PENDULUM, RUE_SEEDS, RUE_LEAF, RUE_INCENSE, RUE_CIGAR, EVAPORATION_TRAY_ITEM, COARSE_SALT,
                MITHRIL_FILTER, DISTILLER_ITEM, PURIFIED_ESSENCE_BUCKET, PURIFIED_ESSENCE_BOTTLE, TAINTED_RESIDUE, TAINTED_SOIL_ITEM,
                CRYSTALLIZER_ITEM, PURE_MAGIC_SHARD, PURE_MAGIC_BLOCK_ITEM, PURE_GENERATOR_ITEM, SANCTUARY_FONT_ITEM,
                PURE_PICKAXE, PURE_AXE, PURE_HOE);
    }

    public static void register(IEventBus modEventBus) {
        ATTACHMENTS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        COMPONENTS.register(modEventBus);
        modEventBus.addListener(PurityContent::onCapabilities);
        PureToolEvents.init();
    }

    private static void onCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Energy.BLOCK, DISTILLER_BE.get(), (be, side) -> be.getEnergyHandler());
        event.registerBlockEntity(Capabilities.Item.BLOCK, DISTILLER_BE.get(), (be, side) -> new WorldlyContainerWrapper(be, side));
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, DISTILLER_BE.get(), (be, side) -> be.getTanks());
        event.registerBlockEntity(Capabilities.Energy.BLOCK, PURE_GENERATOR_BE.get(), (be, side) -> be.getEnergyHandler());
        event.registerBlockEntity(Capabilities.Item.BLOCK, PURE_GENERATOR_BE.get(), (be, side) -> new WorldlyContainerWrapper(be, side));
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, PURE_GENERATOR_BE.get(), (be, side) -> be.getTank());
    }
}

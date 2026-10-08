package com.mistech.mistechmod;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import com.mistech.mistechmod.block.AstrologyTableBlock;
import com.mistech.mistechmod.block.BelladonnaCropBlock;
import com.mistech.mistechmod.registry.MistechAttachments;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.TooltipDisplay;
import com.mistech.mistechmod.block.EssenceExtractorBlock;
import com.mistech.mistechmod.block.MagicCompressorBlock;
import com.mistech.mistechmod.block.arcane.ArcaneNodeBlock;
import com.mistech.mistechmod.block.arcane.ArcaneReceiverBlock;
import com.mistech.mistechmod.fluid.ImpureEssenceBlock;
import com.mistech.mistechmod.registry.MistechFluids;
import com.mistech.mistechmod.block.BottledLightningBlock;
import com.mistech.mistechmod.block.CondenserBlock;
import com.mistech.mistechmod.block.GlassJarBlock;
import com.mistech.mistechmod.block.HempCropBlock;
import com.mistech.mistechmod.block.IncenseHolderBlock;
import com.mistech.mistechmod.block.RunicAshtrayBlock;
import com.mistech.mistechmod.block.RunicFurnaceBlock;
import com.mistech.mistechmod.block.TobaccoCropBlock;
import com.mistech.mistechmod.block.WritingTableBlock;
import com.mistech.mistechmod.block.RunicTankBlock;
import com.mistech.mistechmod.block.SpatialBarrelBlock;
import com.mistech.mistechmod.block.confector.RuneConfectorBlock;
import com.mistech.mistechmod.item.BackpackItem;
import com.mistech.mistechmod.item.StorageBlockItem;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;
import com.mistech.mistechmod.item.GuideBookItem;
import com.mistech.mistechmod.item.IncenseItem;
import com.mistech.mistechmod.item.CigarItem;
import com.mistech.mistechmod.item.JointItem;
import com.mistech.mistechmod.item.rune.AirRuneItem;
import com.mistech.mistechmod.item.rune.DarknessRuneItem;
import com.mistech.mistechmod.item.rune.LightRuneItem;
import com.mistech.mistechmod.item.rune.SpaceRuneItem;
import com.mistech.mistechmod.item.rune.TimeRuneItem;
import com.mistech.mistechmod.item.rune.EarthRuneItem;
import com.mistech.mistechmod.item.rune.EnergyRuneItem;
import com.mistech.mistechmod.item.rune.FireRuneItem;
import com.mistech.mistechmod.item.rune.IceRuneItem;
import com.mistech.mistechmod.item.rune.WaterRuneItem;
import com.mistech.mistechmod.item.rune.MagmaRuneItem;
import com.mistech.mistechmod.item.rune.VaporRuneItem;
import com.mistech.mistechmod.item.rune.VoidRuneItem;
import com.mistech.mistechmod.registry.MistechBlockEntities;
import com.mistech.mistechmod.registry.MistechMenus;
import com.mistech.mistechmod.registry.MistechRecipes;
import com.mistech.mistechmod.registry.MistechComponents;
import com.mistech.mistechmod.registry.MistechEffects;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.ColorRGBA;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ColoredFallingBlock;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(Mistech.MODID)
public class Mistech {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "mistech";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();
    // Create a Deferred Register to hold Blocks which will all be registered under the "mistech" namespace
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    // Create a Deferred Register to hold Items which will all be registered under the "mistech" namespace
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the "mistech" namespace
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    // Creates a new Block with the id "mistech:example_block", combining the namespace and path
    public static final DeferredBlock<Block> EXAMPLE_BLOCK = BLOCKS.registerSimpleBlock("example_block", p -> p.mapColor(MapColor.STONE));
    // Creates a new BlockItem with the id "mistech:example_block", combining the namespace and path
    public static final DeferredItem<BlockItem> EXAMPLE_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("example_block", EXAMPLE_BLOCK);

    // Creates a new food item with the id "mistech:example_id", nutrition 1 and saturation 2
    public static final DeferredItem<Item> EXAMPLE_ITEM = ITEMS.registerSimpleItem("example_item", p -> p.food(new FoodProperties.Builder()
            .alwaysEdible().nutrition(1).saturationModifier(2f).build()));

    // Incenso: acende com isqueiro na outra mão, queima por 2 minutos e afasta a atenção de zumbis/esqueletos
    public static final DeferredItem<IncenseItem> INCENSE = ITEMS.registerItem("incense", IncenseItem::new);

    // Cinzas: sobram (25% de chance) quando um incenso termina no Porta-Incenso
    public static final DeferredItem<Item> INCENSE_ASH = ITEMS.registerSimpleItem("incense_ash");

    // Giz de Cinzas: argila + garrafa de água + cinzas de incenso
    // Giz é ferramenta: 1 por slot, 100 de durabilidade (gasta na Mesa de Escrita)
    public static final DeferredItem<Item> ASH_CHALK = ITEMS.registerSimpleItem("ash_chalk", p -> p.durability(100));

    // Incenso Mágico: ponta de Fragmento de Magia Impura; 4x o tempo (480 s), 4x o alcance,
    // gera 1 Resíduo Mágico a cada 30 s queimando
    public static final DeferredItem<IncenseItem> MAGIC_INCENSE = ITEMS.registerItem("magic_incense",
            p -> new IncenseItem(p, IncenseItem.BURN_SECONDS * 4, 4, 30));

    // Resíduo Mágico: cinza preta deixada pelo Incenso Mágico
    public static final DeferredItem<Item> MAGIC_RESIDUE = ITEMS.registerSimpleItem("magic_residue");

    // Giz Impuro: argila + garrafa de água + Resíduo Mágico; 800 de durabilidade
    public static final DeferredItem<Item> IMPURE_CHALK = ITEMS.registerSimpleItem("impure_chalk", p -> p.durability(800));

    // Bloco de Cinzas: cai como areia/cascalho; 4 cinzas <-> 1 bloco
    public static final DeferredBlock<ColoredFallingBlock> ASH_BLOCK = BLOCKS.registerBlock("ash_block",
            p -> new ColoredFallingBlock(new ColorRGBA(0xFF8F8C88), p),
            p -> p.mapColor(MapColor.COLOR_LIGHT_GRAY).strength(0.5F).sound(SoundType.SAND));
    public static final DeferredItem<BlockItem> ASH_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("ash_block", ASH_BLOCK);

    // Mesa de Escrita: escreve runas (a cor do pano vem do tapete da receita)
    public static final DeferredBlock<WritingTableBlock> WRITING_TABLE = BLOCKS.registerBlock("writing_table",
            WritingTableBlock::new,
            p -> p.mapColor(MapColor.WOOD).strength(2.0F).sound(SoundType.WOOD).noOcclusion());
    public static final DeferredItem<BlockItem> WRITING_TABLE_ITEM = ITEMS.registerSimpleBlockItem("writing_table", WRITING_TABLE);

    // Runas (escritas na Mesa de Escrita) — 16 usos cada
    public static final int RUNE_USES = 16;
    public static final DeferredItem<FireRuneItem> FIRE_RUNE = ITEMS.registerItem("fire_rune", FireRuneItem::new, p -> p.durability(RUNE_USES));
    public static final DeferredItem<WaterRuneItem> WATER_RUNE = ITEMS.registerItem("water_rune", WaterRuneItem::new, p -> p.durability(RUNE_USES));
    public static final DeferredItem<EarthRuneItem> EARTH_RUNE = ITEMS.registerItem("earth_rune", EarthRuneItem::new, p -> p.durability(RUNE_USES));
    public static final DeferredItem<AirRuneItem> AIR_RUNE = ITEMS.registerItem("air_rune", AirRuneItem::new, p -> p.durability(RUNE_USES));
    public static final DeferredItem<IceRuneItem> ICE_RUNE = ITEMS.registerItem("ice_rune", IceRuneItem::new, p -> p.durability(RUNE_USES));
    public static final DeferredItem<EnergyRuneItem> ENERGY_RUNE = ITEMS.registerItem("energy_rune", EnergyRuneItem::new, p -> p.durability(RUNE_USES));
    public static final DeferredItem<LightRuneItem> LIGHT_RUNE = ITEMS.registerItem("light_rune", LightRuneItem::new, p -> p.durability(RUNE_USES));
    public static final DeferredItem<DarknessRuneItem> DARKNESS_RUNE = ITEMS.registerItem("darkness_rune", DarknessRuneItem::new, p -> p.durability(RUNE_USES));
    public static final DeferredItem<SpaceRuneItem> SPACE_RUNE = ITEMS.registerItem("space_rune", SpaceRuneItem::new, p -> p.durability(RUNE_USES));
    public static final DeferredItem<TimeRuneItem> TIME_RUNE = ITEMS.registerItem("time_rune", TimeRuneItem::new, p -> p.durability(RUNE_USES));
    // Runas avançadas (combinam duas runas básicas)
    public static final DeferredItem<MagmaRuneItem> MAGMA_RUNE = ITEMS.registerItem("magma_rune", MagmaRuneItem::new, p -> p.durability(RUNE_USES));
    public static final DeferredItem<VaporRuneItem> VAPOR_RUNE = ITEMS.registerItem("vapor_rune", VaporRuneItem::new, p -> p.durability(RUNE_USES));
    public static final DeferredItem<VoidRuneItem> VOID_RUNE = ITEMS.registerItem("void_rune", VoidRuneItem::new, p -> p.durability(RUNE_USES));
    // runas complexas (ritual Tier I, catalisador = runa básica)
    public static final DeferredItem<com.mistech.mistechmod.item.rune.LuckRuneItem> LUCK_RUNE = ITEMS.registerItem("luck_rune",
            com.mistech.mistechmod.item.rune.LuckRuneItem::new, p -> p.durability(RUNE_USES).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<com.mistech.mistechmod.item.rune.ArcaneRuneItem> ARCANE_RUNE = ITEMS.registerItem("arcane_rune",
            com.mistech.mistechmod.item.rune.ArcaneRuneItem::new, p -> p.durability(RUNE_USES).rarity(Rarity.UNCOMMON));

    // Pote de Vidro: guarda 1 pack de um item e mostra ele dentro
    public static final DeferredBlock<GlassJarBlock> GLASS_JAR = BLOCKS.registerBlock("glass_jar", GlassJarBlock::new,
            p -> p.mapColor(MapColor.NONE).strength(0.4F).sound(SoundType.GLASS).noOcclusion());
    public static final DeferredItem<BlockItem> GLASS_JAR_ITEM = ITEMS.registerSimpleBlockItem("glass_jar", GLASS_JAR);

    // Raio Engarrafado: um raio caindo perto de um Pote de Vidro vazio
    public static final DeferredBlock<BottledLightningBlock> BOTTLED_LIGHTNING = BLOCKS.registerBlock("bottled_lightning", BottledLightningBlock::new,
            p -> p.mapColor(MapColor.COLOR_YELLOW).strength(0.4F).sound(SoundType.GLASS).noOcclusion().lightLevel(state -> 12));
    public static final DeferredItem<BlockItem> BOTTLED_LIGHTNING_ITEM = ITEMS.registerSimpleBlockItem("bottled_lightning", BOTTLED_LIGHTNING);

    // Condensador: energia + água/lava encostada -> gelo, gelo compacto, gelo azul, obsidiana
    public static final DeferredBlock<CondenserBlock> CONDENSER = BLOCKS.registerBlock("condenser", CondenserBlock::new,
            p -> p.mapColor(MapColor.ICE).requiresCorrectToolForDrops().strength(3.5F).sound(SoundType.METAL)
                    .lightLevel(state -> state.getValue(CondenserBlock.LIT) ? 6 : 0));
    public static final DeferredItem<BlockItem> CONDENSER_ITEM = ITEMS.registerSimpleBlockItem("condenser", CONDENSER);

    // Rede Arcana: Ponto (transmite a energia do bloco onde está preso) e Receptor (entrega no bloco onde está preso)
    public static final DeferredBlock<ArcaneNodeBlock> ARCANE_NODE = BLOCKS.registerBlock("arcane_node", ArcaneNodeBlock::new,
            p -> p.mapColor(MapColor.EMERALD).strength(0.5F).sound(SoundType.AMETHYST).noOcclusion().lightLevel(state -> 5));
    public static final DeferredItem<BlockItem> ARCANE_NODE_ITEM = ITEMS.registerSimpleBlockItem("arcane_node", ARCANE_NODE);
    public static final DeferredBlock<ArcaneReceiverBlock> ARCANE_RECEIVER = BLOCKS.registerBlock("arcane_receiver", ArcaneReceiverBlock::new,
            p -> p.mapColor(MapColor.COLOR_PURPLE).strength(0.5F).sound(SoundType.AMETHYST).noOcclusion().lightLevel(state -> 5));
    public static final DeferredItem<BlockItem> ARCANE_RECEIVER_ITEM = ITEMS.registerSimpleBlockItem("arcane_receiver", ARCANE_RECEIVER);

    // Essência Mágica Impura (fluido): bloco líquido, balde e frasco
    public static final DeferredBlock<ImpureEssenceBlock> IMPURE_ESSENCE_BLOCK = BLOCKS.registerBlock("impure_essence",
            p -> new ImpureEssenceBlock(MistechFluids.IMPURE_ESSENCE.get(), p),
            p -> p.mapColor(MapColor.COLOR_PURPLE).replaceable().noCollision().strength(100.0F)
                    .pushReaction(PushReaction.DESTROY).noLootTable().liquid().sound(SoundType.EMPTY).lightLevel(state -> 2));
    public static final DeferredItem<BucketItem> IMPURE_ESSENCE_BUCKET = ITEMS.registerItem("impure_essence_bucket",
            p -> new BucketItem(MistechFluids.IMPURE_ESSENCE.get(), p.craftRemainder(Items.BUCKET).stacksTo(1)));
    public static final DeferredItem<Item> IMPURE_ESSENCE_BOTTLE = ITEMS.registerSimpleItem("impure_essence_bottle",
            p -> p.stacksTo(16).craftRemainder(Items.GLASS_BOTTLE));

    // Extrator de Essência Mágica: caldeirão que enche de essência com energia
    public static final DeferredBlock<EssenceExtractorBlock> ESSENCE_EXTRACTOR = BLOCKS.registerBlock("essence_extractor",
            EssenceExtractorBlock::new,
            p -> p.mapColor(MapColor.COLOR_BLACK).requiresCorrectToolForDrops().strength(3.0F, 1200.0F).sound(SoundType.STONE)
                    .noOcclusion().lightLevel(state -> Math.min(15, (state.getValue(EssenceExtractorBlock.LIT) ? 7 : 0)
                            + state.getValue(EssenceExtractorBlock.LEVEL) * 2)));
    public static final DeferredItem<BlockItem> ESSENCE_EXTRACTOR_ITEM = ITEMS.registerSimpleBlockItem("essence_extractor", ESSENCE_EXTRACTOR);

    // Compressor de Magia + Fragmento de Magia Impura (material para craftings avançados)
    public static final DeferredBlock<MagicCompressorBlock> MAGIC_COMPRESSOR = BLOCKS.registerBlock("magic_compressor",
            MagicCompressorBlock::new,
            p -> p.mapColor(MapColor.COLOR_BLACK).requiresCorrectToolForDrops().strength(2.5F).sound(SoundType.POLISHED_DEEPSLATE)
                    .noOcclusion());
    public static final DeferredItem<BlockItem> MAGIC_COMPRESSOR_ITEM = ITEMS.registerSimpleBlockItem("magic_compressor", MAGIC_COMPRESSOR);
    public static final DeferredItem<Item> IMPURE_MAGIC_SHARD = ITEMS.registerSimpleItem("impure_magic_shard");

    // Bloco de Magia Impura: bloco sólido (como diamante/esmeralda), escuro, pulsando roxo <-> laranja
    // Receita: Fragmento + Resíduo / Resíduo + Fragmento (2x2)
    public static final DeferredBlock<Block> IMPURE_MAGIC_BLOCK = BLOCKS.registerSimpleBlock("impure_magic_block",
            p -> p.mapColor(MapColor.COLOR_PURPLE).instrument(net.minecraft.world.level.block.state.properties.NoteBlockInstrument.BIT)
                    .requiresCorrectToolForDrops().strength(5.0F, 6.0F).sound(SoundType.AMETHYST).lightLevel(state -> 3));
    // Pilar de Conexão Astral: multibloco Tronco Turvo + Lápis + Bloco de Magia Impura (Pedra Guia); 3 blocos de altura
    public static final DeferredBlock<com.mistech.mistechmod.block.AstralPillarBlock> ASTRAL_PILLAR = BLOCKS.registerBlock("astral_pillar",
            com.mistech.mistechmod.block.AstralPillarBlock::new,
            p -> p.mapColor(MapColor.COLOR_BLUE).requiresCorrectToolForDrops().strength(3.0F, 6.0F).sound(SoundType.AMETHYST)
                    .noOcclusion().lightLevel(state -> state.getValue(com.mistech.mistechmod.block.AstralPillarBlock.PART)
                            == com.mistech.mistechmod.block.AstralPillarBlock.Part.TOP ? 7 : 0)
                    .pushReaction(PushReaction.BLOCK));
    public static final DeferredItem<BlockItem> ASTRAL_PILLAR_ITEM = ITEMS.registerSimpleBlockItem("astral_pillar", ASTRAL_PILLAR,
            p -> p.rarity(Rarity.RARE));
    public static final DeferredItem<BlockItem> IMPURE_MAGIC_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("impure_magic_block", IMPURE_MAGIC_BLOCK,
            p -> p.rarity(Rarity.UNCOMMON));


    // Grimório Mistech: livro de tutorial (vem na primeira entrada no mundo; livro + ametista)
    public static final DeferredItem<GuideBookItem> GUIDE_BOOK = ITEMS.registerItem("guide_book", GuideBookItem::new,
            p -> p.stacksTo(1).rarity(Rarity.UNCOMMON));

    // Pedra Guia: chave de rituais e multiblocos. A cor vem do corante da receita (componente dyed_color)
    public static final DeferredItem<Item> GUIDE_STONE = ITEMS.registerSimpleItem("guide_stone", p -> p.stacksTo(1)
            .rarity(Rarity.RARE)
            .component(DataComponents.DYED_COLOR, new DyedItemColor(0x9B5FD6))
            .component(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT.withHidden(DataComponents.DYED_COLOR, true)));

    // Mesa de Astrologia: Mesa de Escrita + Pedra Guia sobre o Campo de Astrologia (multibloco 7x7 de lápis e ouro)
    public static final DeferredBlock<AstrologyTableBlock> ASTROLOGY_TABLE = BLOCKS.registerBlock("astrology_table",
            AstrologyTableBlock::new,
            p -> p.mapColor(MapColor.COLOR_BLUE).strength(2.5F).sound(SoundType.WOOD).noOcclusion().lightLevel(state -> 6));
    public static final DeferredItem<BlockItem> ASTROLOGY_TABLE_ITEM = ITEMS.registerSimpleBlockItem("astrology_table", ASTROLOGY_TABLE);

    // Fornalha Rúnica: sem combustível, 25% mais rápida que a comum
    public static final DeferredBlock<RunicFurnaceBlock> RUNIC_FURNACE = BLOCKS.registerBlock("runic_furnace",
            RunicFurnaceBlock::new,
            p -> p.mapColor(MapColor.STONE).requiresCorrectToolForDrops().strength(3.5F).sound(SoundType.STONE)
                    .lightLevel(state -> state.getValue(RunicFurnaceBlock.LIT) ? 13 : 0));
    public static final DeferredItem<BlockItem> RUNIC_FURNACE_ITEM = ITEMS.registerSimpleBlockItem("runic_furnace", RUNIC_FURNACE);

    // Plantações (plantam em terra arada e crescem como o trigo)
    public static final DeferredBlock<TobaccoCropBlock> TOBACCO = BLOCKS.registerBlock("tobacco", TobaccoCropBlock::new, Mistech::cropProperties);
    public static final DeferredBlock<HempCropBlock> HEMP = BLOCKS.registerBlock("hemp", HempCropBlock::new, Mistech::cropProperties);
    public static final DeferredBlock<com.mistech.mistechmod.block.CoffeeCropBlock> COFFEE =
            BLOCKS.registerBlock("coffee", com.mistech.mistechmod.block.CoffeeCropBlock::new, Mistech::cropProperties);
    public static final DeferredBlock<BelladonnaCropBlock> BELLADONNA = BLOCKS.registerBlock("belladonna", BelladonnaCropBlock::new, Mistech::cropProperties);

    public static final DeferredItem<BlockItem> TOBACCO_SEEDS = ITEMS.registerItem("tobacco_seeds",
            p -> new BlockItem(TOBACCO.get(), p.useItemDescriptionPrefix()));
    public static final DeferredItem<BlockItem> HEMP_SEEDS = ITEMS.registerItem("hemp_seeds",
            p -> new BlockItem(HEMP.get(), p.useItemDescriptionPrefix()));
    public static final DeferredItem<BlockItem> BELLADONNA_SEEDS = ITEMS.registerItem("belladonna_seeds",
            p -> new BlockItem(BELLADONNA.get(), p.useItemDescriptionPrefix()));

    public static final DeferredItem<Item> TOBACCO_LEAF = ITEMS.registerSimpleItem("tobacco_leaf");

    // Café: semente (planta), fruta (colhida), grão torrado (fornalha), pó (moedor) -> água + pó na Chaleira
    public static final DeferredItem<BlockItem> COFFEE_SEEDS = ITEMS.registerItem("coffee_seeds",
            p -> new BlockItem(COFFEE.get(), p.useItemDescriptionPrefix()));
    public static final DeferredItem<Item> COFFEE_CHERRY = ITEMS.registerSimpleItem("coffee_cherry");
    public static final DeferredItem<Item> ROASTED_COFFEE_BEANS = ITEMS.registerSimpleItem("roasted_coffee_beans");
    public static final DeferredItem<Item> COFFEE_POWDER = ITEMS.registerSimpleItem("coffee_powder");
    public static final DeferredItem<com.mistech.mistechmod.item.GrinderItem> GRINDER = ITEMS.registerItem("grinder",
            com.mistech.mistechmod.item.GrinderItem::new, p -> p.durability(64));
    // Chaleira: esquenta sozinha (Runa de Fogo na receita). Água + Pó de Café = café; água + Bagas de Beladona = Visão Mística
    public static final DeferredBlock<com.mistech.mistechmod.block.KettleBlock> KETTLE = BLOCKS.registerBlock("kettle",
            com.mistech.mistechmod.block.KettleBlock::new,
            p -> p.mapColor(MapColor.COLOR_ORANGE).strength(1.5F).sound(SoundType.COPPER).noOcclusion()
                    .lightLevel(state -> 2));
    public static final DeferredItem<BlockItem> KETTLE_ITEM = ITEMS.registerSimpleBlockItem("kettle", KETTLE);
    // Xícara: 3 argilas em forma de pote = Xícara Crua; assada na fornalha = Xícara. Cabe meia garrafa (a Chaleira enche 6)
    public static final DeferredItem<Item> RAW_CUP = ITEMS.registerSimpleItem("raw_cup");
    public static final DeferredItem<Item> CUP = ITEMS.registerSimpleItem("cup");
    // Xícara de Café: Pressa + Velocidade por 1:30 (metade da antiga garrafa)
    public static final DeferredItem<Item> COFFEE_CUP = ITEMS.registerSimpleItem("coffee_cup", p -> p.stacksTo(16)
            .usingConvertsTo(CUP.get())
            .component(DataComponents.CONSUMABLE, net.minecraft.world.item.component.Consumables.defaultDrink()
                    .onConsume(new net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect(java.util.List.of(
                            new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.HASTE, 1800, 0),
                            new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SPEED, 1800, 0))))
                    .build()));
    // Chá de Beladona: Visão Mística por 1:30
    public static final DeferredItem<Item> BELLADONNA_TEA_CUP = ITEMS.registerSimpleItem("belladonna_tea_cup", p -> p.stacksTo(16)
            .usingConvertsTo(CUP.get())
            .component(DataComponents.CONSUMABLE, net.minecraft.world.item.component.Consumables.defaultDrink()
                    .onConsume(new net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect(
                            new net.minecraft.world.effect.MobEffectInstance(MistechEffects.MYSTIC_SIGHT, 1800, 0)))
                    .build()));
    public static final DeferredItem<Item> HEMP_FIBER = ITEMS.registerSimpleItem("hemp_fiber");
    public static final DeferredItem<Item> BELLADONNA_BERRIES = ITEMS.registerSimpleItem("belladonna_berries");
    public static final DeferredItem<Item> HEMP_LEAF = ITEMS.registerSimpleItem("hemp_leaf");

    // Folhas secas: fornalha, defumador ou fogueira
    public static final DeferredItem<Item> DRIED_TOBACCO_LEAF = ITEMS.registerSimpleItem("dried_tobacco_leaf");
    public static final DeferredItem<Item> DRIED_HEMP_LEAF = ITEMS.registerSimpleItem("dried_hemp_leaf");

    // Fumáveis: acendem como o incenso; segurar botão direito dá uma tragada
    public static final DeferredItem<CigarItem> CIGAR = ITEMS.registerItem("cigar", CigarItem::new);
    public static final DeferredItem<JointItem> JOINT = ITEMS.registerItem("joint", JointItem::new);
    /** Cachimbo: carrega folhas secas de tabaco ou cânhamo (substitui charuto/baseado). */
    public static final DeferredItem<com.mistech.mistechmod.item.PipeItem> PIPE = ITEMS.registerItem("pipe",
            com.mistech.mistechmod.item.PipeItem::new, p -> p.stacksTo(1));
    /** Mapa Astral: cópia em papel do mapa de um jogador (Mesa de Astrologia). */
    public static final DeferredItem<com.mistech.mistechmod.item.NatalChartItem> NATAL_CHART = ITEMS.registerItem("natal_chart",
            com.mistech.mistechmod.item.NatalChartItem::new, p -> p.stacksTo(1).rarity(Rarity.UNCOMMON));
    /** Cachimbo Rúnico (ritual Tier II): cachimbo que aprende runas como o Cajado do Gnomo. */
    public static final DeferredItem<com.mistech.mistechmod.item.RunicPipeItem> RUNIC_PIPE = ITEMS.registerItem("runic_pipe",
            com.mistech.mistechmod.item.RunicPipeItem::new, p -> p.stacksTo(1).rarity(Rarity.RARE));
    /** Cachimbo da Névoa (ritual): vira névoa por alguns segundos. */
    public static final DeferredItem<com.mistech.mistechmod.item.MistPipeItem> MIST_PIPE = ITEMS.registerItem("mist_pipe",
            com.mistech.mistechmod.item.MistPipeItem::new, p -> p.durability(32).rarity(Rarity.RARE));

    // Cinzeiro Rúnico: acende/apaga incenso, charuto e baseado com um clique
    public static final DeferredBlock<RunicAshtrayBlock> RUNIC_ASHTRAY = BLOCKS.registerBlock("runic_ashtray",
            RunicAshtrayBlock::new,
            p -> p.mapColor(MapColor.STONE).strength(1.0F).sound(SoundType.STONE).noOcclusion().lightLevel(state -> 3));
    public static final DeferredItem<BlockItem> RUNIC_ASHTRAY_ITEM = ITEMS.registerSimpleBlockItem("runic_ashtray", RUNIC_ASHTRAY);

    // Porta-Incenso: queima incensos do estoque e aplica o Véu de Incenso num raio de 10 blocos
    public static final DeferredBlock<IncenseHolderBlock> INCENSE_HOLDER = BLOCKS.registerBlock("incense_holder",
            IncenseHolderBlock::new,
            p -> p.mapColor(MapColor.WOOD).strength(0.5F).sound(SoundType.WOOD).noOcclusion()
                    .lightLevel(state -> state.getValue(IncenseHolderBlock.LIT) ? 4 : 0));
    public static final DeferredItem<BlockItem> INCENSE_HOLDER_ITEM = ITEMS.registerSimpleBlockItem("incense_holder", INCENSE_HOLDER);

    // ----- Automação -----

    // Barril Espacial: 162 espaços (3 baús duplos); quebrar não derruba os itens (igual shulker)
    public static final DeferredBlock<SpatialBarrelBlock> SPATIAL_BARREL = BLOCKS.registerBlock("spatial_barrel",
            SpatialBarrelBlock::new,
            p -> p.mapColor(MapColor.COLOR_PURPLE).strength(2.5F).sound(SoundType.WOOD));
    public static final DeferredItem<StorageBlockItem> SPATIAL_BARREL_ITEM = ITEMS.registerItem("spatial_barrel",
            p -> new StorageBlockItem(SPATIAL_BARREL.get(), p, true), p -> p.useBlockDescriptionPrefix().stacksTo(1));

    // Tanque Rúnico: 100 baldes de um fluido; quebrar mantém o fluido no item
    public static final DeferredBlock<RunicTankBlock> RUNIC_TANK = BLOCKS.registerBlock("runic_tank",
            RunicTankBlock::new,
            p -> p.mapColor(MapColor.WOOD).strength(2.0F).sound(SoundType.WOOD).noOcclusion());
    public static final DeferredItem<StorageBlockItem> RUNIC_TANK_ITEM = ITEMS.registerItem("runic_tank",
            p -> new StorageBlockItem(RUNIC_TANK.get(), p, false), p -> p.useBlockDescriptionPrefix());

    // Barril Carregável (mochila): vai no peitoral; abre com B
    public static final DeferredItem<BackpackItem> CARRYABLE_BARREL = ITEMS.registerItem("carryable_barrel",
            BackpackItem::new, p -> p.stacksTo(1)
                    .component(DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.CHEST)
                            .setAsset(ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(MODID, "carryable_barrel")))
                            .setEquipSound(SoundEvents.ARMOR_EQUIP_LEATHER)
                            .setDamageOnHurt(false)
                            .build()));

    // Confector de Runas: multibloco 2x2 (Barril Espacial + Mesa de Escrita + 2 Tanques), montado com a Pedra Guia
    public static final DeferredBlock<RuneConfectorBlock> RUNE_CONFECTOR = BLOCKS.registerBlock("rune_confector",
            RuneConfectorBlock::new,
            p -> p.mapColor(MapColor.COLOR_PURPLE).strength(2.5F).sound(SoundType.WOOD).noOcclusion().noLootTable()
                    .pushReaction(PushReaction.BLOCK).lightLevel(state -> 5));

    // Charuto e Baseado Mágicos: enrolados numa runa (1 uso) + folha seca + Resíduo Mágico
    public static final DeferredItem<com.mistech.mistechmod.item.MagicSmokableItem> MAGIC_CIGAR = ITEMS.registerItem("magic_cigar",
            p -> new com.mistech.mistechmod.item.MagicSmokableItem(p, true), p -> p.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<com.mistech.mistechmod.item.MagicSmokableItem> MAGIC_JOINT = ITEMS.registerItem("magic_joint",
            p -> new com.mistech.mistechmod.item.MagicSmokableItem(p, false), p -> p.rarity(Rarity.UNCOMMON));

    // Enroladora Rúnica: faz os charutos/baseados mágicos sozinha
    public static final DeferredBlock<com.mistech.mistechmod.block.roller.RuneRollerBlock> RUNE_ROLLER = BLOCKS.registerBlock("rune_roller",
            com.mistech.mistechmod.block.roller.RuneRollerBlock::new,
            p -> p.mapColor(MapColor.STONE).requiresCorrectToolForDrops().strength(2.0F).sound(SoundType.STONE).noOcclusion());
    public static final DeferredItem<BlockItem> RUNE_ROLLER_ITEM = ITEMS.registerSimpleBlockItem("rune_roller", RUNE_ROLLER);

    // Creates a creative tab with the id "mistech:example_tab" for the example item, that is placed after the combat tab
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EXAMPLE_TAB = CREATIVE_MODE_TABS.register("example_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.mistech")) //The language key for the title of your CreativeModeTab
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> INCENSE.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(GUIDE_BOOK.get());
                output.accept(INCENSE.get());
                output.accept(INCENSE_HOLDER_ITEM.get());
                output.accept(INCENSE_ASH.get());
                output.accept(ASH_CHALK.get());
                output.accept(MAGIC_INCENSE.get());
                output.accept(MAGIC_RESIDUE.get());
                output.accept(IMPURE_CHALK.get());
                output.accept(ASH_BLOCK_ITEM.get());
                output.accept(SPATIAL_BARREL_ITEM.get());
                output.accept(RUNIC_TANK_ITEM.get());
                output.accept(CARRYABLE_BARREL.get());
                for (DyeColor color : DyeColor.values()) {
                    output.accept(writingTableStack(color));
                }
                output.accept(FIRE_RUNE.get());
                output.accept(WATER_RUNE.get());
                output.accept(EARTH_RUNE.get());
                output.accept(AIR_RUNE.get());
                output.accept(ICE_RUNE.get());
                output.accept(ENERGY_RUNE.get());
                output.accept(LIGHT_RUNE.get());
                output.accept(DARKNESS_RUNE.get());
                output.accept(SPACE_RUNE.get());
                output.accept(TIME_RUNE.get());
                output.accept(MAGMA_RUNE.get());
                output.accept(VAPOR_RUNE.get());
                output.accept(VOID_RUNE.get());
                output.accept(LUCK_RUNE.get());
                output.accept(ARCANE_RUNE.get());
                com.mistech.mistechmod.registry.MithrilContent.creativeItems().forEach(item -> output.accept(item.get()));
                com.mistech.mistechmod.registry.SpatialContent.creativeItems().forEach(item -> output.accept(item.get()));
                com.mistech.mistechmod.registry.AltarContent.creativeItems().forEach(item -> output.accept(item.get()));
                output.accept(GLASS_JAR_ITEM.get());
                output.accept(BOTTLED_LIGHTNING_ITEM.get());
                output.accept(RUNIC_FURNACE_ITEM.get());
                output.accept(CONDENSER_ITEM.get());
                output.accept(ARCANE_NODE_ITEM.get());
                output.accept(ARCANE_RECEIVER_ITEM.get());
                output.accept(ESSENCE_EXTRACTOR_ITEM.get());
                output.accept(IMPURE_ESSENCE_BUCKET.get());
                output.accept(IMPURE_ESSENCE_BOTTLE.get());
                output.accept(MAGIC_COMPRESSOR_ITEM.get());
                output.accept(IMPURE_MAGIC_SHARD.get());
                output.accept(IMPURE_MAGIC_BLOCK_ITEM.get());
                output.accept(ASTRAL_PILLAR_ITEM.get());
                for (DyeColor color : DyeColor.values()) {
                    output.accept(guideStoneStack(color));
                }
                output.accept(ASTROLOGY_TABLE_ITEM.get());
                output.accept(TOBACCO_SEEDS.get());
                output.accept(TOBACCO_LEAF.get());
                output.accept(HEMP_SEEDS.get());
                output.accept(HEMP_LEAF.get());
                output.accept(HEMP_FIBER.get());
                output.accept(BELLADONNA_SEEDS.get());
                output.accept(COFFEE_SEEDS.get());
                output.accept(COFFEE_CHERRY.get());
                output.accept(ROASTED_COFFEE_BEANS.get());
                output.accept(COFFEE_POWDER.get());
                output.accept(GRINDER.get());
                output.accept(KETTLE_ITEM.get());
                output.accept(RAW_CUP.get());
                output.accept(CUP.get());
                output.accept(COFFEE_CUP.get());
                output.accept(BELLADONNA_TEA_CUP.get());
                com.mistech.mistechmod.registry.DreamContent.creativeItems().forEach(item -> output.accept(item.get()));
                com.mistech.mistechmod.registry.RealmContent.creativeItems().forEach(item -> output.accept(item.get()));
                com.mistech.mistechmod.registry.OreContent.creativeItems().forEach(item -> output.accept(item.get()));
                com.mistech.mistechmod.registry.GoblinContent.creativeItems().forEach(item -> output.accept(item.get()));
                output.accept(BELLADONNA_BERRIES.get());
                output.accept(DRIED_TOBACCO_LEAF.get());
                output.accept(DRIED_HEMP_LEAF.get());
                output.accept(CIGAR.get());
                output.accept(JOINT.get());
                output.accept(PIPE.get());
                output.accept(RUNIC_PIPE.get());
                output.accept(MIST_PIPE.get());
                output.accept(NATAL_CHART.get());
                output.accept(RUNIC_ASHTRAY_ITEM.get());
                output.accept(RUNE_ROLLER_ITEM.get());
                // Máquinas II
                com.mistech.mistechmod.registry.MachineContent.creativeItems().forEach(item -> output.accept(item.get()));
                com.mistech.mistechmod.registry.GolemContent.creativeItems().forEach(item -> output.accept(item.get()));
                com.mistech.mistechmod.registry.AccessoryContent.creativeItems().forEach(item -> output.accept(item.get()));
                // Árvore Turva
                com.mistech.mistechmod.registry.MurkyWood.creativeItems().forEach(item -> output.accept(item.get()));
                // Gnomos
                com.mistech.mistechmod.registry.GnomeContent.creativeItems().forEach(item -> output.accept(item.get()));
                // Rituais Tier I
                com.mistech.mistechmod.registry.RitualContent.creativeItems().forEach(item -> output.accept(item.get()));
                // Magia Purificada
                com.mistech.mistechmod.purity.PurityContent.creativeItems().forEach(item -> output.accept(item.get()));
                com.mistech.mistechmod.celestial.CelestialContent.creativeItems().forEach(item -> output.accept(item.get()));
                com.mistech.mistechmod.folklore.FolkloreContent.creativeItems().forEach(item -> output.accept(item.get()));
                com.mistech.mistechmod.beyond.BeyondContent.creativeItems().forEach(item -> output.accept(item.get()));
                com.mistech.mistechmod.sky.SkyContent.creativeItems().forEach(item -> output.accept(item.get()));
                com.mistech.mistechmod.fey.FeyContent.creativeItems().forEach(item -> output.accept(item.get()));
                com.mistech.mistechmod.life.LifeContent.creativeItems().forEach(item -> output.accept(item.get()));
                com.mistech.mistechmod.ranged.RangedContent.creativeItems().forEach(item -> output.accept(item.get()));
                com.mistech.mistechmod.mage.MageContent.creativeItems().forEach(item -> output.accept(item.get()));
                com.mistech.mistechmod.seeking.SeekingContent.creativeItems().forEach(item -> output.accept(item.get()));
                for (com.mistech.mistechmod.item.RuneElement element : com.mistech.mistechmod.item.RuneElement.values()) {
                    output.accept(com.mistech.mistechmod.ranged.RuneArrowRecipe.of(element, 1));
                }
                com.mistech.mistechmod.artifact.ArtifactContent.creativeItems().forEach(item -> output.accept(item.get()));
                com.mistech.mistechmod.ether.EtherContent.creativeItems().forEach(item -> output.accept(item.get()));
                com.mistech.mistechmod.power.PowerContent.creativeItems().forEach(item -> output.accept(item.get()));
                com.mistech.mistechmod.archive.ArchiveContent.creativeItems().forEach(item -> output.accept(item.get()));
                com.mistech.mistechmod.golem.GolemRegistry.creativeItems().forEach(item -> output.accept(item.get()));
                com.mistech.mistechmod.tarot.TarotContent.creativeItems().forEach(item -> output.accept(item.get()));
                output.accept(com.mistech.mistechmod.registry.MistechWear.GNOME_HAT.get());
                output.accept(com.mistech.mistechmod.registry.MistechWear.MYSTIC_ROBE.get());
                output.accept(com.mistech.mistechmod.registry.MistechWear.COMFY_PANTS.get());
                output.accept(com.mistech.mistechmod.registry.MistechWear.SLIPPERS.get());
                for (com.mistech.mistechmod.item.RuneElement element : com.mistech.mistechmod.item.RuneElement.values()) {
                    output.accept(com.mistech.mistechmod.item.MagicSmokableItem.create(MAGIC_CIGAR.get(), element, 1));
                    output.accept(com.mistech.mistechmod.item.MagicSmokableItem.create(MAGIC_JOINT.get(), element, 1));
                }
                output.accept(EXAMPLE_ITEM.get());// Add the example item to the tab. For your own tabs, this method is preferred over the event
            }).build());

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public Mistech(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register the Deferred Register to the mod event bus so blocks get registered
        BLOCKS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so items get registered
        ITEMS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so tabs get registered
        CREATIVE_MODE_TABS.register(modEventBus);
        MistechComponents.register(modEventBus);
        MistechEffects.register(modEventBus);
        com.mistech.mistechmod.registry.MistechPotions.register(modEventBus);
        com.mistech.mistechmod.registry.MistechWear.init();
        com.mistech.mistechmod.registry.MurkyWood.register(modEventBus);
        com.mistech.mistechmod.registry.GnomeContent.register(modEventBus);
        com.mistech.mistechmod.registry.MachineContent.register(modEventBus);
        com.mistech.mistechmod.registry.GolemContent.register(modEventBus);
        com.mistech.mistechmod.registry.RitualContent.register(modEventBus);
        com.mistech.mistechmod.registry.DreamContent.register(modEventBus);
        com.mistech.mistechmod.registry.RealmContent.register(modEventBus);
        com.mistech.mistechmod.registry.OreContent.init();
        com.mistech.mistechmod.registry.SpatialContent.register(modEventBus);
        com.mistech.mistechmod.registry.MithrilContent.register(modEventBus);
        com.mistech.mistechmod.registry.AltarContent.register(modEventBus);
        com.mistech.mistechmod.registry.GoblinContent.register(modEventBus);
        com.mistech.mistechmod.registry.AccessoryContent.init();
        com.mistech.mistechmod.purity.PurityContent.register(modEventBus);
        com.mistech.mistechmod.celestial.CelestialContent.init();
        com.mistech.mistechmod.folklore.FolkloreContent.register(modEventBus);
        com.mistech.mistechmod.beyond.BeyondContent.register(modEventBus);
        com.mistech.mistechmod.sky.SkyContent.register(modEventBus);
        com.mistech.mistechmod.fey.FeyContent.register(modEventBus);
        com.mistech.mistechmod.life.LifeContent.register(modEventBus);
        com.mistech.mistechmod.ranged.RangedContent.register(modEventBus);
        com.mistech.mistechmod.mage.MageContent.register(modEventBus);
        com.mistech.mistechmod.seeking.SeekingContent.register(modEventBus);
        com.mistech.mistechmod.artifact.ArtifactContent.register(modEventBus);
        com.mistech.mistechmod.ether.EtherContent.register(modEventBus);
        com.mistech.mistechmod.power.PowerContent.register(modEventBus);
        com.mistech.mistechmod.archive.ArchiveContent.register(modEventBus);
        com.mistech.mistechmod.golem.GolemRegistry.register(modEventBus);
        com.mistech.mistechmod.tarot.TarotContent.register(modEventBus);
        com.mistech.mistechmod.event.MurkySaplingModifier.register(modEventBus);
        MistechBlockEntities.register(modEventBus);
        MistechMenus.register(modEventBus);
        MistechRecipes.register(modEventBus);
        MistechFluids.register(modEventBus);
        MistechAttachments.register(modEventBus);
        modEventBus.addListener(this::registerCapabilities);

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (Mistech) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private static BlockBehaviour.Properties cropProperties(BlockBehaviour.Properties p) {
        return p.mapColor(MapColor.PLANT).noCollision().randomTicks().instabreak()
                .sound(SoundType.CROP).pushReaction(PushReaction.DESTROY);
    }

    /** Expõe a energia das máquinas antigas para cabos/máquinas de qualquer mod. */
    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Energy.BLOCK, MistechBlockEntities.CONDENSER.get(),
                (condenser, side) -> condenser.getEnergyHandler());
        event.registerBlockEntity(Capabilities.Energy.BLOCK, MistechBlockEntities.ESSENCE_EXTRACTOR.get(),
                (extractor, side) -> extractor.getEnergyHandler());

        // Automação: barril (itens), tanque (fluido) e Confector (itens + fluido em qualquer parte)
        event.registerBlockEntity(Capabilities.Item.BLOCK, MistechBlockEntities.SPATIAL_BARREL.get(),
                (barrel, side) -> net.neoforged.neoforge.transfer.item.VanillaContainerWrapper.of(barrel));
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, MistechBlockEntities.RUNIC_TANK.get(),
                (tank, side) -> tank.getTank());
        event.registerBlock(Capabilities.Item.BLOCK, (level, pos, state, be, side) -> {
            var master = RuneConfectorBlock.getMaster(level, state, pos);
            return master == null ? null : new net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper(master, side);
        }, RUNE_CONFECTOR.get());
        event.registerBlockEntity(Capabilities.Item.BLOCK, MistechBlockEntities.RUNE_ROLLER.get(),
                (roller, side) -> new net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper(roller, side));
        event.registerBlock(Capabilities.Fluid.BLOCK, (level, pos, state, be, side) -> {
            var master = RuneConfectorBlock.getMaster(level, state, pos);
            return master == null ? null : master.getFluidHandler();
        }, RUNE_CONFECTOR.get());
    }

    /** Mesa de Escrita com o pano da cor indicada. */
    public static ItemStack writingTableStack(DyeColor color) {
        ItemStack stack = new ItemStack(WRITING_TABLE_ITEM.get());
        stack.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(WritingTableBlock.COLOR, color));
        return stack;
    }

    /** Pedra Guia da cor do corante. */
    public static ItemStack guideStoneStack(DyeColor color) {
        ItemStack stack = new ItemStack(GUIDE_STONE.get());
        stack.set(DataComponents.DYED_COLOR, new DyedItemColor(color.getTextureDiffuseColor() & 0xFFFFFF));
        return stack;
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");
        event.enqueueWork(com.mistech.mistechmod.registry.MurkyWood::setup);

        if (Config.LOG_DIRT_BLOCK.getAsBoolean()) {
            LOGGER.info("DIRT BLOCK >> {}", BuiltInRegistries.BLOCK.getKey(Blocks.DIRT));
        }

        LOGGER.info("{}{}", Config.MAGIC_NUMBER_INTRODUCTION.get(), Config.MAGIC_NUMBER.getAsInt());

        Config.ITEM_STRINGS.get().forEach((item) -> LOGGER.info("ITEM >> {}", item));
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(EXAMPLE_BLOCK_ITEM);
        }
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(com.mistech.mistechmod.registry.GnomeContent.GNOME_SPAWN_EGG);
        }
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }
}

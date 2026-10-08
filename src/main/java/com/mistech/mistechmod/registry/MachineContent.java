package com.mistech.mistechmod.registry;

import java.util.List;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.machine.AshGeneratorBlock;
import com.mistech.mistechmod.machine.AstralGeneratorBlock;
import com.mistech.mistechmod.machine.MachineBlock;
import com.mistech.mistechmod.machine.MachineBlockEntity;
import com.mistech.mistechmod.machine.RunicCrusherBlock;
import com.mistech.mistechmod.machine.RunicHarvesterBlock;
import com.mistech.mistechmod.machine.RunicPlanterBlock;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper;

/** Máquinas II: Triturador Rúnico, Gerador de Cinzas, Gerador Astral, Colheitadeira e Plantadeira (+ pós), Baterias Rúnicas. */
public final class MachineContent {
    // ----- Triturador + pós -----
    public static final DeferredBlock<RunicCrusherBlock> RUNIC_CRUSHER = Mistech.BLOCKS.registerBlock("runic_crusher",
            RunicCrusherBlock::new, MachineContent::stoneMachine);
    public static final DeferredItem<BlockItem> RUNIC_CRUSHER_ITEM = Mistech.ITEMS.registerSimpleBlockItem("runic_crusher", RUNIC_CRUSHER);

    public static final DeferredItem<Item> IRON_DUST = Mistech.ITEMS.registerSimpleItem("iron_dust");
    public static final DeferredItem<Item> GOLD_DUST = Mistech.ITEMS.registerSimpleItem("gold_dust");
    public static final DeferredItem<Item> COPPER_DUST = Mistech.ITEMS.registerSimpleItem("copper_dust");
    public static final DeferredItem<Item> IMPURE_MAGIC_DUST = Mistech.ITEMS.registerSimpleItem("impure_magic_dust");

    // ----- Geradores -----
    public static final DeferredBlock<AshGeneratorBlock> ASH_GENERATOR = Mistech.BLOCKS.registerBlock("ash_generator",
            AshGeneratorBlock::new, p -> stoneMachine(p).lightLevel(state -> state.getValue(MachineBlock.LIT) ? 9 : 0));
    public static final DeferredItem<BlockItem> ASH_GENERATOR_ITEM = Mistech.ITEMS.registerSimpleBlockItem("ash_generator", ASH_GENERATOR);

    public static final DeferredBlock<AstralGeneratorBlock> ASTRAL_GENERATOR = Mistech.BLOCKS.registerBlock("astral_generator",
            AstralGeneratorBlock::new, p -> p.mapColor(MapColor.LAPIS).requiresCorrectToolForDrops().strength(3.0F)
                    .sound(SoundType.AMETHYST).noOcclusion().lightLevel(state -> state.getValue(MachineBlock.LIT) ? 7 : 2));
    public static final DeferredItem<BlockItem> ASTRAL_GENERATOR_ITEM = Mistech.ITEMS.registerSimpleBlockItem("astral_generator", ASTRAL_GENERATOR);

    // ----- Fazenda -----
    public static final DeferredBlock<RunicHarvesterBlock> RUNIC_HARVESTER = Mistech.BLOCKS.registerBlock("runic_harvester",
            RunicHarvesterBlock::new, MachineContent::woodMachine);
    public static final DeferredItem<BlockItem> RUNIC_HARVESTER_ITEM = Mistech.ITEMS.registerSimpleBlockItem("runic_harvester", RUNIC_HARVESTER);

    public static final DeferredBlock<RunicPlanterBlock> RUNIC_PLANTER = Mistech.BLOCKS.registerBlock("runic_planter",
            RunicPlanterBlock::new, MachineContent::woodMachine);
    public static final DeferredItem<BlockItem> RUNIC_PLANTER_ITEM = Mistech.ITEMS.registerSimpleBlockItem("runic_planter", RUNIC_PLANTER);

    // ----- Mineração -----
    public static final DeferredBlock<com.mistech.mistechmod.machine.TunnelerBlock> TUNNELER = Mistech.BLOCKS.registerBlock("tunneler",
            com.mistech.mistechmod.machine.TunnelerBlock::new, MachineContent::stoneMachine);
    public static final DeferredItem<BlockItem> TUNNELER_ITEM = Mistech.ITEMS.registerSimpleBlockItem("tunneler", TUNNELER);

    /** Núcleo da Mineradora: sem item (nasce do Bloco de Magia Impura; quebrado, devolve o bloco). */
    public static final DeferredBlock<com.mistech.mistechmod.machine.MinerBlock> MINER = Mistech.BLOCKS.registerBlock("miner",
            com.mistech.mistechmod.machine.MinerBlock::new, p -> p.mapColor(MapColor.COLOR_PURPLE).requiresCorrectToolForDrops()
                    .strength(4.0F, 6.0F).sound(SoundType.AMETHYST).noOcclusion()
                    .lightLevel(state -> state.getValue(MachineBlock.LIT) ? 10 : 4));

    // ----- Baterias -----
    public static final DeferredBlock<com.mistech.mistechmod.machine.RunicBatteryBlock> RUNIC_BATTERY = Mistech.BLOCKS.registerBlock("runic_battery",
            com.mistech.mistechmod.machine.RunicBatteryBlock::new, p -> p.mapColor(MapColor.COLOR_ORANGE).requiresCorrectToolForDrops()
                    .strength(3.0F).sound(SoundType.COPPER).noOcclusion().lightLevel(state -> state.getValue(MachineBlock.LIT) ? 6 : 0));
    public static final DeferredItem<com.mistech.mistechmod.item.EnergyBlockItem> RUNIC_BATTERY_ITEM = Mistech.ITEMS.registerItem("runic_battery",
            p -> new com.mistech.mistechmod.item.EnergyBlockItem(RUNIC_BATTERY.get(), p, com.mistech.mistechmod.machine.RunicBatteryBlockEntity.CAPACITY),
            p -> p.useBlockDescriptionPrefix());

    public static final DeferredBlock<com.mistech.mistechmod.machine.AdvancedRunicBatteryBlock> ADVANCED_RUNIC_BATTERY = Mistech.BLOCKS.registerBlock("advanced_runic_battery",
            com.mistech.mistechmod.machine.AdvancedRunicBatteryBlock::new, p -> p.mapColor(MapColor.COLOR_PURPLE).requiresCorrectToolForDrops()
                    .strength(4.0F, 6.0F).sound(SoundType.AMETHYST).noOcclusion().lightLevel(state -> state.getValue(MachineBlock.LIT) ? 9 : 2));
    public static final DeferredItem<com.mistech.mistechmod.item.EnergyBlockItem> ADVANCED_RUNIC_BATTERY_ITEM = Mistech.ITEMS.registerItem("advanced_runic_battery",
            p -> new com.mistech.mistechmod.item.EnergyBlockItem(ADVANCED_RUNIC_BATTERY.get(), p, com.mistech.mistechmod.machine.AdvancedRunicBatteryBlockEntity.CAPACITY),
            p -> p.useBlockDescriptionPrefix().rarity(net.minecraft.world.item.Rarity.UNCOMMON));

    /** Bateria de Bolso (ritual Tier I com a Bateria Aprimorada). */
    public static final DeferredItem<com.mistech.mistechmod.item.PocketBatteryItem> POCKET_BATTERY = Mistech.ITEMS.registerItem("pocket_battery",
            com.mistech.mistechmod.item.PocketBatteryItem::new, p -> p.stacksTo(1).rarity(net.minecraft.world.item.Rarity.RARE));

    private MachineContent() {}

    private static BlockBehaviour.Properties stoneMachine(BlockBehaviour.Properties p) {
        return p.mapColor(MapColor.STONE).requiresCorrectToolForDrops().strength(3.5F).sound(SoundType.STONE).noOcclusion();
    }

    private static BlockBehaviour.Properties woodMachine(BlockBehaviour.Properties p) {
        return p.mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD).noOcclusion();
    }

    public static List<DeferredItem<? extends Item>> creativeItems() {
        return List.of(RUNIC_CRUSHER_ITEM, IRON_DUST, GOLD_DUST, COPPER_DUST, IMPURE_MAGIC_DUST,
                ASH_GENERATOR_ITEM, ASTRAL_GENERATOR_ITEM, RUNIC_HARVESTER_ITEM, RUNIC_PLANTER_ITEM, TUNNELER_ITEM,
                RUNIC_BATTERY_ITEM, ADVANCED_RUNIC_BATTERY_ITEM, POCKET_BATTERY);
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(MachineContent::onCapabilities);
    }

    private static void onCapabilities(RegisterCapabilitiesEvent event) {
        registerMachine(event, MistechBlockEntities.RUNIC_CRUSHER.get());
        registerMachine(event, MistechBlockEntities.ASH_GENERATOR.get());
        registerMachine(event, MistechBlockEntities.ASTRAL_GENERATOR.get());
        registerMachine(event, MistechBlockEntities.RUNIC_HARVESTER.get());
        registerMachine(event, MistechBlockEntities.RUNIC_PLANTER.get());
        // Mineradora: energia no núcleo; itens = os do barril de baixo (Câmara Coletora) enquanto montada
        event.registerBlockEntity(Capabilities.Energy.BLOCK, MistechBlockEntities.MINER.get(), (miner, side) -> miner.getEnergyHandler());
        event.registerBlockEntity(Capabilities.Item.BLOCK, MistechBlockEntities.MINER.get(), (miner, side) ->
                miner.chamber() != null && miner.getLevel() != null
                        ? miner.getLevel().getCapability(Capabilities.Item.BLOCK, miner.getBlockPos().below(), net.minecraft.core.Direction.UP)
                        : null);
        event.registerBlockEntity(Capabilities.Energy.BLOCK, MistechBlockEntities.TUNNELER.get(), (tunneler, side) -> tunneler.getEnergyHandler());
        // Bateria de Bolso: recebe e entrega até 10.000 FE/t
        event.registerItem(Capabilities.Energy.ITEM, (stack, access) -> new net.neoforged.neoforge.transfer.energy.ItemAccessEnergyHandler(access,
                MistechComponents.BATTERY_ENERGY.get(), com.mistech.mistechmod.item.PocketBatteryItem.CAPACITY,
                com.mistech.mistechmod.item.PocketBatteryItem.MAX_CHARGE, com.mistech.mistechmod.item.PocketBatteryItem.MAX_CHARGE),
                POCKET_BATTERY.get());
        // baterias: só energia (o espaço de carga não é exposto a funis/canos)
        event.registerBlockEntity(Capabilities.Energy.BLOCK, MistechBlockEntities.RUNIC_BATTERY.get(), (battery, side) -> battery.handlerFor(side));
        event.registerBlockEntity(Capabilities.Energy.BLOCK, MistechBlockEntities.ADVANCED_RUNIC_BATTERY.get(), (battery, side) -> battery.handlerFor(side));
    }

    private static <T extends MachineBlockEntity> void registerMachine(RegisterCapabilitiesEvent event,
            net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        event.registerBlockEntity(Capabilities.Energy.BLOCK, type, (machine, side) -> machine.getEnergyHandler());
        event.registerBlockEntity(Capabilities.Item.BLOCK, type, (machine, side) -> new WorldlyContainerWrapper(machine, side));
    }
}

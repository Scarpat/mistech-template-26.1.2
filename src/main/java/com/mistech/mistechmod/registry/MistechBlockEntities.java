package com.mistech.mistechmod.registry;

import java.util.function.Supplier;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.block.CondenserBlockEntity;
import com.mistech.mistechmod.block.EssenceExtractorBlockEntity;
import com.mistech.mistechmod.block.MagicCompressorBlockEntity;
import com.mistech.mistechmod.block.arcane.ArcaneNodeBlockEntity;
import com.mistech.mistechmod.block.arcane.ArcaneReceiverBlockEntity;
import com.mistech.mistechmod.block.GlassJarBlockEntity;
import com.mistech.mistechmod.block.IncenseHolderBlockEntity;
import com.mistech.mistechmod.block.RunicFurnaceBlockEntity;
import com.mistech.mistechmod.block.WritingTableBlockEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class MistechBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Mistech.MODID);

    public static final Supplier<BlockEntityType<IncenseHolderBlockEntity>> INCENSE_HOLDER =
            BLOCK_ENTITY_TYPES.register("incense_holder",
                    () -> new BlockEntityType<>(IncenseHolderBlockEntity::new, Mistech.INCENSE_HOLDER.get()));

    public static final Supplier<BlockEntityType<WritingTableBlockEntity>> WRITING_TABLE =
            BLOCK_ENTITY_TYPES.register("writing_table",
                    () -> new BlockEntityType<>(WritingTableBlockEntity::new, Mistech.WRITING_TABLE.get()));

    public static final Supplier<BlockEntityType<RunicFurnaceBlockEntity>> RUNIC_FURNACE =
            BLOCK_ENTITY_TYPES.register("runic_furnace",
                    () -> new BlockEntityType<>(RunicFurnaceBlockEntity::new, Mistech.RUNIC_FURNACE.get()));

    public static final Supplier<BlockEntityType<GlassJarBlockEntity>> GLASS_JAR =
            BLOCK_ENTITY_TYPES.register("glass_jar",
                    () -> new BlockEntityType<>(GlassJarBlockEntity::new, Mistech.GLASS_JAR.get()));

    public static final Supplier<BlockEntityType<CondenserBlockEntity>> CONDENSER =
            BLOCK_ENTITY_TYPES.register("condenser",
                    () -> new BlockEntityType<>(CondenserBlockEntity::new, Mistech.CONDENSER.get()));

    public static final Supplier<BlockEntityType<ArcaneNodeBlockEntity>> ARCANE_NODE =
            BLOCK_ENTITY_TYPES.register("arcane_node", () -> new BlockEntityType<>(ArcaneNodeBlockEntity::new, Mistech.ARCANE_NODE.get()));
    public static final Supplier<BlockEntityType<ArcaneReceiverBlockEntity>> ARCANE_RECEIVER =
            BLOCK_ENTITY_TYPES.register("arcane_receiver", () -> new BlockEntityType<>(ArcaneReceiverBlockEntity::new, Mistech.ARCANE_RECEIVER.get()));
    public static final Supplier<BlockEntityType<EssenceExtractorBlockEntity>> ESSENCE_EXTRACTOR =
            BLOCK_ENTITY_TYPES.register("essence_extractor", () -> new BlockEntityType<>(EssenceExtractorBlockEntity::new, Mistech.ESSENCE_EXTRACTOR.get()));
    public static final Supplier<BlockEntityType<MagicCompressorBlockEntity>> MAGIC_COMPRESSOR =
            BLOCK_ENTITY_TYPES.register("magic_compressor", () -> new BlockEntityType<>(MagicCompressorBlockEntity::new, Mistech.MAGIC_COMPRESSOR.get()));

    public static final Supplier<BlockEntityType<com.mistech.mistechmod.block.SpatialBarrelBlockEntity>> SPATIAL_BARREL =
            BLOCK_ENTITY_TYPES.register("spatial_barrel", () -> new BlockEntityType<>(com.mistech.mistechmod.block.SpatialBarrelBlockEntity::new, Mistech.SPATIAL_BARREL.get()));
    public static final Supplier<BlockEntityType<com.mistech.mistechmod.block.RunicTankBlockEntity>> RUNIC_TANK =
            BLOCK_ENTITY_TYPES.register("runic_tank", () -> new BlockEntityType<>(com.mistech.mistechmod.block.RunicTankBlockEntity::new, Mistech.RUNIC_TANK.get()));
    public static final Supplier<BlockEntityType<com.mistech.mistechmod.block.confector.RuneConfectorBlockEntity>> RUNE_CONFECTOR =
            BLOCK_ENTITY_TYPES.register("rune_confector", () -> new BlockEntityType<>(com.mistech.mistechmod.block.confector.RuneConfectorBlockEntity::new, Mistech.RUNE_CONFECTOR.get()));

    public static final Supplier<BlockEntityType<com.mistech.mistechmod.block.roller.RuneRollerBlockEntity>> RUNE_ROLLER =
            BLOCK_ENTITY_TYPES.register("rune_roller", () -> new BlockEntityType<>(com.mistech.mistechmod.block.roller.RuneRollerBlockEntity::new, Mistech.RUNE_ROLLER.get()));

    public static final Supplier<BlockEntityType<com.mistech.mistechmod.block.BottledGnomeBlockEntity>> BOTTLED_GNOME =
            BLOCK_ENTITY_TYPES.register("bottled_gnome", () -> new BlockEntityType<>(com.mistech.mistechmod.block.BottledGnomeBlockEntity::new, GnomeContent.BOTTLED_GNOME.get()));
    public static final Supplier<BlockEntityType<com.mistech.mistechmod.machine.GnomeBicycleBlockEntity>> GNOME_BICYCLE =
            BLOCK_ENTITY_TYPES.register("gnome_bicycle", () -> new BlockEntityType<>(com.mistech.mistechmod.machine.GnomeBicycleBlockEntity::new, GnomeContent.GNOME_BICYCLE.get()));
    public static final Supplier<BlockEntityType<com.mistech.mistechmod.block.KettleBlockEntity>> KETTLE =
            BLOCK_ENTITY_TYPES.register("kettle", () -> new BlockEntityType<>(com.mistech.mistechmod.block.KettleBlockEntity::new, Mistech.KETTLE.get()));

    // ----- Máquinas II -----
    public static final Supplier<BlockEntityType<com.mistech.mistechmod.machine.RunicCrusherBlockEntity>> RUNIC_CRUSHER =
            BLOCK_ENTITY_TYPES.register("runic_crusher", () -> new BlockEntityType<>(com.mistech.mistechmod.machine.RunicCrusherBlockEntity::new, MachineContent.RUNIC_CRUSHER.get()));
    public static final Supplier<BlockEntityType<com.mistech.mistechmod.machine.AshGeneratorBlockEntity>> ASH_GENERATOR =
            BLOCK_ENTITY_TYPES.register("ash_generator", () -> new BlockEntityType<>(com.mistech.mistechmod.machine.AshGeneratorBlockEntity::new, MachineContent.ASH_GENERATOR.get()));
    public static final Supplier<BlockEntityType<com.mistech.mistechmod.machine.AstralGeneratorBlockEntity>> ASTRAL_GENERATOR =
            BLOCK_ENTITY_TYPES.register("astral_generator", () -> new BlockEntityType<>(com.mistech.mistechmod.machine.AstralGeneratorBlockEntity::new, MachineContent.ASTRAL_GENERATOR.get()));
    public static final Supplier<BlockEntityType<com.mistech.mistechmod.machine.RunicHarvesterBlockEntity>> RUNIC_HARVESTER =
            BLOCK_ENTITY_TYPES.register("runic_harvester", () -> new BlockEntityType<>(com.mistech.mistechmod.machine.RunicHarvesterBlockEntity::new, MachineContent.RUNIC_HARVESTER.get()));
    public static final Supplier<BlockEntityType<com.mistech.mistechmod.machine.RunicPlanterBlockEntity>> RUNIC_PLANTER =
            BLOCK_ENTITY_TYPES.register("runic_planter", () -> new BlockEntityType<>(com.mistech.mistechmod.machine.RunicPlanterBlockEntity::new, MachineContent.RUNIC_PLANTER.get()));

    // ----- Mineração -----
    public static final Supplier<BlockEntityType<com.mistech.mistechmod.machine.TunnelerBlockEntity>> TUNNELER =
            BLOCK_ENTITY_TYPES.register("tunneler", () -> new BlockEntityType<>(com.mistech.mistechmod.machine.TunnelerBlockEntity::new, MachineContent.TUNNELER.get()));
    public static final Supplier<BlockEntityType<com.mistech.mistechmod.machine.MinerBlockEntity>> MINER =
            BLOCK_ENTITY_TYPES.register("miner", () -> new BlockEntityType<>(com.mistech.mistechmod.machine.MinerBlockEntity::new, MachineContent.MINER.get()));

    // ----- Baterias -----
    public static final Supplier<BlockEntityType<com.mistech.mistechmod.machine.RunicBatteryBlockEntity>> RUNIC_BATTERY =
            BLOCK_ENTITY_TYPES.register("runic_battery", () -> new BlockEntityType<>(com.mistech.mistechmod.machine.RunicBatteryBlockEntity::new, MachineContent.RUNIC_BATTERY.get()));
    public static final Supplier<BlockEntityType<com.mistech.mistechmod.machine.AdvancedRunicBatteryBlockEntity>> ADVANCED_RUNIC_BATTERY =
            BLOCK_ENTITY_TYPES.register("advanced_runic_battery", () -> new BlockEntityType<>(com.mistech.mistechmod.machine.AdvancedRunicBatteryBlockEntity::new, MachineContent.ADVANCED_RUNIC_BATTERY.get()));

    private MistechBlockEntities() {}

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITY_TYPES.register(modEventBus);
    }
}

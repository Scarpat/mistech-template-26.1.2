package com.mistech.mistechmod.power;

import java.util.List;
import java.util.function.Supplier;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.machine.MachineBlock;
import com.mistech.mistechmod.machine.MachineBlockEntity;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper;

/**
 * Energia avançada (08/10/2026): 4 geradores avançados e 2 Baterias de Bolso novas.
 * <ul>
 * <li><b>Reator Impuro</b> — Magia Impura, 1.000 FE/t, solta Borra Impura.</li>
 * <li><b>Observatório Estelar</b> — Gerador Astral avançado com lente de Fragmento Estelar, até ~700 FE/t.</li>
 * <li><b>Caldeira Ígnea</b> — Gerador de Blaze avançado: lava + catalisador de blaze, até 1.000 FE/t.</li>
 * <li><b>Gerador de Experiência</b> — pontos de experiência, 600 FE/t.</li>
 * <li><b>Bateria de Bolso Criativa</b> (testes) e <b>Bateria de Bolso Astral</b> (janela para a Rede Etérea).</li>
 * </ul>
 */
public final class PowerContent {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Mistech.MODID);
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Mistech.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<AstralView>> ASTRAL_VIEW =
            COMPONENTS.registerComponentType("astral_battery_view", b -> b.persistent(AstralView.CODEC).networkSynchronized(AstralView.STREAM_CODEC));

    // ----- Geradores -----
    public static final DeferredBlock<ImpureReactorBlock> IMPURE_REACTOR = Mistech.BLOCKS.registerBlock("impure_reactor", ImpureReactorBlock::new,
            p -> p.mapColor(MapColor.COLOR_PURPLE).requiresCorrectToolForDrops().strength(5.0F, 9.0F).sound(SoundType.NETHERITE_BLOCK).noOcclusion()
                    .lightLevel(state -> state.getValue(MachineBlock.LIT) ? 11 : 3));
    public static final DeferredItem<BlockItem> IMPURE_REACTOR_ITEM = Mistech.ITEMS.registerSimpleBlockItem("impure_reactor", IMPURE_REACTOR,
            p -> p.rarity(Rarity.UNCOMMON));

    public static final DeferredBlock<StellarObservatoryBlock> STELLAR_OBSERVATORY = Mistech.BLOCKS.registerBlock("stellar_observatory",
            StellarObservatoryBlock::new, p -> p.mapColor(MapColor.LAPIS).requiresCorrectToolForDrops().strength(4.0F, 6.0F)
                    .sound(SoundType.AMETHYST).noOcclusion().lightLevel(state -> state.getValue(MachineBlock.LIT) ? 12 : 4));
    public static final DeferredItem<BlockItem> STELLAR_OBSERVATORY_ITEM = Mistech.ITEMS.registerSimpleBlockItem("stellar_observatory",
            STELLAR_OBSERVATORY, p -> p.rarity(Rarity.RARE));

    public static final DeferredBlock<IgneousBoilerBlock> IGNEOUS_BOILER = Mistech.BLOCKS.registerBlock("igneous_boiler", IgneousBoilerBlock::new,
            p -> p.mapColor(MapColor.COLOR_BLACK).requiresCorrectToolForDrops().strength(4.0F, 9.0F).sound(SoundType.POLISHED_DEEPSLATE).noOcclusion()
                    .lightLevel(state -> state.getValue(MachineBlock.LIT) ? 14 : 2));
    public static final DeferredItem<BlockItem> IGNEOUS_BOILER_ITEM = Mistech.ITEMS.registerSimpleBlockItem("igneous_boiler", IGNEOUS_BOILER,
            p -> p.rarity(Rarity.UNCOMMON).fireResistant());

    public static final DeferredBlock<ExperienceGeneratorBlock> EXPERIENCE_GENERATOR = Mistech.BLOCKS.registerBlock("experience_generator",
            ExperienceGeneratorBlock::new, p -> p.mapColor(MapColor.COLOR_CYAN).requiresCorrectToolForDrops().strength(4.0F, 6.0F)
                    .sound(SoundType.SCULK_CATALYST).noOcclusion().lightLevel(state -> state.getValue(MachineBlock.LIT) ? 10 : 3));
    public static final DeferredItem<BlockItem> EXPERIENCE_GENERATOR_ITEM = Mistech.ITEMS.registerSimpleBlockItem("experience_generator",
            EXPERIENCE_GENERATOR, p -> p.rarity(Rarity.UNCOMMON));

    public static final Supplier<BlockEntityType<ImpureReactorBlockEntity>> IMPURE_REACTOR_BE = BLOCK_ENTITIES.register("impure_reactor",
            () -> new BlockEntityType<>(ImpureReactorBlockEntity::new, IMPURE_REACTOR.get()));
    public static final Supplier<BlockEntityType<StellarObservatoryBlockEntity>> STELLAR_OBSERVATORY_BE = BLOCK_ENTITIES.register("stellar_observatory",
            () -> new BlockEntityType<>(StellarObservatoryBlockEntity::new, STELLAR_OBSERVATORY.get()));
    public static final Supplier<BlockEntityType<IgneousBoilerBlockEntity>> IGNEOUS_BOILER_BE = BLOCK_ENTITIES.register("igneous_boiler",
            () -> new BlockEntityType<>(IgneousBoilerBlockEntity::new, IGNEOUS_BOILER.get()));
    public static final Supplier<BlockEntityType<ExperienceGeneratorBlockEntity>> EXPERIENCE_GENERATOR_BE = BLOCK_ENTITIES.register("experience_generator",
            () -> new BlockEntityType<>(ExperienceGeneratorBlockEntity::new, EXPERIENCE_GENERATOR.get()));

    // ----- Baterias de Bolso -----
    public static final DeferredItem<CreativePocketBatteryItem> CREATIVE_POCKET_BATTERY = Mistech.ITEMS.registerItem("creative_pocket_battery",
            CreativePocketBatteryItem::new, p -> p.stacksTo(1).rarity(Rarity.EPIC).fireResistant());
    public static final DeferredItem<AstralPocketBatteryItem> ASTRAL_POCKET_BATTERY = Mistech.ITEMS.registerItem("astral_pocket_battery",
            AstralPocketBatteryItem::new, p -> p.stacksTo(1).rarity(Rarity.EPIC).fireResistant());

    private PowerContent() {}

    public static List<DeferredItem<? extends Item>> creativeItems() {
        return List.of(IMPURE_REACTOR_ITEM, STELLAR_OBSERVATORY_ITEM, IGNEOUS_BOILER_ITEM, EXPERIENCE_GENERATOR_ITEM,
                ASTRAL_POCKET_BATTERY, CREATIVE_POCKET_BATTERY);
    }

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITIES.register(modEventBus);
        COMPONENTS.register(modEventBus);
        modEventBus.addListener(PowerContent::onCapabilities);
    }

    private static void onCapabilities(RegisterCapabilitiesEvent event) {
        machine(event, IMPURE_REACTOR_BE.get());
        machine(event, STELLAR_OBSERVATORY_BE.get());
        machine(event, IGNEOUS_BOILER_BE.get());
        machine(event, EXPERIENCE_GENERATOR_BE.get());
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, IMPURE_REACTOR_BE.get(), (be, side) -> be.getTank());
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, IGNEOUS_BOILER_BE.get(), (be, side) -> be.getTank());

        event.registerItem(Capabilities.Energy.ITEM, (stack, access) -> InfiniteEnergyHandler.INSTANCE, CREATIVE_POCKET_BATTERY.get());
        event.registerItem(Capabilities.Energy.ITEM, (stack, access) -> AstralPocketBatteryItem.capability(stack), ASTRAL_POCKET_BATTERY.get());
    }

    private static <T extends MachineBlockEntity> void machine(RegisterCapabilitiesEvent event, BlockEntityType<T> type) {
        event.registerBlockEntity(Capabilities.Energy.BLOCK, type, (machine, side) -> machine.getEnergyHandler());
        event.registerBlockEntity(Capabilities.Item.BLOCK, type, (machine, side) -> new WorldlyContainerWrapper(machine, side));
    }
}

package com.mistech.mistechmod.spellcraft.conjurer;

import java.util.List;
import java.util.function.Supplier;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.machine.MachineBlock;
import com.mistech.mistechmod.machine.MachineLayout;
import com.mistech.mistechmod.machine.MachineMenu;
import com.mistech.mistechmod.registry.MistechBlockEntities;
import com.mistech.mistechmod.registry.MistechMenus;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
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

import static com.mistech.mistechmod.machine.MachineLayout.Kind.*;

/**
 * Fase 1 do plano magia × tech: Conjurador Rúnico e Cristal de Feitiço.
 * O cristal é gravado na Mesa de Inscrição (modo Cristal) e o Conjurador lança a magia dele sozinho.
 */
public final class ConjurerContent {
    private ConjurerContent() {}

    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Mistech.MODID);

    /** Magia, poder e dono gravados no cristal. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SpellCrystal>> CRYSTAL_DATA =
            COMPONENTS.registerComponentType("spell_crystal", b -> b.persistent(SpellCrystal.CODEC).networkSynchronized(SpellCrystal.STREAM_CODEC));

    // ----- itens -----
    public static final DeferredItem<Item> BLANK_SPELL_CRYSTAL = Mistech.ITEMS.registerSimpleItem("blank_spell_crystal",
            p -> p.stacksTo(16).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<SpellCrystalItem> SPELL_CRYSTAL = Mistech.ITEMS.registerItem("spell_crystal", SpellCrystalItem::new,
            p -> p.stacksTo(1).rarity(Rarity.RARE));

    // ----- bloco -----
    public static final DeferredBlock<RunicConjurerBlock> RUNIC_CONJURER = Mistech.BLOCKS.registerBlock("runic_conjurer", RunicConjurerBlock::new,
            p -> p.mapColor(MapColor.COLOR_PURPLE).requiresCorrectToolForDrops().strength(4.0F, 8.0F).sound(SoundType.DEEPSLATE_BRICKS)
                    .noOcclusion().lightLevel(state -> state.getValue(MachineBlock.LIT) ? 10 : 3));
    public static final DeferredItem<BlockItem> RUNIC_CONJURER_ITEM = Mistech.ITEMS.registerSimpleBlockItem("runic_conjurer", RUNIC_CONJURER,
            p -> p.rarity(Rarity.RARE));

    public static final Supplier<BlockEntityType<RunicConjurerBlockEntity>> RUNIC_CONJURER_BE = MistechBlockEntities.BLOCK_ENTITY_TYPES.register(
            "runic_conjurer", () -> new BlockEntityType<>(RunicConjurerBlockEntity::new, RUNIC_CONJURER.get()));

    // ----- tela (mesma geometria do Destilador: tanque impuro à esquerda, purificado à direita) -----
    public static final Supplier<MenuType<MachineMenu>> MENU = menu();

    private static Supplier<MenuType<MachineMenu>> menu() {
        java.util.concurrent.atomic.AtomicReference<MenuType<MachineMenu>> ref = new java.util.concurrent.atomic.AtomicReference<>();
        return MistechMenus.MENUS.register("runic_conjurer", () -> {
            MenuType<MachineMenu> type = new MenuType<>((id, inv) -> new MachineMenu(ref.get(), ConjurerContent.LAYOUT, id, inv), FeatureFlags.VANILLA_SET);
            ref.set(type);
            return type;
        });
    }

    /** Cristal em cima, frascos/baldes de essência embaixo, 2 saídas de recipientes vazios; seta = recarga. */
    public static final MachineLayout LAYOUT = MachineLayout.builder("runic_conjurer", MENU::get)
            .slot(40, 25, INPUT)
            .slot(40, 53, FUEL)
            .grid(98, 26, 1, 2, OUTPUT)
            .capacity(RunicConjurerBlockEntity.CAPACITY)
            .input(RunicConjurerBlockEntity::isCrystal)
            .fuel(RunicConjurerBlockEntity::isEssenceContainer)
            .arrow(66, 34)
            .build();

    public static List<DeferredItem<? extends Item>> creativeItems() {
        return List.of(RUNIC_CONJURER_ITEM, BLANK_SPELL_CRYSTAL);
    }

    public static void register(IEventBus modEventBus) {
        COMPONENTS.register(modEventBus);
        modEventBus.addListener(ConjurerContent::onCapabilities);
    }

    private static void onCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Energy.BLOCK, RUNIC_CONJURER_BE.get(), (be, side) -> be.getEnergyHandler());
        event.registerBlockEntity(Capabilities.Item.BLOCK, RUNIC_CONJURER_BE.get(), (be, side) -> new WorldlyContainerWrapper(be, side));
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, RUNIC_CONJURER_BE.get(), (be, side) -> be.getTanks());
    }
}

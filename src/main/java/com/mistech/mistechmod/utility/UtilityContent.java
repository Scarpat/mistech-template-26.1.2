package com.mistech.mistechmod.utility;

import java.util.List;
import java.util.function.Supplier;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.registry.MistechBlockEntities;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

/** Utilitários: Elevador Rúnico e Narguilé Rúnico. */
public final class UtilityContent {
    public static final DeferredBlock<RunicElevatorBlock> RUNIC_ELEVATOR = Mistech.BLOCKS.registerBlock("runic_elevator", RunicElevatorBlock::new,
            p -> p.mapColor(MapColor.COLOR_BLACK).requiresCorrectToolForDrops().strength(2.0F, 6.0F).sound(SoundType.POLISHED_DEEPSLATE));
    public static final DeferredItem<BlockItem> RUNIC_ELEVATOR_ITEM = Mistech.ITEMS.registerSimpleBlockItem("runic_elevator", RUNIC_ELEVATOR);

    public static final DeferredBlock<RunicHookahBlock> RUNIC_HOOKAH = Mistech.BLOCKS.registerBlock("runic_hookah", RunicHookahBlock::new,
            p -> p.mapColor(MapColor.COLOR_PURPLE).strength(1.0F).sound(SoundType.GLASS).noOcclusion()
                    .lightLevel(s -> s.getValue(RunicHookahBlock.LIT) ? 6 : 0));
    public static final DeferredItem<BlockItem> RUNIC_HOOKAH_ITEM = Mistech.ITEMS.registerSimpleBlockItem("runic_hookah", RUNIC_HOOKAH);
    public static final Supplier<BlockEntityType<RunicHookahBlockEntity>> RUNIC_HOOKAH_BE = MistechBlockEntities.BLOCK_ENTITY_TYPES.register(
            "runic_hookah", () -> new BlockEntityType<>(RunicHookahBlockEntity::new, RUNIC_HOOKAH.get()));

    private UtilityContent() {}

    public static List<DeferredItem<? extends Item>> creativeItems() {
        return List.of(RUNIC_ELEVATOR_ITEM, RUNIC_HOOKAH_ITEM);
    }

    public static void register(IEventBus modEventBus) {
    }
}

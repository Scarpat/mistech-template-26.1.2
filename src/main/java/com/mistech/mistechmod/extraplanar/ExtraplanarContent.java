package com.mistech.mistechmod.extraplanar;

import java.util.List;
import java.util.function.Supplier;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.registry.MistechBlockEntities;
import com.mojang.serialization.Codec;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Barril e Tanque Extraplanares: versões do Barril Espacial e do Tanque Rúnico que se ligam por um código (senha).
 * Todos os blocos com o mesmo código dividem o mesmo conteúdo, em qualquer distância e dimensão.
 */
public final class ExtraplanarContent {
    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Mistech.MODID);

    /** Código guardado no item (vem do bloco ao quebrar e volta ao colocar). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> CODE =
            DATA_COMPONENTS.registerComponentType("extraplanar_code",
                    builder -> builder.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));

    public static final DeferredBlock<ExtraplanarBarrelBlock> BARREL = Mistech.BLOCKS.registerBlock("extraplanar_barrel",
            ExtraplanarBarrelBlock::new,
            p -> p.mapColor(MapColor.COLOR_CYAN).strength(2.5F).sound(SoundType.WOOD));
    public static final DeferredItem<ExtraplanarBlockItem> BARREL_ITEM = Mistech.ITEMS.registerItem("extraplanar_barrel",
            p -> new ExtraplanarBlockItem(BARREL.get(), p), p -> p.useBlockDescriptionPrefix().rarity(Rarity.RARE));

    public static final DeferredBlock<ExtraplanarTankBlock> TANK = Mistech.BLOCKS.registerBlock("extraplanar_tank",
            ExtraplanarTankBlock::new,
            p -> p.mapColor(MapColor.COLOR_CYAN).strength(2.0F).sound(SoundType.WOOD).noOcclusion());
    public static final DeferredItem<ExtraplanarBlockItem> TANK_ITEM = Mistech.ITEMS.registerItem("extraplanar_tank",
            p -> new ExtraplanarBlockItem(TANK.get(), p), p -> p.useBlockDescriptionPrefix().rarity(Rarity.RARE));

    public static final Supplier<BlockEntityType<ExtraplanarBarrelBlockEntity>> BARREL_BE =
            MistechBlockEntities.BLOCK_ENTITY_TYPES.register("extraplanar_barrel",
                    () -> new BlockEntityType<>(ExtraplanarBarrelBlockEntity::new, BARREL.get()));
    public static final Supplier<BlockEntityType<ExtraplanarTankBlockEntity>> TANK_BE =
            MistechBlockEntities.BLOCK_ENTITY_TYPES.register("extraplanar_tank",
                    () -> new BlockEntityType<>(ExtraplanarTankBlockEntity::new, TANK.get()));

    private ExtraplanarContent() {}

    public static List<DeferredItem<? extends Item>> creativeItems() {
        return List.of(BARREL_ITEM, TANK_ITEM);
    }

    public static void register(IEventBus modEventBus) {
        DATA_COMPONENTS.register(modEventBus);
    }
}

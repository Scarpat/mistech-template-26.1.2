package com.mistech.mistechmod.ether;

import java.util.List;
import java.util.function.Supplier;

import com.mistech.mistechmod.Mistech;
import com.mojang.serialization.Codec;

import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Rede Etérea (energia avançada).
 * <ul>
 * <li>Etapa A — <b>Pilar de Transmissão</b>: liga Pontos/Receptores Arcanos através de uma malha de pilares (48 blocos entre pilares).</li>
 * <li>Etapa B — <b>Nexo Etéreo</b> (ritual Tier III) + <b>Ponto/Receptor Etéreo</b>: energia guardada na sintonia, sem limite de distância
 * nem de dimensão, sem perda.</li>
 * </ul>
 * Tudo se liga com o <b>Diapasão Arcano</b>.
 */
public final class EtherContent {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Mistech.MODID);
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Mistech.MODID);

    /** Sintonia (rede) guardada no Diapasão / no item do Nexo. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> NETWORK =
            COMPONENTS.registerComponentType("ether_network", b -> b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));
    /** Diapasão: primeiro pilar escolhido para ligar. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GlobalPos>> TUNER_PYLON =
            COMPONENTS.registerComponentType("tuner_pylon", b -> b.persistent(GlobalPos.CODEC).networkSynchronized(GlobalPos.STREAM_CODEC));

    public static final DeferredItem<ArcaneTunerItem> ARCANE_TUNER = Mistech.ITEMS.registerItem("arcane_tuner", ArcaneTunerItem::new,
            p -> p.stacksTo(1).rarity(Rarity.UNCOMMON));

    public static final DeferredBlock<TransmissionPylonBlock> TRANSMISSION_PYLON = Mistech.BLOCKS.registerBlock("transmission_pylon",
            TransmissionPylonBlock::new, p -> p.mapColor(MapColor.COLOR_PURPLE).requiresCorrectToolForDrops().strength(4.0F, 9.0F)
                    .sound(SoundType.METAL).noOcclusion().lightLevel(s -> 7).pushReaction(PushReaction.BLOCK));
    public static final DeferredItem<BlockItem> TRANSMISSION_PYLON_ITEM = Mistech.ITEMS.registerSimpleBlockItem("transmission_pylon",
            TRANSMISSION_PYLON, p -> p.rarity(Rarity.UNCOMMON));

    public static final DeferredBlock<EtherealNexusBlock> ETHEREAL_NEXUS = Mistech.BLOCKS.registerBlock("ethereal_nexus",
            EtherealNexusBlock::new, p -> p.mapColor(MapColor.COLOR_PURPLE).requiresCorrectToolForDrops().strength(6.0F, 1200.0F)
                    .sound(SoundType.AMETHYST).noOcclusion().lightLevel(s -> 12).pushReaction(PushReaction.BLOCK));
    public static final DeferredItem<BlockItem> ETHEREAL_NEXUS_ITEM = Mistech.ITEMS.registerSimpleBlockItem("ethereal_nexus",
            ETHEREAL_NEXUS, p -> p.rarity(Rarity.EPIC));

    public static final DeferredBlock<EtherealEndpointBlock> ETHEREAL_NODE = Mistech.BLOCKS.registerBlock("ethereal_node",
            p -> new EtherealEndpointBlock(p, false), p -> p.mapColor(MapColor.COLOR_PURPLE).strength(1.0F).sound(SoundType.AMETHYST)
                    .noOcclusion().lightLevel(s -> 9).pushReaction(PushReaction.DESTROY));
    public static final DeferredItem<BlockItem> ETHEREAL_NODE_ITEM = Mistech.ITEMS.registerSimpleBlockItem("ethereal_node",
            ETHEREAL_NODE, p -> p.rarity(Rarity.RARE));
    public static final DeferredBlock<EtherealEndpointBlock> ETHEREAL_RECEIVER = Mistech.BLOCKS.registerBlock("ethereal_receiver",
            p -> new EtherealEndpointBlock(p, true), p -> p.mapColor(MapColor.COLOR_PURPLE).strength(1.0F).sound(SoundType.AMETHYST)
                    .noOcclusion().lightLevel(s -> 9).pushReaction(PushReaction.DESTROY));
    public static final DeferredItem<BlockItem> ETHEREAL_RECEIVER_ITEM = Mistech.ITEMS.registerSimpleBlockItem("ethereal_receiver",
            ETHEREAL_RECEIVER, p -> p.rarity(Rarity.RARE));

    public static final Supplier<BlockEntityType<TransmissionPylonBlockEntity>> PYLON_BE = BLOCK_ENTITIES.register("transmission_pylon",
            () -> new BlockEntityType<>(TransmissionPylonBlockEntity::new, TRANSMISSION_PYLON.get()));
    public static final Supplier<BlockEntityType<EtherealNexusBlockEntity>> NEXUS_BE = BLOCK_ENTITIES.register("ethereal_nexus",
            () -> new BlockEntityType<>(EtherealNexusBlockEntity::new, ETHEREAL_NEXUS.get()));
    public static final Supplier<BlockEntityType<EtherealEndpointBlockEntity>> NODE_BE = BLOCK_ENTITIES.register("ethereal_node",
            () -> new BlockEntityType<>((pos, state) -> new EtherealEndpointBlockEntity(EtherContent.NODE_BE.get(), pos, state, false),
                    ETHEREAL_NODE.get()));
    public static final Supplier<BlockEntityType<EtherealEndpointBlockEntity>> RECEIVER_BE = BLOCK_ENTITIES.register("ethereal_receiver",
            () -> new BlockEntityType<>((pos, state) -> new EtherealEndpointBlockEntity(EtherContent.RECEIVER_BE.get(), pos, state, true),
                    ETHEREAL_RECEIVER.get()));

    private EtherContent() {}

    public static List<DeferredItem<? extends Item>> creativeItems() {
        return List.of(ARCANE_TUNER, TRANSMISSION_PYLON_ITEM, ETHEREAL_NEXUS_ITEM, ETHEREAL_NODE_ITEM, ETHEREAL_RECEIVER_ITEM);
    }

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITIES.register(modEventBus);
        COMPONENTS.register(modEventBus);
    }
}

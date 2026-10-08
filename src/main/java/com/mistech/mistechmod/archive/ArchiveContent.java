package com.mistech.mistechmod.archive;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.item.mithril.GearTier;
import com.mistech.mistechmod.registry.MistechComponents;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.transfer.energy.ItemAccessEnergyHandler;

/**
 * Arquivo Espacial (armazenamento central, sem cabos): Núcleo + Pergaminhos Espaciais, Mesa do Arquivo (grade + craft 3×3),
 * Relé, Selo (recipientes no Arquivo), Boca (importa/exporta) e Espelho (acesso de longe).
 */
public final class ArchiveContent {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Mistech.MODID);
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Mistech.MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Mistech.MODID);

    /** Quanto o pergaminho guarda (só para o tooltip; o conteúdo real está em {@link ArchiveCells}). */
    public record Usage(int types, long items) {
        public static final Codec<Usage> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("types").forGetter(Usage::types), Codec.LONG.fieldOf("items").forGetter(Usage::items)).apply(i, Usage::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, Usage> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Usage::types, ByteBufCodecs.VAR_LONG, Usage::items, Usage::new);
    }

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UUID>> CELL_ID =
            COMPONENTS.registerComponentType("archive_cell", b -> b.persistent(UUIDUtil.CODEC).networkSynchronized(UUIDUtil.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Usage>> CELL_USAGE =
            COMPONENTS.registerComponentType("archive_usage", b -> b.persistent(Usage.CODEC).networkSynchronized(Usage.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GlobalPos>> MIRROR_CORE =
            COMPONENTS.registerComponentType("archive_mirror_core", b -> b.persistent(GlobalPos.CODEC).networkSynchronized(GlobalPos.STREAM_CODEC));

    // ----- blocos
    public static final DeferredBlock<ArchiveCoreBlock> ARCHIVE_CORE = Mistech.BLOCKS.registerBlock("archive_core", ArchiveCoreBlock::new,
            p -> p.mapColor(MapColor.COLOR_PURPLE).requiresCorrectToolForDrops().strength(4.0F, 9.0F).sound(SoundType.AMETHYST)
                    .lightLevel(s -> 9).pushReaction(PushReaction.BLOCK));
    public static final DeferredItem<BlockItem> ARCHIVE_CORE_ITEM = Mistech.ITEMS.registerSimpleBlockItem("archive_core", ARCHIVE_CORE,
            p -> p.rarity(Rarity.RARE));
    public static final DeferredBlock<ArchiveTerminalBlock> ARCHIVE_TERMINAL = Mistech.BLOCKS.registerBlock("archive_terminal",
            ArchiveTerminalBlock::new, p -> p.mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD).noOcclusion().lightLevel(s -> 6));
    public static final DeferredItem<BlockItem> ARCHIVE_TERMINAL_ITEM = Mistech.ITEMS.registerSimpleBlockItem("archive_terminal",
            ARCHIVE_TERMINAL, p -> p.rarity(Rarity.UNCOMMON));
    public static final DeferredBlock<ArchiveRelayBlock> ARCHIVE_RELAY = Mistech.BLOCKS.registerBlock("archive_relay", ArchiveRelayBlock::new,
            p -> p.mapColor(MapColor.COLOR_PURPLE).requiresCorrectToolForDrops().strength(2.0F).sound(SoundType.AMETHYST).noOcclusion()
                    .lightLevel(s -> 7));
    public static final DeferredItem<BlockItem> ARCHIVE_RELAY_ITEM = Mistech.ITEMS.registerSimpleBlockItem("archive_relay", ARCHIVE_RELAY);
    public static final DeferredBlock<ArchiveSealBlock> ARCHIVE_SEAL = Mistech.BLOCKS.registerBlock("archive_seal", ArchiveSealBlock::new,
            p -> p.mapColor(MapColor.COLOR_PURPLE).strength(0.5F).sound(SoundType.AMETHYST).noOcclusion().lightLevel(s -> 4)
                    .pushReaction(PushReaction.DESTROY));
    public static final DeferredItem<BlockItem> ARCHIVE_SEAL_ITEM = Mistech.ITEMS.registerSimpleBlockItem("archive_seal", ARCHIVE_SEAL);
    public static final DeferredBlock<ArchivePortBlock> ARCHIVE_PORT = Mistech.BLOCKS.registerBlock("archive_port", ArchivePortBlock::new,
            p -> p.mapColor(MapColor.COLOR_PURPLE).strength(1.0F).sound(SoundType.AMETHYST).noOcclusion().lightLevel(s -> 5)
                    .pushReaction(PushReaction.DESTROY));
    public static final DeferredItem<BlockItem> ARCHIVE_PORT_ITEM = Mistech.ITEMS.registerSimpleBlockItem("archive_port", ARCHIVE_PORT);

    // ----- itens
    public static final List<DeferredItem<ArchiveScrollItem>> SCROLLS = new ArrayList<>();
    static {
        for (ScrollTier tier : ScrollTier.values()) {
            SCROLLS.add(Mistech.ITEMS.registerItem(tier.id, p -> new ArchiveScrollItem(p, tier),
                    p -> p.stacksTo(1).rarity(tier == ScrollTier.ORICHALCUM || tier == ScrollTier.VOID ? Rarity.EPIC : Rarity.UNCOMMON)));
        }
    }
    public static final DeferredItem<ArchiveMirrorItem> ARCHIVE_MIRROR = Mistech.ITEMS.registerItem("archive_mirror", ArchiveMirrorItem::new,
            p -> p.stacksTo(1).rarity(Rarity.RARE));

    // ----- block entities e tela
    public static final Supplier<BlockEntityType<ArchiveCoreBlockEntity>> CORE_BE = BLOCK_ENTITIES.register("archive_core",
            () -> new BlockEntityType<>(ArchiveCoreBlockEntity::new, ARCHIVE_CORE.get()));
    public static final Supplier<BlockEntityType<ArchiveMemberBlockEntity>> RELAY_BE = BLOCK_ENTITIES.register("archive_relay",
            () -> new BlockEntityType<>((pos, state) -> new ArchiveMemberBlockEntity(ArchiveContent.RELAY_BE.get(), pos, state,
                    ArchiveRegistry.Kind.RELAY), ARCHIVE_RELAY.get()));
    public static final Supplier<BlockEntityType<ArchiveMemberBlockEntity>> SEAL_BE = BLOCK_ENTITIES.register("archive_seal",
            () -> new BlockEntityType<>((pos, state) -> new ArchiveMemberBlockEntity(ArchiveContent.SEAL_BE.get(), pos, state,
                    ArchiveRegistry.Kind.SEAL), ARCHIVE_SEAL.get()));
    public static final Supplier<BlockEntityType<ArchivePortBlockEntity>> PORT_BE = BLOCK_ENTITIES.register("archive_port",
            () -> new BlockEntityType<>(ArchivePortBlockEntity::new, ARCHIVE_PORT.get()));
    public static final Supplier<MenuType<ArchiveTerminalMenu>> TERMINAL_MENU = MENUS.register("archive_terminal",
            () -> new MenuType<>(ArchiveTerminalMenu::new, FeatureFlags.VANILLA_SET));

    private ArchiveContent() {}

    public static List<DeferredItem<? extends Item>> creativeItems() {
        List<DeferredItem<? extends Item>> list = new ArrayList<>(List.of(ARCHIVE_CORE_ITEM, ARCHIVE_TERMINAL_ITEM, ARCHIVE_RELAY_ITEM,
                ARCHIVE_SEAL_ITEM, ARCHIVE_PORT_ITEM, ARCHIVE_MIRROR));
        list.addAll(SCROLLS);
        return list;
    }

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITIES.register(modEventBus);
        COMPONENTS.register(modEventBus);
        MENUS.register(modEventBus);
        modEventBus.addListener(ArchiveContent::onCapabilities);
    }

    private static void onCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Energy.BLOCK, CORE_BE.get(), (core, side) -> core.getEnergyHandler());
        event.registerItem(Capabilities.Energy.ITEM, (stack, access) -> new ItemAccessEnergyHandler(access, MistechComponents.GEAR_ENERGY.get(),
                GearTier.MITHRIL.capacity(), GearTier.MITHRIL.chargeRate(), 0), ARCHIVE_MIRROR.get());
    }
}

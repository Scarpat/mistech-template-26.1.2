package com.mistech.mistechmod.beyond;

import java.util.List;
import java.util.function.Supplier;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.registry.GnomeContent;
import com.mistech.mistechmod.registry.RitualContent;
import com.mojang.serialization.Codec;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper;

/**
 * Jardim Pálido e Nether: Runa da Vigília, Runa das Almas, Selamento em Resina, Incenso de Olho-florido, Giz Pálido,
 * Sentinela Rangente, Essência Ígnea, Fragmento Choroso, Pacto Piglin, Strider Rúnico, Ferramentas Infernais,
 * Gerador de Blaze e Âncora de Portal.
 */
public final class PaleNetherContent {
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Mistech.MODID);
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Mistech.MODID);

    /** Mobs do Nether que a Runa das Almas (Véu das Almas) acalma. */
    public static final TagKey<EntityType<?>> SOUL_VEIL_IGNORES = TagKey.create(Registries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(Mistech.MODID, "soul_veil_ignores"));

    // ----- componentes e dados -----
    /** Runa selada em resina: não gasta usos, mas ganha uma recarga longa depois de cada uso. */
    public static final DeferredHolder<net.minecraft.core.component.DataComponentType<?>, net.minecraft.core.component.DataComponentType<Boolean>> AMBER_SEALED =
            COMPONENTS.registerComponentType("amber_sealed", b -> b.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));
    /** Pacto Piglin: gameTime até quando os piglins ficam neutros (mantido ao morrer). */
    public static final Supplier<AttachmentType<Long>> PIGLIN_PACT = ATTACHMENTS.register("piglin_pact",
            () -> AttachmentType.builder(() -> 0L).serialize(Codec.LONG.fieldOf("until")).copyOnDeath().build());
    /** Âncora de Portal a que o jogador está vinculado (dimensão:x,y,z; vazio = nenhuma). */
    public static final Supplier<AttachmentType<String>> PORTAL_ANCHOR = ATTACHMENTS.register("portal_anchor",
            () -> AttachmentType.builder(() -> "").serialize(Codec.STRING.fieldOf("pos")).copyOnDeath().build());

    // ----- runas -----
    public static final DeferredItem<VigilRuneItem> VIGIL_RUNE = Mistech.ITEMS.registerItem("vigil_rune", VigilRuneItem::new,
            p -> p.durability(Mistech.RUNE_USES));
    public static final DeferredItem<SoulRuneItem> SOUL_RUNE = Mistech.ITEMS.registerItem("soul_rune", SoulRuneItem::new,
            p -> p.durability(Mistech.RUNE_USES));

    // ----- Jardim Pálido -----
    public static final DeferredItem<EyeblossomIncenseItem> EYEBLOSSOM_INCENSE = Mistech.ITEMS.registerItem("eyeblossom_incense",
            EyeblossomIncenseItem::new);
    public static final DeferredItem<FlowerIncenseItem> WITHER_ROSE_INCENSE = Mistech.ITEMS.registerItem("wither_rose_incense",
            p -> new FlowerIncenseItem(p, FlowerIncenseItem.Flower.WITHER_ROSE));
    public static final DeferredItem<FlowerIncenseItem> TORCHFLOWER_INCENSE = Mistech.ITEMS.registerItem("torchflower_incense",
            p -> new FlowerIncenseItem(p, FlowerIncenseItem.Flower.TORCHFLOWER));
    public static final DeferredItem<FlowerIncenseItem> LILY_INCENSE = Mistech.ITEMS.registerItem("lily_incense",
            p -> new FlowerIncenseItem(p, FlowerIncenseItem.Flower.LILY));
    public static final DeferredItem<Item> PALE_CHALK = Mistech.ITEMS.registerSimpleItem("pale_chalk", p -> p.durability(300));
    public static final DeferredHolder<EntityType<?>, EntityType<PaleSentinelEntity>> PALE_SENTINEL = GnomeContent.ENTITIES.registerEntityType(
            "pale_sentinel", PaleSentinelEntity::new, MobCategory.MISC, b -> b.sized(0.9F, 2.7F).eyeHeight(2.3F).clientTrackingRange(8));

    // ----- Nether -----
    public static final DeferredItem<Item> IGNEOUS_ESSENCE_BOTTLE = Mistech.ITEMS.registerSimpleItem("igneous_essence_bottle",
            p -> p.stacksTo(16).fireResistant().craftRemainder(net.minecraft.world.item.Items.GLASS_BOTTLE));
    public static final DeferredItem<Item> CRYING_SHARD = Mistech.ITEMS.registerSimpleItem("crying_shard",
            p -> p.rarity(Rarity.UNCOMMON).fireResistant());
    public static final DeferredHolder<EntityType<?>, EntityType<RunicStriderEntity>> RUNIC_STRIDER = GnomeContent.ENTITIES.registerEntityType(
            "runic_strider", RunicStriderEntity::new, MobCategory.CREATURE, b -> b.sized(0.9F, 1.7F).fireImmune().clientTrackingRange(10));

    /** Ferramentas Infernais: as impuras revestidas de netherita. */
    public static final ToolMaterial INFERNAL = new ToolMaterial(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 2500, 9.5F, 4.5F, 18,
            RitualContent.IMPURE_TOOL_MATERIALS);
    public static final DeferredItem<com.mistech.mistechmod.item.ritual.RitualDaggerItem> INFERNAL_DAGGER = Mistech.ITEMS.registerItem("infernal_dagger",
            com.mistech.mistechmod.item.ritual.RitualDaggerItem::new, p -> p.sword(INFERNAL, 2.0F, -2.0F).rarity(Rarity.RARE).fireResistant());
    public static final DeferredItem<Item> INFERNAL_PICKAXE = Mistech.ITEMS.registerItem("infernal_pickaxe", Item::new,
            p -> p.pickaxe(INFERNAL, 1.0F, -2.8F).rarity(Rarity.RARE).fireResistant());
    public static final DeferredItem<AxeItem> INFERNAL_AXE = Mistech.ITEMS.registerItem("infernal_axe",
            p -> new AxeItem(INFERNAL, 5.0F, -3.0F, p.rarity(Rarity.RARE).fireResistant()));
    public static final DeferredItem<com.mistech.mistechmod.item.ritual.ImpureHoeItem> INFERNAL_HOE = Mistech.ITEMS.registerItem("infernal_hoe",
            p -> new com.mistech.mistechmod.item.ritual.ImpureHoeItem(INFERNAL, -4.5F, 0.0F, p.rarity(Rarity.RARE).fireResistant(), 2));

    public static final DeferredBlock<BlazeGeneratorBlock> BLAZE_GENERATOR = Mistech.BLOCKS.registerBlock("blaze_generator", BlazeGeneratorBlock::new,
            p -> p.mapColor(MapColor.COLOR_BLACK).requiresCorrectToolForDrops().strength(3.5F).sound(SoundType.POLISHED_DEEPSLATE).noOcclusion()
                    .lightLevel(state -> state.getValue(com.mistech.mistechmod.machine.MachineBlock.LIT) ? 13 : 0));
    public static final DeferredItem<BlockItem> BLAZE_GENERATOR_ITEM = Mistech.ITEMS.registerSimpleBlockItem("blaze_generator", BLAZE_GENERATOR);
    public static final Supplier<BlockEntityType<BlazeGeneratorBlockEntity>> BLAZE_GENERATOR_BE = BeyondContent.BLOCK_ENTITIES.register("blaze_generator",
            () -> new BlockEntityType<>(BlazeGeneratorBlockEntity::new, BLAZE_GENERATOR.get()));

    public static final DeferredBlock<PortalAnchorBlock> PORTAL_ANCHOR_BLOCK = Mistech.BLOCKS.registerBlock("portal_anchor", PortalAnchorBlock::new,
            p -> p.mapColor(MapColor.COLOR_PURPLE).requiresCorrectToolForDrops().strength(25.0F, 1200.0F).sound(SoundType.STONE)
                    .lightLevel(state -> 10));
    public static final DeferredItem<BlockItem> PORTAL_ANCHOR_ITEM = Mistech.ITEMS.registerSimpleBlockItem("portal_anchor", PORTAL_ANCHOR_BLOCK,
            p -> p.rarity(Rarity.UNCOMMON));

    public static final DeferredBlock<RunicNoteBlock> RUNIC_NOTE_BLOCK = Mistech.BLOCKS.registerBlock("runic_note_block", RunicNoteBlock::new,
            p -> p.mapColor(MapColor.COLOR_CYAN).strength(1.0F).sound(SoundType.SCULK_SENSOR));
    public static final DeferredItem<BlockItem> RUNIC_NOTE_BLOCK_ITEM = Mistech.ITEMS.registerSimpleBlockItem("runic_note_block", RUNIC_NOTE_BLOCK,
            p -> p.rarity(Rarity.UNCOMMON));

    private PaleNetherContent() {}

    public static List<DeferredItem<? extends Item>> creativeItems() {
        return List.of(VIGIL_RUNE, SOUL_RUNE, EYEBLOSSOM_INCENSE, WITHER_ROSE_INCENSE, TORCHFLOWER_INCENSE, LILY_INCENSE, PALE_CHALK, IGNEOUS_ESSENCE_BOTTLE, CRYING_SHARD,
                INFERNAL_DAGGER, INFERNAL_PICKAXE, INFERNAL_AXE, INFERNAL_HOE, BLAZE_GENERATOR_ITEM, PORTAL_ANCHOR_ITEM, RUNIC_NOTE_BLOCK_ITEM);
    }

    static void register(IEventBus modEventBus) {
        COMPONENTS.register(modEventBus);
        ATTACHMENTS.register(modEventBus);
        modEventBus.addListener(PaleNetherContent::onAttributes);
        modEventBus.addListener(PaleNetherContent::onCapabilities);
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(PALE_SENTINEL.get(), PaleSentinelEntity.createAttributes().build());
        event.put(RUNIC_STRIDER.get(), Strider.createAttributes().build());
    }

    private static void onCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Energy.BLOCK, BLAZE_GENERATOR_BE.get(), (machine, side) -> machine.getEnergyHandler());
        event.registerBlockEntity(Capabilities.Item.BLOCK, BLAZE_GENERATOR_BE.get(), (machine, side) -> new WorldlyContainerWrapper(machine, side));
    }
}

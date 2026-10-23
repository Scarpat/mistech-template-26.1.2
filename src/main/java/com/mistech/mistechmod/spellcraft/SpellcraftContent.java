package com.mistech.mistechmod.spellcraft;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.item.RuneElement;
import com.mojang.serialization.Codec;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Sistema de magias: mana do jogador, magias que se aprendem com pergaminhos (achados no mundo ou inscritos na
 * Mesa de Inscrição), Tomos para lançar. Ver {@link Spells}.
 */
public final class SpellcraftContent {
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Mistech.MODID);
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Mistech.MODID);

    // ----- componentes -----
    /** Id da magia de um pergaminho. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> SPELL_ID =
            COMPONENTS.registerComponentType("spell_id", b -> b.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));
    /** Espaço escolhido no Tomo/Varinha. */
    /** Mana guardada num Cajado (a reserva é gasta antes da mana do jogador). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> STAFF_MANA =
            COMPONENTS.registerComponentType("staff_mana", b -> b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> TOME_SELECTED =
            COMPONENTS.registerComponentType("tome_selected", b -> b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** Magias nos espaços de um Tomo/Varinha ("" = vazio). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<String>>> SPELL_SLOTS =
            COMPONENTS.registerComponentType("spell_slots", b -> b.persistent(Codec.STRING.listOf())
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list())));

    /** Runas engastadas no Tomo/Varinha/Cajado (ids de elemento). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<String>>> FOCUS_GEMS =
            COMPONENTS.registerComponentType("focus_gems", b -> b.persistent(Codec.STRING.listOf())
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list())));

    /** Mana máxima extra de itens (Coroa de Éter). Calculada no servidor e sincronizada com o dono para a barra de mana. */
    public static final Supplier<AttachmentType<Float>> MANA_BONUS = ATTACHMENTS.register("mana_bonus",
            () -> AttachmentType.builder(() -> 0.0F).sync((holder, to) -> holder == to, ByteBufCodecs.FLOAT).build());
    /** Bênção do Círculo de Pedras Rúnico (mantém ao morrer): a marca de ressonância dura 8 s em vez de 5. */
    public static final Supplier<AttachmentType<Boolean>> CIRCLE_BLESSED = ATTACHMENTS.register("circle_blessed",
            () -> AttachmentType.builder(() -> false).serialize(Codec.BOOL.fieldOf("blessed")).copyOnDeath().build());

    // ----- dados do jogador -----
    /** Mana (mantém ao morrer; sincroniza só com o dono). */
    public static final Supplier<AttachmentType<Float>> MANA = ATTACHMENTS.register("mana",
            () -> AttachmentType.builder(() -> ManaService.BASE_MAX)
                    .serialize(Codec.FLOAT.fieldOf("mana"))
                    .copyOnDeath()
                    .sync((holder, to) -> holder == to, ByteBufCodecs.FLOAT)
                    .build());
    /** Magias aprendidas (mantém ao morrer; sincroniza só com o dono). */
    public static final Supplier<AttachmentType<SpellBook>> SPELL_BOOK = ATTACHMENTS.register("spell_book",
            () -> AttachmentType.builder(() -> SpellBook.EMPTY)
                    .serialize(SpellBook.CODEC.fieldOf("learned"))
                    .copyOnDeath()
                    .sync((holder, to) -> holder == to, SpellBook.STREAM_CODEC)
                    .build());
    /** Marca de ressonância ("elemento|tick_de_fim"); só na memória, sincroniza com o dono para o HUD. */
    public static final Supplier<AttachmentType<String>> RESONANCE = ATTACHMENTS.register("resonance",
            () -> AttachmentType.builder(() -> "")
                    .sync((holder, to) -> holder == to, ByteBufCodecs.STRING_UTF8)
                    .build());
    /** Recargas (id da magia → tick em que libera). Só na memória do servidor. */
    public static final Supplier<AttachmentType<Map<String, Long>>> COOLDOWNS = ATTACHMENTS.register("spell_cooldowns",
            () -> AttachmentType.<Map<String, Long>>builder(() -> new HashMap<>()).build());

    // ----- itens e blocos -----
    public static final DeferredItem<SpellScrollItem> SPELL_SCROLL = Mistech.ITEMS.registerItem("spell_scroll", SpellScrollItem::new,
            p -> p.stacksTo(16).rarity(Rarity.UNCOMMON));
    /** Tomos e Varinhas: espaços, tier máximo e bônus por elemento (ver {@link SpellFocusItem.Focus}). */
    public static final DeferredItem<SpellFocusItem> APPRENTICE_TOME = focus("apprentice_tome", Rarity.UNCOMMON, new SpellFocusItem.Focus(3, 2));
    public static final DeferredItem<SpellFocusItem> ADEPT_TOME = focus("adept_tome", Rarity.RARE, new SpellFocusItem.Focus(4, 3));
    public static final DeferredItem<SpellFocusItem> ARCHMAGE_TOME = focus("archmage_tome", Rarity.EPIC, new SpellFocusItem.Focus(5, 5));
    /** Exemplo de Tomo especializado: Fogo −10% de mana, Gelo +10%. */
    public static final DeferredItem<SpellFocusItem> FLAME_TOME = focus("flame_tome", Rarity.RARE,
            new SpellFocusItem.Focus(4, 3, Map.of("fire", 0.9, "ice", 1.1), 1.0, 0, 0.0, 1.0, "fire"));
    /** Tomos de elemento: 4 espaços, até tier 4; −20% de mana no elemento, +20% no oposto, ressonância reforçada. */
    public static final DeferredItem<SpellFocusItem> ICE_TOME = focus("ice_tome", Rarity.RARE, SpellFocusItem.Focus.tome(4, 4, RuneElement.ICE));
    public static final DeferredItem<SpellFocusItem> WATER_TOME = focus("water_tome", Rarity.RARE, SpellFocusItem.Focus.tome(4, 4, RuneElement.WATER));
    public static final DeferredItem<SpellFocusItem> EARTH_TOME = focus("earth_tome", Rarity.RARE, SpellFocusItem.Focus.tome(4, 4, RuneElement.EARTH));
    public static final DeferredItem<SpellFocusItem> AIR_TOME = focus("air_tome", Rarity.RARE, SpellFocusItem.Focus.tome(4, 4, RuneElement.AIR));
    public static final DeferredItem<SpellFocusItem> ENERGY_TOME = focus("energy_tome", Rarity.RARE, SpellFocusItem.Focus.tome(4, 4, RuneElement.ENERGY));
    public static final DeferredItem<SpellFocusItem> LIGHT_TOME = focus("light_tome", Rarity.RARE, SpellFocusItem.Focus.tome(4, 4, RuneElement.LIGHT));
    public static final DeferredItem<SpellFocusItem> DARKNESS_TOME = focus("darkness_tome", Rarity.RARE, SpellFocusItem.Focus.tome(4, 4, RuneElement.DARKNESS));
    public static final DeferredItem<SpellFocusItem> TIME_TOME = focus("time_tome", Rarity.RARE, SpellFocusItem.Focus.tome(4, 4, RuneElement.TIME));
    public static final DeferredItem<SpellFocusItem> SPACE_TOME = focus("space_tome", Rarity.RARE, SpellFocusItem.Focus.tome(4, 4, RuneElement.SPACE));
    public static final DeferredItem<SpellFocusItem> LIFE_TOME = focus("life_tome", Rarity.RARE, SpellFocusItem.Focus.tome(4, 4, RuneElement.LIFE));

    /** Varinhas por material: quanto melhor o metal, mais rápida a recarga. */
    public static final DeferredItem<SpellFocusItem> COPPER_WAND = focus("copper_wand", Rarity.UNCOMMON, new SpellFocusItem.Focus(2, 3, Map.of(), 0.75));
    public static final DeferredItem<SpellFocusItem> GOLD_WAND = focus("gold_wand", Rarity.RARE, new SpellFocusItem.Focus(3, 3, Map.of(), 0.65));
    public static final DeferredItem<SpellFocusItem> MITHRIL_WAND = focus("mithril_wand", Rarity.RARE, new SpellFocusItem.Focus(3, 4, Map.of(), 0.5));

    /** Varinha: menos espaços, recarga mais curta. */
    public static final DeferredItem<SpellFocusItem> APPRENTICE_WAND = focus("apprentice_wand", Rarity.UNCOMMON,
            new SpellFocusItem.Focus(2, 2, Map.of(), 0.8));

    /** Tomo Proibido (T6): dispensa a afinidade exigida, mas cobra sangue — cada magia custa vida (tier ÷ 2 corações). Só achado na Biblioteca Proibida. */
    public static final DeferredItem<SpellFocusItem> FORBIDDEN_TOME = focus("forbidden_tome", Rarity.EPIC,
            new SpellFocusItem.Focus(5, 6, Map.of(), 1.0, 0, 0.15, 1.0, SpellFocusItem.FORBIDDEN));

    /** Varinha do Vazio (única): 3 espaços, tier 6, recarga −60%, +10% de poder. */
    public static final DeferredItem<SpellFocusItem> VOID_WAND = focus("void_wand", Rarity.EPIC,
            new SpellFocusItem.Focus(3, 6, Map.of(), 0.4, 0, 0.1, 1.0, ""));
    /** Coroa de Éter (única, espaço de amuleto): +150 de mana máxima e regeneração ×1,75. */
    public static final DeferredItem<com.mistech.mistechmod.accessory.AccessoryItem> AETHER_CROWN = Mistech.ITEMS.registerItem("aether_crown",
            p -> new com.mistech.mistechmod.accessory.AccessoryItem(p, com.mistech.mistechmod.accessory.Accessory.AETHER_CROWN),
            p -> p.stacksTo(1).rarity(Rarity.EPIC).fireResistant());

    /** Cajados: poucos espaços mas tier alto, reserva de mana própria, mais poder e pulsos contínuos mais baratos. */
    public static final DeferredItem<SpellFocusItem> WOODEN_STAFF = focus("wooden_staff", Rarity.UNCOMMON, SpellFocusItem.Focus.staff(2, 3, 100, 0.0, 0.9));
    public static final DeferredItem<SpellFocusItem> RUNIC_STAFF = focus("runic_staff", Rarity.RARE, SpellFocusItem.Focus.staff(3, 4, 300, 0.1, 0.8));
    public static final DeferredItem<SpellFocusItem> MITHRIL_STAFF = focus("mithril_staff", Rarity.RARE, SpellFocusItem.Focus.staff(3, 5, 800, 0.2, 0.7));
    /** Único: só achado no mundo (sem receita). */
    public static final DeferredItem<SpellFocusItem> ARCHMAGE_STAFF = focus("archmage_staff", Rarity.EPIC, SpellFocusItem.Focus.staff(4, 6, 2000, 0.3, 0.6));

    private static DeferredItem<SpellFocusItem> focus(String name, Rarity rarity, SpellFocusItem.Focus focus) {
        return Mistech.ITEMS.registerItem(name, p -> new SpellFocusItem(p, focus), p -> p.stacksTo(1).rarity(rarity).component(net.minecraft.core.component.DataComponents.USE_EFFECTS, new net.minecraft.world.item.component.UseEffects(true, false, 0.7F)));
    }

    public static final DeferredBlock<InscriptionTableBlock> INSCRIPTION_TABLE = Mistech.BLOCKS.registerBlock("inscription_table", InscriptionTableBlock::new,
            p -> p.mapColor(MapColor.COLOR_CYAN).strength(2.5F).sound(SoundType.WOOD).noOcclusion().lightLevel(s -> 5));
    public static final DeferredItem<BlockItem> INSCRIPTION_TABLE_ITEM = Mistech.ITEMS.registerSimpleBlockItem("inscription_table", INSCRIPTION_TABLE,
            p -> p.rarity(Rarity.UNCOMMON));

    public static final DeferredBlock<ArcaneChargerBlock> ARCANE_CHARGER = Mistech.BLOCKS.registerBlock("arcane_charger", ArcaneChargerBlock::new,
            p -> p.mapColor(MapColor.COLOR_PURPLE).requiresCorrectToolForDrops().strength(3.0F).sound(SoundType.AMETHYST)
                    .lightLevel(s -> s.getValue(ArcaneChargerBlock.FILLED) ? 8 : 2));
    public static final DeferredItem<BlockItem> ARCANE_CHARGER_ITEM = Mistech.ITEMS.registerSimpleBlockItem("arcane_charger", ARCANE_CHARGER,
            p -> p.rarity(Rarity.UNCOMMON));
    public static final DeferredRegister<net.minecraft.world.level.block.entity.BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Mistech.MODID);
    public static final Supplier<net.minecraft.world.level.block.entity.BlockEntityType<ArcaneChargerBlockEntity>> ARCANE_CHARGER_BE =
            BLOCK_ENTITIES.register("arcane_charger", () -> new net.minecraft.world.level.block.entity.BlockEntityType<>(ArcaneChargerBlockEntity::new, ARCANE_CHARGER.get()));

    public static final DeferredBlock<RuneAltarBlock> RUNE_ALTAR = Mistech.BLOCKS.registerBlock("rune_altar", RuneAltarBlock::new,
            p -> p.mapColor(MapColor.STONE).strength(-1.0F, 3600000.0F).sound(SoundType.STONE).lightLevel(s -> 7).noLootTable());
    public static final DeferredItem<BlockItem> RUNE_ALTAR_ITEM = Mistech.ITEMS.registerSimpleBlockItem("rune_altar", RUNE_ALTAR, p -> p.rarity(Rarity.RARE));

    public static final DeferredBlock<LightOrbBlock> LIGHT_ORB = Mistech.BLOCKS.registerBlock("light_orb", LightOrbBlock::new,
            p -> p.mapColor(MapColor.NONE).noCollision().instabreak().noLootTable().lightLevel(s -> 15).sound(SoundType.AMETHYST)
                    .replaceable().noOcclusion());

    // ----- invocações (entidades próprias) -----
    public static final DeferredHolder<net.minecraft.world.entity.EntityType<?>, net.minecraft.world.entity.EntityType<SummonedGolemEntity>> STONE_GOLEM =
            com.mistech.mistechmod.registry.GnomeContent.ENTITIES.registerEntityType("stone_golem", SummonedGolemEntity::new,
                    net.minecraft.world.entity.MobCategory.MISC, b -> b.sized(1.4F, 2.7F).clientTrackingRange(10));
    public static final DeferredHolder<net.minecraft.world.entity.EntityType<?>, net.minecraft.world.entity.EntityType<SummonedGolemEntity>> WOOD_GUARDIAN =
            com.mistech.mistechmod.registry.GnomeContent.ENTITIES.registerEntityType("wood_guardian", SummonedGolemEntity::new,
                    net.minecraft.world.entity.MobCategory.MISC, b -> b.sized(1.2F, 2.9F).clientTrackingRange(10));
    public static final DeferredHolder<net.minecraft.world.entity.EntityType<?>, net.minecraft.world.entity.EntityType<ShadowWolfEntity>> SHADOW_WOLF =
            com.mistech.mistechmod.registry.GnomeContent.ENTITIES.registerEntityType("shadow_wolf", ShadowWolfEntity::new,
                    net.minecraft.world.entity.MobCategory.MISC, b -> b.sized(0.75F, 1.05F).eyeHeight(0.85F).clientTrackingRange(10));

    public static final DeferredHolder<net.minecraft.world.entity.EntityType<?>, net.minecraft.world.entity.EntityType<SummonFlyerEntity>> SPIRIT_GUARDIAN =
            com.mistech.mistechmod.registry.GnomeContent.ENTITIES.registerEntityType("spirit_guardian", SummonFlyerEntity::new,
                    net.minecraft.world.entity.MobCategory.MISC, b -> b.sized(0.4F, 0.8F).eyeHeight(0.51F).clientTrackingRange(10));
    public static final DeferredHolder<net.minecraft.world.entity.EntityType<?>, net.minecraft.world.entity.EntityType<SummonFlyerEntity>> PHOENIX =
            com.mistech.mistechmod.registry.GnomeContent.ENTITIES.registerEntityType("phoenix", SummonFlyerEntity::new,
                    net.minecraft.world.entity.MobCategory.MISC, b -> b.sized(0.8F, 1.2F).eyeHeight(0.8F).fireImmune().clientTrackingRange(10));

    private static void onAttributes(net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent event) {
        event.put(STONE_GOLEM.get(), net.minecraft.world.entity.animal.golem.IronGolem.createAttributes().build());
        event.put(WOOD_GUARDIAN.get(), net.minecraft.world.entity.animal.golem.IronGolem.createAttributes().build());
        event.put(SHADOW_WOLF.get(), net.minecraft.world.entity.animal.wolf.Wolf.createAttributes().build());
        event.put(SPIRIT_GUARDIAN.get(), net.minecraft.world.entity.monster.Vex.createAttributes().build());
        event.put(PHOENIX.get(), net.minecraft.world.entity.monster.Vex.createAttributes().build());
    }

    // ----- estruturas únicas: Torre do Arquimago e Santuários de Elemento -----
    public static final DeferredRegister<net.minecraft.world.level.levelgen.structure.StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, Mistech.MODID);
    public static final DeferredRegister<net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType> PIECE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, Mistech.MODID);
    public static final DeferredHolder<net.minecraft.world.level.levelgen.structure.StructureType<?>, net.minecraft.world.level.levelgen.structure.StructureType<ArcaneRuinStructure>> RUIN_TYPE =
            STRUCTURE_TYPES.register("arcane_ruin", () -> () -> ArcaneRuinStructure.CODEC);
    public static final DeferredHolder<net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType, net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType> RUIN_PIECE =
            PIECE_TYPES.register("arcane_ruin", () -> (net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType) ArcaneRuinPiece::new);

    private SpellcraftContent() {}

    public static List<DeferredItem<? extends Item>> creativeItems() {
        return List.of(INSCRIPTION_TABLE_ITEM, ARCANE_CHARGER_ITEM, APPRENTICE_TOME, ADEPT_TOME, ARCHMAGE_TOME, FORBIDDEN_TOME, VOID_WAND, AETHER_CROWN, RUNE_ALTAR_ITEM, FLAME_TOME, APPRENTICE_WAND, COPPER_WAND, GOLD_WAND, MITHRIL_WAND, ICE_TOME,WATER_TOME,EARTH_TOME,AIR_TOME,ENERGY_TOME,LIGHT_TOME,DARKNESS_TOME,TIME_TOME,SPACE_TOME,LIFE_TOME, WOODEN_STAFF, RUNIC_STAFF, MITHRIL_STAFF, ARCHMAGE_STAFF);
    }

    /** Um pergaminho de cada magia (aba criativa). */
    public static List<ItemStack> creativeStacks() {
        List<ItemStack> list = new ArrayList<>();
        for (Spell spell : Spells.sorted(Spells.all())) {
            list.add(spell.scroll());
        }
        return list;
    }

    public static void register(IEventBus modEventBus) {
        COMPONENTS.register(modEventBus);
        ATTACHMENTS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        com.mistech.mistechmod.spellcraft.fx.VfxContent.register(modEventBus);
        STRUCTURE_TYPES.register(modEventBus);
        modEventBus.addListener(SpellcraftContent::onAttributes);
        PIECE_TYPES.register(modEventBus);
    }
}

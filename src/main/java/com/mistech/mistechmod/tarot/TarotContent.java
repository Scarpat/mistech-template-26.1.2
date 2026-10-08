package com.mistech.mistechmod.tarot;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.mistech.mistechmod.Mistech;
import com.mojang.serialization.Codec;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Tarô: Mesa de Tarologia (Mesa de Escrita + Pedra Guia sobre o Círculo de Tarologia), Cartas de Tarô (em branco,
 * invertidas e dos arcanos), Fenda Arcana (portal) e o Plano Arcano (arena). Ver {@link TarotArcana}.
 */
public final class TarotContent {
    public static final DeferredRegister<net.minecraft.world.level.block.entity.BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Mistech.MODID);
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Mistech.MODID);
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Mistech.MODID);

    /** Dimensão das lutas contra os arcanos (data/mistech/dimension/arcane_plane.json). */
    public static final ResourceKey<Level> ARCANE_PLANE =
            ResourceKey.create(Registries.DIMENSION, Identifier.fromNamespaceAndPath(Mistech.MODID, "arcane_plane"));

    // ----- blocos -----
    public static final DeferredBlock<TarotTableBlock> TAROT_TABLE = Mistech.BLOCKS.registerBlock("tarot_table", TarotTableBlock::new,
            p -> p.mapColor(MapColor.COLOR_PURPLE).strength(2.5F).sound(SoundType.WOOD).noOcclusion().lightLevel(s -> 6));
    public static final DeferredItem<BlockItem> TAROT_TABLE_ITEM = Mistech.ITEMS.registerSimpleBlockItem("tarot_table", TAROT_TABLE,
            p -> p.rarity(Rarity.RARE));

    /** Fenda Arcana: portal de 2 blocos aberto pela Carta Invertida. Não dá para quebrar; fecha sozinha. */
    public static final DeferredBlock<ArcanaRiftBlock> ARCANA_RIFT = Mistech.BLOCKS.registerBlock("arcana_rift", ArcanaRiftBlock::new,
            p -> p.mapColor(MapColor.COLOR_PURPLE).strength(-1.0F, 3600000.0F).noCollision().noOcclusion().noLootTable().sound(SoundType.AMETHYST)
                    .lightLevel(s -> 12).pushReaction(PushReaction.BLOCK));

    // ----- itens -----
    public static final DeferredItem<Item> BLANK_CARD = Mistech.ITEMS.registerSimpleItem("blank_tarot_card",
            p -> p.stacksTo(16).rarity(Rarity.UNCOMMON));
    public static final Map<TarotArcana, DeferredItem<InvertedCardItem>> INVERTED_CARDS = new EnumMap<>(TarotArcana.class);
    public static final Map<TarotArcana, DeferredItem<TarotCardItem>> CARDS = new EnumMap<>(TarotArcana.class);
    static {
        for (TarotArcana arcana : TarotArcana.values()) {
            INVERTED_CARDS.put(arcana, Mistech.ITEMS.registerItem("inverted_tarot_card_" + arcana.id(),
                    p -> new InvertedCardItem(p, arcana), p -> p.stacksTo(16).rarity(Rarity.RARE)));
            CARDS.put(arcana, Mistech.ITEMS.registerItem("tarot_card_" + arcana.id(),
                    p -> new TarotCardItem(p, arcana), p -> p.stacksTo(16).rarity(Rarity.EPIC)));
        }
    }

    // ----- block entities -----
    public static final Supplier<BlockEntityType<ArcanaRiftBlockEntity>> ARCANA_RIFT_BE = BLOCK_ENTITIES.register("arcana_rift",
            () -> new BlockEntityType<>(ArcanaRiftBlockEntity::new, ARCANA_RIFT.get()));

    // ----- dados -----
    /** Arcanos derrotados (por jogador; mantém ao morrer; sincroniza só com o dono). */
    public static final Supplier<AttachmentType<TarotData>> TAROT = ATTACHMENTS.register("tarot",
            () -> AttachmentType.builder(() -> TarotData.EMPTY)
                    .serialize(TarotData.CODEC)
                    .copyOnDeath()
                    .sync((holder, to) -> holder == to, TarotData.STREAM_CODEC)
                    .build());

    /** Arcano vinculado ao equipamento de Oricalco Puro (id de {@link TarotArcana}). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> GEAR_ARCANA =
            COMPONENTS.registerComponentType("gear_arcana",
                    b -> b.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));

    private TarotContent() {}

    public static Item invertedCard(TarotArcana arcana) {
        return INVERTED_CARDS.get(arcana).get();
    }

    public static Item card(TarotArcana arcana) {
        return CARDS.get(arcana).get();
    }

    public static List<DeferredItem<? extends Item>> creativeItems() {
        List<DeferredItem<? extends Item>> list = new ArrayList<>(List.of(TAROT_TABLE_ITEM, BLANK_CARD));
        for (TarotArcana arcana : TarotArcana.values()) {
            list.add(INVERTED_CARDS.get(arcana));
        }
        for (TarotArcana arcana : TarotArcana.values()) {
            list.add(CARDS.get(arcana));
        }
        list.addAll(com.mistech.mistechmod.tarot.fool.FoolContent.creativeItems());
        list.addAll(com.mistech.mistechmod.tarot.magus.MagusContent.creativeItems());
        list.addAll(com.mistech.mistechmod.tarot.priestess.PriestessContent.creativeItems());
        return list;
    }

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITIES.register(modEventBus);
        COMPONENTS.register(modEventBus);
        ATTACHMENTS.register(modEventBus);
        // chefes dos arcanos
        com.mistech.mistechmod.tarot.fool.FoolContent.register(modEventBus);
        com.mistech.mistechmod.tarot.magus.MagusContent.register(modEventBus);
        com.mistech.mistechmod.tarot.priestess.PriestessContent.register(modEventBus);
    }
}

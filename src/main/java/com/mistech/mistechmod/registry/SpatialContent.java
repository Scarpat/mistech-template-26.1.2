package com.mistech.mistechmod.registry;

import java.util.List;
import java.util.function.Supplier;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.spatial.SpatialAnchorBlock;
import com.mistech.mistechmod.spatial.TranslocationOrbItem;
import com.mojang.serialization.Codec;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Viagem espacial: Âncora Espacial (pilar de 2 blocos que funciona como uma waystone, ritual Tier I) e
 * Orbe de Translocação (âncora de bolso, ritual Tier II).
 */
public final class SpatialContent {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Mistech.MODID);

    public static final DeferredBlock<SpatialAnchorBlock> SPATIAL_ANCHOR = Mistech.BLOCKS.registerBlock("spatial_anchor", SpatialAnchorBlock::new,
            p -> p.mapColor(MapColor.COLOR_PURPLE).requiresCorrectToolForDrops().strength(5.0F, 1200.0F).sound(SoundType.POLISHED_DEEPSLATE)
                    .noOcclusion().lightLevel(state -> state.getValue(SpatialAnchorBlock.HALF) == SpatialAnchorBlock.Half.UPPER ? 10 : 2)
                    .pushReaction(PushReaction.BLOCK));
    public static final DeferredItem<BlockItem> SPATIAL_ANCHOR_ITEM = Mistech.ITEMS.registerSimpleBlockItem("spatial_anchor", SPATIAL_ANCHOR,
            p -> p.rarity(Rarity.RARE));

    public static final DeferredItem<TranslocationOrbItem> TRANSLOCATION_ORB = Mistech.ITEMS.registerItem("translocation_orb",
            TranslocationOrbItem::new, p -> p.durability(TranslocationOrbItem.USES).rarity(Rarity.EPIC));

    /** Âncoras que o jogador já tocou (chaves de {@link com.mistech.mistechmod.spatial.SpatialAnchors}). */
    public static final Supplier<AttachmentType<List<String>>> KNOWN_ANCHORS = ATTACHMENTS.register("known_anchors",
            () -> AttachmentType.<List<String>>builder(() -> List.of())
                    .serialize(Codec.STRING.listOf().fieldOf("anchors"))
                    .copyOnDeath()
                    .build());

    private SpatialContent() {}

    public static List<DeferredItem<? extends Item>> creativeItems() {
        return List.of(SPATIAL_ANCHOR_ITEM, TRANSLOCATION_ORB);
    }

    public static void register(IEventBus modEventBus) {
        ATTACHMENTS.register(modEventBus);
    }
}

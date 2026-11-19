package com.mistech.mistechmod.conduit;

import java.util.List;
import java.util.function.Supplier;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.registry.MistechBlockEntities;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

/** Condutos Rúnicos: canos do Mistech para itens, fluidos e energia (ver {@link RunicConduitBlock}). */
public final class ConduitContent {
    private ConduitContent() {}

    public static final java.util.Map<ConduitTier, DeferredBlock<RunicConduitBlock>> BLOCKS = new java.util.EnumMap<>(ConduitTier.class);
    public static final java.util.Map<ConduitTier, DeferredItem<BlockItem>> ITEMS = new java.util.EnumMap<>(ConduitTier.class);

    static {
        for (ConduitTier tier : ConduitTier.values()) {
            MapColor color = switch (tier) {
                case RUNIC -> MapColor.COLOR_ORANGE;
                case MITHRIL -> MapColor.COLOR_LIGHT_BLUE;
                case ADAMANTINE -> MapColor.COLOR_RED;
                case ORICHALCUM -> MapColor.GOLD;
            };
            DeferredBlock<RunicConduitBlock> block = Mistech.BLOCKS.registerBlock(tier.id, p -> new RunicConduitBlock(p, tier),
                    p -> p.mapColor(color).strength(1.5F, 6.0F).sound(SoundType.COPPER).noOcclusion()
                            .isRedstoneConductor((s, l, q) -> false).isSuffocating((s, l, q) -> false).isViewBlocking((s, l, q) -> false));
            BLOCKS.put(tier, block);
            Rarity rarity = tier == ConduitTier.RUNIC ? Rarity.UNCOMMON : tier == ConduitTier.ORICHALCUM ? Rarity.EPIC : Rarity.RARE;
            ITEMS.put(tier, Mistech.ITEMS.registerSimpleBlockItem(tier.id, block, p -> p.rarity(rarity)));
        }
    }

    public static final DeferredBlock<RunicConduitBlock> RUNIC_CONDUIT = BLOCKS.get(ConduitTier.RUNIC);
    public static final DeferredItem<BlockItem> RUNIC_CONDUIT_ITEM = ITEMS.get(ConduitTier.RUNIC);

    public static final Supplier<BlockEntityType<RunicConduitBlockEntity>> RUNIC_CONDUIT_BE = MistechBlockEntities.BLOCK_ENTITY_TYPES.register(
            "runic_conduit", () -> new BlockEntityType<>(RunicConduitBlockEntity::new,
                    BLOCKS.values().stream().map(DeferredBlock::get).toArray(net.minecraft.world.level.block.Block[]::new)));

    public static void init() {}

    public static List<DeferredItem<? extends Item>> creativeItems() {
        return List.copyOf(ITEMS.values());
    }
}

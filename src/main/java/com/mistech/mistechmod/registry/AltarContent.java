package com.mistech.mistechmod.registry;

import java.util.List;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.block.ritual.MithrilObeliskBlock;
import com.mistech.mistechmod.block.ritual.OrichalcumBrazierBlock;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Progressão do Altar Impuro (Tier II e III):
 * Coração Impuro → Coração Desperto (tempestade) → Ascensão Tier II (precisa dos Obeliscos de Mitril);
 * Coração Impuro → Coração Astral (lua nova, altar Tier II) → Ascensão Tier III (precisa dos Pilares Astrais e dos Braseiros de Oricalco acesos).
 */
public final class AltarContent {
    public static final DeferredItem<Item> AWAKENED_HEART = Mistech.ITEMS.registerSimpleItem("awakened_heart",
            p -> p.rarity(Rarity.EPIC).stacksTo(16).fireResistant());
    public static final DeferredItem<Item> ASTRAL_HEART = Mistech.ITEMS.registerSimpleItem("astral_heart",
            p -> p.rarity(Rarity.EPIC).stacksTo(16).fireResistant());

    public static final DeferredBlock<MithrilObeliskBlock> MITHRIL_OBELISK = Mistech.BLOCKS.registerBlock("mithril_obelisk",
            MithrilObeliskBlock::new,
            p -> p.mapColor(MapColor.COLOR_LIGHT_BLUE).requiresCorrectToolForDrops().strength(5.0F, 1200.0F).sound(SoundType.NETHERITE_BLOCK)
                    .noOcclusion().lightLevel(state -> 5).pushReaction(PushReaction.BLOCK));
    public static final DeferredItem<BlockItem> MITHRIL_OBELISK_ITEM = Mistech.ITEMS.registerSimpleBlockItem("mithril_obelisk", MITHRIL_OBELISK,
            p -> p.rarity(Rarity.UNCOMMON));

    public static final DeferredBlock<OrichalcumBrazierBlock> ORICHALCUM_BRAZIER = Mistech.BLOCKS.registerBlock("orichalcum_brazier",
            OrichalcumBrazierBlock::new,
            p -> p.mapColor(MapColor.COLOR_ORANGE).requiresCorrectToolForDrops().strength(5.0F, 1200.0F).sound(SoundType.NETHERITE_BLOCK)
                    .noOcclusion().lightLevel(state -> state.getValue(OrichalcumBrazierBlock.LIT) ? 15 : 3).pushReaction(PushReaction.BLOCK));
    public static final DeferredItem<BlockItem> ORICHALCUM_BRAZIER_ITEM = Mistech.ITEMS.registerSimpleBlockItem("orichalcum_brazier",
            ORICHALCUM_BRAZIER, p -> p.rarity(Rarity.RARE).fireResistant());

    private AltarContent() {}

    public static List<DeferredItem<? extends Item>> creativeItems() {
        return List.of(AWAKENED_HEART, ASTRAL_HEART, MITHRIL_OBELISK_ITEM, ORICHALCUM_BRAZIER_ITEM);
    }

    public static void register(IEventBus modEventBus) {
        // tudo é registrado pelos DeferredRegister do Mistech; o método força o carregamento da classe
    }
}

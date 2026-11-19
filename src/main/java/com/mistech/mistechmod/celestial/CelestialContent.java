package com.mistech.mistechmod.celestial;

import java.util.List;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.block.DreamcatcherBlock;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

/** Itens e blocos dos rituais Tier II/III e do Santuário (Eventos Celestes). */
public final class CelestialContent {
    /** Cai dos monstros mortos na Lua de Sangue. */
    public static final DeferredItem<Item> LUNAR_BLOOD = Mistech.ITEMS.registerSimpleItem("lunar_blood", p -> p.rarity(Rarity.UNCOMMON));
    /** Cai dos meteoros da Chuva de Estrelas. Na Mesa de Astrologia, paga o XP de uma estrela. */
    public static final DeferredItem<Item> STAR_FRAGMENT = Mistech.ITEMS.registerSimpleItem("star_fragment", p -> p.rarity(Rarity.RARE));

    /** Apanha Sonho Purificado (Santuário): também segura a Pisadeira que a Mácula chama. */
    public static final DeferredBlock<DreamcatcherBlock> PURIFIED_DREAMCATCHER = Mistech.BLOCKS.registerBlock("purified_dreamcatcher",
            DreamcatcherBlock::new,
            p -> p.mapColor(MapColor.QUARTZ).noCollision().strength(0.3F).sound(SoundType.WOOL).noOcclusion()
                    .pushReaction(PushReaction.DESTROY).lightLevel(state -> 5));
    public static final DeferredItem<BlockItem> PURIFIED_DREAMCATCHER_ITEM = Mistech.ITEMS.registerSimpleBlockItem("purified_dreamcatcher",
            PURIFIED_DREAMCATCHER, p -> p.rarity(Rarity.UNCOMMON));

    private CelestialContent() {}

    /** Força o carregamento da classe (os registros acontecem nos campos estáticos). */
    public static void init() {}

    /** Relógio Astral (Fase 6): o céu vira sinal de comparador. */
    public static final DeferredBlock<AstralClockBlock> ASTRAL_CLOCK = Mistech.BLOCKS.registerBlock("astral_clock", AstralClockBlock::new,
            p -> p.mapColor(net.minecraft.world.level.material.MapColor.LAPIS).strength(2.0F, 6.0F)
                    .sound(net.minecraft.world.level.block.SoundType.AMETHYST).lightLevel(state -> 5).noOcclusion());
    public static final DeferredItem<net.minecraft.world.item.BlockItem> ASTRAL_CLOCK_ITEM = Mistech.ITEMS.registerSimpleBlockItem("astral_clock",
            ASTRAL_CLOCK, p -> p.rarity(net.minecraft.world.item.Rarity.UNCOMMON));

    public static List<DeferredItem<? extends Item>> creativeItems() {
        return List.of(LUNAR_BLOOD, STAR_FRAGMENT, PURIFIED_DREAMCATCHER_ITEM, ASTRAL_CLOCK_ITEM);
    }
}

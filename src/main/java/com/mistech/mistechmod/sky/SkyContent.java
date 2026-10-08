package com.mistech.mistechmod.sky;

import java.util.List;

import com.mistech.mistechmod.Mistech;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * "Céu": Runa das Nuvens (ritual Tier II, Runa de Vapor + Runas de Ar), Bloco de Nuvem (temporário) e Nuvem Estável.
 * Base para os artefatos do ar (Botas do Vento, Vassoura das Nuvens).
 */
public final class SkyContent {
    public static final int CLOUD_RUNE_USES = 16;

    public static final DeferredItem<CloudRuneItem> CLOUD_RUNE = Mistech.ITEMS.registerItem("cloud_rune", CloudRuneItem::new,
            p -> p.durability(CLOUD_RUNE_USES).rarity(Rarity.UNCOMMON));

    private static BlockBehaviour.Properties cloud(BlockBehaviour.Properties p) {
        return p.mapColor(MapColor.SNOW).sound(SoundType.WOOL).noOcclusion().pushReaction(PushReaction.DESTROY)
                .isValidSpawn((state, level, pos, type) -> false).isRedstoneConductor((state, level, pos) -> false)
                .isSuffocating((state, level, pos) -> false).isViewBlocking((state, level, pos) -> false);
    }

    /** Nuvem da runa: some em 30 s, sem drop. */
    public static final DeferredBlock<TemporaryCloudBlock> CLOUD_BLOCK = Mistech.BLOCKS.registerBlock("cloud_block", TemporaryCloudBlock::new,
            p -> cloud(p).instabreak().noLootTable());

    /** Nuvem permanente, para construir (Runa das Nuvens em lã branca). */
    public static final DeferredBlock<CloudBlock> STABLE_CLOUD = Mistech.BLOCKS.registerBlock("stable_cloud", CloudBlock::new,
            p -> cloud(p).strength(0.3F));
    public static final DeferredItem<BlockItem> STABLE_CLOUD_ITEM = Mistech.ITEMS.registerSimpleBlockItem("stable_cloud", STABLE_CLOUD);

    private SkyContent() {}

    public static List<DeferredItem<? extends Item>> creativeItems() {
        return List.of(CLOUD_RUNE, STABLE_CLOUD_ITEM);
    }

    public static void register(IEventBus modEventBus) {
        // por enquanto tudo vai nos registros do Mistech; aqui entram os próximos (artefatos, vassoura)
    }
}

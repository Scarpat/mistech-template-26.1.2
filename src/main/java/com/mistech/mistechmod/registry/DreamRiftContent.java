package com.mistech.mistechmod.registry;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.realm.dream.rift.DreamFragmentItem;
import com.mistech.mistechmod.realm.dream.rift.DreamRegion;
import com.mistech.mistechmod.realm.dream.rift.DreamRiftBlock;
import com.mistech.mistechmod.realm.dream.rift.DreamRiftBlockEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Fragmentos de Sonho: trazem uma região do Sonho para a Superfície por uma Fenda Onírica.
 * <ul>
 * <li>Fragmento de Sonho (ritual Tier I) — região sorteada, raio {@link #RADIUS}.</li>
 * <li>Fragmento de Sonho Amplificado (ritual Tier II) — região sorteada, raio {@link #RADIUS_AMPLIFIED}.</li>
 * <li>Fragmento de Sonho: &lt;região&gt; (ritual Tier II, um por região) — a região escolhida, raio {@link #RADIUS}.</li>
 * </ul>
 */
public final class DreamRiftContent {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Mistech.MODID);

    public static final int RADIUS = 12;
    public static final int RADIUS_AMPLIFIED = 28;

    public static final DeferredBlock<DreamRiftBlock> DREAM_RIFT = Mistech.BLOCKS.registerBlock("dream_rift", DreamRiftBlock::new,
            p -> p.mapColor(MapColor.COLOR_PINK).strength(-1.0F, 3600000.0F).noCollision().noOcclusion().noLootTable()
                    .sound(SoundType.AMETHYST).lightLevel(s -> 12).pushReaction(PushReaction.BLOCK));
    public static final Supplier<BlockEntityType<DreamRiftBlockEntity>> DREAM_RIFT_BE = BLOCK_ENTITIES.register("dream_rift",
            () -> new BlockEntityType<>(DreamRiftBlockEntity::new, DREAM_RIFT.get()));

    public static final DeferredItem<DreamFragmentItem> DREAM_FRAGMENT = Mistech.ITEMS.registerItem("dream_fragment",
            p -> new DreamFragmentItem(p, RADIUS, null), p -> p.stacksTo(16).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<DreamFragmentItem> AMPLIFIED_DREAM_FRAGMENT = Mistech.ITEMS.registerItem("amplified_dream_fragment",
            p -> new DreamFragmentItem(p, RADIUS_AMPLIFIED, null), p -> p.stacksTo(16).rarity(Rarity.RARE));
    /** Um fragmento sintonizado por região ({@code mistech:dream_fragment_<região>}). */
    public static final Map<DreamRegion, DeferredItem<DreamFragmentItem>> ATTUNED = new EnumMap<>(DreamRegion.class);
    static {
        for (DreamRegion region : DreamRegion.values()) {
            ATTUNED.put(region, Mistech.ITEMS.registerItem("dream_fragment_" + region.id(),
                    p -> new DreamFragmentItem(p, RADIUS, region), p -> p.stacksTo(16).rarity(Rarity.RARE)));
        }
    }

    private DreamRiftContent() {}

    public static List<DeferredItem<? extends Item>> creativeItems() {
        List<DeferredItem<? extends Item>> list = new ArrayList<>();
        list.add(DREAM_FRAGMENT);
        list.add(AMPLIFIED_DREAM_FRAGMENT);
        for (DreamRegion region : DreamRegion.values()) {
            list.add(ATTUNED.get(region));
        }
        return list;
    }

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITIES.register(modEventBus);
    }
}

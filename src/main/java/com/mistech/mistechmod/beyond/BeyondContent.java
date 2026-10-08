package com.mistech.mistechmod.beyond;

import java.util.List;
import java.util.function.Supplier;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.registry.GnomeContent;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * "Além do Overworld": Deep Dark (Runa do Eco, Coletor de Ecos, Bússola do Eco, Selo do Silêncio, Ecólito, Chamado do Guardião)
 * e o eixo dos Quatro Corações (Impuro + Sculk + Rangente + Infernal → Coração Primordial).
 */
public final class BeyondContent {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Mistech.MODID);

    /** A Runa do Eco é rara (Fragmentos de Eco): só 8 usos. */
    public static final int ECHO_RUNE_USES = 8;

    // ----- runa -----
    public static final DeferredItem<EchoRuneItem> ECHO_RUNE = Mistech.ITEMS.registerItem("echo_rune", EchoRuneItem::new,
            p -> p.durability(ECHO_RUNE_USES).rarity(Rarity.UNCOMMON));

    // ----- Deep Dark -----
    public static final DeferredItem<EchoCompassItem> ECHO_COMPASS = Mistech.ITEMS.registerItem("echo_compass", EchoCompassItem::new,
            p -> p.stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<SilenceSealItem> SILENCE_SEAL = Mistech.ITEMS.registerItem("silence_seal", SilenceSealItem::new,
            p -> p.stacksTo(16).rarity(Rarity.UNCOMMON));

    public static final DeferredBlock<EchoCollectorBlock> ECHO_COLLECTOR = Mistech.BLOCKS.registerBlock("echo_collector", EchoCollectorBlock::new,
            p -> p.mapColor(MapColor.COLOR_CYAN).requiresCorrectToolForDrops().strength(3.0F, 6.0F).sound(SoundType.SCULK_CATALYST)
                    .lightLevel(state -> state.getValue(EchoCollectorBlock.FULLNESS) * 2));
    public static final DeferredItem<BlockItem> ECHO_COLLECTOR_ITEM = Mistech.ITEMS.registerSimpleBlockItem("echo_collector", ECHO_COLLECTOR,
            p -> p.rarity(Rarity.UNCOMMON));
    public static final Supplier<BlockEntityType<EchoCollectorBlockEntity>> ECHO_COLLECTOR_BE = BLOCK_ENTITIES.register("echo_collector",
            () -> new BlockEntityType<>(EchoCollectorBlockEntity::new, ECHO_COLLECTOR.get()));

    public static final DeferredHolder<EntityType<?>, EntityType<EcholingEntity>> ECHOLING = GnomeContent.ENTITIES.registerEntityType("echoling",
            EcholingEntity::new, MobCategory.CREATURE, b -> b.sized(0.5F, 0.7F).eyeHeight(0.45F).clientTrackingRange(8));

    // ----- os Quatro Corações -----
    public static final DeferredItem<HeartItem> SCULK_HEART = Mistech.ITEMS.registerItem("sculk_heart", HeartItem::new,
            p -> p.rarity(Rarity.EPIC).stacksTo(16).fireResistant());
    public static final DeferredItem<PaleHeartItem> PALE_HEART = Mistech.ITEMS.registerItem("pale_heart", PaleHeartItem::new,
            p -> p.rarity(Rarity.EPIC).stacksTo(16).fireResistant());
    public static final DeferredItem<HeartItem> INFERNAL_HEART = Mistech.ITEMS.registerItem("infernal_heart", HeartItem::new,
            p -> p.rarity(Rarity.EPIC).stacksTo(16).fireResistant());
    public static final DeferredItem<HeartItem> PRIMORDIAL_HEART = Mistech.ITEMS.registerItem("primordial_heart", HeartItem::new,
            p -> p.rarity(Rarity.EPIC).stacksTo(1).fireResistant());

    private BeyondContent() {}

    public static List<DeferredItem<? extends Item>> creativeItems() {
        List<DeferredItem<? extends Item>> list = new java.util.ArrayList<>(List.of(ECHO_RUNE, ECHO_COMPASS, SILENCE_SEAL, ECHO_COLLECTOR_ITEM,
                SCULK_HEART, PALE_HEART, INFERNAL_HEART, PRIMORDIAL_HEART));
        list.addAll(PaleNetherContent.creativeItems());
        return list;
    }

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITIES.register(modEventBus);
        PaleNetherContent.register(modEventBus);
        modEventBus.addListener(BeyondContent::onAttributes);
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(ECHOLING.get(), Parrot.createAttributes().build());
    }
}

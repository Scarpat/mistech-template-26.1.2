package com.mistech.mistechmod.registry;

import java.util.List;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.block.DreamcatcherBlock;
import com.mistech.mistechmod.entity.PisadeiraEntity;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Sonhos: Apanha Sonho (bloco de parede), Chá de Cogumelo (Visão Psicodélica) e a Pisadeira (paralisia do sono).
 * Quem dorme sob a Visão Psicodélica: sem Apanha Sonho perto da cama = paralisia do sono com a Pisadeira;
 * com Apanha Sonho = vai para a Dimensão dos Gnomos (ver {@link com.mistech.mistechmod.dream.GnomeRealm}).
 */
public final class DreamContent {
    // Apanha Sonho: graveto / linha / Resíduo Mágico. Fica na parede.
    public static final DeferredBlock<DreamcatcherBlock> DREAMCATCHER = Mistech.BLOCKS.registerBlock("dreamcatcher",
            DreamcatcherBlock::new,
            p -> p.mapColor(MapColor.WOOL).noCollision().strength(0.3F).sound(SoundType.WOOL).noOcclusion()
                    .pushReaction(PushReaction.DESTROY));
    public static final DeferredItem<BlockItem> DREAMCATCHER_ITEM = Mistech.ITEMS.registerSimpleBlockItem("dreamcatcher", DREAMCATCHER);

    // Chá de Cogumelo (Chaleira: água + Cogumelo Amarelado): Visão Psicodélica por 1:30
    public static final DeferredItem<Item> MUSHROOM_TEA_CUP = Mistech.ITEMS.registerSimpleItem("mushroom_tea_cup", p -> p.stacksTo(16)
            .usingConvertsTo(Mistech.CUP.get())
            .component(DataComponents.CONSUMABLE, Consumables.defaultDrink()
                    .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MistechEffects.PSYCHEDELIC_SIGHT, 1800, 0)))
                    .build()));

    // Pisadeira: aparece em cima de quem acorda com paralisia do sono
    public static final DeferredHolder<EntityType<?>, EntityType<PisadeiraEntity>> PISADEIRA = GnomeContent.ENTITIES.registerEntityType("pisadeira",
            PisadeiraEntity::new, MobCategory.MISC, b -> b.sized(0.6F, 1.2F).eyeHeight(1.0F).clientTrackingRange(8).noSave().noSummon().fireImmune());

    private DreamContent() {}

    public static List<DeferredItem<? extends Item>> creativeItems() {
        return List.of(DREAMCATCHER_ITEM, MUSHROOM_TEA_CUP);
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(DreamContent::onAttributes);
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(PISADEIRA.get(), PisadeiraEntity.createAttributes().build());
    }
}

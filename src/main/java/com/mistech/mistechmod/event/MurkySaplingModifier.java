package com.mistech.mistechmod.event;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.registry.MistechEffects;
import com.mistech.mistechmod.registry.MurkyWood;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Sob a Visão Mística (poção de Beladona), as mudas que caem de folhas e troncos viram Mudas de Árvore Turva.
 * Vale para quem quebra as folhas e também para as folhas que caem sozinhas depois de cortar a árvore
 * (nesse caso conta o jogador com o efeito mais perto, até {@link #DECAY_RADIUS} blocos).
 * Arquivo: data/mistech/loot_modifiers/murky_sapling.json.
 */
public class MurkySaplingModifier extends LootModifier {
    public static final double DECAY_RADIUS = 24.0;

    public static final MapCodec<MurkySaplingModifier> CODEC = RecordCodecBuilder.mapCodec(
            instance -> codecStart(instance).apply(instance, MurkySaplingModifier::new));

    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, Mistech.MODID);
    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<MurkySaplingModifier>> TYPE =
            SERIALIZERS.register("murky_sapling", () -> CODEC);

    public MurkySaplingModifier(LootItemCondition[] conditions, int priority) {
        super(conditions, priority);
    }

    public static void register(IEventBus modEventBus) {
        SERIALIZERS.register(modEventBus);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        BlockState state = context.getOptionalParameter(LootContextParams.BLOCK_STATE);
        if (state == null || !(state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS))) {
            return generatedLoot;
        }
        if (!hasMysticSight(context)) {
            return generatedLoot;
        }
        for (int i = 0; i < generatedLoot.size(); i++) {
            ItemStack stack = generatedLoot.get(i);
            if (stack.is(ItemTags.SAPLINGS) && !stack.is(MurkyWood.SAPLING_ITEM.get())) {
                generatedLoot.set(i, new ItemStack(MurkyWood.SAPLING_ITEM.get(), stack.getCount()));
            }
        }
        return generatedLoot;
    }

    private static boolean hasMysticSight(LootContext context) {
        Entity breaker = context.getOptionalParameter(LootContextParams.THIS_ENTITY);
        if (breaker instanceof Player player) {
            return (player.hasEffect(MistechEffects.MYSTIC_SIGHT) || player.hasEffect(MistechEffects.PSYCHEDELIC_SIGHT));
        }
        // folhas caindo sozinhas: não há quem quebrou; vale o jogador com o efeito que estiver perto
        Vec3 origin = context.getOptionalParameter(LootContextParams.ORIGIN);
        if (breaker == null && origin != null) {
            ServerLevel level = context.getLevel();
            for (Player player : level.players()) {
                if ((player.hasEffect(MistechEffects.MYSTIC_SIGHT) || player.hasEffect(MistechEffects.PSYCHEDELIC_SIGHT)) && player.distanceToSqr(origin) <= DECAY_RADIUS * DECAY_RADIUS) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return TYPE.get();
    }
}

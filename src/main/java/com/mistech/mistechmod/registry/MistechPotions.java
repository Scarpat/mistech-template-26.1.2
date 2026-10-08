package com.mistech.mistechmod.registry;

import com.mistech.mistechmod.Mistech;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Poções do Mistech (café e visão mística). A base é feita na Chaleira; o suporte de poções só faz as versões arremessáveis do café. */
@EventBusSubscriber(modid = Mistech.MODID)
public final class MistechPotions {
    public static final DeferredRegister<Potion> POTIONS = DeferredRegister.create(Registries.POTION, Mistech.MODID);

    /** Garrafa de Café: Pressa + Velocidade por 3 minutos. Chaleira: água + Pó de Café. */
    public static final DeferredHolder<Potion, Potion> COFFEE = POTIONS.register("coffee",
            () -> new Potion("coffee", new MobEffectInstance(MobEffects.HASTE, 3600, 0), new MobEffectInstance(MobEffects.SPEED, 3600, 0)));

    /** Visão Mística: campo de visão maior e cores estranhas por 3 minutos. Chaleira: água + Beladona. */
    public static final DeferredHolder<Potion, Potion> MYSTIC_SIGHT = POTIONS.register("mystic_sight",
            () -> new Potion("mystic_sight", new MobEffectInstance(MistechEffects.MYSTIC_SIGHT, 3600, 0)));

    /** Cor do café (marrom). */
    public static final int COFFEE_COLOR = 0x5A3217;

    private MistechPotions() {}

    /** Garrafa de café (normal, arremessável ou persistente) já com a cor marrom. */
    public static net.minecraft.world.item.ItemStack coffeeStack(net.minecraft.world.item.Item container) {
        net.minecraft.world.item.ItemStack stack = new net.minecraft.world.item.ItemStack(container);
        stack.set(net.minecraft.core.component.DataComponents.POTION_CONTENTS, new net.minecraft.world.item.alchemy.PotionContents(
                java.util.Optional.of(COFFEE), java.util.Optional.of(COFFEE_COLOR), java.util.List.of(), java.util.Optional.empty()));
        return stack;
    }

    private static boolean isPotion(net.minecraft.world.item.ItemStack stack, net.minecraft.core.Holder<Potion> potion) {
        return stack.getOrDefault(net.minecraft.core.component.DataComponents.POTION_CONTENTS,
                net.minecraft.world.item.alchemy.PotionContents.EMPTY).is(potion);
    }

    /** Café + pólvora = arremessável; arremessável + bafo do dragão = persistente. Sempre marrom. (O café em si sai da Chaleira.) */
    private static final class CoffeeBrewing implements net.neoforged.neoforge.common.brewing.IBrewingRecipe {
        private static boolean isBottle(net.minecraft.world.item.ItemStack stack) {
            return stack.is(net.minecraft.world.item.Items.POTION) || stack.is(net.minecraft.world.item.Items.SPLASH_POTION)
                    || stack.is(net.minecraft.world.item.Items.LINGERING_POTION);
        }

        @Override
        public boolean isInput(net.minecraft.world.item.ItemStack input) {
            return isBottle(input) && isPotion(input, COFFEE);
        }

        @Override
        public boolean isIngredient(net.minecraft.world.item.ItemStack ingredient) {
            return ingredient.is(net.minecraft.world.item.Items.GUNPOWDER)
                    || ingredient.is(net.minecraft.world.item.Items.DRAGON_BREATH);
        }

        @Override
        public net.minecraft.world.item.ItemStack getOutput(net.minecraft.world.item.ItemStack input, net.minecraft.world.item.ItemStack ingredient) {
            if (!isBottle(input)) {
                return net.minecraft.world.item.ItemStack.EMPTY;
            }
            if (ingredient.is(net.minecraft.world.item.Items.GUNPOWDER) && input.is(net.minecraft.world.item.Items.POTION) && isPotion(input, COFFEE)) {
                return coffeeStack(net.minecraft.world.item.Items.SPLASH_POTION);
            }
            if (ingredient.is(net.minecraft.world.item.Items.DRAGON_BREATH) && input.is(net.minecraft.world.item.Items.SPLASH_POTION) && isPotion(input, COFFEE)) {
                return coffeeStack(net.minecraft.world.item.Items.LINGERING_POTION);
            }
            return net.minecraft.world.item.ItemStack.EMPTY;
        }
    }

    public static void register(IEventBus modEventBus) {
        POTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void onRegisterBrewing(RegisterBrewingRecipesEvent event) {
        // Café e Visão Mística agora são bebidas de Xícara (Chaleira). As poções continuam registradas só para
        // não quebrar itens de mundos antigos; nenhuma receita do suporte de poções faz elas.
    }
}

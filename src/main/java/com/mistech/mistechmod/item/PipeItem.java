package com.mistech.mistechmod.item;

import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.registry.MistechComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * Cachimbo: em vez de enrolar charutos/baseados, carrega folhas secas direto nele.
 * - Carregar: folha seca de tabaco OU de cânhamo na outra mão + botão direito (1 folha; agachado = a pilha toda),
 *   ou clicar com a pilha de folhas em cima do cachimbo no inventário. Até {@link #MAX_LEAVES} folhas, de um tipo só.
 * - Agachado + botão direito com o cachimbo apagado devolve as folhas.
 * - Acende igual ao charuto (isqueiro/Runa de Fogo na outra mão, Cinzeiro Rúnico). Aceso, queima folha por folha
 *   (tabaco 180 s, cânhamo 120 s, igual a um charuto/baseado) e apaga sozinho quando a última acaba.
 * - Cada tragada dá os efeitos do charuto (tabaco) ou do baseado (cânhamo).
 */
public class PipeItem extends SmokableItem {
    public static final int MAX_LEAVES = 16;
    public static final String TOBACCO = "tobacco";
    public static final String HEMP = "hemp";

    public PipeItem(Item.Properties properties) {
        super(properties, 180, 5);
    }

    // ----- conteúdo -----

    public static @Nullable String herbOf(ItemStack stack) {
        return stack.getOrDefault(MistechComponents.PIPE_LEAVES.get(), 0) > 0 ? stack.get(MistechComponents.PIPE_HERB.get()) : null;
    }

    public static int leavesOf(ItemStack stack) {
        return stack.getOrDefault(MistechComponents.PIPE_LEAVES.get(), 0);
    }

    /** Qual erva é esta folha (null se não servir para o cachimbo). */
    public static @Nullable String herbOfLeaf(ItemStack leaf) {
        if (leaf.is(Mistech.DRIED_TOBACCO_LEAF.get())) {
            return TOBACCO;
        }
        if (leaf.is(Mistech.DRIED_HEMP_LEAF.get())) {
            return HEMP;
        }
        return null;
    }

    private static int secondsPerLeaf(@Nullable String herb) {
        return HEMP.equals(herb) ? 120 : 180;
    }

    /** Põe até {@code max} folhas; devolve quantas entraram. */
    private static int load(ItemStack pipe, ItemStack leaves, int max) {
        String herb = herbOfLeaf(leaves);
        if (herb == null) {
            return 0;
        }
        String current = herbOf(pipe);
        if (current != null && !current.equals(herb)) {
            return 0; // não mistura tabaco com cânhamo
        }
        int moved = Math.min(Math.min(max, leaves.getCount()), MAX_LEAVES - leavesOf(pipe));
        if (moved <= 0) {
            return 0;
        }
        pipe.set(MistechComponents.PIPE_HERB.get(), herb);
        pipe.set(MistechComponents.PIPE_LEAVES.get(), leavesOf(pipe) + moved);
        leaves.shrink(moved);
        return moved;
    }

    private static void empty(ItemStack pipe) {
        pipe.remove(MistechComponents.PIPE_HERB.get());
        pipe.remove(MistechComponents.PIPE_LEAVES.get());
        pipe.remove(MistechComponents.INCENSE_BURN.get());
        pipe.remove(MistechComponents.INCENSE_LIT.get());
    }

    @Override
    public boolean canLight(ItemStack stack) {
        return leavesOf(stack) > 0;
    }

    @Override
    public int getRemainingSeconds(ItemStack stack) {
        return stack.getOrDefault(MistechComponents.INCENSE_BURN.get(), secondsPerLeaf(herbOf(stack)));
    }

    /** Queima folha por folha; quando acaba a última, o cachimbo fica vazio e apagado (não some). */
    @Override
    protected boolean burn(ItemStack stack, int seconds) {
        String herb = herbOf(stack);
        int leaves = leavesOf(stack);
        int remaining = this.getRemainingSeconds(stack) - seconds;
        while (remaining <= 0 && leaves > 1) {
            leaves--;
            remaining += secondsPerLeaf(herb);
        }
        if (remaining <= 0) {
            empty(stack);
            return true;
        }
        stack.set(MistechComponents.PIPE_LEAVES.get(), leaves);
        stack.set(MistechComponents.INCENSE_BURN.get(), remaining);
        return false;
    }

    @Override
    protected void applyPuffEffects(ServerLevel level, LivingEntity entity) {
        // o SmokableItem não passa o stack: veja finishUsingItem
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        String herb = herbOf(stack);
        boolean lit = isLit(stack);
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (!level.isClientSide() && lit && herb != null) {
            this.applyHerbEffects(result, herb, entity);
        }
        return result;
    }

    /** Efeitos de uma tragada (o Cachimbo Rúnico troca pelos da runa escolhida). */
    protected void applyHerbEffects(ItemStack stack, String herb, LivingEntity entity) {
        if (HEMP.equals(herb)) {
            JointItem.applyHemp(entity);
        } else if (TOBACCO.equals(herb)) {
            CigarItem.applyTobacco(entity);
        }
    }

    // ----- carregar / esvaziar -----

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        ItemStack other = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);

        if (!isLit(stack)) {
            if (herbOfLeaf(other) != null) {
                if (!level.isClientSide()) {
                    int moved = load(stack, other, player.isShiftKeyDown() ? MAX_LEAVES : 1);
                    if (moved > 0) {
                        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.BUNDLE_INSERT, SoundSource.PLAYERS, 0.8F, 0.8F);
                    }
                }
                return InteractionResult.SUCCESS;
            }
            if (player.isShiftKeyDown() && leavesOf(stack) > 0) {
                if (!level.isClientSide()) {
                    // a folha que já começou a queimar se perde
                    boolean started = stack.has(MistechComponents.INCENSE_BURN.get());
                    int back = leavesOf(stack) - (started ? 1 : 0);
                    Item leaf = HEMP.equals(herbOf(stack)) ? Mistech.DRIED_HEMP_LEAF.get() : Mistech.DRIED_TOBACCO_LEAF.get();
                    empty(stack);
                    if (back > 0) {
                        player.getInventory().placeItemBackInInventory(new ItemStack(leaf, back));
                    }
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.BUNDLE_REMOVE_ONE, SoundSource.PLAYERS, 0.8F, 0.8F);
                }
                return InteractionResult.SUCCESS;
            }
        }
        return super.use(level, player, hand);
    }

    /** Clicar com a pilha de folhas em cima do cachimbo (no inventário) carrega o quanto couber. */
    @Override
    public boolean overrideOtherStackedOnMe(ItemStack self, ItemStack other, Slot slot, ClickAction clickAction, Player player, SlotAccess carriedItem) {
        if (clickAction != ClickAction.SECONDARY || other.isEmpty() || herbOfLeaf(other) == null
                || isLit(self) || self.getCount() != 1 || !slot.allowModification(player)) {
            return false;
        }
        if (load(self, other, MAX_LEAVES) > 0) {
            player.playSound(SoundEvents.BUNDLE_INSERT, 0.8F, 0.8F);
        } else {
            player.playSound(SoundEvents.BUNDLE_INSERT_FAIL, 1.0F, 1.0F);
        }
        return true;
    }

    // ----- visual -----

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return leavesOf(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        int perLeaf = secondsPerLeaf(herbOf(stack));
        int leaves = leavesOf(stack);
        int total = (leaves - 1) * perLeaf + this.getRemainingSeconds(stack);
        return Mth.clamp(Math.round(13.0F * total / (MAX_LEAVES * perLeaf)), 1, 13);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        String herb = herbOf(stack);
        if (herb == null) {
            builder.accept(Component.translatable("item.mistech.pipe.empty").withStyle(ChatFormatting.GRAY));
        } else {
            Item leaf = HEMP.equals(herb) ? Mistech.DRIED_HEMP_LEAF.get() : Mistech.DRIED_TOBACCO_LEAF.get();
            builder.accept(Component.translatable("item.mistech.pipe.contents", Component.translatable(leaf.getDescriptionId()),
                    leavesOf(stack), MAX_LEAVES).withStyle(ChatFormatting.GRAY));
        }
    }
}

package com.mistech.mistechmod.spellcraft;

import java.util.List;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * Tomo do Aprendiz: lança as magias que o jogador conhece. Botão direito lança a escolhida (funciona em qualquer
 * mão); Shift + roda do mouse troca. É a base para os Tomos, Varinhas e Cajados com bônus por elemento (próxima leva).
 */
public class SpellTomeItem extends Item {
    public SpellTomeItem(Item.Properties properties) {
        super(properties);
    }

    public static boolean is(ItemStack stack) {
        return stack.getItem() instanceof SpellTomeItem;
    }

    /** Multiplicador do gasto de mana por elemento (1.0 = normal). Os Tomos especializados mudam isto. */
    public double manaMultiplier(ItemStack stack, Spell spell) {
        return 1.0;
    }

    public static @Nullable Spell selected(Player player, ItemStack stack) {
        List<Spell> list = Spells.learnedBy(player);
        if (list.isEmpty()) {
            return null;
        }
        int index = Math.floorMod(stack.getOrDefault(SpellcraftContent.TOME_SELECTED.get(), 0), list.size());
        return list.get(index);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }
        ItemStack stack = player.getItemInHand(hand);
        Spell spell = selected(player, stack);
        if (spell == null) {
            serverPlayer.sendOverlayMessage(Component.translatable("message.mistech.spell.none_learned").withStyle(ChatFormatting.GRAY));
            return InteractionResult.FAIL;
        }
        double multiplier = stack.getItem() instanceof SpellTomeItem tome ? tome.manaMultiplier(stack, spell) : 1.0;
        return SpellCaster.tryCast(serverPlayer, spell, multiplier, 1.0) == SpellCaster.Result.OK ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }

    /** Shift + roda: próxima ou anterior magia. */
    public static void scroll(ServerPlayer player, int direction) {
        ItemStack stack = is(player.getMainHandItem()) ? player.getMainHandItem() : is(player.getOffhandItem()) ? player.getOffhandItem() : ItemStack.EMPTY;
        if (stack.isEmpty()) {
            return;
        }
        List<Spell> list = Spells.learnedBy(player);
        if (list.isEmpty()) {
            player.sendOverlayMessage(Component.translatable("message.mistech.spell.none_learned").withStyle(ChatFormatting.GRAY));
            return;
        }
        int index = Math.floorMod(stack.getOrDefault(SpellcraftContent.TOME_SELECTED.get(), 0) + (direction > 0 ? 1 : -1), list.size());
        stack.set(SpellcraftContent.TOME_SELECTED.get(), index);
        Spell spell = list.get(index);
        int cost = ManaService.cost(player, spell.mana());
        player.sendOverlayMessage(Component.translatable("message.mistech.spell.selected",
                Component.translatable(spell.nameKey()).withColor(spell.element().color()), cost,
                spell.hasAffinity(player) ? Component.empty() : Component.translatable("message.mistech.spell.locked", spell.requiredLevel()).withStyle(ChatFormatting.RED)));
        player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.6F, 1.4F);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder,
                                TooltipFlag flag) {
        builder.accept(Component.translatable("item.mistech.apprentice_tome.desc").withStyle(ChatFormatting.GRAY));
        builder.accept(Component.translatable("item.mistech.apprentice_tome.hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}

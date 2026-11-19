package com.mistech.mistechmod.spellcraft.conjurer;

import java.util.List;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.spellcraft.Spell;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * Cristal de Feitiço gravado: guarda uma magia para o Conjurador Rúnico lançar. Não se gasta.
 * A cor do brilho vem do elemento da magia (custom_model_data, como o selo do pergaminho).
 */
public class SpellCrystalItem extends Item {
    public SpellCrystalItem(Item.Properties properties) {
        super(properties);
    }

    public static ItemStack of(Spell spell, float power, Player engraver) {
        ItemStack stack = new ItemStack(ConjurerContent.SPELL_CRYSTAL.get());
        stack.set(ConjurerContent.CRYSTAL_DATA.get(), new SpellCrystal(spell.id(), power, engraver.getUUID(), engraver.getName().getString()));
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(), List.of(), List.of(spell.element().color())));
        return stack;
    }

    public static @Nullable SpellCrystal data(ItemStack stack) {
        return stack.get(ConjurerContent.CRYSTAL_DATA.get());
    }

    /** Magia do cristal, se ele estiver gravado com uma magia que existe. */
    public static @Nullable Spell spell(ItemStack stack) {
        SpellCrystal data = data(stack);
        return data == null ? null : data.resolve();
    }

    @Override
    public Component getName(ItemStack stack) {
        Spell spell = spell(stack);
        if (spell == null) {
            return super.getName(stack);
        }
        return Component.translatable("item.mistech.spell_crystal.named", Component.translatable(spell.nameKey()));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder,
                                TooltipFlag flag) {
        SpellCrystal data = data(stack);
        Spell spell = data == null ? null : data.resolve();
        if (spell == null) {
            builder.accept(Component.translatable("tooltip.mistech.spell_crystal.blank").withStyle(ChatFormatting.GRAY));
            return;
        }
        builder.accept(Component.translatable(spell.element().translationKey()).append(Component.literal(" · "))
                .append(Component.translatable("tooltip.mistech.spell.tier", spell.tier())).withColor(spell.element().color()));
        builder.accept(Component.translatable("tooltip.mistech.spell_crystal.power", Math.round(data.power() * 100)).withStyle(ChatFormatting.AQUA));
        BlockCasts.Mode mode = BlockCasts.mode(spell.id());
        if (mode != null) {
            builder.accept(Component.translatable("tooltip.mistech.spell_crystal.use." + mode.id()).withStyle(ChatFormatting.GRAY));
        }
        builder.accept(Component.translatable("tooltip.mistech.spell_crystal.owner", data.ownerName()).withStyle(ChatFormatting.DARK_GRAY));
        builder.accept(Component.translatable("tooltip.mistech.spell_crystal.hint").withStyle(ChatFormatting.DARK_GRAY));
        builder.accept(Component.translatable("tooltip.mistech.spell_crystal.clear").withStyle(ChatFormatting.DARK_GRAY));
    }

    /** Agachado + botão direito no ar: apaga a magia e devolve o Cristal de Feitiço vazio. */
    @Override
    public net.minecraft.world.InteractionResult use(net.minecraft.world.level.Level level, Player player, net.minecraft.world.InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isSecondaryUseActive() || data(stack) == null) {
            return super.use(level, player, hand);
        }
        if (!level.isClientSide()) {
            ItemStack blank = new ItemStack(ConjurerContent.BLANK_SPELL_CRYSTAL.get());
            stack.shrink(1);
            if (stack.isEmpty()) {
                player.setItemInHand(hand, blank);
            } else if (!player.getInventory().add(blank)) {
                player.drop(blank, false);
            }
            level.playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_BREAK,
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.8F, 1.4F);
            player.sendOverlayMessage(Component.translatable("message.mistech.spell_crystal.cleared"));
        }
        return net.minecraft.world.InteractionResult.SUCCESS;
    }
}

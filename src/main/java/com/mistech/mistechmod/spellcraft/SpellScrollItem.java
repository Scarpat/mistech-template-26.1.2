package com.mistech.mistechmod.spellcraft;

import java.util.List;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.item.RuneElement;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * Pergaminho de magia: botão direito ensina a magia (consome o pergaminho). Pode ser achado no mundo ou inscrito na
 * Mesa de Inscrição por quem já conhece a magia. A cor do selo vem do elemento (custom_model_data).
 */
public class SpellScrollItem extends Item {
    public SpellScrollItem(Item.Properties properties) {
        super(properties);
    }

    public static ItemStack of(Spell spell) {
        ItemStack stack = new ItemStack(SpellcraftContent.SPELL_SCROLL.get());
        stack.set(SpellcraftContent.SPELL_ID.get(), spell.id());
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(), List.of(), List.of(spell.element().color())));
        return stack;
    }

    public static @Nullable Spell spell(ItemStack stack) {
        return Spells.byId(stack.get(SpellcraftContent.SPELL_ID.get()));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Spell spell = spell(stack);
        if (spell == null) {
            return InteractionResult.FAIL;
        }
        if (!(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }
        SpellBook book = SpellBook.of(player);
        Component name = Component.translatable(spell.nameKey()).withColor(spell.element().color());
        if (book.knows(spell.id())) {
            serverPlayer.sendOverlayMessage(Component.translatable("message.mistech.spell.already_known", name).withStyle(ChatFormatting.GRAY));
            return InteractionResult.FAIL;
        }
        player.setData(SpellcraftContent.SPELL_BOOK, book.with(spell.id()));
        stack.consume(1, player);
        serverPlayer.sendOverlayMessage(Component.translatable("message.mistech.spell.learned", name));
        serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT, player.getX(), player.getY() + 1.2, player.getZ(), 40, 0.5, 0.6, 0.5, 0.8);
        serverLevel.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0F, 1.1F);
        return InteractionResult.SUCCESS;
    }

    @Override
    public Component getName(ItemStack stack) {
        Spell spell = spell(stack);
        if (spell == null) {
            return super.getName(stack);
        }
        return Component.translatable("item.mistech.spell_scroll.named", Component.translatable(spell.nameKey()));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder,
                                TooltipFlag flag) {
        Spell spell = spell(stack);
        if (spell == null) {
            return;
        }
        RuneElement element = spell.element();
        builder.accept(Component.translatable(element.translationKey()).append(Component.literal(" · "))
                .append(Component.translatable("tooltip.mistech.spell.tier", spell.tier()))
                .append(Component.literal(" · ")).append(Component.translatable("tooltip.mistech.spell.kind." + spell.kind().name().toLowerCase(java.util.Locale.ROOT))).withColor(element.color()));
        builder.accept(Component.translatable(spell.descKey()).withStyle(ChatFormatting.GRAY));
        builder.accept(Component.translatable(spell.channelTicks() > 0 ? "tooltip.mistech.spell.mana_channel" : "tooltip.mistech.spell.mana", spell.mana()).withStyle(ChatFormatting.AQUA));
        builder.accept(Component.translatable("tooltip.mistech.spell.requires", spell.requiredLevel(),
                spell.requirementName()).withStyle(ChatFormatting.DARK_GRAY));
        if (spell.worldOnly()) {
            builder.accept(Component.translatable("tooltip.mistech.spell.world_only").withStyle(ChatFormatting.GOLD));
        }
        builder.accept(Component.translatable("tooltip.mistech.spell.use").withStyle(ChatFormatting.DARK_GRAY));
    }
}

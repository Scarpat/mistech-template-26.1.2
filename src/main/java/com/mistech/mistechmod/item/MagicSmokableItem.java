package com.mistech.mistechmod.item;

import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.registry.MistechComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * Charuto Mágico (tabaco) e Baseado Mágico (cânhamo): enrolados numa runa em vez de papel.
 * A runa fica no componente mistech:smoke_rune e a cor da faixa em minecraft:dyed_color.
 * Cada tragada dá (e renova) os efeitos da runa por 90 s. Acende e queima igual ao charuto/baseado comum.
 */
public class MagicSmokableItem extends SmokableItem {
    private final boolean tobacco;

    public MagicSmokableItem(Item.Properties properties, boolean tobacco) {
        super(properties, tobacco ? 240 : 180, 5);
        this.tobacco = tobacco;
    }

    public boolean isTobacco() {
        return this.tobacco;
    }

    public static @Nullable RuneElement elementOf(ItemStack stack) {
        return RuneElement.byId(stack.get(MistechComponents.SMOKE_RUNE.get()));
    }

    /** Cria o item já com a runa e a cor da faixa. */
    public static ItemStack create(Item item, RuneElement element, int count) {
        ItemStack stack = new ItemStack(item, count);
        stack.set(MistechComponents.SMOKE_RUNE.get(), element.id());
        stack.set(DataComponents.DYED_COLOR, new DyedItemColor(element.color()));
        stack.set(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT.withHidden(DataComponents.DYED_COLOR, true));
        return stack;
    }

    @Override
    protected void applyPuffEffects(ServerLevel level, LivingEntity entity) {
        // o SmokableItem chama isto sem o stack; guardamos o elemento da última tragada em finishUsingItem
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, net.minecraft.world.level.Level level, LivingEntity entity) {
        RuneElement element = elementOf(stack);
        boolean lit = isLit(stack);
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (!level.isClientSide() && lit && element != null) {
            for (RuneElement.Effect effect : element.effects(this.tobacco)) {
                entity.addEffect(new MobEffectInstance(effect.effect(), RuneElement.DURATION_TICKS, effect.amplifier(), false, true, true));
            }
        }
        return result;
    }

    @Override
    public Component getName(ItemStack stack) {
        RuneElement element = elementOf(stack);
        if (element == null) {
            return Component.translatable(this.getDescriptionId() + ".plain"); // sem runa (ex.: ícone do grimório)
        }
        String key = this.getDescriptionId() + (isLit(stack) ? ".lit" : "");
        return Component.translatable(key, Component.translatable(element.translationKey()));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, builder, flag);
        RuneElement element = elementOf(stack);
        if (element == null) {
            return;
        }
        builder.accept(Component.translatable("tooltip.mistech.magic_smoke.puff").withStyle(ChatFormatting.GRAY));
        for (RuneElement.Effect effect : element.effects(this.tobacco)) {
            Component name = Component.translatable(effect.effect().value().getDescriptionId());
            if (effect.amplifier() > 0) {
                name = Component.translatable("potion.withAmplifier", name,
                        Component.translatable("potion.potency." + effect.amplifier()));
            }
            builder.accept(Component.literal(" ").append(name).append(" (1:30)").withStyle(ChatFormatting.BLUE));
        }
    }
}

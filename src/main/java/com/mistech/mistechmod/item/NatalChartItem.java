package com.mistech.mistechmod.item;

import java.util.function.Consumer;

import com.mistech.mistechmod.astrology.NatalChart;
import com.mistech.mistechmod.astrology.NatalChartCopy;
import com.mistech.mistechmod.registry.MistechComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * Mapa Astral: cópia em papel do mapa de um jogador (feita na Mesa de Astrologia).
 * Mostra de quem é e os signos que já tinham sido revelados. Também é o catalisador do Renascimento Astral.
 */
public class NatalChartItem extends Item {
    public NatalChartItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        NatalChartCopy copy = stack.get(MistechComponents.NATAL_CHART_COPY.get());
        if (copy == null) {
            builder.accept(Component.translatable("item.mistech.natal_chart.blank").withStyle(ChatFormatting.GRAY));
            return;
        }
        builder.accept(Component.translatable("item.mistech.natal_chart.owner", copy.owner()).withStyle(ChatFormatting.GRAY));
        for (NatalChart.Part part : NatalChart.Part.values()) {
            int sign = copy.sign(part);
            Component value = sign < 0 ? Component.literal("???").withStyle(ChatFormatting.DARK_GRAY)
                    : Component.literal(NatalChart.glyph(sign) + " ").append(NatalChart.signName(sign))
                            .withColor(NatalChart.elementOf(sign).color());
            builder.accept(Component.translatable("item.mistech.natal_chart.line", part.displayName(), value).withStyle(ChatFormatting.GRAY));
        }
    }
}

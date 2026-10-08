package com.mistech.mistechmod.item;

import java.util.function.Consumer;

import com.mistech.mistechmod.registry.MistechComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

/** Item das Baterias Rúnicas: mostra a energia guardada (componente mistech:battery_energy) no tooltip e na barra. */
public class EnergyBlockItem extends BlockItem {
    private final int capacity;

    public EnergyBlockItem(Block block, Properties properties, int capacity) {
        super(block, properties);
        this.capacity = capacity;
    }

    private static int stored(ItemStack stack) {
        return stack.getOrDefault(MistechComponents.BATTERY_ENERGY.get(), 0);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return stored(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * Math.min(1.0F, stored(stack) / (float) this.capacity));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        // roxo (vazio) → dourado (cheio), como a barra da tela
        float f = Math.min(1.0F, stored(stack) / (float) this.capacity);
        int r = Mth.lerpInt(f, 0x96, 0xF0);
        int g = Mth.lerpInt(f, 0x5A, 0xC8);
        int b = Mth.lerpInt(f, 0xE6, 0x70);
        return (r << 16) | (g << 8) | b;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, builder, flag);
        builder.accept(Component.translatable("gui.mistech.energy_tooltip", String.format(java.util.Locale.ROOT, "%,d", stored(stack)).replace(',', '.'),
                String.format(java.util.Locale.ROOT, "%,d", this.capacity).replace(',', '.')).withStyle(ChatFormatting.GRAY));
    }
}

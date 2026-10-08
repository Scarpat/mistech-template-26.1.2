package com.mistech.mistechmod.item;

import java.util.function.Consumer;

import com.mistech.mistechmod.registry.MistechComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Item de bloco que guarda coisas dentro (Barril Espacial, Tanque Rúnico).
 * Não pode ir para dentro de outro recipiente (evita barril dentro de barril) e mostra o fluido guardado no tooltip.
 */
public class StorageBlockItem extends BlockItem {
    /** true = guarda itens (não pode ir dentro de outro recipiente). */
    private final boolean holdsItems;

    public StorageBlockItem(Block block, Properties properties, boolean holdsItems) {
        super(block, properties);
        this.holdsItems = holdsItems;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean canFitInsideContainerItems() {
        return !this.holdsItems;
    }

    @Override
    public boolean canFitInsideContainerItems(ItemStack stack) {
        return !this.holdsItems;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, builder, flag);
        net.neoforged.neoforge.fluids.SimpleFluidContent content = stack.get(MistechComponents.TANK_FLUID.get());
        FluidStack fluid = content == null ? FluidStack.EMPTY : content.copy();
        if (!fluid.isEmpty()) {
            builder.accept(Component.translatable("tooltip.mistech.tank_fluid", fluid.getHoverName(),
                    String.format(java.util.Locale.ROOT, "%.1f", fluid.getAmount() / 1000.0)).withStyle(ChatFormatting.GRAY));
        }
    }
}

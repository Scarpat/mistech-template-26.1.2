package com.mistech.mistechmod.extraplanar;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

/** Item do Barril/Tanque Extraplanar: o código fica no item ao quebrar o bloco e mostra no tooltip. */
public class ExtraplanarBlockItem extends BlockItem {
    public ExtraplanarBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, builder, flag);
        String code = stack.get(ExtraplanarContent.CODE.get());
        if (code == null || code.isEmpty()) {
            builder.accept(Component.translatable("tooltip.mistech.extraplanar.no_code").withStyle(ChatFormatting.GRAY));
        } else {
            builder.accept(Component.translatable("tooltip.mistech.extraplanar.code", code).withStyle(ChatFormatting.AQUA));
        }
    }
}

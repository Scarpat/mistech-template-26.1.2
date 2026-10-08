package com.mistech.mistechmod.mage;

import java.util.function.Consumer;

import com.mistech.mistechmod.item.mithril.MithrilItem;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/** Peça das Vestes de Mago: energia no lugar da durabilidade (como o mitril), tingível, bônus de magia por peça. */
public class MageRobeItem extends MithrilItem {
    private final MageRobeTier robe;

    public MageRobeItem(MageRobeTier robe, Item.Properties properties) {
        super(robe.gear, properties);
        this.robe = robe;
    }

    public MageRobeTier robe() {
        return this.robe;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("item.mistech.mage_robe.per_piece").withStyle(ChatFormatting.DARK_PURPLE));
        builder.accept(Component.translatable("item.mistech.mage_robe.cost", pct(this.robe.costCut)).withStyle(ChatFormatting.LIGHT_PURPLE));
        if (this.robe.magicDamage > 0) {
            builder.accept(Component.translatable("item.mistech.mage_robe.damage", pct(this.robe.magicDamage)).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        if (this.robe.affinity > 0) {
            builder.accept(Component.translatable("item.mistech.mage_robe.affinity", pct(this.robe.affinity)).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        if (this.robe.taintCut > 0) {
            builder.accept(Component.translatable("item.mistech.mage_robe.taint", pct(this.robe.taintCut)).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        builder.accept(Component.translatable("item.mistech.mage_robe.set", pct(this.robe.shield)).withStyle(ChatFormatting.AQUA));
        super.appendHoverText(stack, context, display, builder, flag);
    }

    private static String pct(double v) {
        return Math.round(v * 100) + "%";
    }
}

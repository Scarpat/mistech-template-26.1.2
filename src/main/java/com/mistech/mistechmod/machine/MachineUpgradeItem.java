package com.mistech.mistechmod.machine;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/** Melhoria Rúnica: clicar numa máquina instala (ver {@link MachineUpgradeEvents}). */
public class MachineUpgradeItem extends Item {
    private final MachineUpgrade type;

    public MachineUpgradeItem(Item.Properties properties, MachineUpgrade type) {
        super(properties);
        this.type = type;
    }

    public MachineUpgrade type() {
        return this.type;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.mistech.upgrade." + this.type.id() + ".desc").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("item.mistech.upgrade.how", this.type.max()).withStyle(ChatFormatting.DARK_PURPLE));
    }
}

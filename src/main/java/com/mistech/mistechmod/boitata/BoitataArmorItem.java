package com.mistech.mistechmod.boitata;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.equipment.ArmorType;

/** Peça da Armadura do Boitatá (efeitos em {@link BoitataArmorEvents}). */
public class BoitataArmorItem extends Item {
    private final ArmorType type;

    public BoitataArmorItem(ArmorType type, Item.Properties properties) {
        super(properties);
        this.type = type;
    }

    public ArmorType armorType() {
        return this.type;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("item.mistech.boitata_armor." + this.type.getName()).withStyle(ChatFormatting.GOLD));
        builder.accept(Component.translatable("item.mistech.boitata_armor.set").withStyle(ChatFormatting.DARK_RED));
        super.appendHoverText(stack, context, display, builder, flag);
    }
}

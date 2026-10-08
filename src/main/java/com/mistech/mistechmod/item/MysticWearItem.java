package com.mistech.mistechmod.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Vestes místicas de lã (tingíveis como couro): Chapéu de Gnomo, Roupão Místico,
 * Calças Magicamente Confortáveis e Pantufas. Os bônus vêm dos atributos do item
 * e de {@link com.mistech.mistechmod.event.MysticWearEvents}.
 */
public class MysticWearItem extends Item {
    private final boolean walksOnPowderSnow;

    public MysticWearItem(Item.Properties properties, boolean walksOnPowderSnow) {
        super(properties);
        this.walksOnPowderSnow = walksOnPowderSnow;
    }

    /** Pantufas: andam sobre neve fofa, igual às botas de couro. */
    @Override
    public boolean canWalkOnPowderedSnow(ItemStack stack, LivingEntity wearer) {
        return this.walksOnPowderSnow || super.canWalkOnPowderedSnow(stack, wearer);
    }
}

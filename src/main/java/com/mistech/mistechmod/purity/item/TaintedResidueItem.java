package com.mistech.mistechmod.purity.item;

import com.mistech.mistechmod.purity.block.TaintedSoilBlock;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Borra Impura: o que sobra da destilação. Macula quem a carrega (tag mistech:tainting) e, largada no chão
 * por {@link #SPOIL_TICKS}, se infiltra na terra: cada unidade vira 1 bloco de Solo Maculado em volta.
 */
public class TaintedResidueItem extends Item {
    public static final int SPOIL_TICKS = 30 * 20;

    public TaintedResidueItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        if (entity.level() instanceof ServerLevel level && entity.getAge() >= SPOIL_TICKS && entity.onGround()) {
            int count = stack.getCount();
            TaintedSoilBlock.contaminateNear(level, entity.blockPosition().below(), 2, count, 0);
            entity.discard();
            return true;
        }
        return false;
    }
}

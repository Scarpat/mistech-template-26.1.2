package com.mistech.mistechmod.purity.item;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.item.IncenseItem;
import com.mistech.mistechmod.purity.Taint;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Incenso de Arruda: funciona como o incenso comum (2 minutos, Véu de Incenso) e ainda limpa a Mácula de quem
 * está na fumaça: {@link #CLEANSE_PER_SECOND} por segundo, no máximo até 20. No Porta-Incenso também limpa,
 * devagar, o Solo Maculado na área.
 */
public class RueIncenseItem extends IncenseItem {
    public static final float CLEANSE_PER_SECOND = 0.05F;

    public RueIncenseItem(Properties properties) {
        super(properties);
    }

    public static boolean isRue(ItemStack stack) {
        return stack.getItem() instanceof RueIncenseItem;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        boolean lit = isLit(stack);
        super.inventoryTick(stack, level, entity, slot);
        if (lit && entity instanceof Player player && level.getGameTime() % 20 == 0) {
            Taint.cleanse(player, CLEANSE_PER_SECOND);
        }
    }
}

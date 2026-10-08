package com.mistech.mistechmod.block;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.registry.MistechFluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;

/** Balde/garrafa/frasco clicando num tanque: enche ou esvazia. */
public final class FluidInteraction {
    public static final int BOTTLE_AMOUNT = 250;

    private FluidInteraction() {}

    public static boolean isWaterBottle(ItemStack stack) {
        return stack.is(Items.POTION)
                && stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).is(Potions.WATER);
    }

    /** Itens que mexem com fluido (no cliente o clique é só "previsto"; quem mexe no tanque é o servidor). */
    public static boolean isFluidItem(ItemStack stack) {
        return isWaterBottle(stack) || stack.is(Mistech.IMPURE_ESSENCE_BOTTLE.get())
                || stack.is(com.mistech.mistechmod.purity.PurityContent.PURIFIED_ESSENCE_BOTTLE.get())
                || stack.is(Items.GLASS_BOTTLE)
                || stack.is(Items.BUCKET) || stack.getItem() instanceof net.minecraft.world.item.BucketItem;
    }

    /** Só no servidor. @return true se algo aconteceu (o chamador devolve SUCCESS) */
    public static boolean interact(Player player, InteractionHand hand, Level level, BlockPos pos, FluidTank tank) {
        ItemStack held = player.getItemInHand(hand);
        FluidResource water = FluidResource.of(Fluids.WATER);
        FluidResource essence = FluidResource.of(MistechFluids.IMPURE_ESSENCE.get());
        FluidResource pure = FluidResource.of(MistechFluids.PURIFIED_ESSENCE.get());

        // garrafa de água / frasco de essência -> tanque
        FluidResource bottleFluid = isWaterBottle(held) ? water
                : held.is(Mistech.IMPURE_ESSENCE_BOTTLE.get()) ? essence
                : held.is(com.mistech.mistechmod.purity.PurityContent.PURIFIED_ESSENCE_BOTTLE.get()) ? pure : null;
        if (bottleFluid != null) {
            if (tank.fill(bottleFluid, BOTTLE_AMOUNT, true) < BOTTLE_AMOUNT) {
                return false; // não cabe a garrafa inteira
            }
            tank.fill(bottleFluid, BOTTLE_AMOUNT, false);
            if (!level.isClientSide()) {
                player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, new ItemStack(Items.GLASS_BOTTLE)));
                level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return true;
        }

        // garrafa vazia <- tanque (água ou essência)
        if (held.is(Items.GLASS_BOTTLE)) {
            ItemStack filled;
            FluidResource from;
            if (tank.has(water, BOTTLE_AMOUNT)) {
                from = water;
                filled = PotionContents.createItemStack(Items.POTION, Potions.WATER);
            } else if (tank.has(essence, BOTTLE_AMOUNT)) {
                from = essence;
                filled = new ItemStack(Mistech.IMPURE_ESSENCE_BOTTLE.get());
            } else if (tank.has(pure, BOTTLE_AMOUNT)) {
                from = pure;
                filled = new ItemStack(com.mistech.mistechmod.purity.PurityContent.PURIFIED_ESSENCE_BOTTLE.get());
            } else {
                return false;
            }
            tank.drain(from, BOTTLE_AMOUNT, false);
            if (!level.isClientSide()) {
                player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, filled));
                level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return true;
        }

        // baldes (e qualquer item com capability de fluido)
        return FluidUtil.interactWithFluidHandler(player, hand, pos, tank, null);
    }
}

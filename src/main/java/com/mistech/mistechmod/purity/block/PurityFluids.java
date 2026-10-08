package com.mistech.mistechmod.purity.block;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.purity.PurityContent;
import com.mistech.mistechmod.registry.MistechFluids;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;

/** Ajudas de fluido das máquinas da Magia Purificada. */
public final class PurityFluids {
    public static final int BOTTLE = 250;
    public static final int BUCKET = 1000;

    private PurityFluids() {}

    public static FluidResource impure() {
        return FluidResource.of(MistechFluids.IMPURE_ESSENCE.get());
    }

    public static FluidResource pure() {
        return FluidResource.of(MistechFluids.PURIFIED_ESSENCE.get());
    }

    public static boolean isContainer(ItemStack stack) {
        return stack.is(Items.GLASS_BOTTLE) || stack.is(Items.BUCKET)
                || stack.is(Mistech.IMPURE_ESSENCE_BOTTLE.get()) || stack.is(Mistech.IMPURE_ESSENCE_BUCKET.get())
                || stack.is(PurityContent.PURIFIED_ESSENCE_BOTTLE.get()) || stack.is(PurityContent.PURIFIED_ESSENCE_BUCKET.get());
    }

    /** Troca o item da mão do jogador (1 unidade) pelo recipiente cheio/vazio. */
    public static void swap(Player player, InteractionHand hand, ItemStack result) {
        ItemStack held = player.getItemInHand(hand);
        player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, result));
    }

    /**
     * Tanques de 1 fluido por espaço, com regras de quem entra e quem sai pelos canos/funis.
     * As máquinas mexem direto com {@link #add}/{@link #take} (sem as regras).
     */
    public static class Tanks extends FluidStacksResourceHandler {
        private final FluidResource[] allowed;
        private final boolean[] canInsert;
        private final boolean[] canExtract;
        private final Runnable onChanged;

        public Tanks(int capacity, FluidResource[] allowed, boolean[] canInsert, boolean[] canExtract, Runnable onChanged) {
            super(allowed.length, capacity);
            this.allowed = allowed;
            this.canInsert = canInsert;
            this.canExtract = canExtract;
            this.onChanged = onChanged;
        }

        @Override
        public boolean isValid(int index, FluidResource resource) {
            return resource.isEmpty() || resource.equals(this.allowed[index]);
        }

        @Override
        public int insert(int index, FluidResource resource, int amount, net.neoforged.neoforge.transfer.transaction.TransactionContext transaction) {
            return this.canInsert[index] ? super.insert(index, resource, amount, transaction) : 0;
        }

        @Override
        public int extract(int index, FluidResource resource, int amount, net.neoforged.neoforge.transfer.transaction.TransactionContext transaction) {
            return this.canExtract[index] ? super.extract(index, resource, amount, transaction) : 0;
        }

        @Override
        protected void onContentsChanged(int index, FluidStack previousContents) {
            this.onChanged.run();
        }

        public int amount(int index) {
            return (int) this.getAmountAsLong(index);
        }

        public int space(int index) {
            return this.capacity - this.amount(index);
        }

        public void setAmount(int index, int amount) {
            amount = Math.max(0, Math.min(this.capacity, amount));
            this.set(index, amount == 0 ? FluidResource.EMPTY : this.allowed[index], amount);
        }

        public void add(int index, int amount) {
            this.setAmount(index, this.amount(index) + amount);
        }

        public boolean take(int index, int amount) {
            if (this.amount(index) < amount) {
                return false;
            }
            this.setAmount(index, this.amount(index) - amount);
            return true;
        }
    }
}

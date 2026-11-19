package com.mistech.mistechmod.block;

import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * Os 4 tanques da Cafeteira Rúnica. O tanque 0 só aceita água (ele se enche sozinho);
 * os tanques 1 a 3 guardam outros líquidos, cada um diferente dos demais.
 */
public class MakerTanks extends FluidStacksResourceHandler {
    public static final int TANKS = 4;
    public static final int CAPACITY = 8_000;

    private final Runnable onChanged;

    public MakerTanks(Runnable onChanged) {
        super(TANKS, CAPACITY);
        this.onChanged = onChanged;
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        if (resource.isEmpty()) {
            return false;
        }
        boolean water = resource.getFluid() == Fluids.WATER && resource.isComponentsPatchEmpty();
        if (index == 0) {
            return water;
        }
        if (water) {
            return false;
        }
        for (int i = 1; i < TANKS; i++) {
            if (i != index && resource.equals(this.getResource(i))) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void onContentsChanged(int index, FluidStack previousContents) {
        this.onChanged.run();
    }

    public FluidStack getFluid(int index) {
        return this.stacks.get(index);
    }

    public int amountOf(FluidResource resource) {
        int total = 0;
        for (int i = 0; i < TANKS; i++) {
            if (resource.equals(this.getResource(i))) {
                total += this.getAmountAsInt(i);
            }
        }
        return total;
    }

    public boolean has(FluidResource resource, int amount) {
        return this.amountOf(resource) >= amount;
    }

    /** Coloca até {@code amount}; devolve quanto coube. */
    public int fill(FluidResource resource, int amount, boolean simulate) {
        if (resource.isEmpty() || amount <= 0) {
            return 0;
        }
        try (Transaction tx = Transaction.openRoot()) {
            int inserted = this.insert(resource, amount, tx);
            if (!simulate) {
                tx.commit();
            }
            return inserted;
        }
    }

    /** Tira até {@code amount}; devolve quanto saiu. */
    public int drain(FluidResource resource, int amount, boolean simulate) {
        if (resource.isEmpty() || amount <= 0) {
            return 0;
        }
        try (Transaction tx = Transaction.openRoot()) {
            int extracted = this.extract(resource, amount, tx);
            if (!simulate) {
                tx.commit();
            }
            return extracted;
        }
    }
}

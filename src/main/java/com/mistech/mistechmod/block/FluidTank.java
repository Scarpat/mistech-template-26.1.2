package com.mistech.mistechmod.block;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** Tanque de 1 fluido (usado pelo Tanque Rúnico e pelos dois tanques do Confector de Runas). */
public class FluidTank extends FluidStacksResourceHandler {
    private final Runnable onChanged;

    public FluidTank(int capacity, Runnable onChanged) {
        super(1, capacity);
        this.onChanged = onChanged;
    }

    @Override
    protected void onContentsChanged(int index, FluidStack previousContents) {
        this.onChanged.run();
    }

    public FluidStack getFluid() {
        return this.stacks.get(0);
    }

    public int getAmount() {
        return this.getFluid().getAmount();
    }

    public int getCapacity() {
        return this.capacity;
    }

    public boolean isEmpty() {
        return this.getFluid().isEmpty();
    }

    /** Troca o conteúdo direto (carregar do item, montar o Confector...). */
    public void setFluid(FluidStack stack) {
        if (stack.isEmpty()) {
            this.set(0, FluidResource.EMPTY, 0);
        } else {
            this.set(0, FluidResource.of(stack), Math.min(stack.getAmount(), this.capacity));
        }
    }

    /** Coloca até {@code amount} do fluido; devolve quanto coube. */
    public int fill(FluidResource resource, int amount, boolean simulate) {
        if (resource.isEmpty() || amount <= 0) {
            return 0;
        }
        try (Transaction tx = Transaction.openRoot()) {
            int inserted = this.insert(0, resource, amount, tx);
            if (!simulate) {
                tx.commit();
            }
            return inserted;
        }
    }

    /** Tira até {@code amount} do fluido; devolve quanto saiu. */
    public int drain(FluidResource resource, int amount, boolean simulate) {
        if (resource.isEmpty() || amount <= 0) {
            return 0;
        }
        try (Transaction tx = Transaction.openRoot()) {
            int extracted = this.extract(0, resource, amount, tx);
            if (!simulate) {
                tx.commit();
            }
            return extracted;
        }
    }

    public boolean has(FluidResource resource, int amount) {
        return !this.isEmpty() && resource.matches(this.getFluid()) && this.getAmount() >= amount;
    }
}

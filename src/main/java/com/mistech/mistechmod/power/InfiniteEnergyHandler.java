package com.mistech.mistechmod.power;

import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/** Energia sem fim (Bateria de Bolso Criativa): dá tudo o que pedem e engole tudo o que mandam. */
public final class InfiniteEnergyHandler implements EnergyHandler {
    public static final InfiniteEnergyHandler INSTANCE = new InfiniteEnergyHandler();

    private InfiniteEnergyHandler() {}

    @Override
    public long getAmountAsLong() {
        return Integer.MAX_VALUE;
    }

    @Override
    public long getCapacityAsLong() {
        return Integer.MAX_VALUE;
    }

    @Override
    public int insert(int amount, TransactionContext transaction) {
        return Math.max(0, amount);
    }

    @Override
    public int extract(int amount, TransactionContext transaction) {
        return Math.max(0, amount);
    }
}

package com.mistech.mistechmod.power;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.ether.EtherNetworks;

import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * Janela para a energia de uma sintonia da Rede Etérea (Bateria de Bolso Astral). Respeita as transações:
 * se a transação for desfeita, a energia volta para a rede.
 */
public final class NetworkEnergyHandler extends SnapshotJournal<Long> implements EnergyHandler {
    private final EtherNetworks networks;
    private final EtherNetworks.Net net;
    private final int maxInsert;
    private final int maxExtract;

    private NetworkEnergyHandler(EtherNetworks networks, EtherNetworks.Net net, int maxInsert, int maxExtract) {
        this.networks = networks;
        this.net = net;
        this.maxInsert = maxInsert;
        this.maxExtract = maxExtract;
    }

    /** Só no thread do servidor (no cliente devolve {@code null}). */
    public static @Nullable NetworkEnergyHandler of(int networkId, int maxInsert, int maxExtract) {
        if (networkId <= 0) {
            return null;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || !server.isSameThread()) {
            return null;
        }
        EtherNetworks networks = EtherNetworks.get(server);
        EtherNetworks.Net net = networks.get(networkId);
        return net == null ? null : new NetworkEnergyHandler(networks, net, maxInsert, maxExtract);
    }

    public EtherNetworks.Net net() {
        return this.net;
    }

    @Override
    public long getAmountAsLong() {
        return this.net.energy;
    }

    @Override
    public long getCapacityAsLong() {
        return this.net.capacity;
    }

    @Override
    public int insert(int amount, TransactionContext transaction) {
        int moved = (int) Math.min(Math.min(amount, this.maxInsert), this.net.space());
        if (moved <= 0) {
            return 0;
        }
        this.updateSnapshots(transaction);
        this.net.energy += moved;
        return moved;
    }

    @Override
    public int extract(int amount, TransactionContext transaction) {
        int moved = (int) Math.min(Math.min(amount, this.maxExtract), this.net.energy);
        if (moved <= 0) {
            return 0;
        }
        this.updateSnapshots(transaction);
        this.net.energy -= moved;
        return moved;
    }

    @Override
    protected Long createSnapshot() {
        return this.net.energy;
    }

    @Override
    protected void revertToSnapshot(Long snapshot) {
        this.net.energy = snapshot;
    }

    @Override
    protected void onRootCommit(Long original) {
        long delta = this.net.energy - original;
        if (delta > 0) {
            this.net.movedIn += delta;
        } else if (delta < 0) {
            this.net.movedOut -= delta;
        }
        if (delta != 0) {
            this.networks.setDirty();
        }
    }
}

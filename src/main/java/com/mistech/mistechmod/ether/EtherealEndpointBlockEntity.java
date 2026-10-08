package com.mistech.mistechmod.ether;

import com.mistech.mistechmod.block.arcane.ArcaneAttachedBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** Ponto/Receptor Etéreo: move energia entre o bloco onde está preso e a sintonia, sem perda. */
public class EtherealEndpointBlockEntity extends BlockEntity {
    /** Por tick. */
    public static final int NODE_RATE = 50_000;
    public static final int RECEIVER_RATE = 20_000;

    private final boolean output;
    private int network;

    public EtherealEndpointBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, boolean output) {
        super(type, pos, state);
        this.output = output;
    }

    public int network() {
        return this.network;
    }

    public void setNetwork(int network) {
        if (this.level instanceof ServerLevel server) {
            this.unregister(server);
        }
        this.network = network;
        this.setChanged();
        if (this.level instanceof ServerLevel server) {
            this.register(server);
        }
    }

    private String key() {
        return this.level.dimension().identifier() + "@" + this.worldPosition.toShortString();
    }

    private void register(ServerLevel level) {
        EtherNetworks.Net net = EtherNetworks.get(level.getServer()).get(this.network);
        if (net != null) {
            (this.output ? net.outputs : net.inputs).add(this.key());
        }
    }

    private void unregister(ServerLevel level) {
        EtherNetworks.Net net = EtherNetworks.get(level.getServer()).get(this.network);
        if (net != null) {
            (this.output ? net.outputs : net.inputs).remove(this.key());
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level instanceof ServerLevel server) {
            this.register(server);
        }
    }

    @Override
    public void setRemoved() {
        if (this.level instanceof ServerLevel server) {
            this.unregister(server);
        }
        super.setRemoved();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, EtherealEndpointBlockEntity be) {
        if (be.network <= 0 || !(level instanceof ServerLevel server)) {
            return;
        }
        EtherNetworks nets = EtherNetworks.get(server.getServer());
        EtherNetworks.Net net = nets.get(be.network);
        if (net == null) {
            return;
        }
        Direction facing = state.getValue(ArcaneAttachedBlock.FACING);
        EnergyHandler handler = level.getCapability(Capabilities.Energy.BLOCK, ArcaneAttachedBlock.attachedPos(pos, state), facing);
        if (handler == null) {
            return;
        }
        if (be.output) {
            int want = (int) Math.min(RECEIVER_RATE, net.energy);
            if (want <= 0) {
                return;
            }
            try (Transaction tx = Transaction.openRoot()) {
                int moved = handler.insert(want, tx);
                if (moved > 0) {
                    nets.extract(net, moved);
                    tx.commit();
                }
            }
        } else {
            int want = (int) Math.min(NODE_RATE, net.space());
            if (want <= 0) {
                return;
            }
            try (Transaction tx = Transaction.openRoot()) {
                int moved = handler.extract(want, tx);
                if (moved > 0) {
                    nets.insert(net, moved);
                    tx.commit();
                }
            }
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Network", this.network);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.network = input.getIntOr("Network", 0);
    }
}

package com.mistech.mistechmod.conduit;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.mistech.mistechmod.conduit.RunicConduitBlock.Side;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandlerUtil;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;

/**
 * Lógica do Conduto Rúnico. Só os condutos com ponta "puxa" trabalham: acham a rede de cada tipo (energia, itens, fluidos: só pelos
 * condutos que levam aquele tipo; guardada até algum conduto mudar) e mandam o que tiram para as pontas "entrega", em rodízio.
 * A velocidade é a do conduto mais lento da rede ({@link ConduitTier}).
 */
public class RunicConduitBlockEntity extends BlockEntity {
    public static final int MAX_CONDUITS = 512;

    /** Muda sempre que algum conduto muda (as redes guardadas ficam velhas). */
    private static final AtomicInteger VERSION = new AtomicInteger();

    public static void invalidateNetworks() {
        VERSION.incrementAndGet();
    }

    /** Uma ponta: o bloco encostado e a face dele virada para o conduto. */
    private record End(BlockPos target, Direction face) {}

    /** A rede de um tipo vista daqui: as pontas de entrega e o nível mais lento. */
    private record Net(List<End> ends, ConduitTier tier) {}

    private final java.util.EnumMap<ConduitCarry.Kind, Net> nets = new java.util.EnumMap<>(ConduitCarry.Kind.class);
    private int cachedVersion = -1;
    private int roundRobin;
    private ConduitCarry carry = ConduitCarry.ALL;

    public RunicConduitBlockEntity(BlockPos pos, BlockState state) {
        super(ConduitContent.RUNIC_CONDUIT_BE.get(), pos, state);
    }

    public ConduitCarry carry() {
        return this.carry;
    }

    public void setCarry(ConduitCarry carry) {
        this.carry = carry;
        invalidateNetworks();
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
        }
    }

    // ----- cliente: o modo vai junto para pintar a runa do miolo -----

    @Override
    public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection connection, net.minecraft.world.level.storage.ValueInput input) {
        ConduitCarry before = this.carry;
        this.loadAdditional(input);
        if (before != this.carry && this.level != null && this.level.isClientSide()) {
            // redesenha o bloco com a cor nova
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 8);
        }
    }

    private ConduitTier tier() {
        return this.getBlockState().getBlock() instanceof RunicConduitBlock b ? b.tier() : ConduitTier.RUNIC;
    }

    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput input) {
        super.loadAdditional(input);
        int c = input.getIntOr("Carry", ConduitCarry.ALL.ordinal());
        this.carry = ConduitCarry.values()[Math.floorMod(c, ConduitCarry.values().length)];
    }

    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Carry", this.carry.ordinal());
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, RunicConduitBlockEntity be) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        List<Direction> pulls = new ArrayList<>(2);
        for (Direction d : Direction.values()) {
            if (state.getValue(RunicConduitBlock.SIDES.get(d)) == Side.PULL) {
                pulls.add(d);
            }
        }
        if (pulls.isEmpty()) {
            return;
        }
        if (be.cachedVersion != VERSION.get()) {
            be.cachedVersion = VERSION.get();
            be.nets.clear();
            for (ConduitCarry.Kind kind : ConduitCarry.Kind.values()) {
                if (be.carry.carries(kind)) {
                    be.nets.put(kind, findNet(server, pos, kind));
                }
            }
        }
        Net energyNet = be.nets.get(ConduitCarry.Kind.ENERGY);
        Net itemNet = be.nets.get(ConduitCarry.Kind.ITEMS);
        Net fluidNet = be.nets.get(ConduitCarry.Kind.FLUIDS);
        for (Direction d : pulls) {
            BlockPos source = pos.relative(d);
            Direction face = d.getOpposite();
            if (!server.isLoaded(source)) {
                continue;
            }
            if (energyNet != null) {
                be.energy(server, energyNet, source, face);
            }
            boolean moved = false;
            if (itemNet != null && be.due(server, pos, itemNet.tier())) {
                moved |= be.items(server, itemNet, source, face);
            }
            if (fluidNet != null && be.due(server, pos, fluidNet.tier())) {
                moved |= be.fluids(server, fluidNet, source, face);
            }
            if (moved && server.getRandom().nextInt(3) == 0) {
                server.sendParticles(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 2, 0.15, 0.15, 0.15, 0.2);
            }
        }
    }

    private boolean due(ServerLevel level, BlockPos pos, ConduitTier tier) {
        return Math.floorMod(level.getGameTime() + pos.asLong(), tier.interval) == 0;
    }

    /** As pontas "entrega" da rede deste conduto para um tipo (só anda por condutos que levam o tipo) e o nível mais lento dela. */
    private static Net findNet(ServerLevel level, BlockPos start, ConduitCarry.Kind kind) {
        List<End> out = new ArrayList<>();
        ConduitTier slowest = ConduitTier.ORICHALCUM;
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        seen.add(start);
        while (!queue.isEmpty() && seen.size() <= MAX_CONDUITS) {
            BlockPos p = queue.poll();
            BlockState s = level.getBlockState(p);
            if (!(s.getBlock() instanceof RunicConduitBlock block)
                    || !(level.getBlockEntity(p) instanceof RunicConduitBlockEntity c) || !c.carry.carries(kind)) {
                continue;
            }
            if (block.tier().ordinal() < slowest.ordinal()) {
                slowest = block.tier();
            }
            for (Direction d : Direction.values()) {
                Side side = s.getValue(RunicConduitBlock.SIDES.get(d));
                BlockPos n = p.relative(d);
                if (side == Side.CONDUIT && seen.add(n) && level.isLoaded(n)) {
                    queue.add(n);
                } else if (side == Side.LINK) {
                    out.add(new End(n, d.getOpposite()));
                }
            }
        }
        return new Net(out, slowest);
    }

    private int next(Net net) {
        return net.ends().isEmpty() ? 0 : Math.floorMod(this.roundRobin++, net.ends().size());
    }

    private boolean items(ServerLevel level, Net net, BlockPos source, Direction face) {
        ResourceHandler<ItemResource> from = level.getCapability(Capabilities.Item.BLOCK, source, face);
        if (from == null) {
            return false;
        }
        int max = net.tier().items, left = max;
        int start = this.next(net);
        for (int i = 0; i < net.ends().size() && left > 0; i++) {
            End end = net.ends().get((start + i) % net.ends().size());
            if (end.target().equals(source)) {
                continue;
            }
            ResourceHandler<ItemResource> to = level.getCapability(Capabilities.Item.BLOCK, end.target(), end.face());
            if (to != null) {
                left -= ResourceHandlerUtil.moveStacking(from, to, r -> true, left, null);
            }
        }
        return left < max;
    }

    private boolean fluids(ServerLevel level, Net net, BlockPos source, Direction face) {
        ResourceHandler<FluidResource> from = level.getCapability(Capabilities.Fluid.BLOCK, source, face);
        if (from == null) {
            return false;
        }
        int max = net.tier().fluid, left = max;
        int start = this.next(net);
        for (int i = 0; i < net.ends().size() && left > 0; i++) {
            End end = net.ends().get((start + i) % net.ends().size());
            if (end.target().equals(source)) {
                continue;
            }
            ResourceHandler<FluidResource> to = level.getCapability(Capabilities.Fluid.BLOCK, end.target(), end.face());
            if (to != null) {
                left -= ResourceHandlerUtil.move(from, to, r -> true, left, null);
            }
        }
        return left < max;
    }

    private void energy(ServerLevel level, Net net, BlockPos source, Direction face) {
        EnergyHandler from = level.getCapability(Capabilities.Energy.BLOCK, source, face);
        if (from == null) {
            return;
        }
        int left = net.tier().energy;
        int start = this.next(net);
        for (int i = 0; i < net.ends().size() && left > 0; i++) {
            End end = net.ends().get((start + i) % net.ends().size());
            if (end.target().equals(source)) {
                continue;
            }
            EnergyHandler to = level.getCapability(Capabilities.Energy.BLOCK, end.target(), end.face());
            if (to != null) {
                left -= EnergyHandlerUtil.move(from, to, left, null);
            }
        }
    }
}

package com.mistech.mistechmod.ether;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Guarda as ligações do pilar (sincronizadas para desenhar os feixes). */
public class TransmissionPylonBlockEntity extends BlockEntity {
    private final Set<BlockPos> links = new LinkedHashSet<>();

    public TransmissionPylonBlockEntity(BlockPos pos, BlockState state) {
        super(EtherContent.PYLON_BE.get(), pos, state);
    }

    public Set<BlockPos> links() {
        return Set.copyOf(this.links);
    }

    public boolean isLinked(BlockPos other) {
        return this.links.contains(other);
    }

    public boolean canLink() {
        return this.links.size() < PylonMesh.MAX_LINKS;
    }

    public void addLink(BlockPos other) {
        if (this.links.add(other.immutable())) {
            this.changed();
        }
    }

    public void removeLink(BlockPos other) {
        if (this.links.remove(other)) {
            this.changed();
        }
    }

    private void changed() {
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    /** Pilares ligados a este (direta ou indiretamente), incluindo ele mesmo. */
    public java.util.Set<BlockPos> mesh() {
        java.util.Set<BlockPos> seen = new java.util.LinkedHashSet<>();
        java.util.ArrayDeque<BlockPos> queue = new java.util.ArrayDeque<>();
        seen.add(this.worldPosition);
        queue.add(this.worldPosition);
        while (!queue.isEmpty() && seen.size() < 256 && this.level != null) {
            BlockPos at = queue.poll();
            if (this.level.isLoaded(at) && this.level.getBlockEntity(at) instanceof TransmissionPylonBlockEntity be) {
                for (BlockPos next : be.links) {
                    if (seen.add(next)) {
                        queue.add(next);
                    }
                }
            }
        }
        return seen;
    }

    private PylonMode mode() {
        return this.getBlockState().hasProperty(TransmissionPylonBlock.MODE) ? this.getBlockState().getValue(TransmissionPylonBlock.MODE) : PylonMode.RELAY;
    }

    /** Pilar de Entrada: puxa do bloco embaixo e entrega nos blocos embaixo dos Pilares de Saída da malha (e nos Receptores Arcanos perto dela). */
    public static void serverTick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, TransmissionPylonBlockEntity be) {
        net.neoforged.neoforge.transfer.energy.EnergyHandler source = level.getCapability(
                net.neoforged.neoforge.capabilities.Capabilities.Energy.BLOCK, pos.below(), net.minecraft.core.Direction.UP);
        if (source == null || source.getAmountAsLong() <= 0) {
            return;
        }
        if (level.getGameTime() % 20 == (pos.asLong() & 15) || be.targets == null) {
            be.targets = be.findTargets(level);
        }
        int moved = 0;
        for (Target t : be.targets) {
            net.neoforged.neoforge.transfer.energy.EnergyHandler sink = level.getCapability(
                    net.neoforged.neoforge.capabilities.Capabilities.Energy.BLOCK, t.pos(), t.side());
            if (sink == null || sink == source) {
                continue;
            }
            moved += net.neoforged.neoforge.transfer.energy.EnergyHandlerUtil.move(source, sink, PylonMesh.TRANSFER_PER_RECEIVER, null);
        }
        be.lastMoved = moved;
        if (moved > 0 && level.getGameTime() % 20 == 0 && level instanceof net.minecraft.server.level.ServerLevel server) {
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                    4, 0.15, 0.2, 0.15, 0.05);
        }
    }

    private record Target(BlockPos pos, net.minecraft.core.Direction side) {}

    private java.util.@org.jspecify.annotations.Nullable List<Target> targets;
    private int lastMoved;

    private java.util.List<Target> findTargets(net.minecraft.world.level.Level level) {
        java.util.List<Target> list = new java.util.ArrayList<>();
        for (BlockPos p : this.mesh()) {
            BlockState s = level.getBlockState(p);
            if (s.hasProperty(TransmissionPylonBlock.MODE) && s.getValue(TransmissionPylonBlock.MODE) == PylonMode.OUTPUT) {
                list.add(new Target(p.below(), net.minecraft.core.Direction.UP));
            }
            for (BlockPos r : com.mistech.mistechmod.block.arcane.ArcaneNetwork.receiversNear(level, p)) {
                BlockState rs = level.getBlockState(r);
                if (rs.getBlock() instanceof com.mistech.mistechmod.block.arcane.ArcaneReceiverBlock) {
                    list.add(new Target(com.mistech.mistechmod.block.arcane.ArcaneAttachedBlock.attachedPos(r, rs),
                            rs.getValue(com.mistech.mistechmod.block.arcane.ArcaneAttachedBlock.FACING)));
                }
            }
        }
        return list;
    }

    /** Mensagem com o modo e o que a malha alcança. */
    public void showStatus(net.minecraft.server.level.ServerLevel level, net.minecraft.world.entity.player.Player player) {
        java.util.Set<BlockPos> mesh = this.mesh();
        int inputs = 0, outputs = 0;
        for (BlockPos p : mesh) {
            BlockState s = level.getBlockState(p);
            if (s.hasProperty(TransmissionPylonBlock.MODE)) {
                if (s.getValue(TransmissionPylonBlock.MODE) == PylonMode.INPUT) inputs++;
                if (s.getValue(TransmissionPylonBlock.MODE) == PylonMode.OUTPUT) outputs++;
            }
        }
        PylonMode mode = this.mode();
        net.minecraft.network.chat.Component modeName = net.minecraft.network.chat.Component.translatable("message.mistech.pylon.mode." + mode.getSerializedName());
        player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.mistech.pylon.status", modeName, this.links.size(),
                mesh.size(), inputs, outputs).withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));
        if (mode == PylonMode.INPUT && level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.Energy.BLOCK,
                this.worldPosition.below(), net.minecraft.core.Direction.UP) == null) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.mistech.pylon.no_source").withStyle(net.minecraft.ChatFormatting.RED));
        }
        if (mode == PylonMode.OUTPUT && level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.Energy.BLOCK,
                this.worldPosition.below(), net.minecraft.core.Direction.UP) == null) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.mistech.pylon.no_sink").withStyle(net.minecraft.ChatFormatting.RED));
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level != null && !this.level.isClientSide()) {
            PylonMesh.add(this.level, this.worldPosition);
        }
    }

    @Override
    public void setRemoved() {
        if (this.level != null && !this.level.isClientSide()) {
            PylonMesh.remove(this.level, this.worldPosition);
        }
        super.setRemoved();
    }

    /** Quebrado: desfaz as ligações do outro lado. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (this.level != null && !this.level.isClientSide()) {
            for (BlockPos other : List.copyOf(this.links)) {
                if (this.level.isLoaded(other) && this.level.getBlockEntity(other) instanceof TransmissionPylonBlockEntity be) {
                    be.removeLink(this.worldPosition);
                }
            }
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("Links", BlockPos.CODEC.listOf(), new ArrayList<>(this.links));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.links.clear();
        this.links.addAll(input.read("Links", BlockPos.CODEC.listOf()).orElse(List.of()));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}

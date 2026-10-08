package com.mistech.mistechmod.ether;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.registry.MachineContent;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Nexo: mantém o teto da sintonia ({@link #BASE_CAPACITY} + {@link #PER_CELL} por Bateria Aprimorada encostada). */
public class EtherealNexusBlockEntity extends BlockEntity {
    public static final long BASE_CAPACITY = 500_000L;
    public static final long PER_CELL = 1_000_000L;

    private int network;
    private int cells;

    public EtherealNexusBlockEntity(BlockPos pos, BlockState state) {
        super(EtherContent.NEXUS_BE.get(), pos, state);
    }

    public int network() {
        return this.network;
    }

    /** Ao colocar: usa a sintonia do item (se existir) ou cria uma nova. */
    public void attach(ServerLevel level, @Nullable Integer fromItem, @Nullable String name) {
        EtherNetworks nets = EtherNetworks.get(level.getServer());
        EtherNetworks.Net net = fromItem == null ? null : nets.get(fromItem);
        if (net == null) {
            net = nets.create(name);
        } else if (name != null && !name.isBlank()) {
            net.name = name;
            nets.setDirty();
        }
        this.network = net.id;
        this.setChanged();
        this.recount(level, nets, net);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, EtherealNexusBlockEntity nexus) {
        if (!(level instanceof ServerLevel server) || level.getGameTime() % 20 != 0) {
            return;
        }
        EtherNetworks nets = EtherNetworks.get(server.getServer());
        EtherNetworks.Net net = nets.get(nexus.network);
        if (net == null) {
            nexus.attach(server, null, null);
            return;
        }
        nexus.recount(server, nets, net);
        EtherNetworks.rollSecond(net);
        if (net.movedInLastSecond + net.movedOutLastSecond > 0) {
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 4, 0.3, 0.2, 0.3, 0.05);
        }
    }

    private void recount(ServerLevel level, EtherNetworks nets, EtherNetworks.Net net) {
        int count = 0;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            BlockPos side = this.worldPosition.relative(d);
            if (level.getBlockState(side).is(MachineContent.ADVANCED_RUNIC_BATTERY.get())) count++;
            if (level.getBlockState(side.relative(d.getClockWise())).is(MachineContent.ADVANCED_RUNIC_BATTERY.get())) count++;
        }
        this.cells = count;
        nets.setCapacity(net, BASE_CAPACITY + PER_CELL * count);
    }

    public void showInfo(ServerLevel level, Player player) {
        EtherNetworks.Net net = EtherNetworks.get(level.getServer()).get(this.network);
        if (net == null) {
            return;
        }
        player.sendSystemMessage(Component.translatable("message.mistech.ether.nexus_info", net.name, net.id).withStyle(ChatFormatting.LIGHT_PURPLE));
        player.sendSystemMessage(Component.translatable("message.mistech.ether.nexus_energy", net.energy, net.capacity, this.cells)
                .withStyle(ChatFormatting.AQUA));
        player.sendSystemMessage(Component.translatable("message.mistech.ether.nexus_flow", net.movedInLastSecond / 20, net.movedOutLastSecond / 20,
                net.inputs.size(), net.outputs.size()).withStyle(ChatFormatting.GRAY));
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

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        Integer id = components.get(EtherContent.NETWORK.get());
        if (id != null) {
            this.network = id;
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (this.network > 0) {
            components.set(EtherContent.NETWORK.get(), this.network);
        }
    }
}

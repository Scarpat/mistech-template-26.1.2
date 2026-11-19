package com.mistech.mistechmod.folklore.machine;

import com.mistech.mistechmod.folklore.FolkloreContent;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

/**
 * Bomba do Boitatá: a Bomba da Iara para lava. Com uma Escama do Boitatá no espaço (não é gasta) puxa fontes de lava em volta.
 * No <b>Nether</b>, perto de um mar de lava (pelo menos {@link #SEA_SOURCES} fontes a até 4 blocos, para baixo), dá lava sem fim:
 * {@link #SEA_PER_TICK} mB/t, sem secar nada.
 */
public class BoitataPumpBlockEntity extends IaraPumpBlockEntity {
    public static final int SEA_SOURCES = 16;
    public static final int SEA_PER_TICK = 25;

    public BoitataPumpBlockEntity(BlockPos pos, BlockState state) {
        super(FolkMachines.BOITATA_PUMP_BE.get(), pos, state, FolkMachines.LAVA_PUMP_LAYOUT);
    }

    public static boolean isBoitataScale(ItemStack stack) {
        return stack.is(FolkloreContent.BOITATA_SCALE.get());
    }

    @Override
    protected boolean hasCatalyst() {
        return isBoitataScale(this.items.get(SLOT_SCALE));
    }

    @Override
    protected boolean pumps(Fluid fluid) {
        return fluid.isSame(Fluids.LAVA);
    }

    @Override
    protected Fluid endlessFluid() {
        return Fluids.LAVA;
    }

    @Override
    protected int endlessFlow(ServerLevel level, BlockPos pos) {
        this.endlessStatus = ST_INFINITE;
        return this.infiniteNearby() ? SEA_PER_TICK : 0;
    }

    private boolean sea;

    private boolean infiniteNearby() {
        return this.sea;
    }

    @Override
    protected boolean findInfinite(ServerLevel level, BlockPos pos) {
        int count = 0;
        if (level.dimension() == Level.NETHER) {
            for (BlockPos p : BlockPos.betweenClosed(pos.offset(-INFINITE_RADIUS, -INFINITE_DEPTH, -INFINITE_RADIUS),
                    pos.offset(INFINITE_RADIUS, 0, INFINITE_RADIUS))) {
                FluidState f = level.getFluidState(p);
                if (f.isSource() && f.is(FluidTags.LAVA) && ++count >= SEA_SOURCES) {
                    break;
                }
            }
        }
        this.sea = count >= SEA_SOURCES;
        return this.sea;
    }

    @Override
    protected void working(ServerLevel level, BlockPos pos) {
        if (level.getGameTime() % 10 == 0) {
            level.sendParticles(ParticleTypes.LAVA, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 2, 0.2, 0.1, 0.2, 0.0);
            level.sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 3, 0.25, 0.1, 0.25, 0.01);
        }
    }

    @Override
    protected String statusPrefix() {
        return "gui.mistech.boitata_pump.status.";
    }

    public static java.util.List<net.minecraft.network.chat.Component> boitataLines(int status) {
        return java.util.List.of(net.minecraft.network.chat.Component.translatable("gui.mistech.boitata_pump.status."
                + STATUS[Math.max(0, Math.min(STATUS.length - 1, status))]));
    }
}

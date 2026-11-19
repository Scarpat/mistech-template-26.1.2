package com.mistech.mistechmod.purity.area;

import java.util.List;

import com.mistech.mistechmod.Config;
import com.mistech.mistechmod.machine.MachineBlockEntity;
import com.mistech.mistechmod.purity.PurityContent;
import com.mistech.mistechmod.purity.Taint;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Filtro de Arruda: queima Folhas de Arruda e energia para tirar Mácula do chunk dele e dos 8 em volta
 * ({@link Config#areaTaintFilter()} por minuto em cada um). Uma folha dura {@link Config#areaTaintFilterLeaf()} ticks.
 * Só trabalha (e só gasta) quando algum desses chunks tem Mácula.
 * info0 = situação ({@link #STATUS}), info1 = Mácula do chunk dele em décimos, info2 = maior Mácula dos 3×3 em décimos.
 */
public class RueFilterBlockEntity extends MachineBlockEntity {
    public static final int SLOT_LEAF = 0;
    public static final String[] STATUS = {"working", "no_leaf", "no_energy", "clean"};
    public static final int ST_WORKING = 0, ST_NO_LEAF = 1, ST_NO_ENERGY = 2, ST_CLEAN = 3;

    private int leafLeft;
    private int workedTicks;
    private int scanTimer;
    private boolean needed;

    public RueFilterBlockEntity(BlockPos pos, BlockState state) {
        super(AreaTaintContent.RUE_FILTER_BE.get(), pos, state, AreaTaintContent.RUE_FILTER_LAYOUT, 200, 0);
    }

    public static boolean isLeaf(ItemStack stack) {
        return stack.is(PurityContent.RUE_LEAF.get());
    }

    @Override
    protected void tick(ServerLevel level, BlockPos pos, BlockState state) {
        if (--this.scanTimer <= 0) {
            this.scanTimer = 20;
            int worst = 0;
            int cx = pos.getX() >> 4, cz = pos.getZ() >> 4;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    LevelChunk chunk = AreaTaint.loaded(level, cx + dx, cz + dz);
                    if (chunk != null) {
                        worst = Math.max(worst, Math.round(AreaTaint.get(chunk) * 10));
                    }
                }
            }
            this.needed = worst > 0;
            this.info1 = Math.round(AreaTaint.get(level, pos) * 10);
            this.info2 = worst;
            // a cada segundo, tira a parte do minuto que trabalhou
            if (this.workedTicks > 0) {
                AreaTaint.clean(level, pos, 1, Config.areaTaintFilter() * com.mistech.mistechmod.celestial.MachineSky.filter(this.ownerMask) * this.workedTicks / 1200.0F);
                this.workedTicks = 0;
            }
        }
        int status;
        if (!this.needed) {
            status = ST_CLEAN;
        } else {
            ItemStack leaf = this.items.get(SLOT_LEAF);
            if (this.leafLeft <= 0 && isLeaf(leaf)) {
                leaf.shrink(1);
                this.leafLeft = Config.areaTaintFilterLeaf();
                this.setChanged();
            }
            int fe = this.energyStep(Config.areaTaintFilterFe());
            if (this.leafLeft <= 0) {
                status = ST_NO_LEAF;
            } else if (!this.hasEnergy(fe)) {
                status = ST_NO_ENERGY;
            } else {
                status = ST_WORKING;
                this.drainEnergy(fe);
                int step = Math.max(1, this.workStep() / 2);
                this.leafLeft = Math.max(0, this.leafLeft - 1);
                this.workedTicks += step;
            }
        }
        this.info0 = status;
        this.maxProgress = Config.areaTaintFilterLeaf();
        this.progress = Math.min(this.leafLeft, this.maxProgress);
        this.setLit(level, pos, state, status == ST_WORKING);
    }

    @Override
    public void addInfo(List<Component> lines) {
        super.addInfo(lines);
        lines.add(Component.translatable("gui.mistech.rue_filter.status." + STATUS[Math.max(0, Math.min(STATUS.length - 1, this.info0))]));
    }

    /** Linhas da tela (cliente): situação, Mácula aqui e a pior em volta. */
    public static List<Component> infoLines(int status, int here, int worst) {
        return List.of(
                Component.translatable("gui.mistech.rue_filter.status." + STATUS[Math.max(0, Math.min(STATUS.length - 1, status))]),
                Component.translatable("gui.mistech.rue_filter.here", value(here)),
                Component.translatable("gui.mistech.rue_filter.worst", value(worst)));
    }

    /** "37.0" na cor da faixa. */
    public static Component value(int tenths) {
        float v = tenths / 10.0F;
        return Component.literal(String.format(java.util.Locale.ROOT, "%.1f", v)).withColor(Taint.Stage.of(v).color & 0xFFFFFF);
    }

    @Override
    protected void loadExtra(ValueInput input) {
        this.leafLeft = input.getIntOr("LeafLeft", 0);
    }

    @Override
    protected void saveExtra(ValueOutput output) {
        output.putInt("LeafLeft", this.leafLeft);
    }
}

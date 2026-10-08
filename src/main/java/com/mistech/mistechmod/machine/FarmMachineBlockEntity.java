package com.mistech.mistechmod.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Base da Colheitadeira e da Plantadeira: trabalham numa área quadrada à frente (lado 9, ou 13 com runa),
 * olhando uma coluna por vez a cada {@link #STEP_TICKS} ticks (mais rápido com Impulso de Gnomo).
 */
public abstract class FarmMachineBlockEntity extends MachineBlockEntity {
    public static final int STEP_TICKS = 5;
    public static final int BASE_RADIUS = 4;
    public static final int BIG_RADIUS = 6;

    protected int cursor;

    protected FarmMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, MachineLayout layout) {
        super(type, pos, state, layout, 200, 0);
        this.maxProgress = STEP_TICKS * 2;
    }

    /** Raio da área agora (depende da runa). */
    protected abstract int radius();

    /** Centro da área: logo à frente da máquina. */
    protected BlockPos areaCenter(BlockPos pos, BlockState state, int radius) {
        Direction facing = state.hasProperty(MachineBlock.FACING) ? state.getValue(MachineBlock.FACING) : Direction.NORTH;
        return pos.relative(facing, radius + 1);
    }

    /** Próxima coluna (x, z) da área, na altura da máquina. Anda o cursor. */
    protected BlockPos nextColumn(BlockPos pos, BlockState state) {
        int radius = this.radius();
        int side = radius * 2 + 1;
        this.info0 = side;
        if (this.cursor >= side * side) {
            this.cursor = 0;
        }
        BlockPos center = this.areaCenter(pos, state, radius);
        int dx = this.cursor % side - radius;
        int dz = this.cursor / side - radius;
        this.cursor++;
        return new BlockPos(center.getX() + dx, pos.getY(), center.getZ() + dz);
    }

    /** Anda o relógio; true quando é hora de olhar a próxima coluna. */
    protected boolean stepReady() {
        this.progress += this.workStep();
        if (this.progress >= this.maxProgress) {
            this.progress = 0;
            return true;
        }
        return false;
    }

    @Override
    protected void loadExtra(ValueInput input) {
        this.cursor = input.getIntOr("Cursor", 0);
    }

    @Override
    protected void saveExtra(ValueOutput output) {
        output.putInt("Cursor", this.cursor);
    }
}

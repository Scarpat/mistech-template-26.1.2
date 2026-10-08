package com.mistech.mistechmod.beyond;

import com.mistech.mistechmod.machine.MachineBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Gerador de Blaze: queima Vara de Blaze (24.000 FE) ou Pó de Blaze (10.000 FE) a <b>40 FE/t</b>.
 * ×1,5 com lava encostada (qualquer lado) e ×1,5 no Nether (os dois somam: até 90 FE/t).
 * Buffer {@link #CAPACITY} FE; envia até 400 FE/t para cada bloco encostado. Sem Impulso de Gnomo.
 */
public class BlazeGeneratorBlockEntity extends MachineBlockEntity {
    public static final int CAPACITY = 40_000;
    public static final int MAX_OUTPUT_PER_TICK = 400;
    public static final int BASE_RATE = 40;
    public static final int SLOT_FUEL = 0;

    private int burnLeft;

    public BlazeGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(PaleNetherContent.BLAZE_GENERATOR_BE.get(), pos, state, BeyondLayouts.BLAZE_GENERATOR, 0, MAX_OUTPUT_PER_TICK);
    }

    public static int fuelValue(ItemStack stack) {
        return stack.is(Items.BLAZE_ROD) ? 24_000 : stack.is(Items.BLAZE_POWDER) ? 10_000 : 0;
    }

    /** FE/t agora, com os bônus de lava e do Nether. */
    public static int rate(Level level, BlockPos pos) {
        double rate = BASE_RATE;
        for (Direction dir : Direction.values()) {
            if (level.getFluidState(pos.relative(dir)).is(FluidTags.LAVA)) {
                rate *= 1.5;
                break;
            }
        }
        if (level.dimension() == Level.NETHER) {
            rate *= 1.5;
        }
        return (int) rate;
    }

    @Override
    protected boolean canBeBoosted() {
        return false;
    }

    @Override
    protected void tick(ServerLevel level, BlockPos pos, BlockState state) {
        int space = this.layout.capacity - this.storedEnergy();
        ItemStack fuel = this.items.get(SLOT_FUEL);
        if (this.burnLeft <= 0 && space > 0 && fuelValue(fuel) > 0) {
            this.burnLeft = fuelValue(fuel);
            this.maxProgress = this.burnLeft / 10;
            fuel.shrink(1);
            this.setChanged();
        }
        int generated = 0;
        if (this.burnLeft > 0 && space > 0) {
            generated = Math.min(Math.min(rate(level, pos), this.burnLeft), space);
            this.burnLeft -= generated;
            this.addEnergy(generated);
        }
        this.progress = this.burnLeft / 10;
        this.info0 = generated;
        this.pushEnergy(level, pos, MAX_OUTPUT_PER_TICK);
        this.setLit(level, pos, state, generated > 0);
    }

    @Override
    protected void loadExtra(ValueInput input) {
        this.burnLeft = input.getIntOr("BurnLeft", 0);
    }

    @Override
    protected void saveExtra(ValueOutput output) {
        output.putInt("BurnLeft", this.burnLeft);
    }
}

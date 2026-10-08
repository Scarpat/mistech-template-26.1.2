package com.mistech.mistechmod.mage;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.Mistech;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Tear Mágico: tece {@link #FIBERS} Fibras de Cânhamo em 1 Tecido Mágico a cada {@link #WORK_TICKS} ticks gastando
 * {@link #ENERGY_PER_TICK} FE/tick. Runa de Ar no espaço de runa: +1 tecido (gasta 1 uso a cada {@link #RUNE_OPS_PER_USE}).
 * Espaços: 0 = entrada, 1 = runa, 2..5 = saída (mesmo layout do Triturador).
 */
public class MagicLoomBlockEntity extends com.mistech.mistechmod.machine.MachineBlockEntity {
    public static final int WORK_TICKS = 100;
    public static final int ENERGY_PER_TICK = 10;
    public static final int FIBERS = 4;
    public static final int RUNE_OPS_PER_USE = 8;
    public static final int SLOT_INPUT = 0;

    public MagicLoomBlockEntity(BlockPos pos, BlockState state) {
        super(MageContent.MAGIC_LOOM_BE.get(), pos, state, MageContent.LOOM_LAYOUT, 200, 0);
        this.maxProgress = WORK_TICKS * 2;
    }

    private boolean airRune() {
        return this.hasRune(Mistech.AIR_RUNE.get());
    }

    @Override
    protected void tick(ServerLevel level, BlockPos pos, BlockState state) {
        ItemStack input = this.items.get(SLOT_INPUT);
        boolean working = false;
        if (input.is(Mistech.HEMP_FIBER.get()) && input.getCount() >= FIBERS) {
            ItemStack result = new ItemStack(MageContent.MAGIC_CLOTH.get(), this.airRune() ? 2 : 1);
            int cost = this.energyStep(ENERGY_PER_TICK);
            if (this.canOutput(result) && this.hasEnergy(cost)) {
                working = true;
                this.drainEnergy(cost);
                this.progress += this.workStep();
                if (this.progress >= this.maxProgress) {
                    this.progress = 0;
                    input.shrink(FIBERS);
                    if (this.airRune()) {
                        this.countRuneOp(RUNE_OPS_PER_USE);
                    }
                    this.insertOutput(result);
                    level.playSound(null, pos, SoundEvents.UI_LOOM_TAKE_RESULT, SoundSource.BLOCKS, 0.6F, 0.9F + level.getRandom().nextFloat() * 0.2F);
                }
                this.setChanged();
            }
        } else if (this.progress > 0) {
            this.progress = 0;
            this.setChanged();
        }
        this.setLit(level, pos, state, working);
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
        if (!super.canPlaceItemThroughFace(slot, stack, direction)) {
            return false;
        }
        return slot != SLOT_INPUT || stack.is(Mistech.HEMP_FIBER.get());
    }
}

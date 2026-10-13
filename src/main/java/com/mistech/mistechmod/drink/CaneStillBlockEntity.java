package com.mistech.mistechmod.drink;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.machine.MachineBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

/**
 * Alambique: destila 1 Garapa Fermentada em 1 Cachaça a cada {@link #WORK_TICKS} ticks.
 * Calor: fogo, fogueira acesa, lava ou bloco de magma <b>embaixo</b> (de graça), ou uma Runa de Fogo no espaço de runa
 * (1 uso a cada {@link #RUNE_OPS_PER_USE}); sem nenhum dos dois gasta {@link #ENERGY_PER_TICK} FE/t.
 * info0 = fonte de calor (0 nenhuma/energia, 1 fogo embaixo, 2 runa).
 */
public class CaneStillBlockEntity extends MachineBlockEntity {
    public static final int WORK_TICKS = 200;
    public static final int ENERGY_PER_TICK = 20;
    public static final int RUNE_OPS_PER_USE = 8;
    public static final int SLOT_INPUT = 0;
    public static final int HEAT_NONE = 0, HEAT_FIRE = 1, HEAT_RUNE = 2;

    public CaneStillBlockEntity(BlockPos pos, BlockState state) {
        super(DrinkContent.CANE_STILL_BE.get(), pos, state, DrinkContent.STILL_LAYOUT, 200, 0);
        this.maxProgress = WORK_TICKS * 2;
    }

    public static boolean heatedFromBelow(Level level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        if (below.is(BlockTags.FIRE) || below.is(Blocks.MAGMA_BLOCK) || below.getFluidState().is(Fluids.LAVA)
                || below.getFluidState().is(Fluids.FLOWING_LAVA)) {
            return true;
        }
        return below.is(BlockTags.CAMPFIRES) && below.hasProperty(CampfireBlock.LIT) && below.getValue(CampfireBlock.LIT);
    }

    @Override
    protected void tick(ServerLevel level, BlockPos pos, BlockState state) {
        ItemStack input = this.items.get(SLOT_INPUT);
        boolean fire = heatedFromBelow(level, pos);
        boolean rune = !fire && this.hasRune(Mistech.FIRE_RUNE.get());
        this.info0 = fire ? HEAT_FIRE : rune ? HEAT_RUNE : HEAT_NONE;
        boolean working = false;
        if (input.is(DrinkContent.FERMENTED_GARAPA.get())) {
            ItemStack result = new ItemStack(DrinkContent.CACHACA.get());
            int cost = fire || rune ? 0 : this.energyStep(ENERGY_PER_TICK);
            if (this.canOutput(result) && (cost == 0 || this.hasEnergy(cost))) {
                working = true;
                if (cost > 0) {
                    this.drainEnergy(cost);
                }
                this.progress += this.workStep();
                if (this.progress >= this.maxProgress) {
                    this.progress = 0;
                    input.shrink(1);
                    if (rune) {
                        this.countRuneOp(RUNE_OPS_PER_USE);
                    }
                    this.insertOutput(result);
                    level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.6F, 0.8F + level.getRandom().nextFloat() * 0.2F);
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
        return slot != SLOT_INPUT || stack.is(DrinkContent.FERMENTED_GARAPA.get());
    }
}

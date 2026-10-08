package com.mistech.mistechmod.block;

import com.mistech.mistechmod.registry.MistechBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;

/**
 * Enche 1 nível de essência a cada {@link #TICKS_PER_LEVEL} ticks com energia,
 * gastando {@link #ENERGY_PER_TICK} FE/tick (3 minutos para encher com 3 níveis).
 */
public class EssenceExtractorBlockEntity extends BlockEntity {
    public static final int CAPACITY = 10_000;
    public static final int MAX_INPUT_PER_TICK = 100;
    public static final int ENERGY_PER_TICK = 10;
    public static final int TICKS_PER_LEVEL = 60 * 20;

    private int progress;

    private final SimpleEnergyHandler energy = new SimpleEnergyHandler(CAPACITY, MAX_INPUT_PER_TICK, 0) {
        @Override
        protected void onEnergyChanged(int previousAmount) {
            EssenceExtractorBlockEntity.this.setChanged();
        }
    };

    public EssenceExtractorBlockEntity(BlockPos pos, BlockState state) {
        super(MistechBlockEntities.ESSENCE_EXTRACTOR.get(), pos, state);
    }

    public EnergyHandler getEnergyHandler() {
        return this.energy;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, EssenceExtractorBlockEntity extractor) {
        int current = state.getValue(EssenceExtractorBlock.LEVEL);
        boolean working = current < EssenceExtractorBlock.MAX_LEVEL && extractor.energy.getAmountAsInt() >= ENERGY_PER_TICK;
        if (state.getValue(EssenceExtractorBlock.LIT) != working) {
            state = state.setValue(EssenceExtractorBlock.LIT, working);
            level.setBlock(pos, state, Block.UPDATE_ALL);
        }
        if (!working) {
            return;
        }
        extractor.energy.set(extractor.energy.getAmountAsInt() - ENERGY_PER_TICK);
        extractor.progress++;
        // Impulso de Gnomo: +50% (1 tick extra a cada 2), gastando a energia desse tick extra
        if (com.mistech.mistechmod.machine.GnomeBoost.extraStep(level, pos)
                && extractor.energy.getAmountAsInt() >= ENERGY_PER_TICK) {
            extractor.energy.set(extractor.energy.getAmountAsInt() - ENERGY_PER_TICK);
            extractor.progress++;
        }
        if (extractor.progress >= TICKS_PER_LEVEL) {
            extractor.progress = 0;
            level.setBlock(pos, state.setValue(EssenceExtractorBlock.LEVEL, current + 1), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.7F, 0.6F);
        }
        extractor.setChanged();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.progress = input.getIntOr("Progress", 0);
        this.energy.deserialize(input);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Progress", this.progress);
        this.energy.serialize(output);
    }
}

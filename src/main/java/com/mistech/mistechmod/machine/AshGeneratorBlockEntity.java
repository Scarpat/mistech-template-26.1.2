package com.mistech.mistechmod.machine;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.registry.MistechBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Gerador de Cinzas: queima o que sobra do incenso (tag {@code mistech:ash_fuels}).
 * Cinzas 4.000 FE a 40 FE/t, Bloco de Cinzas 18.000 FE a 40 FE/t, Resíduo Mágico 12.000 FE a 60 FE/t.
 * Buffer 20.000 FE; envia até 200 FE/t para cada bloco encostado. Sem Impulso de Gnomo.
 */
public class AshGeneratorBlockEntity extends MachineBlockEntity {
    public static final int MAX_OUTPUT_PER_TICK = 200;
    public static final int SLOT_FUEL = 0;

    /** FE que falta sair do combustível que está queimando e o ritmo dele. */
    private int burnLeft;
    private int burnRate;

    public AshGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(MistechBlockEntities.ASH_GENERATOR.get(), pos, state, MachineLayouts.ASH_GENERATOR, 0, MAX_OUTPUT_PER_TICK);
    }

    /** FE total e FE/t de um combustível. */
    public static int[] fuelValue(ItemStack stack) {
        if (stack.is(Mistech.ASH_BLOCK_ITEM.get())) {
            return new int[] {18_000, 40};
        }
        if (stack.is(Mistech.MAGIC_RESIDUE.get())) {
            return new int[] {12_000, 60};
        }
        return new int[] {4_000, 40}; // Cinzas de Incenso e qualquer outro item da tag
    }

    @Override
    protected boolean canBeBoosted() {
        return false;
    }

    @Override
    protected void tick(ServerLevel level, BlockPos pos, BlockState state) {
        int space = this.layout.capacity - this.storedEnergy();
        ItemStack fuel = this.items.get(SLOT_FUEL);
        if (this.burnLeft <= 0 && space > 0 && !fuel.isEmpty() && this.layout.fuelFilter.test(fuel)) {
            int[] value = fuelValue(fuel);
            this.burnLeft = value[0];
            this.burnRate = value[1];
            this.maxProgress = value[0] / 10;
            fuel.shrink(1);
            this.setChanged();
        }

        int generated = 0;
        if (this.burnLeft > 0 && space > 0) {
            generated = Math.min(Math.min(this.burnRate, this.burnLeft), space);
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
        this.burnRate = input.getIntOr("BurnRate", 40);
    }

    @Override
    protected void saveExtra(ValueOutput output) {
        output.putInt("BurnLeft", this.burnLeft);
        output.putInt("BurnRate", this.burnRate);
    }
}

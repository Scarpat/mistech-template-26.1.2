package com.mistech.mistechmod.endgame;

import com.mistech.mistechmod.spellcraft.conjurer.RunicConjurerBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Conjurador Celeste: dois Cristais de Feitiço (espaços 0 e {@link #SLOT_CRYSTAL_B}) que se alternam a cada lançamento.
 * Magias com {@link #POWER}× de poder e {@link #COOLDOWN}× da recarga.
 */
public class CelestialConjurerBlockEntity extends RunicConjurerBlockEntity {
    public static final int SLOT_CRYSTAL_B = 4;
    public static final double POWER = 1.25;
    public static final double COOLDOWN = 0.75;

    private int active = SLOT_CRYSTAL;

    public CelestialConjurerBlockEntity(BlockPos pos, BlockState state) {
        super(EndgameContent.CELESTIAL_CONJURER_BE.get(), pos, state, EndgameContent.CELESTIAL_CONJURER_LAYOUT);
    }

    @Override
    protected int crystalSlot() {
        // sem cristal no ativo: usa o outro
        if (!isCrystal(this.items.get(this.active))) {
            int other = this.other();
            if (isCrystal(this.items.get(other))) {
                return other;
            }
        }
        return this.active;
    }

    private int other() {
        return this.active == SLOT_CRYSTAL ? SLOT_CRYSTAL_B : SLOT_CRYSTAL;
    }

    @Override
    protected void afterCast() {
        int other = this.other();
        if (isCrystal(this.items.get(other))) {
            this.active = other;
            this.progress = 0;
        }
    }

    @Override
    protected double powerFactor() {
        return POWER;
    }

    @Override
    protected double cooldownFactor() {
        return COOLDOWN;
    }

    @Override
    protected void loadExtra(ValueInput input) {
        super.loadExtra(input);
        this.active = input.getIntOr("Active", SLOT_CRYSTAL) == SLOT_CRYSTAL_B ? SLOT_CRYSTAL_B : SLOT_CRYSTAL;
    }

    @Override
    protected void saveExtra(ValueOutput output) {
        super.saveExtra(output);
        output.putInt("Active", this.active);
    }
}

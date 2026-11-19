package com.mistech.mistechmod.endgame;

import com.mistech.mistechmod.machine.AdvancedRunicBatteryBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Bateria Celeste: 10.000.000 FE, sem fio como a Aprimorada, só que maior em tudo.
 * Suga de geradores a até {@link #PULL_RANGE} blocos e energiza máquinas a até {@link #PUSH_RANGE}, {@link #PER_LINK} FE/t cada;
 * pelas faces troca até {@link #MAX_IO} FE/t.
 */
public class CelestialBatteryBlockEntity extends AdvancedRunicBatteryBlockEntity {
    public static final int CAPACITY = 10_000_000;
    public static final int MAX_IO = 100_000;
    public static final int PULL_RANGE = 5;
    public static final int PUSH_RANGE = 8;
    public static final int PER_LINK = 10_000;

    public CelestialBatteryBlockEntity(BlockPos pos, BlockState state) {
        super(EndgameContent.CELESTIAL_BATTERY_BE.get(), pos, state, EndgameContent.CELESTIAL_BATTERY_LAYOUT, MAX_IO);
    }

    @Override
    protected int perLink() {
        return PER_LINK;
    }

    @Override
    protected int pullRange() {
        return PULL_RANGE;
    }

    @Override
    protected int pushRange() {
        return PUSH_RANGE;
    }
}

package com.mistech.mistechmod.block.ritual;

import com.mistech.mistechmod.registry.RitualContent;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** Pedestal Ritual: guarda 1 ingrediente do ritual. Travado enquanto o altar vizinho faz um ritual. */
public class RitualPedestalBlockEntity extends SingleItemBlockEntity {
    public RitualPedestalBlockEntity(BlockPos pos, BlockState state) {
        super(RitualContent.RITUAL_PEDESTAL_BE.get(), pos, state);
    }

    @Override
    protected boolean isLocked() {
        if (this.level == null) {
            return false;
        }
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                BlockPos pos = this.worldPosition.offset(dx, 0, dz);
                if (this.level.getBlockEntity(pos) instanceof ImpureAltarBlockEntity altar && altar.isRunning()
                        && com.mistech.mistechmod.ritual.AltarStructure.usesPedestal(pos, altar.tier(), this.worldPosition)) {
                    return true;
                }
                if (this.level.getBlockEntity(pos) instanceof com.mistech.mistechmod.purity.block.SanctuaryFontBlockEntity font && font.isRunning()
                        && com.mistech.mistechmod.purity.SanctuaryStructure.pedestals(pos).contains(this.worldPosition)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean locked() {
        return this.isLocked();
    }
}

package com.mistech.mistechmod.refining;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Lavadora de Essência (tier 3× da cadeia de minério): minério + {@value #ESSENCE} mB de Essência Impura →
 * 3 Torrões Impregnados (triturar cada um dá 1 pó) e 10% de Borra Impura. {@value #SECONDS} s, {@value #FE} FE/t.
 */
public class EssenceWasherBlockEntity extends RefineryBlockEntity {
    public static final int ESSENCE = 100, SECONDS = 10, FE = 40;

    public EssenceWasherBlockEntity(BlockPos pos, BlockState state) {
        super(RefiningContent.ESSENCE_WASHER_BE.get(), pos, state, RefiningContent.WASHER_LAYOUT);
    }

    @Override
    protected float taintPerMinute() {
        return com.mistech.mistechmod.Config.areaTaintWasher();
    }

    @Override
    protected OreRefiningRecipe.Stage stage() {
        return OreRefiningRecipe.Stage.WASHING;
    }

    @Override
    protected boolean pure() {
        return false;
    }

    @Override
    protected int essencePerOre() {
        return ESSENCE;
    }

    @Override
    protected int ticksPerOre() {
        return SECONDS * 20;
    }

    @Override
    protected int fePerTick() {
        return FE;
    }

    @Override
    protected void finished(ServerLevel level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.5F, 0.7F);
        level.sendParticles(ParticleTypes.WITCH, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 10, 0.25, 0.1, 0.25, 0.02);
    }
}

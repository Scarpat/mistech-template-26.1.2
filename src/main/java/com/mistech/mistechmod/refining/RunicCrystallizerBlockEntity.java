package com.mistech.mistechmod.refining;

import com.mistech.mistechmod.item.RuneElement;
import com.mistech.mistechmod.purity.block.CrystallizerBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Cristalizador Rúnico (tier 4× da cadeia): minério + {@value #ESSENCE} mB de Essência Purificada + luz → 4 Cristais de
 * Minério (triturar cada um dá 1 pó), 15% de Resíduo Mágico e 5% do metal irmão do mod (ferro → mitril, ouro → oricalco,
 * cobre → adamantina). {@value #SECONDS} s com sol, {@value #FE} FE/t.
 * <p>
 * Luz igual ao Cristalizador da pureza: sol 100% (Solstício 200%), lua cheia 50%, escuro/chuva/telhado = parado.
 * Com a Runa da Luz engastada trabalha a pelo menos 75% sem sol.
 */
public class RunicCrystallizerBlockEntity extends RefineryBlockEntity {
    public static final int ESSENCE = 250, SECONDS = 20, FE = 60;

    public RunicCrystallizerBlockEntity(BlockPos pos, BlockState state) {
        super(RefiningContent.RUNIC_CRYSTALLIZER_BE.get(), pos, state, RefiningContent.CRYSTALLIZER_LAYOUT);
    }

    @Override
    protected OreRefiningRecipe.Stage stage() {
        return OreRefiningRecipe.Stage.CRYSTALLIZING;
    }

    @Override
    protected boolean pure() {
        return true;
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
    protected double lightFactor(ServerLevel level, BlockPos pos) {
        double sun = CrystallizerBlockEntity.lightRate(level, pos) / 20.0;
        if (this.socketActive(RuneElement.LIGHT)) {
            sun = Math.max(sun, 0.75);
        }
        return sun;
    }

    @Override
    protected void finished(ServerLevel level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.AMETHYST_CLUSTER_PLACE, SoundSource.BLOCKS, 0.7F, 1.2F);
        level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 8, 0.25, 0.2, 0.25, 0.02);
    }
}

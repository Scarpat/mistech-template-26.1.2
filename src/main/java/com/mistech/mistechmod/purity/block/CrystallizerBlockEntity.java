package com.mistech.mistechmod.purity.block;

import com.mistech.mistechmod.purity.PurityContent;
import com.mistech.mistechmod.purity.Taint;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.MoonPhase;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Cristaliza a Essência Purificada com luz: sol a céu aberto (100%) ou luar de lua cheia (50%). Sem luz, pausa. */
public class CrystallizerBlockEntity extends BlockEntity {
    public static final int HARDEN_TICKS = 15 * 20;

    private int progress;

    public CrystallizerBlockEntity(BlockPos pos, BlockState state) {
        super(PurityContent.CRYSTALLIZER_BE.get(), pos, state);
    }

    /** Quanto anda por segundo agora (20 = sol, 10 = lua cheia, 0 = sem luz). */
    public static int lightRate(Level level, BlockPos pos) {
        BlockPos above = pos.above();
        if (!Taint.openSky(level, above) || level.isRainingAt(above)) {
            return 0;
        }
        if (com.mistech.mistechmod.celestial.Celestial.isActive(level, com.mistech.mistechmod.celestial.CelestialEvent.ECLIPSE)) {
            return 0; // o sol sumiu
        }
        if (com.mistech.mistechmod.celestial.Celestial.isActive(level, com.mistech.mistechmod.celestial.CelestialEvent.SILVER_MOON)) {
            return 20; // Lua de Prata: tão forte quanto o sol
        }
        if (level.isBrightOutside()) {
            return com.mistech.mistechmod.celestial.Celestial.isActive(level, com.mistech.mistechmod.celestial.CelestialEvent.SOLSTICE) ? 40 : 20;
        }
        return level.environmentAttributes().getValue(EnvironmentAttributes.MOON_PHASE, pos) == MoonPhase.FULL_MOON ? 10 : 0;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CrystallizerBlockEntity be) {
        if (!state.getValue(CrystallizerBlock.FILLED) || level.getGameTime() % 20 != 0 || !(level instanceof ServerLevel server)) {
            return;
        }
        int rate = lightRate(level, pos);
        if (rate <= 0) {
            return;
        }
        be.progress += com.mistech.mistechmod.machine.GnomeBoost.isBoosted(level, pos) ? rate * 3 / 2 : rate;
        server.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, 3, 0.12, 0.05, 0.12, 0.0);
        if (be.progress >= HARDEN_TICKS) {
            be.progress = 0;
            level.setBlock(pos, state.setValue(CrystallizerBlock.FILLED, false), Block.UPDATE_ALL);
            Block.popResource(level, pos.above(), new ItemStack(PurityContent.PURE_MAGIC_SHARD.get()));
            level.playSound(null, pos, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.BLOCKS, 1.0F, 1.4F);
            server.sendParticles(ParticleTypes.GLOW, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 20, 0.2, 0.1, 0.2, 0.05);
        }
        be.setChanged();
    }

    public Component status() {
        if (this.level == null || !this.getBlockState().getValue(CrystallizerBlock.FILLED)) {
            return Component.translatable("message.mistech.crystallizer.empty");
        }
        int rate = lightRate(this.level, this.worldPosition);
        int percent = this.progress * 100 / HARDEN_TICKS;
        return Component.translatable(rate > 0 ? "message.mistech.crystallizer.working" : "message.mistech.crystallizer.no_light", percent);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.progress = input.getIntOr("Progress", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Progress", this.progress);
    }
}

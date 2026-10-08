package com.mistech.mistechmod.block;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.item.IncenseItem;
import com.mistech.mistechmod.registry.MistechBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

/**
 * Endurece a essência em {@link #HARDEN_TICKS} ticks enquanto houver incenso aceso por perto.
 * Sem incenso, o progresso fica parado (não perde).
 */
public class MagicCompressorBlockEntity extends BlockEntity {
    public static final int HARDEN_TICKS = 15 * 20;
    /** Distância até o jogador com incenso aceso. */
    public static final double PLAYER_RANGE = 6.0;

    private int progress;
    /** Cheio de Essência Ígnea: endurece em Fragmento Choroso. */
    private boolean igneous;

    public void setIgneous(boolean igneous) {
        this.igneous = igneous;
        this.setChanged();
    }

    public MagicCompressorBlockEntity(BlockPos pos, BlockState state) {
        super(MistechBlockEntities.MAGIC_COMPRESSOR.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MagicCompressorBlockEntity compressor) {
        if (!state.getValue(MagicCompressorBlock.FILLED) || level.getGameTime() % 20 != 0
                || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!hasIncenseNearby(serverLevel, pos)) {
            return;
        }
        compressor.progress += com.mistech.mistechmod.machine.GnomeBoost.isBoosted(level, pos) ? 30 : 20;
        serverLevel.sendParticles(ParticleTypes.WITCH, pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5,
                4, 0.12, 0.05, 0.12, 0.0);
        if (compressor.progress >= HARDEN_TICKS) {
            compressor.progress = 0;
            level.setBlock(pos, state.setValue(MagicCompressorBlock.FILLED, false), Block.UPDATE_ALL);
            Block.popResource(level, pos.above(), new ItemStack(compressor.igneous
                    ? com.mistech.mistechmod.beyond.PaleNetherContent.CRYING_SHARD.get() : Mistech.IMPURE_MAGIC_SHARD.get()));
            compressor.igneous = false;
            level.playSound(null, pos, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.BLOCKS, 1.0F, 0.7F);
            serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5,
                    20, 0.2, 0.1, 0.2, 0.05);
        }
        compressor.setChanged();
    }

    /** Jogador perto com incenso aceso no inventário, ou Porta-Incenso aceso cuja área alcança este bloco. */
    public static boolean hasIncenseNearby(ServerLevel level, BlockPos pos) {
        for (Player player : level.getEntitiesOfClass(Player.class, new AABB(pos).inflate(PLAYER_RANGE))) {
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (IncenseItem.isIncense(stack) && IncenseItem.isLit(stack)) {
                    return true;
                }
            }
        }

        double radius = IncenseHolderBlockEntity.MAX_EFFECT_RADIUS;
        int minCx = SectionPos.blockToSectionCoord(pos.getX() - (int) radius);
        int maxCx = SectionPos.blockToSectionCoord(pos.getX() + (int) radius);
        int minCz = SectionPos.blockToSectionCoord(pos.getZ() - (int) radius);
        int maxCz = SectionPos.blockToSectionCoord(pos.getZ() + (int) radius);
        for (int cx = minCx; cx <= maxCx; cx++) {
            for (int cz = minCz; cz <= maxCz; cz++) {
                if (!level.hasChunk(cx, cz)) {
                    continue;
                }
                LevelChunk chunk = level.getChunk(cx, cz);
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (be instanceof IncenseHolderBlockEntity holder
                            && be.getBlockState().hasProperty(IncenseHolderBlock.LIT)
                            && be.getBlockState().getValue(IncenseHolderBlock.LIT)
                            && be.getBlockPos().getCenter().distanceToSqr(pos.getCenter())
                                    <= holder.getEffectRadius() * holder.getEffectRadius()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.progress = input.getIntOr("Progress", 0);
        this.igneous = input.getBooleanOr("Igneous", false);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Progress", this.progress);
        output.putBoolean("Igneous", this.igneous);
    }
}

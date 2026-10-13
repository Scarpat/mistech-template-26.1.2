package com.mistech.mistechmod.boitata;

import java.util.UUID;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Ovo do Boitatá: choca em {@link #HATCH_TICKS} (3 dias) enquanto estiver <b>quente</b> (lava, fogo, magma ou fogueira acesa
 * embaixo ou encostado). Runa da Vida (botão direito) adianta {@link #LIFE_BONUS}; a Ampulheta de Bolso por perto deixa 2×.
 */
public class BoitataEggBlockEntity extends BlockEntity {
    public static final int HATCH_TICKS = 72000;
    public static final int LIFE_BONUS = 7200;

    private int progress;
    private @Nullable UUID owner;
    private boolean warm;

    public BoitataEggBlockEntity(BlockPos pos, BlockState state) {
        super(BoitataGear.EGG_BE.get(), pos, state);
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
        this.setChanged();
    }

    public int progress() {
        return this.progress;
    }

    public boolean warm() {
        return this.warm;
    }

    public void addProgress(int ticks) {
        this.progress = Math.min(HATCH_TICKS, this.progress + ticks);
        this.setChanged();
    }

    public static boolean isHeat(BlockState state) {
        if (state.is(BlockTags.FIRE) || state.is(Blocks.MAGMA_BLOCK) || state.getFluidState().is(FluidTags.LAVA)) {
            return true;
        }
        return state.is(BlockTags.CAMPFIRES) && state.hasProperty(CampfireBlock.LIT) && state.getValue(CampfireBlock.LIT);
    }

    public static boolean heated(Level level, BlockPos pos) {
        if (isHeat(level.getBlockState(pos.below()))) {
            return true;
        }
        for (Direction d : Direction.Plane.HORIZONTAL) {
            if (isHeat(level.getBlockState(pos.relative(d))) || isHeat(level.getBlockState(pos.relative(d).below()))) {
                return true;
            }
        }
        return false;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BoitataEggBlockEntity egg) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        if (level.getGameTime() % 20 == 0) {
            boolean warm = heated(level, pos);
            if (warm != egg.warm) {
                egg.warm = warm;
                egg.sync();
            }
        }
        if (!egg.warm) {
            return;
        }
        egg.progress++;
        if (egg.progress % 200 == 0) {
            egg.setChanged();
            if (egg.progress > HATCH_TICKS * 3 / 4) {
                level.playSound(null, pos, SoundEvents.SNIFFER_EGG_CRACK, SoundSource.BLOCKS, 0.6F, 0.8F);
            }
        }
        if (egg.progress >= HATCH_TICKS) {
            egg.hatch(server, pos);
        }
    }

    private void hatch(ServerLevel level, BlockPos pos) {
        BoitataMountEntity baby = BoitataGear.MOUNT.get().create(level, EntitySpawnReason.BREEDING);
        level.removeBlock(pos, false);
        level.playSound(null, pos, SoundEvents.SNIFFER_EGG_HATCH, SoundSource.BLOCKS, 1.0F, 0.7F);
        level.sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 40, 0.4, 0.4, 0.4, 0.05);
        level.sendParticles(ParticleTypes.LAVA, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 10, 0.3, 0.3, 0.3, 0.0);
        if (baby == null) {
            return;
        }
        baby.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.getRandom().nextFloat() * 360.0F, 0.0F);
        Player player = this.owner != null ? level.getPlayerByUUID(this.owner) : null;
        if (player == null) {
            player = level.getNearestPlayer(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 16.0, false);
        }
        if (player != null) {
            baby.bindTo(player);
            player.sendSystemMessage(Component.translatable("message.mistech.boitata_egg.hatched").withStyle(net.minecraft.ChatFormatting.GOLD));
        } else if (this.owner != null) {
            baby.setOwnerReference(net.minecraft.world.entity.EntityReference.of(this.owner));
            baby.setTame(true, true);
        }
        level.addFreshEntity(baby);
    }

    private void sync() {
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Progress", this.progress);
        output.putBoolean("Warm", this.warm);
        if (this.owner != null) {
            output.store("Owner", UUIDUtil.CODEC, this.owner);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.progress = input.getIntOr("Progress", 0);
        this.warm = input.getBooleanOr("Warm", false);
        this.owner = input.read("Owner", UUIDUtil.CODEC).orElse(null);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}

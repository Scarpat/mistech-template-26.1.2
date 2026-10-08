package com.mistech.mistechmod.event;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.entity.GnomeEntity;
import com.mistech.mistechmod.registry.GnomeContent;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Isca de Gnomo na fogueira acesa: a isca queima (fumaça) e depois de {@link #SUMMON_DELAY} ticks
 * um gnomo aparece perto da fogueira.
 */
@EventBusSubscriber(modid = Mistech.MODID)
public final class GnomeEvents {
    public static final int SUMMON_DELAY = 60;

    private record PendingGnome(ResourceKey<Level> dimension, BlockPos campfire, int[] ticksLeft) {}

    private static final List<PendingGnome> PENDING = new ArrayList<>();

    private GnomeEvents() {}

    @SubscribeEvent
    static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        if (!stack.is(GnomeContent.GNOME_BAIT.get())) {
            return;
        }
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof CampfireBlock) || !state.getValue(CampfireBlock.LIT)) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (level instanceof ServerLevel server) {
            if (!event.getEntity().getAbilities().instabuild) {
                stack.shrink(1);
            }
            server.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5,
                    6, 0.2, 0.1, 0.2, 0.02);
            server.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.6F, 1.4F);
            PENDING.add(new PendingGnome(server.dimension(), pos.immutable(), new int[] {SUMMON_DELAY}));
        }
    }

    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        Iterator<PendingGnome> it = PENDING.iterator();
        while (it.hasNext()) {
            PendingGnome pending = it.next();
            ServerLevel level = event.getServer().getLevel(pending.dimension());
            if (level == null || !level.isLoaded(pending.campfire())) {
                it.remove();
                continue;
            }
            BlockPos pos = pending.campfire();
            int left = --pending.ticksLeft()[0];
            if (left % 10 == 0 && left > 0) {
                level.sendParticles(ParticleTypes.WAX_ON, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 3, 0.3, 0.3, 0.3, 0.0);
            }
            if (left <= 0) {
                it.remove();
                summon(level, pos);
            }
        }
    }

    /** Coloca o gnomo num lugar livre ao lado da fogueira. */
    private static void summon(ServerLevel level, BlockPos campfire) {
        GnomeEntity gnome = GnomeContent.GNOME.get().create(level, EntitySpawnReason.MOB_SUMMONED);
        if (gnome == null) {
            return;
        }
        BlockPos spot = campfire.above();
        for (int attempt = 0; attempt < 16; attempt++) {
            BlockPos candidate = campfire.offset(level.getRandom().nextInt(5) - 2, 0, level.getRandom().nextInt(5) - 2);
            if (!candidate.equals(campfire) && level.getBlockState(candidate).getCollisionShape(level, candidate).isEmpty()
                    && level.getBlockState(candidate.above()).getCollisionShape(level, candidate.above()).isEmpty()
                    && level.getBlockState(candidate.below()).isFaceSturdy(level, candidate.below(), net.minecraft.core.Direction.UP)) {
                spot = candidate;
                break;
            }
        }
        gnome.snapTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, level.getRandom().nextFloat() * 360.0F, 0.0F);
        gnome.setPersistenceRequired();
        level.addFreshEntity(gnome);
        level.sendParticles(ParticleTypes.POOF, spot.getX() + 0.5, spot.getY() + 0.5, spot.getZ() + 0.5, 20, 0.3, 0.4, 0.3, 0.03);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, spot.getX() + 0.5, spot.getY() + 0.6, spot.getZ() + 0.5, 8, 0.3, 0.4, 0.3, 0.0);
        level.playSound(null, spot, SoundEvents.VILLAGER_CELEBRATE, SoundSource.NEUTRAL, 1.0F, 1.8F);
    }
}

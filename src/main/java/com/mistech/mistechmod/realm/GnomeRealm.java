package com.mistech.mistechmod.realm;

import java.util.Optional;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.registry.RealmContent;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Entrar e sair da Dimensão dos Gnomos.
 * - Entra: dormindo sob a Visão Psicodélica com um Apanha Sonho perto da cama.
 * - Sai: dormindo numa cama de lá, pedindo ao Rei Gnomo (agachado + clique) ou morrendo.
 *   Morrer lá não mata: o jogador acorda na cama de onde veio, com todos os itens.
 * A cama de volta fica guardada nos dados do jogador ({@link #RETURN_TAG}).
 */
@EventBusSubscriber(modid = Mistech.MODID)
public final class GnomeRealm {
    public static final String RETURN_TAG = "mistech_realm_return";
    /** Quanto tempo deitado numa cama da dimensão até acordar em casa (2 s). */
    public static final int SLEEP_TO_LEAVE = 40;

    private GnomeRealm() {}

    public static boolean isRealm(Level level) {
        return level.dimension() == RealmContent.REALM;
    }

    // ----- entrar -----

    /** Chamado quando o jogador acorda sob a Visão Psicodélica com um Apanha Sonho perto da cama. */
    public static void dreamInto(ServerPlayer player, BlockPos bed) {
        ServerLevel realm = player.level().getServer().getLevel(RealmContent.REALM);
        if (realm == null) {
            player.sendSystemMessage(Component.translatable("message.mistech.gnome_realm.dream"));
            return;
        }
        // guarda a cama para a volta
        CompoundTag back = new CompoundTag();
        back.putString("Dim", player.level().dimension().identifier().toString());
        back.putInt("X", bed.getX());
        back.putInt("Y", bed.getY());
        back.putInt("Z", bed.getZ());
        player.getPersistentData().put(RETURN_TAG, back);

        BlockPos arrival = GnomeRealmHouse.ensureBuilt(realm);
        player.teleport(new TeleportTransition(realm, Vec3.atBottomCenterOf(arrival), Vec3.ZERO, 180.0F, 0.0F,
                TeleportTransition.DO_NOTHING));
        player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 100, 0, false, false));
        realm.playSound(null, arrival, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 0.6F);
        player.sendSystemMessage(Component.translatable("message.mistech.gnome_realm.enter"));
    }

    // ----- sair -----

    /** Leva o jogador de volta para a cama de onde ele veio (ou o spawn do mundo). */
    public static void sendHome(ServerPlayer player) {
        var server = player.level().getServer();
        ServerLevel target = server.overworld();
        Vec3 pos = null;
        CompoundTag back = player.getPersistentData().getCompoundOrEmpty(RETURN_TAG);
        if (back.contains("Dim")) {
            Identifier dimId = Identifier.tryParse(back.getStringOr("Dim", "minecraft:overworld"));
            if (dimId != null) {
                ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, dimId));
                if (level != null && !isRealm(level)) {
                    target = level;
                    BlockPos bed = new BlockPos(back.getIntOr("X", 0), back.getIntOr("Y", 64), back.getIntOr("Z", 0));
                    pos = standUpPosition(level, bed);
                }
            }
        }
        if (pos == null) {
            BlockPos spawn = target.getRespawnData().pos();
            pos = Vec3.atBottomCenterOf(target.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawn));
        }
        player.getPersistentData().remove(RETURN_TAG);
        if (player.isSleeping()) {
            player.stopSleepInBed(true, false);
        }
        player.teleport(new TeleportTransition(target, pos, Vec3.ZERO, player.getYRot(), 0.0F, TeleportTransition.DO_NOTHING));
        player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 60, 0, false, false));
        player.sendSystemMessage(Component.translatable("message.mistech.gnome_realm.wake"));
    }

    private static Vec3 standUpPosition(ServerLevel level, BlockPos bed) {
        BlockState state = level.getBlockState(bed);
        if (state.getBlock() instanceof BedBlock) {
            Optional<Vec3> standUp = BedBlock.findStandUpPosition(net.minecraft.world.entity.EntityType.PLAYER, level, bed,
                    state.getValue(BedBlock.FACING), 0.0F);
            if (standUp.isPresent()) {
                return standUp.get();
            }
        }
        return Vec3.atBottomCenterOf(bed.above());
    }

    /** Dormiu numa cama da dimensão: acorda em casa. */
    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && isRealm(player.level()) && player.isSleeping()
                && player.getSleepTimer() >= SLEEP_TO_LEAVE) {
            sendHome(player);
        }
    }

    /** Morrer na dimensão: não morre, não perde nada - só acorda na cama. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && isRealm(player.level())) {
            event.setCanceled(true);
            player.setHealth(player.getMaxHealth());
            player.clearFire();
            player.removeAllEffects();
            player.getFoodData().setFoodLevel(Math.max(player.getFoodData().getFoodLevel(), 6));
            if (player.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.POOF, player.getX(), player.getY() + 1.0, player.getZ(), 20, 0.4, 0.6, 0.4, 0.02);
            }
            player.sendSystemMessage(Component.translatable("message.mistech.gnome_realm.died"));
            sendHome(player);
        }
    }
}

package com.mistech.mistechmod.dream;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.block.DreamcatcherBlock;
import com.mistech.mistechmod.entity.PisadeiraEntity;
import com.mistech.mistechmod.registry.DreamContent;
import com.mistech.mistechmod.registry.MistechEffects;
import com.mistech.mistechmod.realm.GnomeRealm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.CanContinueSleepingEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Paralisia do sono. Quem acorda de manhã (depois de dormir a noite) sob a Visão Psicodélica:
 * - sem Apanha Sonho perto da cama: fica preso na cama por {@link #DURATION} ticks com a Pisadeira sentada no peito;
 * - com Apanha Sonho: é levado para a Dimensão dos Gnomos ({@link GnomeRealm}).
 * Além disso, a Mácula atrai a Pisadeira em qualquer manhã (chance pela faixa, ver
 * {@link com.mistech.mistechmod.purity.TaintOmens}) e, nesse caso, o Apanha Sonho não protege.
 * Só no servidor.
 */
@EventBusSubscriber(modid = Mistech.MODID)
public final class SleepParalysis {
    /** Quanto tempo a Pisadeira segura você (5 s). */
    public static final int DURATION = 5 * 20;

    private static final class State {
        final BlockPos bed;
        int ticks;
        UUID pisadeira;

        State(BlockPos bed) {
            this.bed = bed;
        }
    }

    /** Pesadelo marcado: cama e se veio da Mácula (aí o Apanha Sonho não protege). */
    private record Pending(BlockPos bed, boolean taint) {}

    /** Jogadores esperando o próximo tick para o pesadelo começar (a cama ainda está sendo liberada). */
    private static final Map<UUID, Pending> PENDING = new HashMap<>();
    private static final Map<UUID, State> ACTIVE = new HashMap<>();

    private SleepParalysis() {}

    public static boolean isParalyzed(UUID player) {
        return ACTIVE.containsKey(player);
    }

    public static boolean isPisadeira(PisadeiraEntity entity) {
        for (State state : ACTIVE.values()) {
            if (entity.getUUID().equals(state.pisadeira)) {
                return true;
            }
        }
        return false;
    }

    /** Acordou de manhã (acordar natural: não é imediato e é o mundo que manda acordar). */
    @SubscribeEvent
    static void onWakeUp(PlayerWakeUpEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.wakeImmediately() || event.updateLevel()) {
            return;
        }
        if (ACTIVE.containsKey(player.getUUID())) {
            return;
        }
        boolean psychedelic = player.hasEffect(MistechEffects.PSYCHEDELIC_SIGHT);
        // Mácula: quanto mais maculado, mais chance da Pisadeira vir — e nem o Apanha Sonho segura
        boolean taint = com.mistech.mistechmod.purity.TaintOmens.rollPisadeira(player);
        if (!psychedelic && !taint) {
            return;
        }
        player.getSleepingPos().ifPresent(bed -> PENDING.put(player.getUUID(), new Pending(bed.immutable(), taint)));
        // o efeito do chá passa ao acordar (o pesadelo/sonho já foi marcado acima)
        player.removeEffect(MistechEffects.PSYCHEDELIC_SIGHT);
    }

    /** Enquanto paralisado, a cama "segura" o jogador mesmo de dia. */
    @SubscribeEvent
    static void onCanContinueSleeping(CanContinueSleepingEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && ACTIVE.containsKey(player.getUUID())) {
            event.setContinueSleeping(true);
        }
    }

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        UUID id = player.getUUID();
        Pending pending = PENDING.remove(id);
        if (pending != null) {
            begin(player, level, pending.bed(), pending.taint());
            return;
        }
        State state = ACTIVE.get(id);
        if (state != null) {
            tickParalysis(player, level, state);
        }
    }

    @SubscribeEvent
    static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        PENDING.remove(event.getEntity().getUUID());
        State state = ACTIVE.remove(event.getEntity().getUUID());
        if (state != null && event.getEntity().level() instanceof ServerLevel level && state.pisadeira != null
                && level.getEntity(state.pisadeira) != null) {
            level.getEntity(state.pisadeira).discard();
        }
    }

    private static void begin(ServerPlayer player, ServerLevel level, BlockPos bed, boolean taint) {
        if (!taint && (DreamcatcherBlock.isNear(level, bed, DreamContent.DREAMCATCHER.get())
                || DreamcatcherBlock.isNear(level, bed, com.mistech.mistechmod.celestial.CelestialContent.PURIFIED_DREAMCATCHER.get()))) {
            GnomeRealm.dreamInto(player, bed);
            return;
        }
        BlockState bedState = level.getBlockState(bed);
        if (!(bedState.getBlock() instanceof BedBlock)) {
            return;
        }
        State state = new State(bed);
        ACTIVE.put(player.getUUID(), state);
        putInBed(player, bed);

        // a Pisadeira senta no peito, olhando para o rosto
        PisadeiraEntity pisadeira = DreamContent.PISADEIRA.get().create(level, EntitySpawnReason.EVENT);
        if (pisadeira != null) {
            placeOnChest(pisadeira, bedState, bed);
            level.addFreshEntity(pisadeira);
            state.pisadeira = pisadeira.getUUID();
        }

        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, DURATION + 40, 0, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, DURATION + 20, 4, false, false));
        level.playSound(null, bed, SoundEvents.WITCH_CELEBRATE, SoundSource.HOSTILE, 1.0F, 1.5F);
        player.sendOverlayMessage(Component.translatable(taint ? "message.mistech.sleep_paralysis.taint" : "message.mistech.sleep_paralysis.start"));
    }

    /**
     * Deita o jogador de volta na cama. Ao acordar ele já tinha sido movido para o lado da cama, e o startSleeping
     * só muda a posição no servidor: sem o teleport o cliente continua vendo o jogador deitado no chão.
     */
    private static void putInBed(ServerPlayer player, BlockPos bed) {
        player.startSleeping(bed);
        player.connection.teleport(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
    }

    private static void placeOnChest(PisadeiraEntity pisadeira, BlockState bedState, BlockPos bed) {
        // bed = cabeceira; FACING aponta para a cabeceira. Deitado, a câmera olha para os pés da cama,
        // então ela fica de cócoras na beirada dos pés (1,3 bloco da cabeça), de frente para o rosto do jogador.
        Direction facing = bedState.getValue(BedBlock.FACING);
        double x = bed.getX() + 0.5 - facing.getStepX() * 1.3;
        double z = bed.getZ() + 0.5 - facing.getStepZ() * 1.3;
        float yaw = facing.toYRot();
        pisadeira.snapTo(x, bed.getY() + 0.56, z, yaw, 30.0F);
        pisadeira.setYHeadRot(yaw);
        pisadeira.setYBodyRot(yaw);
    }

    private static void tickParalysis(ServerPlayer player, ServerLevel level, State state) {
        state.ticks++;
        // não deixa levantar (botão "Sair da cama" ou o dia)
        if (!player.isSleeping()) {
            putInBed(player, state.bed);
        }
        if (state.ticks % 20 == 0) {
            level.playSound(null, state.bed, SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.0F, 0.9F);
        }
        if (state.ticks % 10 == 0) {
            level.sendParticles(ParticleTypes.SMOKE, state.bed.getX() + 0.5, state.bed.getY() + 1.2, state.bed.getZ() + 0.5,
                    3, 0.3, 0.2, 0.3, 0.0);
        }
        if (state.ticks >= DURATION) {
            end(player, level, state);
        }
    }

    private static void end(ServerPlayer player, ServerLevel level, State state) {
        ACTIVE.remove(player.getUUID());
        if (state.pisadeira != null) {
            var entity = level.getEntity(state.pisadeira);
            if (entity != null) {
                level.sendParticles(ParticleTypes.LARGE_SMOKE, entity.getX(), entity.getY() + 0.6, entity.getZ(), 12, 0.3, 0.4, 0.3, 0.02);
                entity.discard();
            }
        }
        if (player.isSleeping()) {
            player.stopSleepInBed(true, true);
        }
        player.removeEffect(MobEffects.SLOWNESS);
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_BREATH, SoundSource.PLAYERS, 1.0F, 0.8F);
        player.sendOverlayMessage(Component.translatable("message.mistech.sleep_paralysis.end"));
    }
}

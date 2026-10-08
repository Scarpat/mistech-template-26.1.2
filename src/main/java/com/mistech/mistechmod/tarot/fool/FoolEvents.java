package com.mistech.mistechmod.tarot.fool;

import com.mistech.mistechmod.Mistech;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Habilidades dos itens do Louco: Salto do Louco (chapéu) e pouso seguro depois do salto/Rajada do cajado. */
@EventBusSubscriber(modid = Mistech.MODID)
public final class FoolEvents {
    /** Recarga do Salto do Louco (ticks). */
    public static final int LEAP_COOLDOWN = 60;
    /** Quanto tempo agachado no chão para carregar o salto (ticks). */
    public static final int LEAP_CHARGE = 12;
    public static final double LEAP_FORWARD = 1.15;
    public static final double LEAP_UP = 0.95;
    /** Quanto tempo (ticks) o pouso seguro vale depois do salto/rajada. */
    private static final int SAFE_LANDING_TICKS = 200;
    private static final String SAFE_TAG = "mistech_fool_safe_until";
    private static final String LEAP_TAG = "mistech_fool_leap_at";

    private FoolEvents() {}

    /** Cliente -> servidor: "dei o Salto do Louco" (o cliente já aplicou o impulso). */
    public record LeapPayload() implements CustomPacketPayload {
        public static final LeapPayload INSTANCE = new LeapPayload();
        public static final CustomPacketPayload.Type<LeapPayload> TYPE =
                new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Mistech.MODID, "fool_leap"));
        public static final StreamCodec<RegistryFriendlyByteBuf, LeapPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

        @Override
        public CustomPacketPayload.Type<LeapPayload> type() {
            return TYPE;
        }
    }

    @SubscribeEvent
    static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(LeapPayload.TYPE, LeapPayload.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                leap(player);
            }
        });
    }

    public static boolean wearsCap(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.HEAD).is(FoolContent.FOOL_CAP.get());
    }

    /** Nada de dano de queda até pousar (dentro de alguns segundos). */
    public static void grantSafeLanding(ServerPlayer player) {
        player.getPersistentData().putLong(SAFE_TAG, player.level().getGameTime() + SAFE_LANDING_TICKS);
    }

    private static void leap(ServerPlayer player) {
        if (!wearsCap(player) || player.isPassenger()) {
            return;
        }
        long now = player.level().getGameTime();
        if (now - player.getPersistentData().getLongOr(LEAP_TAG, -1000L) < LEAP_COOLDOWN - 5) {
            return; // o cliente também confere; aqui só evita abuso
        }
        player.getPersistentData().putLong(LEAP_TAG, now);
        grantSafeLanding(player);
        player.resetFallDistance();
        ServerLevel level = player.level();
        level.sendParticles(ParticleTypes.GUST, player.getX(), player.getY(), player.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
        level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY(), player.getZ(), 12, 0.3, 0.05, 0.3, 0.04);
        level.playSound(null, player.blockPosition(), SoundEvents.BREEZE_JUMP, SoundSource.PLAYERS, 0.8F, 1.4F);
        level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 0.6F, 1.9F);
        player.getItemBySlot(EquipmentSlot.HEAD).hurtAndBreak(1, player, EquipmentSlot.HEAD);
    }

    @SubscribeEvent
    static void onFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        long until = player.getPersistentData().getLongOr(SAFE_TAG, 0L);
        if (until > 0L) {
            player.getPersistentData().remove(SAFE_TAG);
            if (player.level().getGameTime() <= until) {
                event.setDamageMultiplier(0.0F);
                if (event.getDistance() > 2.0) {
                    player.level().sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.1, player.getZ(), 8, 0.4, 0.05, 0.4, 0.02);
                    player.level().playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 0.4F, 2.0F);
                }
            }
        }
    }
}

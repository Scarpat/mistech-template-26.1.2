package com.mistech.mistechmod.purity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.entity.EvilEyeEntity;
import com.mistech.mistechmod.realm.EvilEyeWatcher;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Presságios da Mácula: a corrupção chama coisas ruins.
 * <ul>
 * <li><b>Pisadeira</b>: ao acordar de manhã, chance de paralisia do sono pela faixa (mesmo com Apanha Sonho).</li>
 * <li><b>Mal Olhado</b> no overworld: 1x por minuto, chance pela faixa (×1,5 à noite) de um Mal Olhado aparecer
 * atrás de você. Se você o notar (encarar ou chegar perto), ele lança o mau-olhado: Azar, Fraqueza, Fome e +2 de Mácula.</li>
 * </ul>
 */
@EventBusSubscriber(modid = Mistech.MODID)
public final class TaintOmens {
    public static final int CHECK_INTERVAL = 60 * 20;
    /** Intervalo mínimo entre dois Mal Olhados para o mesmo jogador. */
    public static final int EYE_COOLDOWN = 4 * 60 * 20;
    public static final float CURSE_TAINT = 2.0F;

    private static final Map<UUID, Long> LAST_EYE = new HashMap<>();

    private TaintOmens() {}

    /** Chance da Pisadeira ao acordar. */
    public static float pisadeiraChance(Taint.Stage stage) {
        return switch (stage) {
            case PURE -> 0.0F;
            case STAINED -> 0.05F;
            case TAINTED -> 0.15F;
            case CORRUPTED -> 0.35F;
            case PROFANE -> 0.6F;
        };
    }

    /** Chance por minuto de atrair um Mal Olhado no overworld. */
    public static float evilEyeChance(Taint.Stage stage) {
        return switch (stage) {
            case PURE, STAINED -> 0.0F;
            case TAINTED -> 0.08F;
            case CORRUPTED -> 0.2F;
            case PROFANE -> 0.4F;
        };
    }

    public static boolean rollPisadeira(Player player) {
        if (com.mistech.mistechmod.celestial.Celestial.shieldsFromPisadeira(player)) {
            return false; // Exorcismo ou Lua de Prata
        }
        if (player.level() instanceof ServerLevel level && player.getSleepingPos().map(bed -> com.mistech.mistechmod.block.DreamcatcherBlock.isNear(level, bed,
                com.mistech.mistechmod.celestial.CelestialContent.PURIFIED_DREAMCATCHER.get())).orElse(false)) {
            return false; // Apanha Sonho Purificado
        }
        return player.getRandom().nextFloat() < pisadeiraChance(Taint.stage(player));
    }

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)
                || level.dimension() != Level.OVERWORLD || player.isSpectator() || player.isCreative()
                || (player.tickCount + 7) % CHECK_INTERVAL != 0) {
            return;
        }
        if (com.mistech.mistechmod.celestial.Celestial.isBlessed(player)) {
            return;
        }
        float chance = evilEyeChance(Taint.stage(player)) * (level.isDarkOutside() ? 1.5F : 1.0F);
        if (chance <= 0 || player.getRandom().nextFloat() >= chance) {
            return;
        }
        long now = level.getGameTime();
        Long last = LAST_EYE.get(player.getUUID());
        if (last != null && now - last < EYE_COOLDOWN) {
            return;
        }
        // só na superfície (o Mal Olhado aparece em cima do chão)
        int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, player.getBlockX(), player.getBlockZ());
        if (player.getY() < surface - 6 || EvilEyeWatcher.alreadyWatching(level, player)) {
            return;
        }
        EvilEyeEntity eye = EvilEyeWatcher.spawnBehind(level, player, false);
        if (eye != null) {
            eye.setCursing(true);
            LAST_EYE.put(player.getUUID(), now);
        }
    }

    /** O mau-olhado: Azar 3 min, Fraqueza 30 s, Fome 30 s e um pouco mais de Mácula. */
    public static void curse(ServerLevel level, ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.UNLUCK, 3 * 60 * 20, 0));
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 30 * 20, 0));
        player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 30 * 20, 0));
        Taint.add(player, CURSE_TAINT);
        level.playSound(null, player.blockPosition(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 0.6F, 0.8F);
        player.sendOverlayMessage(Component.translatable("message.mistech.evil_eye.curse").withStyle(ChatFormatting.DARK_PURPLE));
    }

    @SubscribeEvent
    static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_EYE.remove(event.getEntity().getUUID());
    }
}

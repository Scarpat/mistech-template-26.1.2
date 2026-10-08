package com.mistech.mistechmod.folklore;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.celestial.Celestial;
import com.mistech.mistechmod.celestial.CelestialEvent;
import com.mistech.mistechmod.purity.Taint;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.MoonPhase;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Licantropia: a mordida do Lobisomem. Em noite de lua cheia (ou na Lua de Sangue), o jogador se transforma —
 * Força II, Velocidade, Salto, Visão Noturna, mas Fome e um pouco de Mácula. Prata (mitril) machuca mais.
 * Só a "Cura da Licantropia" do Santuário tira.
 */
@EventBusSubscriber(modid = Mistech.MODID)
public final class Lycanthropy {
    public static final String FORM_TAG = "mistech_wolf_form";
    public static final float SILVER_VS_WEREWOLF = 2.5F;
    public static final float SILVER_VS_CURSED = 1.5F;

    private Lycanthropy() {}

    public static boolean has(Player player) {
        return player.getData(FolkloreContent.LYCANTHROPY);
    }

    public static void infect(Player player) {
        if (player.isCreative() || player.isSpectator() || has(player)) {
            return;
        }
        player.setData(FolkloreContent.LYCANTHROPY, true);
        player.sendSystemMessage(Component.translatable("message.mistech.lycanthropy.infected").withStyle(ChatFormatting.DARK_RED));
        player.level().playSound(null, player.blockPosition(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.PLAYERS, 0.5F, 1.4F);
    }

    public static void cure(Player player) {
        if (!has(player)) {
            return;
        }
        player.setData(FolkloreContent.LYCANTHROPY, false);
        player.getPersistentData().remove(FORM_TAG);
        player.sendSystemMessage(Component.translatable("message.mistech.lycanthropy.cured").withStyle(ChatFormatting.GOLD));
    }

    /** Noite de lua cheia na superfície (ou Lua de Sangue). */
    public static boolean wolfNight(ServerLevel level, Player player) {
        if (level.dimension() != Level.OVERWORLD || !level.isDarkOutside()) {
            return false;
        }
        return Celestial.isActive(level, CelestialEvent.BLOOD_MOON)
                || level.environmentAttributes().getValue(EnvironmentAttributes.MOON_PHASE, player.blockPosition()) == MoonPhase.FULL_MOON;
    }

    public static boolean isTransformed(Player player) {
        return player.getPersistentData().getBooleanOr(FORM_TAG, false);
    }

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)
                || player.tickCount % 20 != 3 || !has(player)) {
            return;
        }
        boolean wolf = wolfNight(level, player) && !player.isSpectator();
        boolean was = isTransformed(player);
        if (wolf != was) {
            player.getPersistentData().putBoolean(FORM_TAG, wolf);
            if (wolf) {
                level.playSound(null, player.blockPosition(), WerewolfSounds.howl(), SoundSource.PLAYERS, 1.5F, 0.6F);
                level.sendParticles(ParticleTypes.LARGE_SMOKE, player.getX(), player.getY() + 1.0, player.getZ(), 30, 0.4, 0.8, 0.4, 0.02);
                player.sendSystemMessage(Component.translatable("message.mistech.lycanthropy.transform").withStyle(ChatFormatting.DARK_RED));
            } else {
                player.sendSystemMessage(Component.translatable("message.mistech.lycanthropy.revert").withStyle(ChatFormatting.GRAY));
            }
        }
        if (wolf) {
            player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 60, 1, true, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.SPEED, 60, 1, true, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 60, 1, true, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 15 * 20, 0, true, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 60, 0, true, false, true));
            Taint.add(player, 0.05F);
            if (player.getRandom().nextInt(8) == 0) {
                level.sendParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + 1.6, player.getZ(), 3, 0.2, 0.2, 0.2, 0.0);
            }
        }
    }

    /** Prata (armas de mitril) contra lobisomens e amaldiçoados transformados. */
    @SubscribeEvent
    static void onDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker) || !attacker.getMainHandItem().is(FolkloreContent.SILVER_WEAPONS)) {
            return;
        }
        LivingEntity target = event.getEntity();
        if (target instanceof WerewolfEntity werewolf) {
            event.setAmount(event.getAmount() * SILVER_VS_WEREWOLF);
            werewolf.markSilver();
            if (target.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.END_ROD, target.getX(), target.getY() + 1.5, target.getZ(), 6, 0.3, 0.4, 0.3, 0.05);
            }
        } else if (target instanceof Player player && isTransformed(player)) {
            event.setAmount(event.getAmount() * SILVER_VS_CURSED);
        }
    }

    /** Sons de lobo grande (o jogo não tem uivo próprio). */
    static final class WerewolfSounds {
        static net.minecraft.sounds.SoundEvent howl() {
            return SoundEvents.WOLF_SOUNDS.get(net.minecraft.world.entity.animal.wolf.WolfSoundVariants.SoundSet.BIG).adultSounds().growlSound().value();
        }
    }
}

package com.mistech.mistechmod.sky;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.item.RuneElement;
import com.mistech.mistechmod.item.mithril.GearRunes;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Runa das Nuvens engastada: −{@link #FALL_PER_RUNE} do dano de queda por runa; com {@link #IMMUNE_AT} ou mais,
 * nenhum dano de queda e Queda Lenta enquanto agachado no ar.
 */
@EventBusSubscriber(modid = Mistech.MODID)
public final class CloudEvents {
    public static final float FALL_PER_RUNE = 0.15F;
    public static final double IMMUNE_AT = 3.0;

    private CloudEvents() {}

    @SubscribeEvent
    static void onFall(LivingFallEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Player) || entity.level().isClientSide()) {
            return;
        }
        double clouds = GearRunes.count(GearRunes.tallyEquipped(entity), RuneElement.CLOUD);
        if (clouds <= 0.0) {
            return;
        }
        if (clouds >= IMMUNE_AT) {
            event.setDamageMultiplier(0.0F);
            if (event.getDistance() > 3.0 && entity.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.CLOUD, entity.getX(), entity.getY() + 0.1, entity.getZ(), 10, 0.4, 0.05, 0.4, 0.02);
            }
        } else {
            event.setDamageMultiplier(event.getDamageMultiplier() * Math.max(0.0F, 1.0F - FALL_PER_RUNE * (float) clouds));
        }
    }

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || player.tickCount % 5 != 0 || !player.isShiftKeyDown() || player.onGround()
                || player.getAbilities().flying || player.isInWater() || player.getDeltaMovement().y >= 0.0) {
            return;
        }
        if (GearRunes.count(GearRunes.tallyEquipped(player), RuneElement.CLOUD) >= IMMUNE_AT) {
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 15, 0, true, false, true));
        }
    }
}

package com.mistech.mistechmod.item.rune;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Runa da Escuridão: cria uma nuvem de cegueira (como a nuvem da poção de bafo de dragão)
 * no ponto para onde o jogador mira (até 16 blocos). 16 usos.
 */
public class DarknessRuneItem extends Item {
    private static final double RANGE = 16.0;

    public DarknessRuneItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel serverLevel) {
            HitResult hit = player.pick(RANGE, 0.0F, false);
            Vec3 at = hit.getType() == HitResult.Type.MISS
                    ? player.getEyePosition().add(player.getLookAngle().scale(6.0))
                    : hit.getLocation();

            AreaEffectCloud cloud = new AreaEffectCloud(serverLevel, at.x, at.y, at.z);
            cloud.setOwner(player);
            cloud.setRadius(3.0F);
            cloud.setRadiusOnUse(-0.1F);
            cloud.setWaitTime(10);
            cloud.setDuration(20 * 12);
            cloud.setRadiusPerTick(-cloud.getRadius() / cloud.getDuration());
            cloud.setCustomParticle(ParticleTypes.SQUID_INK);
            cloud.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 20 * 6, 0));
            serverLevel.addFreshEntity(cloud);

            level.playSound(null, at.x, at.y, at.z, SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.PLAYERS, 0.4F, 1.6F);
            player.getCooldowns().addCooldown(stack, 20);
            stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
            com.mistech.mistechmod.affinity.Affinity.grant(player, com.mistech.mistechmod.item.RuneElement.DARKNESS, 1);
        }
        return InteractionResult.SUCCESS;
    }
}

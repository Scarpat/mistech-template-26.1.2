package com.mistech.mistechmod.item.rune;

import com.mistech.mistechmod.affinity.Affinity;
import com.mistech.mistechmod.item.RuneElement;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Runa de Vapor (Fogo + Água): solta uma nuvem de névoa em volta do jogador
 * (raio de 5 blocos, dura 20 s). 16 usos, recarga de 1 s.
 * Cada uso dá 1 ponto de afinidade com o Fogo e 1 com a Água.
 */
public class VaporRuneItem extends Item {
    private static final float RADIUS = 5.0F;
    private static final int DURATION = 20 * 20;

    public VaporRuneItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel serverLevel) {
            double x = player.getX(), y = player.getY(), z = player.getZ();

            // névoa que fica no chão
            AreaEffectCloud cloud = new AreaEffectCloud(serverLevel, x, y, z);
            cloud.setOwner(player);
            cloud.setRadius(RADIUS);
            cloud.setWaitTime(0);
            cloud.setDuration(DURATION);
            cloud.setCustomParticle(ParticleTypes.CLOUD);
            serverLevel.addFreshEntity(cloud);

            // rajada inicial de vapor em volta do jogador
            serverLevel.sendParticles(ParticleTypes.CLOUD, x, y + 1.0, z, 120, RADIUS * 0.5, 1.2, RADIUS * 0.5, 0.02);
            serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y + 0.5, z, 30, RADIUS * 0.4, 0.5, RADIUS * 0.4, 0.01);

            level.playSound(null, x, y, z, SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.0F, 0.7F);
            player.getCooldowns().addCooldown(stack, 20);
            stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
            Affinity.grant(player, RuneElement.FIRE, 1);
            Affinity.grant(player, RuneElement.WATER, 1);
        }
        return InteractionResult.SUCCESS;
    }
}

package com.mistech.mistechmod.item.rune;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Runa da Luz: visão noturna + brilho (glowing) por 90 s. 16 usos. */
public class LightRuneItem extends Item {
    public static final int DURATION_TICKS = 90 * 20;

    public LightRuneItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel serverLevel) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, DURATION_TICKS, 0, false, true, true));
            player.addEffect(new MobEffectInstance(MobEffects.GLOWING, DURATION_TICKS, 0, false, true, true));
            serverLevel.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0, player.getZ(), 20, 0.4, 0.6, 0.4, 0.05);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.5F);
            player.getCooldowns().addCooldown(stack, 20);
            stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
            com.mistech.mistechmod.affinity.Affinity.grant(player, com.mistech.mistechmod.item.RuneElement.LIGHT, 1);
        }
        return InteractionResult.SUCCESS;
    }
}

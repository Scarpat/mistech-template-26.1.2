package com.mistech.mistechmod.item;

import com.mistech.mistechmod.event.MistFormEvents;
import com.mistech.mistechmod.registry.MistechEffects;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Cachimbo da Névoa (ritual): não precisa de erva nem de fogo. Botão direito dá uma baforada e
 * você vira névoa por {@link #MIST_TICKS} (efeito mistech:mist_form).
 * Usar de novo durante a névoa volta ao normal na hora (se houver espaço).
 * 32 usos; recarga de 30 s que começa quando a névoa acaba.
 */
public class MistPipeItem extends Item {
    public static final int MIST_TICKS = 12 * 20;
    public static final int COOLDOWN_TICKS = 30 * 20;

    public MistPipeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        MobEffectInstance mist = player.getEffect(MistechEffects.MIST_FORM);
        if (mist != null) {
            // voltar à forma sólida mais cedo (ignora o primeiro segundo: segurar o botão repete o clique)
            if (!level.isClientSide() && mist.getDuration() < MIST_TICKS - 20) {
                if (MistFormEvents.hasRoomToReform(player)) {
                    player.removeEffect(MistechEffects.MIST_FORM);
                } else {
                    MistFormEvents.warnNoRoom(player);
                }
            }
            return InteractionResult.SUCCESS;
        }
        // uma baforada no clique: vira névoa na hora
        if (level instanceof ServerLevel serverLevel) {
            player.addEffect(new MobEffectInstance(MistechEffects.MIST_FORM, MIST_TICKS, 0, false, false, true));
            serverLevel.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 1.0, player.getZ(),
                    60, 0.5, 0.8, 0.5, 0.03);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.8F, 0.6F);
            MistFormEvents.dropAggro(serverLevel, player);
            stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
        } else {
            Vec3 look = player.getLookAngle();
            Vec3 mouth = player.getEyePosition().add(look.x * 0.5, -0.1, look.z * 0.5);
            for (int i = 0; i < 8; i++) {
                level.addParticle(ParticleTypes.CLOUD, mouth.x, mouth.y, mouth.z, look.x * 0.08, 0.02, look.z * 0.08);
            }
        }
        return InteractionResult.SUCCESS;
    }
}

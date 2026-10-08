package com.mistech.mistechmod.beyond;

import com.mistech.mistechmod.affinity.Affinity;
import com.mistech.mistechmod.item.RuneElement;
import com.mistech.mistechmod.registry.MistechEffects;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Runa da Vigília (Mesa de Escrita: Musgo Pálido + Olho-florido aberto + Grumo de Resina + Runa da Escuridão; 15 de giz). 16 usos.
 * Botão direito: por {@link #SECONDS} s, toda criatura que você olhar fica <b>travada</b> (o Rangedor ao contrário) — não anda,
 * não voa e não ataca. Chefes não são afetados. A lógica do olhar fica em {@link PaleNetherEvents}.
 */
public class VigilRuneItem extends Item {
    public static final int SECONDS = 8;

    public VigilRuneItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel server) {
            player.addEffect(new MobEffectInstance(MistechEffects.VIGIL_GAZE, SECONDS * 20, 0, false, true, true));
            server.sendParticles(ParticleTypes.WHITE_ASH, player.getX(), player.getEyeY(), player.getZ(), 30, 0.5, 0.4, 0.5, 0.02);
            server.playSound(null, player.blockPosition(), SoundEvents.CREAKING_ACTIVATE, SoundSource.PLAYERS, 1.0F, 0.8F);
            stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
            Affinity.grant(player, RuneElement.VIGIL, 1);
            player.getCooldowns().addCooldown(stack, 40);
        }
        return InteractionResult.SUCCESS;
    }
}

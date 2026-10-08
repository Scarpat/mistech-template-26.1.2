package com.mistech.mistechmod.item.rune;

import com.mistech.mistechmod.affinity.Affinity;
import com.mistech.mistechmod.event.FortuneEvents;
import com.mistech.mistechmod.item.RuneElement;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Runa da Sorte (ritual Tier I: Runa do Tempo + 2 Pó de Fada + 2 esmeraldas). 16 usos.
 * - Botão direito no ar: Sorte II por 2 min.
 * - Numa criatura: fica "marcada pela sorte" por 60 s — se morrer nesse tempo, os drops e a XP dobram.
 */
public class LuckRuneItem extends Item {
    public static final int LUCK_TICKS = 20 * 120;
    public static final int MARK_TICKS = 20 * 60;

    public LuckRuneItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel server) {
            player.addEffect(new MobEffectInstance(MobEffects.LUCK, LUCK_TICKS, 1));
            fx(server, player.getX(), player.getY() + 1.0, player.getZ());
            stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
            Affinity.grant(player, RuneElement.LUCK, 1);
            player.getCooldowns().addCooldown(stack, 20);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (target instanceof Player) {
            return InteractionResult.PASS;
        }
        if (player.level() instanceof ServerLevel server) {
            FortuneEvents.mark(target, MARK_TICKS);
            fx(server, target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ());
            stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
            Affinity.grant(player, RuneElement.LUCK, 1);
            player.sendOverlayMessage(Component.translatable("item.mistech.luck_rune.marked", target.getDisplayName()));
        }
        return InteractionResult.SUCCESS;
    }

    public static void fx(ServerLevel level, double x, double y, double z) {
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y, z, 14, 0.4, 0.4, 0.4, 0.0);
        level.sendParticles(ParticleTypes.WAX_ON, x, y, z, 10, 0.4, 0.5, 0.4, 0.2);
        level.playSound(null, x, y, z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.6F);
        level.playSound(null, x, y, z, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.6F, 1.3F);
    }
}

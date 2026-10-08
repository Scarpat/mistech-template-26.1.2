package com.mistech.mistechmod.purity.item;

import com.mistech.mistechmod.purity.Taint;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * Frasco de Essência Purificada: beber tira {@link #DRINK_CLEANSE} de Mácula (até zero) e o Wither (recarga de 60 s);
 * para quem está Profano, a pureza queima. Usado num aldeão zumbi, cura na hora.
 */
public class PurifiedEssenceBottleItem extends Item {
    public static final float DRINK_CLEANSE = 15.0F;
    public static final int COOLDOWN = 60 * 20;

    public PurifiedEssenceBottleItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.DRINK;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return 32;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (level instanceof ServerLevel server && entity instanceof Player player) {
            if (Taint.stage(player) == Taint.Stage.PROFANE) {
                player.hurtServer(server, server.damageSources().magic(), 4.0F);
                player.sendOverlayMessage(Component.translatable("message.mistech.purified.burns").withStyle(ChatFormatting.DARK_RED));
            }
            Taint.purify(player, DRINK_CLEANSE);
            player.removeEffect(MobEffects.WITHER);
            server.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0, player.getZ(), 15, 0.3, 0.5, 0.3, 0.03);
            player.sendOverlayMessage(Component.translatable("message.mistech.pendulum.self", Taint.describe(Taint.get(player))));
            player.getCooldowns().addCooldown(stack, COOLDOWN);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
                player.getInventory().placeItemBackInInventory(new ItemStack(Items.GLASS_BOTTLE));
            }
        }
        return stack;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof ZombieVillager zombie)) {
            return InteractionResult.PASS;
        }
        if (zombie.isConverting()) {
            return InteractionResult.PASS;
        }
        if (player.level() instanceof ServerLevel server) {
            // mesma rotina da maçã dourada + Fraqueza, só que a cura é quase instantânea
            zombie.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.WEAKNESS, 40, 0));
            ItemStack keep = player.getItemInHand(hand);
            player.setItemInHand(hand, new ItemStack(Items.GOLDEN_APPLE));
            zombie.mobInteract(player, hand);
            player.setItemInHand(hand, keep);
            if (!zombie.isConverting()) {
                return InteractionResult.PASS;
            }
            zombie.setVillagerConversionTime(20);
            server.sendParticles(ParticleTypes.END_ROD, zombie.getX(), zombie.getY() + 1.0, zombie.getZ(), 25, 0.3, 0.6, 0.3, 0.05);
            server.playSound(null, zombie.blockPosition(), SoundEvents.ZOMBIE_VILLAGER_CONVERTED, SoundSource.NEUTRAL, 1.0F, 1.0F);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
                player.getInventory().placeItemBackInInventory(new ItemStack(Items.GLASS_BOTTLE));
            }
        }
        return InteractionResult.SUCCESS;
    }
}

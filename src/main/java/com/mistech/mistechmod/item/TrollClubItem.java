package com.mistech.mistechmod.item;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * Clava de Troll: arma pesada e lenta (drop raro do Troll). Cada golpe joga o alvo longe
 * e tem 25% de chance de atordoar (Lentidão III + Fraqueza por 2 s).
 */
public class TrollClubItem extends Item {
    public TrollClubItem(Properties properties) {
        super(properties);
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        super.hurtEnemy(stack, target, attacker);
        double dx = target.getX() - attacker.getX(), dz = target.getZ() - attacker.getZ();
        double len = Math.max(0.001, Math.sqrt(dx * dx + dz * dz));
        target.knockback(1.1, -dx / len, -dz / len);
        if (attacker.getRandom().nextFloat() < 0.25F) {
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 2), attacker);
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0), attacker);
            target.level().playSound(null, target.blockPosition(), SoundEvents.MACE_SMASH_GROUND, SoundSource.PLAYERS, 0.8F, 0.8F);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.mistech.troll_club.tooltip").withStyle(ChatFormatting.GRAY));
    }
}

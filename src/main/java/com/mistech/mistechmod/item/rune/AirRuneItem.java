package com.mistech.mistechmod.item.rune;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.WindCharge;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Runa de Ar: atira uma Wind Charge, igual ao item vanilla. 16 usos, recarga de 0,5 s. */
public class AirRuneItem extends Item {
    private static final int COOLDOWN_TICKS = 10;

    public AirRuneItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel serverLevel) {
            Projectile.spawnProjectileFromRotation(
                    (lvl, shooter, itemStack) -> new WindCharge(player, level,
                            player.position().x(), player.getEyePosition().y(), player.position().z()),
                    serverLevel, stack, player, 0.0F, 1.5F, 1.0F);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WIND_CHARGE_THROW, SoundSource.NEUTRAL, 0.5F,
                0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
        player.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
        stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
        com.mistech.mistechmod.affinity.Affinity.grant(player, com.mistech.mistechmod.item.RuneElement.AIR, 1);
        return InteractionResult.SUCCESS;
    }
}

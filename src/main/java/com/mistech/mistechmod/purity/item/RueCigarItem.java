package com.mistech.mistechmod.purity.item;

import com.mistech.mistechmod.item.SmokableItem;
import com.mistech.mistechmod.purity.Taint;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** Charuto de Arruda: cada tragada tira {@link #CLEANSE} de Mácula (no máximo até 20). */
public class RueCigarItem extends SmokableItem {
    public static final float CLEANSE = 3.0F;

    public RueCigarItem(Properties properties) {
        super(properties, 180, 5);
    }

    @Override
    protected void applyPuffEffects(ServerLevel level, LivingEntity entity) {
        if (entity instanceof Player player) {
            Taint.cleanse(player, CLEANSE);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getEyeY(), player.getZ(), 4, 0.3, 0.2, 0.3, 0.0);
        }
    }
}

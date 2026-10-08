package com.mistech.mistechmod.beyond;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.item.IncenseItem;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Incenso de Olho-florido (crafting: 2 gravetos, Olho-florido aberto e Grumo de Resina). Funciona como o incenso comum
 * (Véu de Incenso) e ainda faz os <b>hostis em volta brilharem</b> (contorno): {@link #RADIUS} blocos de quem carrega aceso,
 * ou a área toda do Porta-Incenso.
 */
public class EyeblossomIncenseItem extends IncenseItem {
    public static final double RADIUS = 10.0;

    public EyeblossomIncenseItem(Properties properties) {
        super(properties);
    }

    public static boolean isEyeblossom(ItemStack stack) {
        return stack.getItem() instanceof EyeblossomIncenseItem;
    }

    /** Hostis a até {@code radius} de {@code center} brilham por 2 s. */
    public static void reveal(ServerLevel level, Vec3 center, double radius) {
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(BlockPos.containing(center)).inflate(radius),
                e -> e instanceof Enemy && e.isAlive() && e.position().distanceToSqr(center) <= radius * radius)) {
            entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, true, false, false));
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        boolean lit = isLit(stack);
        super.inventoryTick(stack, level, entity, slot);
        if (lit && entity instanceof Player player && level.getGameTime() % 20 == 0) {
            reveal(level, player.position(), RADIUS);
        }
    }
}

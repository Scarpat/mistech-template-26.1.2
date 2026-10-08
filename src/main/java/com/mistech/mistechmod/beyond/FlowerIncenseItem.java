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
 * Incensos de flor (crafting: 2 gravetos + 2 da flor). Funcionam como o incenso comum (Véu de Incenso) e ainda, a cada segundo,
 * na fumaça ({@link #RADIUS} blocos de quem carrega aceso, ou a área do Porta-Incenso):
 * <ul>
 *   <li><b>Rosa do Wither</b>: hostis ganham Murchar I;</li>
 *   <li><b>Tocha-flor</b>: jogadores ganham Visão Noturna;</li>
 *   <li><b>Lírio-do-vale</b>: jogadores ganham Regeneração I.</li>
 * </ul>
 */
public class FlowerIncenseItem extends IncenseItem {
    public static final double RADIUS = 10.0;

    public enum Flower { WITHER_ROSE, TORCHFLOWER, LILY }

    private final Flower flower;

    public FlowerIncenseItem(Properties properties, Flower flower) {
        super(properties);
        this.flower = flower;
    }

    /** Aplica o efeito da flor (se {@code stack} for um incenso de flor). */
    public static void applyAny(ItemStack stack, ServerLevel level, Vec3 center, double radius) {
        if (stack.getItem() instanceof FlowerIncenseItem incense) {
            incense.apply(level, center, radius);
        }
    }

    public void apply(ServerLevel level, Vec3 center, double radius) {
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(BlockPos.containing(center)).inflate(radius),
                e -> e.isAlive() && e.position().distanceToSqr(center) <= radius * radius)) {
            switch (this.flower) {
                case WITHER_ROSE -> {
                    if (entity instanceof Enemy) {
                        entity.addEffect(new MobEffectInstance(MobEffects.WITHER, 50, 0));
                    }
                }
                case TORCHFLOWER -> {
                    if (entity instanceof Player) {
                        entity.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 15 * 20, 0, true, false, true));
                    }
                }
                case LILY -> {
                    if (entity instanceof Player && !entity.hasEffect(MobEffects.REGENERATION)) {
                        entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, true, false, true));
                    }
                }
            }
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        boolean lit = isLit(stack);
        super.inventoryTick(stack, level, entity, slot);
        if (lit && entity instanceof Player player && level.getGameTime() % 20 == 0) {
            this.apply(level, player.position(), RADIUS);
        }
    }
}

package com.mistech.mistechmod.fey;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.item.IncenseItem;
import com.mistech.mistechmod.registry.MistechEffects;
import com.mistech.mistechmod.registry.MistechTags;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Incenso de Fada (incenso com Pó de Fada na ponta). Funciona como o incenso comum (Véu de Incenso, 2 min) e ainda dá o
 * <b>Véu Feérico</b>: duendes corrompidos, trolls e aberrações (tag {@code mistech:fey_ignores}) não escolhem quem tem o
 * efeito como alvo, e os que já perseguiam perdem o alvo. A Rainha Duende não é enganada.
 * Na mão: só quem carrega. No Porta-Incenso: todos os jogadores na área (10 blocos).
 */
public class FairyIncenseItem extends IncenseItem {
    /** Duração aplicada a cada renovação; renovada a cada segundo enquanto queima. */
    public static final int REFRESH_TICKS = 30;
    /** Raio em que duendes que já perseguiam o jogador perdem o alvo. */
    public static final double CALM_RADIUS = 32.0;

    public FairyIncenseItem(Properties properties) {
        super(properties);
    }

    public static boolean isFairy(ItemStack stack) {
        return stack.getItem() instanceof FairyIncenseItem;
    }

    /** Porta-Incenso: se {@code stack} for Incenso de Fada, dá o Véu Feérico aos jogadores na área. */
    public static void applyAny(ItemStack stack, ServerLevel level, Vec3 center, double radius) {
        if (!isFairy(stack)) {
            return;
        }
        for (Player player : level.getEntitiesOfClass(Player.class, new AABB(BlockPos.containing(center)).inflate(radius),
                p -> p.isAlive() && p.position().distanceToSqr(center) <= radius * radius)) {
            veil(level, player);
        }
        if (level.getRandom().nextInt(3) == 0) {
            level.sendParticles(ParticleTypes.WAX_OFF, center.x, center.y + 0.6, center.z, 2, 0.4, 0.3, 0.4, 0.0);
        }
    }

    /** Dá/renova o Véu Feérico e faz os duendes que já perseguiam o jogador desistirem. */
    public static void veil(ServerLevel level, LivingEntity target) {
        target.addEffect(new MobEffectInstance(MistechEffects.FEY_VEIL, REFRESH_TICKS, 0, true, true, true));
        for (Mob mob : level.getEntitiesOfClass(Mob.class, target.getBoundingBox().inflate(CALM_RADIUS),
                m -> m.getTarget() == target && m.is(MistechTags.FEY_IGNORES) && m.getLastHurtByMob() != target)) {
            mob.setTarget(null);
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        boolean lit = isLit(stack);
        super.inventoryTick(stack, level, entity, slot);
        if (lit && entity instanceof Player player && level.getGameTime() % 20 == 0) {
            veil(level, player);
            level.sendParticles(ParticleTypes.WAX_OFF, player.getX(), player.getY() + 1.0, player.getZ(), 1, 0.3, 0.4, 0.3, 0.0);
        }
    }
}

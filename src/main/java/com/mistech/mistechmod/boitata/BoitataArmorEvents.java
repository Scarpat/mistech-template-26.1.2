package com.mistech.mistechmod.boitata;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.ranged.TempBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Efeitos da Armadura do Boitatá.
 * Capacete: enxerga dentro da lava (névoa no cliente) e Visão Noturna nela. Peitoral: imune a fogo; quem bate pega fogo.
 * Calças: correndo deixa um rastro de fogo (só partículas + queima quem passa, não o dono nem os bichos dele; não põe fogo em blocos).
 * Botas: andam sobre lava (crosta de magma temporária; agachar afunda) e sem dano do magma.
 * Conjunto (Forma de Serpente): dano de fogo cura, nada rápido na lava (cliente) e Sopro de Fogo (tecla R, 1×/min).
 */
@EventBusSubscriber(modid = Mistech.MODID)
public final class BoitataArmorEvents {
    public static final int TRAIL_TICKS = 60;

    private record Flame(Vec3 pos, long until) {}

    private static final Map<UUID, Deque<Flame>> TRAILS = new HashMap<>();

    private BoitataArmorEvents() {}

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }
        Player player = event.getEntity();
        if (BoitataGear.wearing(player, EquipmentSlot.HEAD) && player.isEyeInFluid(FluidTags.LAVA)) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, 0, true, false, true));
        }
        if (BoitataGear.wearing(player, EquipmentSlot.CHEST) && player.isOnFire() && player.tickCount % 20 == 0 && !player.isInLava()) {
            player.clearFire();
        }
        if (BoitataGear.wearing(player, EquipmentSlot.FEET) && !player.isShiftKeyDown()) {
            lavaWalk(level, player);
        }
        trail(level, player);
    }

    /** Lava parada embaixo dos pés vira crosta de magma por 3 s (3×3). */
    private static void lavaWalk(ServerLevel level, Player player) {
        if (player.isInLava() || player.getDeltaMovement().y > 0.1) {
            return;
        }
        BlockPos feet = BlockPos.containing(player.getX(), player.getY() - 0.2, player.getZ());
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos pos = feet.offset(dx, 0, dz);
                FluidState fluid = level.getFluidState(pos);
                boolean crust = level.getBlockState(pos).is(Blocks.MAGMA_BLOCK);
                if ((crust || fluid.is(FluidTags.LAVA) && fluid.isSource()) && level.getBlockState(pos.above()).isAir()) {
                    TempBlocks.place(level, pos, Blocks.MAGMA_BLOCK.defaultBlockState(), 60);
                }
            }
        }
    }

    private static void trail(ServerLevel level, Player player) {
        long now = level.getGameTime();
        Deque<Flame> flames = TRAILS.get(player.getUUID());
        if (BoitataGear.wearing(player, EquipmentSlot.LEGS) && player.isSprinting() && player.onGround() && player.tickCount % 3 == 0) {
            if (flames == null) {
                flames = new ArrayDeque<>();
                TRAILS.put(player.getUUID(), flames);
            }
            flames.addLast(new Flame(player.position(), now + TRAIL_TICKS));
        }
        if (flames == null) {
            return;
        }
        Iterator<Flame> it = flames.iterator();
        while (it.hasNext()) {
            if (it.next().until() < now) {
                it.remove();
            }
        }
        if (flames.isEmpty()) {
            TRAILS.remove(player.getUUID());
            return;
        }
        if (player.tickCount % 4 != 0) {
            return;
        }
        for (Flame f : flames) {
            level.sendParticles(ParticleTypes.FLAME, f.pos().x, f.pos().y + 0.1, f.pos().z, 2, 0.2, 0.05, 0.2, 0.01);
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(f.pos(), f.pos()).inflate(0.7, 1.0, 0.7))) {
                if (target == player || (target instanceof OwnableEntity pet && pet.getOwner() == player) || target.fireImmune()) {
                    continue;
                }
                target.igniteForSeconds(3.0F);
            }
        }
    }

    @SubscribeEvent
    static void onDamage(LivingIncomingDamageEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player player && entity.level() instanceof ServerLevel) {
            var source = event.getSource();
            boolean fire = source.is(DamageTypeTags.IS_FIRE);
            if (fire && BoitataGear.fullSet(player)) {
                // Forma de Serpente: o fogo cura
                player.heal(event.getAmount() * 0.5F);
                event.setCanceled(true);
                return;
            }
            if (fire && BoitataGear.wearing(player, EquipmentSlot.CHEST)) {
                event.setCanceled(true);
                return;
            }
            if (source.is(DamageTypes.HOT_FLOOR) && BoitataGear.wearing(player, EquipmentSlot.FEET)) {
                event.setCanceled(true);
                return;
            }
            if (BoitataGear.wearing(player, EquipmentSlot.CHEST) && source.getEntity() instanceof LivingEntity attacker
                    && source.getDirectEntity() == attacker && attacker != player) {
                attacker.igniteForSeconds(4.0F);
            }
        }
    }
}

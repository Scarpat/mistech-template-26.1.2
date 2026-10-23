package com.mistech.mistechmod.spellcraft;

import java.util.EnumSet;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Invocação voadora (Espírito Guardião, Fênix): voa e atravessa blocos como o Vex, mas obedece ao dono — fica por perto,
 * caça criaturas hostis num raio de 16 do dono e nunca ataca jogadores. Não impede de dormir.
 */
public class SummonFlyerEntity extends Vex {
    private @Nullable UUID master;
    private int attackCooldown;

    public SummonFlyerEntity(EntityType<? extends Vex> type, Level level) {
        super(type, level);
    }

    public void setMaster(Player player) {
        this.master = player.getUUID();
    }

    private @Nullable Player master() {
        return this.master == null ? null : this.level().getPlayerByUUID(this.master);
    }

    public boolean fiery() {
        return this.getType() == SpellcraftContent.PHOENIX.get();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new GuardGoal());
    }

    @Override
    public boolean isPreventingPlayerRest(ServerLevel level, Player player) {
        return false;
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return !(target instanceof Player) && !target.entityTags().contains(Summons.TAG) && super.canAttack(target);
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit && this.fiery()) {
            target.igniteForSeconds(5.0F);
        }
        return hit;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level() instanceof ServerLevel server && this.tickCount % 2 == 0) {
            server.sendParticles(this.fiery() ? ParticleTypes.FLAME : ParticleTypes.SOUL_FIRE_FLAME,
                    this.getX(), this.getY() + 0.4, this.getZ(), 1, 0.15, 0.15, 0.15, 0.01);
        }
    }

    /** Fica perto do dono; quando há criatura hostil perto dele, voa até ela e ataca. */
    private class GuardGoal extends Goal {
        GuardGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.TARGET, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return true;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            SummonFlyerEntity self = SummonFlyerEntity.this;
            Player owner = self.master();
            if (owner == null) {
                return;
            }
            LivingEntity target = self.getTarget();
            if (target == null || !target.isAlive() || target.distanceToSqr(owner) > 24 * 24) {
                target = null;
                double best = 16 * 16;
                for (LivingEntity e : self.level().getEntitiesOfClass(LivingEntity.class, new AABB(owner.blockPosition()).inflate(16),
                        e -> e instanceof Enemy && e.isAlive() && self.canAttack(e))) {
                    double d = e.distanceToSqr(owner);
                    if (d < best) {
                        best = d;
                        target = e;
                    }
                }
                self.setTarget(target);
            }
            if (self.attackCooldown > 0) {
                self.attackCooldown--;
            }
            if (target != null) {
                Vec3 eye = target.getEyePosition();
                self.getMoveControl().setWantedPosition(eye.x, eye.y - 0.3, eye.z, 1.2);
                self.getLookControl().setLookAt(target);
                self.setIsCharging(self.distanceToSqr(target) < 9.0);
                if (self.attackCooldown == 0 && self.getBoundingBox().inflate(0.4).intersects(target.getBoundingBox())
                        && self.level() instanceof ServerLevel server) {
                    self.doHurtTarget(server, target);
                    self.attackCooldown = 16;
                }
            } else {
                self.setIsCharging(false);
                double a = (self.tickCount + self.getId() * 37) * 0.05;
                Vec3 spot = owner.position().add(Math.cos(a) * 1.8, 2.2, Math.sin(a) * 1.8);
                if (self.distanceToSqr(owner) > 64 * 64) {
                    self.teleportTo(spot.x, spot.y, spot.z);
                }
                self.getMoveControl().setWantedPosition(spot.x, spot.y, spot.z, 0.8);
            }
        }
    }
}

package com.mistech.mistechmod.realm.dream.mob;

import java.util.EnumSet;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Base dos bichinhos que voam no Sonho (Peixe-do-Ar, Mariposa-Prisma, Memória): sem gravidade, sem dano de queda,
 * e um objetivo de voo "solto" que escolhe pontos no ar perto de onde está (sem entrar em blocos).
 */
public abstract class DreamFlyerEntity extends PathfinderMob {
    protected DreamFlyerEntity(EntityType<? extends DreamFlyerEntity> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setNoGravity(true);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        return nav;
    }

    @Override
    protected void checkFallDamage(double ya, boolean onGround, BlockState onState, BlockPos pos) {
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, net.minecraft.world.damagesource.DamageSource source) {
        return false;
    }

    /** Ponto de ar livre perto de {@code center} (ou null). */
    public static @Nullable Vec3 airSpot(PathfinderMob mob, Vec3 center, int horizontal, int vertical) {
        for (int i = 0; i < 10; i++) {
            BlockPos p = BlockPos.containing(center.x + mob.getRandom().nextInt(horizontal * 2 + 1) - horizontal,
                    center.y + mob.getRandom().nextInt(vertical * 2 + 1) - vertical,
                    center.z + mob.getRandom().nextInt(horizontal * 2 + 1) - horizontal);
            if (mob.level().getBlockState(p).isAir() && mob.level().getBlockState(p.above()).isAir()) {
                return Vec3.atCenterOf(p);
            }
        }
        return null;
    }

    /** Voa direto (move control) até um ponto; sem pathfinding. */
    public void flyTo(Vec3 at, double speed) {
        this.getMoveControl().setWantedPosition(at.x, at.y, at.z, speed);
    }

    /** Voo à toa: um ponto de ar a cada poucos segundos, mantendo uma altura mínima do chão. */
    public static class WanderGoal extends Goal {
        protected final DreamFlyerEntity mob;
        private final double speed;
        private final int minGround;
        private @Nullable Vec3 target;
        private int time;

        public WanderGoal(DreamFlyerEntity mob, double speed, int minGround) {
            this.mob = mob;
            this.speed = speed;
            this.minGround = minGround;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return this.mob.getRandom().nextInt(20) == 0;
        }

        @Override
        public void start() {
            Vec3 at = airSpot(this.mob, this.mob.position(), 8, 3);
            if (at != null && this.minGround > 0 && this.mob.level() instanceof ServerLevel level) {
                BlockPos ground = BlockPos.containing(at);
                int below = 0;
                while (below < this.minGround + 2 && level.getBlockState(ground.below(below + 1)).isAir()) {
                    below++;
                }
                if (below < this.minGround) {
                    at = at.add(0, this.minGround - below, 0);
                }
            }
            this.target = at;
            this.time = 60 + this.mob.getRandom().nextInt(60);
        }

        @Override
        public boolean canContinueToUse() {
            return this.target != null && this.time > 0 && this.mob.distanceToSqr(this.target) > 1.0;
        }

        @Override
        public void tick() {
            this.time--;
            if (this.target != null) {
                this.mob.flyTo(this.target, this.speed);
            }
        }
    }

    /** Ângulo de "batida" para animações de voo no cliente. */
    public float flap(float partialTick, float speed) {
        return Mth.sin((this.tickCount + partialTick) * speed);
    }
}

package com.mistech.mistechmod.realm.dream.mob;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;

/**
 * Gnomo Minerador (mina da Casa do Rei e Profundezas do Sonho): capacete com lanterna, picareta e saco de minério.
 * Passa o tempo batendo a picareta na pedra perto dele (só animação e som, não quebra nada) e revida se apanhar.
 * Mão vazia: ele "fareja" o minério mais próximo (raio {@link #PROSPECT_RADIUS}) e diz qual é, a que distância e para que lado.
 */
public class GnomeMinerEntity extends PathfinderMob {
    public static final int PROSPECT_RADIUS = 20;
    public static final int PROSPECT_COOLDOWN = 30 * 20;

    private final Map<UUID, Long> prospected = new HashMap<>();

    public GnomeMinerEntity(EntityType<? extends GnomeMinerEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 24.0)
                .add(Attributes.ARMOR, 4.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, true));
        this.goalSelector.addGoal(4, new MineGoal(this));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!player.getItemInHand(hand).isEmpty() || this.getTarget() == player) {
            return super.mobInteract(player, hand);
        }
        if (this.level() instanceof ServerLevel level) {
            long now = level.getGameTime();
            Long ready = this.prospected.get(player.getUUID());
            if (ready != null && now < ready) {
                player.sendOverlayMessage(Component.translatable("message.mistech.gnome_miner.tired"));
            } else {
                this.prospected.put(player.getUUID(), now + PROSPECT_COOLDOWN);
                this.prospect(level, player);
            }
            this.getLookControl().setLookAt(player);
            this.playSound(SoundEvents.VILLAGER_AMBIENT, 0.8F, 1.8F);
        }
        return InteractionResult.SUCCESS;
    }

    private void prospect(ServerLevel level, Player player) {
        BlockPos origin = this.blockPosition();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        int r = PROSPECT_RADIUS;
        for (BlockPos p : BlockPos.betweenClosed(origin.offset(-r, -r, -r), origin.offset(r, r, r))) {
            double d = p.distSqr(origin);
            if (d < bestDist && d <= r * r && level.getBlockState(p).is(Tags.Blocks.ORES)) {
                bestDist = d;
                best = p.immutable();
            }
        }
        if (best == null) {
            player.sendSystemMessage(Component.translatable("message.mistech.gnome_miner.none"));
            return;
        }
        BlockState state = level.getBlockState(best);
        int dx = best.getX() - origin.getX(), dy = best.getY() - origin.getY(), dz = best.getZ() - origin.getZ();
        String dir;
        if (Math.abs(dy) > Math.max(Math.abs(dx), Math.abs(dz))) {
            dir = dy > 0 ? "up" : "down";
        } else {
            dir = Direction.getApproximateNearest(dx, 0, dz).getName();
        }
        player.sendSystemMessage(Component.translatable("message.mistech.gnome_miner.found", state.getBlock().getName(),
                (int) Math.round(Math.sqrt(bestDist)), Component.translatable("message.mistech.gnome_miner.dir." + dir)));
        level.sendParticles(ParticleTypes.WAX_ON, this.getX(), this.getEyeY() + 0.4, this.getZ(), 5, 0.2, 0.1, 0.2, 0.0);
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return SoundEvents.VILLAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.VILLAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.VILLAGER_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 1.6F + (this.random.nextFloat() - 0.5F) * 0.15F;
    }

    /** Procura pedra ou minério perto, vai até lá e fica batendo a picareta (sem quebrar). */
    static class MineGoal extends Goal {
        private final GnomeMinerEntity gnome;
        private @Nullable BlockPos target;
        private int time;

        MineGoal(GnomeMinerEntity gnome) {
            this.gnome = gnome;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (this.gnome.getTarget() != null || this.gnome.getRandom().nextInt(60) != 0) {
                return false;
            }
            BlockPos origin = this.gnome.blockPosition();
            for (int i = 0; i < 16; i++) {
                BlockPos p = origin.offset(this.gnome.getRandom().nextInt(9) - 4, this.gnome.getRandom().nextInt(3), this.gnome.getRandom().nextInt(9) - 4);
                BlockState s = this.gnome.level().getBlockState(p);
                if ((s.is(Tags.Blocks.ORES) || s.is(Tags.Blocks.STONES) || s.is(net.minecraft.tags.BlockTags.BASE_STONE_OVERWORLD))
                        && this.gnome.level().getBlockState(p.below()).isAir() == false) {
                    for (Direction d : Direction.Plane.HORIZONTAL) {
                        BlockPos stand = p.relative(d);
                        if (this.gnome.level().getBlockState(stand).isAir() && this.gnome.level().getBlockState(stand.above()).isAir()) {
                            this.target = p;
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        @Override
        public void start() {
            this.time = 80 + this.gnome.getRandom().nextInt(80);
            BlockPos t = this.target;
            if (t != null) {
                this.gnome.getNavigation().moveTo(t.getX() + 0.5, t.getY(), t.getZ() + 0.5, 0.6);
            }
        }

        @Override
        public boolean canContinueToUse() {
            return this.target != null && this.time > 0 && this.gnome.getTarget() == null;
        }

        @Override
        public void tick() {
            BlockPos t = this.target;
            if (t == null) {
                return;
            }
            this.time--;
            this.gnome.getLookControl().setLookAt(Vec3.atCenterOf(t));
            if (this.gnome.distanceToSqr(Vec3.atCenterOf(t)) > 2.6 * 2.6) {
                if (this.gnome.getNavigation().isDone()) {
                    this.time -= 5;
                }
                return;
            }
            this.gnome.getNavigation().stop();
            if (this.time % 14 == 0 && this.gnome.level() instanceof ServerLevel level) {
                this.gnome.swing(InteractionHand.MAIN_HAND);
                BlockState s = level.getBlockState(t);
                level.playSound(null, t, s.getSoundType().getHitSound(), SoundSource.NEUTRAL, 0.6F, 0.9F + this.gnome.getRandom().nextFloat() * 0.2F);
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, s), t.getX() + 0.5, t.getY() + 0.5, t.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.05);
            }
        }

        @Override
        public void stop() {
            this.target = null;
        }
    }
}

package com.mistech.mistechmod.lore;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.Mistech;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Matinta Pereira: de dia/noite voa pela mata como um pássaro escuro que assobia ("fiiit") à noite.
 * Quem der <b>tabaco seco</b> ou <b>café</b> ganha presentes e Sorte, e ela some no ar.
 * Quem bater nela vê a <b>bruxa velha</b>: 70 de vida, bengala, maldições, espíritos (vexes) e some na fumaça.
 * Sem alvo por 30 s, volta a ser pássaro e vai embora.
 */
public class MatintaEntity extends PathfinderMob {
    private static final EntityDataAccessor<Boolean> WITCH = SynchedEntityData.defineId(MatintaEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> CASTING = SynchedEntityData.defineId(MatintaEntity.class, EntityDataSerializers.INT);
    public static final double BIRD_HEALTH = 16.0, WITCH_HEALTH = 70.0;

    private @Nullable Vec3 flyTarget;
    private int whistleTimer = 200;
    private int leavingTicks = -1;
    private int noTargetTicks;

    public MatintaEntity(EntityType<? extends MatintaEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, BIRD_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.ARMOR, 4.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(WITCH, false);
        builder.define(CASTING, 0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SpellGoal());
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, true) {
            @Override
            public boolean canUse() {
                return MatintaEntity.this.isWitch() && super.canUse();
            }
        });
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    public boolean isWitch() {
        return this.entityData.get(WITCH);
    }

    public boolean isCasting() {
        return this.entityData.get(CASTING) > 0;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (WITCH.equals(key)) {
            this.refreshDimensions();
        }
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        return this.isWitch() ? EntityDimensions.scalable(0.7F, 1.75F).withEyeHeight(1.45F) : super.getDefaultDimensions(pose);
    }

    // ----- forma -----

    private void becomeWitch(@Nullable LivingEntity attacker) {
        if (this.isWitch() || !(this.level() instanceof ServerLevel server)) {
            return;
        }
        this.entityData.set(WITCH, true);
        this.setNoGravity(false);
        this.flyTarget = null;
        var health = this.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.setBaseValue(WITCH_HEALTH);
        }
        this.setHealth((float) WITCH_HEALTH);
        this.setPersistenceRequired();
        server.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY() + 0.8, this.getZ(), 40, 0.4, 0.6, 0.4, 0.02);
        server.playSound(null, this.blockPosition(), SoundEvents.WITCH_CELEBRATE, SoundSource.HOSTILE, 1.4F, 0.6F);
        if (attacker != null) {
            this.setTarget(attacker);
            if (attacker instanceof Player player) {
                player.sendSystemMessage(Component.translatable("message.mistech.matinta.reveal").withStyle(ChatFormatting.DARK_PURPLE));
            }
        }
    }

    private void becomeBird() {
        if (!this.isWitch() || !(this.level() instanceof ServerLevel server)) {
            return;
        }
        this.entityData.set(WITCH, false);
        this.setNoGravity(true);
        var health = this.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.setBaseValue(BIRD_HEALTH);
        }
        this.setHealth((float) BIRD_HEALTH);
        server.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY() + 0.8, this.getZ(), 30, 0.4, 0.6, 0.4, 0.02);
        this.leavingTicks = 60;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (!this.isWitch() && source.getEntity() instanceof LivingEntity attacker) {
            // bater no pássaro revela a bruxa (o golpe não conta)
            this.becomeWitch(attacker);
            return false;
        }
        boolean hurt = super.hurtServer(level, source, amount);
        if (hurt && this.isWitch() && this.isAlive() && this.random.nextFloat() < 0.25F) {
            this.blink(level);
        }
        return hurt;
    }

    /** Some na fumaça e aparece perto (até 8 blocos). */
    private void blink(ServerLevel level) {
        for (int i = 0; i < 10; i++) {
            double x = this.getX() + (this.random.nextDouble() - 0.5) * 16;
            double z = this.getZ() + (this.random.nextDouble() - 0.5) * 16;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(x), (int) Math.floor(z));
            if (Math.abs(y - this.getY()) > 6) {
                continue;
            }
            level.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY() + 0.8, this.getZ(), 25, 0.3, 0.6, 0.3, 0.02);
            if (this.randomTeleport(x, y, z, false)) {
                level.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY() + 0.8, this.getZ(), 25, 0.3, 0.6, 0.3, 0.02);
                level.playSound(null, this.blockPosition(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.HOSTILE, 1.0F, 0.8F);
                return;
            }
        }
    }

    // ----- voo do pássaro -----

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        if (this.entityData.get(CASTING) > 0) {
            this.entityData.set(CASTING, this.entityData.get(CASTING) - 1);
        }
        if (this.leavingTicks >= 0) {
            this.setDeltaMovement(this.getDeltaMovement().add(0, 0.06, 0));
            if (--this.leavingTicks <= 0) {
                level.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY(), this.getZ(), 15, 0.3, 0.3, 0.3, 0.02);
                this.discard();
            }
            return;
        }
        if (this.isWitch()) {
            if (this.getTarget() == null || !this.getTarget().isAlive()) {
                if (++this.noTargetTicks > 600) {
                    this.becomeBird();
                }
            } else {
                this.noTargetTicks = 0;
            }
            return;
        }
        this.fly(level);
        if (level.isDarkOutside() && --this.whistleTimer <= 0) {
            this.whistleTimer = 300 + this.random.nextInt(400);
            level.playSound(null, this.blockPosition(), SoundEvents.NOTE_BLOCK_FLUTE.value(), SoundSource.NEUTRAL, 2.0F, 2.0F);
            level.playSound(null, this.blockPosition(), SoundEvents.NOTE_BLOCK_FLUTE.value(), SoundSource.NEUTRAL, 1.6F, 1.9F);
        }
    }

    private void fly(ServerLevel level) {
        RandomSource r = this.random;
        if (this.flyTarget == null || this.position().distanceToSqr(this.flyTarget) < 2.0 || r.nextInt(200) == 0 || this.horizontalCollision) {
            double x = this.getX() + (r.nextDouble() - 0.5) * 20;
            double z = this.getZ() + (r.nextDouble() - 0.5) * 20;
            int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(x), (int) Math.floor(z));
            this.flyTarget = new Vec3(x, ground + 2 + r.nextInt(6), z);
        }
        Vec3 to = this.flyTarget.subtract(this.position());
        Vec3 v = this.getDeltaMovement().scale(0.9).add(to.normalize().scale(0.03));
        if (v.length() > 0.35) {
            v = v.normalize().scale(0.35);
        }
        this.setDeltaMovement(v);
        if (v.horizontalDistanceSqr() > 1.0E-4) {
            this.setYRot((float) (Mth.atan2(v.z, v.x) * Mth.RAD_TO_DEG) - 90.0F);
            this.yBodyRot = this.getYRot();
        }
    }

    @Override
    public void travel(Vec3 input) {
        if (!this.isWitch()) {
            this.move(net.minecraft.world.entity.MoverType.SELF, this.getDeltaMovement());
            return;
        }
        super.travel(input);
    }

    @Override
    public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
        return false;
    }

    // ----- presente -----

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        boolean offering = held.is(Mistech.DRIED_TOBACCO_LEAF.get()) || held.is(Mistech.COFFEE_CUP.get());
        if (this.isWitch() || !offering || this.leavingTicks >= 0) {
            return super.mobInteract(player, hand);
        }
        if (this.level() instanceof ServerLevel level) {
            if (held.is(Mistech.COFFEE_CUP.get())) {
                player.getInventory().placeItemBackInInventory(new ItemStack(Mistech.CUP.get()));
            }
            held.consume(1, player);
            for (ItemStack gift : this.gifts(level.getRandom())) {
                ItemEntity item = new ItemEntity(level, player.getX(), player.getY() + 0.5, player.getZ(), gift);
                item.setPickUpDelay(10);
                level.addFreshEntity(item);
            }
            player.addEffect(new MobEffectInstance(MobEffects.LUCK, 6000, 1));
            player.sendSystemMessage(Component.translatable("message.mistech.matinta.gift").withStyle(ChatFormatting.GOLD));
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, this.getX(), this.getY() + 0.5, this.getZ(), 10, 0.3, 0.3, 0.3, 0.0);
            level.playSound(null, this.blockPosition(), SoundEvents.NOTE_BLOCK_FLUTE.value(), SoundSource.NEUTRAL, 1.5F, 1.5F);
            this.leavingTicks = 50;
        }
        return InteractionResult.SUCCESS;
    }

    private List<ItemStack> gifts(RandomSource r) {
        List<ItemStack> options = List.of(
                new ItemStack(Items.EMERALD, 1 + r.nextInt(3)),
                new ItemStack(Items.GOLD_NUGGET, 3 + r.nextInt(6)),
                new ItemStack(Mistech.TOBACCO_SEEDS.get(), 2 + r.nextInt(3)),
                new ItemStack(Items.AMETHYST_SHARD, 2 + r.nextInt(3)),
                new ItemStack(Items.GLOW_BERRIES, 3 + r.nextInt(4)),
                new ItemStack(Items.RABBIT_FOOT));
        ItemStack extra = options.get(r.nextInt(options.size()));
        return r.nextFloat() < 0.7F ? List.of(new ItemStack(LoreContent.MATINTA_FEATHER.get(), 1 + r.nextInt(2)), extra) : List.of(extra);
    }

    // ----- feitiços da bruxa -----

    private class SpellGoal extends Goal {
        private int cooldown = 40;

        @Override
        public boolean canUse() {
            LivingEntity target = MatintaEntity.this.getTarget();
            return MatintaEntity.this.isWitch() && target != null && target.isAlive() && --this.cooldown <= 0
                    && MatintaEntity.this.distanceToSqr(target) < 16 * 16 && MatintaEntity.this.hasLineOfSight(target);
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            MatintaEntity m = MatintaEntity.this;
            LivingEntity target = m.getTarget();
            if (target == null || !(m.level() instanceof ServerLevel level)) {
                return;
            }
            this.cooldown = 70 + m.random.nextInt(50);
            m.entityData.set(CASTING, 20);
            switch (m.random.nextInt(3)) {
                case 0 -> { // maldição: escuridão e fraqueza
                    target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0), m);
                    target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 0), m);
                    target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 1), m);
                    level.sendParticles(ParticleTypes.WITCH, target.getX(), target.getY() + 1, target.getZ(), 30, 0.4, 0.6, 0.4, 0.05);
                    level.playSound(null, m.blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.HOSTILE, 1.0F, 0.7F);
                }
                case 1 -> { // grito: tontura e empurrão
                    target.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 140, 0), m);
                    Vec3 push = target.position().subtract(m.position()).normalize().scale(1.2);
                    target.push(push.x, 0.4, push.z);
                    target.hurtMarked = true;
                    target.hurtServer(level, m.damageSources().indirectMagic(m, m), 3.0F);
                    level.playSound(null, m.blockPosition(), SoundEvents.NOTE_BLOCK_FLUTE.value(), SoundSource.HOSTILE, 3.0F, 2.0F);
                    level.sendParticles(ParticleTypes.SONIC_BOOM, m.getX(), m.getEyeY(), m.getZ(), 1, 0, 0, 0, 0);
                }
                default -> { // espíritos
                    for (int i = 0; i < 2; i++) {
                        Vex vex = EntityType.VEX.create(level, EntitySpawnReason.MOB_SUMMONED);
                        if (vex != null) {
                            BlockPos p = m.blockPosition().offset(m.random.nextInt(3) - 1, 1, m.random.nextInt(3) - 1);
                            vex.snapTo(p, 0.0F, 0.0F);
                            vex.setOwner(m);
                            vex.setBoundOrigin(p);
                            vex.setLimitedLife(20 * (15 + m.random.nextInt(10)));
                            vex.setTarget(target);
                            level.addFreshEntity(vex);
                        }
                    }
                    level.playSound(null, m.blockPosition(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, 1.0F, 0.8F);
                }
            }
        }
    }

    // ----- sons, despawn e salvar -----

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return this.isWitch() ? SoundEvents.WITCH_AMBIENT : null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return this.isWitch() ? SoundEvents.WITCH_HURT : SoundEvents.PARROT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WITCH_DEATH;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return !this.isWitch();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("Witch", this.isWitch());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        boolean witch = input.getBooleanOr("Witch", false);
        this.entityData.set(WITCH, witch);
        this.setNoGravity(!witch);
    }
}

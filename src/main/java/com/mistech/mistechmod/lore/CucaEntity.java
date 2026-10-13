package com.mistech.mistechmod.lore;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Cuca, a bruxa-jacaré (chefe, 400 de vida). Morde forte; a cada poucos segundos um feitiço:
 * <b>poção</b> (veneno, lentidão, fraqueza ou dano arremessados), <b>caldeirão</b> (nuvem venenosa borbulhando em volta dela),
 * <b>"Dorme neném"</b> (cegueira, lentidão forte e fadiga em quem está perto; ela se cura um pouco).
 * Abaixo da metade chama 2 bruxas aprendizes (uma vez) e fica mais rápida. Às vezes some e reaparece perto.
 * Vem por ritual ou atrás de quem passa 3 noites sem dormir na Mata Ancestral. Solta o Caldeirão da Cuca.
 */
public class CucaEntity extends Monster {
    private static final EntityDataAccessor<Integer> CASTING = SynchedEntityData.defineId(CucaEntity.class, EntityDataSerializers.INT);
    private final ServerBossEvent bossEvent = new ServerBossEvent(Mth.createInsecureUUID(this.random), this.getDisplayName(),
            BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);
    private int spellCooldown = 60;
    private boolean summoned;

    public CucaEntity(EntityType<? extends CucaEntity> type, Level level) {
        super(type, level);
        this.xpReward = 200;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 400.0)
                .add(Attributes.ATTACK_DAMAGE, 10.0)
                .add(Attributes.ARMOR, 8.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CASTING, 0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public boolean isCasting() {
        return this.entityData.get(CASTING) > 0;
    }

    private boolean enraged() {
        return this.getHealth() < this.getMaxHealth() / 2;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        if (this.entityData.get(CASTING) > 0) {
            this.entityData.set(CASTING, this.entityData.get(CASTING) - 1);
        }
        var speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.setBaseValue(this.enraged() ? 0.31 : 0.25);
        }
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }
        if (this.enraged() && !this.summoned) {
            this.summoned = true;
            this.summonApprentices(level, target);
        }
        if (--this.spellCooldown > 0 || this.distanceToSqr(target) > 24 * 24) {
            return;
        }
        this.spellCooldown = (this.enraged() ? 45 : 70) + this.random.nextInt(30);
        this.entityData.set(CASTING, 25);
        int roll = this.random.nextInt(10);
        if (roll < 5) {
            this.throwPotion(level, target);
        } else if (roll < 8) {
            this.cauldron(level);
        } else {
            this.lullaby(level);
        }
    }

    private void throwPotion(ServerLevel level, LivingEntity target) {
        Vec3 tm = target.getDeltaMovement();
        double xd = target.getX() + tm.x - this.getX();
        double yd = target.getEyeY() - 1.1 - this.getY() - 1.0;
        double zd = target.getZ() + tm.z - this.getZ();
        double dist = Math.sqrt(xd * xd + zd * zd);
        Holder<Potion> potion = switch (this.random.nextInt(4)) {
            case 0 -> Potions.POISON;
            case 1 -> Potions.SLOWNESS;
            case 2 -> Potions.WEAKNESS;
            default -> Potions.HARMING;
        };
        ItemStack stack = PotionContents.createItemStack(Items.SPLASH_POTION, potion);
        for (int i = 0; i < (this.enraged() ? 2 : 1); i++) {
            Projectile.spawnProjectileUsingShoot(ThrownSplashPotion::new, level, stack.copy(), this, xd, yd + dist * 0.2, zd, 0.85F, 6.0F + i * 6.0F);
        }
        level.playSound(null, this.blockPosition(), SoundEvents.WITCH_THROW, SoundSource.HOSTILE, 1.2F, 0.7F);
    }

    /** O caldeirão da Cuca: uma nuvem venenosa borbulhando em volta dela. */
    private void cauldron(ServerLevel level) {
        AreaEffectCloud cloud = new AreaEffectCloud(level, this.getX(), this.getY(), this.getZ());
        cloud.setOwner(this);
        cloud.setRadius(4.5F);
        cloud.setDuration(200);
        cloud.setRadiusPerTick(-0.01F);
        cloud.setWaitTime(10);
        cloud.addEffect(new MobEffectInstance(MobEffects.POISON, 120, 1));
        cloud.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 120, 0));
        level.addFreshEntity(cloud);
        level.sendParticles(ParticleTypes.WITCH, this.getX(), this.getY() + 1, this.getZ(), 40, 2, 0.5, 2, 0.05);
        level.playSound(null, this.blockPosition(), SoundEvents.BREWING_STAND_BREW, SoundSource.HOSTILE, 2.0F, 0.5F);
        level.playSound(null, this.blockPosition(), SoundEvents.WITCH_CELEBRATE, SoundSource.HOSTILE, 1.5F, 0.6F);
    }

    /** "Dorme neném, que a Cuca vem pegar..." */
    private void lullaby(ServerLevel level) {
        for (Player player : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(16))) {
            if (player.isCreative() || player.isSpectator()) {
                continue;
            }
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0), this);
            player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 160, 2), this);
            player.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 160, 1), this);
            player.sendSystemMessage(Component.translatable("message.mistech.cuca.lullaby").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
        }
        this.heal(15.0F);
        level.sendParticles(ParticleTypes.NOTE, this.getX(), this.getY() + 3.2, this.getZ(), 12, 1.0, 0.4, 1.0, 1.0);
        for (int i = 0; i < 4; i++) {
            level.playSound(null, this.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.HOSTILE, 2.0F, 0.6F + i * 0.1F);
        }
    }

    private void summonApprentices(ServerLevel level, LivingEntity target) {
        for (int i = 0; i < 2; i++) {
            Witch witch = EntityType.WITCH.create(level, EntitySpawnReason.MOB_SUMMONED);
            if (witch == null) {
                continue;
            }
            double a = this.random.nextDouble() * Math.PI * 2;
            int x = (int) Math.floor(this.getX() + Math.cos(a) * 4), z = (int) Math.floor(this.getZ() + Math.sin(a) * 4);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            witch.snapTo(x + 0.5, y, z + 0.5, this.random.nextFloat() * 360, 0);
            witch.setTarget(target);
            level.addFreshEntity(witch);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, x + 0.5, y + 1, z + 0.5, 20, 0.3, 0.6, 0.3, 0.02);
        }
        level.playSound(null, this.blockPosition(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, 2.0F, 0.6F);
        for (Player p : level.getEntitiesOfClass(Player.class, new AABB(this.blockPosition()).inflate(48))) {
            p.sendSystemMessage(Component.translatable("message.mistech.cuca.apprentices").withStyle(ChatFormatting.DARK_PURPLE));
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        boolean hurt = super.hurtServer(level, source, amount);
        if (hurt && this.isAlive() && this.random.nextFloat() < 0.12F) {
            for (int i = 0; i < 10; i++) {
                double x = this.getX() + (this.random.nextDouble() - 0.5) * 20;
                double z = this.getZ() + (this.random.nextDouble() - 0.5) * 20;
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(x), (int) Math.floor(z));
                BlockPos from = this.blockPosition();
                if (Math.abs(y - this.getY()) < 6 && this.randomTeleport(x, y, z, false)) {
                    level.sendParticles(ParticleTypes.LARGE_SMOKE, from.getX() + 0.5, from.getY() + 1.5, from.getZ() + 0.5, 30, 0.5, 1, 0.5, 0.02);
                    level.playSound(null, this.blockPosition(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.HOSTILE, 1.5F, 0.6F);
                    break;
                }
            }
        }
        return hurt;
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return effect.getEffect() != MobEffects.POISON && super.canBeAffected(effect);
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return this.random.nextBoolean() ? SoundEvents.WITCH_AMBIENT : SoundEvents.HOGLIN_AMBIENT;
    }

    @Override
    public float getVoicePitch() {
        return 0.6F;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WITCH_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WITCH_DEATH;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("Summoned", this.summoned);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.summoned = input.getBooleanOr("Summoned", false);
        if (this.hasCustomName()) {
            this.bossEvent.setName(this.getDisplayName());
        }
    }
}

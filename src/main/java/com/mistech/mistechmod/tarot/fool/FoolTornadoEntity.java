package com.mistech.mistechmod.tarot.fool;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Tornado do Louco. Anda pelo ar (atravessa blocos e o vazio entre as ilhas), girando; quem encosta leva dano e é
 * erguido e empurrado — de novo só depois de 1 s.
 * <ul>
 * <li>Do chefe ({@link #fromBoss}): persegue devagar o alvo, 6 s, maior.</li>
 * <li>Do Cajado do Louco ({@link #fromPlayer}): vai reto, 3 s, menor; não afeta jogadores nem quem o lançou.</li>
 * </ul>
 * Não é salvo no mundo.
 */
public class FoolTornadoEntity extends Entity {
    private static final EntityDataAccessor<Boolean> DATA_BIG = SynchedEntityData.defineId(FoolTornadoEntity.class, EntityDataSerializers.BOOLEAN);
    private static final int HIT_COOLDOWN = 20;

    private @Nullable UUID owner;
    private @Nullable UUID target;
    private boolean fromPlayer;
    private Vec3 direction = new Vec3(0.0, 0.0, 1.0);
    private double speed = 0.22;
    private int maxLife = 120;
    private float damage = 3.0F;
    private double lift = 1.0;
    private final Map<UUID, Integer> lastHit = new HashMap<>();

    public FoolTornadoEntity(EntityType<? extends FoolTornadoEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public static FoolTornadoEntity fromBoss(ServerLevel level, FoolEntity boss, Vec3 pos, Vec3 direction, @Nullable Player target) {
        FoolTornadoEntity tornado = new FoolTornadoEntity(FoolContent.TORNADO.get(), level);
        tornado.setPos(pos);
        tornado.owner = boss.getUUID();
        tornado.target = target != null ? target.getUUID() : null;
        tornado.direction = direction.normalize();
        tornado.entityData.set(DATA_BIG, true);
        tornado.speed = boss.isPhase2() ? 0.27 : 0.22;
        tornado.maxLife = 120;
        tornado.damage = 3.0F;
        tornado.lift = 1.0;
        return tornado;
    }

    public static FoolTornadoEntity fromPlayer(ServerLevel level, Player player) {
        FoolTornadoEntity tornado = new FoolTornadoEntity(FoolContent.TORNADO.get(), level);
        Vec3 look = player.getLookAngle().multiply(1.0, 0.0, 1.0);
        look = look.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : look.normalize();
        tornado.setPos(player.getX() + look.x * 1.2, player.getY(), player.getZ() + look.z * 1.2);
        tornado.owner = player.getUUID();
        tornado.fromPlayer = true;
        tornado.direction = look;
        tornado.speed = 0.45;
        tornado.maxLife = 60;
        tornado.damage = FoolContent.STAFF_TORNADO_DAMAGE;
        tornado.lift = 0.8;
        return tornado;
    }

    public boolean isBig() {
        return this.entityData.get(DATA_BIG);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_BIG, false);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            this.clientParticles();
            return;
        }
        ServerLevel level = (ServerLevel) this.level();
        if (this.tickCount > this.maxLife) {
            level.sendParticles(ParticleTypes.POOF, this.getX(), this.getY() + 1.0, this.getZ(), 10, 0.3, 0.6, 0.3, 0.02);
            this.discard();
            return;
        }
        // persegue o alvo (só o do chefe), virando devagar
        if (this.target != null && level.getEntity(this.target) instanceof Player player && player.isAlive()) {
            Vec3 want = new Vec3(player.getX() - this.getX(), 0.0, player.getZ() - this.getZ());
            if (want.lengthSqr() > 1.0E-4) {
                this.direction = this.direction.lerp(want.normalize(), 0.05).normalize();
            }
            double dy = player.getY() - this.getY();
            this.setPos(this.getX(), this.getY() + Math.max(-0.08, Math.min(0.08, dy * 0.05)), this.getZ());
        }
        this.setPos(this.getX() + this.direction.x * this.speed, this.getY(), this.getZ() + this.direction.z * this.speed);
        if (this.tickCount % 20 == 0) {
            level.playSound(null, this.blockPosition(), SoundEvents.BREEZE_WHIRL, SoundSource.HOSTILE, 0.6F, 1.2F);
        }
        this.hitEntities(level);
    }

    private void hitEntities(ServerLevel level) {
        Entity ownerEntity = this.owner == null ? null : level.getEntity(this.owner);
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(0.2))) {
            if (!living.isAlive() || living.getUUID().equals(this.owner) || living instanceof FoolEntity) {
                continue;
            }
            if (living instanceof Player player && (player.isSpectator() || player.isCreative() || this.fromPlayer)) {
                continue;
            }
            Integer last = this.lastHit.get(living.getUUID());
            if (last != null && this.tickCount - last < HIT_COOLDOWN) {
                continue;
            }
            this.lastHit.put(living.getUUID(), this.tickCount);
            DamageSource source = ownerEntity instanceof Player player ? this.damageSources().playerAttack(player)
                    : ownerEntity instanceof LivingEntity mob ? this.damageSources().mobAttack(mob)
                    : this.damageSources().magic();
            living.hurtServer(level, source, this.damage);
            living.setDeltaMovement(this.direction.x * 0.6, this.lift, this.direction.z * 0.6);
            living.hurtMarked = true;
            if (living instanceof ServerPlayer serverPlayer) {
                serverPlayer.resetFallDistance();
            }
            level.sendParticles(ParticleTypes.GUST, living.getX(), living.getY() + 0.5, living.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    private void clientParticles() {
        double scale = this.isBig() ? 1.0 : 0.7;
        float t = this.tickCount;
        for (int i = 0; i < 3; i++) {
            double h = this.random.nextDouble() * 2.8 * scale;
            double r = (0.25 + h * 0.35) * scale;
            double a = t * 0.5 + h * 2.2 + i * 2.1;
            this.level().addParticle(ParticleTypes.CLOUD, this.getX() + Math.cos(a) * r, this.getY() + h, this.getZ() + Math.sin(a) * r,
                    -Math.sin(a) * 0.05, 0.03, Math.cos(a) * 0.05);
        }
        if (this.random.nextInt(6) == 0) {
            this.level().addParticle(ParticleTypes.SMALL_GUST, this.getX(), this.getY() + 1.2 * scale, this.getZ(), 0.0, 0.0, 0.0);
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {}

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {}
}

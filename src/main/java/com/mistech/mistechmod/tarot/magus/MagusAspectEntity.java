package com.mistech.mistechmod.tarot.magus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.tarot.magus.MagusSpellEntity.Kind;

import net.minecraft.core.BlockPos;
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
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Um dos Quatro Aspectos (Fase III): a forma monstruosa de um elemento, menor (60%), que luta em time.
 * <ul>
 * <li><b>Terra</b> (tanque): provoca (puxa os jogadores), levanta muralhas na frente de quem está fraco e absorve
 * metade do dano dos aliados perto dele.</li>
 * <li><b>Água</b> (suporte): cura os outros, põe bolhas neles e, viva, revive quem cair depois de 15 s.</li>
 * <li><b>Ar</b> (controle): troca de lugar com um aliado em perigo, empurra os jogadores para longe dele e para perto da Terra.</li>
 * <li><b>Fogo</b> (dano): fica atrás da Terra soltando meteoros; frágil (+50% de dano).</li>
 * </ul>
 * Cada um tem sua barra de vida. Não é salvo (a luta não sobrevive a reiniciar o servidor).
 */
public class MagusAspectEntity extends Monster implements MagusCaster {
    private static final EntityDataAccessor<Integer> DATA_ELEMENT = SynchedEntityData.defineId(MagusAspectEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ACTION = SynchedEntityData.defineId(MagusAspectEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_BUBBLE = SynchedEntityData.defineId(MagusAspectEntity.class, EntityDataSerializers.FLOAT);
    public static final float SCALE = 0.6F;

    private @Nullable UUID owner;
    private @Nullable BlockPos arenaCenter;
    private final List<Task> tasks = new ArrayList<>();
    private @Nullable ServerBossEvent bar;
    private int actionTicks;
    private int actionLength;
    private int cdA = 40, cdB = 100, cdC = 160, cdD = 300;
    private float bubbleHp;
    private int clientActionStart;

    private record Task(int at, Runnable run) {}

    public MagusAspectEntity(EntityType<? extends MagusAspectEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.xpReward = 20;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.ARMOR, 8.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.FOLLOW_RANGE, 64.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    static @Nullable MagusAspectEntity create(ServerLevel level, MagusEntity owner, MagusElement element, float maxHealth, float fraction, Vec3 at) {
        MagusAspectEntity a = MagusContent.ASPECT.get().create(level, EntitySpawnReason.EVENT);
        if (a == null) {
            return null;
        }
        a.owner = owner.getUUID();
        a.arenaCenter = owner.arenaCenter();
        a.entityData.set(DATA_ELEMENT, element.ordinal());
        a.refreshDimensions();
        a.setNoGravity(element == MagusElement.AIR || element == MagusElement.WATER);
        AttributeInstance hp = a.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) {
            hp.setBaseValue(Math.max(10.0F, maxHealth));
        }
        a.setHealth(a.getMaxHealth() * fraction);
        a.snapTo(at.x, at.y + (a.isNoGravity() ? 1.5 : 0), at.z, owner.getYRot(), 0);
        a.setCustomName(Component.translatable("entity.mistech.magician_aspect." + element.id).withStyle(element.text));
        a.bar = new ServerBossEvent(UUID.randomUUID(), a.getCustomName(), element.bar, BossEvent.BossBarOverlay.PROGRESS);
        return a;
    }

    public MagusElement element() {
        return MagusElement.byId(this.entityData.get(DATA_ELEMENT));
    }

    public int action() {
        return this.entityData.get(DATA_ACTION);
    }

    public float bubble() {
        return this.entityData.get(DATA_BUBBLE);
    }

    public float actionTime(float partialTick) {
        return this.tickCount - this.clientActionStart + partialTick;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ELEMENT, 0);
        builder.define(DATA_ACTION, MagusEntity.IDLE);
        builder.define(DATA_BUBBLE, 0.0F);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (DATA_ELEMENT.equals(accessor)) {
            this.refreshDimensions();
        }
        if (DATA_ACTION.equals(accessor)) {
            this.clientActionStart = this.tickCount;
        }
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        return MagusEntity.dimensionsFor(this.element(), SCALE);
    }

    @Override
    public Mob self() {
        return this;
    }

    @Override
    public @Nullable BlockPos arenaCenter() {
        return this.arenaCenter;
    }

    @Override
    public float power() {
        return 0.75F;
    }

    @Override
    public void schedule(int delay, Runnable task) {
        this.tasks.add(new Task(this.tickCount + Math.max(0, delay), task));
    }

    private void setAction(int action, int length) {
        this.entityData.set(DATA_ACTION, action);
        this.actionTicks = 0;
        this.actionLength = length;
    }

    // ------------------------------------------------------------------ IA

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        MagusEntity boss = MagusEntity.owner(level, this.owner);
        if (boss == null) {
            this.discard();
            return;
        }
        this.runTasks();
        this.actionTicks++;
        if (this.action() != MagusEntity.IDLE && this.actionTicks >= this.actionLength) {
            this.setAction(MagusEntity.IDLE, 0);
        }
        cdA--; cdB--; cdC--; cdD--;
        Player target = MagusSpells.players(level, this).stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
        if (target != null) {
            double dx = target.getX() - this.getX(), dz = target.getZ() - this.getZ();
            float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
            this.setYRot(yaw);
            this.setYHeadRot(yaw);
            this.setYBodyRot(yaw);
        }
        // barra de vida
        if (this.bar != null) {
            this.bar.setProgress(Mth.clamp(this.getHealth() / this.getMaxHealth(), 0.0F, 1.0F));
            if (this.tickCount % 20 == 1) {
                List<Player> ps = MagusSpells.players(level, this);
                for (ServerPlayer p : new ArrayList<>(this.bar.getPlayers())) {
                    if (!ps.contains(p)) this.bar.removePlayer(p);
                }
                for (Player p : ps) {
                    if (p instanceof ServerPlayer sp) this.bar.addPlayer(sp);
                }
            }
        }
        // presa na arena
        if (this.arenaCenter != null) {
            Vec3 c = Vec3.atBottomCenterOf(this.arenaCenter.above());
            if (this.position().subtract(c).horizontalDistance() > MagusArena.RADIUS || this.getY() < c.y - 6) {
                this.teleportTo(c.x, c.y + 1, c.z);
            }
        }
        if (target == null) {
            return;
        }
        List<MagusAspectEntity> allies = boss.aliveAspects(level);
        allies.remove(this);
        switch (this.element()) {
            case EARTH -> this.tank(level, target, allies);
            case WATER -> this.support(level, target, allies);
            case AIR -> this.control(level, target, allies, boss);
            case FIRE -> this.damage(level, target, boss);
            default -> {}
        }
    }

    private void runTasks() {
        if (this.tasks.isEmpty()) return;
        List<Task> due = new ArrayList<>();
        Iterator<Task> it = this.tasks.iterator();
        while (it.hasNext()) {
            Task t = it.next();
            if (t.at <= this.tickCount) {
                due.add(t);
                it.remove();
            }
        }
        for (Task t : due) {
            if (this.isAlive()) t.run.run();
        }
    }

    private boolean idle() {
        return this.action() == MagusEntity.IDLE;
    }

    private void walk(Vec3 goal, double speed) {
        Vec3 d = new Vec3(goal.x - this.getX(), 0, goal.z - this.getZ());
        if (d.length() < 1.2 || !this.idle()) {
            this.setDeltaMovement(0, this.getDeltaMovement().y, 0);
            return;
        }
        d = d.normalize().scale(speed);
        double vy = this.horizontalCollision && this.onGround() ? 0.5 : this.getDeltaMovement().y;
        this.setDeltaMovement(d.x, vy, d.z);
    }

    private void fly(Vec3 goal, double factor) {
        Vec3 d = goal.subtract(this.position()).scale(factor);
        if (d.length() > 0.5) d = d.normalize().scale(0.5);
        this.setDeltaMovement(d);
    }

    private double floor() {
        return this.arenaCenter != null ? this.arenaCenter.getY() + 1.0 : MagusSpells.groundY((ServerLevel) this.level(), this.position());
    }

    private Vec3 inArena(Vec3 goal) {
        if (this.arenaCenter == null) return goal;
        Vec3 c = Vec3.atBottomCenterOf(this.arenaCenter);
        Vec3 d = new Vec3(goal.x - c.x, 0, goal.z - c.z);
        double max = MagusArena.PAINT_RADIUS - 3;
        if (d.length() > max) {
            d = d.normalize().scale(max);
            return new Vec3(c.x + d.x, goal.y, c.z + d.z);
        }
        return goal;
    }

    // ----- Terra: tanque -----
    private void tank(ServerLevel level, Player target, List<MagusAspectEntity> allies) {
        this.walk(target.position(), 0.1);
        if (!this.idle()) return;
        if (this.distanceTo(target) < 4.0 && cdA <= 0) {
            cdA = 35;
            this.setAction(MagusEntity.SLAM, 20);
            this.schedule(10, () -> MagusSpells.slam(level, this, 3.5, MagusSpells.SLAM));
        } else if (cdB <= 0) {
            cdB = 140;
            this.setAction(MagusEntity.ROAR, 24);
            // provoca: puxa todos para perto
            level.playSound(null, this.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 2.5F, 0.8F);
            for (Player p : MagusSpells.players(level, this)) {
                Vec3 to = this.position().subtract(p.position());
                if (to.horizontalDistance() < 18 && to.horizontalDistance() > 3) {
                    MagusSpellEntity.push(p, MagusSpells.flat(to).scale(1.2), 0.35);
                    p.sendOverlayMessage(Component.translatable("message.mistech.magus.taunt"));
                }
            }
        } else if (cdC <= 0 && !allies.isEmpty()) {
            cdC = 200;
            MagusAspectEntity weak = allies.stream().min(Comparator.comparingDouble(a -> a.getHealth() / a.getMaxHealth())).orElse(null);
            if (weak != null) {
                this.setAction(MagusEntity.STOMP, 24);
                this.schedule(10, () -> this.raiseWall(level, weak, target));
            }
        } else if (cdD <= 0) {
            cdD = 160;
            this.setAction(MagusEntity.SLAM, 24);
            this.schedule(10, () -> MagusSpells.spikeLine(level, this, target.position(), MagusSpells.SPIKE));
        }
    }

    /** Muralha de pedra entre o aliado fraco e o jogador. */
    private void raiseWall(ServerLevel level, MagusAspectEntity ally, Player target) {
        MagusArena.Field field = this.field();
        Vec3 dir = MagusSpells.flat(target.position().subtract(ally.position()));
        Vec3 mid = ally.position().add(dir.scale(3.0));
        Vec3 side = new Vec3(-dir.z, 0, dir.x);
        double y = MagusSpells.groundY(level, mid.add(0, 1, 0));
        level.playSound(null, BlockPos.containing(mid), SoundEvents.ROOTED_DIRT_PLACE, SoundSource.HOSTILE, 2.0F, 0.5F);
        for (int i = -3; i <= 3; i++) {
            Vec3 p = mid.add(side.scale(i));
            for (int h = 0; h < 3; h++) {
                BlockPos pos = BlockPos.containing(p.x, y + h + 0.1, p.z);
                if (field != null) {
                    field.temp(level, pos, Blocks.MOSSY_STONE_BRICKS.defaultBlockState(), 160, false);
                }
                level.sendParticles(ParticleTypes.DUST_PLUME, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 2, 0.3, 0.3, 0.3, 0.02);
            }
        }
    }

    // ----- Água: suporte -----
    private void support(ServerLevel level, Player target, List<MagusAspectEntity> allies) {
        Vec3 away = MagusSpells.flat(this.position().subtract(target.position()));
        Vec3 goal = target.position().add(away.scale(11));
        this.fly(this.inArena(new Vec3(goal.x, this.floor() + 1.0 + Math.sin(this.tickCount * 0.08) * 0.3, goal.z)), 0.05);
        if (this.bubbleHp > 0 && this.tickCount % 20 == 0) {
            this.heal(this.getMaxHealth() * 0.02F);
        }
        if (!this.idle()) return;
        if (cdA <= 0 && !allies.isEmpty()) {
            MagusAspectEntity weak = allies.stream().min(Comparator.comparingDouble(a -> a.getHealth() / a.getMaxHealth())).orElse(null);
            if (weak != null && weak.getHealth() < weak.getMaxHealth() * 0.95F) {
                cdA = 50;
                this.setAction(MagusEntity.CAST, 16);
                weak.heal(weak.getMaxHealth() * 0.06F);
                Vec3 from = this.position().add(0, this.getBbHeight() * 0.8, 0);
                Vec3 to = weak.position().add(0, weak.getBbHeight() * 0.6, 0);
                for (int i = 0; i <= 12; i++) {
                    Vec3 q = from.lerp(to, i / 12.0);
                    level.sendParticles(ParticleTypes.SPLASH, q.x, q.y, q.z, 2, 0.05, 0.05, 0.05, 0.0);
                }
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, to.x, to.y, to.z, 6, 0.5, 0.5, 0.5, 0.0);
                level.playSound(null, weak.blockPosition(), SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE, SoundSource.HOSTILE, 1.5F, 1.2F);
                return;
            }
        }
        if (cdB <= 0) {
            cdB = 240;
            List<MagusAspectEntity> all = new ArrayList<>(allies);
            all.add(this);
            MagusAspectEntity pick = all.stream().filter(a -> a.bubbleHp <= 0)
                    .min(Comparator.comparingDouble(a -> a.getHealth() / a.getMaxHealth())).orElse(null);
            if (pick != null) {
                this.setAction(MagusEntity.CAST, 16);
                pick.giveBubble(pick.getMaxHealth() * 0.3F);
            }
            return;
        }
        if (cdC <= 0) {
            cdC = 70;
            this.setAction(MagusEntity.CAST, 14);
            this.schedule(7, () -> MagusSpells.fan(level, this, this.position().add(0, this.getBbHeight() * 0.7, 0),
                    target.position().add(0, 1, 0), Kind.ICE_ORB, 2, 0.3, 0.6, MagusSpells.ICE));
        }
    }

    void giveBubble(float hp) {
        this.bubbleHp = hp;
        this.entityData.set(DATA_BUBBLE, 1.0F);
        if (this.level() instanceof ServerLevel level) {
            level.playSound(null, this.blockPosition(), SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, SoundSource.HOSTILE, 2.0F, 0.8F);
        }
    }

    // ----- Ar: controle -----
    private void control(ServerLevel level, Player target, List<MagusAspectEntity> allies, MagusEntity boss) {
        double ang = Math.atan2(this.getZ() - target.getZ(), this.getX() - target.getX()) + 0.04;
        Vec3 goal = target.position().add(Math.cos(ang) * 8, 0, Math.sin(ang) * 8);
        this.fly(this.inArena(new Vec3(goal.x, this.floor() + 3.0 + Math.sin(this.tickCount * 0.1) * 0.6, goal.z)), 0.1);
        if (!this.idle()) return;
        if (cdB <= 0) {
            // aliado em perigo: troca de lugar com ele e afasta os jogadores
            MagusAspectEntity danger = null;
            for (MagusAspectEntity a : allies) {
                if (a.getHealth() < a.getMaxHealth() * 0.4F) {
                    for (Player p : MagusSpells.players(level, this)) {
                        if (p.distanceTo(a) < 6) {
                            danger = a;
                        }
                    }
                }
            }
            if (danger != null) {
                cdB = 160;
                this.setAction(MagusEntity.BLINK, 12);
                Vec3 mine = this.position();
                Vec3 theirs = danger.position();
                this.teleportTo(theirs.x, theirs.y + 1, theirs.z);
                danger.teleportTo(mine.x, this.floor(), mine.z);
                level.sendParticles(ParticleTypes.GUST_EMITTER_SMALL, theirs.x, theirs.y + 1, theirs.z, 1, 0, 0, 0, 0);
                level.playSound(null, this.blockPosition(), SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), SoundSource.HOSTILE, 2.0F, 0.8F);
                for (Player p : MagusSpells.players(level, this)) {
                    Vec3 away = p.position().subtract(theirs);
                    if (away.horizontalDistance() < 7) {
                        MagusSpellEntity.push(p, MagusSpells.flat(away).scale(1.4), 0.4);
                    }
                }
                return;
            }
        }
        if (cdC <= 0) {
            MagusAspectEntity earth = boss.aspect(level, MagusElement.EARTH);
            cdC = 200;
            if (earth != null) {
                // rajada que joga os jogadores para perto da Terra
                this.setAction(MagusEntity.CAST, 16);
                level.playSound(null, this.blockPosition(), SoundEvents.BREEZE_INHALE, SoundSource.HOSTILE, 2.0F, 0.7F);
                this.schedule(10, () -> {
                    for (Player p : MagusSpells.players(level, this)) {
                        Vec3 to = earth.position().subtract(p.position());
                        if (to.horizontalDistance() > 4 && to.horizontalDistance() < 22) {
                            MagusSpellEntity.push(p, MagusSpells.flat(to).scale(1.3), 0.5);
                            level.sendParticles(ParticleTypes.GUST, p.getX(), p.getY() + 1, p.getZ(), 1, 0, 0, 0, 0);
                        }
                    }
                });
                return;
            }
        }
        if (cdA <= 0) {
            cdA = 45 + this.random.nextInt(20);
            this.setAction(MagusEntity.CAST, 14);
            if (this.random.nextBoolean()) {
                this.schedule(7, () -> MagusSpells.fan(level, this, this.position().add(0, this.getBbHeight() * 0.6, 0),
                        target.position().add(0, 1, 0), Kind.WIND_BOLT, 5, 0.22, 0.75, MagusSpells.BOLT));
            } else {
                this.schedule(7, () -> MagusSpells.ring(level, this, this.position().add(0, this.getBbHeight() * 0.6, 0),
                        Kind.WIND_BOLT, 10, 0.6, MagusSpells.BOLT, this.random.nextDouble()));
            }
        }
    }

    // ----- Fogo: dano -----
    private void damage(ServerLevel level, Player target, MagusEntity boss) {
        MagusAspectEntity earth = boss.aspect(level, MagusElement.EARTH);
        Vec3 goal;
        if (earth != null) {
            goal = earth.position().add(MagusSpells.flat(earth.position().subtract(target.position())).scale(5));
        } else {
            goal = target.position().add(MagusSpells.flat(this.position().subtract(target.position())).scale(12));
        }
        this.walk(this.inArena(goal), 0.11);
        if (!this.idle()) return;
        if (cdD <= 0 && boss.aspect(level, MagusElement.WATER) != null) {
            cdD = 300;
            this.setAction(MagusEntity.CAST, 20);
            this.schedule(10, () -> MagusSpells.steam(level, this, target.position(), 3.5, 100, MagusSpells.STEAM));
            return;
        }
        if (cdB <= 0) {
            cdB = 90;
            this.setAction(MagusEntity.CAST, 22);
            this.schedule(8, () -> MagusSpells.meteors(level, this, MagusSpells.strikePoints(level, this, 2, 5), Kind.METEOR, MagusSpells.METEOR, 30));
        } else if (cdA <= 0) {
            cdA = 50;
            this.setAction(MagusEntity.THROW, 18);
            this.schedule(9, () -> MagusSpells.fan(level, this, this.position().add(0, this.getBbHeight() * 0.8, 0),
                    target.position().add(0, 1, 0), Kind.FIREBALL, 1, 0, 0.7, MagusSpells.FIREBALL));
        }
    }

    // ------------------------------------------------------------------ dano

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (source.getEntity() == null && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        if (source.is(DamageTypeTags.IS_FALL) || source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.LIGHTNING_BOLT)
                || source.getEntity() instanceof MagusAspectEntity || source.getEntity() instanceof MagusEntity) {
            return false;
        }
        MagusElement e = this.element();
        if (e == MagusElement.FIRE && source.is(DamageTypeTags.IS_FIRE)) {
            return false;
        }
        if (e == MagusElement.FIRE) {
            damage *= 1.5F;
        } else if (e == MagusElement.EARTH) {
            damage *= 0.6F;
        }
        if (this.bubbleHp > 0) {
            this.bubbleHp -= damage;
            level.sendParticles(ParticleTypes.SPLASH, this.getX(), this.getY() + this.getBbHeight() / 2, this.getZ(), 10, 0.8, 0.8, 0.8, 0.1);
            if (this.bubbleHp <= 0) {
                this.entityData.set(DATA_BUBBLE, 0.0F);
                level.playSound(null, this.blockPosition(), SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, SoundSource.HOSTILE, 2.0F, 0.6F);
            }
            return false;
        }
        // a Terra perto segura metade
        MagusEntity boss = MagusEntity.owner(level, this.owner);
        if (e != MagusElement.EARTH && boss != null) {
            MagusAspectEntity earth = boss.aspect(level, MagusElement.EARTH);
            if (earth != null && earth.distanceTo(this) < 8) {
                earth.hurtShared(level, source, damage * 0.5F);
                damage *= 0.5F;
                level.sendParticles(MagusElement.EARTH.dust(1.5F), this.getX(), this.getY() + 1, this.getZ(), 6, 0.4, 0.4, 0.4, 0.0);
            }
        }
        return super.hurtServer(level, source, damage);
    }

    private void hurtShared(ServerLevel level, DamageSource source, float damage) {
        super.hurtServer(level, source, damage);
    }

    @Override
    public void die(DamageSource source) {
        if (this.level() instanceof ServerLevel level) {
            MagusEntity boss = MagusEntity.owner(level, this.owner);
            if (boss != null) {
                boss.onAspectDeath(level, this.element(), new Vec3(this.getX(), this.floor(), this.getZ()));
            }
            level.sendParticles(this.element().dust(2.0F), this.getX(), this.getY() + this.getBbHeight() / 2, this.getZ(), 50, 0.6, 1.0, 0.6, 0.0);
        }
        super.die(source);
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        if (this.bar != null) {
            this.bar.removeAllPlayers();
        }
        super.remove(reason);
    }

    @Override
    protected boolean shouldDropLoot(ServerLevel level) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distanceSqr) {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {}

    @Override
    public boolean canUsePortal(boolean allowPassengers) {
        return false;
    }

    @Override
    public boolean fireImmune() {
        return this.element() == MagusElement.FIRE;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide() && this.random.nextInt(3) == 0) {
            this.level().addParticle(this.element().particle(), this.getRandomX(0.6), this.getY() + this.random.nextDouble() * this.getBbHeight(),
                    this.getRandomZ(0.6), 0, 0.02, 0);
        }
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return switch (this.element()) {
            case EARTH -> SoundEvents.RAVAGER_AMBIENT;
            case AIR -> SoundEvents.PHANTOM_AMBIENT;
            case WATER -> SoundEvents.GUARDIAN_AMBIENT;
            default -> SoundEvents.BLAZE_AMBIENT;
        };
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return switch (this.element()) {
            case EARTH -> SoundEvents.IRON_GOLEM_HURT;
            case AIR -> SoundEvents.PHANTOM_HURT;
            case WATER -> SoundEvents.GUARDIAN_HURT;
            default -> SoundEvents.BLAZE_HURT;
        };
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ILLUSIONER_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 1.2F;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
    }
}

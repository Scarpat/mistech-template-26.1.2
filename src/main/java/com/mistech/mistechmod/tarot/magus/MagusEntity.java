package com.mistech.mistechmod.tarot.magus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.tarot.ArcanaBossEntity;
import com.mistech.mistechmod.tarot.ArcanePlane;
import com.mistech.mistechmod.tarot.TarotArcana;
import com.mistech.mistechmod.tarot.magus.MagusElement.Mixture;
import com.mistech.mistechmod.tarot.magus.MagusSpellEntity.Kind;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * O Mago (arcano I): potencial e poder. Muda de forma — as primeiras são monstruosas, uma por elemento, e só no
 * fim ele chega ao ápice, uma forma humanoide alta.
 * <ul>
 * <li><b>Fase I — Os Quatro Modos</b> (100% → 75%): um elemento por vez, trocando a cada ~20 s e escolhendo o que
 * contraria o jogador. Terra = colosso de pedra (quase não sente dano; 3 Pedras Rúnicas); Ar = harpia de lâminas
 * (rápida, pisca, chuva de projéteis); Água = leviatã da taça (bolha que cura e acumula Cargas); Fogo = demônio do
 * bastão (dano avassalador, mas leva +50%).</li>
 * <li><b>Fase II — Alquimia</b> (75% → 50%): dois elementos de uma vez (Magma, Vapor, Tempestade, Vida, Areia, Chama Viva).</li>
 * <li><b>Fase III — Os Quatro Aspectos</b> (50% → 25%): explode em 4 monstros menores que lutam em time
 * ({@link MagusAspectEntity}); a Água revive quem cair.</li>
 * <li><b>Fase IV — Quintessência</b> (25% → 0%): a forma humanoide. Surge o plano de cima ("assim em cima, como
 * embaixo"): inversões, reflexo, Raio do ∞, Transmutação do chão, Quatro em Um.</li>
 * </ul>
 */
public class MagusEntity extends ArcanaBossEntity implements MagusCaster {
    // ações (para a animação)
    public static final int IDLE = 0, MORPH = 1, CAST = 2, SLAM = 3, STOMP = 4, THROW = 5, BREATH = 6, BLINK = 7, STUN = 8,
            SPLIT = 9, HIDDEN = 10, REFORM = 11, INVERT = 12, RAY = 13, SWING = 14, ROAR = 15;

    private static final EntityDataAccessor<Integer> DATA_FORM = SynchedEntityData.defineId(MagusEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_MIX = SynchedEntityData.defineId(MagusEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ACTION = SynchedEntityData.defineId(MagusEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_PHASE = SynchedEntityData.defineId(MagusEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_SHIELD = SynchedEntityData.defineId(MagusEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_MIRROR = SynchedEntityData.defineId(MagusEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_CHARGES = SynchedEntityData.defineId(MagusEntity.class, EntityDataSerializers.INT);

    // ----- números (provisórios) -----
    public static final double MAX_HEALTH = 500.0;
    private static final int MODE_LENGTH = 400;
    private static final int MIXTURE_LENGTH = 500;
    private static final int MORPH_LENGTH = 24;
    private static final float WATER_SHIELD = 0.15F;
    /** Vida de cada Aspecto (fração da vida máxima do Mago). */
    private static final float ASPECT_SHARE = 0.125F;

    // ----- estado do servidor -----
    private @Nullable BlockPos arenaCenter;
    private @Nullable Vec3 home;
    private final List<Task> tasks = new ArrayList<>();
    private int actionTicks;
    private int actionLength;
    private int modeTicks;
    private int attackCooldown = 40;
    private @Nullable MagusElement pendingForm;
    private @Nullable Mixture mixture;
    private @Nullable Mixture pendingMixture;
    private int stonesBroken;
    private final List<UUID> stones = new ArrayList<>();
    private int rangedHits;
    private float healthAtModeStart;
    private float shieldHp;
    private int wetTicks;
    private int chargeTimer;
    private int boostCharges;
    // fase III
    private final Map<MagusElement, UUID> aspects = new EnumMap<>(MagusElement.class);
    private final Map<MagusElement, Integer> reviving = new EnumMap<>(MagusElement.class);
    private final Map<MagusElement, Vec3> revivePos = new EnumMap<>(MagusElement.class);
    private int aspectsSeen;
    // fase IV
    private boolean upper;
    private int invertTimer = 520;
    private int transmuteTimer = 260;
    private int transmuteEnd = -1;
    private int strikeTimer = 120;
    private int rayCooldown = 300;
    private int fourCooldown = 180;
    private int swingCooldown;
    private int boltCooldown = 60;

    /** Cliente: tick em que a ação atual começou (para a animação). */
    private int clientActionStart;
    private int clientFormStart = -100;
    private int clientPrevForm = -1;

    private record Task(int at, Runnable run) {}

    public MagusEntity(EntityType<? extends MagusEntity> type, Level level) {
        super(type, level);
        this.xpReward = 200;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.ARMOR, 10.0)
                .add(Attributes.ARMOR_TOUGHNESS, 4.0)
                .add(Attributes.ATTACK_DAMAGE, MagusSpells.SLAM)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 64.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.STEP_HEIGHT, 1.5);
    }

    @Override
    public TarotArcana arcana() {
        return TarotArcana.THE_MAGICIAN;
    }

    public void setArena(BlockPos center) {
        this.arenaCenter = center;
    }

    @Override
    public @Nullable BlockPos arenaCenter() {
        return this.arenaCenter;
    }

    @Override
    public Mob self() {
        return this;
    }

    @Override
    public float power() {
        return 1.0F + this.boostCharges * 0.15F;
    }

    @Override
    public void schedule(int delay, Runnable task) {
        this.tasks.add(new Task(this.tickCount + Math.max(0, delay), task));
    }

    // ------------------------------------------------------------------ dados sincronizados

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_FORM, MagusElement.EARTH.ordinal());
        builder.define(DATA_MIX, -1);
        builder.define(DATA_ACTION, IDLE);
        builder.define(DATA_PHASE, 1);
        builder.define(DATA_SHIELD, 0.0F);
        builder.define(DATA_MIRROR, 0.0F);
        builder.define(DATA_CHARGES, 0);
    }

    public MagusElement form() {
        return MagusElement.byId(this.entityData.get(DATA_FORM));
    }

    public @Nullable MagusElement mixElement() {
        return MagusElement.byIdOrNull(this.entityData.get(DATA_MIX));
    }

    public int action() {
        return this.entityData.get(DATA_ACTION);
    }

    public int phase() {
        return this.entityData.get(DATA_PHASE);
    }

    public float shield() {
        return this.entityData.get(DATA_SHIELD);
    }

    /** Altura do "espelho" da Fase IV (0 = sem reflexo). */
    public float mirrorY() {
        return this.entityData.get(DATA_MIRROR);
    }

    public int charges() {
        return this.entityData.get(DATA_CHARGES);
    }

    public boolean isHiddenPhase() {
        return this.action() == HIDDEN;
    }

    public float actionTime(float partialTick) {
        return this.tickCount - this.clientActionStart + partialTick;
    }

    /** Ticks desde a última troca de forma (cliente), para a animação de metamorfose. */
    public float formTime(float partialTick) {
        return this.tickCount - this.clientFormStart + partialTick;
    }

    public int previousForm() {
        return this.clientPrevForm;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (DATA_ACTION.equals(accessor)) {
            this.clientActionStart = this.tickCount;
        }
        if (DATA_FORM.equals(accessor)) {
            this.refreshDimensions();
        }
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        return dimensionsFor(this.form(), 1.0F);
    }

    static EntityDimensions dimensionsFor(MagusElement form, float scale) {
        return switch (form) {
            case EARTH -> EntityDimensions.scalable(2.4F * scale, 4.0F * scale).withEyeHeight(3.1F * scale);
            case AIR -> EntityDimensions.scalable(1.4F * scale, 3.4F * scale).withEyeHeight(3.0F * scale);
            case WATER -> EntityDimensions.scalable(2.6F * scale, 2.8F * scale).withEyeHeight(1.9F * scale);
            case FIRE -> EntityDimensions.scalable(1.6F * scale, 4.0F * scale).withEyeHeight(3.4F * scale);
            case QUINTESSENCE -> EntityDimensions.scalable(1.8F * scale, 5.9F * scale).withEyeHeight(5.0F * scale);
        };
    }

    private void setForm(MagusElement form, @Nullable MagusElement mix) {
        this.entityData.set(DATA_FORM, form.ordinal());
        this.entityData.set(DATA_MIX, mix == null ? -1 : mix.ordinal());
        this.refreshDimensions();
        boolean flies = form == MagusElement.AIR || form == MagusElement.WATER || form == MagusElement.QUINTESSENCE;
        this.setNoGravity(flies);
    }

    private void setAction(int action, int length) {
        this.entityData.set(DATA_ACTION, action);
        this.actionTicks = 0;
        this.actionLength = length;
    }

    private void setPhase(int phase) {
        this.entityData.set(DATA_PHASE, phase);
    }

    // ------------------------------------------------------------------ IA

    @Override
    public void onFightTick(ArcanePlane.Fight fight) {
        if (this.arenaCenter == null) {
            this.setArena(fight.center());
        }
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (this.home == null) {
            this.home = this.arenaCenter != null ? Vec3.atBottomCenterOf(this.arenaCenter.above()) : this.position();
            this.healthAtModeStart = this.getHealth();
            this.enterElement(level, MagusElement.EARTH, true);
        }
        this.runTasks();
        this.actionTicks++;
        this.modeTicks++;
        if (this.attackCooldown > 0) this.attackCooldown--;
        if (this.swingCooldown > 0) this.swingCooldown--;
        if (this.wetTicks > 0) this.wetTicks--;

        Player target = this.findTarget(level);
        if (target != null && this.action() != HIDDEN) {
            this.lookAt(target);
        }
        int phase = this.phase();
        // transições de fase
        float ratio = this.getHealth() / this.getMaxHealth();
        if (phase == 1 && ratio <= 0.75F && this.action() != MORPH) {
            this.startAlchemy(level);
        } else if (phase == 2 && ratio <= 0.5F && this.action() != SPLIT && this.action() != MORPH) {
            this.startSplit(level);
        }
        switch (this.phase()) {
            case 1, 2 -> this.tickModes(level, target);
            case 3 -> this.tickAspects(level);
            default -> this.tickQuintessence(level, target);
        }
        this.ambient(level);
    }

    private void runTasks() {
        if (this.tasks.isEmpty()) {
            return;
        }
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
            if (this.isAlive()) {
                t.run.run();
            }
        }
    }

    private @Nullable Player findTarget(ServerLevel level) {
        return MagusSpells.players(level, this).stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
    }

    private void lookAt(Player target) {
        double dx = target.getX() - this.getX();
        double dz = target.getZ() - this.getZ();
        float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
        float cur = this.getYRot();
        float next = this.form() == MagusElement.EARTH ? Mth.approachDegrees(cur, yaw, 6.0F) : yaw;
        this.setYRot(next);
        this.setYHeadRot(next);
        this.setYBodyRot(next);
        double dy = target.getEyeY() - this.getEyeY();
        this.setXRot((float) (-(Mth.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * Mth.RAD_TO_DEG)));
    }

    private Vec3 homePos() {
        return this.home == null ? this.position() : this.home;
    }

    private double floorY() {
        return this.arenaCenter != null ? this.arenaCenter.getY() + 1.0 + (this.upper ? MagusArena.UPPER : 0) : this.homePos().y;
    }

    private void glideTo(Vec3 goal, double factor) {
        Vec3 d = goal.subtract(this.position()).scale(factor);
        if (d.length() > 0.6) {
            d = d.normalize().scale(0.6);
        }
        this.setDeltaMovement(d);
    }

    private void walkTo(Vec3 goal, double speed) {
        Vec3 d = goal.subtract(this.position());
        d = new Vec3(d.x, 0, d.z);
        if (d.length() < 1.0) {
            this.setDeltaMovement(0, this.getDeltaMovement().y, 0);
            return;
        }
        d = d.normalize().scale(speed);
        double vy = this.horizontalCollision && this.onGround() ? 0.55 : this.getDeltaMovement().y;
        this.setDeltaMovement(d.x, vy, d.z);
    }

    // ================================================================= FASES I e II

    private MagusElement element() {
        return this.form();
    }

    private void tickModes(ServerLevel level, @Nullable Player target) {
        int action = this.action();
        if (action == MORPH) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.5, this.isNoGravity() ? 0.5 : 1.0, 0.5));
            if (this.actionTicks == MORPH_LENGTH / 2) {
                if (this.pendingMixture != null) {
                    this.enterMixture(level, this.pendingMixture);
                } else if (this.pendingForm != null) {
                    this.enterElement(level, this.pendingForm, false);
                }
            }
            if (this.actionTicks % 2 == 0) {
                MagusElement e = this.form();
                level.sendParticles(e.dust(2.0F), this.getX(), this.getY() + this.getBbHeight() / 2, this.getZ(), 12,
                        this.getBbWidth() * 0.6, this.getBbHeight() * 0.4, this.getBbWidth() * 0.6, 0.0);
            }
            if (this.actionTicks >= this.actionLength) {
                this.setAction(IDLE, 0);
            }
            return;
        }
        if (action == STUN) {
            this.setDeltaMovement(0, this.getDeltaMovement().y, 0);
            if (this.actionTicks % 6 == 0) {
                level.sendParticles(ParticleTypes.WAX_OFF, this.getX(), this.getY() + this.getBbHeight() + 0.3, this.getZ(), 4, 0.6, 0.1, 0.6, 0.0);
            }
            if (this.actionTicks >= this.actionLength) {
                this.setAction(IDLE, 0);
                this.beginMorph(level, target);
            }
            return;
        }
        // Água: bolha que cura e acumula cargas
        if (this.shieldHp > 0) {
            if (this.tickCount % 20 == 0 && this.getHealth() < this.getMaxHealth()) {
                this.heal(this.getMaxHealth() * 0.01F);
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, this.getX(), this.getY() + this.getBbHeight(), this.getZ(), 3, 0.6, 0.3, 0.6, 0.0);
            }
            if (++this.chargeTimer >= 100) {
                this.chargeTimer = 0;
                if (this.charges() < 3) {
                    this.entityData.set(DATA_CHARGES, this.charges() + 1);
                    level.playSound(null, this.blockPosition(), SoundEvents.CONDUIT_ACTIVATE, SoundSource.HOSTILE, 1.5F, 1.4F);
                }
            }
        }
        if (this.mixture == Mixture.LIFE && this.tickCount % 20 == 0) {
            this.heal(this.getMaxHealth() * 0.003F);
        }
        // fim do modo?
        int length = this.phase() == 1 ? MODE_LENGTH : MIXTURE_LENGTH;
        if (this.modeTicks >= length && (action == IDLE)) {
            this.beginMorph(level, target);
            return;
        }
        // acabou a ação atual?
        if (action != IDLE && this.actionTicks >= this.actionLength) {
            this.setAction(IDLE, 0);
            action = IDLE;
        }
        // movimento
        this.move(level, target, action);
        // ataque
        if (action == IDLE && target != null && this.attackCooldown <= 0) {
            if (this.mixture != null) {
                this.attackMixture(level, target);
            } else {
                this.attackElement(level, target, this.element());
            }
        }
    }

    private void move(ServerLevel level, @Nullable Player target, int action) {
        MagusElement e = this.element();
        if (target == null) {
            if (e == MagusElement.EARTH || e == MagusElement.FIRE) {
                this.walkTo(this.homePos(), 0.08);
            } else {
                this.glideTo(this.homePos().add(0, 2, 0), 0.05);
            }
            return;
        }
        boolean busy = action != IDLE;
        switch (e) {
            case EARTH -> {
                if (busy || this.distanceTo(target) < 4.0) {
                    this.setDeltaMovement(0, this.getDeltaMovement().y, 0);
                } else {
                    this.walkTo(target.position(), this.mixture != null ? 0.12 : 0.08);
                }
                if (!busy && this.tickCount % 8 == 0 && this.getDeltaMovement().horizontalDistanceSqr() > 0.001) {
                    level.playSound(null, this.blockPosition(), SoundEvents.RAVAGER_STEP, SoundSource.HOSTILE, 1.2F, 0.5F);
                    level.sendParticles(ParticleTypes.DUST_PLUME, this.getX(), this.getY() + 0.1, this.getZ(), 4, 1.0, 0.0, 1.0, 0.02);
                }
            }
            case AIR -> {
                // gira em volta do alvo a 9 blocos, no alto
                double ang = Math.atan2(this.getZ() - target.getZ(), this.getX() - target.getX()) + 0.035;
                Vec3 goal = target.position().add(Math.cos(ang) * 9, 0, Math.sin(ang) * 9);
                goal = new Vec3(goal.x, this.floorY() + 3.5 + Math.sin(this.tickCount * 0.1) * 0.8, goal.z);
                this.glideTo(this.clampToArena(goal), busy ? 0.05 : 0.12);
            }
            case WATER -> {
                Vec3 goal = target.position().add(this.position().subtract(target.position()).multiply(1, 0, 1).normalize().scale(7));
                goal = new Vec3(goal.x, this.floorY() + 0.8 + Math.sin(this.tickCount * 0.08) * 0.3, goal.z);
                this.glideTo(this.clampToArena(goal), busy ? 0.02 : 0.05);
            }
            case FIRE -> {
                // fica parado conjurando; só anda se o alvo estiver muito longe
                if (!busy && this.distanceTo(target) > 16) {
                    this.walkTo(target.position(), 0.07);
                } else {
                    this.setDeltaMovement(0, this.getDeltaMovement().y, 0);
                }
            }
            default -> {}
        }
    }

    private Vec3 clampToArena(Vec3 goal) {
        if (this.arenaCenter == null) {
            return goal;
        }
        Vec3 c = Vec3.atBottomCenterOf(this.arenaCenter);
        Vec3 d = new Vec3(goal.x - c.x, 0, goal.z - c.z);
        double max = MagusArena.PAINT_RADIUS - 3;
        if (d.length() > max) {
            d = d.normalize().scale(max);
            return new Vec3(c.x + d.x, goal.y, c.z + d.z);
        }
        return goal;
    }

    /** Ataques de um elemento só (Fase I; também usados pelas misturas). */
    private void attackElement(ServerLevel level, Player target, MagusElement e) {
        double dist = this.distanceTo(target);
        float pace = this.phase() == 2 ? 0.8F : 1.0F;
        Vec3 tp = target.position();
        switch (e) {
            case EARTH -> {
                if (dist < 5.5) {
                    this.setAction(SLAM, 22);
                    this.schedule(12, () -> MagusSpells.slam(level, this, 5.0, MagusSpells.SLAM));
                    this.attackCooldown = (int) (30 * pace);
                } else {
                    int pick = this.random.nextInt(3);
                    if (pick == 0) {
                        this.setAction(SLAM, 26);
                        this.schedule(10, () -> MagusSpells.spikeLine(level, this, target.position(), MagusSpells.SPIKE));
                    } else if (pick == 1 && dist < 14) {
                        this.setAction(STOMP, 30);
                        this.schedule(14, () -> MagusSpells.stomp(level, this, this.phase() == 2 ? 3 : 2, MagusSpells.STOMP));
                    } else {
                        this.setAction(THROW, 28);
                        this.schedule(16, () -> MagusSpells.boulder(level, this, target.position(), MagusSpells.BOULDER));
                    }
                    this.attackCooldown = (int) (60 * pace) + this.random.nextInt(20);
                }
            }
            case AIR -> {
                int pick = this.random.nextInt(5);
                Vec3 from = this.position().add(0, this.getBbHeight() * 0.6, 0);
                if (pick == 0) {
                    this.setAction(CAST, 16);
                    this.schedule(8, () -> MagusSpells.fan(level, this, this.position().add(0, this.getBbHeight() * 0.6, 0),
                            target.position().add(0, 1, 0), Kind.WIND_BOLT, 5, 0.22, 0.75, MagusSpells.BOLT));
                } else if (pick == 1) {
                    this.setAction(CAST, 16);
                    this.schedule(8, () -> MagusSpells.ring(level, this, this.position().add(0, this.getBbHeight() * 0.6, 0),
                            Kind.WIND_BOLT, 12, 0.6, MagusSpells.BOLT, this.random.nextDouble()));
                } else if (pick == 2) {
                    this.setAction(BREATH, 62);
                    MagusSpells.spiral(level, this, Kind.WIND_BOLT, 60, MagusSpells.BOLT * 0.8F);
                } else if (pick == 3) {
                    this.setAction(SLAM, 20);
                    this.schedule(10, () -> MagusSpells.windBlade(level, this, target.position(), Kind.WIND_BLADE, MagusSpells.BLADE));
                } else {
                    this.setAction(BLINK, 10);
                    this.schedule(4, () -> MagusSpells.blink(level, this, target.position(), 6, 10, 3.0));
                }
                this.attackCooldown = (int) (34 * pace) + this.random.nextInt(14);
            }
            case WATER -> {
                int pick = this.random.nextInt(3);
                if (pick == 0) {
                    this.setAction(CAST, 24);
                    this.schedule(12, () -> {
                        for (Player p : MagusSpells.players(level, this)) {
                            MagusSpells.whirlpool(level, this, p.position(), 100, MagusSpells.WHIRL);
                        }
                    });
                } else if (pick == 1 && dist < 16) {
                    this.setAction(ROAR, 30);
                    this.schedule(14, () -> MagusSpells.wave(level, this, MagusSpells.WAVE));
                } else {
                    this.setAction(CAST, 18);
                    this.schedule(9, () -> MagusSpells.fan(level, this, this.position().add(0, this.getBbHeight() * 0.7, 0),
                            target.position().add(0, 1, 0), Kind.ICE_ORB, 3, 0.25, 0.6, MagusSpells.ICE));
                }
                this.attackCooldown = (int) (50 * pace) + this.random.nextInt(15);
            }
            case FIRE -> {
                int pick = this.random.nextInt(4);
                if (pick == 0) {
                    int extra = this.boostCharges;
                    this.setAction(CAST, 30);
                    this.schedule(10, () -> MagusSpells.meteors(level, this,
                            MagusSpells.strikePoints(level, this, 3 + extra + (this.phase() == 2 ? 1 : 0), 7), Kind.METEOR, MagusSpells.METEOR, 30));
                } else if (pick == 1 && dist < 11) {
                    this.setAction(BREATH, 50);
                    this.schedule(8, () -> MagusSpells.breath(level, this, () -> {
                        Player t = this.findTarget(level);
                        return t != null ? t.position().add(0, 1, 0) : this.position().add(this.getLookAngle());
                    }, 40, MagusSpells.BREATH));
                } else if (pick == 2) {
                    this.setAction(THROW, 22);
                    this.schedule(12, () -> MagusSpells.fan(level, this, this.position().add(0, this.getBbHeight() * 0.8, 0),
                            target.position().add(0, 1, 0), Kind.FIREBALL, 1, 0, 0.7, MagusSpells.FIREBALL));
                } else {
                    this.setAction(STOMP, 28);
                    this.schedule(12, () -> MagusSpells.lavaCracks(level, this, MagusSpells.CRACK));
                }
                this.attackCooldown = (int) (40 * pace) + this.random.nextInt(15);
            }
            default -> {}
        }
    }

    private void attackMixture(ServerLevel level, Player target) {
        Mixture m = this.mixture;
        if (m == null) {
            return;
        }
        // 45%: o especial da mistura; senão um ataque de um dos dois elementos
        if (this.random.nextFloat() > 0.45F) {
            this.attackElement(level, target, this.random.nextBoolean() ? m.primary : m.secondary);
            return;
        }
        switch (m) {
            case MAGMA -> {
                this.setAction(STOMP, 30);
                this.schedule(12, () -> MagusSpells.meteors(level, this, MagusSpells.strikePoints(level, this, 8, 7), Kind.ROCK, MagusSpells.ROCK, 26));
            }
            case VAPOR -> {
                this.setAction(CAST, 26);
                this.schedule(12, () -> {
                    List<Player> ps = MagusSpells.players(level, this);
                    for (Player p : ps) {
                        MagusSpells.steam(level, this, p.position(), 3.5, 120, MagusSpells.STEAM);
                    }
                    if (!ps.isEmpty()) {
                        Player p = ps.get(this.random.nextInt(ps.size()));
                        this.schedule(20, () -> MagusSpells.blink(level, this, p.position(), 3, 5, 1.0));
                    }
                });
            }
            case STORM -> {
                this.setAction(CAST, 34);
                this.schedule(6, () -> {
                    for (Vec3 p : MagusSpells.strikePoints(level, this, MagusSpells.players(level, this).size() + 2, 6)) {
                        MagusSpells.lightning(level, this, p, MagusSpells.LIGHTNING);
                    }
                });
            }
            case LIFE -> {
                this.setAction(ROAR, 26);
                this.schedule(8, () -> {
                    for (Player p : MagusSpells.players(level, this)) {
                        MagusSpells.roots(level, this, p.position(), MagusSpells.ROOTS);
                    }
                });
            }
            case SAND -> {
                this.setAction(BREATH, 40);
                MagusSpells.sandstorm(level, this, 140);
                MagusSpells.repeat(this, 3, 12, k -> MagusSpells.fan(level, this, this.position().add(0, this.getBbHeight() * 0.6, 0),
                        target.position().add(0, 1, 0), Kind.SAND_BLADE, 5, 0.2, 0.7, MagusSpells.SAND));
            }
            case LIVING_FLAME -> {
                this.setAction(CAST, 24);
                this.schedule(10, () -> MagusSpells.fireTornadoes(level, this, MagusSpells.TORNADO));
            }
        }
        this.attackCooldown = 50 + this.random.nextInt(20);
    }

    // ----- troca de forma -----

    private void beginMorph(ServerLevel level, @Nullable Player target) {
        if (this.phase() == 1) {
            this.pendingForm = this.chooseElement(target);
            this.pendingMixture = null;
        } else {
            this.pendingMixture = this.chooseMixture(target);
        }
        this.setAction(MORPH, MORPH_LENGTH);
        level.playSound(null, this.blockPosition(), SoundEvents.ILLUSIONER_PREPARE_MIRROR, SoundSource.HOSTILE, 2.0F, 0.6F);
    }

    /** Adaptável: escolhe o elemento que contraria o jogador. */
    private MagusElement chooseElement(@Nullable Player target) {
        double[] w = {1, 1, 1, 1};
        if (target != null) {
            double dist = this.distanceTo(target);
            if (this.rangedHits >= 3) {
                w[0] += 2.0;
                w[1] += 1.0;
            }
            if (dist < 6) {
                w[1] += 2.0;
                w[3] += 1.0;
            }
            if (!this.hasLineOfSight(target)) {
                w[3] += 2.0;
            }
        }
        if (this.healthAtModeStart - this.getHealth() > this.getMaxHealth() * 0.06F) {
            w[2] += 2.5;
        }
        w[this.form().ordinal() < 4 ? this.form().ordinal() : 0] = 0;
        return MagusElement.CLASSIC[pick(w)];
    }

    private Mixture chooseMixture(@Nullable Player target) {
        Mixture[] all = Mixture.values();
        double[] w = new double[all.length];
        for (int i = 0; i < all.length; i++) {
            Mixture m = all[i];
            w[i] = 1.0;
            if (target != null && this.rangedHits >= 3 && m.has(MagusElement.EARTH)) w[i] += 1.5;
            if (target != null && this.distanceTo(target) < 6 && m.has(MagusElement.AIR)) w[i] += 1.5;
            if (this.healthAtModeStart - this.getHealth() > this.getMaxHealth() * 0.06F && m.has(MagusElement.WATER)) w[i] += 1.5;
            if (m == this.mixture) w[i] = 0;
        }
        return all[pick(w)];
    }

    private int pick(double[] w) {
        double sum = 0;
        for (double x : w) sum += x;
        double r = this.random.nextDouble() * sum;
        for (int i = 0; i < w.length; i++) {
            r -= w[i];
            if (r <= 0 && w[i] > 0) {
                return i;
            }
        }
        for (int i = 0; i < w.length; i++) {
            if (w[i] > 0) return i;
        }
        return 0;
    }

    /** Começo de um modo da Fase I. */
    private void enterElement(ServerLevel level, MagusElement e, boolean first) {
        MagusElement old = this.form();
        // as cargas da Água valem para o modo seguinte
        this.boostCharges = old == MagusElement.WATER && !first ? this.charges() : 0;
        if (old != MagusElement.WATER || first) {
            this.entityData.set(DATA_CHARGES, 0);
        }
        this.clearModeStuff();
        this.setForm(e, null);
        this.modeTicks = 0;
        this.rangedHits = 0;
        this.healthAtModeStart = this.getHealth();
        this.attackCooldown = 20;
        if (e == MagusElement.EARTH) {
            this.stonesBroken = 0;
            for (int i = 0; i < 3; i++) {
                MagusRunestoneEntity stone = MagusRunestoneEntity.create(level, this, i);
                level.addFreshEntity(stone);
                this.stones.add(stone.getUUID());
            }
        } else if (e == MagusElement.WATER) {
            this.shieldHp = this.getMaxHealth() * WATER_SHIELD;
            this.chargeTimer = 0;
            this.entityData.set(DATA_SHIELD, 1.0F);
        }
        MagusArena.Field field = this.field();
        if (field != null) {
            field.paint(level, List.of(e), false);
        }
        this.announceOverlay(level, Component.translatable("message.mistech.magus.form", e.displayName()));
        level.playSound(null, this.blockPosition(), this.formSound(e), SoundSource.HOSTILE, 3.0F, 0.7F);
    }

    private void enterMixture(ServerLevel level, Mixture m) {
        this.clearModeStuff();
        this.boostCharges = 0;
        this.entityData.set(DATA_CHARGES, 0);
        this.mixture = m;
        this.setForm(m.primary, m.secondary);
        this.modeTicks = 0;
        this.rangedHits = 0;
        this.healthAtModeStart = this.getHealth();
        this.attackCooldown = 25;
        MagusArena.Field field = this.field();
        if (field != null) {
            field.paint(level, List.of(m.primary, m.secondary), false);
        }
        this.announceOverlay(level, Component.translatable("message.mistech.magus.mixture", m.displayName()));
        level.playSound(null, this.blockPosition(), this.formSound(m.primary), SoundSource.HOSTILE, 3.0F, 0.6F);
        level.playSound(null, this.blockPosition(), this.formSound(m.secondary), SoundSource.HOSTILE, 3.0F, 0.9F);
    }

    private void clearModeStuff() {
        if (this.level() instanceof ServerLevel level) {
            for (UUID id : this.stones) {
                Entity s = level.getEntity(id);
                if (s != null) {
                    s.discard();
                }
            }
        }
        this.stones.clear();
        this.shieldHp = 0;
        this.entityData.set(DATA_SHIELD, 0.0F);
        this.wetTicks = 0;
    }

    private SoundEvent formSound(MagusElement e) {
        return switch (e) {
            case EARTH -> SoundEvents.RAVAGER_ROAR;
            case AIR -> SoundEvents.PHANTOM_AMBIENT;
            case WATER -> SoundEvents.ELDER_GUARDIAN_AMBIENT;
            case FIRE -> SoundEvents.BLAZE_AMBIENT;
            case QUINTESSENCE -> SoundEvents.BEACON_ACTIVATE;
        };
    }

    /** Uma Pedra Rúnica quebrou: tira 25% da proteção; as três → atordoado e sai do modo. */
    void onRunestoneBroken() {
        this.stonesBroken++;
        if (this.stonesBroken >= 3 && this.level() instanceof ServerLevel level) {
            this.setAction(STUN, 60);
            level.playSound(null, this.blockPosition(), SoundEvents.IRON_GOLEM_DAMAGE, SoundSource.HOSTILE, 3.0F, 0.4F);
            this.announceOverlay(level, Component.translatable("message.mistech.magus.stunned"));
        }
    }

    private void popShield(ServerLevel level) {
        this.shieldHp = 0;
        this.entityData.set(DATA_SHIELD, 0.0F);
        this.wetTicks = 80;
        this.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 2));
        level.playSound(null, this.blockPosition(), SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, SoundSource.HOSTILE, 3.0F, 0.5F);
        level.playSound(null, this.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 2.0F, 0.6F);
        level.sendParticles(ParticleTypes.SPLASH, this.getX(), this.getY() + this.getBbHeight() / 2, this.getZ(), 80, 1.5, 1.5, 1.5, 0.2);
        this.announceOverlay(level, Component.translatable("message.mistech.magus.bubble_pop"));
    }

    // ----- Fase II -----

    private void startAlchemy(ServerLevel level) {
        this.setPhase(2);
        this.announceChat(level, "message.mistech.magus.phase2");
        level.playSound(null, this.blockPosition(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, 3.0F, 0.6F);
        this.pendingMixture = this.chooseMixture(this.findTarget(level));
        this.setAction(MORPH, MORPH_LENGTH);
        this.tasks.clear();
    }

    // ================================================================= FASE III

    private void startSplit(ServerLevel level) {
        this.tasks.clear();
        this.clearModeStuff();
        this.mixture = null;
        // já entra na Fase III: é o tickAspects que conduz a divisão (antes ficava no tickModes, que encerrava
        // a ação SPLIT sem criar os Aspectos e começava a divisão de novo, girando para sempre)
        this.setPhase(3);
        this.setAction(SPLIT, 40);
        this.setDeltaMovement(Vec3.ZERO);
        this.announceChat(level, "message.mistech.magus.phase3");
        level.playSound(null, this.blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 3.0F, 0.8F);
        MagusArena.Field field = this.field();
        if (field != null) {
            field.unpaintAll();
        }
    }

    private void tickAspects(ServerLevel level) {
        if (this.action() == SPLIT) {
            this.setDeltaMovement(Vec3.ZERO);
            level.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + this.getBbHeight() / 2, this.getZ(), 8,
                    this.getBbWidth() / 2, this.getBbHeight() / 2, this.getBbWidth() / 2, 0.15);
            if (this.actionTicks >= this.actionLength) {
                this.spawnAspects(level);
            }
            return;
        }
        if (this.action() == REFORM) {
            this.tickReform(level);
            return;
        }
        // escondido no alto, como uma luz
        Vec3 hideAt = this.homePos().add(0, 11, 0);
        this.setDeltaMovement(hideAt.subtract(this.position()).scale(0.2));
        if (this.tickCount % 3 == 0) {
            level.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + 1, this.getZ(), 3, 0.4, 0.4, 0.4, 0.02);
            for (MagusElement e : MagusElement.CLASSIC) {
                if (this.aspects.containsKey(e) && level.getEntity(this.aspects.get(e)) instanceof MagusAspectEntity a && a.isAlive()) {
                    // fio de luz até cada Aspecto
                    Vec3 from = this.position().add(0, 1, 0);
                    Vec3 to = a.position().add(0, a.getBbHeight() * 0.6, 0);
                    Vec3 step = to.subtract(from).scale(1 / 12.0);
                    int k = (this.tickCount / 3) % 12;
                    Vec3 q = from.add(step.scale(k));
                    level.sendParticles(e.dust(1.0F), q.x, q.y, q.z, 1, 0, 0, 0, 0);
                }
            }
        }
        // vida = 25% + o que sobra dos Aspectos
        float sum = 0;
        boolean anyAlive = false;
        for (MagusElement e : MagusElement.CLASSIC) {
            UUID id = this.aspects.get(e);
            if (id != null && level.getEntity(id) instanceof MagusAspectEntity a && a.isAlive()) {
                sum += a.getHealth();
                anyAlive = true;
            } else if (id != null && !this.reviving.containsKey(e)) {
                this.aspects.remove(e);
            }
        }
        // revive da Água
        boolean waterAlive = this.aspectAlive(level, MagusElement.WATER);
        Iterator<Map.Entry<MagusElement, Integer>> it = this.reviving.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<MagusElement, Integer> r = it.next();
            Vec3 at = this.revivePos.get(r.getKey());
            if (!waterAlive || at == null) {
                it.remove();
                continue;
            }
            int left = r.getValue() - 1;
            r.setValue(left);
            if (left % 10 == 0) {
                MagusSpells.warnCircle(level, at, 1.5, r.getKey().color);
                level.sendParticles(ParticleTypes.SPLASH, at.x, at.y + 1, at.z, 6, 0.5, 0.5, 0.5, 0.0);
            }
            if (left <= 0) {
                it.remove();
                MagusAspectEntity a = this.spawnAspect(level, r.getKey(), at, 0.5F);
                if (a != null) {
                    anyAlive = true;
                    level.playSound(null, BlockPos.containing(at), SoundEvents.CONDUIT_ACTIVATE, SoundSource.HOSTILE, 2.5F, 0.8F);
                    this.announceOverlay(level, Component.translatable("message.mistech.magus.revived", r.getKey().displayName()));
                }
            }
        }
        if (!this.reviving.isEmpty()) {
            anyAlive = true;
        }
        // cada Aspecto tem 1/8 da vida do Mago; a barra vai de 50% a 25% conforme eles caem
        float quarter = this.getMaxHealth() * 0.25F;
        this.setHealth(Math.max(1.0F, Math.min(this.getMaxHealth() * 0.5F, quarter + sum * 0.5F)));
        if (!anyAlive && this.aspectsSeen > 0) {
            this.startReform(level);
        }
    }

    private boolean aspectAlive(ServerLevel level, MagusElement e) {
        UUID id = this.aspects.get(e);
        return id != null && level.getEntity(id) instanceof MagusAspectEntity a && a.isAlive();
    }

    private void spawnAspects(ServerLevel level) {
        this.setPhase(3);
        this.setHealth(this.getMaxHealth() * 0.5F);
        Vec3 c = this.homePos();
        level.sendParticles(net.minecraft.core.particles.ColorParticleOption.create(ParticleTypes.FLASH, 0xFFFFFFFF), c.x, c.y + 2, c.z, 1, 0, 0, 0, 0);
        level.playSound(null, BlockPos.containing(c), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 3.0F, 1.4F);
        for (int i = 0; i < 4; i++) {
            MagusElement e = MagusElement.CLASSIC[i];
            double a = Math.toRadians(225 + i * 90);
            Vec3 p = new Vec3(c.x + Math.cos(a) * 9, c.y, c.z + Math.sin(a) * 9);
            this.spawnAspect(level, e, p, 1.0F);
        }
        if (this.aspectsSeen == 0) {
            this.startReform(level);
            return;
        }
        this.setAction(HIDDEN, 0);
        this.setInvisible(true);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    private @Nullable MagusAspectEntity spawnAspect(ServerLevel level, MagusElement e, Vec3 at, float healthFraction) {
        MagusAspectEntity a = MagusAspectEntity.create(level, this, e, this.getMaxHealth() * ASPECT_SHARE, healthFraction, at);
        if (a == null || !level.addFreshEntity(a)) {
            return null;
        }
        this.aspects.put(e, a.getUUID());
        this.aspectsSeen++;
        level.sendParticles(e.dust(2.0F), at.x, at.y + 1, at.z, 40, 0.6, 1.0, 0.6, 0.0);
        return a;
    }

    /** Chamado pelo Aspecto quando cai: com a Água viva, ele volta em 15 s (marcado no chão). */
    void onAspectDeath(ServerLevel level, MagusElement e, Vec3 at) {
        if (e != MagusElement.WATER && this.aspectAlive(level, MagusElement.WATER)) {
            this.reviving.put(e, 300);
            this.revivePos.put(e, at);
            this.announceOverlay(level, Component.translatable("message.mistech.magus.reviving", e.displayName()));
        }
    }

    /** Para os Aspectos: o Aspecto vivo do elemento (ou null). */
    @Nullable MagusAspectEntity aspect(ServerLevel level, MagusElement e) {
        UUID id = this.aspects.get(e);
        return id != null && level.getEntity(id) instanceof MagusAspectEntity a && a.isAlive() ? a : null;
    }

    List<MagusAspectEntity> aliveAspects(ServerLevel level) {
        List<MagusAspectEntity> list = new ArrayList<>();
        for (MagusElement e : MagusElement.CLASSIC) {
            MagusAspectEntity a = this.aspect(level, e);
            if (a != null) {
                list.add(a);
            }
        }
        return list;
    }

    private void startReform(ServerLevel level) {
        this.setAction(REFORM, 80);
        this.announceChat(level, "message.mistech.magus.phase4");
        level.playSound(null, this.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 3.0F, 0.5F);
    }

    private void tickReform(ServerLevel level) {
        Vec3 c = this.homePos();
        // as quatro luzes se juntam no centro
        for (int i = 0; i < 4; i++) {
            MagusElement e = MagusElement.CLASSIC[i];
            double a = Math.toRadians(225 + i * 90) + this.actionTicks * 0.05;
            double r = 12.0 * (1.0 - this.actionTicks / 80.0);
            level.sendParticles(e.dust(2.0F), c.x + Math.cos(a) * r, c.y + 2 + this.actionTicks * 0.04, c.z + Math.sin(a) * r, 4, 0.2, 0.2, 0.2, 0.0);
        }
        this.setDeltaMovement(c.add(0, 4, 0).subtract(this.position()).scale(0.2));
        if (this.actionTicks == 40) {
            MagusArena.Field field = this.field();
            if (field != null) {
                field.buildUpper(level);
            }
        }
        if (this.actionTicks >= this.actionLength) {
            this.setPhase(4);
            this.setForm(MagusElement.QUINTESSENCE, null);
            this.setInvisible(false);
            this.noPhysics = false;
            this.setHealth(this.getMaxHealth() * 0.25F);
            this.teleportTo(c.x, this.floorY() + 0.5, c.z);
            this.setAction(ROAR, 50);
            this.updateMirror();
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y + 3, c.z, 1, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, c.x, c.y + 3, c.z, 120, 1.5, 3, 1.5, 0.5);
            level.playSound(null, this.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 4.0F, 0.5F);
            level.playSound(null, this.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.0F, 1.6F);
        }
    }

    // ================================================================= FASE IV

    private void updateMirror() {
        if (this.arenaCenter == null) {
            this.entityData.set(DATA_MIRROR, 0.0F);
            return;
        }
        float m = this.arenaCenter.getY() + 1.0F + (MagusArena.UPPER - 1) / 2.0F;
        this.entityData.set(DATA_MIRROR, m);
    }

    private void tickQuintessence(ServerLevel level, @Nullable Player target) {
        MagusArena.Field field = this.field();
        int action = this.action();
        if (action != IDLE && this.actionTicks >= this.actionLength) {
            if (action == INVERT) {
                this.invert(level);
            }
            this.setAction(IDLE, 0);
            action = IDLE;
        }
        // flutua sobre o plano atual, perto (mas não colado) do alvo
        double y = this.floorY() + 0.6 + Math.sin(this.tickCount * 0.07) * 0.25;
        if (target != null && action != RAY && action != INVERT) {
            Vec3 away = this.position().subtract(target.position()).multiply(1, 0, 1);
            away = away.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : away.normalize();
            double want = action == SWING ? 2.5 : 8.0;
            Vec3 goal = target.position().add(away.scale(want));
            goal = this.clampToArena(new Vec3(goal.x, y, goal.z));
            this.glideTo(goal, 0.06);
        } else {
            this.glideTo(new Vec3(this.getX(), y, this.getZ()), 0.2);
        }
        if (field != null && field.upperBuilt) {
            if (--this.invertTimer <= 0 && action == IDLE) {
                this.invertTimer = 560 + this.random.nextInt(120);
                this.setAction(INVERT, 60);
                for (Player p : MagusSpells.players(level, this)) {
                    p.sendOverlayMessage(Component.translatable("message.mistech.magus.invert").withStyle(ChatFormatting.GOLD));
                }
                level.playSound(null, this.blockPosition(), SoundEvents.PORTAL_TRIGGER, SoundSource.HOSTILE, 1.5F, 1.4F);
            }
            if (action == INVERT) {
                for (Player p : MagusSpells.players(level, this)) {
                    level.sendParticles(ParticleTypes.REVERSE_PORTAL, p.getX(), p.getY() + 0.1, p.getZ(), 6, 0.4, 0.0, 0.4, 0.05);
                    MagusSpells.warnCircle(level, p.position(), 1.0, 0xC08AFF);
                }
                return;
            }
            // Transmutação: o chão do plano vira um elemento por um tempo
            if (--this.transmuteTimer <= 0) {
                this.transmuteTimer = 600;
                this.transmuteEnd = this.tickCount + 200;
                MagusElement e = MagusElement.CLASSIC[this.random.nextInt(4)];
                field.paint(level, List.of(e), this.upper);
                this.announceOverlay(level, Component.translatable("message.mistech.magus.transmute", e.displayName()));
            }
            if (this.transmuteEnd > 0 && this.tickCount >= this.transmuteEnd) {
                this.transmuteEnd = -1;
                field.unpaintAll();
            }
            // "como embaixo": golpes que vêm do outro plano
            if (--this.strikeTimer <= 0) {
                this.strikeTimer = 90 + this.random.nextInt(50);
                double floor = this.floorY() - 1.0;
                double other = this.upper ? this.arenaCenter.getY() + 1.0 : this.arenaCenter.getY() + MagusArena.UPPER;
                for (Vec3 p : MagusSpells.strikePoints(level, this, MagusSpells.players(level, this).size() + 2, 5)) {
                    MagusSpells.mirrorStrike(level, this, p, floor, other, MagusSpells.STRIKE);
                }
            }
        }
        if (action != IDLE || target == null) {
            return;
        }
        if (this.boltCooldown > 0) this.boltCooldown--;
        if (this.rayCooldown > 0) this.rayCooldown--;
        if (this.fourCooldown > 0) this.fourCooldown--;
        double dist = this.distanceTo(target);
        if (dist < 5.0 && this.swingCooldown <= 0) {
            this.setAction(SWING, 18);
            this.swingCooldown = 40;
            this.schedule(9, () -> MagusSpells.slam(level, this, 5.5, MagusSpells.SWING));
            return;
        }
        if (this.rayCooldown <= 0 && this.arenaCenter != null) {
            this.rayCooldown = 420;
            this.setAction(RAY, 130);
            Vec3 c = Vec3.atBottomCenterOf(this.arenaCenter);
            double floor = this.floorY() - 1.0;
            level.playSound(null, this.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 3.0F, 1.5F);
            this.schedule(10, () -> MagusSpells.infinityRay(level, this, c, floor, 120, MagusSpells.RAY));
            return;
        }
        if (this.fourCooldown <= 0) {
            this.fourCooldown = 380;
            this.setAction(CAST, 70);
            this.announceOverlay(level, Component.translatable("message.mistech.magus.four"));
            this.schedule(8, () -> MagusSpells.spikeLine(level, this, target.position(), MagusSpells.SPIKE));
            this.schedule(24, () -> MagusSpells.fan(level, this, this.position().add(0, 4, 0), target.position().add(0, 1, 0),
                    Kind.WIND_BOLT, 7, 0.18, 0.8, MagusSpells.BOLT));
            this.schedule(40, () -> MagusSpells.fan(level, this, this.position().add(0, 4, 0), target.position().add(0, 1, 0),
                    Kind.ICE_ORB, 3, 0.3, 0.6, MagusSpells.ICE));
            this.schedule(52, () -> MagusSpells.meteors(level, this, MagusSpells.strikePoints(level, this, 4, 6), Kind.METEOR, MagusSpells.METEOR, 30));
            return;
        }
        if (this.boltCooldown <= 0) {
            this.boltCooldown = 50 + this.random.nextInt(30);
            this.setAction(CAST, 20);
            this.schedule(10, () -> {
                for (Player p : MagusSpells.players(level, this)) {
                    MagusSpells.fan(level, this, this.position().add(0, this.getBbHeight() * 0.85, 0), p.position().add(0, 1, 0),
                            Kind.ARCANE_BOLT, 3, 0.4, 0.55, MagusSpells.ARCANE);
                }
            });
        }
    }

    /** Todos (jogadores e o Mago) passam para o outro plano, na mesma posição. */
    private void invert(ServerLevel level) {
        if (this.arenaCenter == null) {
            return;
        }
        MagusArena.Field field = this.field();
        if (field != null) {
            field.unpaintAll();
            this.transmuteEnd = -1;
        }
        this.upper = !this.upper;
        if (field != null) {
            field.playersUpper = this.upper;
        }
        double y = this.floorY();
        for (Player p : MagusSpells.players(level, this)) {
            Vec3 at = this.clampToArena(new Vec3(p.getX(), y + 0.2, p.getZ()));
            double cx = this.arenaCenter.getX() + 0.5, cz = this.arenaCenter.getZ() + 0.5;
            Vec3 d = new Vec3(at.x - cx, 0, at.z - cz);
            if (this.upper && d.length() > MagusArena.UPPER_RADIUS - 3) {
                d = d.normalize().scale(MagusArena.UPPER_RADIUS - 3);
                at = new Vec3(cx + d.x, at.y, cz + d.z);
            }
            p.teleportTo(at.x, at.y, at.z);
            p.setDeltaMovement(Vec3.ZERO);
            p.hurtMarked = true;
            p.resetFallDistance();
            p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 30, 0, false, false));
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, at.x, at.y + 1, at.z, 30, 0.3, 1.0, 0.3, 0.1);
        }
        Vec3 me = this.clampToArena(new Vec3(this.getX(), y + 0.6, this.getZ()));
        this.teleportTo(me.x, me.y, me.z);
        level.playSound(null, this.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 3.0F, 0.5F);
        this.announceOverlay(level, Component.translatable(this.upper ? "message.mistech.magus.above" : "message.mistech.magus.below"));
    }

    // ================================================================= dano

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurtServer(level, source, damage);
        }
        if (source.getEntity() == null) {
            return false; // o ambiente (magma, água, queda...) não fere o Mago
        }
        if (source.is(DamageTypeTags.IS_FALL) || source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.FLY_INTO_WALL)
                || source.is(DamageTypeTags.IS_FIRE) && this.form() == MagusElement.FIRE || source.is(DamageTypes.LIGHTNING_BOLT)
                || source.getEntity() instanceof MagusAspectEntity || source.getEntity() == this) {
            return false;
        }
        int action = this.action();
        int phase = this.phase();
        if (phase == 3 || action == SPLIT || action == HIDDEN || action == REFORM || action == INVERT) {
            return false;
        }
        boolean projectile = source.is(DamageTypeTags.IS_PROJECTILE);
        if (projectile) {
            this.rangedHits++;
        }
        float mult = 1.0F;
        if (action == MORPH) {
            mult *= 0.5F;
        }
        if (this.mixture != null) {
            mult *= (elementMult(this.mixture.primary) + elementMult(this.mixture.secondary)) / 2.0F;
        } else if (phase < 4) {
            MagusElement e = this.form();
            if (e == MagusElement.EARTH) {
                if (action == STUN) {
                    mult *= 1.25F;
                } else {
                    if (projectile && this.stones.size() - this.stonesBroken > 0) {
                        level.playSound(null, this.blockPosition(), SoundEvents.SHIELD_BLOCK.value(), SoundSource.HOSTILE, 1.5F, 0.6F);
                        return false;
                    }
                    mult *= 0.15F + 0.25F * Math.min(3, this.stonesBroken);
                }
            } else if (e == MagusElement.WATER) {
                if (this.shieldHp > 0) {
                    this.shieldHp -= damage;
                    level.sendParticles(ParticleTypes.SPLASH, this.getX(), this.getY() + this.getBbHeight() / 2, this.getZ(), 12, 1.2, 1.2, 1.2, 0.1);
                    level.playSound(null, this.blockPosition(), SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, SoundSource.HOSTILE, 1.5F, 1.0F);
                    if (this.shieldHp <= 0) {
                        this.popShield(level);
                    } else {
                        this.entityData.set(DATA_SHIELD, this.shieldHp / (this.getMaxHealth() * WATER_SHIELD));
                    }
                    return false;
                }
                if (this.wetTicks > 0) {
                    mult *= 1.25F;
                }
            } else {
                mult *= elementMult(e);
            }
        }
        damage *= mult;
        // não morre antes da última fase
        if (phase < 4) {
            damage = Math.min(damage, Math.max(0.0F, this.getHealth() - 1.0F));
            if (damage <= 0) {
                return false;
            }
        }
        return super.hurtServer(level, source, damage);
    }

    private static float elementMult(MagusElement e) {
        return switch (e) {
            case EARTH -> 0.5F;
            case FIRE -> 1.5F;
            default -> 1.0F;
        };
    }

    // ================================================================= visual e afins

    private void ambient(ServerLevel level) {
        MagusElement mix = this.mixElement();
        if (mix != null && this.tickCount % 4 == 0) {
            level.sendParticles(mix.particle(), this.getRandomX(0.8), this.getRandomY(), this.getRandomZ(0.8), 2, 0.1, 0.1, 0.1, 0.01);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide()) {
            int f = this.entityData.get(DATA_FORM);
            if (f != this.clientPrevFormNow) {
                this.clientPrevForm = this.clientPrevFormNow;
                this.clientPrevFormNow = f;
                this.clientFormStart = this.tickCount;
            }
            if (this.isInvisible()) {
                return;
            }
            MagusElement e = this.form();
            if (this.random.nextInt(3) == 0) {
                this.level().addParticle(e.particle(), this.getRandomX(0.7), this.getY() + this.random.nextDouble() * this.getBbHeight(),
                        this.getRandomZ(0.7), 0, 0.02, 0);
            }
            if (e == MagusElement.QUINTESSENCE && this.random.nextInt(2) == 0) {
                MagusElement c = MagusElement.CLASSIC[this.random.nextInt(4)];
                this.level().addParticle(c.dust(1.0F), this.getRandomX(1.5), this.getY() + this.random.nextDouble() * this.getBbHeight(),
                        this.getRandomZ(1.5), 0, 0.02, 0);
            }
        }
    }

    private int clientPrevFormNow = -1;

    private void announceOverlay(ServerLevel level, Component message) {
        for (Player p : MagusSpells.players(level, this)) {
            p.sendOverlayMessage(message);
        }
    }

    private void announceChat(ServerLevel level, String key) {
        for (Player p : MagusSpells.players(level, this)) {
            p.sendSystemMessage(Component.translatable(key).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        if (this.level() instanceof ServerLevel level) {
            for (UUID id : this.stones) {
                Entity s = level.getEntity(id);
                if (s != null) s.discard();
            }
            for (UUID id : this.aspects.values()) {
                Entity s = level.getEntity(id);
                if (s != null) s.discard();
            }
        }
        super.remove(reason);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return !this.isInvisible() && super.isPickable();
    }

    @Override
    protected void doPush(Entity entity) {}

    @Override
    public boolean canBeCollidedWith(@Nullable Entity entity) {
        return false;
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return switch (this.form()) {
            case EARTH -> SoundEvents.RAVAGER_AMBIENT;
            case AIR -> SoundEvents.PHANTOM_AMBIENT;
            case WATER -> SoundEvents.GUARDIAN_AMBIENT;
            case FIRE -> SoundEvents.BLAZE_AMBIENT;
            case QUINTESSENCE -> SoundEvents.ILLUSIONER_AMBIENT;
        };
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return switch (this.form()) {
            case EARTH -> SoundEvents.IRON_GOLEM_HURT;
            case AIR -> SoundEvents.PHANTOM_HURT;
            case WATER -> SoundEvents.ELDER_GUARDIAN_HURT;
            case FIRE -> SoundEvents.BLAZE_HURT;
            case QUINTESSENCE -> SoundEvents.ILLUSIONER_HURT;
        };
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ILLUSIONER_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return this.form() == MagusElement.QUINTESSENCE ? 0.6F : 0.75F;
    }

    @Override
    public boolean fireImmune() {
        return this.form() == MagusElement.FIRE || this.form() == MagusElement.QUINTESSENCE;
    }

    /** Para os Aspectos acharem o dono. */
    static @Nullable MagusEntity owner(ServerLevel level, @Nullable UUID id) {
        return id != null && level.getEntity(id) instanceof MagusEntity m && m.isAlive() ? m : null;
    }

    /** Jogadores para mensagens (os da luta, ou em volta). */
    List<ServerPlayer> nearbyPlayers(ServerLevel level) {
        List<ServerPlayer> list = new ArrayList<>();
        for (Player p : MagusSpells.players(level, this)) {
            if (p instanceof ServerPlayer sp) {
                list.add(sp);
            }
        }
        return list;
    }
}

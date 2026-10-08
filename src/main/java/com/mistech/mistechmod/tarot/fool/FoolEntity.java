package com.mistech.mistechmod.tarot.fool;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.tarot.ArcanaBossEntity;
import com.mistech.mistechmod.tarot.ArcanePlane;
import com.mistech.mistechmod.tarot.TarotArcana;

import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * O Louco (arcano 0): gênio do vento com chapéu de bobo e cajado. Não anda: flutua sobre as ilhas da arena e
 * salta de uma para outra em arco, jogando tornados e "brincando" com o desafiante.
 * <p>
 * Ações (sincronizadas para a animação):
 * <ul>
 * <li>{@link #HOVER}: flutua no meio da ilha, virado para o alvo; se chegarem perto, dá um golpe de cajado.</li>
 * <li>{@link #HOP}: salta em arco para outra ilha (de preferência a do alvo); ao pousar solta uma rajada em volta.</li>
 * <li>{@link #CAST}: ergue o cajado e lança tornados (1; 2 abaixo de 75% de vida; leque de 3 na fase 2).</li>
 * <li>{@link #SWIPE}: golpe de cajado que arremessa para longe e para cima.</li>
 * <li>{@link #DIZZY}: depois de alguns pousos fica tonto, rindo, no chão da ilha — leva +50% de dano.</li>
 * <li>{@link #GALE} (só fase 2, abaixo de 50%): "Ciranda": gira puxando todos e depois explode jogando-os para o alto.</li>
 * </ul>
 * Pensado como o chefe mais fraco: ~180 de vida, golpes moderados; o perigo maior é cair das ilhas
 * (a arena devolve o jogador com um pouco de dano).
 */
public class FoolEntity extends ArcanaBossEntity {
    public static final int HOVER = 0, HOP = 1, CAST = 2, SWIPE = 3, DIZZY = 4, GALE = 5;

    private static final EntityDataAccessor<Integer> DATA_ACTION = SynchedEntityData.defineId(FoolEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_PHASE2 = SynchedEntityData.defineId(FoolEntity.class, EntityDataSerializers.BOOLEAN);

    // ----- números (provisórios) -----
    public static final double MAX_HEALTH = 180.0;
    private static final float SWIPE_DAMAGE = 6.0F;
    private static final float LANDING_DAMAGE = 3.0F;
    private static final float GALE_DAMAGE = 4.0F;
    private static final double HOVER_HEIGHT = 1.0;
    private static final double SWIPE_RANGE = 3.8;

    private @Nullable BlockPos arenaCenter;
    private List<FoolArena.Island> islands = List.of();
    private int island;
    private int actionTicks;
    private int actionLength;
    private Vec3 hopFrom = Vec3.ZERO;
    private Vec3 hopTo = Vec3.ZERO;
    private double hopArc;
    private int hopsInARow;
    private int landingsSinceDizzy;
    private int swipeCooldown;
    private int galeCooldown;

    /** Cliente: tick em que a ação atual começou (para a animação). */
    private int clientActionStart;

    public FoolEntity(EntityType<? extends FoolEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.xpReward = 50;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.ARMOR, 4.0)
                .add(Attributes.ATTACK_DAMAGE, SWIPE_DAMAGE)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 64.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3);
    }

    @Override
    public TarotArcana arcana() {
        return TarotArcana.THE_FOOL;
    }

    /** Liga o Louco à arena (as ilhas por onde ele salta). Sem arena (ovo/comando), ele fica na própria "ilha". */
    public void setArena(BlockPos center) {
        this.arenaCenter = center;
        this.islands = FoolArena.islands(center);
        this.island = FoolArena.islands(center).size() > 3 ? 3 : 0;
        // começa na ilha mais próxima de onde surgiu
        double best = Double.MAX_VALUE;
        for (int i = 0; i < this.islands.size(); i++) {
            double d = this.islands.get(i).surface().distanceToSqr(this.position());
            if (d < best) {
                best = d;
                this.island = i;
            }
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ACTION, HOVER);
        builder.define(DATA_PHASE2, false);
    }

    public int action() {
        return this.entityData.get(DATA_ACTION);
    }

    public boolean isPhase2() {
        return this.entityData.get(DATA_PHASE2);
    }

    /** Ticks (com fração) desde que a ação atual começou — só no cliente. */
    public float actionTime(float partialTick) {
        return this.tickCount - this.clientActionStart + partialTick;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (DATA_ACTION.equals(accessor)) {
            this.clientActionStart = this.tickCount;
        }
    }

    private void setAction(int action, int length) {
        this.entityData.set(DATA_ACTION, action);
        this.actionTicks = 0;
        this.actionLength = length;
        this.noPhysics = action == HOP;
    }

    private FoolArena.Island currentIsland() {
        if (this.islands.isEmpty()) {
            return new FoolArena.Island(this.blockPosition().below(2), 3);
        }
        return this.islands.get(Mth.clamp(this.island, 0, this.islands.size() - 1));
    }

    // ------------------------------------------------------------------ IA

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (this.swipeCooldown > 0) this.swipeCooldown--;
        if (this.galeCooldown > 0) this.galeCooldown--;
        this.actionTicks++;

        // fase 2: abaixo de metade da vida
        if (!this.isPhase2() && this.getHealth() < this.getMaxHealth() * 0.5F) {
            this.entityData.set(DATA_PHASE2, true);
            this.announce(level, "message.mistech.fool.phase2");
            level.playSound(null, this.blockPosition(), SoundEvents.WITCH_CELEBRATE, SoundSource.HOSTILE, 2.0F, 1.5F);
            this.startGale();
        }

        Player target = this.findTarget(level);
        if (target != null) {
            this.lookAtTarget(target);
        }
        switch (this.action()) {
            case HOP -> this.tickHop(level);
            case CAST -> this.tickCast(level, target);
            case SWIPE -> this.tickSwipe(level);
            case DIZZY -> this.tickDizzy(level);
            case GALE -> this.tickGale(level);
            default -> this.tickHover(level, target);
        }
        if (this.tickCount % 50 == 0) {
            level.playSound(null, this.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.HOSTILE, 0.5F, 1.6F + this.random.nextFloat() * 0.3F);
        }
    }

    private @Nullable Player findTarget(ServerLevel level) {
        Vec3 from = this.arenaCenter != null ? Vec3.atCenterOf(this.arenaCenter) : this.position();
        AABB box = new AABB(from, from).inflate(40.0, 30.0, 40.0);
        return level.getEntitiesOfClass(Player.class, box, p -> p.isAlive() && !p.isSpectator() && !p.isCreative()).stream()
                .min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
    }

    private void lookAtTarget(Player target) {
        double dx = target.getX() - this.getX();
        double dz = target.getZ() - this.getZ();
        float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
        this.setYRot(yaw);
        this.setYHeadRot(yaw);
        this.setYBodyRot(yaw);
        double dy = target.getEyeY() - this.getEyeY();
        this.setXRot((float) (-(Mth.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * Mth.RAD_TO_DEG)));
    }

    /** Vai suavemente até {@code goal}. */
    private void glideTo(Vec3 goal, double factor) {
        this.setDeltaMovement(goal.subtract(this.position()).scale(factor));
    }

    private void tickHover(ServerLevel level, @Nullable Player target) {
        FoolArena.Island here = this.currentIsland();
        Vec3 base = here.surface();
        // segue o alvo dentro da ilha
        Vec3 offset = Vec3.ZERO;
        if (target != null) {
            Vec3 toTarget = new Vec3(target.getX() - base.x, 0.0, target.getZ() - base.z);
            double max = Math.max(0.0, here.radius() - 1.5);
            if (toTarget.length() > max) {
                toTarget = toTarget.normalize().scale(max);
            }
            offset = toTarget.scale(0.6);
        }
        double bob = Math.sin(this.tickCount * 0.12) * 0.35;
        this.glideTo(base.add(offset).add(0.0, HOVER_HEIGHT + bob, 0.0), 0.15);

        if (target != null && this.swipeCooldown <= 0 && this.distanceToSqr(target) < SWIPE_RANGE * SWIPE_RANGE) {
            this.setAction(SWIPE, 12);
            return;
        }
        if (this.actionTicks < this.actionLength) {
            return;
        }
        if (target == null) {
            this.setAction(HOVER, 40);
            return;
        }
        if (this.isPhase2() && this.galeCooldown <= 0 && this.random.nextFloat() < 0.35F) {
            this.startGale();
        } else if (this.hopsInARow >= 2 || (this.hopsInARow > 0 && this.random.nextFloat() < 0.5F) || this.islands.size() < 2) {
            this.hopsInARow = 0;
            this.setAction(CAST, this.isPhase2() ? 14 : 18);
            level.playSound(null, this.blockPosition(), SoundEvents.BREEZE_CHARGE, SoundSource.HOSTILE, 1.0F, 1.2F);
        } else {
            this.startHop(level, target);
        }
    }

    private int hoverLength() {
        return this.isPhase2() ? 14 + this.random.nextInt(16) : 24 + this.random.nextInt(22);
    }

    private void startHop(ServerLevel level, Player target) {
        // escolhe a ilha: 60% a mais perto do alvo (pula em cima dele), senão uma qualquer
        int dest = this.island;
        if (this.random.nextFloat() < 0.6F) {
            double best = Double.MAX_VALUE;
            for (int i = 0; i < this.islands.size(); i++) {
                double d = this.islands.get(i).surface().distanceToSqr(target.position());
                if (i != this.island && d < best) {
                    best = d;
                    dest = i;
                }
            }
        }
        if (dest == this.island) {
            List<Integer> options = new ArrayList<>();
            for (int i = 0; i < this.islands.size(); i++) {
                if (i != this.island) {
                    options.add(i);
                }
            }
            dest = options.get(this.random.nextInt(options.size()));
        }
        FoolArena.Island to = this.islands.get(dest);
        this.hopFrom = this.position();
        // pousa um pouco deslocado do centro, para o lado do alvo
        Vec3 land = to.surface();
        Vec3 toTarget = new Vec3(target.getX() - land.x, 0.0, target.getZ() - land.z);
        if (toTarget.length() > 0.1) {
            land = land.add(toTarget.normalize().scale(Math.min(toTarget.length(), Math.max(0.0, to.radius() - 1.5))));
        }
        this.hopTo = land.add(0.0, HOVER_HEIGHT, 0.0);
        double dist = this.hopFrom.distanceTo(this.hopTo);
        this.hopArc = 4.0 + dist * 0.25;
        this.island = dest;
        this.hopsInARow++;
        int length = (int) (18 + dist * 0.6);
        this.setAction(HOP, this.isPhase2() ? (int) (length * 0.75) : length);
        level.playSound(null, this.blockPosition(), SoundEvents.BREEZE_JUMP, SoundSource.HOSTILE, 1.5F, 0.9F);
        level.sendParticles(ParticleTypes.GUST, this.getX(), this.getY(), this.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
    }

    private void tickHop(ServerLevel level) {
        double t = Math.min(1.0, (this.actionTicks + 1) / (double) this.actionLength);
        // curva suave (sobe rápido, plana no alto, desce)
        double ease = t * t * (3.0 - 2.0 * t);
        Vec3 next = this.hopFrom.lerp(this.hopTo, ease).add(0.0, Math.sin(Math.PI * t) * this.hopArc, 0.0);
        this.setDeltaMovement(next.subtract(this.position()));
        if (this.actionTicks % 2 == 0) {
            level.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY() + 0.3, this.getZ(), 2, 0.2, 0.2, 0.2, 0.01);
        }
        if (this.actionTicks >= this.actionLength) {
            this.land(level);
        }
    }

    private void land(ServerLevel level) {
        this.setDeltaMovement(Vec3.ZERO);
        level.playSound(null, this.blockPosition(), SoundEvents.BREEZE_LAND, SoundSource.HOSTILE, 1.5F, 1.0F);
        level.sendParticles(ParticleTypes.GUST_EMITTER_SMALL, this.getX(), this.getY(), this.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
        // rajada do pouso: empurra quem está perto
        for (Player player : this.playersNear(level, 3.5)) {
            Vec3 push = player.position().subtract(this.position()).multiply(1.0, 0.0, 1.0);
            push = push.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : push.normalize();
            player.hurtServer(level, this.damageSources().mobAttack(this), LANDING_DAMAGE);
            this.fling(player, push.scale(0.9), 0.55);
        }
        this.landingsSinceDizzy++;
        float dizzyChance = this.isPhase2() ? 0.2F : 0.35F;
        if (this.landingsSinceDizzy >= 3 || this.random.nextFloat() < dizzyChance) {
            this.landingsSinceDizzy = 0;
            this.setAction(DIZZY, 45);
            level.playSound(null, this.blockPosition(), SoundEvents.WITCH_CELEBRATE, SoundSource.HOSTILE, 1.5F, 1.8F);
        } else {
            this.setAction(HOVER, this.hoverLength());
        }
    }

    private void tickCast(ServerLevel level, @Nullable Player target) {
        Vec3 here = this.currentIsland().surface().add(0.0, HOVER_HEIGHT + 0.3, 0.0);
        this.glideTo(here, 0.1);
        if (this.actionTicks < this.actionLength - 4 && this.actionTicks % 2 == 0) {
            Vec3 tip = this.staffTip();
            level.sendParticles(ParticleTypes.SMALL_GUST, tip.x, tip.y, tip.z, 1, 0.1, 0.1, 0.1, 0.0);
        }
        if (this.actionTicks == this.actionLength - 4 && target != null) {
            int count = this.isPhase2() ? 3 : this.getHealth() < this.getMaxHealth() * 0.75F ? 2 : 1;
            Vec3 tip = this.staffTip();
            Vec3 dir = target.position().subtract(tip).multiply(1.0, 0.0, 1.0);
            dir = dir.lengthSqr() < 1.0E-4 ? this.getLookAngle() : dir.normalize();
            for (int i = 0; i < count; i++) {
                double spread = count == 1 ? 0.0 : (i - (count - 1) / 2.0) * 0.45;
                Vec3 d = dir.yRot((float) spread);
                FoolTornadoEntity tornado = FoolTornadoEntity.fromBoss(level, this, new Vec3(tip.x, this.currentIsland().surface().y, tip.z), d, target);
                level.addFreshEntity(tornado);
            }
            level.playSound(null, this.blockPosition(), SoundEvents.BREEZE_SHOOT, SoundSource.HOSTILE, 1.5F, 0.8F);
        }
        if (this.actionTicks >= this.actionLength) {
            if (target != null && this.islands.size() > 1 && this.random.nextFloat() < 0.5F) {
                this.startHop(level, target);
            } else {
                this.setAction(HOVER, this.hoverLength());
            }
        }
    }

    /** Ponta do cajado (mais ou menos acima da mão direita). */
    private Vec3 staffTip() {
        Vec3 side = Vec3.directionFromRotation(0.0F, this.getYRot() + 90.0F).scale(-0.6);
        return this.position().add(side).add(0.0, 3.0, 0.0);
    }

    private void tickSwipe(ServerLevel level) {
        this.setDeltaMovement(this.getDeltaMovement().scale(0.5));
        if (this.actionTicks == 6) {
            level.playSound(null, this.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.2F, 0.8F);
            level.playSound(null, this.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.HOSTILE, 1.0F, 2.0F);
            Vec3 look = Vec3.directionFromRotation(0.0F, this.getYRot());
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, this.getX() + look.x * 1.5, this.getY() + 1.2, this.getZ() + look.z * 1.5,
                    1, 0.0, 0.0, 0.0, 0.0);
            for (Player player : this.playersNear(level, SWIPE_RANGE)) {
                Vec3 to = player.position().subtract(this.position()).multiply(1.0, 0.0, 1.0);
                if (to.lengthSqr() > 1.0E-4 && to.normalize().dot(look) < 0.1) {
                    continue; // só na frente
                }
                if (player.hurtServer(level, this.damageSources().mobAttack(this), SWIPE_DAMAGE)) {
                    this.fling(player, (to.lengthSqr() < 1.0E-4 ? look : to.normalize()).scale(1.4), 0.6);
                }
            }
        }
        if (this.actionTicks >= this.actionLength) {
            this.swipeCooldown = this.isPhase2() ? 30 : 45;
            this.setAction(HOVER, this.hoverLength() / 2);
        }
    }

    private void tickDizzy(ServerLevel level) {
        // cai sentado na ilha, tonto
        this.glideTo(this.currentIsland().surface().add(0.0, 0.05, 0.0), 0.2);
        if (this.actionTicks % 5 == 0) {
            level.sendParticles(ParticleTypes.WAX_OFF, this.getX(), this.getY() + 2.9, this.getZ(), 3, 0.4, 0.1, 0.4, 0.0);
        }
        if (this.actionTicks >= this.actionLength) {
            this.setAction(HOVER, 10);
        }
    }

    private void startGale() {
        this.galeCooldown = 320;
        this.setAction(GALE, 50);
        if (this.level() instanceof ServerLevel level) {
            level.playSound(null, this.blockPosition(), SoundEvents.BREEZE_INHALE, SoundSource.HOSTILE, 2.0F, 0.7F);
        }
    }

    private void tickGale(ServerLevel level) {
        this.glideTo(this.currentIsland().surface().add(0.0, HOVER_HEIGHT + 0.5, 0.0), 0.15);
        if (this.actionTicks < 36) {
            // puxa todos para perto, girando
            for (Player player : this.playersNear(level, 13.0)) {
                Vec3 pull = this.position().subtract(player.position()).multiply(1.0, 0.0, 1.0);
                if (pull.lengthSqr() > 4.0) {
                    Vec3 v = pull.normalize().scale(0.09);
                    player.push(v.x, 0.0, v.z);
                    player.hurtMarked = true;
                }
            }
            if (this.actionTicks % 2 == 0) {
                double a = this.actionTicks * 0.6;
                for (int i = 0; i < 4; i++) {
                    double ang = a + i * Math.PI / 2.0;
                    double r = 6.0 - this.actionTicks * 0.12;
                    level.sendParticles(ParticleTypes.CLOUD, this.getX() + Math.cos(ang) * r, this.getY() + 0.5,
                            this.getZ() + Math.sin(ang) * r, 1, 0.0, 0.2, 0.0, 0.0);
                }
            }
        } else if (this.actionTicks == 40) {
            // estoura: todo mundo para o alto
            level.playSound(null, this.blockPosition(), SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), SoundSource.HOSTILE, 2.0F, 0.8F);
            level.sendParticles(ParticleTypes.GUST_EMITTER_LARGE, this.getX(), this.getY() + 0.5, this.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
            for (Player player : this.playersNear(level, 8.0)) {
                Vec3 away = player.position().subtract(this.position()).multiply(1.0, 0.0, 1.0);
                away = away.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : away.normalize();
                player.hurtServer(level, this.damageSources().mobAttack(this), GALE_DAMAGE);
                this.fling(player, away.scale(0.9), 1.15);
            }
        }
        if (this.actionTicks >= this.actionLength) {
            this.setAction(HOVER, this.hoverLength());
        }
    }

    private List<Player> playersNear(ServerLevel level, double radius) {
        return level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(radius, 3.0, radius),
                p -> p.isAlive() && !p.isSpectator() && !p.isCreative() && p.distanceToSqr(this) < (radius + 1.0) * (radius + 1.0));
    }

    /** Arremessa (para jogadores, a velocidade precisa ser enviada ao cliente). */
    private void fling(Player player, Vec3 horizontal, double up) {
        player.setDeltaMovement(horizontal.x, up, horizontal.z);
        player.hurtMarked = true;
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.resetFallDistance();
        }
    }

    private void announce(ServerLevel level, String key) {
        for (Player player : this.playersNear(level, 48.0)) {
            player.sendSystemMessage(Component.translatable(key).withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));
        }
    }

    @Override
    public void onFightTick(ArcanePlane.Fight fight) {
        if (this.arenaCenter == null) {
            this.setArena(fight.center());
        }
    }

    // ------------------------------------------------------------------ dano e afins

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (source.is(DamageTypeTags.IS_FALL) || source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.FLY_INTO_WALL)) {
            return false;
        }
        if (this.action() == DIZZY) {
            damage *= 1.5F;
        } else if (this.action() == HOP) {
            damage *= 0.75F;
        }
        return super.hurtServer(level, source, damage);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide()) {
            // rastro de vento na cauda
            if (this.random.nextInt(2) == 0) {
                this.level().addParticle(ParticleTypes.WHITE_SMOKE, this.getX() + (this.random.nextDouble() - 0.5) * 0.5, this.getY() + 0.2,
                        this.getZ() + (this.random.nextDouble() - 0.5) * 0.5, 0.0, -0.02, 0.0);
            }
            if (this.isPhase2() && this.random.nextInt(3) == 0) {
                this.level().addParticle(ParticleTypes.SMALL_GUST, this.getRandomX(0.8), this.getRandomY(), this.getRandomZ(0.8), 0.0, 0.0, 0.0);
            }
        }
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(net.minecraft.world.entity.Entity entity) {}

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return SoundEvents.BREEZE_IDLE_AIR;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.BREEZE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.BREEZE_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 1.25F;
    }
}

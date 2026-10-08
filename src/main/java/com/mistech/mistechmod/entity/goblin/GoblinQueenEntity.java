package com.mistech.mistechmod.entity.goblin;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.block.goblin.GoblinSealBlock;
import com.mistech.mistechmod.registry.GoblinContent;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

/**
 * Rainha Duende: chefe da arena no centro das catacumbas. Gorda e lenta, mas conjura:
 * <ul>
 * <li><b>Presas</b>: fileira de presas do evocador até o alvo.</li>
 * <li><b>Orbes</b>: 2-3 projéteis teleguiados (levitação).</li>
 * <li><b>Troca</b>: teleporta todos os jogadores da arena para lugares aleatórios dela (cegueira rápida).</li>
 * <li><b>Chamado</b>: invoca Duendes Corrompidos para defendê-la (até 3; 5 na fase 2).</li>
 * </ul>
 * Fase 2 (metade da vida): conjura mais rápido e às vezes some de perto quando apanha.
 * Nunca sai da arena. Ao morrer o selo da sala do tesouro (atrás do trono) some.
 */
public class GoblinQueenEntity extends Monster implements GoblinKin {
    private static final EntityDataAccessor<Integer> CASTING = SynchedEntityData.defineId(GoblinQueenEntity.class, EntityDataSerializers.INT);
    public static final int ARENA_RADIUS = 9;
    private static final int CAST_TIME = 20;
    private static final String SUMMON_TAG = "mistech_queen_summon";

    private enum Spell { FANGS, ORBS, SWAP, SUMMON }

    private final ServerBossEvent bossEvent = (ServerBossEvent) new ServerBossEvent(java.util.UUID.randomUUID(),
            Component.translatable("entity.mistech.goblin_queen"), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10)
            .setDarkenScreen(true);

    private @Nullable BlockPos arena;
    private @Nullable Spell pending;
    private int spellCooldown = 60;
    private int swapCooldown = 200;
    private int summonCooldown = 120;
    private boolean awake;

    public GoblinQueenEntity(EntityType<? extends GoblinQueenEntity> type, Level level) {
        super(type, level);
        this.xpReward = 150;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 300.0)
                .add(Attributes.ARMOR, 8.0)
                .add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.ATTACK_DAMAGE, 10.0)
                .add(Attributes.ATTACK_KNOCKBACK, 1.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CASTING, 0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true) {
            @Override
            public boolean canUse() {
                return GoblinQueenEntity.this.pending == null && super.canUse();
            }
        });
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** Centro da arena (chamado pela estrutura ao gerar). */
    public void setArena(BlockPos center) {
        this.arena = center.immutable();
        this.setHomeTo(center, ARENA_RADIUS);
    }

    /** 0..1 da animação de conjurar (cliente). */
    public float castProgress(float partialTicks) {
        int c = this.entityData.get(CASTING);
        return c <= 0 ? 0.0F : Mth.clamp((c - partialTicks) / 6.0F, 0.0F, 1.0F);
    }

    private boolean phaseTwo() {
        return this.getHealth() <= this.getMaxHealth() / 2.0F;
    }

    // ------------------------------------------------------------------ IA

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (this.arena == null) {
            this.setArena(this.blockPosition());
        }
        this.updateBossBar(level);

        int casting = this.entityData.get(CASTING);
        if (casting > 0) {
            this.entityData.set(CASTING, casting - 1);
        }

        // nunca sai da arena: volta andando e, se estiver longe demais, teleporta de volta
        double dist = Math.sqrt(this.blockPosition().distSqr(this.arena));
        if (dist > ARENA_RADIUS + 4) {
            this.teleportTo(this.arena.getX() + 0.5, this.arena.getY(), this.arena.getZ() + 0.5);
        } else if (dist > ARENA_RADIUS - 1 && this.getNavigation().isDone()) {
            this.getNavigation().moveTo(this.arena.getX() + 0.5, this.arena.getY(), this.arena.getZ() + 0.5, 1.0);
        }

        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            if (this.getHealth() < this.getMaxHealth() && this.tickCount % 20 == 0) {
                this.heal(2.0F); // sem ninguém lutando, ela se recupera
            }
            return;
        }
        if (!this.awake) {
            this.awake = true;
            this.playSound(SoundEvents.EVOKER_PREPARE_WOLOLO, 2.0F, 0.6F);
            for (ServerPlayer p : this.playersInArena(level)) {
                p.sendSystemMessage(Component.translatable("message.mistech.goblin_queen.awake"));
            }
        }

        this.swapCooldown--;
        this.summonCooldown--;
        if (this.pending != null) {
            // conjurando: parada, partículas, e solta no fim
            this.getNavigation().stop();
            this.getLookControl().setLookAt(target);
            level.sendParticles(ParticleTypes.WITCH, this.getX(), this.getY() + 2.6, this.getZ(), 3, 0.5, 0.4, 0.5, 0.0);
            if (casting <= 1) {
                this.release(level, this.pending, target);
                this.pending = null;
            }
            return;
        }
        if (--this.spellCooldown > 0) {
            return;
        }
        Spell spell = this.chooseSpell(level);
        this.pending = spell;
        this.entityData.set(CASTING, CAST_TIME);
        this.spellCooldown = this.phaseTwo() ? 45 : 70;
        this.playSound(spell == Spell.SUMMON ? SoundEvents.EVOKER_PREPARE_SUMMON : SoundEvents.EVOKER_PREPARE_ATTACK, 2.0F, 0.7F);
    }

    private Spell chooseSpell(ServerLevel level) {
        int maxSummons = this.phaseTwo() ? 5 : 3;
        if (this.summonCooldown <= 0 && this.countSummons(level) < maxSummons) {
            this.summonCooldown = this.phaseTwo() ? 240 : 360;
            return Spell.SUMMON;
        }
        if (this.swapCooldown <= 0) {
            this.swapCooldown = this.phaseTwo() ? 140 : 220;
            return Spell.SWAP;
        }
        return this.random.nextBoolean() ? Spell.FANGS : Spell.ORBS;
    }

    private void release(ServerLevel level, Spell spell, LivingEntity target) {
        switch (spell) {
            case FANGS -> this.castFangs(level, target);
            case ORBS -> this.castOrbs(level, target);
            case SWAP -> this.castSwap(level);
            case SUMMON -> this.castSummon(level);
        }
    }

    /** Fileira de presas do evocador em direção ao alvo (e um círculo em volta dela na fase 2). */
    private void castFangs(ServerLevel level, LivingEntity target) {
        double minY = Math.min(target.getY(), this.getY());
        double maxY = Math.max(target.getY(), this.getY()) + 1.0;
        float angle = (float) Mth.atan2(target.getZ() - this.getZ(), target.getX() - this.getX());
        for (int i = 0; i < 14; i++) {
            double d = 1.25 * (i + 1);
            this.spawnFang(level, this.getX() + Mth.cos(angle) * d, this.getZ() + Mth.sin(angle) * d, minY, maxY, angle, i);
        }
        if (this.phaseTwo()) {
            for (int i = 0; i < 8; i++) {
                float a = angle + i * Mth.PI * 2.0F / 8.0F;
                this.spawnFang(level, this.getX() + Mth.cos(a) * 2.5, this.getZ() + Mth.sin(a) * 2.5, minY, maxY, a, 3);
            }
        }
        this.playSound(SoundEvents.EVOKER_CAST_SPELL, 2.0F, 0.8F);
    }

    private void spawnFang(ServerLevel level, double x, double z, double minY, double maxY, float yRot, int delay) {
        BlockPos pos = BlockPos.containing(x, maxY, z);
        boolean found = false;
        double top = 0.0;
        do {
            BlockPos below = pos.below();
            if (level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
                if (!level.isEmptyBlock(pos)) {
                    var shape = level.getBlockState(pos).getCollisionShape(level, pos);
                    if (!shape.isEmpty()) {
                        top = shape.max(Direction.Axis.Y);
                    }
                }
                found = true;
                break;
            }
            pos = pos.below();
        } while (pos.getY() >= Mth.floor(minY) - 1);
        if (found) {
            level.addFreshEntity(new EvokerFangs(level, x, pos.getY() + top, z, yRot, delay, this));
        }
    }

    /** Orbes teleguiados (os do shulker): quem é acertado flutua - e cai. */
    private void castOrbs(ServerLevel level, LivingEntity target) {
        int n = this.phaseTwo() ? 3 : 2;
        for (int i = 0; i < n; i++) {
            ShulkerBullet bullet = new ShulkerBullet(level, this, target, Direction.Axis.values()[this.random.nextInt(3)]);
            bullet.setPos(this.getX() + (this.random.nextDouble() - 0.5), this.getY() + 2.8, this.getZ() + (this.random.nextDouble() - 0.5));
            level.addFreshEntity(bullet);
        }
        this.playSound(SoundEvents.SHULKER_SHOOT, 2.0F, 0.6F);
    }

    /** Teleporta cada jogador da arena para um lugar aleatório dela. */
    private void castSwap(ServerLevel level) {
        for (ServerPlayer player : this.playersInArena(level)) {
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1.0, player.getZ(), 30, 0.4, 0.8, 0.4, 0.1);
            for (int tries = 0; tries < 16; tries++) {
                double a = this.random.nextDouble() * Math.PI * 2;
                double r = 2.5 + this.random.nextDouble() * (ARENA_RADIUS - 3.5);
                double x = this.arena.getX() + 0.5 + Math.cos(a) * r;
                double z = this.arena.getZ() + 0.5 + Math.sin(a) * r;
                if (player.randomTeleport(x, this.arena.getY() + 1, z, true)) {
                    break;
                }
            }
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 25, 0, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 1, false, false));
            level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 0.6F);
        }
        this.playSound(SoundEvents.ILLUSIONER_MIRROR_MOVE, 2.0F, 0.8F);
    }

    /** Invoca duendes em volta dela. */
    private void castSummon(ServerLevel level) {
        int n = this.phaseTwo() ? 3 : 2;
        for (int i = 0; i < n; i++) {
            CorruptedGoblinEntity goblin = GoblinContent.CORRUPTED_GOBLIN.get().create(level, EntitySpawnReason.MOB_SUMMONED);
            if (goblin == null) {
                continue;
            }
            double a = this.random.nextDouble() * Math.PI * 2;
            double r = 2.0 + this.random.nextDouble() * 3.0;
            double x = this.getX() + Math.cos(a) * r, z = this.getZ() + Math.sin(a) * r;
            goblin.snapTo(x, this.getY(), z, this.random.nextFloat() * 360.0F, 0.0F);
            if (!level.noCollision(goblin)) {
                goblin.snapTo(this.getX(), this.getY(), this.getZ(), 0.0F, 0.0F);
            }
            goblin.getPersistentData().putBoolean(SUMMON_TAG, true);
            goblin.setTarget(this.getTarget());
            level.addFreshEntity(goblin);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, goblin.getX(), goblin.getY() + 0.6, goblin.getZ(), 15, 0.3, 0.4, 0.3, 0.02);
        }
        this.playSound(SoundEvents.EVOKER_CAST_SPELL, 2.0F, 0.6F);
    }

    private int countSummons(ServerLevel level) {
        return level.getEntitiesOfClass(CorruptedGoblinEntity.class, new AABB(this.arena).inflate(ARENA_RADIUS + 6),
                g -> g.isAlive() && g.getPersistentData().getBooleanOr(SUMMON_TAG, false)).size();
    }

    private List<ServerPlayer> playersInArena(ServerLevel level) {
        BlockPos c = this.arena != null ? this.arena : this.blockPosition();
        return level.getEntitiesOfClass(ServerPlayer.class, new AABB(c).inflate(ARENA_RADIUS + 1, 8, ARENA_RADIUS + 1),
                p -> p.isAlive() && !p.isSpectator());
    }

    private void updateBossBar(ServerLevel level) {
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        if (this.tickCount % 10 != 0) {
            return;
        }
        List<ServerPlayer> inside = this.playersInArena(level);
        for (ServerPlayer p : List.copyOf(this.bossEvent.getPlayers())) {
            if (!inside.contains(p)) {
                this.bossEvent.removePlayer(p);
            }
        }
        for (ServerPlayer p : inside) {
            this.bossEvent.addPlayer(p);
        }
    }

    // ------------------------------------------------------------------ dano / morte

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        boolean hurt = super.hurtServer(level, source, amount);
        // fase 2: às vezes some de perto de quem bateu
        if (hurt && this.phaseTwo() && this.arena != null && this.random.nextFloat() < 0.2F && source.getEntity() instanceof Player) {
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, this.getX(), this.getY() + 1.2, this.getZ(), 30, 0.6, 1.0, 0.6, 0.1);
            double a = this.random.nextDouble() * Math.PI * 2;
            this.randomTeleport(this.arena.getX() + 0.5 + Math.cos(a) * 6, this.arena.getY() + 1, this.arena.getZ() + 0.5 + Math.sin(a) * 6, true);
        }
        return hurt;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (this.level() instanceof ServerLevel level) {
            BlockPos c = this.arena != null ? this.arena : this.blockPosition();
            // a sala do tesouro fica a leste (atrás do trono): procura o selo a partir de lá
            GoblinSealBlock.dissolveNearest(level, c.offset(6, 1, 0), ARENA_RADIUS + 6);
            for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, new AABB(c).inflate(32))) {
                p.sendSystemMessage(Component.translatable("message.mistech.goblin_queen.dead"));
            }
            // os duendes invocados viram fumaça
            for (CorruptedGoblinEntity g : level.getEntitiesOfClass(CorruptedGoblinEntity.class, new AABB(c).inflate(ARENA_RADIUS + 6),
                    g -> g.getPersistentData().getBooleanOr(SUMMON_TAG, false))) {
                level.sendParticles(ParticleTypes.LARGE_SMOKE, g.getX(), g.getY() + 0.6, g.getZ(), 10, 0.3, 0.4, 0.3, 0.02);
                g.discard();
            }
        }
        this.bossEvent.removeAllPlayers();
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected boolean considersEntityAsAlly(Entity other) {
        return GoblinKin.allied(this, other) || super.considersEntityAsAlly(other);
    }

    // ------------------------------------------------------------------ salvar

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (this.arena != null) {
            output.store("Arena", BlockPos.CODEC, this.arena);
        }
        output.putBoolean("Awake", this.awake);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        input.read("Arena", BlockPos.CODEC).ifPresent(this::setArena);
        this.awake = input.getBooleanOr("Awake", false);
        if (this.hasCustomName()) {
            this.bossEvent.setName(this.getDisplayName());
        }
    }

    // ------------------------------------------------------------------ sons

    @Override
    public float getVoicePitch() {
        return 0.7F + (this.random.nextFloat() - 0.5F) * 0.1F;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.WITCH_CELEBRATE;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WITCH_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.EVOKER_DEATH;
    }
}

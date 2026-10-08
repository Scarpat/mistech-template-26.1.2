package com.mistech.mistechmod.tarot.magus;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Magias que voam (do Mago, dos Aspectos e do Cajado do Mago). Um tipo só com várias {@link Kind}s: muda o modelo,
 * a cor, o rastro e o que acontece ao acertar. Não é salva no mundo.
 */
public class MagusSpellEntity extends Entity {
    public enum Kind {
        WIND_BOLT(0xE8F8FF, 0.6F), WIND_BLADE(0xCFEFFF, 1.6F), SAND_BLADE(0xE8C77A, 0.7F), ICE_ORB(0x9FE8FF, 0.8F),
        WATER_BOLT(0x3AB8F0, 0.7F), FIREBALL(0xFF7A1A, 1.5F), METEOR(0xFF5A1A, 2.2F), ROCK(0xFF8A2A, 0.8F), BOULDER(0x8E8A82, 1.5F),
        ARCANE_BOLT(0xFFD87A, 0.8F), FIRE_TORNADO(0xFF7A1A, 1.0F);

        public final int color;
        public final float scale;

        Kind(int color, float scale) {
            this.color = color;
            this.scale = scale;
        }

        public static Kind byId(int id) {
            Kind[] v = values();
            return id >= 0 && id < v.length ? v[id] : WIND_BOLT;
        }
    }

    private static final EntityDataAccessor<Integer> DATA_KIND = SynchedEntityData.defineId(MagusSpellEntity.class, EntityDataSerializers.INT);

    private @Nullable UUID owner;
    private @Nullable UUID target;
    private boolean fromPlayer;
    private Vec3 velocity = Vec3.ZERO;
    private float damage = 6.0F;
    private int maxLife = 100;
    private double gravity;
    private double homing;
    private final Map<UUID, Integer> lastHit = new HashMap<>();

    public MagusSpellEntity(EntityType<? extends MagusSpellEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    /** Cria a magia já posicionada e com velocidade (não adiciona ao mundo). */
    public static MagusSpellEntity create(ServerLevel level, Kind kind, @Nullable Entity owner, Vec3 pos, Vec3 velocity, float damage) {
        MagusSpellEntity spell = new MagusSpellEntity(MagusContent.SPELL.get(), level);
        spell.entityData.set(DATA_KIND, kind.ordinal());
        spell.setPos(pos);
        spell.owner = owner == null ? null : owner.getUUID();
        spell.fromPlayer = owner instanceof Player;
        spell.velocity = velocity;
        spell.setDeltaMovement(velocity);
        spell.damage = damage;
        switch (kind) {
            case WIND_BLADE -> spell.maxLife = 45;
            case BOULDER, ROCK -> spell.gravity = 0.04;
            case METEOR -> spell.gravity = 0.03;
            case ARCANE_BOLT -> spell.homing = 0.07;
            case FIRE_TORNADO -> {
                spell.maxLife = 160;
                spell.homing = 0.05;
            }
            default -> spell.maxLife = 100;
        }
        return spell;
    }

    public MagusSpellEntity target(@Nullable Entity target) {
        this.target = target == null ? null : target.getUUID();
        return this;
    }

    public MagusSpellEntity life(int ticks) {
        this.maxLife = ticks;
        return this;
    }

    public Kind kind() {
        return Kind.byId(this.entityData.get(DATA_KIND));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_KIND, 0);
    }

    // ------------------------------------------------------------------ movimento

    @Override
    public void tick() {
        super.tick();
        Kind kind = this.kind();
        if (this.level().isClientSide()) {
            this.trail(kind);
            return;
        }
        ServerLevel level = (ServerLevel) this.level();
        if (this.tickCount > this.maxLife) {
            if (kind == Kind.FIREBALL || kind == Kind.METEOR) {
                this.impact(level, null);
            } else {
                level.sendParticles(ParticleTypes.POOF, this.getX(), this.getY(), this.getZ(), 6, 0.2, 0.2, 0.2, 0.02);
            }
            this.discard();
            return;
        }
        // perseguir
        if (this.homing > 0 && this.target != null && level.getEntity(this.target) instanceof LivingEntity t && t.isAlive()) {
            Vec3 want = t.position().add(0, kind == Kind.FIRE_TORNADO ? 0 : t.getBbHeight() * 0.5, 0).subtract(this.position());
            if (kind == Kind.FIRE_TORNADO) {
                want = new Vec3(want.x, 0, want.z);
            }
            if (want.lengthSqr() > 1.0E-4) {
                double speed = this.velocity.length();
                this.velocity = this.velocity.normalize().lerp(want.normalize(), this.homing).normalize().scale(speed);
            }
        }
        this.velocity = this.velocity.add(0, -this.gravity, 0);
        Vec3 from = this.position();
        Vec3 to = from.add(this.velocity);
        if (kind == Kind.FIRE_TORNADO) {
            // anda pelo chão: acompanha a altura do terreno
            BlockPos below = BlockPos.containing(to.x, to.y - 0.5, to.z);
            if (level.getBlockState(below).isAir() && !level.getBlockState(below.below()).isAir()) {
                to = to.add(0, -1, 0);
            } else if (!level.getBlockState(BlockPos.containing(to)).isAir()) {
                to = to.add(0, 1, 0);
            }
            if (this.tickCount % 15 == 0) {
                level.playSound(null, this.blockPosition(), SoundEvents.BLAZE_BURN, SoundSource.HOSTILE, 0.8F, 0.8F);
            }
        }
        // acertou bloco?
        boolean passesBlocks = kind == Kind.WIND_BLADE || kind == Kind.FIRE_TORNADO;
        if (!passesBlocks) {
            BlockHitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if (hit.getType() == HitResult.Type.BLOCK) {
                this.setPos(hit.getLocation());
                this.impact(level, null);
                this.discard();
                return;
            }
        }
        this.setPos(to);
        this.setDeltaMovement(this.velocity);
        // acertou alguém?
        double r = this.kind().scale * 0.45 + (kind == Kind.WIND_BLADE ? 1.0 : 0.2);
        AABB box = new AABB(from, to).inflate(r, kind == Kind.FIRE_TORNADO ? 1.5 : r, r);
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, box, this::canHit)) {
            if (kind == Kind.WIND_BLADE || kind == Kind.FIRE_TORNADO) {
                Integer last = this.lastHit.get(living.getUUID());
                if (last != null && this.tickCount - last < 20) {
                    continue;
                }
                this.lastHit.put(living.getUUID(), this.tickCount);
                this.hit(level, living, kind);
            } else {
                this.impact(level, living);
                this.discard();
                return;
            }
        }
    }

    private boolean canHit(LivingEntity e) {
        if (!e.isAlive() || e.getUUID().equals(this.owner)) {
            return false;
        }
        if (e instanceof Player p) {
            return !this.fromPlayer && !p.isSpectator() && !p.isCreative();
        }
        if (e instanceof MagusEntity || e instanceof MagusAspectEntity
                || e instanceof com.mistech.mistechmod.tarot.priestess.PriestessFigure) {
            return this.fromPlayer;
        }
        return true;
    }

    private @Nullable LivingEntity ownerLiving(ServerLevel level) {
        return this.owner != null && level.getEntity(this.owner) instanceof LivingEntity l ? l : null;
    }

    private DamageSource source(ServerLevel level) {
        LivingEntity o = this.ownerLiving(level);
        if (o instanceof Player p) {
            return this.damageSources().playerAttack(p);
        }
        return this.damageSources().mobProjectile(this, o);
    }

    /** Dano direto num alvo + efeito do tipo. */
    private void hit(ServerLevel level, LivingEntity living, Kind kind) {
        if (!living.hurtServer(level, this.source(level), this.damage)) {
            return;
        }
        Vec3 dir = this.velocity.lengthSqr() < 1.0E-4 ? Vec3.ZERO : this.velocity.normalize();
        switch (kind) {
            case WIND_BOLT, WATER_BOLT -> push(living, dir.scale(0.8), 0.35);
            case WIND_BLADE -> push(living, dir.scale(0.6), 0.5);
            case SAND_BLADE -> living.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 0));
            case ICE_ORB -> {
                living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 3));
                living.setTicksFrozen(Math.max(living.getTicksFrozen(), 160));
            }
            case FIRE_TORNADO -> {
                living.igniteForSeconds(3.0F);
                push(living, dir.scale(0.4), 0.6);
            }
            case ARCANE_BOLT -> living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0));
            default -> {}
        }
    }

    private void impact(ServerLevel level, @Nullable LivingEntity direct) {
        Kind kind = this.kind();
        Vec3 p = this.position();
        switch (kind) {
            case FIREBALL, METEOR, ROCK, BOULDER -> {
                double radius = kind == Kind.ROCK ? 1.8 : kind == Kind.BOULDER ? 2.5 : kind == Kind.METEOR ? 3.2 : 3.5;
                LivingEntity o = this.ownerLiving(level);
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(p, p).inflate(radius), this::canHit)) {
                    double d = e.position().add(0, e.getBbHeight() / 2, 0).distanceTo(p);
                    if (d > radius + 0.5) {
                        continue;
                    }
                    float dmg = this.damage * (float) Math.max(0.35, 1.0 - d / (radius + 1.0));
                    DamageSource src = o instanceof Player pl ? this.damageSources().playerAttack(pl) : this.damageSources().explosion(this, o);
                    if (e.hurtServer(level, src, dmg)) {
                        Vec3 away = e.position().subtract(p).multiply(1, 0, 1);
                        away = away.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : away.normalize();
                        push(e, away.scale(kind == Kind.BOULDER ? 0.9 : 0.6), 0.45);
                        if (kind != Kind.BOULDER) {
                            e.igniteForSeconds(kind == Kind.ROCK ? 2.0F : 4.0F);
                        }
                    }
                }
                if (kind == Kind.BOULDER) {
                    level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MOSSY_COBBLESTONE.defaultBlockState()),
                            p.x, p.y + 0.5, p.z, 40, 1.0, 0.5, 1.0, 0.1);
                    level.playSound(null, BlockPos.containing(p), SoundEvents.STONE_BREAK, SoundSource.HOSTILE, 2.0F, 0.5F);
                    MagusSpells.coverMound(level, this.ownerCaster(level), BlockPos.containing(p.x, p.y + 0.2, p.z));
                } else {
                    level.sendParticles(kind == Kind.ROCK ? ParticleTypes.EXPLOSION : ParticleTypes.EXPLOSION_EMITTER, p.x, p.y + 0.5, p.z, 1, 0, 0, 0, 0);
                    level.sendParticles(ParticleTypes.LAVA, p.x, p.y + 0.5, p.z, kind == Kind.ROCK ? 4 : 12, 0.8, 0.3, 0.8, 0.0);
                    level.playSound(null, BlockPos.containing(p), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE,
                            kind == Kind.ROCK ? 1.0F : 2.5F, kind == Kind.ROCK ? 1.3F : 0.8F);
                    if (kind == Kind.METEOR || kind == Kind.ROCK) {
                        MagusSpells.magmaPatch(level, this.ownerCaster(level), BlockPos.containing(p.x, p.y - 0.5, p.z),
                                kind == Kind.METEOR ? 2 : 1, kind == Kind.METEOR ? 160 : 200);
                    }
                }
            }
            default -> {
                if (direct != null) {
                    this.hit(level, direct, kind);
                }
                level.sendParticles(switch (kind) {
                    case ICE_ORB -> ParticleTypes.SNOWFLAKE;
                    case WATER_BOLT -> ParticleTypes.SPLASH;
                    case ARCANE_BOLT -> ParticleTypes.END_ROD;
                    case SAND_BLADE -> new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState());
                    default -> ParticleTypes.CLOUD;
                }, p.x, p.y, p.z, 10, 0.3, 0.3, 0.3, 0.05);
                level.playSound(null, BlockPos.containing(p), switch (kind) {
                    case ICE_ORB -> SoundEvents.GLASS_BREAK;
                    case WATER_BOLT -> SoundEvents.GENERIC_SPLASH;
                    case ARCANE_BOLT -> SoundEvents.AMETHYST_BLOCK_RESONATE;
                    default -> SoundEvents.BREEZE_WIND_CHARGE_BURST.value();
                }, SoundSource.HOSTILE, 0.8F, 1.2F);
            }
        }
    }

    private @Nullable MagusCaster ownerCaster(ServerLevel level) {
        return this.owner != null && level.getEntity(this.owner) instanceof MagusCaster c ? c : null;
    }

    static void push(LivingEntity e, Vec3 horizontal, double up) {
        e.setDeltaMovement(e.getDeltaMovement().add(horizontal.x, up, horizontal.z));
        e.hurtMarked = true;
        if (e instanceof ServerPlayer sp) {
            sp.resetFallDistance();
        }
    }

    // ------------------------------------------------------------------ visual (cliente)

    private void trail(Kind kind) {
        Level level = this.level();
        double x = this.getX(), y = this.getY(), z = this.getZ();
        switch (kind) {
            case WIND_BOLT, WIND_BLADE -> level.addParticle(ParticleTypes.CLOUD, x, y, z, 0, 0, 0);
            case SAND_BLADE -> level.addParticle(new DustParticleOptions(0xE8C77A, 1.2F), x, y, z, 0, 0, 0);
            case ICE_ORB -> level.addParticle(ParticleTypes.SNOWFLAKE, x, y, z, 0, 0, 0);
            case WATER_BOLT -> level.addParticle(ParticleTypes.SPLASH, x, y, z, 0, 0, 0);
            case FIREBALL, METEOR, ROCK -> {
                level.addParticle(ParticleTypes.FLAME, x + (this.random.nextDouble() - 0.5) * 0.6, y, z + (this.random.nextDouble() - 0.5) * 0.6, 0, 0.02, 0);
                level.addParticle(ParticleTypes.LARGE_SMOKE, x, y, z, 0, 0.02, 0);
            }
            case BOULDER -> level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MOSSY_COBBLESTONE.defaultBlockState()), x, y, z, 0, 0, 0);
            case ARCANE_BOLT -> level.addParticle(ParticleTypes.END_ROD, x, y, z, 0, 0, 0);
            case FIRE_TORNADO -> {
                for (int i = 0; i < 3; i++) {
                    double h = this.random.nextDouble() * 3.0;
                    double rr = 0.3 + h * 0.35;
                    double a = this.tickCount * 0.5 + h * 2.2 + i * 2.1;
                    level.addParticle(i == 0 ? ParticleTypes.LARGE_SMOKE : ParticleTypes.FLAME, x + Math.cos(a) * rr, y + h, z + Math.sin(a) * rr,
                            -Math.sin(a) * 0.05, 0.03, Math.cos(a) * 0.05);
                }
            }
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

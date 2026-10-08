package com.mistech.mistechmod.tarot.magus;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.tarot.magus.MagusSpellEntity.Kind;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * As magias do Mago, usadas por ele e pelos Aspectos. Quase tudo tem aviso (partículas no chão) antes de acertar.
 * Danos-base pensados para quem luta com Oricalco (a armadura tira bastante); multiplicados por {@link MagusCaster#power()}.
 */
public final class MagusSpells {
    // ----- danos-base (provisórios) -----
    public static final float SLAM = 20, SPIKE = 15, STOMP = 12, BOULDER = 18;
    public static final float BOLT = 8, BLADE = 14;
    public static final float WAVE = 10, ICE = 10, WHIRL = 4;
    public static final float METEOR = 22, BREATH = 5, FIREBALL = 18, CRACK = 8;
    public static final float ROCK = 10, STEAM = 3, LIGHTNING = 16, ROOTS = 8, SAND = 7, TORNADO = 8;
    public static final float RAY = 12, STRIKE = 16, ARCANE = 10, SWING = 22;

    private static final int EARTH_C = 0x8B6A3A, AIR_C = 0xDFF6FF, WATER_C = 0x3AB8F0, FIRE_C = 0xFF4A1A, GOLD_C = 0xFFD87A;

    private MagusSpells() {}

    // ================================================================= utilidades

    /** Jogadores que valem como alvo (em volta da arena, ou do conjurador fora dela). */
    public static List<Player> players(ServerLevel level, MagusCaster c) {
        BlockPos center = c.arenaCenter();
        Vec3 from = center != null ? Vec3.atCenterOf(center) : c.self().position();
        return level.getEntitiesOfClass(Player.class, new AABB(from, from).inflate(48, 40, 48), MagusSpells::valid);
    }

    public static boolean valid(Player p) {
        return p.isAlive() && !p.isSpectator() && !p.isCreative();
    }

    static void repeat(MagusCaster c, int times, int interval, IntConsumer action) {
        for (int i = 0; i < times; i++) {
            int k = i;
            c.schedule(i * interval, () -> action.accept(k));
        }
    }

    static DamageSource melee(MagusCaster c) {
        return c.self().damageSources().mobAttack(c.self());
    }

    static void hurt(ServerLevel level, MagusCaster c, LivingEntity e, float base) {
        e.hurtServer(level, melee(c), base * c.power());
    }

    static void fling(LivingEntity e, Vec3 horizontal, double up) {
        MagusSpellEntity.push(e, horizontal, up);
    }

    /** Altura do chão embaixo de {@code pos} (até 8 blocos para baixo). */
    public static double groundY(ServerLevel level, Vec3 pos) {
        BlockPos.MutableBlockPos m = BlockPos.containing(pos).mutable();
        for (int i = 0; i < 14; i++) {
            BlockState b = level.getBlockState(m.below());
            if (!b.isAir() && b.getFluidState().isEmpty()) {
                return m.getY();
            }
            m.move(Direction.DOWN);
        }
        return pos.y;
    }

    static Vec3 flat(Vec3 v) {
        Vec3 f = new Vec3(v.x, 0, v.z);
        return f.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : f.normalize();
    }

    static Vec3 hand(Mob self, double height) {
        return self.position().add(0, height, 0).add(self.getLookAngle().multiply(1, 0, 1).scale(0.8));
    }

    static void spawn(ServerLevel level, MagusSpellEntity spell) {
        level.addFreshEntity(spell);
    }

    public static void warnCircle(ServerLevel level, Vec3 at, double radius, int color) {
        MagusArena.Field.warnCircle(level, at, radius, color);
    }

    // ================================================================= TERRA

    /** Golpe de punho/braço na frente (corpo a corpo). */
    public static void slam(ServerLevel level, MagusCaster c, double range, float dmg) {
        Mob self = c.self();
        Vec3 look = flat(self.getLookAngle());
        Vec3 at = self.position().add(look.scale(range * 0.5));
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MOSSY_COBBLESTONE.defaultBlockState()),
                at.x, at.y + 0.2, at.z, 40, 1.2, 0.2, 1.2, 0.15);
        level.playSound(null, BlockPos.containing(at), SoundEvents.IRON_GOLEM_ATTACK, SoundSource.HOSTILE, 2.0F, 0.6F);
        for (Player p : players(level, c)) {
            Vec3 to = p.position().subtract(self.position());
            if (to.horizontalDistance() > range + self.getBbWidth() / 2 || Math.abs(to.y) > 4) {
                continue;
            }
            if (to.horizontalDistance() > 1.5 && flat(to).dot(look) < 0.2) {
                continue;
            }
            hurt(level, c, p, dmg);
            fling(p, flat(to).scale(1.3), 0.75);
        }
    }

    /** Linha de espinhos de pedra que brota do conjurador até depois do alvo (aviso de 1 s no chão). */
    public static void spikeLine(ServerLevel level, MagusCaster c, Vec3 targetPos, float dmg) {
        Mob self = c.self();
        Vec3 start = self.position();
        Vec3 dir = flat(targetPos.subtract(start));
        int length = 16;
        double y = groundY(level, start);
        List<Vec3> pts = new ArrayList<>();
        for (int i = 2; i <= length; i++) {
            Vec3 p = start.add(dir.scale(i));
            pts.add(new Vec3(p.x, groundY(level, new Vec3(p.x, y + 2, p.z)), p.z));
        }
        repeat(c, 5, 4, k -> {
            for (Vec3 p : pts) {
                level.sendParticles(new DustParticleOptions(EARTH_C, 1.4F), p.x, p.y + 0.1, p.z, 1, 0.2, 0, 0.2, 0);
            }
        });
        level.playSound(null, self.blockPosition(), SoundEvents.ROOTED_DIRT_BREAK, SoundSource.HOSTILE, 2.0F, 0.5F);
        MagusArena.Field field = c.field();
        for (int i = 0; i < pts.size(); i++) {
            Vec3 p = pts.get(i);
            c.schedule(20 + i, () -> {
                BlockPos base = BlockPos.containing(p.x, p.y + 0.1, p.z);
                if (field != null) {
                    BlockState frustum = Blocks.POINTED_DRIPSTONE.defaultBlockState().setValue(PointedDripstoneBlock.THICKNESS, DripstoneThickness.FRUSTUM);
                    BlockState tip = Blocks.POINTED_DRIPSTONE.defaultBlockState();
                    if (field.temp(level, base, frustum, 50, false)) {
                        field.temp(level, base.above(), tip, 50, false);
                    }
                }
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DRIPSTONE_BLOCK.defaultBlockState()),
                        p.x, p.y + 0.5, p.z, 12, 0.3, 0.5, 0.3, 0.1);
                level.playSound(null, base, SoundEvents.POINTED_DRIPSTONE_LAND, SoundSource.HOSTILE, 1.0F, 0.7F);
                for (Player pl : players(level, c)) {
                    if (pl.position().subtract(p).horizontalDistance() < 1.3 && pl.getY() - p.y < 2.0) {
                        hurt(level, c, pl, dmg);
                        fling(pl, Vec3.ZERO, 0.9);
                    }
                }
            });
        }
    }

    /** Pisão: anéis de choque que se expandem pelo chão (pular por cima). */
    public static void stomp(ServerLevel level, MagusCaster c, int rings, float dmg) {
        Mob self = c.self();
        Vec3 center = self.position();
        double floor = groundY(level, center);
        level.playSound(null, self.blockPosition(), SoundEvents.WARDEN_ATTACK_IMPACT, SoundSource.HOSTILE, 2.5F, 0.5F);
        for (int ring = 0; ring < rings; ring++) {
            List<Player> hit = new ArrayList<>();
            for (int t = 0; t < 24; t++) {
                double r = 1.5 + t * 0.6;
                c.schedule(ring * 14 + t, () -> {
                    int n = (int) (r * 6);
                    for (int i = 0; i < n; i++) {
                        double a = i * Math.PI * 2 / n;
                        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.COARSE_DIRT.defaultBlockState()),
                                center.x + Math.cos(a) * r, floor + 0.15, center.z + Math.sin(a) * r, 1, 0, 0.05, 0, 0.05);
                    }
                    for (Player p : players(level, c)) {
                        double d = p.position().subtract(center).horizontalDistance();
                        if (Math.abs(d - r) < 0.8 && p.getY() - floor < 0.7 && !hit.contains(p)) {
                            hit.add(p);
                            hurt(level, c, p, dmg);
                            fling(p, flat(p.position().subtract(center)).scale(0.5), 0.6);
                        }
                    }
                });
            }
        }
    }

    /** Arremessa uma rocha enorme em arco; onde cai vira cobertura. */
    public static void boulder(ServerLevel level, MagusCaster c, Vec3 targetPos, float dmg) {
        Mob self = c.self();
        Vec3 from = self.position().add(0, self.getBbHeight() * 0.9, 0);
        Vec3 d = targetPos.subtract(from);
        double dist = d.horizontalDistance();
        double t = Math.max(18, Math.min(40, dist * 1.4));
        double g = 0.04;
        Vec3 vel = new Vec3(d.x / t, (d.y + 0.5 * g * t * t) / t, d.z / t);
        spawn(level, MagusSpellEntity.create(level, Kind.BOULDER, self, from, vel, dmg * c.power()));
        warnCircle(level, targetPos, 2.5, EARTH_C);
        level.playSound(null, self.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.5F, 0.6F);
    }

    /** Montinho de pedra temporário (cobertura) onde a rocha caiu. */
    public static void coverMound(ServerLevel level, @Nullable MagusCaster c, BlockPos at) {
        MagusArena.Field field = c == null ? null : c.field();
        if (field == null) {
            return;
        }
        BlockState s = Blocks.MOSSY_COBBLESTONE.defaultBlockState();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (Math.abs(dx) + Math.abs(dz) <= 1) {
                    field.temp(level, at.offset(dx, 0, dz), s, 240, false);
                }
            }
        }
        field.temp(level, at.above(), s, 240, false);
    }

    // ================================================================= AR

    /** Leque de projéteis na direção do alvo. */
    public static void fan(ServerLevel level, MagusCaster c, Vec3 from, Vec3 targetPos, Kind kind, int count, double spread, double speed, float dmg) {
        Vec3 dir = targetPos.subtract(from).normalize();
        for (int i = 0; i < count; i++) {
            double a = count == 1 ? 0 : (i - (count - 1) / 2.0) * spread;
            Vec3 v = dir.yRot((float) a).scale(speed);
            spawn(level, MagusSpellEntity.create(level, kind, c.self(), from, v, dmg * c.power()));
        }
    }

    /** Anel de projéteis em volta. */
    public static void ring(ServerLevel level, MagusCaster c, Vec3 from, Kind kind, int count, double speed, float dmg, double phase) {
        for (int i = 0; i < count; i++) {
            double a = phase + i * Math.PI * 2 / count;
            Vec3 v = new Vec3(Math.cos(a), -0.02, Math.sin(a)).scale(speed);
            spawn(level, MagusSpellEntity.create(level, kind, c.self(), from, v, dmg * c.power()));
        }
    }

    /** Espiral contínua (3 s): dois braços girando. */
    public static void spiral(ServerLevel level, MagusCaster c, Kind kind, int ticks, float dmg) {
        repeat(c, ticks / 2, 2, k -> {
            Mob self = c.self();
            if (!self.isAlive()) {
                return;
            }
            Vec3 from = self.position().add(0, self.getBbHeight() * 0.55, 0);
            double a = k * 0.33;
            for (int arm = 0; arm < 3; arm++) {
                double ang = a + arm * Math.PI * 2 / 3;
                spawn(level, MagusSpellEntity.create(level, kind, self, from, new Vec3(Math.cos(ang), -0.03, Math.sin(ang)).scale(0.55),
                        dmg * c.power()));
            }
            if (k % 4 == 0) {
                level.playSound(null, self.blockPosition(), SoundEvents.BREEZE_SHOOT, SoundSource.HOSTILE, 0.8F, 1.4F);
            }
        });
    }

    /** Lâmina de vento que cruza a arena em linha reta, atravessando tudo (desviar para o lado). */
    public static void windBlade(ServerLevel level, MagusCaster c, Vec3 targetPos, Kind kind, float dmg) {
        Mob self = c.self();
        Vec3 from = self.position().add(0, 1.0, 0);
        Vec3 dir = flat(targetPos.subtract(from));
        Vec3 base = new Vec3(from.x, groundY(level, targetPos) + 1.0, from.z);
        spawn(level, MagusSpellEntity.create(level, kind, self, base, dir.scale(0.95), dmg * c.power()));
        level.playSound(null, self.blockPosition(), SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), SoundSource.HOSTILE, 2.0F, 0.6F);
    }

    /** Pisca: teleporte curto para perto (mas não colado) do alvo. */
    public static void blink(ServerLevel level, MagusCaster c, Vec3 around, double minDist, double maxDist, double height) {
        Mob self = c.self();
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, self.getX(), self.getY() + self.getBbHeight() / 2, self.getZ(), 30, 0.5, 1.0, 0.5, 0.1);
        BlockPos center = c.arenaCenter();
        for (int tries = 0; tries < 10; tries++) {
            double a = self.getRandom().nextDouble() * Math.PI * 2;
            double d = minDist + self.getRandom().nextDouble() * (maxDist - minDist);
            Vec3 p = around.add(Math.cos(a) * d, height, Math.sin(a) * d);
            if (center != null && p.subtract(Vec3.atCenterOf(center)).horizontalDistance() > MagusArena.PAINT_RADIUS - 2) {
                continue;
            }
            if (level.noCollision(self, self.getBoundingBox().move(p.subtract(self.position())))) {
                self.teleportTo(p.x, p.y, p.z);
                break;
            }
        }
        level.sendParticles(ParticleTypes.GUST, self.getX(), self.getY() + 1, self.getZ(), 1, 0, 0, 0, 0);
        level.playSound(null, self.blockPosition(), SoundEvents.BREEZE_JUMP, SoundSource.HOSTILE, 1.5F, 1.2F);
    }

    // ================================================================= ÁGUA

    /** Redemoinho: puxa para o meio e deixa lento por alguns segundos. */
    public static void whirlpool(ServerLevel level, MagusCaster c, Vec3 at, int ticks, float dmg) {
        Vec3 center = new Vec3(at.x, groundY(level, at), at.z);
        level.playSound(null, BlockPos.containing(center), SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_INSIDE, SoundSource.HOSTILE, 2.0F, 0.6F);
        repeat(c, ticks / 2, 2, k -> {
            double a = k * 0.5;
            for (int i = 0; i < 6; i++) {
                double ang = a + i * Math.PI / 3;
                double r = 4.5 - (k % 10) * 0.35;
                level.sendParticles(i % 2 == 0 ? ParticleTypes.SPLASH : ParticleTypes.BUBBLE_POP, center.x + Math.cos(ang) * r, center.y + 0.2,
                        center.z + Math.sin(ang) * r, 2, 0.1, 0.1, 0.1, 0.0);
            }
            for (Player p : players(level, c)) {
                Vec3 to = center.subtract(p.position());
                if (to.horizontalDistance() < 5.0 && Math.abs(to.y) < 3) {
                    Vec3 pull = flat(to).scale(0.09);
                    p.push(pull.x, 0, pull.z);
                    p.hurtMarked = true;
                    p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20, 1));
                    if (k % 10 == 0) {
                        hurt(level, c, p, dmg);
                    }
                }
            }
        });
    }

    /** Onda que sai do conjurador e empurra todos para a borda. */
    public static void wave(ServerLevel level, MagusCaster c, float dmg) {
        Mob self = c.self();
        Vec3 center = self.position();
        double floor = groundY(level, center);
        List<Player> hit = new ArrayList<>();
        level.playSound(null, self.blockPosition(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 1.0F, 1.6F);
        level.playSound(null, self.blockPosition(), SoundEvents.GENERIC_SPLASH, SoundSource.HOSTILE, 3.0F, 0.5F);
        for (int t = 0; t < 30; t++) {
            double r = 1.5 + t * 0.75;
            c.schedule(t, () -> {
                int n = (int) (r * 5);
                for (int i = 0; i < n; i++) {
                    double a = i * Math.PI * 2 / n;
                    level.sendParticles(ParticleTypes.SPLASH, center.x + Math.cos(a) * r, floor + 1.0, center.z + Math.sin(a) * r, 3, 0.1, 0.4, 0.1, 0.0);
                    if (i % 3 == 0) {
                        level.sendParticles(ParticleTypes.FALLING_WATER, center.x + Math.cos(a) * r, floor + 2.0, center.z + Math.sin(a) * r, 1, 0.1, 0.3, 0.1, 0.0);
                    }
                }
                for (Player p : players(level, c)) {
                    double d = p.position().subtract(center).horizontalDistance();
                    if (Math.abs(d - r) < 1.0 && p.getY() - floor < 2.5 && !hit.contains(p)) {
                        hit.add(p);
                        hurt(level, c, p, dmg);
                        fling(p, flat(p.position().subtract(center)).scale(1.6), 0.35);
                    }
                }
            });
        }
    }

    // ================================================================= FOGO

    /** Meteoros: círculo vermelho no chão 1,5 s antes; caem de bem alto. */
    public static void meteors(ServerLevel level, MagusCaster c, List<Vec3> points, Kind kind, float dmg, int warn) {
        for (int i = 0; i < points.size(); i++) {
            Vec3 at = points.get(i);
            Vec3 ground = new Vec3(at.x, groundY(level, at.add(0, 1, 0)), at.z);
            int delay = i * 6;
            double radius = kind == Kind.ROCK ? 1.8 : 3.0;
            repeat(c, warn / 5, 5, k -> c.schedule(delay, () -> warnCircle(level, ground, radius, FIRE_C)));
            c.schedule(delay + warn - 20, () -> {
                Vec3 sky = ground.add(2.0, 20.0, 1.0);
                spawn(level, MagusSpellEntity.create(level, kind, c.self(), sky, ground.subtract(sky).normalize().scale(1.0), dmg * c.power())
                        .life(60));
                level.playSound(null, BlockPos.containing(ground), SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 1.5F, 0.6F);
            });
        }
    }

    /** Pontos para meteoros: cada jogador + alguns aleatórios em volta. */
    public static List<Vec3> strikePoints(ServerLevel level, MagusCaster c, int count, double spread) {
        List<Player> ps = players(level, c);
        List<Vec3> out = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Vec3 base = ps.isEmpty() ? c.self().position() : ps.get(i % ps.size()).position();
            if (i >= ps.size()) {
                double a = c.self().getRandom().nextDouble() * Math.PI * 2;
                double d = 2 + c.self().getRandom().nextDouble() * spread;
                base = base.add(Math.cos(a) * d, 0, Math.sin(a) * d);
            }
            out.add(base);
        }
        return out;
    }

    /** Sopro de chamas em cone (segue o alvo devagar). */
    public static void breath(ServerLevel level, MagusCaster c, Supplier<Vec3> aim, int ticks, float dmg) {
        repeat(c, ticks / 2, 2, k -> {
            Mob self = c.self();
            if (!self.isAlive()) {
                return;
            }
            Vec3 from = self.position().add(0, self.getBbHeight() * 0.75, 0);
            Vec3 dir = aim.get().subtract(from).normalize();
            for (int i = 0; i < 6; i++) {
                Vec3 v = dir.add((self.getRandom().nextDouble() - 0.5) * 0.4, (self.getRandom().nextDouble() - 0.5) * 0.2,
                        (self.getRandom().nextDouble() - 0.5) * 0.4).scale(0.9);
                level.sendParticles(ParticleTypes.FLAME, from.x, from.y, from.z, 0, v.x, v.y, v.z, 1.0);
            }
            if (k % 3 == 0) {
                level.playSound(null, self.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 1.0F, 0.6F);
            }
            if (k % 2 == 0) {
                for (Player p : players(level, c)) {
                    Vec3 to = p.position().add(0, 1, 0).subtract(from);
                    if (to.length() < 10 && to.normalize().dot(dir) > 0.8) {
                        hurt(level, c, p, dmg);
                        p.igniteForSeconds(4.0F);
                    }
                }
            }
        });
    }

    /** Rachaduras de lava que correm pelo chão até cada jogador. */
    public static void lavaCracks(ServerLevel level, MagusCaster c, float dmg) {
        Mob self = c.self();
        Vec3 start = self.position();
        MagusArena.Field field = c.field();
        level.playSound(null, self.blockPosition(), SoundEvents.LAVA_AMBIENT, SoundSource.HOSTILE, 2.5F, 0.5F);
        for (Player target : players(level, c)) {
            Vec3 dir = flat(target.position().subtract(start));
            for (int i = 1; i <= 20; i++) {
                Vec3 p = start.add(dir.scale(i * 1.1));
                int k = i;
                c.schedule(i, () -> {
                    double y = groundY(level, p.add(0, 1.5, 0));
                    BlockPos floor = BlockPos.containing(p.x, y - 0.5, p.z);
                    if (field != null) {
                        field.temp(level, floor, Blocks.MAGMA_BLOCK.defaultBlockState(), 100 + k * 2, true);
                    }
                    level.sendParticles(ParticleTypes.LAVA, p.x, y + 0.1, p.z, 2, 0.2, 0, 0.2, 0);
                    level.sendParticles(ParticleTypes.FLAME, p.x, y + 0.2, p.z, 4, 0.3, 0.2, 0.3, 0.02);
                    for (Player pl : players(level, c)) {
                        if (pl.position().subtract(p).horizontalDistance() < 1.2 && pl.getY() - y < 1.5) {
                            hurt(level, c, pl, dmg);
                            pl.igniteForSeconds(3.0F);
                        }
                    }
                });
            }
        }
    }

    /** Poça de magma temporária no chão. */
    public static void magmaPatch(ServerLevel level, @Nullable MagusCaster c, BlockPos floor, int r, int ticks) {
        MagusArena.Field field = c == null ? null : c.field();
        if (field == null) {
            return;
        }
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz <= r * r + 1) {
                    field.temp(level, floor.offset(dx, 0, dz), Blocks.MAGMA_BLOCK.defaultBlockState(), ticks, true);
                }
            }
        }
    }

    // ================================================================= MISTURAS (Fase II)

    /** Vapor: nuvem escaldante (dano contínuo, um pouco de cegueira). */
    public static void steam(ServerLevel level, MagusCaster c, Vec3 at, double radius, int ticks, float dmg) {
        Vec3 center = new Vec3(at.x, groundY(level, at.add(0, 1, 0)), at.z);
        level.playSound(null, BlockPos.containing(center), SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 2.0F, 0.5F);
        repeat(c, ticks / 4, 4, k -> {
            level.sendParticles(ParticleTypes.CLOUD, center.x, center.y + 1.2, center.z, 14, radius * 0.6, 0.8, radius * 0.6, 0.01);
            level.sendParticles(ParticleTypes.WHITE_ASH, center.x, center.y + 1.0, center.z, 6, radius * 0.6, 0.6, radius * 0.6, 0.0);
            if (k % 5 == 0) {
                for (Player p : players(level, c)) {
                    if (p.position().subtract(center).horizontalDistance() < radius && Math.abs(p.getY() - center.y) < 3) {
                        hurt(level, c, p, dmg);
                        p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 0));
                    }
                }
            }
        });
    }

    /** Tempestade: marca o alvo e um raio cai em cima. */
    public static void lightning(ServerLevel level, MagusCaster c, Vec3 at, float dmg) {
        Vec3 ground = new Vec3(at.x, groundY(level, at.add(0, 1, 0)), at.z);
        repeat(c, 6, 5, k -> warnCircle(level, ground, 2.2, 0xFFF27A));
        c.schedule(30, () -> {
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level, EntitySpawnReason.EVENT);
            if (bolt != null) {
                bolt.snapTo(ground.x, ground.y, ground.z);
                bolt.setVisualOnly(true);
                level.addFreshEntity(bolt);
            }
            for (Player p : players(level, c)) {
                if (p.position().subtract(ground).horizontalDistance() < 2.5 && Math.abs(p.getY() - ground.y) < 4) {
                    hurt(level, c, p, dmg);
                    p.igniteForSeconds(2.0F);
                }
            }
        });
    }

    /** Vida: raízes brotam embaixo do alvo e prendem por 1,5 s. */
    public static void roots(ServerLevel level, MagusCaster c, Vec3 at, float dmg) {
        Vec3 ground = new Vec3(at.x, groundY(level, at.add(0, 1, 0)), at.z);
        repeat(c, 4, 5, k -> warnCircle(level, ground, 1.8, 0x5A8A2A));
        c.schedule(20, () -> {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ROOTED_DIRT.defaultBlockState()),
                    ground.x, ground.y + 0.5, ground.z, 30, 0.8, 0.6, 0.8, 0.1);
            level.sendParticles(ParticleTypes.COMPOSTER, ground.x, ground.y + 1, ground.z, 15, 0.8, 0.8, 0.8, 0.0);
            level.playSound(null, BlockPos.containing(ground), SoundEvents.HANGING_ROOTS_PLACE, SoundSource.HOSTILE, 2.0F, 0.5F);
            for (Player p : players(level, c)) {
                if (p.position().subtract(ground).horizontalDistance() < 1.9 && Math.abs(p.getY() - ground.y) < 2.5) {
                    hurt(level, c, p, dmg);
                    p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 30, 6));
                    p.setDeltaMovement(Vec3.ZERO);
                    p.hurtMarked = true;
                }
            }
        });
    }

    /** Areia: tempestade de areia que deixa todos lentos e com pouca visão por alguns segundos. */
    public static void sandstorm(ServerLevel level, MagusCaster c, int ticks) {
        repeat(c, ticks / 5, 5, k -> {
            for (Player p : players(level, c)) {
                level.sendParticles(new DustParticleOptions(0xE8C77A, 2.0F), p.getX(), p.getY() + 1, p.getZ(), 10, 3.0, 1.5, 3.0, 0.0);
                if (k % 4 == 0) {
                    p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 25, 0));
                    p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 30, 0, false, false));
                }
            }
            if (k % 6 == 0) {
                level.playSound(null, c.self().blockPosition(), SoundEvents.SAND_FALL, SoundSource.HOSTILE, 2.0F, 0.6F);
            }
        });
    }

    /** Chama Viva: tornados de fogo que perseguem cada jogador. */
    public static void fireTornadoes(ServerLevel level, MagusCaster c, float dmg) {
        Mob self = c.self();
        for (Player p : players(level, c)) {
            Vec3 dir = flat(p.position().subtract(self.position()));
            Vec3 from = new Vec3(self.getX(), groundY(level, self.position().add(0, 1, 0)), self.getZ()).add(dir.scale(2));
            spawn(level, MagusSpellEntity.create(level, Kind.FIRE_TORNADO, self, from, dir.scale(0.22), dmg * c.power()).target(p));
        }
        level.playSound(null, self.blockPosition(), SoundEvents.BLAZE_AMBIENT, SoundSource.HOSTILE, 2.0F, 0.5F);
    }

    // ================================================================= QUINTESSÊNCIA (Fase IV)

    /**
     * Raio do ∞: um feixe sai do cajado e risca no chão um oito (lemniscata) em volta do centro. Quem estiver no
     * caminho do ponto que queima leva dano (uma vez a cada meio segundo).
     */
    public static void infinityRay(ServerLevel level, MagusCaster c, Vec3 center, double floorY, int ticks, float dmg) {
        int[] last = new int[1];
        java.util.Map<java.util.UUID, Integer> hits = new java.util.HashMap<>();
        double a = 15.0;
        for (int t = 0; t < ticks; t++) {
            int k = t;
            c.schedule(t, () -> {
                Mob self = c.self();
                if (!self.isAlive()) {
                    return;
                }
                double th = k / (double) ticks * Math.PI * 2 + Math.PI / 2;
                double den = 1 + Math.sin(th) * Math.sin(th);
                Vec3 p = new Vec3(center.x + a * Math.cos(th) / den, floorY + 1.05, center.z + a * Math.sin(th) * Math.cos(th) / den * 1.3);
                Vec3 from = self.position().add(0, self.getBbHeight() + 0.5, 0);
                Vec3 seg = p.subtract(from);
                int n = (int) (seg.length() * 2);
                for (int i = 0; i < n; i += 2) {
                    Vec3 q = from.add(seg.scale(i / (double) n));
                    level.sendParticles(new DustParticleOptions(GOLD_C, 1.3F), q.x, q.y, q.z, 1, 0, 0, 0, 0);
                }
                level.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 3, 0.2, 0.1, 0.2, 0.02);
                level.sendParticles(ParticleTypes.FLAME, p.x, p.y - 0.4, p.z, 2, 0.3, 0.0, 0.3, 0.0);
                if (k % 10 == 0) {
                    level.playSound(null, BlockPos.containing(p), SoundEvents.BEACON_AMBIENT, SoundSource.HOSTILE, 2.0F, 1.6F);
                }
                for (Player pl : players(level, c)) {
                    if (pl.position().subtract(p).horizontalDistance() < 1.7 && Math.abs(pl.getY() - floorY - 1) < 2.0) {
                        Integer prev = hits.get(pl.getUUID());
                        if (prev == null || k - prev >= 10) {
                            hits.put(pl.getUUID(), k);
                            hurt(level, c, pl, dmg);
                        }
                    }
                }
                last[0] = k;
            });
        }
    }

    /** "Como embaixo": um golpe vem do outro plano e atravessa o chão onde está o aviso. */
    public static void mirrorStrike(ServerLevel level, MagusCaster c, Vec3 at, double floorY, double otherY, float dmg) {
        Vec3 ground = new Vec3(at.x, floorY + 1.0, at.z);
        repeat(c, 6, 5, k -> warnCircle(level, ground.add(0, -0.9, 0), 1.8, GOLD_C));
        c.schedule(30, () -> {
            double y0 = Math.min(floorY + 1, otherY), y1 = Math.max(floorY + 1, otherY);
            for (double y = y0; y <= y1; y += 0.6) {
                level.sendParticles(ParticleTypes.END_ROD, ground.x, y, ground.z, 1, 0.15, 0, 0.15, 0.0);
            }
            level.sendParticles(net.minecraft.core.particles.ColorParticleOption.create(ParticleTypes.FLASH, 0xFFFFE08A), ground.x, ground.y, ground.z, 1, 0, 0, 0, 0);
            level.playSound(null, BlockPos.containing(ground), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 2.5F, 0.5F);
            for (Player p : players(level, c)) {
                if (p.position().subtract(ground).horizontalDistance() < 2.0 && Math.abs(p.getY() - ground.y) < 3) {
                    hurt(level, c, p, dmg);
                    fling(p, Vec3.ZERO, otherY > floorY ? 0.9 : -0.3);
                }
            }
        });
    }
}

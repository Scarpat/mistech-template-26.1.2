package com.mistech.mistechmod.spellcraft.client.fx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.spellcraft.fx.DreamFx;
import com.mistech.mistechmod.spellcraft.fx.DreamFxPayload;
import com.mistech.mistechmod.spellcraft.fx.VfxContent;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

/**
 * Coreografia dos efeitos oníricos das magias (ver {@link DreamFx}). Cada magia tem a sua cena, montada com
 * {@link DreamParticle}: mariposas de brasa, olhos que abrem e encaram, uma pálpebra rasgando o ar, um mostrador de
 * algarismos andando aos trancos, um jardim que respira, rodas de olhos. Nada aqui muda a regra da magia.
 */
@EventBusSubscriber(modid = Mistech.MODID, value = Dist.CLIENT)
public final class DreamFxClient {
    private static final RandomSource RND = RandomSource.create();
    /** Peças em voo de cada disparo (somem quando chega o impacto). */
    private static final Map<Integer, List<DreamParticle>> FLYERS = new HashMap<>();
    /** Mostrador da Lentidão Temporal de cada mago. */
    private static final Map<Integer, Dial> DIALS = new HashMap<>();

    private DreamFxClient() {}

    @SubscribeEvent
    static void onPayloads(RegisterClientPayloadHandlersEvent event) {
        event.register(DreamFxPayload.TYPE, (payload, context) -> play(payload));
    }

    private static void play(DreamFxPayload p) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || !DreamSprites.ready()) {
            return;
        }
        Vec3 a = new Vec3(p.ax(), p.ay(), p.az());
        Vec3 b = new Vec3(p.bx(), p.by(), p.bz());
        switch (p.kind()) {
            case DreamFx.MOTH_LAUNCH -> mothLaunch(level, p.id(), a, b, p.power());
            case DreamFx.MOTH_HIT -> mothHit(level, p.id(), a);
            case DreamFx.EYE_LAUNCH -> eyeLaunch(level, p.id(), a, b, p.power());
            case DreamFx.EYE_HIT -> eyeHit(level, p.id(), a, p.entityId());
            case DreamFx.BLINK -> blink(level, a, b);
            case DreamFx.TIME_PULSE -> timePulse(level, p.entityId(), a, p.power());
            case DreamFx.BLOOM -> bloom(level, p.entityId(), a);
            case DreamFx.HALO -> halo(level, p.entityId(), a, p.power());
            case DreamFx.COCOON -> cocoon(level, a, b, (int) p.power());
            case DreamFx.COCOON_HIT -> cocoonHit(level, a, p.power());
            case DreamFx.CIRANDA -> ciranda(level, a, p.power(), 10, 0, 24);
            case DreamFx.SUN_FALL -> sunFall(level, a, (int) p.power(), (float) b.x, (float) b.y);
            case DreamFx.EGG -> egg(level, a);
            case DreamFx.INFERNO -> inferno(level, a, p.power());
            case DreamFx.VEIL -> veil(level, a);
            case DreamFx.MOUTH_LAUNCH -> mouthLaunch(level, p.id(), a, b, p.power());
            case DreamFx.MOUTH_HIT -> mouthHit(level, p.id(), a);
            case DreamFx.FEED -> feed(level, a, p.entityId());
            case DreamFx.PUDDLE -> puddle(level, a);
            case DreamFx.ECLIPSE -> eclipse(level, a, p.power());
            case DreamFx.SHARD_LAUNCH -> shardLaunch(level, p.id(), a, b, p.power());
            case DreamFx.SHARD_HIT -> shardHit(level, p.id(), a);
            case DreamFx.FROST_NOVA -> frostNova(level, a, p.power());
            case DreamFx.BLIZZARD -> blizzard(level, p.entityId(), a, p.power());
            case DreamFx.ICE_AGE -> iceAge(level, a, p.power());
            case DreamFx.FISH_LAUNCH -> fishLaunch(level, p.id(), a, b, p.power());
            case DreamFx.FISH_HIT -> fishHit(level, p.id(), a);
            case DreamFx.MEND -> mend(level, p.entityId(), a);
            case DreamFx.PURIFY -> purify(level, a, p.power());
            case DreamFx.WHALE -> whale(level, a, b, (int) p.power());
            case DreamFx.HEAD_LAUNCH -> headLaunch(level, p.id(), a, b, p.power());
            case DreamFx.HEAD_HIT -> headHit(level, p.id(), a);
            case DreamFx.STONE_SKIN -> stoneSkin(level, p.entityId(), a);
            case DreamFx.GOLEM_RISE -> golemRise(level, a);
            case DreamFx.QUAKE -> quake(level, a, p.power());
            case DreamFx.CATACLYSM -> cataclysm(level, a, p.power());
            case DreamFx.GUST -> gust(level, a, b, p.power());
            case DreamFx.UPDRAFT -> updraft(level, p.entityId(), a);
            case DreamFx.WIND_STEP -> windStep(level, p.entityId(), a, b);
            case DreamFx.HURRICANE -> hurricane(level, a, p.power());
            case DreamFx.STITCH -> stitch(level, a, b, p.id(), p.power(), 16);
            case DreamFx.NEEDLE_FALL -> needleFall(level, p.entityId(), a, (int) b.x, p.power());
            case DreamFx.STORM_WEB -> stormWeb(level, a, p.power());
            case DreamFx.GLIMMER -> glimmer(level, p.entityId(), a, p.power());
            case DreamFx.GLIMMER_MARK -> glimmerMark(level, p.entityId(), a);
            case DreamFx.LIGHT_ORB -> lightOrb(level, a);
            case DreamFx.DAWN -> dawn(level, p.entityId(), a, p.power());
            case DreamFx.QUICKEN -> quicken(level, p.entityId(), a);
            case DreamFx.FORESIGHT -> foresight(level, p.entityId(), a);
            case DreamFx.REWIND -> rewind(level, a, b);
            case DreamFx.TIME_STOP -> timeStop(level, p.entityId(), a, p.power());
            case DreamFx.TIME_MARK -> timeMark(level, p.entityId(), a, (int) p.power());
            case DreamFx.ETERNAL -> eternal(level, p.entityId(), a);
            case DreamFx.PULL -> pull(level, a, b);
            case DreamFx.TELEKINESIS -> telekinesis(level, p.entityId(), a, p.power());
            case DreamFx.ANCHOR_SET -> anchorSet(level, a);
            case DreamFx.RIFT -> rift(level, a, b);
            case DreamFx.BLACK_HOLE -> blackHole(level, a, p.power(), (int) b.x);
            case DreamFx.SPROUT -> sprout(level, p.entityId(), a, p.power());
            case DreamFx.VINE -> vine(level, a, b, (int) p.power(), p.id());
            case DreamFx.VINE_LATCH -> vineLatch(level, a);
            case DreamFx.THORNS -> thorns(level, a, p.power());
            case DreamFx.THORN_BIND -> thornBind(level, p.entityId(), a, (int) p.power());
            case DreamFx.POD -> pod(level, a);
            case DreamFx.TREE -> tree(level, a, p.power(), (int) b.x);
            case DreamFx.LAVA_RIVER -> lavaRiver(level, a, b, (int) p.power());
            case DreamFx.ERUPTION -> eruption(level, a, p.power());
            case DreamFx.MAGMA_ARMOR -> magmaArmor(level, p.entityId(), a);
            case DreamFx.MIST -> mist(level, p.entityId(), a, (int) p.power());
            case DreamFx.STEAM_JET -> steamJet(level, a, b, p.power());
            case DreamFx.SHEEP_PLATFORM -> sheepPlatform(level, a);
            case DreamFx.SHEEP_BRIDGE -> sheepBridge(level, a, b, (int) p.power());
            case DreamFx.CANDLE -> candleSummon(level, a);
            case DreamFx.SOUL_TAKE -> soulTake(level, a, p.entityId(), b);
            case DreamFx.SOUL_HARVEST -> soulHarvest(level, p.entityId(), a, p.power());
            case DreamFx.RECHARGE -> recharge(level, p.entityId(), a);
            case DreamFx.MACHINE_BOOST -> machineBoost(level, a, p.power() > 0.5F);
            case DreamFx.ARCANE_FOCUS -> arcaneFocus(level, p.entityId(), a);
            case DreamFx.VIGIL -> vigil(level, p.entityId(), a);
            case DreamFx.VIGIL_MARK -> vigilMark(level, p.entityId(), a);
            case DreamFx.COIN -> coin(level, p.entityId(), a, p.power() > 0.5F);
            case DreamFx.ECHO -> echo(level, p.entityId(), a);
            case DreamFx.VOID_LAUNCH -> voidLaunch(level, p.id(), a, b, p.power());
            case DreamFx.VOID_HIT -> voidHit(level, p.id(), a, p.entityId());
            case DreamFx.CHARGE -> charge(level, p.id(), p.entityId(), a, p.power());
            case DreamFx.CHARGE_FULL -> chargeFull(level, p.id(), p.entityId(), a);
            case DreamFx.RITUAL -> ritual(level, p.id(), p.entityId(), a, (int) p.power());
            default -> { }
        }
    }

    // ------------------------------------------------------------------ ferramentas

    private static DreamParticle spawn(ClientLevel level, Vec3 pos, int frame, int life, int facing, DreamParticle.Script script) {
        DreamParticle particle = new DreamParticle(level, pos, frame, life, facing, script);
        Minecraft.getInstance().particleEngine.add(particle);
        return particle;
    }

    /** O roteiro só começa depois de {@code delay} ticks (até lá a peça fica invisível). */
    private static DreamParticle.Script after(int delay, DreamParticle.Script script) {
        return (p, age) -> {
            if (age < delay) {
                p.alpha(0.0F);
            } else {
                script.run(p, age - delay);
            }
        };
    }

    private static float rf() {
        return RND.nextFloat();
    }

    private static double rs(double spread) {
        return (RND.nextDouble() * 2.0 - 1.0) * spread;
    }

    /** Dois vetores perpendiculares a {@code dir} (e entre si). */
    private static Vec3[] basis(Vec3 dir) {
        Vec3 up = Math.abs(dir.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 u = dir.cross(up).normalize();
        return new Vec3[] {u, dir.cross(u).normalize()};
    }

    private static @Nullable Entity entity(ClientLevel level, int id) {
        return id < 0 ? null : level.getEntity(id);
    }

    /** Altura do chão logo abaixo do ponto (NaN se não achar em 5 blocos). */
    private static double ground(ClientLevel level, double x, double y, double z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dy = 2; dy >= -5; dy--) {
            pos.set(Mth.floor(x), Mth.floor(y) + dy, Mth.floor(z));
            VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos);
            if (!shape.isEmpty()) {
                double top = pos.getY() + shape.max(Direction.Axis.Y);
                if (top <= y + 2.5) {
                    return top;
                }
            }
        }
        return Double.NaN;
    }

    private static void track(int id, DreamParticle particle) {
        FLYERS.values().removeIf(list -> list.stream().noneMatch(DreamParticle::isAlive));
        FLYERS.computeIfAbsent(id, k -> new ArrayList<>()).add(particle);
    }

    private static void land(int id) {
        List<DreamParticle> list = FLYERS.remove(id);
        if (list != null) {
            list.forEach(DreamParticle::kill);
        }
    }

    /** Clarão macio que cresce e some. */
    private static void pulse(ClientLevel level, Vec3 pos, int rgb, float from, float to, float alpha, int life) {
        spawn(level, pos, DreamSprites.GLOW, life, DreamParticle.BILLBOARD, (p, age) -> {
            float t = age / (float) life;
            p.tint(rgb).size(Mth.lerp(t, from, to)).alpha(alpha * (1.0F - t));
        });
    }

    // ------------------------------------------------------------------ Fagulha: mariposas de brasa

    private static void ash(ClientLevel level, Vec3 from) {
        int life = 9 + RND.nextInt(7);
        double dx = rs(0.012), dz = rs(0.012);
        float spin = (rf() - 0.5F) * 0.5F;
        spawn(level, from, DreamSprites.ASH, life, DreamParticle.BILLBOARD, (p, age) -> {
            float t = age / (float) life;
            p.at(from.x + dx * age, from.y - 0.003 * age * age, from.z + dz * age).size(0.075F * (1.0F - t * 0.7F)).alpha(1.0F - t * t).roll(spin * age);
        });
    }

    private static void mothLaunch(ClientLevel level, int id, Vec3 start, Vec3 velocity, float range) {
        double speed = Math.max(0.1, velocity.length());
        int life = (int) Math.ceil(range / speed) + 1;
        Vec3[] uw = basis(velocity.normalize());
        for (int i = 0; i < 5; i++) {
            final int k = i;
            double phase = Math.PI * 2 * i / 5.0;
            double radius = 0.2 + rf() * 0.22;
            double turn = 0.65 + rf() * 0.45;
            track(id, spawn(level, start, DreamSprites.MOTH, life, DreamParticle.BILLBOARD, (p, age) -> {
                double ang = phase + age * turn;
                double r = radius * Math.min(1.0, age / 3.0);
                Vec3 c = start.add(velocity.scale(age));
                p.at(c.add(uw[0].scale(Math.cos(ang) * r)).add(uw[1].scale(Math.sin(ang) * r)));
                p.frame(DreamSprites.MOTH + ((age + k) / 2) % 2).size(0.2F).alpha(1.0F).roll((float) Math.sin(age * 0.8 + phase) * 0.4F);
                if ((age + k) % 2 == 0) {
                    ash(level, p.pos());
                }
            }));
        }
        track(id, spawn(level, start, DreamSprites.GLOW, life, DreamParticle.BILLBOARD,
                (p, age) -> p.at(start.add(velocity.scale(age))).tint(0xFF8A2A).size(0.42F + 0.06F * Mth.sin(age * 1.3F)).alpha(0.4F)));
    }

    private static void mothHit(ClientLevel level, int id, Vec3 at) {
        land(id);
        pulse(level, at, 0xFF8A2A, 0.25F, 1.5F, 0.8F, 6);
        scatter(level, at, 11, 0.24);
        for (int i = 0; i < 8; i++) {
            level.addParticle(VfxContent.ember(0xFF8A2A), at.x, at.y, at.z, rs(0.12), 0.04 + rf() * 0.1, rs(0.12));
        }
    }

    /** Mariposas que se espalham batendo asa, queimam e caem como cinza. */
    private static void scatter(ClientLevel level, Vec3 at, int count, double speed) {
        for (int i = 0; i < count; i++) {
            final int k = i;
            int life = 18 + RND.nextInt(12);
            double[] pos = {at.x, at.y, at.z};
            double[] vel = {rs(speed), 0.05 + rf() * speed * 0.8, rs(speed)};
            spawn(level, at, DreamSprites.MOTH, life, DreamParticle.BILLBOARD, (p, age) -> {
                boolean burnt = age > life * 0.55;
                vel[0] = vel[0] * 0.86 + rs(0.035);
                vel[1] = vel[1] * 0.86 + (burnt ? -0.022 : 0.012 + rs(0.02));
                vel[2] = vel[2] * 0.86 + rs(0.035);
                pos[0] += vel[0];
                pos[1] += vel[1];
                pos[2] += vel[2];
                float t = age / (float) life;
                p.at(pos[0], pos[1], pos[2]);
                if (burnt) {
                    p.frame(DreamSprites.ASH).size(0.09F).alpha(1.0F - (t - 0.55F) / 0.45F).roll(age * 0.3F);
                } else {
                    p.frame(DreamSprites.MOTH + ((age + k) / 2) % 2).size(0.19F).alpha(1.0F).roll((float) Math.sin(age * 0.9 + k) * 0.6F);
                }
            });
        }
    }

    // ------------------------------------------------------------------ Penumbra: o olhar

    private static void tear(ClientLevel level, Vec3 from) {
        tear(level, from, 0xFFFFFF);
    }

    private static void tear(ClientLevel level, Vec3 from, int rgb) {
        int life = 12 + RND.nextInt(6);
        spawn(level, from, DreamSprites.DRIP, life, DreamParticle.BILLBOARD, (p, age) -> {
            float t = age / (float) life;
            p.at(from.x, from.y - 0.006 * age * age, from.z).tint(rgb).size(0.07F).alpha(1.0F - t * t);
        });
    }

    /** Quadro do olho que abre em 4 ticks, encara por {@code stare}, pisca uma vez em {@code blinkAt} e fecha. -1 = sumiu. */
    private static int eyeFrame(int t, int stare, int blinkAt) {
        if (t < 4) {
            return t + 1;
        }
        int closing = t - (4 + stare);
        if (closing >= 0) {
            return 3 - closing;
        }
        int blink = t - blinkAt;
        if (blink >= 0 && blink < 4) {
            return new int[] {2, 0, 0, 3}[blink];
        }
        return 4;
    }

    private static void eyeLaunch(ClientLevel level, int id, Vec3 start, Vec3 velocity, float range) {
        double speed = Math.max(0.1, velocity.length());
        int life = (int) Math.ceil(range / speed) + 1;
        Vec3 dir = velocity.normalize();
        Vec3[] uw = basis(dir);
        // o olho que abre junto do mago e fica vendo o outro ir
        Vec3 watcher = start.add(uw[0].scale(0.55)).add(0, 0.7, 0);
        spawn(level, watcher, DreamSprites.EYE, 24, DreamParticle.FIXED, (p, age) -> {
            int f = eyeFrame(age, 14, 9);
            p.face(dir).frame(DreamSprites.EYE + Math.max(0, f)).size(age < 3 ? 0.2F + age * 0.04F : 0.3F).alpha(f < 0 ? 0.0F : 1.0F);
        });
        track(id, spawn(level, start, DreamSprites.GLOW, life, DreamParticle.BILLBOARD,
                (p, age) -> p.at(start.add(velocity.scale(age))).tint(0x4B2380).size(0.4F).alpha(0.6F)));
        track(id, spawn(level, start, DreamSprites.EYE + 3, life, DreamParticle.BILLBOARD, (p, age) -> {
            p.at(start.add(velocity.scale(age))).size(0.2F).alpha(1.0F).roll((float) Math.sin(age * 0.7) * 0.25F);
            tear(level, p.pos());
        }));
    }

    private static void eyeHit(ClientLevel level, int id, Vec3 at, int targetId) {
        land(id);
        pulse(level, at, 0x3A1A5C, 0.3F, 1.6F, 0.75F, 8);
        boolean hasTarget = entity(level, targetId) != null;
        int count = hasTarget ? 7 : 3;
        for (int i = 0; i < count; i++) {
            double ang = Math.PI * 2 * i / count + rs(0.3);
            double height = hasTarget ? -0.4 + rf() * 1.9 : 0.3 + rf() * 0.9;
            int delay = i * 2 + RND.nextInt(2);
            int stare = hasTarget ? 30 + RND.nextInt(10) : 12 + RND.nextInt(6);
            int blinkAt = 8 + RND.nextInt(Math.max(1, stare - 12));
            float size = 0.3F + rf() * 0.12F;
            double bobPhase = rf() * 6.28;
            Vec3[] centre = {at};
            double[] dist = {1.3 + rf() * 0.7};
            spawn(level, at, DreamSprites.EYE, delay + stare + 10, DreamParticle.FIXED, after(delay, (p, t) -> {
                Entity target = entity(level, targetId);
                if (target != null && target.isAlive()) {
                    centre[0] = target.position().add(0, target.getBbHeight() * 0.55, 0);
                    if (t == 0) {
                        dist[0] += target.getBbWidth() * 0.5;
                    }
                }
                Vec3 c = centre[0];
                Vec3 pos = c.add(Math.cos(ang) * dist[0], height + Math.sin(t * 0.12 + bobPhase) * 0.05, Math.sin(ang) * dist[0]);
                int f = eyeFrame(t, stare, blinkAt);
                p.at(pos).face(c.subtract(pos)).frame(DreamSprites.EYE + Math.max(0, f)).size(t < 2 ? size * 1.2F : size).alpha(f < 0 ? 0.0F : 1.0F);
                if (f == 3 && t > 4 + stare - 1) {
                    tear(level, pos.subtract(0, size * 0.5, 0)); // ao fechar, escorre
                }
            }));
        }
    }

    // ------------------------------------------------------------------ Piscar: a pálpebra

    private static void blink(ClientLevel level, Vec3 from, Vec3 to) {
        Vec3 flat = new Vec3(to.x - from.x, 0, to.z - from.z);
        Vec3 dir = flat.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : flat.normalize();
        Vec3 ca = from.add(0, 1.0, 0);
        Vec3 cb = to.add(0, 1.0, 0);
        int[] closing = {2, 3, 4, 4, 4, 4, 3, 3, 2, 2, 1, 1, 0, 0};
        int[] opening = {1, 3, 4, 4, 4, 4, 3, 3, 2, 2, 1, 0};
        spawn(level, ca, DreamSprites.SLIT, closing.length, DreamParticle.FIXED, (p, age) -> {
            int i = Math.min(age, closing.length - 1);
            p.face(dir).frame(DreamSprites.SLIT + closing[i]).size(1.25F).alpha(age >= closing.length - 2 ? 0.5F : 1.0F);
        });
        spawn(level, cb, DreamSprites.SLIT, opening.length, DreamParticle.FIXED, (p, age) -> {
            int i = Math.min(age, opening.length - 1);
            p.face(dir).frame(DreamSprites.SLIT + opening[i]).size(1.25F).alpha(age >= opening.length - 2 ? 0.5F : 1.0F);
        });
        pulse(level, ca, 0xC050FF, 1.2F, 0.2F, 0.5F, 7);
        pulse(level, cb, 0xC050FF, 0.2F, 1.4F, 0.6F, 9);
        // as estrelas de dentro vazam de uma pálpebra para a outra
        for (int i = 0; i < 28; i++) {
            Vec3 start = ca.add(rs(0.25), rs(0.9), rs(0.25));
            Vec3 end = cb.add(rs(0.25), rs(0.9), rs(0.25));
            Vec3 drift = new Vec3(rs(0.05), 0.01 + rf() * 0.04, rs(0.05));
            int delay = RND.nextInt(6);
            int travel = 4 + RND.nextInt(4);
            float size = 0.05F + rf() * 0.06F;
            float spin = (rf() - 0.5F) * 0.6F;
            spawn(level, start, DreamSprites.STAR, delay + travel + 12, DreamParticle.BILLBOARD, after(delay, (p, t) -> {
                if (t <= travel) {
                    float f = t / (float) travel;
                    f = f * f * (3.0F - 2.0F * f);
                    p.at(start.lerp(end, f)).size(size).alpha(1.0F);
                } else {
                    int out = t - travel;
                    p.at(end.add(drift.scale(out))).size(size * (1.0F - out / 14.0F)).alpha(1.0F - out / 12.0F);
                }
                p.roll(spin * t);
            }));
        }
    }

    // ------------------------------------------------------------------ Lentidão Temporal: a hora parada

    private static final class Dial {
        int age;
        int keepUntil;
        int step;
        float angle;
        Vec3 centre = Vec3.ZERO;
    }

    private static void timePulse(ClientLevel level, int casterId, Vec3 at, float radius) {
        Dial old = DIALS.get(casterId);
        if (old != null && old.age <= old.keepUntil) {
            old.step++;
            old.keepUntil = old.age + 16;
        } else {
            Dial dial = new Dial();
            dial.keepUntil = 16;
            dial.centre = at.add(0, 1.5, 0);
            DIALS.put(casterId, dial);
            for (int i = 0; i < 12; i++) {
                final int k = i;
                spawn(level, dial.centre, DreamSprites.NUMERAL + ((i + 11) % 12), 20 * 120, DreamParticle.BILLBOARD, (p, age) -> {
                    if (k == 0) {
                        // a primeira peça move o mostrador: um tranco de 30° a cada pulso, como ponteiro de relógio
                        dial.age = age;
                        dial.angle += (dial.step * 30.0F - dial.angle) * 0.6F;
                        Entity caster = entity(level, casterId);
                        if (caster != null) {
                            dial.centre = caster.position().add(0, 1.5, 0);
                        }
                    }
                    int left = dial.keepUntil - dial.age;
                    if (left < 0) {
                        p.kill();
                        return;
                    }
                    double a = Math.toRadians(k * 30.0 + dial.angle);
                    p.at(dial.centre.add(Math.sin(a) * 2.6, Math.sin(age * 0.1 + k) * 0.04, -Math.cos(a) * 2.6));
                    p.size(k == 0 ? 0.3F : 0.24F).alpha(Math.min(1.0F, age / 4.0F) * Math.min(1.0F, left / 5.0F));
                });
            }
        }
        // areia que cai, para no ar e depois sobe de volta
        double spread = Math.min(radius, 7.0);
        for (int i = 0; i < 18; i++) {
            Vec3 start = at.add(rs(spread), 0.5 + rf() * 2.8, rs(spread));
            int fall = 3 + RND.nextInt(4);
            int hang = 16 + RND.nextInt(8);
            spawn(level, start, DreamSprites.GRAIN, fall + hang + 8, DreamParticle.BILLBOARD, (p, age) -> {
                if (age <= fall) {
                    p.at(start.x, start.y - 0.11 * age, start.z).size(0.05F).alpha(age / 2.0F);
                } else if (age <= fall + hang) {
                    p.at(start.x, start.y - 0.11 * fall, start.z).size(0.05F).alpha(0.75F + 0.25F * Mth.sin(age * 0.9F + p.r1 * 6.0F));
                } else {
                    int up = age - fall - hang;
                    p.at(start.x, start.y - 0.11 * fall + 0.02 * up * up, start.z).size(0.05F).alpha(1.0F - up / 8.0F);
                }
            });
        }
    }

    // ------------------------------------------------------------------ Floração: o jardim que respira

    private static void petal(ClientLevel level, Vec3 from) {
        int life = 30 + RND.nextInt(10);
        double phase = rf() * 6.28;
        double sway = 0.15 + rf() * 0.2;
        spawn(level, from, DreamSprites.PETAL, life, DreamParticle.BILLBOARD, (p, age) -> {
            float t = age / (float) life;
            p.at(from.x + Math.sin(age * 0.18 + phase) * sway * t, from.y + 0.035 * age, from.z + Math.cos(age * 0.15 + phase) * sway * t);
            p.size(0.07F).alpha(t > 0.65F ? 1.0F - (t - 0.65F) / 0.35F : 1.0F).roll((float) (age * 0.12 + phase));
        });
    }

    private static void bloom(ClientLevel level, int casterId, Vec3 at) {
        for (int i = 0; i < 16; i++) {
            double r = 0.7 + 0.2 * i;
            double ang = i * 2.39996;
            double x = at.x + Math.cos(ang) * r;
            double z = at.z + Math.sin(ang) * r;
            double y = ground(level, x, at.y, z);
            if (Double.isNaN(y)) {
                continue;
            }
            int delay = (int) (i * 1.5);
            float big = 0.26F + rf() * 0.1F;
            Vec3 base = new Vec3(x, y, z);
            spawn(level, base, DreamSprites.FLOWER, delay + 70, DreamParticle.UPRIGHT, (p, age) -> {
                int t = age - delay;
                if (t < 0) {
                    p.alpha(0.0F);
                    return;
                }
                int f = t > 56 ? Math.max(0, 4 - (t - 56) / 3) : Math.min(4, t / 3);
                float s = big * (1.0F + 0.08F * Mth.sin(age * Mth.TWO_PI / 24.0F)); // todas respiram juntas
                p.at(base.x, base.y + s, base.z).frame(DreamSprites.FLOWER + f).size(s).alpha(t > 66 ? (70 - t) / 4.0F : 1.0F);
                if (t == 18 || t == 34 || t == 50) {
                    petal(level, base.add(0, s * 1.5, 0));
                }
                if (t > 14 && t < 56 && (age + p.lifetime()) % 9 == 0) {
                    Vec3 spore = base.add(rs(0.1), s * 1.5, rs(0.1));
                    spawn(level, spore, DreamSprites.GLOW, 22, DreamParticle.BILLBOARD,
                            (g, a) -> g.at(spore.x, spore.y + 0.02 * a, spore.z).tint(0xD8FF6A).size(0.05F).alpha(1.0F - a / 22.0F));
                }
            });
        }
        // pétalas subindo em volta de quem foi curado
        for (int i = 0; i < 8; i++) {
            double phase = Math.PI * 2 * i / 8.0;
            spawn(level, at, DreamSprites.PETAL, 26, DreamParticle.BILLBOARD, (p, age) -> {
                Entity caster = entity(level, casterId);
                Vec3 c = caster != null ? caster.position() : at;
                double a = phase + age * 0.3;
                p.at(c.x + Math.cos(a) * 0.7, c.y + 0.1 + age * 0.08, c.z + Math.sin(a) * 0.7).size(0.07F).alpha(age > 18 ? (26 - age) / 8.0F : 1.0F).roll((float) a);
            });
        }
    }

    // ------------------------------------------------------------------ Luz Sagrada: as rodas

    private static void halo(ClientLevel level, int casterId, Vec3 at, float radius) {
        int life = 54;
        float[] sizes = {1.5F, 1.1F, 0.72F};
        Vec3[] centre = {at.add(0, 3.4, 0)};
        for (int i = 0; i < 3; i++) {
            final int k = i;
            spawn(level, centre[0], DreamSprites.WHEEL, life, DreamParticle.FIXED, (p, age) -> {
                if (k == 0) {
                    Entity caster = entity(level, casterId);
                    if (caster != null) {
                        centre[0] = caster.position().add(0, caster.getBbHeight() + 1.6, 0);
                    }
                }
                float in = Math.min(1.0F, age / 8.0F);
                in = 1.0F - (1.0F - in) * (1.0F - in);
                float out = Math.min(1.0F, (life - age) / 7.0F);
                Quaternionf q = switch (k) {
                    case 0 -> new Quaternionf().rotationY(age * 0.12F).rotateX(-Mth.HALF_PI + 0.15F);
                    case 1 -> new Quaternionf().rotationY(-age * 0.09F).rotateX(1.0F + age * 0.11F);
                    default -> new Quaternionf().rotationZ(age * 0.13F).rotateY(age * 0.17F);
                };
                boolean shut = (age + k * 7) % 23 < 2;
                p.at(centre[0].add(0, (1.0F - in) * 2.0, 0)).orient(q).frame(DreamSprites.WHEEL + (shut ? 1 : 0)).size(sizes[k] * in * out).alpha(1.0F);
            });
        }
        spawn(level, centre[0], DreamSprites.GLOW, life, DreamParticle.BILLBOARD, (p, age) -> {
            float in = Math.min(1.0F, age / 8.0F);
            float out = Math.min(1.0F, (life - age) / 7.0F);
            p.at(centre[0].add(0, (1.0F - in) * 2.0, 0)).tint(0xFFF2B0).size((1.0F + 0.1F * Mth.sin(age * 0.5F)) * in * out).alpha(0.5F);
            if (age == 8) {
                Vec3 feet = centre[0].subtract(0, 3.3, 0);
                level.addParticle(VfxContent.wave(0xFFF2B0), feet.x, feet.y, feet.z, radius, 0, 0);
            }
            if (age == life - 7) {
                pulse(level, centre[0], 0xFFFFFF, 0.3F, 2.2F, 0.8F, 7);
            }
        });
        // penas de luz caindo devagar
        for (int i = 0; i < 20; i++) {
            int delay = 6 + RND.nextInt(30);
            double ang = rf() * Math.PI * 2;
            double reach = 0.5 + rf() * Math.min(radius, 8.0F) * 0.45;
            double phase = rf() * 6.28;
            int fall = 40 + RND.nextInt(10);
            spawn(level, centre[0], DreamSprites.FEATHER, delay + fall, DreamParticle.BILLBOARD, after(delay, (p, t) -> {
                float f = t / (float) fall;
                double out = reach * (1.0 - (1.0 - f) * (1.0 - f));
                Vec3 c = centre[0];
                p.at(c.x + Math.cos(ang) * out + Math.sin(t * 0.2 + phase) * 0.12, c.y - 0.075 * t, c.z + Math.sin(ang) * out + Math.cos(t * 0.17 + phase) * 0.12);
                p.size(0.16F).alpha(f > 0.7F ? 1.0F - (f - 0.7F) / 0.3F : Math.min(1.0F, t / 3.0F)).roll((float) Math.sin(t * 0.15 + phase) * 0.7F);
            }));
        }
    }

    // ================================================================== FOGO

    /** Explosão de Fogo: mariposas apertadas num casulo voando até o ponto. */
    private static void cocoon(ClientLevel level, Vec3 from, Vec3 to, int ticks) {
        int life = Math.max(1, ticks);
        Vec3 dir = to.subtract(from);
        Vec3[] uw = basis(dir.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : dir.normalize());
        for (int i = 0; i < 8; i++) {
            final int k = i;
            double phase = Math.PI * 2 * i / 8.0;
            spawn(level, from, DreamSprites.MOTH, life, DreamParticle.BILLBOARD, (p, age) -> {
                Vec3 c = from.lerp(to, age / (double) life);
                double ang = phase + age * 1.1;
                double r = 0.16 + 0.05 * Math.sin(age * 0.9 + phase);
                p.at(c.add(uw[0].scale(Math.cos(ang) * r)).add(uw[1].scale(Math.sin(ang) * r)));
                p.frame(DreamSprites.MOTH + ((age + k) / 2) % 2).size(0.17F).alpha(1.0F).roll((float) ang);
                if ((age + k) % 3 == 0) {
                    ash(level, p.pos());
                }
            });
        }
        spawn(level, from, DreamSprites.GLOW, life, DreamParticle.BILLBOARD,
                (p, age) -> p.at(from.lerp(to, age / (double) life)).tint(0xFFB040).size(0.55F + 0.08F * Mth.sin(age * 1.5F)).alpha(0.55F));
    }

    /** O casulo abre: uma mariposa enorme bate asa duas vezes e queima. */
    private static void cocoonHit(ClientLevel level, Vec3 at, float radius) {
        Vec3 c = at.add(0, 1.0, 0);
        float big = Math.max(1.4F, radius * 0.5F);
        pulse(level, c, 0xFF8A2A, 0.5F, radius * 1.1F, 0.85F, 8);
        spawn(level, c, DreamSprites.BIGMOTH, 26, DreamParticle.BILLBOARD, (p, age) -> {
            float in = Math.min(1.0F, age / 4.0F);
            in = 1.0F - (1.0F - in) * (1.0F - in);
            p.at(c.x, c.y + (age > 12 ? (age - 12) * 0.06 : 0.0), c.z).frame(DreamSprites.BIGMOTH + (age / 3) % 2).size(big * in);
            p.alpha(age > 14 ? 1.0F - (age - 14) / 12.0F : 1.0F).roll(Mth.sin(age * 0.4F) * 0.12F);
        });
        scatter(level, c, 16, 0.3);
        for (int i = 0; i < 18; i++) {
            ash(level, c.add(rs(radius * 0.7), rs(1.0), rs(radius * 0.7)));
        }
    }

    /** Roda de gente de fogo dançando: abre de 0,8 até {@code radius}. */
    private static void ciranda(ClientLevel level, Vec3 centre, float radius, int count, int delay, int life) {
        double dir = RND.nextBoolean() ? 1.0 : -1.0;
        for (int i = 0; i < count; i++) {
            final int k = i;
            double phase = Math.PI * 2 * i / count;
            spawn(level, centre, DreamSprites.DANCER, delay + life, DreamParticle.UPRIGHT, after(delay, (p, t) -> {
                float f = Math.min(1.0F, t / (life * 0.55F));
                f = 1.0F - (1.0F - f) * (1.0F - f);
                double r = Mth.lerp(f, 0.8F, radius);
                double ang = phase + dir * t * 0.22;
                double x = centre.x + Math.cos(ang) * r, z = centre.z + Math.sin(ang) * r;
                double y = ground(level, x, centre.y, z);
                if (Double.isNaN(y)) {
                    y = centre.y;
                }
                float s = 0.3F;
                p.at(x, y + s + Math.abs(Math.sin(t * 0.5 + k)) * 0.18, z).frame(DreamSprites.DANCER + ((t + k) / 3) % 2).size(s);
                p.alpha(t > life - 6 ? (life - t) / 6.0F : Math.min(1.0F, t / 2.0F));
                if ((t + k) % 4 == 0) {
                    ash(level, p.pos());
                }
            }));
        }
    }

    /** Meteoro: a sombra cresce no chão enquanto um sol de olho fechado cai; ele abre o olho antes de bater. */
    private static void sunFall(ClientLevel level, Vec3 target, int delay, float size, float radius) {
        Quaternionf flat = new Quaternionf().rotationX(-Mth.HALF_PI);
        spawn(level, target.add(0, 0.06, 0), DreamSprites.GLOW, delay + 2, DreamParticle.FIXED, (p, age) -> {
            float f = Math.min(1.0F, age / (float) delay);
            p.orient(flat).tint(0x0A0204).size(0.4F + radius * f * f).alpha(0.25F + 0.6F * f);
        });
        Vec3 top = target.add(-5.5, 22, 3.3);
        int fall = Math.min(12, delay);
        spawn(level, top, DreamSprites.SUN, delay, DreamParticle.BILLBOARD, (p, age) -> {
            int t = age - (delay - fall);
            if (t <= 0) {
                p.at(top).size(size * 0.5F).alpha(Math.min(0.8F, age / 6.0F));
            } else {
                double f = t / (double) fall;
                p.at(top.lerp(target.add(0, size * 0.5, 0), f * f)).size(size * (0.5F + 0.5F * (float) f)).alpha(1.0F);
                ash(level, p.pos().add(rs(0.4), rs(0.4), rs(0.4)));
                level.addParticle(VfxContent.ember(0xFF8A2A), p.pos().x, p.pos().y, p.pos().z, rs(0.05), 0.05, rs(0.05));
            }
            p.frame(DreamSprites.SUN + (age >= delay - 5 ? 1 : 0)).roll(age * 0.05F);
            if (age == delay - 1) {
                Vec3 c = target.add(0, 0.6, 0);
                pulse(level, c, 0xFFE0A0, 0.6F, radius * 1.3F, 0.9F, 9);
                scatter(level, c, 20, 0.38);
                ciranda(level, target, radius * 1.2F, 12, 0, 18);
            }
        });
    }

    /** Fênix: um ovo de brasa treme, racha e abre. */
    private static void egg(ClientLevel level, Vec3 at) {
        Vec3 c = at.add(0, 0.55, 0);
        spawn(level, c, DreamSprites.EGG, 24, DreamParticle.UPRIGHT, (p, age) -> {
            int f = age < 7 ? 0 : age < 13 ? 1 : 2;
            float shake = f == 0 ? 0.08F : f == 1 ? 0.2F : 0.0F;
            p.frame(DreamSprites.EGG + f).size(age < 3 ? 0.2F + age * 0.1F : 0.5F).roll(Mth.sin(age * 2.2F) * shake).alpha(age > 18 ? (24 - age) / 6.0F : 1.0F);
            if (age == 13) {
                pulse(level, c, 0xFFB040, 0.3F, 2.2F, 0.85F, 8);
                scatter(level, c, 8, 0.22);
                for (int i = 0; i < 14; i++) {
                    double ang = rf() * Math.PI * 2, reach = 0.6 + rf() * 1.6, phase = rf() * 6.28;
                    spawn(level, c, DreamSprites.FEATHER, 30, DreamParticle.BILLBOARD, (q, t) -> {
                        float g = t / 30.0F;
                        double out = reach * (1.0 - (1.0 - g) * (1.0 - g));
                        q.at(c.x + Math.cos(ang) * out, c.y + 1.2 * Math.sin(g * Math.PI) - 0.5 * g, c.z + Math.sin(ang) * out);
                        q.tint(0xFF9A3A).size(0.15F).alpha(g > 0.6F ? 1.0F - (g - 0.6F) / 0.4F : 1.0F).roll((float) Math.sin(t * 0.2 + phase));
                    });
                }
            }
        });
    }

    /** Inferno: a mariposa gigante paira sobre o lugar, chove cinza e três cirandas se abrem. */
    private static void inferno(ClientLevel level, Vec3 at, float radius) {
        Vec3 sky = at.add(0, 6.5, 0);
        int life = 62;
        spawn(level, sky.add(0, 0.6, 0), DreamSprites.GLOW, life, DreamParticle.BILLBOARD,
                (p, age) -> p.tint(0xFF6A1F).size(5.0F).alpha(0.35F * Math.min(1.0F, age / 6.0F) * Math.min(1.0F, (life - age) / 10.0F)));
        spawn(level, sky, DreamSprites.BIGMOTH, life, DreamParticle.BILLBOARD, (p, age) -> {
            p.at(sky.x, sky.y + Math.sin(age * 0.2) * 0.3, sky.z).frame(DreamSprites.BIGMOTH + (age / 5) % 2).size(4.0F * Math.min(1.0F, age / 5.0F));
            p.alpha(Math.min(1.0F, (life - age) / 10.0F));
            for (int i = 0; i < 3; i++) {
                ash(level, at.add(rs(radius), 1.0 + rf() * 5.0, rs(radius)));
            }
        });
        for (int k = 0; k < 3; k++) {
            ciranda(level, at, radius * (0.5F + 0.25F * k), 12 + 4 * k, k * 5, 34);
        }
        scatter(level, at.add(0, 1, 0), 24, 0.4);
    }

    // ================================================================== TREVAS

    /** Mão de sombra que sai do chão em {@code base}, virada para {@code centre}; em {@code grab} ela fecha e vai até lá. */
    private static void hand(ClientLevel level, Vec3 base, Vec3 centre, float size, int delay, int grab, int life) {
        spawn(level, base, DreamSprites.HAND, delay + life, DreamParticle.FIXED, after(delay, (p, t) -> {
            float up = Math.min(1.0F, t / 6.0F);
            up = 1.0F - (1.0F - up) * (1.0F - up);
            float down = Math.max(0.0F, (t - (life - 7)) / 7.0F);
            float in = grab < 0 ? 0.0F : Mth.clamp((t - grab) / 4.0F, 0.0F, 1.0F);
            Vec3 pos = base.lerp(new Vec3(centre.x, base.y, centre.z), in * 0.7);
            double y = base.y + size * (2.0 * up - 1.0) - size * 2.0 * down * down;
            Vec3 to = new Vec3(centre.x - pos.x, 0, centre.z - pos.z);
            p.at(pos.x + Math.sin(t * 0.3 + base.x) * 0.03, y, pos.z).face(to).frame(DreamSprites.HAND + (in > 0.3F ? 1 : 0)).size(size).alpha(1.0F);
        }));
    }

    /** Véu de Sombras: mãos saem do chão em volta do mago e se fecham sobre ele. */
    private static void veil(ClientLevel level, Vec3 at) {
        Quaternionf flat = new Quaternionf().rotationX(-Mth.HALF_PI);
        spawn(level, at.add(0, 0.04, 0), DreamSprites.PUDDLE, 24, DreamParticle.FIXED, (p, age) -> {
            float s = age < 5 ? age / 5.0F : age > 16 ? (24 - age) / 8.0F : 1.0F;
            p.orient(new Quaternionf().rotationY(age * 0.05F).mul(flat)).size(1.9F * s).alpha(0.95F);
        });
        for (int i = 0; i < 7; i++) {
            double ang = Math.PI * 2 * i / 7.0 + rs(0.2);
            hand(level, at.add(Math.cos(ang) * 1.5, 0, Math.sin(ang) * 1.5), at, 0.85F + rf() * 0.25F, RND.nextInt(3), 7, 22);
        }
        spawn(level, at, DreamSprites.GLOW, 16, DreamParticle.BILLBOARD,
                (p, age) -> p.at(at.x, at.y + 1.0, at.z).tint(0x0A0414).size(age < 9 ? 0.0F : 1.6F - (age - 9) * 0.2F).alpha(0.9F));
    }

    /** Dreno: uma boca voa mastigando. */
    private static void mouthLaunch(ClientLevel level, int id, Vec3 start, Vec3 velocity, float range) {
        int life = (int) Math.ceil(range / Math.max(0.1, velocity.length())) + 1;
        track(id, spawn(level, start, DreamSprites.GLOW, life, DreamParticle.BILLBOARD,
                (p, age) -> p.at(start.add(velocity.scale(age))).tint(0x5A0F2A).size(0.45F).alpha(0.6F)));
        track(id, spawn(level, start, DreamSprites.MOUTH, life, DreamParticle.BILLBOARD, (p, age) -> {
            p.at(start.add(velocity.scale(age))).frame(DreamSprites.MOUTH + (age / 2) % 2).size(0.26F).alpha(1.0F).roll((float) Math.sin(age * 0.6) * 0.2F);
            tear(level, p.pos(), 0xFF5A78);
        }));
    }

    private static void mouthHit(ClientLevel level, int id, Vec3 at) {
        land(id);
        spawn(level, at, DreamSprites.MOUTH, 14, DreamParticle.BILLBOARD, (p, age) -> {
            boolean shut = age >= 3;
            p.frame(DreamSprites.MOUTH + (shut ? 1 : 0)).size(shut ? 0.85F : 0.4F + age * 0.2F).alpha(age > 9 ? (14 - age) / 5.0F : 1.0F);
            if (age == 3) {
                pulse(level, at, 0x8A1238, 0.4F, 1.6F, 0.8F, 6);
                for (int i = 0; i < 8; i++) {
                    tear(level, at.add(rs(0.5), rs(0.3), rs(0.5)), 0xFF5A78);
                }
            }
        });
    }

    /** O que a boca tirou volta em gotas, num arco, até o mago. */
    private static void feed(ClientLevel level, Vec3 from, int casterId) {
        for (int i = 0; i < 14; i++) {
            int delay = 4 + i;
            double side = rs(0.5), lift = 0.6 + rf() * 0.8;
            Vec3[] end = {from};
            spawn(level, from, DreamSprites.DRIP, delay + 11, DreamParticle.BILLBOARD, after(delay, (p, t) -> {
                Entity caster = entity(level, casterId);
                if (caster != null) {
                    end[0] = caster.position().add(0, caster.getBbHeight() * 0.6, 0);
                }
                double f = t / 10.0;
                Vec3 pos = from.lerp(end[0], f * f);
                double arc = Math.sin(Math.min(1.0, f) * Math.PI);
                p.at(pos.x + side * arc, pos.y + lift * arc, pos.z + side * arc).tint(0xFF5A78).size(0.08F).alpha(f >= 1.0 ? 0.0F : 1.0F).roll((float) (t * 0.5));
                if (t == 10) {
                    pulse(level, end[0], 0xC2224A, 0.1F, 0.5F, 0.5F, 5);
                }
            }));
        }
    }

    /** Lobo das Sombras: uma poça escura se abre, olhos abrem dentro dela e uma mão ajuda o lobo a sair. */
    private static void puddle(ClientLevel level, Vec3 at) {
        Quaternionf flat = new Quaternionf().rotationX(-Mth.HALF_PI);
        int life = 36;
        spawn(level, at.add(0, 0.04, 0), DreamSprites.PUDDLE, life, DreamParticle.FIXED, (p, age) -> {
            float s = age < 6 ? age / 6.0F : age > life - 8 ? (life - age) / 8.0F : 1.0F;
            p.orient(new Quaternionf().rotationY(age * 0.03F).mul(flat)).size(1.7F * s).alpha(0.96F);
        });
        for (int i = 0; i < 5; i++) {
            double ang = Math.PI * 2 * i / 5.0 + rs(0.4), r = 0.35 + rf() * 0.6;
            Vec3 pos = at.add(Math.cos(ang) * r, 0.08, Math.sin(ang) * r);
            float turn = rf() * 6.28F;
            int delay = 5 + RND.nextInt(6);
            spawn(level, pos, DreamSprites.EYE, delay + 22, DreamParticle.FIXED, after(delay, (p, t) -> {
                int f = eyeFrame(t, 12, 5);
                p.orient(new Quaternionf().rotationY(turn).mul(flat)).frame(DreamSprites.EYE + Math.max(0, f)).size(0.2F).alpha(f < 0 ? 0.0F : 1.0F);
            }));
        }
        hand(level, at.add(0.5, 0, 0.3), at.add(3, 0, 2), 0.6F, 4, -1, 24);
        hand(level, at.add(-0.5, 0, -0.2), at.add(-3, 0, 2), 0.6F, 8, -1, 22);
    }

    /** Eclipse Total: um olho enorme abre no céu, uma roda de olhos cerca tudo e mãos saem do chão. */
    private static void eclipse(ClientLevel level, Vec3 at, float radius) {
        Vec3 sky = at.add(0, 10, 0);
        int life = 74;
        spawn(level, sky.add(0, 0.9, 0), DreamSprites.GLOW, life, DreamParticle.BILLBOARD,
                (p, age) -> p.tint(0x030106).size(5.6F * Math.min(1.0F, age / 8.0F)).alpha(0.95F * Math.min(1.0F, (life - age) / 8.0F)));
        spawn(level, sky, DreamSprites.BIGEYE, life, DreamParticle.BILLBOARD, (p, age) -> {
            int f;
            if (age < 16) {
                f = Math.max(0, (age - 6) / 2);
            } else if (age > life - 10) {
                f = Math.max(0, (life - age) / 2 - 1);
            } else {
                int blink = age - 40;
                f = blink >= 0 && blink < 4 ? new int[] {2, 0, 0, 3}[blink] : 4;
            }
            p.frame(DreamSprites.BIGEYE + Math.min(4, f)).size(3.2F).alpha(age < 6 ? age / 6.0F : Math.min(1.0F, (life - age) / 4.0F));
        });
        double ring = Math.min(radius * 0.8, 11.0);
        for (int i = 0; i < 18; i++) {
            double ang = Math.PI * 2 * i / 18.0;
            Vec3 pos = at.add(Math.cos(ang) * ring, 1.0 + rf() * 3.0, Math.sin(ang) * ring);
            int delay = 8 + i;
            int stare = 40 - i;
            int blinkAt = 8 + RND.nextInt(20);
            float size = 0.4F + rf() * 0.2F;
            spawn(level, pos, DreamSprites.EYE, delay + stare + 10, DreamParticle.FIXED, after(delay, (p, t) -> {
                int f = eyeFrame(t, stare, blinkAt);
                p.face(at.add(0, 1.5, 0).subtract(pos)).frame(DreamSprites.EYE + Math.max(0, f)).size(size).alpha(f < 0 ? 0.0F : 1.0F);
            }));
        }
        for (int i = 0; i < 12; i++) {
            double ang = rf() * Math.PI * 2, r = 2.0 + rf() * (ring - 2.0);
            double x = at.x + Math.cos(ang) * r, z = at.z + Math.sin(ang) * r;
            double y = ground(level, x, at.y, z);
            if (!Double.isNaN(y)) {
                hand(level, new Vec3(x, y, z), at, 0.8F + rf() * 0.5F, 6 + RND.nextInt(20), -1, 34);
            }
        }
    }

    // ================================================================== PREPARAÇÃO (carga e ritual)

    private static final int FIRE = com.mistech.mistechmod.item.RuneElement.FIRE.ordinal();
    private static final int ICE = com.mistech.mistechmod.item.RuneElement.ICE.ordinal();
    private static final int WATER = com.mistech.mistechmod.item.RuneElement.WATER.ordinal();
    private static final int EARTH = com.mistech.mistechmod.item.RuneElement.EARTH.ordinal();
    private static final int ENERGY = com.mistech.mistechmod.item.RuneElement.ENERGY.ordinal();
    private static final int LIGHT = com.mistech.mistechmod.item.RuneElement.LIGHT.ordinal();
    private static final int SPACE = com.mistech.mistechmod.item.RuneElement.SPACE.ordinal();
    private static final int LIFE = com.mistech.mistechmod.item.RuneElement.LIFE.ordinal();
    private static final int MAGMA = com.mistech.mistechmod.item.RuneElement.MAGMA.ordinal();

    private static Vec3 chest(ClientLevel level, int casterId, Vec3 fallback) {
        Entity caster = entity(level, casterId);
        return caster != null ? caster.position().add(0, caster.getBbHeight() * 0.65, 0) : fallback.add(0, 1.1, 0);
    }

    /** Carga: quanto mais carregada, mais coisas vêm de longe e entram no mago. Fogo = mariposas; Trevas = gotas e olhos. */
    private static void charge(ClientLevel level, int element, int casterId, Vec3 at, float factor) {
        int count = 1 + (int) (factor * 3.0F);
        for (int i = 0; i < count; i++) {
            Vec3 dir = new Vec3(rs(1.0), rs(0.6), rs(1.0));
            if (dir.lengthSqr() < 0.01) {
                continue;
            }
            Vec3 off = dir.normalize().scale(2.0 + factor * 1.5 + rf() * 0.6);
            Vec3 side = basis(off.normalize())[0].scale(rs(0.9));
            final int k = i;
            int sprite = element == FIRE ? DreamSprites.MOTH : element == EARTH ? DreamSprites.PEBBLE + (i % 2)
                    : element == ICE ? DreamSprites.SHARD + 1 : element == WATER ? DreamSprites.BUBBLE
                    : element == ENERGY ? DreamSprites.KNOT : element == LIGHT ? DreamSprites.FEATHER
                    : element == SPACE ? DreamSprites.STAR : element == LIFE ? DreamSprites.LEAF : DreamSprites.DRIP;
            spawn(level, at, sprite, 10, DreamParticle.BILLBOARD, (p, age) -> {
                float f = age / 10.0F;
                Vec3 c = chest(level, casterId, at);
                double bend = Math.sin(f * Math.PI);
                p.at(c.add(off.scale(1.0 - f * f)).add(side.scale(bend)));
                if (element == FIRE) {
                    p.frame(DreamSprites.MOTH + ((age + k) / 2) % 2).size(0.17F * (1.0F - f * 0.5F)).roll((float) Math.sin(age + k) * 0.5F);
                } else {
                    p.size(element == EARTH || element == ICE ? 0.11F : 0.08F).roll(age * 0.6F);
                }
                p.alpha(Math.min(1.0F, age / 2.0F));
            });
        }
        Vec3 c = chest(level, casterId, at);
        if (element == FIRE) {
            pulse(level, c, 0xFF8A2A, 0.25F + factor * 0.35F, 0.1F, 0.35F + factor * 0.3F, 4);
        } else if (element == EARTH) {
            pulse(level, c, 0x8A7A5A, 0.25F + factor * 0.35F, 0.1F, 0.45F, 4);
        } else if (element == ICE || element == WATER) {
            pulse(level, c, 0xBFEAFF, 0.25F + factor * 0.35F, 0.1F, 0.4F, 4);
        } else if (element == ENERGY || element == LIGHT) {
            pulse(level, c, 0xFFE66A, 0.25F + factor * 0.35F, 0.1F, 0.4F, 4);
        } else {
            pulse(level, c, 0x3A1A5C, 0.3F + factor * 0.4F, 0.1F, 0.6F, 4);
        }
    }

    /** Carga completa: o sol pequeno abre o olho sobre a cabeça do mago (Fogo) ou um olho abre (Trevas). */
    private static void chargeFull(ClientLevel level, int element, int casterId, Vec3 at) {
        if (element == ENERGY || element == LIGHT) {
            // Energia: uma agulha em pé sobre a cabeça, apontando para baixo; Luz: uma rodinha
            boolean needle = element == ENERGY;
            spawn(level, at, needle ? DreamSprites.NEEDLE : DreamSprites.WHEEL, 16, needle ? DreamParticle.AXIS : DreamParticle.FIXED, (p, age) -> {
                Entity caster = entity(level, casterId);
                Vec3 head = caster != null ? caster.position().add(0, caster.getBbHeight() + 0.8, 0) : at.add(0, 2.6, 0);
                p.at(head).along(new Vec3(0, -1, 0)).orient(new Quaternionf().rotationY(age * 0.3F).rotateX(-Mth.HALF_PI + 0.3F));
                p.size(age < 3 ? 0.2F + age * 0.1F : 0.45F).alpha(age > 11 ? (16 - age) / 5.0F : 1.0F);
            });
            pulse(level, chest(level, casterId, at), 0xFFF6C2, 0.3F, 1.6F, 0.7F, 7);
            return;
        }
        int frame = element == FIRE ? DreamSprites.SUN + 1 : element == EARTH ? DreamSprites.HEAD + 1 : element == ICE ? DreamSprites.MASK + 1
                : element == WATER ? DreamSprites.JELLY : DreamSprites.EYE;
        spawn(level, at, frame, 16, DreamParticle.BILLBOARD, (p, age) -> {
            Entity caster = entity(level, casterId);
            Vec3 head = caster != null ? caster.position().add(0, caster.getBbHeight() + 0.7, 0) : at.add(0, 2.5, 0);
            p.at(head).size(age < 3 ? 0.2F + age * 0.12F : 0.5F).alpha(age > 11 ? (16 - age) / 5.0F : 1.0F);
            if (element == FIRE) {
                p.roll(age * 0.08F);
            } else if (frame == DreamSprites.EYE) {
                p.frame(DreamSprites.EYE + Math.min(4, age + 1));
            }
        });
        pulse(level, chest(level, casterId, at), element == FIRE ? 0xFFE0A0 : element == EARTH ? 0xC9B58A : element == ICE || element == WATER ? 0xDFF6FF : 0x8D3FD1,
                0.3F, 1.6F, 0.7F, 7);
    }

    /** O mago ainda está no ritual? (para a cena sumir se ele se mexer ou soltar) */
    private static boolean casting(ClientLevel level, int casterId) {
        return entity(level, casterId) instanceof net.minecraft.world.entity.LivingEntity living && living.isAlive() && living.isUsingItem();
    }

    /**
     * Ritual: a roda vai se juntando em volta do mago enquanto ele fica parado. Fogo = gente de fogo chegando uma a uma
     * e dançando em roda; Trevas = olhos abrindo no chão, um por vez, e mãos subindo devagar.
     */
    private static void ritual(ClientLevel level, int element, int casterId, Vec3 at, int total) {
        Quaternionf flat = new Quaternionf().rotationX(-Mth.HALF_PI);
        if (element == MAGMA) {
            // Magma: o círculo de dentes da Terra, com o chão rachando em brasa por baixo
            ritualRing(level, EARTH, casterId, at, total);
            for (int i = 0; i < 7; i++) {
                crack(level, at, Math.PI * 2 * i / 7.0 + 0.2, 3.2F, i * Math.max(1, (total - 12) / 7), total);
            }
            return;
        }
        if (element == ICE || element == WATER || element == EARTH || element == LIFE) {
            ritualRing(level, element, casterId, at, total);
            return;
        }
        if (element == ENERGY || element == LIGHT || element == SPACE) {
            ritualSky(level, element, casterId, at, total);
            return;
        }
        int pieces = element == FIRE ? 9 : 10;
        boolean[] over = {false};
        for (int i = 0; i < pieces; i++) {
            final int k = i;
            int delay = 2 + (total - 12) * i / pieces;
            double phase = Math.PI * 2 * i / pieces;
            float turn = rf() * 6.28F;
            spawn(level, at, element == FIRE ? DreamSprites.DANCER : DreamSprites.EYE, total + 12, element == FIRE ? DreamParticle.UPRIGHT : DreamParticle.FIXED,
                    after(delay, (p, t) -> {
                        int age = t + delay;
                        if (k == 0 && age > 4 && !casting(level, casterId)) {
                            over[0] = true;
                        }
                        if (over[0]) {
                            p.kill();
                            return;
                        }
                        float close = Mth.clamp((age - (total - 6)) / 6.0F, 0.0F, 1.0F); // no fim, tudo corre para o mago
                        if (element == FIRE) {
                            double r = 2.6 * (1.0 - close * 0.8);
                            double ang = phase + age * 0.07 + close * 1.5;
                            float s = 0.3F;
                            p.at(at.x + Math.cos(ang) * r, at.y + s + Math.abs(Math.sin(age * 0.35 + k)) * 0.14, at.z + Math.sin(ang) * r);
                            p.frame(DreamSprites.DANCER + ((age + k) / 4) % 2).size(s).alpha(Math.min(1.0F, t / 4.0F));
                            if ((age + k * 3) % 7 == 0) {
                                ash(level, p.pos());
                            }
                        } else {
                            double r = 2.4 * (1.0 - close * 0.7);
                            int f = Math.min(4, t / 2 + 1);
                            if ((age + k * 11) % 37 < 3) {
                                f = 1; // pisca
                            }
                            p.at(at.x + Math.cos(phase) * r, at.y + 0.07, at.z + Math.sin(phase) * r);
                            p.orient(new Quaternionf().rotationY(turn).mul(flat)).frame(DreamSprites.EYE + f).size(0.3F).alpha(1.0F - close * 0.5F);
                        }
                    }));
        }
        if (element != FIRE) {
            for (int i = 0; i < 5; i++) {
                double ang = Math.PI * 2 * i / 5.0 + 0.3;
                Vec3 base = at.add(Math.cos(ang) * 3.3, 0, Math.sin(ang) * 3.3);
                float size = 0.7F + rf() * 0.3F;
                spawn(level, base, DreamSprites.HAND, total + 12, DreamParticle.FIXED, (p, age) -> {
                    if (over[0]) {
                        p.kill();
                        return;
                    }
                    float up = Math.min(1.0F, age / (float) total); // sobem devagar, o ritual inteiro
                    p.at(base.x + Math.sin(age * 0.15 + base.x) * 0.04, base.y + size * (2.0 * up - 1.0), base.z);
                    p.face(at.subtract(base)).frame(DreamSprites.HAND + (age > total - 5 ? 1 : 0)).size(size).alpha(1.0F);
                });
            }
        }
    }

    /**
     * Ritual de Gelo, Água e Terra: as peças chegam uma a uma em roda. Gelo = máscaras dormindo viradas para o mago,
     * que acordam no fim; Água = cardume que cresce nadando em volta; Terra = dentes de pedra subindo como um cromeleque.
     */
    private static void ritualRing(ClientLevel level, int element, int casterId, Vec3 at, int total) {
        int pieces = element == WATER ? 12 : 9;
        boolean[] over = {false};
        for (int i = 0; i < pieces; i++) {
            final int k = i;
            int delay = 2 + (total - 12) * i / pieces;
            double phase = Math.PI * 2 * i / pieces;
            float size = element == EARTH ? 0.6F + rf() * 0.25F : element == ICE ? 0.34F : 0.2F + rf() * 0.08F;
            double height = element == WATER ? 0.5 + rf() * 1.6 : 0.0;
            int sprite = element == ICE ? DreamSprites.MASK : element == WATER ? DreamSprites.FISH : element == LIFE ? DreamSprites.FLOWER : DreamSprites.TOOTH;
            spawn(level, at, sprite, total + 12, element == LIFE ? DreamParticle.UPRIGHT : DreamParticle.FIXED, after(delay, (p, t) -> {
                int age = t + delay;
                if (k == 0 && age > 4 && !casting(level, casterId)) {
                    over[0] = true;
                }
                if (over[0]) {
                    p.kill();
                    return;
                }
                float close = Mth.clamp((age - (total - 6)) / 6.0F, 0.0F, 1.0F);
                float in = Math.min(1.0F, t / 6.0F);
                in = 1.0F - (1.0F - in) * (1.0F - in);
                if (element == LIFE) {
                    // flores brotam uma a uma em roda, respiram juntas e se recolhem no fim
                    Vec3 base = at.add(Math.cos(phase) * 2.5, 0, Math.sin(phase) * 2.5);
                    double gy = ground(level, base.x, base.y, base.z);
                    int f = close > 0.0F ? Math.max(0, 4 - (int) (close * 5.0F)) : Math.min(4, t / 3);
                    float s = 0.32F * (1.0F + 0.08F * Mth.sin(age * Mth.TWO_PI / 24.0F));
                    p.at(base.x, (Double.isNaN(gy) ? base.y : gy) + s, base.z).frame(DreamSprites.FLOWER + f).size(s).alpha(1.0F);
                    if (t > 12 && (age + k * 7) % 16 == 0) {
                        petal(level, p.pos().add(0, s * 0.5, 0));
                    }
                } else if (element == WATER) {
                    double ang = phase + age * (0.09 + close * 0.2);
                    double r = 2.4 * (1.0 - close * 0.6);
                    Vec3 pos = at.add(Math.cos(ang) * r, height + Math.sin(age * 0.2 + k) * 0.12, Math.sin(ang) * r);
                    Vec3 tangent = new Vec3(-Math.sin(ang), 0, Math.cos(ang));
                    swim(p.at(pos), tangent).frame(DreamSprites.FISH + ((age + k) / 3) % 2).size(size).alpha(in);
                    if ((age + k * 5) % 9 == 0) {
                        bubble(level, pos);
                    }
                } else if (element == ICE) {
                    double r = 2.5 * (1.0 - close * 0.5);
                    Vec3 pos = at.add(Math.cos(phase) * r, size * (2.0 * in - 1.0) + 0.9 * in + Math.sin(age * 0.1 + k) * 0.05, Math.sin(phase) * r);
                    p.at(pos).face(at.add(0, 1.2, 0).subtract(pos)).frame(DreamSprites.MASK + (close > 0.0F ? 1 : 0)).size(size).alpha(in);
                } else {
                    double r = 2.7;
                    Vec3 base = at.add(Math.cos(phase) * r, 0, Math.sin(phase) * r);
                    double y = base.y + size * (2.0 * in - 1.0) - size * 2.0 * close;
                    p.at(base.x, y, base.z).face(at.subtract(base).add(0, -0.25, 0)).size(size).alpha(1.0F);
                    if (t == 5) {
                        pebbles(level, base, 3, 0.12);
                    }
                }
            }));
        }
    }

    // ================================================================== GELO

    private static void sparkle(ClientLevel level, Vec3 at, int rgb) {
        int life = 8 + RND.nextInt(6);
        spawn(level, at, DreamSprites.GLOW, life, DreamParticle.BILLBOARD,
                (p, age) -> p.at(at.x, at.y - 0.01 * age, at.z).tint(rgb).size(0.05F).alpha(1.0F - age / (float) life));
    }

    /** Cacos que voam para fora girando e caem. */
    private static void shards(ClientLevel level, Vec3 at, int count, double speed) {
        for (int i = 0; i < count; i++) {
            int life = 14 + RND.nextInt(8);
            double[] pos = {at.x, at.y, at.z};
            double[] vel = {rs(speed), 0.08 + rf() * speed, rs(speed)};
            float spin = (rf() - 0.5F) * 1.2F;
            float size = 0.08F + rf() * 0.08F;
            spawn(level, at, DreamSprites.SHARD + 1, life, DreamParticle.BILLBOARD, (p, age) -> {
                vel[1] -= 0.035;
                pos[0] += vel[0];
                pos[1] += vel[1];
                pos[2] += vel[2];
                p.at(pos[0], pos[1], pos[2]).size(size).roll(spin * age).alpha(age > life - 5 ? (life - age) / 5.0F : 1.0F);
            });
        }
    }

    /** Geada deitada no chão que cresce até {@code radius} e some. */
    private static void frost(ClientLevel level, Vec3 at, float radius, int grow, int life, float alpha) {
        Quaternionf flat = new Quaternionf().rotationX(-Mth.HALF_PI);
        double y = ground(level, at.x, at.y, at.z);
        Vec3 c = new Vec3(at.x, (Double.isNaN(y) ? at.y : y) + 0.05, at.z);
        float turn = rf() * 6.28F;
        spawn(level, c, DreamSprites.FROST, life, DreamParticle.FIXED, (p, age) -> {
            float f = Math.min(1.0F, age / (float) grow);
            p.orient(new Quaternionf().rotationY(turn).mul(flat)).size(radius * (1.0F - (1.0F - f) * (1.0F - f)));
            p.alpha(alpha * Math.min(1.0F, (life - age) / 8.0F));
        });
    }

    private static void shardLaunch(ClientLevel level, int id, Vec3 start, Vec3 velocity, float range) {
        int life = (int) Math.ceil(range / Math.max(0.1, velocity.length())) + 1;
        for (int i = 0; i < 3; i++) {
            final int k = i;
            float spin = 0.7F + rf() * 0.5F;
            track(id, spawn(level, start, DreamSprites.SHARD + (i == 0 ? 0 : 1), life, DreamParticle.BILLBOARD, (p, age) -> {
                p.at(start.add(velocity.scale(Math.max(0.0, age - k * 0.45)))).size(k == 0 ? 0.24F : 0.14F).roll(spin * age + k).alpha(1.0F);
                if (k == 0) {
                    sparkle(level, p.pos().add(rs(0.12), rs(0.12), rs(0.12)), 0xDFF6FF);
                    sparkle(level, p.pos().add(rs(0.2), rs(0.2), rs(0.2)), 0x7FC4EA);
                }
            }));
        }
    }

    private static void shardHit(ClientLevel level, int id, Vec3 at) {
        land(id);
        pulse(level, at, 0xDFF6FF, 0.2F, 1.3F, 0.8F, 6);
        shards(level, at, 10, 0.2);
        frost(level, at, 1.3F, 5, 34, 0.9F);
    }

    /** Máscara que sai do chão em {@code base} olhando para fora de {@code centre}; acorda, sopra e racha. */
    private static void maskRise(ClientLevel level, Vec3 base, Vec3 centre, float size, int delay, int wake, int life) {
        Vec3 out = new Vec3(base.x - centre.x, 0, base.z - centre.z);
        Vec3 dir = out.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : out.normalize();
        spawn(level, base, DreamSprites.MASK, delay + life, DreamParticle.FIXED, after(delay, (p, t) -> {
            float up = Math.min(1.0F, t / 6.0F);
            up = 1.0F - (1.0F - up) * (1.0F - up);
            float down = Math.max(0.0F, (t - (life - 6)) / 6.0F);
            p.at(base.x, base.y + size * (2.0 * up - 1.0) + 0.5 * up - (size * 2.0 + 0.5) * down * down, base.z).face(dir);
            p.frame(DreamSprites.MASK + (t >= wake ? 1 : 0)).size(size).alpha(1.0F);
            if (t >= wake && t < wake + 8 && t % 2 == 0) {
                Vec3 mouth = p.pos().add(dir.scale(0.2)).subtract(0, size * 0.35, 0);
                int breath = 14;
                spawn(level, mouth, DreamSprites.GLOW, breath, DreamParticle.BILLBOARD, (g, a) -> {
                    float f = a / (float) breath;
                    g.at(mouth.add(dir.scale(a * 0.16)).add(0, f * 0.2, 0)).tint(0xDFF6FF).size(0.12F + f * 0.4F).alpha(0.5F * (1.0F - f));
                });
            }
            if (t == life - 6) {
                shards(level, p.pos(), 3, 0.1);
            }
        }));
    }

    private static void frostNova(ClientLevel level, Vec3 at, float radius) {
        frost(level, at, radius, 8, 40, 0.95F);
        pulse(level, at.add(0, 0.6, 0), 0xDFF6FF, 0.4F, radius * 0.9F, 0.6F, 8);
        for (int i = 0; i < 8; i++) {
            double ang = Math.PI * 2 * i / 8.0 + 0.2;
            double x = at.x + Math.cos(ang) * radius * 0.6, z = at.z + Math.sin(ang) * radius * 0.6;
            double y = ground(level, x, at.y, z);
            maskRise(level, new Vec3(x, Double.isNaN(y) ? at.y : y, z), at, 0.36F + rf() * 0.1F, 2 + RND.nextInt(4), 8, 30);
        }
        shards(level, at.add(0, 0.8, 0), 14, 0.3);
    }

    /** Nevasca: a cada pulso, cacos e máscaras passam girando no vento em volta do mago. */
    private static void blizzard(ClientLevel level, int casterId, Vec3 at, float radius) {
        frost(level, at, radius, 4, 16, 0.45F);
        for (int i = 0; i < 20; i++) {
            boolean isMask = i < 3;
            double r = 1.5 + rf() * (radius - 1.5);
            double start = rf() * Math.PI * 2;
            double h = 0.4 + rf() * 3.4;
            double turn = 1.6 / r;
            int life = 12 + RND.nextInt(6);
            float size = isMask ? 0.26F : 0.09F + rf() * 0.1F;
            float spin = (rf() - 0.5F) * (isMask ? 0.3F : 1.4F);
            spawn(level, at, isMask ? DreamSprites.MASK : DreamSprites.SHARD + RND.nextInt(2), life, DreamParticle.BILLBOARD, (p, age) -> {
                Entity caster = entity(level, casterId);
                Vec3 c = caster != null ? caster.position() : at;
                double ang = start + age * turn;
                p.at(c.x + Math.cos(ang) * r, c.y + h - age * 0.04, c.z + Math.sin(ang) * r).size(size).roll(spin * age);
                p.alpha(Math.min(1.0F, age / 3.0F) * Math.min(1.0F, (life - age) / 4.0F));
                if (age % 3 == 0) {
                    sparkle(level, p.pos(), 0xFFFFFF);
                }
            });
        }
    }

    /** Era Glacial: a grande máscara sobe atrás do mago, acorda e sopra; a geada cobre tudo e cacos brotam do chão. */
    private static void iceAge(ClientLevel level, Vec3 at, float radius) {
        int life = 76;
        float big = 3.4F;
        spawn(level, at, DreamSprites.BIGMASK, life, DreamParticle.UPRIGHT, (p, age) -> {
            float up = Math.min(1.0F, age / 16.0F);
            up = 1.0F - (1.0F - up) * (1.0F - up);
            float down = Math.max(0.0F, (age - (life - 12)) / 12.0F);
            p.at(at.x, at.y - big + up * (big + 5.0) - down * down * (big + 5.0), at.z).frame(DreamSprites.BIGMASK + (age >= 20 ? 1 : 0)).size(big).alpha(1.0F);
            if (age == 20) {
                frost(level, at, radius, 14, 54, 1.0F);
                pulse(level, at.add(0, 4, 0), 0xDFF6FF, 1.0F, radius, 0.7F, 12);
            }
            if (age > 20 && age < 50) {
                for (int i = 0; i < 4; i++) {
                    sparkle(level, at.add(rs(radius), 0.3 + rf() * 4.0, rs(radius)), 0xFFFFFF);
                }
            }
        });
        for (int i = 0; i < 44; i++) {
            double ang = rf() * Math.PI * 2, r = 1.5 + Math.sqrt(rf()) * (radius - 1.5);
            double x = at.x + Math.cos(ang) * r, z = at.z + Math.sin(ang) * r;
            double y = ground(level, x, at.y, z);
            if (Double.isNaN(y)) {
                continue;
            }
            int delay = 20 + (int) (r / radius * 14.0);
            float size = 0.45F + rf() * 0.7F;
            float lean = (rf() - 0.5F) * 0.5F;
            int stay = life - delay - 4;
            int sprite = DreamSprites.SHARD + (i % 4 == 0 ? 0 : 1);
            spawn(level, new Vec3(x, y, z), sprite, life, DreamParticle.UPRIGHT, after(delay, (p, t) -> {
                float up = Math.min(1.0F, t / 4.0F);
                float down = Math.max(0.0F, (t - (stay - 8)) / 8.0F);
                p.at(x, y + size * (2.0 * up - 1.0) * 0.8 - size * 2.0 * down, z).size(size).roll(lean).alpha(1.0F);
            }));
        }
        for (int i = 0; i < 10; i++) {
            double ang = Math.PI * 2 * i / 10.0;
            double x = at.x + Math.cos(ang) * radius * 0.7, z = at.z + Math.sin(ang) * radius * 0.7;
            double y = ground(level, x, at.y, z);
            if (!Double.isNaN(y)) {
                maskRise(level, new Vec3(x, y, z), at, 0.5F, 24 + RND.nextInt(8), 8, 40);
            }
        }
    }

    // ================================================================== ÁGUA

    /** Vira o desenho (que olha para a direita) para nadar na direção dada. */
    private static DreamParticle swim(DreamParticle p, Vec3 dir) {
        return p.face(new Vec3(-dir.z, 0, dir.x));
    }

    private static void bubble(ClientLevel level, Vec3 from) {
        int life = 12 + RND.nextInt(10);
        double phase = rf() * 6.28;
        float size = 0.04F + rf() * 0.05F;
        spawn(level, from, DreamSprites.BUBBLE, life, DreamParticle.BILLBOARD, (p, age) -> {
            p.at(from.x + Math.sin(age * 0.4 + phase) * 0.06, from.y + 0.045 * age, from.z + Math.cos(age * 0.35 + phase) * 0.06);
            p.size(size * (1.0F + age * 0.02F)).alpha(age >= life - 1 ? 0.0F : 0.9F);
        });
    }

    /** Onda redonda: deitada no chão ({@code flatOnGround}) ou de frente para a câmera, no ar. */
    private static void ripple(ClientLevel level, Vec3 at, float to, int delay, int life, boolean flatOnGround) {
        Quaternionf flat = new Quaternionf().rotationX(-Mth.HALF_PI);
        spawn(level, at, DreamSprites.RIPPLE, delay + life, flatOnGround ? DreamParticle.FIXED : DreamParticle.BILLBOARD, after(delay, (p, t) -> {
            float f = t / (float) life;
            p.orient(flat).size(0.15F + (to - 0.15F) * (1.0F - (1.0F - f) * (1.0F - f))).alpha(1.0F - f * f);
        }));
    }

    private static void drops(ClientLevel level, Vec3 at, int count, double speed) {
        for (int i = 0; i < count; i++) {
            int life = 12 + RND.nextInt(6);
            double vx = rs(speed), vy = 0.1 + rf() * speed, vz = rs(speed);
            spawn(level, at, DreamSprites.DROP, life, DreamParticle.BILLBOARD, (p, age) -> {
                p.at(at.x + vx * age, at.y + vy * age - 0.02 * age * age, at.z + vz * age).size(0.07F).alpha(age > life - 4 ? (life - age) / 4.0F : 1.0F);
            });
        }
    }

    private static void fishLaunch(ClientLevel level, int id, Vec3 start, Vec3 velocity, float range) {
        int life = (int) Math.ceil(range / Math.max(0.1, velocity.length())) + 1;
        Vec3 dir = velocity.normalize();
        Vec3[] uw = basis(dir);
        for (int i = 0; i < 3; i++) {
            final int k = i;
            double side = i == 0 ? 0.0 : (i == 1 ? 0.35 : -0.3);
            double lift = i == 0 ? 0.0 : (i == 1 ? 0.15 : -0.2);
            track(id, spawn(level, start, DreamSprites.FISH, life, DreamParticle.FIXED, (p, age) -> {
                Vec3 c = start.add(velocity.scale(Math.max(0.0, age - k * 0.5)));
                double wave = Math.sin(age * 1.1 + k * 2.0) * 0.08;
                swim(p.at(c.add(uw[0].scale(side + wave)).add(uw[1].scale(lift))), dir).frame(DreamSprites.FISH + ((age + k) / 2) % 2);
                p.size(k == 0 ? 0.26F : 0.16F).alpha(1.0F);
                if ((age + k) % 2 == 0) {
                    bubble(level, p.pos());
                }
            }));
        }
    }

    private static void fishHit(ClientLevel level, int id, Vec3 at) {
        land(id);
        ripple(level, at, 1.3F, 0, 9, false);
        ripple(level, at, 0.9F, 3, 9, false);
        drops(level, at, 12, 0.16);
        for (int i = 0; i < 5; i++) {
            bubble(level, at.add(rs(0.4), rs(0.3), rs(0.4)));
        }
        for (int i = 0; i < 4; i++) {
            double ang = rf() * Math.PI * 2;
            Vec3 dir = new Vec3(Math.cos(ang), 0, Math.sin(ang));
            double vy = rs(0.05);
            spawn(level, at, DreamSprites.FISH, 16, DreamParticle.FIXED, (p, age) -> {
                swim(p.at(at.add(dir.scale(age * 0.14)).add(0, vy * age, 0)), dir).frame(DreamSprites.FISH + (age / 2) % 2).size(0.13F).alpha(1.0F - age / 16.0F);
            });
        }
    }

    /** Cura d'Água: três carpas sobem nadando em volta de quem é curado. */
    private static void mend(ClientLevel level, int targetId, Vec3 at) {
        int life = 30;
        for (int i = 0; i < 3; i++) {
            final int k = i;
            double phase = Math.PI * 2 * i / 3.0;
            spawn(level, at, DreamSprites.FISH, life, DreamParticle.FIXED, (p, age) -> {
                Entity target = entity(level, targetId);
                Vec3 c = target != null ? target.position() : at;
                double ang = phase + age * 0.36;
                Vec3 pos = c.add(Math.cos(ang) * 0.85, 0.15 + age * 0.075, Math.sin(ang) * 0.85);
                swim(p.at(pos), new Vec3(-Math.sin(ang), 0, Math.cos(ang))).frame(DreamSprites.FISH + ((age + k) / 2) % 2).size(0.22F);
                p.alpha(Math.min(1.0F, age / 3.0F) * Math.min(1.0F, (life - age) / 6.0F));
                if ((age + k) % 3 == 0) {
                    bubble(level, pos);
                }
            });
        }
        double y = ground(level, at.x, at.y, at.z);
        Vec3 feet = new Vec3(at.x, (Double.isNaN(y) ? at.y : y) + 0.05, at.z);
        ripple(level, feet, 1.5F, 0, 14, true);
        ripple(level, feet, 1.5F, 8, 14, true);
    }

    /** Purificar: águas-vivas sobem do chão, cada uma levando uma gota escura embora. */
    private static void purify(ClientLevel level, Vec3 at, float radius) {
        double gy = ground(level, at.x, at.y, at.z);
        Vec3 feet = new Vec3(at.x, (Double.isNaN(gy) ? at.y : gy) + 0.05, at.z);
        ripple(level, feet, radius, 0, 18, true);
        ripple(level, feet, radius, 8, 18, true);
        for (int i = 0; i < 8; i++) {
            double ang = Math.PI * 2 * i / 8.0 + rs(0.3), r = i == 0 ? 0.0 : 0.8 + rf() * radius * 0.55;
            Vec3 base = at.add(Math.cos(ang) * r, 0.2, Math.sin(ang) * r);
            int delay = RND.nextInt(8);
            int life = 44;
            float size = 0.26F + rf() * 0.12F;
            double[] y = {base.y};
            spawn(level, base, DreamSprites.JELLY, delay + life, DreamParticle.UPRIGHT, after(delay, (p, t) -> {
                int beat = t % 10;
                y[0] += beat < 4 ? 0.13 : 0.02; // sobe aos pulsos
                p.at(base.x + Math.sin(t * 0.1 + base.z) * 0.1, y[0], base.z).frame(DreamSprites.JELLY + (beat < 4 ? 1 : 0)).size(size);
                p.alpha(Math.min(1.0F, t / 4.0F) * Math.min(1.0F, (life - t) / 8.0F) * 0.95F);
                if (beat == 4) {
                    bubble(level, p.pos());
                }
            }));
            spawn(level, base, DreamSprites.DRIP, delay + 26, DreamParticle.BILLBOARD, after(delay, (p, t) -> {
                p.at(base.x + Math.sin(t * 0.1 + base.z) * 0.1, y[0] - size * 1.1, base.z).size(0.08F).alpha(1.0F - t / 26.0F);
            }));
        }
    }

    /** Maremoto: uma baleia com estrelas nas costas atravessa o ar, com o cardume e uma parede de bolhas. */
    private static void whale(ClientLevel level, Vec3 from, Vec3 dir, int steps) {
        Vec3 flat = dir.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : dir.normalize();
        Vec3 side = new Vec3(-flat.z, 0, flat.x);
        int life = steps + 8;
        Vec3 start = from.add(flat.scale(-3.0)).add(0, 2.6, 0);
        spawn(level, start, DreamSprites.WHALE, life, DreamParticle.FIXED, (p, age) -> {
            Vec3 pos = start.add(flat.scale(age * 1.05)).add(0, Math.sin(age * 0.25) * 0.4, 0);
            swim(p.at(pos), flat).frame(DreamSprites.WHALE + (age / 4) % 2).size(3.0F).alpha(Math.min(1.0F, age / 3.0F) * Math.min(1.0F, (life - age) / 5.0F));
            Vec3 front = from.add(flat.scale(age));
            for (int i = 0; i < 6; i++) {
                bubble(level, front.add(side.scale(rs(3.5))).add(0, rf() * 2.8, 0));
            }
            if (age % 3 == 0) {
                double gy = ground(level, front.x, from.y, front.z);
                if (!Double.isNaN(gy)) {
                    ripple(level, new Vec3(front.x, gy + 0.05, front.z), 2.6F, 0, 12, true);
                }
                drops(level, front.add(0, 0.5, 0), 5, 0.14);
            }
        });
        for (int i = 0; i < 16; i++) {
            final int k = i;
            double lateral = rs(3.6), height = 0.4 + rf() * 3.0, lead = -2.0 + rf() * 5.0;
            float size = 0.16F + rf() * 0.14F;
            double speed = 0.95 + rf() * 0.25;
            spawn(level, from, DreamSprites.FISH, life, DreamParticle.FIXED, (p, age) -> {
                Vec3 pos = from.add(flat.scale(lead + age * speed)).add(side.scale(lateral + Math.sin(age * 0.5 + k) * 0.2)).add(0, height + Math.sin(age * 0.3 + k) * 0.15, 0);
                swim(p.at(pos), flat).frame(DreamSprites.FISH + ((age + k) / 2) % 2).size(size).alpha(Math.min(1.0F, age / 3.0F) * Math.min(1.0F, (life - age) / 5.0F));
            });
        }
    }

    // ================================================================== TERRA

    /** Seixos que pulam para fora e caem. */
    private static void pebbles(ClientLevel level, Vec3 at, int count, double speed) {
        for (int i = 0; i < count; i++) {
            int life = 14 + RND.nextInt(8);
            double vx = rs(speed), vy = 0.12 + rf() * speed * 1.4, vz = rs(speed);
            float spin = (rf() - 0.5F) * 0.9F;
            float size = 0.06F + rf() * 0.06F;
            spawn(level, at, DreamSprites.PEBBLE + RND.nextInt(2), life, DreamParticle.BILLBOARD, (p, age) -> {
                p.at(at.x + vx * age, Math.max(at.y - 0.3, at.y + vy * age - 0.025 * age * age), at.z + vz * age).size(size).roll(spin * age);
                p.alpha(age > life - 4 ? (life - age) / 4.0F : 1.0F);
            });
        }
    }

    /** Rachadura no chão que corre de {@code centre} na direção {@code angle} até {@code length}. */
    private static void crack(ClientLevel level, Vec3 centre, double angle, float length, int delay, int life) {
        Quaternionf q = new Quaternionf().rotationY((float) -angle).mul(new Quaternionf().rotationX(-Mth.HALF_PI));
        Vec3 dir = new Vec3(Math.cos(angle), 0, Math.sin(angle));
        double gy = ground(level, centre.x + dir.x * length * 0.5, centre.y, centre.z + dir.z * length * 0.5);
        Vec3 c = new Vec3(centre.x, (Double.isNaN(gy) ? centre.y : gy) + 0.04, centre.z);
        spawn(level, c, DreamSprites.CRACK, delay + life, DreamParticle.FIXED, after(delay, (p, t) -> {
            float half = length * 0.5F * Math.min(1.0F, t / 4.0F);
            p.at(c.add(dir.scale(half))).orient(q).size(Math.max(0.01F, half)).alpha(Math.min(1.0F, (life - t) / 6.0F));
        }));
    }

    /** Dente de pedra que sobe do chão, fica e afunda. {@code lean} > 0 inclina a ponta para o centro; < 0, para fora. */
    private static void tooth(ClientLevel level, Vec3 base, Vec3 centre, float size, float lean, int delay, int life) {
        spawn(level, base, DreamSprites.TOOTH, delay + life, DreamParticle.FIXED, after(delay, (p, t) -> {
            float up = Math.min(1.0F, t / 4.0F);
            float down = Math.max(0.0F, (t - (life - 6)) / 6.0F);
            float tilt = lean * Mth.clamp((t - 4) / 5.0F, 0.0F, 1.0F);
            Vec3 to = new Vec3(centre.x - base.x, 0, centre.z - base.z);
            Vec3 flatTo = to.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : to.normalize();
            p.at(base.x + flatTo.x * tilt * size * 0.5, base.y + size * (2.0 * up - 1.0) - size * 2.0 * down * down, base.z + flatTo.z * tilt * size * 0.5);
            p.face(flatTo.add(0, -tilt, 0)).size(size).alpha(1.0F);
            if (t == 3) {
                pebbles(level, base, 2, 0.1);
            }
        }));
    }

    private static void headLaunch(ClientLevel level, int id, Vec3 start, Vec3 velocity, float range) {
        int life = (int) Math.ceil(range / Math.max(0.1, velocity.length())) + 1;
        track(id, spawn(level, start, DreamSprites.HEAD, life, DreamParticle.BILLBOARD, (p, age) -> {
            p.at(start.add(velocity.scale(age))).size(0.34F).roll(age * 0.55F).alpha(1.0F);
            if (age % 2 == 0) {
                pebbles(level, p.pos(), 1, 0.04);
            }
            Vec3 dust = p.pos();
            spawn(level, dust, DreamSprites.GLOW, 8, DreamParticle.BILLBOARD, (g, a) -> g.tint(0x8A7A5A).size(0.16F + a * 0.03F).alpha(0.35F * (1.0F - a / 8.0F)));
        }));
    }

    private static void headHit(ClientLevel level, int id, Vec3 at) {
        land(id);
        spawn(level, at, DreamSprites.HEAD + 1, 7, DreamParticle.BILLBOARD, (p, age) -> p.size(0.4F + age * 0.07F).alpha(age > 4 ? (7 - age) / 3.0F : 1.0F));
        pulse(level, at, 0x8A7A5A, 0.4F, 1.6F, 0.6F, 8);
        pebbles(level, at, 14, 0.22);
        double a0 = rf() * 6.28;
        for (int i = 0; i < 3; i++) {
            crack(level, at, a0 + i * 2.1 + rs(0.3), 1.8F, 2, 34);
        }
    }

    /** Pele de Pedra: dentes sobem em volta e se fecham sobre o mago como uma boca; seixos grudam nele. */
    private static void stoneSkin(ClientLevel level, int targetId, Vec3 at) {
        for (int i = 0; i < 8; i++) {
            double ang = Math.PI * 2 * i / 8.0;
            double x = at.x + Math.cos(ang) * 1.3, z = at.z + Math.sin(ang) * 1.3;
            double y = ground(level, x, at.y, z);
            tooth(level, new Vec3(x, Double.isNaN(y) ? at.y : y, z), at, 0.85F + rf() * 0.2F, 0.7F, RND.nextInt(3), 22);
        }
        for (int i = 0; i < 12; i++) {
            Vec3 off = new Vec3(rs(1.0), rs(0.5), rs(1.0)).normalize().scale(1.6 + rf());
            int delay = 6 + RND.nextInt(6);
            spawn(level, at, DreamSprites.PEBBLE + i % 2, delay + 9, DreamParticle.BILLBOARD, after(delay, (p, t) -> {
                float f = t / 9.0F;
                p.at(chest(level, targetId, at).add(off.scale(1.0 - f * f))).size(0.09F).roll(t * 0.5F).alpha(1.0F);
            }));
        }
    }

    /** Golem de Pedra: uma cabeça grande sai do chão, abre os olhos e afunda de novo quando o golem chega. */
    private static void golemRise(ClientLevel level, Vec3 at) {
        double gy = ground(level, at.x, at.y, at.z);
        Vec3 base = new Vec3(at.x, Double.isNaN(gy) ? at.y : gy, at.z);
        float size = 1.3F;
        int life = 34;
        spawn(level, base, DreamSprites.HEAD, life, DreamParticle.UPRIGHT, (p, age) -> {
            float up = Math.min(1.0F, age / 10.0F);
            up = 1.0F - (1.0F - up) * (1.0F - up);
            float down = Math.max(0.0F, (age - (life - 10)) / 10.0F);
            p.at(base.x, base.y + size * (2.0 * up - 1.0) - size * 2.0 * down * down, base.z).frame(DreamSprites.HEAD + (age >= 13 && age < life - 8 ? 1 : 0)).size(size).alpha(1.0F);
            if (age == 13) {
                pulse(level, base.add(0, size * 1.3, 0), 0xFFC23D, 0.2F, 1.4F, 0.6F, 6);
            }
            if (age < 10 && age % 2 == 0) {
                pebbles(level, base.add(rs(0.8), 0.1, rs(0.8)), 2, 0.12);
            }
        });
        for (int i = 0; i < 6; i++) {
            double ang = Math.PI * 2 * i / 6.0 + 0.4;
            crack(level, base, ang, 2.4F, 2, 40);
            tooth(level, base.add(Math.cos(ang) * 1.9, 0, Math.sin(ang) * 1.9), base, 0.45F + rf() * 0.2F, -0.3F, 3 + RND.nextInt(5), 26);
        }
    }

    /** Terremoto: a cada pulso o chão racha, dentes aparecem e somem, seixos pulam. */
    private static void quake(ClientLevel level, Vec3 at, float radius) {
        double a0 = rf() * 6.28;
        for (int i = 0; i < 5; i++) {
            crack(level, at, a0 + Math.PI * 2 * i / 5.0 + rs(0.3), radius, RND.nextInt(3), 16);
        }
        for (int i = 0; i < 7; i++) {
            double ang = rf() * Math.PI * 2, r = 1.5 + rf() * (radius - 1.5);
            double x = at.x + Math.cos(ang) * r, z = at.z + Math.sin(ang) * r;
            double y = ground(level, x, at.y, z);
            if (!Double.isNaN(y)) {
                tooth(level, new Vec3(x, y, z), at, 0.35F + rf() * 0.35F, (rf() - 0.5F) * 0.6F, RND.nextInt(4), 12);
            }
        }
        for (int i = 0; i < 6; i++) {
            pebbles(level, at.add(rs(radius * 0.8), 0.1, rs(radius * 0.8)), 2, 0.14);
        }
    }

    /** Cataclismo: quatro fileiras de dentes se abrem de dentro para fora; três cabeças sobem e acordam; os seixos flutuam e caem. */
    private static void cataclysm(ClientLevel level, Vec3 at, float radius) {
        int life = 60;
        for (int k = 0; k < 4; k++) {
            double rr = radius * (k + 1) / 4.0;
            int count = 8 + k * 5;
            for (int i = 0; i < count; i++) {
                double ang = Math.PI * 2 * i / count + k * 0.3;
                double x = at.x + Math.cos(ang) * rr, z = at.z + Math.sin(ang) * rr;
                double y = ground(level, x, at.y, z);
                if (!Double.isNaN(y)) {
                    tooth(level, new Vec3(x, y, z), at, 0.7F + k * 0.3F + rf() * 0.3F, -0.5F, k * 3 + RND.nextInt(2), life - k * 3 - 6);
                }
            }
        }
        for (int i = 0; i < 12; i++) {
            crack(level, at, Math.PI * 2 * i / 12.0 + rs(0.15), radius, RND.nextInt(4), life - 6);
        }
        for (int i = 0; i < 3; i++) {
            double ang = Math.PI * 2 * i / 3.0 + 0.5;
            double x = at.x + Math.cos(ang) * radius * 0.72, z = at.z + Math.sin(ang) * radius * 0.72;
            double y = ground(level, x, at.y, z);
            if (Double.isNaN(y)) {
                continue;
            }
            float size = 1.9F;
            Vec3 base = new Vec3(x, y, z);
            spawn(level, base, DreamSprites.HEAD, life, DreamParticle.FIXED, (p, age) -> {
                float up = Mth.clamp((age - 6) / 12.0F, 0.0F, 1.0F);
                up = 1.0F - (1.0F - up) * (1.0F - up);
                float down = Math.max(0.0F, (age - (life - 10)) / 10.0F);
                p.at(base.x, base.y + size * (2.0 * up - 1.0) - size * 2.0 * down * down, base.z).face(at.subtract(base).multiply(1, 0, 1));
                p.frame(DreamSprites.HEAD + (age >= 22 && age < life - 10 ? 1 : 0)).size(size).alpha(1.0F);
            });
        }
        for (int i = 0; i < 50; i++) {
            double ang = rf() * Math.PI * 2, r = Math.sqrt(rf()) * radius;
            Vec3 base = at.add(Math.cos(ang) * r, 0.1, Math.sin(ang) * r);
            double top = 1.0 + rf() * 3.5;
            int delay = RND.nextInt(10);
            float size = 0.07F + rf() * 0.09F;
            float spin = (rf() - 0.5F) * 0.3F;
            spawn(level, base, DreamSprites.PEBBLE + i % 2, life, DreamParticle.BILLBOARD, after(delay, (p, t) -> {
                int hang = 34;
                double y;
                if (t < hang) {
                    float f = Math.min(1.0F, t / 16.0F);
                    y = top * (1.0 - (1.0 - f) * (1.0 - f)) + Math.sin(t * 0.2 + top) * 0.06;
                } else {
                    int fall = t - hang;
                    y = Math.max(0.0, top - 0.04 * fall * fall);
                }
                p.at(base.x, base.y + y, base.z).size(size).roll(spin * t).alpha(t > life - delay - 6 ? 0.0F : 1.0F);
            }));
        }
    }

    /**
     * Ritual de Energia e de Luz. Energia: agulhas aparecem uma a uma em roda, de ponta para baixo, e cada uma costura um
     * fio até a anterior, fechando uma teia; no fim todas caem. Luz: penas sobem em roda e uma roda de olhos vai crescendo
     * sobre a cabeça do mago.
     */
    private static void ritualSky(ClientLevel level, int element, int casterId, Vec3 at, int total) {
        boolean needles = element == ENERGY;
        boolean stars = element == SPACE;
        int pieces = needles ? 8 : stars ? 9 : 10;
        boolean[] over = {false};
        Vec3[] spots = new Vec3[pieces];
        for (int i = 0; i < pieces; i++) {
            double phase = Math.PI * 2 * i / pieces;
            spots[i] = at.add(Math.cos(phase) * 2.5, needles ? 2.0 : stars ? 0.8 + rf() * 2.0 : 0.3, Math.sin(phase) * 2.5);
        }
        for (int i = 0; i < pieces; i++) {
            final int k = i;
            int delay = 2 + (total - 12) * i / pieces;
            double phase = Math.PI * 2 * i / pieces;
            spawn(level, spots[k], needles ? DreamSprites.NEEDLE : stars ? DreamSprites.STAR : DreamSprites.FEATHER, total + 12, needles ? DreamParticle.AXIS : DreamParticle.BILLBOARD,
                    after(delay, (p, t) -> {
                        int age = t + delay;
                        if (k == 0 && age > 4 && !casting(level, casterId)) {
                            over[0] = true;
                        }
                        if (over[0]) {
                            p.kill();
                            return;
                        }
                        float close = Mth.clamp((age - (total - 6)) / 6.0F, 0.0F, 1.0F);
                        if (stars) {
                            // constelação: cada estrela nova é ligada à anterior; no fim ela desaba no centro
                            Vec3 c = at.add(0, 1.6, 0);
                            p.at(spots[k].lerp(c, close * close)).size(0.16F + 0.04F * Mth.sin(age * 0.6F + k)).roll(age * 0.05F).alpha(Math.min(1.0F, t / 3.0F));
                            if (t == 2 && k > 0) {
                                thread(level, spots[k - 1], spots[k], 0, 0.5F, total - age - 8, 0.0, 0xC89BFF, false);
                            }
                            if (t == 2 && k == pieces - 1) {
                                thread(level, spots[k], spots[0], 3, 0.5F, total - age - 11, 0.0, 0xC89BFF, false);
                            }
                        } else if (needles) {
                            p.at(spots[k].x, spots[k].y + Math.sin(age * 0.2 + k) * 0.06 - close * close * 2.0, spots[k].z).along(new Vec3(0, -1, 0));
                            p.size(0.42F).alpha(Math.min(1.0F, t / 3.0F));
                            if (t == 2 && k > 0) {
                                stitch(level, spots[k - 1], spots[k], 0, 0.8F, total - age - 2);
                            }
                            if (t == 2 && k == pieces - 1) {
                                stitch(level, spots[k], spots[0], 3, 0.8F, total - age - 5);
                            }
                        } else {
                            double ang = phase + age * 0.05;
                            double r = 2.5 * (1.0 - close * 0.7);
                            p.at(at.x + Math.cos(ang) * r, at.y + 0.3 + age * 0.03 + close * 1.5, at.z + Math.sin(ang) * r);
                            p.size(0.17F).roll((float) Math.sin(age * 0.15 + k) * 0.5F).alpha(Math.min(1.0F, t / 4.0F) * (1.0F - close * 0.6F));
                        }
                    }));
        }
        if (!needles) {
            spawn(level, at, DreamSprites.WHEEL, total + 4, DreamParticle.FIXED, (p, age) -> {
                if (over[0]) {
                    p.kill();
                    return;
                }
                Entity caster = entity(level, casterId);
                Vec3 head = caster != null ? caster.position().add(0, caster.getBbHeight() + 1.0, 0) : at.add(0, 2.8, 0);
                float f = Math.min(1.0F, age / (float) total);
                p.at(head).orient(new Quaternionf().rotationY(age * 0.1F).rotateX(-Mth.HALF_PI + 0.2F)).frame(DreamSprites.WHEEL + (age % 19 < 2 ? 1 : 0));
                p.size(0.15F + 0.75F * f).alpha(1.0F);
            });
        }
    }

    // ================================================================== AR

    /** Pássaro de papel batendo asa: {@code path} devolve a posição e a direção (em [1]) a cada tick. */
    private static void crane(ClientLevel level, Vec3 at, float size, int delay, int life, java.util.function.IntFunction<Vec3[]> path) {
        int beat = RND.nextInt(4);
        spawn(level, at, DreamSprites.CRANE, delay + life, DreamParticle.FIXED, after(delay, (p, t) -> {
            Vec3[] pd = path.apply(t);
            swim(p.at(pd[0]), pd[1]).frame(DreamSprites.CRANE + ((t + beat) / 2) % 2).size(size);
            p.alpha(Math.min(1.0F, t / 2.0F) * Math.min(1.0F, (life - t) / 4.0F));
        }));
    }

    private static void paper(ClientLevel level, Vec3 from, Vec3 push) {
        int life = 16 + RND.nextInt(12);
        float spin = (rf() - 0.5F) * 0.8F;
        double phase = rf() * 6.28;
        spawn(level, from, DreamSprites.PAPER, life, DreamParticle.BILLBOARD, (p, age) -> {
            double slow = 1.0 - Math.pow(0.85, age);
            p.at(from.x + push.x * slow * 6.0 + Math.sin(age * 0.3 + phase) * 0.15, from.y + push.y * slow * 6.0 - 0.012 * age * age * 0.3 + Math.sin(age * 0.5 + phase) * 0.05,
                    from.z + push.z * slow * 6.0 + Math.cos(age * 0.27 + phase) * 0.15);
            p.size(0.07F).roll(spin * age).alpha(age > life - 6 ? (life - age) / 6.0F : 1.0F);
        });
    }

    private static void swirlAt(ClientLevel level, Vec3 from, Vec3 drift, float from0, float to, int life) {
        float dir = RND.nextBoolean() ? 1.0F : -1.0F;
        spawn(level, from, DreamSprites.SWIRL, life, DreamParticle.BILLBOARD, (p, age) -> {
            float f = age / (float) life;
            p.at(from.add(drift.scale(age))).size(Mth.lerp(f, from0, to)).roll(dir * age * 0.35F).alpha(0.8F * (1.0F - f));
        });
    }

    /** Rajada: uma revoada de pássaros de papel sai do mago e se abre em leque. */
    private static void gust(ClientLevel level, Vec3 from, Vec3 look, float reach) {
        Vec3 dir = look.normalize();
        Vec3[] uw = basis(dir);
        for (int i = 0; i < 11; i++) {
            double side = rs(1.0), lift = rs(0.6), speed = 0.7 + rf() * 0.35, phase = rf() * 6.28;
            crane(level, from, 0.2F + rf() * 0.1F, RND.nextInt(3), 11, t -> {
                double d = t * speed * reach / 8.0;
                double open = 0.25 + d * 0.3;
                return new Vec3[] {from.add(dir.scale(0.6 + d)).add(uw[0].scale(side * open)).add(uw[1].scale(lift * open + Math.sin(t * 0.6 + phase) * 0.08)), dir};
            });
        }
        for (int i = 0; i < 4; i++) {
            swirlAt(level, from.add(dir.scale(1.0 + i * 1.3)).add(uw[0].scale(rs(0.6))).add(uw[1].scale(rs(0.4))), dir.scale(0.35), 0.2F, 0.9F, 10 + i * 2);
        }
        for (int i = 0; i < 10; i++) {
            paper(level, from.add(dir.scale(0.8)).add(uw[0].scale(rs(0.8))).add(uw[1].scale(rs(0.5))), dir.scale(0.8 + rf() * 0.5).add(rs(0.2), rs(0.15), rs(0.2)));
        }
    }

    /** Corrente Ascendente: os pássaros sobem em espiral junto com quem foi lançado. */
    private static void updraft(ClientLevel level, int targetId, Vec3 at) {
        for (int i = 0; i < 9; i++) {
            double phase = Math.PI * 2 * i / 9.0, start = -0.5 - rf() * 1.5, r = 0.8 + rf() * 0.5, turn = 0.35 + rf() * 0.15;
            crane(level, at, 0.2F, RND.nextInt(4), 18, t -> {
                Entity target = entity(level, targetId);
                Vec3 c = target != null ? target.position() : at.add(0, t * 0.5, 0);
                double ang = phase + t * turn;
                Vec3 tangent = new Vec3(-Math.sin(ang), 0, Math.cos(ang));
                return new Vec3[] {c.add(Math.cos(ang) * r, start + t * 0.16, Math.sin(ang) * r), tangent};
            });
        }
        for (int i = 0; i < 4; i++) {
            swirlAt(level, at.add(rs(0.5), 0.2 + i * 0.5, rs(0.5)), new Vec3(0, 0.25, 0), 0.3F, 1.0F, 12);
        }
        for (int i = 0; i < 8; i++) {
            paper(level, at.add(rs(0.6), 0.2, rs(0.6)), new Vec3(rs(0.1), 0.5 + rf() * 0.4, rs(0.1)));
        }
    }

    /** Passo de Vento: a revoada acompanha o mago no arranque e fica papel para trás. */
    private static void windStep(ClientLevel level, int casterId, Vec3 at, Vec3 look) {
        Vec3 dir = look.normalize();
        Vec3 flat = new Vec3(dir.x, 0, dir.z);
        Vec3 heading = flat.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : flat.normalize();
        Vec3 side = new Vec3(-heading.z, 0, heading.x);
        swirlAt(level, at.add(0, 0.8, 0), Vec3.ZERO, 0.4F, 1.6F, 8);
        Vec3[] last = {at};
        for (int i = 0; i < 8; i++) {
            final int k = i;
            double lateral = (i % 2 == 0 ? 1 : -1) * (0.6 + rf() * 0.7), back = 0.3 + rf() * 1.6, height = 0.5 + rf() * 1.4, phase = rf() * 6.28;
            crane(level, at, 0.2F, 0, 14, t -> {
                Entity caster = entity(level, casterId);
                if (caster != null) {
                    last[0] = caster.position();
                }
                Vec3 pos = last[0].add(side.scale(lateral)).subtract(heading.scale(back)).add(0, height + Math.sin(t * 0.7 + phase) * 0.1, 0);
                if (k == 0 || k == 3) {
                    paper(level, pos, heading.scale(-0.15).add(rs(0.05), 0.02, rs(0.05)));
                }
                return new Vec3[] {pos, heading};
            });
        }
    }

    /** Furacão: a cada pulso, um funil de pássaros de papel girando no ponto. */
    private static void hurricane(ClientLevel level, Vec3 at, float radius) {
        for (int i = 0; i < 18; i++) {
            double h = 0.3 + rf() * 6.2, r = 0.6 + h * 0.5, start = rf() * Math.PI * 2, turn = Math.min(0.5, 1.3 / r);
            crane(level, at, 0.18F + (float) h * 0.02F, RND.nextInt(3), 14, t -> {
                double ang = start + t * turn;
                return new Vec3[] {at.add(Math.cos(ang) * r, h + t * 0.05, Math.sin(ang) * r), new Vec3(-Math.sin(ang), 0, Math.cos(ang))};
            });
        }
        for (int i = 0; i < 12; i++) {
            double ang = rf() * Math.PI * 2, r = 1.0 + rf() * Math.min(radius, 5.0);
            Vec3 from = at.add(Math.cos(ang) * r, 0.2 + rf() * 4.0, Math.sin(ang) * r);
            paper(level, from, new Vec3(-Math.sin(ang) * 0.5 - Math.cos(ang) * 0.2, 0.2, Math.cos(ang) * 0.5 - Math.sin(ang) * 0.2));
        }
        for (int i = 0; i < 3; i++) {
            swirlAt(level, at.add(rs(0.6), 0.3 + i * 1.8, rs(0.6)), new Vec3(0, 0.12, 0), 0.5F + i * 0.5F, 1.6F + i * 0.9F, 12);
        }
    }

    // ================================================================== ENERGIA

    /**
     * Costura: uma agulha vai de {@code a} até {@code b} deixando um fio de luz em ziguezague, ponto por ponto, que fica
     * esticado por {@code hold} ticks e some. {@code thick} = grossura (1 = normal).
     */
    private static void stitch(ClientLevel level, Vec3 a, Vec3 b, int delay, float thick, int hold) {
        thread(level, a, b, delay, thick, hold, 1.0, 0xFFFFFF, true);
    }

    /** O fio em si: {@code zig} = quanto ziguezagueia (0 = reto), {@code rgb} = tinta, {@code needle} = com agulha e nó. */
    private static void thread(ClientLevel level, Vec3 a, Vec3 b, int delay, float thick, int hold, double zig, int rgb, boolean needle) {
        Vec3 span = b.subtract(a);
        double dist = span.length();
        if (dist < 0.05) {
            return;
        }
        Vec3 dir = span.scale(1.0 / dist);
        Vec3[] uw = basis(dir);
        int n = Math.max(2, (int) Math.ceil(dist / 0.7));
        Vec3[] pts = new Vec3[n + 1];
        double roll = rf() * Math.PI;
        Vec3 off = uw[0].scale(Math.cos(roll)).add(uw[1].scale(Math.sin(roll)));
        for (int i = 0; i <= n; i++) {
            Vec3 p = a.add(span.scale(i / (double) n));
            pts[i] = i == 0 || i == n ? p : p.add(off.scale((i % 2 == 0 ? 1 : -1) * (0.14 + rf() * 0.1) * thick * zig));
        }
        int perTick = 4;
        int travel = (n + perTick - 1) / perTick;
        int keep = Math.max(4, hold);
        for (int i = 0; i < n; i++) {
            Vec3 p0 = pts[i], p1 = pts[i + 1];
            Vec3 mid = p0.add(p1).scale(0.5);
            Vec3 seg = p1.subtract(p0);
            float half = (float) (seg.length() * 0.5);
            int born = delay + i / perTick;
            int life = travel - i / perTick + keep;
            spawn(level, mid, DreamSprites.THREAD, born + life, DreamParticle.AXIS, after(born, (p, t) -> {
                p.along(seg).tint(rgb).size(half * (0.9F + 0.2F * thick)).alpha((t > life - 5 ? (life - t) / 5.0F : 1.0F) * (0.75F + 0.25F * Mth.sin(t * 2.3F + p.r1 * 6.0F)));
            }));
        }
        if (!needle) {
            return;
        }
        spawn(level, a, DreamSprites.NEEDLE, delay + travel + 2, DreamParticle.AXIS, after(delay, (p, t) -> {
            int i = Math.min(n - 1, t * perTick);
            p.at(pts[Math.min(n, i + 1)]).along(pts[i + 1].subtract(pts[i])).size(0.3F * thick).alpha(t > travel ? 0.0F : 1.0F);
            if (t == travel) {
                knot(level, b, thick);
            }
        }));
    }

    /** O nó do ponto: um xis de luz que pulsa onde a agulha entrou. */
    private static void knot(ClientLevel level, Vec3 at, float thick) {
        spawn(level, at, DreamSprites.KNOT, 12, DreamParticle.BILLBOARD,
                (p, age) -> p.size((0.16F + 0.05F * Mth.sin(age * 1.5F)) * thick).roll(age * 0.2F).alpha(age > 7 ? (12 - age) / 5.0F : 1.0F));
        pulse(level, at, 0xFFE66A, 0.15F, 0.8F * thick, 0.6F, 5);
    }

    /** Uma agulha cai do céu puxando o fio, crava no ponto e o fio fica esticado até arrebentar. */
    private static void needleFall(ClientLevel level, int targetId, Vec3 at, int delay, float size) {
        double top = 16.0;
        int fall = 4;
        int wait = Math.max(0, delay - fall);
        Vec3[] spot = {at};
        spawn(level, at.add(0, top, 0), DreamSprites.NEEDLE, wait + fall + 12, DreamParticle.AXIS, after(wait, (p, t) -> {
            Entity target = entity(level, targetId);
            if (target != null && t <= fall) {
                spot[0] = target.position();
            }
            float f = Math.min(1.0F, t / (float) fall);
            p.at(spot[0].add(0, top * (1.0 - f * f) + 0.5 * size, 0)).along(new Vec3(0, -1, 0)).size(0.5F * size).alpha(t > fall + 7 ? (fall + 12 - t) / 5.0F : 1.0F);
            if (t == fall) {
                knot(level, spot[0].add(0, 0.1, 0), size);
                stitch(level, spot[0].add(0, top, 0), spot[0].add(0, size, 0), 0, size * 0.7F, 8);
            }
        }));
    }

    /** Tempestade: uma teia de fios se costura no céu sobre o lugar. */
    private static void stormWeb(ClientLevel level, Vec3 at, float radius) {
        Vec3 sky = at.add(0, 13, 0);
        Vec3[] ring = new Vec3[7];
        for (int i = 0; i < ring.length; i++) {
            double ang = Math.PI * 2 * i / ring.length + rs(0.2);
            ring[i] = sky.add(Math.cos(ang) * radius, rs(1.0), Math.sin(ang) * radius);
        }
        for (int i = 0; i < ring.length; i++) {
            stitch(level, ring[i], ring[(i + 3) % ring.length], i * 2, 1.6F, 40 - i * 2); // estrela de sete pontas
        }
        spawn(level, sky, DreamSprites.GLOW, 50, DreamParticle.BILLBOARD,
                (p, age) -> p.tint(0xFFE66A).size(radius * 0.9F).alpha(0.22F * Math.min(1.0F, age / 8.0F) * Math.min(1.0F, (50 - age) / 10.0F)));
    }

    // ================================================================== LUZ (resto)

    private static void feather(ClientLevel level, Vec3 from, Vec3 velocity, double fallSpeed, int delay, int life) {
        double phase = rf() * 6.28;
        spawn(level, from, DreamSprites.FEATHER, delay + life, DreamParticle.BILLBOARD, after(delay, (p, t) -> {
            float f = t / (float) life;
            double slow = 1.0 - Math.pow(0.9, t);
            p.at(from.x + velocity.x * slow * 10.0 + Math.sin(t * 0.2 + phase) * 0.12, from.y + velocity.y * slow * 10.0 - fallSpeed * t,
                    from.z + velocity.z * slow * 10.0 + Math.cos(t * 0.17 + phase) * 0.12);
            p.size(0.16F).roll((float) Math.sin(t * 0.15 + phase) * 0.7F).alpha(f > 0.7F ? 1.0F - (f - 0.7F) / 0.3F : Math.min(1.0F, t / 3.0F));
        }));
    }

    /** Roda de olhos pequena que acompanha uma entidade, por cima da cabeça. */
    private static void smallWheel(ClientLevel level, int entityId, Vec3 at, float size, int life) {
        float turn = rf() * 6.28F;
        spawn(level, at, DreamSprites.WHEEL, life, DreamParticle.FIXED, (p, age) -> {
            Entity e = entity(level, entityId);
            Vec3 head = e != null ? e.position().add(0, e.getBbHeight() + 0.45 + size, 0) : at.add(0, 2.3, 0);
            float in = Math.min(1.0F, age / 4.0F), out = Math.min(1.0F, (life - age) / 5.0F);
            p.at(head).orient(new Quaternionf().rotationY(turn + age * 0.14F).rotateX(-Mth.HALF_PI + 0.25F)).frame(DreamSprites.WHEEL + ((age + (int) (turn * 5)) % 17 < 2 ? 1 : 0));
            p.size(size * in * out).alpha(1.0F);
        });
    }

    private static void glimmer(ClientLevel level, int casterId, Vec3 at, float radius) {
        smallWheel(level, casterId, at, 0.55F, 26);
        for (int i = 0; i < 16; i++) {
            double ang = Math.PI * 2 * i / 16.0;
            feather(level, at.add(0, 1.2, 0), new Vec3(Math.cos(ang) * radius * 0.1, 0.02, Math.sin(ang) * radius * 0.1), 0.01, 0, 22);
        }
        level.addParticle(VfxContent.wave(0xFFF2B0), at.x, at.y + 0.1, at.z, radius, 0, 0);
    }

    private static void glimmerMark(ClientLevel level, int targetId, Vec3 at) {
        smallWheel(level, targetId, at, 0.3F, 40);
    }

    private static void lightOrb(ClientLevel level, Vec3 at) {
        spawn(level, at, DreamSprites.WHEEL, 18, DreamParticle.FIXED, (p, age) -> {
            p.orient(new Quaternionf().rotationY(age * 0.3F).rotateX(age * 0.2F)).size(age < 4 ? age * 0.14F : 0.55F * (1.0F - (age - 4) / 14.0F)).alpha(1.0F);
            if (age == 16) {
                pulse(level, at, 0xFFF2B0, 0.2F, 0.9F, 0.7F, 6);
            }
        });
        for (int i = 0; i < 6; i++) {
            feather(level, at, new Vec3(rs(0.03), 0.04 + rf() * 0.03, rs(0.03)), 0.0, RND.nextInt(6), 20);
        }
    }

    /** Alvorada: as três rodas sobem no céu como um sol nascendo; fachos de luz descem e chove pena por toda a área. */
    private static void dawn(ClientLevel level, int casterId, Vec3 at, float radius) {
        int life = 84;
        float[] sizes = {5.5F, 4.0F, 2.6F};
        java.util.function.IntFunction<Vec3> centre = age -> {
            float up = Math.min(1.0F, age / 24.0F);
            up = 1.0F - (1.0F - up) * (1.0F - up);
            return at.add(0, 3.5 + up * 8.5, 0);
        };
        for (int i = 0; i < 3; i++) {
            final int k = i;
            spawn(level, at, DreamSprites.WHEEL, life, DreamParticle.FIXED, (p, age) -> {
                float in = Math.min(1.0F, age / 10.0F), out = Math.min(1.0F, (life - age) / 10.0F);
                Quaternionf q = switch (k) {
                    case 0 -> new Quaternionf().rotationY(age * 0.05F).rotateX(-Mth.HALF_PI + 0.12F);
                    case 1 -> new Quaternionf().rotationY(-age * 0.04F).rotateX(1.0F + age * 0.05F);
                    default -> new Quaternionf().rotationZ(age * 0.06F).rotateY(age * 0.08F);
                };
                p.at(centre.apply(age)).orient(q).frame(DreamSprites.WHEEL + ((age + k * 9) % 31 < 2 ? 1 : 0)).size(sizes[k] * in * out).alpha(1.0F);
            });
        }
        spawn(level, at, DreamSprites.GLOW, life, DreamParticle.BILLBOARD, (p, age) -> {
            p.at(centre.apply(age)).tint(0xFFF2B0).size(4.5F * Math.min(1.0F, age / 10.0F) * Math.min(1.0F, (life - age) / 10.0F)).alpha(0.5F);
            if (age == 24 || age == 40) {
                level.addParticle(VfxContent.wave(0xFFF2B0), at.x, at.y + 0.1, at.z, radius, 0, 0);
            }
        });
        // fachos de luz
        for (int i = 0; i < 14; i++) {
            double ang = rf() * Math.PI * 2, r = Math.sqrt(rf()) * radius * 0.9;
            double x = at.x + Math.cos(ang) * r, z = at.z + Math.sin(ang) * r;
            double gy = ground(level, x, at.y, z);
            double y = Double.isNaN(gy) ? at.y : gy;
            int delay = 22 + RND.nextInt(36);
            float half = 6.0F;
            spawn(level, new Vec3(x, y + half, z), DreamSprites.THREAD, delay + 22, DreamParticle.AXIS, after(delay, (p, t) -> {
                p.along(new Vec3(0, 1, 0)).tint(0xFFFFFF).size(half).alpha(0.55F * Math.min(1.0F, t / 4.0F) * Math.min(1.0F, (22 - t) / 8.0F));
            }));
        }
        for (int i = 0; i < 60; i++) {
            double ang = rf() * Math.PI * 2, r = Math.sqrt(rf()) * radius;
            feather(level, at.add(Math.cos(ang) * r, 5.0 + rf() * 5.0, Math.sin(ang) * r), new Vec3(rs(0.02), 0, rs(0.02)), 0.09, 20 + RND.nextInt(40), 50);
        }
    }

    // ================================================================== TEMPO (resto)

    private static Vec3 headOf(ClientLevel level, int entityId, Vec3 fallback, double above) {
        Entity e = entity(level, entityId);
        return e != null ? e.position().add(0, e.getBbHeight() + above, 0) : fallback.add(0, 1.8 + above, 0);
    }

    private static void grain(ClientLevel level, Vec3 from, Vec3 velocity, int life) {
        spawn(level, from, DreamSprites.GRAIN, life, DreamParticle.BILLBOARD,
                (p, age) -> p.at(from.add(velocity.scale(age))).size(0.05F).alpha(1.0F - age / (float) life));
    }

    /** Anel de algarismos girando em volta de uma entidade. {@code speed} em rad/tick; some depois de {@code life}. */
    private static void numeralRing(ClientLevel level, int entityId, Vec3 at, double radius, double height, double speed, float size, int life, boolean shrink) {
        for (int i = 0; i < 12; i++) {
            final int k = i;
            spawn(level, at, DreamSprites.NUMERAL + ((i + 11) % 12), life, DreamParticle.BILLBOARD, (p, age) -> {
                Entity e = entity(level, entityId);
                Vec3 c = e != null ? e.position() : at;
                double ang = Math.toRadians(k * 30.0) + age * speed;
                double r = shrink ? radius * (1.0 - 0.6 * age / (double) life) : radius;
                p.at(c.x + Math.sin(ang) * r, c.y + height, c.z - Math.cos(ang) * r).size(size).alpha(Math.min(1.0F, age / 3.0F) * Math.min(1.0F, (life - age) / 5.0F));
            });
        }
    }

    /** Acelerar: dão corda no mago. Uma chave gira aos trancos nas costas dele e os algarismos disparam em volta. */
    private static void quicken(ClientLevel level, int casterId, Vec3 at) {
        spawn(level, at, DreamSprites.KEY, 18, DreamParticle.BILLBOARD, (p, age) -> {
            Entity e = entity(level, casterId);
            Vec3 back = e != null ? e.position().add(0, e.getBbHeight() * 0.65, 0).subtract(e.getLookAngle().multiply(1, 0, 1).normalize().scale(0.5)) : at.add(0, 1.1, 0);
            p.at(back).size(0.3F).roll((age / 2) * 0.9F).alpha(age > 13 ? (18 - age) / 5.0F : 1.0F);
            if (age % 2 == 0) {
                grain(level, back, new Vec3(rs(0.06), 0.02 + rf() * 0.05, rs(0.06)), 10);
            }
        });
        numeralRing(level, casterId, at, 1.2, 1.0, 0.7, 0.14F, 16, true);
    }

    /** Presságio: três relógios de bolso orbitam a cabeça, cada um marcando uma hora diferente. */
    private static void foresight(ClientLevel level, int casterId, Vec3 at) {
        int life = 36;
        for (int i = 0; i < 3; i++) {
            final int k = i;
            spawn(level, at, DreamSprites.WATCH + i, life, DreamParticle.BILLBOARD, (p, age) -> {
                Vec3 head = headOf(level, casterId, at, 0.15);
                double ang = Math.PI * 2 * k / 3.0 + age * 0.2;
                p.at(head.x + Math.cos(ang) * 0.8, head.y + Math.sin(age * 0.25 + k) * 0.08, head.z + Math.sin(ang) * 0.8);
                p.frame(DreamSprites.WATCH + (k + age / 6) % 3).size(0.2F).roll(Mth.sin(age * 0.3F + k) * 0.15F);
                p.alpha(Math.min(1.0F, age / 3.0F) * Math.min(1.0F, (life - age) / 6.0F));
            });
        }
    }

    /** Ampulheta que vira de ponta-cabeça, com a areia subindo. */
    private static void hourglassFlip(ClientLevel level, Vec3 at, float size, int life) {
        spawn(level, at, DreamSprites.HOURGLASS, life, DreamParticle.BILLBOARD, (p, age) -> {
            float f = Math.min(1.0F, age / 10.0F);
            f = f * f * (3.0F - 2.0F * f);
            p.size(size).roll(Mth.PI * f).alpha(Math.min(1.0F, age / 2.0F) * Math.min(1.0F, (life - age) / 5.0F));
            if (age > 4 && age % 2 == 0) {
                grain(level, at.add(rs(0.15), 0, rs(0.15)), new Vec3(0, 0.07, 0), 12);
            }
        });
    }

    /** Rebobinar: a ampulheta vira e os algarismos contam para trás (V, IV, III, II, I) pelo caminho de volta. */
    private static void rewind(ClientLevel level, Vec3 from, Vec3 to) {
        if (from.distanceToSqr(to) < 0.01) {
            hourglassFlip(level, to.add(0, 2.4, 0), 0.4F, 20);
            return;
        }
        hourglassFlip(level, from.add(0, 1.2, 0), 0.45F, 20);
        for (int i = 0; i < 5; i++) {
            Vec3 pos = from.lerp(to, (i + 1) / 6.0).add(0, 1.2, 0);
            spawn(level, pos, DreamSprites.NUMERAL + (4 - i), i * 2 + 16, DreamParticle.BILLBOARD,
                    after(i * 2, (p, t) -> p.size(t < 2 ? 0.34F : 0.24F).alpha(1.0F - t / 16.0F)));
        }
    }

    /** Parar o Tempo: o mostrador grande dá três trancos, treme e trava; a areia fica parada no ar. */
    private static void timeStop(ClientLevel level, int casterId, Vec3 at, float radius) {
        int life = 72;
        double r = Math.min(radius, 9.0) * 0.8;
        for (int i = 0; i < 12; i++) {
            final int k = i;
            spawn(level, at, DreamSprites.NUMERAL + ((i + 11) % 12), life, DreamParticle.BILLBOARD, (p, age) -> {
                double step = Math.min(3, age / 4) * 30.0 + (age % 4 == 0 && age <= 12 ? 6.0 : 0.0);
                double shudder = age > 14 && age < 22 ? Math.sin(age * 2.6) * 3.0 : 0.0;
                double ang = Math.toRadians(k * 30.0 + step + shudder);
                p.at(at.x + Math.sin(ang) * r, at.y + 1.7, at.z - Math.cos(ang) * r).size(k == 0 ? 0.6F : 0.48F);
                p.alpha(Math.min(1.0F, age / 3.0F) * Math.min(1.0F, (life - age) / 8.0F));
            });
        }
        spawn(level, at, DreamSprites.WATCH, life, DreamParticle.BILLBOARD, (p, age) -> {
            p.at(headOf(level, casterId, at, 1.6)).frame(DreamSprites.WATCH + Math.min(3, age / 4) % 3).size(0.75F).roll(age > 14 && age < 22 ? Mth.sin(age * 2.6F) * 0.1F : 0.0F);
            p.alpha(Math.min(1.0F, age / 3.0F) * Math.min(1.0F, (life - age) / 8.0F));
        });
        for (int i = 0; i < 70; i++) {
            double ang = rf() * Math.PI * 2, d = Math.sqrt(rf()) * radius;
            Vec3 start = at.add(Math.cos(ang) * d, 0.6 + rf() * 3.6, Math.sin(ang) * d);
            spawn(level, start, DreamSprites.GRAIN, life, DreamParticle.BILLBOARD, (p, age) -> {
                double fall = Math.min(age, 14) * 0.05;
                double drop = age > life - 10 ? (age - (life - 10)) * (age - (life - 10)) * 0.03 : 0.0;
                p.at(start.x, start.y - fall - drop, start.z).size(0.05F).alpha(age > 14 ? 0.7F + 0.3F * Mth.sin(age * 0.8F + p.r1 * 6.0F) : age / 4.0F);
            });
        }
    }

    private static void timeMark(ClientLevel level, int targetId, Vec3 at, int duration) {
        int life = Math.max(20, Math.min(duration, 100));
        int frame = RND.nextInt(3);
        spawn(level, at, DreamSprites.WATCH + frame, life, DreamParticle.BILLBOARD, (p, age) -> {
            p.at(headOf(level, targetId, at, 0.5)).size(0.22F).roll(age % 12 == 0 ? 0.12F : 0.0F).alpha(Math.min(1.0F, age / 3.0F) * Math.min(1.0F, (life - age) / 6.0F));
        });
    }

    /** Instante Eterno: a ampulheta gira sem parar sobre o mago e dois mostradores rodam em sentidos opostos. */
    private static void eternal(ClientLevel level, int casterId, Vec3 at) {
        int life = 44;
        spawn(level, at, DreamSprites.HOURGLASS, life, DreamParticle.BILLBOARD, (p, age) -> {
            Vec3 head = headOf(level, casterId, at, 0.9);
            p.at(head).size(0.5F).roll(age * 0.55F).alpha(Math.min(1.0F, age / 3.0F) * Math.min(1.0F, (life - age) / 6.0F));
            grain(level, head, new Vec3(rs(0.12), rs(0.08), rs(0.12)), 10);
            if (age == life - 6) {
                pulse(level, head, 0xFFE9A0, 0.3F, 2.2F, 0.7F, 7);
            }
        });
        numeralRing(level, casterId, at, 1.4, 0.6, 0.45, 0.18F, life, false);
        numeralRing(level, casterId, at, 2.0, 1.7, -0.3, 0.22F, life, false);
    }

    // ================================================================== ESPAÇO (resto)

    /** Pálpebra pequena que abre, fica {@code hold} ticks e fecha. {@code facing} nulo = de frente para a câmera. */
    private static void lid(ClientLevel level, Vec3 at, @Nullable Quaternionf facing, float size, int hold) {
        int life = 5 + hold + 6;
        spawn(level, at, DreamSprites.SLIT, life, facing == null ? DreamParticle.BILLBOARD : DreamParticle.FIXED, (p, age) -> {
            int f = age < 5 ? age : age > 5 + hold ? Math.max(0, 4 - (age - 5 - hold)) : 4;
            if (facing != null) {
                p.orient(facing);
            }
            p.frame(DreamSprites.SLIT + Math.min(4, f)).size(size).alpha(1.0F);
        });
    }

    private static void starTo(ClientLevel level, Vec3 from, java.util.function.Supplier<Vec3> to, int delay, int travel, double curve) {
        Vec3 side = new Vec3(rs(1.0), rs(0.6), rs(1.0)).scale(curve);
        float size = 0.05F + rf() * 0.06F, spin = (rf() - 0.5F) * 0.6F;
        spawn(level, from, DreamSprites.STAR, delay + travel + 1, DreamParticle.BILLBOARD, after(delay, (p, t) -> {
            float f = Math.min(1.0F, t / (float) travel);
            Vec3 pos = from.lerp(to.get(), f * f).add(side.scale(Math.sin(f * Math.PI)));
            p.at(pos).size(size * (1.0F - 0.4F * f)).roll(spin * t).alpha(t >= travel ? 0.0F : 1.0F);
        }));
    }

    /** Puxão: uma pálpebra abre no alvo e outra na mão; as estrelas correm de uma para a outra por um fio. */
    private static void pull(ClientLevel level, Vec3 target, Vec3 hand) {
        lid(level, target, null, 0.55F, 6);
        lid(level, hand, null, 0.3F, 8);
        thread(level, target, hand, 0, 0.5F, 6, 0.0, 0xC89BFF, false);
        for (int i = 0; i < 12; i++) {
            starTo(level, target.add(rs(0.2), rs(0.2), rs(0.2)), () -> hand, i / 2, 6 + RND.nextInt(3), 0.15);
        }
    }

    /** Telecinese: as estrelas de toda a volta caem em espiral numa pálpebra que abre no peito do mago. */
    private static void telekinesis(ClientLevel level, int casterId, Vec3 at, float radius) {
        Vec3[] centre = {at.add(0, 1.1, 0)};
        spawn(level, centre[0], DreamSprites.SLIT, 22, DreamParticle.BILLBOARD, (p, age) -> {
            centre[0] = chest(level, casterId, at);
            int f = age < 4 ? age + 1 : age > 16 ? Math.max(0, 4 - (age - 16)) : 4;
            p.at(centre[0]).frame(DreamSprites.SLIT + Math.min(4, f)).size(0.5F).alpha(1.0F);
        });
        for (int i = 0; i < 30; i++) {
            double ang = rf() * Math.PI * 2, r = radius * (0.4 + rf() * 0.6);
            starTo(level, at.add(Math.cos(ang) * r, 0.3 + rf() * 2.0, Math.sin(ang) * r), () -> centre[0], RND.nextInt(8), 8 + RND.nextInt(5), 1.2);
        }
    }

    /** Âncora marcada: o chão pisca e uma estrela sobe e fica presa ali um tempo. */
    private static void anchorSet(ClientLevel level, Vec3 at) {
        double gy = ground(level, at.x, at.y, at.z);
        Vec3 floor = new Vec3(at.x, (Double.isNaN(gy) ? at.y : gy) + 0.05, at.z);
        lid(level, floor, new Quaternionf().rotationX(-Mth.HALF_PI), 1.1F, 6);
        spawn(level, floor, DreamSprites.STAR, 50, DreamParticle.BILLBOARD, (p, age) -> {
            float up = Math.min(1.0F, age / 12.0F);
            up = 1.0F - (1.0F - up) * (1.0F - up);
            p.at(floor.x, floor.y + up * 1.7, floor.z).size(0.18F + 0.05F * Mth.sin(age * 0.5F)).roll(age * 0.06F).alpha(Math.min(1.0F, (50 - age) / 10.0F));
        });
        thread(level, floor, floor.add(0, 1.7, 0), 6, 0.4F, 30, 0.0, 0xC89BFF, false);
    }

    /** Fenda: uma pálpebra pequena abre na frente do mago e fica aberta enquanto ele mexe no baú; estrelas escapam. */
    private static void rift(ClientLevel level, Vec3 at, Vec3 look) {
        Vec3 flat = new Vec3(look.x, 0, look.z);
        Quaternionf q = new Quaternionf().rotationY((float) Math.atan2(flat.x, flat.z));
        lid(level, at, q, 0.6F, 26);
        for (int i = 0; i < 14; i++) {
            Vec3 from = at.add(rs(0.1), rs(0.4), rs(0.1));
            Vec3 drift = new Vec3(rs(0.03), 0.01 + rf() * 0.03, rs(0.03));
            int delay = 4 + RND.nextInt(22);
            spawn(level, from, DreamSprites.STAR, delay + 16, DreamParticle.BILLBOARD,
                    after(delay, (p, t) -> p.at(from.add(drift.scale(t))).size(0.06F).roll(t * 0.2F).alpha(1.0F - t / 16.0F)));
        }
    }

    private static Vec3 towardCamera(Vec3 pos, double distance) {
        Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().position();
        Vec3 d = cam.subtract(pos);
        return d.lengthSqr() < 1.0E-4 ? pos : pos.add(d.normalize().scale(distance));
    }

    /** Buraco Negro: um olho do tamanho de uma casa abre no ar; a pupila é o buraco, e as estrelas caem nela em espiral. */
    private static void blackHole(ClientLevel level, Vec3 at, float radius, int duration) {
        int life = Math.max(30, duration);
        spawn(level, at, DreamSprites.SLIT, life, DreamParticle.BILLBOARD, (p, age) -> {
            int f = age < 10 ? age / 2 : age > life - 8 ? Math.max(0, (life - age) / 2) : 4;
            p.frame(DreamSprites.SLIT + Math.min(4, f)).size(4.4F).roll(Mth.HALF_PI).alpha(1.0F); // deitada: um olho, não um rasgo
            if (age > 6 && age < life - 8) {
                for (int i = 0; i < 3; i++) {
                    Vec3 dir = new Vec3(rs(1.0), rs(0.5), rs(1.0));
                    if (dir.lengthSqr() > 0.01) {
                        starTo(level, at.add(dir.normalize().scale(radius * (0.5 + rf() * 0.5))), () -> at, 0, 14 + RND.nextInt(8), 2.0);
                    }
                }
            }
            if (age == life - 8) {
                pulse(level, at, 0xC050FF, 3.0F, 0.2F, 0.8F, 8);
            }
        });
        spawn(level, at, DreamSprites.GLOW, life, DreamParticle.BILLBOARD, (p, age) -> {
            float open = age < 10 ? age / 10.0F : age > life - 8 ? Math.max(0.0F, (life - age) / 8.0F) : 1.0F;
            p.at(towardCamera(at, 0.4)).tint(0x000000).size((1.7F + 0.12F * Mth.sin(age * 0.3F)) * open).alpha(1.0F);
        });
    }

    // ================================================================== VIDA (resto)

    private static void leaves(ClientLevel level, Vec3 at, int count, double speed) {
        for (int i = 0; i < count; i++) {
            int life = 16 + RND.nextInt(10);
            double vx = rs(speed), vy = 0.05 + rf() * speed, vz = rs(speed), phase = rf() * 6.28;
            float spin = (rf() - 0.5F) * 0.6F;
            spawn(level, at, DreamSprites.LEAF, life, DreamParticle.BILLBOARD, (p, age) -> {
                double slow = 1.0 - Math.pow(0.85, age);
                p.at(at.x + vx * slow * 7.0 + Math.sin(age * 0.3 + phase) * 0.08, at.y + vy * slow * 7.0 - 0.004 * age * age, at.z + vz * slow * 7.0);
                p.size(0.08F).roll(spin * age).alpha(age > life - 5 ? (life - age) / 5.0F : 1.0F);
            });
        }
    }

    /** Broto: cogumelos nascem em volta, soltam esporos e somem; folhas sobem em volta de quem foi curado. */
    private static void sprout(ClientLevel level, int casterId, Vec3 at, float radius) {
        for (int i = 0; i < 16; i++) {
            double ang = rf() * Math.PI * 2, r = 0.8 + Math.sqrt(rf()) * (radius - 0.8);
            double x = at.x + Math.cos(ang) * r, z = at.z + Math.sin(ang) * r;
            double y = ground(level, x, at.y, z);
            if (Double.isNaN(y)) {
                continue;
            }
            int delay = RND.nextInt(12);
            float size = 0.22F + rf() * 0.2F;
            spawn(level, new Vec3(x, y, z), DreamSprites.SHROOM, delay + 44, DreamParticle.UPRIGHT, after(delay, (p, t) -> {
                int f = t > 36 ? Math.max(0, 2 - (t - 36) / 3) : Math.min(2, t / 3);
                float s = size * (t < 9 ? 1.0F + 0.15F * Mth.sin(t * 1.2F) : 1.0F);
                p.at(x, y + s, z).frame(DreamSprites.SHROOM + f).size(s).alpha(t > 40 ? (44 - t) / 4.0F : 1.0F);
                if (t == 12 || t == 22) {
                    for (int k = 0; k < 3; k++) {
                        Vec3 spore = new Vec3(x + rs(0.15), y + s * 1.6, z + rs(0.15));
                        spawn(level, spore, DreamSprites.GLOW, 20, DreamParticle.BILLBOARD,
                                (g, a) -> g.at(spore.x, spore.y + 0.025 * a, spore.z).tint(0xD8FF6A).size(0.05F).alpha(1.0F - a / 20.0F));
                    }
                }
            }));
        }
        for (int i = 0; i < 10; i++) {
            double phase = Math.PI * 2 * i / 10.0;
            spawn(level, at, DreamSprites.LEAF, 26, DreamParticle.BILLBOARD, (p, age) -> {
                Entity caster = entity(level, casterId);
                Vec3 c = caster != null ? caster.position() : at;
                double a = phase + age * 0.3;
                p.at(c.x + Math.cos(a) * 0.75, c.y + 0.1 + age * 0.08, c.z + Math.sin(a) * 0.75).size(0.08F).alpha(age > 18 ? (26 - age) / 8.0F : 1.0F).roll((float) a);
            });
        }
    }

    /** O cipó: gomos de ramo com folhas, emendados de ponta a ponta, com uma ondulação que anda. */
    private static void vine(ClientLevel level, Vec3 from, Vec3 to, int life, int tick) {
        Vec3 span = to.subtract(from);
        double dist = span.length();
        if (dist < 0.3) {
            return;
        }
        Vec3 dir = span.scale(1.0 / dist);
        Vec3[] uw = basis(dir);
        int n = Math.max(2, (int) Math.ceil(dist / 0.75));
        Vec3 prev = from;
        for (int i = 1; i <= n; i++) {
            double f = i / (double) n;
            double sag = Math.sin(f * Math.PI);
            Vec3 point = i == n ? to : from.add(span.scale(f)).add(uw[0].scale(Math.sin(f * 9.0 + tick * 0.4) * 0.1 * sag)).add(uw[1].scale(Math.cos(f * 9.0 + tick * 0.4) * 0.1 * sag));
            Vec3 seg = point.subtract(prev);
            Vec3 mid = prev.add(point).scale(0.5);
            float half = (float) (seg.length() * 0.5) * 1.06F;
            spawn(level, mid, DreamSprites.VINE, life, DreamParticle.AXIS, (p, age) -> p.along(seg).size(half).alpha(life > 6 && age > life - 5 ? (life - age) / 5.0F : 1.0F));
            prev = point;
        }
    }

    private static void vineLatch(ClientLevel level, Vec3 at) {
        leaves(level, at, 10, 0.12);
        spawn(level, at, DreamSprites.FLOWER, 24, DreamParticle.BILLBOARD,
                (p, age) -> p.frame(DreamSprites.FLOWER + Math.min(4, age / 2)).size(0.26F).alpha(age > 18 ? (24 - age) / 6.0F : 1.0F));
    }

    /** Espinheiro que sobe do chão, fica e afunda. {@code followId} >= 0: fecha em volta daquela criatura e anda com ela. */
    private static void bramble(ClientLevel level, Vec3 base, Vec3 centre, float size, int delay, int life, int followId, Vec3 offset) {
        spawn(level, base, DreamSprites.BRAMBLE, delay + life, DreamParticle.FIXED, after(delay, (p, t) -> {
            float up = Math.min(1.0F, t / 5.0F);
            float down = Math.max(0.0F, (t - (life - 6)) / 6.0F);
            Vec3 b = base, c = centre;
            Entity e = entity(level, followId);
            if (e != null) {
                c = e.position();
                b = c.add(offset);
            }
            Vec3 to = new Vec3(c.x - b.x, 0, c.z - b.z);
            Vec3 flatTo = to.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : to.normalize();
            float lean = followId >= 0 ? 0.45F * up : 0.0F;
            p.at(b.x + flatTo.x * lean * size * 0.5, b.y + size * (2.0 * up - 1.0) - size * 2.0 * down * down, b.z + flatTo.z * lean * size * 0.5);
            p.face(flatTo.add(0, -lean, 0)).size(size).alpha(1.0F);
        }));
    }

    private static void thorns(ClientLevel level, Vec3 at, float radius) {
        for (int i = 0; i < 22; i++) {
            boolean rim = i < 14;
            double ang = rim ? Math.PI * 2 * i / 14.0 : rf() * Math.PI * 2;
            double r = rim ? radius * 0.92 : Math.sqrt(rf()) * radius * 0.8;
            double x = at.x + Math.cos(ang) * r, z = at.z + Math.sin(ang) * r;
            double y = ground(level, x, at.y, z);
            if (!Double.isNaN(y)) {
                bramble(level, new Vec3(x, y, z), at, 0.6F + rf() * 0.45F, RND.nextInt(6), 54, -1, Vec3.ZERO);
            }
        }
        leaves(level, at.add(0, 0.4, 0), 16, 0.18);
    }

    private static void thornBind(ClientLevel level, int targetId, Vec3 at, int duration) {
        Entity target = entity(level, targetId);
        double r = (target != null ? target.getBbWidth() * 0.5 : 0.3) + 0.4;
        float size = target != null ? Math.max(0.6F, target.getBbHeight() * 0.55F) : 0.8F;
        int life = Math.max(20, Math.min(duration, 90));
        for (int i = 0; i < 4; i++) {
            double ang = Math.PI * 2 * i / 4.0 + 0.5;
            Vec3 off = new Vec3(Math.cos(ang) * r, 0, Math.sin(ang) * r);
            bramble(level, at.add(off), at, size, i, life, targetId, off);
        }
    }

    /** Guardião de madeira: uma vagem sai do chão, treme e abre. */
    private static void pod(ClientLevel level, Vec3 at) {
        double gy = ground(level, at.x, at.y, at.z);
        Vec3 base = new Vec3(at.x, Double.isNaN(gy) ? at.y : gy, at.z);
        float size = 0.9F;
        spawn(level, base, DreamSprites.POD, 32, DreamParticle.UPRIGHT, (p, age) -> {
            float up = Math.min(1.0F, age / 8.0F);
            up = 1.0F - (1.0F - up) * (1.0F - up);
            boolean open = age >= 15;
            p.at(base.x, base.y + size * (2.0 * up - 1.0), base.z).frame(DreamSprites.POD + (open ? 1 : 0)).size(size).roll(open ? 0.0F : Mth.sin(age * 1.6F) * 0.08F);
            p.alpha(age > 24 ? (32 - age) / 8.0F : 1.0F);
            if (age == 15) {
                pulse(level, base.add(0, size * 1.2, 0), 0xD8FF6A, 0.3F, 1.8F, 0.6F, 7);
                leaves(level, base.add(0, size, 0), 14, 0.16);
            }
        });
    }

    /** Árvore da Vida: a árvore cresce, respira e fica; folhas caem da copa, flores abrem em volta a cada batida. */
    private static void tree(ClientLevel level, Vec3 at, float radius, int duration) {
        double gy = ground(level, at.x, at.y, at.z);
        Vec3 base = new Vec3(at.x, Double.isNaN(gy) ? at.y : gy, at.z);
        int life = Math.max(60, duration);
        float full = 4.6F;
        spawn(level, base, DreamSprites.TREE, life, DreamParticle.UPRIGHT, (p, age) -> {
            float grow = Math.min(1.0F, age / 40.0F);
            grow = 1.0F - (1.0F - grow) * (1.0F - grow);
            float fade = Math.min(1.0F, (life - age) / 30.0F);
            float s = full * grow * (1.0F + 0.015F * Mth.sin(age * Mth.TWO_PI / 40.0F)) * (0.6F + 0.4F * fade);
            p.at(base.x, base.y + s, base.z).size(s).alpha(fade);
            if (grow < 1.0F || age > life - 30) {
                return;
            }
            if (age % 4 == 0) {
                Vec3 top = base.add(rs(3.2), full * (1.2 + rf() * 0.5), rs(3.2));
                int fall = 50;
                double phase = rf() * 6.28;
                spawn(level, top, RND.nextInt(3) == 0 ? DreamSprites.PETAL : DreamSprites.LEAF, fall, DreamParticle.BILLBOARD, (q, t) -> {
                    q.at(top.x + Math.sin(t * 0.15 + phase) * 0.4, top.y - 0.09 * t, top.z + Math.cos(t * 0.13 + phase) * 0.4).size(0.08F).roll((float) (t * 0.1 + phase));
                    q.alpha(t > fall - 10 ? (fall - t) / 10.0F : 1.0F);
                });
            }
            if (age % 20 == 0) {
                level.addParticle(VfxContent.wave(0x9AD84A), base.x, base.y + 0.1, base.z, radius, 0, 0);
                for (int i = 0; i < 6; i++) {
                    double ang = rf() * Math.PI * 2, r = 1.5 + Math.sqrt(rf()) * (radius - 1.5);
                    double x = base.x + Math.cos(ang) * r, z = base.z + Math.sin(ang) * r;
                    double y = ground(level, x, base.y, z);
                    if (Double.isNaN(y)) {
                        continue;
                    }
                    spawn(level, new Vec3(x, y, z), DreamSprites.FLOWER, 34, DreamParticle.UPRIGHT, (f, t) -> {
                        int frame = t > 22 ? Math.max(0, 4 - (t - 22) / 3) : Math.min(4, t / 2);
                        f.at(x, y + 0.28, z).frame(DreamSprites.FLOWER + frame).size(0.28F).alpha(1.0F);
                    });
                }
            }
            if (age % 5 == 0) {
                Vec3 spore = base.add(rs(1.5), 0.3, rs(1.5));
                spawn(level, spore, DreamSprites.GLOW, 40, DreamParticle.BILLBOARD,
                        (g, a) -> g.at(spore.x, spore.y + 0.06 * a, spore.z).tint(0xD8FF6A).size(0.06F).alpha(1.0F - a / 40.0F));
            }
        });
    }

    // ================================================================== MAGMA

    /** Rio de Lava: o chão racha em brasa pelo caminho e uma procissão de gente de fogo anda por cima, indo e voltando. */
    private static void lavaRiver(ClientLevel level, Vec3 start, Vec3 dir, int duration) {
        Vec3 flat = dir.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : dir.normalize();
        double angle = Math.atan2(flat.z, flat.x);
        for (int i = 0; i < 6; i++) {
            crack(level, start.add(flat.scale(0.5 + i * 2.0)), angle + rs(0.12), 2.3F, i * 2, duration - i * 2);
        }
        for (int i = 0; i < 9; i++) {
            final int k = i;
            double offset = rs(0.35), phase = i / 9.0;
            spawn(level, start, DreamSprites.DANCER, duration, DreamParticle.UPRIGHT, (p, age) -> {
                double f = (phase + age * 0.012) % 1.0;
                double along = 1.0 + (f < 0.5 ? f * 2.0 : 2.0 - f * 2.0) * 11.0; // vai até o fim e volta
                double x = start.x + flat.x * along - flat.z * offset, z = start.z + flat.z * along + flat.x * offset;
                double y = ground(level, x, start.y, z);
                float s = 0.26F;
                p.at(x, (Double.isNaN(y) ? start.y : y) + s + Math.abs(Math.sin(age * 0.4 + k)) * 0.1, z).frame(DreamSprites.DANCER + ((age + k) / 4) % 2).size(s);
                p.alpha(Math.min(1.0F, age / 6.0F) * Math.min(1.0F, (duration - age) / 8.0F));
                if ((age + k * 3) % 6 == 0) {
                    ash(level, p.pos());
                }
            });
        }
    }

    /** Erupção: uma cabeça de pedra sobe, abre a boca e vomita brasa para o alto; o chão racha em volta. */
    private static void eruption(ClientLevel level, Vec3 at, float radius) {
        double gy = ground(level, at.x, at.y, at.z);
        Vec3 base = new Vec3(at.x, Double.isNaN(gy) ? at.y : gy, at.z);
        float size = 1.6F;
        int life = 40;
        spawn(level, base, DreamSprites.HEAD, life, DreamParticle.UPRIGHT, (p, age) -> {
            float up = Math.min(1.0F, age / 5.0F);
            float down = Math.max(0.0F, (age - (life - 10)) / 10.0F);
            p.at(base.x, base.y + size * (2.0 * up - 1.0) - size * 2.0 * down * down, base.z).frame(DreamSprites.HEAD + (age >= 4 ? 1 : 0)).size(size).tint(0xFFD0B0).alpha(1.0F);
            if (age >= 5 && age < 26) {
                Vec3 mouth = base.add(0, size * 0.6, 0);
                for (int i = 0; i < 3; i++) {
                    double vx = rs(0.12), vz = rs(0.12), vy = 0.5 + rf() * 0.35;
                    int fly = 18 + RND.nextInt(8);
                    int k = RND.nextInt(4);
                    spawn(level, mouth, DreamSprites.MOTH, fly, DreamParticle.BILLBOARD, (m, t) -> {
                        m.at(mouth.x + vx * t, mouth.y + vy * t - 0.02 * t * t, mouth.z + vz * t).frame(t > fly * 0.6 ? DreamSprites.ASH : DreamSprites.MOTH + ((t + k) / 2) % 2);
                        m.size(t > fly * 0.6 ? 0.1F : 0.18F).roll(t * 0.2F).alpha(t > fly - 5 ? (fly - t) / 5.0F : 1.0F);
                    });
                }
                level.addParticle(VfxContent.ember(0xFF8A2A), mouth.x, mouth.y, mouth.z, rs(0.1), 0.3 + rf() * 0.2, rs(0.1));
            }
            if (age == 5) {
                pulse(level, base.add(0, 1.0, 0), 0xFF8A2A, 0.5F, radius, 0.8F, 9);
            }
        });
        for (int i = 0; i < 9; i++) {
            crack(level, base, Math.PI * 2 * i / 9.0 + rs(0.15), radius, RND.nextInt(3), 44);
        }
        pebbles(level, base.add(0, 0.2, 0), 18, 0.3);
    }

    private static void magmaArmor(ClientLevel level, int targetId, Vec3 at) {
        for (int i = 0; i < 6; i++) {
            crack(level, at, Math.PI * 2 * i / 6.0 + 0.3, 1.6F, i, 30);
        }
        for (int i = 0; i < 14; i++) {
            Vec3 off = new Vec3(rs(1.0), rs(0.5), rs(1.0)).normalize().scale(1.6 + rf());
            int delay = 4 + RND.nextInt(8);
            spawn(level, at, DreamSprites.PEBBLE + i % 2, delay + 9, DreamParticle.BILLBOARD, after(delay, (p, t) -> {
                float f = t / 9.0F;
                p.at(chest(level, targetId, at).add(off.scale(1.0 - f * f))).tint(0xFFB070).size(0.1F).roll(t * 0.5F).alpha(1.0F);
            }));
        }
        for (int i = 0; i < 6; i++) {
            final int k = i;
            double phase = Math.PI * 2 * i / 6.0;
            spawn(level, at, DreamSprites.MOTH, 22, DreamParticle.BILLBOARD, (p, age) -> {
                Vec3 c = chest(level, targetId, at);
                double ang = phase + age * 0.4, r = 1.3 * (1.0 - age / 24.0);
                p.at(c.x + Math.cos(ang) * r, c.y + Math.sin(age * 0.3 + k) * 0.4, c.z + Math.sin(ang) * r).frame(DreamSprites.MOTH + ((age + k) / 2) % 2).size(0.17F).alpha(age > 17 ? (22 - age) / 5.0F : 1.0F);
            });
        }
        spawn(level, at, DreamSprites.GLOW, 14, DreamParticle.BILLBOARD,
                (p, age) -> p.at(chest(level, targetId, at)).tint(0xFF8A2A).size(age < 8 ? 0.0F : 0.3F + (age - 8) * 0.2F).alpha(age < 8 ? 0.0F : 0.7F * (1.0F - (age - 8) / 6.0F)));
    }

    // ================================================================== VAPOR

    private static void puff(ClientLevel level, Vec3 from, java.util.function.IntFunction<Vec3> path, float from0, float to, int delay, int life, float alpha) {
        int beat = RND.nextInt(8);
        spawn(level, from, DreamSprites.PUFF, delay + life, DreamParticle.BILLBOARD, after(delay, (p, t) -> {
            float f = t / (float) life;
            p.at(path.apply(t)).frame(DreamSprites.PUFF + ((t + beat) / 5) % 2).size(Mth.lerp(f, from0, to));
            p.alpha(alpha * Math.min(1.0F, t / 4.0F) * Math.min(1.0F, (life - t) / 8.0F));
        }));
    }

    /** Névoa: fantasmas de vapor sobem do chão e rodam devagar em volta do mago, tapando a vista. */
    private static void mist(ClientLevel level, int casterId, Vec3 at, int duration) {
        Vec3[] centre = {at};
        for (int i = 0; i < 34; i++) {
            final int k = i;
            double phase = rf() * Math.PI * 2, r = 0.8 + rf() * 2.6, h = 0.3 + rf() * 2.2, turn = (0.02 + rf() * 0.03) * (i % 2 == 0 ? 1 : -1);
            int delay = RND.nextInt(Math.max(1, duration - 30));
            puff(level, at, t -> {
                if (k == 0) {
                    Entity caster = entity(level, casterId);
                    if (caster != null) {
                        centre[0] = caster.position();
                    }
                }
                double ang = phase + t * turn;
                return centre[0].add(Math.cos(ang) * r, h + t * 0.012, Math.sin(ang) * r);
            }, 0.3F, 0.75F + rf() * 0.4F, i < 12 ? 0 : delay, 34 + RND.nextInt(16), 0.8F);
        }
    }

    /** Jato de Vapor: os fantasmas disparam em leque, crescendo. */
    private static void steamJet(ClientLevel level, Vec3 from, Vec3 look, float reach) {
        Vec3 dir = look.normalize();
        Vec3[] uw = basis(dir);
        for (int i = 0; i < 22; i++) {
            double side = rs(1.0), lift = rs(1.0), speed = 0.6 + rf() * 0.5;
            puff(level, from, t -> {
                double d = (1.0 - Math.pow(0.86, t)) * reach * speed * 1.2;
                double open = d * 0.16;
                return from.add(dir.scale(0.5 + d)).add(uw[0].scale(side * open)).add(uw[1].scale(lift * open + t * 0.01));
            }, 0.12F, 0.5F + rf() * 0.3F, RND.nextInt(5), 16 + RND.nextInt(8), 0.85F);
        }
        drops(level, from.add(dir.scale(1.0)), 8, 0.15);
    }

    // ================================================================== NUVEM

    /** Ovelha de nuvem pulando de {@code from} para {@code to} em {@code hops} saltos; some num sopro. */
    private static void sheep(ClientLevel level, Vec3 from, Vec3 to, int hops, int delay, int perHop) {
        Vec3 span = to.subtract(from);
        Vec3 heading = span.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : span.normalize();
        int life = hops * perHop;
        spawn(level, from, DreamSprites.SHEEP, delay + life + 1, DreamParticle.FIXED, after(delay, (p, t) -> {
            double f = Math.min(1.0, t / (double) life);
            double hop = (t % perHop) / (double) perHop;
            double arc = Math.sin(hop * Math.PI);
            swim(p.at(from.add(span.scale(f)).add(0, 0.3 + arc * 0.7, 0)), heading).frame(DreamSprites.SHEEP + (arc > 0.25 ? 1 : 0)).size(0.36F);
            p.alpha(Math.min(1.0F, t / 3.0F) * (t >= life ? 0.0F : 1.0F));
            if (t == life - 1) {
                Vec3 end = p.pos();
                puff(level, end, k -> end.add(0, k * 0.03, 0), 0.25F, 0.6F, 0, 14, 0.8F);
            }
        }));
    }

    private static void sheepPlatform(ClientLevel level, Vec3 at) {
        for (int i = 0; i < 6; i++) {
            double ang = Math.PI * 2 * i / 6.0 + 0.3;
            sheep(level, at.add(0, 0.5, 0), at.add(Math.cos(ang) * 2.4, 0.5, Math.sin(ang) * 2.4), 2, i, 7);
        }
        puff(level, at, t -> at.add(0, 0.5 + t * 0.02, 0), 0.4F, 1.3F, 0, 16, 0.7F);
    }

    /** Ponte de Nuvem: contando carneirinhos. Eles pulam em fila pela ponte, um atrás do outro. */
    private static void sheepBridge(ClientLevel level, Vec3 start, Vec3 dir, int length) {
        Vec3 flat = dir.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : dir.normalize();
        Vec3 from = start.add(0, 0.5, 0);
        int hops = Math.max(3, length / 3);
        for (int i = 0; i < 7; i++) {
            sheep(level, from, from.add(flat.scale(length)), hops, i * 5, 6);
        }
    }

    // ================================================================== ALMA

    private static void candle(ClientLevel level, Vec3 at, float size, int delay, int life) {
        int beat = RND.nextInt(6);
        spawn(level, at, DreamSprites.CANDLE, delay + life, DreamParticle.UPRIGHT, after(delay, (p, t) -> {
            p.frame(DreamSprites.CANDLE + ((t + beat) / 3) % 2).size(size * Math.min(1.0F, t / 3.0F)).alpha(t > life - 5 ? (life - t) / 5.0F : 1.0F);
            if (t == life - 5) {
                Vec3 smoke = at.add(0, size * 0.8, 0);
                spawn(level, smoke, DreamSprites.GLOW, 14, DreamParticle.BILLBOARD, (g, a) -> g.at(smoke.x, smoke.y + a * 0.04, smoke.z).tint(0x9AA8B8).size(0.08F + a * 0.01F).alpha(0.5F * (1.0F - a / 14.0F)));
            }
        }));
    }

    /** Uma alma (com dois olhinhos) que sai de {@code from} e vai num arco até {@code to}. */
    private static void soul(ClientLevel level, Vec3 from, java.util.function.Supplier<Vec3> to, int delay) {
        double side = rs(0.8), lift = 0.8 + rf() * 1.0;
        int travel = 14;
        spawn(level, from, DreamSprites.SOUL, delay + travel + 1, DreamParticle.BILLBOARD, after(delay, (p, t) -> {
            float f = Math.min(1.0F, t / (float) travel);
            Vec3 end = to.get();
            Vec3 pos = from.lerp(end, f * f);
            double arc = Math.sin(f * Math.PI);
            p.at(pos.x + side * arc, pos.y + lift * arc, pos.z + side * arc).size(0.14F).roll(Mth.sin(t * 0.5F) * 0.3F).alpha(t >= travel ? 0.0F : 1.0F);
            if (t % 2 == 0) {
                sparkle(level, p.pos(), 0x3FD6E8);
            }
            if (t == travel) {
                pulse(level, end, 0xB8F6FF, 0.1F, 0.6F, 0.6F, 5);
            }
        }));
    }

    /** Espírito Guardião: uma vela acende sozinha no ar, a chama cresce e uma alma sai dela. */
    private static void candleSummon(ClientLevel level, Vec3 at) {
        candle(level, at.add(0, 0.4, 0), 0.5F, 0, 28);
        Vec3 flame = at.add(0, 0.8, 0);
        soul(level, flame, () -> flame.add(0, 1.4, 0), 10);
        pulse(level, flame, 0x3FD6E8, 0.2F, 1.4F, 0.6F, 10);
    }

    private static void soulTake(ClientLevel level, Vec3 from, int casterId, Vec3 fallback) {
        soul(level, from, () -> entity(level, casterId) != null ? chest(level, casterId, fallback) : fallback, RND.nextInt(4));
    }

    /** Colheita de Almas: uma roda de velas acende em volta do mago, uma a uma, e apaga quando as almas chegam. */
    private static void soulHarvest(ClientLevel level, int casterId, Vec3 at, float radius) {
        for (int i = 0; i < 10; i++) {
            double ang = Math.PI * 2 * i / 10.0;
            double x = at.x + Math.cos(ang) * 1.8, z = at.z + Math.sin(ang) * 1.8;
            double y = ground(level, x, at.y, z);
            candle(level, new Vec3(x, (Double.isNaN(y) ? at.y : y) + 0.3, z), 0.3F, i, 30 - i);
        }
        level.addParticle(VfxContent.wave(0x3FD6E8), at.x, at.y + 0.1, at.z, radius, 0, 0);
    }

    // ================================================================== ARCANO

    /** Engrenagem de luz girando num ponto que pode andar. {@code spin} em rad/tick (o sinal dá o sentido). */
    private static void gear(ClientLevel level, java.util.function.Supplier<Vec3> at, float size, float spin, int delay, int life) {
        spawn(level, at.get(), DreamSprites.GEAR, delay + life, DreamParticle.BILLBOARD, after(delay, (p, t) -> {
            p.at(at.get()).size(size * Math.min(1.0F, t / 3.0F) * Math.min(1.0F, (life - t) / 3.0F)).roll(spin * t).alpha(1.0F);
        }));
    }

    /** Recarregar: três engrenagens engatadas giram ao lado do mago e fios levam a carga até ele. */
    private static void recharge(ClientLevel level, int targetId, Vec3 at) {
        Vec3[] offs = {new Vec3(0.9, 1.5, 0.0), new Vec3(-0.75, 1.9, 0.5), new Vec3(-0.3, 0.9, -0.9)};
        float[] sizes = {0.3F, 0.22F, 0.26F};
        for (int i = 0; i < 3; i++) {
            final int k = i;
            java.util.function.Supplier<Vec3> where = () -> {
                Entity e = entity(level, targetId);
                return (e != null ? e.position() : at).add(offs[k]);
            };
            gear(level, where, sizes[i], (i % 2 == 0 ? 0.35F : -0.45F), i * 2, 26);
            spawn(level, at, DreamSprites.GLOW, 26, DreamParticle.BILLBOARD, (p, age) -> {
                if (age == 6 + k * 3 || age == 14 + k * 3) {
                    thread(level, where.get(), chest(level, targetId, at), 0, 0.6F, 4, 0.6, 0xE0A0FF, false);
                }
                p.alpha(0.0F);
            });
        }
        spawn(level, at, DreamSprites.GLOW, 26, DreamParticle.BILLBOARD,
                (p, age) -> p.at(chest(level, targetId, at)).tint(0xD07CFF).size(0.3F + 0.1F * Mth.sin(age * 0.8F)).alpha(0.45F * Math.min(1.0F, (26 - age) / 6.0F)));
    }

    /** Impulso Arcano: quatro engrenagens engatam nas faces da máquina; a cada segundo uma dá um tranco. */
    private static void machineBoost(ClientLevel level, Vec3 block, boolean start) {
        if (!start) {
            Vec3 spot = block.add(rs(0.7), 0.7 + rf() * 0.3, rs(0.7));
            gear(level, () -> spot, 0.16F, RND.nextBoolean() ? 0.5F : -0.5F, 0, 12);
            return;
        }
        Vec3[] faces = {new Vec3(0.75, 0.1, 0), new Vec3(-0.75, 0.1, 0), new Vec3(0, 0.1, 0.75), new Vec3(0, 0.1, -0.75), new Vec3(0, 0.85, 0)};
        for (int i = 0; i < faces.length; i++) {
            Vec3 spot = block.add(faces[i]);
            gear(level, () -> spot, i == 4 ? 0.36F : 0.26F, i % 2 == 0 ? 0.4F : -0.4F, i * 2, 36);
        }
        pulse(level, block, 0xD07CFF, 0.5F, 3.0F, 0.5F, 10);
    }

    /** Foco Arcano: uma engrenagem gira devagar atrás da cabeça e, num tranco, encaixa. */
    private static void arcaneFocus(ClientLevel level, int casterId, Vec3 at) {
        int life = 34;
        spawn(level, at, DreamSprites.GEAR, life, DreamParticle.BILLBOARD, (p, age) -> {
            Entity e = entity(level, casterId);
            Vec3 head = e != null ? e.position().add(0, e.getBbHeight() * 0.92, 0).subtract(e.getLookAngle().multiply(1, 0, 1).normalize().scale(0.35)) : at.add(0, 1.7, 0);
            float click = age > life - 8 ? (age - (life - 8)) / 8.0F : 0.0F;
            p.at(head).size(0.5F * Math.min(1.0F, age / 4.0F) * (1.0F - click * 0.8F)).roll((age / 3) * 0.39F).alpha(1.0F); // anda aos trancos, dente por dente
            if (age == life - 2) {
                pulse(level, head, 0xF3B6FF, 0.2F, 1.2F, 0.7F, 6);
            }
        });
    }

    // ================================================================== VIGÍLIA, SORTE, ECO, VAZIO

    /** Olhar Vigilante: uma coruja pousa sobre a cabeça do mago, arregala os olhos e vira a cabeça para os dois lados. */
    private static void vigil(ClientLevel level, int casterId, Vec3 at) {
        int life = 44;
        spawn(level, at, DreamSprites.OWL, life, DreamParticle.BILLBOARD, (p, age) -> {
            Vec3 head = casterId >= 0 ? headOf(level, casterId, at, 0.55) : at.add(0, 0.8, 0);
            boolean open = age >= 8 && age < life - 6 && (age % 19) > 1;
            float turn = age > 10 && age < life - 8 ? Mth.sin((age - 10) * 0.22F) * 0.5F : 0.0F;
            p.at(head.x, head.y + Math.max(0.0, 1.2 - age * 0.3), head.z).frame(DreamSprites.OWL + (open ? 1 : 0)).size(0.42F).roll(turn);
            p.alpha(Math.min(1.0F, age / 3.0F) * Math.min(1.0F, (life - age) / 5.0F));
            if (age == 8) {
                level.addParticle(VfxContent.wave(0xFFD23D), at.x, at.y + 0.1, at.z, 12, 0, 0);
            }
        });
    }

    private static void vigilMark(ClientLevel level, int targetId, Vec3 at) {
        int life = 36;
        spawn(level, at, DreamSprites.OWL + 1, life, DreamParticle.BILLBOARD, (p, age) -> {
            p.at(headOf(level, targetId, at, 0.4)).frame(DreamSprites.OWL + (age % 13 < 2 ? 0 : 1)).size(0.18F);
            p.alpha(Math.min(1.0F, age / 4.0F) * Math.min(1.0F, (life - age) / 6.0F));
        });
    }

    /** Moeda da Sorte: a moeda sobe girando, cai de volta e mostra a cara; se deu ruim, cai de lado e escurece. */
    private static void coin(ClientLevel level, int casterId, Vec3 at, boolean good) {
        int life = 34;
        int[] spin = {0, 1, 2, 1};
        spawn(level, at, DreamSprites.COIN, life, DreamParticle.BILLBOARD, (p, age) -> {
            Vec3 head = headOf(level, casterId, at, 0.5);
            double f = Math.min(1.0, age / 20.0);
            double h = Math.sin(f * Math.PI) * 1.6;
            boolean landed = age >= 20;
            p.at(head.x, head.y + h, head.z).frame(DreamSprites.COIN + (landed ? (good ? 0 : 2) : spin[age % 4])).size(0.26F);
            p.tint(landed && !good ? 0x777788 : 0xFFFFFF).alpha(Math.min(1.0F, (life - age) / 5.0F));
            if (age == 20) {
                if (good) {
                    pulse(level, head, 0xFFE66A, 0.2F, 1.2F, 0.7F, 7);
                    for (int i = 0; i < 10; i++) {
                        sparkle(level, head.add(rs(0.5), rs(0.4), rs(0.5)), 0xFFE66A);
                    }
                } else {
                    pulse(level, head, 0x333344, 0.2F, 0.9F, 0.7F, 7);
                }
            }
        });
    }

    /** Eco: três ondas saem do mago, cada uma mais fraca, e três bocas repetem o que foi dito, cada vez mais longe. */
    private static void echo(ClientLevel level, int casterId, Vec3 at) {
        Entity e = entity(level, casterId);
        Vec3 look = e != null ? e.getLookAngle() : new Vec3(0, 0, 1);
        Vec3 from = e != null ? e.getEyePosition().subtract(0, 0.15, 0) : at.add(0, 1.5, 0);
        Quaternionf facing = new Quaternionf().rotationY((float) Math.atan2(look.x, look.z));
        for (int i = 0; i < 3; i++) {
            final int k = i;
            Vec3 pos = from.add(look.scale(0.8 + i * 0.9));
            float fade = 1.0F - i * 0.3F;
            spawn(level, pos, DreamSprites.RIPPLE, i * 5 + 12, DreamParticle.FIXED, after(i * 5, (p, t) -> {
                p.orient(facing).tint(0x58D6C0).size(0.2F + t * 0.07F * (1.0F + k * 0.3F)).alpha(fade * (1.0F - t / 12.0F));
            }));
            spawn(level, pos, DreamSprites.MOUTH, i * 5 + 12, DreamParticle.BILLBOARD, after(i * 5, (p, t) -> {
                p.frame(DreamSprites.MOUTH + (t / 2) % 2).tint(0x8FF0DC).size(0.22F * fade).alpha(fade * Math.min(1.0F, (12 - t) / 4.0F));
            }));
        }
    }

    /** Toque do Vazio: um buraco redondo voa pelo ar, e as estrelas em volta caem nele. */
    private static void voidLaunch(ClientLevel level, int id, Vec3 start, Vec3 velocity, float range) {
        int life = (int) Math.ceil(range / Math.max(0.1, velocity.length())) + 1;
        track(id, spawn(level, start, DreamSprites.RIPPLE, life, DreamParticle.BILLBOARD,
                (p, age) -> p.at(start.add(velocity.scale(age))).tint(0xB070FF).size(0.3F + 0.04F * Mth.sin(age * 1.4F)).alpha(1.0F)));
        track(id, spawn(level, start, DreamSprites.GLOW, life, DreamParticle.BILLBOARD, (p, age) -> {
            Vec3 c = start.add(velocity.scale(age));
            p.at(towardCamera(c, 0.15)).tint(0x000000).size(0.3F).alpha(1.0F);
            for (int i = 0; i < 2; i++) {
                Vec3 from = c.add(rs(0.7), rs(0.7), rs(0.7));
                Vec3 to = c.add(velocity.scale(4));
                starTo(level, from, () -> to, 0, 4, 0.0);
            }
        }));
    }

    /** O Vazio acertou: o chão abre uma pálpebra debaixo do alvo, e as estrelas sobem por ele enquanto ele flutua. */
    private static void voidHit(ClientLevel level, int id, Vec3 at, int targetId) {
        land(id);
        pulse(level, at, 0x000000, 0.3F, 1.3F, 0.9F, 7);
        Entity target = entity(level, targetId);
        Vec3 feet = target != null ? target.position() : at;
        double gy = ground(level, feet.x, feet.y, feet.z);
        Vec3 floor = new Vec3(feet.x, (Double.isNaN(gy) ? feet.y : gy) + 0.05, feet.z);
        lid(level, floor, new Quaternionf().rotationX(-Mth.HALF_PI), 1.0F, 16);
        for (int i = 0; i < 16; i++) {
            Vec3 from = floor.add(rs(0.5), 0, rs(0.5));
            int delay = 3 + RND.nextInt(16);
            double speed = 0.06 + rf() * 0.06;
            spawn(level, from, DreamSprites.STAR, delay + 18, DreamParticle.BILLBOARD,
                    after(delay, (p, t) -> p.at(from.x, from.y + t * speed, from.z).size(0.07F).roll(t * 0.2F).alpha(1.0F - t / 18.0F)));
        }
    }
}

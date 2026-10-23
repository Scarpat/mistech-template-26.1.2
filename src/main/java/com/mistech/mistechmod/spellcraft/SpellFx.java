package com.mistech.mistechmod.spellcraft;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.item.RuneElement;
import com.mistech.mistechmod.spellcraft.fx.Vfx;
import com.mistech.mistechmod.spellcraft.fx.VfxContent;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Peças visuais das magias. Por baixo usam o kit de efeitos ({@link com.mistech.mistechmod.spellcraft.fx.Vfx}):
 * círculos mágicos 3D que giram, ondas de choque deitadas, esferas de luz que estouram, raios de luz de verdade,
 * partículas próprias (runas, faíscas, brasas, estilhaços) na cor do elemento. O agendador ({@link #later}) dá
 * movimento a tudo.
 */
@EventBusSubscriber(modid = Mistech.MODID)
public final class SpellFx {
    private static final class Task {
        final ServerLevel level;
        final Runnable run;
        int left;

        Task(ServerLevel level, int left, Runnable run) {
            this.level = level;
            this.left = left;
            this.run = run;
        }
    }

    private static final List<Task> TASKS = new ArrayList<>();
    private static final List<Task> PENDING = new ArrayList<>();

    private SpellFx() {}

    public static void later(ServerLevel level, int delay, Runnable run) {
        if (delay <= 0) {
            run.run();
        } else {
            PENDING.add(new Task(level, delay, run));
        }
    }

    @SubscribeEvent
    static void tick(ServerTickEvent.Post event) {
        TASKS.addAll(PENDING);
        PENDING.clear();
        Iterator<Task> it = TASKS.iterator();
        List<Runnable> due = new ArrayList<>();
        while (it.hasNext()) {
            Task task = it.next();
            if (--task.left <= 0) {
                it.remove();
                due.add(task.run);
            }
        }
        for (Runnable run : due) {
            try {
                run.run();
            } catch (RuntimeException ignored) {
                // um efeito visual nunca pode derrubar o servidor
            }
        }
    }

    @SubscribeEvent
    static void stopped(ServerStoppedEvent event) {
        TASKS.clear();
        PENDING.clear();
    }

    // ------------------------------------------------------------------ básicos

    /** Clareia cores muito escuras (Trevas) para aparecerem no escuro. */
    public static int lift(int rgb) {
        int r = rgb >> 16 & 255;
        int g = rgb >> 8 & 255;
        int b = rgb & 255;
        if (r * 3 + g * 6 + b < 1100) {
            r += (255 - r) * 35 / 100;
            g += (255 - g) * 35 / 100;
            b += (255 - b) * 35 / 100;
        }
        return r << 16 | g << 8 | b;
    }

    /** Ponto de luz na cor do elemento (antigo pó colorido). */
    public static ParticleOptions dust(RuneElement element, float size) {
        return VfxContent.mote(lift(element.color()));
    }

    public static ParticleOptions glyph(RuneElement element) {
        return VfxContent.glyph(lift(element.color()));
    }

    public static ParticleOptions spark(RuneElement element) {
        return VfxContent.spark(lift(element.color()));
    }

    public static ParticleOptions ember(RuneElement element) {
        return VfxContent.ember(lift(element.color()));
    }

    public static ParticleOptions shard(RuneElement element) {
        return VfxContent.shard(lift(element.color()));
    }

    private static void at(ServerLevel level, ParticleOptions particle, Vec3 pos) {
        level.sendParticles(particle, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
    }

    /** Partícula com direção: contagem 0 faz o cliente usar o vetor como velocidade. */
    public static void fly(ServerLevel level, ParticleOptions particle, Vec3 pos, Vec3 dir, double speed) {
        level.sendParticles(particle, pos.x, pos.y, pos.z, 0, dir.x, dir.y, dir.z, speed);
    }

    private static Vec3[] basis(Vec3 dir) {
        Vec3 d = dir.normalize();
        Vec3 helper = Math.abs(d.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 u = d.cross(helper).normalize();
        Vec3 v = d.cross(u).normalize();
        return new Vec3[] {u, v};
    }

    public static Vec3 randomDir(ServerLevel level) {
        Vec3 v = new Vec3(level.getRandom().nextGaussian(), level.getRandom().nextGaussian(), level.getRandom().nextGaussian());
        return v.lengthSqr() < 1.0E-4 ? new Vec3(0, 1, 0) : v.normalize();
    }

    public static void line(ServerLevel level, Vec3 from, Vec3 to, ParticleOptions particle, double step) {
        Vec3 delta = to.subtract(from);
        double length = delta.length();
        if (length < 0.05) {
            return;
        }
        Vec3 dir = delta.scale(1.0 / length);
        for (double d = 0; d <= length; d += step) {
            at(level, particle, from.add(dir.scale(d)));
        }
    }

    public static void ring(ServerLevel level, Vec3 center, double radius, ParticleOptions particle, int points) {
        for (int i = 0; i < points; i++) {
            double a = Math.PI * 2 * i / points;
            at(level, particle, center.add(Math.cos(a) * radius, 0, Math.sin(a) * radius));
        }
    }

    /** Runas subindo num círculo. */
    public static void glyphs(ServerLevel level, Vec3 center, RuneElement element, double radius, int count) {
        for (int i = 0; i < count; i++) {
            double a = level.getRandom().nextDouble() * Math.PI * 2;
            double r = radius * (0.6 + level.getRandom().nextDouble() * 0.4);
            fly(level, glyph(element), center.add(Math.cos(a) * r, 0.1 + level.getRandom().nextDouble() * 0.6, Math.sin(a) * r),
                    new Vec3(0, 1, 0), 0.03 + level.getRandom().nextDouble() * 0.04);
        }
    }

    /** Faíscas estourando em todas as direções. */
    public static void sparks(ServerLevel level, Vec3 center, RuneElement element, int count, double speed) {
        for (int i = 0; i < count; i++) {
            fly(level, spark(element), center, randomDir(level), speed * (0.4 + level.getRandom().nextDouble() * 0.8));
        }
    }

    // ------------------------------------------------------------------ peças maiores

    /** Círculo mágico 3D no chão que aparece girando e some. */
    public static void sigil(ServerLevel level, Vec3 center, RuneElement element, double radius) {
        Vfx.circle(level, center, element, radius, 40);
        glyphs(level, center, element, radius * 0.9, 6 + (int) radius * 2);
    }

    /** Anel que sobe do chão até a altura dada. */
    public static void risingRing(ServerLevel level, Vec3 base, RuneElement element, double radius, int ticks, double height) {
        Vfx.risingRing(level, base, element, radius, height, Math.max(4, ticks));
        glyphs(level, base, element, radius, 4);
    }

    /** Onda de choque que abre do centro até o raio máximo. */
    public static void expandingRing(ServerLevel level, Vec3 center, RuneElement element, double maxRadius, int ticks, @Nullable ParticleOptions extra) {
        Vfx.shockwave(level, center, element, maxRadius, Math.max(4, ticks));
        if (extra != null) {
            for (int t = 2; t <= ticks; t += 2) {
                double r = maxRadius * t / ticks;
                int points = Math.max(6, (int) (r * 3));
                later(level, t, () -> {
                    Vec3 c = center.add(0, 0.15, 0);
                    for (int i = 0; i < points; i++) {
                        double a = level.getRandom().nextDouble() * Math.PI * 2;
                        fly(level, extra, c.add(Math.cos(a) * r, 0, Math.sin(a) * r), new Vec3(Math.cos(a), 0.5, Math.sin(a)), 0.08);
                    }
                });
            }
        }
    }

    /** O contrário: um anel que se fecha para o centro (sombras, sucção). */
    public static void contractingRing(ServerLevel level, Vec3 center, RuneElement element, double startRadius, int ticks, @Nullable ParticleOptions extra) {
        Vfx.implode(level, center, element, startRadius, Math.max(4, ticks));
        for (int i = 0; i < Math.max(8, (int) (startRadius * 4)); i++) {
            double a = level.getRandom().nextDouble() * Math.PI * 2;
            Vec3 from = center.add(Math.cos(a) * startRadius, 0.3 + level.getRandom().nextDouble(), Math.sin(a) * startRadius);
            fly(level, extra != null ? extra : dust(element, 1.0F), from, center.add(0, 0.8, 0).subtract(from), 0.12);
        }
    }

    /** Explosão: esfera de luz que estoura, faíscas para todo lado e o efeito extra. */
    public static void burst(ServerLevel level, Vec3 center, RuneElement element, double speed, int count, @Nullable ParticleOptions extra) {
        Vfx.flare(level, center, element, (float) Math.min(3.0, 0.6 + count / 25.0), 8);
        sparks(level, center, element, Math.max(6, count / 2), speed * 1.6);
        if (extra != null) {
            for (int i = 0; i < count / 2; i++) {
                fly(level, extra, center, randomDir(level), speed * (0.3 + level.getRandom().nextDouble() * 0.7));
            }
        }
    }

    /** Projétil decorativo: uma esfera de luz viaja do ponto A ao B com rastro; chama {@code onArrive} ao chegar. */
    public static void projectile(ServerLevel level, Vec3 from, Vec3 to, RuneElement element, @Nullable ParticleOptions trail, Runnable onArrive) {
        Vec3 delta = to.subtract(from);
        double dist = delta.length();
        int n = Math.max(2, Math.min(12, (int) (dist / 2.0)));
        Display orb = Vfx.item(level, from, Vfx.tinted(VfxContent.VFX_ORB.get(), element.color()), Vfx.centeredItem(0.55F), n + 2, true);
        for (int i = 1; i <= n; i++) {
            int step = i;
            later(level, i, () -> {
                Vec3 prev = from.add(delta.scale((step - 1.0) / n));
                Vec3 head = from.add(delta.scale((double) step / n));
                Vfx.move(orb, head);
                for (int k = 0; k < 4; k++) {
                    Vec3 pos = prev.add(head.subtract(prev).scale(k / 4.0));
                    at(level, dust(element, 1.0F), pos);
                    if (trail != null && k % 2 == 0) {
                        at(level, trail, pos);
                    }
                }
                if (step == n) {
                    onArrive.run();
                }
            });
        }
    }

    /** Raio em zigue-zague: segmentos de luz de verdade (somem em meio segundo) e faíscas. */
    public static void lightning(ServerLevel level, Vec3 from, Vec3 to, RuneElement element) {
        Vec3 delta = to.subtract(from);
        double length = delta.length();
        if (length < 0.1) {
            return;
        }
        int segments = Math.max(2, Math.min(10, (int) (length / 1.5)));
        Vec3 prev = from;
        for (int i = 1; i <= segments; i++) {
            Vec3 next = from.add(delta.scale((double) i / segments));
            if (i < segments) {
                next = next.add(randomDir(level).scale(0.45));
            }
            Vfx.beam(level, prev, next, Vfx.glowMaterial(element), 0.14F, 7);
            line(level, prev, next, spark(element), 0.6);
            prev = next;
        }
        sparks(level, to, element, 10, 0.4);
    }

    /** Hélice de runas subindo ao redor de uma criatura (acompanha o movimento dela). */
    public static void helix(ServerLevel level, Entity who, RuneElement element, int ticks, double height, @Nullable ParticleOptions extra) {
        Vfx.risingRing(level, who.position(), element, 0.9, height, Math.max(6, ticks));
        for (int t = 0; t <= ticks; t += 2) {
            int step = t;
            later(level, t, () -> {
                if (!who.isAlive()) {
                    return;
                }
                double y = height * step / ticks;
                for (int k = 0; k < 2; k++) {
                    double a = step * 0.75 + k * Math.PI;
                    Vec3 pos = who.position().add(Math.cos(a) * 0.8, y, Math.sin(a) * 0.8);
                    at(level, k == 0 ? glyph(element) : dust(element, 1.0F), pos);
                    if (extra != null) {
                        at(level, extra, pos);
                    }
                }
            });
        }
    }

    /** Coluna de luz que sobe de um ponto. */
    public static void column(ServerLevel level, Vec3 base, RuneElement element, double height, int ticks, @Nullable ParticleOptions extra) {
        Vfx.beam(level, base, base.add(0, height, 0), Vfx.glowMaterial(element), 0.45F, Math.max(8, ticks + 6));
        for (int t = 0; t <= ticks; t += 2) {
            double y = height * t / ticks;
            later(level, t, () -> {
                fly(level, spark(element), base.add(0, y, 0), randomDir(level), 0.2);
                if (extra != null) {
                    fly(level, extra, base.add(0, y, 0), new Vec3(0, 1, 0), 0.15);
                }
            });
        }
    }

    /** Sopro de vento em cone: anéis de vento 3D voando para a frente e nuvens. */
    public static void gust(ServerLevel level, Vec3 origin, Vec3 look, RuneElement element) {
        for (int i = 0; i < 50; i++) {
            double d = 1 + level.getRandom().nextDouble() * 7;
            Vec3 pos = origin.add(look.scale(d)).add(randomDir(level).scale(d * 0.28));
            fly(level, net.minecraft.core.particles.ParticleTypes.CLOUD, pos, look, 0.5);
        }
        for (int k = 0; k < 3; k++) {
            int delay = k * 2;
            later(level, delay, () -> Vfx.windRing(level, origin.add(look.scale(1.2)), look, element, 8.0, 8));
        }
    }

    /** Toda magia, ao ser lançada: pequeno círculo mágico 3D sob o mago e runas subindo. */
    public static void cast(ServerLevel level, Entity caster, RuneElement element) {
        Vfx.circle(level, caster.position(), element, 1.1, 16);
        glyphs(level, caster.position(), element, 0.9, 6);
    }

    /** Energia sendo puxada para o mago durante a carga: faíscas convergindo, mais intensas com a carga. */
    public static void gather(ServerLevel level, Entity caster, RuneElement element, double factor) {
        Vec3 center = caster.position().add(0, caster.getBbHeight() * 0.6, 0);
        int n = 2 + (int) (factor * 6);
        for (int i = 0; i < n; i++) {
            Vec3 dir = randomDir(level);
            Vec3 from = center.add(dir.scale(1.8 + factor));
            fly(level, spark(element), from, dir.scale(-1), 0.35);
            if (i % 2 == 0) {
                fly(level, glyph(element), from, dir.scale(-1), 0.12);
            }
        }
    }

    /** Ritual: grande círculo mágico girando; colunas de luz sobem conforme o tempo passa. */
    public static void ritualRise(ServerLevel level, Entity caster, RuneElement element, int elapsed, int total) {
        double progress = Math.min(1.0, elapsed / (double) total);
        Vec3 base = caster.position();
        if (elapsed == 1) {
            Vfx.circle(level, base, element, 2.6, total + 12);
            if (element == RuneElement.TIME) {
                Vfx.clock(level, base, element, 1.6, total + 12, 2.6F);
            }
        }
        if (elapsed % 10 == 5) {
            for (int i = 0; i < 3; i++) {
                double a = Math.PI * 2 * (elapsed * 0.05 + i / 3.0);
                Vec3 pos = base.add(Math.cos(a) * 2.2, 0, Math.sin(a) * 2.2);
                Vfx.beam(level, pos, pos.add(0, 0.6 + 2.4 * progress, 0), Vfx.glowMaterial(element), 0.18F, 12);
            }
        }
        if (elapsed % 2 == 0) {
            glyphs(level, base, element, 2.2, 2);
            gather(level, caster, element, progress * 0.6);
        }
        if (elapsed == total - 2) {
            Vfx.shake(level, base, 10, 0.25F, 8);
        }
    }
}

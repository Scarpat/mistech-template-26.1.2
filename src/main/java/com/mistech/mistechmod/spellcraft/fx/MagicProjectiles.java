package com.mistech.mistechmod.spellcraft.fx;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.item.RuneElement;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Projéteis das magias de verdade: viajam alguns blocos por tick, param no primeiro bloco ou criatura do caminho e só
 * então aplicam o efeito. A aparência é uma esfera de luz (ou um bloco girando, como a pedra do Pedregulho) com rastro.
 */
@EventBusSubscriber(modid = Mistech.MODID)
public final class MagicProjectiles {
    /** Como o projétil se parece e o que faz ao acertar. */
    public static final class Shot {
        final ServerLevel level;
        final ServerPlayer owner;
        final RuneElement element;
        Vec3 pos;
        final Vec3 velocity;
        double left;
        final double hitRadius;
        final @Nullable ParticleOptions trail;
        final Consumer<LivingEntity> onHit;
        final Consumer<Vec3> onImpact;
        @Nullable Display body;
        float spin;
        float bodySize;
        boolean blockBody;
        int age;

        Shot(ServerLevel level, ServerPlayer owner, RuneElement element, Vec3 pos, Vec3 velocity, double range, double hitRadius,
             @Nullable ParticleOptions trail, Consumer<LivingEntity> onHit, Consumer<Vec3> onImpact) {
            this.level = level;
            this.owner = owner;
            this.element = element;
            this.pos = pos;
            this.velocity = velocity;
            this.left = range;
            this.hitRadius = hitRadius;
            this.trail = trail;
            this.onHit = onHit;
            this.onImpact = onImpact;
        }
    }

    private static final List<Shot> SHOTS = new ArrayList<>();
    private static final List<Shot> NEW = new ArrayList<>();

    private MagicProjectiles() {}

    /** Esfera de luz do elemento. {@code speed} em blocos por tick. */
    public static Shot orb(ServerLevel level, ServerPlayer owner, RuneElement element, Vec3 start, Vec3 dir, double speed, double range,
                           float size, @Nullable ParticleOptions trail, Consumer<LivingEntity> onHit, Consumer<Vec3> onImpact) {
        Shot shot = new Shot(level, owner, element, start, dir.normalize().scale(speed), range, 0.35 + size * 0.3, trail, onHit, onImpact);
        shot.body = Vfx.item(level, start, Vfx.tinted(VfxContent.VFX_ORB.get(), element.color()), Vfx.centeredItem(size), 0, true);
        shot.bodySize = size;
        NEW.add(shot);
        return shot;
    }

    /** Bloco girando (pedra, gelo, magma). */
    public static Shot block(ServerLevel level, ServerPlayer owner, RuneElement element, Vec3 start, Vec3 dir, double speed, double range,
                             BlockState state, float size, @Nullable ParticleOptions trail, Consumer<LivingEntity> onHit, Consumer<Vec3> onImpact) {
        Shot shot = new Shot(level, owner, element, start, dir.normalize().scale(speed), range, 0.3 + size * 0.4, trail, onHit, onImpact);
        shot.body = Vfx.block(level, start, state, Vfx.centered(size, size, size, new Quaternionf()), 0, false);
        shot.bodySize = size;
        shot.blockBody = true;
        NEW.add(shot);
        return shot;
    }

    private static boolean friendly(ServerPlayer owner, LivingEntity e) {
        return e == owner || e.isAlliedTo(owner) || e instanceof TamableAnimal pet && pet.isOwnedBy(owner)
                || e.entityTags().contains(com.mistech.mistechmod.spellcraft.Summons.TAG);
    }

    @SubscribeEvent
    static void tick(ServerTickEvent.Post event) {
        SHOTS.addAll(NEW);
        NEW.clear();
        Iterator<Shot> it = SHOTS.iterator();
        while (it.hasNext()) {
            Shot s = it.next();
            if (step(s)) {
                it.remove();
                if (s.body != null) {
                    Vfx.remove(s.level, s.body, 1);
                }
            }
        }
    }

    /** Avança um tick; true quando acabou. */
    private static boolean step(Shot s) {
        s.age++;
        Vec3 from = s.pos;
        Vec3 to = from.add(s.velocity);
        // bloco no caminho
        BlockHitResult block = s.level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, s.owner));
        Vec3 end = block.getType() == HitResult.Type.MISS ? to : block.getLocation();
        // criatura no caminho (a mais próxima)
        LivingEntity hit = null;
        double best = Double.MAX_VALUE;
        AABB sweep = new AABB(from, end).inflate(s.hitRadius + 0.5);
        for (LivingEntity e : s.level.getEntitiesOfClass(LivingEntity.class, sweep, e -> e.isAlive() && !e.isSpectator() && !friendly(s.owner, e))) {
            var clip = e.getBoundingBox().inflate(s.hitRadius).clip(from, end);
            if (clip.isPresent()) {
                double d = clip.get().distanceToSqr(from);
                if (d < best) {
                    best = d;
                    hit = e;
                }
            }
        }
        // rastro ao longo do trecho
        int n = Math.max(2, (int) (from.distanceTo(end) / 0.35));
        for (int i = 0; i < n; i++) {
            Vec3 p = from.add(end.subtract(from).scale(i / (double) n));
            s.level.sendParticles(VfxContent.mote(s.element.color()), p.x, p.y, p.z, 1, 0.04, 0.04, 0.04, 0.0);
            if (s.trail != null && i % 2 == 0) {
                s.level.sendParticles(s.trail, p.x, p.y, p.z, 1, 0.06, 0.06, 0.06, 0.005);
            }
        }
        if (hit != null) {
            Vec3 at = hit.position().add(0, hit.getBbHeight() * 0.5, 0);
            s.onHit.accept(hit);
            s.onImpact.accept(at);
            return true;
        }
        if (block.getType() != HitResult.Type.MISS) {
            s.onImpact.accept(end);
            return true;
        }
        s.left -= s.velocity.length();
        s.pos = to;
        if (s.body != null) {
            Vfx.move(s.body, to);
            if (s.blockBody && s.age % 3 == 0) {
                s.spin += 90;
                Vfx.animate(s.level, s.body, 0, 3, Vfx.centered(s.bodySize, s.bodySize, s.bodySize,
                        new Quaternionf().rotateX((float) Math.toRadians(s.spin)).rotateZ((float) Math.toRadians(s.spin * 0.5))));
            }
        }
        if (s.left <= 0) {
            s.onImpact.accept(to);
            return true;
        }
        return false;
    }

    @SubscribeEvent
    static void stopped(ServerStoppedEvent event) {
        SHOTS.clear();
        NEW.clear();
    }
}

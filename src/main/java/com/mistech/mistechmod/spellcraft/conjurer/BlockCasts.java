package com.mistech.mistechmod.spellcraft.conjurer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.item.RuneElement;
import com.mistech.mistechmod.spellcraft.SpellFx;
import com.mistech.mistechmod.spellcraft.SpellcraftContent;
import com.mistech.mistechmod.spellcraft.Summons;
import com.mistech.mistechmod.spellcraft.fx.MagicProjectiles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * As magias que o Conjurador Rúnico sabe lançar e o que cada uma faz quando sai de um bloco (sem mira, sem corpo).
 * Cada entrada reaproveita os números da magia original ({@code Spells}) multiplicados pelo poder do cristal.
 * <p>
 * Ninguém do lado do dono é atingido: jogadores, animais com dono, golens de trabalho e invocações ficam de fora.
 * O dano sai em nome de um jogador falso com o nome do dono, para contar loot e XP.
 */
public final class BlockCasts {
    private BlockCasts() {}

    /** Grupos de uso (vão para a dica do cristal e para o Grimório). */
    public enum Mode {
        TURRET("turret"), AREA("area"), FARM("farm"), SUPPORT("support"), LOGISTICS("logistics"), UTILITY("utility");

        private final String id;

        Mode(String id) {
            this.id = id;
        }

        public String id() {
            return this.id;
        }
    }

    /** Tudo o que um lançamento precisa saber sobre o Conjurador. */
    public record Ctx(ServerLevel level, BlockPos pos, Direction facing, double power, FakePlayer caster, UUID owner, int rangeBonus,
                      RunicConjurerBlockEntity be) {
        /** Onde o cristal flutua (de onde as magias saem). */
        public Vec3 origin() {
            return new Vec3(this.pos.getX() + 0.5, this.pos.getY() + 1.15, this.pos.getZ() + 0.5);
        }

        public double range(double base) {
            return base + this.rangeBonus * 2.0;
        }

        public float dmg(double base) {
            return (float) (base * this.power);
        }

        public int dur(int ticks) {
            return (int) Math.round(ticks * this.power);
        }
    }

    @FunctionalInterface
    public interface Cast {
        /** Devolve false quando não havia o que fazer (sem alvo etc.): aí não gasta energia nem começa a recarga. */
        boolean cast(Ctx c);
    }

    public record Entry(Mode mode, Cast cast) {}

    private static final Map<String, Entry> ALL = new LinkedHashMap<>();

    private static void add(String spell, Mode mode, Cast cast) {
        ALL.put(spell, new Entry(mode, cast));
    }

    public static @Nullable Entry get(@Nullable String spell) {
        return spell == null ? null : ALL.get(spell);
    }

    public static @Nullable Mode mode(@Nullable String spell) {
        Entry e = get(spell);
        return e == null ? null : e.mode();
    }

    public static boolean compatible(@Nullable String spell) {
        return get(spell) != null;
    }

    public static java.util.Collection<String> ids() {
        return ALL.keySet();
    }

    // ------------------------------------------------------------------ alvos

    /** Criatura hostil que o Conjurador pode atingir. */
    public static boolean hostile(Ctx c, LivingEntity e) {
        if (!e.isAlive() || e.isSpectator() || e instanceof Player || !(e instanceof Enemy)) {
            return false;
        }
        if (e.entityTags().contains(Summons.TAG) || e instanceof com.mistech.mistechmod.golem.WorkGolemEntity) {
            return false;
        }
        return !(e instanceof OwnableEntity own && own.getOwnerReference() != null);
    }

    private static boolean sees(Ctx c, Vec3 from, LivingEntity target) {
        Vec3 to = target.position().add(0, target.getBbHeight() * 0.6, 0);
        return c.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, c.caster()))
                .getType() == HitResult.Type.MISS;
    }

    private static List<LivingEntity> hostilesAround(Ctx c, Vec3 center, double radius) {
        return c.level().getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius),
                e -> hostile(c, e) && e.position().distanceTo(center) <= radius + 0.5);
    }

    private static @Nullable LivingEntity nearestHostile(Ctx c, double radius) {
        Vec3 o = c.origin();
        LivingEntity best = null;
        double bestD = Double.MAX_VALUE;
        for (LivingEntity e : hostilesAround(c, o, radius)) {
            double d = e.distanceToSqr(o);
            if (d < bestD && sees(c, o, e)) {
                bestD = d;
                best = e;
            }
        }
        return best;
    }

    private static void hurt(Ctx c, LivingEntity target, float amount) {
        target.hurtServer(c.level(), c.level().damageSources().indirectMagic(c.caster(), c.caster()), amount);
    }

    private static List<Player> playersAround(Ctx c, double radius) {
        Vec3 o = c.origin();
        return c.level().getEntitiesOfClass(Player.class, new AABB(o, o).inflate(radius),
                p -> p.isAlive() && !p.isSpectator() && p.position().distanceTo(o) <= radius + 0.5);
    }

    // ------------------------------------------------------------------ projéteis (torreta)

    private static Cast turret(double range, float damage, RuneElement element, ParticleOptions particle, SoundEvent sound,
                               @Nullable BlockState body, float size, Consumer<LivingEntity> extra) {
        return c -> {
            LivingEntity target = nearestHostile(c, c.range(range));
            if (target == null) {
                return false;
            }
            Vec3 o = c.origin();
            Vec3 to = target.position().add(0, target.getBbHeight() * 0.5, 0);
            Vec3 dir = to.subtract(o);
            Vec3 start = o.add(dir.normalize().scale(0.5));
            float dmg = c.dmg(damage);
            Consumer<LivingEntity> hit = t -> {
                if (hostile(c, t)) {
                    hurt(c, t, dmg);
                    extra.accept(t);
                }
            };
            Consumer<Vec3> impact = at -> {
                SpellFx.burst(c.level(), at, element, 0.25, 22, particle);
                c.level().playSound(null, BlockPos.containing(at), sound, SoundSource.BLOCKS, 0.7F, 1.3F);
            };
            if (body != null) {
                MagicProjectiles.block(c.level(), c.caster(), element, start, dir, 1.4, c.range(range) + 2, body, size, particle, hit, impact);
            } else {
                MagicProjectiles.orb(c.level(), c.caster(), element, start, dir, 1.6, c.range(range) + 2, size, particle, hit, impact);
            }
            c.level().playSound(null, c.pos(), sound, SoundSource.BLOCKS, 0.6F, 1.6F);
            return true;
        };
    }

    /** Torreta com efeito onírico (ver DreamFx): mesma regra, mas o voo e o impacto são desenhados no cliente. */
    private static Cast dreamTurret(double range, float damage, RuneElement element, SoundEvent sound, int launchFx, int hitFx, Consumer<LivingEntity> extra) {
        return c -> {
            LivingEntity target = nearestHostile(c, c.range(range));
            if (target == null) {
                return false;
            }
            Vec3 o = c.origin();
            Vec3 dir = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(o);
            Vec3 start = o.add(dir.normalize().scale(0.5));
            float dmg = c.dmg(damage);
            int id = com.mistech.mistechmod.spellcraft.fx.DreamFx.newId();
            LivingEntity[] struck = new LivingEntity[1];
            MagicProjectiles.bare(c.level(), c.caster(), element, start, dir, 1.6, c.range(range) + 2, 0.5, t -> {
                if (hostile(c, t)) {
                    hurt(c, t, dmg);
                    extra.accept(t);
                    struck[0] = t;
                }
            }, at -> {
                com.mistech.mistechmod.spellcraft.fx.DreamFx.send(c.level(), hitFx, id, struck[0], 0.0F, at, Vec3.ZERO);
                c.level().playSound(null, BlockPos.containing(at), sound, SoundSource.BLOCKS, 0.7F, 1.3F);
            });
            com.mistech.mistechmod.spellcraft.fx.DreamFx.send(c.level(), launchFx, id, null, (float) (c.range(range) + 2), start, dir.normalize().scale(1.6));
            c.level().playSound(null, c.pos(), sound, SoundSource.BLOCKS, 0.6F, 1.6F);
            return true;
        };
    }

    /** Empurra para longe do Conjurador. */
    private static void knock(Ctx c, LivingEntity t, double strength, double up) {
        Vec3 d = t.position().subtract(c.origin()).multiply(1, 0, 1);
        if (d.lengthSqr() > 1.0E-4) {
            Vec3 n = d.normalize().scale(strength);
            t.push(n.x, up, n.z);
            t.hurtMarked = true;
        }
    }

    /** Raio que salta de criatura em criatura (Descarga e Arco Elétrico). */
    private static Cast chain(double range, float first, float next, int jumps, double jumpRadius) {
        return c -> {
            LivingEntity current = nearestHostile(c, c.range(range));
            if (current == null) {
                return false;
            }
            Vec3 from = c.origin();
            Set<LivingEntity> hit = new HashSet<>();
            for (int i = 0; i <= jumps && current != null; i++) {
                hit.add(current);
                Vec3 at = current.position().add(0, current.getBbHeight() * 0.5, 0);
                hurt(c, current, c.dmg(i == 0 ? first : next));
                com.mistech.mistechmod.spellcraft.fx.DreamFx.send(c.level(), com.mistech.mistechmod.spellcraft.fx.DreamFx.STITCH, 0, null, 1.0F, from, at);
                from = at;
                LivingEntity best = null;
                double bestD = jumpRadius * jumpRadius;
                for (LivingEntity e : hostilesAround(c, current.position(), jumpRadius)) {
                    double d = e.distanceToSqr(current);
                    if (!hit.contains(e) && d < bestD) {
                        bestD = d;
                        best = e;
                    }
                }
                current = best;
            }
            c.level().playSound(null, c.pos(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.BLOCKS, 0.5F, 1.9F);
            return true;
        };
    }

    // ------------------------------------------------------------------ área (em volta do bloco)

    private static Cast area(double radius, RuneElement element, @Nullable ParticleOptions ring, SoundEvent sound, Consumer<Ctx> fx,
                             java.util.function.BiConsumer<Ctx, LivingEntity> action) {
        return c -> {
            double r = c.range(radius);
            List<LivingEntity> targets = hostilesAround(c, c.origin(), r);
            if (targets.isEmpty()) {
                return false;
            }
            for (LivingEntity t : targets) {
                action.accept(c, t);
            }
            Vec3 ground = Vec3.atBottomCenterOf(c.pos());
            if (ring != null) {
                SpellFx.expandingRing(c.level(), ground, element, r, 8, ring);
            }
            fx.accept(c);
            c.level().playSound(null, c.pos(), sound, SoundSource.BLOCKS, 1.0F, 0.8F);
            return true;
        };
    }

    // ------------------------------------------------------------------ fazenda

    private static Cast farm(int radius, int tries) {
        return c -> {
            int r = radius + c.rangeBonus();
            BlockPos center = c.pos().relative(c.facing(), r + 1);
            ItemStack meal = new ItemStack(Items.BONE_MEAL);
            int grew = 0;
            int attempts = (int) Math.round(tries * Math.max(0.5, c.power()));
            for (int i = 0; i < attempts; i++) {
                BlockPos p = center.offset(c.level().getRandom().nextInt(r * 2 + 1) - r, c.level().getRandom().nextInt(3) - 1,
                        c.level().getRandom().nextInt(r * 2 + 1) - r);
                if (net.minecraft.world.item.BoneMealItem.growCrop(meal, c.level(), p)) {
                    grew++;
                }
            }
            if (grew == 0) {
                return false;
            }
            com.mistech.mistechmod.spellcraft.fx.DreamFx.send(c.level(), com.mistech.mistechmod.spellcraft.fx.DreamFx.BLOOM, null, 0.0F, Vec3.atBottomCenterOf(center));
            c.level().playSound(null, center, SoundEvents.BONE_MEAL_USE, SoundSource.BLOCKS, 1.0F, 1.1F);
            return true;
        };
    }

    // ------------------------------------------------------------------ registro

    static {
        // ----- torreta: mira a criatura hostil mais próxima que o cristal enxerga -----
        add("fire_spark", Mode.TURRET, dreamTurret(12, 5.0F, RuneElement.FIRE, SoundEvents.FIRECHARGE_USE, com.mistech.mistechmod.spellcraft.fx.DreamFx.MOTH_LAUNCH, com.mistech.mistechmod.spellcraft.fx.DreamFx.MOTH_HIT,
                t -> t.igniteForSeconds(4.0F)));
        add("ice_shard", Mode.TURRET, dreamTurret(12, 5.0F, RuneElement.ICE, SoundEvents.GLASS_BREAK, com.mistech.mistechmod.spellcraft.fx.DreamFx.SHARD_LAUNCH, com.mistech.mistechmod.spellcraft.fx.DreamFx.SHARD_HIT, t -> {
                    t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1));
                    t.setTicksFrozen(Math.max(t.getTicksFrozen(), t.getTicksRequiredToFreeze() + 40));
                }));
        add("boulder", Mode.TURRET, c -> dreamTurret(12, 4.0F, RuneElement.EARTH, SoundEvents.STONE_BREAK, com.mistech.mistechmod.spellcraft.fx.DreamFx.HEAD_LAUNCH, com.mistech.mistechmod.spellcraft.fx.DreamFx.HEAD_HIT,
                t -> knock(c, t, 0.6, 0.25)).cast(c));
        add("water_bolt", Mode.TURRET, c -> dreamTurret(12, 4.0F, RuneElement.WATER, SoundEvents.PLAYER_SPLASH, com.mistech.mistechmod.spellcraft.fx.DreamFx.FISH_LAUNCH, com.mistech.mistechmod.spellcraft.fx.DreamFx.FISH_HIT,
                t -> knock(c, t, 0.9, 0.3)).cast(c));
        add("dark_drain", Mode.TURRET, dreamTurret(12, 6.0F, RuneElement.DARKNESS, SoundEvents.WITHER_HURT, com.mistech.mistechmod.spellcraft.fx.DreamFx.MOUTH_LAUNCH, com.mistech.mistechmod.spellcraft.fx.DreamFx.MOUTH_HIT,
                t -> t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0))));
        add("void_touch", Mode.TURRET, dreamTurret(12, 9.0F, RuneElement.VOID, SoundEvents.ENDERMAN_HURT, com.mistech.mistechmod.spellcraft.fx.DreamFx.VOID_LAUNCH, com.mistech.mistechmod.spellcraft.fx.DreamFx.VOID_HIT, t -> {
            t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1));
            t.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 20, 0));
        }));
        add("energy_discharge", Mode.TURRET, chain(12, 6.0F, 4.0F, 3, 5.0));
        add("spark_arc", Mode.TURRET, chain(12, 3.5F, 3.5F, 2, 6.0));

        // ----- defesa em área: centro no bloco -----
        add("ember_ring", Mode.AREA, area(4.0, RuneElement.FIRE, null, SoundEvents.FIRECHARGE_USE,
                c -> com.mistech.mistechmod.spellcraft.fx.DreamFx.send(c.level(), com.mistech.mistechmod.spellcraft.fx.DreamFx.CIRANDA, null, (float) c.range(4.0), Vec3.atBottomCenterOf(c.pos())), (c, t) -> {
                    hurt(c, t, c.dmg(3.0));
                    t.igniteForSeconds(5.0F);
                }));
        add("frost_nova", Mode.AREA, area(5.0, RuneElement.ICE, null, SoundEvents.GLASS_BREAK,
                c -> com.mistech.mistechmod.spellcraft.fx.DreamFx.send(c.level(), com.mistech.mistechmod.spellcraft.fx.DreamFx.FROST_NOVA, null, (float) c.range(5.0), Vec3.atBottomCenterOf(c.pos())), (c, t) -> {
                    hurt(c, t, c.dmg(4.0));
                    t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, c.dur(80), 2));
                    t.setTicksFrozen(Math.max(t.getTicksFrozen(), t.getTicksRequiredToFreeze() + 60));
                }));
        add("earthquake", Mode.AREA, area(6.0, RuneElement.EARTH, null, SoundEvents.GENERIC_EXPLODE.value(),
                c -> com.mistech.mistechmod.spellcraft.fx.DreamFx.send(c.level(), com.mistech.mistechmod.spellcraft.fx.DreamFx.QUAKE, null, (float) c.range(6.0), Vec3.atBottomCenterOf(c.pos())), (c, t) -> {
                    hurt(c, t, c.dmg(3.0));
                    t.push(0, 0.5, 0);
                    t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 30, 1));
                    t.hurtMarked = true;
                }));
        add("blizzard", Mode.AREA, area(7.0, RuneElement.ICE, null, SoundEvents.POWDER_SNOW_STEP,
                c -> com.mistech.mistechmod.spellcraft.fx.DreamFx.send(c.level(), com.mistech.mistechmod.spellcraft.fx.DreamFx.BLIZZARD, null, (float) c.range(7.0), Vec3.atBottomCenterOf(c.pos())),
                (c, t) -> {
                    hurt(c, t, c.dmg(1.5));
                    t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 30, 2));
                    t.setTicksFrozen(Math.max(t.getTicksFrozen(), t.getTicksRequiredToFreeze() + 40));
                }));
        add("soul_harvest", Mode.AREA, area(8.0, RuneElement.SOUL, null, SoundEvents.SOUL_ESCAPE.value(),
                c -> com.mistech.mistechmod.spellcraft.fx.DreamFx.send(c.level(), com.mistech.mistechmod.spellcraft.fx.DreamFx.SOUL_HARVEST, null, (float) c.range(8.0), Vec3.atBottomCenterOf(c.pos())),
                (c, t) -> {
                    hurt(c, t, c.dmg(7.0));
                    com.mistech.mistechmod.spellcraft.fx.DreamFx.send(c.level(), com.mistech.mistechmod.spellcraft.fx.DreamFx.SOUL_TAKE, 0, null, 0.0F, t.position().add(0, t.getBbHeight() * 0.5, 0), c.origin());
                }));

        // ----- fazenda: farinha de osso numa área à frente -----
        add("bloom", Mode.FARM, farm(2, 6));
        add("sprout", Mode.FARM, farm(3, 12));

        // ----- suporte: jogadores perto do bloco -----
        add("water_mend", Mode.SUPPORT, c -> {
            boolean any = false;
            for (Player p : playersAround(c, c.range(8))) {
                if (p.getHealth() < p.getMaxHealth() || p.isOnFire()) {
                    p.heal(c.dmg(6.0));
                    p.clearFire();
                    com.mistech.mistechmod.spellcraft.fx.DreamFx.send(c.level(), com.mistech.mistechmod.spellcraft.fx.DreamFx.MEND, p, 0.0F, p.position());
                    any = true;
                }
            }
            if (any) {
                c.level().playSound(null, c.pos(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.0F, 1.4F);
            }
            return any;
        });
        add("stone_skin", Mode.SUPPORT, c -> {
            boolean any = false;
            for (Player p : playersAround(c, c.range(8))) {
                if (!p.hasEffect(MobEffects.RESISTANCE)) {
                    p.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, c.dur(400), 1));
                    com.mistech.mistechmod.spellcraft.fx.DreamFx.send(c.level(), com.mistech.mistechmod.spellcraft.fx.DreamFx.STONE_SKIN, p, 0.0F, p.position());
                    any = true;
                }
            }
            if (any) {
                c.level().playSound(null, c.pos(), SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 1.0F, 0.7F);
            }
            return any;
        });
        add("holy_light", Mode.SUPPORT, c -> {
            boolean any = false;
            for (Player p : playersAround(c, c.range(8))) {
                if (p.getHealth() < p.getMaxHealth()) {
                    p.heal(c.dmg(5.0));
                    any = true;
                }
            }
            for (LivingEntity t : hostilesAround(c, c.origin(), c.range(8))) {
                if (t.isInvertedHealAndHarm()) {
                    hurt(c, t, c.dmg(10.0));
                    any = true;
                }
            }
            if (any) {
                com.mistech.mistechmod.spellcraft.fx.DreamFx.send(c.level(), com.mistech.mistechmod.spellcraft.fx.DreamFx.HALO, null, (float) c.range(8), Vec3.atBottomCenterOf(c.pos()));
                c.level().playSound(null, c.pos(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.2F, 1.0F);
            }
            return any;
        });
        add("purify", Mode.SUPPORT, c -> {
            boolean any = false;
            for (Player p : playersAround(c, c.range(6))) {
                boolean cleaned = false;
                for (MobEffectInstance inst : new ArrayList<>(p.getActiveEffects())) {
                    if (inst.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                        p.removeEffect(inst.getEffect());
                        cleaned = true;
                    }
                }
                if (cleaned || p.isOnFire() || p.getTicksFrozen() > 0) {
                    p.clearFire();
                    p.setTicksFrozen(0);
                    com.mistech.mistechmod.spellcraft.fx.DreamFx.send(c.level(), com.mistech.mistechmod.spellcraft.fx.DreamFx.MEND, p, 0.0F, p.position());
                    any = true;
                }
            }
            // e limpa a Mácula do lugar (Fase 4): o Conjurador com Purificar vira um limpador
            if (com.mistech.mistechmod.purity.area.AreaTaint.clean(c.level(), c.pos(), 0, com.mistech.mistechmod.Config.areaTaintPurify()) > 0) {
                com.mistech.mistechmod.spellcraft.fx.DreamFx.send(c.level(), com.mistech.mistechmod.spellcraft.fx.DreamFx.PURIFY, null, 4.0F, Vec3.atBottomCenterOf(c.pos()));
                any = true;
            }
            if (any) {
                c.level().playSound(null, c.pos(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.0F, 1.8F);
            }
            return any;
        });

        // ----- logística -----
        add("pull", Mode.LOGISTICS, c -> {
            LivingEntity t = nearestHostile(c, c.range(10));
            if (t == null || t.distanceToSqr(c.origin()) < 4.0) {
                return false;
            }
            Vec3 d = c.origin().subtract(t.position());
            Vec3 n = new Vec3(d.x, 0, d.z).normalize();
            double f = Math.min(1.6, 0.5 + d.length() * 0.1) * Math.min(1.5, c.power());
            t.setDeltaMovement(n.x * f, 0.3, n.z * f);
            t.hurtMarked = true;
            com.mistech.mistechmod.spellcraft.fx.DreamFx.send(c.level(), com.mistech.mistechmod.spellcraft.fx.DreamFx.PULL, 0, null, 0.0F, t.position().add(0, 1, 0), c.origin());
            c.level().playSound(null, c.pos(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 0.5F, 1.6F);
            return true;
        });
        add("telekinesis", Mode.LOGISTICS, BlockCasts::telekinesis);

        // ----- utilidades -----
        add("recharge", Mode.UTILITY, c -> {
            int moved = 0;
            int per = (int) Math.round(6000 * c.power());
            for (Player p : playersAround(c, c.range(8))) {
                int budget = Math.min(per, c.be().energyForSpells());
                if (budget <= 0) {
                    break;
                }
                int m = com.mistech.mistechmod.item.mithril.MithrilGear.chargeInventory(p, budget);
                if (m > 0) {
                    c.be().useEnergyForSpells(m);
                    com.mistech.mistechmod.spellcraft.fx.DreamFx.send(c.level(), com.mistech.mistechmod.spellcraft.fx.DreamFx.RECHARGE, p, 0.0F, p.position());
                    moved += m;
                }
            }
            if (moved > 0) {
                c.level().playSound(null, c.pos(), SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 0.8F, 1.6F);
            }
            return moved > 0;
        });
        add("machine_boost", Mode.UTILITY, c -> {
            c.be().startMachineBoost(c.dur(1200));
            com.mistech.mistechmod.spellcraft.fx.DreamFx.send(c.level(), com.mistech.mistechmod.spellcraft.fx.DreamFx.MACHINE_BOOST, null, 1.0F, Vec3.atCenterOf(c.pos()));
            c.level().playSound(null, c.pos(), SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.8F, 1.8F);
            return true;
        });
        add("light_orb", Mode.UTILITY, c -> {
            int r = (int) c.range(12);
            for (int i = 0; i < 32; i++) {
                BlockPos p = c.pos().offset(c.level().getRandom().nextInt(r * 2 + 1) - r, c.level().getRandom().nextInt(7) - 2,
                        c.level().getRandom().nextInt(r * 2 + 1) - r);
                if (c.level().isLoaded(p) && c.level().getBlockState(p).isAir() && c.level().getBrightness(LightLayer.BLOCK, p) < 7
                        && !c.level().canSeeSky(p) && c.level().getBlockState(p.below()).isFaceSturdy(c.level(), p.below(), Direction.UP)) {
                    c.level().setBlockAndUpdate(p, SpellcraftContent.LIGHT_ORB.get().defaultBlockState());
                    com.mistech.mistechmod.spellcraft.fx.DreamFx.send(c.level(), com.mistech.mistechmod.spellcraft.fx.DreamFx.LIGHT_ORB, null, 0.0F, Vec3.atCenterOf(p));
                    c.level().playSound(null, p, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 1.8F);
                    return true;
                }
            }
            return false;
        });
        add("updraft", Mode.UTILITY, c -> {
            AABB above = new AABB(c.pos()).inflate(0.6, 0, 0.6).expandTowards(0, 2.5, 0);
            List<Player> riders = c.level().getEntitiesOfClass(Player.class, above, p -> p.isAlive() && !p.isSpectator());
            if (riders.isEmpty()) {
                return false;
            }
            for (Player p : riders) {
                p.setDeltaMovement(p.getDeltaMovement().x, 1.1 * Math.min(1.5, Math.max(0.8, c.power())), p.getDeltaMovement().z);
                p.hurtMarked = true;
                p.resetFallDistance();
                p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 120, 0));
            }
            com.mistech.mistechmod.spellcraft.fx.DreamFx.send(c.level(), com.mistech.mistechmod.spellcraft.fx.DreamFx.UPDRAFT, null, 0.0F, Vec3.atBottomCenterOf(c.pos().above()));
            c.level().playSound(null, c.pos(), SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), SoundSource.BLOCKS, 1.0F, 1.2F);
            return true;
        });
        add("vigilant_gaze", Mode.UTILITY, c -> {
            List<LivingEntity> seen = hostilesAround(c, c.origin(), c.range(32));
            if (seen.isEmpty()) {
                c.be().setAlarm(0);
                return false;
            }
            for (LivingEntity e : seen) {
                e.addEffect(new MobEffectInstance(MobEffects.GLOWING, c.dur(400), 0));
            }
            c.be().setAlarm(Math.min(15, seen.size()));
            com.mistech.mistechmod.spellcraft.fx.DreamFx.send(c.level(), com.mistech.mistechmod.spellcraft.fx.DreamFx.VIGIL, null, 0.0F, Vec3.atBottomCenterOf(c.pos()).add(0, 0.6, 0));
            c.level().playSound(null, c.pos(), SoundEvents.SCULK_CLICKING, SoundSource.BLOCKS, 1.0F, 1.0F);
            return true;
        });
    }

    /** Telecinese: puxa os itens soltos e guarda no inventário atrás do Conjurador (ou deixa em cima dele). */
    private static boolean telekinesis(Ctx c) {
        double r = c.range(10);
        Vec3 o = c.origin();
        List<ItemEntity> items = c.level().getEntitiesOfClass(ItemEntity.class, new AABB(o, o).inflate(r), e -> e.isAlive() && !e.getItem().isEmpty());
        if (items.isEmpty()) {
            return false;
        }
        Direction back = c.facing().getOpposite();
        BlockPos behind = c.pos().relative(back);
        var handler = c.level().getCapability(Capabilities.Item.BLOCK, behind, back.getOpposite());
        int stored = 0;
        for (ItemEntity item : items) {
            ItemStack stack = item.getItem();
            if (handler != null) {
                int inserted;
                try (Transaction tx = Transaction.openRoot()) {
                    inserted = handler.insert(ItemResource.of(stack), stack.getCount(), tx);
                    tx.commit();
                }
                if (inserted > 0) {
                    com.mistech.mistechmod.spellcraft.fx.DreamFx.send(c.level(), com.mistech.mistechmod.spellcraft.fx.DreamFx.PULL, 0, null, 0.0F, item.position().add(0, 0.3, 0), o);
                    stored += inserted;
                    if (inserted >= stack.getCount()) {
                        item.discard();
                    } else {
                        ItemStack rest = stack.copy();
                        rest.shrink(inserted);
                        item.setItem(rest);
                    }
                    continue;
                }
            }
            if (item.position().distanceTo(o) > 1.5) {
                Vec3 d = o.subtract(item.position());
                item.setDeltaMovement(d.scale(0.25));
                item.hurtMarked = true;
                stored++;
            }
        }
        if (stored == 0) {
            return false;
        }
        com.mistech.mistechmod.spellcraft.fx.DreamFx.send(c.level(), com.mistech.mistechmod.spellcraft.fx.DreamFx.TELEKINESIS, null, (float) Math.min(r, 6.0), Vec3.atBottomCenterOf(c.pos()));
        c.level().playSound(null, c.pos(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 0.4F, 1.9F);
        return true;
    }
}

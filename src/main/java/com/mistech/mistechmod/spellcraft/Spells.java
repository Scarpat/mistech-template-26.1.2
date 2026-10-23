package com.mistech.mistechmod.spellcraft;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.affinity.Affinity;
import com.mistech.mistechmod.item.RuneElement;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Todas as magias do Mistech (sistema novo: pergaminhos + mana + afinidade). Para criar uma magia nova, é só
 * registrar aqui com {@link #add}; o resto (pergaminho, texto da Mesa, tooltip) sai sozinho. Os textos ficam em
 * {@code spell.mistech.<id>} e {@code spell.mistech.<id>.desc} (gerados por tools/spellcraft/gen_lang.py).
 */
public final class Spells {
    private static final Map<String, Spell> ALL = new LinkedHashMap<>();

    private Spells() {}

    public static @Nullable Spell byId(@Nullable String id) {
        return id == null ? null : ALL.get(id);
    }

    public static Collection<Spell> all() {
        return ALL.values();
    }

    /** Ordem de exibição: por tier, depois elemento, depois id. */
    public static final Comparator<Spell> ORDER = Comparator.comparingInt(Spell::tier)
            .thenComparing(s -> s.element().ordinal()).thenComparing(Spell::id);

    public static List<Spell> sorted(Collection<Spell> spells) {
        List<Spell> list = new ArrayList<>(spells);
        list.sort(ORDER);
        return list;
    }

    /** Magias aprendidas pelo jogador, em ordem de exibição. */
    public static List<Spell> learnedBy(Player player) {
        List<Spell> list = new ArrayList<>();
        for (String id : SpellBook.of(player).learned()) {
            Spell spell = byId(id);
            if (spell != null) {
                list.add(spell);
            }
        }
        list.sort(ORDER);
        return list;
    }

    // ------------------------------------------------------------------ registro

    private static Spell add(String id, RuneElement element, int tier, int mana, int cooldown, boolean worldOnly, Spell.Effect effect) {
        Spell spell = new Spell(id, element, tier, mana, cooldown, worldOnly, costs(element, tier), tier * 3 - 2, effect);
        ALL.put(id, spell);
        return spell;
    }

    /** Custo na Mesa de Inscrição: papel, lápis (tinta), runa do elemento; tiers altos pedem mais. */
    private static List<Spell.Cost> costs(RuneElement element, int tier) {
        List<Spell.Cost> list = new ArrayList<>();
        list.add(new Spell.Cost(() -> Items.PAPER, 1));
        list.add(new Spell.Cost(() -> Items.LAPIS_LAZULI, tier));
        list.add(new Spell.Cost(element::rune, tier >= 3 ? 2 : 1));
        if (tier >= 3) {
            list.add(new Spell.Cost(() -> Mistech.IMPURE_MAGIC_SHARD.get(), tier - 2));
        }
        if (tier >= 4) {
            list.add(new Spell.Cost(() -> Mistech.ARCANE_RUNE.get(), 1));
        }
        return list;
    }

    public static final Spell FIRE_SPARK = add("fire_spark", RuneElement.FIRE, 1, 8, 10, false,
            (l, p) -> bolt(l, p, 24, 5.0F, RuneElement.FIRE, ParticleTypes.FLAME, SoundEvents.FIRECHARGE_USE, t -> t.igniteForSeconds(4.0F)));
    public static final Spell ICE_SHARD = add("ice_shard", RuneElement.ICE, 1, 8, 10, false,
            (l, p) -> bolt(l, p, 24, 5.0F, RuneElement.ICE, ParticleTypes.SNOWFLAKE, SoundEvents.GLASS_BREAK, Blocks.PACKED_ICE.defaultBlockState(), 0.35F, t -> {
                t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1));
                t.setTicksFrozen(Math.max(t.getTicksFrozen(), t.getTicksRequiredToFreeze() + 40));
            }));
    public static final Spell WATER_BOLT = add("water_bolt", RuneElement.WATER, 1, 8, 12, false,
            (l, p) -> bolt(l, p, 20, 4.0F, RuneElement.WATER, ParticleTypes.SPLASH, SoundEvents.PLAYER_SPLASH, t -> {
                Vec3 look = p(l, t);
                t.push(look.x * 0.9, 0.3, look.z * 0.9);
                t.hurtMarked = true;
            }));
    /** Pedregulho: carregada. Quanto mais carga, mais dano e arremesso. */
    public static final Spell BOULDER = add("boulder", RuneElement.EARTH, 1, 10, 12, false,
            (l, p) -> bolt(l, p, 22, 4.0F, RuneElement.EARTH, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()),
                    SoundEvents.STONE_BREAK, Blocks.COBBLESTONE.defaultBlockState(), (float) (0.5 + 0.6 * Spells.CHARGE.get()), t -> {
                        Vec3 look = p(l, t);
                        t.push(look.x * 0.6, 0.25, look.z * 0.6);
                        t.hurtMarked = true;
                    })).charged(24);
    public static final Spell GUST = add("gust", RuneElement.AIR, 1, 10, 30, false, Spells::gust);

    public static final Spell ENERGY_DISCHARGE = add("energy_discharge", RuneElement.ENERGY, 2, 20, 30, false, Spells::discharge);
    public static final Spell WATER_MEND = add("water_mend", RuneElement.WATER, 2, 18, 80, false, Spells::mend);
    public static final Spell STONE_SKIN = add("stone_skin", RuneElement.EARTH, 2, 22, 300, false, (l, p) -> {
        p.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, dur(p, RuneElement.EARTH, 400), 1));
        p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, dur(p, RuneElement.EARTH, 400), 0));
        l.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()), p.getX(), p.getY() + 1.0, p.getZ(), 60, 0.5, 0.9, 0.5, 0.1);
        SpellFx.helix(l, p, RuneElement.EARTH, 12, 2.0, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()));
        SpellFx.expandingRing(l, p.position(), RuneElement.EARTH, 2.5, 5, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState()));
        l.playSound(null, p.blockPosition(), SoundEvents.STONE_PLACE, SoundSource.PLAYERS, 1.0F, 0.7F);
        return true;
    });
    public static final Spell HOLY_LIGHT = add("holy_light", RuneElement.LIGHT, 2, 28, 100, false, Spells::holyLight);
    public static final Spell SHADOW_VEIL = add("shadow_veil", RuneElement.DARKNESS, 2, 25, 300, false, (l, p) -> {
        p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, dur(p, RuneElement.DARKNESS, 300), 0));
        p.addEffect(new MobEffectInstance(MobEffects.SPEED, dur(p, RuneElement.DARKNESS, 300), 0));
        SpellFx.contractingRing(l, p.position(), RuneElement.DARKNESS, 3.0, 8, ParticleTypes.LARGE_SMOKE);
        SpellFx.burst(l, p.position().add(0, 1.0, 0), RuneElement.DARKNESS, 0.25, 40, ParticleTypes.SQUID_INK);
        l.sendParticles(ParticleTypes.LARGE_SMOKE, p.getX(), p.getY() + 1.0, p.getZ(), 30, 0.4, 0.8, 0.4, 0.02);
        l.playSound(null, p.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.6F, 0.6F);
        return true;
    });
    public static final Spell BLINK = add("blink", RuneElement.SPACE, 2, 30, 60, false, Spells::blink);

    public static final Spell FIRE_BURST = add("fire_burst", RuneElement.FIRE, 3, 35, 60, false, Spells::fireBurst);
    public static final Spell WIND_STEP = add("wind_step", RuneElement.AIR, 3, 15, 60, false, Spells::windStep);
    public static final Spell DARK_DRAIN = add("dark_drain", RuneElement.DARKNESS, 3, 35, 60, false,
            (l, p) -> bolt(l, p, 20, 6.0F, RuneElement.DARKNESS, ParticleTypes.SOUL, SoundEvents.WITHER_HURT, t -> {
                p.heal((float) (3.0 * pw(p, RuneElement.DARKNESS)));
                Vec3 from = t.position().add(0, t.getBbHeight() * 0.5, 0);
                SpellFx.later(l, 4, () -> SpellFx.projectile(l, from, p.position().add(0, 1.0, 0), RuneElement.DARKNESS, ParticleTypes.SOUL, () -> {}));
            }));
    public static final Spell TIME_SLOW = add("time_slow", RuneElement.TIME, 3, 9, 100, false, (l, p) -> {
        int hit = areaEffects(l, p, p.position(), rad(p, RuneElement.TIME, 8.0), t -> {
            t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 30, 3));
            t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 30, 0));
        });
        SpellFx.expandingRing(l, p.position(), RuneElement.TIME, 8.0, 8, ParticleTypes.REVERSE_PORTAL);
        SpellFx.contractingRing(l, p.position(), RuneElement.TIME, 5.0, 8, ParticleTypes.END_ROD);
        l.sendParticles(ParticleTypes.REVERSE_PORTAL, p.getX(), p.getY() + 1.0, p.getZ(), 40, 4.0, 0.6, 4.0, 0.02);
        l.playSound(null, p.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.5F, 0.6F);
        return true;
    }).channeled(10);

    // ------------------------------------------------------------------ magias básicas que faltavam (T1-T2)
    /** Arco Elétrico: salta por até 3 criaturas próximas umas das outras. */
    public static final Spell SPARK_ARC = add("spark_arc", RuneElement.ENERGY, 1, 8, 14, false, Spells::sparkArc);
    public static final Spell GLIMMER = add("glimmer", RuneElement.LIGHT, 1, 8, 100, false, (l, p) -> {
        double r = rad(p, RuneElement.LIGHT, 12.0);
        areaEffects(l, p, p.position(), r, t -> t.addEffect(new MobEffectInstance(MobEffects.GLOWING, dur(p, RuneElement.LIGHT, 200), 0)));
        SpellFx.expandingRing(l, p.position(), RuneElement.LIGHT, r, 8, ParticleTypes.END_ROD);
        l.sendParticles(ParticleTypes.GLOW, p.getX(), p.getY() + 1.0, p.getZ(), 30, 1.0, 0.8, 1.0, 0.0);
        l.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 1.6F);
        return true;
    });
    public static final Spell GLOOM = add("gloom", RuneElement.DARKNESS, 1, 8, 40, false,
            (l, p) -> bolt(l, p, 18, 3.0F, RuneElement.DARKNESS, ParticleTypes.SMOKE, SoundEvents.ENDERMAN_STARE, t -> {
                t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, dur(p, RuneElement.DARKNESS, 60), 0));
                t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, dur(p, RuneElement.DARKNESS, 60), 1));
            }));
    public static final Spell QUICKEN = add("quicken", RuneElement.TIME, 1, 10, 160, false, (l, p) -> {
        p.addEffect(new MobEffectInstance(MobEffects.SPEED, dur(p, RuneElement.TIME, 200), 0));
        p.addEffect(new MobEffectInstance(MobEffects.HASTE, dur(p, RuneElement.TIME, 200), 0));
        SpellFx.contractingRing(l, p.position(), RuneElement.TIME, 2.5, 6, ParticleTypes.END_ROD);
        l.sendParticles(ParticleTypes.REVERSE_PORTAL, p.getX(), p.getY() + 1.0, p.getZ(), 25, 0.4, 0.8, 0.4, 0.05);
        l.playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.5F, 1.8F);
        return true;
    });
    /** Puxão: traz a criatura mirada até você. */
    public static final Spell PULL = add("pull", RuneElement.SPACE, 1, 10, 40, false, (l, p) -> {
        Aim aim = aim(l, p, 16);
        Vec3 from = p.getEyePosition().add(p.getLookAngle().scale(0.8));
        Vec3 end = aim != null ? aim.point() : p.getEyePosition().add(p.getLookAngle().scale(10));
        if (aim != null && aim.entity() != null) {
            LivingEntity t = aim.entity();
            Vec3 d = p.position().subtract(t.position());
            Vec3 n = new Vec3(d.x, 0, d.z).normalize();
            double f = Math.min(1.6, 0.5 + d.length() * 0.1) * Math.min(1.5, pw(p, RuneElement.SPACE));
            t.setDeltaMovement(n.x * f, 0.3, n.z * f);
            t.hurtMarked = true;
        }
        SpellFx.projectile(l, end, from, RuneElement.SPACE, ParticleTypes.PORTAL, () -> {});
        l.playSound(null, p.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.5F, 1.6F);
        return true;
    });
    public static final Spell EMBER_RING = add("ember_ring", RuneElement.FIRE, 2, 18, 80, false, (l, p) -> {
        double r = rad(p, RuneElement.FIRE, 4.0);
        areaEffects(l, p, p.position(), r, t -> {
            t.hurtServer(l, l.damageSources().indirectMagic(p, p), (float) (3.0 * pw(p, RuneElement.FIRE)));
            t.igniteForSeconds(5.0F);
        });
        SpellFx.expandingRing(l, p.position(), RuneElement.FIRE, r, 8, ParticleTypes.FLAME);
        SpellFx.burst(l, p.position().add(0, 0.5, 0), RuneElement.FIRE, 0.3, 40, ParticleTypes.FLAME);
        l.playSound(null, p.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 0.8F);
        return true;
    });
    public static final Spell FROST_NOVA = add("frost_nova", RuneElement.ICE, 2, 20, 100, false, (l, p) -> {
        double r = rad(p, RuneElement.ICE, 5.0);
        areaEffects(l, p, p.position(), r, t -> {
            t.hurtServer(l, l.damageSources().indirectMagic(p, p), (float) (4.0 * pw(p, RuneElement.ICE)));
            t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, dur(p, RuneElement.ICE, 80), 2));
            t.setTicksFrozen(Math.max(t.getTicksFrozen(), t.getTicksRequiredToFreeze() + 60));
        });
        SpellFx.expandingRing(l, p.position(), RuneElement.ICE, r, 8, ParticleTypes.SNOWFLAKE);
        SpellFx.burst(l, p.position().add(0, 0.5, 0), RuneElement.ICE, 0.35, 50, ParticleTypes.SNOWFLAKE);
        l.playSound(null, p.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0F, 0.7F);
        return true;
    });
    public static final Spell UPDRAFT = add("updraft", RuneElement.AIR, 2, 14, 80, false, (l, p) -> {
        p.setDeltaMovement(p.getDeltaMovement().x, 1.1, p.getDeltaMovement().z);
        p.hurtMarked = true;
        p.resetFallDistance();
        p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 120, 0));
        SpellFx.column(l, p.position(), RuneElement.AIR, 3.0, 6, ParticleTypes.CLOUD);
        SpellFx.burst(l, p.position().add(0, 0.3, 0), RuneElement.AIR, 0.3, 30, ParticleTypes.CLOUD);
        l.playSound(null, p.blockPosition(), SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), SoundSource.PLAYERS, 1.0F, 1.2F);
        return true;
    });
    public static final Spell FORESIGHT = add("foresight", RuneElement.TIME, 2, 22, 300, false, (l, p) -> {
        p.addEffect(new MobEffectInstance(MobEffects.SPEED, dur(p, RuneElement.TIME, 240), 1));
        p.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, dur(p, RuneElement.TIME, 240), 0));
        SpellFx.helix(l, p, RuneElement.TIME, 14, 2.0, ParticleTypes.END_ROD);
        SpellFx.contractingRing(l, p.position(), RuneElement.TIME, 3.0, 8, ParticleTypes.REVERSE_PORTAL);
        l.playSound(null, p.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.8F, 1.4F);
        return true;
    });
    /** Floração: cura leve, regeneração e faz brotar o que está por perto. */
    public static final Spell BLOOM = add("bloom", RuneElement.LIFE, 1, 8, 100, false, (l, p) -> {
        double w = pw(p, RuneElement.LIFE);
        p.heal((float) (2.0 * w));
        p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, dur(p, RuneElement.LIFE, 100), 0));
        net.minecraft.world.item.ItemStack meal = new net.minecraft.world.item.ItemStack(Items.BONE_MEAL);
        BlockPos c = p.blockPosition();
        for (int i = 0; i < 6; i++) {
            BlockPos pos = c.offset(l.getRandom().nextInt(5) - 2, l.getRandom().nextInt(3) - 1, l.getRandom().nextInt(5) - 2);
            if (net.minecraft.world.item.BoneMealItem.growCrop(meal, l, pos)) {
                l.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 5, 0.3, 0.3, 0.3, 0.0);
            }
        }
        SpellFx.helix(l, p, RuneElement.LIFE, 10, 1.6, ParticleTypes.HAPPY_VILLAGER);
        l.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OAK_LEAVES.defaultBlockState()), p.getX(), p.getY() + 1.0, p.getZ(), 15, 0.5, 0.7, 0.5, 0.05);
        l.playSound(null, p.blockPosition(), SoundEvents.BONE_MEAL_USE, SoundSource.PLAYERS, 0.8F, 1.3F);
        return true;
    });

    // ------------------------------------------------------------------ invocações e utilidades (fase E)
    public static final Spell STONE_GOLEM = add("stone_golem", RuneElement.EARTH, 4, 60, 600, false, Summons::stoneGolem).ritual(40);
    public static final Spell LIVING_FOREST = add("living_forest", RuneElement.LIFE, 4, 70, 700, false, Summons::livingForest).ritual(50);
    public static final Spell SHADOW_WOLF = add("shadow_wolf", RuneElement.DARKNESS, 3, 40, 400, false, Summons::shadowWolf);
    public static final Spell ANCHOR = add("anchor", RuneElement.SPACE, 3, 25, 200, false, Summons::anchor);
    public static final Spell REWIND = add("rewind", RuneElement.TIME, 4, 45, 400, false, Summons::rewind);
    public static final Spell TELEKINESIS = add("telekinesis", RuneElement.SPACE, 2, 12, 60, false, (l, p) -> {
        double r = rad(p, RuneElement.SPACE, 10.0);
        int n = 0;
        for (net.minecraft.world.entity.item.ItemEntity item : l.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, p.getBoundingBox().inflate(r))) {
            Vec3 d = p.position().add(0, 0.5, 0).subtract(item.position());
            item.setDeltaMovement(d.scale(0.25));
            item.hurtMarked = true;
            item.setNoPickUpDelay();
            n++;
        }
        SpellFx.contractingRing(l, p.position(), RuneElement.SPACE, r, 8, ParticleTypes.PORTAL);
        l.playSound(null, p.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.4F, 1.9F);
        return true;
    });
    public static final Spell PURIFY = add("purify", RuneElement.WATER, 3, 30, 200, false, (l, p) -> {
        double r = rad(p, RuneElement.WATER, 6.0);
        java.util.List<Player> group = new ArrayList<>();
        group.add(p);
        group.addAll(l.getEntitiesOfClass(Player.class, p.getBoundingBox().inflate(r), e -> e != p && e.isAlive() && e.isAlliedTo(p)));
        for (Player target : group) {
            for (MobEffectInstance inst : new ArrayList<>(target.getActiveEffects())) {
                if (inst.getEffect().value().getCategory() == net.minecraft.world.effect.MobEffectCategory.HARMFUL) {
                    target.removeEffect(inst.getEffect());
                }
            }
            target.clearFire();
            target.setTicksFrozen(0);
        }
        SpellFx.expandingRing(l, p.position(), RuneElement.WATER, r, 8, ParticleTypes.SPLASH);
        SpellFx.helix(l, p, RuneElement.WATER, 14, 2.0, ParticleTypes.BUBBLE_POP);
        l.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 1.8F);
        return true;
    });

    // ------------------------------------------------------------------ Vida (Água + Terra)
    public static final Spell SPROUT = add("sprout", RuneElement.LIFE, 2, 20, 120, false, Spells::sprout);
    /** Cipó: contínua. Prende num bloco ou criatura e puxa enquanto o botão fica pressionado (ver {@link VineGrapple}). */
    public static final Spell VINE = add("vine", RuneElement.LIFE, 3, 8, 20, false, (l, p) -> VineGrapple.start(l, p)).channeled(10);
    /** Cipó Rápido: alcance menor (14), corda encolhe bem mais rápido. */
    public static final Spell QUICK_VINE = add("quick_vine", RuneElement.LIFE, 2, 6, 20, false,
            (l, p) -> VineGrapple.start(l, p, VineGrapple.Kind.QUICK)).channeled(10);
    /** Teia Viva: alcance 40, prende as criaturas em volta do ponto e dá um impulso ao soltar. */
    public static final Spell LIVING_WEB = add("living_web", RuneElement.LIFE, 4, 10, 30, false,
            (l, p) -> VineGrapple.start(l, p, VineGrapple.Kind.WEB)).channeled(10);
    public static final Spell THORNS = add("thorns", RuneElement.LIFE, 3, 30, 160, false, Spells::thorns);

    /** Meteoro: carregada. Marca o ponto mirado e, depois de um instante, uma a três pedras de fogo caem. */
    public static final Spell METEOR = add("meteor", RuneElement.FIRE, 4, 50, 240, false, Spells::meteor).charged(40);

    public static final Spell BLIZZARD = add("blizzard", RuneElement.ICE, 4, 10, 120, false, Spells::blizzard).channeled(8);
    /** Só achadas no mundo: tier alto, poderosas. */
    public static final Spell EARTHQUAKE = add("earthquake", RuneElement.EARTH, 4, 12, 200, true, Spells::earthquake).channeled(15);

    public static final Spell THUNDERSTORM = add("thunderstorm", RuneElement.ENERGY, 5, 90, 400, true, Spells::thunderstorm).ritual(50);
    public static final Spell TIME_STOP = add("time_stop", RuneElement.TIME, 5, 120, 600, true, (l, p) -> {
        int hit = areaEffects(l, p, p.position(), rad(p, RuneElement.TIME, 10.0), t -> {
            t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, dur(p, RuneElement.TIME, 100), 6));
            t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, dur(p, RuneElement.TIME, 100), 4));
            t.addEffect(new MobEffectInstance(MobEffects.GLOWING, dur(p, RuneElement.TIME, 100), 0));
        });
        SpellFx.expandingRing(l, p.position(), RuneElement.TIME, 10.0, 8, ParticleTypes.END_ROD);
        SpellFx.expandingRing(l, p.position().add(0, 1.2, 0), RuneElement.TIME, 10.0, 12, null);
        SpellFx.burst(l, p.position().add(0, 1.0, 0), RuneElement.TIME, 0.5, 80, ParticleTypes.END_ROD);
        SpellFx.column(l, p.position(), RuneElement.TIME, 5.0, 10, ParticleTypes.REVERSE_PORTAL);
        l.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1.0, p.getZ(), 120, 5.0, 1.0, 5.0, 0.01);
        l.playSound(null, p.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.5F, 0.4F);
        return true;
    });

    // ------------------------------------------------------------------ combinações: Magma, Vapor, Nuvem
    public static final Spell LAVA_RIVER = add("lava_river", RuneElement.MAGMA, 3, 35, 200, false, ComboSpells::lavaRiver);
    public static final Spell ERUPTION = add("eruption", RuneElement.MAGMA, 4, 60, 400, false, ComboSpells::eruption).ritual(40);
    public static final Spell MAGMA_ARMOR = add("magma_armor", RuneElement.MAGMA, 4, 45, 900, false, ComboSpells::magmaArmor);
    public static final Spell MIST = add("mist", RuneElement.VAPOR, 2, 20, 300, false, ComboSpells::mist);
    public static final Spell STEAM_JET = add("steam_jet", RuneElement.VAPOR, 3, 28, 80, false, ComboSpells::steamJet);
    public static final Spell CLOUD_PLATFORM = add("cloud_platform", RuneElement.CLOUD, 3, 25, 200, false, ComboSpells::cloudPlatform);
    public static final Spell CLOUD_BRIDGE = add("cloud_bridge", RuneElement.CLOUD, 4, 40, 400, false, ComboSpells::cloudBridge);

    // ------------------------------------------------------------------ invocações voadoras, utilidades e elementos avançados
    public static final Spell SPIRIT_GUARDIAN = add("spirit_guardian", RuneElement.SOUL, 3, 40, 500, false, Summons::spiritGuardian);
    public static final Spell PHOENIX = add("phoenix", RuneElement.FIRE, 5, 90, 900, false, Summons::phoenix).ritual(50);
    public static final Spell SOUL_HARVEST = add("soul_harvest", RuneElement.SOUL, 4, 45, 300, false, UtilitySpells::soulHarvest);
    public static final Spell RECHARGE = add("recharge", RuneElement.ARCANE, 2, 30, 100, false, UtilitySpells::recharge);
    public static final Spell MACHINE_BOOST = add("machine_boost", RuneElement.ARCANE, 3, 35, 400, false, UtilitySpells::machineBoost);
    public static final Spell ARCANE_FOCUS = add("arcane_focus", RuneElement.ARCANE, 3, 30, 300, false, UtilitySpells::arcaneFocus);
    public static final Spell RIFT = add("rift", RuneElement.SPACE, 3, 20, 100, false, UtilitySpells::rift);
    public static final Spell LIGHT_ORB = add("light_orb", RuneElement.LIGHT, 1, 5, 10, false, UtilitySpells::lightOrb);
    public static final Spell VIGILANT_GAZE = add("vigilant_gaze", RuneElement.VIGIL, 2, 20, 300, false, UtilitySpells::vigilantGaze);
    public static final Spell LUCKY_COIN = add("lucky_coin", RuneElement.LUCK, 2, 20, 600, false, UtilitySpells::luckyCoin);
    public static final Spell ECHO = add("echo", RuneElement.ECHO, 3, 30, 200, false, UtilitySpells::echo);
    public static final Spell VOID_TOUCH = add("void_touch", RuneElement.VOID, 3, 30, 60, false,
            (l, p) -> bolt(l, p, 20, 9.0F, RuneElement.VOID, ParticleTypes.REVERSE_PORTAL, SoundEvents.ENDERMAN_HURT, t -> {
                t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1));
                t.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 20, 0));
            }));

    // ------------------------------------------------------------------ lendárias (tier 6: nível 6; só achadas no mundo)
    public static final Spell INFERNO = add("inferno", RuneElement.FIRE, 6, 110, 900, true, LegendarySpells::inferno).ritual(60);
    public static final Spell ICE_AGE = add("ice_age", RuneElement.ICE, 6, 110, 900, true, LegendarySpells::iceAge).ritual(60);
    public static final Spell TIDAL_WAVE = add("tidal_wave", RuneElement.WATER, 6, 90, 700, true, LegendarySpells::tidalWave).charged(40);
    public static final Spell CATACLYSM = add("cataclysm", RuneElement.EARTH, 6, 110, 900, true, LegendarySpells::cataclysm).ritual(50);
    public static final Spell HURRICANE = add("hurricane", RuneElement.AIR, 6, 16, 700, true, LegendarySpells::hurricane).channeled(10);
    public static final Spell JUDGEMENT = add("judgement", RuneElement.ENERGY, 6, 100, 800, true, LegendarySpells::judgement).charged(50);
    public static final Spell DAWN = add("dawn", RuneElement.LIGHT, 6, 120, 1200, true, LegendarySpells::dawn).ritual(50);
    public static final Spell TOTAL_ECLIPSE = add("total_eclipse", RuneElement.DARKNESS, 6, 110, 900, true, LegendarySpells::totalEclipse).ritual(50);
    public static final Spell ETERNAL_INSTANT = add("eternal_instant", RuneElement.TIME, 6, 100, 1800, true, LegendarySpells::eternalInstant);
    public static final Spell BLACK_HOLE = add("black_hole", RuneElement.SPACE, 6, 120, 1200, true, LegendarySpells::blackHole).ritual(60);
    public static final Spell TREE_OF_LIFE = add("tree_of_life", RuneElement.LIFE, 6, 110, 1200, true, LegendarySpells::treeOfLife).ritual(60);

    // ------------------------------------------------------------------ poder de magia

    /** Multiplicador de dano, cura e duração: +10% por nível de afinidade no elemento. */
    static double pw(ServerPlayer player, RuneElement element) {
        return Affinity.power(player, element) * (1.0 + CHARGE.get()) * FOCUS_POWER.get();
    }

    /** Carga da magia que está sendo lançada (0 a 1) e poder do item que lança (Cajados somam). Valem só durante o lançamento. */
    static final ThreadLocal<Double> CHARGE = ThreadLocal.withInitial(() -> 0.0);
    static final ThreadLocal<Double> FOCUS_POWER = ThreadLocal.withInitial(() -> 1.0);

    /** O raio cresce metade do que o poder cresce (nível 7: +35%). */
    static double rad(ServerPlayer player, RuneElement element, double base) {
        return base * (1.0 + (pw(player, element) - 1.0) * 0.5);
    }

    static int dur(ServerPlayer player, RuneElement element, int ticks) {
        return (int) Math.round(ticks * pw(player, element));
    }

    // ------------------------------------------------------------------ mira e alvos

    /** Para onde o jogador está olhando: criatura (se houver) ou ponto do bloco. */
    record Aim(@Nullable LivingEntity entity, Vec3 point, double distance) {}

    static @Nullable Aim aim(ServerLevel level, ServerPlayer player, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(range));
        BlockHitResult block = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        double max = block.getType() == HitResult.Type.MISS ? range : block.getLocation().distanceTo(eye);
        Vec3 limit = eye.add(look.scale(max));
        LivingEntity best = null;
        double bestDistance = max;
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(eye, limit).inflate(1.5),
                e -> e != player && e.isAlive() && !e.isSpectator() && !friendly(player, e))) {
            Optional<Vec3> hit = entity.getBoundingBox().inflate(0.3).clip(eye, limit);
            if (hit.isPresent()) {
                double d = hit.get().distanceTo(eye);
                if (d < bestDistance) {
                    bestDistance = d;
                    best = entity;
                }
            }
        }
        if (best != null) {
            return new Aim(best, best.position().add(0, best.getBbHeight() * 0.5, 0), bestDistance);
        }
        if (block.getType() == HitResult.Type.MISS) {
            return null;
        }
        return new Aim(null, block.getLocation(), max);
    }

    private static boolean friendly(Player player, LivingEntity entity) {
        return entity.isAlliedTo(player) || entity instanceof TamableAnimal pet && pet.isOwnedBy(player);
    }

    private static Vec3 p(ServerLevel level, LivingEntity target) {
        // direção de empurrão: do jogador mais próximo para o alvo
        Player near = level.getNearestPlayer(target, 32.0);
        Vec3 dir = near == null ? new Vec3(0, 0, 1) : target.position().subtract(near.position());
        return new Vec3(dir.x, 0, dir.z).normalize();
    }

    static boolean bolt(ServerLevel level, ServerPlayer player, double range, float damage, RuneElement element, ParticleOptions particle,
                                SoundEvent sound, Consumer<LivingEntity> onHit) {
        return bolt(level, player, range, damage, element, particle, sound, null, 0.5F, onHit);
    }

    /**
     * Projétil de verdade: sai da mão, viaja ~1,6 bloco por tick e só fere ao acertar (o primeiro bloco ou criatura do
     * caminho). {@code body} = bloco girando no lugar da esfera de luz (pedra, gelo...).
     */
    static boolean bolt(ServerLevel level, ServerPlayer player, double range, float damage, RuneElement element, ParticleOptions particle,
                        SoundEvent sound, net.minecraft.world.level.block.state.@Nullable BlockState body, float size, Consumer<LivingEntity> onHit) {
        Aim aim = aim(level, player, range);
        Vec3 start = player.getEyePosition().add(player.getLookAngle().scale(0.8)).subtract(0, 0.25, 0);
        Vec3 end = aim != null ? aim.point() : player.getEyePosition().add(player.getLookAngle().scale(range));
        Vec3 dir = end.subtract(start);
        if (dir.lengthSqr() < 1.0E-4) {
            dir = player.getLookAngle();
        }
        float dmg = (float) (damage * pw(player, element));
        level.playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS, 0.6F, 1.6F);
        Consumer<LivingEntity> hit = target -> {
            target.hurtServer(level, level.damageSources().indirectMagic(player, player), dmg);
            onHit.accept(target);
        };
        Consumer<Vec3> impact = at -> {
            SpellFx.burst(level, at, element, 0.25, 26, particle);
            com.mistech.mistechmod.spellcraft.fx.Vfx.shockwave(level, at.subtract(0, 0.1, 0), element, 1.4, 6);
            SpellVisuals.impact(level, at, element);
            level.playSound(null, BlockPos.containing(at), sound, SoundSource.PLAYERS, 0.8F, 1.3F);
        };
        if (body != null) {
            com.mistech.mistechmod.spellcraft.fx.MagicProjectiles.block(level, player, element, start, dir, 1.4, range + 2, body, size, particle, hit, impact);
        } else {
            com.mistech.mistechmod.spellcraft.fx.MagicProjectiles.orb(level, player, element, start, dir, 1.6, range + 2, size, particle, hit, impact);
        }
        return true;
    }

    static List<LivingEntity> around(ServerLevel level, ServerPlayer player, Vec3 center, double radius) {
        return level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius, Math.max(2.0, radius / 2), radius),
                e -> e != player && e.isAlive() && !e.isSpectator() && !friendly(player, e) && e.position().distanceTo(center) <= radius + 0.5);
    }

    /** Aplica algo em todas as criaturas hostis do raio (jogadores não entram). Devolve quantas. */
    static int areaEffects(ServerLevel level, ServerPlayer player, Vec3 center, double radius, Consumer<LivingEntity> action) {
        int count = 0;
        for (LivingEntity target : around(level, player, center, radius)) {
            if (target instanceof Player) {
                continue;
            }
            action.accept(target);
            count++;
        }
        return count;
    }

    // ------------------------------------------------------------------ magias maiores

    private static boolean sparkArc(ServerLevel level, ServerPlayer player) {
        Aim aim = aim(level, player, 16);
        LivingEntity first = aim != null ? aim.entity() : null;
        if (first == null) {
            double best = Double.MAX_VALUE;
            for (LivingEntity e : around(level, player, player.position(), 10.0)) {
                double d = e.distanceToSqr(player);
                if (!(e instanceof Player) && d < best) {
                    best = d;
                    first = e;
                }
            }
        }
        Vec3 from = player.getEyePosition().add(player.getLookAngle().scale(0.6));
        if (first == null) {
            Vec3 end = from.add(player.getLookAngle().scale(6));
            SpellFx.projectile(level, from, end, RuneElement.ENERGY, ParticleTypes.ELECTRIC_SPARK, () -> {});
            level.playSound(null, player.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.3F, 2.0F);
            return true;
        }
        java.util.Set<LivingEntity> hit = new java.util.HashSet<>();
        LivingEntity current = first;
        Vec3 origin = from;
        for (int i = 0; i < 3 && current != null; i++) {
            hit.add(current);
            Vec3 target = current.position().add(0, current.getBbHeight() * 0.5, 0);
            current.hurtServer(level, level.damageSources().indirectMagic(player, player), (float) (3.5 * pw(player, RuneElement.ENERGY)));
            SpellFx.lightning(level, origin, target, RuneElement.ENERGY);
            SpellFx.burst(level, target, RuneElement.ENERGY, 0.2, 12, ParticleTypes.ELECTRIC_SPARK);
            origin = target;
            LivingEntity next = null;
            double best = 36.0;
            for (LivingEntity e : around(level, player, current.position(), 6.0)) {
                if (!hit.contains(e) && !(e instanceof Player) && e.distanceToSqr(current) < best) {
                    best = e.distanceToSqr(current);
                    next = e;
                }
            }
            current = next;
        }
        level.playSound(null, player.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.4F, 2.0F);
        return true;
    }

    private static boolean gust(ServerLevel level, ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z).normalize();
        for (LivingEntity target : around(level, player, player.position(), 8.0)) {
            Vec3 to = target.position().subtract(player.position());
            if (to.length() < 0.01 || to.normalize().dot(look) < 0.55) {
                continue;
            }
            target.push(flat.x * 1.8, 0.45, flat.z * 1.8);
            target.hurtMarked = true;
        }
        SpellFx.gust(level, player.getEyePosition().subtract(0, 0.3, 0), look, RuneElement.AIR);
        level.playSound(null, player.blockPosition(), SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), SoundSource.PLAYERS, 1.0F, 1.2F);
        return true;
    }

    private static boolean discharge(ServerLevel level, ServerPlayer player) {
        Aim aim = aim(level, player, 24);
        Vec3 start = player.getEyePosition().subtract(0, 0.25, 0);
        Vec3 end = aim != null ? aim.point() : player.getEyePosition().add(player.getLookAngle().scale(24));
        SpellFx.lightning(level, start, end, RuneElement.ENERGY);
        level.playSound(null, player.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.6F, 1.8F);
        if (aim == null || aim.entity() == null) {
            SpellFx.burst(level, end, RuneElement.ENERGY, 0.3, 24, ParticleTypes.ELECTRIC_SPARK);
            return true;
        }
        LivingEntity first = aim.entity();
        first.hurtServer(level, level.damageSources().indirectMagic(player, player), (float) (6.0 * pw(player, RuneElement.ENERGY)));
        SpellFx.burst(level, end, RuneElement.ENERGY, 0.3, 24, ParticleTypes.ELECTRIC_SPARK);
        List<LivingEntity> hit = new ArrayList<>(List.of(first));
        LivingEntity from = first;
        for (int i = 0; i < 3; i++) {
            LivingEntity current = from;
            LivingEntity next = null;
            double best = 25.0;
            for (LivingEntity candidate : around(level, player, current.position(), 5.0)) {
                double d = candidate.distanceToSqr(current);
                if (!hit.contains(candidate) && d < best) {
                    best = d;
                    next = candidate;
                }
            }
            if (next == null) {
                break;
            }
            Vec3 a = current.position().add(0, current.getBbHeight() * 0.5, 0);
            Vec3 b = next.position().add(0, next.getBbHeight() * 0.5, 0);
            SpellFx.lightning(level, a, b, RuneElement.ENERGY);
            SpellFx.burst(level, b, RuneElement.ENERGY, 0.2, 14, ParticleTypes.ELECTRIC_SPARK);
            next.hurtServer(level, level.damageSources().indirectMagic(player, player), (float) (4.0 * pw(player, RuneElement.ENERGY)));
            hit.add(next);
            from = next;
        }
        return true;
    }

    private static boolean mend(ServerLevel level, ServerPlayer player) {
        double w = pw(player, RuneElement.WATER);
        player.heal((float) (6.0 * w));
        player.clearFire();
        for (Player other : level.getEntitiesOfClass(Player.class, player.getBoundingBox().inflate(6.0), e -> e != player && e.isAlive() && e.isAlliedTo(player))) {
            other.heal((float) (4.0 * w));
        }
        SpellFx.helix(level, player, RuneElement.WATER, 14, 2.0, ParticleTypes.BUBBLE_POP);
        SpellFx.expandingRing(level, player.position(), RuneElement.WATER, 3.0, 6, ParticleTypes.FALLING_WATER);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1.0, player.getZ(), 30, 0.5, 0.8, 0.5, 0.0);
        level.sendParticles(ParticleTypes.FALLING_WATER, player.getX(), player.getY() + 2.4, player.getZ(), 30, 0.6, 0.1, 0.6, 0.0);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 1.4F);
        return true;
    }

    private static boolean holyLight(ServerLevel level, ServerPlayer player) {
        double w = pw(player, RuneElement.LIGHT);
        player.heal((float) (3.0 * w));
        for (Player other : level.getEntitiesOfClass(Player.class, player.getBoundingBox().inflate(8.0), e -> e != player && e.isAlive() && e.isAlliedTo(player))) {
            other.heal((float) (5.0 * w));
        }
        for (LivingEntity target : around(level, player, player.position(), 8.0)) {
            if (target.isInvertedHealAndHarm()) {
                target.hurtServer(level, level.damageSources().indirectMagic(player, player), (float) (10.0 * w));
            }
        }
        SpellFx.column(level, player.position(), RuneElement.LIGHT, 5.0, 8, ParticleTypes.END_ROD);
        SpellFx.expandingRing(level, player.position(), RuneElement.LIGHT, 8.0, 8, ParticleTypes.END_ROD);
        SpellFx.burst(level, player.position().add(0, 1.0, 0), RuneElement.LIGHT, 0.45, 60, ParticleTypes.END_ROD);
        level.sendParticles(ParticleTypes.GLOW, player.getX(), player.getY() + 1.0, player.getZ(), 40, 4.0, 1.0, 4.0, 0.0);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2F, 1.0F);
        return true;
    }

    /** Teleporta até 16 blocos na direção do olhar; recua até achar um lugar livre (nunca exige alvo). */
    private static boolean blink(ServerLevel level, ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(look.scale(16.0)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        double max = hit.getType() == HitResult.Type.MISS ? 16.0 : Math.max(0.0, hit.getLocation().distanceTo(eye) - 0.5);
        Vec3 from = player.position();
        Vec3 dest = null;
        for (double d = max; d >= 1.5 && dest == null; d -= 0.5) {
            Vec3 feet = eye.add(look.scale(d)).subtract(0, player.getEyeHeight(), 0);
            for (double up : new double[] {0.0, 1.0, -1.0}) {
                Vec3 candidate = feet.add(0, up, 0);
                if (level.noCollision(player, player.getBoundingBox().move(candidate.subtract(from)))) {
                    dest = candidate;
                    break;
                }
            }
        }
        if (dest == null || dest.distanceTo(from) < 1.5) {
            return false;
        }
        player.teleportTo(dest.x, dest.y, dest.z);
        player.resetFallDistance();
        SpellFx.line(level, from.add(0, 1, 0), player.position().add(0, 1, 0), SpellFx.dust(RuneElement.SPACE, 1.1F), 0.4);
        SpellFx.line(level, from.add(0, 1.2, 0), player.position().add(0, 1.2, 0), ParticleTypes.PORTAL, 0.8);
        SpellFx.sigil(level, from, RuneElement.SPACE, 1.2);
        SpellFx.burst(level, from.add(0, 1, 0), RuneElement.SPACE, 0.4, 40, ParticleTypes.PORTAL);
        SpellFx.burst(level, player.position().add(0, 1, 0), RuneElement.SPACE, 0.35, 40, ParticleTypes.REVERSE_PORTAL);
        SpellFx.expandingRing(level, player.position(), RuneElement.SPACE, 2.5, 5, ParticleTypes.PORTAL);
        level.playSound(null, BlockPos.containing(from), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.2F);
        level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.2F);
        return true;
    }

    private static boolean fireBurst(ServerLevel level, ServerPlayer player) {
        Aim aim = aim(level, player, 24);
        Vec3 c = aim != null ? aim.point() : player.getEyePosition().add(player.getLookAngle().scale(24));
        Vec3 start = player.getEyePosition().add(player.getLookAngle().scale(0.8)).subtract(0, 0.25, 0);
        double radius = rad(player, RuneElement.FIRE, 4.0);
        float damage = (float) (9.0 * pw(player, RuneElement.FIRE));
        level.playSound(null, player.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 0.8F);
        SpellFx.projectile(level, start, c, RuneElement.FIRE, ParticleTypes.FLAME, () -> {
            for (LivingEntity target : around(level, player, c, radius)) {
                target.hurtServer(level, level.damageSources().indirectMagic(player, player), damage);
                target.igniteForSeconds(6.0F);
            }
            SpellFx.expandingRing(level, c, RuneElement.FIRE, 4.0, 6, ParticleTypes.FLAME);
            SpellFx.burst(level, c, RuneElement.FIRE, 0.55, 90, ParticleTypes.FLAME);
            SpellFx.burst(level, c, RuneElement.FIRE, 0.3, 30, ParticleTypes.LAVA);
            SpellFx.burst(level, c, RuneElement.FIRE, 0.2, 20, ParticleTypes.LARGE_SMOKE);
            level.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y + 0.3, c.z, 3, 0.8, 0.3, 0.8, 0.0);
            level.playSound(null, BlockPos.containing(c), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.0F, 1.1F);
        });
        return true;
    }

    private static boolean windStep(ServerLevel level, ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        player.setDeltaMovement(look.x * 1.8, Math.max(0.25, look.y * 1.8 + 0.2), look.z * 1.8);
        player.hurtMarked = true;
        player.resetFallDistance();
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 80, 0));
        SpellFx.burst(level, player.position().add(0, 0.3, 0), RuneElement.AIR, 0.35, 30, ParticleTypes.CLOUD);
        for (int t = 1; t <= 10; t++) {
            SpellFx.later(level, t, () -> {
                if (player.isAlive()) {
                    Vec3 pos = player.position().add(0, 0.8, 0);
                    level.sendParticles(ParticleTypes.CLOUD, pos.x, pos.y, pos.z, 4, 0.3, 0.3, 0.3, 0.02);
                    SpellFx.ring(level, pos, 0.7, SpellFx.dust(RuneElement.AIR, 1.0F), 10);
                }
            });
        }
        level.playSound(null, player.blockPosition(), SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), SoundSource.PLAYERS, 1.0F, 1.4F);
        return true;
    }

    private static boolean sprout(ServerLevel level, ServerPlayer player) {
        double w = pw(player, RuneElement.LIFE);
        player.heal((float) (4.0 * w));
        for (Player other : level.getEntitiesOfClass(Player.class, player.getBoundingBox().inflate(rad(player, RuneElement.LIFE, 5.0)),
                e -> e != player && e.isAlive() && e.isAlliedTo(player))) {
            other.heal((float) (4.0 * w));
        }
        // faz as plantações em volta crescerem
        int radius = (int) Math.round(rad(player, RuneElement.LIFE, 4.0));
        BlockPos center = player.blockPosition();
        net.minecraft.world.item.ItemStack meal = new net.minecraft.world.item.ItemStack(Items.BONE_MEAL);
        for (int i = 0; i < 12; i++) {
            BlockPos pos = center.offset(level.getRandom().nextInt(radius * 2 + 1) - radius, level.getRandom().nextInt(3) - 1,
                    level.getRandom().nextInt(radius * 2 + 1) - radius);
            if (net.minecraft.world.item.BoneMealItem.growCrop(meal, level, pos)) {
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.0);
            }
        }
        SpellFx.helix(level, player, RuneElement.LIFE, 14, 2.0, ParticleTypes.HAPPY_VILLAGER);
        SpellFx.expandingRing(level, player.position(), RuneElement.LIFE, rad(player, RuneElement.LIFE, 5.0), 7, ParticleTypes.COMPOSTER);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OAK_LEAVES.defaultBlockState()), player.getX(), player.getY() + 1.0, player.getZ(), 30, 0.6, 0.8, 0.6, 0.1);
        level.playSound(null, player.blockPosition(), SoundEvents.BONE_MEAL_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }

    /** Raízes: prendem as criaturas hostis no ponto mirado (ou ao redor de você) e as machucam. */
    private static boolean thorns(ServerLevel level, ServerPlayer player) {
        Aim aim = aim(level, player, 16);
        Vec3 c = aim != null ? aim.point() : player.position();
        double radius = rad(player, RuneElement.LIFE, 4.0);
        double w = pw(player, RuneElement.LIFE);
        for (LivingEntity target : around(level, player, c, radius)) {
            target.hurtServer(level, level.damageSources().indirectMagic(player, player), (float) (2.0 * w));
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, dur(player, RuneElement.LIFE, 80), 6));
            target.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, dur(player, RuneElement.LIFE, 80), 128));
            Vec3 at = target.position();
            for (int t = 0; t < 6; t++) {
                int step = t;
                SpellFx.later(level, t, () -> {
                    for (int k = 0; k < 6; k++) {
                        double a = Math.PI * 2 * k / 6 + step * 0.5;
                        level.sendParticles(SpellFx.dust(RuneElement.LIFE, 1.2F), at.x + Math.cos(a) * 0.5, at.y + step * 0.35, at.z + Math.sin(a) * 0.5, 1, 0, 0, 0, 0);
                    }
                });
            }
        }
        SpellFx.expandingRing(level, c, RuneElement.LIFE, radius, 6, ParticleTypes.COMPOSTER);
        SpellFx.sigil(level, c, RuneElement.LIFE, Math.min(radius, 3.0));
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OAK_LOG.defaultBlockState()), c.x, c.y + 0.2, c.z, 50, radius * 0.5, 0.2, radius * 0.5, 0.15);
        level.playSound(null, BlockPos.containing(c), SoundEvents.GRASS_BREAK, SoundSource.PLAYERS, 1.2F, 0.6F);
        return true;
    }

    private static boolean meteor(ServerLevel level, ServerPlayer player) {
        Aim aim = aim(level, player, 40);
        Vec3 c = aim != null ? aim.point() : player.getEyePosition().add(player.getLookAngle().scale(30));
        double charge = CHARGE.get();
        int count = 1 + (charge >= 0.5 ? 1 : 0) + (charge >= 0.99 ? 1 : 0);
        double power = pw(player, RuneElement.FIRE);
        double radius = rad(player, RuneElement.FIRE, 5.0);
        float damage = (float) (14.0 * power / (1.0 + charge));
        for (int i = 0; i < count; i++) {
            Vec3 target = i == 0 ? c : c.add((level.getRandom().nextDouble() - 0.5) * 8.0, 0, (level.getRandom().nextDouble() - 0.5) * 8.0);
            int delay = 22 + i * 7;
            SpellFx.sigil(level, target, RuneElement.FIRE, 2.2);
            SpellFx.expandingRing(level, target, RuneElement.FIRE, radius, 6, ParticleTypes.SMALL_FLAME);
            // o meteoro de verdade: bloco de magma em brasa caindo e girando
            float rock = (float) (1.4 + charge * 1.0);
            SpellFx.later(level, delay - 11, () -> {
                Vec3 top = target.add(-5.5, 22, 3.3);
                var body = com.mistech.mistechmod.spellcraft.fx.Vfx.block(level, top, Blocks.MAGMA_BLOCK.defaultBlockState(),
                        com.mistech.mistechmod.spellcraft.fx.Vfx.centered(rock, rock, rock, new org.joml.Quaternionf()), 12, true);
                for (int k = 1; k <= 10; k++) {
                    double f = k / 10.0;
                    Vec3 p2 = top.add(target.subtract(top).scale(f));
                    SpellFx.later(level, k, () -> com.mistech.mistechmod.spellcraft.fx.Vfx.move(body, p2));
                }
                com.mistech.mistechmod.spellcraft.fx.Vfx.animate(level, body, 1, 10, com.mistech.mistechmod.spellcraft.fx.Vfx.centered(rock, rock, rock,
                        new org.joml.Quaternionf().rotateX(2.5F).rotateZ(1.2F)));
            });
            for (int t = 0; t < 10; t++) {
                int step = t;
                SpellFx.later(level, delay - 10 + t, () -> {
                    double y = 22.0 * (1.0 - (step + 1) / 10.0);
                    Vec3 pos = target.add(-y * 0.25, y, y * 0.15);
                    level.sendParticles(SpellFx.dust(RuneElement.FIRE, 2.4F), pos.x, pos.y, pos.z, 6, 0.35, 0.35, 0.35, 0);
                    level.sendParticles(ParticleTypes.FLAME, pos.x, pos.y, pos.z, 8, 0.4, 0.4, 0.4, 0.03);
                    level.sendParticles(ParticleTypes.LARGE_SMOKE, pos.x, pos.y + 0.8, pos.z, 3, 0.3, 0.3, 0.3, 0.01);
                });
            }
            SpellFx.later(level, delay, () -> {
                for (LivingEntity victim : around(level, player, target, radius)) {
                    victim.hurtServer(level, level.damageSources().indirectMagic(player, player), damage);
                    victim.igniteForSeconds(6.0F);
                    Vec3 away = victim.position().subtract(target).multiply(1, 0, 1);
                    if (away.lengthSqr() > 0.01) {
                        Vec3 push = away.normalize().scale(0.8);
                        victim.push(push.x, 0.5, push.z);
                        victim.hurtMarked = true;
                    }
                }
                SpellFx.burst(level, target.add(0, 0.5, 0), RuneElement.FIRE, 0.7, 120, ParticleTypes.FLAME);
                SpellFx.burst(level, target.add(0, 0.5, 0), RuneElement.FIRE, 0.4, 40, ParticleTypes.LAVA);
                SpellFx.burst(level, target.add(0, 0.5, 0), RuneElement.FIRE, 0.25, 30, ParticleTypes.LARGE_SMOKE);
                SpellFx.expandingRing(level, target, RuneElement.FIRE, radius, 5, ParticleTypes.FLAME);
                level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, target.x, target.y + 0.5, target.z, 1, 0, 0, 0, 0);
                com.mistech.mistechmod.spellcraft.fx.Vfx.shake(level, target, 40, 0.8F, 16);
                com.mistech.mistechmod.spellcraft.fx.Vfx.flash(level, target, 24, 0xFF7A20, 8);
                com.mistech.mistechmod.spellcraft.fx.Vfx.spikes(level, target, Blocks.MAGMA_BLOCK.defaultBlockState(), 10, radius * 0.7, 1.6, 30);
                SpellVisuals.flames(level, target, 12, radius, 40);
                level.playSound(null, BlockPos.containing(target), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.6F, 0.7F);
            });
        }
        level.playSound(null, player.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 0.5F);
        return true;
    }

    private static boolean blizzard(ServerLevel level, ServerPlayer player) {
        for (LivingEntity target : around(level, player, player.position(), rad(player, RuneElement.ICE, 7.0))) {
            target.hurtServer(level, level.damageSources().indirectMagic(player, player), (float) (1.5 * pw(player, RuneElement.ICE)));
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 30, 2));
            target.setTicksFrozen(Math.max(target.getTicksFrozen(), target.getTicksRequiredToFreeze() + 40));
        }
        Vec3 c = player.position();
        SpellFx.expandingRing(level, c, RuneElement.ICE, 7.0, 6, ParticleTypes.SNOWFLAKE);
        for (int t = 0; t < 8; t++) {
            SpellFx.later(level, t, () -> {
                for (int i = 0; i < 22; i++) {
                    double a = level.getRandom().nextDouble() * Math.PI * 2;
                    double r = Math.sqrt(level.getRandom().nextDouble()) * 7.0;
                    Vec3 pos = c.add(Math.cos(a) * r, 2.5 + level.getRandom().nextDouble() * 2.0, Math.sin(a) * r);
                    level.sendParticles(ParticleTypes.SNOWFLAKE, pos.x, pos.y, pos.z, 0, 0.4, -0.5, 0.2, 0.25);
                    if (i % 3 == 0) {
                        level.sendParticles(SpellFx.dust(RuneElement.ICE, 1.0F), pos.x, pos.y, pos.z, 0, 0.3, -0.5, 0.1, 0.2);
                    }
                }
            });
        }
        level.playSound(null, player.blockPosition(), SoundEvents.POWDER_SNOW_STEP, SoundSource.PLAYERS, 1.0F, 0.6F);
        return true;
    }

    private static boolean earthquake(ServerLevel level, ServerPlayer player) {
        for (LivingEntity target : around(level, player, player.position(), rad(player, RuneElement.EARTH, 6.0))) {
            target.hurtServer(level, level.damageSources().indirectMagic(player, player), (float) (3.0 * pw(player, RuneElement.EARTH)));
            target.push(0, 0.5, 0);
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 30, 1));
            target.hurtMarked = true;
        }
        Vec3 c = player.position();
        BlockParticleOption dirt = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState());
        BlockParticleOption stone = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState());
        SpellFx.expandingRing(level, c, RuneElement.EARTH, 6.0, 8, dirt);
        // rachaduras: 8 linhas que se abrem do centro para fora
        for (int line = 0; line < 8; line++) {
            double a = Math.PI * 2 * line / 8 + 0.2;
            for (int step = 1; step <= 6; step++) {
                int st = step;
                SpellFx.later(level, step, () -> {
                    Vec3 pos = c.add(Math.cos(a) * st, 0.2, Math.sin(a) * st);
                    level.sendParticles(stone, pos.x, pos.y, pos.z, 6, 0.2, 0.1, 0.2, 0.15);
                    level.sendParticles(SpellFx.dust(RuneElement.EARTH, 1.2F), pos.x, pos.y + 0.2, pos.z, 3, 0.15, 0.2, 0.15, 0);
                });
            }
        }
        level.sendParticles(dirt, c.x, c.y + 0.3, c.z, 70, 3.0, 0.2, 3.0, 0.2);
        level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.5F, 0.5F);
        return true;
    }

    private static boolean thunderstorm(ServerLevel level, ServerPlayer player) {
        Aim aim = aim(level, player, 48);
        Vec3 center = aim != null ? aim.point() : player.getEyePosition().add(player.getLookAngle().scale(32));
        SpellFx.sigil(level, center, RuneElement.ENERGY, 4.0);
        SpellFx.expandingRing(level, center, RuneElement.ENERGY, 7.0, 8, ParticleTypes.ELECTRIC_SPARK);
        for (int t = 0; t < 14; t++) {
            SpellFx.later(level, t, () -> {
                for (int i = 0; i < 10; i++) {
                    double a = level.getRandom().nextDouble() * Math.PI * 2;
                    double r = Math.sqrt(level.getRandom().nextDouble()) * 6.0;
                    level.sendParticles(ParticleTypes.LARGE_SMOKE, center.x + Math.cos(a) * r, center.y + 12, center.z + Math.sin(a) * r, 1, 0.3, 0.2, 0.3, 0.01);
                }
            });
        }
        int bolts = Math.max(5, 2 + Affinity.effectiveLevel(player, RuneElement.ENERGY));
        for (int i = 0; i < bolts; i++) {
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
            if (bolt != null) {
                double ox = i == 0 ? 0 : (level.getRandom().nextDouble() - 0.5) * 7.0;
                double oz = i == 0 ? 0 : (level.getRandom().nextDouble() - 0.5) * 7.0;
                BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, BlockPos.containing(center.x + ox, center.y, center.z + oz));
                double y = Math.abs(ground.getY() - center.y) < 6 ? ground.getY() : center.y;
                bolt.snapTo(center.x + ox, y, center.z + oz);
                bolt.setCause(player);
                int delay = i * 2;
                Vec3 spot = new Vec3(center.x + ox, y, center.z + oz);
                SpellFx.later(level, delay, () -> {
                    level.addFreshEntity(bolt);
                    SpellFx.burst(level, spot.add(0, 0.5, 0), RuneElement.ENERGY, 0.5, 40, ParticleTypes.ELECTRIC_SPARK);
                    SpellFx.ring(level, spot.add(0, 0.2, 0), 1.6, SpellFx.dust(RuneElement.ENERGY, 1.4F), 20);
                });
            }
        }
        return true;
    }
}

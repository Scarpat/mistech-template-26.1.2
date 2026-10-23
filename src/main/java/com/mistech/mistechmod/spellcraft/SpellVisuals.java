package com.mistech.mistechmod.spellcraft;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.mistech.mistechmod.item.RuneElement;
import com.mistech.mistechmod.spellcraft.fx.Vfx;
import com.mistech.mistechmod.spellcraft.fx.VfxContent;
import com.mojang.math.Transformation;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Assinatura visual de cada magia, por cima das peças básicas do {@link SpellFx}: blocos 3D (espinhos de gelo, colunas
 * de terra, fogo, árvores), círculos e relógios girando, raios de luz, tremor de câmera e clarão de tela. Chamado pelo
 * {@link SpellCaster} ao lançar e a cada pulso das contínuas, e pelos projéteis ao acertar.
 */
public final class SpellVisuals {
    private SpellVisuals() {}

    private static Vec3 aimPoint(ServerLevel level, ServerPlayer player, double range) {
        Spells.Aim aim = Spells.aim(level, player, range);
        return aim != null ? aim.point() : player.getEyePosition().add(player.getLookAngle().scale(range * 0.6));
    }

    // ------------------------------------------------------------------ pecinhas

    /** Chamas 3D (bloco de fogo animado do jogo) em volta de um ponto. */
    static void flames(ServerLevel level, Vec3 center, int count, double radius, int life) {
        for (int i = 0; i < count; i++) {
            double a = Math.PI * 2 * i / count + level.getRandom().nextDouble() * 0.3;
            double r = radius * (0.5 + level.getRandom().nextDouble() * 0.5);
            Vec3 p = Vfx.ground(level, center.add(Math.cos(a) * r, 0, Math.sin(a) * r));
            float h = 0.8F + level.getRandom().nextFloat() * 0.8F;
            Display d = Vfx.block(level, p, Blocks.FIRE.defaultBlockState(), Vfx.standing(0.9F, 0.01F, 0.9F, new Quaternionf()), life + 4, true);
            Vfx.animate(level, d, 1, 3, Vfx.standing(0.9F, h, 0.9F, new Quaternionf()));
            Vfx.animate(level, d, life, 4, Vfx.standing(0.5F, 0.01F, 0.5F, new Quaternionf()));
        }
    }

    /** Blocos do elemento que aparecem, giram para dentro e somem (armaduras, escudos). */
    static void orbitIn(ServerLevel level, ServerPlayer player, BlockState state, int count) {
        Vec3 c = player.position().add(0, 1.0, 0);
        for (int i = 0; i < count; i++) {
            double a = Math.PI * 2 * i / count;
            Vec3 from = c.add(Math.cos(a) * 2.2, (i % 2) * 0.6 - 0.3, Math.sin(a) * 2.2);
            Display d = Vfx.block(level, from, state, Vfx.centered(0.45F, 0.45F, 0.45F, Vfx.rotY((float) Math.toDegrees(a))), 14, false);
            for (int t = 1; t <= 8; t++) {
                double f = t / 8.0;
                Vec3 pos = from.add(c.subtract(from).scale(f * 0.75));
                SpellFx.later(level, t, () -> Vfx.move(d, pos));
            }
            Vfx.animate(level, d, 9, 4, Vfx.centered(0.01F, 0.01F, 0.01F, new Quaternionf()));
        }
    }

    /** Folhas/flores que brotam do chão em volta. */
    static void blossoms(ServerLevel level, Vec3 center, int count, double radius, int life) {
        for (int i = 0; i < count; i++) {
            double a = level.getRandom().nextDouble() * Math.PI * 2;
            double r = radius * Math.sqrt(level.getRandom().nextDouble());
            Vec3 p = Vfx.ground(level, center.add(Math.cos(a) * r, 0, Math.sin(a) * r));
            BlockState s = switch (i % 4) {
                case 0 -> Blocks.FLOWERING_AZALEA_LEAVES.defaultBlockState();
                case 1 -> Blocks.AZALEA_LEAVES.defaultBlockState();
                case 2 -> Blocks.PINK_PETALS.defaultBlockState();
                default -> Blocks.MOSS_BLOCK.defaultBlockState();
            };
            float w = 0.4F + level.getRandom().nextFloat() * 0.3F;
            Display d = Vfx.block(level, p, s, Vfx.standing(0.01F, 0.01F, 0.01F, Vfx.rotY(level.getRandom().nextInt(360))), life + 6, false);
            Vfx.animate(level, d, 1 + level.getRandom().nextInt(4), 4, Vfx.standing(w, w, w, Vfx.rotY(level.getRandom().nextInt(360))));
            Vfx.animate(level, d, life, 5, Vfx.standing(0.01F, 0.01F, 0.01F, new Quaternionf()));
        }
    }

    /** Esfera escura (buraco negro, eclipse): bloco preto centrado que cresce, pulsa e some. */
    static Display darkSphere(ServerLevel level, Vec3 center, float size, int life) {
        Display d = Vfx.block(level, center, Blocks.BLACK_CONCRETE.defaultBlockState(), Vfx.centered(0.01F, 0.01F, 0.01F, new Quaternionf()), life + 2, false);
        Vfx.animate(level, d, 1, 6, Vfx.centered(size, size, size, Vfx.rotY(45).rotateX(0.6F)));
        for (int t = 10; t < life - 6; t += 10) {
            float s = size * (t % 20 == 0 ? 1.12F : 0.92F);
            Vfx.animate(level, d, t, 10, Vfx.centered(s, s, s, Vfx.rotY(45 + t * 6).rotateX(0.6F)));
        }
        Vfx.animate(level, d, life - 5, 5, Vfx.centered(0.01F, 0.01F, 0.01F, new Quaternionf()));
        return d;
    }

    /** Anel deitado grande que gira devagar (disco de acreção, nuvem de tempestade). */
    static void spinningDisc(ServerLevel level, Vec3 center, RuneElement element, float size, int life, float tilt) {
        Display d = Vfx.item(level, center, Vfx.tinted(VfxContent.VFX_RING.get(), element.color()), Vfx.flat(0.01F, 0, 0), life + 2, false);
        Quaternionf flat = Vfx.rotX(-90 + tilt);
        Vfx.animate(level, d, 1, 6, new Transformation(new Vector3f(), flat, new Vector3f(size, size, size), Vfx.rotZ(60)));
        for (int t = 7, k = 1; t < life - 5; t += 10, k++) {
            Vfx.animate(level, d, t, 10, new Transformation(new Vector3f(), flat, new Vector3f(size, size, size), Vfx.rotZ(60 + 120 * k)));
        }
        Vfx.animate(level, d, life - 5, 5, new Transformation(new Vector3f(), flat, new Vector3f(0.01F, 0.01F, 0.01F), new Quaternionf()));
    }

    /** Tornado: anéis de vento empilhados, girando. */
    static void tornado(ServerLevel level, Vec3 base, RuneElement element, int rings, int life) {
        for (int i = 0; i < rings; i++) {
            float size = 1.2F + i * 0.9F;
            spinningDisc(level, base.add(0, 0.3 + i * 0.9, 0), element, size, life, (i % 2 == 0 ? 6 : -6));
        }
    }

    /** Árvore que cresce do chão (tronco e copa de azaleia florida) e some depois. */
    static void growingTree(ServerLevel level, Vec3 base, int life) {
        Vec3 g = Vfx.ground(level, base);
        Display trunk = Vfx.block(level, g, Blocks.OAK_LOG.defaultBlockState(), Vfx.standing(0.7F, 0.01F, 0.7F, new Quaternionf()), life + 8, false);
        Vfx.animate(level, trunk, 1, 20, Vfx.standing(0.7F, 4.5F, 0.7F, new Quaternionf()));
        Display crown = Vfx.block(level, g.add(0, 4.6, 0), Blocks.FLOWERING_AZALEA_LEAVES.defaultBlockState(), Vfx.centered(0.01F, 0.01F, 0.01F, new Quaternionf()), life + 8, false);
        Vfx.animate(level, crown, 14, 14, Vfx.centered(4.2F, 2.6F, 4.2F, Vfx.rotY(45)));
        Vfx.animate(level, trunk, life, 8, Vfx.standing(0.7F, 0.01F, 0.7F, new Quaternionf()));
        Vfx.animate(level, crown, life, 8, Vfx.centered(0.01F, 0.01F, 0.01F, new Quaternionf()));
    }

    /** Parede de água que avança (Maremoto). */
    static void waterWall(ServerLevel level, ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z).normalize();
        Vec3 side = new Vec3(-flat.z, 0, flat.x);
        BlockState water = Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState();
        float yaw = (float) Math.toDegrees(Math.atan2(-flat.x, flat.z));
        for (int k = -3; k <= 3; k++) {
            Vec3 start = Vfx.ground(level, player.position().add(flat.scale(1.5)).add(side.scale(k)));
            Display d = Vfx.block(level, start, water, Vfx.standing(1.0F, 0.01F, 0.8F, Vfx.rotY(yaw)), 22, false);
            Vfx.animate(level, d, 1, 3, Vfx.standing(1.0F, 2.6F - Math.abs(k) * 0.25F, 0.8F, Vfx.rotY(yaw).rotateX(-0.25F)));
            for (int t = 2; t <= 17; t++) {
                Vec3 p = start.add(flat.scale(t));
                SpellFx.later(level, t, () -> Vfx.move(d, p));
            }
            Vfx.animate(level, d, 17, 4, Vfx.standing(1.0F, 0.01F, 0.8F, Vfx.rotY(yaw)));
        }
    }

    /** Moeda dourada girando sobre a cabeça. */
    static void coin(ServerLevel level, ServerPlayer player) {
        Vec3 p = player.position().add(0, 2.6, 0);
        Display d = Vfx.block(level, p, Blocks.GOLD_BLOCK.defaultBlockState(), Vfx.centered(0.6F, 0.6F, 0.08F, new Quaternionf()), 22, true);
        for (int t = 1; t < 18; t += 3) {
            Vfx.animate(level, d, t, 3, Vfx.centered(0.6F, 0.6F, 0.08F, Vfx.rotY(90 * ((t + 2) / 3))));
            Vec3 up = p.add(0, t * 0.04, 0);
            SpellFx.later(level, t, () -> Vfx.move(d, up));
        }
        Vfx.animate(level, d, 18, 4, Vfx.centered(0.01F, 0.01F, 0.01F, new Quaternionf()));
    }

    // ------------------------------------------------------------------ por magia

    /** Depois que a magia saiu (lançamento normal, carregada, ritual ou o primeiro pulso de uma contínua). */
    static void onCast(ServerLevel level, ServerPlayer player, Spell spell) {
        Vec3 me = player.position();
        RuneElement el = spell.element();
        switch (spell.id()) {
            // ---------- Fogo
            case "fire_burst" -> SpellFx.later(level, 6, () -> {
                Vec3 c = aimPoint(level, player, 24);
                flames(level, c, 10, 3.5, 24);
                Vfx.spikes(level, c, Blocks.MAGMA_BLOCK.defaultBlockState(), 6, 3.0, 1.2, 16);
                Vfx.shake(level, c, 18, 0.25F, 8);
            });
            case "ember_ring" -> {
                flames(level, me, 14, Spells.rad(player, el, 4.0), 20);
                Vfx.flash(level, me, 6, 0xFF8A30, 5);
            }
            case "inferno" -> {
                Vec3 c = aimPoint(level, player, 32);
                Vfx.circle(level, c, el, 6.0, 50);
                for (int k = 0; k < 3; k++) {
                    int wave = k;
                    SpellFx.later(level, k * 5, () -> {
                        flames(level, c, 16 + wave * 6, 3.0 + wave * 3.5, 40);
                        Vfx.shake(level, c, 30, 0.5F, 10);
                    });
                }
                Vfx.flash(level, c, 40, 0xFF6A1F, 14);
            }
            case "phoenix" -> {
                flames(level, me, 10, 2.5, 24);
                Vfx.flash(level, me, 16, 0xFFB040, 10);
            }
            // ---------- Gelo
            case "frost_nova" -> {
                Vfx.spikes(level, me, Blocks.PACKED_ICE.defaultBlockState(), 16, Spells.rad(player, el, 5.0), 1.6, 20);
                Vfx.flash(level, me, 8, 0xBFEFFF, 6);
                Vfx.shake(level, me, 10, 0.15F, 6);
            }
            case "ice_age" -> {
                Vfx.spikes(level, me, Blocks.BLUE_ICE.defaultBlockState(), 26, 12, 3.2, 60);
                Vfx.spikes(level, me, Blocks.PACKED_ICE.defaultBlockState(), 20, 8, 2.0, 60);
                Vfx.flash(level, me, 40, 0xCFF6FF, 18);
                Vfx.shake(level, me, 30, 0.45F, 14);
            }
            // ---------- Água
            case "water_mend", "purify" -> {
                Vfx.beam(level, me, me.add(0, 3.5, 0), Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState(), 1.3F, 16);
                Vfx.risingRing(level, me, el, 1.4, 2.8, 14);
            }
            case "tidal_wave" -> {
                waterWall(level, player);
                Vfx.shake(level, me, 20, 0.3F, 12);
            }
            // ---------- Terra
            case "stone_skin" -> orbitIn(level, player, Blocks.STONE.defaultBlockState(), 8);
            case "cataclysm" -> {
                for (int k = 0; k < 4; k++) {
                    int ring = k;
                    SpellFx.later(level, k * 3, () -> {
                        double r = Spells.rad(player, el, 10.0) * (ring + 1) / 4.0;
                        for (int i = 0; i < 6 + ring * 3; i++) {
                            double a = level.getRandom().nextDouble() * Math.PI * 2;
                            Vfx.pillar(level, me.add(Math.cos(a) * r, 0, Math.sin(a) * r),
                                    ring % 2 == 0 ? Blocks.DEEPSLATE.defaultBlockState() : Blocks.COARSE_DIRT.defaultBlockState(),
                                    0.9F, 1.0F + level.getRandom().nextFloat() * 2.0F, 24);
                        }
                        Vfx.shake(level, me, 30, 0.7F, 12);
                    });
                }
                Vfx.flash(level, me, 30, 0x8A6A3A, 12);
            }
            case "stone_golem" -> Vfx.shake(level, me, 16, 0.3F, 10);
            // ---------- Ar
            case "wind_step", "updraft" -> {
                Vfx.windRing(level, me.add(0, 0.5, 0), new Vec3(0, -1, 0), el, 2.5, 8);
                tornado(level, me, el, 3, 14);
            }
            // ---------- Energia
            case "thunderstorm" -> {
                Vec3 c = aimPoint(level, player, 48);
                Vec3 sky = c.add(0, 12, 0);
                Display cloud = Vfx.block(level, sky, Blocks.GRAY_WOOL.defaultBlockState(), Vfx.centered(0.01F, 0.01F, 0.01F, new Quaternionf()), 46, false);
                Vfx.animate(level, cloud, 1, 10, Vfx.centered(12F, 1.6F, 12F, Vfx.rotY(20)));
                Vfx.animate(level, cloud, 38, 8, Vfx.centered(0.01F, 0.01F, 0.01F, Vfx.rotY(40)));
                spinningDisc(level, sky.subtract(0, 1, 0), el, 14F, 44, 0);
                SpellFx.later(level, 4, () -> {
                    Vfx.flash(level, c, 48, 0xFFFFFF, 6);
                    Vfx.shake(level, c, 48, 0.4F, 16);
                });
            }
            // ---------- Luz
            case "holy_light" -> {
                Vfx.beam(level, me.add(0, 16, 0), me, Blocks.GLOWSTONE.defaultBlockState(), 1.6F, 14);
                Vfx.flash(level, me, 10, 0xFFF6C0, 8);
            }
            case "glimmer" -> Vfx.circle(level, me, el, 3.0, 20);
            case "dawn" -> {
                Vfx.beam(level, me.add(0, 30, 0), me, Blocks.GLOWSTONE.defaultBlockState(), 3.2F, 24);
                Vfx.beam(level, me.add(0, 30, 0), me, Blocks.SEA_LANTERN.defaultBlockState(), 1.6F, 26);
                Vfx.circle(level, me, el, 7.0, 40);
                Vfx.flash(level, me, 40, 0xFFFBE0, 20);
                Vfx.shake(level, me, 20, 0.2F, 10);
            }
            // ---------- Trevas
            case "shadow_veil" -> darkSphere(level, me.add(0, 1, 0), 2.2F, 14);
            case "total_eclipse" -> {
                Vec3 sun = me.add(0, 9, 0);
                darkSphere(level, sun, 5.0F, 60);
                spinningDisc(level, sun, RuneElement.LIGHT, 7.5F, 60, 75);
                Vfx.flash(level, me, 40, 0x1A0A2A, 30);
                Vfx.shake(level, me, 30, 0.3F, 16);
            }
            // ---------- Tempo
            case "quicken", "foresight" -> Vfx.clock(level, me, el, 1.4, 20, 2.4F);
            case "time_stop" -> {
                Vfx.clock(level, me, el, 6.0, 50, 0.1F);
                Vfx.clock(level, me, el, 2.0, 50, 3.0F);
                Vfx.flash(level, me, 20, 0x5FD6B0, 16);
                Vfx.shake(level, me, 16, 0.2F, 8);
            }
            case "eternal_instant" -> {
                Vfx.clock(level, me, el, 2.5, 30, 0.1F);
                Vfx.flash(level, me, 8, 0x5FD6B0, 10);
            }
            case "rewind" -> Vfx.clock(level, me, el, 1.5, 24, 2.4F);
            // ---------- Espaço
            case "pull" -> {
                Spells.Aim aim = Spells.aim(level, player, 16);
                if (aim != null) {
                    Vfx.beam(level, player.getEyePosition().subtract(0, 0.3, 0), aim.point(), Blocks.PURPLE_STAINED_GLASS.defaultBlockState(), 0.2F, 8);
                }
            }
            case "anchor", "rift" -> Vfx.circle(level, me, el, 1.8, 30);
            case "black_hole" -> {
                Vec3 c = aimPoint(level, player, 32).add(0, 1.5, 0);
                darkSphere(level, c, 2.4F, 120);
                spinningDisc(level, c, el, 9F, 120, 20);
                spinningDisc(level, c, RuneElement.ARCANE, 6F, 120, -25);
                Vfx.flash(level, c, 30, 0x2A0A4A, 10);
                for (int t = 0; t < 120; t += 20) {
                    SpellFx.later(level, t, () -> Vfx.shake(level, c, 18, 0.25F, 18));
                }
            }
            // ---------- Vida
            case "sprout", "bloom" -> blossoms(level, me, 12, 4.0, 24);
            case "thorns" -> {
                Vec3 c = aimPoint(level, player, 16);
                Vfx.spikes(level, c, Blocks.MANGROVE_ROOTS.defaultBlockState(), 14, Spells.rad(player, el, 4.0), 1.8, 40);
            }
            case "tree_of_life" -> {
                growingTree(level, me, 400);
                Vfx.circle(level, me, el, Spells.rad(player, el, 8.0), 400);
                blossoms(level, me, 24, 7.0, 400);
            }
            case "living_forest" -> blossoms(level, me, 16, 4.0, 30);
            // ---------- Magma / Vapor / Nuvem
            case "lava_river" -> {
                Vec3 look = player.getLookAngle();
                Vec3 flat = new Vec3(look.x, 0, look.z).normalize();
                for (int i = 1; i <= 12; i++) {
                    Vec3 p = Vfx.ground(level, me.add(flat.scale(i)));
                    Display d = Vfx.block(level, p.add(0, 0.01, 0), Blocks.MAGMA_BLOCK.defaultBlockState(), Vfx.standing(0.01F, 0.08F, 0.01F, new Quaternionf()), 104, true);
                    Vfx.animate(level, d, i, 3, Vfx.standing(1.4F, 0.08F, 1.4F, Vfx.rotY(i * 17)));
                    Vfx.animate(level, d, 96, 6, Vfx.standing(0.01F, 0.08F, 0.01F, new Quaternionf()));
                }
            }
            case "eruption" -> {
                Vec3 c = aimPoint(level, player, 28);
                Vfx.spikes(level, c, Blocks.MAGMA_BLOCK.defaultBlockState(), 12, 4.0, 2.4, 30);
                Vfx.beam(level, c, c.add(0, 9, 0), Blocks.SHROOMLIGHT.defaultBlockState(), 1.4F, 14);
                flames(level, c, 10, 4.0, 30);
                Vfx.shake(level, c, 24, 0.5F, 12);
                Vfx.flash(level, c, 20, 0xE0521B, 8);
            }
            case "magma_armor" -> orbitIn(level, player, Blocks.MAGMA_BLOCK.defaultBlockState(), 10);
            case "mist" -> Vfx.flare(level, me.add(0, 1, 0), el, 6F, 30);
            case "steam_jet" -> Vfx.windRing(level, player.getEyePosition(), player.getLookAngle(), el, 8.0, 8);
            // ---------- outros
            case "lucky_coin" -> coin(level, player);
            case "vigilant_gaze" -> Vfx.flash(level, me, 4, 0xE8E2D6, 10);
            case "echo" -> Vfx.shockwave(level, me, el, 6.0, 12);
            case "arcane_focus" -> Vfx.circle(level, me.add(0, 2.4, 0), el, 1.0, 600);
            case "machine_boost" -> {
                var hit = player.pick(16.0, 1.0F, false);
                Vfx.beam(level, player.getEyePosition().subtract(0, 0.3, 0), hit.getLocation(), Blocks.MAGENTA_STAINED_GLASS.defaultBlockState(), 0.25F, 10);
                Vfx.circle(level, hit.getLocation(), el, 3.0, 40);
            }
            case "recharge" -> Vfx.risingRing(level, me, el, 1.0, 2.2, 12);
            case "soul_harvest" -> Vfx.flash(level, me, 10, 0x4FD7E6, 8);
            default -> { }
        }
    }

    /** Cada pulso das contínuas. */
    static void onPulse(ServerLevel level, ServerPlayer player, Spell spell) {
        Vec3 me = player.position();
        switch (spell.id()) {
            case "blizzard" -> {
                Vfx.spikes(level, me, Blocks.PACKED_ICE.defaultBlockState(), 3, Spells.rad(player, spell.element(), 6.0), 1.4, 14);
                spinningDisc(level, me.add(0, 5, 0), spell.element(), 10F, 12, 0);
            }
            case "earthquake" -> {
                for (int i = 0; i < 4; i++) {
                    double a = level.getRandom().nextDouble() * Math.PI * 2;
                    double r = 2 + level.getRandom().nextDouble() * Spells.rad(player, spell.element(), 5.0);
                    Vfx.pillar(level, me.add(Math.cos(a) * r, 0, Math.sin(a) * r), Blocks.COARSE_DIRT.defaultBlockState(), 0.8F, 0.6F + level.getRandom().nextFloat(), 10);
                }
                Vfx.shake(level, me, 16, 0.35F, 10);
            }
            case "time_slow" -> Vfx.clock(level, me, spell.element(), 3.5, 12, 0.1F);
            case "hurricane" -> {
                Vec3 c = aimPoint(level, player, 24);
                tornado(level, c, spell.element(), 6, 12);
            }
            default -> { }
        }
    }

    /** Projétil acertou: acabamento do elemento no ponto de impacto. */
    static void impact(ServerLevel level, Vec3 at, RuneElement element) {
        switch (element) {
            case FIRE, MAGMA -> {
                flames(level, at, 3, 0.9, 12);
                SpellFx.sparks(level, at, element, 8, 0.3);
                level.sendParticles(SpellFx.ember(element), at.x, at.y, at.z, 12, 0.3, 0.3, 0.3, 0.02);
            }
            case ICE -> {
                Vfx.spikes(level, at, Blocks.PACKED_ICE.defaultBlockState(), 4, 0.8, 1.1, 12);
                level.sendParticles(SpellFx.shard(element), at.x, at.y, at.z, 10, 0.2, 0.2, 0.2, 0.25);
            }
            case EARTH -> {
                Vfx.spikes(level, at, Blocks.COARSE_DIRT.defaultBlockState(), 3, 0.8, 0.8, 10);
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()), at.x, at.y, at.z, 30, 0.3, 0.3, 0.3, 0.2);
                Vfx.shake(level, at, 6, 0.15F, 5);
            }
            case WATER -> level.sendParticles(ParticleTypes.SPLASH, at.x, at.y + 0.3, at.z, 30, 0.5, 0.3, 0.5, 0.2);
            case DARKNESS, VOID -> Vfx.implode(level, at, element, 1.6, 8);
            default -> level.sendParticles(SpellFx.shard(element), at.x, at.y, at.z, 8, 0.2, 0.2, 0.2, 0.2);
        }
        level.playSound(null, net.minecraft.core.BlockPos.containing(at), SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS, 0.5F, 0.8F);
    }
}

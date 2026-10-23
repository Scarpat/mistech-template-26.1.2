package com.mistech.mistechmod.spellcraft;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.item.RuneElement;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Ressonância: cada magia lançada deixa uma marca do elemento por 5 s; se a próxima for de um elemento que combina,
 * ela ganha +25% de poder e um efeito extra. O dado é "elemento|tick_de_fim" (sincroniza com o dono para o HUD).
 */
public final class Resonance {
    public static final int DURATION = 100;
    /** Duração com a bênção do Círculo de Pedras Rúnico. */
    public static final int DURATION_BLESSED = 160;
    public static final double BONUS = 1.25;

    /** Uma combinação: marca anterior → elemento novo. */
    public record Pair(String key, RuneElement from, RuneElement to) {}

    public static final List<Pair> PAIRS = List.of(
            new Pair("steam", RuneElement.FIRE, RuneElement.WATER),
            new Pair("shatter", RuneElement.ICE, RuneElement.ENERGY),
            new Pair("deepfreeze", RuneElement.WATER, RuneElement.ICE),
            new Pair("bloom", RuneElement.EARTH, RuneElement.LIFE),
            new Pair("chain", RuneElement.AIR, RuneElement.ENERGY),
            new Pair("eclipse", RuneElement.LIGHT, RuneElement.DARKNESS),
            new Pair("fold", RuneElement.TIME, RuneElement.SPACE),
            new Pair("magma", RuneElement.FIRE, RuneElement.EARTH));

    private Resonance() {}

    /** Elemento da marca ativa (ou null). */
    public static @Nullable RuneElement mark(Player player, long now) {
        String raw = player.getData(SpellcraftContent.RESONANCE);
        int bar = raw.indexOf('|');
        if (bar <= 0) {
            return null;
        }
        try {
            if (Long.parseLong(raw.substring(bar + 1)) <= now) {
                return null;
            }
        } catch (NumberFormatException e) {
            return null;
        }
        String id = raw.substring(0, bar);
        for (RuneElement element : RuneElement.values()) {
            if (element.id().equals(id)) {
                return element;
            }
        }
        return null;
    }

    public static long markEnd(Player player) {
        String raw = player.getData(SpellcraftContent.RESONANCE);
        int bar = raw.indexOf('|');
        try {
            return bar > 0 ? Long.parseLong(raw.substring(bar + 1)) : 0L;
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    /** Bônus da combinação: +25%, ou +40% com um Tomo do elemento em que ela termina. */
    public static double bonus(Player player, Pair pair) {
        net.minecraft.world.item.ItemStack held = SpellFocusItem.held(player);
        if (held.getItem() instanceof SpellFocusItem item && item.focus().element().equals(pair.to().id())) {
            return 1.4;
        }
        return BONUS;
    }

    public static @Nullable Pair match(@Nullable RuneElement from, RuneElement to) {
        if (from == null) {
            return null;
        }
        for (Pair pair : PAIRS) {
            if (pair.from() == from && pair.to() == to) {
                return pair;
            }
        }
        return null;
    }

    public static void setMark(ServerPlayer player, RuneElement element) {
        player.setData(SpellcraftContent.RESONANCE, element.id() + "|" + (player.level().getGameTime() + (player.getData(SpellcraftContent.CIRCLE_BLESSED.get()) ? DURATION_BLESSED : DURATION)));
    }

    public static void clear(ServerPlayer player) {
        player.setData(SpellcraftContent.RESONANCE, "");
    }

    /** Anuncia a combinação (som, partículas e mensagem). */
    public static void announce(ServerLevel level, ServerPlayer player, Pair pair) {
        player.sendOverlayMessage(Component.translatable("message.mistech.resonance", Component.translatable("resonance.mistech." + pair.key()))
                .withColor(pair.to().color()));
        SpellFx.risingRing(level, player.position(), pair.to(), 1.2, 8, 2.5);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 1.7F);
    }

    /** Efeito extra da combinação, no ponto mirado (ou em você). Roda depois da magia. */
    public static void extra(ServerLevel level, ServerPlayer player, Pair pair) {
        Spells.Aim aim = Spells.aim(level, player, 24);
        Vec3 center = aim != null ? aim.point() : player.position();
        double power = Spells.pw(player, pair.to());
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(4.0, 2.0, 4.0),
                e -> e != player && e.isAlive() && !(e instanceof Player) && !e.isAlliedTo(player));
        switch (pair.key()) {
            case "steam" -> {
                for (LivingEntity t : targets) {
                    t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0));
                }
                level.sendParticles(ParticleTypes.CLOUD, center.x, center.y + 0.8, center.z, 60, 1.6, 0.8, 1.6, 0.02);
            }
            case "shatter" -> {
                for (LivingEntity t : targets) {
                    if (t.getTicksFrozen() > 0 || t.hasEffect(MobEffects.SLOWNESS)) {
                        t.hurtServer(level, level.damageSources().indirectMagic(player, player), (float) (6.0 * power));
                        t.setTicksFrozen(0);
                    }
                }
                level.sendParticles(ParticleTypes.SNOWFLAKE, center.x, center.y + 0.8, center.z, 50, 1.2, 0.8, 1.2, 0.15);
            }
            case "deepfreeze" -> {
                for (LivingEntity t : targets) {
                    t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 6));
                    t.setTicksFrozen(t.getTicksRequiredToFreeze() + 80);
                }
                level.sendParticles(ParticleTypes.SNOWFLAKE, center.x, center.y + 0.8, center.z, 60, 1.4, 0.8, 1.4, 0.05);
            }
            case "bloom" -> {
                player.heal((float) (3.0 * power));
                net.minecraft.world.item.ItemStack meal = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BONE_MEAL);
                BlockPos c = player.blockPosition();
                for (int i = 0; i < 8; i++) {
                    BlockPos pos = c.offset(level.getRandom().nextInt(7) - 3, level.getRandom().nextInt(3) - 1, level.getRandom().nextInt(7) - 3);
                    net.minecraft.world.item.BoneMealItem.growCrop(meal, level, pos);
                }
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1.0, player.getZ(), 30, 1.0, 0.8, 1.0, 0.0);
            }
            case "chain" -> {
                for (LivingEntity t : targets) {
                    t.hurtServer(level, level.damageSources().indirectMagic(player, player), (float) (3.0 * power));
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK, t.getX(), t.getY() + t.getBbHeight() * 0.5, t.getZ(), 14, 0.3, 0.4, 0.3, 0.2);
                }
            }
            case "eclipse" -> {
                for (LivingEntity t : targets) {
                    t.hurtServer(level, level.damageSources().indirectMagic(player, player), (float) (5.0 * power));
                    t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
                }
                level.sendParticles(ParticleTypes.SQUID_INK, center.x, center.y + 1.0, center.z, 40, 1.2, 0.8, 1.2, 0.05);
                level.sendParticles(ParticleTypes.END_ROD, center.x, center.y + 1.0, center.z, 30, 1.2, 0.8, 1.2, 0.05);
            }
            case "fold" -> level.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1.0, player.getZ(), 40, 0.5, 0.9, 0.5, 0.1);
            case "magma" -> {
                for (LivingEntity t : targets) {
                    t.igniteForSeconds(6.0F);
                }
                for (int i = 0; i < 6; i++) {
                    BlockPos pos = BlockPos.containing(center).offset(level.getRandom().nextInt(5) - 2, 0, level.getRandom().nextInt(5) - 2);
                    if (level.getBlockState(pos).isAir() && level.getBlockState(pos.below()).isSolidRender()) {
                        level.setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.FIRE.defaultBlockState());
                    }
                }
                level.sendParticles(ParticleTypes.LAVA, center.x, center.y + 0.5, center.z, 20, 1.2, 0.4, 1.2, 0.0);
            }
            default -> { }
        }
    }
}

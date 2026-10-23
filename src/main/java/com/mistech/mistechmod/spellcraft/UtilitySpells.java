package com.mistech.mistechmod.spellcraft;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.mistech.mistechmod.item.RuneElement;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Utilidades e magias dos elementos avançados (Arcana, Alma, Vigília, Eco, Sorte, Vazio). Registradas em {@link Spells}. */
final class UtilitySpells {
    private UtilitySpells() {}

    /** Jogadores com a Foco Arcano ativa: a próxima magia sai 50% mais forte. */
    static final Set<UUID> ARCANE_FOCUS = new HashSet<>();
    /** Última magia lançada por jogador (para o Eco). */
    static final Map<UUID, String> LAST_CAST = new HashMap<>();

    // ------------------------------------------------------------------ Arcana (Energia)

    /** Recarregar: transforma a magia em energia (FE) nos itens do inventário. */
    static boolean recharge(ServerLevel level, ServerPlayer player) {
        int amount = (int) Math.round(6000 * Spells.pw(player, RuneElement.ARCANE));
        int moved = com.mistech.mistechmod.item.mithril.MithrilGear.chargeInventory(player, amount);
        if (moved <= 0) {
            player.sendOverlayMessage(Component.translatable("item.mistech.arcane_rune.nothing").withStyle(net.minecraft.ChatFormatting.GRAY));
            return false;
        }
        player.sendOverlayMessage(Component.translatable("item.mistech.arcane_rune.charged", moved).withColor(RuneElement.ARCANE.color()));
        SpellFx.helix(level, player, RuneElement.ARCANE, 12, 2.0, ParticleTypes.ELECTRIC_SPARK);
        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.8F, 1.6F);
        return true;
    }

    /** Impulso Arcano: as máquinas num raio de 3 do bloco mirado trabalham 50% mais rápido por 60 s. */
    static boolean machineBoost(ServerLevel level, ServerPlayer player) {
        HitResult hit = player.pick(16.0, 1.0F, false);
        if (!(hit instanceof BlockHitResult block) || hit.getType() != HitResult.Type.BLOCK) {
            return false;
        }
        BlockPos pos = block.getBlockPos().immutable();
        com.mistech.mistechmod.machine.GnomeBoost.setActive(level, pos, true);
        int ticks = (int) Math.round(1200 * Spells.pw(player, RuneElement.ARCANE));
        SpellFx.later(level, ticks, () -> com.mistech.mistechmod.machine.GnomeBoost.setActive(level, pos, false));
        for (int t = 0; t < ticks; t += 20) {
            SpellFx.later(level, t, () -> level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 4, 0.4, 0.2, 0.4, 0.05));
        }
        SpellFx.ring(level, Vec3.atCenterOf(pos), 3.0, SpellFx.dust(RuneElement.ARCANE, 1.4F), 40);
        level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8F, 1.8F);
        return true;
    }

    /** Foco Arcano: a próxima magia (em até 30 s) sai 50% mais forte. */
    static boolean arcaneFocus(ServerLevel level, ServerPlayer player) {
        ARCANE_FOCUS.add(player.getUUID());
        UUID id = player.getUUID();
        SpellFx.later(level, 600, () -> ARCANE_FOCUS.remove(id));
        SpellFx.contractingRing(level, player.position().add(0, 1.0, 0), RuneElement.ARCANE, 2.5, 8, ParticleTypes.ENCHANT);
        player.sendOverlayMessage(Component.translatable("message.mistech.spell.arcane_focus").withColor(RuneElement.ARCANE.color()));
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 0.6F);
        return true;
    }

    // ------------------------------------------------------------------ Espaço / Luz

    /** Fenda: abre o seu baú do Fim onde você estiver. */
    static boolean rift(ServerLevel level, ServerPlayer player) {
        var ender = player.getEnderChestInventory();
        ender.setActiveChest(null);
        player.openMenu(new SimpleMenuProvider((id, inv, p) -> ChestMenu.threeRows(id, inv, ender), Component.translatable("spell.mistech.rift")));
        SpellFx.contractingRing(level, player.position().add(0, 1.0, 0), RuneElement.SPACE, 1.5, 8, ParticleTypes.PORTAL);
        level.playSound(null, player.blockPosition(), SoundEvents.ENDER_CHEST_OPEN, SoundSource.PLAYERS, 1.0F, 0.8F);
        return true;
    }

    /** Orbe de Luz: deixa uma luz mágica (quebra com um toque) no lugar mirado. */
    static boolean lightOrb(ServerLevel level, ServerPlayer player) {
        HitResult hit = player.pick(24.0, 1.0F, false);
        BlockPos pos;
        if (hit instanceof BlockHitResult block && hit.getType() == HitResult.Type.BLOCK) {
            pos = block.getBlockPos().relative(block.getDirection());
        } else {
            pos = BlockPos.containing(player.getEyePosition().add(player.getLookAngle().scale(6)));
        }
        if (!level.getBlockState(pos).canBeReplaced()) {
            return false;
        }
        level.setBlockAndUpdate(pos, SpellcraftContent.LIGHT_ORB.get().defaultBlockState());
        SpellFx.projectile(level, player.getEyePosition(), Vec3.atCenterOf(pos), RuneElement.LIGHT, ParticleTypes.END_ROD, () -> {});
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.8F);
        return true;
    }

    // ------------------------------------------------------------------ Vigília, Sorte, Vazio, Alma

    /** Olhar Vigilante: revela todas as criaturas num raio de 32 por 20 s (brilho através das paredes). */
    static boolean vigilantGaze(ServerLevel level, ServerPlayer player) {
        int n = 0;
        int ticks = Spells.dur(player, RuneElement.VIGIL, 400);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(32), e -> e != player && e.isAlive())) {
            e.addEffect(new MobEffectInstance(MobEffects.GLOWING, ticks, 0));
            n++;
        }
        player.sendOverlayMessage(Component.translatable("message.mistech.spell.vigil", n).withColor(RuneElement.VIGIL.color()));
        SpellFx.expandingRing(level, player.position(), RuneElement.VIGIL, 12.0, 10, ParticleTypes.END_ROD);
        level.playSound(null, player.blockPosition(), SoundEvents.SCULK_CLICKING, SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }

    /** Moeda da Sorte: um efeito aleatório — quase sempre bom. */
    static boolean luckyCoin(ServerLevel level, ServerPlayer player) {
        int d = Spells.dur(player, RuneElement.LUCK, 1);
        int roll = level.getRandom().nextInt(10);
        String key;
        switch (roll) {
            case 0, 1 -> {
                player.addEffect(new MobEffectInstance(MobEffects.LUCK, 2400 * d, 1));
                key = "luck";
            }
            case 2, 3 -> {
                player.addEffect(new MobEffectInstance(MobEffects.SPEED, 600 * d, 1));
                player.addEffect(new MobEffectInstance(MobEffects.HASTE, 600 * d, 1));
                key = "swift";
            }
            case 4, 5 -> {
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200 * d, 1));
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 1200 * d, 1));
                key = "health";
            }
            case 6, 7 -> {
                player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 600 * d, 0));
                player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 600 * d, 0));
                key = "might";
            }
            case 8 -> {
                ManaService.set(player, ManaService.max(player));
                key = "mana";
            }
            default -> {
                player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 200, 1));
                key = "bad";
            }
        }
        player.sendOverlayMessage(Component.translatable("message.mistech.spell.coin." + key).withColor(RuneElement.LUCK.color()));
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 2.2, player.getZ(), 20, 0.4, 0.3, 0.4, 0.05);
        level.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0F, 1.2F);
        return true;
    }

    /** Colheita de Almas: arranca um pouco da alma de cada criatura num raio de 8 e transforma em vida para você. */
    static boolean soulHarvest(ServerLevel level, ServerPlayer player) {
        double w = Spells.pw(player, RuneElement.SOUL);
        int hit = Spells.areaEffects(level, player, player.position(), Spells.rad(player, RuneElement.SOUL, 8.0), t -> {
            t.hurtServer(level, level.damageSources().indirectMagic(player, player), (float) (7.0 * w));
            Vec3 from = t.position().add(0, t.getBbHeight() * 0.5, 0);
            SpellFx.projectile(level, from, player.position().add(0, 1.0, 0), RuneElement.SOUL, ParticleTypes.SOUL, () -> {});
        });
        player.heal((float) (Math.min(8, hit) * 1.0 * w));
        if (hit > 0) {
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 600, Math.min(3, hit / 2)));
        }
        SpellFx.contractingRing(level, player.position(), RuneElement.SOUL, 8.0, 10, ParticleTypes.SOUL_FIRE_FLAME);
        level.playSound(null, player.blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 1.5F, 0.7F);
        return true;
    }

    // ------------------------------------------------------------------ Eco (Ar + Trevas)

    /** Eco: repete a última magia que você lançou (até tier 4), com 60% do poder, sem gastar mana dela. */
    static boolean echo(ServerLevel level, ServerPlayer player) {
        Spell last = Spells.byId(LAST_CAST.get(player.getUUID()));
        if (last == null || last == Spells.ECHO || last.tier() > 4 || last.kind() == Spell.Kind.CHANNEL || last.kind() == Spell.Kind.RITUAL) {
            player.sendOverlayMessage(Component.translatable("message.mistech.spell.echo_none").withStyle(net.minecraft.ChatFormatting.GRAY));
            return false;
        }
        double before = Spells.FOCUS_POWER.get();
        Spells.FOCUS_POWER.set(before * 0.6);
        try {
            last.effect().cast(level, player);
        } finally {
            Spells.FOCUS_POWER.set(before);
        }
        SpellFx.risingRing(level, player.position(), RuneElement.ECHO, 1.2, 8, 2.0);
        level.playSound(null, player.blockPosition(), SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.PLAYERS, 0.4F, 1.6F);
        return true;
    }
}

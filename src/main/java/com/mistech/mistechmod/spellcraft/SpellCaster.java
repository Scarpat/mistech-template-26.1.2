package com.mistech.mistechmod.spellcraft;

import java.util.Map;

import com.mistech.mistechmod.affinity.Affinity;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Regras de lançar uma magia (servidor): conhecida, afinidade, recarga, mana. */
public final class SpellCaster {
    public enum Result {
        OK, UNKNOWN, AFFINITY, COOLDOWN, MANA, FAILED
    }

    private SpellCaster() {}

    /** Confere se dá para começar (conhecida, afinidade, recarga, mana do custo base) sem gastar nada. */
    public static Result check(ServerPlayer player, Spell spell, double manaMultiplier) {
        if (!SpellBook.of(player).knows(spell.id())) {
            return fail(player, Result.UNKNOWN, Component.translatable("message.mistech.spell.unknown"));
        }
        boolean forbidden = SpellFocusItem.forbiddenHeld(player);
        if (forbidden && !player.hasInfiniteMaterials() && player.getHealth() <= bloodCost(spell) + 1.0F) {
            return fail(player, Result.MANA, Component.translatable("message.mistech.spell.forbidden_blood"));
        }
        if (!forbidden && !spell.hasAffinity(player)) {
            return fail(player, Result.AFFINITY, Component.translatable("message.mistech.spell.affinity",
                    spell.requirementName().copy().withColor(spell.element().color()), spell.requiredLevel()));
        }
        long now = player.level().getGameTime();
        long until = player.getData(SpellcraftContent.COOLDOWNS).getOrDefault(spell.id(), 0L);
        if (until > now) {
            return fail(player, Result.COOLDOWN, Component.translatable("message.mistech.spell.cooldown", (until - now + 19) / 20));
        }
        int cost = ManaService.cost(player, (int) Math.ceil(spell.mana() * manaMultiplier * mastery(player, spell.element())));
        if (!ManaService.has(player, cost)) {
            return fail(player, Result.MANA, Component.translatable("message.mistech.spell.no_mana", cost));
        }
        return Result.OK;
    }

    /** Maestria (afinidade nível 7 no elemento): magias do elemento custam e recarregam 15% menos. */
    public static final double MASTERY = 0.85;

    /** Recarga: maestria (nível 7) e Cajado do Mago. */
    static double cooldownMods(net.minecraft.world.entity.player.Player player, Spell spell) {
        return (Affinity.effectiveLevel(player, spell.element()) >= Affinity.MAX_LEVEL ? MASTERY : 1.0) * heldStaffCooldown(player);
    }

    /** Custo de mana: maestria (nível 7) e Cajado do Gnomo. */
    public static double mastery(net.minecraft.world.entity.player.Player player, com.mistech.mistechmod.item.RuneElement element) {
        return (Affinity.effectiveLevel(player, element) >= Affinity.MAX_LEVEL ? MASTERY : 1.0) * heldStaffMana(player);
    }

    private static boolean holding(net.minecraft.world.entity.player.Player player, Class<?> type) {
        return type.isInstance(player.getMainHandItem().getItem()) || type.isInstance(player.getOffhandItem().getItem());
    }

    /** Cajado do Mago (Tarô) em uma das mãos: +15% de poder. */
    static double heldStaffPower(net.minecraft.world.entity.player.Player player) {
        return holding(player, com.mistech.mistechmod.tarot.magus.MagusStaffItem.class) ? 1.15 : 1.0;
    }

    /** Cajado do Gnomo em uma das mãos: −10% de mana. Cajado do Mago: −15% de recarga. */
    public static double heldStaffMana(net.minecraft.world.entity.player.Player player) {
        return holding(player, com.mistech.mistechmod.item.staff.GnomeStaffItem.class) ? 0.9 : 1.0;
    }

    static double heldStaffCooldown(net.minecraft.world.entity.player.Player player) {
        return holding(player, com.mistech.mistechmod.tarot.magus.MagusStaffItem.class) ? 0.85 : 1.0;
    }

    /** Vida que o Tomo Proibido cobra por magia: tier ÷ 2 corações. */
    static float bloodCost(Spell spell) {
        return spell.tier();
    }

    /** Tenta lançar. {@code manaMultiplier} vem do item (Tomos, Varinhas e Cajados mudam por elemento). */
    public static Result tryCast(ServerPlayer player, Spell spell, double manaMultiplier, double cooldownMultiplier) {
        return tryCast(player, spell, manaMultiplier, cooldownMultiplier, 0.0, 1.0);
    }

    /** {@code charge}: 0 a 1 nas magias carregadas (poder ×1 a ×2, custo ×1 a ×1,6); {@code focusPower}: poder do item. */
    public static Result tryCast(ServerPlayer player, Spell spell, double manaMultiplier, double cooldownMultiplier, double charge, double focusPower) {
        Result check = check(player, spell, manaMultiplier);
        if (check != Result.OK) {
            return check;
        }
        ServerLevel level = player.level();
        int cost = ManaService.cost(player, (int) Math.ceil(spell.mana() * manaMultiplier * mastery(player, spell.element()) * (1.0 + 0.6 * charge)));
        if (!ManaService.has(player, cost)) {
            return fail(player, Result.MANA, Component.translatable("message.mistech.spell.no_mana", cost));
        }
        boolean worked;
        Resonance.Pair resonance = Resonance.match(Resonance.mark(player, level.getGameTime()), spell.element());
        if (resonance != null) {
            focusPower *= Resonance.bonus(player, resonance);
        }
        // Foco Arcano: a próxima magia sai 50% mais forte
        boolean focused = spell != Spells.ARCANE_FOCUS && UtilitySpells.ARCANE_FOCUS.contains(player.getUUID());
        if (focused) {
            focusPower *= 1.5;
        }
        // Cajados do Gnomo e do Mago (Tarô) na outra mão também reforçam as magias
        focusPower *= heldStaffPower(player);
        Spells.CHARGE.set(charge);
        Spells.FOCUS_POWER.set(focusPower);
        try {
            worked = spell.effect().cast(level, player);
        } finally {
            Spells.CHARGE.set(0.0);
            Spells.FOCUS_POWER.set(1.0);
        }
        if (!worked) {
            return fail(player, Result.FAILED, Component.translatable("message.mistech.spell.no_target"));
        }
        ManaService.spend(player, cost);
        if (focused) {
            UtilitySpells.ARCANE_FOCUS.remove(player.getUUID());
            com.mistech.mistechmod.spellcraft.fx.DreamFx.send(level, com.mistech.mistechmod.spellcraft.fx.DreamFx.MACHINE_BOOST, null, 0.0F, player.position().add(0, 0.6, 0)); // a engrenagem do Foco Arcano solta
        }
        if (spell != Spells.ECHO) {
            UtilitySpells.LAST_CAST.put(player.getUUID(), spell.id());
        }
        if (SpellFocusItem.forbiddenHeld(player) && !player.hasInfiniteMaterials()) {
            player.hurtServer(level, level.damageSources().magic(), bloodCost(spell));
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.DAMAGE_INDICATOR, player.getX(), player.getY() + 1.0, player.getZ(), 6, 0.3, 0.5, 0.3, 0.1);
        }
        if (!com.mistech.mistechmod.spellcraft.fx.DreamFx.owns(spell)) {
            SpellFx.cast(level, player, spell.element());
            SpellVisuals.onCast(level, player, spell);
        }
        if (resonance != null) {
            Resonance.announce(level, player, resonance);
            Resonance.extra(level, player, resonance);
            if (resonance.key().equals("fold")) {
                cooldownMultiplier *= 0.5;
            }
            Resonance.clear(player);
        }
        Resonance.setMark(player, spell.element());
        if (spell.channelTicks() == 0 && !player.hasInfiniteMaterials()) {
            player.getData(SpellcraftContent.COOLDOWNS).put(spell.id(), level.getGameTime() + Math.max(1, (long) (spell.cooldown() * cooldownMultiplier * cooldownMods(player, spell))));
        }
        Affinity.grant(player, spell.element(), 2 + spell.tier());
        return Result.OK;
    }

    /** Um pulso de magia contínua: gasta a mana do pulso e repete o efeito. Falso se acabou a mana. */
    public static boolean pulse(ServerPlayer player, Spell spell, double manaMultiplier, double focusPower) {
        int cost = ManaService.cost(player, (int) Math.ceil(spell.mana() * manaMultiplier * mastery(player, spell.element())));
        if (!ManaService.has(player, cost)) {
            fail(player, Result.MANA, Component.translatable("message.mistech.spell.no_mana", cost));
            return false;
        }
        Spells.FOCUS_POWER.set(focusPower);
        try {
            spell.effect().cast(player.level(), player);
        } finally {
            Spells.FOCUS_POWER.set(1.0);
        }
        ManaService.spend(player, cost);
        if (!com.mistech.mistechmod.spellcraft.fx.DreamFx.owns(spell)) {
            SpellVisuals.onPulse(player.level(), player, spell);
        }
        return true;
    }

    /** Fim da magia contínua (soltou o botão): só agora começa a recarga. */
    public static void finishChannel(ServerPlayer player, Spell spell, double cooldownMultiplier) {
        if (!player.hasInfiniteMaterials()) {
            player.getData(SpellcraftContent.COOLDOWNS).put(spell.id(), player.level().getGameTime() + Math.max(1, (long) (spell.cooldown() * cooldownMultiplier * cooldownMods(player, spell))));
        }
    }

    public static Result tryCast(ServerPlayer player, Spell spell) {
        return tryCast(player, spell, 1.0, 1.0);
    }

    private static Result fail(ServerPlayer player, Result result, Component message) {
        player.sendOverlayMessage(message.copy().withStyle(ChatFormatting.RED));
        return result;
    }

}

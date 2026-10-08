package com.mistech.mistechmod.affinity;

import java.util.HashMap;
import java.util.Map;

import com.mistech.mistechmod.item.RuneElement;
import com.mistech.mistechmod.registry.RitualContent;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * Afinidade Elemental: o jogador ganha pontos num elemento ao usar runas, magias do Cajado e rituais.
 * <ul>
 * <li>Níveis 0 a {@link #MAX_LEVEL} (25/100/250/500/1000 pontos); cada elemento vai até {@link #MAX_POINTS}.</li>
 * <li><b>Exclusiva</b>: ganhar N pontos tira N/2 do elemento oposto; a soma de todos tem teto de {@link #TOTAL_CAP}
 * (o que passa sai dos outros, proporcional). Na prática dá para ter 2–3 elementos altos.</li>
 * </ul>
 */
public final class Affinity {
    public static final int[] THRESHOLDS = {0, 25, 100, 250, 500, 1000};
    public static final int MAX_LEVEL = THRESHOLDS.length - 1;
    public static final int MAX_POINTS = 1000;
    public static final int TOTAL_CAP = 2500;

    private Affinity() {}

    public static AffinityData get(Player player) {
        return player.getData(RitualContent.AFFINITY);
    }

    public static RuneElement opposite(RuneElement element) {
        return switch (element) {
            case FIRE -> RuneElement.ICE;
            case ICE -> RuneElement.FIRE;
            case WATER -> RuneElement.ENERGY;
            case ENERGY -> RuneElement.WATER;
            case EARTH -> RuneElement.AIR;
            case AIR -> RuneElement.EARTH;
            case LIGHT -> RuneElement.DARKNESS;
            case DARKNESS -> RuneElement.LIGHT;
            case SPACE -> RuneElement.TIME;
            case TIME -> RuneElement.SPACE;
            // avançadas não têm afinidade própria (grant manda para os componentes)
            case MAGMA, VAPOR, VOID, LUCK, ARCANE, ECHO, VIGIL, SOUL, CLOUD, LIFE -> element;
        };
    }

    /** Aplica as regras de exclusividade (puro, para testar). */
    public static AffinityData apply(AffinityData data, RuneElement element, int amount) {
        Map<String, Integer> points = new HashMap<>(data.points());
        int before = points.getOrDefault(element.id(), 0);
        int after = Math.min(MAX_POINTS, before + amount);
        int gained = after - before;
        points.put(element.id(), after);
        if (gained <= 0) {
            return new AffinityData(points);
        }
        // oposto perde metade do ganho
        RuneElement opposite = opposite(element);
        points.computeIfPresent(opposite.id(), (k, v) -> Math.max(0, v - gained / 2));
        // teto total: o excesso sai dos outros, proporcional
        int total = 0;
        for (int v : points.values()) {
            total += v;
        }
        int excess = total - TOTAL_CAP;
        if (excess > 0) {
            int others = total - after;
            if (others > 0) {
                int removed = 0;
                for (Map.Entry<String, Integer> entry : points.entrySet()) {
                    if (entry.getKey().equals(element.id()) || entry.getValue() <= 0) {
                        continue;
                    }
                    int cut = (int) Math.ceil(excess * (entry.getValue() / (double) others));
                    cut = Math.min(cut, Math.min(entry.getValue(), excess - removed));
                    entry.setValue(entry.getValue() - cut);
                    removed += cut;
                    if (removed >= excess) {
                        break;
                    }
                }
            }
        }
        points.values().removeIf(v -> v <= 0);
        return new AffinityData(points);
    }

    /** Dá pontos (só no servidor). Avisa quando sobe/desce de nível. */
    public static void grant(@org.jspecify.annotations.Nullable Player player, RuneElement element, int amount) {
        if (player == null || player.level().isClientSide() || amount <= 0) {
            return;
        }
        if (amount == 1) {
            com.mistech.mistechmod.beyond.AmberSeal.onRuneUse(player, element);
        }
        if (element.isAdvanced()) {
            for (RuneElement component : element.components()) {
                grant(player, component, amount);
            }
            return;
        }
        amount = com.mistech.mistechmod.astrology.NatalService.affinityWithBonus(player, element, amount); // Sol do elemento
        amount = com.mistech.mistechmod.accessory.Accessories.affinityGain(player, element, amount); // Pingente do Arcano
        amount = com.mistech.mistechmod.mage.MageRobes.affinity(player, amount); // Vestes de Mago
        AffinityData data = get(player);
        AffinityData updated = apply(data, element, amount);
        player.setData(RitualContent.AFFINITY, updated);

        int before = level(data.get(element));
        int after = level(updated.get(element));
        if (after > before) {
            player.sendOverlayMessage(Component.translatable("message.mistech.affinity.level_up",
                    Component.translatable(element.translationKey()), after).withColor(element.color()));
        } else {
            RuneElement opposite = opposite(element);
            if (level(updated.get(opposite)) < level(data.get(opposite))) {
                player.sendOverlayMessage(Component.translatable("message.mistech.affinity.level_down",
                        Component.translatable(opposite.translationKey()), level(updated.get(opposite))).withColor(opposite.color()));
            }
        }
    }

    public static int level(int points) {
        int level = 0;
        for (int i = 1; i < THRESHOLDS.length; i++) {
            if (points >= THRESHOLDS[i]) {
                level = i;
            }
        }
        return level;
    }

    public static float progress(int points) {
        int level = level(points);
        if (level >= MAX_LEVEL) {
            return 1.0F;
        }
        int from = THRESHOLDS[level];
        int to = THRESHOLDS[level + 1];
        return (points - from) / (float) (to - from);
    }
}

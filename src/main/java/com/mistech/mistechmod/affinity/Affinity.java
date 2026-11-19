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
 * <li>Níveis 0 a {@link #MAX_LEVEL} (25/100/250/500/1000/2000/4000 pontos; 6 = Mestre, 7 = Arquimago); cada elemento vai até {@link #MAX_POINTS}.</li>
 * <li><b>Não diminui</b>: ganhar pontos num elemento não tira de nenhum outro (decisão de 23/10/2026).</li>
 * <li>Poder das magias: +10% por nível. Tier 6 exige nível 6. Nível 7 (Arquimago) dá maestria: −15% de mana e de recarga
 * nas magias do elemento (ver {@code SpellCaster.mastery}).</li>
 * </ul>
 */
public final class Affinity {
    public static final int[] THRESHOLDS = {0, 25, 100, 250, 500, 1000, 2000, 4000};
    public static final int MAX_LEVEL = THRESHOLDS.length - 1;
    public static final int MAX_POINTS = 4000;
    public static final int TOTAL_CAP = 6000;

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

    /** Soma os pontos (teto por elemento). Afinidade nunca diminui. */
    public static AffinityData apply(AffinityData data, RuneElement element, int amount) {
        Map<String, Integer> points = new HashMap<>(data.points());
        int before = points.getOrDefault(element.id(), 0);
        int after = Math.min(MAX_POINTS, before + amount);
        int gained = after - before;
        points.put(element.id(), after);
        // sem perda: nem o oposto nem o teto total tiram pontos (só o teto por elemento vale)
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
        }
    }

    /** Elementos básicos que formam um elemento (ele mesmo se já é básico); avançados são abertos recursivamente. */
    public static java.util.List<RuneElement> basics(RuneElement element) {
        if (!element.isAdvanced()) {
            return java.util.List.of(element);
        }
        java.util.List<RuneElement> list = new java.util.ArrayList<>();
        for (RuneElement component : element.components()) {
            for (RuneElement basic : basics(component)) {
                if (!list.contains(basic)) {
                    list.add(basic);
                }
            }
        }
        return list;
    }

    /** Nível "efetivo" do jogador num elemento: o próprio nível se for básico; a média arredondada para baixo dos componentes se for avançado. */
    public static int effectiveLevel(Player player, RuneElement element) {
        AffinityData data = get(player);
        java.util.List<RuneElement> basics = basics(element);
        int sum = 0;
        for (RuneElement basic : basics) {
            sum += level(data.get(basic));
        }
        return basics.isEmpty() ? 0 : sum / basics.size();
    }

    /** Tem pelo menos {@code level} em todos os componentes básicos do elemento? */
    public static boolean meets(Player player, RuneElement element, int level) {
        AffinityData data = get(player);
        for (RuneElement basic : basics(element)) {
            if (level(data.get(basic)) < level) {
                return false;
            }
        }
        return true;
    }

    /** Poder de magia: 1 + 10% por nível de afinidade do elemento (nível 7 = ×1,7). */
    public static double power(Player player, RuneElement element) {
        return (1.0 + 0.10 * effectiveLevel(player, element)) * com.mistech.mistechmod.purity.area.AreaTaint.spellPower(player, element);
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

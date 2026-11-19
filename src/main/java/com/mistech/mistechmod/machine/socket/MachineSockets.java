package com.mistech.mistechmod.machine.socket;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import com.mistech.mistechmod.item.RuneElement;

/**
 * Engastes de máquina (Fase 2 do plano magia × tech): espaços extras onde qualquer runa dá um efeito universal,
 * por cima do espaço de runa próprio de cada máquina.
 * <p>
 * Quantos espaços e qual a categoria de cada máquina ficam aqui, num lugar só ({@link #SPECS}, pelo nome do layout).
 * O {@code MachineLayout} acrescenta os espaços no FIM da lista (para não embaralhar os itens salvos) e o
 * {@code MachineBlockEntity} aplica os efeitos.
 */
public final class MachineSockets {
    private MachineSockets() {}

    /** Tipo de máquina: decide quais runas fazem efeito nela. */
    public enum Category {
        PROCESSING, GENERATION, AREA, MINING, FLUID, STORAGE, SPELL;

        public String id() {
            return this.name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    /** Categoria, quantos engastes e "família" (geradores de calor, impuros, celestes, de experiência). */
    public record Spec(Category category, int sockets, String family) {}

    /** Posição dos engastes na tela: coluna à esquerda do painel. */
    public static final int SLOT_X = -20, SLOT_Y = 17;

    public static final String HEAT = "heat", IMPURE = "impure", CELESTIAL = "celestial", XP = "xp", NONE = "";

    private static final Map<String, Spec> SPECS = Map.ofEntries(
            // processamento
            Map.entry("runic_crusher", new Spec(Category.PROCESSING, 1, NONE)),
            Map.entry("magic_loom", new Spec(Category.PROCESSING, 1, NONE)),
            Map.entry("cane_still", new Spec(Category.PROCESSING, 1, NONE)),
            Map.entry("arcane_crusher", new Spec(Category.PROCESSING, 2, NONE)),
            Map.entry("arcane_furnace", new Spec(Category.PROCESSING, 2, NONE)),
            Map.entry("arcane_compressor", new Spec(Category.PROCESSING, 2, NONE)),
            Map.entry("crusher_forge", new Spec(Category.PROCESSING, 3, NONE)),
            Map.entry("essence_washer", new Spec(Category.PROCESSING, 2, NONE)),
            Map.entry("runic_crystallizer", new Spec(Category.PROCESSING, 2, CELESTIAL)),
            Map.entry("astral_prism", new Spec(Category.PROCESSING, 3, NONE)),
            // fluidos
            Map.entry("liquefier", new Spec(Category.FLUID, 1, NONE)),
            Map.entry("distiller", new Spec(Category.FLUID, 1, NONE)),
            Map.entry("arcane_extractor", new Spec(Category.FLUID, 2, NONE)),
            // área e mineração
            Map.entry("runic_harvester", new Spec(Category.AREA, 1, NONE)),
            Map.entry("runic_planter", new Spec(Category.AREA, 1, NONE)),
            Map.entry("miner", new Spec(Category.MINING, 2, NONE)),
            // geradores
            Map.entry("ash_generator", new Spec(Category.GENERATION, 1, HEAT)),
            Map.entry("blaze_generator", new Spec(Category.GENERATION, 1, HEAT)),
            Map.entry("gnome_bicycle", new Spec(Category.GENERATION, 1, NONE)),
            Map.entry("astral_generator", new Spec(Category.GENERATION, 1, CELESTIAL)),
            Map.entry("pure_generator", new Spec(Category.GENERATION, 1, CELESTIAL)),
            Map.entry("igneous_boiler", new Spec(Category.GENERATION, 2, HEAT)),
            Map.entry("impure_reactor", new Spec(Category.GENERATION, 2, IMPURE)),
            Map.entry("stellar_observatory", new Spec(Category.GENERATION, 2, CELESTIAL)),
            Map.entry("experience_generator", new Spec(Category.GENERATION, 2, XP)),
            Map.entry("mana_siphon", new Spec(Category.GENERATION, 1, NONE)),
            Map.entry("saci_windmill", new Spec(Category.GENERATION, 1, NONE)),
            Map.entry("iara_pump", new Spec(Category.FLUID, 1, NONE)),
            Map.entry("boitata_pump", new Spec(Category.FLUID, 1, HEAT)),
            Map.entry("curupira_stump", new Spec(Category.AREA, 1, NONE)),
            Map.entry("potion_still", new Spec(Category.PROCESSING, 1, NONE)),
            Map.entry("scroll_printer", new Spec(Category.PROCESSING, 1, NONE)),
            // baterias
            Map.entry("runic_battery", new Spec(Category.STORAGE, 1, NONE)),
            Map.entry("advanced_runic_battery", new Spec(Category.STORAGE, 2, NONE)),
            // magia
            Map.entry("runic_conjurer", new Spec(Category.SPELL, 2, NONE)),
            // endgame (Fase 9)
            Map.entry("star_forge", new Spec(Category.PROCESSING, 3, CELESTIAL)),
            Map.entry("dream_reactor", new Spec(Category.GENERATION, 2, NONE)),
            Map.entry("celestial_battery", new Spec(Category.STORAGE, 3, NONE)),
            Map.entry("celestial_conjurer", new Spec(Category.SPELL, 3, NONE)));

    private static final Spec DEFAULT = new Spec(Category.PROCESSING, 0, NONE);

    public static Spec spec(String layoutName) {
        return SPECS.getOrDefault(layoutName, DEFAULT);
    }

    /** Categorias que gastam energia para trabalhar (Gelo, Vapor, Arcano, Tempo fazem efeito). */
    private static final Set<Category> WORKERS = EnumSet.of(Category.PROCESSING, Category.AREA, Category.MINING, Category.FLUID, Category.SPELL);
    private static final Set<Category> PRODUCERS = EnumSet.of(Category.PROCESSING, Category.AREA);

    /** A runa faz efeito nesta máquina? */
    public static boolean applies(RuneElement element, Spec spec) {
        Category c = spec.category();
        return switch (element) {
            case FIRE, EARTH, ECHO, SOUL -> c == Category.PROCESSING || element == RuneElement.SOUL && XP.equals(spec.family());
            case ICE, VAPOR, ARCANE, TIME -> WORKERS.contains(c);
            case MAGMA -> HEAT.equals(spec.family());
            case DARKNESS -> IMPURE.equals(spec.family());
            case LIGHT -> CELESTIAL.equals(spec.family());
            case WATER -> c == Category.FLUID;
            case AIR -> c != Category.GENERATION && c != Category.STORAGE;
            case ENERGY -> true;
            case LUCK -> PRODUCERS.contains(c) || c == Category.MINING;
            case SPACE, VOID -> PRODUCERS.contains(c);
            case CLOUD, LIFE -> c == Category.AREA;
            case VIGIL -> c != Category.STORAGE;
        };
    }

    /** Chave do texto do efeito (na dica do engaste). */
    public static String effectKey(RuneElement element) {
        return "socket.mistech." + element.id();
    }

    /**
     * Runas ativas: as engastadas que valem para a máquina, sem repetir, e tirando Fogo + Gelo (se anulam).
     */
    public static EnumSet<RuneElement> active(Iterable<RuneElement> socketed, Spec spec) {
        EnumSet<RuneElement> set = EnumSet.noneOf(RuneElement.class);
        for (RuneElement e : socketed) {
            if (e != null && applies(e, spec)) {
                set.add(e);
            }
        }
        if (set.contains(RuneElement.FIRE) && set.contains(RuneElement.ICE)) {
            set.remove(RuneElement.FIRE);
            set.remove(RuneElement.ICE);
        }
        return set;
    }

    /** Fogo e Gelo engastados juntos (para avisar na tela). */
    public static boolean cancelled(Iterable<RuneElement> socketed) {
        boolean fire = false, ice = false;
        for (RuneElement e : socketed) {
            fire |= e == RuneElement.FIRE;
            ice |= e == RuneElement.ICE;
        }
        return fire && ice;
    }

    // ------------------------------------------------------------------ números

    /** Velocidade extra (soma ao passo da máquina). */
    public static double speedBonus(Set<RuneElement> active) {
        double b = 0.0;
        if (active.contains(RuneElement.TIME)) b += 0.25;
        if (active.contains(RuneElement.VAPOR)) b += 0.10;
        if (active.contains(RuneElement.LIFE)) b += 0.30;
        if (active.contains(RuneElement.WATER)) b += 0.30;
        return b;
    }

    /** Multiplicador do gasto de energia por tick (depois da penalidade das Melhorias de Velocidade). */
    public static double energyFactor(Set<RuneElement> active) {
        double f = 1.0;
        if (active.contains(RuneElement.ARCANE)) f *= 0.8;
        if (active.contains(RuneElement.FIRE)) f *= 1.1;
        return f;
    }

    /** Gelo/Vapor: as Melhorias de Velocidade deixam de gastar energia extra. */
    public static boolean cooled(Set<RuneElement> active) {
        return active.contains(RuneElement.ICE) || active.contains(RuneElement.VAPOR);
    }

    /** Multiplicador da energia gerada (geradores). */
    public static double generationFactor(Set<RuneElement> active) {
        double f = 1.0;
        if (active.contains(RuneElement.MAGMA)) f *= 1.25;
        if (active.contains(RuneElement.ENERGY)) f *= 1.10;
        if (active.contains(RuneElement.DARKNESS)) f *= 1.15;
        if (active.contains(RuneElement.LIGHT)) f *= 1.15;
        if (active.contains(RuneElement.SOUL)) f *= 1.15;
        return f;
    }

    /** Entrada/saída de energia (Runa de Energia: +50%). */
    public static double rateFactor(Set<RuneElement> active) {
        return active.contains(RuneElement.ENERGY) ? 1.5 : 1.0;
    }

    /** Chance de cada bônus de produto: Terra 10%, Eco 10% (15% com Tempo), Sorte 5%. */
    public static int bonusRolls(Set<RuneElement> active, net.minecraft.util.RandomSource random) {
        return bonusRolls(active, random, 1.0F, 1.0F);
    }

    /** Igual, com multiplicadores das chances de Sorte e Eco (Aurora, Virgem: Fase 6). */
    public static int bonusRolls(Set<RuneElement> active, net.minecraft.util.RandomSource random, float luck, float echo) {
        int extra = 0;
        if (active.contains(RuneElement.EARTH) && random.nextFloat() < 0.10F) extra++;
        if (active.contains(RuneElement.ECHO) && random.nextFloat() < (active.contains(RuneElement.TIME) ? 0.15F : 0.10F) * echo) extra++;
        if (active.contains(RuneElement.LUCK) && random.nextFloat() < 0.05F * luck) extra++;
        return extra;
    }

    /** Mácula solta pela máquina (Fase 4): Luz corta pela metade, Escuridão soma 50%. */
    public static double taintFactor(Set<RuneElement> active) {
        double f = 1.0;
        if (active.contains(RuneElement.LIGHT)) f *= 0.5;
        if (active.contains(RuneElement.DARKNESS)) f *= 1.5;
        return f;
    }
}

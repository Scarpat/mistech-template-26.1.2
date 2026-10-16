package com.mistech.mistechmod.realm;

import net.minecraft.util.Mth;

/**
 * Geometria do Sonho (Dimensão dos Gnomos): anéis concêntricos em volta de (0,0).
 * <pre>
 *  1. Sonho Comum      0 .. 400    (centro sempre Prado: Casa do Rei + mina)
 *  2. Pesadelo       400 .. 2000
 *  3. Sonho Profundo 2000 .. 5000  (ilhas flutuantes sobre o Mar de Nuvens)
 *  4. Sono REM       5000 .. 29000 (ilhas sobre a Névoa Estelar)
 *  5. Borda do Sonho 29000 .. 30000 (névoa branca); passou de 30000 = acorda em casa
 * </pre>
 * A distância é "ondulada" (senos fixos, sem semente) para os anéis não serem círculos perfeitos;
 * a mesma conta serve ao {@link GnomeRealmBiomeSource}, às density functions ({@link DreamMaskFunction}) e aos eventos.
 * Os sub-biomas de cada anel saem de dois ruídos grandes (temperatura e vegetação do noise router).
 */
public final class DreamLayout {
    public static final int CENTER = 70;
    public static final int COMMON = 400;
    public static final int NIGHTMARE = 2000;
    public static final int DEEP = 5000;
    public static final int EDGE = 30000;
    public static final int EDGE_BAND = 1000;
    /** Meia largura da transição do relevo normal para as ilhas. */
    public static final double ISLAND_FADE = 64.0;

    public static final int RING_COMMON = 1, RING_NIGHTMARE = 2, RING_DEEP = 3, RING_REM = 4, RING_EDGE = 5, RING_BEYOND = 6;

    /** Limiar dos sub-biomas comuns (~1/4 de cada lado, o resto é o sub-bioma principal). */
    public static final double T_SPLIT = 0.15;
    /** Limiar dos sub-biomas raros (~10%). */
    public static final double T_RARE = 0.42;
    public static final double V_RARE = 0.42;

    private DreamLayout() {}

    /** Distância ondulada do centro. */
    public static double distance(double x, double z) {
        double d = Math.sqrt(x * x + z * z);
        if (d < 1.0) {
            return d;
        }
        double a = Math.atan2(z, x);
        double wobble = 0.045 * Math.sin(a * 5.0 + 0.7) + 0.03 * Math.sin(a * 9.0 + 2.1) + 0.02 * Math.sin(a * 17.0 + 4.0);
        return d * (1.0 + wobble);
    }

    public static int ring(double x, double z) {
        return ringAt(distance(x, z));
    }

    public static int ringAt(double d) {
        if (d < COMMON) return RING_COMMON;
        if (d < NIGHTMARE) return RING_NIGHTMARE;
        if (d < DEEP) return RING_DEEP;
        if (d < EDGE - EDGE_BAND) return RING_REM;
        if (d < EDGE) return RING_EDGE;
        return RING_BEYOND;
    }

    /** Distância real (sem ondulação) — para a borda, que é um círculo. */
    public static double plainDistance(double x, double z) {
        return Math.sqrt(x * x + z * z);
    }

    /** 0 = relevo normal (anéis 1-2), 1 = ilhas flutuantes (anéis 3+); transição suave. */
    public static double islandWeight(double x, double z) {
        return smooth((distance(x, z) - (NIGHTMARE - ISLAND_FADE)) / (2.0 * ISLAND_FADE));
    }

    /** 1 dentro do anel, 0 fora, com borda suave de {@code fade} blocos. */
    public static double ringMask(double x, double z, int ring, double fade) {
        double d = distance(x, z);
        double inner = switch (ring) {
            case RING_COMMON -> -1e9;
            case RING_NIGHTMARE -> COMMON;
            case RING_DEEP -> NIGHTMARE;
            case RING_REM -> DEEP;
            default -> EDGE - EDGE_BAND;
        };
        double outer = switch (ring) {
            case RING_COMMON -> COMMON;
            case RING_NIGHTMARE -> NIGHTMARE;
            case RING_DEEP -> DEEP;
            case RING_REM -> EDGE - EDGE_BAND;
            default -> 1e9;
        };
        double in = smooth((d - inner + fade) / (2.0 * fade));
        double out = 1.0 - smooth((d - outer + fade) / (2.0 * fade));
        return Math.min(in, out);
    }

    private static double smooth(double t) {
        t = Mth.clamp(t, 0.0, 1.0);
        return t * t * (3.0 - 2.0 * t);
    }

    /**
     * Qual sub-bioma do anel (índice na lista do anel), pelos ruídos de região {@code t} (temperatura) e {@code v} (vegetação).
     * <ul>
     * <li>Anel 1: 0 Prado, 1 Cogumelal Doce, 2 Colinas de Travesseiro.</li>
     * <li>Anel 2: 0 Ermo Turvo, 1 Floresta das Mãos, 2 Abismos de Queda, 3 Bairro Errado (raro).</li>
     * <li>Anel 3: 0 Mar de Nuvens, 1 Jardim Fractal, 2 Floresta Invertida.</li>
     * <li>Anel 4: 0 Vazio Estrelado, 1 Colcha de Memórias, 2 Olhar Abissal (raro).</li>
     * </ul>
     */
    public static int subBiome(int ring, double x, double z, double t, double v) {
        switch (ring) {
            case RING_COMMON: {
                if (distance(x, z) < CENTER) return 0;
                double u = commonSector(x, z, t);
                if (u > 0.6) return 1;
                if (u < -0.6) return 2;
                return 0;
            }
            case RING_NIGHTMARE:
                if (v > V_RARE) return 3;
                if (t > T_SPLIT) return 1;
                if (t < -T_SPLIT) return 2;
                return 0;
            case RING_DEEP:
                if (t > T_SPLIT) return 1;
                if (t < -T_SPLIT) return 2;
                return 0;
            case RING_REM:
                if (t > T_SPLIT) return 1;
                if (t < -T_RARE) return 2;
                return 0;
            default:
                return 0;
        }
    }

    /**
     * No Sonho Comum (pequeno demais para as manchas grandes do ruído) os sub-biomas vêm em 3 "pétalas" em volta do centro,
     * com a borda torta pelo ruído: &gt; 0,6 Cogumelal, &lt; −0,6 Colinas, meio = Prado.
     */
    public static double commonSector(double x, double z, double t) {
        return Math.sin(Math.atan2(z, x) * 3.0 + t * 2.0);
    }
}

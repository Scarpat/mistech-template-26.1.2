package com.mistech.mistechmod.celestial;

import java.util.Set;

import com.mistech.mistechmod.astrology.AstrologyData;
import com.mistech.mistechmod.astrology.Constellations;
import com.mistech.mistechmod.item.RuneElement;
import com.mistech.mistechmod.machine.MachineBlockEntity;
import com.mistech.mistechmod.machine.socket.MachineSockets;
import com.mistech.mistechmod.registry.MistechAttachments;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Fase 6 do plano magia × tech: o céu nas máquinas.
 * <ul>
 * <li>Eventos Celestes ativos dão bônus a certos tipos de máquina (tabela em {@link #speed}, {@link #generation}, {@link #taint}...).</li>
 * <li>Cada constelação completa do dono da máquina (quem a colocou) dá um bônus fixo a um tipo de máquina.</li>
 * </ul>
 * As máquinas guardam o dono e a máscara das constelações completas dele (bit = índice em {@link Constellations#ALL}),
 * atualizada enquanto ele está online; assim funciona com o dono fora do jogo. Os valores são consultados a cada segundo.
 */
public final class MachineSky {
    // índices das constelações em Constellations.ALL
    public static final int ARIES = 0, TAURUS = 1, GEMINI = 2, CANCER = 3, LEO = 4, VIRGO = 5, LIBRA = 6, SCORPIUS = 7,
            SAGITTARIUS = 8, CAPRICORNUS = 9, AQUARIUS = 10, PISCES = 11;

    /** Máquinas "biológicas" (plantas e bichos). */
    public static final Set<String> BIOLOGICAL = Set.of("runic_harvester", "runic_planter", "curupira_stump");

    private MachineSky() {}

    /** Constelações completas do jogador (bit por constelação). */
    public static int completeMask(Player player) {
        AstrologyData data = player.getData(MistechAttachments.ASTROLOGY);
        int mask = 0;
        for (int i = 0; i < Constellations.ALL.size(); i++) {
            if (data.countIn(i) >= Constellations.get(i).stars().size()) {
                mask |= 1 << i;
            }
        }
        return mask;
    }

    public static boolean has(int mask, int constellation) {
        return (mask & (1 << constellation)) != 0;
    }

    private static boolean on(Level level, CelestialEvent event) {
        return Celestial.isActive(level, event);
    }

    private static boolean bloodNight(Level level) {
        return on(level, CelestialEvent.BLOOD_MOON) || on(level, CelestialEvent.ETERNAL_NIGHT);
    }

    /** Velocidade de trabalho. */
    public static double speed(MachineBlockEntity be, Level level, int mask) {
        String name = be.layoutName();
        MachineSockets.Category category = be.socketCategory();
        double f = 1.0;
        if (BIOLOGICAL.contains(name)) {
            if (on(level, CelestialEvent.SOLSTICE)) f *= 1.5;
            if (has(mask, TAURUS)) f *= 1.2;
        }
        if (be.socketActive(RuneElement.DARKNESS) && on(level, CelestialEvent.ECLIPSE)) f *= 1.25;
        if (be.socketActive(RuneElement.WATER) && on(level, CelestialEvent.BLESSED_RAIN)) f *= 1.25;
        if (category == MachineSockets.Category.PROCESSING && has(mask, LEO)) f *= 1.1;
        if (category == MachineSockets.Category.MINING && has(mask, CAPRICORNUS)) f *= 1.2;
        if (category == MachineSockets.Category.FLUID && has(mask, AQUARIUS)) f *= 1.25;
        return f;
    }

    /** Energia gerada. */
    public static double generation(MachineBlockEntity be, Level level, int mask) {
        String name = be.layoutName();
        double f = 1.0;
        switch (name) {
            case "pure_generator" -> { if (on(level, CelestialEvent.SOLSTICE)) f *= 1.25; }
            case "impure_reactor" -> { if (bloodNight(level)) f *= 1.5; }
            case "astral_generator" -> { if (on(level, CelestialEvent.AURORA)) f *= 1.5; }
            case "stellar_observatory" -> { if (on(level, CelestialEvent.STARFALL)) f *= 2.0; }
            default -> { }
        }
        if (be.socketCategory() == MachineSockets.Category.GENERATION && has(mask, LIBRA)) f *= 1.1;
        return f;
    }

    /** Mácula do lugar solta. */
    public static double taint(MachineBlockEntity be, Level level, int mask) {
        double f = 1.0;
        if (be.layoutName().equals("impure_reactor") && bloodNight(level)) f *= 2.0;
        if (has(mask, SCORPIUS)) f *= 0.75;
        return f;
    }

    /** Chance dos engastes de Sorte. */
    public static float luck(Level level, int mask) {
        return (on(level, CelestialEvent.AURORA) ? 2.0F : 1.0F) * (has(mask, VIRGO) ? 1.5F : 1.0F);
    }

    /** Chance dos engastes de Eco. */
    public static float echo(Level level) {
        return on(level, CelestialEvent.AURORA) ? 2.0F : 1.0F;
    }

    /** Divisor do intervalo de transferência com engaste de Ar (2 normal, 3 com Gêmeos). */
    public static int airDivisor(int mask) {
        return has(mask, GEMINI) ? 3 : 2;
    }

    /** Alcance extra das máquinas de área (Sagitário). */
    public static int range(MachineBlockEntity be, int mask) {
        return be.socketCategory() == MachineSockets.Category.AREA && has(mask, SAGITTARIUS) ? 1 : 0;
    }

    /** Conjurador em modo torreta: poder. */
    public static double turret(Level level, int mask) {
        return (bloodNight(level) ? 1.25 : 1.0) * (has(mask, ARIES) ? 1.15 : 1.0);
    }

    /** Filtro de Arruda: limpeza (Câncer). */
    public static float filter(int mask) {
        return has(mask, CANCER) ? 1.25F : 1.0F;
    }

    /** Bomba da Iara (Fase 7): Peixes. */
    public static float iara(int mask) {
        return has(mask, PISCES) ? 1.5F : 1.0F;
    }

    /** Prisma Astral: velocidade extra na Lua de Prata e na Noite Eterna. */
    public static double prism(Level level) {
        return on(level, CelestialEvent.SILVER_MOON) || on(level, CelestialEvent.ETERNAL_NIGHT) ? 1.5 : 1.0;
    }

    /** Prisma Astral: na Chuva de Estrelas não gasta Fragmento Estelar. */
    public static boolean freeFragments(Level level) {
        return on(level, CelestialEvent.STARFALL);
    }
}

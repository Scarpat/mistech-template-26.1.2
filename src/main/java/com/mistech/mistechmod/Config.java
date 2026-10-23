package com.mistech.mistechmod;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Configurações do Mistech.
 * <ul>
 * <li><b>SERVER</b> ({@code saves/<mundo>/serverconfig/mistech-server.toml}): regras do jogo, sincronizadas com os jogadores.</li>
 * <li><b>CLIENT</b> ({@code config/mistech-client.toml}): só visual (holograma da Pedra Guia, alcance, Porta-Pergaminhos).</li>
 * </ul>
 * Use sempre os métodos (ex.: {@link #machineEnergy()}): antes de o mundo carregar eles devolvem o valor padrão.
 */
public final class Config {
    private Config() {}

    // =====================================================================
    // SERVER
    // =====================================================================
    private static final ModConfigSpec.Builder SERVER_BUILDER = new ModConfigSpec.Builder();

    // ----- máquinas -----
    private static final ModConfigSpec.DoubleValue MACHINE_ENERGY;
    private static final ModConfigSpec.DoubleValue MACHINE_SPEED;
    private static final ModConfigSpec.DoubleValue GENERATOR_OUTPUT;
    private static final ModConfigSpec.IntValue AUTO_IO_INTERVAL;
    private static final ModConfigSpec.IntValue AUTO_IO_AMOUNT;

    // ----- melhorias -----
    private static final ModConfigSpec.IntValue MAX_SPEED_UPGRADES;
    private static final ModConfigSpec.IntValue MAX_EFFICIENCY_UPGRADES;
    private static final ModConfigSpec.IntValue MAX_RANGE_UPGRADES;
    private static final ModConfigSpec.DoubleValue SPEED_PER_UPGRADE;
    private static final ModConfigSpec.DoubleValue SPEED_ENERGY_PENALTY;
    private static final ModConfigSpec.DoubleValue EFFICIENCY_PER_UPGRADE;

    // ----- máquinas iniciais -----
    private static final ModConfigSpec.IntValue EXTRACTOR_SECONDS;
    private static final ModConfigSpec.IntValue COMPRESSOR_SECONDS;
    private static final ModConfigSpec.DoubleValue RUNIC_FURNACE_TIME;

    // ----- âncoras -----
    private static final ModConfigSpec.IntValue ANCHOR_BLOCKS_PER_LEVEL;
    private static final ModConfigSpec.IntValue ANCHOR_MAX_COST;
    private static final ModConfigSpec.IntValue ANCHOR_CROSS_DIMENSION;

    // ----- Porta-Pergaminhos -----
    private static final ModConfigSpec.IntValue SCROLL_CASE_SLOTS;

    static {
        ModConfigSpec.Builder b = SERVER_BUILDER;
        b.comment("Máquinas do Mistech (as que têm tela com barra de energia)").push("machines");
        MACHINE_ENERGY = b.comment("Multiplicador do gasto de energia (FE/t) das máquinas. 0.5 = metade, 2.0 = dobro.")
                .defineInRange("energyMultiplier", 1.0, 0.0, 100.0);
        MACHINE_SPEED = b.comment("Multiplicador da velocidade das máquinas (progresso por tick).")
                .defineInRange("speedMultiplier", 1.0, 0.1, 20.0);
        GENERATOR_OUTPUT = b.comment("Multiplicador da energia produzida pelos geradores.")
                .defineInRange("generatorMultiplier", 1.0, 0.0, 100.0);
        AUTO_IO_INTERVAL = b.comment("De quantos em quantos ticks a máquina empurra/puxa itens dos blocos encostados (Saída/Entrada automática).")
                .defineInRange("autoIoInterval", 8, 1, 200);
        AUTO_IO_AMOUNT = b.comment("Quantos itens a máquina move por face a cada vez.")
                .defineInRange("autoIoAmount", 16, 1, 64);
        b.pop();

        b.comment("Melhorias Rúnicas (aplicadas clicando na máquina com a melhoria)").push("upgrades");
        MAX_SPEED_UPGRADES = b.comment("Máximo de Melhorias de Velocidade por máquina.").defineInRange("maxSpeed", 4, 0, 16);
        MAX_EFFICIENCY_UPGRADES = b.comment("Máximo de Melhorias de Eficiência por máquina.").defineInRange("maxEfficiency", 4, 0, 16);
        MAX_RANGE_UPGRADES = b.comment("Máximo de Melhorias de Alcance por máquina.").defineInRange("maxRange", 3, 0, 16);
        SPEED_PER_UPGRADE = b.comment("Velocidade extra por Melhoria de Velocidade (0.5 = +50%).")
                .defineInRange("speedPerUpgrade", 0.5, 0.0, 10.0);
        SPEED_ENERGY_PENALTY = b.comment("Gasto extra de energia por operação para cada Melhoria de Velocidade (0.1 = +10%).")
                .defineInRange("speedEnergyPenalty", 0.1, 0.0, 10.0);
        EFFICIENCY_PER_UPGRADE = b.comment("Economia de energia por Melhoria de Eficiência (0.15 = -15%). O total nunca passa de 90%.")
                .defineInRange("efficiencyPerUpgrade", 0.15, 0.0, 0.9);
        b.pop();

        b.comment("Máquinas iniciais").push("early");
        EXTRACTOR_SECONDS = b.comment("Segundos para o Extrator de Essência encher 1 nível.").defineInRange("extractorSecondsPerLevel", 60, 1, 3600);
        COMPRESSOR_SECONDS = b.comment("Segundos para o Compressor de Magia endurecer a essência.").defineInRange("compressorSeconds", 15, 1, 3600);
        RUNIC_FURNACE_TIME = b.comment("Tempo da Fornalha Rúnica em relação à fornalha comum (0.75 = 25% mais rápida).")
                .defineInRange("runicFurnaceTime", 0.75, 0.05, 4.0);
        b.pop();

        b.comment("Âncoras Espaciais e Orbe de Translocação").push("anchors");
        ANCHOR_BLOCKS_PER_LEVEL = b.comment("Blocos de distância por nível de experiência gasto.").defineInRange("blocksPerLevel", 1000, 1, 1_000_000);
        ANCHOR_MAX_COST = b.comment("Custo máximo (níveis) na mesma dimensão.").defineInRange("maxCost", 5, 0, 1000);
        ANCHOR_CROSS_DIMENSION = b.comment("Custo (níveis) para outra dimensão.").defineInRange("crossDimensionCost", 5, 0, 1000);
        b.pop();

        b.comment("Porta-Pergaminhos").push("scroll_case");
        SCROLL_CASE_SLOTS = b.comment("Quantas runas cabem no Porta-Pergaminhos (1 a 9).").defineInRange("slots", 9, 1, 9);
        b.pop();
    }

    public static final ModConfigSpec SERVER_SPEC = SERVER_BUILDER.build();

    // =====================================================================
    // CLIENT
    // =====================================================================
    private static final ModConfigSpec.Builder CLIENT_BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.IntValue RANGE_SCAN;
    private static final ModConfigSpec.BooleanValue RANGE_NEEDS_SNEAK;
    private static final ModConfigSpec.IntValue HOLOGRAM_ALPHA;
    private static final ModConfigSpec.BooleanValue HOLOGRAM_LABELS;
    private static final ModConfigSpec.BooleanValue SCROLL_WHEEL;

    static {
        ModConfigSpec.Builder b = CLIENT_BUILDER;
        b.comment("Pedra Guia: alcance das máquinas e holograma dos multiblocos").push("guide_stone");
        RANGE_SCAN = b.comment("Até quantos blocos de distância a Pedra Guia mostra o alcance dos blocos.").defineInRange("rangeScanRadius", 24, 4, 64);
        RANGE_NEEDS_SNEAK = b.comment("Mostrar o alcance só agachado (false = sempre que estiver com a Pedra Guia na mão).")
                .define("rangeNeedsSneak", true);
        HOLOGRAM_ALPHA = b.comment("Opacidade do holograma (0 a 255).").defineInRange("hologramAlpha", 90, 10, 255);
        HOLOGRAM_LABELS = b.comment("Mostrar o nome do bloco que falta em cima de cada peça perto de você.").define("hologramLabels", true);
        b.pop();

        b.comment("Porta-Pergaminhos").push("scroll_case");
        SCROLL_WHEEL = b.comment("Agachado + roda do mouse troca a runa do Porta-Pergaminhos na mão.").define("sneakScrollCycles", true);
        b.pop();
    }

    public static final ModConfigSpec CLIENT_SPEC = CLIENT_BUILDER.build();

    // =====================================================================
    // leitura segura (antes de carregar, devolve o padrão)
    // =====================================================================
    private static double d(ModConfigSpec.DoubleValue v) {
        try {
            return v.getAsDouble();
        } catch (IllegalStateException e) {
            return v.getDefault();
        }
    }

    private static int i(ModConfigSpec.IntValue v) {
        try {
            return v.getAsInt();
        } catch (IllegalStateException e) {
            return v.getDefault();
        }
    }

    private static boolean bool(ModConfigSpec.BooleanValue v) {
        try {
            return v.getAsBoolean();
        } catch (IllegalStateException e) {
            return v.getDefault();
        }
    }

    public static double machineEnergy() { return d(MACHINE_ENERGY); }
    public static double machineSpeed() { return d(MACHINE_SPEED); }
    public static double generatorOutput() { return d(GENERATOR_OUTPUT); }
    public static int autoIoInterval() { return i(AUTO_IO_INTERVAL); }
    public static int autoIoAmount() { return i(AUTO_IO_AMOUNT); }

    public static int maxSpeedUpgrades() { return i(MAX_SPEED_UPGRADES); }
    public static int maxEfficiencyUpgrades() { return i(MAX_EFFICIENCY_UPGRADES); }
    public static int maxRangeUpgrades() { return i(MAX_RANGE_UPGRADES); }
    public static double speedPerUpgrade() { return d(SPEED_PER_UPGRADE); }
    public static double speedEnergyPenalty() { return d(SPEED_ENERGY_PENALTY); }
    public static double efficiencyPerUpgrade() { return d(EFFICIENCY_PER_UPGRADE); }

    public static int extractorTicksPerLevel() { return i(EXTRACTOR_SECONDS) * 20; }
    public static int compressorTicks() { return i(COMPRESSOR_SECONDS) * 20; }
    public static float runicFurnaceTime() { return (float) d(RUNIC_FURNACE_TIME); }

    public static int anchorBlocksPerLevel() { return i(ANCHOR_BLOCKS_PER_LEVEL); }
    public static int anchorMaxCost() { return i(ANCHOR_MAX_COST); }
    public static int anchorCrossDimension() { return i(ANCHOR_CROSS_DIMENSION); }

    public static int scrollCaseSlots() { return i(SCROLL_CASE_SLOTS); }

    public static int rangeScanRadius() { return i(RANGE_SCAN); }
    public static boolean rangeNeedsSneak() { return bool(RANGE_NEEDS_SNEAK); }
    public static int hologramAlpha() { return i(HOLOGRAM_ALPHA); }
    public static boolean hologramLabels() { return bool(HOLOGRAM_LABELS); }
    public static boolean sneakScrollCycles() { return bool(SCROLL_WHEEL); }
}

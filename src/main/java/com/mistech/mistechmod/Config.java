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

    // ----- Conjurador Rúnico -----
    private static final ModConfigSpec.IntValue CONJURER_FE_PER_MANA;
    private static final ModConfigSpec.IntValue CONJURER_ESSENCE_PER_TIER;
    private static final ModConfigSpec.DoubleValue CONJURER_COOLDOWN;
    private static final ModConfigSpec.DoubleValue CONJURER_POWER;
    private static final ModConfigSpec.IntValue CONJURER_ENGRAVE_MANA;

    // ----- engastes de máquina -----
    private static final ModConfigSpec.IntValue SOCKET_WEAR_TICKS;

    // ----- Mácula do lugar -----
    private static final ModConfigSpec.BooleanValue AREA_TAINT_ENABLED;
    private static final ModConfigSpec.DoubleValue AREA_TAINT_REACTOR;
    private static final ModConfigSpec.DoubleValue AREA_TAINT_WASHER;
    private static final ModConfigSpec.DoubleValue AREA_TAINT_ESSENCE_MACHINES;
    private static final ModConfigSpec.DoubleValue AREA_TAINT_RITUAL;
    private static final ModConfigSpec.DoubleValue AREA_TAINT_CONJURER;
    private static final ModConfigSpec.DoubleValue AREA_TAINT_FILTER;
    private static final ModConfigSpec.IntValue AREA_TAINT_FILTER_FE;
    private static final ModConfigSpec.IntValue AREA_TAINT_FILTER_LEAF;
    private static final ModConfigSpec.DoubleValue AREA_TAINT_PURE_GENERATOR;
    private static final ModConfigSpec.DoubleValue AREA_TAINT_PURIFY;
    private static final ModConfigSpec.BooleanValue AREA_TAINT_WORLD_EFFECTS;

    // ----- Poço de Mana e Sangria Arcana -----
    private static final ModConfigSpec.IntValue WELL_FE_PER_MANA;
    private static final ModConfigSpec.IntValue WELL_ESSENCE_PER_MANA;
    private static final ModConfigSpec.DoubleValue WELL_AURA;
    private static final ModConfigSpec.IntValue WELL_RADIUS;
    private static final ModConfigSpec.IntValue WELL_COOLDOWN;
    private static final ModConfigSpec.DoubleValue WELL_AFFINITY_BONUS;
    private static final ModConfigSpec.DoubleValue SIPHON_MANA;
    private static final ModConfigSpec.IntValue SIPHON_FE_PER_MANA;
    private static final ModConfigSpec.DoubleValue SIPHON_FLOOR;
    private static final ModConfigSpec.DoubleValue SIPHON_TAINT;

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

        b.comment("Conjurador Rúnico (lança a magia de um Cristal de Feitiço)").push("conjurer");
        CONJURER_FE_PER_MANA = b.comment("FE gasto por ponto de mana da magia, a cada lançamento.").defineInRange("fePerMana", 40, 0, 10_000);
        CONJURER_ESSENCE_PER_TIER = b.comment("mB de Essência por tier da magia (só magias de tier 3 ou mais).").defineInRange("essencePerTier", 25, 0, 4000);
        CONJURER_COOLDOWN = b.comment("Multiplicador da recarga da magia no Conjurador.").defineInRange("cooldownMultiplier", 1.5, 0.1, 20.0);
        CONJURER_POWER = b.comment("Fração do poder do jogador que o Cristal guarda ao ser gravado.").defineInRange("powerFactor", 0.75, 0.05, 4.0);
        CONJURER_ENGRAVE_MANA = b.comment("Gravar um Cristal custa esta vezes a mana da magia.").defineInRange("engraveManaMultiplier", 5, 0, 100);
        b.pop();

        b.comment("Engastes de máquina (runas nos espaços da esquerda da tela)").push("machine_sockets");
        SOCKET_WEAR_TICKS = b.comment("Ticks de máquina trabalhando para cada runa engastada perder 1 uso (runas seladas no Santuário não gastam).")
                .defineInRange("wearTicks", 4800, 20, 1_000_000);
        b.pop();

        b.comment("Mácula do lugar (por chunk): quem solta, quem limpa e o que ela faz").push("area_taint");
        AREA_TAINT_ENABLED = b.comment("Liga a Mácula do lugar. Desligado, nada solta nem limpa e os efeitos param.").define("enabled", true);
        AREA_TAINT_REACTOR = b.comment("Mácula por minuto do Reator Impuro ligado.").defineInRange("impureReactor", 3.0, 0.0, 100.0);
        AREA_TAINT_WASHER = b.comment("Mácula por minuto da Lavadora de Essência trabalhando.").defineInRange("essenceWasher", 1.0, 0.0, 100.0);
        AREA_TAINT_ESSENCE_MACHINES = b.comment("Mácula por minuto do Extrator, Compressor e Liquefator (Rúnicos e Arcanos) trabalhando.")
                .defineInRange("essenceMachines", 0.5, 0.0, 100.0);
        AREA_TAINT_RITUAL = b.comment("Mácula de uma vez a cada ritual concluído no Altar Impuro.").defineInRange("impureRitual", 5.0, 0.0, 100.0);
        AREA_TAINT_CONJURER = b.comment("Mácula a cada lançamento de magia de Escuridão ou Vazio no Conjurador.").defineInRange("conjurerDarkCast", 0.2, 0.0, 100.0);
        AREA_TAINT_FILTER = b.comment("Mácula tirada por minuto pelo Filtro de Arruda (no chunk dele e nos 8 em volta).").defineInRange("rueFilter", 2.0, 0.0, 100.0);
        AREA_TAINT_FILTER_FE = b.comment("FE por tick do Filtro de Arruda.").defineInRange("rueFilterFePerTick", 20, 0, 100_000);
        AREA_TAINT_FILTER_LEAF = b.comment("Ticks que uma Folha de Arruda dura no Filtro.").defineInRange("rueFilterLeafTicks", 2400, 20, 1_000_000);
        AREA_TAINT_PURE_GENERATOR = b.comment("Mácula tirada por minuto pelo Gerador Purificado ligado (só no chunk dele).").defineInRange("pureGenerator", 1.0, 0.0, 100.0);
        AREA_TAINT_PURIFY = b.comment("Mácula tirada de uma vez pela magia Purificar (jogador ou Conjurador).").defineInRange("purifySpell", 10.0, 0.0, 100.0);
        AREA_TAINT_WORLD_EFFECTS = b.comment("A Mácula alta muda blocos (Grama Murcha, Solo Maculado).").define("changeBlocks", true);
        b.pop();

        b.comment("Conversões de mana: Poço de Mana (FE + Essência Purificada → mana do jogador) e Sangria Arcana (mana → FE).",
                "O Carregador Arcano (FE → mana do cajado) usa 20 FE por ponto. Mantenha a Sangria abaixo do Poço para não virar geração infinita.")
                .push("mana_conversion");
        WELL_FE_PER_MANA = b.comment("Poço de Mana: FE por ponto de mana.").defineInRange("wellFePerMana", 20, 0, 100_000);
        WELL_ESSENCE_PER_MANA = b.comment("Poço de Mana: mB de Essência Purificada por ponto de mana.").defineInRange("wellEssencePerMana", 1, 0, 1000);
        WELL_AURA = b.comment("Poço de Mana: mana por segundo dada a cada jogador na aura (e o mesmo na reserva do cajado na mão).")
                .defineInRange("wellAuraPerSecond", 2.0, 0.0, 1000.0);
        WELL_RADIUS = b.comment("Poço de Mana: raio da aura, em blocos.").defineInRange("wellRadius", 6, 1, 32);
        WELL_COOLDOWN = b.comment("Poço de Mana: ticks de espera por jogador entre dois cliques de encher.").defineInRange("wellFillCooldown", 600, 0, 72_000);
        WELL_AFFINITY_BONUS = b.comment("Poço de Mana: rendimento extra com a runa do elemento no espaço de runa, para quem tem nível 5+ nele.")
                .defineInRange("wellAffinityBonus", 0.25, 0.0, 10.0);
        SIPHON_MANA = b.comment("Sangria Arcana: mana drenada por segundo enquanto o jogador segura o botão.").defineInRange("siphonManaPerSecond", 10.0, 0.1, 1000.0);
        SIPHON_FE_PER_MANA = b.comment("Sangria Arcana: FE por ponto de mana.").defineInRange("siphonFePerMana", 15, 0, 100_000);
        SIPHON_FLOOR = b.comment("Sangria Arcana: para quando a mana chega a esta fração do máximo.").defineInRange("siphonFloor", 0.2, 0.0, 1.0);
        SIPHON_TAINT = b.comment("Sangria Arcana: Mácula ganha a cada 100 de mana drenada, para quem está Corrompido ou pior (que rende 2×).")
                .defineInRange("siphonTaintPer100", 1.0, 0.0, 100.0);
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

    public static int conjurerFePerMana() { return i(CONJURER_FE_PER_MANA); }
    public static int conjurerEssencePerTier() { return i(CONJURER_ESSENCE_PER_TIER); }
    public static double conjurerCooldown() { return d(CONJURER_COOLDOWN); }
    public static double conjurerPower() { return d(CONJURER_POWER); }
    public static int conjurerEngraveMana() { return i(CONJURER_ENGRAVE_MANA); }

    public static int socketWearTicks() { return i(SOCKET_WEAR_TICKS); }

    public static boolean areaTaint() { return bool(AREA_TAINT_ENABLED); }
    public static float areaTaintReactor() { return (float) d(AREA_TAINT_REACTOR); }
    public static float areaTaintWasher() { return (float) d(AREA_TAINT_WASHER); }
    public static float areaTaintEssenceMachines() { return (float) d(AREA_TAINT_ESSENCE_MACHINES); }
    public static float areaTaintRitual() { return (float) d(AREA_TAINT_RITUAL); }
    public static float areaTaintConjurer() { return (float) d(AREA_TAINT_CONJURER); }
    public static float areaTaintFilter() { return (float) d(AREA_TAINT_FILTER); }
    public static int areaTaintFilterFe() { return i(AREA_TAINT_FILTER_FE); }
    public static int areaTaintFilterLeaf() { return i(AREA_TAINT_FILTER_LEAF); }
    public static float areaTaintPureGenerator() { return (float) d(AREA_TAINT_PURE_GENERATOR); }
    public static float areaTaintPurify() { return (float) d(AREA_TAINT_PURIFY); }
    public static boolean areaTaintBlocks() { return bool(AREA_TAINT_WORLD_EFFECTS); }

    public static int wellFePerMana() { return i(WELL_FE_PER_MANA); }
    public static int wellEssencePerMana() { return i(WELL_ESSENCE_PER_MANA); }
    public static float wellAura() { return (float) d(WELL_AURA); }
    public static int wellRadius() { return i(WELL_RADIUS); }
    public static int wellCooldown() { return i(WELL_COOLDOWN); }
    public static float wellAffinityBonus() { return (float) d(WELL_AFFINITY_BONUS); }
    public static float siphonManaPerSecond() { return (float) d(SIPHON_MANA); }
    public static int siphonFePerMana() { return i(SIPHON_FE_PER_MANA); }
    public static float siphonFloor() { return (float) d(SIPHON_FLOOR); }
    public static float siphonTaint() { return (float) d(SIPHON_TAINT); }

    public static int rangeScanRadius() { return i(RANGE_SCAN); }
    public static boolean rangeNeedsSneak() { return bool(RANGE_NEEDS_SNEAK); }
    public static int hologramAlpha() { return i(HOLOGRAM_ALPHA); }
    public static boolean hologramLabels() { return bool(HOLOGRAM_LABELS); }
    public static boolean sneakScrollCycles() { return bool(SCROLL_WHEEL); }
}

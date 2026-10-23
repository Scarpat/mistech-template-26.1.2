package com.mistech.mistechmod.machine;

import java.util.function.IntSupplier;

import com.mistech.mistechmod.Config;

/**
 * Melhorias Rúnicas das máquinas (aplicadas com botão direito na máquina; tiradas pela aba da tela).
 * <ul>
 * <li>Velocidade: +50% de progresso por melhoria (gasta um pouco mais por operação).</li>
 * <li>Eficiência: −15% de energia por melhoria.</li>
 * <li>Alcance: +1 de raio nas máquinas de área (Colheitadeira, Plantadeira).</li>
 * </ul>
 * Números no config do servidor ({@link Config}).
 */
public enum MachineUpgrade {
    SPEED("speed", Config::maxSpeedUpgrades),
    EFFICIENCY("efficiency", Config::maxEfficiencyUpgrades),
    RANGE("range", Config::maxRangeUpgrades);

    private final String id;
    private final IntSupplier max;

    MachineUpgrade(String id, IntSupplier max) {
        this.id = id;
        this.max = max;
    }

    public String id() {
        return this.id;
    }

    public int max() {
        return Math.min(7, this.max.getAsInt());
    }

    public net.minecraft.world.item.Item item() {
        return switch (this) {
            case SPEED -> com.mistech.mistechmod.registry.MachineContent.SPEED_UPGRADE.get();
            case EFFICIENCY -> com.mistech.mistechmod.registry.MachineContent.EFFICIENCY_UPGRADE.get();
            case RANGE -> com.mistech.mistechmod.registry.MachineContent.RANGE_UPGRADE.get();
        };
    }
}

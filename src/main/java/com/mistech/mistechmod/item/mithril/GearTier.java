package com.mistech.mistechmod.item.mithril;

import com.mistech.mistechmod.astrology.NatalChart;

import net.minecraft.ChatFormatting;

/**
 * Os níveis do equipamento revestido (todos gastam energia no lugar da durabilidade, ver {@link MithrilGear}).
 * <ul>
 * <li>Mitril (altar Tier I): base.</li>
 * <li>Adamantina (altar Tier II): 3 engastes de runa.</li>
 * <li>Oricalco (altar Tier III): 6 engastes, vínculo com constelação, fortalece Sol/Lua/Ascendente.</li>
 * <li>Oricalco Puro (Santuário): 8 engastes, vínculo com constelação, Sol bem mais forte, vínculo com o Tarô (futuro).</li>
 * </ul>
 */
public enum GearTier {
    MITHRIL("mithril", 200_000, 4_000, 0, false, 0.0, 0.0, false, 1.0, 0x7FD8FF, ChatFormatting.AQUA),
    ADAMANTINE("adamantine", 400_000, 6_000, 3, false, 0.0, 0.0, false, 1.0, 0xE8576A, ChatFormatting.RED),
    ORICHALCUM("orichalcum", 1_000_000, 10_000, 6, true, 0.10, 0.10, false, 1.25, 0xFFB04A, ChatFormatting.GOLD),
    PURE_ORICHALCUM("pure_orichalcum", 2_000_000, 20_000, 8, true, 0.25, 0.125, true, 1.5, 0xFFF1B8, ChatFormatting.YELLOW);

    private final String id;
    private final int capacity;
    private final int chargeRate;
    private final int runeSlots;
    private final boolean constellation;
    /** Quanto cada peça de armadura vestida aumenta o Sol (fração: 0,25 = +25%). */
    private final double sunBoost;
    /** Quanto cada peça de armadura vestida aumenta a Lua e o Ascendente. */
    private final double moonRisingBoost;
    private final boolean tarot;
    /** Quanto cada runa engastada nesta peça vale (Oricalco ×1,25, Oricalco Puro ×1,5). */
    private final double runeMultiplier;
    private final int barColor;
    private final ChatFormatting textColor;

    GearTier(String id, int capacity, int chargeRate, int runeSlots, boolean constellation, double sunBoost,
            double moonRisingBoost, boolean tarot, double runeMultiplier, int barColor, ChatFormatting textColor) {
        this.id = id;
        this.capacity = capacity;
        this.chargeRate = chargeRate;
        this.runeSlots = runeSlots;
        this.constellation = constellation;
        this.sunBoost = sunBoost;
        this.moonRisingBoost = moonRisingBoost;
        this.tarot = tarot;
        this.runeMultiplier = runeMultiplier;
        this.barColor = barColor;
        this.textColor = textColor;
    }

    public String id() {
        return this.id;
    }

    public int capacity() {
        return this.capacity;
    }

    public int chargeRate() {
        return this.chargeRate;
    }

    /** Quantas runas cabem na peça (Bigorna Rúnica). */
    public int runeSlots() {
        return this.runeSlots;
    }

    /** Pode ser vinculada a uma constelação na Mesa de Astrologia. */
    public boolean bindsConstellation() {
        return this.constellation;
    }

    /** Pode ser vinculada a um arcano derrotado na Mesa de Tarologia ({@link com.mistech.mistechmod.tarot.GearArcana}). */
    public boolean bindsTarot() {
        return this.tarot;
    }

    public double natalBoost(NatalChart.Part part) {
        return part == NatalChart.Part.SUN ? this.sunBoost : this.moonRisingBoost;
    }

    public double runeMultiplier() {
        return this.runeMultiplier;
    }

    /** Este nível é pelo menos tão alto quanto {@code other}? */
    public boolean atLeast(GearTier other) {
        return this.ordinal() >= other.ordinal();
    }

    public int barColor() {
        return this.barColor;
    }

    public ChatFormatting textColor() {
        return this.textColor;
    }
}

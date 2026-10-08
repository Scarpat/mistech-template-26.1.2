package com.mistech.mistechmod.mage;

import com.mistech.mistechmod.item.mithril.GearTier;

/**
 * Níveis das Vestes de Mago (segunda linha de armadura: pouca defesa, bônus de magia). Os números são POR PEÇA vestida
 * e carregada; os totais têm teto em {@link MageRobes}.
 */
public enum MageRobeTier {
    //          id          energia/engastes     custo FE  dano mág.  afinidade  Mácula  escudo  defesa (botas, calças, peito, capuz)  cor
    HEMP("hemp", GearTier.MITHRIL, 0.05, 0.00, 0.00, 0.00, 0.30, new int[] {1, 2, 3, 1}, 0.0F, 0x9C8A5A),
    INITIATE("initiate", GearTier.MITHRIL, 0.08, 0.05, 0.00, 0.00, 0.40, new int[] {1, 3, 5, 2}, 0.0F, 0x2E4F9C),
    ADEPT("adept", GearTier.ADAMANTINE, 0.10, 0.08, 0.10, 0.00, 0.50, new int[] {2, 5, 6, 2}, 0.0F, 0x6A2E8C),
    ARCHMAGE("archmage", GearTier.ORICHALCUM, 0.12, 0.12, 0.20, 0.15, 0.60, new int[] {2, 5, 7, 3}, 1.0F, 0x2A1C48);

    public final String id;
    public final GearTier gear;
    /** Quanto cada peça corta do custo de energia das magias (0,05 = −5%). */
    public final double costCut;
    /** Dano mágico extra por peça. */
    public final double magicDamage;
    /** Afinidade extra por peça. */
    public final double affinity;
    /** Quanto cada peça corta da Mácula recebida. */
    public final double taintCut;
    /** Fração do dano que o Escudo de Mana absorve (conjunto completo; vale o nível mais baixo vestido). */
    public final double shield;
    public final int[] defense;
    public final float toughness;
    public final int color;

    MageRobeTier(String id, GearTier gear, double costCut, double magicDamage, double affinity, double taintCut, double shield,
            int[] defense, float toughness, int color) {
        this.id = id;
        this.gear = gear;
        this.costCut = costCut;
        this.magicDamage = magicDamage;
        this.affinity = affinity;
        this.taintCut = taintCut;
        this.shield = shield;
        this.defense = defense;
        this.toughness = toughness;
        this.color = color;
    }
}

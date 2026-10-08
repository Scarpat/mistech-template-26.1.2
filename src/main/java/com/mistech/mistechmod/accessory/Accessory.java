package com.mistech.mistechmod.accessory;

/** Os anéis e amuletos (efeitos em {@link Accessories}). */
public enum Accessory {
    VEIL_RING("veil_ring", true),
    EMBER_RING("ember_ring", true),
    BREEZE_RING("breeze_ring", true),
    STONE_RING("stone_ring", true),
    TIDE_RING("tide_ring", true),
    FROST_RING("frost_ring", true),
    CONSTELLATION_AMULET("constellation_amulet", false),
    GNOME_AMULET("gnome_amulet", false),
    ESSENCE_AMULET("essence_amulet", false),
    // artefatos rúnicos (energia própria; efeitos em com.mistech.mistechmod.artifact.ArtifactEvents)
    VOID_MAGNET("void_magnet", false),
    POCKET_HOURGLASS("pocket_hourglass", false),
    SPATIAL_RING("spatial_ring", true),
    // Tarô: Forja do Mago (efeitos aqui em Accessories, no Cajado do Mago e na Afinidade)
    ARCANE_PENDANT("arcane_pendant", false),
    // Tarô: Forja da Sacerdotisa (efeito em com.mistech.mistechmod.tarot.priestess.VeilEvents)
    PRIESTESS_VEIL("priestess_veil", false),
    // Coroas da Rainha Duende (vão no espaço de amuleto; efeitos em com.mistech.mistechmod.fey.FeyEvents)
    CORRUPTED_CROWN("corrupted_crown", false),
    PURIFIED_CROWN("purified_crown", false);

    public final String id;
    public final boolean ring;

    Accessory(String id, boolean ring) {
        this.id = id;
        this.ring = ring;
    }
}

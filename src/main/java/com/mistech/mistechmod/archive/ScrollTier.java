package com.mistech.mistechmod.archive;

/** Pergaminhos Espaciais: quantos tipos e quantos itens cabem, e o custo de energia no Núcleo. */
public enum ScrollTier {
    COMMON("archive_scroll", 27, 4_096L, 2),
    MITHRIL("mithril_archive_scroll", 63, 32_768L, 5),
    ADAMANTINE("adamantine_archive_scroll", 63, 262_144L, 10),
    ORICHALCUM("orichalcum_archive_scroll", 127, 2_097_152L, 20),
    /** Um tipo só, quantidade praticamente ilimitada. */
    VOID("void_archive_scroll", 1, Long.MAX_VALUE / 4, 5);

    public final String id;
    public final int types;
    public final long capacity;
    /** FE por tick no Núcleo. */
    public final int energy;

    ScrollTier(String id, int types, long capacity, int energy) {
        this.id = id;
        this.types = types;
        this.capacity = capacity;
        this.energy = energy;
    }
}

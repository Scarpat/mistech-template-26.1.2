package com.mistech.mistechmod.conduit;

/**
 * Níveis do conduto: a cada {@link #interval} ticks cada ponta que puxa move até {@link #items} itens e {@link #fluid} mB;
 * energia anda todo tick (até {@link #energy} FE/t por ponta). Numa rede com níveis misturados vale o mais lento.
 */
public enum ConduitTier {
    RUNIC("runic_conduit", 20, 8, 500, 2_000),
    MITHRIL("mithril_conduit", 10, 16, 1_000, 8_000),
    ADAMANTINE("adamantine_conduit", 5, 32, 4_000, 32_000),
    ORICHALCUM("orichalcum_conduit", 2, 64, 16_000, 128_000);

    public final String id;
    public final int interval, items, fluid, energy;

    ConduitTier(String id, int interval, int items, int fluid, int energy) {
        this.id = id;
        this.interval = interval;
        this.items = items;
        this.fluid = fluid;
        this.energy = energy;
    }
}

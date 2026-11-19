package com.mistech.mistechmod.conduit;

/** O que o conduto leva (agachar + clique no miolo troca). Só passa por um conduto o que ele leva. */
public enum ConduitCarry {
    ENERGY(true, false, false, 0xFFD23A), ITEMS(false, true, false, 0x5AE05A), FLUIDS(false, false, true, 0x3A8AFF),
    ENERGY_FLUIDS(true, false, true, 0xB05CFF), ENERGY_ITEMS(true, true, false, 0xFF8A2A), FLUIDS_ITEMS(false, true, true, 0x3AE0E0),
    ALL(true, true, true, 0xF0F0FF);

    public final boolean energy, items, fluids;
    /** Cor da runa do miolo (energia amarela, itens verde, fluidos azul; misturas: roxo, laranja, ciano; tudo branco). */
    public final int color;

    ConduitCarry(boolean energy, boolean items, boolean fluids, int color) {
        this.energy = energy;
        this.items = items;
        this.fluids = fluids;
        this.color = color;
    }

    public boolean carries(Kind kind) {
        return switch (kind) {
            case ENERGY -> this.energy;
            case ITEMS -> this.items;
            case FLUIDS -> this.fluids;
        };
    }

    public ConduitCarry next() {
        return values()[(this.ordinal() + 1) % values().length];
    }

    public String key() {
        return "conduit.mistech.carry." + this.name().toLowerCase(java.util.Locale.ROOT);
    }

    public enum Kind {
        ENERGY, ITEMS, FLUIDS
    }
}

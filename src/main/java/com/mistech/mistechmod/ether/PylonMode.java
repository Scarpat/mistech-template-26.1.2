package com.mistech.mistechmod.ether;

import net.minecraft.util.StringRepresentable;

/** Modo do Pilar de Transmissão (cor do cristal): só repassa, puxa do bloco embaixo ou entrega no bloco embaixo. */
public enum PylonMode implements StringRepresentable {
    RELAY("relay"), INPUT("input"), OUTPUT("output");

    private final String name;

    PylonMode(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public PylonMode next() {
        return values()[(this.ordinal() + 1) % values().length];
    }
}

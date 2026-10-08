package com.mistech.mistechmod.block.confector;

import net.minecraft.util.StringRepresentable;

/** As 4 partes do Confector de Runas (2 de largura x 2 de altura). A parte TABLE é a principal (tem o block entity). */
public enum ConfectorPart implements StringRepresentable {
    TABLE("table"),
    BARREL("barrel"),
    TABLE_TANK("table_tank"),
    BARREL_TANK("barrel_tank");

    private final String name;

    ConfectorPart(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public boolean isTank() {
        return this == TABLE_TANK || this == BARREL_TANK;
    }

    /** Qual tanque do Confector esta parte mostra (0 = em cima do barril, 1 = em cima da mesa). */
    public int tankIndex() {
        return this == BARREL_TANK ? 0 : 1;
    }
}

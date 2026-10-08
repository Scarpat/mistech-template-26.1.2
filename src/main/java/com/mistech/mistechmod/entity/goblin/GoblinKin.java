package com.mistech.mistechmod.entity.goblin;

import net.minecraft.world.entity.Entity;

/** Criaturas da Vila dos Duendes: não se atacam entre si (e as magias da Rainha não acertam os aliados). */
public interface GoblinKin {
    static boolean allied(Entity a, Entity b) {
        return a instanceof GoblinKin && b instanceof GoblinKin;
    }
}

package com.mistech.mistechmod.spellcraft;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.level.Level;

/** Lobo Sombrio invocado: lobo domado com pelagem de sombra e olhos violeta (renderer próprio). */
public class ShadowWolfEntity extends Wolf {
    public ShadowWolfEntity(EntityType<? extends Wolf> type, Level level) {
        super(type, level);
    }
}

package com.mistech.mistechmod.golem;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

/** Golem que recebe rota da Vara de Comando (Golem de Barro e golems de trabalho). */
public interface RoutedGolem {
    void setRoute(BlockPos source, BlockPos target);

    boolean hasRoute();

    void showRoute(ServerLevel level);

    Component describe();
}

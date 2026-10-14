package com.mistech.mistechmod.golem;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

/** Golem que recebe rota da Vara de Comando (Golem de Barro e golems de trabalho). */
public interface RoutedGolem {
    void setRoute(BlockPos source, BlockPos target);

    /** Vara de Comando Rúnica: várias origens/destinos. Quem não entende usa só a primeira origem e o primeiro destino. */
    default void setRoute(GolemRoute route) {
        if (route.isComplete()) {
            this.setRoute(route.sources().get(0), route.targets().get(0));
        }
    }

    /** A rota atual (para copiar de volta para a vara). */
    default GolemRoute getRoute() {
        return GolemRoute.EMPTY;
    }

    boolean hasRoute();

    void showRoute(ServerLevel level);

    Component describe();
}

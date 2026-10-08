package com.mistech.mistechmod.tarot.magus;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.tarot.ArcanePlane;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;

/** Quem conjura as magias do Mago (o próprio Mago e os Aspectos). */
public interface MagusCaster {
    Mob self();

    /** Centro do Salão Arcano (null fora da arena, ex.: ovo gerador). */
    @Nullable BlockPos arenaCenter();

    /** Multiplicador de dano das magias (Aspectos batem menos; cargas da Água aumentam). */
    float power();

    /** Agenda algo para daqui a {@code delay} ticks (cancelado se o conjurador morrer). */
    void schedule(int delay, Runnable task);

    /** Estado da arena (só no Plano Arcano). */
    default MagusArena.@Nullable Field field() {
        BlockPos c = this.arenaCenter();
        return c != null && ArcanePlane.isPlane(this.self().level()) ? MagusArena.field(c) : null;
    }
}

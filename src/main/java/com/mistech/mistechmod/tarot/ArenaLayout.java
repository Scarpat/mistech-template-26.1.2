package com.mistech.mistechmod.tarot;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Formato da arena de um arcano no Plano Arcano. Cada arcano pode ter a sua ({@link ArcanaBosses#registerArena});
 * sem registro vale a arena circular padrão ({@link CircleArena}).
 * <p>
 * Todas as posições são relativas ao {@code center} da luta (bloco do chão no meio da arena, y = {@link ArcanePlane#ARENA_Y}).
 */
public interface ArenaLayout {
    /** Constrói (ou reconstrói) a arena. Deve limpar sobras de outras arenas em {@link #clearRadius()}. */
    void build(ServerLevel level, BlockPos center);

    /** Onde o jogador número {@code index} do grupo (0, 1, 2...) chega. */
    Vec3 arrival(BlockPos center, int index);

    /** Onde o chefe aparece. */
    default Vec3 bossSpawn(BlockPos center) {
        return Vec3.atBottomCenterOf(center.above());
    }

    /** Distância horizontal do centro além da qual o chefe é puxado de volta. */
    double leashRadius();

    /** Altura (relativa ao centro) abaixo da qual o chefe é puxado de volta. */
    default double leashBelow() {
        return -6.0;
    }

    /** Raio horizontal que a arena ocupa (a limpeza usa pelo menos {@link ArenaTools#MAX_CLEAR_RADIUS}). */
    int clearRadius();

    /** Chamado todo tick da luta (efeitos da arena: correntes de ar etc.). */
    default void tick(ServerLevel level, ArcanePlane.Fight fight, List<ServerPlayer> players) {}

    /**
     * O jogador caiu abaixo de {@link #fallLimit()}: devolver true se a arena "pegou" o jogador (não conta como queda
     * no vazio). Falso = cai no vazio e morre (o Plano Arcano trata como derrota).
     */
    default boolean catchFall(ServerLevel level, ArcanePlane.Fight fight, ServerPlayer player) {
        return false;
    }

    /** Altura (relativa ao centro) abaixo da qual {@link #catchFall} é chamado. */
    default double fallLimit() {
        return -30.0;
    }
}

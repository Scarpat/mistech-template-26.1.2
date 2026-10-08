package com.mistech.mistechmod.client;

import net.minecraft.client.renderer.entity.state.HoldingEntityRenderState;

/** heldItem (herdado) = o item que o golem está carregando. */
public class ClayGolemRenderState extends HoldingEntityRenderState {
    /** Carregando itens: braços levantados segurando a carga. */
    public boolean carrying;
    /** Golem de trabalho: índice do {@link com.mistech.mistechmod.golem.GolemKind} (o de barro ignora). */
    public int kind;
}

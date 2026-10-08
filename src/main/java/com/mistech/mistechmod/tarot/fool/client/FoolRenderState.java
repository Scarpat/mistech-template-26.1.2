package com.mistech.mistechmod.tarot.fool.client;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** Estado de desenho do Louco: ação atual (ver {@link com.mistech.mistechmod.tarot.fool.FoolEntity}) e há quanto tempo. */
public class FoolRenderState extends LivingEntityRenderState {
    public int action;
    public float actionTime;
    public boolean phase2;
}

package com.mistech.mistechmod.tarot.priestess.client;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** Estado de desenho da Sacerdotisa (e dos reflexos, que são iguais). */
public class PriestessRenderState extends LivingEntityRenderState {
    public int action;
    public float actionTime;
    public boolean seated;
}

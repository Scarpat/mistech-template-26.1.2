package com.mistech.mistechmod.tarot.magus.client;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** Estado de desenho do Mago e dos Aspectos. */
public class MagusRenderState extends LivingEntityRenderState {
    /** Forma (ordinal de {@link com.mistech.mistechmod.tarot.magus.MagusElement}). */
    public int form;
    /** Segundo elemento da mistura (Fase II) ou -1. */
    public int mix = -1;
    public int action;
    public float actionTime;
    public int phase = 1;
    public float shield;
    public int charges;
    public boolean aspect;
    /** Cópia translúcida e de cabeça para baixo (Fase IV). */
    public boolean reflection;
    public float mirrorY;
    public double entityY;

    public void copyFrom(MagusRenderState o) {
        this.form = o.form;
        this.mix = o.mix;
        this.action = o.action;
        this.actionTime = o.actionTime;
        this.phase = o.phase;
        this.shield = o.shield;
        this.charges = o.charges;
        this.aspect = o.aspect;
        this.ageInTicks = o.ageInTicks;
        this.yRot = o.yRot;
        this.xRot = o.xRot;
        this.bodyRot = o.bodyRot;
        this.walkAnimationPos = o.walkAnimationPos;
        this.walkAnimationSpeed = o.walkAnimationSpeed;
        this.lightCoords = o.lightCoords;
        this.mirrorY = o.mirrorY;
        this.entityY = o.entityY;
    }
}

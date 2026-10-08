package com.mistech.mistechmod.folklore.client;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.phys.Vec3;

public class BoitataRenderState extends LivingEntityRenderState {
    /** Centros dos segmentos, relativos à cabeça. */
    public Vec3[] segments = new Vec3[0];
    public float[] segmentYaw = new float[0];
    public float[] segmentPitch = new float[0];
    /** 0–1: abaixo da metade da vida abre mais a boca. */
    public float rage;
}

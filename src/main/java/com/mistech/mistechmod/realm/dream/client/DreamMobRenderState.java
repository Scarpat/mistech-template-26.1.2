package com.mistech.mistechmod.realm.dream.client;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** Estado de render das criaturas do Sonho. */
public class DreamMobRenderState extends LivingEntityRenderState {
    public boolean sheared;
    public boolean watched;
    public boolean bright;
    public float attackTime;
    /** Fase 3: cor do Peixe-do-Ar, alarme do Vigia, Sonhador acordado. */
    public int variant;
    public boolean alert;
    public boolean awake;
    /** Baleia-Nuvem selada. */
    public boolean saddled;
}

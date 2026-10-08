package com.mistech.mistechmod.client;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** Estado de render dos mobs da Vila dos Duendes (duende, rainha, troll, aberração). */
public class GoblinRenderState extends LivingEntityRenderState {
    /** 0..1 durante o golpe. */
    public float attackAnim;
    /** 0..1 enquanto a Rainha conjura. */
    public float castAnim;
    /** Duende invocado pela Coroa Purificada (textura dourada). */
    public boolean pure;
}

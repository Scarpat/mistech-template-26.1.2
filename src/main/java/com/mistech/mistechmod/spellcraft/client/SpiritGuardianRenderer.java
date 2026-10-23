package com.mistech.mistechmod.spellcraft.client;

import com.mistech.mistechmod.Mistech;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.VexRenderer;
import net.minecraft.client.renderer.entity.state.VexRenderState;
import net.minecraft.resources.Identifier;

/** Espírito Guardião: forma do Vex, em azul-alma translúcido. */
public class SpiritGuardianRenderer extends VexRenderer {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Mistech.MODID, "textures/entity/spirit_guardian.png");
    private static final Identifier CHARGING = Identifier.fromNamespaceAndPath(Mistech.MODID, "textures/entity/spirit_guardian_charging.png");

    public SpiritGuardianRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public Identifier getTextureLocation(VexRenderState state) {
        return state.isCharging ? CHARGING : TEXTURE;
    }
}

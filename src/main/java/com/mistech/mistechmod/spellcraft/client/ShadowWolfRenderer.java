package com.mistech.mistechmod.spellcraft.client;

import com.mistech.mistechmod.Mistech;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.WolfRenderer;
import net.minecraft.client.renderer.entity.state.WolfRenderState;
import net.minecraft.resources.Identifier;

/** Lobo Sombrio: forma do lobo, 25% maior, pelagem de sombra e olhos violeta. */
public class ShadowWolfRenderer extends WolfRenderer {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Mistech.MODID, "textures/entity/shadow_wolf.png");

    public ShadowWolfRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public Identifier getTextureLocation(WolfRenderState state) {
        return TEXTURE;
    }

    @Override
    protected void scale(WolfRenderState state, PoseStack poseStack) {
        super.scale(state, poseStack);
        poseStack.scale(1.25F, 1.25F, 1.25F);
    }
}

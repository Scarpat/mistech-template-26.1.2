package com.mistech.mistechmod.client;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.entity.GnomeKingEntity;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

/** Rei Gnomo: o mesmo modelo do gnomo, 1,6x maior, chapéu roxo com coroa dourada e cetro de ouro. */
public class GnomeKingRenderer extends MobRenderer<GnomeKingEntity, GnomeRenderState, GnomeModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Mistech.MODID, "textures/entity/gnome_king.png");
    private static final float SCALE = 1.6F;

    public GnomeKingRenderer(EntityRendererProvider.Context context) {
        super(context, new GnomeModel(context.bakeLayer(GnomeModel.LAYER)), 0.45F);
    }

    @Override
    public GnomeRenderState createRenderState() {
        return new GnomeRenderState();
    }

    @Override
    protected void scale(GnomeRenderState state, PoseStack poseStack) {
        poseStack.scale(SCALE, SCALE, SCALE);
    }

    @Override
    public Identifier getTextureLocation(GnomeRenderState state) {
        return TEXTURE;
    }
}

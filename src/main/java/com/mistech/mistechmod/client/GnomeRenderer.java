package com.mistech.mistechmod.client;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.entity.GnomeEntity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public class GnomeRenderer extends MobRenderer<GnomeEntity, GnomeRenderState, GnomeModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Mistech.MODID, "textures/entity/gnome.png");

    public GnomeRenderer(EntityRendererProvider.Context context) {
        super(context, new GnomeModel(context.bakeLayer(GnomeModel.LAYER)), 0.3F);
    }

    @Override
    public GnomeRenderState createRenderState() {
        return new GnomeRenderState();
    }

    @Override
    public void extractRenderState(GnomeEntity entity, GnomeRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.attackAnim = entity.getAttackAnim(partialTicks);
    }

    @Override
    public Identifier getTextureLocation(GnomeRenderState state) {
        return TEXTURE;
    }
}

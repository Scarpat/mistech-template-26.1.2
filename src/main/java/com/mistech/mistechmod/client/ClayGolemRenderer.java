package com.mistech.mistechmod.client;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.entity.ClayGolemEntity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public class ClayGolemRenderer extends MobRenderer<ClayGolemEntity, ClayGolemRenderState, ClayGolemModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Mistech.MODID, "textures/entity/clay_golem.png");

    public ClayGolemRenderer(EntityRendererProvider.Context context) {
        super(context, new ClayGolemModel(context.bakeLayer(ClayGolemModel.LAYER)), 0.3F);
        this.addLayer(new ClayGolemCarriedItemLayer(this));
    }

    @Override
    public ClayGolemRenderState createRenderState() {
        return new ClayGolemRenderState();
    }

    @Override
    public void extractRenderState(ClayGolemEntity entity, ClayGolemRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.carrying = !entity.getCarried().isEmpty();
        this.itemModelResolver.updateForLiving(state.heldItem, entity.getCarried(),
                net.minecraft.world.item.ItemDisplayContext.GROUND, entity);
    }

    @Override
    public Identifier getTextureLocation(ClayGolemRenderState state) {
        return TEXTURE;
    }
}

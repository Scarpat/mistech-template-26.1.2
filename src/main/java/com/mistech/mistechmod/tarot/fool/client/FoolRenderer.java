package com.mistech.mistechmod.tarot.fool.client;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.tarot.fool.FoolEntity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/** O Louco, com olhos e guizos brilhando. */
public class FoolRenderer extends MobRenderer<FoolEntity, FoolRenderState, FoolModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Mistech.MODID, "textures/entity/the_fool.png");
    private static final RenderType GLOW = RenderTypes.eyes(Identifier.fromNamespaceAndPath(Mistech.MODID, "textures/entity/the_fool_glow.png"));

    public FoolRenderer(EntityRendererProvider.Context context) {
        super(context, new FoolModel(context.bakeLayer(FoolModel.LAYER)), 0.6F);
        this.addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return GLOW;
            }
        });
    }

    @Override
    public FoolRenderState createRenderState() {
        return new FoolRenderState();
    }

    @Override
    public void extractRenderState(FoolEntity entity, FoolRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.action = entity.action();
        state.actionTime = entity.actionTime(partialTicks);
        state.phase2 = entity.isPhase2();
    }

    @Override
    public Identifier getTextureLocation(FoolRenderState state) {
        return TEXTURE;
    }
}

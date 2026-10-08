package com.mistech.mistechmod.tarot.priestess.client;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.tarot.priestess.PriestessFigure;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;

/**
 * Desenha a Sacerdotisa e os reflexos com o mesmo modelo e a mesma textura (olhos, lua e cruz brilhando).
 * A única diferença é a <b>sombra</b>: só a verdadeira tem ({@code shadow} > 0).
 */
public class PriestessRenderer<T extends Mob & PriestessFigure> extends MobRenderer<T, PriestessRenderState, PriestessModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Mistech.MODID, "textures/entity/the_high_priestess.png");
    private static final RenderType GLOW = RenderTypes.eyes(Identifier.fromNamespaceAndPath(Mistech.MODID, "textures/entity/the_high_priestess_glow.png"));
    public static final float SCALE = 1.2F;

    public PriestessRenderer(EntityRendererProvider.Context context, float shadow) {
        super(context, new PriestessModel(context.bakeLayer(PriestessModel.LAYER)), shadow);
        this.addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return GLOW;
            }
        });
    }

    @Override
    public PriestessRenderState createRenderState() {
        return new PriestessRenderState();
    }

    @Override
    public void extractRenderState(T entity, PriestessRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.action = entity.figureAction();
        state.actionTime = entity.figureActionTime(partialTicks);
        state.seated = entity.seated();
    }

    @Override
    protected void scale(PriestessRenderState state, PoseStack poseStack) {
        poseStack.scale(SCALE, SCALE, SCALE);
    }

    @Override
    public Identifier getTextureLocation(PriestessRenderState state) {
        return TEXTURE;
    }
}

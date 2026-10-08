package com.mistech.mistechmod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;

/** Desenha o item que o Golem de Barro carrega, seguro entre as mãos na frente do peito. */
public class ClayGolemCarriedItemLayer extends RenderLayer<ClayGolemRenderState, ClayGolemModel> {
    public ClayGolemCarriedItemLayer(RenderLayerParent<ClayGolemRenderState, ClayGolemModel> renderer) {
        super(renderer);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, ClayGolemRenderState state, float yRot, float xRot) {
        ItemStackRenderState item = state.heldItem;
        if (item.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        // espaço do modelo: y cresce para baixo, 1 unidade = 16 px; frente do peito, na altura das mãos levantadas
        float bob = Mth.sin(state.walkAnimationPos * 0.8F) * 0.02F * Math.min(1.0F, state.walkAnimationSpeed);
        poseStack.translate(0.0F, 16.5F / 16.0F + bob, -5.0F / 16.0F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F)); // o modelo é desenhado de cabeça para baixo
        poseStack.scale(1.25F, 1.25F, 1.25F);
        item.submit(poseStack, collector, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();
    }
}

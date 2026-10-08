package com.mistech.mistechmod.artifact.client;

import com.mistech.mistechmod.artifact.BroomEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;

/**
 * Desenha a vassoura montada com o modelo 3D do item (contexto NONE → sem transformação de exibição):
 * o modelo é em pé (cabo em +Y, palha embaixo, assento em y=8); aqui ele é deitado com o cabo para a frente.
 */
public class BroomRenderer extends EntityRenderer<BroomEntity, BroomRenderer.State> {
    private final ItemModelResolver itemModelResolver;

    public BroomRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemModelResolver = context.getItemModelResolver();
        this.shadowRadius = 0.3F;
    }

    public static class State extends EntityRenderState {
        public final ItemStackRenderState item = new ItemStackRenderState();
        public float yRot;
        public float bob;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(BroomEntity broom, State state, float partialTick) {
        super.extractRenderState(broom, state, partialTick);
        state.yRot = broom.getYRot(partialTick);
        state.bob = broom.isVehicle() ? 0.0F : Mth.sin((broom.tickCount + partialTick) * 0.1F) * 0.04F;
        this.itemModelResolver.updateForTopItem(state.item, broom.getItem(), ItemDisplayContext.NONE, broom.level(), null, broom.getId());
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.item.isEmpty()) {
            poseStack.pushPose();
            poseStack.translate(0.0F, 0.25F + state.bob, 0.0F);
            poseStack.mulPose(Axis.YP.rotationDegrees(-state.yRot)); // +Z local = para onde a vassoura olha
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));       // +Y do modelo (ponta do cabo) vira a frente
            poseStack.scale(0.9F, 0.9F, 0.9F);
            state.item.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
        super.submit(state, poseStack, collector, camera);
    }
}

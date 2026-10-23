package com.mistech.mistechmod.spellcraft.client;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.spellcraft.ArcaneChargerBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;

/** O Cajado guardado flutua, em pé e girando devagar, em cima do Carregador Arcano. */
public class ArcaneChargerRenderer implements BlockEntityRenderer<ArcaneChargerBlockEntity, ArcaneChargerRenderer.State> {
    private final ItemModelResolver itemModelResolver;

    public ArcaneChargerRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    public static class State extends BlockEntityRenderState {
        public final ItemStackRenderState itemState = new ItemStackRenderState();
        public float time;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ArcaneChargerBlockEntity be, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, cameraPosition, breakProgress);
        long gameTime = be.getLevel() != null ? be.getLevel().getGameTime() : 0;
        state.time = gameTime + partialTicks;
        this.itemModelResolver.updateForTopItem(state.itemState, be.getStaff(), ItemDisplayContext.GROUND, be.getLevel(), null, 0);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.itemState.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0.5F, 1.35F + Mth.sin(state.time * 0.07F) * 0.06F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.time * 1.5F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(45.0F)); // a haste diagonal fica em pé
        poseStack.scale(1.7F, 1.7F, 1.7F);
        state.itemState.submit(poseStack, collector, 15728880, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }
}

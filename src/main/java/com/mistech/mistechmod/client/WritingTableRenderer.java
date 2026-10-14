package com.mistech.mistechmod.client;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.block.WritingTableBlockEntity;
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

/** Mesa de Escrita: enquanto alguém escreve, o item que vai sair flutua girando em cima da mesa. */
public class WritingTableRenderer implements BlockEntityRenderer<WritingTableBlockEntity, WritingTableRenderer.State> {
    /** Altura do item (a mesa tem ~1 bloco). */
    private static final float HEIGHT = 1.35F;

    private final ItemModelResolver itemModelResolver;

    public WritingTableRenderer(BlockEntityRendererProvider.Context context) {
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
    public void extractRenderState(WritingTableBlockEntity be, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, cameraPosition, breakProgress);
        long gameTime = be.getLevel() != null ? be.getLevel().getGameTime() : 0;
        state.time = gameTime + partialTicks + (be.getBlockPos().asLong() & 63);
        this.itemModelResolver.updateForTopItem(state.itemState, be.getPreview(), ItemDisplayContext.GROUND, be.getLevel(), null, 0);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.itemState.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0.5F, HEIGHT + Mth.sin(state.time * 0.1F) * 0.06F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.time * 2.5F));
        poseStack.scale(0.85F, 0.85F, 0.85F);
        state.itemState.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }
}

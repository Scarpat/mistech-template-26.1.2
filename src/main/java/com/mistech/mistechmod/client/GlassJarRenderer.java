package com.mistech.mistechmod.client;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.block.GlassJarBlockEntity;
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
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Desenha o item guardado dentro do Pote de Vidro (mais cópias quanto mais cheio), girando devagar. */
public class GlassJarRenderer implements BlockEntityRenderer<GlassJarBlockEntity, GlassJarRenderer.State> {
    private final ItemModelResolver itemModelResolver;

    public GlassJarRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    public static class State extends BlockEntityRenderState {
        public final ItemStackRenderState itemState = new ItemStackRenderState();
        public int copies;
        public float spin;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(GlassJarBlockEntity jar, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(jar, state, partialTicks, cameraPosition, breakProgress);
        ItemStack stored = jar.getStored();
        // 1 cópia até 1/4 cheio, até 4 cópias quando cheio
        state.copies = stored.isEmpty() ? 0 : 1 + Math.min(3, (stored.getCount() - 1) * 4 / Math.max(1, stored.getMaxStackSize()));
        long time = jar.getLevel() != null ? jar.getLevel().getGameTime() : 0;
        state.spin = (time + partialTicks) * 1.5F;
        this.itemModelResolver.updateForTopItem(state.itemState, stored, ItemDisplayContext.FIXED, jar.getLevel(), null, 0);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.copies <= 0 || state.itemState.isEmpty()) {
            return;
        }
        for (int i = 0; i < state.copies; i++) {
            poseStack.pushPose();
            // empilha de baixo para cima dentro do vidro (altura útil ~0,05..0,62)
            poseStack.translate(0.5F, 0.16F + i * 0.13F, 0.5F);
            poseStack.mulPose(Axis.YP.rotationDegrees(state.spin + i * 47.0F));
            poseStack.scale(0.42F, 0.42F, 0.42F);
            state.itemState.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
    }
}

package com.mistech.mistechmod.client;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.block.RunicTankBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;

/** Desenha o fluido dentro do Tanque Rúnico (altura = quanto está cheio). */
public class RunicTankRenderer implements BlockEntityRenderer<RunicTankBlockEntity, RunicTankRenderer.State> {
    public RunicTankRenderer(BlockEntityRendererProvider.Context context) {
    }

    public static class State extends BlockEntityRenderState {
        public Fluid fluid = Fluids.EMPTY;
        public float fill;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(RunicTankBlockEntity tank, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(tank, state, partialTicks, cameraPosition, breakProgress);
        state.fluid = tank.getTank().isEmpty() ? Fluids.EMPTY : tank.getTank().getFluid().getFluid();
        state.fill = tank.getTank().getAmount() / (float) RunicTankBlockEntity.CAPACITY;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        submitTankFluid(poseStack, collector, state.fluid, state.fill, state.lightCoords);
    }

    /** Interior do tanque: 2,5..13,5 px nos lados, 2..14 px de altura. */
    public static void submitTankFluid(PoseStack poseStack, SubmitNodeCollector collector, Fluid fluid, float fill, int light) {
        float fillShown = fill > 0 ? Math.max(fill, 0.03F) : 0;
        FluidRenderHelper.submitBox(poseStack, collector, fluid, fillShown, light,
                2.6F / 16F, 2.0F / 16F, 2.6F / 16F, 13.4F / 16F, 14.0F / 16F, 13.4F / 16F);
    }
}

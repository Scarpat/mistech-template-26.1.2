package com.mistech.mistechmod.extraplanar.client;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.client.RunicTankRenderer;
import com.mistech.mistechmod.extraplanar.ExtraplanarTankBlockEntity;
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

/** Desenha o fluido dentro do Tanque Extraplanar (cópia que o servidor manda para o cliente). */
public class ExtraplanarTankRenderer implements BlockEntityRenderer<ExtraplanarTankBlockEntity, ExtraplanarTankRenderer.State> {
    public ExtraplanarTankRenderer(BlockEntityRendererProvider.Context context) {
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
    public void extractRenderState(ExtraplanarTankBlockEntity tank, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(tank, state, partialTicks, cameraPosition, breakProgress);
        state.fluid = tank.getView().isEmpty() ? Fluids.EMPTY : tank.getView().getFluid();
        state.fill = tank.getFill();
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        RunicTankRenderer.submitTankFluid(poseStack, collector, state.fluid, state.fill, state.lightCoords);
    }
}

package com.mistech.mistechmod.client;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.block.RunicTankBlockEntity;
import com.mistech.mistechmod.block.confector.ConfectorPart;
import com.mistech.mistechmod.block.confector.RuneConfectorBlock;
import com.mistech.mistechmod.block.confector.RuneConfectorBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Desenha o fluido dos 2 tanques de cima do Confector (o block entity fica na parte de baixo, a da mesa). */
public class RuneConfectorRenderer implements BlockEntityRenderer<RuneConfectorBlockEntity, RuneConfectorRenderer.State> {
    public RuneConfectorRenderer(BlockEntityRendererProvider.Context context) {
    }

    public static class State extends BlockEntityRenderState {
        public final Fluid[] fluids = {Fluids.EMPTY, Fluids.EMPTY};
        public final float[] fills = new float[2];
        public final int[] lights = new int[2];
        /** Deslocamento (x, y, z) de cada tanque em relação ao block entity. */
        public final int[][] offsets = new int[2][3];
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(RuneConfectorBlockEntity be, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, cameraPosition, breakProgress);
        BlockState blockState = be.getBlockState();
        if (!blockState.hasProperty(RuneConfectorBlock.FACING)) {
            return;
        }
        Direction facing = blockState.getValue(RuneConfectorBlock.FACING);
        boolean mirrored = blockState.getValue(RuneConfectorBlock.MIRRORED);
        BlockPos master = be.getBlockPos();
        for (int i = 0; i < 2; i++) {
            ConfectorPart part = i == 0 ? ConfectorPart.BARREL_TANK : ConfectorPart.TABLE_TANK;
            BlockPos tankPos = RuneConfectorBlock.partPos(master, facing, mirrored, part);
            state.offsets[i][0] = tankPos.getX() - master.getX();
            state.offsets[i][1] = tankPos.getY() - master.getY();
            state.offsets[i][2] = tankPos.getZ() - master.getZ();
            var tank = be.getTank(i);
            state.fluids[i] = tank.isEmpty() ? Fluids.EMPTY : tank.getFluid().getFluid();
            state.fills[i] = tank.getAmount() / (float) RuneConfectorBlockEntity.TANK_CAPACITY;
            state.lights[i] = be.getLevel() != null ? LevelRenderer.getLightCoords(be.getLevel(), tankPos) : state.lightCoords;
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        for (int i = 0; i < 2; i++) {
            poseStack.pushPose();
            poseStack.translate(state.offsets[i][0], state.offsets[i][1], state.offsets[i][2]);
            // vidro do tanque do modelo do Confector: 3,5..12,5 px nos lados, 1..13 px de altura no bloco de cima
            float fill = state.fills[i] > 0 ? Math.max(state.fills[i], 0.03F) : 0;
            FluidRenderHelper.submitBox(poseStack, collector, state.fluids[i], fill, state.lights[i],
                    3.7F / 16F, 1.05F / 16F, 3.7F / 16F, 12.3F / 16F, 12.95F / 16F, 12.3F / 16F);
            poseStack.popPose();
        }
    }

    @Override
    public AABB getRenderBoundingBox(RuneConfectorBlockEntity be) {
        return new AABB(be.getBlockPos()).inflate(1.0, 0.0, 1.0).expandTowards(0.0, 1.0, 0.0);
    }
}

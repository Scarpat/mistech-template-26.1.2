package com.mistech.mistechmod.spellcraft.conjurer.client;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.machine.MachineBlock;
import com.mistech.mistechmod.spellcraft.conjurer.RunicConjurerBlockEntity;
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

/** O Cristal de Feitiço flutuando e girando em cima do Conjurador (mais rápido e mais alto quando está ligado). */
public class ConjurerRenderer implements BlockEntityRenderer<RunicConjurerBlockEntity, ConjurerRenderer.State> {
    private final ItemModelResolver itemModelResolver;

    public ConjurerRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    public static class State extends BlockEntityRenderState {
        public final ItemStackRenderState itemState = new ItemStackRenderState();
        public float time;
        public boolean lit;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(RunicConjurerBlockEntity be, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, cameraPosition, breakProgress);
        long gameTime = be.getLevel() != null ? be.getLevel().getGameTime() : 0;
        state.time = gameTime + partialTicks + (be.getBlockPos().asLong() & 63);
        state.lit = be.getBlockState().hasProperty(MachineBlock.LIT) && be.getBlockState().getValue(MachineBlock.LIT);
        this.itemModelResolver.updateForTopItem(state.itemState, be.crystal(), ItemDisplayContext.GROUND, be.getLevel(), null, 0);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.itemState.isEmpty()) {
            return;
        }
        float speed = state.lit ? 4.0F : 1.2F;
        poseStack.pushPose();
        poseStack.translate(0.5F, (state.lit ? 1.0F : 0.9F) + Mth.sin(state.time * 0.08F) * 0.05F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.time * speed));
        poseStack.scale(0.85F, 0.85F, 0.85F);
        state.itemState.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }
}

package com.mistech.mistechmod.husbandry.client;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.husbandry.GrowingPotBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Desenha a planta do Vaso de Cultivo, crescendo conforme o progresso da próxima cópia. */
public class GrowingPotRenderer implements BlockEntityRenderer<GrowingPotBlockEntity, GrowingPotRenderer.State> {
    private static final BlockDisplayContext DISPLAY = BlockDisplayContext.create();
    private final BlockModelResolver blockModelResolver;
    private final ItemModelResolver itemModelResolver;

    public GrowingPotRenderer(BlockEntityRendererProvider.Context context) {
        this.blockModelResolver = context.blockModelResolver();
        this.itemModelResolver = context.itemModelResolver();
    }

    public static class State extends BlockEntityRenderState {
        public final BlockModelRenderState plant = new BlockModelRenderState();
        public final ItemStackRenderState item = new ItemStackRenderState();
        public boolean hasBlock;
        public boolean hasItem;
        public float size;
        public int stackedCane;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(GrowingPotBlockEntity pot, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(pot, state, partialTicks, cameraPosition, breakProgress);
        ItemStack plant = pot.getPlant();
        state.hasBlock = false;
        state.hasItem = false;
        state.stackedCane = 0;
        if (plant.isEmpty()) {
            state.plant.clear();
            return;
        }
        state.size = 0.45F + 0.4F * pot.growth();
        if (plant.getItem() instanceof BlockItem blockItem && blockItem.getBlock() != Blocks.AIR) {
            BlockState plantState = blockItem.getBlock().defaultBlockState();
            this.blockModelResolver.update(state.plant, plantState, DISPLAY);
            state.hasBlock = true;
            // cana: mais um "gomo" por cima quando está quase brotando
            state.stackedCane = plantState.is(Blocks.SUGAR_CANE) && pot.growth() > 0.5F ? 1 : 0;
        } else {
            this.itemModelResolver.updateForTopItem(state.item, plant, ItemDisplayContext.FIXED, pot.getLevel(), null, 0);
            state.hasItem = true;
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.hasBlock) {
            for (int i = 0; i <= state.stackedCane; i++) {
                poseStack.pushPose();
                poseStack.translate(0.5F, 7.0F / 16.0F + i * state.size, 0.5F);
                poseStack.scale(state.size, state.size, state.size);
                poseStack.translate(-0.5F, 0.0F, -0.5F);
                state.plant.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
                poseStack.popPose();
            }
        } else if (state.hasItem && !state.item.isEmpty()) {
            poseStack.pushPose();
            poseStack.translate(0.5F, 0.75F, 0.5F);
            poseStack.scale(state.size * 0.6F, state.size * 0.6F, state.size * 0.6F);
            state.item.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
    }
}

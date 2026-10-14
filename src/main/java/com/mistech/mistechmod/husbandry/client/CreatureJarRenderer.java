package com.mistech.mistechmod.husbandry.client;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.husbandry.CreatureJarBlock;
import com.mistech.mistechmod.husbandry.CreatureJarBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Desenha o bicho pequenininho dentro do Pote com Criatura, olhando para a frente do pote. */
public class CreatureJarRenderer implements BlockEntityRenderer<CreatureJarBlockEntity, CreatureJarRenderer.State> {
    /** Maior medida do bicho dentro do vidro (em blocos). */
    private static final float FIT = 0.52F;
    private final EntityRenderDispatcher entityRenderer;

    public CreatureJarRenderer(BlockEntityRendererProvider.Context context) {
        this.entityRenderer = context.entityRenderer();
    }

    public static class State extends BlockEntityRenderState {
        public @Nullable EntityRenderState creature;
        public float scale;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(CreatureJarBlockEntity jar, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(jar, state, partialTicks, cameraPosition, breakProgress);
        state.creature = null;
        Entity creature = jar.getLevel() != null ? jar.creatureEntity() : null;
        if (creature == null) {
            return;
        }
        // de frente para quem colocou o pote; cabeça reta
        float facing = jar.getBlockState().getValue(CreatureJarBlock.FACING).toYRot();
        creature.setYRot(facing);
        creature.yRotO = facing;
        creature.setXRot(0.0F);
        if (creature instanceof LivingEntity living) {
            living.yBodyRot = living.yBodyRotO = facing;
            living.yHeadRot = living.yHeadRotO = facing;
        }
        // anima devagar (asas da galinha, tentáculos da lula...)
        long time = jar.getLevel().getGameTime();
        creature.tickCount = (int) time;
        state.creature = this.entityRenderer.extractEntity(creature, partialTicks);
        state.creature.lightCoords = state.lightCoords;
        state.creature.shadowPieces.clear();
        state.creature.nameTag = null;
        float size = Math.max(creature.getBbWidth(), creature.getBbHeight());
        state.scale = FIT / Math.max(0.4F, size);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.creature == null) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0.5F, 1.0F / 16.0F, 0.5F);
        poseStack.scale(state.scale, state.scale, state.scale);
        this.entityRenderer.submit(state.creature, camera, 0.0, 0.0, 0.0, poseStack, collector);
        poseStack.popPose();
    }
}

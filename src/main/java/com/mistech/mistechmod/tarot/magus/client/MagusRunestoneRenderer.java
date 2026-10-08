package com.mistech.mistechmod.tarot.magus.client;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.tarot.magus.MagusRunestoneEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/** Pedra Rúnica: cubo de pedra com o pentáculo brilhando, girando devagar (mais rachada = mais escura). */
public class MagusRunestoneRenderer extends EntityRenderer<MagusRunestoneEntity, MagusRunestoneRenderer.State> {
    private static final Identifier GLOW = Identifier.fromNamespaceAndPath(Mistech.MODID, "textures/entity/magician_fx_glow.png");
    private final ModelPart stone;

    public MagusRunestoneRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.stone = MagusGeometry.fxParts(context.bakeLayer(MagusGeometry.FX_LAYER)).get("runestone");
        this.shadowRadius = 0.3F;
    }

    public static class State extends EntityRenderState {
        public float time;
        public int cracks;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(MagusRunestoneEntity entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.time = entity.tickCount + partialTick;
        state.cracks = entity.cracks();
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.4F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.time * 3.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(20.0F));
        poseStack.scale(1.4F, 1.4F, 1.4F);
        int shade = 255 - state.cracks * 45;
        int color = 0xFF000000 | (shade << 16) | (shade << 8) | shade;
        collector.submitModelPart(this.stone, poseStack, RenderTypes.entityCutout(MagusBodyRenderer.FX), state.lightCoords,
                OverlayTexture.NO_OVERLAY, null, color, null);
        collector.submitModelPart(this.stone, poseStack, RenderTypes.eyes(GLOW), 0xF000F0, OverlayTexture.NO_OVERLAY, null);
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }
}

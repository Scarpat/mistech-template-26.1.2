package com.mistech.mistechmod.tarot.magus.client;

import java.util.Map;

import com.mistech.mistechmod.tarot.magus.MagusSpellEntity;
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
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Magias que voam: orbes coloridos, lâminas de vento/areia, rochas e meteoros (o tornado de fogo é só partícula). */
public class MagusSpellRenderer extends EntityRenderer<MagusSpellEntity, MagusSpellRenderer.State> {
    private final ModelPart orb;
    private final ModelPart blade;
    private final ModelPart boulder;
    private final ModelPart meteor;

    public MagusSpellRenderer(EntityRendererProvider.Context context) {
        super(context);
        Map<String, ModelPart> fx = MagusGeometry.fxParts(context.bakeLayer(MagusGeometry.FX_LAYER));
        this.orb = fx.get("orb");
        this.blade = fx.get("blade");
        this.boulder = fx.get("boulder");
        this.meteor = fx.get("meteor");
        this.shadowRadius = 0.2F;
    }

    public static class State extends EntityRenderState {
        public MagusSpellEntity.Kind kind = MagusSpellEntity.Kind.WIND_BOLT;
        public float time;
        public float yaw;
        public float pitch;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(MagusSpellEntity entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.kind = entity.kind();
        state.time = entity.tickCount + partialTick;
        Vec3 v = entity.getDeltaMovement();
        state.yaw = (float) (Mth.atan2(v.x, v.z) * Mth.RAD_TO_DEG);
        state.pitch = (float) (Mth.atan2(v.y, v.horizontalDistance()) * Mth.RAD_TO_DEG);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        MagusSpellEntity.Kind kind = state.kind;
        if (kind == MagusSpellEntity.Kind.FIRE_TORNADO) {
            super.submit(state, poseStack, collector, camera);
            return;
        }
        float s = kind.scale;
        int light = 0xF000F0;
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.1F, 0.0F);
        switch (kind) {
            case WIND_BLADE, SAND_BLADE -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw + 90.0F));
                poseStack.scale(s * 1.6F, s, s * 1.6F);
                int a = kind == MagusSpellEntity.Kind.WIND_BLADE ? 0xB0 : 0xD0;
                collector.submitModelPart(this.blade, poseStack, RenderTypes.entityTranslucent(MagusBodyRenderer.FX), light,
                        OverlayTexture.NO_OVERLAY, null, (a << 24) | kind.color, null);
            }
            case BOULDER -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(state.time * 12.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(state.time * 7.0F));
                poseStack.scale(s, s, s);
                collector.submitModelPart(this.boulder, poseStack, RenderTypes.entityCutout(MagusBodyRenderer.FX), state.lightCoords,
                        OverlayTexture.NO_OVERLAY, null);
            }
            case METEOR, ROCK -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(state.time * 15.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(state.time * 9.0F));
                poseStack.scale(s, s, s);
                collector.submitModelPart(this.meteor, poseStack, RenderTypes.entityCutout(MagusBodyRenderer.FX), light,
                        OverlayTexture.NO_OVERLAY, null);
            }
            default -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(state.time * 18.0F));
                poseStack.mulPose(Axis.XP.rotationDegrees(state.time * 11.0F));
                float pulse = 1.0F + Mth.sin(state.time * 0.8F) * 0.12F;
                poseStack.scale(s * 1.6F * pulse, s * 1.6F * pulse, s * 1.6F * pulse);
                collector.submitModelPart(this.orb, poseStack, RenderTypes.entityTranslucent(MagusBodyRenderer.FX), light,
                        OverlayTexture.NO_OVERLAY, null, 0xD0000000 | kind.color, null);
            }
        }
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }
}

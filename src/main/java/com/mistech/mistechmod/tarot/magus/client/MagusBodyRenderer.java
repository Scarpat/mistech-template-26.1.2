package com.mistech.mistechmod.tarot.magus.client;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.tarot.magus.MagusElement;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;

/**
 * Desenho comum do Mago e dos Aspectos: a forma atual (com brilho nos olhos, ∞, runas e lava), a bolha d'água da
 * Taça e, na Fase IV, o reflexo de cabeça para baixo no plano de cima.
 */
public abstract class MagusBodyRenderer<T extends Mob> extends MobRenderer<T, MagusRenderState, MagusModel> {
    static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Mistech.MODID, "textures/entity/the_magician.png");
    static final Identifier GLOW = Identifier.fromNamespaceAndPath(Mistech.MODID, "textures/entity/the_magician_glow.png");
    static final Identifier FX = Identifier.fromNamespaceAndPath(Mistech.MODID, "textures/entity/magician_fx.png");

    private final ModelPart bubble;

    protected MagusBodyRenderer(EntityRendererProvider.Context context) {
        super(context, new MagusModel(context.bakeLayer(MagusGeometry.BODY_LAYER)), 1.2F);
        this.bubble = MagusGeometry.fxParts(context.bakeLayer(MagusGeometry.FX_LAYER)).get("bubble");
        this.addLayer(new RenderLayer<>(this) {
            @Override
            public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, MagusRenderState state, float yRot, float xRot) {
                if (state.isInvisible) {
                    return;
                }
                int color = -1;
                MagusElement mix = MagusElement.byIdOrNull(state.mix);
                if (mix != null) {
                    // aura do segundo elemento (Fase II): o brilho pulsa misturando as cores
                    float k = 0.5F + 0.5F * (float) Math.sin(state.ageInTicks * 0.2F);
                    color = ARGB.srgbLerp(k, 0xFFFFFFFF, 0xFF000000 | mix.color);
                }
                collector.order(1).submitModel(this.getParentModel(), state, poseStack, RenderTypes.eyes(GLOW), light,
                        OverlayTexture.NO_OVERLAY, color, null, state.outlineColor, null);
            }
        });
    }

    @Override
    public MagusRenderState createRenderState() {
        return new MagusRenderState();
    }

    @Override
    public Identifier getTextureLocation(MagusRenderState state) {
        return TEXTURE;
    }

    @Override
    protected RenderType getRenderType(MagusRenderState state, boolean bodyVisible, boolean translucent, boolean glowing) {
        if (!bodyVisible) {
            return null;
        }
        return RenderTypes.entityTranslucent(TEXTURE);
    }

    @Override
    protected AABB getBoundingBoxForCulling(T entity) {
        return super.getBoundingBoxForCulling(entity).inflate(3.0, 2.0, 3.0).expandTowards(0, 32, 0);
    }

    @Override
    protected float getShadowRadius(MagusRenderState state) {
        return state.isInvisible ? 0.0F : state.aspect ? 0.8F : 1.4F;
    }

    @Override
    public void submit(MagusRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        super.submit(state, poseStack, collector, camera);
        if (state.isInvisible) {
            return;
        }
        // bolha d'água
        if (state.shield > 0.0F) {
            poseStack.pushPose();
            float h = state.boundingBoxHeight;
            float w = Math.max(state.boundingBoxWidth, h * 0.7F);
            poseStack.translate(0.0F, h * 0.5F, 0.0F);
            float s = w * 1.3F / 1.0F;
            poseStack.mulPose(Axis.YP.rotationDegrees(state.ageInTicks * 2.0F));
            poseStack.scale(s, h * 1.15F, s);
            int alpha = (int) (110 + 80 * state.shield);
            collector.submitModelPart(this.bubble, poseStack, RenderTypes.entityTranslucent(FX), state.lightCoords, OverlayTexture.NO_OVERLAY,
                    null, (alpha << 24) | 0xFFFFFF, null);
            poseStack.popPose();
        }
        // reflexo (Fase IV): de cabeça para baixo sob o plano de cima. Só quando o Mago está embaixo do "espelho"
        // (quando ele sobe, o reflexo ficaria enterrado sob o chão de baixo). Virado com rotação (e não escala
        // negativa), para não inverter as normais e a luz do modelo.
        if (state.mirrorY != 0.0F && !state.aspect && !state.reflection && state.entityY < state.mirrorY) {
            MagusRenderState r = new MagusRenderState();
            r.copyFrom(state);
            r.reflection = true;
            poseStack.pushPose();
            poseStack.translate(0.0, 2.0 * (state.mirrorY - state.entityY), 0.0);
            poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(state.bodyRot - 180.0F));
            poseStack.scale(-1.0F, -1.0F, 1.0F);
            poseStack.translate(0.0F, -1.501F, 0.0F);
            collector.submitModel(this.model, r, poseStack, RenderTypes.entityTranslucent(TEXTURE), 0xF000F0, OverlayTexture.NO_OVERLAY,
                    0x80D8C8FF, null, 0, null);
            collector.order(1).submitModel(this.model, r, poseStack, RenderTypes.eyes(GLOW), 0xF000F0, OverlayTexture.NO_OVERLAY,
                    0x90FFFFFF, null, 0, null);
            poseStack.popPose();
        }
    }
}

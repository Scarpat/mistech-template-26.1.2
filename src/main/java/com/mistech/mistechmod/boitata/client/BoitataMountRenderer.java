package com.mistech.mistechmod.boitata.client;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.boitata.BoitataMountEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/** Boitatá montaria: corpo inteiro (ver {@link BoitataMountModel}), brilho das chamas e dos olhos, e a armadura vestida. */
public class BoitataMountRenderer extends MobRenderer<BoitataMountEntity, BoitataMountRenderer.State, BoitataMountModel> {
    private static final Identifier TEXTURE = tex("boitata_mount");
    private static final Identifier GLOW = tex("boitata_mount_glow");
    private static final Identifier[] ARMOR = {null, tex("boitata_mount_armor_scale"), tex("boitata_mount_armor_orichalcum")};
    private static final Identifier[] ARMOR_GLOW = {null, tex("boitata_mount_armor_scale_glow"), tex("boitata_mount_armor_orichalcum_glow")};

    private static Identifier tex(String name) {
        return Identifier.fromNamespaceAndPath(Mistech.MODID, "textures/entity/" + name + ".png");
    }

    public static class State extends LivingEntityRenderState {
        public float size = 1.0F;
        public boolean flying;
        public boolean breathing;
        public int armor;
        public float bodyPitch;
    }

    public BoitataMountRenderer(EntityRendererProvider.Context context) {
        super(context, new BoitataMountModel(context.bakeLayer(BoitataMountModel.LAYER)), 0.8F);
        this.addLayer(new GlowLayer(this));
        this.addLayer(new ArmorLayer(this, new BoitataMountArmorModel(context.bakeLayer(BoitataMountArmorModel.LAYER))));
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    protected net.minecraft.world.phys.AABB getBoundingBoxForCulling(BoitataMountEntity entity) {
        return entity.getBoundingBox().inflate(3.5 * entity.size());
    }

    @Override
    public void extractRenderState(BoitataMountEntity entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.size = entity.size();
        state.flying = entity.isFlyingNow();
        state.breathing = entity.isBreathing();
        state.armor = entity.armorKind();
        state.bodyPitch = state.flying ? entity.getXRot(partialTicks) : 0.0F;
        this.shadowRadius = 0.7F * state.size;
    }

    @Override
    protected void scale(State state, PoseStack poseStack) {
        poseStack.scale(state.size, state.size, state.size);
    }

    @Override
    protected void setupRotations(State state, PoseStack poseStack, float bodyRot, float scale) {
        super.setupRotations(state, poseStack, bodyRot, scale);
        if (state.bodyPitch != 0.0F) {
            float h = 0.25F * state.size;
            poseStack.translate(0.0F, h, 0.0F);
            poseStack.mulPose(Axis.XP.rotationDegrees(-state.bodyPitch));
            poseStack.translate(0.0F, -h, 0.0F);
        }
    }

    @Override
    public Identifier getTextureLocation(State state) {
        return TEXTURE;
    }

    /** Chamas das barbatanas, crista, cauda e os olhos brilham no escuro. */
    private static class GlowLayer extends RenderLayer<State, BoitataMountModel> {
        GlowLayer(RenderLayerParent<State, BoitataMountModel> parent) {
            super(parent);
        }

        @Override
        public void submit(PoseStack pose, SubmitNodeCollector collector, int light, State state, float yRot, float xRot) {
            if (state.isInvisible) {
                return;
            }
            collector.submitModel(this.getParentModel(), state, pose, RenderTypes.eyes(GLOW), 0xF000F0, OverlayTexture.NO_OVERLAY, -1, null,
                    state.outlineColor, null);
        }
    }

    /** Armadura vestida (escamas ou oricalco): peças 3D próprias, na pose do corpo; as gemas brilham. */
    private static class ArmorLayer extends RenderLayer<State, BoitataMountModel> {
        private final BoitataMountArmorModel model;

        ArmorLayer(RenderLayerParent<State, BoitataMountModel> parent, BoitataMountArmorModel model) {
            super(parent);
            this.model = model;
        }

        @Override
        public void submit(PoseStack pose, SubmitNodeCollector collector, int light, State state, float yRot, float xRot) {
            if (state.isInvisible || state.armor <= 0 || state.armor >= ARMOR.length) {
                return;
            }
            // a pose é montada na hora de desenhar (setupAnim do coletor), a partir do corpo — ver BoitataMountArmorModel.setupAnim
            this.model.bindSource(this.getParentModel());
            collector.submitModel(this.model, state, pose, RenderTypes.armorCutoutNoCull(ARMOR[state.armor]), light, OverlayTexture.NO_OVERLAY,
                    -1, null, state.outlineColor, null);
            collector.submitModel(this.model, state, pose, RenderTypes.eyes(ARMOR_GLOW[state.armor]), 0xF000F0, OverlayTexture.NO_OVERLAY,
                    -1, null, state.outlineColor, null);
        }
    }
}

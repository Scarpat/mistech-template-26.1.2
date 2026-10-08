package com.mistech.mistechmod.tarot.fool.client;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.tarot.fool.FoolTornadoEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/** Tornado: anéis translúcidos empilhados, cada um girando numa velocidade, abrindo para cima (como um funil). */
public class FoolTornadoRenderer extends EntityRenderer<FoolTornadoEntity, FoolTornadoRenderer.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Mistech.MODID, "fool_tornado"), "main");
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Mistech.MODID, "textures/entity/fool_tornado.png");
    private static final int RINGS = 6;

    private final ModelPart[] rings = new ModelPart[RINGS];

    public FoolTornadoRenderer(EntityRendererProvider.Context context) {
        super(context);
        ModelPart root = context.bakeLayer(LAYER);
        for (int i = 0; i < RINGS; i++) {
            this.rings[i] = root.getChild("ring" + i);
        }
        this.shadowRadius = 0.0F;
    }

    public static class State extends EntityRenderState {
        public float time;
        public boolean big;
    }

    /** Anel i: caixa oca (4 paredes finas) de largura 6 + 4i px, 8 px de altura. */
    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        for (int i = 0; i < RINGS; i++) {
            float w = 6 + i * 4;
            float h = w / 2.0F;
            root.addOrReplaceChild("ring" + i, CubeListBuilder.create()
                    .texOffs(0, 0).addBox(-h, 0.0F, -h, w, 8, 1)
                    .texOffs(0, 0).addBox(-h, 0.0F, h - 1.0F, w, 8, 1)
                    .texOffs(0, 0).addBox(-h, 0.0F, -h + 1.0F, 1, 8, w - 2.0F)
                    .texOffs(0, 0).addBox(h - 1.0F, 0.0F, -h + 1.0F, 1, 8, w - 2.0F),
                    PartPose.offset(0.0F, -i * 8.0F, 0.0F));
        }
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(FoolTornadoEntity entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.time = entity.tickCount + partialTick;
        state.big = entity.isBig();
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        RenderType type = RenderTypes.entityTranslucent(TEXTURE);
        float scale = state.big ? 1.0F : 0.7F;
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.5F * scale, 0.0F);
        poseStack.scale(scale, -scale, -scale); // modelo com y para baixo, como os de entidade
        for (int i = 0; i < RINGS; i++) {
            poseStack.pushPose();
            float wobble = (float) Math.sin(state.time * 0.3F + i) * 0.06F;
            poseStack.translate(wobble, 0.0F, wobble * 0.5F);
            poseStack.mulPose(Axis.YP.rotationDegrees(state.time * (14.0F + i * 5.0F)));
            int alpha = 150 - i * 12;
            collector.submitModelPart(this.rings[i], poseStack, type, state.lightCoords, OverlayTexture.NO_OVERLAY, null,
                    (alpha << 24) | 0xF4FBFF, null);
            poseStack.popPose();
        }
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }
}

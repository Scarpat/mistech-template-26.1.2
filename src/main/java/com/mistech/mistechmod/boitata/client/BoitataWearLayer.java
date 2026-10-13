package com.mistech.mistechmod.boitata.client;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.boitata.BoitataArmorItem;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Partes 3D da Armadura do Boitatá: crista de fogo no capacete, o Olho no peito (brilha no escuro) e espinhos nas costas.
 * Textura: {@code boitata_wear.png}; o brilho do olho: {@code boitata_wear_eye.png} (gerados por tools/boitata/gen_textures.py).
 */
public class BoitataWearLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(Mistech.MODID, "boitata_wear"), "main");
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Mistech.MODID, "textures/entity/boitata_wear.png");
    private static final Identifier EYE = Identifier.fromNamespaceAndPath(Mistech.MODID, "textures/entity/boitata_wear_eye.png");
    private static final int FULL_BRIGHT = 0xF000F0;

    private final ModelPart crest, crestA, crestB, crestC, eye, spines;

    public BoitataWearLayer(RenderLayerParent<AvatarRenderState, PlayerModel> renderer, ModelPart root) {
        super(renderer);
        this.crest = root.getChild("crest");
        this.crestA = this.crest.getChild("crest_a");
        this.crestB = this.crest.getChild("crest_b");
        this.crestC = this.crest.getChild("crest_c");
        this.eye = root.getChild("eye");
        this.spines = root.getChild("spines");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition crest = root.addOrReplaceChild("crest", CubeListBuilder.create(), PartPose.ZERO);
        crest.addOrReplaceChild("crest_a", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, -5.0F, -1.5F, 2.0F, 5.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, -8.6F, -3.2F, -0.3F, 0.0F, 0.0F));
        crest.addOrReplaceChild("crest_b", CubeListBuilder.create().texOffs(10, 0).addBox(-1.0F, -7.0F, -1.5F, 2.0F, 7.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, -8.6F, 0.0F, -0.55F, 0.0F, 0.0F));
        crest.addOrReplaceChild("crest_c", CubeListBuilder.create().texOffs(20, 0).addBox(-1.0F, -5.0F, -1.5F, 2.0F, 5.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, -8.4F, 3.4F, -0.85F, 0.0F, 0.0F));
        root.addOrReplaceChild("eye", CubeListBuilder.create().texOffs(0, 12).addBox(-2.0F, 3.0F, -3.7F, 4.0F, 4.0F, 1.0F), PartPose.ZERO);
        PartDefinition spines = root.addOrReplaceChild("spines", CubeListBuilder.create(), PartPose.ZERO);
        for (int i = 0; i < 3; i++) {
            spines.addOrReplaceChild("spine_" + i, CubeListBuilder.create().texOffs(30, 0).addBox(-0.5F, -3.0F, -1.0F, 1.0F, 3.0F, 2.0F),
                    PartPose.offsetAndRotation(0.0F, 2.5F + i * 3.2F, 3.2F, -0.9F, 0.0F, 0.0F));
        }
        return LayerDefinition.create(mesh, 64, 32);
    }

    private static void part(PoseStack pose, SubmitNodeCollector collector, int light, ModelPart part, ModelPart parent) {
        pose.pushPose();
        parent.translateAndRotate(pose);
        collector.submitModelPart(part, pose, RenderTypes.armorCutoutNoCull(TEXTURE), light, OverlayTexture.NO_OVERLAY, null, -1, null);
        pose.popPose();
    }

    @Override
    public void submit(PoseStack pose, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot) {
        if (state.isInvisible) {
            return;
        }
        PlayerModel model = this.getParentModel();
        if (state.headEquipment.getItem() instanceof BoitataArmorItem) {
            float t = state.ageInTicks;
            this.crestA.yScale = 1.0F + Mth.sin(t * 0.5F) * 0.15F;
            this.crestB.yScale = 1.0F + Mth.sin(t * 0.45F + 1.0F) * 0.18F;
            this.crestC.yScale = 1.0F + Mth.sin(t * 0.55F + 2.0F) * 0.15F;
            part(pose, collector, FULL_BRIGHT, this.crest, model.head);
        }
        if (state.chestEquipment.getItem() instanceof BoitataArmorItem) {
            part(pose, collector, light, this.eye, model.body);
            part(pose, collector, light, this.spines, model.body);
            pose.pushPose();
            model.body.translateAndRotate(pose);
            collector.submitModelPart(this.eye, pose, RenderTypes.eyes(EYE), FULL_BRIGHT, OverlayTexture.NO_OVERLAY, null, -1, null);
            pose.popPose();
        }
    }
}

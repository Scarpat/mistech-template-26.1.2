package com.mistech.mistechmod.lore.client;

import com.mistech.mistechmod.Mistech;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Iara: mãe-d'água de cauda de peixe, cabelos longos e flor no cabelo. Geometria e textura: {@code tools/lore/models2.py}. */
public class IaraModel extends EntityModel<LoreRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Mistech.MODID, "iara"), "main");

    private final ModelPart head, hair, rightArm, leftArm, tail0, tail1, tail2, fin, hairBack;

    public IaraModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.hair = this.head.getChild("hair");
        this.hairBack = this.hair.getChild("hair_back");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.tail0 = root.getChild("tail0");
        this.tail1 = this.tail0.getChild("tail1");
        this.tail2 = this.tail1.getChild("tail2");
        this.fin = this.tail2.getChild("tail_fin");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 19).addBox(-3.5F, -7.0F, -2.0F, 7.0F, 7.0F, 4.0F), PartPose.offset(0.0F, 8.0F, 0.0F));
        PartDefinition p_head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(58, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F), PartPose.offset(0.0F, 1.0F, 0.0F));
        PartDefinition p_hair = p_head.addOrReplaceChild("hair", CubeListBuilder.create().texOffs(90, 0).addBox(-4.5F, -1.0F, -4.5F, 9.0F, 2.5F, 9.0F), PartPose.offset(0.0F, -8.0F, 0.0F));
        PartDefinition p_hair_back = p_hair.addOrReplaceChild("hair_back", CubeListBuilder.create().texOffs(0, 0).addBox(-4.5F, 0.0F, -0.5F, 9.0F, 17.0F, 1.5F), PartPose.offset(0.0F, 1.0F, 4.0F));
        PartDefinition p_hair_r = p_hair.addOrReplaceChild("hair_r", CubeListBuilder.create().texOffs(22, 0).addBox(-0.6F, 0.0F, -4.2F, 1.0F, 9.0F, 7.5F), PartPose.offset(-4.5F, 1.0F, 1.0F));
        PartDefinition p_hair_l = p_hair.addOrReplaceChild("hair_l", CubeListBuilder.create().texOffs(40, 0).addBox(-0.4F, 0.0F, -4.2F, 1.0F, 9.0F, 7.5F), PartPose.offset(4.5F, 1.0F, 1.0F));
        PartDefinition p_lock_r = p_hair.addOrReplaceChild("lock_r", CubeListBuilder.create().texOffs(80, 19).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 8.0F, 1.0F), PartPose.offset(-3.6F, 8.0F, -4.3F));
        PartDefinition p_lock_l = p_hair.addOrReplaceChild("lock_l", CubeListBuilder.create().texOffs(86, 19).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 8.0F, 1.0F), PartPose.offset(3.6F, 8.0F, -4.3F));
        PartDefinition p_flower = p_hair.addOrReplaceChild("flower", CubeListBuilder.create().texOffs(16, 30).addBox(-1.5F, -1.5F, -1.5F, 3.0F, 1.5F, 3.0F), PartPose.offset(3.0F, -0.5F, -2.5F));
        PartDefinition p_flower_1 = p_hair.addOrReplaceChild("flower_1", CubeListBuilder.create().texOffs(54, 30).addBox(-1.0F, -1.5F, -1.0F, 2.0F, 1.5F, 2.0F), PartPose.offset(-3.2F, -0.5F, -1.5F));
        PartDefinition p_flower_2 = p_hair.addOrReplaceChild("flower_2", CubeListBuilder.create().texOffs(62, 30).addBox(-1.0F, -1.5F, -1.0F, 2.0F, 1.5F, 2.0F), PartPose.offset(0.5F, -0.5F, -4.0F));
        PartDefinition p_lily_pad = p_hair.addOrReplaceChild("lily_pad", CubeListBuilder.create().texOffs(38, 30).addBox(-2.0F, -0.5F, -2.0F, 4.0F, 0.5F, 4.0F), PartPose.offsetAndRotation(3.0F, -0.6F, -1.5F, 0.0F, 0.4F, 0.15F));
        PartDefinition p_right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(64, 19).addBox(-2.0F, 0.0F, -1.0F, 2.0F, 7.0F, 2.0F).texOffs(70, 30).addBox(-2.2F, 4.5F, -1.2F, 2.4F, 0.8F, 2.4F), PartPose.offset(-3.5F, 1.5F, 0.0F));
        PartDefinition p_left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(72, 19).addBox(0.0F, 0.0F, -1.0F, 2.0F, 7.0F, 2.0F).texOffs(78, 30).addBox(-0.2F, 4.5F, -1.2F, 2.4F, 0.8F, 2.4F), PartPose.offset(3.5F, 1.5F, 0.0F));
        PartDefinition p_tail0 = root.addOrReplaceChild("tail0", CubeListBuilder.create().texOffs(22, 19).addBox(-3.5F, 0.0F, -2.0F, 7.0F, 6.0F, 4.0F), PartPose.offset(0.0F, 8.0F, 0.0F));
        PartDefinition p_hip_fin_r = p_tail0.addOrReplaceChild("hip_fin_r", CubeListBuilder.create().texOffs(0, 30).addBox(-0.25F, 0.0F, -1.5F, 0.5F, 4.0F, 3.0F), PartPose.offsetAndRotation(-3.5F, 2.0F, 0.0F, 0.0F, 0.0F, 0.6F));
        PartDefinition p_hip_fin_l = p_tail0.addOrReplaceChild("hip_fin_l", CubeListBuilder.create().texOffs(6, 30).addBox(-0.25F, 0.0F, -1.5F, 0.5F, 4.0F, 3.0F), PartPose.offsetAndRotation(3.5F, 2.0F, 0.0F, 0.0F, 0.0F, -0.6F));
        PartDefinition p_tail1 = p_tail0.addOrReplaceChild("tail1", CubeListBuilder.create().texOffs(44, 19).addBox(-3.0F, 0.0F, -1.75F, 6.0F, 6.0F, 3.5F), PartPose.offset(0.0F, 6.0F, 0.0F));
        PartDefinition p_tail2 = p_tail1.addOrReplaceChild("tail2", CubeListBuilder.create().texOffs(92, 19).addBox(-2.5F, 0.0F, -1.5F, 5.0F, 5.0F, 3.0F), PartPose.offset(0.0F, 6.0F, 0.0F));
        PartDefinition p_dorsal = p_tail2.addOrReplaceChild("dorsal", CubeListBuilder.create().texOffs(12, 30).addBox(-0.25F, 0.0F, 0.0F, 0.5F, 5.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 1.5F, -0.3F, 0.0F, 0.0F));
        PartDefinition p_tail3 = p_tail2.addOrReplaceChild("tail3", CubeListBuilder.create().texOffs(28, 30).addBox(-1.5F, 0.0F, -1.0F, 3.0F, 3.0F, 2.0F), PartPose.offset(0.0F, 5.0F, 0.0F));
        PartDefinition p_tail_fin = p_tail2.addOrReplaceChild("tail_fin", CubeListBuilder.create(), PartPose.offset(0.0F, 7.5F, 0.0F));
        PartDefinition p_fin_r = p_tail_fin.addOrReplaceChild("fin_r", CubeListBuilder.create().texOffs(108, 19).addBox(-2.0F, 0.0F, -0.5F, 4.0F, 7.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.65F));
        PartDefinition p_fin_l = p_tail_fin.addOrReplaceChild("fin_l", CubeListBuilder.create().texOffs(118, 19).addBox(-2.0F, 0.0F, -0.5F, 4.0F, 7.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.65F));
        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void setupAnim(LoreRenderState state) {
        super.setupAnim(state);
        float t = state.ageInTicks;
        float speed = state.swimming ? 1.0F : 0.35F;
        // cauda ondulando (mais forte nadando)
        this.tail0.xRot = Mth.sin(t * 0.25F) * 0.25F * speed;
        this.tail1.xRot = Mth.sin(t * 0.25F - 0.8F) * 0.35F * speed;
        this.tail2.xRot = Mth.sin(t * 0.25F - 1.6F) * 0.45F * speed;
        this.fin.xRot = Mth.sin(t * 0.25F - 2.4F) * 0.5F * speed;
        this.head.yRot = state.yRot * Mth.DEG_TO_RAD;
        this.head.xRot = state.xRot * Mth.DEG_TO_RAD * (state.swimming ? 0.3F : 1.0F) - (state.swimming ? 0.9F : 0.0F);
        this.hairBack.xRot = 0.15F + Mth.sin(t * 0.08F) * 0.08F + (state.swimming ? 0.9F : 0.0F);
        if (state.swimming) {
            // braços para trás, junto do corpo
            this.rightArm.xRot = 2.6F + Mth.sin(t * 0.2F) * 0.2F;
            this.leftArm.xRot = 2.6F - Mth.sin(t * 0.2F) * 0.2F;
        } else if (state.casting) {
            // cantando: braços abertos, chamando
            this.rightArm.xRot = -1.6F + Mth.sin(t * 0.15F) * 0.2F;
            this.leftArm.xRot = -1.6F + Mth.cos(t * 0.15F) * 0.2F;
            this.rightArm.zRot = 0.5F;
            this.leftArm.zRot = -0.5F;
        } else {
            this.rightArm.xRot = Mth.sin(t * 0.07F) * 0.1F;
            this.leftArm.xRot = -Mth.sin(t * 0.07F) * 0.1F;
        }
    }
}

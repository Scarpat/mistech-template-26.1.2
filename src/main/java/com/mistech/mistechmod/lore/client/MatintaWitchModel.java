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

/** Matinta Pereira revelada: bruxa velha corcunda de xale preto, nariz torto e bengala. Geometria e textura: {@code tools/lore/models2.py}. */
public class MatintaWitchModel extends EntityModel<LoreRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Mistech.MODID, "matinta_witch"), "main");

    private final ModelPart head, skirt, rightArm, leftArm;

    public MatintaWitchModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.skirt = root.getChild("skirt");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_skirt = root.addOrReplaceChild("skirt", CubeListBuilder.create().texOffs(0, 0).addBox(-4.5F, 0.0F, -3.0F, 9.0F, 12.0F, 6.0F), PartPose.offset(0.0F, 12.0F, 0.0F));
        PartDefinition p_body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 18).addBox(-4.0F, -9.0F, -2.5F, 8.0F, 9.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 12.0F, 0.5F, 0.4F, 0.0F, 0.0F));
        PartDefinition p_hump = p_body.addOrReplaceChild("hump", CubeListBuilder.create().texOffs(98, 18).addBox(-3.5F, -2.5F, -1.0F, 7.0F, 5.0F, 2.5F), PartPose.offset(0.0F, -7.0F, 2.5F));
        PartDefinition p_beads = p_body.addOrReplaceChild("beads", CubeListBuilder.create().texOffs(56, 32).addBox(-2.5F, 0.0F, -0.4F, 5.0F, 3.0F, 0.4F), PartPose.offsetAndRotation(0.0F, -8.5F, -2.6F, -0.1F, 0.0F, 0.0F));
        PartDefinition p_shoulder_feather_0 = p_body.addOrReplaceChild("shoulder_feather_0", CubeListBuilder.create().texOffs(48, 32).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(-3.5F, -9.0F, 0.0F, 0.0F, 0.0F, -0.5F));
        PartDefinition p_shoulder_feather_1 = p_body.addOrReplaceChild("shoulder_feather_1", CubeListBuilder.create().texOffs(52, 32).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(-2.5F, -9.0F, 0.0F, 0.0F, 0.0F, -0.2F));
        PartDefinition p_head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(30, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F), PartPose.offset(0.0F, 4.5F, -2.5F));
        PartDefinition p_nose = p_head.addOrReplaceChild("nose", CubeListBuilder.create().texOffs(8, 32).addBox(-1.0F, -1.0F, -2.5F, 2.0F, 2.5F, 2.5F), PartPose.offsetAndRotation(0.0F, -3.5F, -4.0F, 0.35F, 0.0F, 0.0F));
        PartDefinition p_nose_tip = p_nose.addOrReplaceChild("nose_tip", CubeListBuilder.create().texOffs(66, 32).addBox(-0.5F, -0.5F, -1.2F, 1.0F, 1.2F, 1.2F), PartPose.offsetAndRotation(0.0F, 1.2F, -2.5F, 0.6F, 0.0F, 0.0F));
        PartDefinition p_hood = p_head.addOrReplaceChild("hood", CubeListBuilder.create().texOffs(26, 18).addBox(-4.6F, -8.7F, -4.6F, 9.2F, 2.0F, 9.2F), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p_hood_r = p_hood.addOrReplaceChild("hood_r", CubeListBuilder.create().texOffs(66, 0).addBox(-0.5F, 0.0F, 0.0F, 1.0F, 6.0F, 9.2F), PartPose.offset(-4.6F, -6.7F, -4.6F));
        PartDefinition p_hood_l = p_hood.addOrReplaceChild("hood_l", CubeListBuilder.create().texOffs(86, 0).addBox(-0.5F, 0.0F, 0.0F, 1.0F, 6.0F, 9.2F), PartPose.offset(4.6F, -6.7F, -4.6F));
        PartDefinition p_drape = p_hood.addOrReplaceChild("drape", CubeListBuilder.create().texOffs(78, 18).addBox(-4.6F, 0.0F, 0.0F, 9.2F, 9.0F, 1.2F), PartPose.offset(0.0F, -6.7F, 4.1F));
        PartDefinition p_hair_r = p_head.addOrReplaceChild("hair_r", CubeListBuilder.create().texOffs(116, 18).addBox(-1.0F, 0.0F, 0.0F, 1.5F, 5.0F, 2.0F), PartPose.offsetAndRotation(-4.2F, -3.0F, -3.4F, 0.0F, 0.0F, 0.25F));
        PartDefinition p_hair_l = p_head.addOrReplaceChild("hair_l", CubeListBuilder.create().texOffs(0, 32).addBox(-0.5F, 0.0F, 0.0F, 1.5F, 5.0F, 2.0F), PartPose.offsetAndRotation(4.2F, -3.0F, -3.4F, 0.0F, 0.0F, -0.25F));
        PartDefinition p_right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(62, 18).addBox(-1.5F, 0.0F, -1.0F, 2.0F, 9.0F, 2.0F), PartPose.offset(-4.5F, 5.0F, -1.0F));
        PartDefinition p_right_hand = p_right_arm.addOrReplaceChild("right_hand", CubeListBuilder.create().texOffs(16, 32).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 2.0F, 2.0F), PartPose.offset(-0.5F, 9.0F, 0.0F));
        PartDefinition p_right_nail_0 = p_right_arm.addOrReplaceChild("right_nail_0", CubeListBuilder.create().texOffs(70, 32).addBox(-0.2F, 0.0F, -0.2F, 0.4F, 1.5F, 0.4F), PartPose.offsetAndRotation(-1.0F, 11.0F, -0.6F, -0.3F, 0.0F, 0.0F));
        PartDefinition p_right_nail_1 = p_right_arm.addOrReplaceChild("right_nail_1", CubeListBuilder.create().texOffs(70, 32).addBox(-0.2F, 0.0F, -0.2F, 0.4F, 1.5F, 0.4F), PartPose.offsetAndRotation(-0.25F, 11.0F, -0.6F, -0.3F, 0.0F, 0.0F));
        PartDefinition p_right_nail_2 = p_right_arm.addOrReplaceChild("right_nail_2", CubeListBuilder.create().texOffs(70, 32).addBox(-0.2F, 0.0F, -0.2F, 0.4F, 1.5F, 0.4F), PartPose.offsetAndRotation(0.5F, 11.0F, -0.6F, -0.3F, 0.0F, 0.0F));
        PartDefinition p_left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(70, 18).addBox(-0.5F, 0.0F, -1.0F, 2.0F, 9.0F, 2.0F), PartPose.offset(4.5F, 5.0F, -1.0F));
        PartDefinition p_left_hand = p_left_arm.addOrReplaceChild("left_hand", CubeListBuilder.create().texOffs(24, 32).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 2.0F, 2.0F), PartPose.offset(0.5F, 9.0F, 0.0F));
        PartDefinition p_cane = p_left_arm.addOrReplaceChild("cane", CubeListBuilder.create().texOffs(62, 0).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 15.0F, 1.0F), PartPose.offset(0.5F, 10.0F, -0.5F));
        PartDefinition p_cane_knob = p_cane.addOrReplaceChild("cane_knob", CubeListBuilder.create().texOffs(32, 32).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 2.0F, 2.0F), PartPose.offsetAndRotation(0.0F, -3.0F, 0.0F, 0.0F, 0.6F, 0.4F));
        PartDefinition p_cane_skull = p_cane.addOrReplaceChild("cane_skull", CubeListBuilder.create().texOffs(40, 32).addBox(-0.75F, -1.5F, -0.75F, 1.5F, 1.5F, 1.5F), PartPose.offset(0.0F, -4.5F, 0.0F));
        PartDefinition p_cane_ribbon = p_cane.addOrReplaceChild("cane_ribbon", CubeListBuilder.create().texOffs(56, 32).addBox(0.0F, 0.0F, -0.25F, 0.5F, 4.0F, 0.5F), PartPose.offset(0.5F, -2.0F, 0.0F));
        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void setupAnim(LoreRenderState state) {
        super.setupAnim(state);
        float t = state.ageInTicks;
        float pos = state.walkAnimationPos;
        float speed = state.walkAnimationSpeed;
        this.head.yRot = state.yRot * Mth.DEG_TO_RAD;
        this.head.xRot = state.xRot * Mth.DEG_TO_RAD * 0.6F;
        this.skirt.zRot = Mth.sin(pos * 0.6F) * 0.06F * speed;
        this.rightArm.xRot = Mth.cos(pos * 0.6662F + Mth.PI) * 0.8F * speed;
        this.leftArm.xRot = -0.3F + Mth.cos(pos * 0.6662F) * 0.4F * speed;
        if (state.casting) {
            // braços para cima lançando o feitiço
            this.rightArm.xRot = -2.6F + Mth.sin(t * 0.8F) * 0.2F;
            this.rightArm.zRot = 0.3F;
            this.leftArm.xRot = -2.2F;
            this.leftArm.zRot = -0.3F;
        }
        this.head.zRot = Mth.sin(t * 0.07F) * 0.05F;
    }
}

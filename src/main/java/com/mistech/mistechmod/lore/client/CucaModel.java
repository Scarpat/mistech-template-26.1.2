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

/** Cuca: bruxa-jacaré de vestido roxo, cabelo ralo com alfinete de osso e concha de mexer o caldeirão. Geometria e textura: {@code tools/lore/models2.py}. */
public class CucaModel extends EntityModel<LoreRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Mistech.MODID, "cuca"), "main");

    private final ModelPart head, jaw, body, rightArm, leftArm, tail0, tail1, tail2, skirt;

    public CucaModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.jaw = this.head.getChild("jaw");
        this.body = root.getChild("body");
        this.skirt = root.getChild("skirt");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.tail0 = root.getChild("tail0");
        this.tail1 = this.tail0.getChild("tail1");
        this.tail2 = this.tail1.getChild("tail2");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_skirt = root.addOrReplaceChild("skirt", CubeListBuilder.create().texOffs(0, 0).addBox(-6.0F, 0.0F, -4.5F, 12.0F, 11.0F, 9.0F), PartPose.offset(0.0F, 13.0F, 0.0F));
        PartDefinition p_body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(42, 0).addBox(-5.0F, -12.0F, -3.0F, 10.0F, 12.0F, 6.0F), PartPose.offsetAndRotation(0.0F, 13.0F, 0.5F, 0.25F, 0.0F, 0.0F));
        PartDefinition p_belt = p_body.addOrReplaceChild("belt", CubeListBuilder.create().texOffs(70, 33).addBox(-5.4F, 0.0F, -3.4F, 10.8F, 1.5F, 6.8F), PartPose.offset(0.0F, -2.0F, 0.0F));
        PartDefinition p_vial_0 = p_belt.addOrReplaceChild("vial_0", CubeListBuilder.create().texOffs(106, 44).addBox(-0.75F, 0.0F, -1.0F, 1.5F, 2.5F, 1.5F).texOffs(0, 56).addBox(-0.4F, -0.6F, -0.65F, 0.8F, 0.6F, 0.8F), PartPose.offset(-3.5F, 1.0F, -3.6F));
        PartDefinition p_vial_1 = p_belt.addOrReplaceChild("vial_1", CubeListBuilder.create().texOffs(114, 44).addBox(-0.75F, 0.0F, -1.0F, 1.5F, 2.5F, 1.5F).texOffs(4, 56).addBox(-0.4F, -0.6F, -0.65F, 0.8F, 0.6F, 0.8F), PartPose.offset(-1.5F, 1.0F, -3.6F));
        PartDefinition p_vial_2 = p_belt.addOrReplaceChild("vial_2", CubeListBuilder.create().texOffs(0, 52).addBox(-0.75F, 0.0F, -1.0F, 1.5F, 2.5F, 1.5F).texOffs(8, 56).addBox(-0.4F, -0.6F, -0.65F, 0.8F, 0.6F, 0.8F), PartPose.offset(3.0F, 1.0F, -3.6F));
        PartDefinition p_necklace = p_body.addOrReplaceChild("necklace", CubeListBuilder.create().texOffs(12, 56).addBox(-3.5F, 0.0F, -0.4F, 7.0F, 1.0F, 0.4F), PartPose.offset(0.0F, -11.5F, -3.1F));
        PartDefinition p_skull = p_necklace.addOrReplaceChild("skull", CubeListBuilder.create().texOffs(34, 52).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 2.0F, 1.0F), PartPose.offset(0.0F, 1.0F, 0.0F));
        PartDefinition p_head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(74, 0).addBox(-4.0F, -6.0F, -5.0F, 8.0F, 6.0F, 8.0F), PartPose.offset(0.0F, 1.5F, -1.5F));
        PartDefinition p_snout = p_head.addOrReplaceChild("snout", CubeListBuilder.create().texOffs(34, 20).addBox(-3.0F, -2.5F, -10.0F, 6.0F, 3.0F, 10.0F), PartPose.offset(0.0F, -2.0F, -5.0F));
        PartDefinition p_nostrils = p_snout.addOrReplaceChild("nostrils", CubeListBuilder.create().texOffs(24, 52).addBox(-1.5F, -1.0F, -1.0F, 3.0F, 1.0F, 2.0F), PartPose.offset(0.0F, -2.5F, -9.0F));
        PartDefinition p_fang_0 = p_snout.addOrReplaceChild("fang_0", CubeListBuilder.create().texOffs(94, 52).addBox(-0.3F, 0.0F, -0.3F, 0.6F, 1.5F, 0.6F), PartPose.offset(-2.8049999999999997F, 0.5F, -8.5F));
        PartDefinition p_fang_1 = p_snout.addOrReplaceChild("fang_1", CubeListBuilder.create().texOffs(98, 52).addBox(-0.3F, 0.0F, -0.3F, 0.6F, 1.5F, 0.6F), PartPose.offset(2.8049999999999997F, 0.5F, -8.5F));
        PartDefinition p_jaw = p_head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(0, 20).addBox(-3.0F, 0.0F, -11.0F, 6.0F, 2.0F, 11.0F), PartPose.offsetAndRotation(0.0F, 0.0F, -4.0F, 0.25F, 0.0F, 0.0F));
        PartDefinition p_right_eye = p_head.addOrReplaceChild("right_eye", CubeListBuilder.create().texOffs(8, 52).addBox(-1.25F, -2.0F, -1.25F, 2.5F, 2.0F, 2.5F), PartPose.offset(-3.0F, -6.0F, -3.0F));
        PartDefinition p_left_eye = p_head.addOrReplaceChild("left_eye", CubeListBuilder.create().texOffs(16, 52).addBox(-1.25F, -2.0F, -1.25F, 2.5F, 2.0F, 2.5F), PartPose.offset(3.0F, -6.0F, -3.0F));
        PartDefinition p_hair = p_head.addOrReplaceChild("hair", CubeListBuilder.create().texOffs(0, 44).addBox(-4.5F, -1.5F, -2.0F, 9.0F, 2.5F, 6.5F), PartPose.offset(0.0F, -6.0F, 0.5F));
        PartDefinition p_hair_back = p_hair.addOrReplaceChild("hair_back", CubeListBuilder.create().texOffs(66, 20).addBox(-4.5F, 0.0F, -0.5F, 9.0F, 11.0F, 1.5F), PartPose.offset(0.0F, 0.5F, 4.0F));
        PartDefinition p_hair_r = p_hair.addOrReplaceChild("hair_r", CubeListBuilder.create().texOffs(24, 33).addBox(-1.0F, 0.0F, 0.0F, 1.5F, 7.0F, 4.0F), PartPose.offset(-4.5F, 0.5F, 0.0F));
        PartDefinition p_hair_l = p_hair.addOrReplaceChild("hair_l", CubeListBuilder.create().texOffs(36, 33).addBox(-0.5F, 0.0F, 0.0F, 1.5F, 7.0F, 4.0F), PartPose.offset(4.5F, 0.5F, 0.0F));
        PartDefinition p_bone_pin = p_hair.addOrReplaceChild("bone_pin", CubeListBuilder.create().texOffs(66, 44).addBox(-0.5F, -5.0F, -0.5F, 1.0F, 6.0F, 1.0F), PartPose.offsetAndRotation(2.5F, -1.0F, 1.0F, 0.0F, 0.0F, 0.6F));
        PartDefinition p_crow_feather = p_hair.addOrReplaceChild("crow_feather", CubeListBuilder.create().texOffs(30, 44).addBox(-0.25F, -6.0F, -0.75F, 0.5F, 6.0F, 1.5F), PartPose.offsetAndRotation(-2.5F, -1.0F, 2.0F, -0.3F, 0.0F, -0.5F));
        PartDefinition p_right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(88, 20).addBox(-2.5F, 0.0F, -1.5F, 3.0F, 9.0F, 3.0F), PartPose.offset(-5.5F, 3.0F, -0.5F));
        PartDefinition p_right_arm_sleeve = p_right_arm.addOrReplaceChild("right_arm_sleeve", CubeListBuilder.create().texOffs(34, 44).addBox(-2.2F, 0.0F, -2.2F, 4.4F, 3.0F, 4.4F), PartPose.offset(-1.0F, 7.0F, 0.0F));
        PartDefinition p_right_hand = p_right_arm.addOrReplaceChild("right_hand", CubeListBuilder.create().texOffs(70, 44).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 2.0F, 3.0F), PartPose.offset(-1.0F, 10.0F, 0.0F));
        PartDefinition p_right_arm_claw_0 = p_right_hand.addOrReplaceChild("right_arm_claw_0", CubeListBuilder.create().texOffs(102, 52).addBox(-0.3F, 0.0F, -0.3F, 0.6F, 2.0F, 0.6F), PartPose.offsetAndRotation(-0.9F, 2.0F, -1.0F, -0.4F, 0.0F, 0.0F));
        PartDefinition p_right_arm_claw_1 = p_right_hand.addOrReplaceChild("right_arm_claw_1", CubeListBuilder.create().texOffs(106, 52).addBox(-0.3F, 0.0F, -0.3F, 0.6F, 2.0F, 0.6F), PartPose.offsetAndRotation(0.0F, 2.0F, -1.0F, -0.4F, 0.0F, 0.0F));
        PartDefinition p_right_arm_claw_2 = p_right_hand.addOrReplaceChild("right_arm_claw_2", CubeListBuilder.create().texOffs(110, 52).addBox(-0.3F, 0.0F, -0.3F, 0.6F, 2.0F, 0.6F), PartPose.offsetAndRotation(0.9F, 2.0F, -1.0F, -0.4F, 0.0F, 0.0F));
        PartDefinition p_ladle = p_right_hand.addOrReplaceChild("ladle", CubeListBuilder.create().texOffs(112, 20).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 11.0F, 1.0F).texOffs(82, 44).addBox(-1.5F, 7.5F, -1.5F, 3.0F, 2.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 1.0F, -0.5F, -0.4F, 0.0F, 0.0F));
        PartDefinition p_left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(100, 20).addBox(-0.5F, 0.0F, -1.5F, 3.0F, 9.0F, 3.0F), PartPose.offset(5.5F, 3.0F, -0.5F));
        PartDefinition p_left_arm_sleeve = p_left_arm.addOrReplaceChild("left_arm_sleeve", CubeListBuilder.create().texOffs(50, 44).addBox(-2.2F, 0.0F, -2.2F, 4.4F, 3.0F, 4.4F), PartPose.offset(1.0F, 7.0F, 0.0F));
        PartDefinition p_left_hand = p_left_arm.addOrReplaceChild("left_hand", CubeListBuilder.create().texOffs(94, 44).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 2.0F, 3.0F), PartPose.offset(1.0F, 10.0F, 0.0F));
        PartDefinition p_left_arm_claw_0 = p_left_hand.addOrReplaceChild("left_arm_claw_0", CubeListBuilder.create().texOffs(114, 52).addBox(-0.3F, 0.0F, -0.3F, 0.6F, 2.0F, 0.6F), PartPose.offsetAndRotation(-0.9F, 2.0F, -1.0F, -0.4F, 0.0F, 0.0F));
        PartDefinition p_left_arm_claw_1 = p_left_hand.addOrReplaceChild("left_arm_claw_1", CubeListBuilder.create().texOffs(118, 52).addBox(-0.3F, 0.0F, -0.3F, 0.6F, 2.0F, 0.6F), PartPose.offsetAndRotation(0.0F, 2.0F, -1.0F, -0.4F, 0.0F, 0.0F));
        PartDefinition p_left_arm_claw_2 = p_left_hand.addOrReplaceChild("left_arm_claw_2", CubeListBuilder.create().texOffs(122, 52).addBox(-0.3F, 0.0F, -0.3F, 0.6F, 2.0F, 0.6F), PartPose.offsetAndRotation(0.9F, 2.0F, -1.0F, -0.4F, 0.0F, 0.0F));
        PartDefinition p_tail0 = root.addOrReplaceChild("tail0", CubeListBuilder.create().texOffs(0, 33).addBox(-2.5F, -2.0F, 0.0F, 5.0F, 4.0F, 7.0F), PartPose.offsetAndRotation(0.0F, 21.0F, 4.0F, -0.25F, 0.0F, 0.0F));
        PartDefinition p_tail1 = p_tail0.addOrReplaceChild("tail1", CubeListBuilder.create().texOffs(48, 33).addBox(-2.0F, -1.5F, 0.0F, 4.0F, 3.0F, 7.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 7.0F, 0.15F, 0.0F, 0.0F));
        PartDefinition p_tail2 = p_tail1.addOrReplaceChild("tail2", CubeListBuilder.create().texOffs(106, 33).addBox(-1.5F, -1.0F, 0.0F, 3.0F, 2.0F, 7.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 7.0F, 0.1F, 0.0F, 0.0F));
        PartDefinition p_tail0_ridge_0 = p_tail0.addOrReplaceChild("tail0_ridge_0", CubeListBuilder.create().texOffs(40, 52).addBox(-0.5F, -1.0F, -0.5F, 1.0F, 1.0F, 1.5F), PartPose.offset(0.0F, -2.0F, 1.0F));
        PartDefinition p_tail0_ridge_1 = p_tail0.addOrReplaceChild("tail0_ridge_1", CubeListBuilder.create().texOffs(46, 52).addBox(-0.5F, -1.0F, -0.5F, 1.0F, 1.0F, 1.5F), PartPose.offset(0.0F, -2.0F, 3.2F));
        PartDefinition p_tail0_ridge_2 = p_tail0.addOrReplaceChild("tail0_ridge_2", CubeListBuilder.create().texOffs(52, 52).addBox(-0.5F, -1.0F, -0.5F, 1.0F, 1.0F, 1.5F), PartPose.offset(0.0F, -2.0F, 5.4F));
        PartDefinition p_tail1_ridge_0 = p_tail1.addOrReplaceChild("tail1_ridge_0", CubeListBuilder.create().texOffs(58, 52).addBox(-0.5F, -1.0F, -0.5F, 1.0F, 1.0F, 1.5F), PartPose.offset(0.0F, -1.5F, 1.0F));
        PartDefinition p_tail1_ridge_1 = p_tail1.addOrReplaceChild("tail1_ridge_1", CubeListBuilder.create().texOffs(64, 52).addBox(-0.5F, -1.0F, -0.5F, 1.0F, 1.0F, 1.5F), PartPose.offset(0.0F, -1.5F, 3.2F));
        PartDefinition p_tail1_ridge_2 = p_tail1.addOrReplaceChild("tail1_ridge_2", CubeListBuilder.create().texOffs(70, 52).addBox(-0.5F, -1.0F, -0.5F, 1.0F, 1.0F, 1.5F), PartPose.offset(0.0F, -1.5F, 5.4F));
        PartDefinition p_tail2_ridge_0 = p_tail2.addOrReplaceChild("tail2_ridge_0", CubeListBuilder.create().texOffs(76, 52).addBox(-0.5F, -1.0F, -0.5F, 1.0F, 1.0F, 1.5F), PartPose.offset(0.0F, -1.0F, 1.0F));
        PartDefinition p_tail2_ridge_1 = p_tail2.addOrReplaceChild("tail2_ridge_1", CubeListBuilder.create().texOffs(82, 52).addBox(-0.5F, -1.0F, -0.5F, 1.0F, 1.0F, 1.5F), PartPose.offset(0.0F, -1.0F, 3.2F));
        PartDefinition p_tail2_ridge_2 = p_tail2.addOrReplaceChild("tail2_ridge_2", CubeListBuilder.create().texOffs(88, 52).addBox(-0.5F, -1.0F, -0.5F, 1.0F, 1.0F, 1.5F), PartPose.offset(0.0F, -1.0F, 5.4F));
        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void setupAnim(LoreRenderState state) {
        super.setupAnim(state);
        float t = state.ageInTicks;
        float pos = state.walkAnimationPos;
        float speed = state.walkAnimationSpeed;
        this.head.yRot = state.yRot * Mth.DEG_TO_RAD;
        this.head.xRot = state.xRot * Mth.DEG_TO_RAD;
        // anda balançando (a saia cobre as pernas)
        this.skirt.zRot = Mth.sin(pos * 0.6F) * 0.06F * speed;
        this.body.zRot = Mth.sin(pos * 0.6F) * 0.05F * speed;
        this.rightArm.xRot = Mth.cos(pos * 0.6F) * 0.5F * speed;
        this.leftArm.xRot = -Mth.cos(pos * 0.6F) * 0.5F * speed;
        this.jaw.xRot = 0.25F + Mth.sin(t * 0.1F) * 0.05F;
        if (state.casting) {
            // feitiço: bocarra aberta, braços erguidos mexendo a concha
            this.jaw.xRot = 0.75F + Mth.sin(t * 0.5F) * 0.15F;
            this.rightArm.xRot = -2.2F + Mth.sin(t * 0.4F) * 0.3F;
            this.rightArm.zRot = Mth.cos(t * 0.4F) * 0.3F;
            this.leftArm.xRot = -2.0F - Mth.sin(t * 0.4F) * 0.2F;
            this.leftArm.zRot = -0.3F;
        }
        float sway = Mth.sin(t * 0.09F + pos * 0.3F);
        this.tail0.yRot = sway * 0.25F;
        this.tail1.yRot = sway * 0.3F;
        this.tail2.yRot = sway * 0.35F;
    }
}

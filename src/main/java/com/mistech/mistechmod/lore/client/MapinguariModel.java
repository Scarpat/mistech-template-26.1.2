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

/** Mapinguari: gigante peludo de um olho só, boca na barriga e pés virados para trás. Geometria e textura: {@code tools/lore/models2.py}. */
public class MapinguariModel extends EntityModel<LoreRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Mistech.MODID, "mapinguari"), "main");

    private final ModelPart head, body, bellyLip, rightArm, leftArm, rightLeg, leftLeg;

    public MapinguariModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.bellyLip = this.body.getChild("belly_lip");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -15.0F, -6.0F, 16.0F, 16.0F, 12.0F), PartPose.offsetAndRotation(0.0F, 14.0F, 0.0F, 0.22F, 0.0F, 0.0F));
        PartDefinition p_belly_lip = p_body.addOrReplaceChild("belly_lip", CubeListBuilder.create().texOffs(0, 75).addBox(-5.0F, 0.0F, -1.0F, 10.0F, 2.0F, 1.5F), PartPose.offset(0.0F, -4.0F, -6.0F));
        PartDefinition p_back_armor = p_body.addOrReplaceChild("back_armor", CubeListBuilder.create().texOffs(78, 44).addBox(-6.5F, -1.5F, -4.0F, 13.0F, 3.0F, 9.0F), PartPose.offset(0.0F, -15.0F, 1.0F));
        PartDefinition p_back_armor2 = p_body.addOrReplaceChild("back_armor2", CubeListBuilder.create().texOffs(0, 58).addBox(-6.0F, 0.0F, -0.5F, 12.0F, 8.0F, 1.5F), PartPose.offset(0.0F, -11.0F, 6.0F));
        PartDefinition p_fringe = p_body.addOrReplaceChild("fringe", CubeListBuilder.create().texOffs(0, 28).addBox(-8.5F, 0.0F, -6.5F, 17.0F, 3.0F, 13.0F), PartPose.offset(0.0F, 1.0F, 0.0F));
        PartDefinition p_shag_0 = p_body.addOrReplaceChild("shag_0", CubeListBuilder.create().texOffs(40, 68).addBox(-1.5F, -3.5F, -1.0F, 3.0F, 4.0F, 2.0F), PartPose.offsetAndRotation(-4.5F, -16.0F, -2.0F, -0.5F, 0.0F, -0.3F));
        PartDefinition p_shag_1 = p_body.addOrReplaceChild("shag_1", CubeListBuilder.create().texOffs(50, 68).addBox(-1.5F, -3.5F, -1.0F, 3.0F, 4.0F, 2.0F), PartPose.offsetAndRotation(0.0F, -16.0F, -3.0F, -0.5F, 0.0F, 0.0F));
        PartDefinition p_shag_2 = p_body.addOrReplaceChild("shag_2", CubeListBuilder.create().texOffs(60, 68).addBox(-1.5F, -3.5F, -1.0F, 3.0F, 4.0F, 2.0F), PartPose.offsetAndRotation(4.5F, -16.0F, -2.0F, -0.5F, 0.0F, 0.3F));
        PartDefinition p_head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(56, 0).addBox(-5.5F, -9.0F, -5.0F, 11.0F, 9.0F, 9.0F), PartPose.offset(0.0F, 1.0F, -7.0F));
        PartDefinition p_brow = p_head.addOrReplaceChild("brow", CubeListBuilder.create().texOffs(48, 75).addBox(-4.0F, -1.0F, -1.2F, 8.0F, 1.5F, 1.2F), PartPose.offset(0.0F, -7.5F, -5.0F));
        PartDefinition p_muzzle = p_head.addOrReplaceChild("muzzle", CubeListBuilder.create().texOffs(70, 68).addBox(-2.0F, -1.0F, -1.5F, 4.0F, 3.0F, 1.5F), PartPose.offset(0.0F, -2.5F, -5.0F));
        PartDefinition p_mane = p_head.addOrReplaceChild("mane", CubeListBuilder.create().texOffs(48, 44).addBox(-6.0F, -1.0F, -2.0F, 12.0F, 10.0F, 3.0F), PartPose.offset(0.0F, -9.0F, 3.0F));
        PartDefinition p_right_ear = p_head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(66, 75).addBox(-1.0F, -2.5F, -0.5F, 2.0F, 2.5F, 1.0F), PartPose.offsetAndRotation(-5.5F, -8.0F, -1.0F, 0.0F, 0.0F, -0.7F));
        PartDefinition p_left_ear = p_head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(72, 75).addBox(-1.0F, -2.5F, -0.5F, 2.0F, 2.5F, 1.0F), PartPose.offsetAndRotation(5.5F, -8.0F, -1.0F, 0.0F, 0.0F, 0.7F));
        PartDefinition p_right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(96, 0).addBox(-3.0F, 0.0F, -2.0F, 4.0F, 13.0F, 4.0F), PartPose.offset(-9.0F, 0.0F, -2.0F));
        PartDefinition p_right_arm_pauldron = p_right_arm.addOrReplaceChild("right_arm_pauldron", CubeListBuilder.create().texOffs(28, 58).addBox(-3.0F, -1.5F, -3.0F, 6.0F, 4.0F, 6.0F), PartPose.offset(-1.0F, -0.5F, 0.0F));
        PartDefinition p_right_arm_fore = p_right_arm.addOrReplaceChild("right_arm_fore", CubeListBuilder.create().texOffs(60, 28).addBox(-3.0F, 0.0F, -3.0F, 6.0F, 8.0F, 6.0F).texOffs(0, 68).addBox(-2.5F, 7.5F, -2.5F, 5.0F, 2.0F, 5.0F), PartPose.offset(-1.0F, 12.0F, 0.0F));
        PartDefinition p_right_arm_claw_0 = p_right_arm_fore.addOrReplaceChild("right_arm_claw_0", CubeListBuilder.create().texOffs(82, 68).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(-1.6F, 9.5F, -1.5F, -0.35F, 0.0F, 0.0F));
        PartDefinition p_right_arm_claw_tip_0 = p_right_arm_claw_0.addOrReplaceChild("right_arm_claw_tip_0", CubeListBuilder.create().texOffs(24, 75).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 4.0F, 0.0F, -0.7F, 0.0F, 0.0F));
        PartDefinition p_right_arm_claw_1 = p_right_arm_fore.addOrReplaceChild("right_arm_claw_1", CubeListBuilder.create().texOffs(86, 68).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 9.5F, -1.5F, -0.35F, 0.0F, 0.0F));
        PartDefinition p_right_arm_claw_tip_1 = p_right_arm_claw_1.addOrReplaceChild("right_arm_claw_tip_1", CubeListBuilder.create().texOffs(28, 75).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 4.0F, 0.0F, -0.7F, 0.0F, 0.0F));
        PartDefinition p_right_arm_claw_2 = p_right_arm_fore.addOrReplaceChild("right_arm_claw_2", CubeListBuilder.create().texOffs(90, 68).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(1.6F, 9.5F, -1.5F, -0.35F, 0.0F, 0.0F));
        PartDefinition p_right_arm_claw_tip_2 = p_right_arm_claw_2.addOrReplaceChild("right_arm_claw_tip_2", CubeListBuilder.create().texOffs(32, 75).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 4.0F, 0.0F, -0.7F, 0.0F, 0.0F));
        PartDefinition p_left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(112, 0).addBox(-1.0F, 0.0F, -2.0F, 4.0F, 13.0F, 4.0F), PartPose.offset(9.0F, 0.0F, -2.0F));
        PartDefinition p_left_arm_pauldron = p_left_arm.addOrReplaceChild("left_arm_pauldron", CubeListBuilder.create().texOffs(52, 58).addBox(-3.0F, -1.5F, -3.0F, 6.0F, 4.0F, 6.0F), PartPose.offset(1.0F, -0.5F, 0.0F));
        PartDefinition p_left_arm_fore = p_left_arm.addOrReplaceChild("left_arm_fore", CubeListBuilder.create().texOffs(84, 28).addBox(-3.0F, 0.0F, -3.0F, 6.0F, 8.0F, 6.0F).texOffs(20, 68).addBox(-2.5F, 7.5F, -2.5F, 5.0F, 2.0F, 5.0F), PartPose.offset(1.0F, 12.0F, 0.0F));
        PartDefinition p_left_arm_claw_0 = p_left_arm_fore.addOrReplaceChild("left_arm_claw_0", CubeListBuilder.create().texOffs(94, 68).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(-1.6F, 9.5F, -1.5F, -0.35F, 0.0F, 0.0F));
        PartDefinition p_left_arm_claw_tip_0 = p_left_arm_claw_0.addOrReplaceChild("left_arm_claw_tip_0", CubeListBuilder.create().texOffs(36, 75).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 4.0F, 0.0F, -0.7F, 0.0F, 0.0F));
        PartDefinition p_left_arm_claw_1 = p_left_arm_fore.addOrReplaceChild("left_arm_claw_1", CubeListBuilder.create().texOffs(98, 68).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 9.5F, -1.5F, -0.35F, 0.0F, 0.0F));
        PartDefinition p_left_arm_claw_tip_1 = p_left_arm_claw_1.addOrReplaceChild("left_arm_claw_tip_1", CubeListBuilder.create().texOffs(40, 75).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 4.0F, 0.0F, -0.7F, 0.0F, 0.0F));
        PartDefinition p_left_arm_claw_2 = p_left_arm_fore.addOrReplaceChild("left_arm_claw_2", CubeListBuilder.create().texOffs(102, 68).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(1.6F, 9.5F, -1.5F, -0.35F, 0.0F, 0.0F));
        PartDefinition p_left_arm_claw_tip_2 = p_left_arm_claw_2.addOrReplaceChild("left_arm_claw_tip_2", CubeListBuilder.create().texOffs(44, 75).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 4.0F, 0.0F, -0.7F, 0.0F, 0.0F));
        PartDefinition p_right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 44).addBox(-3.0F, 0.0F, -3.0F, 6.0F, 8.0F, 6.0F), PartPose.offset(-4.5F, 15.0F, 1.0F));
        PartDefinition p_right_leg_foot = p_right_leg.addOrReplaceChild("right_leg_foot", CubeListBuilder.create().texOffs(76, 58).addBox(-3.0F, 0.0F, -2.5F, 6.0F, 1.0F, 7.0F), PartPose.offset(0.0F, 8.0F, 0.0F));
        PartDefinition p_right_leg_toe_0 = p_right_leg_foot.addOrReplaceChild("right_leg_toe_0", CubeListBuilder.create().texOffs(78, 75).addBox(-0.5F, 0.0F, 0.0F, 1.0F, 1.0F, 2.0F), PartPose.offset(-2.0F, 0.0F, 4.5F));
        PartDefinition p_right_leg_toe_1 = p_right_leg_foot.addOrReplaceChild("right_leg_toe_1", CubeListBuilder.create().texOffs(84, 75).addBox(-0.5F, 0.0F, 0.0F, 1.0F, 1.0F, 2.0F), PartPose.offset(0.0F, 0.0F, 4.5F));
        PartDefinition p_right_leg_toe_2 = p_right_leg_foot.addOrReplaceChild("right_leg_toe_2", CubeListBuilder.create().texOffs(90, 75).addBox(-0.5F, 0.0F, 0.0F, 1.0F, 1.0F, 2.0F), PartPose.offset(2.0F, 0.0F, 4.5F));
        PartDefinition p_left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(24, 44).addBox(-3.0F, 0.0F, -3.0F, 6.0F, 8.0F, 6.0F), PartPose.offset(4.5F, 15.0F, 1.0F));
        PartDefinition p_left_leg_foot = p_left_leg.addOrReplaceChild("left_leg_foot", CubeListBuilder.create().texOffs(102, 58).addBox(-3.0F, 0.0F, -2.5F, 6.0F, 1.0F, 7.0F), PartPose.offset(0.0F, 8.0F, 0.0F));
        PartDefinition p_left_leg_toe_0 = p_left_leg_foot.addOrReplaceChild("left_leg_toe_0", CubeListBuilder.create().texOffs(96, 75).addBox(-0.5F, 0.0F, 0.0F, 1.0F, 1.0F, 2.0F), PartPose.offset(-2.0F, 0.0F, 4.5F));
        PartDefinition p_left_leg_toe_1 = p_left_leg_foot.addOrReplaceChild("left_leg_toe_1", CubeListBuilder.create().texOffs(102, 75).addBox(-0.5F, 0.0F, 0.0F, 1.0F, 1.0F, 2.0F), PartPose.offset(0.0F, 0.0F, 4.5F));
        PartDefinition p_left_leg_toe_2 = p_left_leg_foot.addOrReplaceChild("left_leg_toe_2", CubeListBuilder.create().texOffs(108, 75).addBox(-0.5F, 0.0F, 0.0F, 1.0F, 1.0F, 2.0F), PartPose.offset(2.0F, 0.0F, 4.5F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(LoreRenderState state) {
        super.setupAnim(state);
        float t = state.ageInTicks;
        float pos = state.walkAnimationPos;
        float speed = state.walkAnimationSpeed;
        float swing = Mth.cos(pos * 0.5F) * 0.9F * speed;
        this.rightLeg.xRot = swing;
        this.leftLeg.xRot = -swing;
        // braços longos pendurados, balançando como um preguiça-gigante
        this.rightArm.xRot = -swing * 0.7F + Mth.sin(t * 0.06F) * 0.05F;
        this.leftArm.xRot = swing * 0.7F - Mth.sin(t * 0.06F) * 0.05F;
        this.rightArm.zRot = 0.12F;
        this.leftArm.zRot = -0.12F;
        float bob = Math.abs(Mth.sin(pos * 0.5F)) * speed * 1.5F;
        this.body.y += bob;
        this.head.y += bob;
        this.body.zRot = Mth.sin(pos * 0.5F) * 0.06F * speed;
        this.head.yRot = state.yRot * Mth.DEG_TO_RAD * 0.6F;
        this.head.xRot = state.xRot * Mth.DEG_TO_RAD * 0.6F;
        // boca da barriga respira; no urro escancara
        this.bellyLip.y += 0.3F + Mth.sin(t * 0.08F) * 0.3F;
        if (state.angry) {
            this.bellyLip.y += 2.5F + Mth.sin(t * 0.9F) * 0.6F;
            this.rightArm.xRot = -2.6F + Mth.sin(t * 0.5F) * 0.2F;
            this.leftArm.xRot = -2.6F - Mth.sin(t * 0.5F) * 0.2F;
            this.rightArm.zRot = 0.5F;
            this.leftArm.zRot = -0.5F;
            this.head.xRot -= 0.4F;
        }
    }
}

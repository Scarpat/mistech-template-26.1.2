package com.mistech.mistechmod.realm.dream.client;

import com.mistech.mistechmod.Mistech;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Bicho-Papão: fera corcunda, bocão de orelha a orelha, chifre torto e o cobertor da cama nas costas. Na luz se encolhe e cobre o rosto. Geometria e textura: {@code tools/dream/mobs.py}. */
public class BogeymanModel extends EntityModel<DreamMobRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Mistech.MODID, "bogeyman"), "main");
    private final ModelPart rightLeg, leftLeg, torso, quilt, head, jaw, rightArm, leftArm;

    public BogeymanModel(ModelPart root) {
        super(root);
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
        this.torso = root.getChild("torso");
        this.quilt = this.torso.getChild("quilt");
        this.head = this.torso.getChild("head");
        this.jaw = this.head.getChild("jaw");
        this.rightArm = this.torso.getChild("right_arm");
        this.leftArm = this.torso.getChild("left_arm");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(82, 0).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 13.0F, 3.0F).texOffs(90, 25).addBox(-2.0F, 11.0F, -4.5F, 4.0F, 2.0F, 5.0F), PartPose.offset(-3.0F, 11.0F, 3.0F));
        PartDefinition p_left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(94, 0).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 13.0F, 3.0F).texOffs(108, 25).addBox(-2.0F, 11.0F, -4.5F, 4.0F, 2.0F, 5.0F), PartPose.offset(3.0F, 11.0F, 3.0F));
        PartDefinition p_torso = root.addOrReplaceChild("torso", CubeListBuilder.create().texOffs(24, 0).addBox(-5.0F, -17.0F, -3.0F, 10.0F, 17.0F, 6.0F).texOffs(36, 25).addBox(-4.0F, -17.0F, 2.0F, 8.0F, 9.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 12.0F, 3.0F, 0.95F, 0.0F, 0.0F));
        PartDefinition p_quilt = p_torso.addOrReplaceChild("quilt", CubeListBuilder.create().texOffs(56, 0).addBox(-6.0F, 0.0F, 0.0F, 12.0F, 16.0F, 1.0F), PartPose.offsetAndRotation(0.0F, -17.0F, 5.0F, -0.12F, 0.0F, 0.0F));
        PartDefinition p_head = p_torso.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 25).addBox(-5.0F, -7.0F, -6.0F, 10.0F, 7.0F, 8.0F).texOffs(0, 40).addBox(-2.0F, -9.0F, -3.0F, 4.0F, 2.0F, 4.0F), PartPose.offsetAndRotation(0.0F, -17.0F, -1.0F, -0.95F, 0.0F, 0.0F));
        PartDefinition p_right_horn = p_head.addOrReplaceChild("right_horn", CubeListBuilder.create().texOffs(48, 40).addBox(-1.0F, -4.0F, -1.0F, 2.0F, 4.0F, 2.0F), PartPose.offsetAndRotation(-4.0F, -6.0F, -1.0F, 0.0F, 0.0F, -0.7F));
        PartDefinition p_right_horn_tip = p_right_horn.addOrReplaceChild("right_horn_tip", CubeListBuilder.create().texOffs(88, 40).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, -4.0F, 0.0F, 0.0F, 0.0F, 0.9F));
        PartDefinition p_left_horn = p_head.addOrReplaceChild("left_horn", CubeListBuilder.create().texOffs(80, 40).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 2.0F, 2.0F), PartPose.offsetAndRotation(4.0F, -6.0F, -1.0F, 0.0F, 0.0F, 0.5F));
        PartDefinition p_jaw = p_head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(58, 25).addBox(-4.5F, 0.0F, -5.5F, 9.0F, 2.0F, 7.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.15F, 0.0F, 0.0F));
        PartDefinition p_right_arm = p_torso.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(0, 0).addBox(-1.5F, -1.0F, -1.5F, 3.0F, 22.0F, 3.0F), PartPose.offsetAndRotation(-6.0F, -15.0F, 0.0F, -0.95F, 0.0F, 0.0F));
        PartDefinition p_right_hand = p_right_arm.addOrReplaceChild("right_hand", CubeListBuilder.create().texOffs(16, 40).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 2.0F, 4.0F), PartPose.offset(0.0F, 21.0F, 0.0F));
        PartDefinition p_right_arm_claw_0 = p_right_hand.addOrReplaceChild("right_arm_claw_0", CubeListBuilder.create().texOffs(56, 40).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(-1.5F, 2.0F, -1.5F, -0.35F, 0.0F, 0.0F));
        PartDefinition p_right_arm_claw_1 = p_right_hand.addOrReplaceChild("right_arm_claw_1", CubeListBuilder.create().texOffs(60, 40).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 2.0F, -1.5F, -0.35F, 0.0F, 0.0F));
        PartDefinition p_right_arm_claw_2 = p_right_hand.addOrReplaceChild("right_arm_claw_2", CubeListBuilder.create().texOffs(64, 40).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(1.5F, 2.0F, -1.5F, -0.35F, 0.0F, 0.0F));
        PartDefinition p_left_arm = p_torso.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(12, 0).addBox(-1.5F, -1.0F, -1.5F, 3.0F, 22.0F, 3.0F), PartPose.offsetAndRotation(6.0F, -15.0F, 0.0F, -0.95F, 0.0F, 0.0F));
        PartDefinition p_left_hand = p_left_arm.addOrReplaceChild("left_hand", CubeListBuilder.create().texOffs(32, 40).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 2.0F, 4.0F), PartPose.offset(0.0F, 21.0F, 0.0F));
        PartDefinition p_left_arm_claw_0 = p_left_hand.addOrReplaceChild("left_arm_claw_0", CubeListBuilder.create().texOffs(68, 40).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(-1.5F, 2.0F, -1.5F, -0.35F, 0.0F, 0.0F));
        PartDefinition p_left_arm_claw_1 = p_left_hand.addOrReplaceChild("left_arm_claw_1", CubeListBuilder.create().texOffs(72, 40).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 2.0F, -1.5F, -0.35F, 0.0F, 0.0F));
        PartDefinition p_left_arm_claw_2 = p_left_hand.addOrReplaceChild("left_arm_claw_2", CubeListBuilder.create().texOffs(76, 40).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(1.5F, 2.0F, -1.5F, -0.35F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void setupAnim(DreamMobRenderState state) {
        super.setupAnim(state);
        float pos = state.walkAnimationPos, speed = state.walkAnimationSpeed, age = state.ageInTicks;
        // anda meio de quatro: braço e perna opostos juntos
        this.rightLeg.xRot = Mth.cos(pos * 0.55F) * 0.9F * speed;
        this.leftLeg.xRot = Mth.cos(pos * 0.55F + Mth.PI) * 0.9F * speed;
        this.rightArm.xRot = -0.95F + Mth.cos(pos * 0.55F + Mth.PI) * 0.8F * speed;
        this.leftArm.xRot = -0.95F + Mth.cos(pos * 0.55F) * 0.8F * speed;
        this.rightArm.zRot = 0.1F + Mth.sin(age * 0.06F) * 0.05F;
        this.leftArm.zRot = -0.1F - Mth.sin(age * 0.06F) * 0.05F;
        // cabeça inquieta, dentes batendo
        this.head.yRot = state.yRot * Mth.DEG_TO_RAD * 0.7F;
        this.head.xRot = -0.95F + state.xRot * Mth.DEG_TO_RAD * 0.5F;
        this.head.zRot = Mth.sin(age * 0.07F) * 0.12F;
        this.jaw.xRot = 0.15F + Math.max(0.0F, Mth.sin(age * 0.35F)) * 0.12F;
        this.quilt.xRot = -0.12F - Mth.sin(pos * 0.55F) * 0.15F * speed - Mth.sin(age * 0.05F) * 0.04F;
        this.torso.xRot = 0.95F;
        if (state.attackTime > 0.0F) {
            float a = Mth.sin(state.attackTime * Mth.PI);
            this.rightArm.xRot -= a * 1.4F;
            this.leftArm.xRot -= a * 1.4F;
            this.jaw.xRot += a * 0.7F;
        }
        if (state.bright) {
            // na luz forte: se encolhe todo e cobre o rosto com os braços
            this.torso.xRot = 1.25F;
            this.head.xRot = -0.6F;
            this.rightArm.xRot = -2.3F;
            this.leftArm.xRot = -2.3F;
            this.rightArm.zRot = -0.45F;
            this.leftArm.zRot = 0.45F;
            this.jaw.xRot = 0.0F;
        }
    }
}

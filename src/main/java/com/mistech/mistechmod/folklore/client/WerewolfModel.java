package com.mistech.mistechmod.folklore.client;

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


/** Lobisomem (lobo-guará): modelo do Blockbench feito pelo usuário; pivôs de pernas e braços movidos para o quadril e os ombros. Geometria e textura geradas por script (gen/werewolf.py). */
public class WerewolfModel extends EntityModel<WerewolfRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Mistech.MODID, "werewolf"), "main");

    private final ModelPart torso;
    private final ModelPart head;
    private final ModelPart rightEar;
    private final ModelPart leftEar;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    private final ModelPart tail;

    public WerewolfModel(ModelPart root) {
        super(root);
        this.torso = root.getChild("torso");
        this.head = root.getChild("head");
        this.rightEar = root.getChild("head").getChild("right_ear");
        this.leftEar = root.getChild("head").getChild("left_ear");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
        this.tail = root.getChild("tail");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(18, 35).addBox(-1.0F, 7.5F, -3.0F, 2.0F, 1.0F, 3.0F), PartPose.offset(-3.0F, 15.5F, 0.0F));
        PartDefinition p_cube_r1 = p_right_leg.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(34, 0).addBox(-1.0F, -3.0F, -1.0F, 2.0F, 3.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 2.2F, -0.2F, -0.7418F, 0.0F, 0.0F));
        PartDefinition p_cube_r2 = p_right_leg.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(20, 0).addBox(-1.0F, -1.0F, -2.0F, 2.0F, 1.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 3.5F, -0.1F, -0.7418F, 0.0F, 0.0F));
        PartDefinition p_cube_r3 = p_right_leg.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(0, 17).addBox(-1.0F, -1.0F, -2.0F, 2.0F, 1.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 7.2F, 1.0F, 0.6981F, 0.0F, 0.0F));
        PartDefinition p_left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(28, 36).addBox(-1.0F, 7.5F, -3.0F, 2.0F, 1.0F, 3.0F), PartPose.offset(3.0F, 15.5F, 0.0F));
        PartDefinition p_cube_r4 = p_left_leg.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(0, 35).addBox(-1.0F, -3.0F, -1.0F, 2.0F, 3.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 2.2F, -0.2F, -0.7418F, 0.0F, 0.0F));
        PartDefinition p_cube_r5 = p_left_leg.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(14, 24).addBox(-1.0F, -1.0F, -2.0F, 2.0F, 1.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 3.5F, -0.1F, -0.7418F, 0.0F, 0.0F));
        PartDefinition p_cube_r6 = p_left_leg.addOrReplaceChild("cube_r6", CubeListBuilder.create().texOffs(0, 23).addBox(-1.0F, -1.0F, -2.0F, 2.0F, 1.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 7.2F, 1.0F, 0.6981F, 0.0F, 0.0F));
        PartDefinition p_left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(4.0F, 8.0F, -3.2F));
        PartDefinition p_cube_r7 = p_left_arm.addOrReplaceChild("cube_r7", CubeListBuilder.create().texOffs(38, 36).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 4.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 1.7000000000000002F, 2.220446049250313e-16F, -0.2182F, 0.0F, 0.0F));
        PartDefinition p_cube_r8 = p_left_arm.addOrReplaceChild("cube_r8", CubeListBuilder.create().texOffs(28, 24).addBox(-1.0F, -1.0F, -2.0F, 2.0F, 1.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 2.3F, 0.8000000000000003F, -1.789F, 0.0F, 0.0F));
        PartDefinition p_cube_r9 = p_left_arm.addOrReplaceChild("cube_r9", CubeListBuilder.create().texOffs(32, 6).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 1.0F, 4.0F), PartPose.offsetAndRotation(0.0F, 7.7F, -0.7999999999999998F, 0.829F, 0.0F, 0.0F));
        PartDefinition p_cube_r10 = p_left_arm.addOrReplaceChild("cube_r10", CubeListBuilder.create().texOffs(30, 21).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 9.8F, -2.0999999999999996F, 1.3963F, 0.0F, 0.0F));
        PartDefinition p_right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-4.0F, 8.0F, -3.2F));
        PartDefinition p_cube_r11 = p_right_arm.addOrReplaceChild("cube_r11", CubeListBuilder.create().texOffs(18, 39).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 4.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 1.7000000000000002F, 2.220446049250313e-16F, -0.2182F, 0.0F, 0.0F));
        PartDefinition p_cube_r12 = p_right_arm.addOrReplaceChild("cube_r12", CubeListBuilder.create().texOffs(0, 29).addBox(-1.0F, -1.0F, -2.0F, 2.0F, 1.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 2.3F, 0.8000000000000003F, -1.789F, 0.0F, 0.0F));
        PartDefinition p_cube_r13 = p_right_arm.addOrReplaceChild("cube_r13", CubeListBuilder.create().texOffs(32, 11).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 1.0F, 4.0F), PartPose.offsetAndRotation(0.0F, 7.7F, -0.7999999999999998F, 0.829F, 0.0F, 0.0F));
        PartDefinition p_cube_r14 = p_right_arm.addOrReplaceChild("cube_r14", CubeListBuilder.create().texOffs(42, 25).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 9.8F, -2.0999999999999996F, 1.3963F, 0.0F, 0.0F));
        PartDefinition p_torso = root.addOrReplaceChild("torso", CubeListBuilder.create(), PartPose.offset(-1.0F, 16.0F, 0.0F));
        PartDefinition p_cube_r15 = p_torso.addOrReplaceChild("cube_r15", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -4.0F, -1.0F, 6.0F, 4.0F, 4.0F), PartPose.offsetAndRotation(0.0F, -3.8F, -3.3F, 0.4276F, 0.0F, 0.0F));
        PartDefinition p_cube_r16 = p_torso.addOrReplaceChild("cube_r16", CubeListBuilder.create().texOffs(16, 16).addBox(-1.0F, -4.0F, 0.0F, 4.0F, 5.0F, 3.0F), PartPose.offsetAndRotation(0.0F, -1.8F, -2.3F, 0.5585F, 0.0F, 0.0F));
        PartDefinition p_cube_r17 = p_torso.addOrReplaceChild("cube_r17", CubeListBuilder.create().texOffs(16, 8).addBox(-1.0F, -2.0F, -1.0F, 4.0F, 4.0F, 4.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.6981F, 0.0F, 0.0F));
        PartDefinition p_head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(-1.7F, 9.4F, -6.8F));
        PartDefinition p_cube_r18 = p_head.addOrReplaceChild("cube_r18", CubeListBuilder.create().texOffs(10, 35).addBox(0.0F, -8.0F, 1.0F, 2.0F, 6.0F, 2.0F).texOffs(0, 8).addBox(-1.0F, -5.0F, 1.0F, 4.0F, 5.0F, 4.0F), PartPose.offsetAndRotation(0.7F, -1.2F, 3.0F, 1.9548F, 0.0F, 0.0F));
        PartDefinition p_cube_r19 = p_head.addOrReplaceChild("cube_r19", CubeListBuilder.create().texOffs(28, 30).addBox(-1.0F, -4.0F, 0.0F, 4.0F, 4.0F, 2.0F), PartPose.offsetAndRotation(0.7F, 0.8F, 3.0F, 0.9948F, 0.0F, 0.0F));
        PartDefinition p_right_ear = p_head.addOrReplaceChild("right_ear", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.3054F));
        PartDefinition p_cube_r20 = p_right_ear.addOrReplaceChild("cube_r20", CubeListBuilder.create().texOffs(40, 30).addBox(0.0F, -7.0F, 1.0F, 3.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.5149F, 0.0F, 0.0F));
        PartDefinition p_cube_r21 = p_right_ear.addOrReplaceChild("cube_r21", CubeListBuilder.create().texOffs(36, 42).addBox(0.0F, -6.0F, 1.0F, 2.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, -2.0F, -1.0F, 0.5149F, 0.0F, 0.0F));
        PartDefinition p_left_ear = p_head.addOrReplaceChild("left_ear", CubeListBuilder.create(), PartPose.offsetAndRotation(0.5F, -0.9F, 0.0F, 0.0F, 0.0F, 0.3054F));
        PartDefinition p_cube_r22 = p_left_ear.addOrReplaceChild("cube_r22", CubeListBuilder.create().texOffs(0, 41).addBox(0.0F, -7.0F, 1.0F, 3.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.5149F, 0.0F, 0.0F));
        PartDefinition p_cube_r23 = p_left_ear.addOrReplaceChild("cube_r23", CubeListBuilder.create().texOffs(42, 42).addBox(0.0F, -6.0F, 1.0F, 2.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(1.0F, -2.0F, -1.0F, 0.5149F, 0.0F, 0.0F));
        PartDefinition p_tail = root.addOrReplaceChild("tail", CubeListBuilder.create(), PartPose.offset(-1.0F, 16.0F, 4.0F));
        PartDefinition p_cube_r24 = p_tail.addOrReplaceChild("cube_r24", CubeListBuilder.create().texOffs(26, 40).addBox(0.0F, -2.0F, -1.0F, 2.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.6981F, 0.0F, 0.0F));
        PartDefinition p_cube_r25 = p_tail.addOrReplaceChild("cube_r25", CubeListBuilder.create().texOffs(14, 30).addBox(-1.0F, -3.0F, -2.0F, 4.0F, 2.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 1.0F, 0.9F, -0.5672F, 0.0F, 0.0F));
        PartDefinition p_cube_r26 = p_tail.addOrReplaceChild("cube_r26", CubeListBuilder.create().texOffs(30, 16).addBox(-1.0F, -3.0F, -2.0F, 4.0F, 2.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 2.1F, 1.4F, -1.2654F, 0.0F, 0.0F));
        PartDefinition p_cube_r27 = p_tail.addOrReplaceChild("cube_r27", CubeListBuilder.create().texOffs(42, 21).addBox(0.0F, -3.0F, -2.0F, 2.0F, 2.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 4.1F, 1.4F, -1.7017F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(WerewolfRenderState state) {
        super.setupAnim(state);
        float speed = Math.min(1.0F, state.walkAnimationSpeed);
        float pos = state.walkAnimationPos;
        float t = state.ageInTicks;
        this.head.yRot = state.yRot * Mth.DEG_TO_RAD * 0.8F;
        this.head.xRot = state.xRot * Mth.DEG_TO_RAD * 0.6F;
        this.torso.xRot += Mth.sin(t * 0.08F) * 0.03F;
        this.rightLeg.xRot = Mth.cos(pos * 0.6662F) * 0.9F * speed;
        this.leftLeg.xRot = Mth.cos(pos * 0.6662F + Mth.PI) * 0.9F * speed;
        this.rightArm.xRot = Mth.cos(pos * 0.6662F + Mth.PI) * 0.7F * speed;
        this.leftArm.xRot = Mth.cos(pos * 0.6662F) * 0.7F * speed;
        if (state.attackAnim > 0.0F) {
            float swing = Mth.sin(state.attackAnim * Mth.PI);
            this.rightArm.xRot -= swing * 1.4F;
            this.leftArm.xRot -= swing * 1.1F;
            this.head.xRot += swing * 0.3F;
        }
        // orelhas mexendo de vez em quando
        float twitch = Mth.sin(t * 0.07F) > 0.93F ? 0.25F : 0.0F;
        this.rightEar.zRot -= twitch;
        this.leftEar.zRot += twitch * 0.6F;
        this.tail.yRot = Mth.sin(t * 0.12F) * 0.2F + Mth.cos(pos * 0.6662F) * 0.25F * speed;
    }
}

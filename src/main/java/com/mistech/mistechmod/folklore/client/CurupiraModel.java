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


/** Curupira: menino de cabelo de fogo, saia de folhas e pés virados para trás. Geometria e textura geradas por script (gen/curupira.py). */
public class CurupiraModel extends EntityModel<FolkRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Mistech.MODID, "curupira"), "main");

    private final ModelPart body;
    private final ModelPart skirt;
    private final ModelPart head;
    private final ModelPart flameA;
    private final ModelPart flameB;
    private final ModelPart flameC;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public CurupiraModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.skirt = root.getChild("skirt");
        this.head = root.getChild("head");
        this.flameA = root.getChild("head").getChild("hair").getChild("flame_a");
        this.flameB = root.getChild("head").getChild("hair").getChild("flame_b");
        this.flameC = root.getChild("head").getChild("hair").getChild("flame_c");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(8, 37).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F).texOffs(48, 37).addBox(-1.0F, 5.0F, -1.0F, 2.0F, 1.0F, 4.0F), PartPose.offset(-1.2F, 18.0F, 0.0F));
        PartDefinition p_left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(16, 37).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F).texOffs(0, 45).addBox(-1.0F, 5.0F, -1.0F, 2.0F, 1.0F, 4.0F), PartPose.offset(1.2F, 18.0F, 0.0F));
        PartDefinition p_skirt = root.addOrReplaceChild("skirt", CubeListBuilder.create().texOffs(36, 16).addBox(-3.5F, 0.0F, -2.5F, 7.0F, 4.0F, 5.0F), PartPose.offset(0.0F, 15.0F, 0.0F));
        PartDefinition p_body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(34, 28).addBox(-2.5F, 0.0F, -1.5F, 5.0F, 5.0F, 3.0F), PartPose.offset(0.0F, 10.0F, 0.0F));
        PartDefinition p_head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F), PartPose.offset(0.0F, 10.0F, 0.0F));
        PartDefinition p_nose = p_head.addOrReplaceChild("nose", CubeListBuilder.create().texOffs(12, 45).addBox(-0.5F, 0.0F, -1.0F, 1.0F, 1.0F, 1.0F), PartPose.offset(0.0F, -4.0F, -4.0F));
        PartDefinition p_hair = p_head.addOrReplaceChild("hair", CubeListBuilder.create().texOffs(0, 16).addBox(-4.5F, -2.0F, -4.5F, 9.0F, 3.0F, 9.0F), PartPose.offset(0.0F, -8.0F, 0.0F));
        PartDefinition p_mane = p_hair.addOrReplaceChild("mane", CubeListBuilder.create().texOffs(12, 28).addBox(-4.5F, 0.0F, -0.5F, 9.0F, 6.0F, 2.0F), PartPose.offset(0.0F, 0.0F, 4.0F));
        PartDefinition p_flame_a = p_hair.addOrReplaceChild("flame_a", CubeListBuilder.create().texOffs(24, 37).addBox(-1.0F, -4.0F, -1.0F, 2.0F, 4.0F, 2.0F), PartPose.offsetAndRotation(-2.5F, -2.0F, 0.0F, -0.2F, 0.0F, -0.35F));
        PartDefinition p_flame_b = p_hair.addOrReplaceChild("flame_b", CubeListBuilder.create().texOffs(0, 28).addBox(-1.5F, -6.0F, -1.5F, 3.0F, 6.0F, 3.0F), PartPose.offsetAndRotation(0.0F, -2.0F, 0.5F, -0.25F, 0.0F, 0.0F));
        PartDefinition p_flame_c = p_hair.addOrReplaceChild("flame_c", CubeListBuilder.create().texOffs(32, 37).addBox(-1.0F, -4.0F, -1.0F, 2.0F, 4.0F, 2.0F), PartPose.offsetAndRotation(2.5F, -2.0F, 0.0F, -0.2F, 0.0F, 0.35F));
        PartDefinition p_flame_d = p_hair.addOrReplaceChild("flame_d", CubeListBuilder.create().texOffs(40, 37).addBox(-1.0F, -4.0F, -1.0F, 2.0F, 4.0F, 2.0F), PartPose.offsetAndRotation(0.0F, -1.0F, 3.0F, -0.7F, 0.0F, 0.0F));
        PartDefinition p_right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(50, 28).addBox(-2.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F), PartPose.offset(-2.5F, 10.5F, 0.0F));
        PartDefinition p_left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(0, 37).addBox(0.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F), PartPose.offset(2.5F, 10.5F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(FolkRenderState state) {
        super.setupAnim(state);
        float speed = state.walkAnimationSpeed;
        float pos = state.walkAnimationPos;
        this.head.yRot = state.yRot * Mth.DEG_TO_RAD;
        this.head.xRot = state.xRot * Mth.DEG_TO_RAD;
        this.rightLeg.xRot = Mth.cos(pos * 0.6662F) * 1.2F * speed;
        this.leftLeg.xRot = Mth.cos(pos * 0.6662F + Mth.PI) * 1.2F * speed;
        this.rightArm.xRot = Mth.cos(pos * 0.6662F + Mth.PI) * 1.0F * speed;
        this.leftArm.xRot = Mth.cos(pos * 0.6662F) * 1.0F * speed;
        this.skirt.xRot = Math.abs(Mth.cos(pos * 0.6662F)) * 0.08F * speed;
        if (state.sitting) {
            for (ModelPart p : new ModelPart[] {this.body, this.skirt, this.head, this.rightArm, this.leftArm}) {
                p.y += 5.0F;
            }
            this.rightLeg.y += 5.0F;
            this.leftLeg.y += 5.0F;
            this.rightLeg.xRot = -1.4F;
            this.leftLeg.xRot = -1.4F;
            this.rightLeg.yRot = 0.3F;
            this.leftLeg.yRot = -0.3F;
        }
        // o cabelo de fogo tremula
        float t = state.ageInTicks;
        this.flameA.yScale = 1.0F + Mth.sin(t * 0.45F) * 0.18F;
        this.flameB.yScale = 1.0F + Mth.sin(t * 0.5F + 1.3F) * 0.22F;
        this.flameC.yScale = 1.0F + Mth.sin(t * 0.4F + 2.1F) * 0.18F;
        this.flameB.zRot += Mth.sin(t * 0.2F) * 0.08F;
    }
}

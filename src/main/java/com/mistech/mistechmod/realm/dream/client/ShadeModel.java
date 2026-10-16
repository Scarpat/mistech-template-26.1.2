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

/** Sombra: figura alta de capuz e máscara de porcelana, flutua sem pernas. Encarada, congela com os dedos estendidos para você. Geometria e textura: {@code tools/dream/mobs.py}. */
public class ShadeModel extends EntityModel<DreamMobRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Mistech.MODID, "shade"), "main");
    private final ModelPart head, body, skirt, rightArm, leftArm;

    public ShadeModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.skirt = this.body.getChild("skirt");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -9.0F, -4.0F, 8.0F, 9.0F, 8.0F).texOffs(4, 32).addBox(-2.0F, -11.0F, -1.0F, 4.0F, 2.0F, 4.0F), PartPose.offset(0.0F, -1.0F, 0.0F));
        PartDefinition p_mask = p_head.addOrReplaceChild("mask", CubeListBuilder.create().texOffs(46, 17).addBox(-3.0F, -3.5F, -0.5F, 6.0F, 7.0F, 1.0F), PartPose.offset(0.0F, -4.5F, -3.6F));
        PartDefinition p_body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 17).addBox(-3.5F, 0.0F, -2.0F, 7.0F, 10.0F, 4.0F), PartPose.offset(0.0F, -1.0F, 0.0F));
        PartDefinition p_skirt = p_body.addOrReplaceChild("skirt", CubeListBuilder.create().texOffs(32, 0).addBox(-4.5F, 0.0F, -3.0F, 9.0F, 9.0F, 6.0F), PartPose.offset(0.0F, 10.0F, 0.0F));
        PartDefinition p_wisp_0 = p_skirt.addOrReplaceChild("wisp_0", CubeListBuilder.create().texOffs(20, 32).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 4.0F, 2.0F), PartPose.offset(-2.5F, 9.0F, -1.0F));
        PartDefinition p_wisp_1 = p_skirt.addOrReplaceChild("wisp_1", CubeListBuilder.create().texOffs(44, 32).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F), PartPose.offset(1.5F, 9.0F, 0.0F));
        PartDefinition p_wisp_2 = p_skirt.addOrReplaceChild("wisp_2", CubeListBuilder.create().texOffs(28, 32).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 4.0F, 2.0F), PartPose.offset(-0.5F, 9.0F, 1.5F));
        PartDefinition p_right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(0, 17).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 13.0F, 2.0F), PartPose.offset(-4.5F, 1.0F, 0.0F));
        PartDefinition p_right_arm_finger_0 = p_right_arm.addOrReplaceChild("right_arm_finger_0", CubeListBuilder.create().texOffs(60, 17).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 6.0F, 1.0F, new CubeDeformation(-0.2F, 0.0F, -0.2F)), PartPose.offset(-0.6F, 12.0F, -0.3F));
        PartDefinition p_right_arm_finger_1 = p_right_arm.addOrReplaceChild("right_arm_finger_1", CubeListBuilder.create().texOffs(36, 32).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 5.0F, 1.0F, new CubeDeformation(-0.2F, 0.0F, -0.2F)), PartPose.offset(0.0F, 12.0F, 0.0F));
        PartDefinition p_right_arm_finger_2 = p_right_arm.addOrReplaceChild("right_arm_finger_2", CubeListBuilder.create().texOffs(38, 17).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 8.0F, 1.0F, new CubeDeformation(-0.2F, 0.0F, -0.2F)), PartPose.offset(0.6F, 12.0F, -0.3F));
        PartDefinition p_left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(8, 17).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 13.0F, 2.0F), PartPose.offset(4.5F, 1.0F, 0.0F));
        PartDefinition p_left_arm_finger_0 = p_left_arm.addOrReplaceChild("left_arm_finger_0", CubeListBuilder.create().texOffs(0, 32).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 6.0F, 1.0F, new CubeDeformation(-0.2F, 0.0F, -0.2F)), PartPose.offset(-0.6F, 12.0F, -0.3F));
        PartDefinition p_left_arm_finger_1 = p_left_arm.addOrReplaceChild("left_arm_finger_1", CubeListBuilder.create().texOffs(40, 32).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 5.0F, 1.0F, new CubeDeformation(-0.2F, 0.0F, -0.2F)), PartPose.offset(0.0F, 12.0F, 0.0F));
        PartDefinition p_left_arm_finger_2 = p_left_arm.addOrReplaceChild("left_arm_finger_2", CubeListBuilder.create().texOffs(42, 17).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 8.0F, 1.0F, new CubeDeformation(-0.2F, 0.0F, -0.2F)), PartPose.offset(0.6F, 12.0F, -0.3F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(DreamMobRenderState state) {
        super.setupAnim(state);
        float speed = state.walkAnimationSpeed, age = state.ageInTicks;
        if (state.watched) {
            // congelada: cabeça torta, um braço estendido para você, a capa parada no ar
            this.head.yRot = state.yRot * Mth.DEG_TO_RAD;
            this.head.xRot = state.xRot * Mth.DEG_TO_RAD;
            this.head.zRot = 0.35F;
            this.rightArm.xRot = -1.5F;
            this.rightArm.zRot = 0.05F;
            this.leftArm.xRot = -0.2F;
            this.leftArm.zRot = -0.15F;
            this.skirt.xRot = 0.25F;
            return;
        }
        // flutua: sobe e desce devagar, a capa ondula e se arrasta para trás quando anda
        float hover = Mth.sin(age * 0.08F) * 0.8F;
        this.head.y += hover;
        this.body.y += hover;
        this.rightArm.y += hover;
        this.leftArm.y += hover;
        this.head.yRot = state.yRot * Mth.DEG_TO_RAD;
        this.head.xRot = state.xRot * Mth.DEG_TO_RAD;
        this.head.zRot = Mth.sin(age * 0.11F) * 0.06F;
        this.skirt.xRot = 0.35F * speed + Mth.sin(age * 0.1F) * 0.06F;
        this.skirt.zRot = Mth.sin(age * 0.07F) * 0.05F;
        this.rightArm.xRot = 0.25F * speed + Mth.sin(age * 0.08F) * 0.05F;
        this.leftArm.xRot = 0.25F * speed - Mth.sin(age * 0.08F) * 0.05F;
        this.rightArm.zRot = 0.06F;
        this.leftArm.zRot = -0.06F;
    }
}

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


/** Saci: menino de uma perna só, gorro vermelho de ponta e cachimbo no canto da boca. Geometria e textura geradas por script (gen/saci.py). */
public class SaciModel extends EntityModel<FolkRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Mistech.MODID, "saci"), "main");

    private final ModelPart body;
    private final ModelPart shorts;
    private final ModelPart leg;
    private final ModelPart head;
    private final ModelPart capTip;
    private final ModelPart capTip2;
    private final ModelPart rightArm;
    private final ModelPart leftArm;

    public SaciModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.shorts = root.getChild("shorts");
        this.leg = root.getChild("leg");
        this.head = root.getChild("head");
        this.capTip = root.getChild("head").getChild("cap").getChild("cap_tip");
        this.capTip2 = root.getChild("head").getChild("cap").getChild("cap_tip").getChild("cap_tip2");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_leg = root.addOrReplaceChild("leg", CubeListBuilder.create().texOffs(16, 27).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 5.0F, 3.0F).texOffs(16, 35).addBox(-1.5F, 5.0F, -2.5F, 3.0F, 1.0F, 4.0F), PartPose.offset(0.0F, 18.0F, 0.0F));
        PartDefinition p_shorts = root.addOrReplaceChild("shorts", CubeListBuilder.create().texOffs(28, 27).addBox(-3.0F, 0.0F, -2.0F, 6.0F, 3.0F, 4.0F), PartPose.offset(0.0F, 15.0F, 0.0F));
        PartDefinition p_body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 27).addBox(-2.5F, 0.0F, -1.5F, 5.0F, 5.0F, 3.0F), PartPose.offset(0.0F, 10.0F, 0.0F));
        PartDefinition p_head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F), PartPose.offset(0.0F, 10.0F, 0.0F));
        PartDefinition p_nose = p_head.addOrReplaceChild("nose", CubeListBuilder.create().texOffs(22, 42).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 1.0F, 1.0F), PartPose.offset(0.0F, -4.0F, -4.0F));
        PartDefinition p_right_ear = p_head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(54, 35).addBox(-1.0F, -1.0F, -1.0F, 1.0F, 2.0F, 2.0F), PartPose.offset(-4.0F, -5.0F, 0.0F));
        PartDefinition p_left_ear = p_head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(0, 42).addBox(0.0F, -1.0F, -1.0F, 1.0F, 2.0F, 2.0F), PartPose.offset(4.0F, -5.0F, 0.0F));
        PartDefinition p_cap = p_head.addOrReplaceChild("cap", CubeListBuilder.create().texOffs(0, 16).addBox(-4.5F, -2.0F, -4.5F, 9.0F, 2.0F, 9.0F), PartPose.offset(0.0F, -8.0F, 0.0F));
        PartDefinition p_cap_tip = p_cap.addOrReplaceChild("cap_tip", CubeListBuilder.create().texOffs(36, 16).addBox(-3.0F, -3.0F, -3.0F, 6.0F, 3.0F, 6.0F), PartPose.offsetAndRotation(0.0F, -2.0F, 1.0F, -0.3F, 0.0F, 0.0F));
        PartDefinition p_cap_tip2 = p_cap_tip.addOrReplaceChild("cap_tip2", CubeListBuilder.create().texOffs(48, 27).addBox(-2.0F, -3.0F, -2.0F, 4.0F, 3.0F, 4.0F), PartPose.offsetAndRotation(0.0F, -3.0F, 1.0F, -0.45F, 0.0F, 0.0F));
        PartDefinition p_cap_tip3 = p_cap_tip2.addOrReplaceChild("cap_tip3", CubeListBuilder.create().texOffs(30, 35).addBox(-1.0F, -3.0F, -1.0F, 2.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(0.0F, -3.0F, 0.5F, -0.7F, 0.0F, 0.0F));
        PartDefinition p_pipe = p_head.addOrReplaceChild("pipe", CubeListBuilder.create().texOffs(46, 35).addBox(-0.5F, -0.5F, -3.0F, 1.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(2.0F, -1.5F, -4.0F, 0.35F, -0.55F, 0.0F));
        PartDefinition p_bowl = p_pipe.addOrReplaceChild("bowl", CubeListBuilder.create().texOffs(38, 35).addBox(-1.0F, -3.0F, -1.5F, 2.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.0F, -3.0F, -0.55F, 0.0F, 0.0F));
        PartDefinition p_right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(0, 35).addBox(-2.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F).texOffs(6, 42).addBox(-2.0F, 5.0F, -1.0F, 2.0F, 1.0F, 2.0F), PartPose.offset(-2.5F, 10.5F, 0.0F));
        PartDefinition p_left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(8, 35).addBox(0.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F).texOffs(14, 42).addBox(0.0F, 5.0F, -1.0F, 2.0F, 1.0F, 2.0F), PartPose.offset(2.5F, 10.5F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(FolkRenderState state) {
        super.setupAnim(state);
        float speed = Math.min(1.0F, state.walkAnimationSpeed * 1.5F);
        float pos = state.walkAnimationPos;
        this.head.yRot = state.yRot * Mth.DEG_TO_RAD;
        this.head.xRot = state.xRot * Mth.DEG_TO_RAD;
        // pula numa perna só
        float hop = Math.abs(Mth.sin(pos * 0.6662F)) * 2.5F * speed;
        float drop = state.sitting ? 4.0F : 0.0F;
        for (ModelPart p : new ModelPart[] {this.body, this.shorts, this.leg, this.head, this.rightArm, this.leftArm}) {
            p.y += drop - hop;
        }
        this.leg.xRot = state.sitting ? -1.4F : -0.35F * hop / 2.5F;
        this.rightArm.xRot = Mth.cos(pos * 0.6662F) * 1.1F * speed;
        this.leftArm.xRot = -this.rightArm.xRot;
        this.rightArm.zRot = 0.12F + Mth.sin(state.ageInTicks * 0.09F) * 0.05F;
        this.leftArm.zRot = -this.rightArm.zRot;
        // a ponta do gorro balança
        this.capTip.zRot = Mth.sin(state.ageInTicks * 0.1F) * 0.15F;
        this.capTip2.zRot = Mth.sin(state.ageInTicks * 0.1F + 0.6F) * 0.25F;
    }
}

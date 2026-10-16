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

/** Eco: anda como você; as cabeças-fantasma tremem em leque, como foto de longa exposição. Geometria e textura: {@code tools/dream/mobs3.py}. */
public class EchoModel extends EntityModel<DreamMobRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Mistech.MODID, "echo"), "main");
    private final ModelPart head, ghostR, ghostL, ghostR2, ghostL2, rightArm, leftArm, rightLeg, leftLeg;

    public EchoModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.ghostR = this.head.getChild("ghost_head_r");
        this.ghostL = this.head.getChild("ghost_head_l");
        this.ghostR2 = this.head.getChild("ghost_head_r2");
        this.ghostL2 = this.head.getChild("ghost_head_l2");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 19).addBox(-3.0F, -7.0F, -3.0F, 6.0F, 7.0F, 6.0F), PartPose.offset(0.0F, -1.0F, 0.0F));
        PartDefinition p_ghost_head_r = p_head.addOrReplaceChild("ghost_head_r", CubeListBuilder.create().texOffs(24, 19).addBox(-3.0F, -7.0F, -3.0F, 6.0F, 7.0F, 6.0F), PartPose.offsetAndRotation(-4.5F, -0.95F, 0.5F, 0.0F, 0.0F, -0.3F));
        PartDefinition p_ghost_head_l = p_head.addOrReplaceChild("ghost_head_l", CubeListBuilder.create().texOffs(0, 32).addBox(-3.0F, -7.0F, -3.0F, 6.0F, 7.0F, 6.0F), PartPose.offsetAndRotation(4.5F, -0.95F, 0.5F, 0.0F, 0.0F, 0.3F));
        PartDefinition p_ghost_head_r2 = p_head.addOrReplaceChild("ghost_head_r2", CubeListBuilder.create().texOffs(24, 32).addBox(-3.0F, -7.0F, -3.0F, 6.0F, 7.0F, 6.0F), PartPose.offsetAndRotation(-8.0F, -1.3F, 0.5F, 0.0F, 0.0F, -0.55F));
        PartDefinition p_ghost_head_l2 = p_head.addOrReplaceChild("ghost_head_l2", CubeListBuilder.create().texOffs(0, 45).addBox(-3.0F, -7.0F, -3.0F, 6.0F, 7.0F, 6.0F), PartPose.offsetAndRotation(8.0F, -1.3F, 0.5F, 0.0F, 0.0F, 0.55F));
        PartDefinition p_body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 0).addBox(-3.0F, 0.0F, -1.5F, 6.0F, 13.0F, 3.0F), PartPose.offset(0.0F, -1.0F, 0.0F));
        PartDefinition p_right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 17.0F, 2.0F), PartPose.offset(-4.0F, 0.0F, 0.0F));
        PartDefinition p_left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(8, 0).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 17.0F, 2.0F), PartPose.offset(4.0F, 0.0F, 0.0F));
        PartDefinition p_right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(34, 0).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 12.0F, 3.0F, new CubeDeformation(-0.25F, 0.0F, -0.25F)), PartPose.offset(-1.5F, 12.0F, 0.0F));
        PartDefinition p_left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(46, 0).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 12.0F, 3.0F, new CubeDeformation(-0.25F, 0.0F, -0.25F)), PartPose.offset(1.5F, 12.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(DreamMobRenderState state) {
        super.setupAnim(state);
        float age = state.ageInTicks;
        float pos = state.walkAnimationPos, speed = state.walkAnimationSpeed;
        this.head.yRot = state.yRot * Mth.DEG_TO_RAD;
        this.head.xRot = state.xRot * Mth.DEG_TO_RAD;
        float j = Mth.sin(age * 0.9F) * 0.06F + (Mth.sin(age * 0.13F) > 0.95F ? 0.2F : 0.0F);
        this.ghostR.zRot -= j;
        this.ghostL.zRot += j;
        this.ghostR2.zRot -= j * 1.6F;
        this.ghostL2.zRot += j * 1.6F;
        this.rightLeg.xRot = Mth.cos(pos * 0.6662F) * 1.2F * speed;
        this.leftLeg.xRot = Mth.cos(pos * 0.6662F + Mth.PI) * 1.2F * speed;
        // braços parados, pendurados: é só um reflexo de quem anda
        this.rightArm.zRot = 0.05F;
        this.leftArm.zRot = -0.05F;
    }
}

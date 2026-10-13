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

/** Matinta Pereira em forma de pássaro (coruja escura de olhos amarelos). Geometria e textura: {@code tools/lore/models2.py}. */
public class MatintaBirdModel extends EntityModel<LoreRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Mistech.MODID, "matinta_bird"), "main");

    private final ModelPart body, head, rightWing, leftWing, tail, rightLeg, leftLeg;

    public MatintaBirdModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = root.getChild("head");
        this.rightWing = root.getChild("right_wing");
        this.leftWing = root.getChild("left_wing");
        this.tail = root.getChild("tail");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -3.5F, -3.0F, 6.0F, 7.0F, 6.0F), PartPose.offsetAndRotation(0.0F, 19.0F, 0.0F, 0.35F, 0.0F, 0.0F));
        PartDefinition p_head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 13).addBox(-3.5F, -5.0F, -3.0F, 7.0F, 5.0F, 6.0F), PartPose.offset(0.0F, 16.0F, -1.0F));
        PartDefinition p_beak_tip = p_head.addOrReplaceChild("beak_tip", CubeListBuilder.create().texOffs(18, 24).addBox(-0.5F, 0.0F, -1.0F, 1.0F, 1.0F, 1.0F), PartPose.offset(0.0F, -1.5F, -3.0F));
        PartDefinition p_right_tuft = p_head.addOrReplaceChild("right_tuft", CubeListBuilder.create().texOffs(44, 13).addBox(-0.5F, -2.5F, -0.5F, 1.5F, 2.5F, 1.0F), PartPose.offsetAndRotation(-2.5F, -5.0F, -1.0F, -0.2F, 0.0F, -0.5F));
        PartDefinition p_left_tuft = p_head.addOrReplaceChild("left_tuft", CubeListBuilder.create().texOffs(50, 13).addBox(-1.0F, -2.5F, -0.5F, 1.5F, 2.5F, 1.0F), PartPose.offsetAndRotation(2.5F, -5.0F, -1.0F, -0.2F, 0.0F, 0.5F));
        PartDefinition p_right_wing = root.addOrReplaceChild("right_wing", CubeListBuilder.create().texOffs(24, 0).addBox(-1.0F, 0.0F, 0.0F, 1.0F, 6.0F, 7.0F), PartPose.offsetAndRotation(-3.0F, 16.5F, -2.0F, 0.35F, 0.0F, 0.0F));
        PartDefinition p_left_wing = root.addOrReplaceChild("left_wing", CubeListBuilder.create().texOffs(40, 0).addBox(0.0F, 0.0F, 0.0F, 1.0F, 6.0F, 7.0F), PartPose.offsetAndRotation(3.0F, 16.5F, -2.0F, 0.35F, 0.0F, 0.0F));
        PartDefinition p_tail = root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(26, 13).addBox(-2.0F, 0.0F, 0.0F, 4.0F, 1.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 21.5F, 2.5F, 0.6F, 0.0F, 0.0F));
        PartDefinition p_tail_ribbon = p_tail.addOrReplaceChild("tail_ribbon", CubeListBuilder.create().texOffs(0, 24).addBox(-0.5F, 0.0F, 0.0F, 1.0F, 3.0F, 0.5F), PartPose.offset(1.0F, 0.5F, 4.5F));
        PartDefinition p_right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(56, 13).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 2.0F, 1.0F).texOffs(2, 24).addBox(-1.0F, 1.5F, -1.5F, 2.0F, 0.5F, 2.5F), PartPose.offset(-1.3F, 22.0F, 0.0F));
        PartDefinition p_left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(60, 13).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 2.0F, 1.0F).texOffs(10, 24).addBox(-1.0F, 1.5F, -1.5F, 2.0F, 0.5F, 2.5F), PartPose.offset(1.3F, 22.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(LoreRenderState state) {
        super.setupAnim(state);
        float t = state.ageInTicks;
        float pos = state.walkAnimationPos;
        float speed = state.walkAnimationSpeed;
        this.head.yRot = state.yRot * Mth.DEG_TO_RAD;
        this.head.xRot = state.xRot * Mth.DEG_TO_RAD;
        if (state.flying) {
            // asas batendo, pernas recolhidas
            float flap = Mth.sin(t * 1.3F) * 1.1F;
            this.rightWing.zRot = 0.4F + flap;
            this.leftWing.zRot = -0.4F - flap;
            this.rightWing.xRot = 0.0F;
            this.leftWing.xRot = 0.0F;
            this.rightLeg.xRot = 0.8F;
            this.leftLeg.xRot = 0.8F;
            this.tail.xRot = 0.3F + Mth.sin(t * 0.4F) * 0.1F;
        } else {
            this.rightLeg.xRot = Mth.cos(pos * 0.9F) * 0.8F * speed;
            this.leftLeg.xRot = Mth.cos(pos * 0.9F + Mth.PI) * 0.8F * speed;
            this.head.zRot = Mth.sin(t * 0.05F) * 0.15F; // cabeça inclinada, de coruja
        }
    }
}

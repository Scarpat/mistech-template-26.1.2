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
import net.minecraft.client.renderer.entity.state.ParrotRenderState;


/** Corvo: pássaro preto de brilho azulado, bico grosso e cauda em leque. Geometria e textura geradas por script (gen/raven.py). */
public class RavenModel extends EntityModel<ParrotRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Mistech.MODID, "raven"), "main");

    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart leftWing;
    private final ModelPart rightWing;
    private final ModelPart tail;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;

    public RavenModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = root.getChild("head");
        this.leftWing = root.getChild("left_wing");
        this.rightWing = root.getChild("right_wing");
        this.tail = root.getChild("tail");
        this.leftLeg = root.getChild("left_leg");
        this.rightLeg = root.getChild("right_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(28, 0).addBox(-2.5F, -7.0F, -2.5F, 5.0F, 7.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 22.0F, 0.0F, 0.2F, 0.0F, 0.0F));
        PartDefinition p_head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 14).addBox(-2.5F, -5.0F, -3.0F, 5.0F, 5.0F, 5.0F), PartPose.offset(0.0F, 15.5F, -0.5F));
        PartDefinition p_beak = p_head.addOrReplaceChild("beak", CubeListBuilder.create().texOffs(52, 14).addBox(-1.0F, -1.0F, -3.0F, 2.0F, 2.0F, 3.0F).texOffs(28, 24).addBox(-0.5F, 0.0F, -4.0F, 1.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(0.0F, -2.5F, -3.0F, 0.15F, 0.0F, 0.0F));
        PartDefinition p_crest = p_head.addOrReplaceChild("crest", CubeListBuilder.create().texOffs(40, 14).addBox(-1.0F, -1.0F, -2.0F, 2.0F, 1.0F, 4.0F), PartPose.offsetAndRotation(0.0F, -5.0F, 0.0F, -0.35F, 0.0F, 0.0F));
        PartDefinition p_left_wing = root.addOrReplaceChild("left_wing", CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, 0.0F, -2.5F, 1.0F, 8.0F, 6.0F), PartPose.offset(2.5F, 15.5F, 0.0F));
        PartDefinition p_right_wing = root.addOrReplaceChild("right_wing", CubeListBuilder.create().texOffs(14, 0).mirror().addBox(-1.0F, 0.0F, -2.5F, 1.0F, 8.0F, 6.0F).mirror(false), PartPose.offset(-2.5F, 15.5F, 0.0F));
        PartDefinition p_tail = root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(20, 14).addBox(-2.0F, -0.5F, 0.0F, 4.0F, 1.0F, 6.0F), PartPose.offsetAndRotation(0.0F, 21.0F, 2.0F, -0.6F, 0.0F, 0.0F));
        PartDefinition p_left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(20, 24).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 2.0F, 1.0F).texOffs(0, 24).addBox(-1.0F, 1.5F, -2.0F, 2.0F, 0.5F, 3.0F), PartPose.offset(1.2F, 22.0F, 0.0F));
        PartDefinition p_right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(24, 24).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 2.0F, 1.0F).texOffs(10, 24).addBox(-1.0F, 1.5F, -2.0F, 2.0F, 0.5F, 3.0F), PartPose.offset(-1.2F, 22.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(ParrotRenderState state) {
        super.setupAnim(state);
        this.head.xRot = state.xRot * Mth.DEG_TO_RAD;
        this.head.yRot = state.yRot * Mth.DEG_TO_RAD;
        float bob = state.flapAngle * 0.3F;
        switch (state.pose) {
            case SITTING -> {
                for (ModelPart p : new ModelPart[] {this.body, this.head, this.leftWing, this.rightWing, this.tail}) {
                    p.y += 1.0F;
                }
                this.leftLeg.visible = false;
                this.rightLeg.visible = false;
            }
            case FLYING -> {
                this.leftLeg.visible = true;
                this.rightLeg.visible = true;
                this.body.xRot = 0.45F;
                this.leftWing.zRot = -1.2F - state.flapAngle * 1.2F;
                this.rightWing.zRot = 1.2F + state.flapAngle * 1.2F;
                this.leftLeg.xRot = 0.9F;
                this.rightLeg.xRot = 0.9F;
                for (ModelPart p : new ModelPart[] {this.body, this.head, this.leftWing, this.rightWing, this.tail}) {
                    p.y += bob;
                }
            }
            default -> {
                this.leftLeg.visible = true;
                this.rightLeg.visible = true;
                this.leftLeg.xRot = Mth.cos(state.walkAnimationPos * 0.6662F) * 1.2F * state.walkAnimationSpeed;
                this.rightLeg.xRot = Mth.cos(state.walkAnimationPos * 0.6662F + Mth.PI) * 1.2F * state.walkAnimationSpeed;
                this.leftWing.zRot = -0.05F - state.flapAngle * 0.5F;
                this.rightWing.zRot = 0.05F + state.flapAngle * 0.5F;
                // o corvo inclina a cabeça de lado de vez em quando
                this.head.zRot = Mth.sin(state.ageInTicks * 0.06F) > 0.9F ? -0.4F : 0.0F;
            }
        }
    }
}

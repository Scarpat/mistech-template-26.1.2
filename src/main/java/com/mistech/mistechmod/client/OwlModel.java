package com.mistech.mistechmod.client;

import com.mistech.mistechmod.Mistech;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.animal.parrot.ParrotModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.ParrotRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Coruja: corpo gordinho e em pé, cabeça grande com tufos de "orelha", disco facial e olhos grandes,
 * asas dobradas nos lados (abrem ao voar), cauda curta e patas com garras. Textura 64×32 ({@code textures/entity/owl.png}).
 */
public class OwlModel extends EntityModel<ParrotRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Mistech.MODID, "owl"), "main");

    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart leftWing;
    private final ModelPart rightWing;
    private final ModelPart tail;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;

    public OwlModel(ModelPart root) {
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
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -7.0F, -2.5F, 6.0F, 7.0F, 5.0F),
                PartPose.offset(0.0F, 22.0F, 0.0F));
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(24, 0).addBox(-3.5F, -6.0F, -3.0F, 7.0F, 6.0F, 6.0F),
                PartPose.offset(0.0F, 15.5F, -0.5F));
        head.addOrReplaceChild("left_tuft", CubeListBuilder.create().texOffs(52, 0).addBox(-0.5F, -2.0F, -0.5F, 1.0F, 2.0F, 1.0F),
                PartPose.offsetAndRotation(2.8F, -6.0F, -1.5F, -0.2F, 0.0F, 0.35F));
        head.addOrReplaceChild("right_tuft", CubeListBuilder.create().texOffs(52, 0).mirror().addBox(-0.5F, -2.0F, -0.5F, 1.0F, 2.0F, 1.0F),
                PartPose.offsetAndRotation(-2.8F, -6.0F, -1.5F, -0.2F, 0.0F, -0.35F));
        head.addOrReplaceChild("beak", CubeListBuilder.create().texOffs(56, 0).addBox(-0.5F, 0.0F, -1.0F, 1.0F, 2.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, -3.0F, -3.0F, 0.15F, 0.0F, 0.0F));
        root.addOrReplaceChild("left_wing", CubeListBuilder.create().texOffs(0, 14).addBox(0.0F, 0.0F, -2.0F, 1.0F, 6.0F, 4.0F),
                PartPose.offset(3.0F, 15.5F, 0.0F));
        root.addOrReplaceChild("right_wing", CubeListBuilder.create().texOffs(0, 14).mirror().addBox(-1.0F, 0.0F, -2.0F, 1.0F, 6.0F, 4.0F),
                PartPose.offset(-3.0F, 15.5F, 0.0F));
        root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(12, 14).addBox(-2.0F, -0.5F, 0.0F, 4.0F, 1.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, 21.0F, 2.0F, -0.7F, 0.0F, 0.0F));
        CubeListBuilder leg = CubeListBuilder.create().texOffs(52, 4).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 2.0F, 1.0F)
                .texOffs(52, 8).addBox(-1.0F, 1.0F, -1.5F, 2.0F, 1.0F, 2.0F);
        root.addOrReplaceChild("left_leg", leg, PartPose.offset(1.5F, 22.0F, 0.0F));
        root.addOrReplaceChild("right_leg", leg, PartPose.offset(-1.5F, 22.0F, 0.0F));
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
                // agachadinha: tudo desce 1 px, asas fechadas, patas escondidas
                this.body.y += 1.0F;
                this.head.y += 1.0F;
                this.leftWing.y += 1.0F;
                this.rightWing.y += 1.0F;
                this.tail.y += 1.0F;
                this.tail.xRot += 0.3F;
                this.leftLeg.visible = false;
                this.rightLeg.visible = false;
            }
            case FLYING -> {
                // asas abertas batendo, patas para trás, corpo inclinado
                this.leftLeg.visible = true;
                this.rightLeg.visible = true;
                this.body.xRot = 0.35F;
                this.leftWing.zRot = -1.2F - state.flapAngle * 1.2F;
                this.rightWing.zRot = 1.2F + state.flapAngle * 1.2F;
                this.leftLeg.xRot = 0.9F;
                this.rightLeg.xRot = 0.9F;
                this.head.y += bob;
                this.body.y += bob;
                this.leftWing.y += bob;
                this.rightWing.y += bob;
                this.tail.y += bob;
            }
            default -> {
                this.leftLeg.visible = true;
                this.rightLeg.visible = true;
                this.leftLeg.xRot = Mth.cos(state.walkAnimationPos * 0.6662F) * 1.2F * state.walkAnimationSpeed;
                this.rightLeg.xRot = Mth.cos(state.walkAnimationPos * 0.6662F + Mth.PI) * 1.2F * state.walkAnimationSpeed;
                this.leftWing.zRot = -0.05F - state.flapAngle * 0.5F;
                this.rightWing.zRot = 0.05F + state.flapAngle * 0.5F;
                this.tail.xRot += Mth.cos(state.walkAnimationPos * 0.6662F) * 0.2F * state.walkAnimationSpeed;
            }
        }
        // de vez em quando a coruja inclina a cabeça de lado
        if (state.pose != ParrotModel.Pose.FLYING) {
            this.head.zRot = Mth.sin(state.ageInTicks * 0.05F) > 0.92F ? 0.35F : 0.0F;
        }
    }
}

package com.mistech.mistechmod.client;

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

/** Troll: corcunda, braços compridos, presas e clava (gerado por script: modelo e textura saem da mesma especificação) */
public class TrollModel extends EntityModel<GoblinRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Mistech.MODID, "troll"), "main");

    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart armR;
    private final ModelPart armL;
    private final ModelPart legR;
    private final ModelPart legL;

    public TrollModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = root.getChild("body").getChild("head");
        this.armR = root.getChild("body").getChild("arm_r");
        this.armL = root.getChild("body").getChild("arm_l");
        this.legR = root.getChild("leg_r");
        this.legL = root.getChild("leg_l");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition leg_r = root.addOrReplaceChild("leg_r", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0000F, 0.0000F, -3.0000F, 6.0000F, 12.0000F, 6.0000F), PartPose.offset(-4.5000F, 12.0000F, 2.0000F));
        PartDefinition leg_l = root.addOrReplaceChild("leg_l", CubeListBuilder.create().texOffs(24, 0).addBox(-3.0000F, 0.0000F, -3.0000F, 6.0000F, 12.0000F, 6.0000F), PartPose.offset(4.5000F, 12.0000F, 2.0000F));
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(48, 0).addBox(-8.0000F, -16.0000F, -5.0000F, 16.0000F, 16.0000F, 10.0000F).texOffs(0, 26).addBox(-7.0000F, -5.0000F, -6.5000F, 14.0000F, 6.0000F, 2.0000F).texOffs(32, 26).addBox(-6.0000F, -18.0000F, -4.0000F, 12.0000F, 3.0000F, 8.0000F), PartPose.offsetAndRotation(0.0000F, 12.0000F, 2.0000F, 0.4500F, 0.0000F, 0.0000F));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(72, 26).addBox(-5.0000F, -8.0000F, -7.0000F, 10.0000F, 8.0000F, 8.0000F).texOffs(0, 42).addBox(-5.5000F, 0.0000F, -8.0000F, 11.0000F, 3.0000F, 7.0000F).texOffs(36, 42).addBox(-1.5000F, -5.0000F, -10.0000F, 3.0000F, 4.0000F, 3.0000F), PartPose.offsetAndRotation(0.0000F, -15.0000F, -4.0000F, -0.4500F, 0.0000F, 0.0000F));
        PartDefinition tusk_r = head.addOrReplaceChild("tusk_r", CubeListBuilder.create().texOffs(48, 42).addBox(-0.5000F, -3.0000F, -0.5000F, 1.0000F, 3.0000F, 1.0000F), PartPose.offsetAndRotation(-3.5000F, 0.0000F, -7.5000F, -0.3000F, 0.0000F, 0.0000F));
        PartDefinition tusk_l = head.addOrReplaceChild("tusk_l", CubeListBuilder.create().texOffs(52, 42).addBox(-0.5000F, -3.0000F, -0.5000F, 1.0000F, 3.0000F, 1.0000F), PartPose.offsetAndRotation(3.5000F, 0.0000F, -7.5000F, -0.3000F, 0.0000F, 0.0000F));
        PartDefinition arm_r = body.addOrReplaceChild("arm_r", CubeListBuilder.create().texOffs(56, 42).addBox(-3.5000F, -2.0000F, -3.0000F, 6.0000F, 22.0000F, 6.0000F), PartPose.offsetAndRotation(-9.5000F, -13.0000F, 0.0000F, -0.4500F, 0.0000F, 0.0000F));
        PartDefinition club = arm_r.addOrReplaceChild("club", CubeListBuilder.create().texOffs(80, 42).addBox(-1.0000F, -1.0000F, -3.0000F, 2.0000F, 2.0000F, 4.0000F).texOffs(92, 42).addBox(-2.0000F, -1.5000F, -14.0000F, 4.0000F, 4.0000F, 11.0000F).texOffs(0, 70).addBox(-3.0000F, -2.5000F, -19.0000F, 6.0000F, 6.0000F, 6.0000F), PartPose.offset(-0.5000F, 19.0000F, 0.0000F));
        PartDefinition arm_l = body.addOrReplaceChild("arm_l", CubeListBuilder.create().texOffs(24, 70).addBox(-2.5000F, -2.0000F, -3.0000F, 6.0000F, 22.0000F, 6.0000F), PartPose.offsetAndRotation(9.5000F, -13.0000F, 0.0000F, -0.4500F, 0.0000F, 0.0000F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(GoblinRenderState state) {
        super.setupAnim(state);
        float walk = state.walkAnimationPos * 0.5F;
        float speed = Math.min(1.0F, state.walkAnimationSpeed);
        float t = state.ageInTicks;
        this.head.yRot = state.yRot * Mth.DEG_TO_RAD * 0.7F;
        this.head.xRot = -0.45F + state.xRot * Mth.DEG_TO_RAD * 0.5F;
        this.legR.xRot = Mth.cos(walk) * 0.8F * speed;
        this.legL.xRot = Mth.cos(walk + Mth.PI) * 0.8F * speed;
        // corpo pesado: balança de um lado para o outro e respira
        this.body.zRot = Mth.sin(walk) * 0.06F * speed;
        this.body.xRot = 0.45F + Mth.sin(t * 0.06F) * 0.03F;
        this.armL.xRot = -0.45F + Mth.cos(walk) * 0.5F * speed;
        this.armL.zRot = -0.1F;
        // clava: levanta acima da cabeça e desce com tudo
        float a = state.attackAnim;
        float swing = a < 0.4F ? -a / 0.4F * 2.6F : -2.6F + (a - 0.4F) / 0.6F * 3.0F;
        this.armR.xRot = -0.45F + Mth.cos(walk + Mth.PI) * 0.5F * speed + (a > 0 ? swing : 0.0F);
        this.armR.zRot = 0.1F;
    }
}

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

/** Duende Corrompido: cabeçudo, orelhas pontudas, olhos roxos, adaga enferrujada (gerado por script: modelo e textura saem da mesma especificação) */
public class CorruptedGoblinModel extends EntityModel<GoblinRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Mistech.MODID, "corrupted_goblin"), "main");

    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart earR;
    private final ModelPart earL;
    private final ModelPart armR;
    private final ModelPart armL;
    private final ModelPart legR;
    private final ModelPart legL;

    public CorruptedGoblinModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = root.getChild("head");
        this.earR = root.getChild("head").getChild("ear_r");
        this.earL = root.getChild("head").getChild("ear_l");
        this.armR = root.getChild("arm_r");
        this.armL = root.getChild("arm_l");
        this.legR = root.getChild("leg_r");
        this.legL = root.getChild("leg_l");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-3.5000F, 0.0000F, -2.0000F, 7.0000F, 7.0000F, 4.0000F), PartPose.offset(0.0000F, 13.0000F, 0.0000F));
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(22, 0).addBox(-4.0000F, -8.0000F, -4.0000F, 8.0000F, 8.0000F, 7.0000F).texOffs(52, 0).addBox(-1.0000F, -4.0000F, -6.0000F, 2.0000F, 3.0000F, 2.0000F).texOffs(0, 15).addBox(-2.5000F, -9.5000F, -2.0000F, 5.0000F, 2.0000F, 5.0000F), PartPose.offset(0.0000F, 13.0000F, 0.0000F));
        PartDefinition ear_r = head.addOrReplaceChild("ear_r", CubeListBuilder.create().texOffs(20, 15).addBox(-5.0000F, -1.0000F, -0.5000F, 5.0000F, 3.0000F, 1.0000F), PartPose.offsetAndRotation(-4.0000F, -5.0000F, 0.0000F, 0.0000F, 0.2500F, 0.3500F));
        PartDefinition ear_l = head.addOrReplaceChild("ear_l", CubeListBuilder.create().texOffs(32, 15).addBox(0.0000F, -1.0000F, -0.5000F, 5.0000F, 3.0000F, 1.0000F), PartPose.offsetAndRotation(4.0000F, -5.0000F, 0.0000F, 0.0000F, -0.2500F, -0.3500F));
        PartDefinition arm_r = root.addOrReplaceChild("arm_r", CubeListBuilder.create().texOffs(44, 15).addBox(-1.5000F, -1.0000F, -1.5000F, 3.0000F, 8.0000F, 3.0000F), PartPose.offset(-4.5000F, 14.0000F, 0.0000F));
        PartDefinition dagger = arm_r.addOrReplaceChild("dagger", CubeListBuilder.create().texOffs(56, 15).addBox(-0.5000F, -0.5000F, -1.5000F, 1.0000F, 1.0000F, 2.0000F).texOffs(0, 26).addBox(-0.5000F, -0.5000F, -7.5000F, 1.0000F, 1.5000F, 6.0000F), PartPose.offset(0.0000F, 6.5000F, 0.0000F));
        PartDefinition arm_l = root.addOrReplaceChild("arm_l", CubeListBuilder.create().texOffs(14, 26).addBox(-1.5000F, -1.0000F, -1.5000F, 3.0000F, 8.0000F, 3.0000F), PartPose.offset(4.5000F, 14.0000F, 0.0000F));
        PartDefinition leg_r = root.addOrReplaceChild("leg_r", CubeListBuilder.create().texOffs(26, 26).addBox(-1.5000F, 0.0000F, -1.5000F, 3.0000F, 4.0000F, 3.0000F), PartPose.offset(-2.0000F, 20.0000F, 0.0000F));
        PartDefinition leg_l = root.addOrReplaceChild("leg_l", CubeListBuilder.create().texOffs(38, 26).addBox(-1.5000F, 0.0000F, -1.5000F, 3.0000F, 4.0000F, 3.0000F), PartPose.offset(2.0000F, 20.0000F, 0.0000F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(GoblinRenderState state) {
        super.setupAnim(state);
        float walk = state.walkAnimationPos * 0.9F;
        float speed = Math.min(1.0F, state.walkAnimationSpeed);
        this.head.yRot = state.yRot * Mth.DEG_TO_RAD;
        this.head.xRot = state.xRot * Mth.DEG_TO_RAD;
        this.legR.xRot = Mth.cos(walk) * 1.3F * speed;
        this.legL.xRot = Mth.cos(walk + Mth.PI) * 1.3F * speed;
        this.armL.xRot = Mth.cos(walk) * 0.9F * speed;
        // braço da adaga: em guarda e golpeia ao atacar
        this.armR.xRot = -0.35F + Mth.cos(walk + Mth.PI) * 0.6F * speed - Mth.sin(state.attackAnim * Mth.PI) * 1.8F;
        this.armR.zRot = 0.1F;
        this.armL.zRot = -0.1F - Mth.sin(state.ageInTicks * 0.09F) * 0.05F;
        // corcunda e tremedeira da corrupção
        this.body.xRot = 0.15F;
        this.head.zRot = Mth.sin(state.ageInTicks * 0.6F) * 0.03F;
        this.earR.zRot = 0.35F + Mth.sin(state.ageInTicks * 0.2F) * 0.08F;
        this.earL.zRot = -0.35F - Mth.sin(state.ageInTicks * 0.2F) * 0.08F;
    }
}

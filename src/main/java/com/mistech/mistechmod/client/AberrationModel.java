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

/** Aberração: bolo de carne com seis pernas, um olho só e boca de dentes (gerado por script: modelo e textura saem da mesma especificação) */
public class AberrationModel extends EntityModel<GoblinRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Mistech.MODID, "aberration"), "main");

    private final ModelPart body;
    private final ModelPart eye;
    private final ModelPart tentacle;
    private final ModelPart[] legs;
    private final ModelPart leg0;
    private final ModelPart leg1;
    private final ModelPart leg2;
    private final ModelPart leg3;
    private final ModelPart leg4;
    private final ModelPart leg5;

    public AberrationModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.eye = root.getChild("body").getChild("eye");
        this.tentacle = root.getChild("body").getChild("tentacle");
        this.legs = new ModelPart[6];
        this.leg0 = root.getChild("leg_r0");
        this.leg1 = root.getChild("leg_l0");
        this.leg2 = root.getChild("leg_r1");
        this.leg3 = root.getChild("leg_l1");
        this.leg4 = root.getChild("leg_r2");
        this.leg5 = root.getChild("leg_l2");
        this.legs[0] = this.leg0; this.legs[1] = this.leg1; this.legs[2] = this.leg2;
        this.legs[3] = this.leg3; this.legs[4] = this.leg4; this.legs[5] = this.leg5;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-6.0000F, -5.0000F, -6.0000F, 12.0000F, 9.0000F, 12.0000F).texOffs(0, 21).addBox(1.5000F, -8.0000F, -3.0000F, 5.0000F, 4.0000F, 5.0000F).texOffs(20, 21).addBox(-7.0000F, -2.0000F, 0.0000F, 3.0000F, 4.0000F, 4.0000F).texOffs(34, 21).addBox(-4.0000F, 2.0000F, -6.5000F, 8.0000F, 2.0000F, 1.0000F), PartPose.offset(0.0000F, 17.0000F, 0.0000F));
        PartDefinition eye = body.addOrReplaceChild("eye", CubeListBuilder.create().texOffs(0, 30).addBox(-3.0000F, -3.0000F, -1.0000F, 6.0000F, 5.0000F, 1.0000F), PartPose.offset(0.0000F, -1.0000F, -6.0000F));
        PartDefinition tentacle = body.addOrReplaceChild("tentacle", CubeListBuilder.create().texOffs(14, 30).addBox(-1.0000F, -7.0000F, -1.0000F, 2.0000F, 7.0000F, 2.0000F).texOffs(22, 30).addBox(-1.5000F, -9.0000F, -1.5000F, 3.0000F, 2.0000F, 3.0000F), PartPose.offsetAndRotation(-3.0000F, -5.0000F, 2.0000F, 0.2000F, 0.0000F, 0.5000F));
        PartDefinition leg_r0 = root.addOrReplaceChild("leg_r0", CubeListBuilder.create().texOffs(34, 30).addBox(-10.0000F, -1.0000F, -1.0000F, 10.0000F, 2.0000F, 2.0000F), PartPose.offset(-5.0000F, 19.0000F, -3.5000F));
        PartDefinition leg_l0 = root.addOrReplaceChild("leg_l0", CubeListBuilder.create().texOffs(0, 39).addBox(0.0000F, -1.0000F, -1.0000F, 10.0000F, 2.0000F, 2.0000F), PartPose.offset(5.0000F, 19.0000F, -3.5000F));
        PartDefinition leg_r1 = root.addOrReplaceChild("leg_r1", CubeListBuilder.create().texOffs(24, 39).addBox(-10.0000F, -1.0000F, -1.0000F, 10.0000F, 2.0000F, 2.0000F), PartPose.offset(-5.0000F, 19.0000F, 0.0000F));
        PartDefinition leg_l1 = root.addOrReplaceChild("leg_l1", CubeListBuilder.create().texOffs(0, 43).addBox(0.0000F, -1.0000F, -1.0000F, 10.0000F, 2.0000F, 2.0000F), PartPose.offset(5.0000F, 19.0000F, 0.0000F));
        PartDefinition leg_r2 = root.addOrReplaceChild("leg_r2", CubeListBuilder.create().texOffs(24, 43).addBox(-10.0000F, -1.0000F, -1.0000F, 10.0000F, 2.0000F, 2.0000F), PartPose.offset(-5.0000F, 19.0000F, 3.5000F));
        PartDefinition leg_l2 = root.addOrReplaceChild("leg_l2", CubeListBuilder.create().texOffs(0, 47).addBox(0.0000F, -1.0000F, -1.0000F, 10.0000F, 2.0000F, 2.0000F), PartPose.offset(5.0000F, 19.0000F, 3.5000F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(GoblinRenderState state) {
        super.setupAnim(state);
        float walk = state.walkAnimationPos * 1.4F;
        float speed = Math.min(1.0F, state.walkAnimationSpeed);
        float t = state.ageInTicks;
        // pernas de aranha, cada uma num ritmo
        for (int i = 0; i < 6; i++) {
            ModelPart leg = this.legs[i];
            float side = i % 2 == 0 ? -1.0F : 1.0F;
            int row = i / 2;
            float phase = row * 2.1F + (side > 0 ? Mth.PI : 0.0F);
            leg.yRot = side * ((row - 1) * 0.55F) + Mth.cos(walk + phase) * 0.45F * speed;
            leg.zRot = side * (0.55F + Math.abs(Mth.sin(walk + phase)) * 0.35F * speed + Mth.sin(t * 0.2F + i) * 0.04F);
        }
        // corpo pulsa, olho procura
        float pulse = 1.0F + Mth.sin(t * 0.25F) * 0.04F;
        this.body.xScale = pulse;
        this.body.zScale = pulse;
        this.body.yScale = 2.0F - pulse;
        this.eye.yRot = state.yRot * Mth.DEG_TO_RAD * 0.6F;
        this.eye.xRot = state.xRot * Mth.DEG_TO_RAD * 0.6F;
        this.tentacle.zRot = 0.5F + Mth.sin(t * 0.15F) * 0.35F;
        this.tentacle.xRot = 0.2F + Mth.cos(t * 0.11F) * 0.3F - Mth.sin(state.attackAnim * Mth.PI) * 1.4F;
    }
}

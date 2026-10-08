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

/** Rainha Duende: gorda, coroa de ouro, vestido roxo e cajado com orbe (gerado por script: modelo e textura saem da mesma especificação) */
public class GoblinQueenModel extends EntityModel<GoblinRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Mistech.MODID, "goblin_queen"), "main");

    private final ModelPart head;
    private final ModelPart belly;
    private final ModelPart chest;
    private final ModelPart armR;
    private final ModelPart staff;
    private final ModelPart armL;
    private final ModelPart legR;
    private final ModelPart legL;

    public GoblinQueenModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.belly = root.getChild("belly");
        this.chest = root.getChild("chest");
        this.armR = root.getChild("arm_r");
        this.staff = root.getChild("arm_r").getChild("staff");
        this.armL = root.getChild("arm_l");
        this.legR = root.getChild("leg_r");
        this.legL = root.getChild("leg_l");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition belly = root.addOrReplaceChild("belly", CubeListBuilder.create().texOffs(0, 0).addBox(-8.0000F, 0.0000F, -7.0000F, 16.0000F, 11.0000F, 13.0000F).texOffs(58, 0).addBox(-8.5000F, 8.0000F, -7.5000F, 17.0000F, 3.0000F, 14.0000F), PartPose.offset(0.0000F, 10.0000F, 0.0000F));
        PartDefinition chest = root.addOrReplaceChild("chest", CubeListBuilder.create().texOffs(0, 24).addBox(-6.0000F, -5.0000F, -5.0000F, 12.0000F, 5.0000F, 10.0000F), PartPose.offset(0.0000F, 10.0000F, 0.0000F));
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(44, 24).addBox(-6.0000F, -11.0000F, -5.0000F, 12.0000F, 11.0000F, 10.0000F).texOffs(88, 24).addBox(-5.0000F, 0.0000F, -6.0000F, 10.0000F, 2.0000F, 8.0000F).texOffs(0, 45).addBox(-1.5000F, -6.0000F, -8.0000F, 3.0000F, 4.0000F, 3.0000F).texOffs(12, 45).addBox(-5.0000F, -14.0000F, -4.0000F, 10.0000F, 3.0000F, 8.0000F), PartPose.offset(0.0000F, 5.0000F, -1.0000F));
        PartDefinition spike0 = head.addOrReplaceChild("spike0", CubeListBuilder.create().texOffs(48, 45).addBox(-4.5000F, -16.0000F, -3.5000F, 1.0000F, 2.0000F, 1.0000F), PartPose.offset(0.0000F, 0.0000F, 0.0000F));
        PartDefinition spike1 = head.addOrReplaceChild("spike1", CubeListBuilder.create().texOffs(52, 45).addBox(3.5000F, -16.0000F, -3.5000F, 1.0000F, 2.0000F, 1.0000F), PartPose.offset(0.0000F, 0.0000F, 0.0000F));
        PartDefinition spike2 = head.addOrReplaceChild("spike2", CubeListBuilder.create().texOffs(56, 45).addBox(-4.5000F, -16.0000F, 2.5000F, 1.0000F, 2.0000F, 1.0000F), PartPose.offset(0.0000F, 0.0000F, 0.0000F));
        PartDefinition spike3 = head.addOrReplaceChild("spike3", CubeListBuilder.create().texOffs(60, 45).addBox(3.5000F, -16.0000F, 2.5000F, 1.0000F, 2.0000F, 1.0000F), PartPose.offset(0.0000F, 0.0000F, 0.0000F));
        PartDefinition spike4 = head.addOrReplaceChild("spike4", CubeListBuilder.create().texOffs(64, 45).addBox(-0.5000F, -16.0000F, -3.5000F, 1.0000F, 2.0000F, 1.0000F), PartPose.offset(0.0000F, 0.0000F, 0.0000F));
        PartDefinition ear_r = head.addOrReplaceChild("ear_r", CubeListBuilder.create().texOffs(68, 45).addBox(-7.0000F, -1.5000F, -0.5000F, 7.0000F, 4.0000F, 1.0000F), PartPose.offsetAndRotation(-6.0000F, -7.0000F, 0.0000F, 0.0000F, 0.2000F, 0.3000F));
        PartDefinition ear_l = head.addOrReplaceChild("ear_l", CubeListBuilder.create().texOffs(84, 45).addBox(0.0000F, -1.5000F, -0.5000F, 7.0000F, 4.0000F, 1.0000F), PartPose.offsetAndRotation(6.0000F, -7.0000F, 0.0000F, 0.0000F, -0.2000F, -0.3000F));
        PartDefinition arm_r = root.addOrReplaceChild("arm_r", CubeListBuilder.create().texOffs(100, 45).addBox(-4.0000F, -1.0000F, -2.5000F, 4.0000F, 12.0000F, 5.0000F), PartPose.offsetAndRotation(-8.0000F, 8.0000F, 0.0000F, -0.8000F, 0.0000F, 0.1500F));
        PartDefinition staff = arm_r.addOrReplaceChild("staff", CubeListBuilder.create().texOffs(118, 45).addBox(-0.5000F, -18.0000F, -0.5000F, 1.0000F, 26.0000F, 1.0000F).texOffs(0, 72).addBox(-2.0000F, -22.0000F, -2.0000F, 4.0000F, 4.0000F, 4.0000F).texOffs(16, 72).addBox(-1.5000F, -19.0000F, -1.5000F, 3.0000F, 1.0000F, 3.0000F), PartPose.offsetAndRotation(-2.0000F, 10.0000F, 0.0000F, 0.8000F, 0.0000F, -0.1500F));
        PartDefinition arm_l = root.addOrReplaceChild("arm_l", CubeListBuilder.create().texOffs(28, 72).addBox(0.0000F, -1.0000F, -2.5000F, 4.0000F, 12.0000F, 5.0000F), PartPose.offset(8.0000F, 8.0000F, 0.0000F));
        PartDefinition leg_r = root.addOrReplaceChild("leg_r", CubeListBuilder.create().texOffs(46, 72).addBox(-2.5000F, 0.0000F, -2.5000F, 5.0000F, 3.0000F, 5.0000F), PartPose.offset(-4.0000F, 21.0000F, 0.0000F));
        PartDefinition leg_l = root.addOrReplaceChild("leg_l", CubeListBuilder.create().texOffs(66, 72).addBox(-2.5000F, 0.0000F, -2.5000F, 5.0000F, 3.0000F, 5.0000F), PartPose.offset(4.0000F, 21.0000F, 0.0000F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(GoblinRenderState state) {
        super.setupAnim(state);
        float walk = state.walkAnimationPos * 0.6F;
        float speed = Math.min(1.0F, state.walkAnimationSpeed);
        float t = state.ageInTicks;
        this.head.yRot = state.yRot * Mth.DEG_TO_RAD;
        this.head.xRot = state.xRot * Mth.DEG_TO_RAD * 0.5F;
        // anda gingando (gorda) e a barriga balança
        this.legR.xRot = Mth.cos(walk) * 0.9F * speed;
        this.legL.xRot = Mth.cos(walk + Mth.PI) * 0.9F * speed;
        float wobble = Mth.sin(walk) * 0.08F * speed + Mth.sin(t * 0.05F) * 0.02F;
        this.belly.zRot = wobble;
        this.chest.zRot = wobble * 0.5F;
        this.belly.y += Mth.sin(t * 0.1F) * 0.3F;
        this.armL.zRot = -0.15F - Mth.sin(t * 0.07F) * 0.05F;
        this.armL.xRot = Mth.cos(walk) * 0.5F * speed;
        // cajado: ergue ao conjurar
        float cast = state.castAnim;
        // braço dobrado para a frente segurando o cajado; conjurando, ergue o cajado acima da cabeça
        float swing = Mth.sin(state.attackAnim * Mth.PI);
        this.armR.xRot = -0.8F + Mth.cos(walk + Mth.PI) * 0.15F * speed - cast * 1.9F + Mth.sin(t * 0.3F) * 0.1F * cast - swing * 1.0F;
        this.armR.zRot = 0.15F + cast * 0.2F;
        // o cajado fica em pé (desfaz a rotação do braço); ao conjurar/golpear ele acompanha o braço
        float follow = Math.max(cast, swing);
        this.staff.xRot = -this.armR.xRot * (1.0F - follow) + 0.35F * follow;
        this.staff.zRot = -this.armR.zRot * (1.0F - follow);
        this.armL.xRot -= cast * 1.6F;
        this.armL.zRot -= cast * 0.4F;
    }
}

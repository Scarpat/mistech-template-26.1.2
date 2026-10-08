package com.mistech.mistechmod.client;

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

/** Modelo do Gnomo (feito no Blockbench, adaptado para o 26.1): rosto com barba e nariz, pés, chapéu e mãos (a direita segura o cajado). */
public class GnomeModel extends EntityModel<GnomeRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Mistech.MODID, "gnome"), "main");

    private final ModelPart face;
    private final ModelPart leftFoot;
    private final ModelPart rightFoot;
    private final ModelPart hat;
    private final ModelPart staffHand;
    private final ModelPart otherHand;

    public GnomeModel(ModelPart root) {
        super(root);
        this.face = root.getChild("Face");
        this.leftFoot = root.getChild("bone");
        this.rightFoot = root.getChild("bone2");
        this.hat = root.getChild("Hat");
        this.staffHand = root.getChild("Hand");
        this.otherHand = root.getChild("Hand2");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        partdefinition.addOrReplaceChild("Face", CubeListBuilder.create().texOffs(0, 7).addBox(-2.0F, -4.0F, 0.0F, 4.0F, 5.0F, 4.0F, new CubeDeformation(0.0F))
                .texOffs(10, 25).addBox(-2.0F, -1.0F, -1.0F, 4.0F, 3.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(26, 25).addBox(-1.0F, -2.0F, -2.0F, 2.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 22.0F, -3.0F));

        PartDefinition bone = partdefinition.addOrReplaceChild("bone", CubeListBuilder.create(), PartPose.offset(-2.0F, 24.0F, -3.0F));
        bone.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(16, 7).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.3054F, 0.0F));

        PartDefinition bone2 = partdefinition.addOrReplaceChild("bone2", CubeListBuilder.create(), PartPose.offset(2.0F, 24.0F, -3.0F));
        bone2.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(16, 13).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.3054F, 0.0F));

        PartDefinition hat = partdefinition.addOrReplaceChild("Hat", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, 4.0F, -5.0F, 6.0F, 1.0F, 6.0F, new CubeDeformation(0.0F)),
                PartPose.offset(1.0F, 14.0F, 1.0F));
        hat.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(28, 18).addBox(-2.0F, -2.0F, -2.0F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -2.618F, 0.0F, 0.0F));
        hat.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(28, 5).addBox(-2.0F, -3.0F, -3.0F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 1.0F, -1.0F, -1.5708F, 0.0F, 0.0F));
        hat.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(0, 23).addBox(-2.0F, -4.0F, -3.0F, 2.0F, 4.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 3.0F, -1.0F, -0.48F, 0.0F, 0.0F));
        hat.addOrReplaceChild("cube_r6", CubeListBuilder.create().texOffs(24, 0).addBox(-2.0F, -3.0F, -2.0F, 4.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-1.0F, 3.0F, -1.0F, -0.0873F, 0.0F, 0.0F));
        hat.addOrReplaceChild("cube_r7", CubeListBuilder.create().texOffs(0, 16).addBox(-2.0F, -3.0F, -3.0F, 4.0F, 3.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-1.0F, 5.0F, -1.0F, -0.0873F, 0.0F, 0.0F));

        PartDefinition hand = partdefinition.addOrReplaceChild("Hand", CubeListBuilder.create().texOffs(28, 10).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-3.0F, 22.0F, -1.0F));
        hand.addOrReplaceChild("cube_r8", CubeListBuilder.create().texOffs(22, 25).addBox(0.0F, -11.0F, 0.0F, 1.0F, 11.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-1.0F, 2.0F, 0.0F, 0.3054F, 0.0F, 0.0F));
        hand.addOrReplaceChild("cube_r9", CubeListBuilder.create().texOffs(16, 19).addBox(-2.0F, -2.0F, -2.0F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -8.0F, -2.0F, 0.3927F, 0.0F, 0.0F));

        partdefinition.addOrReplaceChild("Hand2", CubeListBuilder.create().texOffs(28, 14).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(3.0F, 22.0F, -1.0F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public void setupAnim(GnomeRenderState state) {
        super.setupAnim(state);
        float walk = state.walkAnimationPos * 0.9F;
        float speed = Math.min(1.0F, state.walkAnimationSpeed);

        // passinhos
        this.leftFoot.xRot = Mth.cos(walk) * 1.2F * speed;
        this.rightFoot.xRot = Mth.cos(walk + Mth.PI) * 1.2F * speed;

        // pulinho ao andar + respiração parado
        float bob = Math.abs(Mth.sin(walk)) * 1.2F * speed + Mth.sin(state.ageInTicks * 0.08F) * 0.2F;
        this.face.y -= bob;
        this.hat.y -= bob;
        this.staffHand.y -= bob;
        this.otherHand.y -= bob;

        // mãos balançando; a do cajado golpeia ao atacar
        this.staffHand.xRot = Mth.cos(walk + Mth.PI) * 0.35F * speed - Mth.sin(state.attackAnim * Mth.PI) * 1.4F;
        this.otherHand.zRot = -Mth.cos(walk) * 0.25F * speed;

        // ponta do chapéu balança
        this.hat.zRot = Mth.sin(state.ageInTicks * 0.1F) * 0.04F + Mth.sin(walk) * 0.05F * speed;
    }
}

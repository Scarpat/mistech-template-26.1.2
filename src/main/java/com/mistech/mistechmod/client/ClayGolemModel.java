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

/**
 * Golem de Barro: bonequinho atarracado com cabeça grande, olhos de ametista, braços compridos e pernas curtas.
 * Quando carrega itens, levanta os braços e segura o item de verdade na frente do peito (ClayGolemCarriedItemLayer).
 */
public class ClayGolemModel extends EntityModel<ClayGolemRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Mistech.MODID, "clay_golem"), "main");

    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;
    private final ModelPart load;

    public ClayGolemModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.leftArm = root.getChild("left_arm");
        this.rightArm = root.getChild("right_arm");
        this.leftLeg = root.getChild("left_leg");
        this.rightLeg = root.getChild("right_leg");
        this.load = root.getChild("load");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // cabeça 6x5x6 (olhos pintados na frente), com um "topete" de barro
        root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3.0F, -5.0F, -3.0F, 6.0F, 5.0F, 6.0F)
                .texOffs(24, 0).addBox(-1.0F, -6.0F, -1.0F, 2.0F, 1.0F, 2.0F),
                PartPose.offset(0.0F, 14.0F, 0.0F));
        // corpo 7x6x4
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 11).addBox(-3.5F, 0.0F, -2.0F, 7.0F, 6.0F, 4.0F),
                PartPose.offset(0.0F, 14.0F, 0.0F));
        // braços compridos 2x7x2
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(22, 11).addBox(-2.0F, -1.0F, -1.0F, 2.0F, 7.0F, 2.0F),
                PartPose.offset(-3.5F, 15.0F, 0.0F));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(30, 11).mirror().addBox(0.0F, -1.0F, -1.0F, 2.0F, 7.0F, 2.0F),
                PartPose.offset(3.5F, 15.0F, 0.0F));
        // pernas curtas 3x4x3
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 21).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 4.0F, 3.0F),
                PartPose.offset(-1.8F, 20.0F, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(12, 21).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 4.0F, 3.0F),
                PartPose.offset(1.8F, 20.0F, 0.0F));
        // carga (bloquinho) na frente do peito
        root.addOrReplaceChild("load", CubeListBuilder.create().texOffs(38, 0).addBox(-2.5F, -2.5F, -2.5F, 5.0F, 5.0F, 5.0F),
                PartPose.offset(0.0F, 16.0F, -4.5F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(ClayGolemRenderState state) {
        super.setupAnim(state);
        float walk = state.walkAnimationPos * 0.8F;
        float speed = Math.min(1.0F, state.walkAnimationSpeed);

        this.head.yRot = state.yRot * Mth.DEG_TO_RAD;
        this.head.xRot = state.xRot * Mth.DEG_TO_RAD;

        this.rightLeg.xRot = Mth.cos(walk) * 1.1F * speed;
        this.leftLeg.xRot = Mth.cos(walk + Mth.PI) * 1.1F * speed;

        // gingado de bonequinho pesado
        float sway = Mth.sin(walk) * 0.12F * speed;
        this.body.zRot = sway;
        this.head.zRot = sway * 0.6F;

        this.load.visible = false; // a carga de verdade é desenhada pela ClayGolemCarriedItemLayer
        if (state.carrying) {
            this.rightArm.xRot = -1.35F;
            this.leftArm.xRot = -1.35F;
            this.rightArm.zRot = -0.15F;
            this.leftArm.zRot = 0.15F;
        } else {
            this.rightArm.xRot = Mth.cos(walk + Mth.PI) * 0.8F * speed + Mth.sin(state.ageInTicks * 0.07F) * 0.05F;
            this.leftArm.xRot = Mth.cos(walk) * 0.8F * speed - Mth.sin(state.ageInTicks * 0.07F) * 0.05F;
            this.rightArm.zRot = 0.1F;
            this.leftArm.zRot = -0.1F;
        }
    }
}

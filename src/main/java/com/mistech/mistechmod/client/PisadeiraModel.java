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

/**
 * Pisadeira: velha magra de cócoras, curvada para frente, cabelo desgrenhado, nariz comprido,
 * braços longos e unhas enormes. Parada em cima do peito de quem dorme, olhando para o rosto.
 */
public class PisadeiraModel extends EntityModel<PisadeiraRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Mistech.MODID, "pisadeira"), "main");

    private static final float TORSO_LEAN = 0.85F;
    private static final float HEAD_TILT = -0.35F;
    private static final float ARM_PITCH = -1.3F;

    private final ModelPart torso;
    private final ModelPart head;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightClaw;
    private final ModelPart leftClaw;

    public PisadeiraModel(ModelPart root) {
        super(root);
        this.torso = root.getChild("torso");
        this.head = this.torso.getChild("head");
        this.rightArm = this.torso.getChild("right_arm");
        this.leftArm = this.torso.getChild("left_arm");
        this.rightClaw = this.rightArm.getChild("claw");
        this.leftClaw = this.leftArm.getChild("claw");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // tronco curvado para frente, a partir do quadril
        PartDefinition torso = root.addOrReplaceChild("torso", CubeListBuilder.create().texOffs(16, 16)
                .addBox(-3.0F, -10.0F, -1.5F, 6.0F, 10.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 18.0F, 1.0F, TORSO_LEAN, 0.0F, 0.0F));

        // cabeça + cabelo (casca) + nariz + cabelo comprido caindo atrás
        PartDefinition head = torso.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3.0F, -7.0F, -3.0F, 6.0F, 7.0F, 6.0F)
                .texOffs(32, 0).addBox(-3.0F, -7.0F, -3.0F, 6.0F, 7.0F, 6.0F, new CubeDeformation(0.6F)),
                PartPose.offsetAndRotation(0.0F, -10.0F, 0.0F, HEAD_TILT, 0.0F, 0.0F));
        head.addOrReplaceChild("nose", CubeListBuilder.create().texOffs(24, 0).addBox(-0.5F, -4.0F, -5.0F, 1.0F, 3.0F, 2.0F),
                PartPose.ZERO);
        head.addOrReplaceChild("long_hair", CubeListBuilder.create().texOffs(0, 44).addBox(-3.5F, 0.0F, 0.0F, 7.0F, 12.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, -6.0F, 3.3F, 0.35F, 0.0F, 0.0F));

        // braços compridos pendurados na frente, com as garras
        PartDefinition rightArm = torso.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(40, 16)
                .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 14.0F, 2.0F), PartPose.offsetAndRotation(-4.0F, -9.0F, 0.0F, ARM_PITCH, 0.0F, 0.15F));
        rightArm.addOrReplaceChild("claw", CubeListBuilder.create().texOffs(48, 16).addBox(-1.5F, 0.0F, -0.5F, 3.0F, 5.0F, 1.0F),
                PartPose.offset(0.0F, 14.0F, 0.0F));
        PartDefinition leftArm = torso.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(40, 16).mirror()
                .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 14.0F, 2.0F), PartPose.offsetAndRotation(4.0F, -9.0F, 0.0F, ARM_PITCH, 0.0F, -0.15F));
        leftArm.addOrReplaceChild("claw", CubeListBuilder.create().texOffs(48, 16).mirror().addBox(-1.5F, 0.0F, -0.5F, 3.0F, 5.0F, 1.0F),
                PartPose.offset(0.0F, 14.0F, 0.0F));

        // pernas de cócoras: coxa para frente e para cima, canela descendo até o pé
        for (int side = -1; side <= 1; side += 2) {
            String name = side < 0 ? "right" : "left";
            CubeListBuilder thighCubes = CubeListBuilder.create().texOffs(0, 16);
            CubeListBuilder shinCubes = CubeListBuilder.create().texOffs(0, 28);
            if (side > 0) {
                thighCubes.mirror();
                shinCubes.mirror();
            }
            PartDefinition thigh = root.addOrReplaceChild(name + "_thigh", thighCubes.addBox(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F),
                    PartPose.offsetAndRotation(side * 2.2F, 18.0F, 1.0F, -1.9F, side * -0.2F, 0.0F));
            thigh.addOrReplaceChild("shin", shinCubes.addBox(-1.0F, 0.0F, -1.0F, 2.0F, 9.0F, 2.0F),
                    PartPose.offsetAndRotation(0.0F, 8.0F, 0.0F, 1.9F, 0.0F, 0.0F));
        }

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(PisadeiraRenderState state) {
        super.setupAnim(state);
        float t = state.ageInTicks;
        // respiração pesada e a cabeça inclinando de lado, de um jeito estranho
        this.torso.xRot = TORSO_LEAN + Mth.sin(t * 0.15F) * 0.04F;
        this.head.zRot = Mth.sin(t * 0.07F) * 0.25F;
        this.head.xRot = HEAD_TILT + Mth.sin(t * 0.11F) * 0.06F;
        // risada: a cabeça treme um pouco a cada 1,5 s
        float laugh = (t % 30.0F) < 8.0F ? Mth.sin(t * 2.2F) * 0.06F : 0.0F;
        this.head.yRot = laugh;
        // braços apertando o peito, garras mexendo
        this.rightArm.xRot = ARM_PITCH + Mth.sin(t * 0.2F) * 0.05F;
        this.leftArm.xRot = ARM_PITCH + Mth.sin(t * 0.2F + 1.3F) * 0.05F;
        this.rightClaw.xRot = 0.3F + Mth.sin(t * 0.35F) * 0.25F;
        this.leftClaw.xRot = 0.3F + Mth.sin(t * 0.35F + 2.0F) * 0.25F;
    }
}

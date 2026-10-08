package com.mistech.mistechmod.beyond.client;

import com.mistech.mistechmod.Mistech;

import net.minecraft.client.model.EntityModel;
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
 * Ecólito: bolinha de sculk flutuando, com duas antenas (como as do Warden) que tremem, barbatanas laterais que batem
 * e um rabicho de névoa. Textura 32×32 gerada por script (olhos e pontas das antenas brilham numa camada à parte).
 */
public class EcholingModel extends EntityModel<ParrotRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Mistech.MODID, "echoling"), "main");

    private final ModelPart body;
    private final ModelPart leftTendril;
    private final ModelPart rightTendril;
    private final ModelPart leftFin;
    private final ModelPart rightFin;
    private final ModelPart tail;

    public EcholingModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.leftTendril = this.body.getChild("left_tendril");
        this.rightTendril = this.body.getChild("right_tendril");
        this.leftFin = this.body.getChild("left_fin");
        this.rightFin = this.body.getChild("right_fin");
        this.tail = this.body.getChild("tail");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -3.0F, -3.0F, 6.0F, 6.0F, 6.0F),
                PartPose.offset(0.0F, 16.0F, 0.0F));
        body.addOrReplaceChild("left_tendril", CubeListBuilder.create().texOffs(0, 12).addBox(0.0F, -5.0F, 0.0F, 5.0F, 5.0F, 0.0F),
                PartPose.offsetAndRotation(1.5F, -2.5F, 0.0F, 0.0F, 0.0F, 0.45F));
        body.addOrReplaceChild("right_tendril", CubeListBuilder.create().texOffs(0, 12).mirror().addBox(-5.0F, -5.0F, 0.0F, 5.0F, 5.0F, 0.0F).mirror(false),
                PartPose.offsetAndRotation(-1.5F, -2.5F, 0.0F, 0.0F, 0.0F, -0.45F));
        body.addOrReplaceChild("left_fin", CubeListBuilder.create().texOffs(12, 12).addBox(0.0F, 0.0F, -2.0F, 0.0F, 3.0F, 4.0F),
                PartPose.offset(3.0F, -0.5F, 0.0F));
        body.addOrReplaceChild("right_fin", CubeListBuilder.create().texOffs(12, 12).mirror().addBox(0.0F, 0.0F, -2.0F, 0.0F, 3.0F, 4.0F).mirror(false),
                PartPose.offset(-3.0F, -0.5F, 0.0F));
        body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(20, 12).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, 1.5F, 3.0F, -0.35F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void setupAnim(ParrotRenderState state) {
        super.setupAnim(state);
        float t = state.ageInTicks;
        this.body.yRot = state.yRot * Mth.DEG_TO_RAD;
        this.body.xRot = state.xRot * Mth.DEG_TO_RAD * 0.5F;
        // flutua subindo e descendo
        this.body.y += Mth.sin(t * 0.12F) * 1.0F;
        // antenas tremem (mais rápido quando voa)
        float shiver = state.pose == net.minecraft.client.model.animal.parrot.ParrotModel.Pose.FLYING ? 0.35F : 0.12F;
        this.leftTendril.zRot = 0.45F + Mth.sin(t * 0.9F) * shiver;
        this.rightTendril.zRot = -0.45F - Mth.sin(t * 0.9F + 1.3F) * shiver;
        this.leftTendril.xRot = Mth.cos(t * 0.21F) * 0.15F;
        this.rightTendril.xRot = Mth.cos(t * 0.21F + 0.7F) * 0.15F;
        // barbatanas batem
        float flap = state.flapAngle;
        this.leftFin.zRot = -0.25F - flap * 0.9F - Mth.sin(t * 0.3F) * 0.1F;
        this.rightFin.zRot = 0.25F + flap * 0.9F + Mth.sin(t * 0.3F) * 0.1F;
        this.tail.yRot = Mth.sin(t * 0.25F) * 0.4F;
        if (state.pose == net.minecraft.client.model.animal.parrot.ParrotModel.Pose.SITTING) {
            this.body.y += 3.0F;
        }
    }
}

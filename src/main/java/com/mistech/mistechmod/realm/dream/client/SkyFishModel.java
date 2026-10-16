package com.mistech.mistechmod.realm.dream.client;

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

/** Peixe-do-Ar: rabo abanando, barbatanas batendo. Geometria e textura: {@code tools/dream/mobs3.py}. */
public class SkyFishModel extends EntityModel<DreamMobRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Mistech.MODID, "sky_fish"), "main");
    private final ModelPart body, tail, rightFin, leftFin;

    public SkyFishModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.tail = this.body.getChild("tail");
        this.rightFin = this.body.getChild("right_fin");
        this.leftFin = this.body.getChild("left_fin");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, -1.5F, -3.0F, 2.0F, 3.0F, 6.0F).texOffs(24, 0).addBox(-0.5F, -3.0F, -1.0F, 1.0F, 2.0F, 3.0F), PartPose.offset(0.0F, 21.0F, 0.0F));
        PartDefinition p_tail = p_body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(16, 0).addBox(-0.5F, -2.0F, 0.0F, 1.0F, 4.0F, 3.0F), PartPose.offset(0.0F, 0.0F, 3.0F));
        PartDefinition p_right_fin = p_body.addOrReplaceChild("right_fin", CubeListBuilder.create().texOffs(0, 9).addBox(-2.0F, 0.0F, 0.0F, 2.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(-1.0F, 0.5F, -1.0F, 0.0F, 0.0F, 0.6F));
        PartDefinition p_left_fin = p_body.addOrReplaceChild("left_fin", CubeListBuilder.create().texOffs(8, 9).addBox(0.0F, 0.0F, 0.0F, 2.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(1.0F, 0.5F, -1.0F, 0.0F, 0.0F, -0.6F));
        return LayerDefinition.create(mesh, 32, 16);
    }

    @Override
    public void setupAnim(DreamMobRenderState state) {
        super.setupAnim(state);
        float age = state.ageInTicks;
        float speed = 1.0F + state.walkAnimationSpeed * 2.0F;
        this.tail.yRot = Mth.sin(age * 0.6F * speed) * 0.5F;
        this.body.yRot = -Mth.sin(age * 0.6F * speed) * 0.08F;
        this.rightFin.zRot += Mth.sin(age * 0.9F) * 0.3F;
        this.leftFin.zRot -= Mth.sin(age * 0.9F) * 0.3F;
    }
}

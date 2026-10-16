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

/** Baleia-Nuvem: o rabo ondula em onda, as nadadeiras remam devagar. Geometria e textura: {@code tools/dream/mobs3.py}. */
public class CloudWhaleModel extends EntityModel<DreamMobRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Mistech.MODID, "cloud_whale"), "main");
    private final ModelPart body, tail, tailEnd, flukes, rightFin, leftFin, saddle;

    public CloudWhaleModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.tail = this.body.getChild("tail");
        this.tailEnd = this.tail.getChild("tail_end");
        this.flukes = this.tailEnd.getChild("flukes");
        this.rightFin = this.body.getChild("right_fin");
        this.leftFin = this.body.getChild("left_fin");
        this.saddle = this.body.getChild("saddle");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -7.0F, -10.0F, 16.0F, 14.0F, 22.0F).texOffs(0, 66).addBox(-6.5F, -8.0F, -9.0F, 13.0F, 1.0F, 20.0F).texOffs(0, 87).addBox(-6.5F, 7.0F, -9.0F, 13.0F, 1.0F, 20.0F).texOffs(76, 0).addBox(-9.0F, -5.0F, -9.0F, 1.0F, 10.0F, 20.0F).texOffs(0, 36).addBox(8.0F, -5.0F, -9.0F, 1.0F, 10.0F, 20.0F).texOffs(0, 122).addBox(-6.0F, -10.0F, -6.0F, 8.0F, 3.0F, 8.0F).texOffs(28, 108).addBox(-1.0F, -11.0F, -2.0F, 7.0F, 4.0F, 9.0F).texOffs(68, 133).addBox(-4.0F, -9.0F, 4.0F, 9.0F, 2.0F, 6.0F).texOffs(98, 133).addBox(-3.0F, -8.0F, -12.0F, 6.0F, 2.0F, 6.0F), PartPose.offset(0.0F, 10.0F, 0.0F));
        PartDefinition p_head = p_body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(42, 36).addBox(-7.0F, -6.0F, -12.0F, 14.0F, 12.0F, 12.0F).texOffs(32, 122).addBox(-5.5F, -4.5F, -14.0F, 11.0F, 9.0F, 2.0F).texOffs(60, 108).addBox(-6.0F, -7.0F, -11.0F, 12.0F, 1.0F, 10.0F), PartPose.offset(0.0F, 0.0F, -10.0F));
        PartDefinition p_tail = p_body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(66, 87).addBox(-5.0F, -4.0F, 0.0F, 10.0F, 9.0F, 10.0F), PartPose.offset(0.0F, -1.0F, 12.0F));
        PartDefinition p_tail_end = p_tail.addOrReplaceChild("tail_end", CubeListBuilder.create().texOffs(0, 108).addBox(-3.0F, -2.5F, 0.0F, 6.0F, 6.0F, 8.0F), PartPose.offset(0.0F, 0.0F, 10.0F));
        PartDefinition p_flukes = p_tail_end.addOrReplaceChild("flukes", CubeListBuilder.create().texOffs(58, 122).addBox(-11.0F, -0.5F, 0.0F, 22.0F, 1.0F, 7.0F), PartPose.offset(0.0F, 0.5F, 7.0F));
        PartDefinition p_right_fin = p_body.addOrReplaceChild("right_fin", CubeListBuilder.create().texOffs(0, 133).addBox(-10.0F, -0.5F, -2.0F, 10.0F, 1.0F, 7.0F), PartPose.offsetAndRotation(-8.0F, 4.0F, -5.0F, 0.0F, 0.0F, 0.3F));
        PartDefinition p_left_fin = p_body.addOrReplaceChild("left_fin", CubeListBuilder.create().texOffs(34, 133).addBox(0.0F, -0.5F, -2.0F, 10.0F, 1.0F, 7.0F), PartPose.offsetAndRotation(8.0F, 4.0F, -5.0F, 0.0F, 0.0F, -0.3F));
        // sela (só aparece selada): assento de couro à frente das nuvens, cilha em volta do corpo e estribos. Textura: tools/dream/whale_saddle.py
        p_body.addOrReplaceChild("saddle", CubeListBuilder.create()
                .texOffs(0, 150).addBox(-4.5F, -11.0F, -10.0F, 9.0F, 3.0F, 7.0F)
                .texOffs(32, 150).addBox(-1.5F, -13.0F, -11.0F, 3.0F, 2.0F, 2.0F)
                .texOffs(44, 150).addBox(-11.0F, -2.0F, -8.0F, 1.0F, 6.0F, 1.0F)
                .texOffs(50, 150).addBox(10.0F, -2.0F, -8.0F, 1.0F, 6.0F, 1.0F)
                .texOffs(0, 160).addBox(-9.0F, -9.0F, -8.5F, 18.0F, 1.0F, 2.0F)
                .texOffs(0, 164).addBox(-9.0F, 8.0F, -8.5F, 18.0F, 1.0F, 2.0F)
                .texOffs(40, 160).addBox(-10.0F, -9.0F, -8.5F, 1.0F, 18.0F, 2.0F)
                .texOffs(46, 160).addBox(9.0F, -9.0F, -8.5F, 1.0F, 18.0F, 2.0F), PartPose.ZERO);
        return LayerDefinition.create(mesh, 128, 256);
    }

    @Override
    public void setupAnim(DreamMobRenderState state) {
        super.setupAnim(state);
        float age = state.ageInTicks;
        float w = age * 0.07F;
        this.body.xRot = Mth.sin(w) * 0.04F;
        this.tail.xRot = Mth.sin(w - 0.8F) * 0.12F;
        this.tailEnd.xRot = Mth.sin(w - 1.6F) * 0.18F;
        this.flukes.xRot = Mth.sin(w - 2.4F) * 0.25F;
        this.rightFin.zRot += Mth.sin(w * 0.8F) * 0.18F;
        this.leftFin.zRot -= Mth.sin(w * 0.8F) * 0.18F;
        this.saddle.visible = state.saddled;
    }
}

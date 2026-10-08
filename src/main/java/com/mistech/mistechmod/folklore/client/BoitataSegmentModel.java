package com.mistech.mistechmod.folklore.client;

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


/** Boitatá (segmento do corpo), desenhado várias vezes ao longo do rastro. Geometria e textura geradas por script (gen/boitata_segment.py). */
public class BoitataSegmentModel extends EntityModel<BoitataRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Mistech.MODID, "boitata_segment"), "main");

    private final ModelPart fin;

    public BoitataSegmentModel(ModelPart root) {
        super(root);
        this.fin = root.getChild("fin");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, -5.0F, -5.0F, 10.0F, 10.0F, 10.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p_fin = root.addOrReplaceChild("fin", CubeListBuilder.create().texOffs(40, 0).addBox(-1.0F, -5.0F, -3.0F, 2.0F, 5.0F, 6.0F), PartPose.offsetAndRotation(0.0F, -5.0F, 0.0F, -0.4F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(BoitataRenderState state) {
        super.setupAnim(state);
        this.fin.yScale = 1.0F + Mth.sin(state.ageInTicks * 0.4F) * 0.2F;
    }
}

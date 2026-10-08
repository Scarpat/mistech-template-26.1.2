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


/** Boitatá (cabeça): cabeça de cobra de fogo com olhos enormes e crista de chamas. Geometria e textura geradas por script (gen/boitata.py). */
public class BoitataModel extends EntityModel<BoitataRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Mistech.MODID, "boitata"), "main");

    private final ModelPart jaw;
    private final ModelPart crestA;
    private final ModelPart crestB;
    private final ModelPart crestC;
    private final ModelPart rightWhisker;
    private final ModelPart leftWhisker;

    public BoitataModel(ModelPart root) {
        super(root);
        this.jaw = root.getChild("jaw");
        this.crestA = root.getChild("crest_a");
        this.crestB = root.getChild("crest_b");
        this.crestC = root.getChild("crest_c");
        this.rightWhisker = root.getChild("right_whisker");
        this.leftWhisker = root.getChild("left_whisker");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_skull = root.addOrReplaceChild("skull", CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, -6.0F, -7.0F, 10.0F, 6.0F, 12.0F), PartPose.offset(0.0F, 24.0F, 0.0F));
        PartDefinition p_brow = root.addOrReplaceChild("brow", CubeListBuilder.create().texOffs(44, 18).addBox(-5.5F, -1.0F, -2.0F, 11.0F, 1.0F, 4.0F), PartPose.offset(0.0F, 18.0F, -5.0F));
        PartDefinition p_snout = root.addOrReplaceChild("snout", CubeListBuilder.create().texOffs(96, 0).addBox(-3.5F, -5.0F, -12.0F, 7.0F, 4.0F, 5.0F), PartPose.offset(0.0F, 24.0F, 0.0F));
        PartDefinition p_right_fang = p_snout.addOrReplaceChild("right_fang", CubeListBuilder.create().texOffs(74, 18).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 2.0F, 1.0F), PartPose.offset(-2.5F, -1.0F, -11.0F));
        PartDefinition p_left_fang = p_snout.addOrReplaceChild("left_fang", CubeListBuilder.create().texOffs(78, 18).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 2.0F, 1.0F), PartPose.offset(2.5F, -1.0F, -11.0F));
        PartDefinition p_jaw = root.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(54, 0).addBox(-4.0F, 0.0F, -7.0F, 8.0F, 2.0F, 8.0F), PartPose.offsetAndRotation(0.0F, 22.0F, -5.0F, 0.25F, 0.0F, 0.0F));
        PartDefinition p_right_eye = root.addOrReplaceChild("right_eye", CubeListBuilder.create().texOffs(20, 18).addBox(-7.0F, -6.5F, -6.5F, 3.0F, 3.0F, 3.0F), PartPose.offset(0.0F, 24.0F, 0.0F));
        PartDefinition p_left_eye = root.addOrReplaceChild("left_eye", CubeListBuilder.create().texOffs(32, 18).addBox(4.0F, -6.5F, -6.5F, 3.0F, 3.0F, 3.0F), PartPose.offset(0.0F, 24.0F, 0.0F));
        PartDefinition p_crest_a = root.addOrReplaceChild("crest_a", CubeListBuilder.create().texOffs(0, 18).addBox(-1.0F, -6.0F, -1.5F, 2.0F, 6.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 18.0F, -4.0F, -0.4F, 0.0F, 0.0F));
        PartDefinition p_crest_b = root.addOrReplaceChild("crest_b", CubeListBuilder.create().texOffs(44, 0).addBox(-1.0F, -9.0F, -1.5F, 2.0F, 9.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 18.0F, -0.5F, -0.6F, 0.0F, 0.0F));
        PartDefinition p_crest_c = root.addOrReplaceChild("crest_c", CubeListBuilder.create().texOffs(86, 0).addBox(-1.0F, -7.0F, -1.5F, 2.0F, 7.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 18.0F, 3.0F, -0.8F, 0.0F, 0.0F));
        PartDefinition p_crest_d = root.addOrReplaceChild("crest_d", CubeListBuilder.create().texOffs(10, 18).addBox(-1.0F, -5.0F, -1.5F, 2.0F, 5.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 18.0F, 5.0F, -1.0F, 0.0F, 0.0F));
        PartDefinition p_right_whisker = root.addOrReplaceChild("right_whisker", CubeListBuilder.create().texOffs(82, 18).addBox(-7.0F, -0.5F, -0.5F, 7.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(-3.5F, 21.0F, -11.0F, 0.0F, 0.6F, 0.25F));
        PartDefinition p_left_whisker = root.addOrReplaceChild("left_whisker", CubeListBuilder.create().texOffs(98, 18).addBox(0.0F, -0.5F, -0.5F, 7.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(3.5F, 21.0F, -11.0F, 0.0F, -0.6F, -0.25F));
        return LayerDefinition.create(mesh, 128, 32);
    }

    @Override
    public void setupAnim(BoitataRenderState state) {
        super.setupAnim(state);
        float t = state.ageInTicks;
        this.jaw.xRot += 0.15F + Mth.sin(t * 0.15F) * 0.15F + state.rage * 0.25F;
        this.crestA.yScale = 1.0F + Mth.sin(t * 0.5F) * 0.2F;
        this.crestB.yScale = 1.0F + Mth.sin(t * 0.45F + 1.0F) * 0.25F;
        this.crestC.yScale = 1.0F + Mth.sin(t * 0.55F + 2.0F) * 0.2F;
        this.rightWhisker.yRot += Mth.sin(t * 0.12F) * 0.25F;
        this.leftWhisker.yRot -= Mth.sin(t * 0.12F) * 0.25F;
    }
}

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

/** Mariposa-Vitral: asas de vitral batendo, abdômen de filamento balançando. Geometria e textura: {@code tools/dream/mobs3.py}. */
public class PrismMothModel extends EntityModel<DreamMobRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Mistech.MODID, "prism_moth"), "main");
    private final ModelPart body, head, rightWing, leftWing, rightHind, leftHind, abdomen, rightAntenna, leftAntenna;

    public PrismMothModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.rightWing = this.body.getChild("right_wing");
        this.leftWing = this.body.getChild("left_wing");
        this.rightHind = this.body.getChild("right_hind_wing");
        this.leftHind = this.body.getChild("left_hind_wing");
        this.abdomen = this.body.getChild("abdomen");
        this.rightAntenna = this.head.getChild("right_antenna");
        this.leftAntenna = this.head.getChild("left_antenna");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(18, 29).addBox(-1.5F, -1.5F, -2.0F, 3.0F, 3.0F, 4.0F), PartPose.offset(0.0F, 18.0F, 0.0F));
        PartDefinition p_head = p_body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(48, 29).addBox(-1.5F, -1.5F, -2.0F, 3.0F, 3.0F, 2.0F), PartPose.offset(0.0F, 0.0F, -2.0F));
        PartDefinition p_right_antenna = p_head.addOrReplaceChild("right_antenna", CubeListBuilder.create().texOffs(32, 29).addBox(-1.5F, -6.0F, -0.495F, 3.0F, 6.0F, 1.0F, new CubeDeformation(0.0F, 0.0F, -0.495F)), PartPose.offsetAndRotation(-1.0F, -1.5F, -1.5F, -0.5F, 0.0F, -0.5F));
        PartDefinition p_left_antenna = p_head.addOrReplaceChild("left_antenna", CubeListBuilder.create().texOffs(40, 29).addBox(-1.5F, -6.0F, -0.495F, 3.0F, 6.0F, 1.0F, new CubeDeformation(0.0F, 0.0F, -0.495F)), PartPose.offsetAndRotation(1.0F, -1.5F, -1.5F, -0.5F, 0.0F, 0.5F));
        PartDefinition p_abdomen = p_body.addOrReplaceChild("abdomen", CubeListBuilder.create().texOffs(0, 29).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 7.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 2.0F, -0.15F, 0.0F, 0.0F));
        PartDefinition p_right_wing = p_body.addOrReplaceChild("right_wing", CubeListBuilder.create().texOffs(0, 0).addBox(-12.0F, -0.495F, -4.0F, 12.0F, 1.0F, 9.0F, new CubeDeformation(0.0F, -0.495F, 0.0F)), PartPose.offset(-1.5F, -1.5F, -1.0F));
        PartDefinition p_right_hind_wing = p_body.addOrReplaceChild("right_hind_wing", CubeListBuilder.create().texOffs(0, 20).addBox(-8.0F, -0.495F, 0.0F, 8.0F, 1.0F, 8.0F, new CubeDeformation(0.0F, -0.495F, 0.0F)), PartPose.offset(-1.5F, -1.0F, 2.0F));
        PartDefinition p_left_wing = p_body.addOrReplaceChild("left_wing", CubeListBuilder.create().texOffs(0, 10).addBox(0.0F, -0.495F, -4.0F, 12.0F, 1.0F, 9.0F, new CubeDeformation(0.0F, -0.495F, 0.0F)), PartPose.offset(1.5F, -1.5F, -1.0F));
        PartDefinition p_left_hind_wing = p_body.addOrReplaceChild("left_hind_wing", CubeListBuilder.create().texOffs(32, 20).addBox(0.0F, -0.495F, 0.0F, 8.0F, 1.0F, 8.0F, new CubeDeformation(0.0F, -0.495F, 0.0F)), PartPose.offset(1.5F, -1.0F, 2.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(DreamMobRenderState state) {
        super.setupAnim(state);
        float age = state.ageInTicks;
        float flap = 0.25F + Mth.sin(age * 1.3F) * 0.7F;
        this.rightWing.zRot = flap;
        this.leftWing.zRot = -flap;
        this.rightHind.zRot = flap * 0.8F;
        this.leftHind.zRot = -flap * 0.8F;
        this.abdomen.xRot += Mth.sin(age * 0.2F) * 0.1F;
        this.rightAntenna.zRot += Mth.sin(age * 0.15F) * 0.1F;
        this.leftAntenna.zRot -= Mth.sin(age * 0.15F) * 0.1F;
        this.body.y += Mth.sin(age * 0.25F) * 0.8F;
    }
}

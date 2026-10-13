package com.mistech.mistechmod.lore.client;

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

/** Caipora montada no queixada: espírito pequeno de pele morena pintada de urucum e jenipapo, juba de fogo, cachimbo (a "mandíbula" animada) e borduna com penas. Geometria e textura: {@code tools/lore/models2.py}. */
public class CaiporaModel extends EntityModel<LoreRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Mistech.MODID, "caipora"), "main");

    private final ModelPart pig, pigHead, legFr, legFl, legBr, legBl, rider, riderHead, spearArm, jaw;

    public CaiporaModel(ModelPart root) {
        super(root);
        this.pig = root.getChild("pig");
        this.pigHead = root.getChild("pig_head");
        this.legFr = root.getChild("leg_fr");
        this.legFl = root.getChild("leg_fl");
        this.legBr = root.getChild("leg_br");
        this.legBl = root.getChild("leg_bl");
        this.rider = root.getChild("rider");
        this.riderHead = this.rider.getChild("rider_head");
        this.spearArm = this.rider.getChild("rider_right_arm");
        this.jaw = this.riderHead.getChild("rider_jaw");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_pig = root.addOrReplaceChild("pig", CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, -5.0F, -8.0F, 10.0F, 10.0F, 16.0F), PartPose.offset(0.0F, 13.0F, 1.0F));
        PartDefinition p_bristle_0 = p_pig.addOrReplaceChild("bristle_0", CubeListBuilder.create().texOffs(10, 58).addBox(-1.0F, -3.0F, -1.0F, 2.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(0.0F, -5.0F, -7.0F, -0.45F, 0.0F, 0.0F));
        PartDefinition p_bristle_1 = p_pig.addOrReplaceChild("bristle_1", CubeListBuilder.create().texOffs(68, 50).addBox(-1.0F, -4.0F, -1.0F, 2.0F, 4.0F, 2.0F), PartPose.offsetAndRotation(0.0F, -5.0F, -4.5F, -0.45F, 0.0F, 0.0F));
        PartDefinition p_bristle_2 = p_pig.addOrReplaceChild("bristle_2", CubeListBuilder.create().texOffs(76, 50).addBox(-1.0F, -4.5F, -1.0F, 2.0F, 4.5F, 2.0F), PartPose.offsetAndRotation(0.0F, -5.0F, -2.0F, -0.45F, 0.0F, 0.0F));
        PartDefinition p_bristle_3 = p_pig.addOrReplaceChild("bristle_3", CubeListBuilder.create().texOffs(84, 50).addBox(-1.0F, -4.0F, -1.0F, 2.0F, 4.0F, 2.0F), PartPose.offsetAndRotation(0.0F, -5.0F, 0.5F, -0.45F, 0.0F, 0.0F));
        PartDefinition p_bristle_4 = p_pig.addOrReplaceChild("bristle_4", CubeListBuilder.create().texOffs(18, 58).addBox(-1.0F, -3.0F, -1.0F, 2.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(0.0F, -5.0F, 3.0F, -0.45F, 0.0F, 0.0F));
        PartDefinition p_bristle_5 = p_pig.addOrReplaceChild("bristle_5", CubeListBuilder.create().texOffs(36, 58).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 2.0F, 2.0F), PartPose.offsetAndRotation(0.0F, -5.0F, 5.5F, -0.45F, 0.0F, 0.0F));
        PartDefinition p_blanket = p_pig.addOrReplaceChild("blanket", CubeListBuilder.create().texOffs(0, 39).addBox(-5.5F, 0.0F, -3.5F, 11.0F, 4.0F, 7.0F), PartPose.offset(0.0F, -5.0F, -1.0F));
        PartDefinition p_pig_tail = p_pig.addOrReplaceChild("pig_tail", CubeListBuilder.create().texOffs(76, 58).addBox(-0.5F, 0.0F, 0.0F, 1.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, -3.0F, 8.0F, 0.5F, 0.0F, 0.0F));
        PartDefinition p_pig_head = root.addOrReplaceChild("pig_head", CubeListBuilder.create().texOffs(84, 0).addBox(-4.0F, -4.0F, -6.0F, 8.0F, 8.0F, 6.0F), PartPose.offset(0.0F, 12.0F, -7.0F));
        PartDefinition p_snout = p_pig_head.addOrReplaceChild("snout", CubeListBuilder.create().texOffs(52, 50).addBox(-2.5F, -1.5F, -3.0F, 5.0F, 4.0F, 3.0F), PartPose.offset(0.0F, 1.0F, -6.0F));
        PartDefinition p_bridle = p_snout.addOrReplaceChild("bridle", CubeListBuilder.create().texOffs(106, 58).addBox(-2.7F, 0.0F, -0.6F, 5.4F, 0.8F, 1.2F), PartPose.offset(0.0F, -1.6F, -1.5F));
        PartDefinition p_right_tusk = p_snout.addOrReplaceChild("right_tusk", CubeListBuilder.create().texOffs(80, 58).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(-2.6F, 2.0F, -2.0F, -0.2F, 0.0F, -0.3F));
        PartDefinition p_right_tusk_tip = p_right_tusk.addOrReplaceChild("right_tusk_tip", CubeListBuilder.create().texOffs(98, 58).addBox(-0.5F, -2.0F, -0.5F, 1.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(0.0F, -3.0F, 0.0F, -0.9F, 0.0F, 0.0F));
        PartDefinition p_left_tusk = p_snout.addOrReplaceChild("left_tusk", CubeListBuilder.create().texOffs(84, 58).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(2.6F, 2.0F, -2.0F, -0.2F, 0.0F, 0.3F));
        PartDefinition p_left_tusk_tip = p_left_tusk.addOrReplaceChild("left_tusk_tip", CubeListBuilder.create().texOffs(102, 58).addBox(-0.5F, -2.0F, -0.5F, 1.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(0.0F, -3.0F, 0.0F, -0.9F, 0.0F, 0.0F));
        PartDefinition p_pig_right_ear = p_pig_head.addOrReplaceChild("pig_right_ear", CubeListBuilder.create().texOffs(52, 58).addBox(-1.0F, -3.0F, 0.0F, 2.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(-3.5F, -4.0F, -1.0F, 0.3F, 0.0F, -0.6F));
        PartDefinition p_pig_left_ear = p_pig_head.addOrReplaceChild("pig_left_ear", CubeListBuilder.create().texOffs(58, 58).addBox(-1.0F, -3.0F, 0.0F, 2.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(3.5F, -4.0F, -1.0F, 0.3F, 0.0F, 0.6F));
        PartDefinition p_feather_charm = p_pig_head.addOrReplaceChild("feather_charm", CubeListBuilder.create().texOffs(34, 58).addBox(-0.25F, 0.0F, -0.5F, 0.5F, 4.0F, 1.0F), PartPose.offsetAndRotation(-3.6F, -3.0F, -1.0F, 0.0F, 0.0F, 0.3F));
        PartDefinition p_leg_fr = root.addOrReplaceChild("leg_fr", CubeListBuilder.create().texOffs(56, 39).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 7.0F, 3.0F), PartPose.offset(-3.0F, 17.0F, -5.5F));
        PartDefinition p_leg_fl = root.addOrReplaceChild("leg_fl", CubeListBuilder.create().texOffs(68, 39).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 7.0F, 3.0F), PartPose.offset(3.0F, 17.0F, -5.5F));
        PartDefinition p_leg_br = root.addOrReplaceChild("leg_br", CubeListBuilder.create().texOffs(80, 39).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 7.0F, 3.0F), PartPose.offset(-3.0F, 17.0F, 6.5F));
        PartDefinition p_leg_bl = root.addOrReplaceChild("leg_bl", CubeListBuilder.create().texOffs(92, 39).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 7.0F, 3.0F), PartPose.offset(3.0F, 17.0F, 6.5F));
        PartDefinition p_rider = root.addOrReplaceChild("rider", CubeListBuilder.create().texOffs(36, 39).addBox(-3.0F, -7.0F, -2.0F, 6.0F, 7.0F, 4.0F), PartPose.offset(0.0F, 8.0F, 1.5F));
        PartDefinition p_skirt = p_rider.addOrReplaceChild("skirt", CubeListBuilder.create().texOffs(0, 50).addBox(-3.5F, 0.0F, -2.5F, 7.0F, 3.0F, 5.0F), PartPose.offset(0.0F, -1.0F, 0.0F));
        PartDefinition p_necklace = p_rider.addOrReplaceChild("necklace", CubeListBuilder.create().texOffs(118, 58).addBox(-2.5F, 0.0F, -0.3F, 5.0F, 2.0F, 0.3F), PartPose.offset(0.0F, -6.5F, -2.1F));
        PartDefinition p_rider_head = p_rider.addOrReplaceChild("rider_head", CubeListBuilder.create().texOffs(52, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F), PartPose.offset(0.0F, -7.0F, 0.0F));
        PartDefinition p_nose = p_rider_head.addOrReplaceChild("nose", CubeListBuilder.create().texOffs(92, 58).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 1.5F, 1.0F), PartPose.offset(0.0F, -3.5F, -4.0F));
        PartDefinition p_right_ear = p_rider_head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(64, 58).addBox(-0.5F, -1.0F, -1.0F, 1.0F, 2.0F, 2.0F), PartPose.offset(-4.0F, -4.0F, 0.0F));
        PartDefinition p_left_ear = p_rider_head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(70, 58).addBox(-0.5F, -1.0F, -1.0F, 1.0F, 2.0F, 2.0F), PartPose.offset(4.0F, -4.0F, 0.0F));
        PartDefinition p_rider_jaw = p_rider_head.addOrReplaceChild("rider_jaw", CubeListBuilder.create().texOffs(44, 58).addBox(-0.5F, -0.5F, -3.0F, 1.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(1.5F, -1.5F, -4.0F, 0.25F, 0.0F, 0.0F));
        PartDefinition p_pipe_bowl = p_rider_jaw.addOrReplaceChild("pipe_bowl", CubeListBuilder.create().texOffs(26, 58).addBox(-1.0F, -2.5F, -1.5F, 2.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.0F, -3.0F, -0.25F, 0.0F, 0.0F));
        PartDefinition p_hair = p_rider_head.addOrReplaceChild("hair", CubeListBuilder.create().texOffs(60, 26).addBox(-4.5F, -3.0F, -4.5F, 9.0F, 3.0F, 9.0F), PartPose.offset(0.0F, -8.0F, 0.0F));
        PartDefinition p_hair_back = p_hair.addOrReplaceChild("hair_back", CubeListBuilder.create().texOffs(0, 26).addBox(-5.0F, 0.0F, 0.0F, 10.0F, 11.0F, 2.5F), PartPose.offset(0.0F, 0.0F, 3.5F));
        PartDefinition p_hair_r = p_hair.addOrReplaceChild("hair_r", CubeListBuilder.create().texOffs(24, 26).addBox(-1.5F, 0.0F, 0.0F, 2.0F, 7.0F, 6.0F), PartPose.offset(-4.5F, 0.0F, -2.0F));
        PartDefinition p_hair_l = p_hair.addOrReplaceChild("hair_l", CubeListBuilder.create().texOffs(40, 26).addBox(-0.5F, 0.0F, 0.0F, 2.0F, 7.0F, 6.0F), PartPose.offset(4.5F, 0.0F, -2.0F));
        PartDefinition p_flame_0 = p_hair.addOrReplaceChild("flame_0", CubeListBuilder.create().texOffs(92, 50).addBox(-1.0F, -3.5F, -1.0F, 2.0F, 3.5F, 2.0F), PartPose.offsetAndRotation(-2.5F, -3.0F, 0.0F, -0.3F, 0.0F, -0.4F));
        PartDefinition p_flame_1 = p_hair.addOrReplaceChild("flame_1", CubeListBuilder.create().texOffs(100, 50).addBox(-1.0F, -3.5F, -1.0F, 2.0F, 3.5F, 2.0F), PartPose.offsetAndRotation(0.0F, -3.0F, -1.0F, -0.15F, 0.0F, 0.0F));
        PartDefinition p_flame_2 = p_hair.addOrReplaceChild("flame_2", CubeListBuilder.create().texOffs(108, 50).addBox(-1.0F, -3.5F, -1.0F, 2.0F, 3.5F, 2.0F), PartPose.offsetAndRotation(2.5F, -3.0F, 0.0F, -0.3F, 0.0F, 0.4F));
        PartDefinition p_flame_3 = p_hair.addOrReplaceChild("flame_3", CubeListBuilder.create().texOffs(116, 50).addBox(-1.0F, -3.5F, -1.0F, 2.0F, 3.5F, 2.0F), PartPose.offsetAndRotation(-1.0F, -3.0F, 2.0F, 0.3F, 0.0F, -0.2F));
        PartDefinition p_flame_4 = p_hair.addOrReplaceChild("flame_4", CubeListBuilder.create().texOffs(0, 58).addBox(-1.0F, -3.5F, -1.0F, 2.0F, 3.5F, 2.0F), PartPose.offsetAndRotation(1.5F, -3.0F, 2.5F, 0.35F, 0.0F, 0.25F));
        PartDefinition p_head_feather = p_hair.addOrReplaceChild("head_feather", CubeListBuilder.create().texOffs(8, 58).addBox(-0.5F, -6.0F, -0.25F, 1.0F, 6.0F, 0.5F), PartPose.offsetAndRotation(3.0F, -2.0F, 2.0F, 0.4F, 0.0F, 0.5F));
        PartDefinition p_rider_right_leg = p_rider.addOrReplaceChild("rider_right_leg", CubeListBuilder.create().texOffs(36, 50).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F), PartPose.offsetAndRotation(-2.0F, 0.0F, 0.0F, -1.3F, 0.0F, 0.75F));
        PartDefinition p_rider_left_leg = p_rider.addOrReplaceChild("rider_left_leg", CubeListBuilder.create().texOffs(44, 50).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F), PartPose.offsetAndRotation(2.0F, 0.0F, 0.0F, -1.3F, 0.0F, -0.75F));
        PartDefinition p_rider_right_arm = p_rider.addOrReplaceChild("rider_right_arm", CubeListBuilder.create().texOffs(104, 39).addBox(-2.0F, 0.0F, -1.0F, 2.0F, 7.0F, 2.0F), PartPose.offsetAndRotation(-3.0F, -6.5F, 0.0F, -0.6F, 0.0F, 0.15F));
        PartDefinition p_spear = p_rider_right_arm.addOrReplaceChild("spear", CubeListBuilder.create().texOffs(56, 26).addBox(-0.5F, -4.0F, -0.5F, 1.0F, 12.0F, 1.0F).texOffs(24, 50).addBox(-1.5F, -9.0F, -1.5F, 3.0F, 5.0F, 3.0F), PartPose.offsetAndRotation(-1.0F, 6.5F, 0.0F, 1.2F, 0.0F, 0.0F));
        PartDefinition p_club_feather_0 = p_spear.addOrReplaceChild("club_feather_0", CubeListBuilder.create().texOffs(88, 58).addBox(-0.25F, -3.0F, -0.5F, 0.5F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, -4.0F, 0.0F, 0.0F, 0.0F, 2.64F));
        PartDefinition p_club_feather_1 = p_spear.addOrReplaceChild("club_feather_1", CubeListBuilder.create().texOffs(90, 58).addBox(-0.25F, -3.0F, -0.5F, 0.5F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, -4.0F, 0.0F, 0.0F, 0.0F, 3.64F));
        PartDefinition p_rider_left_arm = p_rider.addOrReplaceChild("rider_left_arm", CubeListBuilder.create().texOffs(112, 39).addBox(0.0F, 0.0F, -1.0F, 2.0F, 7.0F, 2.0F), PartPose.offsetAndRotation(3.0F, -6.5F, 0.0F, -0.9F, 0.0F, -0.15F));
        PartDefinition p_reins = p_rider_left_arm.addOrReplaceChild("reins", CubeListBuilder.create().texOffs(52, 50).addBox(-0.25F, 0.0F, -0.25F, 0.5F, 8.0F, 0.5F), PartPose.offsetAndRotation(1.0F, 7.0F, 0.0F, 0.5F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void setupAnim(LoreRenderState state) {
        super.setupAnim(state);
        float t = state.ageInTicks;
        float pos = state.walkAnimationPos;
        float speed = state.walkAnimationSpeed;
        float swing = Mth.cos(pos * 0.9F) * 1.0F * speed;
        this.legFr.xRot = swing;
        this.legBl.xRot = swing;
        this.legFl.xRot = -swing;
        this.legBr.xRot = -swing;
        float bob = Math.abs(Mth.sin(pos * 0.9F)) * speed;
        this.pig.y -= bob;
        this.pigHead.y -= bob;
        this.pigHead.xRot = Mth.sin(pos * 0.9F) * 0.1F * speed + Mth.sin(t * 0.1F) * 0.03F;
        this.rider.y -= bob * 1.2F;
        this.rider.xRot = Mth.sin(pos * 0.9F) * 0.06F * speed;
        this.riderHead.yRot = state.yRot * Mth.DEG_TO_RAD;
        this.riderHead.xRot = state.xRot * Mth.DEG_TO_RAD;
        this.jaw.xRot = 0.25F + Mth.sin(t * 0.12F) * 0.08F + (state.angry ? 0.25F + Mth.sin(t * 0.6F) * 0.15F : 0.0F);
        if (state.angry) {
            // lança erguida, pronta para golpear
            this.spearArm.xRot = -2.4F + Mth.sin(t * 0.3F) * 0.15F;
        }
    }
}

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

/** O Sonhador: o véu estrelado ondula, as mãos regem uma canção de ninar e os objetos orbitam a cabeça. Geometria e textura: {@code tools/dream/mobs3.py}. */
public class TheDreamerModel extends EntityModel<DreamMobRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Mistech.MODID, "the_dreamer"), "main");
    private final ModelPart head, veil, veil2, veil3, veil4, rightHand, leftHand, halo;

    public TheDreamerModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.veil = this.head.getChild("veil");
        this.veil2 = this.veil.getChild("veil_2");
        this.veil3 = this.veil2.getChild("veil_3");
        this.veil4 = this.veil3.getChild("veil_4");
        this.rightHand = root.getChild("right_hand");
        this.leftHand = root.getChild("left_hand");
        this.halo = root.getChild("halo");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(58, 0).addBox(-9.0F, -20.0F, -2.0F, 18.0F, 22.0F, 2.0F).texOffs(0, 0).addBox(-10.0F, -21.0F, 0.0F, 20.0F, 23.0F, 9.0F), PartPose.offset(0.0F, -14.0F, 0.0F));
        PartDefinition p_veil = p_head.addOrReplaceChild("veil", CubeListBuilder.create().texOffs(0, 32).addBox(-9.0F, 0.0F, -4.0F, 18.0F, 10.0F, 8.0F), PartPose.offset(0.0F, 2.0F, 4.0F));
        PartDefinition p_veil_2 = p_veil.addOrReplaceChild("veil_2", CubeListBuilder.create().texOffs(52, 32).addBox(-7.0F, 0.0F, -3.5F, 14.0F, 10.0F, 7.0F), PartPose.offset(0.0F, 10.0F, 0.0F));
        PartDefinition p_veil_3 = p_veil_2.addOrReplaceChild("veil_3", CubeListBuilder.create().texOffs(94, 32).addBox(-5.0F, 0.0F, -3.0F, 10.0F, 9.0F, 6.0F), PartPose.offset(0.0F, 10.0F, 0.0F));
        PartDefinition p_veil_4 = p_veil_3.addOrReplaceChild("veil_4", CubeListBuilder.create().texOffs(0, 50).addBox(-3.0F, 0.0F, -2.5F, 6.0F, 9.0F, 5.0F), PartPose.offset(0.0F, 9.0F, 0.0F));
        PartDefinition p_right_hand = root.addOrReplaceChild("right_hand", CubeListBuilder.create().texOffs(22, 50).addBox(-4.0F, -4.0F, -1.5F, 8.0F, 8.0F, 3.0F), PartPose.offset(-20.0F, -8.0F, -6.0F));
        PartDefinition p_right_hand_finger_0 = p_right_hand.addOrReplaceChild("right_hand_finger_0", CubeListBuilder.create().texOffs(110, 50).addBox(-1.0F, -6.875F, -1.0F, 2.0F, 7.0F, 2.0F, new CubeDeformation(-0.25F, -0.125F, -0.25F)), PartPose.offsetAndRotation(-2.75F, -4.0F, 0.0F, 0.0F, 0.0F, -0.14F));
        PartDefinition p_right_hand_finger_1 = p_right_hand.addOrReplaceChild("right_hand_finger_1", CubeListBuilder.create().texOffs(78, 50).addBox(-1.0F, -8.625F, -1.0F, 2.0F, 9.0F, 2.0F, new CubeDeformation(-0.25F, -0.375F, -0.25F)), PartPose.offsetAndRotation(-0.5499999999999998F, -4.0F, 0.0F, 0.2F, 0.0F, -0.05199999999999999F));
        PartDefinition p_right_hand_finger_2 = p_right_hand.addOrReplaceChild("right_hand_finger_2", CubeListBuilder.create().texOffs(86, 50).addBox(-1.0F, -8.625F, -1.0F, 2.0F, 9.0F, 2.0F, new CubeDeformation(-0.25F, -0.375F, -0.25F)), PartPose.offsetAndRotation(1.6500000000000004F, -4.0F, 0.0F, 0.0F, 0.0F, 0.03600000000000002F));
        PartDefinition p_right_hand_finger_3 = p_right_hand.addOrReplaceChild("right_hand_finger_3", CubeListBuilder.create().texOffs(118, 50).addBox(-1.0F, -6.875F, -1.0F, 2.0F, 7.0F, 2.0F, new CubeDeformation(-0.25F, -0.125F, -0.25F)), PartPose.offsetAndRotation(3.8500000000000005F, -4.0F, 0.0F, 0.2F, 0.0F, 0.12400000000000003F));
        PartDefinition p_right_hand_thumb = p_right_hand.addOrReplaceChild("right_hand_thumb", CubeListBuilder.create().texOffs(32, 64).addBox(-1.0F, -6.0F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(-0.25F, 0.0F, -0.25F)), PartPose.offsetAndRotation(4.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.9F));
        PartDefinition p_left_hand = root.addOrReplaceChild("left_hand", CubeListBuilder.create().texOffs(44, 50).addBox(-4.0F, -4.0F, -1.5F, 8.0F, 8.0F, 3.0F), PartPose.offset(20.0F, -8.0F, -6.0F));
        PartDefinition p_left_hand_finger_0 = p_left_hand.addOrReplaceChild("left_hand_finger_0", CubeListBuilder.create().texOffs(0, 64).addBox(-1.0F, -6.875F, -1.0F, 2.0F, 7.0F, 2.0F, new CubeDeformation(-0.25F, -0.125F, -0.25F)), PartPose.offsetAndRotation(-2.75F, -4.0F, 0.0F, 0.0F, 0.0F, -0.14F));
        PartDefinition p_left_hand_finger_1 = p_left_hand.addOrReplaceChild("left_hand_finger_1", CubeListBuilder.create().texOffs(94, 50).addBox(-1.0F, -8.625F, -1.0F, 2.0F, 9.0F, 2.0F, new CubeDeformation(-0.25F, -0.375F, -0.25F)), PartPose.offsetAndRotation(-0.5499999999999998F, -4.0F, 0.0F, 0.2F, 0.0F, -0.05199999999999999F));
        PartDefinition p_left_hand_finger_2 = p_left_hand.addOrReplaceChild("left_hand_finger_2", CubeListBuilder.create().texOffs(102, 50).addBox(-1.0F, -8.625F, -1.0F, 2.0F, 9.0F, 2.0F, new CubeDeformation(-0.25F, -0.375F, -0.25F)), PartPose.offsetAndRotation(1.6500000000000004F, -4.0F, 0.0F, 0.0F, 0.0F, 0.03600000000000002F));
        PartDefinition p_left_hand_finger_3 = p_left_hand.addOrReplaceChild("left_hand_finger_3", CubeListBuilder.create().texOffs(8, 64).addBox(-1.0F, -6.875F, -1.0F, 2.0F, 7.0F, 2.0F, new CubeDeformation(-0.25F, -0.125F, -0.25F)), PartPose.offsetAndRotation(3.8500000000000005F, -4.0F, 0.0F, 0.2F, 0.0F, 0.12400000000000003F));
        PartDefinition p_left_hand_thumb = p_left_hand.addOrReplaceChild("left_hand_thumb", CubeListBuilder.create().texOffs(40, 64).addBox(-1.0F, -6.0F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(-0.25F, 0.0F, -0.25F)), PartPose.offsetAndRotation(-4.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.9F));
        PartDefinition p_halo = root.addOrReplaceChild("halo", CubeListBuilder.create(), PartPose.offset(0.0F, -24.0F, 2.0F));
        PartDefinition p_clock = p_halo.addOrReplaceChild("clock", CubeListBuilder.create().texOffs(16, 64).addBox(-3.5F, -3.5F, -0.5F, 7.0F, 7.0F, 1.0F), PartPose.offsetAndRotation(17.0F, 0.0F, 0.0F, 0.0F, 0.6F, 0.3F));
        PartDefinition p_door = p_halo.addOrReplaceChild("door", CubeListBuilder.create().texOffs(66, 50).addBox(-2.5F, -5.0F, -0.5F, 5.0F, 10.0F, 1.0F), PartPose.offsetAndRotation(-17.0F, 0.0F, 0.0F, 0.0F, -0.5F, -0.2F));
        PartDefinition p_candle = p_halo.addOrReplaceChild("candle", CubeListBuilder.create().texOffs(48, 64).addBox(-1.0F, -3.0F, -1.0F, 2.0F, 6.0F, 2.0F).texOffs(56, 64).addBox(-0.5F, -5.0F, -0.5F, 1.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 17.0F, 0.3F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(DreamMobRenderState state) {
        super.setupAnim(state);
        float age = state.ageInTicks;
        float w = age * (state.awake ? 0.1F : 0.05F);
        this.head.y += Mth.sin(w) * 1.2F;
        this.head.zRot = Mth.sin(w * 0.6F) * 0.05F;
        this.veil.xRot = Mth.sin(w - 0.6F) * 0.08F;
        this.veil2.xRot = Mth.sin(w - 1.2F) * 0.12F;
        this.veil3.xRot = Mth.sin(w - 1.8F) * 0.16F;
        this.veil4.xRot = Mth.sin(w - 2.4F) * 0.2F;
        this.veil2.zRot = Mth.cos(w - 1.2F) * 0.06F;
        this.veil4.zRot = Mth.cos(w - 2.4F) * 0.12F;
        // mãos regendo: sobem e descem fora de fase; acordado, viram para a frente como garras
        this.rightHand.y += Mth.sin(w * 1.3F) * 3.0F;
        this.leftHand.y += Mth.sin(w * 1.3F + Mth.PI) * 3.0F;
        this.rightHand.zRot = Mth.sin(w * 1.3F) * 0.25F;
        this.leftHand.zRot = -Mth.sin(w * 1.3F + Mth.PI) * 0.25F;
        if (state.awake) {
            this.rightHand.xRot = -0.9F;
            this.leftHand.xRot = -0.9F;
        }
        if (state.attackTime > 0.0F) {
            float a = Mth.sin(state.attackTime * Mth.PI);
            this.rightHand.z -= a * 8.0F;
            this.leftHand.z -= a * 8.0F;
        }
        this.halo.yRot = age * (state.awake ? 0.08F : 0.035F);
    }
}

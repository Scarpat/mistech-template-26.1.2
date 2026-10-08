package com.mistech.mistechmod.tarot.magus.client;

import java.util.HashMap;
import java.util.Map;

import com.mistech.mistechmod.Mistech;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;

/**
 * GERADO por {@code tools/tarot/magus/emit_magus.py} — não editar à mão (as animações ficam em {@link MagusModel}).
 * Coordenadas de cada forma: chão em y = 0 local (raiz em y = 24), frente = -Z.
 */
@SuppressWarnings("unused")
public final class MagusGeometry {
    private MagusGeometry() {}

    // ----- as cinco formas (atlas 512x256, textura em 1024x512) -----
    public static final ModelLayerLocation BODY_LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Mistech.MODID, "the_magician"), "main");

    public static LayerDefinition bodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_earth = root.addOrReplaceChild("earth", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));
        PartDefinition p_earth_base = p_earth.addOrReplaceChild("earth_base", CubeListBuilder.create()
                .texOffs(0, 53).addBox(-9.0F, -10.0F, -9.0F, 18.0F, 10.0F, 18.0F)
                .texOffs(72, 126).addBox(-12.0F, -6.0F, -6.0F, 4.0F, 6.0F, 11.0F)
                .texOffs(102, 126).addBox(8.0F, -7.0F, -5.0F, 4.0F, 7.0F, 10.0F)
                .texOffs(458, 158).addBox(-6.0F, -5.0F, -14.0F, 12.0F, 5.0F, 4.0F)
                .texOffs(216, 170).addBox(-5.0F, -4.0F, 10.0F, 9.0F, 4.0F, 4.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p_earth_waist = p_earth_base.addOrReplaceChild("earth_waist", CubeListBuilder.create()
                .texOffs(220, 53).addBox(-8.0F, -12.0F, -7.0F, 16.0F, 12.0F, 14.0F), PartPose.offsetAndRotation(0.0F, -10.0F, 0.0F, 0.12F, 0.0F, 0.0F));
        PartDefinition p_earth_torso = p_earth_waist.addOrReplaceChild("earth_torso", CubeListBuilder.create()
                .texOffs(46, 0).addBox(-12.0F, -20.0F, -8.0F, 24.0F, 20.0F, 16.0F)
                .texOffs(80, 179).addBox(-10.0F, -4.0F, -9.0F, 20.0F, 4.0F, 2.0F), PartPose.offsetAndRotation(0.0F, -11.0F, 1.0F, 0.18F, 0.0F, 0.0F));
        p_earth_torso.addOrReplaceChild("earth_crystal_0", CubeListBuilder.create()
                .texOffs(140, 143).addBox(-1.5F, -11.0F, -1.5F, 3.0F, 11.0F, 3.0F), PartPose.offsetAndRotation(-6.0F, -19.0F, 6.0F, -0.55F, 0.0F, -0.35F));
        p_earth_torso.addOrReplaceChild("earth_crystal_1", CubeListBuilder.create()
                .texOffs(378, 106).addBox(-2.0F, -14.0F, -2.0F, 4.0F, 14.0F, 4.0F), PartPose.offsetAndRotation(5.0F, -19.0F, 5.0F, -0.6F, 0.0F, 0.35F));
        p_earth_torso.addOrReplaceChild("earth_crystal_2", CubeListBuilder.create()
                .texOffs(0, 158).addBox(-1.5F, -9.0F, -1.5F, 3.0F, 9.0F, 3.0F), PartPose.offsetAndRotation(0.0F, -15.0F, 8.0F, -0.95F, 0.0F, 0.0F));
        p_earth_torso.addOrReplaceChild("earth_crystal_3", CubeListBuilder.create()
                .texOffs(200, 170).addBox(-1.0F, -7.0F, -1.0F, 2.0F, 7.0F, 2.0F), PartPose.offsetAndRotation(-10.0F, -13.0F, 5.0F, -0.35F, 0.0F, -0.85F));
        p_earth_torso.addOrReplaceChild("earth_crystal_4", CubeListBuilder.create()
                .texOffs(292, 158).addBox(-1.5F, -8.0F, -1.5F, 3.0F, 8.0F, 3.0F), PartPose.offsetAndRotation(10.0F, -12.0F, 5.0F, -0.4F, 0.0F, 0.9F));
        p_earth_torso.addOrReplaceChild("earth_shoulder_r", CubeListBuilder.create()
                .texOffs(280, 53).addBox(-7.0F, -5.0F, -8.0F, 10.0F, 10.0F, 16.0F), PartPose.offsetAndRotation(-12.0F, -18.0F, 0.0F, 0.0F, 0.0F, 0.12F));
        p_earth_torso.addOrReplaceChild("earth_shoulder_l", CubeListBuilder.create()
                .texOffs(332, 53).addBox(-3.0F, -5.0F, -8.0F, 10.0F, 10.0F, 16.0F), PartPose.offsetAndRotation(12.0F, -18.0F, 0.0F, 0.0F, 0.0F, -0.12F));
        PartDefinition p_earth_head = p_earth_torso.addOrReplaceChild("earth_head", CubeListBuilder.create()
                .texOffs(360, 0).addBox(-9.0F, -15.0F, -12.0F, 18.0F, 15.0F, 15.0F)
                .texOffs(314, 170).addBox(-8.0F, -18.0F, -7.0F, 5.0F, 3.0F, 5.0F)
                .texOffs(236, 179).addBox(3.0F, -17.0F, -9.0F, 4.0F, 2.0F, 4.0F)
                .texOffs(242, 170).addBox(-2.0F, -17.0F, -2.0F, 6.0F, 2.0F, 6.0F), PartPose.offsetAndRotation(0.0F, -15.0F, -7.0F, -0.18F, 0.0F, 0.0F));
        p_earth_head.addOrReplaceChild("earth_jaw", CubeListBuilder.create()
                .texOffs(410, 106).addBox(-8.0F, -1.0F, -9.0F, 16.0F, 5.0F, 12.0F)
                .texOffs(16, 179).addBox(-7.0F, -5.0F, -9.5F, 2.0F, 5.0F, 2.0F)
                .texOffs(24, 179).addBox(5.0F, -5.0F, -9.5F, 2.0F, 5.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.0F, -4.0F, 0.12F, 0.0F, 0.0F));
        PartDefinition p_earth_arm_r = p_earth_torso.addOrReplaceChild("earth_arm_r", CubeListBuilder.create()
                .texOffs(384, 53).addBox(-4.5F, -3.0F, -5.0F, 9.0F, 16.0F, 10.0F), PartPose.offsetAndRotation(-18.0F, -15.0F, 0.0F, -0.2F, 0.0F, 0.12F));
        PartDefinition p_earth_fore_r = p_earth_arm_r.addOrReplaceChild("earth_fore_r", CubeListBuilder.create()
                .texOffs(128, 53).addBox(-5.5F, 0.0F, -6.0F, 11.0F, 15.0F, 12.0F)
                .texOffs(180, 126).addBox(-6.0F, 1.0F, -6.5F, 12.0F, 3.0F, 13.0F), PartPose.offsetAndRotation(0.0F, 12.0F, 0.0F, -0.25F, 0.0F, 0.0F));
        p_earth_fore_r.addOrReplaceChild("earth_fist_r", CubeListBuilder.create()
                .texOffs(158, 81).addBox(-6.5F, 0.0F, -7.0F, 13.0F, 9.0F, 13.0F)
                .texOffs(208, 143).addBox(-5.5F, 9.0F, -6.0F, 11.0F, 2.0F, 11.0F), PartPose.offsetAndRotation(0.0F, 15.0F, 0.0F, 0.35F, 0.0F, 0.0F));
        PartDefinition p_earth_arm_l = p_earth_torso.addOrReplaceChild("earth_arm_l", CubeListBuilder.create()
                .texOffs(422, 53).addBox(-4.5F, -3.0F, -5.0F, 9.0F, 16.0F, 10.0F), PartPose.offsetAndRotation(18.0F, -15.0F, 0.0F, -0.2F, 0.0F, -0.12F));
        PartDefinition p_earth_fore_l = p_earth_arm_l.addOrReplaceChild("earth_fore_l", CubeListBuilder.create()
                .texOffs(174, 53).addBox(-5.5F, 0.0F, -6.0F, 11.0F, 15.0F, 12.0F)
                .texOffs(230, 126).addBox(-6.0F, 1.0F, -6.5F, 12.0F, 3.0F, 13.0F), PartPose.offsetAndRotation(0.0F, 12.0F, 0.0F, -0.25F, 0.0F, 0.0F));
        p_earth_fore_l.addOrReplaceChild("earth_fist_l", CubeListBuilder.create()
                .texOffs(210, 81).addBox(-6.5F, 0.0F, -7.0F, 13.0F, 9.0F, 13.0F)
                .texOffs(252, 143).addBox(-5.5F, 9.0F, -6.0F, 11.0F, 2.0F, 11.0F), PartPose.offsetAndRotation(0.0F, 15.0F, 0.0F, 0.35F, 0.0F, 0.0F));
        PartDefinition p_air = root.addOrReplaceChild("air", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));
        PartDefinition p_air_body = p_air.addOrReplaceChild("air_body", CubeListBuilder.create()
                .texOffs(172, 106).addBox(-5.0F, -14.0F, -3.0F, 10.0F, 14.0F, 6.0F)
                .texOffs(266, 170).addBox(-3.5F, 0.0F, -2.5F, 7.0F, 3.0F, 5.0F)
                .texOffs(332, 158).addBox(-6.0F, -15.0F, -3.5F, 12.0F, 3.0F, 7.0F), PartPose.offset(0.0F, -32.0F, 0.0F));
        PartDefinition p_air_neck = p_air_body.addOrReplaceChild("air_neck", CubeListBuilder.create()
                .texOffs(164, 158).addBox(-2.0F, -7.0F, -2.0F, 4.0F, 7.0F, 4.0F), PartPose.offsetAndRotation(0.0F, -14.0F, -1.0F, 0.65F, 0.0F, 0.0F));
        PartDefinition p_air_head = p_air_neck.addOrReplaceChild("air_head", CubeListBuilder.create()
                .texOffs(466, 106).addBox(-4.5F, -8.0F, -5.5F, 9.0F, 8.0F, 9.0F), PartPose.offsetAndRotation(0.0F, -6.0F, 0.0F, -0.75F, 0.0F, 0.0F));
        p_air_head.addOrReplaceChild("air_beak", CubeListBuilder.create()
                .texOffs(280, 126).addBox(-1.5F, -2.0F, -12.0F, 3.0F, 4.0F, 12.0F), PartPose.offsetAndRotation(0.0F, -2.5F, -5.5F, 0.12F, 0.0F, 0.0F));
        p_air_head.addOrReplaceChild("air_beak_low", CubeListBuilder.create()
                .texOffs(370, 158).addBox(-1.0F, 0.0F, -8.0F, 2.0F, 1.5F, 8.0F), PartPose.offsetAndRotation(0.0F, -0.5F, -5.5F, 0.35F, 0.0F, 0.0F));
        p_air_head.addOrReplaceChild("air_crest_0", CubeListBuilder.create()
                .texOffs(442, 158).addBox(-0.5F, -7.5F, -1.0F, 1.0F, 7.5F, 2.0F), PartPose.offsetAndRotation(-2.5F, -6.0F, 2.0F, -1.25F, 0.0F, -0.25F));
        p_air_head.addOrReplaceChild("air_crest_1", CubeListBuilder.create()
                .texOffs(24, 158).addBox(-0.5F, -10.0F, -1.0F, 1.0F, 10.0F, 2.0F), PartPose.offsetAndRotation(0.0F, -6.0F, 2.0F, -1.0F, 0.0F, 0.0F));
        p_air_head.addOrReplaceChild("air_crest_2", CubeListBuilder.create()
                .texOffs(448, 158).addBox(-0.5F, -7.5F, -1.0F, 1.0F, 7.5F, 2.0F), PartPose.offsetAndRotation(2.5F, -6.0F, 2.0F, -1.25F, 0.0F, 0.25F));
        p_air_body.addOrReplaceChild("air_mantle_r", CubeListBuilder.create()
                .texOffs(304, 158).addBox(-3.0F, -1.0F, -3.0F, 3.0F, 9.0F, 1.5F)
                .texOffs(422, 158).addBox(-4.5F, -1.0F, -1.0F, 3.0F, 8.0F, 1.5F)
                .texOffs(180, 170).addBox(-6.0F, -1.0F, 1.0F, 3.0F, 7.0F, 1.5F), PartPose.offsetAndRotation(-5.0F, -14.0F, 0.0F, 0.25F, 0.0F, 0.5F));
        PartDefinition p_air_wing_r = p_air_body.addOrReplaceChild("air_wing_r", CubeListBuilder.create()
                .texOffs(124, 179).addBox(-16.0F, -1.5F, -1.5F, 16.0F, 3.0F, 3.0F), PartPose.offsetAndRotation(-5.0F, -12.0F, 1.0F, 0.0F, 0.35F, 0.55F));
        PartDefinition p_air_wingtip_r = p_air_wing_r.addOrReplaceChild("air_wingtip_r", CubeListBuilder.create()
                .texOffs(348, 179).addBox(-13.0F, -1.0F, -1.0F, 13.0F, 2.0F, 2.0F), PartPose.offsetAndRotation(-16.0F, 0.0F, 0.0F, 0.0F, -0.2F, -0.9F));
        p_air_wing_r.addOrReplaceChild("air_feather_r0", CubeListBuilder.create()
                .texOffs(346, 126).addBox(-1.25F, 0.0F, -0.5F, 2.5F, 15.0F, 1.0F), PartPose.offsetAndRotation(-3.0F, 1.0F, 0.0F, 0.35F, 0.0F, -0.5F));
        p_air_wing_r.addOrReplaceChild("air_feather_r1", CubeListBuilder.create()
                .texOffs(394, 106).addBox(-1.25F, 0.0F, -0.5F, 2.5F, 17.0F, 1.0F), PartPose.offsetAndRotation(-7.0F, 1.0F, 0.0F, 0.35F, 0.0F, -0.38F));
        p_air_wing_r.addOrReplaceChild("air_feather_r2", CubeListBuilder.create()
                .texOffs(264, 106).addBox(-1.25F, 0.0F, -0.5F, 2.5F, 19.0F, 1.0F), PartPose.offsetAndRotation(-11.0F, 1.0F, 0.0F, 0.35F, 0.0F, -0.26F));
        p_air_wing_r.addOrReplaceChild("air_feather_r3", CubeListBuilder.create()
                .texOffs(276, 81).addBox(-1.25F, 0.0F, -0.5F, 2.5F, 21.0F, 1.0F), PartPose.offsetAndRotation(-15.0F, 1.0F, 0.0F, 0.35F, 0.0F, -0.14F));
        p_air_wingtip_r.addOrReplaceChild("air_feather_r4", CubeListBuilder.create()
                .texOffs(64, 81).addBox(-1.25F, 0.0F, -0.5F, 2.5F, 22.0F, 1.0F), PartPose.offsetAndRotation(-3.0F, 0.5F, 0.0F, 0.35F, 0.0F, 0.2F));
        p_air_wingtip_r.addOrReplaceChild("air_feather_r5", CubeListBuilder.create()
                .texOffs(436, 81).addBox(-1.25F, 0.0F, -0.5F, 2.5F, 20.0F, 1.0F), PartPose.offsetAndRotation(-7.0F, 0.5F, 0.0F, 0.35F, 0.0F, 0.5F));
        p_air_wingtip_r.addOrReplaceChild("air_feather_r6", CubeListBuilder.create()
                .texOffs(310, 106).addBox(-1.25F, 0.0F, -0.5F, 2.5F, 18.0F, 1.0F), PartPose.offsetAndRotation(-11.0F, 0.5F, 0.0F, 0.35F, 0.0F, 0.8F));
        PartDefinition p_air_arm_r = p_air_body.addOrReplaceChild("air_arm_r", CubeListBuilder.create()
                .texOffs(386, 143).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 10.0F, 3.0F), PartPose.offsetAndRotation(-5.0F, -10.0F, -1.0F, -0.35F, 0.0F, 0.25F));
        PartDefinition p_air_fore_r = p_air_arm_r.addOrReplaceChild("air_fore_r", CubeListBuilder.create()
                .texOffs(422, 143).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 11.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 10.0F, 0.0F, -0.7F, 0.0F, 0.0F));
        p_air_fore_r.addOrReplaceChild("air_claws_r", CubeListBuilder.create()
                .texOffs(56, 179).addBox(-1.5F, 0.0F, -1.2F, 1.0F, 6.0F, 1.0F)
                .texOffs(60, 179).addBox(0.5F, 0.0F, -1.2F, 1.0F, 6.0F, 1.0F)
                .texOffs(316, 179).addBox(-0.5F, 0.0F, 0.5F, 1.0F, 5.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 11.0F, 0.0F, 0.3F, 0.0F, 0.0F));
        p_air_body.addOrReplaceChild("air_mantle_l", CubeListBuilder.create()
                .texOffs(314, 158).addBox(-1.0F, -1.0F, -3.0F, 3.0F, 9.0F, 1.5F)
                .texOffs(432, 158).addBox(0.5F, -1.0F, -1.0F, 3.0F, 8.0F, 1.5F)
                .texOffs(190, 170).addBox(2.0F, -1.0F, 1.0F, 3.0F, 7.0F, 1.5F), PartPose.offsetAndRotation(5.0F, -14.0F, 0.0F, 0.25F, 0.0F, -0.5F));
        PartDefinition p_air_wing_l = p_air_body.addOrReplaceChild("air_wing_l", CubeListBuilder.create()
                .texOffs(162, 179).addBox(0.0F, -1.5F, -1.5F, 16.0F, 3.0F, 3.0F), PartPose.offsetAndRotation(5.0F, -12.0F, 1.0F, 0.0F, -0.35F, -0.55F));
        PartDefinition p_air_wingtip_l = p_air_wing_l.addOrReplaceChild("air_wingtip_l", CubeListBuilder.create()
                .texOffs(378, 179).addBox(0.0F, -1.0F, -1.0F, 13.0F, 2.0F, 2.0F), PartPose.offsetAndRotation(16.0F, 0.0F, 0.0F, 0.0F, 0.2F, 0.9F));
        p_air_wing_l.addOrReplaceChild("air_feather_l0", CubeListBuilder.create()
                .texOffs(354, 126).addBox(-1.25F, 0.0F, -0.5F, 2.5F, 15.0F, 1.0F), PartPose.offsetAndRotation(3.0F, 1.0F, 0.0F, 0.35F, 0.0F, 0.5F));
        p_air_wing_l.addOrReplaceChild("air_feather_l1", CubeListBuilder.create()
                .texOffs(402, 106).addBox(-1.25F, 0.0F, -0.5F, 2.5F, 17.0F, 1.0F), PartPose.offsetAndRotation(7.0F, 1.0F, 0.0F, 0.35F, 0.0F, 0.38F));
        p_air_wing_l.addOrReplaceChild("air_feather_l2", CubeListBuilder.create()
                .texOffs(272, 106).addBox(-1.25F, 0.0F, -0.5F, 2.5F, 19.0F, 1.0F), PartPose.offsetAndRotation(11.0F, 1.0F, 0.0F, 0.35F, 0.0F, 0.26F));
        p_air_wing_l.addOrReplaceChild("air_feather_l3", CubeListBuilder.create()
                .texOffs(284, 81).addBox(-1.25F, 0.0F, -0.5F, 2.5F, 21.0F, 1.0F), PartPose.offsetAndRotation(15.0F, 1.0F, 0.0F, 0.35F, 0.0F, 0.14F));
        p_air_wingtip_l.addOrReplaceChild("air_feather_l4", CubeListBuilder.create()
                .texOffs(72, 81).addBox(-1.25F, 0.0F, -0.5F, 2.5F, 22.0F, 1.0F), PartPose.offsetAndRotation(3.0F, 0.5F, 0.0F, 0.35F, 0.0F, -0.2F));
        p_air_wingtip_l.addOrReplaceChild("air_feather_l5", CubeListBuilder.create()
                .texOffs(444, 81).addBox(-1.25F, 0.0F, -0.5F, 2.5F, 20.0F, 1.0F), PartPose.offsetAndRotation(7.0F, 0.5F, 0.0F, 0.35F, 0.0F, -0.5F));
        p_air_wingtip_l.addOrReplaceChild("air_feather_l6", CubeListBuilder.create()
                .texOffs(318, 106).addBox(-1.25F, 0.0F, -0.5F, 2.5F, 18.0F, 1.0F), PartPose.offsetAndRotation(11.0F, 0.5F, 0.0F, 0.35F, 0.0F, -0.8F));
        PartDefinition p_air_arm_l = p_air_body.addOrReplaceChild("air_arm_l", CubeListBuilder.create()
                .texOffs(398, 143).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 10.0F, 3.0F), PartPose.offsetAndRotation(5.0F, -10.0F, -1.0F, -0.35F, 0.0F, -0.25F));
        PartDefinition p_air_fore_l = p_air_arm_l.addOrReplaceChild("air_fore_l", CubeListBuilder.create()
                .texOffs(430, 143).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 11.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 10.0F, 0.0F, -0.7F, 0.0F, 0.0F));
        p_air_fore_l.addOrReplaceChild("air_claws_l", CubeListBuilder.create()
                .texOffs(64, 179).addBox(-1.5F, 0.0F, -1.2F, 1.0F, 6.0F, 1.0F)
                .texOffs(68, 179).addBox(0.5F, 0.0F, -1.2F, 1.0F, 6.0F, 1.0F)
                .texOffs(320, 179).addBox(-0.5F, 0.0F, 0.5F, 1.0F, 5.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 11.0F, 0.0F, 0.3F, 0.0F, 0.0F));
        PartDefinition p_air_tail1 = p_air_body.addOrReplaceChild("air_tail1", CubeListBuilder.create()
                .texOffs(486, 143).addBox(-3.0F, 0.0F, -2.5F, 6.0F, 7.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 3.0F, 0.0F, 0.2F, 0.0F, 0.0F));
        PartDefinition p_air_tail2 = p_air_tail1.addOrReplaceChild("air_tail2", CubeListBuilder.create()
                .texOffs(180, 158).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 7.0F, 4.0F), PartPose.offsetAndRotation(0.0F, 7.0F, 0.0F, 0.25F, 0.0F, 0.0F));
        PartDefinition p_air_tail3 = p_air_tail2.addOrReplaceChild("air_tail3", CubeListBuilder.create()
                .texOffs(410, 158).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 7.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 7.0F, 0.0F, 0.3F, 0.0F, 0.0F));
        p_air_tail3.addOrReplaceChild("air_tail4", CubeListBuilder.create()
                .texOffs(94, 143).addBox(-0.5F, 0.0F, -3.0F, 1.0F, 9.0F, 6.0F), PartPose.offsetAndRotation(0.0F, 7.0F, 0.0F, 0.3F, 0.0F, 0.0F));
        PartDefinition p_water = root.addOrReplaceChild("water", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));
        PartDefinition p_water_body = p_water.addOrReplaceChild("water_body", CubeListBuilder.create()
                .texOffs(126, 0).addBox(-12.0F, -12.0F, -10.0F, 24.0F, 13.0F, 22.0F)
                .texOffs(80, 81).addBox(-10.5F, 1.0F, -9.0F, 21.0F, 4.0F, 18.0F)
                .texOffs(438, 143).addBox(-11.0F, -10.0F, -11.0F, 22.0F, 10.0F, 2.0F), PartPose.offset(0.0F, -17.0F, 0.0F));
        p_water_body.addOrReplaceChild("water_spike_0", CubeListBuilder.create()
                .texOffs(84, 170).addBox(-1.5F, -6.0F, -1.5F, 3.0F, 6.0F, 3.0F), PartPose.offsetAndRotation(-7.0F, -11.0F, 8.0F, 0.5F, 0.0F, 0.35F));
        p_water_body.addOrReplaceChild("water_spike_1", CubeListBuilder.create()
                .texOffs(96, 170).addBox(-1.5F, -6.0F, -1.5F, 3.0F, 6.0F, 3.0F), PartPose.offsetAndRotation(7.0F, -11.0F, 8.0F, 0.5F, 0.0F, -0.35F));
        p_water_body.addOrReplaceChild("water_spike_2", CubeListBuilder.create()
                .texOffs(418, 170).addBox(-1.5F, -5.0F, -1.5F, 3.0F, 5.0F, 3.0F), PartPose.offsetAndRotation(0.0F, -11.0F, 10.0F, 0.8F, 0.0F, 0.0F));
        p_water_body.addOrReplaceChild("water_spike_3", CubeListBuilder.create()
                .texOffs(430, 170).addBox(-1.5F, -5.0F, -1.5F, 3.0F, 5.0F, 3.0F), PartPose.offsetAndRotation(-9.0F, -11.0F, -2.0F, 0.1F, 0.0F, 0.35F));
        p_water_body.addOrReplaceChild("water_spike_4", CubeListBuilder.create()
                .texOffs(442, 170).addBox(-1.5F, -5.0F, -1.5F, 3.0F, 5.0F, 3.0F), PartPose.offsetAndRotation(9.0F, -11.0F, -2.0F, 0.1F, 0.0F, -0.35F));
        PartDefinition p_water_cup = p_water_body.addOrReplaceChild("water_cup", CubeListBuilder.create()
                .texOffs(28, 170).addBox(-3.0F, -3.0F, -3.0F, 6.0F, 3.0F, 6.0F), PartPose.offset(0.0F, -12.0F, 3.0F));
        p_water_cup.addOrReplaceChild("water_bowl", CubeListBuilder.create()
                .texOffs(70, 106).addBox(-8.0F, -6.0F, -7.0F, 16.0F, 6.0F, 14.0F)
                .texOffs(136, 187).addBox(-9.0F, -7.0F, -8.0F, 18.0F, 1.0F, 1.0F)
                .texOffs(174, 187).addBox(-9.0F, -7.0F, 7.0F, 18.0F, 1.0F, 1.0F)
                .texOffs(476, 126).addBox(-9.0F, -7.0F, -7.0F, 1.0F, 1.0F, 14.0F)
                .texOffs(0, 143).addBox(8.0F, -7.0F, -7.0F, 1.0F, 1.0F, 14.0F), PartPose.offset(0.0F, -3.0F, 0.0F));
        PartDefinition p_water_head = p_water_body.addOrReplaceChild("water_head", CubeListBuilder.create()
                .texOffs(326, 106).addBox(-8.0F, -6.0F, -9.0F, 16.0F, 8.0F, 10.0F), PartPose.offset(0.0F, -5.0F, -10.0F));
        p_water_head.addOrReplaceChild("water_jaw", CubeListBuilder.create()
                .texOffs(422, 126).addBox(-8.5F, 0.0F, -9.5F, 17.0F, 5.0F, 10.0F), PartPose.offsetAndRotation(0.0F, 2.0F, -1.0F, 0.35F, 0.0F, 0.0F));
        PartDefinition p_water_lure = p_water_head.addOrReplaceChild("water_lure", CubeListBuilder.create()
                .texOffs(454, 158).addBox(-0.5F, -9.0F, -0.5F, 1.0F, 9.0F, 1.0F), PartPose.offsetAndRotation(0.0F, -6.0F, -4.0F, -0.25F, 0.0F, 0.0F));
        PartDefinition p_water_lure2 = p_water_lure.addOrReplaceChild("water_lure2", CubeListBuilder.create()
                .texOffs(0, 179).addBox(-0.5F, -7.0F, -0.5F, 1.0F, 7.0F, 1.0F), PartPose.offsetAndRotation(0.0F, -9.0F, 0.0F, -1.3F, 0.0F, 0.0F));
        p_water_lure2.addOrReplaceChild("water_lure_tip", CubeListBuilder.create()
                .texOffs(268, 179).addBox(-1.5F, -3.0F, -1.5F, 3.0F, 3.0F, 3.0F), PartPose.offset(0.0F, -7.0F, 0.0F));
        PartDefinition p_water_arm_r = p_water_body.addOrReplaceChild("water_arm_r", CubeListBuilder.create()
                .texOffs(310, 126).addBox(-2.0F, -1.0F, -2.5F, 4.0F, 11.0F, 5.0F), PartPose.offsetAndRotation(-12.0F, -6.0F, -4.0F, -0.2F, 0.0F, 0.3F));
        PartDefinition p_water_fore_r = p_water_arm_r.addOrReplaceChild("water_fore_r", CubeListBuilder.create()
                .texOffs(108, 143).addBox(-1.75F, 0.0F, -2.0F, 3.5F, 10.0F, 4.0F), PartPose.offsetAndRotation(0.0F, 10.0F, 0.0F, -0.7F, 0.0F, -0.2F));
        p_water_fore_r.addOrReplaceChild("water_hand_r", CubeListBuilder.create()
                .texOffs(390, 170).addBox(-3.0F, 0.0F, -0.5F, 6.0F, 7.0F, 1.0F)
                .texOffs(112, 187).addBox(-3.0F, 6.0F, -1.0F, 1.0F, 3.0F, 1.0F)
                .texOffs(116, 187).addBox(-0.5F, 6.5F, -1.0F, 1.0F, 3.0F, 1.0F)
                .texOffs(120, 187).addBox(2.0F, 6.0F, -1.0F, 1.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 10.0F, 0.0F, 0.2F, 0.0F, 0.0F));
        PartDefinition p_water_arm_l = p_water_body.addOrReplaceChild("water_arm_l", CubeListBuilder.create()
                .texOffs(328, 126).addBox(-2.0F, -1.0F, -2.5F, 4.0F, 11.0F, 5.0F), PartPose.offsetAndRotation(12.0F, -6.0F, -4.0F, -0.2F, 0.0F, -0.3F));
        PartDefinition p_water_fore_l = p_water_arm_l.addOrReplaceChild("water_fore_l", CubeListBuilder.create()
                .texOffs(124, 143).addBox(-1.75F, 0.0F, -2.0F, 3.5F, 10.0F, 4.0F), PartPose.offsetAndRotation(0.0F, 10.0F, 0.0F, -0.7F, 0.0F, 0.2F));
        p_water_fore_l.addOrReplaceChild("water_hand_l", CubeListBuilder.create()
                .texOffs(404, 170).addBox(-3.0F, 0.0F, -0.5F, 6.0F, 7.0F, 1.0F)
                .texOffs(124, 187).addBox(-3.0F, 6.0F, -1.0F, 1.0F, 3.0F, 1.0F)
                .texOffs(128, 187).addBox(-0.5F, 6.5F, -1.0F, 1.0F, 3.0F, 1.0F)
                .texOffs(132, 187).addBox(2.0F, 6.0F, -1.0F, 1.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 10.0F, 0.0F, 0.2F, 0.0F, 0.0F));
        PartDefinition p_water_tent0_1 = p_water_body.addOrReplaceChild("water_tent0_1", CubeListBuilder.create()
                .texOffs(196, 158).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 7.0F, 4.0F), PartPose.offsetAndRotation(6.0622F, 4.0F, 3.25F, 0.275F, 0.0F, -0.4763F));
        PartDefinition p_water_tent0_2 = p_water_tent0_1.addOrReplaceChild("water_tent0_2", CubeListBuilder.create()
                .texOffs(108, 170).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 7.0F, 0.0F, -0.175F, 0.0F, 0.3031F));
        p_water_tent0_2.addOrReplaceChild("water_tent0_3", CubeListBuilder.create()
                .texOffs(454, 170).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 6.0F, 0.0F, -0.225F, 0.0F, 0.3897F));
        PartDefinition p_water_tent1_1 = p_water_body.addOrReplaceChild("water_tent1_1", CubeListBuilder.create()
                .texOffs(212, 158).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 7.0F, 4.0F), PartPose.offsetAndRotation(0.0F, 4.0F, 6.5F, 0.55F, 0.0F, -0.0F));
        PartDefinition p_water_tent1_2 = p_water_tent1_1.addOrReplaceChild("water_tent1_2", CubeListBuilder.create()
                .texOffs(120, 170).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 7.0F, 0.0F, -0.35F, 0.0F, 0.0F));
        p_water_tent1_2.addOrReplaceChild("water_tent1_3", CubeListBuilder.create()
                .texOffs(462, 170).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 6.0F, 0.0F, -0.45F, 0.0F, 0.0F));
        PartDefinition p_water_tent2_1 = p_water_body.addOrReplaceChild("water_tent2_1", CubeListBuilder.create()
                .texOffs(228, 158).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 7.0F, 4.0F), PartPose.offsetAndRotation(-6.0622F, 4.0F, 3.25F, 0.275F, 0.0F, 0.4763F));
        PartDefinition p_water_tent2_2 = p_water_tent2_1.addOrReplaceChild("water_tent2_2", CubeListBuilder.create()
                .texOffs(132, 170).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 7.0F, 0.0F, -0.175F, 0.0F, -0.3031F));
        p_water_tent2_2.addOrReplaceChild("water_tent2_3", CubeListBuilder.create()
                .texOffs(470, 170).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 6.0F, 0.0F, -0.225F, 0.0F, -0.3897F));
        PartDefinition p_water_tent3_1 = p_water_body.addOrReplaceChild("water_tent3_1", CubeListBuilder.create()
                .texOffs(244, 158).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 7.0F, 4.0F), PartPose.offsetAndRotation(-6.0622F, 4.0F, -3.25F, -0.275F, 0.0F, 0.4763F));
        PartDefinition p_water_tent3_2 = p_water_tent3_1.addOrReplaceChild("water_tent3_2", CubeListBuilder.create()
                .texOffs(144, 170).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 7.0F, 0.0F, 0.175F, 0.0F, -0.3031F));
        p_water_tent3_2.addOrReplaceChild("water_tent3_3", CubeListBuilder.create()
                .texOffs(478, 170).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 6.0F, 0.0F, 0.225F, 0.0F, -0.3897F));
        PartDefinition p_water_tent4_1 = p_water_body.addOrReplaceChild("water_tent4_1", CubeListBuilder.create()
                .texOffs(260, 158).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 7.0F, 4.0F), PartPose.offsetAndRotation(-0.0F, 4.0F, -6.5F, -0.55F, 0.0F, 0.0F));
        PartDefinition p_water_tent4_2 = p_water_tent4_1.addOrReplaceChild("water_tent4_2", CubeListBuilder.create()
                .texOffs(156, 170).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 7.0F, 0.0F, 0.35F, 0.0F, -0.0F));
        p_water_tent4_2.addOrReplaceChild("water_tent4_3", CubeListBuilder.create()
                .texOffs(486, 170).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 6.0F, 0.0F, 0.45F, 0.0F, -0.0F));
        PartDefinition p_water_tent5_1 = p_water_body.addOrReplaceChild("water_tent5_1", CubeListBuilder.create()
                .texOffs(276, 158).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 7.0F, 4.0F), PartPose.offsetAndRotation(6.0622F, 4.0F, -3.25F, -0.275F, 0.0F, -0.4763F));
        PartDefinition p_water_tent5_2 = p_water_tent5_1.addOrReplaceChild("water_tent5_2", CubeListBuilder.create()
                .texOffs(168, 170).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 7.0F, 0.0F, 0.175F, 0.0F, 0.3031F));
        p_water_tent5_2.addOrReplaceChild("water_tent5_3", CubeListBuilder.create()
                .texOffs(494, 170).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 6.0F, 0.0F, 0.225F, 0.0F, 0.3897F));
        PartDefinition p_fire = root.addOrReplaceChild("fire", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));
        PartDefinition p_fire_hips = p_fire.addOrReplaceChild("fire_hips", CubeListBuilder.create()
                .texOffs(296, 143).addBox(-6.0F, -3.0F, -4.0F, 12.0F, 5.0F, 8.0F), PartPose.offset(0.0F, -23.0F, 0.0F));
        PartDefinition p_fire_thigh_r = p_fire_hips.addOrReplaceChild("fire_thigh_r", CubeListBuilder.create()
                .texOffs(130, 126).addBox(-2.5F, 0.0F, -3.0F, 5.0F, 11.0F, 6.0F), PartPose.offsetAndRotation(-4.5F, 1.0F, 0.0F, -0.6F, 0.0F, 0.08F));
        PartDefinition p_fire_shin_r = p_fire_thigh_r.addOrReplaceChild("fire_shin_r", CubeListBuilder.create()
                .texOffs(30, 143).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 11.0F, 4.0F), PartPose.offsetAndRotation(0.0F, 10.0F, -1.0F, 1.25F, 0.0F, 0.0F));
        p_fire_shin_r.addOrReplaceChild("fire_foot_r", CubeListBuilder.create()
                .texOffs(70, 158).addBox(-2.5F, 0.0F, -6.0F, 5.0F, 3.0F, 8.0F)
                .texOffs(88, 187).addBox(-2.5F, 1.0F, -8.0F, 1.0F, 2.0F, 2.0F)
                .texOffs(328, 179).addBox(-0.5F, 1.0F, -8.5F, 1.0F, 2.0F, 2.5F)
                .texOffs(94, 187).addBox(1.5F, 1.0F, -8.0F, 1.0F, 2.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 11.0F, 0.0F, -0.62F, 0.0F, 0.0F));
        PartDefinition p_fire_thigh_l = p_fire_hips.addOrReplaceChild("fire_thigh_l", CubeListBuilder.create()
                .texOffs(152, 126).addBox(-2.5F, 0.0F, -3.0F, 5.0F, 11.0F, 6.0F), PartPose.offsetAndRotation(4.5F, 1.0F, 0.0F, -0.6F, 0.0F, -0.08F));
        PartDefinition p_fire_shin_l = p_fire_thigh_l.addOrReplaceChild("fire_shin_l", CubeListBuilder.create()
                .texOffs(46, 143).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 11.0F, 4.0F), PartPose.offsetAndRotation(0.0F, 10.0F, -1.0F, 1.25F, 0.0F, 0.0F));
        p_fire_shin_l.addOrReplaceChild("fire_foot_l", CubeListBuilder.create()
                .texOffs(96, 158).addBox(-2.5F, 0.0F, -6.0F, 5.0F, 3.0F, 8.0F)
                .texOffs(100, 187).addBox(-2.5F, 1.0F, -8.0F, 1.0F, 2.0F, 2.0F)
                .texOffs(336, 179).addBox(-0.5F, 1.0F, -8.5F, 1.0F, 2.0F, 2.5F)
                .texOffs(106, 187).addBox(1.5F, 1.0F, -8.0F, 1.0F, 2.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 11.0F, 0.0F, -0.62F, 0.0F, 0.0F));
        PartDefinition p_fire_torso = p_fire_hips.addOrReplaceChild("fire_torso", CubeListBuilder.create()
                .texOffs(144, 158).addBox(-3.0F, -7.0F, -2.0F, 6.0F, 7.0F, 4.0F), PartPose.offsetAndRotation(0.0F, -3.0F, 0.0F, 0.28F, 0.0F, 0.0F));
        PartDefinition p_fire_ribs = p_fire_torso.addOrReplaceChild("fire_ribs", CubeListBuilder.create()
                .texOffs(292, 81).addBox(-7.0F, -12.0F, -5.0F, 14.0F, 12.0F, 9.0F)
                .texOffs(152, 143).addBox(-9.5F, -14.0F, -4.5F, 19.0F, 4.0F, 9.0F), PartPose.offset(0.0F, -7.0F, 0.0F));
        p_fire_ribs.addOrReplaceChild("fire_spike_0", CubeListBuilder.create()
                .texOffs(32, 179).addBox(-1.0F, -5.0F, -1.0F, 2.0F, 5.0F, 2.0F), PartPose.offsetAndRotation(0.0F, -12.0F, 2.0F, -0.6F, 0.0F, 0.0F));
        p_fire_ribs.addOrReplaceChild("fire_spike_1", CubeListBuilder.create()
                .texOffs(502, 170).addBox(-1.0F, -6.0F, -1.0F, 2.0F, 6.0F, 2.0F), PartPose.offsetAndRotation(0.0F, -8.0F, 3.0F, -0.7F, 0.0F, 0.0F));
        p_fire_ribs.addOrReplaceChild("fire_spike_2", CubeListBuilder.create()
                .texOffs(40, 179).addBox(-1.0F, -5.0F, -1.0F, 2.0F, 5.0F, 2.0F), PartPose.offsetAndRotation(0.0F, -4.0F, 3.5F, -0.9F, 0.0F, 0.0F));
        PartDefinition p_fire_head = p_fire_ribs.addOrReplaceChild("fire_head", CubeListBuilder.create()
                .texOffs(130, 106).addBox(-5.5F, -10.0F, -6.5F, 11.0F, 10.0F, 10.0F), PartPose.offsetAndRotation(0.0F, -13.0F, -2.0F, -0.25F, 0.0F, 0.0F));
        p_fire_head.addOrReplaceChild("fire_jaw", CubeListBuilder.create()
                .texOffs(0, 170).addBox(-4.0F, 0.0F, -5.0F, 8.0F, 3.0F, 6.0F), PartPose.offsetAndRotation(0.0F, -1.0F, -1.0F, 0.2F, 0.0F, 0.0F));
        PartDefinition p_fire_horn_r1 = p_fire_head.addOrReplaceChild("fire_horn_r1", CubeListBuilder.create()
                .texOffs(334, 170).addBox(-6.0F, -2.0F, -2.0F, 6.0F, 4.0F, 4.0F), PartPose.offsetAndRotation(-5.0F, -8.0F, 0.0F, 0.2F, 0.0F, -0.25F));
        PartDefinition p_fire_horn_r2 = p_fire_horn_r1.addOrReplaceChild("fire_horn_r2", CubeListBuilder.create()
                .texOffs(200, 179).addBox(-6.0F, -1.5F, -1.5F, 6.0F, 3.0F, 3.0F), PartPose.offsetAndRotation(-5.5F, 0.0F, 0.0F, 0.25F, 0.0F, -0.85F));
        PartDefinition p_fire_horn_r3 = p_fire_horn_r2.addOrReplaceChild("fire_horn_r3", CubeListBuilder.create()
                .texOffs(428, 179).addBox(-5.0F, -1.0F, -1.0F, 5.0F, 2.0F, 2.0F), PartPose.offsetAndRotation(-5.5F, 0.0F, 0.0F, 0.3F, 0.0F, -0.7F));
        p_fire_horn_r3.addOrReplaceChild("fire_horn_r4", CubeListBuilder.create()
                .texOffs(212, 187).addBox(-3.0F, -0.5F, -0.5F, 3.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(-4.5F, 0.0F, 0.0F, 0.2F, 0.0F, -0.5F));
        PartDefinition p_fire_horn_l1 = p_fire_head.addOrReplaceChild("fire_horn_l1", CubeListBuilder.create()
                .texOffs(354, 170).addBox(0.0F, -2.0F, -2.0F, 6.0F, 4.0F, 4.0F), PartPose.offsetAndRotation(5.0F, -8.0F, 0.0F, 0.2F, 0.0F, 0.25F));
        PartDefinition p_fire_horn_l2 = p_fire_horn_l1.addOrReplaceChild("fire_horn_l2", CubeListBuilder.create()
                .texOffs(218, 179).addBox(0.0F, -1.5F, -1.5F, 6.0F, 3.0F, 3.0F), PartPose.offsetAndRotation(5.5F, 0.0F, 0.0F, 0.25F, 0.0F, 0.85F));
        PartDefinition p_fire_horn_l3 = p_fire_horn_l2.addOrReplaceChild("fire_horn_l3", CubeListBuilder.create()
                .texOffs(442, 179).addBox(0.0F, -1.0F, -1.0F, 5.0F, 2.0F, 2.0F), PartPose.offsetAndRotation(5.5F, 0.0F, 0.0F, 0.3F, 0.0F, 0.7F));
        p_fire_horn_l3.addOrReplaceChild("fire_horn_l4", CubeListBuilder.create()
                .texOffs(220, 187).addBox(0.0F, -0.5F, -0.5F, 3.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(4.5F, 0.0F, 0.0F, 0.2F, 0.0F, 0.5F));
        p_fire_head.addOrReplaceChild("fire_crown", CubeListBuilder.create()
                .texOffs(292, 179).addBox(-4.0F, -4.0F, -1.0F, 2.0F, 4.0F, 2.0F)
                .texOffs(208, 170).addBox(-1.0F, -7.0F, -1.0F, 2.0F, 7.0F, 2.0F)
                .texOffs(300, 179).addBox(2.0F, -4.0F, -1.0F, 2.0F, 4.0F, 2.0F), PartPose.offset(0.0F, -10.0F, -1.0F));
        PartDefinition p_fire_arm_r = p_fire_ribs.addOrReplaceChild("fire_arm_r", CubeListBuilder.create()
                .texOffs(62, 143).addBox(-3.0F, -1.0F, -2.0F, 4.0F, 11.0F, 4.0F), PartPose.offsetAndRotation(-9.0F, -11.0F, 0.0F, -0.3F, 0.0F, 0.25F));
        PartDefinition p_fire_fore_r = p_fire_arm_r.addOrReplaceChild("fire_fore_r", CubeListBuilder.create()
                .texOffs(424, 81).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 18.0F, 3.0F)
                .texOffs(252, 179).addBox(-2.0F, 2.0F, -2.0F, 4.0F, 2.0F, 4.0F), PartPose.offsetAndRotation(-1.0F, 10.0F, 0.0F, -0.8F, 0.0F, 0.0F));
        p_fire_fore_r.addOrReplaceChild("fire_staff_tip", CubeListBuilder.create()
                .texOffs(390, 158).addBox(-2.5F, 0.0F, -2.5F, 5.0F, 5.0F, 5.0F), PartPose.offset(0.0F, 18.0F, 0.0F));
        PartDefinition p_fire_arm_l = p_fire_ribs.addOrReplaceChild("fire_arm_l", CubeListBuilder.create()
                .texOffs(78, 143).addBox(-1.0F, -1.0F, -2.0F, 4.0F, 11.0F, 4.0F), PartPose.offsetAndRotation(9.0F, -11.0F, 0.0F, -0.3F, 0.0F, -0.25F));
        PartDefinition p_fire_fore_l = p_fire_arm_l.addOrReplaceChild("fire_fore_l", CubeListBuilder.create()
                .texOffs(410, 143).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 10.0F, 3.0F), PartPose.offsetAndRotation(1.0F, 10.0F, 0.0F, -0.6F, 0.0F, 0.0F));
        p_fire_fore_l.addOrReplaceChild("fire_claw_l", CubeListBuilder.create()
                .texOffs(72, 179).addBox(-1.5F, 0.0F, -1.5F, 1.0F, 6.0F, 1.0F)
                .texOffs(76, 179).addBox(0.5F, 0.0F, -1.5F, 1.0F, 6.0F, 1.0F)
                .texOffs(324, 179).addBox(-0.5F, 0.0F, 0.5F, 1.0F, 5.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 10.0F, 0.0F, 0.3F, 0.0F, 0.0F));
        PartDefinition p_fire_tail1 = p_fire_hips.addOrReplaceChild("fire_tail1", CubeListBuilder.create()
                .texOffs(12, 158).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 9.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 3.0F, 0.9F, 0.0F, 0.0F));
        PartDefinition p_fire_tail2 = p_fire_tail1.addOrReplaceChild("fire_tail2", CubeListBuilder.create()
                .texOffs(324, 158).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 9.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 9.0F, 0.0F, 0.45F, 0.0F, 0.0F));
        p_fire_tail2.addOrReplaceChild("fire_tail3", CubeListBuilder.create()
                .texOffs(4, 179).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 4.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 9.0F, 0.0F, 0.3F, 0.0F, 0.0F));
        PartDefinition p_quint = root.addOrReplaceChild("quint", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));
        PartDefinition p_quint_skirt = p_quint.addOrReplaceChild("quint_skirt", CubeListBuilder.create()
                .texOffs(0, 81).addBox(-9.5F, 0.0F, -6.5F, 19.0F, 12.0F, 13.0F)
                .texOffs(368, 143).addBox(-4.0F, 0.0F, -7.0F, 8.0F, 12.0F, 1.0F), PartPose.offset(0.0F, -34.0F, 0.0F));
        p_quint_skirt.addOrReplaceChild("quint_skirt2", CubeListBuilder.create()
                .texOffs(218, 0).addBox(-11.5F, 0.0F, -8.0F, 23.0F, 18.0F, 16.0F)
                .texOffs(288, 106).addBox(-5.0F, 0.0F, -8.6F, 10.0F, 18.0F, 1.0F), PartPose.offset(0.0F, 12.0F, 0.0F));
        PartDefinition p_quint_body = p_quint.addOrReplaceChild("quint_body", CubeListBuilder.create()
                .texOffs(296, 0).addBox(-9.0F, -24.0F, -5.0F, 18.0F, 24.0F, 10.0F)
                .texOffs(362, 126).addBox(-9.5F, -3.0F, -5.5F, 19.0F, 4.0F, 11.0F)
                .texOffs(262, 81).addBox(-3.0F, -24.0F, -5.6F, 6.0F, 21.0F, 1.0F), PartPose.offset(0.0F, -34.0F, 0.0F));
        p_quint_body.addOrReplaceChild("quint_mantle", CubeListBuilder.create()
                .texOffs(0, 106).addBox(-11.0F, -1.0F, -6.5F, 22.0F, 7.0F, 13.0F)
                .texOffs(30, 158).addBox(-9.0F, -9.0F, 4.0F, 18.0F, 9.0F, 2.0F)
                .texOffs(0, 126).addBox(-14.0F, -2.0F, -6.0F, 6.0F, 5.0F, 12.0F)
                .texOffs(36, 126).addBox(8.0F, -2.0F, -6.0F, 6.0F, 5.0F, 12.0F), PartPose.offset(0.0F, -24.0F, 0.0F));
        p_quint_body.addOrReplaceChild("quint_cape", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-11.0F, 0.0F, 0.0F, 22.0F, 52.0F, 1.0F), PartPose.offsetAndRotation(0.0F, -23.0F, 6.0F, 0.1F, 0.0F, 0.0F));
        PartDefinition p_quint_head = p_quint_body.addOrReplaceChild("quint_head", CubeListBuilder.create()
                .texOffs(72, 53).addBox(-7.0F, -14.0F, -7.0F, 14.0F, 14.0F, 14.0F)
                .texOffs(426, 0).addBox(-7.5F, -14.5F, -7.5F, 15.0F, 15.0F, 15.0F)
                .texOffs(338, 81).addBox(-7.0F, -2.0F, 5.0F, 14.0F, 18.0F, 3.0F), PartPose.offset(0.0F, -25.0F, 0.0F));
        p_quint_head.addOrReplaceChild("quint_halo", CubeListBuilder.create()
                .texOffs(456, 179).addBox(8.1F, -0.9F, -0.9F, 1.8F, 1.8F, 1.8F)
                .texOffs(464, 179).addBox(6.6715F, 2.2075F, -0.9F, 1.8F, 1.8F, 1.8F)
                .texOffs(472, 179).addBox(3.9787F, 2.8631F, -0.9F, 1.8F, 1.8F, 1.8F)
                .texOffs(480, 179).addBox(1.6714F, 1.7723F, -0.9F, 1.8F, 1.8F, 1.8F)
                .texOffs(488, 179).addBox(-0.1066F, 0.0376F, -0.9F, 1.8F, 1.8F, 1.8F)
                .texOffs(496, 179).addBox(-1.6934F, -1.8376F, -0.9F, 1.8F, 1.8F, 1.8F)
                .texOffs(504, 179).addBox(-3.4714F, -3.5723F, -0.9F, 1.8F, 1.8F, 1.8F)
                .texOffs(0, 187).addBox(-5.7787F, -4.6631F, -0.9F, 1.8F, 1.8F, 1.8F)
                .texOffs(8, 187).addBox(-8.4715F, -4.0075F, -0.9F, 1.8F, 1.8F, 1.8F)
                .texOffs(16, 187).addBox(-9.9F, -0.9F, -0.9F, 1.8F, 1.8F, 1.8F)
                .texOffs(24, 187).addBox(-8.4715F, 2.2075F, -0.9F, 1.8F, 1.8F, 1.8F)
                .texOffs(32, 187).addBox(-5.7787F, 2.8631F, -0.9F, 1.8F, 1.8F, 1.8F)
                .texOffs(40, 187).addBox(-3.4714F, 1.7723F, -0.9F, 1.8F, 1.8F, 1.8F)
                .texOffs(48, 187).addBox(-1.6934F, 0.0376F, -0.9F, 1.8F, 1.8F, 1.8F)
                .texOffs(56, 187).addBox(-0.1066F, -1.8376F, -0.9F, 1.8F, 1.8F, 1.8F)
                .texOffs(64, 187).addBox(1.6714F, -3.5723F, -0.9F, 1.8F, 1.8F, 1.8F)
                .texOffs(72, 187).addBox(3.9787F, -4.6631F, -0.9F, 1.8F, 1.8F, 1.8F)
                .texOffs(80, 187).addBox(6.6715F, -4.0075F, -0.9F, 1.8F, 1.8F, 1.8F), PartPose.offset(0.0F, -21.0F, 0.0F));
        PartDefinition p_quint_arm_r = p_quint_body.addOrReplaceChild("quint_arm_r", CubeListBuilder.create()
                .texOffs(372, 81).addBox(-3.0F, -2.0F, -3.5F, 6.0F, 14.0F, 7.0F), PartPose.offsetAndRotation(-11.0F, -21.0F, 0.0F, 0.0F, 0.0F, 0.12F));
        PartDefinition p_quint_fore_r = p_quint_arm_r.addOrReplaceChild("quint_fore_r", CubeListBuilder.create()
                .texOffs(204, 106).addBox(-3.5F, 0.0F, -4.0F, 7.0F, 12.0F, 8.0F), PartPose.offset(0.0F, 12.0F, 0.0F));
        PartDefinition p_quint_hand_r = p_quint_fore_r.addOrReplaceChild("quint_hand_r", CubeListBuilder.create()
                .texOffs(52, 170).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 5.0F, 4.0F), PartPose.offset(0.0F, 12.0F, 0.0F));
        p_quint_hand_r.addOrReplaceChild("quint_wand", CubeListBuilder.create()
                .texOffs(352, 0).addBox(-1.0F, -15.0F, -1.0F, 2.0F, 30.0F, 2.0F), PartPose.offset(0.0F, 3.0F, 0.0F));
        PartDefinition p_quint_arm_l = p_quint_body.addOrReplaceChild("quint_arm_l", CubeListBuilder.create()
                .texOffs(398, 81).addBox(-3.0F, -2.0F, -3.5F, 6.0F, 14.0F, 7.0F), PartPose.offsetAndRotation(11.0F, -21.0F, 0.0F, 0.0F, 0.0F, -0.12F));
        PartDefinition p_quint_fore_l = p_quint_arm_l.addOrReplaceChild("quint_fore_l", CubeListBuilder.create()
                .texOffs(234, 106).addBox(-3.5F, 0.0F, -4.0F, 7.0F, 12.0F, 8.0F), PartPose.offset(0.0F, 12.0F, 0.0F));
        p_quint_fore_l.addOrReplaceChild("quint_hand_l", CubeListBuilder.create()
                .texOffs(68, 170).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 5.0F, 4.0F)
                .texOffs(344, 179).addBox(-0.5F, 5.0F, -1.5F, 1.0F, 4.0F, 1.0F), PartPose.offset(0.0F, 12.0F, 0.0F));
        PartDefinition p_quint_orbit = p_quint.addOrReplaceChild("quint_orbit", CubeListBuilder.create(), PartPose.offset(0.0F, -42.0F, 0.0F));
        p_quint_orbit.addOrReplaceChild("quint_tool_pentacle", CubeListBuilder.create()
                .texOffs(122, 158).addBox(-4.5F, -4.5F, -0.75F, 9.0F, 9.0F, 1.5F), PartPose.offset(26.0F, 0.0F, 0.0F));
        p_quint_orbit.addOrReplaceChild("quint_tool_sword", CubeListBuilder.create()
                .texOffs(174, 126).addBox(-1.0F, -16.0F, -0.5F, 2.0F, 16.0F, 1.0F)
                .texOffs(408, 179).addBox(-4.0F, 0.0F, -1.0F, 8.0F, 1.5F, 2.0F)
                .texOffs(48, 179).addBox(-0.75F, 1.5F, -0.75F, 1.5F, 4.5F, 1.5F)
                .texOffs(280, 179).addBox(-1.25F, 6.0F, -1.25F, 2.5F, 2.5F, 2.5F), PartPose.offset(-26.0F, 0.0F, 0.0F));
        p_quint_orbit.addOrReplaceChild("quint_tool_cup", CubeListBuilder.create()
                .texOffs(336, 143).addBox(-4.0F, -6.0F, -4.0F, 8.0F, 5.0F, 8.0F)
                .texOffs(308, 179).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 4.0F, 2.0F)
                .texOffs(290, 170).addBox(-3.0F, 3.0F, -3.0F, 6.0F, 1.5F, 6.0F), PartPose.offset(0.0F, 0.0F, 26.0F));
        p_quint_orbit.addOrReplaceChild("quint_tool_wand", CubeListBuilder.create()
                .texOffs(280, 106).addBox(-0.75F, -9.0F, -0.75F, 1.5F, 18.0F, 1.5F)
                .texOffs(374, 170).addBox(-1.75F, -13.0F, -1.75F, 3.5F, 4.0F, 3.5F), PartPose.offset(0.0F, 0.0F, -26.0F));
        return LayerDefinition.create(mesh, 512, 256);
    }

    public static Map<String, ModelPart> bodyParts(ModelPart root) {
        Map<String, ModelPart> map = new HashMap<>();
        map.put("earth", root.getChild("earth"));
        map.put("earth_base", root.getChild("earth").getChild("earth_base"));
        map.put("earth_waist", root.getChild("earth").getChild("earth_base").getChild("earth_waist"));
        map.put("earth_torso", root.getChild("earth").getChild("earth_base").getChild("earth_waist").getChild("earth_torso"));
        map.put("earth_crystal_0", root.getChild("earth").getChild("earth_base").getChild("earth_waist").getChild("earth_torso").getChild("earth_crystal_0"));
        map.put("earth_crystal_1", root.getChild("earth").getChild("earth_base").getChild("earth_waist").getChild("earth_torso").getChild("earth_crystal_1"));
        map.put("earth_crystal_2", root.getChild("earth").getChild("earth_base").getChild("earth_waist").getChild("earth_torso").getChild("earth_crystal_2"));
        map.put("earth_crystal_3", root.getChild("earth").getChild("earth_base").getChild("earth_waist").getChild("earth_torso").getChild("earth_crystal_3"));
        map.put("earth_crystal_4", root.getChild("earth").getChild("earth_base").getChild("earth_waist").getChild("earth_torso").getChild("earth_crystal_4"));
        map.put("earth_shoulder_r", root.getChild("earth").getChild("earth_base").getChild("earth_waist").getChild("earth_torso").getChild("earth_shoulder_r"));
        map.put("earth_shoulder_l", root.getChild("earth").getChild("earth_base").getChild("earth_waist").getChild("earth_torso").getChild("earth_shoulder_l"));
        map.put("earth_head", root.getChild("earth").getChild("earth_base").getChild("earth_waist").getChild("earth_torso").getChild("earth_head"));
        map.put("earth_jaw", root.getChild("earth").getChild("earth_base").getChild("earth_waist").getChild("earth_torso").getChild("earth_head").getChild("earth_jaw"));
        map.put("earth_arm_r", root.getChild("earth").getChild("earth_base").getChild("earth_waist").getChild("earth_torso").getChild("earth_arm_r"));
        map.put("earth_fore_r", root.getChild("earth").getChild("earth_base").getChild("earth_waist").getChild("earth_torso").getChild("earth_arm_r").getChild("earth_fore_r"));
        map.put("earth_fist_r", root.getChild("earth").getChild("earth_base").getChild("earth_waist").getChild("earth_torso").getChild("earth_arm_r").getChild("earth_fore_r").getChild("earth_fist_r"));
        map.put("earth_arm_l", root.getChild("earth").getChild("earth_base").getChild("earth_waist").getChild("earth_torso").getChild("earth_arm_l"));
        map.put("earth_fore_l", root.getChild("earth").getChild("earth_base").getChild("earth_waist").getChild("earth_torso").getChild("earth_arm_l").getChild("earth_fore_l"));
        map.put("earth_fist_l", root.getChild("earth").getChild("earth_base").getChild("earth_waist").getChild("earth_torso").getChild("earth_arm_l").getChild("earth_fore_l").getChild("earth_fist_l"));
        map.put("air", root.getChild("air"));
        map.put("air_body", root.getChild("air").getChild("air_body"));
        map.put("air_neck", root.getChild("air").getChild("air_body").getChild("air_neck"));
        map.put("air_head", root.getChild("air").getChild("air_body").getChild("air_neck").getChild("air_head"));
        map.put("air_beak", root.getChild("air").getChild("air_body").getChild("air_neck").getChild("air_head").getChild("air_beak"));
        map.put("air_beak_low", root.getChild("air").getChild("air_body").getChild("air_neck").getChild("air_head").getChild("air_beak_low"));
        map.put("air_crest_0", root.getChild("air").getChild("air_body").getChild("air_neck").getChild("air_head").getChild("air_crest_0"));
        map.put("air_crest_1", root.getChild("air").getChild("air_body").getChild("air_neck").getChild("air_head").getChild("air_crest_1"));
        map.put("air_crest_2", root.getChild("air").getChild("air_body").getChild("air_neck").getChild("air_head").getChild("air_crest_2"));
        map.put("air_mantle_r", root.getChild("air").getChild("air_body").getChild("air_mantle_r"));
        map.put("air_wing_r", root.getChild("air").getChild("air_body").getChild("air_wing_r"));
        map.put("air_wingtip_r", root.getChild("air").getChild("air_body").getChild("air_wing_r").getChild("air_wingtip_r"));
        map.put("air_feather_r0", root.getChild("air").getChild("air_body").getChild("air_wing_r").getChild("air_feather_r0"));
        map.put("air_feather_r1", root.getChild("air").getChild("air_body").getChild("air_wing_r").getChild("air_feather_r1"));
        map.put("air_feather_r2", root.getChild("air").getChild("air_body").getChild("air_wing_r").getChild("air_feather_r2"));
        map.put("air_feather_r3", root.getChild("air").getChild("air_body").getChild("air_wing_r").getChild("air_feather_r3"));
        map.put("air_feather_r4", root.getChild("air").getChild("air_body").getChild("air_wing_r").getChild("air_wingtip_r").getChild("air_feather_r4"));
        map.put("air_feather_r5", root.getChild("air").getChild("air_body").getChild("air_wing_r").getChild("air_wingtip_r").getChild("air_feather_r5"));
        map.put("air_feather_r6", root.getChild("air").getChild("air_body").getChild("air_wing_r").getChild("air_wingtip_r").getChild("air_feather_r6"));
        map.put("air_arm_r", root.getChild("air").getChild("air_body").getChild("air_arm_r"));
        map.put("air_fore_r", root.getChild("air").getChild("air_body").getChild("air_arm_r").getChild("air_fore_r"));
        map.put("air_claws_r", root.getChild("air").getChild("air_body").getChild("air_arm_r").getChild("air_fore_r").getChild("air_claws_r"));
        map.put("air_mantle_l", root.getChild("air").getChild("air_body").getChild("air_mantle_l"));
        map.put("air_wing_l", root.getChild("air").getChild("air_body").getChild("air_wing_l"));
        map.put("air_wingtip_l", root.getChild("air").getChild("air_body").getChild("air_wing_l").getChild("air_wingtip_l"));
        map.put("air_feather_l0", root.getChild("air").getChild("air_body").getChild("air_wing_l").getChild("air_feather_l0"));
        map.put("air_feather_l1", root.getChild("air").getChild("air_body").getChild("air_wing_l").getChild("air_feather_l1"));
        map.put("air_feather_l2", root.getChild("air").getChild("air_body").getChild("air_wing_l").getChild("air_feather_l2"));
        map.put("air_feather_l3", root.getChild("air").getChild("air_body").getChild("air_wing_l").getChild("air_feather_l3"));
        map.put("air_feather_l4", root.getChild("air").getChild("air_body").getChild("air_wing_l").getChild("air_wingtip_l").getChild("air_feather_l4"));
        map.put("air_feather_l5", root.getChild("air").getChild("air_body").getChild("air_wing_l").getChild("air_wingtip_l").getChild("air_feather_l5"));
        map.put("air_feather_l6", root.getChild("air").getChild("air_body").getChild("air_wing_l").getChild("air_wingtip_l").getChild("air_feather_l6"));
        map.put("air_arm_l", root.getChild("air").getChild("air_body").getChild("air_arm_l"));
        map.put("air_fore_l", root.getChild("air").getChild("air_body").getChild("air_arm_l").getChild("air_fore_l"));
        map.put("air_claws_l", root.getChild("air").getChild("air_body").getChild("air_arm_l").getChild("air_fore_l").getChild("air_claws_l"));
        map.put("air_tail1", root.getChild("air").getChild("air_body").getChild("air_tail1"));
        map.put("air_tail2", root.getChild("air").getChild("air_body").getChild("air_tail1").getChild("air_tail2"));
        map.put("air_tail3", root.getChild("air").getChild("air_body").getChild("air_tail1").getChild("air_tail2").getChild("air_tail3"));
        map.put("air_tail4", root.getChild("air").getChild("air_body").getChild("air_tail1").getChild("air_tail2").getChild("air_tail3").getChild("air_tail4"));
        map.put("water", root.getChild("water"));
        map.put("water_body", root.getChild("water").getChild("water_body"));
        map.put("water_spike_0", root.getChild("water").getChild("water_body").getChild("water_spike_0"));
        map.put("water_spike_1", root.getChild("water").getChild("water_body").getChild("water_spike_1"));
        map.put("water_spike_2", root.getChild("water").getChild("water_body").getChild("water_spike_2"));
        map.put("water_spike_3", root.getChild("water").getChild("water_body").getChild("water_spike_3"));
        map.put("water_spike_4", root.getChild("water").getChild("water_body").getChild("water_spike_4"));
        map.put("water_cup", root.getChild("water").getChild("water_body").getChild("water_cup"));
        map.put("water_bowl", root.getChild("water").getChild("water_body").getChild("water_cup").getChild("water_bowl"));
        map.put("water_head", root.getChild("water").getChild("water_body").getChild("water_head"));
        map.put("water_jaw", root.getChild("water").getChild("water_body").getChild("water_head").getChild("water_jaw"));
        map.put("water_lure", root.getChild("water").getChild("water_body").getChild("water_head").getChild("water_lure"));
        map.put("water_lure2", root.getChild("water").getChild("water_body").getChild("water_head").getChild("water_lure").getChild("water_lure2"));
        map.put("water_lure_tip", root.getChild("water").getChild("water_body").getChild("water_head").getChild("water_lure").getChild("water_lure2").getChild("water_lure_tip"));
        map.put("water_arm_r", root.getChild("water").getChild("water_body").getChild("water_arm_r"));
        map.put("water_fore_r", root.getChild("water").getChild("water_body").getChild("water_arm_r").getChild("water_fore_r"));
        map.put("water_hand_r", root.getChild("water").getChild("water_body").getChild("water_arm_r").getChild("water_fore_r").getChild("water_hand_r"));
        map.put("water_arm_l", root.getChild("water").getChild("water_body").getChild("water_arm_l"));
        map.put("water_fore_l", root.getChild("water").getChild("water_body").getChild("water_arm_l").getChild("water_fore_l"));
        map.put("water_hand_l", root.getChild("water").getChild("water_body").getChild("water_arm_l").getChild("water_fore_l").getChild("water_hand_l"));
        map.put("water_tent0_1", root.getChild("water").getChild("water_body").getChild("water_tent0_1"));
        map.put("water_tent0_2", root.getChild("water").getChild("water_body").getChild("water_tent0_1").getChild("water_tent0_2"));
        map.put("water_tent0_3", root.getChild("water").getChild("water_body").getChild("water_tent0_1").getChild("water_tent0_2").getChild("water_tent0_3"));
        map.put("water_tent1_1", root.getChild("water").getChild("water_body").getChild("water_tent1_1"));
        map.put("water_tent1_2", root.getChild("water").getChild("water_body").getChild("water_tent1_1").getChild("water_tent1_2"));
        map.put("water_tent1_3", root.getChild("water").getChild("water_body").getChild("water_tent1_1").getChild("water_tent1_2").getChild("water_tent1_3"));
        map.put("water_tent2_1", root.getChild("water").getChild("water_body").getChild("water_tent2_1"));
        map.put("water_tent2_2", root.getChild("water").getChild("water_body").getChild("water_tent2_1").getChild("water_tent2_2"));
        map.put("water_tent2_3", root.getChild("water").getChild("water_body").getChild("water_tent2_1").getChild("water_tent2_2").getChild("water_tent2_3"));
        map.put("water_tent3_1", root.getChild("water").getChild("water_body").getChild("water_tent3_1"));
        map.put("water_tent3_2", root.getChild("water").getChild("water_body").getChild("water_tent3_1").getChild("water_tent3_2"));
        map.put("water_tent3_3", root.getChild("water").getChild("water_body").getChild("water_tent3_1").getChild("water_tent3_2").getChild("water_tent3_3"));
        map.put("water_tent4_1", root.getChild("water").getChild("water_body").getChild("water_tent4_1"));
        map.put("water_tent4_2", root.getChild("water").getChild("water_body").getChild("water_tent4_1").getChild("water_tent4_2"));
        map.put("water_tent4_3", root.getChild("water").getChild("water_body").getChild("water_tent4_1").getChild("water_tent4_2").getChild("water_tent4_3"));
        map.put("water_tent5_1", root.getChild("water").getChild("water_body").getChild("water_tent5_1"));
        map.put("water_tent5_2", root.getChild("water").getChild("water_body").getChild("water_tent5_1").getChild("water_tent5_2"));
        map.put("water_tent5_3", root.getChild("water").getChild("water_body").getChild("water_tent5_1").getChild("water_tent5_2").getChild("water_tent5_3"));
        map.put("fire", root.getChild("fire"));
        map.put("fire_hips", root.getChild("fire").getChild("fire_hips"));
        map.put("fire_thigh_r", root.getChild("fire").getChild("fire_hips").getChild("fire_thigh_r"));
        map.put("fire_shin_r", root.getChild("fire").getChild("fire_hips").getChild("fire_thigh_r").getChild("fire_shin_r"));
        map.put("fire_foot_r", root.getChild("fire").getChild("fire_hips").getChild("fire_thigh_r").getChild("fire_shin_r").getChild("fire_foot_r"));
        map.put("fire_thigh_l", root.getChild("fire").getChild("fire_hips").getChild("fire_thigh_l"));
        map.put("fire_shin_l", root.getChild("fire").getChild("fire_hips").getChild("fire_thigh_l").getChild("fire_shin_l"));
        map.put("fire_foot_l", root.getChild("fire").getChild("fire_hips").getChild("fire_thigh_l").getChild("fire_shin_l").getChild("fire_foot_l"));
        map.put("fire_torso", root.getChild("fire").getChild("fire_hips").getChild("fire_torso"));
        map.put("fire_ribs", root.getChild("fire").getChild("fire_hips").getChild("fire_torso").getChild("fire_ribs"));
        map.put("fire_spike_0", root.getChild("fire").getChild("fire_hips").getChild("fire_torso").getChild("fire_ribs").getChild("fire_spike_0"));
        map.put("fire_spike_1", root.getChild("fire").getChild("fire_hips").getChild("fire_torso").getChild("fire_ribs").getChild("fire_spike_1"));
        map.put("fire_spike_2", root.getChild("fire").getChild("fire_hips").getChild("fire_torso").getChild("fire_ribs").getChild("fire_spike_2"));
        map.put("fire_head", root.getChild("fire").getChild("fire_hips").getChild("fire_torso").getChild("fire_ribs").getChild("fire_head"));
        map.put("fire_jaw", root.getChild("fire").getChild("fire_hips").getChild("fire_torso").getChild("fire_ribs").getChild("fire_head").getChild("fire_jaw"));
        map.put("fire_horn_r1", root.getChild("fire").getChild("fire_hips").getChild("fire_torso").getChild("fire_ribs").getChild("fire_head").getChild("fire_horn_r1"));
        map.put("fire_horn_r2", root.getChild("fire").getChild("fire_hips").getChild("fire_torso").getChild("fire_ribs").getChild("fire_head").getChild("fire_horn_r1").getChild("fire_horn_r2"));
        map.put("fire_horn_r3", root.getChild("fire").getChild("fire_hips").getChild("fire_torso").getChild("fire_ribs").getChild("fire_head").getChild("fire_horn_r1").getChild("fire_horn_r2").getChild("fire_horn_r3"));
        map.put("fire_horn_r4", root.getChild("fire").getChild("fire_hips").getChild("fire_torso").getChild("fire_ribs").getChild("fire_head").getChild("fire_horn_r1").getChild("fire_horn_r2").getChild("fire_horn_r3").getChild("fire_horn_r4"));
        map.put("fire_horn_l1", root.getChild("fire").getChild("fire_hips").getChild("fire_torso").getChild("fire_ribs").getChild("fire_head").getChild("fire_horn_l1"));
        map.put("fire_horn_l2", root.getChild("fire").getChild("fire_hips").getChild("fire_torso").getChild("fire_ribs").getChild("fire_head").getChild("fire_horn_l1").getChild("fire_horn_l2"));
        map.put("fire_horn_l3", root.getChild("fire").getChild("fire_hips").getChild("fire_torso").getChild("fire_ribs").getChild("fire_head").getChild("fire_horn_l1").getChild("fire_horn_l2").getChild("fire_horn_l3"));
        map.put("fire_horn_l4", root.getChild("fire").getChild("fire_hips").getChild("fire_torso").getChild("fire_ribs").getChild("fire_head").getChild("fire_horn_l1").getChild("fire_horn_l2").getChild("fire_horn_l3").getChild("fire_horn_l4"));
        map.put("fire_crown", root.getChild("fire").getChild("fire_hips").getChild("fire_torso").getChild("fire_ribs").getChild("fire_head").getChild("fire_crown"));
        map.put("fire_arm_r", root.getChild("fire").getChild("fire_hips").getChild("fire_torso").getChild("fire_ribs").getChild("fire_arm_r"));
        map.put("fire_fore_r", root.getChild("fire").getChild("fire_hips").getChild("fire_torso").getChild("fire_ribs").getChild("fire_arm_r").getChild("fire_fore_r"));
        map.put("fire_staff_tip", root.getChild("fire").getChild("fire_hips").getChild("fire_torso").getChild("fire_ribs").getChild("fire_arm_r").getChild("fire_fore_r").getChild("fire_staff_tip"));
        map.put("fire_arm_l", root.getChild("fire").getChild("fire_hips").getChild("fire_torso").getChild("fire_ribs").getChild("fire_arm_l"));
        map.put("fire_fore_l", root.getChild("fire").getChild("fire_hips").getChild("fire_torso").getChild("fire_ribs").getChild("fire_arm_l").getChild("fire_fore_l"));
        map.put("fire_claw_l", root.getChild("fire").getChild("fire_hips").getChild("fire_torso").getChild("fire_ribs").getChild("fire_arm_l").getChild("fire_fore_l").getChild("fire_claw_l"));
        map.put("fire_tail1", root.getChild("fire").getChild("fire_hips").getChild("fire_tail1"));
        map.put("fire_tail2", root.getChild("fire").getChild("fire_hips").getChild("fire_tail1").getChild("fire_tail2"));
        map.put("fire_tail3", root.getChild("fire").getChild("fire_hips").getChild("fire_tail1").getChild("fire_tail2").getChild("fire_tail3"));
        map.put("quint", root.getChild("quint"));
        map.put("quint_skirt", root.getChild("quint").getChild("quint_skirt"));
        map.put("quint_skirt2", root.getChild("quint").getChild("quint_skirt").getChild("quint_skirt2"));
        map.put("quint_body", root.getChild("quint").getChild("quint_body"));
        map.put("quint_mantle", root.getChild("quint").getChild("quint_body").getChild("quint_mantle"));
        map.put("quint_cape", root.getChild("quint").getChild("quint_body").getChild("quint_cape"));
        map.put("quint_head", root.getChild("quint").getChild("quint_body").getChild("quint_head"));
        map.put("quint_halo", root.getChild("quint").getChild("quint_body").getChild("quint_head").getChild("quint_halo"));
        map.put("quint_arm_r", root.getChild("quint").getChild("quint_body").getChild("quint_arm_r"));
        map.put("quint_fore_r", root.getChild("quint").getChild("quint_body").getChild("quint_arm_r").getChild("quint_fore_r"));
        map.put("quint_hand_r", root.getChild("quint").getChild("quint_body").getChild("quint_arm_r").getChild("quint_fore_r").getChild("quint_hand_r"));
        map.put("quint_wand", root.getChild("quint").getChild("quint_body").getChild("quint_arm_r").getChild("quint_fore_r").getChild("quint_hand_r").getChild("quint_wand"));
        map.put("quint_arm_l", root.getChild("quint").getChild("quint_body").getChild("quint_arm_l"));
        map.put("quint_fore_l", root.getChild("quint").getChild("quint_body").getChild("quint_arm_l").getChild("quint_fore_l"));
        map.put("quint_hand_l", root.getChild("quint").getChild("quint_body").getChild("quint_arm_l").getChild("quint_fore_l").getChild("quint_hand_l"));
        map.put("quint_orbit", root.getChild("quint").getChild("quint_orbit"));
        map.put("quint_tool_pentacle", root.getChild("quint").getChild("quint_orbit").getChild("quint_tool_pentacle"));
        map.put("quint_tool_sword", root.getChild("quint").getChild("quint_orbit").getChild("quint_tool_sword"));
        map.put("quint_tool_cup", root.getChild("quint").getChild("quint_orbit").getChild("quint_tool_cup"));
        map.put("quint_tool_wand", root.getChild("quint").getChild("quint_orbit").getChild("quint_tool_wand"));
        return map;
    }

    // ----- pedra rúnica, bolha e magias (atlas 128x128) -----
    public static final ModelLayerLocation FX_LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Mistech.MODID, "magician_fx"), "main");

    public static LayerDefinition fxLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("runestone", CubeListBuilder.create()
                .texOffs(40, 52).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("bubble", CubeListBuilder.create()
                .texOffs(0, 28).addBox(-8.0F, -6.0F, -6.0F, 16.0F, 12.0F, 12.0F)
                .texOffs(56, 0).addBox(-6.0F, -8.0F, -6.0F, 12.0F, 16.0F, 12.0F)
                .texOffs(0, 0).addBox(-6.0F, -6.0F, -8.0F, 12.0F, 12.0F, 16.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p_orb = root.addOrReplaceChild("orb", CubeListBuilder.create()
                .texOffs(72, 52).addBox(-3.0F, -3.0F, -3.0F, 6.0F, 6.0F, 6.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        p_orb.addOrReplaceChild("orb_core", CubeListBuilder.create()
                .texOffs(30, 72).addBox(-1.5F, -1.5F, -1.5F, 3.0F, 3.0F, 3.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("boulder", CubeListBuilder.create()
                .texOffs(56, 28).addBox(-6.0F, -6.0F, -6.0F, 12.0F, 12.0F, 12.0F)
                .texOffs(96, 52).addBox(-4.0F, -7.0F, -5.0F, 8.0F, 2.0F, 8.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("meteor", CubeListBuilder.create()
                .texOffs(0, 52).addBox(-5.0F, -5.0F, -5.0F, 10.0F, 10.0F, 10.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("blade", CubeListBuilder.create()
                .texOffs(42, 72).addBox(-8.0F, -1.0F, -1.0F, 16.0F, 2.0F, 2.0F)
                .texOffs(0, 72).addBox(-5.0F, -0.5F, -2.5F, 10.0F, 1.0F, 5.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    public static Map<String, ModelPart> fxParts(ModelPart root) {
        Map<String, ModelPart> map = new HashMap<>();
        map.put("runestone", root.getChild("runestone"));
        map.put("bubble", root.getChild("bubble"));
        map.put("orb", root.getChild("orb"));
        map.put("orb_core", root.getChild("orb").getChild("orb_core"));
        map.put("boulder", root.getChild("boulder"));
        map.put("meteor", root.getChild("meteor"));
        map.put("blade", root.getChild("blade"));
        return map;
    }
}

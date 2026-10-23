package com.mistech.mistechmod.spellcraft.client;

import com.mistech.mistechmod.Mistech;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;

/**
 * GERADO por tools/spellcraft/gen_summons.py — não editar à mão.
 * Geometria das invocações. As partes têm os mesmos nomes do Golem de Ferro para o IronGolemModel animar.
 */
public final class SummonModels {
    public static final ModelLayerLocation STONE_GOLEM = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Mistech.MODID, "stone_golem"), "main");
    public static final ModelLayerLocation WOOD_GUARDIAN = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Mistech.MODID, "wood_guardian"), "main");

    private SummonModels() {}

    public static LayerDefinition stoneGolem() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(52, 29).addBox(-4.0F, -11.0F, -4.5F, 8.0F, 9.0F, 8.0F)
                .texOffs(0, 68).addBox(-3.0F, -12.0F, -3.0F, 6.0F, 1.0F, 6.0F)
                .texOffs(38, 68).addBox(-4.5F, -9.0F, -5.5F, 9.0F, 2.0F, 2.0F),
                PartPose.offset(0.0F, -7.0F, -2.0F));
        root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(48, 0).addBox(-10.0F, -3.0F, -6.5F, 20.0F, 13.0F, 12.0F)
                .texOffs(32, 51).addBox(-5.0F, 10.0F, -3.5F, 10.0F, 6.0F, 7.0F)
                .texOffs(66, 51).addBox(-12.0F, -5.0F, -4.0F, 5.0F, 5.0F, 8.0F)
                .texOffs(92, 51).addBox(7.0F, -5.0F, -4.0F, 5.0F, 5.0F, 8.0F)
                .texOffs(24, 68).addBox(-3.0F, 1.0F, -7.5F, 6.0F, 6.0F, 1.0F),
                PartPose.offset(0.0F, -7.0F, 0.0F));
        root.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-15.0F, -3.0F, -3.5F, 5.0F, 22.0F, 7.0F)
                .texOffs(84, 29).addBox(-16.0F, 19.0F, -4.5F, 7.0F, 8.0F, 9.0F),
                PartPose.offset(0.0F, -7.0F, 0.0F));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(24, 0).addBox(10.0F, -3.0F, -3.5F, 5.0F, 22.0F, 7.0F)
                .texOffs(0, 51).addBox(9.0F, 19.0F, -4.5F, 7.0F, 8.0F, 9.0F),
                PartPose.offset(0.0F, -7.0F, 0.0F));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(0, 29).addBox(-3.5F, -3.0F, -3.0F, 7.0F, 16.0F, 6.0F),
                PartPose.offset(-4.0F, 11.0F, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(26, 29).addBox(-3.5F, -3.0F, -3.0F, 7.0F, 16.0F, 6.0F),
                PartPose.offset(5.0F, 11.0F, 0.0F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    public static LayerDefinition woodGuardian() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 28).addBox(-3.5F, -10.0F, -3.5F, 7.0F, 10.0F, 7.0F)
                .texOffs(28, 28).addBox(-5.5F, -15.0F, -5.5F, 11.0F, 6.0F, 11.0F)
                .texOffs(84, 57).addBox(-6.0F, -19.0F, -0.5F, 2.0F, 6.0F, 1.0F)
                .texOffs(90, 57).addBox(4.0F, -19.0F, -0.5F, 2.0F, 6.0F, 1.0F),
                PartPose.offset(0.0F, -9.0F, -1.0F));
        root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(32, 0).addBox(-6.0F, -5.0F, -4.0F, 12.0F, 18.0F, 8.0F)
                .texOffs(72, 28).addBox(-7.0F, -6.0F, -5.0F, 14.0F, 3.0F, 10.0F)
                .texOffs(0, 45).addBox(-6.5F, 5.0F, -4.5F, 13.0F, 3.0F, 9.0F),
                PartPose.offset(0.0F, -7.0F, 0.0F));
        root.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-10.0F, -3.0F, -2.0F, 4.0F, 24.0F, 4.0F)
                .texOffs(44, 45).addBox(-11.0F, 21.0F, -2.5F, 6.0F, 6.0F, 5.0F)
                .texOffs(88, 45).addBox(-11.0F, -4.0F, -3.0F, 6.0F, 4.0F, 6.0F),
                PartPose.offset(0.0F, -7.0F, 0.0F));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(16, 0).addBox(6.0F, -3.0F, -2.0F, 4.0F, 24.0F, 4.0F)
                .texOffs(66, 45).addBox(5.0F, 21.0F, -2.5F, 6.0F, 6.0F, 5.0F)
                .texOffs(0, 57).addBox(5.0F, -4.0F, -3.0F, 6.0F, 4.0F, 6.0F),
                PartPose.offset(0.0F, -7.0F, 0.0F));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(72, 0).addBox(-2.5F, -2.0F, -2.5F, 5.0F, 13.0F, 5.0F)
                .texOffs(24, 57).addBox(-3.5F, 11.0F, -4.0F, 7.0F, 2.0F, 8.0F),
                PartPose.offset(-3.5F, 11.0F, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(92, 0).addBox(-2.5F, -2.0F, -2.5F, 5.0F, 13.0F, 5.0F)
                .texOffs(54, 57).addBox(-3.5F, 11.0F, -4.0F, 7.0F, 2.0F, 8.0F),
                PartPose.offset(3.5F, 11.0F, 0.0F));
        return LayerDefinition.create(mesh, 128, 128);
    }
}

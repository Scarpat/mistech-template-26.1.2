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

/** Memória: flutua devagar, a barra do vestido balança e a luz da gaiola pulsa. Geometria e textura: {@code tools/dream/mobs3.py}. */
public class MemoryModel extends EntityModel<DreamMobRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Mistech.MODID, "memory"), "main");
    private final ModelPart body, skirt, rightArm, leftArm, light;

    public MemoryModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.skirt = this.body.getChild("skirt");
        this.rightArm = this.body.getChild("right_arm");
        this.leftArm = this.body.getChild("left_arm");
        this.light = this.body.getChild("cage").getChild("light");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(24, 0).addBox(-2.5F, 0.0F, -1.5F, 5.0F, 6.0F, 3.0F), PartPose.offset(0.0F, 9.0F, 0.0F));
        PartDefinition p_skirt = p_body.addOrReplaceChild("skirt", CubeListBuilder.create().texOffs(0, 0).addBox(-3.5F, 0.0F, -2.5F, 7.0F, 9.0F, 5.0F), PartPose.offset(0.0F, 6.0F, 0.0F));
        PartDefinition p_right_arm = p_body.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(40, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 7.0F, 2.0F, new CubeDeformation(-0.25F, 0.0F, -0.25F)), PartPose.offsetAndRotation(-3.0F, 0.5F, 0.0F, 0.0F, 0.0F, 0.12F));
        PartDefinition p_left_arm = p_body.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(48, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 7.0F, 2.0F, new CubeDeformation(-0.25F, 0.0F, -0.25F)), PartPose.offsetAndRotation(3.0F, 0.5F, 0.0F, 0.0F, 0.0F, -0.12F));
        PartDefinition p_cage = p_body.addOrReplaceChild("cage", CubeListBuilder.create().texOffs(0, 14).addBox(-3.0F, -1.0F, -3.0F, 6.0F, 1.0F, 6.0F).texOffs(36, 14).addBox(-3.0F, -7.0F, -3.0F, 1.0F, 6.0F, 1.0F).texOffs(40, 14).addBox(2.0F, -7.0F, -3.0F, 1.0F, 6.0F, 1.0F).texOffs(44, 14).addBox(-3.0F, -7.0F, 2.0F, 1.0F, 6.0F, 1.0F).texOffs(48, 14).addBox(2.0F, -7.0F, 2.0F, 1.0F, 6.0F, 1.0F).texOffs(52, 14).addBox(-3.0F, -7.0F, -0.5F, 1.0F, 6.0F, 1.0F).texOffs(56, 14).addBox(2.0F, -7.0F, -0.5F, 1.0F, 6.0F, 1.0F).texOffs(0, 21).addBox(-2.5F, -8.0F, -2.5F, 5.0F, 1.0F, 5.0F).texOffs(20, 21).addBox(-1.0F, -9.0F, -1.0F, 2.0F, 1.0F, 2.0F).texOffs(28, 21).addBox(-0.5F, -11.0F, -0.5F, 1.0F, 2.0F, 1.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p_light = p_cage.addOrReplaceChild("light", CubeListBuilder.create().texOffs(24, 14).addBox(-1.5F, -2.0F, -1.5F, 3.0F, 4.0F, 3.0F), PartPose.offset(0.0F, -4.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(DreamMobRenderState state) {
        super.setupAnim(state);
        float age = state.ageInTicks;
        this.body.y += Mth.sin(age * 0.08F) * 1.0F;
        this.body.yRot = state.yRot * Mth.DEG_TO_RAD * 0.3F;
        this.skirt.xRot = Mth.sin(age * 0.06F) * 0.08F + state.walkAnimationSpeed * 0.3F;
        this.rightArm.xRot = Mth.sin(age * 0.05F) * 0.08F;
        this.leftArm.xRot = -Mth.sin(age * 0.05F) * 0.08F;
        float pulse = 1.0F + Mth.sin(age * 0.3F) * 0.08F + Mth.sin(age * 1.7F) * 0.03F;
        this.light.xScale = this.light.yScale = this.light.zScale = pulse;
    }
}

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

/** Ovelha-de-Contar: uma nuvem de lã com rostinho e pezinhos. Tosquiada, sobra o corpinho rosado e a cabeça encosta nele. Geometria e textura: {@code tools/dream/mobs.py}. */
public class CountingSheepModel extends EntityModel<DreamMobRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Mistech.MODID, "counting_sheep"), "main");
    private final ModelPart body, wool, tail, head, legFr, legFl, legBr, legBl;

    public CountingSheepModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.wool = root.getChild("wool");
        this.tail = this.wool.getChild("tail");
        this.head = root.getChild("head");
        this.legFr = root.getChild("leg_fr");
        this.legFl = root.getChild("leg_fl");
        this.legBr = root.getChild("leg_br");
        this.legBl = root.getChild("leg_bl");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(56, 0).addBox(-4.0F, -3.0F, -5.0F, 8.0F, 6.0F, 10.0F), PartPose.offset(0.0F, 17.0F, 0.0F));
        PartDefinition p_wool = root.addOrReplaceChild("wool", CubeListBuilder.create().texOffs(0, 0).addBox(-6.5F, -6.0F, -7.5F, 13.0F, 12.0F, 15.0F).texOffs(0, 41).addBox(-6.0F, -9.0F, -6.0F, 6.0F, 3.0F, 6.0F).texOffs(78, 27).addBox(0.0F, -10.0F, -4.0F, 6.0F, 4.0F, 7.0F).texOffs(56, 41).addBox(-4.0F, -9.0F, 2.0F, 7.0F, 3.0F, 5.0F).texOffs(108, 41).addBox(-2.0F, -11.0F, -1.0F, 4.0F, 2.0F, 4.0F).texOffs(62, 27).addBox(-8.5F, -4.0F, -5.0F, 2.0F, 6.0F, 6.0F).texOffs(104, 27).addBox(-8.0F, -3.0F, 2.0F, 2.0F, 6.0F, 5.0F).texOffs(44, 27).addBox(6.5F, -5.0F, -3.0F, 2.0F, 7.0F, 7.0F).texOffs(44, 41).addBox(6.0F, -2.0F, 4.0F, 2.0F, 5.0F, 4.0F).texOffs(24, 41).addBox(-4.0F, -4.0F, 7.5F, 8.0F, 7.0F, 2.0F).texOffs(80, 41).addBox(-6.0F, -6.0F, -9.0F, 5.0F, 6.0F, 2.0F).texOffs(94, 41).addBox(2.0F, -5.0F, -9.0F, 5.0F, 6.0F, 2.0F).texOffs(0, 27).addBox(-5.0F, 4.0F, -6.0F, 10.0F, 2.0F, 12.0F), PartPose.offset(0.0F, 15.0F, 0.0F));
        PartDefinition p_tail = p_wool.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(14, 50).addBox(-1.5F, -1.5F, 0.0F, 3.0F, 3.0F, 2.0F), PartPose.offset(0.0F, -2.0F, 9.5F));
        PartDefinition p_head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 50).addBox(-2.5F, -2.0F, -2.0F, 5.0F, 4.0F, 2.0F).texOffs(56, 50).addBox(-2.0F, -3.0F, -1.5F, 4.0F, 1.0F, 2.0F), PartPose.offset(0.0F, 15.0F, -9.5F));
        PartDefinition p_right_ear = p_head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(68, 50).addBox(-1.75F, -0.5F, -0.5F, 2.0F, 1.0F, 1.0F, new CubeDeformation(-0.25F, 0.0F, 0.0F)), PartPose.offsetAndRotation(-2.5F, -1.0F, -1.0F, 0.0F, 0.0F, 0.6F));
        PartDefinition p_left_ear = p_head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(74, 50).addBox(-0.25F, -0.5F, -0.5F, 2.0F, 1.0F, 1.0F, new CubeDeformation(-0.25F, 0.0F, 0.0F)), PartPose.offsetAndRotation(2.5F, -1.0F, -1.0F, 0.0F, 0.0F, -0.6F));
        PartDefinition p_leg_fr = root.addOrReplaceChild("leg_fr", CubeListBuilder.create().texOffs(24, 50).addBox(-1.0F, -0.25F, -1.0F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F, -0.25F, 0.0F)), PartPose.offset(-3.0F, 21.5F, -4.5F));
        PartDefinition p_leg_fl = root.addOrReplaceChild("leg_fl", CubeListBuilder.create().texOffs(32, 50).addBox(-1.0F, -0.25F, -1.0F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F, -0.25F, 0.0F)), PartPose.offset(3.0F, 21.5F, -4.5F));
        PartDefinition p_leg_br = root.addOrReplaceChild("leg_br", CubeListBuilder.create().texOffs(40, 50).addBox(-1.0F, -0.25F, -1.0F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F, -0.25F, 0.0F)), PartPose.offset(-3.0F, 21.5F, 4.5F));
        PartDefinition p_leg_bl = root.addOrReplaceChild("leg_bl", CubeListBuilder.create().texOffs(48, 50).addBox(-1.0F, -0.25F, -1.0F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F, -0.25F, 0.0F)), PartPose.offset(3.0F, 21.5F, 4.5F));
        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void setupAnim(DreamMobRenderState state) {
        super.setupAnim(state);
        float pos = state.walkAnimationPos, speed = state.walkAnimationSpeed, age = state.ageInTicks;
        // pezinhos rápidos; a nuvem quica e "respira"
        this.legFr.xRot = Mth.cos(pos * 1.2F) * 1.2F * speed;
        this.legBl.xRot = this.legFr.xRot;
        this.legFl.xRot = Mth.cos(pos * 1.2F + Mth.PI) * 1.2F * speed;
        this.legBr.xRot = this.legFl.xRot;
        float bob = Math.abs(Mth.sin(pos * 0.6F)) * 1.2F * speed;
        float breathe = Mth.sin(age * 0.08F) * 0.025F;
        this.wool.y -= bob;
        this.wool.xScale = this.wool.zScale = 1.0F + breathe;
        this.wool.yScale = 1.0F - breathe;
        this.body.y -= bob;
        this.head.y -= bob;
        this.head.yRot = state.yRot * Mth.DEG_TO_RAD * 0.5F;
        this.head.xRot = state.xRot * Mth.DEG_TO_RAD * 0.5F;
        this.head.zRot = Mth.sin(age * 0.05F) * 0.08F;
        this.tail.yRot = Mth.sin(age * 0.3F) * 0.3F;
        this.wool.visible = !state.sheared;
        this.body.visible = state.sheared;
        if (state.sheared) {
            // sem a lã a cabeça encosta no corpinho
            this.head.z += 4.0F;
            this.head.y += 1.5F;
        }
    }
}

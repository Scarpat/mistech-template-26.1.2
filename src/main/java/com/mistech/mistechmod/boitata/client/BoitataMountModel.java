package com.mistech.mistechmod.boitata.client;

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

/**
 * Boitatá montaria: o corpo inteiro numa malha só (cabeça, pescoço, corpo e cauda em corrente), que ondula como cobra
 * no chão e como dragão chinês no ar. Geometria e texturas geradas por {@code tools/boitata/mount_model.py};
 * a armadura usa a mesma malha um pouco inflada.
 */
public class BoitataMountModel extends EntityModel<BoitataMountRenderer.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Mistech.MODID, "boitata_mount"), "main");

    private final ModelPart body, neck0, neck1, head, jaw, crestA, crestB, crestC;
    private final ModelPart[] chain;
    private final ModelPart[] posed;

    public BoitataMountModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.neck0 = this.body.getChild("neck0");
        this.neck1 = this.neck0.getChild("neck1");
        this.head = this.neck1.getChild("head");
        this.jaw = this.head.getChild("jaw");
        this.crestA = this.head.getChild("crest_a");
        this.crestB = this.head.getChild("crest_b");
        this.crestC = this.head.getChild("crest_c");
        ModelPart b1 = this.body.getChild("body1");
        ModelPart b2 = b1.getChild("body2");
        ModelPart b3 = b2.getChild("body3");
        ModelPart t0 = b3.getChild("tail0");
        ModelPart t1 = t0.getChild("tail1");
        ModelPart t2 = t1.getChild("tail2");
        ModelPart t3 = t2.getChild("tail3");
        this.chain = new ModelPart[] {b1, b2, b3, t0, t1, t2, t3, t3.getChild("tail_flame")};
        this.posed = new ModelPart[] {this.body, this.neck0, this.neck1, this.head, this.head.getChild("snout"), b1, b2, b3, t0, t1};
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-4.5F, -4.0F, -4.5F, 9.0F, 8.0F, 9.0F), PartPose.offset(0.0F, 20.0F, 0.0F));
        PartDefinition p_fin_body = p_body.addOrReplaceChild("fin_body", CubeListBuilder.create().texOffs(54, 44).addBox(-0.5F, -4.0F, -1.5F, 1.0F, 4.0F, 3.0F), PartPose.offsetAndRotation(0.0F, -4.0F, 0.0F, -0.5F, 0.0F, 0.0F));
        PartDefinition p_neck0 = p_body.addOrReplaceChild("neck0", CubeListBuilder.create().texOffs(62, 17).addBox(-3.5F, -3.5F, -7.0F, 7.0F, 7.0F, 7.0F), PartPose.offsetAndRotation(0.0F, -0.5F, -4.5F, -0.45F, 0.0F, 0.0F));
        PartDefinition p_fin_neck0 = p_neck0.addOrReplaceChild("fin_neck0", CubeListBuilder.create().texOffs(44, 52).addBox(-0.5F, -3.0F, -1.0F, 1.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(0.0F, -3.5F, -3.5F, -0.6F, 0.0F, 0.0F));
        PartDefinition p_neck1 = p_neck0.addOrReplaceChild("neck1", CubeListBuilder.create().texOffs(26, 32).addBox(-3.0F, -3.0F, -6.0F, 6.0F, 6.0F, 6.0F), PartPose.offsetAndRotation(0.0F, 0.0F, -7.0F, -0.25F, 0.0F, 0.0F));
        PartDefinition p_head = p_neck1.addOrReplaceChild("head", CubeListBuilder.create().texOffs(72, 0).addBox(-4.5F, -4.0F, -9.0F, 9.0F, 6.0F, 10.0F), PartPose.offsetAndRotation(0.0F, 0.0F, -6.0F, 0.6F, 0.0F, 0.0F));
        PartDefinition p_brow = p_head.addOrReplaceChild("brow", CubeListBuilder.create().texOffs(0, 52).addBox(-5.0F, -5.0F, -8.5F, 10.0F, 1.0F, 4.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p_snout = p_head.addOrReplaceChild("snout", CubeListBuilder.create().texOffs(74, 32).addBox(-3.0F, -2.5F, -14.0F, 6.0F, 4.0F, 5.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p_right_fang = p_snout.addOrReplaceChild("right_fang", CubeListBuilder.create().texOffs(50, 52).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 2.0F, 1.0F), PartPose.offset(-2.0F, 1.5F, -13.0F));
        PartDefinition p_left_fang = p_snout.addOrReplaceChild("left_fang", CubeListBuilder.create().texOffs(54, 52).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 2.0F, 1.0F), PartPose.offset(2.0F, 1.5F, -13.0F));
        PartDefinition p_right_whisker = p_snout.addOrReplaceChild("right_whisker", CubeListBuilder.create().texOffs(58, 52).addBox(-7.0F, -0.5F, -0.5F, 7.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(-3.0F, 0.0F, -12.0F, 0.0F, 0.6F, 0.25F));
        PartDefinition p_left_whisker = p_snout.addOrReplaceChild("left_whisker", CubeListBuilder.create().texOffs(74, 52).addBox(0.0F, -0.5F, -0.5F, 7.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(3.0F, 0.0F, -12.0F, 0.0F, -0.6F, -0.25F));
        PartDefinition p_jaw = p_head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(90, 17).addBox(-3.5F, 0.0F, -11.0F, 7.0F, 2.0F, 11.0F), PartPose.offsetAndRotation(0.0F, 2.0F, -2.0F, 0.15F, 0.0F, 0.0F));
        PartDefinition p_right_eye = p_head.addOrReplaceChild("right_eye", CubeListBuilder.create().texOffs(70, 44).addBox(-6.0F, -4.5F, -8.0F, 3.0F, 3.0F, 3.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p_left_eye = p_head.addOrReplaceChild("left_eye", CubeListBuilder.create().texOffs(82, 44).addBox(3.0F, -4.5F, -8.0F, 3.0F, 3.0F, 3.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p_crest_a = p_head.addOrReplaceChild("crest_a", CubeListBuilder.create().texOffs(44, 44).addBox(-1.0F, -4.0F, -1.5F, 2.0F, 4.0F, 3.0F), PartPose.offsetAndRotation(0.0F, -4.0F, -6.0F, -0.5F, 0.0F, 0.0F));
        PartDefinition p_crest_b = p_head.addOrReplaceChild("crest_b", CubeListBuilder.create().texOffs(116, 32).addBox(-1.0F, -6.0F, -1.5F, 2.0F, 6.0F, 3.0F), PartPose.offsetAndRotation(0.0F, -4.0F, -3.0F, -0.7F, 0.0F, 0.0F));
        PartDefinition p_crest_c = p_head.addOrReplaceChild("crest_c", CubeListBuilder.create().texOffs(34, 44).addBox(-1.0F, -5.0F, -1.5F, 2.0F, 5.0F, 3.0F), PartPose.offsetAndRotation(0.0F, -4.0F, 0.0F, -0.9F, 0.0F, 0.0F));
        PartDefinition p_body1 = p_body.addOrReplaceChild("body1", CubeListBuilder.create().texOffs(36, 0).addBox(-4.5F, -4.0F, 0.0F, 9.0F, 8.0F, 9.0F), PartPose.offset(0.0F, 0.0F, 4.5F));
        PartDefinition p_fin_body1 = p_body1.addOrReplaceChild("fin_body1", CubeListBuilder.create().texOffs(62, 44).addBox(-0.5F, -4.0F, -1.5F, 1.0F, 4.0F, 3.0F), PartPose.offsetAndRotation(0.0F, -4.0F, 4.5F, -0.5F, 0.0F, 0.0F));
        PartDefinition p_body2 = p_body1.addOrReplaceChild("body2", CubeListBuilder.create().texOffs(0, 17).addBox(-4.0F, -3.5F, 0.0F, 8.0F, 7.0F, 8.0F), PartPose.offset(0.0F, 0.5F, 9.0F));
        PartDefinition p_fin_body2 = p_body2.addOrReplaceChild("fin_body2", CubeListBuilder.create().texOffs(106, 44).addBox(-0.5F, -3.0F, -1.5F, 1.0F, 3.0F, 3.0F), PartPose.offsetAndRotation(0.0F, -3.5F, 4.0F, -0.5F, 0.0F, 0.0F));
        PartDefinition p_body3 = p_body2.addOrReplaceChild("body3", CubeListBuilder.create().texOffs(32, 17).addBox(-3.5F, -3.0F, 0.0F, 7.0F, 6.0F, 8.0F), PartPose.offset(0.0F, 0.5F, 8.0F));
        PartDefinition p_fin_body3 = p_body3.addOrReplaceChild("fin_body3", CubeListBuilder.create().texOffs(114, 44).addBox(-0.5F, -3.0F, -1.5F, 1.0F, 3.0F, 3.0F), PartPose.offsetAndRotation(0.0F, -3.0F, 4.0F, -0.5F, 0.0F, 0.0F));
        PartDefinition p_tail0 = p_body3.addOrReplaceChild("tail0", CubeListBuilder.create().texOffs(0, 32).addBox(-3.0F, -2.5F, 0.0F, 6.0F, 5.0F, 7.0F), PartPose.offset(0.0F, 0.5F, 8.0F));
        PartDefinition p_fin_tail0 = p_tail0.addOrReplaceChild("fin_tail0", CubeListBuilder.create().texOffs(28, 52).addBox(-0.5F, -2.0F, -1.5F, 1.0F, 2.0F, 3.0F), PartPose.offsetAndRotation(0.0F, -2.5F, 3.5F, -0.5F, 0.0F, 0.0F));
        PartDefinition p_tail1 = p_tail0.addOrReplaceChild("tail1", CubeListBuilder.create().texOffs(50, 32).addBox(-2.5F, -2.0F, 0.0F, 5.0F, 4.0F, 7.0F), PartPose.offset(0.0F, 0.5F, 7.0F));
        PartDefinition p_fin_tail1 = p_tail1.addOrReplaceChild("fin_tail1", CubeListBuilder.create().texOffs(36, 52).addBox(-0.5F, -2.0F, -1.5F, 1.0F, 2.0F, 3.0F), PartPose.offsetAndRotation(0.0F, -2.0F, 3.5F, -0.5F, 0.0F, 0.0F));
        PartDefinition p_tail2 = p_tail1.addOrReplaceChild("tail2", CubeListBuilder.create().texOffs(96, 32).addBox(-2.0F, -1.5F, 0.0F, 4.0F, 3.0F, 6.0F), PartPose.offset(0.0F, 0.5F, 7.0F));
        PartDefinition p_tail3 = p_tail2.addOrReplaceChild("tail3", CubeListBuilder.create().texOffs(0, 44).addBox(-1.5F, -1.0F, 0.0F, 3.0F, 2.0F, 6.0F), PartPose.offset(0.0F, 0.5F, 6.0F));
        PartDefinition p_tail_flame = p_tail3.addOrReplaceChild("tail_flame", CubeListBuilder.create().texOffs(18, 44).addBox(-2.0F, -2.0F, 0.0F, 4.0F, 4.0F, 4.0F).texOffs(94, 44).addBox(-1.0F, -1.0F, 4.0F, 2.0F, 2.0F, 4.0F), PartPose.offset(0.0F, 0.0F, 6.0F));
        return LayerDefinition.create(mesh, 128, 64);
    }

    /** Partes que a armadura copia a pose (mesmos nomes do {@link BoitataMountArmorModel}). */
    public ModelPart[] posedParts() {
        return this.posed;
    }

    @Override
    public void setupAnim(BoitataMountRenderer.State state) {
        super.setupAnim(state);
        float t = state.ageInTicks;
        float walk = Math.min(1.0F, state.walkAnimationSpeed * 1.5F);
        float amp, phase;
        if (state.flying) {
            amp = 0.10F;
            phase = t * 0.22F;
        } else {
            amp = 0.05F + 0.30F * walk;
            phase = state.walkAnimationPos * 0.8F + t * 0.04F;
        }
        // ondulação de cobra: cada gomo atrasado em relação ao anterior
        for (int i = 0; i < this.chain.length; i++) {
            float k = i == 0 ? 0.6F : 1.0F;
            this.chain[i].yRot += amp * k * Mth.sin(phase - i * 0.75F);
            if (state.flying) {
                this.chain[i].xRot += 0.09F * Mth.sin(t * 0.2F - i * 0.7F);
            }
        }
        this.body.yRot += amp * 0.25F * Mth.sin(phase + 0.75F);
        this.neck0.yRot -= amp * 0.35F * Mth.sin(phase + 0.75F);
        // cabeça acompanha o olhar
        float yaw = state.yRot * Mth.DEG_TO_RAD;
        float pitch = state.xRot * Mth.DEG_TO_RAD;
        this.neck1.yRot += yaw * 0.35F;
        this.head.yRot += yaw * 0.45F;
        this.head.xRot += pitch * 0.4F;
        if (state.flying) {
            this.neck0.xRot += 0.30F;
            this.neck1.xRot += 0.10F;
            this.head.xRot -= 0.30F;
        }
        this.jaw.xRot += 0.06F + Mth.sin(t * 0.1F) * 0.05F + (state.breathing ? 0.65F : 0.0F);
        this.crestA.yScale = 1.0F + Mth.sin(t * 0.5F) * 0.2F;
        this.crestB.yScale = 1.0F + Mth.sin(t * 0.45F + 1.0F) * 0.25F;
        this.crestC.yScale = 1.0F + Mth.sin(t * 0.55F + 2.0F) * 0.2F;
    }
}

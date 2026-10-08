package com.mistech.mistechmod.client;

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
 * Mal Olhado: quase 3 blocos de altura, pernas finas, corcunda, pescoço comprido pendurado para frente,
 * braços que quase arrastam no chão, capuz esfarrapado e um olho só, enorme, no meio do rosto.
 */
public class EvilEyeModel extends EntityModel<EvilEyeRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Mistech.MODID, "evil_eye"), "main");

    private static final float HUNCH = 0.65F;
    private static final float NECK = 0.55F;
    private static final float HEAD = -1.15F;

    private final ModelPart torso;
    private final ModelPart neck;
    private final ModelPart head;
    private final ModelPart rightArm;
    private final ModelPart leftArm;

    public EvilEyeModel(ModelPart root) {
        super(root);
        this.torso = root.getChild("torso");
        this.neck = this.torso.getChild("neck");
        this.head = this.neck.getChild("head");
        this.rightArm = this.torso.getChild("right_arm");
        this.leftArm = this.torso.getChild("left_arm");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // pernas compridas e finas (do quadril em y=4 até o chão em y=24)
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 32).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 20.0F, 2.0F),
                PartPose.offset(-2.0F, 4.0F, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 32).mirror().addBox(-1.0F, 0.0F, -1.0F, 2.0F, 20.0F, 2.0F),
                PartPose.offset(2.0F, 4.0F, 0.0F));

        // tronco corcunda com o manto
        PartDefinition torso = root.addOrReplaceChild("torso", CubeListBuilder.create()
                .texOffs(16, 16).addBox(-4.0F, -14.0F, -2.0F, 8.0F, 14.0F, 4.0F)
                .texOffs(40, 16).addBox(-4.0F, -14.0F, -2.0F, 8.0F, 16.0F, 4.0F, new CubeDeformation(0.4F)),
                PartPose.offsetAndRotation(0.0F, 4.0F, 0.0F, HUNCH, 0.0F, 0.0F));
        // pescoço comprido pendurado para frente
        PartDefinition neck = torso.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(8, 32).addBox(-1.0F, -6.0F, -1.0F, 2.0F, 6.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, -13.0F, -0.5F, NECK, 0.0F, 0.0F));
        // cabeça + capuz (casca)
        neck.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3.0F, -8.0F, -3.0F, 6.0F, 8.0F, 6.0F)
                .texOffs(24, 0).addBox(-3.0F, -8.0F, -3.0F, 6.0F, 8.0F, 6.0F, new CubeDeformation(0.7F)),
                PartPose.offsetAndRotation(0.0F, -5.5F, 0.0F, HEAD, 0.0F, 0.0F));
        // braços enormes, pendurados
        torso.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(16, 36).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 24.0F, 2.0F),
                PartPose.offsetAndRotation(-5.0F, -12.5F, 0.0F, -HUNCH + 0.1F, 0.0F, 0.12F));
        torso.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(16, 36).mirror().addBox(-1.0F, 0.0F, -1.0F, 2.0F, 24.0F, 2.0F),
                PartPose.offsetAndRotation(5.0F, -12.5F, 0.0F, -HUNCH + 0.1F, 0.0F, -0.12F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(EvilEyeRenderState state) {
        super.setupAnim(state);
        float t = state.ageInTicks;
        // respiração lenta; a cabeça segue o jogador (yRot/xRot da cabeça) e tomba de lado de vez em quando
        this.torso.xRot = HUNCH + Mth.sin(t * 0.05F) * 0.03F;
        this.head.yRot = state.yRot * (Mth.PI / 180.0F) * 0.6F;
        this.neck.yRot = state.yRot * (Mth.PI / 180.0F) * 0.4F;
        this.head.xRot = HEAD - state.xRot * (Mth.PI / 180.0F) * 0.5F;
        this.head.zRot = Mth.sin(t * 0.03F) * 0.18F;
        this.rightArm.zRot = 0.12F + Mth.sin(t * 0.04F) * 0.03F;
        this.leftArm.zRot = -0.12F - Mth.sin(t * 0.04F + 1.0F) * 0.03F;
    }
}

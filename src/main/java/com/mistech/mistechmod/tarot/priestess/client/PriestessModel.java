package com.mistech.mistechmod.tarot.priestess.client;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.tarot.priestess.PriestessFigure;

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
 * A Sacerdotisa: manto azul sobre túnica branca, cruz no peito, rosto pálido sob um véu branco-azulado que cai pelas
 * costas, a coroa de Ísis (dois chifres segurando a lua cheia), o rolo da Torá na mão esquerda e a lua crescente aos
 * pés. Sentada no trono no modo Boaz (as coxas vão para a frente e as canelas descem); de pé no Jachin.
 * <p>
 * Textura 128×128 ({@code textures/entity/the_high_priestess.png} + brilho {@code the_high_priestess_glow.png}),
 * geradas por {@code tools/tarot/priestess/gen_priestess_tex.py}. Desenhada 1,2× maior pelo renderer (~3 blocos).
 */
public class PriestessModel extends EntityModel<PriestessRenderState> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(Mistech.MODID, "the_high_priestess"), "main");

    private final ModelPart torso;
    private final ModelPart head;
    private final ModelPart drape;
    private final ModelPart crown;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart cape;
    private final ModelPart skirtUpper;
    private final ModelPart skirtLower;
    private final ModelPart crescent;

    public PriestessModel(ModelPart root) {
        super(root);
        this.torso = root.getChild("torso");
        this.head = this.torso.getChild("head");
        this.drape = this.head.getChild("drape");
        this.crown = this.head.getChild("crown");
        this.rightArm = this.torso.getChild("right_arm");
        this.leftArm = this.torso.getChild("left_arm");
        this.cape = this.torso.getChild("cape");
        this.skirtUpper = root.getChild("skirt_upper");
        this.skirtLower = this.skirtUpper.getChild("skirt_lower");
        this.crescent = root.getChild("crescent");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // tronco (cintura em y = 10)
        PartDefinition torso = root.addOrReplaceChild("torso", CubeListBuilder.create()
                .texOffs(0, 18).addBox(-4.0F, -12.0F, -2.5F, 8, 12, 5), PartPose.offset(0.0F, 10.0F, 0.0F));

        // cabeça + véu (capuz um pouco maior, com a frente aberta) + o véu que cai pelas costas
        PartDefinition head = torso.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8)
                .texOffs(32, 0).addBox(-4.5F, -8.5F, -4.5F, 9, 9, 9, new CubeDeformation(0.05F)),
                PartPose.offset(0.0F, -12.0F, 0.0F));
        head.addOrReplaceChild("drape", CubeListBuilder.create().texOffs(68, 24).addBox(-4.5F, 0.0F, 0.0F, 9, 12, 1),
                PartPose.offsetAndRotation(0.0F, -7.5F, 4.4F, 0.12F, 0.0F, 0.0F));

        // coroa: aro, dois chifres e a lua cheia entre eles
        PartDefinition crown = head.addOrReplaceChild("crown", CubeListBuilder.create()
                .texOffs(90, 0).addBox(-3.5F, -1.0F, -3.5F, 7, 1, 7)
                .texOffs(90, 18).addBox(-2.5F, -6.6F, -0.5F, 5, 5, 1),
                PartPose.offset(0.0F, -8.5F, 0.0F));
        crown.addOrReplaceChild("horn_left", CubeListBuilder.create().texOffs(90, 10).addBox(-0.5F, -5.0F, -0.5F, 1, 5, 1),
                PartPose.offsetAndRotation(-3.2F, -0.6F, 0.0F, 0.0F, 0.0F, -0.32F));
        crown.addOrReplaceChild("horn_right", CubeListBuilder.create().texOffs(96, 10).addBox(-0.5F, -5.0F, -0.5F, 1, 5, 1),
                PartPose.offsetAndRotation(3.2F, -0.6F, 0.0F, 0.0F, 0.0F, 0.32F));
        crown.addOrReplaceChild("horn_tip_left", CubeListBuilder.create().texOffs(102, 10).addBox(-0.5F, -2.0F, -0.5F, 1, 2, 1),
                PartPose.offsetAndRotation(-4.7F, -5.3F, 0.0F, 0.0F, 0.0F, 0.45F));
        crown.addOrReplaceChild("horn_tip_right", CubeListBuilder.create().texOffs(106, 10).addBox(-0.5F, -2.0F, -0.5F, 1, 2, 1),
                PartPose.offsetAndRotation(4.7F, -5.3F, 0.0F, 0.0F, 0.0F, -0.45F));

        // braços com mangas largas; o esquerdo segura o rolo da Torá
        torso.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(40, 20).addBox(-3.0F, -1.0F, -2.0F, 4, 11, 4),
                PartPose.offset(-5.0F, -10.0F, 0.0F));
        PartDefinition leftArm = torso.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(40, 50)
                .addBox(-1.0F, -1.0F, -2.0F, 4, 11, 4), PartPose.offset(5.0F, -10.0F, 0.0F));
        leftArm.addOrReplaceChild("scroll", CubeListBuilder.create().texOffs(40, 36).addBox(-1.0F, -1.0F, -4.0F, 2, 2, 8),
                PartPose.offsetAndRotation(1.0F, 9.5F, -1.0F, 0.0F, 0.0F, 0.0F));

        // manto que cai das costas
        torso.addOrReplaceChild("cape", CubeListBuilder.create().texOffs(68, 0).addBox(-5.0F, 0.0F, 0.0F, 10, 22, 1),
                PartPose.offsetAndRotation(0.0F, -12.0F, 2.6F, 0.06F, 0.0F, 0.0F));

        // saia em duas partes (de pé: tudo para baixo; sentada: coxas para a frente, canelas para baixo)
        PartDefinition skirtUpper = root.addOrReplaceChild("skirt_upper", CubeListBuilder.create()
                .texOffs(0, 36).addBox(-4.5F, 0.0F, -3.0F, 9, 7, 6), PartPose.offset(0.0F, 10.0F, 0.0F));
        skirtUpper.addOrReplaceChild("skirt_lower", CubeListBuilder.create()
                .texOffs(0, 50).addBox(-5.5F, 0.0F, -4.0F, 11, 7, 8), PartPose.offset(0.0F, 7.0F, 0.0F));

        // lua crescente aos pés (arco na frente)
        root.addOrReplaceChild("crescent", CubeListBuilder.create()
                .texOffs(90, 26).addBox(-8.0F, -0.5F, -3.0F, 2, 1, 4)
                .texOffs(90, 26).addBox(-6.5F, -0.5F, -6.0F, 3, 1, 3)
                .texOffs(90, 26).addBox(-3.5F, -0.5F, -8.0F, 7, 1, 2)
                .texOffs(90, 26).addBox(3.5F, -0.5F, -6.0F, 3, 1, 3)
                .texOffs(90, 26).addBox(6.0F, -0.5F, -3.0F, 2, 1, 4),
                PartPose.offset(0.0F, 23.0F, 0.0F));

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(PriestessRenderState s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        float a = s.actionTime;
        float breathe = Mth.sin(t * 0.08F);

        // cabeça segue o olhar (sentada, só um pouco)
        this.head.yRot = s.yRot * Mth.DEG_TO_RAD * (s.seated ? 0.4F : 1.0F);
        this.head.xRot = s.xRot * Mth.DEG_TO_RAD * 0.6F;
        this.head.zRot = 0.0F;
        this.drape.xRot = 0.12F + Mth.sin(t * 0.07F) * 0.04F;
        this.cape.xRot = 0.06F + Mth.sin(t * 0.06F) * 0.03F;
        this.crown.yRot = 0.0F;

        this.torso.xRot = 0.0F;
        this.torso.y = 10.0F + breathe * 0.25F;
        this.rightArm.yRot = 0.0F;
        this.leftArm.yRot = 0.0F;

        if (s.seated) {
            // sentada no trono: coxas para frente, canelas para baixo, mãos no colo segurando o rolo
            this.skirtUpper.xRot = -Mth.HALF_PI;
            this.skirtLower.xRot = Mth.HALF_PI;
            this.cape.xRot = 0.25F;
            this.rightArm.xRot = -0.85F + breathe * 0.03F;
            this.rightArm.zRot = -0.12F;
            this.leftArm.xRot = -0.85F - breathe * 0.03F;
            this.leftArm.zRot = 0.12F;
            this.crescent.visible = false;
            // olhar perdido, cabeça inclinando devagar (introspecção)
            this.head.zRot = Mth.sin(t * 0.03F) * 0.06F;
        } else {
            this.skirtUpper.xRot = 0.0F;
            this.skirtLower.xRot = 0.0F;
            this.rightArm.xRot = -0.3F + Mth.sin(t * 0.07F) * 0.05F;
            this.rightArm.zRot = 0.08F;
            this.leftArm.xRot = -0.7F + Mth.sin(t * 0.07F + 1.0F) * 0.04F;
            this.leftArm.zRot = -0.05F;
            this.crescent.visible = true;
            this.crescent.yRot = Mth.sin(t * 0.04F) * 0.08F;
            // a saia balança de leve, como tecido na água
            this.skirtLower.zRot = Mth.sin(t * 0.09F) * 0.03F;
        }

        switch (s.action) {
            case PriestessFigure.CAST -> {
                // estende a mão direita para a frente e "solta" a gota
                float p = Mth.clamp(a / 10.0F, 0.0F, 1.0F);
                float back = Mth.clamp((a - 12.0F) / 8.0F, 0.0F, 1.0F);
                this.rightArm.xRot = Mth.lerp(back, Mth.lerp(p, this.rightArm.xRot, -1.65F), -0.3F);
                this.rightArm.zRot = 0.0F;
            }
            case PriestessFigure.OMEN -> {
                // ergue as duas mãos devagar, palmas para cima
                float p = Mth.clamp(a / 8.0F, 0.0F, 1.0F) * (1.0F - Mth.clamp((a - 16.0F) / 6.0F, 0.0F, 1.0F));
                this.rightArm.xRot = Mth.lerp(p, this.rightArm.xRot, -2.1F);
                this.leftArm.xRot = Mth.lerp(p, this.leftArm.xRot, -1.4F);
                this.rightArm.zRot = Mth.lerp(p, this.rightArm.zRot, 0.35F);
                this.head.xRot = Mth.lerp(p, this.head.xRot, -0.35F);
            }
            default -> {}
        }
    }
}

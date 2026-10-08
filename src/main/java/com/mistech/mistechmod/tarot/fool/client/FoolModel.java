package com.mistech.mistechmod.tarot.fool.client;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.tarot.fool.FoolEntity;

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
 * O Louco: gênio do vento. Tronco com túnica de arlequim, cabeça com chapéu de bobo (três pontas caídas com guizos),
 * braços finos, cajado com uma estrela na ponta na mão direita e, no lugar das pernas, uma cauda de vento em espiral.
 * <p>
 * Textura 128×64 ({@code textures/entity/the_fool.png}, gerada por {@code tools/tarot/gen_fool_tex.py}; o chapéu por
 * {@code tools/tarot/gen_fool_designs.py}).
 */
public class FoolModel extends EntityModel<FoolRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Mistech.MODID, "the_fool"), "main");

    private final ModelPart body;
    private final ModelPart head;
    private final JesterCap.Parts cap;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart staff;
    private final ModelPart tail1;
    private final ModelPart tail2;
    private final ModelPart tail3;
    private final ModelPart tail4;

    public FoolModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.cap = JesterCap.Parts.of(this.head.getChild("hat"));
        this.rightArm = this.body.getChild("right_arm");
        this.leftArm = this.body.getChild("left_arm");
        this.staff = this.rightArm.getChild("staff");
        this.tail1 = root.getChild("tail1");
        this.tail2 = this.tail1.getChild("tail2");
        this.tail3 = this.tail2.getChild("tail3");
        this.tail4 = this.tail3.getChild("tail4");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // tronco (cintura em y = 4; tronco vai até y = -8)
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 26).addBox(-4.0F, -12.0F, -2.5F, 8, 12, 5)
                .texOffs(72, 0).addBox(-4.5F, -1.5F, -3.0F, 9, 2, 6)                       // faixa da cintura
                .texOffs(72, 8).addBox(-5.0F, -13.0F, -3.0F, 10, 2, 6, new CubeDeformation(0.1F)), // gola de babados
                PartPose.offset(0.0F, 4.0F, 0.0F));

        // cabeça + chapéu
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8),
                PartPose.offset(0.0F, -12.0F, 0.0F));
        // chapéu de bobo da corte (textura a partir de 64,16)
        JesterCap.build(head, "hat", 64, 16, 0.15F);

        // braços finos; o direito segura o cajado
        PartDefinition rightArm = body.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(28, 26)
                .addBox(-2.0F, -1.5F, -1.5F, 3, 11, 3), PartPose.offsetAndRotation(-5.0F, -10.5F, 0.0F, -0.35F, 0.0F, 0.1F));
        body.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(28, 26).mirror()
                .addBox(-1.0F, -1.5F, -1.5F, 3, 11, 3), PartPose.offsetAndRotation(5.0F, -10.5F, 0.0F, 0.0F, 0.0F, -0.1F));
        // cajado: haste comprida na vertical, estrela de 3 cubos na ponta
        PartDefinition staff = rightArm.addOrReplaceChild("staff", CubeListBuilder.create()
                .texOffs(44, 26).addBox(-0.5F, -15.0F, -0.5F, 1, 24, 1)
                .texOffs(48, 26).addBox(-1.5F, -18.0F, -1.5F, 3, 3, 3)
                .texOffs(48, 32).addBox(-2.5F, -17.0F, -0.5F, 5, 1, 1)
                .texOffs(48, 34).addBox(-0.5F, -19.5F, -0.5F, 1, 5, 1),
                PartPose.offset(-0.5F, 8.5F, 0.0F));

        // cauda de vento (no lugar das pernas), afinando até a ponta
        PartDefinition tail1 = root.addOrReplaceChild("tail1", CubeListBuilder.create().texOffs(0, 44).addBox(-3.5F, 0.0F, -2.5F, 7, 6, 5),
                PartPose.offset(0.0F, 4.0F, 0.0F));
        PartDefinition tail2 = tail1.addOrReplaceChild("tail2", CubeListBuilder.create().texOffs(24, 44).addBox(-2.5F, 0.0F, -2.0F, 5, 5, 4),
                PartPose.offset(0.0F, 5.5F, 0.0F));
        PartDefinition tail3 = tail2.addOrReplaceChild("tail3", CubeListBuilder.create().texOffs(44, 44).addBox(-1.5F, 0.0F, -1.5F, 3, 5, 3),
                PartPose.offset(0.0F, 4.5F, 0.0F));
        tail3.addOrReplaceChild("tail4", CubeListBuilder.create().texOffs(56, 44).addBox(-0.5F, 0.0F, -0.5F, 1, 4, 1),
                PartPose.offset(0.0F, 4.5F, 0.0F));

        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void setupAnim(FoolRenderState s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        float a = s.actionTime;
        float spin = s.phase2 ? 1.6F : 1.0F;

        // flutua para cima e para baixo; a cauda gira e balança
        float bob = Mth.sin(t * 0.12F) * 1.2F;
        this.body.y = 4.0F + bob;
        this.tail1.y = 4.0F + bob;
        this.tail1.yRot = t * 0.25F * spin;
        this.tail2.yRot = t * 0.2F * spin;
        this.tail3.yRot = t * 0.25F * spin;
        this.tail1.zRot = Mth.sin(t * 0.15F) * 0.12F;
        this.tail2.zRot = Mth.sin(t * 0.15F + 0.8F) * 0.2F;
        this.tail3.zRot = Mth.sin(t * 0.15F + 1.6F) * 0.3F;
        this.tail4.zRot = Mth.sin(t * 0.15F + 2.4F) * 0.4F;

        // cabeça segue o olhar; pontas do chapéu balançam
        this.head.yRot = s.yRot * Mth.DEG_TO_RAD;
        this.head.xRot = s.xRot * Mth.DEG_TO_RAD;
        this.head.zRot = 0.0F;
        this.cap.animate(t, s.action == FoolEntity.HOP || s.action == FoolEntity.GALE ? 2.5F : s.action == FoolEntity.DIZZY ? 1.8F : 1.0F);

        // pose base
        this.body.xRot = 0.0F;
        this.body.yRot = 0.0F;
        this.body.zRot = 0.0F;
        this.rightArm.xRot = -0.35F + Mth.sin(t * 0.1F) * 0.05F;
        this.rightArm.yRot = 0.0F;
        this.rightArm.zRot = 0.1F;
        this.leftArm.xRot = Mth.sin(t * 0.13F) * 0.25F;
        this.leftArm.yRot = 0.0F;
        this.leftArm.zRot = -0.25F - Mth.sin(t * 0.1F) * 0.15F;
        float staffTilt = 0.1F;

        switch (s.action) {
            case FoolEntity.HOP -> {
                // braços abertos, inclinado para frente, como quem salta de brincadeira
                this.body.xRot = 0.35F;
                this.rightArm.zRot = 1.1F;
                this.rightArm.xRot = -0.6F;
                this.leftArm.zRot = -1.3F;
                this.tail1.xRot = -0.4F;
            }
            case FoolEntity.CAST -> {
                // ergue o cajado e gira; no fim aponta para a frente
                float p = Mth.clamp(a / 14.0F, 0.0F, 1.0F);
                this.rightArm.xRot = p < 0.75F ? Mth.lerp(p / 0.75F, -0.35F, -2.9F) : Mth.lerp((p - 0.75F) / 0.25F, -2.9F, -1.4F);
                this.rightArm.zRot = 0.1F;
                staffTilt = p < 0.75F ? 0.0F : 1.3F * (p - 0.75F) / 0.25F;
                this.leftArm.zRot = -0.9F - Mth.sin(t * 0.6F) * 0.2F;
                this.body.yRot = Mth.sin(a * 0.5F) * 0.15F;
            }
            case FoolEntity.SWIPE -> {
                float p = Mth.clamp(a / 10.0F, 0.0F, 1.0F);
                this.rightArm.xRot = Mth.lerp(p, -2.2F, 0.4F);
                this.rightArm.yRot = Mth.lerp(p, 0.9F, -0.9F);
                this.body.yRot = Mth.lerp(p, 0.4F, -0.5F);
                staffTilt = 1.3F;
            }
            case FoolEntity.DIZZY -> {
                // tonto: cabeça rodando, corpo murcho, braços pendurados
                this.head.zRot = Mth.sin(t * 0.45F) * 0.35F;
                this.head.xRot = 0.25F;
                this.body.zRot = Mth.sin(t * 0.3F) * 0.15F;
                this.body.xRot = 0.3F;
                this.rightArm.xRot = 0.2F;
                this.leftArm.xRot = 0.2F;
                this.leftArm.zRot = -0.1F;
                this.tail1.xRot = 0.6F;
            }
            case FoolEntity.GALE -> {
                // ciranda: gira o corpo inteiro com os braços abertos
                this.body.yRot = a * 0.7F;
                this.rightArm.zRot = 1.4F;
                this.leftArm.zRot = -1.4F;
                this.rightArm.xRot = 0.0F;
            }
            default -> this.tail1.xRot = 0.0F;
        }
        if (s.action != FoolEntity.HOP && s.action != FoolEntity.DIZZY) {
            this.tail1.xRot = 0.0F;
        }
        // o cajado fica em pé (desfaz a rotação do braço) e só inclina para frente nos golpes
        this.staff.xRot = -this.rightArm.xRot + staffTilt;
        this.staff.yRot = -this.rightArm.yRot;
        this.staff.zRot = -this.rightArm.zRot;
    }
}

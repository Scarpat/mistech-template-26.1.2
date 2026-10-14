package com.mistech.mistechmod.boitata.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;

import com.mistech.mistechmod.Mistech;

/**
 * Armadura de montaria do Boitatá: peças 3D (elmo com chifres, focinheira, colares, sela com estribos, placas de dorso com
 * espinhos, anéis na cauda) numa corrente igual à do corpo; copia a pose do {@link BoitataMountModel} a cada quadro.
 * Geometria e texturas: {@code tools/boitata/mount_armor.py}.
 */
public class BoitataMountArmorModel extends EntityModel<BoitataMountRenderer.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Mistech.MODID, "boitata_mount_armor"), "main");

    private final ModelPart[] posed;

    public BoitataMountArmorModel(ModelPart root) {
        super(root);
        ModelPart body = root.getChild("body");
        ModelPart neck0 = body.getChild("neck0");
        ModelPart neck1 = neck0.getChild("neck1");
        ModelPart head = neck1.getChild("head");
        ModelPart b1 = body.getChild("body1");
        ModelPart b2 = b1.getChild("body2");
        ModelPart b3 = b2.getChild("body3");
        ModelPart t0 = b3.getChild("tail0");
        ModelPart t1 = t0.getChild("tail1");
        this.posed = new ModelPart[] {body, neck0, neck1, head, head.getChild("snout"), b1, b2, b3, t0, t1};
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(78, 22).addBox(-4.0F, -5.2F, -3.5F, 8.0F, 1.2F, 7.0F).texOffs(68, 39).addBox(-2.0F, -7.2F, -4.2F, 4.0F, 2.0F, 1.5F).texOffs(34, 39).addBox(-3.5F, -7.5F, 2.5F, 7.0F, 2.5F, 1.5F).texOffs(104, 0).addBox(-5.2F, -4.2F, -3.0F, 0.8F, 5.0F, 6.0F).texOffs(0, 11).addBox(4.4F, -4.2F, -3.0F, 0.8F, 5.0F, 6.0F).texOffs(48, 31).addBox(-5.6F, -3.5F, -0.5F, 0.6F, 7.0F, 1.0F).texOffs(24, 44).addBox(-6.3F, 3.5F, -1.0F, 1.3F, 0.8F, 2.0F).texOffs(52, 31).addBox(5.0F, -3.5F, -0.5F, 0.6F, 7.0F, 1.0F).texOffs(30, 44).addBox(5.0F, 3.5F, -1.0F, 1.3F, 0.8F, 2.0F).texOffs(0, 44).addBox(-4.8F, -4.6F, -4.8F, 9.6F, 0.8F, 1.6F), PartPose.offset(0.0F, 20.0F, 0.0F));
        PartDefinition p_neck0 = p_body.addOrReplaceChild("neck0", CubeListBuilder.create().texOffs(14, 11).addBox(-4.0F, -4.0F, -5.5F, 8.0F, 8.0F, 2.0F).texOffs(0, 39).addBox(-3.8F, -4.4F, -3.5F, 7.6F, 1.0F, 3.5F).texOffs(80, 39).addBox(-0.5F, -6.0F, -3.0F, 1.0F, 2.0F, 2.0F), PartPose.offsetAndRotation(0.0F, -0.5F, -4.5F, -0.45F, 0.0F, 0.0F));
        PartDefinition p_neck1 = p_neck0.addOrReplaceChild("neck1", CubeListBuilder.create().texOffs(98, 11).addBox(-3.5F, -3.5F, -4.5F, 7.0F, 7.0F, 1.8F).texOffs(86, 39).addBox(-0.5F, -5.2F, -3.5F, 1.0F, 2.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.0F, -7.0F, -0.25F, 0.0F, 0.0F));
        PartDefinition p_head = p_neck1.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.8F, -5.0F, -9.4F, 9.6F, 1.2F, 10.0F).texOffs(52, 39).addBox(-3.5F, -4.6F, -9.9F, 7.0F, 3.0F, 0.8F).texOffs(40, 0).addBox(-5.3F, -3.6F, -8.0F, 0.8F, 4.0F, 7.0F).texOffs(56, 0).addBox(4.5F, -3.6F, -8.0F, 0.8F, 4.0F, 7.0F).texOffs(0, 22).addBox(-0.6F, -6.2F, -8.5F, 1.2F, 1.2F, 8.0F), PartPose.offsetAndRotation(0.0F, 0.0F, -6.0F, 0.6F, 0.0F, 0.0F));
        PartDefinition p_horn_r = p_head.addOrReplaceChild("horn_r", CubeListBuilder.create().texOffs(96, 31).addBox(-0.9F, -4.0F, -0.9F, 1.8F, 4.0F, 1.8F), PartPose.offsetAndRotation(-3.2F, -4.6F, -1.0F, 0.7F, -0.35F, 0.0F));
        PartDefinition p_horn_tip_r = p_horn_r.addOrReplaceChild("horn_tip_r", CubeListBuilder.create().texOffs(110, 39).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, -4.0F, 0.0F, 0.45F, 0.0F, 0.0F));
        PartDefinition p_horn_l = p_head.addOrReplaceChild("horn_l", CubeListBuilder.create().texOffs(104, 31).addBox(-0.9F, -4.0F, -0.9F, 1.8F, 4.0F, 1.8F), PartPose.offsetAndRotation(3.2F, -4.6F, -1.0F, 0.7F, 0.35F, 0.0F));
        PartDefinition p_horn_tip_l = p_horn_l.addOrReplaceChild("horn_tip_l", CubeListBuilder.create().texOffs(114, 39).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, -4.0F, 0.0F, 0.45F, 0.0F, 0.0F));
        PartDefinition p_snout = p_head.addOrReplaceChild("snout", CubeListBuilder.create().texOffs(72, 31).addBox(-3.4F, -3.1F, -13.6F, 6.8F, 0.8F, 4.6F).texOffs(24, 39).addBox(-0.5F, -3.9F, -13.2F, 1.0F, 0.8F, 4.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p_body1 = p_body.addOrReplaceChild("body1", CubeListBuilder.create().texOffs(62, 11).addBox(-4.8F, -4.9F, 0.6F, 9.6F, 1.0F, 7.8F).texOffs(72, 0).addBox(-5.1F, -4.0F, 1.2F, 0.6F, 4.4F, 6.6F).texOffs(88, 0).addBox(4.5F, -4.0F, 1.2F, 0.6F, 4.4F, 6.6F).texOffs(92, 39).addBox(-0.5F, -6.8F, 2.5F, 1.0F, 2.0F, 2.5F), PartPose.offset(0.0F, 0.0F, 4.5F));
        PartDefinition p_body2 = p_body1.addOrReplaceChild("body2", CubeListBuilder.create().texOffs(46, 22).addBox(-4.3F, -4.4F, 0.6F, 8.6F, 1.0F, 6.8F).texOffs(34, 11).addBox(-4.6F, -3.5F, 1.2F, 0.6F, 3.8500000000000005F, 5.6F).texOffs(48, 11).addBox(4.0F, -3.5F, 1.2F, 0.6F, 3.8500000000000005F, 5.6F).texOffs(98, 39).addBox(-0.5F, -6.3F, 2.0F, 1.0F, 2.0F, 2.5F), PartPose.offset(0.0F, 0.5F, 9.0F));
        PartDefinition p_body3 = p_body2.addOrReplaceChild("body3", CubeListBuilder.create().texOffs(0, 31).addBox(-3.8F, -3.9F, 0.6F, 7.6F, 1.0F, 6.8F).texOffs(18, 22).addBox(-4.1F, -3.0F, 1.2F, 0.6F, 3.3000000000000003F, 5.6F).texOffs(32, 22).addBox(3.5F, -3.0F, 1.2F, 0.6F, 3.3000000000000003F, 5.6F).texOffs(104, 39).addBox(-0.5F, -5.8F, 2.0F, 1.0F, 2.0F, 2.5F), PartPose.offset(0.0F, 0.5F, 8.0F));
        PartDefinition p_tail0 = p_body3.addOrReplaceChild("tail0", CubeListBuilder.create().texOffs(30, 31).addBox(-3.4F, -2.9F, 2.3F, 6.8F, 5.8F, 2.4F), PartPose.offset(0.0F, 0.5F, 8.0F));
        PartDefinition p_tail1 = p_tail0.addOrReplaceChild("tail1", CubeListBuilder.create().texOffs(56, 31).addBox(-2.9F, -2.4F, 2.3F, 5.8F, 4.8F, 2.4F), PartPose.offset(0.0F, 0.5F, 7.0F));
        PartDefinition p_tail2 = p_tail1.addOrReplaceChild("tail2", CubeListBuilder.create(), PartPose.offset(0.0F, 0.5F, 7.0F));
        PartDefinition p_tail3 = p_tail2.addOrReplaceChild("tail3", CubeListBuilder.create(), PartPose.offset(0.0F, 0.5F, 6.0F));
        return LayerDefinition.create(mesh, 128, 64);
    }

    private BoitataMountModel source;

    /** O corpo de onde a pose é copiada. */
    public void bindSource(BoitataMountModel source) {
        this.source = source;
    }

    /**
     * O coletor chama isto logo antes de desenhar a armadura. Antes a armadura só copiava a pose na hora de enviar e depois o
     * setupAnim padrão zerava tudo — a armadura ficava parada na pose de repouso enquanto a cabeça do corpo virava/olhava
     * (a cabeça "saía" da armadura). Agora recalcula a animação do corpo para este estado e copia.
     */
    @Override
    public void setupAnim(BoitataMountRenderer.State state) {
        super.setupAnim(state);
        if (this.source != null) {
            this.source.setupAnim(state);
            this.copyPose(this.source);
        }
    }

    /** Copia a pose do corpo (mesma ordem de {@link BoitataMountModel#posedParts()}). */
    public void copyPose(BoitataMountModel source) {
        ModelPart[] from = source.posedParts();
        for (int i = 0; i < this.posed.length && i < from.length; i++) {
            this.posed[i].loadPose(from[i].storePose());
        }
    }
}

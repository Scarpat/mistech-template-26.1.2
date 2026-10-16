package com.mistech.mistechmod.realm.dream.client;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.client.GnomeModel;
import com.mistech.mistechmod.client.GnomeRenderState;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Variantes do Gnomo, montadas em cima da malha dele ({@link GnomeModel#createMesh()}); as peças novas usam a área livre da textura
 * (x ≥ 36), pintada por {@code tools/dream/mobs.py}.
 * <ul>
 * <li><b>Mercador</b>: mochilão com saco de dormir e panelinha; o cristal do cajado vira um lampião.</li>
 * <li><b>Corrompido</b>: espinhos negros nas costas, hera-insone pendurada no chapéu, cajado quebrado com cristal vermelho e mão de garra.</li>
 * </ul>
 */
public final class GnomeVariantModels {
    public static final ModelLayerLocation MERCHANT = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Mistech.MODID, "gnome_merchant"), "main");
    public static final ModelLayerLocation CORRUPTED = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Mistech.MODID, "corrupted_gnome"), "main");

    public static final ModelLayerLocation MINER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Mistech.MODID, "gnome_miner"), "main");

    private GnomeVariantModels() {}

    /**
     * Minerador: o chapéu vira capacete de mineiro com lanterna (a pontinha do gorro vermelho escapa por um furo em cima), o cajado
     * vira picareta e nas costas vai um saco de minério.
     */
    public static LayerDefinition miner() {
        MeshDefinition mesh = GnomeModel.createMesh();
        PartDefinition root = mesh.getRoot();
        PartDefinition hat = root.addOrReplaceChild("Hat", CubeListBuilder.create()
                .texOffs(36, 0).addBox(-3.0F, -3.0F, -3.0F, 6.0F, 3.0F, 6.0F)
                .texOffs(36, 10).addBox(-3.5F, 0.0F, -3.5F, 7.0F, 1.0F, 7.0F), PartPose.offset(0.0F, 18.0F, -1.0F));
        hat.addOrReplaceChild("lamp", CubeListBuilder.create().texOffs(36, 18).addBox(-1.0F, -2.0F, -4.0F, 2.0F, 2.0F, 1.0F), PartPose.ZERO);
        hat.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(44, 18).addBox(-1.0F, -3.0F, -1.0F, 2.0F, 3.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, -3.0F, 0.5F, -0.35F, 0.0F, 0.2F));
        PartDefinition face = root.getChild("Face");
        face.addOrReplaceChild("sack", CubeListBuilder.create().texOffs(36, 24).addBox(-2.0F, -3.0F, 4.5F, 4.0F, 4.0F, 3.0F), PartPose.ZERO);
        face.addOrReplaceChild("ore_0", CubeListBuilder.create().texOffs(50, 24).addBox(-1.5F, -4.0F, 6.0F, 1.0F, 1.0F, 1.0F), PartPose.ZERO);
        face.addOrReplaceChild("ore_1", CubeListBuilder.create().texOffs(54, 24).addBox(0.3F, -4.0F, 5.5F, 1.0F, 1.0F, 1.0F), PartPose.ZERO);
        PartDefinition hand = root.getChild("Hand");
        hand.addOrReplaceChild("cube_r8", CubeListBuilder.create().texOffs(22, 25).addBox(-0.5F, -7.0F, -0.5F, 1.0F, 7.0F, 1.0F),
                PartPose.offsetAndRotation(-0.5F, 1.0F, 0.0F, 0.25F, 0.0F, -0.45F));
        // cabeça da picareta presa na ponta do cabo (filha do cabo: acompanha a inclinação)
        hand.getChild("cube_r8").addOrReplaceChild("pick_head", CubeListBuilder.create().texOffs(52, 18).addBox(-0.5F, -0.5F, -2.5F, 1.0F, 1.0F, 5.0F),
                PartPose.offset(0.0F, -7.0F, 0.0F));
        // sem o cristal do cajado
        hand.addOrReplaceChild("cube_r9", CubeListBuilder.create(), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }

    public static LayerDefinition merchant() {
        MeshDefinition mesh = GnomeModel.createMesh();
        PartDefinition root = mesh.getRoot();
        PartDefinition face = root.getChild("Face");
        face.addOrReplaceChild("backpack", CubeListBuilder.create().texOffs(36, 0).addBox(-2.5F, -4.0F, 4.0F, 5.0F, 5.0F, 3.0F), PartPose.ZERO);
        face.addOrReplaceChild("bedroll", CubeListBuilder.create().texOffs(36, 10).addBox(-3.0F, -6.0F, 5.0F, 6.0F, 2.0F, 2.0F), PartPose.ZERO);
        face.addOrReplaceChild("pot", CubeListBuilder.create().texOffs(54, 10).addBox(2.5F, -2.0F, 5.0F, 2.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.15F));
        PartDefinition hand = root.getChild("Hand");
        // tampa do lampião em cima do "cristal" (pintado como vidro amarelo)
        hand.addOrReplaceChild("lantern_cap", CubeListBuilder.create().texOffs(54, 16).addBox(-1.5F, -3.0F, -1.5F, 2.0F, 1.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, -8.0F, -2.0F, 0.3927F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    public static LayerDefinition corrupted() {
        MeshDefinition mesh = GnomeModel.createMesh();
        PartDefinition root = mesh.getRoot();
        PartDefinition face = root.getChild("Face");
        float[][] thorns = {{-1.2F, -3.0F, -0.9F, 0.2F}, {0.8F, -1.5F, -0.7F, -0.3F}, {-0.2F, 0.0F, -1.1F, 0.1F}};
        for (int i = 0; i < thorns.length; i++) {
            face.addOrReplaceChild("thorn_" + i, CubeListBuilder.create().texOffs(36 + i * 4, 20).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F),
                    PartPose.offsetAndRotation(thorns[i][0], thorns[i][1], 4.0F, thorns[i][2], 0.0F, thorns[i][3]));
        }
        PartDefinition hat = root.getChild("Hat");
        float[][] ivy = {{-3.5F, -1.5F, 4.0F}, {1.5F, -4.5F, 3.0F}, {-2.0F, 0.5F, 5.0F}};
        for (int i = 0; i < ivy.length; i++) {
            hat.addOrReplaceChild("ivy_" + i, CubeListBuilder.create().texOffs(48 + i * 4, 20).addBox(-0.5F, 0.0F, -0.5F, 1.0F, ivy[i][2], 1.0F),
                    PartPose.offset(ivy[i][0], 5.0F, ivy[i][1]));
        }
        // cajado quebrado: metade do cabo e o cristal torto, rachado (brilha vermelho)
        PartDefinition hand = root.getChild("Hand");
        hand.addOrReplaceChild("cube_r8", CubeListBuilder.create().texOffs(22, 25).addBox(0.0F, -6.0F, 0.0F, 1.0F, 6.0F, 1.0F),
                PartPose.offsetAndRotation(-1.0F, 2.0F, 0.0F, 0.3054F, 0.0F, 0.0F));
        hand.addOrReplaceChild("cube_r9", CubeListBuilder.create().texOffs(16, 19).addBox(-2.0F, -2.0F, -2.0F, 3.0F, 3.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, -4.0F, -1.5F, 0.9F, 0.3F, 0.5F));
        // mão de garra
        PartDefinition claw = root.getChild("Hand2");
        for (int i = 0; i < 3; i++) {
            claw.addOrReplaceChild("claw_" + i, CubeListBuilder.create().texOffs(36 + i * 4, 26).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 2.0F, 1.0F),
                    PartPose.offsetAndRotation(-0.7F + i * 0.7F, 0.0F, -0.6F, -0.5F, 0.0F, (i - 1) * 0.25F));
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    /** Gnomo Corrompido: o mesmo andar, mas com tremedeira — de vez em quando o corpo todo dá trancos para os lados. */
    public static class Corrupted extends GnomeModel {
        private final ModelPart[] all;
        private final ModelPart hat;

        public Corrupted(ModelPart root) {
            super(root);
            this.hat = root.getChild("Hat");
            this.all = new ModelPart[] {root.getChild("Face"), this.hat, root.getChild("Hand"), root.getChild("Hand2")};
        }

        @Override
        public void setupAnim(GnomeRenderState state) {
            super.setupAnim(state);
            float t = state.ageInTicks;
            // trancos curtos de vez em quando (determinístico pelo tempo)
            float jerk = Mth.sin(t * 0.37F) > 0.9F ? Mth.sin(t * 9.0F) * 0.6F : 0.0F;
            for (ModelPart part : this.all) {
                part.x += jerk;
            }
            this.hat.zRot += Mth.sin(t * 1.7F) * 0.03F;
        }
    }
}

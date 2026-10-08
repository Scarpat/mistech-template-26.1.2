package com.mistech.mistechmod.tarot.fool.client;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Chapéu de bobo da corte (usado pelo Louco e pelo Chapéu do Bobo vestido): touca justa na cabeça, faixa na testa e
 * três pontas compridas e moles (esquerda, direita e atrás) que sobem, se curvam e caem, cada uma com um guizo.
 * <p>
 * Medidas em pixels, relativas à cabeça (cabeça de y -8 a 0). Textura a partir de (u, v), 45 linhas de altura e
 * 40 de largura (ver {@code tools/tarot/gen_fool_designs.py}, função {@code paint_cap}):
 * touca (u, v) 9×4×9 · faixa (u, v+13) 10×1×10 · ponta esquerda (u, v+24) · direita (u, v+31) · de trás (u, v+38).
 * Cada linha de ponta: segmentos 3×4×3, 2×4×2, 2×3×2, 1×3×1 e guizo 2×2×2 lado a lado.
 */
public final class JesterCap {
    /** Curvatura de cada segmento (rad), somando ~150°: a ponta sobe, vira para fora e cai. */
    private static final float[] BEND = {0.7F, 0.6F, 0.7F, 0.65F};

    private JesterCap() {}

    /** Partes animáveis: os 4 segmentos de cada ponta. */
    public record Parts(ModelPart[] left, ModelPart[] right, ModelPart[] back) {
        public static Parts of(ModelPart cap) {
            return new Parts(chain(cap.getChild("horn_left")), chain(cap.getChild("horn_right")), chain(cap.getChild("horn_back")));
        }

        private static ModelPart[] chain(ModelPart base) {
            ModelPart[] parts = new ModelPart[BEND.length];
            parts[0] = base;
            for (int i = 1; i < BEND.length; i++) {
                parts[i] = parts[i - 1].getChild("seg" + (i + 1));
            }
            return parts;
        }

        /** Pontas balançando (mais na ponta que na base); {@code jiggle} = quanto mexe (0 = parado). */
        public void animate(float t, float jiggle) {
            for (int i = 0; i < BEND.length; i++) {
                float k = 0.04F + i * 0.035F;
                this.left[i].zRot = BEND[i] + Mth.sin(t * 0.22F + i * 0.6F) * k * jiggle;
                this.right[i].zRot = -BEND[i] - Mth.sin(t * 0.22F + 1.3F + i * 0.6F) * k * jiggle;
                this.back[i].xRot = -BEND[i] - Mth.sin(t * 0.2F + 2.6F + i * 0.6F) * k * jiggle;
            }
        }
    }

    /**
     * Monta o chapéu dentro de {@code parent} (que deve estar no pivô da cabeça).
     * @param name nome da parte criada (o chapéu inteiro)
     * @param grow deformação da touca/faixa (para não encostar na pele)
     */
    public static PartDefinition build(PartDefinition parent, String name, int u, int v, float grow) {
        PartDefinition cap = parent.addOrReplaceChild(name, CubeListBuilder.create()
                .texOffs(u, v).addBox(-4.5F, -9.0F, -4.5F, 9, 4, 9, new CubeDeformation(grow))
                .texOffs(u, v + 13).addBox(-5.0F, -5.5F, -5.0F, 10, 1, 10, new CubeDeformation(grow)), PartPose.ZERO);
        horn(cap, "horn_left", u, v + 24, 3.0F, 0.0F, 1, 0);
        horn(cap, "horn_right", u, v + 31, -3.0F, 0.0F, -1, 0);
        horn(cap, "horn_back", u, v + 38, 0.0F, 2.5F, 0, 1);
        return cap;
    }

    /**
     * Uma ponta: 4 segmentos encadeados, cada um curvando mais para fora ({@code sideX} = +1 esquerda, -1 direita) ou
     * para trás ({@code back} = 1), e o guizo na ponta.
     */
    private static void horn(PartDefinition cap, String name, int u, int v, float x, float z, int sideX, int back) {
        int[][] sizes = {{3, 4}, {2, 4}, {2, 3}, {1, 3}};
        int[] texU = {0, 12, 20, 28};
        PartDefinition part = cap;
        float pivotY = -8.5F;
        float px = x;
        float pz = z;
        for (int i = 0; i < sizes.length; i++) {
            int w = sizes[i][0];
            int h = sizes[i][1];
            float zRot = sideX * BEND[i];
            float xRot = -back * BEND[i];
            String segName = i == 0 ? name : "seg" + (i + 1);
            part = part.addOrReplaceChild(segName, CubeListBuilder.create().texOffs(u + texU[i], v)
                    .addBox(-w / 2.0F, -h, -w / 2.0F, w, h, w), PartPose.offsetAndRotation(px, pivotY, pz, xRot, 0.0F, zRot));
            // o próximo segmento nasce na ponta deste (coordenadas locais)
            px = 0.0F;
            pz = 0.0F;
            pivotY = -h + 0.3F;
        }
        // guizo pendurado na ponta
        part.addOrReplaceChild("bell", CubeListBuilder.create().texOffs(u + 32, v).addBox(-1.0F, -2.2F, -1.0F, 2, 2, 2),
                PartPose.offset(0.0F, -2.8F, 0.0F));
    }
}

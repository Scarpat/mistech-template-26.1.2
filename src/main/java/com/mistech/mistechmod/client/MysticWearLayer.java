package com.mistech.mistechmod.client;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.registry.MistechWear;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;

/**
 * Partes 3D das vestes místicas: o Chapéu de Gnomo (grande e caído para trás) e a saia do Roupão Místico
 * (desce do tronco até as pernas). Ambos pegam a cor da tinta do item.
 */
public class MysticWearLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(Mistech.MODID, "mystic_wear"), "main");
    private static final Identifier HAT_TEXTURE = Identifier.fromNamespaceAndPath(Mistech.MODID, "textures/entity/gnome_hat.png");
    private static final Identifier ROBE_TEXTURE = Identifier.fromNamespaceAndPath(Mistech.MODID, "textures/entity/mystic_robe_skirt.png");
    private static final Identifier FOOL_CAP_TEXTURE = Identifier.fromNamespaceAndPath(Mistech.MODID, "textures/entity/fool_cap.png");

    private final ModelPart hat;
    private final ModelPart skirt;
    private final ModelPart foolCap;
    private final com.mistech.mistechmod.tarot.fool.client.JesterCap.Parts foolCapParts;

    public MysticWearLayer(RenderLayerParent<AvatarRenderState, PlayerModel> renderer, ModelPart root) {
        super(renderer);
        this.hat = root.getChild("hat");
        this.skirt = root.getChild("skirt");
        this.foolCap = root.getChild("fool_cap");
        this.foolCapParts = com.mistech.mistechmod.tarot.fool.client.JesterCap.Parts.of(this.foolCap);
    }

    /** Modelo em pixels (16 = 1 bloco). Cabeça vai de y -8 a 0; tronco de 0 a 12. */
    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Chapéu: aba caída sobre a cabeça + corpo que vai afinando e cai para trás (como um gorro de gnomo)
        PartDefinition hat = root.addOrReplaceChild("hat", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.5F, -9.5F, -4.5F, 9, 4, 9, new CubeDeformation(0.25F)), PartPose.ZERO);
        PartDefinition t1 = hat.addOrReplaceChild("t1", CubeListBuilder.create()
                .texOffs(0, 14).addBox(-4.0F, -4.0F, -4.0F, 8, 4, 8), PartPose.offsetAndRotation(0.0F, -9.5F, 0.5F, -0.25F, 0.0F, 0.0F));
        PartDefinition t2 = t1.addOrReplaceChild("t2", CubeListBuilder.create()
                .texOffs(0, 26).addBox(-3.0F, -4.0F, -3.0F, 6, 4, 6), PartPose.offsetAndRotation(0.0F, -3.5F, 0.8F, -0.45F, 0.0F, 0.0F));
        PartDefinition t3 = t2.addOrReplaceChild("t3", CubeListBuilder.create()
                .texOffs(36, 0).addBox(-2.0F, -4.0F, -2.0F, 4, 4, 4), PartPose.offsetAndRotation(0.0F, -3.5F, 0.6F, -0.6F, 0.0F, 0.0F));
        PartDefinition t4 = t3.addOrReplaceChild("t4", CubeListBuilder.create()
                .texOffs(36, 8).addBox(-1.5F, -4.0F, -1.5F, 3, 4, 3), PartPose.offsetAndRotation(0.0F, -3.5F, 0.4F, -0.7F, 0.0F, 0.0F));
        t4.addOrReplaceChild("t5", CubeListBuilder.create()
                .texOffs(36, 16).addBox(-1.0F, -4.0F, -1.0F, 2, 4, 2), PartPose.offsetAndRotation(0.0F, -3.5F, 0.3F, -0.6F, 0.0F, 0.0F));

        // Saia do roupão: aberta na frente (só abas estreitas), longa atrás e dos lados, abrindo embaixo.
        // Parte de cima (quadril, y 11..16) justa; parte de baixo (y 16..22) mais larga.
        CubeDeformation d = new CubeDeformation(0.3F);
        root.addOrReplaceChild("skirt", CubeListBuilder.create()
                // costas
                .texOffs(0, 36).addBox(-4.5F, 11.0F, 1.5F, 9, 5, 1, d)
                .texOffs(0, 44).addBox(-5.0F, 16.0F, 2.0F, 10, 6, 1, d)
                // laterais
                .texOffs(24, 36).addBox(-5.0F, 11.0F, -2.5F, 1, 5, 5, d)
                .texOffs(24, 36).addBox(4.0F, 11.0F, -2.5F, 1, 5, 5, d)
                .texOffs(24, 48).addBox(-5.5F, 16.0F, -3.0F, 1, 6, 6, d)
                .texOffs(24, 48).addBox(4.5F, 16.0F, -3.0F, 1, 6, 6, d)
                // abas da frente (deixam o meio aberto)
                .texOffs(40, 36).addBox(-4.5F, 11.0F, -2.5F, 2, 5, 1, d)
                .texOffs(40, 36).addBox(2.5F, 11.0F, -2.5F, 2, 5, 1, d)
                .texOffs(40, 44).addBox(-5.0F, 16.0F, -3.0F, 2, 6, 1, d)
                .texOffs(40, 44).addBox(3.0F, 16.0F, -3.0F, 2, 6, 1, d), PartPose.ZERO);

        // Chapéu do Bobo: chapéu de bobo da corte com três pontas moles e guizos (textura própria, fool_cap.png)
        com.mistech.mistechmod.tarot.fool.client.JesterCap.build(root, "fool_cap", 0, 0, 0.35F);

        return LayerDefinition.create(mesh, 64, 64);
    }

    private static int colorOf(ItemStack stack, int fallback) {
        return ARGB.opaque(DyedItemColor.getOrDefault(stack, fallback));
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, AvatarRenderState state, float yRot, float xRot) {
        if (state.isInvisible) {
            return;
        }
        if (state.headEquipment.is(MistechWear.GNOME_HAT.get())) {
            poseStack.pushPose();
            this.getParentModel().head.translateAndRotate(poseStack);
            collector.submitModelPart(this.hat, poseStack, RenderTypes.armorCutoutNoCull(HAT_TEXTURE), lightCoords,
                    OverlayTexture.NO_OVERLAY, null, colorOf(state.headEquipment, MistechWear.HAT_COLOR), null);
            poseStack.popPose();
        }
        if (state.headEquipment.is(com.mistech.mistechmod.tarot.fool.FoolContent.FOOL_CAP.get())) {
            poseStack.pushPose();
            this.getParentModel().head.translateAndRotate(poseStack);
            // as pontas balançam mais quando o jogador anda/corre
            this.foolCapParts.animate(state.ageInTicks, 0.6F + Math.min(1.5F, state.walkAnimationSpeed * 2.5F));
            collector.submitModelPart(this.foolCap, poseStack, RenderTypes.armorCutoutNoCull(FOOL_CAP_TEXTURE), lightCoords,
                    OverlayTexture.NO_OVERLAY, null, -1, null);
            poseStack.popPose();
        }
        if (state.chestEquipment.is(MistechWear.MYSTIC_ROBE.get())) {
            poseStack.pushPose();
            this.getParentModel().body.translateAndRotate(poseStack);
            collector.submitModelPart(this.skirt, poseStack, RenderTypes.armorCutoutNoCull(ROBE_TEXTURE), lightCoords,
                    OverlayTexture.NO_OVERLAY, null, colorOf(state.chestEquipment, MistechWear.ROBE_COLOR), null);
            poseStack.popPose();
        }
    }
}

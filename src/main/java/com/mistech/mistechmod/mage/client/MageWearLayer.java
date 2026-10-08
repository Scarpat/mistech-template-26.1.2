package com.mistech.mistechmod.mage.client;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.mage.MageRobeItem;
import com.mistech.mistechmod.mage.MageRobeTier;
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
 * Partes 3D das Vestes de Mago: chapéu de mago (capuz), gola + faixa + saia longa e punhos largos (manto), bico dos sapatos.
 * Duas passadas por parte: o tecido ({@code mage_wear_base.png}, tingido com a cor do item) e os enfeites do nível
 * ({@code mage_wear_<nível>_trim.png}, sem tinta). Modelo e texturas gerados por {@code tools/mage/robe_design.py}.
 */
public class MageWearLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(Mistech.MODID, "mage_wear"), "main");
    private static final Identifier BASE = Identifier.fromNamespaceAndPath(Mistech.MODID, "textures/entity/mage_wear_base.png");

    private final ModelPart hat, robe, cuffR, cuffL, shoeR, shoeL;

    public MageWearLayer(RenderLayerParent<AvatarRenderState, PlayerModel> renderer, ModelPart root) {
        super(renderer);
        this.hat = root.getChild("hat");
        this.robe = root.getChild("robe");
        this.cuffR = root.getChild("cuff_r");
        this.cuffL = root.getChild("cuff_l");
        this.shoeR = root.getChild("shoe_r");
        this.shoeL = root.getChild("shoe_l");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_hat = root.addOrReplaceChild("hat", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-6.5F, -9.0F, -6.5F, 13.0F, 1.0F, 13.0F, new CubeDeformation(0.0F))
                .texOffs(0, 27).addBox(-4.5F, -11.5F, -4.5F, 9.0F, 3.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition p_hat_c1 = p_hat.addOrReplaceChild("hat_c1", CubeListBuilder.create()
                .texOffs(0, 39).addBox(-3.5F, -3.0F, -3.5F, 7.0F, 3.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -11.5F, 0.3F, -0.1F, 0.0F, 0.0F));
        PartDefinition p_hat_c2 = p_hat_c1.addOrReplaceChild("hat_c2", CubeListBuilder.create()
                .texOffs(0, 59).addBox(-2.5F, -3.0F, -2.5F, 5.0F, 3.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -3.0F, 0.3F, -0.16F, 0.0F, 0.0F));
        PartDefinition p_hat_c3 = p_hat_c2.addOrReplaceChild("hat_c3", CubeListBuilder.create()
                .texOffs(24, 67).addBox(-2.0F, -3.0F, -2.0F, 4.0F, 3.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -3.0F, 0.3F, -0.28F, 0.0F, 0.0F));
        PartDefinition p_hat_c4 = p_hat_c3.addOrReplaceChild("hat_c4", CubeListBuilder.create()
                .texOffs(22, 74).addBox(-1.5F, -3.0F, -1.5F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -3.0F, 0.3F, -0.45F, 0.0F, 0.0F));
        PartDefinition p_hat_c5 = p_hat_c4.addOrReplaceChild("hat_c5", CubeListBuilder.create()
                .texOffs(54, 74).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -3.0F, 0.2F, -0.6F, 0.0F, 0.0F));
        PartDefinition p_hat_c6 = p_hat_c5.addOrReplaceChild("hat_c6", CubeListBuilder.create()
                .texOffs(6, 80).addBox(-0.5F, -2.0F, -0.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -2.0F, 0.0F, -0.5F, 0.0F, 0.0F));
        PartDefinition p_robe = root.addOrReplaceChild("robe", CubeListBuilder.create()
                .texOffs(28, 49).addBox(-5.5F, -2.0F, -3.5F, 11.0F, 2.0F, 7.0F, new CubeDeformation(0.0F))
                .texOffs(0, 80).addBox(-1.0F, 8.5F, -4.0F, 2.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(40, 67).addBox(-5.0F, 12.0F, 2.4F, 10.0F, 5.0F, 1.0F, new CubeDeformation(0.3F))
                .texOffs(0, 74).addBox(-5.0F, 12.0F, -3.4F, 10.0F, 5.0F, 1.0F, new CubeDeformation(0.3F))
                .texOffs(36, 27).addBox(-5.6F, 12.0F, -3.0F, 1.0F, 5.0F, 6.0F, new CubeDeformation(0.3F))
                .texOffs(50, 27).addBox(4.6F, 12.0F, -3.0F, 1.0F, 5.0F, 6.0F, new CubeDeformation(0.3F))
                .texOffs(20, 59).addBox(-5.5F, 17.0F, 2.9F, 11.0F, 6.0F, 1.0F, new CubeDeformation(0.3F))
                .texOffs(0, 67).addBox(-5.5F, 17.0F, -3.9F, 11.0F, 6.0F, 1.0F, new CubeDeformation(0.3F))
                .texOffs(0, 14).addBox(-6.1F, 17.0F, -3.5F, 1.0F, 6.0F, 7.0F, new CubeDeformation(0.3F))
                .texOffs(16, 14).addBox(5.1F, 17.0F, -3.5F, 1.0F, 6.0F, 7.0F, new CubeDeformation(0.3F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition p_cuff_r = root.addOrReplaceChild("cuff_r", CubeListBuilder.create()
                .texOffs(28, 39).addBox(-4.5F, 7.0F, -3.5F, 7.0F, 3.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition p_cuff_l = root.addOrReplaceChild("cuff_l", CubeListBuilder.create()
                .texOffs(0, 49).addBox(-2.5F, 7.0F, -3.5F, 7.0F, 3.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition p_shoe_r = root.addOrReplaceChild("shoe_r", CubeListBuilder.create()
                .texOffs(34, 74).addBox(-1.5F, 10.5F, -5.0F, 3.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(10, 80).addBox(-0.5F, 9.0F, -6.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition p_shoe_l = root.addOrReplaceChild("shoe_l", CubeListBuilder.create()
                .texOffs(44, 74).addBox(-1.5F, 10.5F, -5.0F, 3.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(14, 80).addBox(-0.5F, 9.0F, -6.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 128);
    }

    private static Identifier trim(MageRobeTier tier) {
        return Identifier.fromNamespaceAndPath(Mistech.MODID, "textures/entity/mage_wear_" + tier.id + "_trim.png");
    }

    private void draw(PoseStack pose, SubmitNodeCollector collector, int light, ModelPart part, ModelPart parent, ItemStack stack, MageRobeTier tier) {
        pose.pushPose();
        parent.translateAndRotate(pose);
        int color = ARGB.opaque(DyedItemColor.getOrDefault(stack, tier.color));
        collector.submitModelPart(part, pose, RenderTypes.armorCutoutNoCull(BASE), light, OverlayTexture.NO_OVERLAY, null, color, null);
        collector.submitModelPart(part, pose, RenderTypes.armorCutoutNoCull(trim(tier)), light, OverlayTexture.NO_OVERLAY, null, -1, null);
        pose.popPose();
    }

    @Override
    public void submit(PoseStack pose, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot) {
        if (state.isInvisible) {
            return;
        }
        PlayerModel model = this.getParentModel();
        if (state.headEquipment.getItem() instanceof MageRobeItem item) {
            this.draw(pose, collector, light, this.hat, model.head, state.headEquipment, item.robe());
        }
        if (state.chestEquipment.getItem() instanceof MageRobeItem item) {
            this.draw(pose, collector, light, this.robe, model.body, state.chestEquipment, item.robe());
            this.draw(pose, collector, light, this.cuffR, model.rightArm, state.chestEquipment, item.robe());
            this.draw(pose, collector, light, this.cuffL, model.leftArm, state.chestEquipment, item.robe());
        }
        if (state.feetEquipment.getItem() instanceof MageRobeItem item) {
            this.draw(pose, collector, light, this.shoeR, model.rightLeg, state.feetEquipment, item.robe());
            this.draw(pose, collector, light, this.shoeL, model.leftLeg, state.feetEquipment, item.robe());
        }
    }
}

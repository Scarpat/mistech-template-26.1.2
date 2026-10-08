package com.mistech.mistechmod.client;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.block.SpatialBarrelBlock;
import com.mistech.mistechmod.item.BackpackItem;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Barril Carregável vestido: desenha um Barril Espacial de verdade (3D) nas costas do jogador,
 * acompanhando o tronco (agachar, girar). As alças ficam na textura de equipamento.
 */
public class BackpackLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    private static final BlockDisplayContext DISPLAY_CONTEXT = BlockDisplayContext.create();
    /** Tamanho do barril em blocos (0,56 = ~9 pixels, um pouco mais largo que o tronco). */
    private static final float SIZE = 0.56F;

    private final BlockModelRenderState barrelState = new BlockModelRenderState();

    public BackpackLayer(RenderLayerParent<AvatarRenderState, PlayerModel> renderer) {
        super(renderer);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, AvatarRenderState state, float yRot, float xRot) {
        if (!(state.chestEquipment.getItem() instanceof BackpackItem) || state.isInvisible) {
            return;
        }
        BlockState barrel = Mistech.SPATIAL_BARREL.get().defaultBlockState()
                .setValue(SpatialBarrelBlock.FACING, Direction.UP);
        Minecraft.getInstance().getBlockModelResolver().update(this.barrelState, barrel, DISPLAY_CONTEXT);
        if (this.barrelState.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        this.getParentModel().body.translateAndRotate(poseStack);
        // espaço do modelo: y cresce para baixo, +z = costas. Centro do barril no meio das costas.
        poseStack.translate(0.0F, 6.5F / 16.0F, 2.2F / 16.0F + SIZE / 2.0F);
        // espelha x e y (o modelo do jogador é de cabeça para baixo) e centraliza o bloco
        poseStack.scale(-SIZE, -SIZE, SIZE);
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        this.barrelState.submit(poseStack, collector, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();
    }
}

package com.mistech.mistechmod.spellcraft.client;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.spellcraft.SummonFlyerEntity;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.animal.parrot.ParrotModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.ParrotRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Fênix: ave de fogo (forma de ave em voo, 1,8× maior), penas em brasa e sempre iluminada. */
public class PhoenixRenderer extends MobRenderer<SummonFlyerEntity, ParrotRenderState, ParrotModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Mistech.MODID, "textures/entity/phoenix.png");

    public PhoenixRenderer(EntityRendererProvider.Context context) {
        super(context, new ParrotModel(context.bakeLayer(ModelLayers.PARROT)), 0.5F);
    }

    @Override
    public ParrotRenderState createRenderState() {
        return new ParrotRenderState();
    }

    @Override
    public void extractRenderState(SummonFlyerEntity entity, ParrotRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.pose = ParrotModel.Pose.FLYING;
        state.flapAngle = (Mth.sin(state.ageInTicks * 0.7F) + 1.0F) * 0.6F;
        state.variant = net.minecraft.world.entity.animal.parrot.Parrot.Variant.RED_BLUE;
    }

    @Override
    protected void scale(ParrotRenderState state, PoseStack poseStack) {
        super.scale(state, poseStack);
        poseStack.scale(1.8F, 1.8F, 1.8F);
    }

    @Override
    protected int getBlockLightLevel(SummonFlyerEntity entity, BlockPos pos) {
        return 15;
    }

    @Override
    public Identifier getTextureLocation(ParrotRenderState state) {
        return TEXTURE;
    }
}

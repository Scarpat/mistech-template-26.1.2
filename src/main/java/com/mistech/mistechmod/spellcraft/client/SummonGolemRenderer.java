package com.mistech.mistechmod.spellcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.model.animal.golem.IronGolemModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.IronGolemRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.golem.IronGolem;

/** Golem de Pedra / Guardião de Madeira: geometria de {@link SummonModels} animada pelo IronGolemModel, textura própria. */
public class SummonGolemRenderer extends MobRenderer<IronGolem, IronGolemRenderState, IronGolemModel> {
    private final Identifier texture;

    public SummonGolemRenderer(EntityRendererProvider.Context context, ModelLayerLocation layer, Identifier texture, float shadow) {
        super(context, new IronGolemModel(context.bakeLayer(layer)), shadow);
        this.texture = texture;
    }

    @Override
    public IronGolemRenderState createRenderState() {
        return new IronGolemRenderState();
    }

    @Override
    public void extractRenderState(IronGolem golem, IronGolemRenderState state, float partialTicks) {
        super.extractRenderState(golem, state, partialTicks);
        state.attackTicksRemaining = golem.getAttackAnimationTick() > 0 ? golem.getAttackAnimationTick() - partialTicks : 0.0F;
        state.offerFlowerTick = 0;
        state.crackiness = golem.getCrackiness();
    }

    @Override
    protected void setupRotations(IronGolemRenderState state, PoseStack poseStack, float bodyRot, float scale) {
        super.setupRotations(state, poseStack, bodyRot, scale);
        // balanço pesado ao andar, como o Golem de Ferro
        if (state.walkAnimationSpeed >= 0.01F) {
            float t = state.walkAnimationPos + 6.0F;
            float sway = (Math.abs(t % 13.0F - 6.5F) - 3.25F) / 3.25F;
            poseStack.mulPose(Axis.ZP.rotationDegrees(6.5F * sway * Mth.clamp(state.walkAnimationSpeed, 0.0F, 1.0F)));
        }
    }

    @Override
    public Identifier getTextureLocation(IronGolemRenderState state) {
        return this.texture;
    }
}

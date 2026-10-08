package com.mistech.mistechmod.beyond.client;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.beyond.PaleSentinelEntity;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.creaking.CreakingModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.StriderRenderer;
import net.minecraft.client.renderer.entity.layers.LivingEntityEmissiveLayer;
import net.minecraft.client.renderer.entity.state.CreakingRenderState;
import net.minecraft.client.renderer.entity.state.StriderRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/** Renderizadores do Jardim Pálido e do Nether. */
public final class PaleNetherRenderers {
    private PaleNetherRenderers() {}

    static Identifier tex(String name) {
        return Identifier.fromNamespaceAndPath(Mistech.MODID, "textures/entity/" + name + ".png");
    }

    /** Sentinela Rangente: modelo do Rangedor, madeira mais clara e olhos laranja (sempre que tem um alvo). */
    public static class Sentinel extends MobRenderer<PaleSentinelEntity, CreakingRenderState, CreakingModel> {
        private static final Identifier TEXTURE = tex("pale_sentinel");
        private static final Identifier EYES = Identifier.withDefaultNamespace("textures/entity/creaking/creaking_eyes.png");

        public Sentinel(EntityRendererProvider.Context context) {
            super(context, new CreakingModel(context.bakeLayer(ModelLayers.CREAKING)), 0.6F);
            this.addLayer(new LivingEntityEmissiveLayer<>(this, state -> EYES, (state, age) -> state.eyesGlowing ? 1.0F : 0.0F,
                    new CreakingModel(context.bakeLayer(ModelLayers.CREAKING_EYES)), RenderTypes::eyes, true));
        }

        @Override
        public Identifier getTextureLocation(CreakingRenderState state) {
            return TEXTURE;
        }

        @Override
        public CreakingRenderState createRenderState() {
            return new CreakingRenderState();
        }

        @Override
        public void extractRenderState(PaleSentinelEntity entity, CreakingRenderState state, float partialTicks) {
            super.extractRenderState(entity, state, partialTicks);
            state.attackAnimationState.copyFrom(entity.attackAnimationState);
            state.invulnerabilityAnimationState.copyFrom(entity.invulnerabilityAnimationState);
            state.deathAnimationState.copyFrom(entity.deathAnimationState);
            state.eyesGlowing = entity.isActive();
            state.canMove = entity.canMove();
        }
    }

    /** Strider Rúnico: o strider vanilla com runas brilhando no corpo. */
    public static class RunicStrider extends StriderRenderer {
        private static final Identifier TEXTURE = tex("runic_strider");

        public RunicStrider(EntityRendererProvider.Context context) {
            super(context);
        }

        @Override
        public Identifier getTextureLocation(StriderRenderState state) {
            return TEXTURE;
        }
    }
}

package com.mistech.mistechmod.client;

import java.util.function.Function;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.entity.goblin.GoblinQueenEntity;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;

/** Renderer dos mobs da Vila dos Duendes: modelo + textura + camada que brilha no escuro (olhos, orbe, coroa). */
public class GoblinFamilyRenderer<T extends Mob, M extends EntityModel<GoblinRenderState>> extends MobRenderer<T, GoblinRenderState, M> {
    private final Identifier texture;
    private final Identifier pureTexture;
    private final float scale;

    public GoblinFamilyRenderer(EntityRendererProvider.Context context, ModelLayerLocation layer, Function<ModelPart, M> model,
            String name, String glowSuffix, float shadow, float scale) {
        super(context, model.apply(context.bakeLayer(layer)), shadow);
        this.texture = Identifier.fromNamespaceAndPath(Mistech.MODID, "textures/entity/" + name + ".png");
        this.pureTexture = Identifier.fromNamespaceAndPath(Mistech.MODID, "textures/entity/" + name + "_pure.png");
        this.scale = scale;
        RenderType glow = RenderTypes.eyes(Identifier.fromNamespaceAndPath(Mistech.MODID, "textures/entity/" + name + "_" + glowSuffix + ".png"));
        this.addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return glow;
            }
        });
    }

    @Override
    public GoblinRenderState createRenderState() {
        return new GoblinRenderState();
    }

    @Override
    public void extractRenderState(T entity, GoblinRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.attackAnim = entity.getAttackAnim(partialTicks);
        state.castAnim = entity instanceof GoblinQueenEntity queen ? queen.castProgress(partialTicks) : 0.0F;
        state.pure = entity instanceof com.mistech.mistechmod.entity.goblin.CorruptedGoblinEntity goblin && goblin.isPure();
    }

    @Override
    protected void scale(GoblinRenderState state, PoseStack poseStack) {
        if (this.scale != 1.0F) {
            poseStack.scale(this.scale, this.scale, this.scale);
        }
    }

    @Override
    public Identifier getTextureLocation(GoblinRenderState state) {
        return state.pure ? this.pureTexture : this.texture;
    }
}

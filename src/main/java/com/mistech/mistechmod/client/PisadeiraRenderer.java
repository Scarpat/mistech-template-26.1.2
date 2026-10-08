package com.mistech.mistechmod.client;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.entity.PisadeiraEntity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/** Pisadeira com os olhos vermelhos brilhando no escuro. */
public class PisadeiraRenderer extends MobRenderer<PisadeiraEntity, PisadeiraRenderState, PisadeiraModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Mistech.MODID, "textures/entity/pisadeira.png");
    private static final RenderType EYES = RenderTypes.eyes(Identifier.fromNamespaceAndPath(Mistech.MODID, "textures/entity/pisadeira_eyes.png"));

    public PisadeiraRenderer(EntityRendererProvider.Context context) {
        super(context, new PisadeiraModel(context.bakeLayer(PisadeiraModel.LAYER)), 0.0F);
        this.addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return EYES;
            }
        });
    }

    @Override
    public PisadeiraRenderState createRenderState() {
        return new PisadeiraRenderState();
    }

    @Override
    public Identifier getTextureLocation(PisadeiraRenderState state) {
        return TEXTURE;
    }
}

package com.mistech.mistechmod.client;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.entity.EvilEyeEntity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/** Mal Olhado: o olho brilha no escuro. */
public class EvilEyeRenderer extends MobRenderer<EvilEyeEntity, EvilEyeRenderState, EvilEyeModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Mistech.MODID, "textures/entity/evil_eye.png");
    private static final RenderType EYES = RenderTypes.eyes(Identifier.fromNamespaceAndPath(Mistech.MODID, "textures/entity/evil_eye_eyes.png"));

    public EvilEyeRenderer(EntityRendererProvider.Context context) {
        super(context, new EvilEyeModel(context.bakeLayer(EvilEyeModel.LAYER)), 0.0F);
        this.addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return EYES;
            }
        });
    }

    @Override
    public EvilEyeRenderState createRenderState() {
        return new EvilEyeRenderState();
    }

    @Override
    public Identifier getTextureLocation(EvilEyeRenderState state) {
        return TEXTURE;
    }
}

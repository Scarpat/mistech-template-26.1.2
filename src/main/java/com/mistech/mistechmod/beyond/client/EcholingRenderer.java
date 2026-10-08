package com.mistech.mistechmod.beyond.client;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.beyond.EcholingEntity;

import net.minecraft.client.model.animal.parrot.ParrotModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.state.ParrotRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Renderizador do Ecólito (olhos e pontas das antenas brilham no escuro). */
public class EcholingRenderer extends MobRenderer<EcholingEntity, ParrotRenderState, EcholingModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Mistech.MODID, "textures/entity/echoling.png");
    private static final RenderType GLOW = RenderTypes.eyes(Identifier.fromNamespaceAndPath(Mistech.MODID, "textures/entity/echoling_glow.png"));

    public EcholingRenderer(EntityRendererProvider.Context context) {
        super(context, new EcholingModel(context.bakeLayer(EcholingModel.LAYER)), 0.25F);
        this.addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return GLOW;
            }
        });
    }

    @Override
    public ParrotRenderState createRenderState() {
        return new ParrotRenderState();
    }

    @Override
    public void extractRenderState(EcholingEntity entity, ParrotRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        float flap = Mth.lerp(partialTicks, entity.oFlap, entity.flap);
        float flapSpeed = Mth.lerp(partialTicks, entity.oFlapSpeed, entity.flapSpeed);
        state.flapAngle = (Mth.sin(flap) + 1.0F) * flapSpeed;
        state.pose = ParrotModel.getPose(entity);
    }

    @Override
    public Identifier getTextureLocation(ParrotRenderState state) {
        return TEXTURE;
    }
}

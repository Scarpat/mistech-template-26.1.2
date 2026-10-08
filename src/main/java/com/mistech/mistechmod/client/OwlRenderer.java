package com.mistech.mistechmod.client;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.entity.OwlEntity;

import net.minecraft.client.model.animal.parrot.ParrotModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.ParrotRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Coruja: modelo próprio ({@link OwlModel}); estado de render e poses iguais aos do papagaio. */
public class OwlRenderer extends MobRenderer<OwlEntity, ParrotRenderState, OwlModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Mistech.MODID, "textures/entity/owl.png");

    public OwlRenderer(EntityRendererProvider.Context context) {
        super(context, new OwlModel(context.bakeLayer(OwlModel.LAYER)), 0.3F);
    }

    @Override
    public Identifier getTextureLocation(ParrotRenderState state) {
        return TEXTURE;
    }

    @Override
    public ParrotRenderState createRenderState() {
        return new ParrotRenderState();
    }

    @Override
    public void extractRenderState(OwlEntity entity, ParrotRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        float flap = Mth.lerp(partialTicks, entity.oFlap, entity.flap);
        float flapSpeed = Mth.lerp(partialTicks, entity.oFlapSpeed, entity.flapSpeed);
        state.flapAngle = (Mth.sin(flap) + 1.0F) * flapSpeed;
        state.pose = ParrotModel.getPose(entity);
    }
}

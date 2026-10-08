package com.mistech.mistechmod.tarot.magus.client;

import com.mistech.mistechmod.tarot.magus.MagusAspectEntity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;

/** Um dos Quatro Aspectos (mesmo modelo, menor). */
public class MagusAspectRenderer extends MagusBodyRenderer<MagusAspectEntity> {
    public MagusAspectRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void extractRenderState(MagusAspectEntity entity, MagusRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.form = entity.element().ordinal();
        state.mix = -1;
        state.action = entity.action();
        state.actionTime = entity.actionTime(partialTicks);
        state.phase = 3;
        state.shield = entity.bubble();
        state.aspect = true;
        state.reflection = false;
        state.mirrorY = 0.0F;
    }

    @Override
    protected boolean shouldShowName(MagusAspectEntity entity, double distanceSq) {
        return false;
    }
}

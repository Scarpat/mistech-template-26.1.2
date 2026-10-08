package com.mistech.mistechmod.tarot.magus.client;

import com.mistech.mistechmod.tarot.magus.MagusElement;
import com.mistech.mistechmod.tarot.magus.MagusEntity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;

/** O Mago. */
public class MagusRenderer extends MagusBodyRenderer<MagusEntity> {
    public MagusRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void extractRenderState(MagusEntity entity, MagusRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.form = entity.form().ordinal();
        MagusElement mix = entity.mixElement();
        state.mix = mix == null ? -1 : mix.ordinal();
        state.action = entity.action();
        state.actionTime = entity.actionTime(partialTicks);
        state.phase = entity.phase();
        state.shield = entity.shield();
        state.charges = entity.charges();
        state.aspect = false;
        state.reflection = false;
        state.mirrorY = entity.mirrorY();
        state.entityY = entity.getY(partialTicks);
    }
}

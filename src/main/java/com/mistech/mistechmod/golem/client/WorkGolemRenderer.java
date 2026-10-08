package com.mistech.mistechmod.golem.client;

import java.util.EnumMap;
import java.util.Map;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.client.ClayGolemCarriedItemLayer;
import com.mistech.mistechmod.client.ClayGolemModel;
import com.mistech.mistechmod.client.ClayGolemRenderState;
import com.mistech.mistechmod.golem.GolemKind;
import com.mistech.mistechmod.golem.WorkGolemEntity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;

/** Mesmo modelo do Golem de Barro, textura do material. */
public class WorkGolemRenderer extends MobRenderer<WorkGolemEntity, ClayGolemRenderState, ClayGolemModel> {
    private static final Map<GolemKind, Identifier> TEXTURES = new EnumMap<>(GolemKind.class);

    static {
        for (GolemKind kind : GolemKind.values()) {
            TEXTURES.put(kind, Identifier.fromNamespaceAndPath(Mistech.MODID, "textures/entity/" + kind.id + "_golem.png"));
        }
    }

    public WorkGolemRenderer(EntityRendererProvider.Context context) {
        super(context, new ClayGolemModel(context.bakeLayer(ClayGolemModel.LAYER)), 0.3F);
        this.addLayer(new ClayGolemCarriedItemLayer(this));
    }

    @Override
    public ClayGolemRenderState createRenderState() {
        return new ClayGolemRenderState();
    }

    @Override
    public void extractRenderState(WorkGolemEntity entity, ClayGolemRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.kind = entity.kind().ordinal();
        state.carrying = !entity.getCarried().isEmpty();
        this.itemModelResolver.updateForLiving(state.heldItem, entity.getCarried(), ItemDisplayContext.GROUND, entity);
    }

    @Override
    public Identifier getTextureLocation(ClayGolemRenderState state) {
        return TEXTURES.get(GolemKind.byId(state.kind));
    }
}

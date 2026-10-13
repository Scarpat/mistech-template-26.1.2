package com.mistech.mistechmod.lore.client;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.lore.CaiporaEntity;
import com.mistech.mistechmod.lore.CucaEntity;
import com.mistech.mistechmod.lore.IaraEntity;
import com.mistech.mistechmod.lore.MapinguariEntity;
import com.mistech.mistechmod.lore.LoreContent;
import com.mistech.mistechmod.lore.MatintaEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Renderizadores da Matinta Pereira (troca de modelo entre pássaro e bruxa) e da Caipora. */
@EventBusSubscriber(modid = Mistech.MODID, value = Dist.CLIENT)
public final class LoreRenderers {
    private LoreRenderers() {}

    static Identifier tex(String name) {
        return Identifier.fromNamespaceAndPath(Mistech.MODID, "textures/entity/" + name + ".png");
    }

    @SubscribeEvent
    static void onLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(MatintaBirdModel.LAYER, MatintaBirdModel::createBodyLayer);
        event.registerLayerDefinition(MatintaWitchModel.LAYER, MatintaWitchModel::createBodyLayer);
        event.registerLayerDefinition(CaiporaModel.LAYER, CaiporaModel::createBodyLayer);
        event.registerLayerDefinition(IaraModel.LAYER, IaraModel::createBodyLayer);
        event.registerLayerDefinition(CucaModel.LAYER, CucaModel::createBodyLayer);
        event.registerLayerDefinition(MapinguariModel.LAYER, MapinguariModel::createBodyLayer);
    }

    @SubscribeEvent
    static void onRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(LoreContent.MATINTA.get(), Matinta::new);
        event.registerEntityRenderer(LoreContent.CAIPORA.get(), Caipora::new);
        event.registerEntityRenderer(LoreContent.IARA.get(), Iara::new);
        event.registerEntityRenderer(LoreContent.CUCA.get(), Cuca::new);
        event.registerEntityRenderer(LoreContent.MAPINGUARI.get(), Mapinguari::new);
    }

    /** Camada de brilho que usa a textura "_glow" do modelo atual. */
    static class Glow extends RenderLayer<LoreRenderState, EntityModel<LoreRenderState>> {
        private final java.util.function.Function<LoreRenderState, Identifier> texture;

        Glow(RenderLayerParent<LoreRenderState, EntityModel<LoreRenderState>> parent, java.util.function.Function<LoreRenderState, Identifier> texture) {
            super(parent);
            this.texture = texture;
        }

        @Override
        public void submit(PoseStack pose, SubmitNodeCollector collector, int light, LoreRenderState state, float yRot, float xRot) {
            if (!state.isInvisible) {
                collector.submitModel(this.getParentModel(), state, pose, RenderTypes.eyes(this.texture.apply(state)), 0xF000F0,
                        OverlayTexture.NO_OVERLAY, -1, null, state.outlineColor, null);
            }
        }
    }

    // ----- Matinta Pereira -----
    public static class Matinta extends MobRenderer<MatintaEntity, LoreRenderState, EntityModel<LoreRenderState>> {
        private static final Identifier BIRD = tex("matinta_bird"), BIRD_GLOW = tex("matinta_bird_glow");
        private static final Identifier WITCH = tex("matinta_witch"), WITCH_GLOW = tex("matinta_witch_glow");
        private final EntityModel<LoreRenderState> bird;
        private final EntityModel<LoreRenderState> witch;

        public Matinta(EntityRendererProvider.Context context) {
            super(context, new MatintaBirdModel(context.bakeLayer(MatintaBirdModel.LAYER)), 0.3F);
            this.bird = this.model;
            this.witch = new MatintaWitchModel(context.bakeLayer(MatintaWitchModel.LAYER));
            this.addLayer(new Glow(this, s -> s.witch ? WITCH_GLOW : BIRD_GLOW));
        }

        @Override
        public LoreRenderState createRenderState() {
            return new LoreRenderState();
        }

        @Override
        public void extractRenderState(MatintaEntity entity, LoreRenderState state, float partialTicks) {
            super.extractRenderState(entity, state, partialTicks);
            state.witch = entity.isWitch();
            state.flying = !entity.isWitch() && !entity.onGround();
            state.casting = entity.isCasting();
        }

        @Override
        public void submit(LoreRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
            this.model = state.witch ? this.witch : this.bird;
            this.shadowRadius = state.witch ? 0.45F : 0.25F;
            super.submit(state, pose, collector, camera);
        }

        @Override
        protected void scale(LoreRenderState state, PoseStack pose) {
            if (state.witch) {
                pose.scale(1.1F, 1.1F, 1.1F);
            }
        }

        @Override
        public Identifier getTextureLocation(LoreRenderState state) {
            return state.witch ? WITCH : BIRD;
        }
    }

    // ----- Caipora -----
    public static class Caipora extends MobRenderer<CaiporaEntity, LoreRenderState, EntityModel<LoreRenderState>> {
        private static final Identifier TEXTURE = tex("caipora");
        private static final Identifier GLOW = tex("caipora_glow");

        public Caipora(EntityRendererProvider.Context context) {
            super(context, new CaiporaModel(context.bakeLayer(CaiporaModel.LAYER)), 0.7F);
            this.addLayer(new Glow(this, s -> GLOW));
        }

        @Override
        protected void scale(LoreRenderState state, PoseStack pose) {
            pose.scale(1.15F, 1.15F, 1.15F);
        }

        @Override
        public LoreRenderState createRenderState() {
            return new LoreRenderState();
        }

        @Override
        public void extractRenderState(CaiporaEntity entity, LoreRenderState state, float partialTicks) {
            super.extractRenderState(entity, state, partialTicks);
            state.angry = entity.isAngry();
        }

        @Override
        public Identifier getTextureLocation(LoreRenderState state) {
            return TEXTURE;
        }
    }

    // ----- Iara -----
    public static class Iara extends MobRenderer<IaraEntity, LoreRenderState, EntityModel<LoreRenderState>> {
        private static final Identifier TEXTURE = tex("iara");
        private static final Identifier GLOW = tex("iara_glow");

        public Iara(EntityRendererProvider.Context context) {
            super(context, new IaraModel(context.bakeLayer(IaraModel.LAYER)), 0.4F);
            this.addLayer(new Glow(this, s -> GLOW));
        }

        @Override
        public LoreRenderState createRenderState() {
            return new LoreRenderState();
        }

        @Override
        public void extractRenderState(IaraEntity entity, LoreRenderState state, float partialTicks) {
            super.extractRenderState(entity, state, partialTicks);
            state.casting = entity.isSinging();
            state.swimming = entity.isInWater() && !entity.isSinging();
        }

        @Override
        protected void setupRotations(LoreRenderState state, PoseStack pose, float bodyRot, float entityScale) {
            super.setupRotations(state, pose, bodyRot, entityScale);
            if (state.swimming) {
                // nada deitada, cauda para trás
                pose.rotateAround(Axis.XP.rotationDegrees(-75.0F - state.xRot * 0.5F), 0.0F, state.boundingBoxHeight / 2.0F / entityScale, 0.0F);
            }
        }

        @Override
        public Identifier getTextureLocation(LoreRenderState state) {
            return TEXTURE;
        }
    }

    // ----- Cuca -----
    public static class Cuca extends MobRenderer<CucaEntity, LoreRenderState, EntityModel<LoreRenderState>> {
        private static final Identifier TEXTURE = tex("cuca");
        private static final Identifier GLOW = tex("cuca_glow");

        public Cuca(EntityRendererProvider.Context context) {
            super(context, new CucaModel(context.bakeLayer(CucaModel.LAYER)), 0.8F);
            this.addLayer(new Glow(this, s -> GLOW));
        }

        @Override
        protected void scale(LoreRenderState state, PoseStack pose) {
            pose.scale(1.4F, 1.4F, 1.4F);
        }

        @Override
        public LoreRenderState createRenderState() {
            return new LoreRenderState();
        }

        @Override
        public void extractRenderState(CucaEntity entity, LoreRenderState state, float partialTicks) {
            super.extractRenderState(entity, state, partialTicks);
            state.casting = entity.isCasting();
        }

        @Override
        public Identifier getTextureLocation(LoreRenderState state) {
            return TEXTURE;
        }
    }

    // ----- Mapinguari -----
    public static class Mapinguari extends MobRenderer<MapinguariEntity, LoreRenderState, EntityModel<LoreRenderState>> {
        private static final Identifier TEXTURE = tex("mapinguari");
        private static final Identifier GLOW = tex("mapinguari_glow");

        public Mapinguari(EntityRendererProvider.Context context) {
            super(context, new MapinguariModel(context.bakeLayer(MapinguariModel.LAYER)), 1.4F);
            this.addLayer(new Glow(this, s -> GLOW));
        }

        @Override
        protected void scale(LoreRenderState state, PoseStack pose) {
            pose.scale(1.7F, 1.7F, 1.7F);
        }

        @Override
        public LoreRenderState createRenderState() {
            return new LoreRenderState();
        }

        @Override
        public void extractRenderState(MapinguariEntity entity, LoreRenderState state, float partialTicks) {
            super.extractRenderState(entity, state, partialTicks);
            state.angry = entity.isRoaring();
        }

        @Override
        public Identifier getTextureLocation(LoreRenderState state) {
            return TEXTURE;
        }
    }
}

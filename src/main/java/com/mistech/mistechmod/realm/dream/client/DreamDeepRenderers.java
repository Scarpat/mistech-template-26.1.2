package com.mistech.mistechmod.realm.dream.client;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.realm.dream.mob.CloudWhaleEntity;
import com.mistech.mistechmod.realm.dream.mob.EchoEntity;
import com.mistech.mistechmod.realm.dream.mob.MemoryEntity;
import com.mistech.mistechmod.realm.dream.mob.PrismMothEntity;
import com.mistech.mistechmod.realm.dream.mob.RemWatcherEntity;
import com.mistech.mistechmod.realm.dream.mob.SkyFishEntity;
import com.mistech.mistechmod.realm.dream.mob.TheDreamerEntity;
import com.mistech.mistechmod.registry.DreamDeepContent;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Renderizadores da fase 3 do Sonho (anéis 3 e 4 e o chefe). */
@EventBusSubscriber(modid = Mistech.MODID, value = Dist.CLIENT)
public final class DreamDeepRenderers {
    private DreamDeepRenderers() {}

    @SubscribeEvent
    static void onLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(CloudWhaleModel.LAYER, CloudWhaleModel::createBodyLayer);
        event.registerLayerDefinition(SkyFishModel.LAYER, SkyFishModel::createBodyLayer);
        event.registerLayerDefinition(PrismMothModel.LAYER, PrismMothModel::createBodyLayer);
        event.registerLayerDefinition(EchoModel.LAYER, EchoModel::createBodyLayer);
        event.registerLayerDefinition(RemWatcherModel.LAYER, RemWatcherModel::createBodyLayer);
        event.registerLayerDefinition(MemoryModel.LAYER, MemoryModel::createBodyLayer);
        event.registerLayerDefinition(TheDreamerModel.LAYER, TheDreamerModel::createBodyLayer);
    }

    @SubscribeEvent
    static void onRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(DreamDeepContent.CLOUD_WHALE.get(),
                c -> new Simple<CloudWhaleEntity>(c, new CloudWhaleModel(c.bakeLayer(CloudWhaleModel.LAYER)), "cloud_whale", 4.0F, 2.2F, true, false));
        event.registerEntityRenderer(DreamDeepContent.SKY_FISH.get(), SkyFish::new);
        event.registerEntityRenderer(DreamDeepContent.PRISM_MOTH.get(),
                c -> new Simple<PrismMothEntity>(c, new PrismMothModel(c.bakeLayer(PrismMothModel.LAYER)), "prism_moth", 0.3F, 1.0F, true, false));
        event.registerEntityRenderer(DreamDeepContent.ECHO.get(),
                c -> new Simple<EchoEntity>(c, new EchoModel(c.bakeLayer(EchoModel.LAYER)), "echo", 0.0F, 1.0F, true, true));
        event.registerEntityRenderer(DreamDeepContent.REM_WATCHER.get(), Watcher::new);
        event.registerEntityRenderer(DreamDeepContent.MEMORY.get(),
                c -> new Simple<MemoryEntity>(c, new MemoryModel(c.bakeLayer(MemoryModel.LAYER)), "memory", 0.0F, 1.0F, true, true));
        event.registerEntityRenderer(DreamDeepContent.THE_DREAMER.get(), Dreamer::new);
    }

    /** Renderer comum: textura fixa, escala, camada que brilha e (opcional) translúcido. */
    public static class Simple<T extends Mob> extends MobRenderer<T, DreamMobRenderState, EntityModel<DreamMobRenderState>> {
        private final Identifier texture;
        private final float scale;
        private final boolean translucent;

        public Simple(EntityRendererProvider.Context context, EntityModel<DreamMobRenderState> model, String name, float shadow, float scale,
                boolean glow, boolean translucent) {
            super(context, model, shadow);
            this.texture = DreamMobRenderers.tex(name);
            this.scale = scale;
            this.translucent = translucent;
            if (glow) {
                Identifier g = DreamMobRenderers.tex(name + "_glow");
                this.addLayer(new DreamMobRenderers.Glow<>(this, s -> g));
            }
        }

        @Override
        public DreamMobRenderState createRenderState() {
            return new DreamMobRenderState();
        }

        @Override
        public void extractRenderState(T entity, DreamMobRenderState state, float partialTicks) {
            super.extractRenderState(entity, state, partialTicks);
            state.attackTime = entity.getAttackAnim(partialTicks);
            state.saddled = entity.isSaddled();
        }

        @Override
        public Identifier getTextureLocation(DreamMobRenderState state) {
            return this.texture;
        }

        @Override
        protected void scale(DreamMobRenderState state, PoseStack pose) {
            if (this.scale != 1.0F) {
                pose.scale(this.scale, this.scale, this.scale);
            }
        }

        @Override
        protected @Nullable RenderType getRenderType(DreamMobRenderState state, boolean bodyVisible, boolean forceTransparent, boolean glowing) {
            if (this.translucent && bodyVisible) {
                return RenderTypes.entityTranslucent(this.getTextureLocation(state));
            }
            return super.getRenderType(state, bodyVisible, forceTransparent, glowing);
        }

        @Override
        protected int getModelTint(DreamMobRenderState state) {
            return this.translucent ? 0xD8FFFFFF : -1;
        }
    }

    public static class SkyFish extends MobRenderer<SkyFishEntity, DreamMobRenderState, SkyFishModel> {
        private static final Identifier[] TEXTURES = {DreamMobRenderers.tex("sky_fish_0"), DreamMobRenderers.tex("sky_fish_1"),
                DreamMobRenderers.tex("sky_fish_2")};

        public SkyFish(EntityRendererProvider.Context context) {
            super(context, new SkyFishModel(context.bakeLayer(SkyFishModel.LAYER)), 0.15F);
        }

        @Override
        public DreamMobRenderState createRenderState() {
            return new DreamMobRenderState();
        }

        @Override
        public void extractRenderState(SkyFishEntity entity, DreamMobRenderState state, float partialTicks) {
            super.extractRenderState(entity, state, partialTicks);
            state.variant = entity.getVariant();
        }

        @Override
        public Identifier getTextureLocation(DreamMobRenderState state) {
            return TEXTURES[Math.floorMod(state.variant, TEXTURES.length)];
        }
    }

    public static class Watcher extends Simple<RemWatcherEntity> {
        public Watcher(EntityRendererProvider.Context context) {
            super(context, new RemWatcherModel(context.bakeLayer(RemWatcherModel.LAYER)), "rem_watcher", 0.0F, 1.0F, true, false);
        }

        @Override
        public void extractRenderState(RemWatcherEntity entity, DreamMobRenderState state, float partialTicks) {
            super.extractRenderState(entity, state, partialTicks);
            state.alert = entity.isAlert();
        }
    }

    public static class Dreamer extends MobRenderer<TheDreamerEntity, DreamMobRenderState, TheDreamerModel> {
        private static final Identifier ASLEEP = DreamMobRenderers.tex("the_dreamer"), AWAKE = DreamMobRenderers.tex("the_dreamer_awake");
        private static final Identifier ASLEEP_GLOW = DreamMobRenderers.tex("the_dreamer_glow"), AWAKE_GLOW = DreamMobRenderers.tex("the_dreamer_awake_glow");

        public Dreamer(EntityRendererProvider.Context context) {
            super(context, new TheDreamerModel(context.bakeLayer(TheDreamerModel.LAYER)), 0.0F);
            this.addLayer(new DreamMobRenderers.Glow<>(this, s -> s.awake ? AWAKE_GLOW : ASLEEP_GLOW));
        }

        @Override
        public DreamMobRenderState createRenderState() {
            return new DreamMobRenderState();
        }

        @Override
        public void extractRenderState(TheDreamerEntity entity, DreamMobRenderState state, float partialTicks) {
            super.extractRenderState(entity, state, partialTicks);
            state.awake = entity.isAwake();
            state.attackTime = entity.getAttackAnim(partialTicks);
        }

        @Override
        public Identifier getTextureLocation(DreamMobRenderState state) {
            return state.awake ? AWAKE : ASLEEP;
        }

        @Override
        protected void scale(DreamMobRenderState state, PoseStack pose) {
            pose.scale(1.6F, 1.6F, 1.6F);
        }
    }
}

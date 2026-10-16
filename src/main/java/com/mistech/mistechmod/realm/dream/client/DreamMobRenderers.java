package com.mistech.mistechmod.realm.dream.client;

import java.util.function.Function;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.client.GnomeModel;
import com.mistech.mistechmod.client.GnomeRenderState;
import com.mistech.mistechmod.realm.dream.mob.BogeymanEntity;
import com.mistech.mistechmod.realm.dream.mob.CorruptedGnomeEntity;
import com.mistech.mistechmod.realm.dream.mob.CountingSheepEntity;
import com.mistech.mistechmod.realm.dream.mob.GnomeMerchantEntity;
import com.mistech.mistechmod.realm.dream.mob.GnomeMinerEntity;
import com.mistech.mistechmod.realm.dream.mob.ShadeEntity;
import com.mistech.mistechmod.registry.DreamMobs;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Renderizadores das criaturas do Sonho (anéis 1 e 2). */
@EventBusSubscriber(modid = Mistech.MODID, value = Dist.CLIENT)
public final class DreamMobRenderers {
    private DreamMobRenderers() {}

    static Identifier tex(String name) {
        return Identifier.fromNamespaceAndPath(Mistech.MODID, "textures/entity/" + name + ".png");
    }

    @SubscribeEvent
    static void onLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(CountingSheepModel.LAYER, CountingSheepModel::createBodyLayer);
        event.registerLayerDefinition(BogeymanModel.LAYER, BogeymanModel::createBodyLayer);
        event.registerLayerDefinition(ShadeModel.LAYER, ShadeModel::createBodyLayer);
        event.registerLayerDefinition(GnomeVariantModels.MERCHANT, GnomeVariantModels::merchant);
        event.registerLayerDefinition(GnomeVariantModels.CORRUPTED, GnomeVariantModels::corrupted);
        event.registerLayerDefinition(GnomeVariantModels.MINER, GnomeVariantModels::miner);
    }

    @SubscribeEvent
    static void onRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(DreamMobs.COUNTING_SHEEP.get(), Sheep::new);
        event.registerEntityRenderer(DreamMobs.GNOME_MERCHANT.get(), c -> new Gnome<GnomeMerchantEntity>(c, "gnome_merchant", new GnomeModel(c.bakeLayer(GnomeVariantModels.MERCHANT)), false));
        event.registerEntityRenderer(DreamMobs.GNOME_MINER.get(),
                c -> new Gnome<GnomeMinerEntity>(c, "gnome_miner", new GnomeModel(c.bakeLayer(GnomeVariantModels.MINER)), true));
        event.registerEntityRenderer(DreamMobs.CORRUPTED_GNOME.get(), c -> new Gnome<CorruptedGnomeEntity>(c, "corrupted_gnome", new GnomeVariantModels.Corrupted(c.bakeLayer(GnomeVariantModels.CORRUPTED)), true));
        event.registerEntityRenderer(DreamMobs.BOGEYMAN.get(), Bogeyman::new);
        event.registerEntityRenderer(DreamMobs.SHADE.get(), Shade::new);
    }

    /** Camada que brilha no escuro (olhos, dentes) usando a textura "_glow". */
    static class Glow<S extends LivingEntityRenderState, M extends EntityModel<? super S>> extends RenderLayer<S, M> {
        private final Function<S, Identifier> texture;

        Glow(RenderLayerParent<S, M> parent, Function<S, Identifier> texture) {
            super(parent);
            this.texture = texture;
        }

        @Override
        public void submit(PoseStack pose, SubmitNodeCollector collector, int light, S state, float yRot, float xRot) {
            if (!state.isInvisible) {
                collector.submitModel(this.getParentModel(), state, pose, RenderTypes.eyes(this.texture.apply(state)), 0xF000F0,
                        OverlayTexture.NO_OVERLAY, -1, null, state.outlineColor, null);
            }
        }
    }

    // ----- Ovelha-de-Contar
    public static class Sheep extends MobRenderer<CountingSheepEntity, DreamMobRenderState, CountingSheepModel> {
        private static final Identifier TEXTURE = tex("counting_sheep");

        public Sheep(EntityRendererProvider.Context context) {
            super(context, new CountingSheepModel(context.bakeLayer(CountingSheepModel.LAYER)), 0.6F);
        }

        @Override
        public DreamMobRenderState createRenderState() {
            return new DreamMobRenderState();
        }

        @Override
        public void extractRenderState(CountingSheepEntity entity, DreamMobRenderState state, float partialTicks) {
            super.extractRenderState(entity, state, partialTicks);
            state.sheared = entity.isSheared();
        }

        @Override
        public Identifier getTextureLocation(DreamMobRenderState state) {
            return TEXTURE;
        }
    }

    // ----- gnomos (mesmo modelo do Gnomo, outra textura)
    public static class Gnome<T extends Mob> extends MobRenderer<T, GnomeRenderState, GnomeModel> {
        private final Identifier texture;

        public Gnome(EntityRendererProvider.Context context, String name, GnomeModel model, boolean glow) {
            super(context, model, 0.3F);
            this.texture = tex(name);
            if (glow) {
                Identifier glowTex = tex(name + "_glow");
                this.addLayer(new Glow<>(this, s -> glowTex));
            }
        }

        @Override
        public GnomeRenderState createRenderState() {
            return new GnomeRenderState();
        }

        @Override
        public void extractRenderState(T entity, GnomeRenderState state, float partialTicks) {
            super.extractRenderState(entity, state, partialTicks);
            state.attackAnim = entity.getAttackAnim(partialTicks);
        }

        @Override
        public Identifier getTextureLocation(GnomeRenderState state) {
            return this.texture;
        }
    }

    // ----- Bicho-Papão
    public static class Bogeyman extends MobRenderer<BogeymanEntity, DreamMobRenderState, BogeymanModel> {
        private static final Identifier TEXTURE = tex("bogeyman"), GLOW = tex("bogeyman_glow");

        public Bogeyman(EntityRendererProvider.Context context) {
            super(context, new BogeymanModel(context.bakeLayer(BogeymanModel.LAYER)), 0.6F);
            this.addLayer(new Glow<>(this, s -> GLOW));
        }

        @Override
        public DreamMobRenderState createRenderState() {
            return new DreamMobRenderState();
        }

        @Override
        public void extractRenderState(BogeymanEntity entity, DreamMobRenderState state, float partialTicks) {
            super.extractRenderState(entity, state, partialTicks);
            state.bright = entity.inBrightLight();
            state.attackTime = entity.getAttackAnim(partialTicks);
        }

        @Override
        public Identifier getTextureLocation(DreamMobRenderState state) {
            return TEXTURE;
        }
    }

    // ----- Sombra
    public static class Shade extends MobRenderer<ShadeEntity, DreamMobRenderState, ShadeModel> {
        private static final Identifier TEXTURE = tex("shade"), GLOW = tex("shade_glow");

        public Shade(EntityRendererProvider.Context context) {
            super(context, new ShadeModel(context.bakeLayer(ShadeModel.LAYER)), 0.0F);
            this.addLayer(new Glow<>(this, s -> GLOW));
        }

        @Override
        public DreamMobRenderState createRenderState() {
            return new DreamMobRenderState();
        }

        @Override
        public void extractRenderState(ShadeEntity entity, DreamMobRenderState state, float partialTicks) {
            super.extractRenderState(entity, state, partialTicks);
            state.watched = entity.isWatched();
        }

        @Override
        public Identifier getTextureLocation(DreamMobRenderState state) {
            return TEXTURE;
        }
    }
}

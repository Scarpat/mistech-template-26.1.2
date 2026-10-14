package com.mistech.mistechmod;

import com.mistech.mistechmod.client.AstrologyScreen;
import com.mistech.mistechmod.client.CondenserScreen;
import com.mistech.mistechmod.network.OpenAstrologyPayload;
import com.mistech.mistechmod.network.OpenGuidePayload;
import com.mistech.mistechmod.client.guide.GuideMapScreen;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import com.mistech.mistechmod.client.GlassJarRenderer;
import com.mistech.mistechmod.client.IncenseHolderScreen;
import com.mistech.mistechmod.registry.MistechBlockEntities;
import com.mistech.mistechmod.client.RunicFurnaceScreen;
import com.mistech.mistechmod.client.WritingTableScreen;
import com.mistech.mistechmod.registry.MistechEffects;
import com.mistech.mistechmod.registry.MistechMenus;

import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
import com.mistech.mistechmod.network.OpenBackpackPayload;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import com.mistech.mistechmod.registry.MistechFluids;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = Mistech.MODID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = Mistech.MODID, value = Dist.CLIENT)
public class MistechClient {
    public MistechClient(ModContainer container) {
        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your mod > clicking on config.
        // Do not forget to add translations for your config options to the en_us.json file.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        // Some client setup code
        Mistech.LOGGER.info("HELLO FROM CLIENT SETUP");
        // texturas das placas da Árvore Turva
        event.enqueueWork(() -> net.minecraft.client.renderer.Sheets.addWoodType(com.mistech.mistechmod.registry.MurkyWood.WOOD_TYPE));
        Mistech.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
    }

    @SubscribeEvent
    static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(MistechMenus.INCENSE_HOLDER.get(), IncenseHolderScreen::new);
        event.register(MistechMenus.WRITING_TABLE.get(), WritingTableScreen::new);
        event.register(MistechMenus.RUNIC_FURNACE.get(), RunicFurnaceScreen::new);
        event.register(MistechMenus.CONDENSER.get(), CondenserScreen::new);
        event.register(MistechMenus.SPATIAL_STORAGE.get(), com.mistech.mistechmod.client.SpatialStorageScreen::new);
        event.register(MistechMenus.RUNE_CONFECTOR.get(), com.mistech.mistechmod.client.RuneConfectorScreen::new);
        event.register(MistechMenus.RUNE_ROLLER.get(), com.mistech.mistechmod.client.RuneRollerScreen::new);
        event.register(MistechMenus.RUNE_ANVIL.get(), com.mistech.mistechmod.client.RuneAnvilScreen::new);
        event.register(MistechMenus.RUNIC_CRUSHER.get(), com.mistech.mistechmod.client.MachineScreen::new);
        event.register(MistechMenus.ACCESSORIES.get(), com.mistech.mistechmod.client.AccessoriesScreen::new);
        event.register(MistechMenus.ASH_GENERATOR.get(), com.mistech.mistechmod.client.MachineScreen::new);
        event.register(MistechMenus.BLAZE_GENERATOR.get(), com.mistech.mistechmod.client.MachineScreen::new);
        event.register(MistechMenus.GNOME_BICYCLE.get(), com.mistech.mistechmod.client.MachineScreen::new);
        event.register(MistechMenus.ASTRAL_GENERATOR.get(), com.mistech.mistechmod.client.MachineScreen::new);
        event.register(MistechMenus.RUNIC_HARVESTER.get(), com.mistech.mistechmod.client.MachineScreen::new);
        event.register(MistechMenus.RUNIC_PLANTER.get(), com.mistech.mistechmod.client.MachineScreen::new);
        event.register(MistechMenus.MINER.get(), com.mistech.mistechmod.client.MinerScreen::new);
        event.register(MistechMenus.RUNIC_BATTERY.get(), com.mistech.mistechmod.client.MachineScreen::new);
        event.register(MistechMenus.ADVANCED_RUNIC_BATTERY.get(), com.mistech.mistechmod.client.MachineScreen::new);
        event.register(MistechMenus.IMPURE_REACTOR.get(), com.mistech.mistechmod.purity.client.FluidMachineScreen::new);
        event.register(MistechMenus.STELLAR_OBSERVATORY.get(), com.mistech.mistechmod.client.MachineScreen::new);
        event.register(MistechMenus.IGNEOUS_BOILER.get(), com.mistech.mistechmod.purity.client.FluidMachineScreen::new);
        event.register(MistechMenus.EXPERIENCE_GENERATOR.get(), com.mistech.mistechmod.purity.client.FluidMachineScreen::new);
    }

    /** Desenha o item guardado dentro do Pote de Vidro. */
    @SubscribeEvent
    static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(MistechBlockEntities.GLASS_JAR.get(), GlassJarRenderer::new);
        event.registerBlockEntityRenderer(MistechBlockEntities.MAGIC_COMPRESSOR.get(), com.mistech.mistechmod.client.MagicCompressorRenderer::new);
        event.registerBlockEntityRenderer(com.mistech.mistechmod.husbandry.HusbandryContent.GROWING_POT_BE.get(),
                com.mistech.mistechmod.husbandry.client.GrowingPotRenderer::new);
        event.registerBlockEntityRenderer(com.mistech.mistechmod.husbandry.HusbandryContent.CREATURE_JAR_BE.get(),
                com.mistech.mistechmod.husbandry.client.CreatureJarRenderer::new);
        event.registerBlockEntityRenderer(MistechBlockEntities.WRITING_TABLE.get(), com.mistech.mistechmod.client.WritingTableRenderer::new);
        event.registerBlockEntityRenderer(MistechBlockEntities.RUNIC_TANK.get(), com.mistech.mistechmod.client.RunicTankRenderer::new);
        event.registerBlockEntityRenderer(MistechBlockEntities.RUNE_CONFECTOR.get(), com.mistech.mistechmod.client.RuneConfectorRenderer::new);
        event.registerBlockEntityRenderer(MistechBlockEntities.MINER.get(), com.mistech.mistechmod.client.MinerRenderer::new);
        event.registerEntityRenderer(com.mistech.mistechmod.registry.GnomeContent.GNOME.get(), com.mistech.mistechmod.client.GnomeRenderer::new);
        event.registerEntityRenderer(com.mistech.mistechmod.registry.DreamContent.PISADEIRA.get(), com.mistech.mistechmod.client.PisadeiraRenderer::new);
        event.registerEntityRenderer(com.mistech.mistechmod.registry.RealmContent.GNOME_KING.get(), com.mistech.mistechmod.client.GnomeKingRenderer::new);
        event.registerEntityRenderer(com.mistech.mistechmod.registry.RealmContent.EVIL_EYE.get(), com.mistech.mistechmod.client.EvilEyeRenderer::new);
        event.registerEntityRenderer(com.mistech.mistechmod.registry.GoblinContent.CORRUPTED_GOBLIN.get(), ctx -> new com.mistech.mistechmod.client.GoblinFamilyRenderer<>(ctx,
                com.mistech.mistechmod.client.CorruptedGoblinModel.LAYER, com.mistech.mistechmod.client.CorruptedGoblinModel::new, "corrupted_goblin", "eyes", 0.35F, 1.0F));
        event.registerEntityRenderer(com.mistech.mistechmod.registry.GoblinContent.TROLL.get(), ctx -> new com.mistech.mistechmod.client.GoblinFamilyRenderer<>(ctx,
                com.mistech.mistechmod.client.TrollModel.LAYER, com.mistech.mistechmod.client.TrollModel::new, "troll", "eyes", 0.9F, 1.0F));
        event.registerEntityRenderer(com.mistech.mistechmod.registry.GoblinContent.ABERRATION.get(), ctx -> new com.mistech.mistechmod.client.GoblinFamilyRenderer<>(ctx,
                com.mistech.mistechmod.client.AberrationModel.LAYER, com.mistech.mistechmod.client.AberrationModel::new, "aberration", "eyes", 0.6F, 1.0F));
        event.registerEntityRenderer(com.mistech.mistechmod.registry.GoblinContent.GOBLIN_QUEEN.get(), ctx -> new com.mistech.mistechmod.client.GoblinFamilyRenderer<>(ctx,
                com.mistech.mistechmod.client.GoblinQueenModel.LAYER, com.mistech.mistechmod.client.GoblinQueenModel::new, "goblin_queen", "glow", 0.9F, 1.0F));
        // Rituais Tier I
        event.registerBlockEntityRenderer(com.mistech.mistechmod.registry.RitualContent.IMPURE_ALTAR_BE.get(),
                context -> new com.mistech.mistechmod.client.FloatingItemRenderer<>(context, 1.1F));
        event.registerBlockEntityRenderer(com.mistech.mistechmod.registry.RitualContent.RITUAL_PEDESTAL_BE.get(),
                context -> new com.mistech.mistechmod.client.FloatingItemRenderer<>(context, 1.05F));
        event.registerEntityRenderer(com.mistech.mistechmod.registry.RitualContent.OWL.get(), com.mistech.mistechmod.client.OwlRenderer::new);
        // folclore
        event.registerEntityRenderer(com.mistech.mistechmod.folklore.FolkloreContent.SACI.get(), com.mistech.mistechmod.folklore.client.FolkRenderers.Saci::new);
        event.registerEntityRenderer(com.mistech.mistechmod.folklore.FolkloreContent.CURUPIRA.get(), com.mistech.mistechmod.folklore.client.FolkRenderers.Curupira::new);
        event.registerEntityRenderer(com.mistech.mistechmod.folklore.FolkloreContent.RAVEN.get(), com.mistech.mistechmod.folklore.client.FolkRenderers.Raven::new);
        event.registerEntityRenderer(com.mistech.mistechmod.beyond.BeyondContent.ECHOLING.get(), com.mistech.mistechmod.beyond.client.EcholingRenderer::new);
        event.registerEntityRenderer(com.mistech.mistechmod.beyond.PaleNetherContent.PALE_SENTINEL.get(), com.mistech.mistechmod.beyond.client.PaleNetherRenderers.Sentinel::new);
        event.registerEntityRenderer(com.mistech.mistechmod.beyond.PaleNetherContent.RUNIC_STRIDER.get(), com.mistech.mistechmod.beyond.client.PaleNetherRenderers.RunicStrider::new);
        event.registerEntityRenderer(com.mistech.mistechmod.folklore.FolkloreContent.WEREWOLF.get(), com.mistech.mistechmod.folklore.client.FolkRenderers.Werewolf::new);
        event.registerEntityRenderer(com.mistech.mistechmod.folklore.FolkloreContent.BOITATA.get(), com.mistech.mistechmod.folklore.client.FolkRenderers.Boitata::new);
        event.registerEntityRenderer(com.mistech.mistechmod.boitata.BoitataGear.MOUNT.get(), com.mistech.mistechmod.boitata.client.BoitataMountRenderer::new);
        event.registerEntityRenderer(com.mistech.mistechmod.folklore.FolkloreContent.HEADLESS_MULE.get(), com.mistech.mistechmod.folklore.client.FolkRenderers.HeadlessMule::new);
        event.registerEntityRenderer(com.mistech.mistechmod.registry.RitualContent.BLACK_CAT.get(), net.minecraft.client.renderer.entity.CatRenderer::new);
        event.registerEntityRenderer(com.mistech.mistechmod.registry.GolemContent.CLAY_GOLEM.get(), com.mistech.mistechmod.client.ClayGolemRenderer::new);
        // barcos da Árvore Turva (texturas em textures/entity/boat/murky.png e chest_boat/murky.png)
        event.registerEntityRenderer(com.mistech.mistechmod.registry.MurkyWood.BOAT_ENTITY.get(),
                context -> new net.minecraft.client.renderer.entity.BoatRenderer(context, MURKY_BOAT_LAYER));
        event.registerEntityRenderer(com.mistech.mistechmod.registry.MurkyWood.CHEST_BOAT_ENTITY.get(),
                context -> new net.minecraft.client.renderer.entity.BoatRenderer(context, MURKY_CHEST_BOAT_LAYER));
    }

    /** Âncora Espacial: runa e cristal tingidos pela cor do bloco (camada 0). */
    @SubscribeEvent
    static void onBlockTints(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.BlockTintSources event) {
        event.register(java.util.List.of(new net.minecraft.client.color.block.BlockTintSource() {
            @Override
            public int color(net.minecraft.world.level.block.state.BlockState state) {
                return com.mistech.mistechmod.spatial.SpatialAnchorBlock.tint(state);
            }

            @Override
            public java.util.Set<net.minecraft.world.level.block.state.properties.Property<?>> relevantProperties() {
                return java.util.Set.of(com.mistech.mistechmod.spatial.SpatialAnchorBlock.COLOR);
            }
        }), com.mistech.mistechmod.registry.SpatialContent.SPATIAL_ANCHOR.get());
    }

    @SubscribeEvent
    static void onRegisterClientPayloads(RegisterClientPayloadHandlersEvent event) {
        event.register(OpenAstrologyPayload.TYPE, (payload, context) ->
                Minecraft.getInstance().setScreen(new AstrologyScreen(payload.tablePos())));
        event.register(com.mistech.mistechmod.celestial.CelestialSyncPayload.TYPE, (payload, context) ->
                com.mistech.mistechmod.celestial.CelestialClient.handle(payload));
        event.register(com.mistech.mistechmod.network.OpenAnchorsPayload.TYPE, (payload, context) ->
                Minecraft.getInstance().setScreen(new com.mistech.mistechmod.client.AnchorScreen(payload)));
        event.register(com.mistech.mistechmod.network.OpenTarotPayload.TYPE, (payload, context) ->
                Minecraft.getInstance().setScreen(new com.mistech.mistechmod.client.TarotScreen(payload.tablePos())));
        event.register(OpenGuidePayload.TYPE, (payload, context) ->
                GuideMapScreen.open());
        event.register(com.mistech.mistechmod.network.ConfectorRecipesPayload.TYPE, (payload, context) ->
                com.mistech.mistechmod.client.ClientConfectorRecipes.set(payload.entries()));
    }

    /** Barril 3D nas costas de quem veste o Barril Carregável (jogadores e manequins). */
    @SubscribeEvent
    static void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(com.mistech.mistechmod.client.MysticWearLayer.LAYER, com.mistech.mistechmod.client.MysticWearLayer::createLayer);
        event.registerLayerDefinition(com.mistech.mistechmod.mage.client.MageWearLayer.LAYER, com.mistech.mistechmod.mage.client.MageWearLayer::createLayer);
        event.registerLayerDefinition(com.mistech.mistechmod.boitata.client.BoitataWearLayer.LAYER, com.mistech.mistechmod.boitata.client.BoitataWearLayer::createLayer);
        event.registerLayerDefinition(com.mistech.mistechmod.boitata.client.BoitataMountModel.LAYER, com.mistech.mistechmod.boitata.client.BoitataMountModel::createBodyLayer);
        event.registerLayerDefinition(com.mistech.mistechmod.boitata.client.BoitataMountArmorModel.LAYER, com.mistech.mistechmod.boitata.client.BoitataMountArmorModel::createBodyLayer);
        event.registerLayerDefinition(com.mistech.mistechmod.client.GnomeModel.LAYER, com.mistech.mistechmod.client.GnomeModel::createBodyLayer);
        event.registerLayerDefinition(com.mistech.mistechmod.folklore.client.FolkRenderers.HeadlessModel.LAYER, com.mistech.mistechmod.folklore.client.FolkRenderers.HeadlessModel::createBodyLayer);
        event.registerLayerDefinition(com.mistech.mistechmod.client.PisadeiraModel.LAYER, com.mistech.mistechmod.client.PisadeiraModel::createBodyLayer);
        event.registerLayerDefinition(com.mistech.mistechmod.client.EvilEyeModel.LAYER, com.mistech.mistechmod.client.EvilEyeModel::createBodyLayer);
        event.registerLayerDefinition(com.mistech.mistechmod.client.OwlModel.LAYER, com.mistech.mistechmod.client.OwlModel::createBodyLayer);
        event.registerLayerDefinition(com.mistech.mistechmod.folklore.client.SaciModel.LAYER, com.mistech.mistechmod.folklore.client.SaciModel::createBodyLayer);
        event.registerLayerDefinition(com.mistech.mistechmod.folklore.client.CurupiraModel.LAYER, com.mistech.mistechmod.folklore.client.CurupiraModel::createBodyLayer);
        event.registerLayerDefinition(com.mistech.mistechmod.folklore.client.RavenModel.LAYER, com.mistech.mistechmod.folklore.client.RavenModel::createBodyLayer);
        event.registerLayerDefinition(com.mistech.mistechmod.beyond.client.EcholingModel.LAYER, com.mistech.mistechmod.beyond.client.EcholingModel::createBodyLayer);
        event.registerLayerDefinition(com.mistech.mistechmod.folklore.client.WerewolfModel.LAYER, com.mistech.mistechmod.folklore.client.WerewolfModel::createBodyLayer);
        event.registerLayerDefinition(com.mistech.mistechmod.folklore.client.BoitataModel.LAYER, com.mistech.mistechmod.folklore.client.BoitataModel::createBodyLayer);
        event.registerLayerDefinition(com.mistech.mistechmod.folklore.client.BoitataSegmentModel.LAYER, com.mistech.mistechmod.folklore.client.BoitataSegmentModel::createBodyLayer);
        event.registerLayerDefinition(com.mistech.mistechmod.client.ClayGolemModel.LAYER, com.mistech.mistechmod.client.ClayGolemModel::createBodyLayer);
        event.registerLayerDefinition(com.mistech.mistechmod.client.CorruptedGoblinModel.LAYER, com.mistech.mistechmod.client.CorruptedGoblinModel::createBodyLayer);
        event.registerLayerDefinition(com.mistech.mistechmod.client.TrollModel.LAYER, com.mistech.mistechmod.client.TrollModel::createBodyLayer);
        event.registerLayerDefinition(com.mistech.mistechmod.client.AberrationModel.LAYER, com.mistech.mistechmod.client.AberrationModel::createBodyLayer);
        event.registerLayerDefinition(com.mistech.mistechmod.client.GoblinQueenModel.LAYER, com.mistech.mistechmod.client.GoblinQueenModel::createBodyLayer);
        event.registerLayerDefinition(MURKY_BOAT_LAYER, net.minecraft.client.model.object.boat.BoatModel::createBoatModel);
        event.registerLayerDefinition(MURKY_CHEST_BOAT_LAYER, net.minecraft.client.model.object.boat.BoatModel::createChestBoatModel);
    }

    public static final net.minecraft.client.model.geom.ModelLayerLocation MURKY_BOAT_LAYER = new net.minecraft.client.model.geom.ModelLayerLocation(
            Identifier.fromNamespaceAndPath(Mistech.MODID, "boat/murky"), "main");
    public static final net.minecraft.client.model.geom.ModelLayerLocation MURKY_CHEST_BOAT_LAYER = new net.minecraft.client.model.geom.ModelLayerLocation(
            Identifier.fromNamespaceAndPath(Mistech.MODID, "chest_boat/murky"), "main");

    private static void unused() {
    }

    @SubscribeEvent
    static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        var wearModel = event.getEntityModels().bakeLayer(com.mistech.mistechmod.client.MysticWearLayer.LAYER);
        var mageModel = event.getEntityModels().bakeLayer(com.mistech.mistechmod.mage.client.MageWearLayer.LAYER);
        var boitataModel = event.getEntityModels().bakeLayer(com.mistech.mistechmod.boitata.client.BoitataWearLayer.LAYER);
        for (var skin : event.getSkins()) {
            net.minecraft.client.renderer.entity.player.AvatarRenderer<net.minecraft.client.player.AbstractClientPlayer> wearer =
                    event.getPlayerRenderer(skin);
            if (wearer != null) {
                wearer.addLayer(new com.mistech.mistechmod.client.MysticWearLayer(wearer, wearModel));
                wearer.addLayer(new com.mistech.mistechmod.mage.client.MageWearLayer(wearer, mageModel));
                wearer.addLayer(new com.mistech.mistechmod.boitata.client.BoitataWearLayer(wearer, boitataModel));
            }
            net.minecraft.client.renderer.entity.player.AvatarRenderer<net.minecraft.client.entity.ClientMannequin> wearerMannequin =
                    event.getMannequinRenderer(skin);
            if (wearerMannequin != null) {
                wearerMannequin.addLayer(new com.mistech.mistechmod.client.MysticWearLayer(wearerMannequin, wearModel));
                wearerMannequin.addLayer(new com.mistech.mistechmod.mage.client.MageWearLayer(wearerMannequin, mageModel));
                wearerMannequin.addLayer(new com.mistech.mistechmod.boitata.client.BoitataWearLayer(wearerMannequin, boitataModel));
            }
            net.minecraft.client.renderer.entity.player.AvatarRenderer<net.minecraft.client.player.AbstractClientPlayer> player =
                    event.getPlayerRenderer(skin);
            if (player != null) {
                player.addLayer(new com.mistech.mistechmod.client.BackpackLayer(player));
            }
            net.minecraft.client.renderer.entity.player.AvatarRenderer<net.minecraft.client.entity.ClientMannequin> mannequin =
                    event.getMannequinRenderer(skin);
            if (mannequin != null) {
                mannequin.addLayer(new com.mistech.mistechmod.client.BackpackLayer(mannequin));
            }
        }
    }

    /** Tecla da mochila (B). */
    public static final KeyMapping.Category KEY_CATEGORY =
            new KeyMapping.Category(Identifier.fromNamespaceAndPath(Mistech.MODID, "mistech"));
    public static final KeyMapping OPEN_BACKPACK = new KeyMapping("key.mistech.open_backpack",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, KEY_CATEGORY);

    /** Tecla dos acessórios (J). */
    public static final KeyMapping OPEN_ACCESSORIES = new KeyMapping("key.mistech.open_accessories",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, KEY_CATEGORY);

    @SubscribeEvent
    static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.registerCategory(KEY_CATEGORY);
        event.register(OPEN_BACKPACK);
        event.register(OPEN_ACCESSORIES);
        event.register(com.mistech.mistechmod.artifact.client.ArtifactClient.SPATIAL_RING);
        event.register(com.mistech.mistechmod.fey.client.FeyClient.CROWN_CALL);
        event.register(com.mistech.mistechmod.boitata.client.BoitataClient.FIRE_BREATH);
    }

    /** Filtro de cores da Visão Mística (assets/mistech/post_effect/mystic_sight.json). */
    private static final Identifier MYSTIC_POST_EFFECT = Identifier.fromNamespaceAndPath(Mistech.MODID, "mystic_sight");
    /** Visão Psicodélica: shader próprio (assets/mistech/shaders/post/psychedelic.fsh). */
    private static final Identifier PSYCHEDELIC_POST_EFFECT = Identifier.fromNamespaceAndPath(Mistech.MODID, "psychedelic");
    /** Dimensão dos Gnomos: filtro de sonho leve (assets/mistech/shaders/post/gnome_dream.fsh). */
    private static final Identifier DREAM_POST_EFFECT = Identifier.fromNamespaceAndPath(Mistech.MODID, "gnome_dream");

    /** Visão Mística: a câmera balança devagar para os lados. */
    @SubscribeEvent
    static void onCameraAngles(net.neoforged.neoforge.client.event.ViewportEvent.ComputeCameraAngles event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.player.hasEffect(MistechEffects.PSYCHEDELIC_SIGHT)) {
            // psicodélico: balança mais e "respira"
            double time = minecraft.player.tickCount + event.getPartialTick();
            event.setRoll(event.getRoll() + (float) (Math.sin(time * 0.07) * 7.0 + Math.sin(time * 0.23) * 1.5));
            event.setYaw(event.getYaw() + (float) (Math.sin(time * 0.043) * 2.5));
            event.setPitch(event.getPitch() + (float) (Math.sin(time * 0.057) * 1.5));
        } else if (minecraft.player != null && minecraft.player.hasEffect(MistechEffects.MYSTIC_SIGHT)) {
            double time = minecraft.player.tickCount + event.getPartialTick();
            event.setRoll(event.getRoll() + (float) (Math.sin(time * 0.05) * 4.0));
            event.setYaw(event.getYaw() + (float) (Math.sin(time * 0.031) * 1.2));
        }
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.gameRenderer != null) {
            // o psicodélico ganha do místico
            Identifier wanted = minecraft.player.hasEffect(MistechEffects.PSYCHEDELIC_SIGHT) ? PSYCHEDELIC_POST_EFFECT
                    : minecraft.player.hasEffect(MistechEffects.MYSTIC_SIGHT) ? MYSTIC_POST_EFFECT
                    : minecraft.player.level().dimension() == com.mistech.mistechmod.registry.RealmContent.REALM ? DREAM_POST_EFFECT : null;
            Identifier current = minecraft.gameRenderer.currentPostEffect();
            boolean ours = MYSTIC_POST_EFFECT.equals(current) || PSYCHEDELIC_POST_EFFECT.equals(current) || DREAM_POST_EFFECT.equals(current);
            if (wanted != null && !wanted.equals(current)) {
                minecraft.gameRenderer.setPostEffect(wanted);
            } else if (wanted == null && ours) {
                minecraft.gameRenderer.clearPostEffect();
            }
        }
        while (OPEN_ACCESSORIES.consumeClick()) {
            if (minecraft.player != null && minecraft.screen == null) {
                ClientPacketDistributor.sendToServer(com.mistech.mistechmod.network.OpenAccessoriesPayload.INSTANCE);
            }
        }
        while (OPEN_BACKPACK.consumeClick()) {
            if (minecraft.player != null && minecraft.screen == null) {
                ClientPacketDistributor.sendToServer(OpenBackpackPayload.INSTANCE);
            }
        }
    }

    /** Cajado do Gnomo e Cachimbo Rúnico: Shift + roda do mouse troca a runa escolhida. */
    @SubscribeEvent
    static void onMouseScroll(net.neoforged.neoforge.client.event.InputEvent.MouseScrollingEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null || !minecraft.player.isShiftKeyDown() || event.getScrollDeltaY() == 0) {
            return;
        }
        if (com.mistech.mistechmod.item.staff.GnomeStaffItem.holdsRunes(minecraft.player.getMainHandItem())
                || com.mistech.mistechmod.item.staff.GnomeStaffItem.holdsRunes(minecraft.player.getOffhandItem())
                || com.mistech.mistechmod.tarot.magus.MagusStaffItem.is(minecraft.player.getMainHandItem())
                || com.mistech.mistechmod.tarot.magus.MagusStaffItem.is(minecraft.player.getOffhandItem())
                || com.mistech.mistechmod.ranged.ArcaneCrossbowItem.is(minecraft.player.getMainHandItem())
                || com.mistech.mistechmod.ranged.ArcaneCrossbowItem.is(minecraft.player.getOffhandItem())) {
            event.setCanceled(true);
            ClientPacketDistributor.sendToServer(new com.mistech.mistechmod.network.StaffScrollPayload(event.getScrollDeltaY() > 0 ? -1 : 1));
        }
    }

    /** Foco do Tabaco: fecha o campo de visão em 20%. */
    @SubscribeEvent
    static void onComputeFov(ComputeFovModifierEvent event) {
        if (event.getPlayer().hasEffect(MistechEffects.TOBACCO_FOCUS)) {
            event.setNewFovModifier(event.getNewFovModifier() * 0.8F);
        }
        // Visão Mística: abre o campo de visão
        if (event.getPlayer().hasEffect(MistechEffects.PSYCHEDELIC_SIGHT)) {
            // campo de visão "respirando"
            float t = event.getPlayer().tickCount;
            event.setNewFovModifier(event.getNewFovModifier() * (1.35F + 0.12F * (float) Math.sin(t * 0.08)));
        } else if (event.getPlayer().hasEffect(MistechEffects.MYSTIC_SIGHT)) {
            event.setNewFovModifier(event.getNewFovModifier() * 1.3F);
        }
    }

    /** Texturas da Essência Mágica Impura (parada e escorrendo). */
    @SubscribeEvent
    static void onRegisterFluidModels(RegisterFluidModelsEvent event) {
        event.register(new FluidModel.Unbaked(
                new Material(Identifier.fromNamespaceAndPath(Mistech.MODID, "block/impure_essence_still")),
                new Material(Identifier.fromNamespaceAndPath(Mistech.MODID, "block/impure_essence_flow")),
                null, null, null), MistechFluids.IMPURE_ESSENCE, MistechFluids.FLOWING_IMPURE_ESSENCE);
        // metais derretidos (Productive Metalworks)
        for (com.mistech.mistechmod.fluid.MoltenMetals.Molten molten : com.mistech.mistechmod.fluid.MoltenMetals.ALL) {
            event.register(new FluidModel.Unbaked(
                    new Material(Identifier.fromNamespaceAndPath(Mistech.MODID, "block/molten_" + molten.metal + "_still")),
                    new Material(Identifier.fromNamespaceAndPath(Mistech.MODID, "block/molten_" + molten.metal + "_flow")),
                    null, null, null), molten.source, molten.flowing);
        }
    }
}

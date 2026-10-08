package com.mistech.mistechmod.tarot.fool.client;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.tarot.fool.FoolContent;
import com.mistech.mistechmod.tarot.fool.FoolEvents;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** Cliente do Louco: desenho do chefe e do tornado, e o Salto do Louco (Chapéu do Bobo). */
@EventBusSubscriber(modid = Mistech.MODID, value = Dist.CLIENT)
public final class FoolClient {
    private static int crouchTicks;
    private static boolean jumpWasDown;
    private static boolean wasOnGround;
    private static long lastLeap = -1000L;

    private FoolClient() {}

    @SubscribeEvent
    static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(FoolContent.FOOL.get(), FoolRenderer::new);
        event.registerEntityRenderer(FoolContent.TORNADO.get(), FoolTornadoRenderer::new);
    }

    @SubscribeEvent
    static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(FoolModel.LAYER, FoolModel::createBodyLayer);
        event.registerLayerDefinition(FoolTornadoRenderer.LAYER, FoolTornadoRenderer::createLayer);
    }

    /**
     * Salto do Louco: agachado no chão por {@link FoolEvents#LEAP_CHARGE} ticks (os guizos brilham) e pular →
     * impulso grande para frente e para cima. O movimento do jogador é do cliente, então o impulso é aplicado aqui;
     * o servidor só confere, faz os efeitos e protege da queda.
     */
    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.screen != null) {
            crouchTicks = 0;
            return;
        }
        boolean jumpDown = player.input.keyPresses.jump();
        boolean wearing = FoolEvents.wearsCap(player);
        long now = player.level().getGameTime();
        boolean ready = now - lastLeap >= FoolEvents.LEAP_COOLDOWN;

        if (wearing && ready && player.onGround() && player.isShiftKeyDown()) {
            crouchTicks++;
            if (crouchTicks >= FoolEvents.LEAP_CHARGE && crouchTicks % 4 == 0) {
                player.level().addParticle(ParticleTypes.SMALL_GUST, player.getX(), player.getY() + 0.1, player.getZ(), 0.0, 0.0, 0.0);
            }
        } else if (!(wasOnGround && jumpDown && !jumpWasDown)) {
            if (player.onGround() && !player.isShiftKeyDown()) {
                crouchTicks = 0;
            }
        }

        // pulou (saindo do chão) com o salto carregado
        if (wearing && ready && jumpDown && !jumpWasDown && wasOnGround && crouchTicks >= FoolEvents.LEAP_CHARGE && !player.isPassenger()) {
            Vec3 look = player.getLookAngle().multiply(1.0, 0.0, 1.0);
            look = look.lengthSqr() < 1.0E-4 ? Vec3.ZERO : look.normalize();
            player.setDeltaMovement(look.x * FoolEvents.LEAP_FORWARD, FoolEvents.LEAP_UP, look.z * FoolEvents.LEAP_FORWARD);
            player.resetFallDistance();
            lastLeap = now;
            crouchTicks = 0;
            ClientPacketDistributor.sendToServer(FoolEvents.LeapPayload.INSTANCE);
        }
        jumpWasDown = jumpDown;
        wasOnGround = player.onGround();
    }
}

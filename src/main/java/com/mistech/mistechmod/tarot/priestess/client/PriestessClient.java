package com.mistech.mistechmod.tarot.priestess.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.tarot.priestess.PriestessContent;
import com.mistech.mistechmod.tarot.priestess.PriestessEntity;
import com.mistech.mistechmod.tarot.priestess.PriestessOmenPayload;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

/**
 * Cliente da Sacerdotisa: desenho dela e dos reflexos, os presságios na tela (o jogador está cego no modo Boaz, então
 * cada golpe aparece como um brilho na borda do lado de onde vem) e o Véu (não desenha quem está velado).
 */
@EventBusSubscriber(modid = Mistech.MODID, value = Dist.CLIENT)
public final class PriestessClient {
    private static final Identifier OMEN_HUD = Identifier.fromNamespaceAndPath(Mistech.MODID, "priestess_omens");

    /** Um presságio na tela: começa em {@code start} (ms) e dura {@code duration} ticks. */
    private record Omen(int kind, float yaw, long start, int duration, int value) {
        float progress(long now) {
            return Mth.clamp((now - this.start) / (this.duration * 50.0F), 0.0F, 1.0F);
        }

        boolean over(long now) {
            return now - this.start > this.duration * 50L + 250L;
        }
    }

    private static final List<Omen> OMENS = new ArrayList<>();

    private PriestessClient() {}

    @SubscribeEvent
    static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(PriestessContent.PRIESTESS.get(), context -> new PriestessRenderer<>(context, 0.75F));
        event.registerEntityRenderer(PriestessContent.REFLECTION.get(), context -> new PriestessRenderer<>(context, 0.0F));
    }

    @SubscribeEvent
    static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(PriestessModel.LAYER, PriestessModel::createBodyLayer);
    }

    @SubscribeEvent
    static void onPayloads(RegisterClientPayloadHandlersEvent event) {
        event.register(PriestessOmenPayload.TYPE, (payload, context) -> {
            synchronized (OMENS) {
                if (payload.kind() == PriestessOmenPayload.BOAZ || payload.kind() == PriestessOmenPayload.JACHIN) {
                    OMENS.clear();
                }
                OMENS.add(new Omen(payload.kind(), payload.yaw(), System.currentTimeMillis(), payload.duration(), payload.value()));
            }
        });
    }

    @SubscribeEvent
    static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, OMEN_HUD, PriestessClient::renderOmens);
    }

    // ------------------------------------------------------------------ Véu

    @SubscribeEvent
    static void onRenderPlayer(RenderPlayerEvent.Pre<?> event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        Entity entity = mc.level.getEntity(event.getRenderState().id);
        if (entity instanceof Player player && player != mc.player && player.getData(PriestessContent.VEILED)) {
            event.setCanceled(true);
        }
    }

    // ------------------------------------------------------------------ presságios

    private static void renderOmens(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        List<Omen> omens;
        synchronized (OMENS) {
            if (OMENS.isEmpty()) {
                return;
            }
            long now = System.currentTimeMillis();
            for (Iterator<Omen> it = OMENS.iterator(); it.hasNext(); ) {
                if (it.next().over(now)) {
                    it.remove();
                }
            }
            omens = new ArrayList<>(OMENS);
        }
        if (player == null || mc.options.hideGui) {
            return;
        }
        long now = System.currentTimeMillis();
        int w = g.guiWidth();
        int h = g.guiHeight();
        Font font = mc.font;
        for (Omen omen : omens) {
            float p = omen.progress(now);
            // pulsa cada vez mais rápido até o golpe
            float pulse = 0.55F + 0.45F * Mth.sin((now - omen.start) * (0.006F + p * 0.02F));
            float strength = Mth.clamp(p * 1.4F, 0.2F, 1.0F) * pulse;
            switch (omen.kind) {
                case PriestessOmenPayload.GUST -> {
                    float rel = Mth.wrapDegrees(omen.yaw - player.getYRot()) * Mth.DEG_TO_RAD;
                    float right = Math.max(0.0F, Mth.sin(rel));
                    float left = Math.max(0.0F, -Mth.sin(rel));
                    float front = Math.max(0.0F, Mth.cos(rel));
                    float back = Math.max(0.0F, -Mth.cos(rel));
                    int color = 0xBFE6FF;
                    edgeLeft(g, w, h, color, strength * left);
                    edgeRight(g, w, h, color, strength * right);
                    edgeTop(g, w, h, color, strength * front);
                    edgeBottom(g, w, h, color, strength * back);
                }
                case PriestessOmenPayload.POMEGRANATE -> {
                    // vermelho por cima e gotas caindo: "algo vai cair em você"
                    edgeTop(g, w, h, 0xC0182F, strength);
                    vignette(g, w, h, 0xC0182F, strength * 0.5F);
                    for (int i = 0; i < 7; i++) {
                        float fall = ((now - omen.start) / 600.0F + i * 0.37F) % 1.0F;
                        int x = w / 2 + (int) ((i - 3) * w * 0.09F);
                        int y = (int) (fall * h * 0.35F);
                        g.fill(x - 2, y, x + 2, y + 5, alpha(0xD02040, strength * (1.0F - fall)));
                    }
                }
                case PriestessOmenPayload.TIDE -> {
                    // a água sobe por baixo: "pule quando a onda chegar"
                    int height = (int) (h * (0.12F + 0.25F * p));
                    g.fillGradient(0, h - height, w, h, alpha(0x2A7BD8, 0.0F), alpha(0x2A7BD8, 0.75F * strength));
                }
                case PriestessOmenPayload.SILENCE -> {
                    float grace = omen.value / (float) Math.max(1, omen.duration);
                    vignette(g, w, h, 0x0F4A4A, 0.6F);
                    Component text = Component.translatable(p < grace ? "hud.mistech.priestess.silence" : "hud.mistech.priestess.silence_hold");
                    g.centeredText(font, text, w / 2, h / 2 - 30, alpha(0x9FF5E6, Math.max(0.3F, pulse)));
                }
                case PriestessOmenPayload.BOAZ -> {
                    float fade = 1.0F - p;
                    g.centeredText(font, Component.translatable("hud.mistech.priestess.boaz"), w / 2, h / 2 - 50, alpha(0xD0D0E0, fade));
                    g.centeredText(font, Component.translatable("hud.mistech.priestess.boaz.hint"), w / 2, h / 2 - 38, alpha(0x8090B0, fade));
                }
                case PriestessOmenPayload.JACHIN -> {
                    float fade = 1.0F - p;
                    g.fill(0, 0, w, h, alpha(0xFFFFFF, 0.35F * Math.max(0.0F, 1.0F - p * 4.0F)));
                    g.centeredText(font, Component.translatable("hud.mistech.priestess.jachin"), w / 2, h / 2 - 50, alpha(0xFFF4C8, fade));
                    g.centeredText(font, Component.translatable("hud.mistech.priestess.jachin.hint"), w / 2, h / 2 - 38, alpha(0xE0D8A8, fade));
                    if (omen.value > 0) {
                        int bonus = Math.round(omen.value * PriestessEntity.INTUITION_BONUS * 100.0F);
                        g.centeredText(font, Component.translatable("hud.mistech.priestess.intuition", omen.value, bonus),
                                w / 2, h / 2 - 26, alpha(0x9FE8FF, fade));
                    }
                }
                default -> {}
            }
        }
    }

    private static int alpha(int rgb, float a) {
        int alpha = Mth.clamp((int) (a * 255.0F), 0, 255);
        return (alpha << 24) | (rgb & 0xFFFFFF);
    }

    private static final int EDGE = 70;

    private static void edgeLeft(GuiGraphicsExtractor g, int w, int h, int color, float s) {
        if (s <= 0.02F) return;
        for (int x = 0; x < EDGE; x += 2) {
            g.fill(x, 0, x + 2, h, alpha(color, s * 0.8F * (1.0F - x / (float) EDGE)));
        }
    }

    private static void edgeRight(GuiGraphicsExtractor g, int w, int h, int color, float s) {
        if (s <= 0.02F) return;
        for (int x = 0; x < EDGE; x += 2) {
            g.fill(w - x - 2, 0, w - x, h, alpha(color, s * 0.8F * (1.0F - x / (float) EDGE)));
        }
    }

    private static void edgeTop(GuiGraphicsExtractor g, int w, int h, int color, float s) {
        if (s <= 0.02F) return;
        g.fillGradient(0, 0, w, EDGE, alpha(color, s * 0.8F), alpha(color, 0.0F));
    }

    private static void edgeBottom(GuiGraphicsExtractor g, int w, int h, int color, float s) {
        if (s <= 0.02F) return;
        g.fillGradient(0, h - EDGE, w, h, alpha(color, 0.0F), alpha(color, s * 0.8F));
    }

    private static void vignette(GuiGraphicsExtractor g, int w, int h, int color, float s) {
        edgeLeft(g, w, h, color, s);
        edgeRight(g, w, h, color, s);
        edgeTop(g, w, h, color, s);
        edgeBottom(g, w, h, color, s);
    }
}

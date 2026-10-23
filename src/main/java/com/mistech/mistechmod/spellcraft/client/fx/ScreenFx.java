package com.mistech.mistechmod.spellcraft.client.fx;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.spellcraft.fx.ScreenFxPayload;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

/** Tremor de câmera e clarão de tela das magias fortes. */
@EventBusSubscriber(modid = Mistech.MODID, value = Dist.CLIENT)
public final class ScreenFx {
    private static final Identifier FLASH_LAYER = Identifier.fromNamespaceAndPath(Mistech.MODID, "spell_flash");
    private static float shake;
    private static int shakeLeft;
    private static int shakeTotal;
    private static int flashColor;
    private static int flashLeft;
    private static int flashTotal;

    private ScreenFx() {}

    @SubscribeEvent
    static void onPayloads(RegisterClientPayloadHandlersEvent event) {
        event.register(ScreenFxPayload.TYPE, (payload, context) -> {
            if (payload.shakeTicks() > 0 && payload.shake() >= shake * shakeLeft / Math.max(1, shakeTotal)) {
                shake = payload.shake();
                shakeLeft = shakeTotal = payload.shakeTicks();
            }
            if (payload.flashTicks() > 0) {
                flashColor = payload.flash();
                flashLeft = flashTotal = payload.flashTicks();
            }
        });
    }

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        if (shakeLeft > 0) {
            shakeLeft--;
        }
        if (flashLeft > 0) {
            flashLeft--;
        }
    }

    @SubscribeEvent
    static void onCamera(ViewportEvent.ComputeCameraAngles event) {
        if (shakeLeft <= 0) {
            return;
        }
        float t = (shakeLeft - (float) event.getPartialTick()) / shakeTotal;
        float amount = shake * Mth.clamp(t, 0.0F, 1.0F) * 3.0F;
        double time = (Minecraft.getInstance().level == null ? 0 : Minecraft.getInstance().level.getGameTime()) + event.getPartialTick();
        event.setYaw(event.getYaw() + (float) Math.sin(time * 2.7) * amount);
        event.setPitch(event.getPitch() + (float) Math.cos(time * 3.1) * amount);
        event.setRoll(event.getRoll() + (float) Math.sin(time * 1.9) * amount * 0.6F);
    }

    @SubscribeEvent
    static void onLayers(RegisterGuiLayersEvent event) {
        event.registerBelow(VanillaGuiLayers.CROSSHAIR, FLASH_LAYER, ScreenFx::renderFlash);
    }

    private static void renderFlash(GuiGraphicsExtractor g, DeltaTracker delta) {
        if (flashLeft <= 0) {
            return;
        }
        float t = (flashLeft - delta.getGameTimeDeltaPartialTick(false)) / flashTotal;
        int alpha = (int) (Mth.clamp(t, 0.0F, 1.0F) * 110);
        if (alpha <= 0) {
            return;
        }
        int color = alpha << 24 | (flashColor & 0xFFFFFF);
        g.fill(0, 0, g.guiWidth(), g.guiHeight(), color);
        // vinheta: bordas um pouco mais fortes
        int edge = Math.min(alpha + 40, 180) << 24 | (flashColor & 0xFFFFFF);
        int w = g.guiWidth(), h = g.guiHeight(), b = Math.max(8, Math.min(w, h) / 10);
        g.fillGradient(0, 0, w, b, edge, color & 0x00FFFFFF);
        g.fillGradient(0, h - b, w, h, color & 0x00FFFFFF, edge);
    }
}

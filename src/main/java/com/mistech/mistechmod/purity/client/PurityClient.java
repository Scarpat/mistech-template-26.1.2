package com.mistech.mistechmod.purity.client;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.purity.Taint;
import com.mistech.mistechmod.purity.item.DowsingPendulumItem;
import com.mistech.mistechmod.registry.MistechFluids;
import com.mistech.mistechmod.registry.MistechMenus;
import com.mojang.serialization.MapCodec;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterRangeSelectItemModelPropertyEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/** Cliente da Magia Purificada: HUD do Pêndulo, cor do cristal do Pêndulo, fluido purificado e telas. */
@EventBusSubscriber(modid = Mistech.MODID, value = Dist.CLIENT)
public final class PurityClient {
    public static final Identifier TAINT_HUD = Identifier.fromNamespaceAndPath(Mistech.MODID, "taint_hud");
    public static final Identifier TAINT_PROPERTY = Identifier.fromNamespaceAndPath(Mistech.MODID, "taint");
    private static final int BAR_W = 101;

    private PurityClient() {}

    @SubscribeEvent
    static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, TAINT_HUD, PurityClient::renderHud);
    }

    /** Segurando o Pêndulo: "Mácula 37.0 · Manchada" e uma barra colorida acima da hotbar. */
    private static void renderHud(GuiGraphicsExtractor graphics, net.minecraft.client.DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.options.hideGui || !DowsingPendulumItem.isHeld(player)) {
            return;
        }
        float taint = Taint.clientValue(player);
        Taint.Stage stage = Taint.Stage.of(taint);
        int x = graphics.guiWidth() / 2 - BAR_W / 2;
        int y = graphics.guiHeight() - 68;
        Component text = Taint.describe(taint);
        int tw = minecraft.font.width(text);
        graphics.text(minecraft.font, text, graphics.guiWidth() / 2 - tw / 2, y - 10, 0xFFFFFFFF, true);
        graphics.fill(x - 1, y - 1, x + BAR_W + 1, y + 4, 0xC0000000);
        // trilho com as divisões das faixas
        for (Taint.Stage s : Taint.Stage.values()) {
            int from = x + Math.round(BAR_W * s.from / Taint.MAX);
            graphics.fill(from, y, Math.min(x + BAR_W, from + Math.round(BAR_W * 20 / Taint.MAX)), y + 3, (s.color & 0x00FFFFFF) | 0x50000000);
        }
        int filled = Math.round(BAR_W * taint / Taint.MAX);
        if (filled > 0) {
            graphics.fill(x, y, x + filled, y + 3, stage.color);
        }
        // marca do limite do early game (20)
        int floor = x + Math.round(BAR_W * Taint.EARLY_FLOOR / Taint.MAX);
        graphics.fill(floor, y - 1, floor + 1, y + 4, 0xFFFFFFFF);
    }

    /** Propriedade "mistech:taint" para o modelo do Pêndulo (range_dispatch): Mácula do jogador local. */
    public record TaintProperty() implements RangeSelectItemModelProperty {
        public static final MapCodec<TaintProperty> MAP_CODEC = MapCodec.unit(new TaintProperty());

        @Override
        public float get(ItemStack stack, ClientLevel level, ItemOwner owner, int seed) {
            Player player = Minecraft.getInstance().player;
            return player == null ? 0.0F : Taint.clientValue(player);
        }

        @Override
        public MapCodec<TaintProperty> type() {
            return MAP_CODEC;
        }
    }

    @SubscribeEvent
    static void onRegisterRangeProperties(RegisterRangeSelectItemModelPropertyEvent event) {
        event.register(TAINT_PROPERTY, TaintProperty.MAP_CODEC);
    }

    @SubscribeEvent
    static void onRegisterFluidModels(RegisterFluidModelsEvent event) {
        event.register(new FluidModel.Unbaked(
                new Material(Identifier.fromNamespaceAndPath(Mistech.MODID, "block/purified_essence_still")),
                new Material(Identifier.fromNamespaceAndPath(Mistech.MODID, "block/purified_essence_flow")),
                null, null, null), MistechFluids.PURIFIED_ESSENCE, MistechFluids.FLOWING_PURIFIED_ESSENCE);
    }

    @SubscribeEvent
    static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(MistechMenus.DISTILLER.get(), FluidMachineScreen::new);
        event.register(MistechMenus.PURE_GENERATOR.get(), FluidMachineScreen::new);
    }

    /** Usado pelo HUD e pela tela. */
    static ClientLevel level() {
        return Minecraft.getInstance().level;
    }
}

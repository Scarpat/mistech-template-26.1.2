package com.mistech.mistechmod.seeking.client;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.seeking.SeekKind;
import com.mistech.mistechmod.seeking.SeekTarget;
import com.mistech.mistechmod.seeking.SeekingPendulumItem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/** HUD do Pêndulo Localizador: "Vila dos Duendes · 340 m ↗" acima da hotbar. */
@EventBusSubscriber(modid = Mistech.MODID, value = Dist.CLIENT)
public final class SeekingClient {
    public static final Identifier SEEK_HUD = Identifier.fromNamespaceAndPath(Mistech.MODID, "seek_hud");
    private static final String[] ARROWS = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};

    private SeekingClient() {}

    @SubscribeEvent
    static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, SEEK_HUD, SeekingClient::renderHud);
    }

    private static void renderHud(GuiGraphicsExtractor graphics, net.minecraft.client.DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.options.hideGui || !SeekingPendulumItem.isHeld(player)) {
            return;
        }
        ItemStack stack = SeekingPendulumItem.held(player);
        SeekTarget target = SeekingPendulumItem.target(stack);
        SeekKind kind = target == null ? null : SeekKind.byId(target.kind());
        Component text;
        int color = 0xFFFFFFFF;
        if (kind == null) {
            text = Component.translatable("item.mistech.seeking_pendulum.hud_none");
        } else if (target.pos().isEmpty()) {
            text = Component.translatable("item.mistech.seeking_pendulum.hud_nothing", Component.translatable(kind.translationKey()));
            color = 0xFFA0A0A0;
        } else if (target.pos().get().dimension() != player.level().dimension()) {
            text = Component.translatable("item.mistech.seeking_pendulum.hud_elsewhere", Component.translatable(kind.translationKey()));
            color = 0xFFA0A0A0;
        } else {
            BlockPos pos = target.pos().get().pos();
            double dx = pos.getX() + 0.5 - player.getX();
            double dz = pos.getZ() + 0.5 - player.getZ();
            double dy = pos.getY() - player.getY();
            int dist = (int) Math.sqrt(dx * dx + dz * dz + dy * dy);
            // ângulo do alvo em relação para onde o jogador olha (0 = à frente)
            double targetYaw = Math.toDegrees(Math.atan2(-dx, dz));
            double rel = Mth.wrapDegrees(targetYaw - player.getYRot());
            String arrow = dist < 3 ? "◆" : ARROWS[Math.floorMod((int) Math.round(rel / 45.0), 8)];
            String vertical = Math.abs(dy) > 6 ? (dy > 0 ? " ▲" : " ▼") : "";
            text = Component.translatable("item.mistech.seeking_pendulum.hud", Component.translatable(kind.translationKey()), dist,
                    arrow + vertical);
            color = 0xFF000000 | kind.color;
        }
        int tw = minecraft.font.width(text);
        graphics.text(minecraft.font, text, graphics.guiWidth() / 2 - tw / 2, graphics.guiHeight() - 82, color, true);
    }
}

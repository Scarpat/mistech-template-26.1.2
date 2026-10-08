package com.mistech.mistechmod.client.guide;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/** Aviso no canto da tela quando uma entrada do Grimório é concluída (como as missões de modpack). */
public class GrimoireToast implements Toast {
    private static final Identifier BACKGROUND = Identifier.withDefaultNamespace("toast/advancement");
    private static final long DISPLAY_MS = 5000L;

    private final ItemStack icon;
    private final Component line;
    private final Component extra;
    private Toast.Visibility visibility = Toast.Visibility.HIDE;

    private GrimoireToast(ItemStack icon, Component line, Component extra) {
        this.icon = icon;
        this.line = line;
        this.extra = extra;
    }

    public static void show(List<GuideBook.Entry> completed, int revealed) {
        if (completed.isEmpty()) {
            return;
        }
        Component line;
        ItemStack icon;
        if (completed.size() == 1) {
            GuideBook.Entry entry = completed.get(0);
            line = Component.translatable(entry.name());
            icon = GuideBook.stack(entry.icon());
        } else {
            line = Component.translatable("guide.mistech.toast.many", completed.size());
            icon = GuideBook.stack("mistech:guide_book");
        }
        Component extra = revealed > 0 ? Component.translatable("guide.mistech.toast.unlocked", revealed) : null;
        ToastManager manager = Minecraft.getInstance().getToastManager();
        manager.addToast(new GrimoireToast(icon, line, extra));
    }

    @Override
    public Toast.Visibility getWantedVisibility() {
        return visibility;
    }

    @Override
    public void update(ToastManager manager, long fullyVisibleForMs) {
        visibility = fullyVisibleForMs >= DISPLAY_MS * manager.getNotificationDisplayTimeMultiplier()
                ? Toast.Visibility.HIDE : Toast.Visibility.SHOW;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, Font font, long fullyVisibleForMs) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND, 0, 0, width(), height());
        // depois de 2s, troca o título pela linha de "novas entradas"
        boolean showExtra = extra != null && fullyVisibleForMs > 2000L;
        Component title = showExtra ? extra : Component.translatable("guide.mistech.toast.title");
        graphics.text(font, title, 30, 7, showExtra ? 0xFFFFD86A : 0xFFD7A8FF, false);
        String text = line.getString();
        if (font.width(text) > 125) {
            text = font.plainSubstrByWidth(text, 120) + "…";
        }
        graphics.text(font, text, 30, 18, 0xFFFFFFFF, false);
        graphics.fakeItem(icon, 8, 8);
    }
}

package com.mistech.mistechmod.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * Base das telas do Mistech: textos, contorno que pulsa e pequenas luzes subindo devagar pelo painel, nas cores do tema
 * da tela ({@link #theme()}), que combina com a textura do bloco que a abre (geradas por tools/gui/restyle.py).
 */
public abstract class MysticContainerScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
    /** Cor dos títulos (dourado de pergaminho). */
    public static final int LABEL = 0xFFE3C98A;
    /** Texto secundário (lilás claro). */
    public static final int TEXT = 0xFFC9B8E8;

    protected MysticContainerScreen(T menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    protected MysticContainerScreen(T menu, Inventory inventory, Component title, int width, int height) {
        super(menu, inventory, title, width, height);
    }

    /** Tema da tela (cores combinando com a textura do bloco; ver {@link GuiThemes}). */
    protected GuiThemes.Theme theme() {
        return GuiThemes.MYSTIC;
    }

    protected int labelColor() {
        return this.theme().label();
    }

    protected int textColor() {
        return this.theme().text();
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, this.labelColor(), false);
        graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, this.textColor(), false);
    }

    /** Contorno pulsando e luzes subindo. Chamar no fim do extractBackground (depois de desenhar a textura). */
    protected void extractMysticAmbient(GuiGraphicsExtractor graphics) {
        int x = this.leftPos, y = this.topPos, w = this.imageWidth, h = this.imageHeight;
        float time = (Util.getMillis() % 1_000_000L) / 1000.0F;

        // brilho em volta do painel
        float pulse = 0.5F + 0.5F * Mth.sin(time * 1.6F);
        GuiThemes.Theme theme = this.theme();
        int outer = ((int) (0x18 + 0x30 * pulse) << 24) | (theme.glow() & 0xFFFFFF);
        int inner = ((int) (0x30 + 0x50 * pulse) << 24) | (theme.glow() & 0xFFFFFF);
        frame(graphics, x - 2, y - 2, w + 4, h + 4, outer);
        frame(graphics, x - 1, y - 1, w + 2, h + 2, inner);

        // luzes subindo
        int motes = 10 + (w * h) / 2600;
        for (int i = 0; i < motes; i++) {
            float fx = hash(i * 3 + 1), fy = hash(i * 3 + 2), fs = hash(i * 3 + 3);
            float speed = 4.0F + fs * 9.0F;
            float py = (h - 8) - ((time * speed + fy * (h - 8)) % (h - 8));
            float px = 6 + fx * (w - 12) + Mth.sin(time * 0.9F + i) * 3.0F;
            float life = Mth.sin((float) Math.PI * (py / (h - 8)));
            int alpha = (int) (Math.max(0.0F, life) * (0x70 + 0x40 * fs));
            if (alpha < 8) {
                continue;
            }
            int[] motesColors = theme.motes();
            if (motesColors.length == 0) {
                break;
            }
            int rgb = motesColors[i % motesColors.length] & 0xFFFFFF;
            int mx = x + (int) px, my = y + 4 + (int) py;
            graphics.fill(mx, my, mx + 1, my + 1, (alpha << 24) | rgb);
            if (fs > 0.75F) { // algumas maiores, com um halo
                int halo = ((alpha / 3) << 24) | rgb;
                graphics.fill(mx - 1, my, mx, my + 1, halo);
                graphics.fill(mx + 1, my, mx + 2, my + 1, halo);
                graphics.fill(mx, my - 1, mx + 1, my, halo);
                graphics.fill(mx, my + 1, mx + 1, my + 2, halo);
            }
        }
    }

    private static void frame(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + 1, color);
        graphics.fill(x, y + h - 1, x + w, y + h, color);
        graphics.fill(x, y + 1, x + 1, y + h - 1, color);
        graphics.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    private static float hash(int n) {
        float s = Mth.sin(n * 12.9898F + 78.233F) * 43758.547F;
        return s - Mth.floor(s);
    }
}

package com.mistech.mistechmod.client;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.menu.IncenseHolderMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class IncenseHolderScreen extends MysticContainerScreen<IncenseHolderMenu> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(Mistech.MODID, "textures/gui/container/incense_holder.png");

    // Barra de queima logo abaixo do slot do incenso ativo
    private static final int BAR_X = IncenseHolderMenu.ACTIVE_X;
    private static final int BAR_Y = IncenseHolderMenu.SLOT_Y + 19;
    private static final int BAR_W = 16;
    private static final int BAR_H = 3;

    public IncenseHolderScreen(IncenseHolderMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
        this.extractMysticAmbient(graphics);

        float progress = this.menu.getBurnProgress();
        if (progress > 0.0F) {
            int filled = Math.max(1, Math.round(BAR_W * progress));
            graphics.fill(x + BAR_X, y + BAR_Y, x + BAR_X + filled, y + BAR_Y + BAR_H, 0xFFE8913A);
            // "brasa" pulsando na ponta da barra
            int glow = (this.menu.getRemainingSeconds() % 2 == 0) ? 0xFFFFD27A : 0xFFFF7A2A;
            graphics.fill(x + BAR_X + filled - 1, y + BAR_Y, x + BAR_X + filled, y + BAR_Y + BAR_H, glow);
        }
    }

    @Override
    protected GuiThemes.Theme theme() {
        return GuiThemes.get("incense_holder");
    }
}

package com.mistech.mistechmod.client;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.menu.RunicFurnaceMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class RunicFurnaceScreen extends MysticContainerScreen<RunicFurnaceMenu> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(Mistech.MODID, "textures/gui/container/runic_furnace.png");
    // Na textura: seta cheia em (176,14) 24x17; runa acesa em (176,0) 14x14
    private static final int ARROW_X = 79, ARROW_Y = 34, ARROW_W = 24, ARROW_H = 17;
    private static final int RUNE_X = 57, RUNE_Y = 54, RUNE_SIZE = 14;

    public RunicFurnaceScreen(RunicFurnaceMenu menu, Inventory inventory, Component title) {
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

        if (this.menu.isCooking()) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + RUNE_X, y + RUNE_Y, 176.0F, 0.0F, RUNE_SIZE, RUNE_SIZE, 256, 256);
        }
        int filled = Math.round(ARROW_W * this.menu.getProgress());
        if (filled > 0) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + ARROW_X, y + ARROW_Y, 176.0F, 14.0F, filled, ARROW_H, 256, 256);
        }
    }

    @Override
    protected GuiThemes.Theme theme() {
        return GuiThemes.get("runic_furnace");
    }
}

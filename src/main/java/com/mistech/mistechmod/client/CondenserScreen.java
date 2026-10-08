package com.mistech.mistechmod.client;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.block.CondenserBlockEntity;
import com.mistech.mistechmod.menu.CondenserMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class CondenserScreen extends MysticContainerScreen<CondenserMenu> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(Mistech.MODID, "textures/gui/container/condenser.png");
    // Na textura: seta cheia em (176,0) 24x17; barra de energia cheia em (200,0) 14x52
    private static final int ARROW_X = 79, ARROW_Y = 34, ARROW_W = 24, ARROW_H = 17;
    private static final int BAR_X = 152, BAR_Y = 17, BAR_W = 14, BAR_H = 52;

    public CondenserScreen(CondenserMenu menu, Inventory inventory, Component title) {
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

        int arrow = Math.round(ARROW_W * this.menu.getProgress());
        if (arrow > 0) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + ARROW_X, y + ARROW_Y, 176.0F, 0.0F, arrow, ARROW_H, 256, 256);
        }
        int bar = Math.round(BAR_H * (this.menu.getEnergy() / (float) CondenserBlockEntity.CAPACITY));
        if (bar > 0) {
            int top = BAR_H - bar;
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + BAR_X, y + BAR_Y + top, 200.0F, (float) top, BAR_W, bar, 256, 256);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        graphics.text(this.font, Component.translatable("gui.mistech.energy_short", this.menu.getEnergy()), 8, 60, 0xFF8EC5FF, false);
    }

    @Override
    protected GuiThemes.Theme theme() {
        return GuiThemes.get("condenser");
    }
}

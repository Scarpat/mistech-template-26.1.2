package com.mistech.mistechmod.client;

import com.mistech.mistechmod.item.scroll.ScrollCaseItem;
import com.mistech.mistechmod.item.scroll.ScrollCaseMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** Tela do Porta-Pergaminhos: a textura de baú do jogo com uma fileira (espaços além do config ficam escuros). */
public class ScrollCaseScreen extends MysticContainerScreen<ScrollCaseMenu> {
    private static final Identifier CHEST = Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");

    public ScrollCaseScreen(ScrollCaseMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 132);
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = this.leftPos, y = this.topPos;
        graphics.blit(RenderPipelines.GUI_TEXTURED, CHEST, x, y, 0.0F, 0.0F, this.imageWidth, 35, 256, 256);
        graphics.blit(RenderPipelines.GUI_TEXTURED, CHEST, x, y + 35, 0.0F, 126.0F, this.imageWidth, 96, 256, 256);
        for (int i = ScrollCaseItem.slots(); i < ScrollCaseMenu.SLOTS; i++) {
            graphics.fill(x + 7 + i * 18, y + ScrollCaseMenu.ROW_Y - 1, x + 25 + i * 18, y + ScrollCaseMenu.ROW_Y + 17, 0xC0201018);
        }
        this.extractMysticAmbient(graphics);
    }
}

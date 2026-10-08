package com.mistech.mistechmod.client;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.menu.SpatialStorageMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** Tela de 162 espaços do Barril Espacial e da Mochila (textura 512x512, área usada 340x276). */
public class SpatialStorageScreen extends MysticContainerScreen<SpatialStorageMenu> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(Mistech.MODID, "textures/gui/container/spatial_storage.png");

    public SpatialStorageScreen(SpatialStorageMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, SpatialStorageMenu.IMAGE_WIDTH, SpatialStorageMenu.IMAGE_HEIGHT);
        this.inventoryLabelX = SpatialStorageMenu.INV_X;
        this.inventoryLabelY = SpatialStorageMenu.INV_Y - 11;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F,
                this.imageWidth, this.imageHeight, 512, 512);
        this.extractMysticAmbient(graphics);
    }

    @Override
    protected GuiThemes.Theme theme() {
        return GuiThemes.get("spatial_storage");
    }
}

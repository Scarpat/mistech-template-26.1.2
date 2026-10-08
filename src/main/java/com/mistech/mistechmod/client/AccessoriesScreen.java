package com.mistech.mistechmod.client;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.accessory.AccessoriesMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** Tela dos acessórios (tecla J): amuleto em cima e dois anéis embaixo, sobre um círculo arcano. */
public class AccessoriesScreen extends MysticContainerScreen<AccessoriesMenu> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Mistech.MODID, "textures/gui/container/accessories.png");

    public AccessoriesScreen(AccessoriesMenu menu, Inventory inventory, Component title) {
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
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
        this.extractMysticAmbient(graphics);
    }

    @Override
    protected GuiThemes.Theme theme() {
        return GuiThemes.get("accessories");
    }
}

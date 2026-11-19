package com.mistech.mistechmod.refining.client;

import com.mistech.mistechmod.machine.MachineMenu;
import com.mistech.mistechmod.purity.client.FluidMachineScreen;
import com.mistech.mistechmod.refining.AstralPrismBlockEntity;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Telas da cadeia de minério: a das máquinas com tanque + uma linha no canto. Cristalizador Rúnico: a luz (%).
 * Prisma Astral: o estado (estrutura, dia, céu, tempestade...) e a velocidade da lua.
 */
public class RefineryScreen extends FluidMachineScreen {
    public RefineryScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        String name = this.menu.layout().name;
        Component line = null;
        int color = this.textColor();
        if (name.equals("runic_crystallizer")) {
            int light = this.menu.info(0);
            line = Component.translatable(light > 0 ? "gui.mistech.runic_crystallizer.light" : "gui.mistech.runic_crystallizer.no_light", light);
            if (light <= 0) {
                color = 0xFFE06060;
            }
        } else if (name.equals("astral_prism")) {
            int status = Math.min(this.menu.info(2), AstralPrismBlockEntity.STATUS_KEYS.length - 1);
            if (status == AstralPrismBlockEntity.ST_WORKING) {
                line = Component.translatable("gui.mistech.astral_prism.speed", this.menu.info(1));
            } else {
                line = Component.translatable("gui.mistech.astral_prism.status." + AstralPrismBlockEntity.STATUS_KEYS[status]);
                if (status != AstralPrismBlockEntity.ST_IDLE) {
                    color = 0xFFE06060;
                }
            }
        }
        if (line != null) {
            String text = this.font.plainSubstrByWidth(line.getString(), 110);
            graphics.text(this.font, text, this.imageWidth - 8 - this.font.width(text), this.inventoryLabelY, color, false);
        }
    }
}

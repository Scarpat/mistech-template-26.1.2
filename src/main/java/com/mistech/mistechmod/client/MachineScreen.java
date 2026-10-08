package com.mistech.mistechmod.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.machine.MachineLayout;
import com.mistech.mistechmod.machine.MachineMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * Tela genérica das máquinas (Triturador, geradores, Colheitadeira, Plantadeira).
 * Desenha fundo, seta de progresso, chama, barra de energia (com dica ao passar o mouse),
 * as linhas de informação do layout e o selo do gnomo quando há Impulso de Gnomo.
 */
public class MachineScreen extends MysticContainerScreen<MachineMenu> {
    private static final int GNOME_X = 152, GNOME_Y = 72;

    private final Identifier texture;
    private final MachineLayout layout;

    public MachineScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.layout = menu.layout();
        this.texture = Identifier.fromNamespaceAndPath(Mistech.MODID, "textures/gui/container/" + this.layout.name + ".png");
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = this.leftPos, y = this.topPos;
        graphics.blit(RenderPipelines.GUI_TEXTURED, this.texture, x, y, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
        this.extractMysticAmbient(graphics);

        if (this.layout.arrowX >= 0) {
            int arrow = Math.round(MachineLayout.ARROW_W * this.menu.progress());
            if (arrow > 0) {
                graphics.blit(RenderPipelines.GUI_TEXTURED, this.texture, x + this.layout.arrowX, y + this.layout.arrowY,
                        176.0F, 0.0F, arrow, MachineLayout.ARROW_H, 256, 256);
            }
        }
        if (this.layout.flameX >= 0) {
            // a chama vai diminuindo conforme o combustível queima (progresso = quanto falta)
            int flame = Math.round(MachineLayout.FLAME_SIZE * this.menu.progress());
            if (flame > 0) {
                int top = MachineLayout.FLAME_SIZE - flame;
                graphics.blit(RenderPipelines.GUI_TEXTURED, this.texture, x + this.layout.flameX, y + this.layout.flameY + top,
                        176.0F, 17.0F + top, MachineLayout.FLAME_SIZE, flame, 256, 256);
            }
        }
        int bar = Math.round(MachineLayout.BAR_H * Math.min(1.0F, this.menu.energy() / (float) this.layout.capacity));
        if (bar > 0) {
            int top = MachineLayout.BAR_H - bar;
            graphics.blit(RenderPipelines.GUI_TEXTURED, this.texture, x + MachineLayout.BAR_X, y + MachineLayout.BAR_Y + top,
                    200.0F, (float) top, MachineLayout.BAR_W, bar, 256, 256);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        List<Component> lines = this.layout.infoLines.apply(this.menu);
        for (int i = 0; i < lines.size(); i++) {
            graphics.text(this.font, lines.get(i), this.layout.infoX, this.layout.infoY + i * 11, this.textColor(), false);
        }
        if (this.menu.boosted()) {
            // selo do gnomo: chapeuzinho vermelho com um brilho dourado
            int gx = GNOME_X, gy = GNOME_Y;
            graphics.fill(gx + 5, gy, gx + 7, gy + 1, 0xFFB93A3A);
            graphics.fill(gx + 4, gy + 1, gx + 8, gy + 3, 0xFFB93A3A);
            graphics.fill(gx + 3, gy + 3, gx + 9, gy + 5, 0xFFD04848);
            graphics.fill(gx + 2, gy + 5, gx + 10, gy + 6, 0xFF8A2A2A);
            graphics.fill(gx + 4, gy + 6, gx + 8, gy + 8, 0xFFF1D2B5);
            graphics.fill(gx + 4, gy + 8, gx + 8, gy + 10, 0xFFEFEFEF);
            graphics.fill(gx + 11, gy + 1, gx + 12, gy + 2, 0xFFF0C870);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (this.isHovering(MachineLayout.BAR_X, MachineLayout.BAR_Y, MachineLayout.BAR_W, MachineLayout.BAR_H, mouseX, mouseY)) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("gui.mistech.energy_tooltip", this.menu.energy(), this.layout.capacity));
            graphics.setTooltipForNextFrame(this.font, lines, Optional.empty(), mouseX, mouseY);
        } else if (this.menu.boosted() && this.isHovering(GNOME_X, GNOME_Y, 12, 10, mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(this.font, List.of(Component.translatable("gui.mistech.gnome_boost")),
                    Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    protected GuiThemes.Theme theme() {
        return GuiThemes.get(this.layout.name);
    }
}

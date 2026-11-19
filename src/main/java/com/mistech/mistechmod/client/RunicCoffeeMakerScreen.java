package com.mistech.mistechmod.client;

import java.util.List;
import java.util.Optional;

import com.mistech.mistechmod.block.MakerTanks;
import com.mistech.mistechmod.block.RunicCoffeeMakerBlockEntity;
import com.mistech.mistechmod.menu.CoffeeMakerMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/** Tela da Cafeteira Rúnica, toda desenhada por código (cobre e café). */
public class RunicCoffeeMakerScreen extends MysticContainerScreen<CoffeeMakerMenu> {
    private static final GuiThemes.Theme THEME = new GuiThemes.Theme(0xFFFFD9A0, 0xFFE6CBB0, 0xD87A4A,
            new int[] {0xF0B070, 0xFFE0B0, 0xB86A3A}, 0xFF1A110C, 0xFFD87A4A, false);
    private static final int[] LINE_COLORS = {0xFFB86A3A, 0xFF4FC060, 0xFFE0C038};
    private static final int BAR_Y = 80, BAR_H = 7, BAR_W = 44, BAR_GAP = 48, BAR_X = 12;
    private static final int PROGRESS_Y = 44, PROGRESS_W = 16, PROGRESS_H = 6;
    /** Reservatório de cada linha: barra vertical ao lado da entrada. */
    private static final int RES_DX = 20, RES_Y = 24, RES_W = 6, RES_H = 16;
    private final Button[] fillButtons = new Button[RunicCoffeeMakerBlockEntity.LINES];

    public RunicCoffeeMakerScreen(CoffeeMakerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, CoffeeMakerMenu.IMAGE_WIDTH, CoffeeMakerMenu.IMAGE_HEIGHT);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
        this.titleLabelY = 4;
        this.inventoryLabelY = CoffeeMakerMenu.INV_Y - 11;
        this.inventoryLabelX = CoffeeMakerMenu.INV_X;
        for (int line = 0; line < RunicCoffeeMakerBlockEntity.LINES; line++) {
            int l = line;
            this.fillButtons[line] = this.addRenderableWidget(Button.builder(Component.translatable("gui.mistech.coffee_maker.fill"),
                    b -> this.fill(l)).bounds(this.leftPos + CoffeeMakerMenu.COLUMN_X[line] + 19, this.topPos + CoffeeMakerMenu.OUTPUT_Y, 32, 16).build());
        }
    }

    /** Clique: enche uma Xícara com a bebida da linha; com Shift, quantas der. */
    private void fill(int line) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, line + (this.minecraft.player != null && this.minecraft.player.isShiftKeyDown() ? 100 : 0));
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        boolean cups = this.menu.getSlot(RunicCoffeeMakerBlockEntity.SLOT_CUPS).hasItem();
        for (int line = 0; line < this.fillButtons.length; line++) {
            this.fillButtons[line].active = cups && this.menu.getServings(line) > 0;
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = this.leftPos;
        int y = this.topPos;
        int w = this.imageWidth;
        int h = this.imageHeight;
        // moldura de cobre e miolo marrom-escuro
        graphics.fill(x - 3, y - 3, x + w + 3, y + h + 3, 0xFF2A1810);
        graphics.fill(x - 2, y - 2, x + w + 2, y + h + 2, 0xFF9A5538);
        graphics.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF5A3322);
        graphics.fillGradient(x, y, x + w, y + h, 0xFF2E1D14, 0xFF1C120C);
        // faixa das máquinas (linhas) e dos tanques
        graphics.fill(x + 4, y + 12, x + w - 4, y + 74, 0x40000000);
        graphics.fill(x + 4, y + 77, x + w - 4, y + 90, 0x40000000);

        for (Slot slot : this.menu.slots) {
            int sx = x + slot.x;
            int sy = y + slot.y;
            graphics.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF0C0806);
            graphics.fill(sx, sy, sx + 16, sy + 16, THEME.well());
            graphics.fill(sx, sy, sx + 16, sy + 1, 0xFF0C0806);
            graphics.fill(sx, sy, sx + 1, sy + 16, 0xFF0C0806);
        }

        // barras de progresso (uma por linha)
        for (int line = 0; line < 3; line++) {
            int bx = x + CoffeeMakerMenu.COLUMN_X[line];
            int by = y + PROGRESS_Y;
            graphics.fill(bx, by, bx + PROGRESS_W, by + PROGRESS_H, 0xFF0C0806);
            graphics.fill(bx + 1, by + 1, bx + PROGRESS_W - 1, by + PROGRESS_H - 1, 0xFF2A1F18);
            int fill = this.menu.getProgress(line) * (PROGRESS_W - 2) / 100;
            if (fill > 0) {
                graphics.fill(bx + 1, by + 1, bx + 1 + fill, by + PROGRESS_H - 1, LINE_COLORS[line]);
                graphics.fill(bx + 1, by + 1, bx + 1 + fill, by + 2, 0x55FFFFFF);
            }
            // seta: entrada -> saída
            int cx = bx + 8;
            graphics.fill(cx, y + 38, cx + 1, y + 42, 0xFF8A6A50);
            graphics.fill(cx - 2, y + 51, cx + 3, y + 52, 0xFF8A6A50);
            graphics.fill(cx - 1, y + 52, cx + 2, y + 53, 0xFF8A6A50);
            graphics.fill(cx, y + 53, cx + 1, y + 54, 0xFF8A6A50);
        }

        // reservatórios das linhas (doses prontas)
        for (int line = 0; line < 3; line++) {
            int rx = x + CoffeeMakerMenu.COLUMN_X[line] + RES_DX;
            int ry = y + RES_Y;
            graphics.fill(rx, ry, rx + RES_W, ry + RES_H, 0xFF0C0806);
            graphics.fill(rx + 1, ry + 1, rx + RES_W - 1, ry + RES_H - 1, 0xFF2A1F18);
            int height = this.menu.getServings(line) * (RES_H - 2) / RunicCoffeeMakerBlockEntity.MAX_SERVINGS;
            if (height > 0) {
                graphics.fill(rx + 1, ry + RES_H - 1 - height, rx + RES_W - 1, ry + RES_H - 1, LINE_COLORS[line]);
                graphics.fill(rx + 1, ry + RES_H - 1 - height, rx + RES_W - 1, ry + RES_H - height, 0x55FFFFFF);
            }
        }

        // tanques
        for (int tank = 0; tank < MakerTanks.TANKS; tank++) {
            int bx = x + BAR_X + tank * BAR_GAP;
            int by = y + BAR_Y;
            graphics.fill(bx, by, bx + BAR_W, by + BAR_H, 0xFF0C0806);
            graphics.fill(bx + 1, by + 1, bx + BAR_W - 1, by + BAR_H - 1, 0xFF2A1F18);
            int amount = this.menu.getTankAmount(tank);
            int fill = amount * (BAR_W - 2) / MakerTanks.CAPACITY;
            if (fill > 0) {
                int color = fluidColor(this.menu.getTankFluid(tank));
                graphics.fill(bx + 1, by + 1, bx + 1 + fill, by + BAR_H - 1, color);
                graphics.fill(bx + 1, by + 1, bx + 1 + fill, by + 2, 0x55FFFFFF);
            }
        }
        this.extractMysticAmbient(graphics);
    }

    private static int fluidColor(Fluid fluid) {
        if (fluid == Fluids.WATER || fluid == Fluids.FLOWING_WATER) {
            return 0xFF3F76E4;
        }
        if (fluid == Fluids.LAVA || fluid == Fluids.FLOWING_LAVA) {
            return 0xFFFF7A1A;
        }
        // outros líquidos: uma cor estável tirada do nome
        float hue = (net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(fluid).hashCode() & 0x7FFFFFFF) % 360 / 360.0F;
        return 0xFF000000 | java.awt.Color.HSBtoRGB(hue, 0.55F, 0.9F);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        for (int line = 0; line < 3; line++) {
            Component name = Component.translatable("gui.mistech.coffee_maker.line." + line);
            graphics.text(this.font, name, CoffeeMakerMenu.COLUMN_X[line] + 8 - this.font.width(name) / 2, 14, LINE_COLORS[line], false);
        }
        graphics.text(this.font, Component.translatable("gui.mistech.coffee_maker.cups"), 6, 29, this.textColor(), false);
        graphics.text(this.font, Component.translatable("gui.mistech.coffee_maker.fluids"), BAR_X, 70, this.textColor(), false);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        for (int line = 0; line < 3; line++) {
            int rx = this.leftPos + CoffeeMakerMenu.COLUMN_X[line] + RES_DX;
            int ry = this.topPos + RES_Y;
            if (mouseX >= rx && mouseX < rx + RES_W && mouseY >= ry && mouseY < ry + RES_H) {
                graphics.setTooltipForNextFrame(this.font, List.of(Component.translatable("gui.mistech.coffee_maker.servings",
                        Component.translatable("gui.mistech.coffee_maker.line." + line), this.menu.getServings(line),
                        RunicCoffeeMakerBlockEntity.MAX_SERVINGS)), Optional.empty(), mouseX, mouseY);
            }
        }
        for (int tank = 0; tank < MakerTanks.TANKS; tank++) {
            int bx = this.leftPos + BAR_X + tank * BAR_GAP;
            int by = this.topPos + BAR_Y;
            if (mouseX >= bx && mouseX < bx + BAR_W && mouseY >= by && mouseY < by + BAR_H) {
                Fluid fluid = this.menu.getTankFluid(tank);
                Component text = this.menu.getTankAmount(tank) <= 0
                        ? Component.translatable(tank == 0 ? "gui.mistech.coffee_maker.tank_water" : "gui.mistech.coffee_maker.tank_empty")
                        : Component.translatable("message.mistech.coffee_maker.tank", fluid.getFluidType().getDescription(),
                                String.format(java.util.Locale.ROOT, "%.1f", this.menu.getTankAmount(tank) / 1000.0));
                graphics.setTooltipForNextFrame(this.font, List.of(text), Optional.empty(), mouseX, mouseY);
            }
        }
    }

    @Override
    protected GuiThemes.Theme theme() {
        return THEME;
    }
}

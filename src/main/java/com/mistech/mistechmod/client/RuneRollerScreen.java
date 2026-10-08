package com.mistech.mistechmod.client;

import static com.mistech.mistechmod.menu.RuneRollerMenu.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.block.roller.RuneRollerBlockEntity;
import com.mistech.mistechmod.menu.RuneRollerMenu;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.TooltipFlag;

/**
 * Tela da Enroladora Rúnica. As opções (runas x charuto/baseado) ficam numa grade de 5 x 4;
 * com as runas avançadas há mais linhas, e a roda do mouse rola a grade. As que não dá para fazer ficam apagadas. Clique escolhe, clique de novo faz 1, shift faz até acabar.
 */
public class RuneRollerScreen extends MysticContainerScreen<RuneRollerMenu> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(Mistech.MODID, "textures/gui/container/rune_roller.png");
    private static final int TEX = 256;
    // extras na textura: células 18x18 em y = 200 (normal, hover, escolhida, apagada), energia em y = 220
    private static final int CELL_V = 200, POWER_V = 220;
    private static final int PROGRESS_U = 0, PROGRESS_V = 240, PROGRESS_W = 16, PROGRESS_H = 5;

    /** Linhas da grade (todas as opções) e a primeira linha visível. */
    private static final int TOTAL_ROWS = (RuneRollerBlockEntity.OPTIONS + GRID_COLS - 1) / GRID_COLS;
    private static final int MAX_SCROLL = Math.max(0, TOTAL_ROWS - GRID_ROWS);
    private int scrollRow;

    public RuneRollerScreen(RuneRollerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, IMAGE_WIDTH, IMAGE_HEIGHT);
        this.inventoryLabelX = INV_X;
        this.inventoryLabelY = INV_Y - 11;
    }

    private int optionAt(double mouseX, double mouseY) {
        double x = mouseX - (this.leftPos + GRID_X), y = mouseY - (this.topPos + GRID_Y);
        if (x < 0 || y < 0 || x >= GRID_COLS * CELL || y >= GRID_ROWS * CELL) {
            return -1;
        }
        int option = ((int) (y / CELL) + this.scrollRow) * GRID_COLS + (int) (x / CELL);
        return option < RuneRollerBlockEntity.OPTIONS ? option : -1;
    }

    private boolean inPower(double mouseX, double mouseY) {
        double x = mouseX - (this.leftPos + POWER_X), y = mouseY - (this.topPos + POWER_Y);
        return x >= 0 && y >= 0 && x < POWER_SIZE && y < POWER_SIZE;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = this.leftPos, y = this.topPos;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0.0F, 0.0F, this.imageWidth, this.imageHeight, TEX, TEX);
        this.extractMysticAmbient(graphics);

        int hovered = this.optionAt(mouseX, mouseY);
        int first = this.scrollRow * GRID_COLS;
        int last = Math.min(RuneRollerBlockEntity.OPTIONS, first + GRID_COLS * GRID_ROWS);
        for (int option = first; option < last; option++) {
            int cx = x + GRID_X + (option % GRID_COLS) * CELL;
            int cy = y + GRID_Y + (option / GRID_COLS - this.scrollRow) * CELL;
            boolean craftable = this.menu.isCraftable(option);
            int u = option == this.menu.getSelected() ? 36 : !craftable ? 54 : option == hovered ? 18 : 0;
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, cx, cy, u, CELL_V, CELL, CELL, TEX, TEX);
            graphics.item(RuneRollerBlockEntity.preview(option), cx + 1, cy + 1);
        }

        // barrinha de rolagem ao lado da grade
        if (MAX_SCROLL > 0) {
            int barX = x + GRID_X + GRID_COLS * CELL + 1;
            int barTop = y + GRID_Y;
            int barH = GRID_ROWS * CELL;
            int thumbH = Math.max(8, barH * GRID_ROWS / TOTAL_ROWS);
            int thumbY = barTop + (barH - thumbH) * this.scrollRow / MAX_SCROLL;
            graphics.fill(barX, barTop, barX + 2, barTop + barH, 0x40000000);
            graphics.fill(barX, thumbY, barX + 2, thumbY + thumbH, this.theme().accent());
        }

        boolean on = this.menu.isAutoMode();
        int powerU = (on ? 16 : 0) + (this.inPower(mouseX, mouseY) ? 32 : 0);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + POWER_X, y + POWER_Y, powerU, POWER_V, POWER_SIZE, POWER_SIZE, TEX, TEX);

        int filled = Math.round(PROGRESS_W * this.menu.getProgress());
        if (filled > 0) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + PROGRESS_X, y + PROGRESS_Y, PROGRESS_U, PROGRESS_V, filled, PROGRESS_H, TEX, TEX);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        int option = this.optionAt(mouseX, mouseY);
        if (option >= 0) {
            ItemStack preview = RuneRollerBlockEntity.preview(option);
            List<Component> lines = new ArrayList<>();
            lines.add(preview.getHoverName());
            preview.getItem().appendHoverText(preview, TooltipContext.EMPTY, net.minecraft.world.item.component.TooltipDisplay.DEFAULT,
                    lines::add, TooltipFlag.NORMAL);
            if (!this.menu.isCraftable(option)) {
                lines.add(Component.translatable("gui.mistech.roller.missing").withStyle(ChatFormatting.RED));
            }
            lines.add(Component.translatable(option == this.menu.getSelected()
                    ? "gui.mistech.confector.hint_make" : "gui.mistech.confector.hint_select").withStyle(ChatFormatting.DARK_GRAY));
            graphics.setTooltipForNextFrame(this.font, lines, Optional.empty(), mouseX, mouseY);
        } else if (this.inPower(mouseX, mouseY)) {
            List<Component> lines = List.of(
                    Component.translatable(this.menu.isAutoMode() ? "gui.mistech.confector.power_on" : "gui.mistech.confector.power_off"),
                    Component.translatable("gui.mistech.roller.power_hint").withStyle(ChatFormatting.GRAY));
            graphics.setTooltipForNextFrame(this.font, lines, Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            int option = this.optionAt(event.x(), event.y());
            if (option >= 0) {
                this.press(event.hasShiftDown() ? BUTTON_MANY + option : option);
                return true;
            }
            if (this.inPower(event.x(), event.y())) {
                this.press(BUTTON_POWER);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (MAX_SCROLL > 0 && scrollY != 0) {
            this.scrollRow = Math.max(0, Math.min(MAX_SCROLL, this.scrollRow - (int) Math.signum(scrollY)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void press(int buttonId) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, buttonId);
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    @Override
    protected GuiThemes.Theme theme() {
        return GuiThemes.get("rune_roller");
    }
}

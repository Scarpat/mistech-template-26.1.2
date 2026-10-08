package com.mistech.mistechmod.client;

import static com.mistech.mistechmod.menu.RuneConfectorMenu.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.block.confector.RuneConfectorBlockEntity;
import com.mistech.mistechmod.menu.RuneConfectorMenu;
import com.mistech.mistechmod.network.ConfectorRecipesPayload;

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
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Tela do Confector de Runas.
 * Esquerda: ingredientes e tanques. Meio: runas possíveis (clique escolhe; clique de novo faz 1; shift faz até acabar).
 * Direita: giz e saída. Canto: botão de energia (modo automático por redstone).
 */
public class RuneConfectorScreen extends MysticContainerScreen<RuneConfectorMenu> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(Mistech.MODID, "textures/gui/container/rune_confector.png");
    private static final int TEX = 512;
    // elementos extras na textura (y = 300)
    private static final int CELL_U = 0, CELL_HOVER_U = 20, CELL_SELECTED_U = 40, CELL_OFF_U = 60, CELL_V = 300;
    private static final int POWER_OFF_U = 80, POWER_ON_U = 96, POWER_HOVER = 32, POWER_V = 300;
    private static final int GAUGE_LINES_U = 144, GAUGE_LINES_V = 300;

    private int scrollRow;

    public RuneConfectorScreen(RuneConfectorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, IMAGE_WIDTH, IMAGE_HEIGHT);
        this.inventoryLabelX = INV_X;
        this.inventoryLabelY = INV_Y - 11;
    }

    /** Índices (na lista do servidor) mostrados: os que dá para fazer + o escolhido. */
    private List<Integer> shown() {
        List<Integer> list = new ArrayList<>();
        int total = ClientConfectorRecipes.get().size();
        for (int i = 0; i < total; i++) {
            if (this.menu.isCraftable(i) || i == this.menu.getSelected()) {
                list.add(i);
            }
        }
        return list;
    }

    private int maxScroll(int count) {
        int rows = (count + LIST_COLS - 1) / LIST_COLS;
        return Math.max(0, rows - LIST_ROWS);
    }

    /** Qual célula da lista está sob o mouse (-1 = nenhuma); devolve a posição na lista mostrada. */
    private int cellAt(double mouseX, double mouseY, int count) {
        double lx = mouseX - (this.leftPos + LIST_X);
        double ly = mouseY - (this.topPos + LIST_Y);
        if (lx < 0 || ly < 0 || lx >= LIST_COLS * LIST_CELL || ly >= LIST_ROWS * LIST_CELL) {
            return -1;
        }
        int pos = (this.scrollRow + (int) (ly / LIST_CELL)) * LIST_COLS + (int) (lx / LIST_CELL);
        return pos < count ? pos : -1;
    }

    private boolean inPower(double mouseX, double mouseY) {
        double x = mouseX - (this.leftPos + POWER_X), y = mouseY - (this.topPos + POWER_Y);
        return x >= 0 && y >= 0 && x < POWER_SIZE && y < POWER_SIZE;
    }

    private int tankAt(double mouseX, double mouseY) {
        double y = mouseY - (this.topPos + TANK_Y);
        if (y < 0 || y >= TANK_H) {
            return -1;
        }
        for (int t = 0; t < 2; t++) {
            double x = mouseX - (this.leftPos + (t == 0 ? TANK0_X : TANK1_X));
            if (x >= 0 && x < TANK_W) {
                return t;
            }
        }
        return -1;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = this.leftPos, y = this.topPos;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0.0F, 0.0F, this.imageWidth, this.imageHeight, TEX, TEX);
        this.extractMysticAmbient(graphics);

        // tanques
        for (int t = 0; t < 2; t++) {
            int tx = x + (t == 0 ? TANK0_X : TANK1_X);
            FluidRenderHelper.drawGauge(graphics, this.menu.getTankFluid(t), this.menu.getTankAmount(t),
                    RuneConfectorBlockEntity.TANK_CAPACITY, tx, y + TANK_Y, TANK_W, TANK_H);
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, tx, y + TANK_Y, GAUGE_LINES_U, GAUGE_LINES_V, TANK_W, TANK_H, TEX, TEX);
        }

        // botão de energia
        boolean on = this.menu.isAutoMode();
        int powerU = (on ? POWER_ON_U : POWER_OFF_U) + (this.inPower(mouseX, mouseY) ? POWER_HOVER : 0);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + POWER_X, y + POWER_Y, powerU, POWER_V, POWER_SIZE, POWER_SIZE, TEX, TEX);

        // lista de runas
        List<Integer> shown = this.shown();
        this.scrollRow = Math.min(this.scrollRow, this.maxScroll(shown.size()));
        int hovered = this.cellAt(mouseX, mouseY, shown.size());
        List<ConfectorRecipesPayload.Entry> entries = ClientConfectorRecipes.get();
        for (int pos = this.scrollRow * LIST_COLS; pos < shown.size() && pos < (this.scrollRow + LIST_ROWS) * LIST_COLS; pos++) {
            int index = shown.get(pos);
            int cx = x + LIST_X + (pos % LIST_COLS) * LIST_CELL;
            int cy = y + LIST_Y + (pos / LIST_COLS - this.scrollRow) * LIST_CELL;
            boolean craftable = this.menu.isCraftable(index);
            int u = index == this.menu.getSelected() ? CELL_SELECTED_U : !craftable ? CELL_OFF_U : pos == hovered ? CELL_HOVER_U : CELL_U;
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, cx, cy, u, CELL_V, LIST_CELL, LIST_CELL, TEX, TEX);
            if (index < entries.size()) {
                graphics.item(entries.get(index).result(), cx + 2, cy + 2);
            }
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        if (this.shown().isEmpty()) {
            List<net.minecraft.util.FormattedCharSequence> lines = this.font.split(
                    Component.translatable("gui.mistech.confector.empty"), LIST_COLS * LIST_CELL - 4);
            for (int i = 0; i < lines.size(); i++) {
                graphics.text(this.font, lines.get(i), LIST_X + 2, LIST_Y + 4 + i * 10, this.textColor(), false);
            }
        }
        // rótulo centralizado em cima do espaço do giz
        Component chalk = Component.translatable("gui.mistech.confector.chalk");
        graphics.text(this.font, chalk, CHALK_X + 8 - this.font.width(chalk) / 2, CHALK_Y - 11, this.labelColor(), false);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        List<Integer> shown = this.shown();
        int cell = this.cellAt(mouseX, mouseY, shown.size());
        List<ConfectorRecipesPayload.Entry> entries = ClientConfectorRecipes.get();
        if (cell >= 0 && shown.get(cell) < entries.size()) {
            int index = shown.get(cell);
            ConfectorRecipesPayload.Entry entry = entries.get(index);
            List<Component> lines = new ArrayList<>();
            lines.add(entry.result().getHoverName());
            for (ItemStack ingredient : entry.ingredients()) {
                lines.add(Component.literal("• ").append(ingredient.getHoverName()).withStyle(ChatFormatting.GRAY));
            }
            lines.add(Component.translatable("gui.mistech.confector.cost", entry.chalkDamage()).withStyle(ChatFormatting.DARK_PURPLE));
            if (!this.menu.isCraftable(index)) {
                lines.add(Component.translatable("gui.mistech.confector.missing").withStyle(ChatFormatting.RED));
            }
            lines.add(Component.translatable(index == this.menu.getSelected()
                    ? "gui.mistech.confector.hint_make" : "gui.mistech.confector.hint_select").withStyle(ChatFormatting.DARK_GRAY));
            graphics.setTooltipForNextFrame(this.font, lines, Optional.empty(), mouseX, mouseY);
        } else if (this.inPower(mouseX, mouseY)) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable(this.menu.isAutoMode() ? "gui.mistech.confector.power_on" : "gui.mistech.confector.power_off"));
            lines.add(Component.translatable("gui.mistech.confector.power_hint").withStyle(ChatFormatting.GRAY));
            graphics.setTooltipForNextFrame(this.font, lines, Optional.empty(), mouseX, mouseY);
        } else {
            int tank = this.tankAt(mouseX, mouseY);
            if (tank >= 0) {
                Fluid fluid = this.menu.getTankFluid(tank);
                int amount = this.menu.getTankAmount(tank);
                Component name = fluid == Fluids.EMPTY ? Component.translatable("gui.mistech.confector.tank_empty")
                        : new FluidStack(fluid, 1000).getHoverName();
                List<Component> lines = new ArrayList<>();
                lines.add(name);
                lines.add(Component.translatable("gui.mistech.confector.tank_amount",
                        String.format(java.util.Locale.ROOT, "%.1f", amount / 1000.0),
                        RuneConfectorBlockEntity.TANK_CAPACITY / 1000).withStyle(ChatFormatting.GRAY));
                lines.add(Component.translatable("gui.mistech.confector.tank_hint").withStyle(ChatFormatting.DARK_GRAY));
                graphics.setTooltipForNextFrame(this.font, lines, Optional.empty(), mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            List<Integer> shown = this.shown();
            int cell = this.cellAt(event.x(), event.y(), shown.size());
            if (cell >= 0) {
                int index = shown.get(cell);
                this.press(event.hasShiftDown() ? BUTTON_MANY + index : index);
                return true;
            }
            if (this.inPower(event.x(), event.y())) {
                this.press(BUTTON_POWER);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void press(int buttonId) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, buttonId);
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        double lx = mouseX - (this.leftPos + LIST_X), ly = mouseY - (this.topPos + LIST_Y);
        if (lx >= 0 && ly >= 0 && lx < LIST_COLS * LIST_CELL && ly < LIST_ROWS * LIST_CELL) {
            int max = this.maxScroll(this.shown().size());
            this.scrollRow = Math.max(0, Math.min(max, this.scrollRow - (int) Math.signum(scrollY)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    protected GuiThemes.Theme theme() {
        return GuiThemes.get("rune_confector");
    }
}

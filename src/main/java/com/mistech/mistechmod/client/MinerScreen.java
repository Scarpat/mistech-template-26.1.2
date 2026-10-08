package com.mistech.mistechmod.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mistech.mistechmod.machine.MachineMenu;
import com.mistech.mistechmod.machine.MinerBlockEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Tela da Mineradora: a genérica das máquinas + véu escuro no filtro quando não há Runa do Vazio
 * e uma dica com os detalhes (velocidade, gasto, runas ativas) ao passar o mouse pelas linhas de texto.
 */
public class MinerScreen extends MachineScreen {
    private static final int FILTER_X = 93, FILTER_Y = 16, FILTER_SIZE = 3 * 18;
    private static final int INFO_X = 6, INFO_Y = 38, INFO_W = 86, INFO_H = 34;
    /** Botão que abre a Câmara Coletora (o Barril Espacial embaixo). */
    private static final int CHAMBER_X = 75, CHAMBER_Y = 39, CHAMBER_SIZE = 16;
    private final net.minecraft.world.item.ItemStack chamberIcon = new net.minecraft.world.item.ItemStack(com.mistech.mistechmod.Mistech.SPATIAL_BARREL_ITEM.get());

    public MinerScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    private boolean voidActive() {
        return (this.menu.info(0) & (1 << 14)) != 0;
    }

    private boolean overChamber(double mouseX, double mouseY) {
        return this.isHovering(CHAMBER_X, CHAMBER_Y, CHAMBER_SIZE, CHAMBER_SIZE, mouseX, mouseY);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        int x = CHAMBER_X, y = CHAMBER_Y;
        boolean hover = this.overChamber(mouseX, mouseY);
        graphics.fill(x - 1, y - 1, x + CHAMBER_SIZE + 1, y + CHAMBER_SIZE + 1, hover ? this.labelColor() : (this.theme().accent() & 0x00FFFFFF) | 0x90000000);
        graphics.fill(x, y, x + CHAMBER_SIZE, y + CHAMBER_SIZE, hover ? brighten(this.theme().well()) : this.theme().well());
        graphics.item(this.chamberIcon, x, y);
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && this.overChamber(event.x(), event.y())
                && this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, MinerBlockEntity.BUTTON_OPEN_CHAMBER);
            this.minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                    net.minecraft.sounds.SoundEvents.BARREL_OPEN, 1.0F));
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    protected void extractSlots(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractSlots(graphics, mouseX, mouseY);
        if (!this.voidActive()) {
            graphics.fill(FILTER_X, FILTER_Y, FILTER_X + FILTER_SIZE, FILTER_Y + FILTER_SIZE, 0xB0120A1E);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (this.overChamber(mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(this.font, List.of(Component.translatable("gui.mistech.miner.open_chamber"),
                    Component.translatable("gui.mistech.miner.open_chamber.help").withStyle(ChatFormatting.GRAY)),
                    Optional.empty(), mouseX, mouseY);
        } else if (this.isHovering(INFO_X, INFO_Y, INFO_W, INFO_H, mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(this.font, this.details(), Optional.empty(), mouseX, mouseY);
        } else if (this.isHovering(FILTER_X, FILTER_Y, FILTER_SIZE, FILTER_SIZE, mouseX, mouseY)
                && (this.hoveredSlot == null || !this.hoveredSlot.hasItem())) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("gui.mistech.miner.filter"));
            lines.add(Component.translatable(this.voidActive() ? "gui.mistech.miner.filter.help" : "gui.mistech.miner.filter.off")
                    .withStyle(ChatFormatting.GRAY));
            graphics.setTooltipForNextFrame(this.font, lines, Optional.empty(), mouseX, mouseY);
        }
    }

    private List<Component> details() {
        int info = this.menu.info(0);
        int space = (info >> 3) & 7, time = (info >> 6) & 7, energy = (info >> 9) & 7;
        int side = space * 2 + 1;
        int t = Math.min(4, time);
        float perSecond = 20.0F * MinerBlockEntity.BLOCKS_BY_TIME[t] / MinerBlockEntity.INTERVAL_BY_TIME[t];
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("gui.mistech.miner.detail.area", side, side, side * 16, side * 16));
        lines.add(Component.translatable("gui.mistech.miner.detail.speed", formatRate(perSecond)));
        lines.add(Component.translatable("gui.mistech.miner.detail.cost", MinerBlockEntity.energyPerBlock(energy)));
        lines.add(flag((info & (1 << 12)) != 0, "gui.mistech.miner.detail.fire"));
        lines.add(flag((info & (1 << 13)) != 0, "gui.mistech.miner.detail.earth"));
        lines.add(flag((info & (1 << 14)) != 0, "gui.mistech.miner.detail.void"));
        return lines;
    }

    private static Component flag(boolean on, String key) {
        return Component.translatable(key).withStyle(on ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY);
    }

    private static String formatRate(float value) {
        return value == Math.floor(value) ? String.valueOf((int) value) : String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    private static int brighten(int argb) {
        int r = Math.min(255, ((argb >> 16) & 0xFF) + 28), g = Math.min(255, ((argb >> 8) & 0xFF) + 28), b = Math.min(255, (argb & 0xFF) + 28);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}

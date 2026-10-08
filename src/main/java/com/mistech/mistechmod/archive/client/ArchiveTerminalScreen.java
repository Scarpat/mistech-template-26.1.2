package com.mistech.mistechmod.archive.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.mistech.mistechmod.archive.ArchiveNetwork;
import com.mistech.mistechmod.archive.ArchiveTerminalMenu;
import com.mistech.mistechmod.client.MysticContainerScreen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * Tela da Mesa do Arquivo: busca (texto, {@code @mod}, {@code #tag}), ordenação (nome / quantidade / mod), roda do mouse,
 * clique pega (direito = metade), Shift = direto para o inventário, clicar com item na mão guarda (direito = 1).
 */
public class ArchiveTerminalScreen extends MysticContainerScreen<ArchiveTerminalMenu> {
    private static final int BG = 0xF0201830, PANEL = 0xFF2B2140, DARK = 0xFF15101F, FRAME = 0xFF4A3A6B, HOVER = 0x60FFFFFF;
    private static final String[] SORTS = {"A", "#", "@"};

    private EditBox search;
    private int scroll;
    private static int sort;
    private static String lastSearch = "";

    public ArchiveTerminalScreen(ArchiveTerminalMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, ArchiveTerminalMenu.WIDTH, ArchiveTerminalMenu.HEIGHT);
        this.inventoryLabelX = ArchiveTerminalMenu.INV_X;
        this.inventoryLabelY = ArchiveTerminalMenu.INV_Y - 11;
    }

    @Override
    protected void init() {
        super.init();
        this.search = new EditBox(this.font, this.leftPos + 82, this.topPos + 5, 84, 11, Component.translatable("screen.mistech.archive.search"));
        this.search.setMaxLength(50);
        this.search.setValue(lastSearch);
        this.search.setResponder(s -> {
            lastSearch = s;
            this.scroll = 0;
        });
        this.addRenderableWidget(this.search);
        this.addRenderableWidget(Button.builder(Component.literal(SORTS[sort]), b -> {
            sort = (sort + 1) % SORTS.length;
            b.setMessage(Component.literal(SORTS[sort]));
        }).bounds(this.leftPos + 170, this.topPos + 4, 16, 13).tooltip(net.minecraft.client.gui.components.Tooltip.create(
                Component.translatable("screen.mistech.archive.sort"))).build());
        this.addRenderableWidget(Button.builder(Component.literal("×"), b -> this.send(ArchiveTerminalMenu.CLEAR_GRID, ItemStack.EMPTY))
                .bounds(this.leftPos + ArchiveTerminalMenu.CRAFT_X - 18, this.topPos + ArchiveTerminalMenu.CRAFT_Y, 14, 14)
                .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("screen.mistech.archive.clear"))).build());
    }

    private void send(int action, ItemStack stack) {
        ClientPacketDistributor.sendToServer(new ArchiveNetwork.ActionPayload(this.menu.containerId, action, stack));
    }

    /** As entradas que passam na busca, já ordenadas. */
    private List<ArchiveNetwork.Entry> visible() {
        String q = this.search == null ? "" : this.search.getValue().trim().toLowerCase(Locale.ROOT);
        List<ArchiveNetwork.Entry> list = new ArrayList<>();
        for (ArchiveNetwork.Entry e : this.menu.entries) {
            if (q.isEmpty() || matches(e, q)) {
                list.add(e);
            }
        }
        Comparator<ArchiveNetwork.Entry> byName = Comparator.comparing(e -> e.resource().getHoverName().getString().toLowerCase(Locale.ROOT));
        switch (sort) {
            case 1 -> list.sort(Comparator.comparingLong(ArchiveNetwork.Entry::count).reversed().thenComparing(byName));
            case 2 -> list.sort(Comparator.<ArchiveNetwork.Entry, String>comparing(e -> BuiltInRegistries.ITEM.getKey(e.resource().getItem()).getNamespace())
                    .thenComparing(byName));
            default -> list.sort(byName);
        }
        return list;
    }

    private static boolean matches(ArchiveNetwork.Entry e, String q) {
        if (q.startsWith("@")) {
            return BuiltInRegistries.ITEM.getKey(e.resource().getItem()).getNamespace().startsWith(q.substring(1));
        }
        if (q.startsWith("#")) {
            String t = q.substring(1);
            return e.resource().getItem().builtInRegistryHolder().tags().anyMatch(tag -> tag.location().toString().contains(t));
        }
        return e.resource().getHoverName().getString().toLowerCase(Locale.ROOT).contains(q);
    }

    private int maxScroll(int count) {
        int rows = (count + ArchiveTerminalMenu.COLS - 1) / ArchiveTerminalMenu.COLS;
        return Math.max(0, rows - ArchiveTerminalMenu.ROWS);
    }

    /** Índice na lista visível sob o mouse (-1 fora da grade; >= tamanho = célula vazia). */
    private int cellAt(double mouseX, double mouseY) {
        double x = mouseX - (this.leftPos + ArchiveTerminalMenu.GRID_X);
        double y = mouseY - (this.topPos + ArchiveTerminalMenu.GRID_Y);
        if (x < 0 || y < 0 || x >= ArchiveTerminalMenu.COLS * ArchiveTerminalMenu.CELL || y >= ArchiveTerminalMenu.ROWS * ArchiveTerminalMenu.CELL) {
            return -1;
        }
        return (this.scroll + (int) (y / ArchiveTerminalMenu.CELL)) * ArchiveTerminalMenu.COLS + (int) (x / ArchiveTerminalMenu.CELL);
    }

    private static String shortCount(long n) {
        if (n < 1_000) return Long.toString(n);
        if (n < 10_000) return String.format(Locale.ROOT, "%.1fk", n / 1000.0);
        if (n < 1_000_000) return (n / 1000) + "k";
        if (n < 10_000_000) return String.format(Locale.ROOT, "%.1fM", n / 1_000_000.0);
        if (n < 1_000_000_000) return (n / 1_000_000) + "M";
        return (n / 1_000_000_000) + "G";
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = this.leftPos, y = this.topPos, w = this.imageWidth, h = this.imageHeight;
        graphics.fill(x, y, x + w, y + h, BG);
        graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, PANEL);
        // grade do Arquivo
        int gx = x + ArchiveTerminalMenu.GRID_X, gy = y + ArchiveTerminalMenu.GRID_Y;
        int gw = ArchiveTerminalMenu.COLS * ArchiveTerminalMenu.CELL, gh = ArchiveTerminalMenu.ROWS * ArchiveTerminalMenu.CELL;
        graphics.fill(gx - 1, gy - 1, gx + gw + 1, gy + gh + 1, FRAME);
        graphics.fill(gx, gy, gx + gw, gy + gh, DARK);
        for (int i = 1; i < ArchiveTerminalMenu.COLS; i++) {
            graphics.fill(gx + i * 18, gy, gx + i * 18 + 1, gy + gh, 0x40000000);
        }
        for (int i = 1; i < ArchiveTerminalMenu.ROWS; i++) {
            graphics.fill(gx, gy + i * 18, gx + gw, gy + i * 18 + 1, 0x40000000);
        }
        List<ArchiveNetwork.Entry> list = this.visible();
        this.scroll = Math.min(this.scroll, this.maxScroll(list.size()));
        int hovered = this.cellAt(mouseX, mouseY);
        int first = this.scroll * ArchiveTerminalMenu.COLS;
        for (int i = first; i < list.size() && i < first + ArchiveTerminalMenu.COLS * ArchiveTerminalMenu.ROWS; i++) {
            int cx = gx + ((i - first) % ArchiveTerminalMenu.COLS) * 18 + 1;
            int cy = gy + ((i - first) / ArchiveTerminalMenu.COLS) * 18 + 1;
            ItemStack stack = list.get(i).resource().toStack();
            graphics.item(stack, cx, cy);
            graphics.itemDecorations(this.font, stack, cx, cy, shortCount(list.get(i).count()));
            if (i == hovered) {
                graphics.fill(cx, cy, cx + 16, cy + 16, HOVER);
            }
        }
        // barra de rolagem
        int sx = gx + gw + 4;
        graphics.fill(sx, gy, sx + 6, gy + gh, DARK);
        int max = this.maxScroll(list.size());
        int thumb = max == 0 ? gh : Math.max(10, gh * ArchiveTerminalMenu.ROWS / (ArchiveTerminalMenu.ROWS + max));
        int ty = max == 0 ? gy : gy + (gh - thumb) * this.scroll / max;
        graphics.fill(sx, ty, sx + 6, ty + thumb, 0xFF8A4FD8);
        // espaços da mesa e do inventário
        for (Slot slot : this.menu.slots) {
            int px = x + slot.x, py = y + slot.y;
            graphics.fill(px - 1, py - 1, px + 17, py + 17, FRAME);
            graphics.fill(px, py, px + 16, py + 16, DARK);
        }
        if (!this.menu.powered) {
            graphics.fill(gx, gy, gx + gw, gy + gh, 0xA0000000);
        }
        this.extractMysticAmbient(graphics);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(this.font, this.title, 8, 6, this.labelColor(), false);
        graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, this.textColor(), false);
        graphics.text(this.font, "→", ArchiveTerminalMenu.RESULT_X - 20, ArchiveTerminalMenu.RESULT_Y + 4, this.textColor(), false);
        int types = this.menu.entries.size();
        long total = this.menu.entries.stream().mapToLong(ArchiveNetwork.Entry::count).sum();
        graphics.text(this.font, Component.translatable("screen.mistech.archive.count", types, shortCount(total)),
                ArchiveTerminalMenu.RESULT_X - 6, ArchiveTerminalMenu.CRAFT_Y + 44, this.textColor(), false);
        if (!this.menu.powered) {
            Component off = Component.translatable("screen.mistech.archive.offline");
            graphics.text(this.font, off, ArchiveTerminalMenu.GRID_X + (ArchiveTerminalMenu.COLS * 18 - this.font.width(off)) / 2,
                    ArchiveTerminalMenu.GRID_Y + 40, 0xFFFF6060, false);
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        if (!this.menu.getCarried().isEmpty()) {
            return;
        }
        int cell = this.cellAt(mouseX, mouseY);
        List<ArchiveNetwork.Entry> list = this.visible();
        if (cell >= 0 && cell < list.size()) {
            ArchiveNetwork.Entry e = list.get(cell);
            ItemStack stack = e.resource().toStack();
            List<Component> lines = new ArrayList<>(getTooltipFromItem(this.minecraft, stack));
            lines.add(Component.translatable("screen.mistech.archive.stored", String.format(Locale.ROOT, "%,d", e.count()))
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
            graphics.setTooltipForNextFrame(this.font, lines, Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int cell = this.cellAt(event.x(), event.y());
        if (cell >= 0 && (event.button() == 0 || event.button() == 1)) {
            List<ArchiveNetwork.Entry> list = this.visible();
            boolean right = event.button() == 1;
            if (!this.menu.getCarried().isEmpty()) {
                this.send(right ? ArchiveTerminalMenu.PUT_ONE : ArchiveTerminalMenu.PUT_ALL, ItemStack.EMPTY);
            } else if (cell < list.size()) {
                ItemStack proto = list.get(cell).resource().toStack();
                int action = hasShiftDown() ? ArchiveTerminalMenu.TAKE_SHIFT : right ? ArchiveTerminalMenu.TAKE_HALF : ArchiveTerminalMenu.TAKE;
                this.send(action, proto);
            }
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private static boolean hasShiftDown() {
        return com.mojang.blaze3d.platform.InputConstants.isKeyDown(net.minecraft.client.Minecraft.getInstance().getWindow(),
                org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT)
                || com.mojang.blaze3d.platform.InputConstants.isKeyDown(net.minecraft.client.Minecraft.getInstance().getWindow(),
                org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.cellAt(mouseX, mouseY) >= 0 || (mouseX >= this.leftPos + ArchiveTerminalMenu.GRID_X + 162
                && mouseX < this.leftPos + ArchiveTerminalMenu.GRID_X + 172 && mouseY >= this.topPos + ArchiveTerminalMenu.GRID_Y
                && mouseY < this.topPos + ArchiveTerminalMenu.GRID_Y + 90)) {
            int max = this.maxScroll(this.visible().size());
            this.scroll = Math.max(0, Math.min(max, this.scroll - (int) Math.signum(scrollY)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.search != null && this.search.isFocused()) {
            if (event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
                this.search.setFocused(false);
                return true;
            }
            return this.search.keyPressed(event) || true;
        }
        return super.keyPressed(event);
    }
}

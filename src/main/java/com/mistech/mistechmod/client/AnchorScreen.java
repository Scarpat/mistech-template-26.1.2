package com.mistech.mistechmod.client;

import java.util.ArrayList;
import java.util.List;

import com.mistech.mistechmod.network.AnchorTeleportPayload;
import com.mistech.mistechmod.network.OpenAnchorsPayload;
import com.mistech.mistechmod.network.RenameAnchorPayload;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** Lista das Âncoras Espaciais conhecidas (aberta pela âncora ou pelo Orbe de Translocação). */
public class AnchorScreen extends Screen {
    private static final int PANEL_W = 236;
    private static final int ROW_H = 26;
    private static final int VISIBLE = 6;
    private static final int GOLD = 0xFFE8C46A;
    private static final int TEXT = 0xFFE6DDF5;
    private static final int TEXT_DIM = 0xFF9C8DB8;
    private static final int GREEN = 0xFF7FE08A;
    private static final int RED = 0xFFE07070;

    private final OpenAnchorsPayload data;
    private final List<OpenAnchorsPayload.Entry> entries;
    private final List<Button> buttons = new ArrayList<>();
    private int scroll;
    /** Nome da âncora onde o jogador está (só quando a lista foi aberta por uma âncora). */
    private EditBox nameBox;
    private Button renameButton;
    private String currentName = "";
    private float time;

    public AnchorScreen(OpenAnchorsPayload data) {
        super(Component.translatable(data.source().isEmpty() ? "screen.mistech.anchors.orb" : "screen.mistech.anchors"));
        this.data = data;
        this.entries = data.entries().stream().filter(e -> !e.key().equals(data.source())).toList();
        for (OpenAnchorsPayload.Entry e : data.entries()) {
            if (e.key().equals(data.source())) {
                this.currentName = e.name();
            }
        }
    }

    private boolean atAnchor() {
        return !this.data.source().isEmpty();
    }

    /** Altura do painel (a linha de renomear só aparece numa âncora). */
    private int panelH() {
        return VISIBLE * ROW_H + (this.atAnchor() ? 56 : 44);
    }

    private int panelX() {
        return (this.width - PANEL_W) / 2;
    }

    private int panelY() {
        return (this.height - this.panelH()) / 2;
    }

    @Override
    protected void init() {
        this.buttons.clear();
        for (int i = 0; i < VISIBLE; i++) {
            int row = i;
            Button button = Button.builder(Component.translatable("screen.mistech.anchors.go"), b -> this.go(row))
                    .bounds(panelX() + PANEL_W - 50, panelY() + 26 + i * ROW_H + 4, 42, 16).build();
            this.buttons.add(this.addRenderableWidget(button));
        }
        if (this.atAnchor()) {
            int ry = panelY() + 26 + VISIBLE * ROW_H + 6;
            this.nameBox = new EditBox(this.font, panelX() + 10, ry, PANEL_W - 70, 16, Component.translatable("screen.mistech.anchors.name"));
            this.nameBox.setMaxLength(32);
            this.nameBox.setValue(this.currentName);
            this.nameBox.setHint(Component.translatable("screen.mistech.anchors.name"));
            this.addRenderableWidget(this.nameBox);
            this.renameButton = this.addRenderableWidget(Button.builder(Component.translatable("screen.mistech.anchors.rename"), b -> this.rename())
                    .bounds(panelX() + PANEL_W - 56, ry, 48, 16).build());
        }
        this.refresh();
    }

    private void rename() {
        String name = this.nameBox == null ? "" : this.nameBox.getValue().strip();
        if (!name.isEmpty() && !name.equals(this.currentName)) {
            ClientPacketDistributor.sendToServer(new RenameAnchorPayload(this.data.source(), name));
            this.currentName = name;
        }
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (this.nameBox != null && this.nameBox.isFocused()) {
            if (event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER || event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER) {
                this.rename();
                this.nameBox.setFocused(false);
                return true;
            }
            if (event.key() != org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
                return this.nameBox.keyPressed(event) || true;
            }
        }
        return super.keyPressed(event);
    }

    private void refresh() {
        if (this.renameButton != null) {
            String name = this.nameBox.getValue().strip();
            this.renameButton.active = !name.isEmpty() && !name.equals(this.currentName);
        }
        LocalPlayer player = this.minecraft.player;
        for (int i = 0; i < VISIBLE; i++) {
            int index = this.scroll + i;
            Button button = this.buttons.get(i);
            button.visible = index < this.entries.size();
            if (button.visible && player != null) {
                button.active = player.hasInfiniteMaterials() || player.experienceLevel >= this.entries.get(index).cost();
            }
        }
    }

    private void go(int row) {
        int index = this.scroll + row;
        if (index < this.entries.size()) {
            ClientPacketDistributor.sendToServer(new AnchorTeleportPayload(this.entries.get(index).key(), this.data.source()));
            this.onClose();
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int max = Math.max(0, this.entries.size() - VISIBLE);
        this.scroll = Mth.clamp(this.scroll - (int) Math.signum(scrollY), 0, max);
        this.refresh();
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        this.refresh();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        graphics.fillGradient(0, 0, this.width, this.height, 0xC0080414, 0xE0180A2A);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        this.time += a * 0.05F;
        int x = panelX();
        int y = panelY();
        int h = this.panelH();
        // moldura: pedra escura com filete dourado e cantos de esmeralda
        graphics.fill(x - 3, y - 3, x + PANEL_W + 3, y + h + 3, 0xFF140A22);
        graphics.fill(x - 2, y - 2, x + PANEL_W + 2, y + h + 2, 0xFF8A6D2E);
        graphics.fillGradient(x, y, x + PANEL_W, y + h, 0xFF22123A, 0xFF120A20);
        for (int[] c : new int[][] {{x - 3, y - 3}, {x + PANEL_W - 2, y - 3}, {x - 3, y + h - 2}, {x + PANEL_W - 2, y + h - 2}}) {
            graphics.fill(c[0], c[1], c[0] + 5, c[1] + 5, 0xFF2FBF71);
        }
        graphics.centeredText(this.font, this.atAnchor() && !this.currentName.isEmpty()
                ? Component.literal(this.currentName) : this.title, x + PANEL_W / 2, y + 8, GOLD);
        graphics.fill(x + 10, y + 20, x + PANEL_W - 10, y + 21, 0x808A6D2E);

        LocalPlayer player = this.minecraft.player;
        if (this.entries.isEmpty()) {
            graphics.centeredText(this.font, Component.translatable("screen.mistech.anchors.empty"), x + PANEL_W / 2, y + 60, TEXT_DIM);
        }
        for (int i = 0; i < VISIBLE && this.scroll + i < this.entries.size(); i++) {
            OpenAnchorsPayload.Entry entry = this.entries.get(this.scroll + i);
            int ry = y + 26 + i * ROW_H;
            int glow = (int) (40 + 30 * Math.sin(this.time + i));
            graphics.fill(x + 6, ry, x + PANEL_W - 6, ry + ROW_H - 2, (glow << 24) | 0x3A1F5C);
            // runa do espaço pulsando
            graphics.fill(x + 10, ry + 8, x + 18, ry + 16, 0xFF6B3FA8);
            graphics.fill(x + 12, ry + 10, x + 16, ry + 14, 0xFFD9B8FF);
            String name = this.font.plainSubstrByWidth(entry.name(), PANEL_W - 90);
            graphics.text(this.font, name, x + 24, ry + 3, TEXT, true);
            Component where = dimensionName(entry);
            if (player != null && player.level().dimension().identifier().equals(entry.dimension())) {
                int dist = (int) Math.sqrt(player.blockPosition().distSqr(entry.pos()));
                where = Component.translatable("screen.mistech.anchors.distance", dist);
            }
            graphics.text(this.font, where, x + 24, ry + 14, TEXT_DIM, false);
            Component cost = entry.cost() == 0 ? Component.translatable("screen.mistech.anchors.free")
                    : Component.translatable("screen.mistech.anchors.cost", entry.cost());
            boolean ok = player != null && (player.hasInfiniteMaterials() || player.experienceLevel >= entry.cost());
            graphics.text(this.font, cost, x + PANEL_W - 56 - this.font.width(cost), ry + 8, ok ? GREEN : RED, false);
        }
        if (this.entries.size() > VISIBLE) {
            int track = VISIBLE * ROW_H;
            int barH = Math.max(10, track * VISIBLE / this.entries.size());
            int barY = y + 26 + (track - barH) * this.scroll / Math.max(1, this.entries.size() - VISIBLE);
            graphics.fill(x + PANEL_W - 4, y + 26, x + PANEL_W - 2, y + 26 + track, 0x40FFFFFF);
            graphics.fill(x + PANEL_W - 4, barY, x + PANEL_W - 2, barY + barH, 0xFF8A6D2E);
        }
        if (!this.atAnchor()) {
            graphics.centeredText(this.font, Component.translatable("screen.mistech.anchors.hint"), x + PANEL_W / 2, y + h - 12, TEXT_DIM);
        } else {
            graphics.fill(x + 10, y + 26 + VISIBLE * ROW_H + 1, x + PANEL_W - 10, y + 26 + VISIBLE * ROW_H + 2, 0x808A6D2E);
        }
        super.extractRenderState(graphics, mouseX, mouseY, a);
    }

    private static Component dimensionName(OpenAnchorsPayload.Entry entry) {
        String id = entry.dimension().getNamespace() + "." + entry.dimension().getPath();
        return Component.translatableWithFallback("screen.mistech.anchors.dim." + id, entry.dimension().getPath());
    }
}

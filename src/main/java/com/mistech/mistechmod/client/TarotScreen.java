package com.mistech.mistechmod.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.joml.Matrix3x2fStack;

import com.mistech.mistechmod.network.TarotActionPayload;
import com.mistech.mistechmod.tarot.TarotArcana;
import com.mistech.mistechmod.tarot.TarotContent;
import com.mistech.mistechmod.tarot.TarotData;
import com.mistech.mistechmod.tarot.TarotField;
import com.mistech.mistechmod.tarot.TarotService;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * Tela da Mesa de Tarologia. Em cima, as 22 cartas (os arcanos); embaixo, o arcano escolhido em uma de duas abas:
 * <ul>
 * <li><b>Invocação</b>: o que pôr nos pedestais (verde = está lá), XP e Carta em Branco → Carta Invertida.</li>
 * <li><b>Forja</b>: com a Carta do Arcano, uma de duas opções de equipamento (materiais nos pedestais).</li>
 * </ul>
 * Tudo que a tela mostra é conferido de novo no servidor ({@link TarotService}).
 */
public class TarotScreen extends Screen {
    private static final int PANEL_W = 276;
    private static final int PANEL_H = 264;
    private static final int CARD_W = 20;
    private static final int CARD_H = 30;
    private static final int CARD_GAP = 4;
    private static final int COLUMNS = 11;
    private static final int GOLD = 0xFFE8C46A;
    private static final int TEXT = 0xFFE6DDF5;
    private static final int TEXT_DIM = 0xFF9C8DB8;
    private static final int GREEN = 0xFF7FE08A;
    private static final int RED = 0xFFE07070;
    private static final double MAX_DISTANCE_SQR = 8.0 * 8.0;

    private static int lastTab;
    private static int lastSelected;

    private final BlockPos tablePos;
    private int tab = lastTab;
    private int selected = lastSelected;
    private float time;
    private Button invokeButton;
    private final Button[] forgeButtons = new Button[TarotArcana.OPTION_COUNT];

    public TarotScreen(BlockPos tablePos) {
        super(Component.translatable("screen.mistech.tarot"));
        this.tablePos = tablePos;
    }

    private int panelX() {
        return (this.width - PANEL_W) / 2;
    }

    private int panelY() {
        return (this.height - PANEL_H) / 2;
    }

    private TarotArcana arcana() {
        return TarotArcana.byIndex(this.selected);
    }

    @Override
    protected void init() {
        int x = panelX();
        int y = panelY();
        this.invokeButton = this.addRenderableWidget(Button.builder(Component.translatable("screen.mistech.tarot.invoke"),
                b -> this.send(TarotService.ACTION_INVOKE, 0)).bounds(x + PANEL_W - 86, y + PANEL_H - 26, 76, 18).build());
        for (int i = 0; i < TarotArcana.OPTION_COUNT; i++) {
            int option = i;
            this.forgeButtons[i] = this.addRenderableWidget(Button.builder(Component.translatable("screen.mistech.tarot.forge"),
                    b -> this.send(TarotService.ACTION_FORGE, option)).bounds(optionX(i) + 34, y + PANEL_H - 26, 60, 18).build());
        }
        this.refresh();
    }

    private int optionX(int index) {
        return panelX() + 10 + index * 130;
    }

    private void send(int action, int option) {
        ClientPacketDistributor.sendToServer(new TarotActionPayload(this.tablePos, action, this.selected, option));
    }

    private void refresh() {
        LocalPlayer player = this.minecraft.player;
        if (player == null || this.invokeButton == null) {
            return;
        }
        TarotArcana arcana = this.arcana();
        this.invokeButton.visible = this.tab == 0;
        this.invokeButton.active = TarotService.canInvoke(player, player.level(), this.tablePos, arcana);
        for (int i = 0; i < TarotArcana.OPTION_COUNT; i++) {
            this.forgeButtons[i].visible = this.tab == 1;
            this.forgeButtons[i].active = TarotService.canForge(player, player.level(), this.tablePos, arcana, i);
        }
    }

    @Override
    public void tick() {
        LocalPlayer player = this.minecraft.player;
        if (player == null || player.distanceToSqr(this.tablePos.getCenter()) > MAX_DISTANCE_SQR
                || !player.level().getBlockState(this.tablePos).is(TarotContent.TAROT_TABLE.get())) {
            this.onClose();
            return;
        }
        this.refresh();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        lastTab = this.tab;
        lastSelected = this.selected;
        super.removed();
    }

    // ---------------------------------------------------------------- entrada

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        if (event.button() != 0) {
            return false;
        }
        double mx = event.x();
        double my = event.y();
        int x = panelX();
        int y = panelY();
        // abas
        for (int i = 0; i < 2; i++) {
            int tx = tabX(i);
            if (mx >= tx && mx < tx + 90 && my >= y + 20 && my < y + 34) {
                this.tab = i;
                this.refresh();
                this.click();
                return true;
            }
        }
        // cartas
        int card = this.cardAt(mx, my);
        if (card >= 0) {
            this.selected = card;
            this.refresh();
            this.click();
            return true;
        }
        return mx >= x && mx < x + PANEL_W && my >= y && my < y + PANEL_H;
    }

    private void click() {
        this.minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                net.minecraft.sounds.SoundEvents.BOOK_PAGE_TURN, 1.2F));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        this.selected = Math.floorMod(this.selected - (int) Math.signum(scrollY), TarotArcana.values().length);
        this.refresh();
        return true;
    }

    private int tabX(int index) {
        return panelX() + PANEL_W / 2 - 92 + index * 94;
    }

    private int cardX(int index) {
        int gridW = COLUMNS * (CARD_W + CARD_GAP) - CARD_GAP;
        return panelX() + (PANEL_W - gridW) / 2 + (index % COLUMNS) * (CARD_W + CARD_GAP);
    }

    private int cardY(int index) {
        return panelY() + 42 + (index / COLUMNS) * (CARD_H + 6);
    }

    private int cardAt(double mx, double my) {
        for (int i = 0; i < TarotArcana.values().length; i++) {
            int cx = cardX(i);
            int cy = cardY(i);
            if (mx >= cx && mx < cx + CARD_W && my >= cy && my < cy + CARD_H) {
                return i;
            }
        }
        return -1;
    }

    // ---------------------------------------------------------------- desenho

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        graphics.fillGradient(0, 0, this.width, this.height, 0xC00A0410, 0xE0200A28);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        this.time += a * 0.05F;
        LocalPlayer player = this.minecraft.player;
        if (player == null) {
            return;
        }
        int x = panelX();
        int y = panelY();
        TarotData data = player.getData(TarotContent.TAROT);

        // moldura: veludo escuro com filete dourado e cantos violeta
        graphics.fill(x - 3, y - 3, x + PANEL_W + 3, y + PANEL_H + 3, 0xFF12061A);
        graphics.fill(x - 2, y - 2, x + PANEL_W + 2, y + PANEL_H + 2, 0xFF8A6D2E);
        graphics.fillGradient(x, y, x + PANEL_W, y + PANEL_H, 0xFF2A0F33, 0xFF12061C);
        for (int[] c : new int[][] {{x - 3, y - 3}, {x + PANEL_W - 2, y - 3}, {x - 3, y + PANEL_H - 2}, {x + PANEL_W - 2, y + PANEL_H - 2}}) {
            graphics.fill(c[0], c[1], c[0] + 5, c[1] + 5, 0xFFB04CFF);
        }
        graphics.centeredText(this.font, this.title, x + PANEL_W / 2, y + 7, GOLD);

        // abas
        String[] tabs = {"screen.mistech.tarot.tab_invocation", "screen.mistech.tarot.tab_forge"};
        for (int i = 0; i < 2; i++) {
            int tx = tabX(i);
            boolean active = this.tab == i;
            boolean hover = mouseX >= tx && mouseX < tx + 90 && mouseY >= y + 20 && mouseY < y + 34;
            graphics.fill(tx, y + 20, tx + 90, y + 34, active ? 0xFF8A6D2E : hover ? 0x60B04CFF : 0x40B04CFF);
            graphics.fill(tx + 1, y + 21, tx + 89, y + 33, active ? 0xFF3A1648 : 0xFF1E0A26);
            graphics.centeredText(this.font, Component.translatable(tabs[i]), tx + 45, y + 23, active ? GOLD : TEXT_DIM);
        }

        // as 22 cartas
        int hovered = this.cardAt(mouseX, mouseY);
        for (int i = 0; i < TarotArcana.values().length; i++) {
            this.drawCard(graphics, TarotArcana.byIndex(i), cardX(i), cardY(i), i == this.selected, i == hovered,
                    data.hasDefeated(TarotArcana.byIndex(i)));
        }
        if (hovered >= 0) {
            TarotArcana h = TarotArcana.byIndex(hovered);
            graphics.setTooltipForNextFrame(this.font, h.fullName(), mouseX, mouseY);
        }

        int dy = y + 42 + 2 * (CARD_H + 6) + 2;
        graphics.fill(x + 10, dy, x + PANEL_W - 10, dy + 1, 0x808A6D2E);

        // detalhes do arcano escolhido
        TarotArcana arcana = this.arcana();
        int tx = x + 12;
        int ty = dy + 6;
        graphics.text(this.font, arcana.fullName(), tx, ty, 0xFF000000 | arcana.color(), true);
        Component status = data.hasDefeated(arcana) ? Component.translatable("screen.mistech.tarot.defeated")
                : Component.translatable("screen.mistech.tarot.not_defeated");
        graphics.text(this.font, status, x + PANEL_W - 12 - this.font.width(status), ty, data.hasDefeated(arcana) ? GREEN : TEXT_DIM, false);
        ty += 12;
        graphics.textWithWordWrap(this.font, arcana.description(), tx, ty, PANEL_W - 24, TEXT_DIM, false);
        ty += 2 * 9 + 4;

        if (this.tab == 0) {
            this.drawInvocation(graphics, player, arcana, tx, ty, mouseX, mouseY);
        } else {
            this.drawForge(graphics, player, arcana, tx, ty, mouseX, mouseY);
        }
        super.extractRenderState(graphics, mouseX, mouseY, a);
    }

    private void drawCard(GuiGraphicsExtractor graphics, TarotArcana arcana, int cx, int cy, boolean selected, boolean hover,
                          boolean defeated) {
        int lift = selected ? -2 : hover ? -1 : 0;
        cy += lift;
        int border = selected ? 0xFFFFFFFF : defeated ? 0xFFE8C46A : 0xFF5A4A6E;
        graphics.fill(cx - 1, cy - 1, cx + CARD_W + 1, cy + CARD_H + 1, border);
        int color = arcana.color();
        int dark = 0xFF000000 | (((color >> 16) & 0xFF) / 4 << 16) | (((color >> 8) & 0xFF) / 4 << 8) | ((color & 0xFF) / 4);
        graphics.fillGradient(cx, cy, cx + CARD_W, cy + CARD_H, 0xFF1A0C22, dark);
        // símbolo: losango da cor do arcano, pulsando
        float pulse = 0.75F + 0.25F * Mth.sin(this.time * 2.0F + arcana.number());
        int glow = ((int) (pulse * 255) << 24) | (color & 0xFFFFFF);
        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(cx + CARD_W / 2.0F, cy + 12.0F);
        pose.rotate(Mth.PI / 4.0F);
        pose.translate(-3.5F, -3.5F);
        pose.scale(7.0F, 7.0F);
        graphics.fill(0, 0, 1, 1, defeated ? glow : (glow & 0xFFFFFF) | 0x70000000);
        pose.popMatrix();
        // número romano pequeno embaixo
        String numeral = arcana.numeral();
        pose.pushMatrix();
        pose.translate(cx + CARD_W / 2.0F, cy + CARD_H - 7.0F);
        pose.scale(0.5F, 0.5F);
        graphics.centeredText(this.font, numeral, 0, 0, defeated ? GOLD : TEXT_DIM);
        pose.popMatrix();
    }

    /** Linha de ícones de itens (1 por pedestal); verde/vermelho embaixo de cada um. */
    private int drawRequirements(GuiGraphicsExtractor graphics, List<Supplier<Item>> items, TarotService.Match match, int x, int y,
                                 int mouseX, int mouseY) {
        for (int i = 0; i < items.size(); i++) {
            int ix = x + i * 18;
            ItemStack stack = new ItemStack(items.get(i).get());
            graphics.fill(ix - 1, y - 1, ix + 17, y + 17, 0x60000000);
            graphics.item(stack, ix, y);
            graphics.fill(ix, y + 17, ix + 16, y + 18, match.found()[i] ? GREEN : RED);
            if (mouseX >= ix && mouseX < ix + 16 && mouseY >= y && mouseY < y + 16) {
                graphics.setTooltipForNextFrame(this.font, stack, mouseX, mouseY);
            }
        }
        return items.size() * 18;
    }

    private void drawInvocation(GuiGraphicsExtractor graphics, LocalPlayer player, TarotArcana arcana, int x, int y,
                                int mouseX, int mouseY) {
        TarotService.Match match = TarotService.match(player.level(), this.tablePos, arcana.invocation());
        graphics.text(this.font, Component.translatable("screen.mistech.tarot.pedestals",
                TarotField.pedestals(player.level(), this.tablePos).size(), TarotField.PEDESTALS), x, y, TEXT, false);
        y += 11;
        this.drawRequirements(graphics, arcana.invocation(), match, x, y, mouseX, mouseY);
        y += 22;
        if (match.pedestals() < arcana.invocation().size()) {
            graphics.text(this.font, Component.translatable("screen.mistech.tarot.need_pedestals", arcana.invocation().size()), x, y, RED, false);
            y += 10;
        } else if (match.extras() > 0) {
            graphics.text(this.font, Component.translatable("screen.mistech.tarot.extras", match.extras()), x, y, RED, false);
            y += 10;
        }
        boolean creative = player.hasInfiniteMaterials();
        boolean xpOk = creative || player.experienceLevel >= arcana.xpCost();
        graphics.text(this.font, Component.translatable("screen.mistech.tarot.xp", arcana.xpCost()), x, y + 4, xpOk ? GREEN : RED, false);
        int bx = x + 90;
        ItemStack blank = new ItemStack(TarotContent.BLANK_CARD.get());
        int have = TarotService.countItem(player, TarotContent.BLANK_CARD.get());
        graphics.item(blank, bx, y);
        graphics.text(this.font, Component.literal("× 1 (" + have + ")"), bx + 18, y + 4, creative || have > 0 ? GREEN : RED, false);
        if (mouseX >= bx && mouseX < bx + 16 && mouseY >= y && mouseY < y + 16) {
            graphics.setTooltipForNextFrame(this.font, blank, mouseX, mouseY);
        }
        // a carta que sai
        int rx = panelX() + PANEL_W - 30;
        int ry = panelY() + PANEL_H - 50;
        ItemStack result = new ItemStack(TarotContent.invertedCard(arcana));
        graphics.fill(rx - 2, ry - 2, rx + 18, ry + 18, 0xFF8A6D2E);
        graphics.fill(rx - 1, ry - 1, rx + 17, ry + 17, 0xFF1E0A26);
        graphics.item(result, rx, ry);
        if (mouseX >= rx && mouseX < rx + 16 && mouseY >= ry && mouseY < ry + 16) {
            graphics.setTooltipForNextFrame(this.font, result, mouseX, mouseY);
        }
        graphics.text(this.font, Component.translatable("screen.mistech.tarot.invoke_hint"), x, panelY() + PANEL_H - 21, TEXT_DIM, false);
    }

    private void drawForge(GuiGraphicsExtractor graphics, LocalPlayer player, TarotArcana arcana, int x, int y, int mouseX, int mouseY) {
        ItemStack card = new ItemStack(TarotContent.card(arcana));
        int have = TarotService.countItem(player, TarotContent.card(arcana));
        graphics.item(card, x, y);
        graphics.text(this.font, Component.translatable("screen.mistech.tarot.card_needed", have), x + 20, y + 4,
                player.hasInfiniteMaterials() || have > 0 ? GREEN : RED, false);
        if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
            graphics.setTooltipForNextFrame(this.font, card, mouseX, mouseY);
        }
        y += 22;
        for (int i = 0; i < TarotArcana.OPTION_COUNT; i++) {
            int ox = optionX(i);
            int bw = 126;
            int bh = PANEL_H - (y - panelY()) - 30;
            graphics.fill(ox, y, ox + bw, y + bh, 0x608A6D2E);
            graphics.fill(ox + 1, y + 1, ox + bw - 1, y + bh - 1, 0xFF1E0A26);
            TarotArcana.Option option = arcana.option(i);
            graphics.text(this.font, Component.translatable("screen.mistech.tarot.option", i + 1), ox + 5, y + 4, GOLD, false);
            String name = this.font.plainSubstrByWidth(arcana.optionName(i).getString(), bw - 30);
            graphics.text(this.font, name, ox + 5, y + 15, option == null ? TEXT_DIM : TEXT, false);
            if (option == null) {
                graphics.textWithWordWrap(this.font, Component.translatable("screen.mistech.tarot.option_soon_desc"), ox + 5, y + 28,
                        bw - 10, TEXT_DIM, false);
                continue;
            }
            ItemStack result = option.result().get();
            graphics.item(result, ox + bw - 22, y + 4);
            if (mouseX >= ox + bw - 22 && mouseX < ox + bw - 6 && mouseY >= y + 4 && mouseY < y + 20) {
                graphics.setTooltipForNextFrame(this.font, result, mouseX, mouseY);
            }
            TarotService.Match match = TarotService.match(player.level(), this.tablePos, option.materials());
            List<Supplier<Item>> materials = new ArrayList<>(option.materials());
            int perRow = (bw - 10) / 18;
            for (int row = 0; row * perRow < materials.size(); row++) {
                List<Supplier<Item>> slice = materials.subList(row * perRow, Math.min(materials.size(), (row + 1) * perRow));
                boolean[] found = new boolean[slice.size()];
                System.arraycopy(match.found(), row * perRow, found, 0, slice.size());
                this.drawRequirements(graphics, slice, new TarotService.Match(found, 0, 0), ox + 5, y + 26 + row * 19, mouseX, mouseY);
            }
            if (match.extras() > 0) {
                graphics.text(this.font, Component.translatable("screen.mistech.tarot.extras_short", match.extras()), ox + 5, y + bh - 11,
                        RED, false);
            }
        }
    }
}

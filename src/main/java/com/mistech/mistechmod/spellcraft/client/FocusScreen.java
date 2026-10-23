package com.mistech.mistechmod.spellcraft.client;

import java.util.ArrayList;
import java.util.List;

import com.mistech.mistechmod.affinity.Affinity;
import com.mistech.mistechmod.item.RuneElement;
import com.mistech.mistechmod.spellcraft.FocusSlotPayload;
import com.mistech.mistechmod.spellcraft.ManaService;
import com.mistech.mistechmod.spellcraft.Spell;
import com.mistech.mistechmod.spellcraft.SpellFocusItem;
import com.mistech.mistechmod.spellcraft.Spells;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * Tela dos espaços de um Tomo/Varinha, no visual do inventário: à esquerda a grade das magias que você conhece (e o
 * item aguenta) com filtro por elemento; à direita os espaços do item. Clique numa magia e depois num espaço para pôr;
 * botão direito num espaço limpa. Passe o mouse numa magia para ver os detalhes.
 */
public class FocusScreen extends Screen {
    private static final int PAD = 8;
    private static final int TILE = 18;
    private static final int COLS = 7;
    private static final int ROWS = 5;
    private static final int GRID_W = COLS * TILE;
    private static final int RIGHT_W = 118;
    private static final int PANEL_W = PAD + GRID_W + 10 + RIGHT_W + PAD;
    private static final int PANEL_H = 178;
    private static final int ROW_H = 21;
    private static final int GRID_Y = 34;
    private static final int FILTER_W = 13;

    private final int hand;
    private int scroll;
    private String picked = "";
    private RuneElement filter;

    public FocusScreen(int hand) {
        super(Component.translatable("screen.mistech.focus"));
        this.hand = hand;
    }

    private ItemStack stack() {
        LocalPlayer player = this.minecraft.player;
        if (player == null) {
            return ItemStack.EMPTY;
        }
        return this.hand == 0 ? player.getMainHandItem() : player.getOffhandItem();
    }

    /** Painel lateral com os níveis de afinidade dos 10 elementos básicos. */
    private static final int AFF_W = 82;
    private static final int AFF_GAP = 4;
    private static final RuneElement[] BASICS = {RuneElement.FIRE, RuneElement.WATER, RuneElement.EARTH, RuneElement.AIR, RuneElement.ICE,
            RuneElement.ENERGY, RuneElement.LIGHT, RuneElement.DARKNESS, RuneElement.SPACE, RuneElement.TIME};

    private int px() {
        return (this.width - PANEL_W - AFF_GAP - AFF_W) / 2;
    }

    private int py() {
        return (this.height - PANEL_H) / 2;
    }

    private List<Spell> eligible() {
        LocalPlayer player = this.minecraft.player;
        ItemStack stack = this.stack();
        List<Spell> list = new ArrayList<>();
        if (player != null && SpellFocusItem.is(stack)) {
            for (Spell spell : Spells.learnedBy(player)) {
                if (SpellFocusItem.canHold(player, stack, spell)) {
                    list.add(spell);
                }
            }
        }
        return list;
    }

    private List<Spell> shown() {
        List<Spell> all = this.eligible();
        return this.filter == null ? all : all.stream().filter(sp -> sp.element() == this.filter).toList();
    }

    private int slotCount() {
        return SpellFocusItem.slots(this.stack()).size();
    }

    @Override
    public void tick() {
        if (!SpellFocusItem.is(this.stack())) {
            this.onClose();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ---------------------------------------------------------------- geometria

    private int filterX() {
        return px() + PAD;
    }

    private int filterY() {
        return py() + 20;
    }

    private int gridX() {
        return px() + PAD;
    }

    private int gridY() {
        return py() + GRID_Y;
    }

    private int rightX() {
        return px() + PAD + GRID_W + 10;
    }

    private int tileAt(double mx, double my) {
        if (mx < gridX() || mx >= gridX() + GRID_W || my < gridY() || my >= gridY() + ROWS * TILE) {
            return -1;
        }
        int col = (int) ((mx - gridX()) / TILE);
        int row = (int) ((my - gridY()) / TILE);
        int index = (this.scroll + row) * COLS + col;
        return index < this.shown().size() ? index : -1;
    }

    private int slotAt(double mx, double my) {
        int n = this.slotCount();
        for (int i = 0; i < n; i++) {
            int sy = gridY() + i * ROW_H;
            if (mx >= rightX() && mx < rightX() + RIGHT_W && my >= sy && my < sy + ROW_H - 1) {
                return i;
            }
        }
        return -1;
    }

    private void click() {
        this.minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                net.minecraft.sounds.SoundEvents.BOOK_PAGE_TURN, 1.2F));
    }

    // ---------------------------------------------------------------- entrada

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        if (event.button() != 0 && event.button() != 1) {
            return false;
        }
        RuneElement clicked = ElementIcon.filterAt(ElementIcon.present(this.eligible()), filterX(), filterY(), FILTER_W, event.x(), event.y());
        if (clicked != null) {
            this.filter = clicked == this.filter ? null : clicked;
            this.scroll = 0;
            this.click();
            return true;
        }
        int tile = this.tileAt(event.x(), event.y());
        if (tile >= 0) {
            String id = this.shown().get(tile).id();
            this.picked = id.equals(this.picked) ? "" : id;
            this.click();
            return true;
        }
        int slot = this.slotAt(event.x(), event.y());
        if (slot >= 0) {
            boolean clear = this.picked.isEmpty() || event.button() == 1;
            ClientPacketDistributor.sendToServer(new FocusSlotPayload(this.hand, slot, clear ? "" : this.picked));
            if (!clear) {
                this.picked = "";
            }
            this.click();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int totalRows = (this.shown().size() + COLS - 1) / COLS;
        int max = Math.max(0, totalRows - ROWS);
        this.scroll = Math.max(0, Math.min(max, this.scroll - (int) Math.signum(scrollY)));
        return true;
    }

    // ---------------------------------------------------------------- desenho

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        graphics.fillGradient(0, 0, this.width, this.height, 0xC0101010, 0xD0101010);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        LocalPlayer player = this.minecraft.player;
        ItemStack stack = this.stack();
        if (player == null || !(stack.getItem() instanceof SpellFocusItem item)) {
            return;
        }
        int x = px();
        int y = py();
        MysticUi.panel(g, x, y, PANEL_W, PANEL_H);

        // título (vanilla, cinza escuro) + glifos de encantamento
        g.text(this.font, stack.getHoverName(), x + PAD, y + 8, MysticUi.TEXT, false);
        Component glyphs = MysticUi.glyphs("arcana");
        g.text(this.font, glyphs, x + PANEL_W - PAD - this.font.width(glyphs), y + 8, MysticUi.ARCANE, false);

        // filtro por elemento
        List<Spell> everything = this.eligible();
        List<RuneElement> elements = ElementIcon.present(everything);
        if (this.filter != null && !elements.contains(this.filter)) {
            this.filter = null;
        }
        ElementIcon.drawFilter(g, elements, this.filter, filterX(), filterY(), FILTER_W, mouseX, mouseY);

        // grade de magias
        List<Spell> list = this.shown();
        int totalRows = (list.size() + COLS - 1) / COLS;
        this.scroll = Math.max(0, Math.min(this.scroll, Math.max(0, totalRows - ROWS)));
        int hovered = this.tileAt(mouseX, mouseY);
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                int tx = gridX() + c * TILE;
                int ty = gridY() + r * TILE;
                int index = (this.scroll + r) * COLS + c;
                if (index >= list.size()) {
                    MysticUi.inset(g, tx, ty, TILE, TILE, MysticUi.SLOT);
                    continue;
                }
                Spell spell = list.get(index);
                boolean sel = spell.id().equals(this.picked);
                MysticUi.spellTile(g, spell, tx, ty, TILE, !spell.hasAffinity(player), false, index);
                if (sel) {
                    MysticUi.glow(g, tx, ty, TILE, spell.element().color());
                    MysticUi.selection(g, tx, ty, TILE);
                } else if (index == hovered) {
                    g.fill(tx + 1, ty + 1, tx + TILE - 1, ty + TILE - 1, 0x50FFFFFF);
                }
            }
        }
        if (totalRows > ROWS) {
            int barH = ROWS * TILE;
            int thumb = Math.max(10, barH * ROWS / totalRows);
            int top = gridY() + (barH - thumb) * this.scroll / (totalRows - ROWS);
            g.fill(gridX() + GRID_W + 2, gridY(), gridX() + GRID_W + 4, gridY() + barH, MysticUi.SLOT_DARK);
            g.fill(gridX() + GRID_W + 2, top, gridX() + GRID_W + 4, top + thumb, MysticUi.ARCANE);
        }
        if (list.isEmpty()) {
            g.textWithWordWrap(this.font, Component.translatable(everything.isEmpty() ? "screen.mistech.focus.none" : "screen.mistech.focus.none_filter"),
                    gridX() + 4, gridY() + 4, GRID_W - 8, MysticUi.TEXT, false);
        }

        // espaços do item
        g.text(this.font, Component.translatable("screen.mistech.focus.slots", item.focus().maxTier()), rightX(), y + 22, MysticUi.TEXT, false);
        List<String> slots = SpellFocusItem.slots(stack);
        int selectedSlot = SpellFocusItem.selectedIndex(stack);
        int hoveredSlot = this.slotAt(mouseX, mouseY);
        Spell picked = Spells.byId(this.picked);
        for (int i = 0; i < slots.size(); i++) {
            int sy = gridY() + i * ROW_H;
            boolean hot = i == hoveredSlot;
            MysticUi.inset(g, rightX(), sy, RIGHT_W, ROW_H - 1, hot ? 0xFF9C9C9C : MysticUi.SLOT);
            Spell spell = Spells.byId(slots.get(i));
            int tx = rightX() + 1;
            int ty = sy + 1;
            if (spell == null) {
                MysticUi.inset(g, tx, ty, TILE, TILE, 0xFF6E6E6E);
                g.text(this.font, Component.translatable("screen.mistech.focus.empty"), tx + TILE + 5, ty + 5, 0xFF5A5A5A, false);
                if (hot && picked != null) {
                    ElementIcon.draw(g, picked.element(), tx + 4, ty + 4, 1);
                }
            } else {
                MysticUi.spellTile(g, spell, tx, ty, TILE, false, false, i + 40);
                g.text(this.font, Component.translatable(spell.nameKey()), tx + TILE + 4, ty, 0xFFFFFFFF, true);
                int cost = ManaService.cost(player, (int) Math.ceil(spell.mana() * SpellFocusItem.manaMult(stack, spell.element())));
                g.text(this.font, Component.translatable(spell.channelTicks() > 0 ? "screen.mistech.focus.cost_channel" : "screen.mistech.focus.cost", cost),
                        tx + TILE + 4, ty + 9, 0xFF9AE0FF, true);
            }
            if (i == selectedSlot) {
                g.outline(rightX() - 1, sy - 1, RIGHT_W + 2, ROW_H + 1, 0xFF000000);
                g.outline(rightX(), sy, RIGHT_W, ROW_H - 1, MysticUi.GOLD);
            }
        }

        // afinidades (painel lateral)
        int ax = x + PANEL_W + AFF_GAP;
        MysticUi.panel(g, ax, y, AFF_W, PANEL_H);
        g.text(this.font, Component.translatable("screen.mistech.focus.affinity"), ax + 6, y + 8, MysticUi.TEXT, false);
        com.mistech.mistechmod.affinity.AffinityData data = Affinity.get(player);
        RuneElement hoverElement = null;
        int hoverPoints = 0;
        for (int i = 0; i < BASICS.length; i++) {
            RuneElement e = BASICS[i];
            int ry = y + 22 + i * 15;
            int points = data.get(e);
            int lvl = Affinity.level(points);
            MysticUi.inset(g, ax + 5, ry, 13, 13, MysticUi.SLOT_DARK);
            ElementIcon.draw(g, e, ax + 7, ry + 2, 1);
            String num = String.valueOf(lvl);
            g.text(this.font, Component.literal(num), ax + 22, ry + 3, lvl >= Affinity.MAX_LEVEL ? MysticUi.GOLD : 0xFFFFFFFF, true);
            int bx = ax + 32, bw = AFF_W - 38;
            g.fill(bx, ry + 4, bx + bw, ry + 9, 0xFF1A1A1A);
            float prog = lvl >= Affinity.MAX_LEVEL ? 1.0F : Affinity.progress(points);
            g.fill(bx, ry + 4, bx + Math.round(bw * prog), ry + 9, 0xFF000000 | e.color());
            if (mouseX >= ax + 4 && mouseX < ax + AFF_W - 4 && mouseY >= ry && mouseY < ry + 14) {
                hoverElement = e;
                hoverPoints = points;
            }
        }

        // faixa de ajuda (casa escura embutida)
        int hy = y + PANEL_H - 36;
        MysticUi.inset(g, x + PAD, hy, PANEL_W - PAD * 2, 26, MysticUi.SLOT_DARK);
        Component hint = Component.translatable(this.picked.isEmpty() ? "screen.mistech.focus.hint" : "screen.mistech.focus.hint_picked");
        g.textWithWordWrap(this.font, hint, x + PAD + 5, hy + 4, PANEL_W - PAD * 2 - 10, 0xFFD8D8D8, false);

        super.extractRenderState(g, mouseX, mouseY, a);
        if (hoverElement != null) {
            int lvl = Affinity.level(hoverPoints);
            List<Component> tip = new ArrayList<>();
            tip.add(Component.translatable(hoverElement.translationKey()).withColor(hoverElement.color()));
            tip.add(Component.translatable("screen.mistech.focus.affinity.level", lvl, Affinity.MAX_LEVEL));
            tip.add(lvl >= Affinity.MAX_LEVEL
                    ? Component.translatable("screen.mistech.focus.affinity.mastery").withColor(0xFFD0A0FF)
                    : Component.translatable("screen.mistech.focus.affinity.next", hoverPoints, Affinity.THRESHOLDS[lvl + 1]).withColor(0xFFAAAAAA));
            tip.add(Component.translatable("screen.mistech.focus.affinity.power", lvl * 10).withColor(0xFFFFD866));
            g.setComponentTooltipForNextFrame(this.font, tip, mouseX, mouseY);
        }

        // dica do Minecraft ao passar o mouse numa magia
        if (hovered >= 0) {
            Spell spell = list.get(hovered);
            g.setComponentTooltipForNextFrame(this.font, MysticUi.spellTooltip(spell, spell.hasAffinity(player),
                    ManaService.cost(player, (int) Math.ceil(spell.mana() * SpellFocusItem.manaMult(stack, spell.element()))),
                    Affinity.effectiveLevel(player, spell.element())), mouseX, mouseY);
        } else if (hoveredSlot >= 0 && Spells.byId(slots.get(hoveredSlot)) != null) {
            Spell spell = Spells.byId(slots.get(hoveredSlot));
            g.setComponentTooltipForNextFrame(this.font, MysticUi.spellTooltip(spell, spell.hasAffinity(player),
                    ManaService.cost(player, (int) Math.ceil(spell.mana() * SpellFocusItem.manaMult(stack, spell.element()))),
                    Affinity.effectiveLevel(player, spell.element())), mouseX, mouseY);
        }
    }
}

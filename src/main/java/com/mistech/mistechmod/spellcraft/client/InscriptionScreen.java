package com.mistech.mistechmod.spellcraft.client;

import java.util.List;

import com.mistech.mistechmod.affinity.Affinity;
import com.mistech.mistechmod.astrology.AstrologyService;
import com.mistech.mistechmod.item.RuneElement;
import com.mistech.mistechmod.spellcraft.InscribePayload;
import com.mistech.mistechmod.spellcraft.InscriptionService;
import com.mistech.mistechmod.spellcraft.ManaService;
import com.mistech.mistechmod.spellcraft.Spell;
import com.mistech.mistechmod.spellcraft.SpellcraftContent;
import com.mistech.mistechmod.spellcraft.Spells;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * Tela da Mesa de Inscrição: à esquerda as magias que o jogador conhece; à direita os detalhes, o que a magia pede
 * (verde = tem, vermelho = falta) e o botão Inscrever. O servidor confere tudo de novo ({@link InscriptionService}).
 */
public class InscriptionScreen extends Screen {
    private static final int PANEL_W = 330;
    private static final int PANEL_H = 188;
    private static final int LIST_W = 130;
    private static final int ROW_H = 18;
    private static final int ROWS = 7;
    private static final int GOLD = 0xFF7A5200;
    private static final int TEXT = 0xFF404040;
    private static final int TEXT_DIM = 0xFF666666;
    private static final int GREEN = 0xFF1E7A2E;
    private static final int RED = 0xFFA02020;

    private static int lastSelected;
    private static boolean lastCrystal;

    private final BlockPos tablePos;
    private int selected = lastSelected;
    private int scroll;
    private RuneElement filter;
    private Button inscribeButton;
    private Button modeButton;
    /** Modo Cristal: grava um Cristal de Feitiço para o Conjurador Rúnico em vez de inscrever um pergaminho. */
    private boolean crystal = lastCrystal;

    public InscriptionScreen(BlockPos tablePos) {
        super(Component.translatable("screen.mistech.inscription"));
        this.tablePos = tablePos;
    }

    private int panelX() {
        return (this.width - PANEL_W) / 2;
    }

    private int panelY() {
        return (this.height - PANEL_H) / 2;
    }

    private List<Spell> allKnown() {
        LocalPlayer player = this.minecraft.player;
        return player == null ? List.of() : Spells.learnedBy(player);
    }

    private List<Spell> known() {
        List<Spell> all = this.allKnown();
        return this.filter == null ? all : all.stream().filter(sp -> sp.element() == this.filter).toList();
    }

    private Spell current() {
        List<Spell> known = this.known();
        if (known.isEmpty()) {
            return null;
        }
        this.selected = Math.max(0, Math.min(this.selected, known.size() - 1));
        return known.get(this.selected);
    }

    @Override
    protected void init() {
        int x = panelX();
        int y = panelY();
        this.inscribeButton = this.addRenderableWidget(Button.builder(Component.translatable("screen.mistech.inscription.inscribe"), b -> {
            Spell spell = this.current();
            if (spell != null) {
                ClientPacketDistributor.sendToServer(new InscribePayload(this.tablePos, spell.id(), this.crystal));
            }
        }).bounds(x + PANEL_W - 96, y + PANEL_H - 26, 86, 18).build());
        this.modeButton = this.addRenderableWidget(Button.builder(this.modeLabel(), b -> {
            this.crystal = !this.crystal;
            b.setMessage(this.modeLabel());
            this.refresh();
        }).bounds(listX(), y + PANEL_H - 24, LIST_W, 16).build());
        this.refresh();
    }

    private Component modeLabel() {
        return Component.translatable(this.crystal ? "screen.mistech.inscription.mode.crystal" : "screen.mistech.inscription.mode.scroll");
    }

    private void refresh() {
        LocalPlayer player = this.minecraft.player;
        Spell spell = this.current();
        if (this.inscribeButton != null) {
            this.inscribeButton.visible = spell != null;
            this.inscribeButton.setMessage(Component.translatable(this.crystal ? "screen.mistech.inscription.engrave" : "screen.mistech.inscription.inscribe"));
            this.inscribeButton.active = player != null && spell != null && (this.crystal
                    ? InscriptionService.canEngrave(player, spell) && InscriptionService.canAffordEngrave(player, spell)
                    : InscriptionService.canInscribe(player, spell) && InscriptionService.canAfford(player, spell));
        }
        if (this.modeButton != null) {
            this.modeButton.visible = !this.allKnown().isEmpty();
        }
    }

    @Override
    public void tick() {
        LocalPlayer player = this.minecraft.player;
        if (player == null || player.distanceToSqr(this.tablePos.getCenter()) > InscriptionService.MAX_DISTANCE_SQR
                || !player.level().getBlockState(this.tablePos).is(SpellcraftContent.INSCRIPTION_TABLE.get())) {
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
        lastSelected = this.selected;
        lastCrystal = this.crystal;
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
        RuneElement clicked = ElementIcon.filterAt(ElementIcon.present(this.allKnown()), listX(), filterY(), 13, event.x(), event.y());
        if (clicked != null) {
            this.filter = clicked == this.filter ? null : clicked;
            this.selected = 0;
            this.scroll = 0;
            this.refresh();
            return true;
        }
        int row = this.rowAt(event.x(), event.y());
        if (row >= 0) {
            this.selected = row;
            this.refresh();
            this.minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                    net.minecraft.sounds.SoundEvents.BOOK_PAGE_TURN, 1.2F));
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int max = Math.max(0, this.known().size() - ROWS);
        this.scroll = Math.max(0, Math.min(max, this.scroll - (int) Math.signum(scrollY)));
        return true;
    }

    private int listX() {
        return panelX() + 10;
    }

    private int filterY() {
        return panelY() + 19;
    }

    private int listY() {
        return panelY() + 36;
    }

    private int rowAt(double mx, double my) {
        if (mx < listX() || mx >= listX() + LIST_W || my < listY() || my >= listY() + ROWS * ROW_H) {
            return -1;
        }
        int row = this.scroll + (int) ((my - listY()) / ROW_H);
        return row < this.known().size() ? row : -1;
    }

    // ---------------------------------------------------------------- desenho

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        graphics.fillGradient(0, 0, this.width, this.height, 0xC0051014, 0xE00A1E22);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        LocalPlayer player = this.minecraft.player;
        if (player == null) {
            return;
        }
        int x = panelX();
        int y = panelY();
        MysticUi.panel(graphics, x, y, PANEL_W, PANEL_H);
        graphics.text(this.font, this.title, x + 8, y + 8, MysticUi.TEXT, false);
        Component glyphs = MysticUi.glyphs("inscribe");
        graphics.text(this.font, glyphs, x + PANEL_W - 8 - this.font.width(glyphs), y + 8, MysticUi.ARCANE, false);

        List<Spell> known = this.known();
        if (this.allKnown().isEmpty()) {
            graphics.textWithWordWrap(this.font, Component.translatable("screen.mistech.inscription.empty"),
                    x + 20, y + 60, PANEL_W - 40, TEXT_DIM, false);
            super.extractRenderState(graphics, mouseX, mouseY, a);
            return;
        }

        ElementIcon.drawFilter(graphics, ElementIcon.present(this.allKnown()), this.filter, listX(), filterY(), 13, mouseX, mouseY);
        if (known.isEmpty()) {
            graphics.textWithWordWrap(this.font, Component.translatable("screen.mistech.focus.none_filter"), listX() + 4, listY() + 4, LIST_W - 8, TEXT_DIM, false);
            super.extractRenderState(graphics, mouseX, mouseY, a);
            return;
        }

        // lista
        int lx = listX();
        int ly = listY();
        MysticUi.inset(graphics, lx - 1, ly - 1, LIST_W + 2, ROWS * ROW_H + 2, MysticUi.SLOT);
        int hovered = this.rowAt(mouseX, mouseY);
        for (int i = 0; i < ROWS; i++) {
            int index = this.scroll + i;
            if (index >= known.size()) {
                break;
            }
            Spell spell = known.get(index);
            int ry = ly + i * ROW_H;
            boolean sel = index == this.selected;
            if (sel) {
                graphics.fill(lx, ry, lx + LIST_W, ry + ROW_H, 0xA0000000 | (MysticUi.ARCANE & 0xFFFFFF));
            } else if (index == hovered) {
                graphics.fill(lx, ry, lx + LIST_W, ry + ROW_H, 0x50FFFFFF);
            }
            MysticUi.spellTile(graphics, spell, lx, ry, ROW_H, !spell.hasAffinity(player), false, index);
            Component name = Component.translatable(spell.nameKey());
            boolean usable = !this.crystal || com.mistech.mistechmod.spellcraft.conjurer.BlockCasts.compatible(spell.id());
            graphics.text(this.font, this.font.plainSubstrByWidth(name.getString(), LIST_W - ROW_H - 6), lx + ROW_H + 3, ry + 5, usable ? 0xFFFFFFFF : 0xFF8A8A8A, true);
        }
        if (known.size() > ROWS) {
            int barH = ROWS * ROW_H;
            int thumb = Math.max(8, barH * ROWS / known.size());
            int top = ly + (barH - thumb) * this.scroll / (known.size() - ROWS);
            graphics.fill(lx + LIST_W + 3, ly, lx + LIST_W + 5, ly + barH, MysticUi.SLOT_DARK);
            graphics.fill(lx + LIST_W + 3, top, lx + LIST_W + 5, top + thumb, MysticUi.ARCANE);
        }

        // detalhes
        Spell spell = this.current();
        if (spell == null) {
            super.extractRenderState(graphics, mouseX, mouseY, a);
            return;
        }
        int dx = lx + LIST_W + 14;
        int dw = PANEL_W - LIST_W - 38;
        int dy = ly;
        ElementIcon.draw(graphics, spell.element(), dx, dy, 1);
        graphics.text(this.font, Component.translatable(spell.nameKey()), dx + 13, dy, MysticUi.TEXT, false);
        dy += 11;
        graphics.text(this.font, Component.translatable(spell.element().translationKey()).append(" · ")
                .append(Component.translatable("tooltip.mistech.spell.tier", spell.tier())), dx, dy, TEXT_DIM, false);
        dy += 12;
        graphics.textWithWordWrap(this.font, Component.translatable(spell.descKey()), dx, dy, dw, TEXT, false);
        dy += 4 * 9 + 4;

        boolean affinity = spell.hasAffinity(player);
        Component req = Component.translatable("screen.mistech.inscription.requires", spell.requiredLevel(),
                spell.requirementName(),
                Affinity.effectiveLevel(player, spell.element()));
        if (this.font.width(req) <= dw) {
            graphics.text(this.font, req, dx, dy, affinity ? GREEN : RED, false);
        } else {
            graphics.text(this.font, this.font.plainSubstrByWidth(req.getString(), dw - this.font.width("...")) + "...", dx, dy, affinity ? GREEN : RED, false);
            if (mouseX >= dx && mouseX < dx + dw && mouseY >= dy - 1 && mouseY < dy + 9) {
                graphics.setTooltipForNextFrame(this.font, req, mouseX, mouseY);
            }
        }
        dy += 10;
        graphics.text(this.font, Component.translatable(spell.channelTicks() > 0 ? "screen.mistech.inscription.mana_channel" : "screen.mistech.inscription.mana", ManaService.cost(player, spell.mana()),
                (int) ManaService.max(player)), dx, dy, 0xFF1B5E8A, false);
        dy += 14;

        if (this.crystal) {
            this.drawCrystalCost(graphics, player, spell, dx, dy, dw, mouseX, mouseY);
        } else if (spell.worldOnly()) {
            graphics.textWithWordWrap(this.font, Component.translatable("screen.mistech.inscription.world_only"), dx, dy, dw, 0xFF8A5A00, false);
        } else {
            boolean xp = player.hasInfiniteMaterials() || player.experienceLevel >= spell.xpLevels();
            int ix = dx;
            for (Spell.Cost cost : spell.costs()) {
                ItemStack stack = new ItemStack(cost.item().get());
                boolean has = player.hasInfiniteMaterials() || AstrologyService.countItem(player, cost.item().get()) >= cost.count();
                MysticUi.inset(graphics, ix - 1, dy - 1, 18, 18, MysticUi.SLOT);
                graphics.item(stack, ix, dy);
                graphics.text(this.font, String.valueOf(cost.count()), ix + 17 - this.font.width(String.valueOf(cost.count())), dy + 9, has ? GREEN : RED, true);
                graphics.fill(ix, dy + 18, ix + 16, dy + 19, has ? GREEN : RED);
                if (mouseX >= ix && mouseX < ix + 16 && mouseY >= dy && mouseY < dy + 16) {
                    graphics.setTooltipForNextFrame(this.font, stack, mouseX, mouseY);
                }
                ix += 21;
                if (ix + 18 > dx + dw && ix > dx) {
                    ix = dx;
                    dy += 22;
                }
            }
            graphics.text(this.font, Component.translatable("screen.mistech.inscription.xp", spell.xpLevels()),
                    x + 10 + LIST_W + 14, y + PANEL_H - 22, xp ? GREEN : RED, false);
        }
        super.extractRenderState(graphics, mouseX, mouseY, a);
    }

    /** Modo Cristal: para que a magia serve no Conjurador, o cristal vazio, a mana e o poder que ficará gravado. */
    private void drawCrystalCost(GuiGraphicsExtractor graphics, LocalPlayer player, Spell spell, int dx, int dy, int dw, int mouseX, int mouseY) {
        var mode = com.mistech.mistechmod.spellcraft.conjurer.BlockCasts.mode(spell.id());
        if (mode == null) {
            graphics.textWithWordWrap(this.font, Component.translatable("screen.mistech.inscription.crystal_incompatible"), dx, dy, dw, RED, false);
            return;
        }
        // uma linha curta com o modo; o texto completo aparece ao passar o mouse
        Component use = Component.translatable("tooltip.mistech.spell_crystal.use." + mode.id());
        graphics.text(this.font, Component.translatable("screen.mistech.inscription.crystal_use." + mode.id()), dx, dy, TEXT, false);
        if (mouseX >= dx && mouseX < dx + dw && mouseY >= dy - 1 && mouseY < dy + 9) {
            graphics.setTooltipForNextFrame(this.font, this.font.split(use, 180), mouseX, mouseY);
        }
        dy += 12;
        ItemStack blank = new ItemStack(com.mistech.mistechmod.spellcraft.conjurer.ConjurerContent.BLANK_SPELL_CRYSTAL.get());
        boolean has = player.hasInfiniteMaterials() || AstrologyService.countItem(player, blank.getItem()) >= 1;
        MysticUi.inset(graphics, dx - 1, dy - 1, 18, 18, MysticUi.SLOT);
        graphics.item(blank, dx, dy);
        graphics.fill(dx, dy + 18, dx + 16, dy + 19, has ? GREEN : RED);
        if (mouseX >= dx && mouseX < dx + 16 && mouseY >= dy && mouseY < dy + 16) {
            graphics.setTooltipForNextFrame(this.font, blank, mouseX, mouseY);
        }
        int mana = InscriptionService.engraveMana(spell);
        boolean enough = player.hasInfiniteMaterials() || ManaService.get(player) >= mana;
        graphics.text(this.font, Component.translatable("screen.mistech.inscription.crystal_mana", mana), dx + 22, dy, enough ? GREEN : RED, false);
        int power = (int) Math.round(Affinity.power(player, spell.element()) * com.mistech.mistechmod.Config.conjurerPower() * 100);
        graphics.text(this.font, Component.translatable("screen.mistech.inscription.crystal_power", power), dx + 22, dy + 10, 0xFF1B5E8A, false);
    }

    private static String roman(int tier) {
        return switch (tier) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> "VI";
        };
    }
}

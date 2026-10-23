package com.mistech.mistechmod.spellcraft.client;

import java.util.ArrayList;
import java.util.List;

import com.mistech.mistechmod.item.RuneElement;
import com.mistech.mistechmod.spellcraft.Spell;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

/**
 * Peças de interface do sistema de magias: o visual do inventário do Minecraft (painel cinza chanfrado, casas
 * embutidas, tooltip roxo) com toques místicos (gemas nos cantos, brilho pulsante, cintilar, glifos da mesa de
 * encantamento).
 */
public final class MysticUi {
    public static final int PANEL = 0xFFC6C6C6;
    public static final int WHITE = 0xFFFFFFFF;
    public static final int SHADOW = 0xFF555555;
    public static final int SLOT = 0xFF8B8B8B;
    public static final int SLOT_DARK = 0xFF373737;
    public static final int TEXT = 0xFF404040;
    public static final int ARCANE = 0xFF8A4FD8;
    public static final int GOLD = 0xFFFFD866;
    public static final int GOOD = 0xFF4AA84A;
    public static final int BAD = 0xFFB83A3A;

    private static final FontDescription ALT = new FontDescription.Resource(Identifier.withDefaultNamespace("alt"));

    private MysticUi() {}

    /** Texto com os glifos da mesa de encantamento (Língua Galáctica Padrão). */
    public static MutableComponent glyphs(String text) {
        return Component.literal(text).withStyle(Style.EMPTY.withFont(ALT));
    }

    /** Painel do inventário (cinza, borda preta chanfrada, realce branco em cima/esquerda, sombra embaixo/direita) + gemas arcanas nos cantos. */
    public static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x, y + 1, x + w, y + h - 1, 0xFF000000);
        g.fill(x + 1, y, x + w - 1, y + h, 0xFF000000);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, PANEL);
        g.fill(x + 1, y + 1, x + w - 3, y + 3, WHITE);
        g.fill(x + 1, y + 1, x + 3, y + h - 3, WHITE);
        g.fill(x + 3, y + h - 3, x + w - 1, y + h - 1, SHADOW);
        g.fill(x + w - 3, y + 3, x + w - 1, y + h - 1, SHADOW);
        g.fill(x + w - 3, y + 1, x + w - 1, y + 3, PANEL);
        g.fill(x + 1, y + h - 3, x + 3, y + h - 1, PANEL);
        gem(g, x + 4, y + 4);
        gem(g, x + w - 7, y + 4);
        gem(g, x + 4, y + h - 7);
        gem(g, x + w - 7, y + h - 7);
    }

    private static void gem(GuiGraphicsExtractor g, int x, int y) {
        g.fill(x + 1, y, x + 2, y + 3, ARCANE);
        g.fill(x, y + 1, x + 3, y + 2, ARCANE);
        g.fill(x + 1, y + 1, x + 2, y + 2, 0xFFE6D0FF);
    }

    /** Casa embutida do inventário: sombra escura em cima/esquerda, branco embaixo/direita. */
    public static void inset(GuiGraphicsExtractor g, int x, int y, int w, int h, int fill) {
        g.fill(x, y, x + w, y + h, SLOT_DARK);
        g.fill(x + 1, y + 1, x + w, y + h, WHITE);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, fill);
    }

    /** Botão chanfrado do Minecraft (relevo em vez de embutido). */
    public static void bevel(GuiGraphicsExtractor g, int x, int y, int w, int h, int fill, boolean active) {
        g.fill(x, y, x + w, y + h, 0xFF000000);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, fill);
        if (active) {
            g.fill(x + 1, y + 1, x + w - 1, y + 2, 0x80FFFFFF);
            g.fill(x + 1, y + 1, x + 2, y + h - 1, 0x80FFFFFF);
            g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, 0x60000000);
            g.fill(x + w - 2, y + 1, x + w - 1, y + h - 1, 0x60000000);
        }
    }

    /** Caixa de dica do Minecraft (fundo quase preto com borda roxa). */
    public static void tooltipBox(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x + 1, y, x + w - 1, y + h, 0xF0100010);
        g.fill(x, y + 1, x + w, y + h - 1, 0xF0100010);
        g.fillGradient(x + 1, y + 1, x + 2, y + h - 1, 0x505000FF, 0x5028007F);
        g.fillGradient(x + w - 2, y + 1, x + w - 1, y + h - 1, 0x505000FF, 0x5028007F);
        g.fill(x + 1, y + 1, x + w - 1, y + 2, 0x505000FF);
        g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, 0x5028007F);
    }

    /** Brilho pulsante na cor do elemento em volta de uma casa. */
    public static void glow(GuiGraphicsExtractor g, int x, int y, int size, int rgb) {
        double pulse = 0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 220.0);
        int a1 = 0x70 + (int) (0x70 * pulse);
        int a2 = 0x20 + (int) (0x30 * pulse);
        g.outline(x - 1, y - 1, size + 2, size + 2, a1 << 24 | (rgb & 0xFFFFFF));
        g.outline(x - 2, y - 2, size + 4, size + 4, a2 << 24 | (rgb & 0xFFFFFF));
    }

    /** Moldura de seleção branca (como a da hotbar). */
    public static void selection(GuiGraphicsExtractor g, int x, int y, int size) {
        g.outline(x - 2, y - 2, size + 4, size + 4, 0xFF000000);
        g.outline(x - 1, y - 1, size + 2, size + 2, WHITE);
    }

    /** Pontinho de luz que aparece e some em posições diferentes de cada casa. */
    public static void sparkle(GuiGraphicsExtractor g, int x, int y, int size, int seed) {
        long step = System.currentTimeMillis() / 110 + seed * 7L;
        int phase = (int) (step % 14);
        if (phase > 3) {
            return;
        }
        long cycle = step / 14;
        int px = x + 2 + (int) Math.floorMod(cycle * 5 + seed * 3, size - 4);
        int py = y + 2 + (int) Math.floorMod(cycle * 3 + seed * 5, size - 4);
        int a = phase == 1 || phase == 2 ? 0xE0 : 0x90;
        g.fill(px, py, px + 1, py + 1, a << 24 | 0xFFFFFF);
        if (phase == 1 || phase == 2) {
            g.fill(px - 1, py, px, py + 1, 0x80FFFFFF);
            g.fill(px + 1, py, px + 2, py + 1, 0x80FFFFFF);
            g.fill(px, py - 1, px + 1, py, 0x80FFFFFF);
            g.fill(px, py + 1, px + 1, py + 2, 0x80FFFFFF);
        }
    }

    /**
     * Uma casa de magia no estilo do inventário: casa embutida tingida pelo elemento, símbolo no meio e pontinhos
     * dourados embaixo (um por tier). {@code size} é o lado externo (18 como no inventário).
     */
    public static void spellTile(GuiGraphicsExtractor g, Spell spell, int x, int y, int size, boolean locked, boolean dim, int seed) {
        RuneElement element = spell.element();
        inset(g, x, y, size, size, SLOT);
        g.fill(x + 1, y + 1, x + size - 1, y + size - 1, 0x66000000 | (element.color() & 0xFFFFFF));
        ElementIcon.draw(g, element, x + (size - ElementIcon.SIZE) / 2, y + 2, 1);
        int step = spell.tier() > 5 ? 2 : 3;
        int pipW = spell.tier() > 5 ? 1 : 2;
        int pipsW = (spell.tier() - 1) * step + pipW;
        int px = x + (size - pipsW) / 2;
        for (int i = 0; i < spell.tier(); i++) {
            g.fill(px + i * step, y + size - 5, px + i * step + pipW, y + size - 3, spell.tier() > 5 ? 0xFFFFA0FF : GOLD);
        }
        if (locked || dim) {
            g.fill(x + 1, y + 1, x + size - 1, y + size - 1, locked ? 0x80A02020 : 0x90203050);
        } else {
            sparkle(g, x, y, size, seed);
        }
    }

    /** Linhas da dica de uma magia (nome, elemento/tier, descrição, mana, requisito). */
    public static List<Component> spellTooltip(Spell spell, boolean hasAffinity, int manaCost, int affinityLevel) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(spell.nameKey()).withColor(spell.element().color()));
        lines.add(Component.translatable(spell.element().translationKey()).append(" · ")
                .append(Component.translatable("tooltip.mistech.spell.tier", spell.tier())).append(" · ")
                .append(Component.translatable("tooltip.mistech.spell.kind." + spell.kind().name().toLowerCase(java.util.Locale.ROOT))).withStyle(net.minecraft.ChatFormatting.GRAY));
        lines.add(Component.translatable(spell.descKey()).withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        lines.add(Component.translatable(spell.channelTicks() > 0 ? "tooltip.mistech.spell.mana_channel" : "tooltip.mistech.spell.mana", manaCost)
                .withStyle(net.minecraft.ChatFormatting.AQUA));
        lines.add(Component.translatable("tooltip.mistech.spell.requires", spell.requiredLevel(), spell.requirementName())
                .withStyle(hasAffinity ? net.minecraft.ChatFormatting.GREEN : net.minecraft.ChatFormatting.RED));
        if (affinityLevel > 0) {
            lines.add(Component.translatable("tooltip.mistech.spell.power", affinityLevel * 10).withStyle(net.minecraft.ChatFormatting.GOLD));
            if (affinityLevel >= com.mistech.mistechmod.affinity.Affinity.MAX_LEVEL) {
                lines.add(Component.translatable("tooltip.mistech.spell.mastery").withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));
            }
        }
        return lines;
    }

    public static int width(Font font, Component text) {
        return font.width(text);
    }
}

package com.mistech.mistechmod.spellcraft.client;

import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.item.RuneElement;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Símbolo de cada elemento (9×9, desenhado por pixels na cor do elemento), usado na lista de magias, nos espaços do
 * Tomo, no HUD e na barra de filtro. Não depende de textura: sai direto do padrão abaixo.
 */
public final class ElementIcon {
    public static final int SIZE = 9;
    /** Largura/altura da célula de um botão de filtro. */
    public static final int CELL = 13;

    private static final Map<RuneElement, String[]> PATTERNS = Map.ofEntries(
            Map.entry(RuneElement.FIRE, new String[] {
                    "....#....", "...##....", "...###...", "..####.#.", ".#######.", ".#######.", ".#######.", "..#####..", "...###..."}),
            Map.entry(RuneElement.ICE, new String[] {
                    "....#....", ".#..#..#.", "..#.#.#..", "...###...", "#########", "...###...", "..#.#.#..", ".#..#..#.", "....#...."}),
            Map.entry(RuneElement.WATER, new String[] {
                    "....#....", "....#....", "...###...", "...###...", "..#####..", ".#######.", ".#######.", ".#######.", "..#####.."}),
            Map.entry(RuneElement.AIR, new String[] {
                    ".........", ".#####...", "#.....#..", "......#..", "#########", ".........", "..#####..", ".#.....#.", "........."}),
            Map.entry(RuneElement.ENERGY, new String[] {
                    ".....###.", "....###..", "...###...", "..######.", "....###..", "...###...", "..##.....", "..#......", ".#......."}),
            Map.entry(RuneElement.EARTH, new String[] {
                    ".........", "....#....", "...###...", "..#####..", ".#######.", "#########", "#########", "#########", "........."}),
            Map.entry(RuneElement.LIGHT, new String[] {
                    "....#....", ".#..#..#.", "...###...", "#.#####.#", "..#####..", "#.#####.#", "...###...", ".#..#..#.", "....#...."}),
            Map.entry(RuneElement.DARKNESS, new String[] {
                    "...####..", "..###....", ".###.....", ".###.....", ".###.....", ".###.....", "..###....", "...#####.", "....###.."}),
            Map.entry(RuneElement.SPACE, new String[] {
                    "....#....", "....#....", "...###...", "..#####..", "#########", "..#####..", "...###...", "....#....", "....#...."}),
            Map.entry(RuneElement.LIFE, new String[] {
                    "......###", "....#####", "...######", "..#######", "..######.", "..####...", "#.##.....", ".#.......", "#........"}),
            Map.entry(RuneElement.TIME, new String[] {
                    "#########", ".#######.", "..#####..", "...###...", "....#....", "...###...", "..#####..", ".#######.", "#########"}));

    private static final String[] FALLBACK = {
            "....#....", "...###...", "..#####..", ".###.###.", "#########", ".###.###.", "..#####..", "...###...", "....#...."};

    private ElementIcon() {}

    /** Cores escuras (Trevas) ficam ilegíveis sobre o painel escuro: clareia um pouco. */
    private static int color(RuneElement element) {
        int rgb = element.color();
        int r = rgb >> 16 & 255;
        int g = rgb >> 8 & 255;
        int b = rgb & 255;
        if (r * 3 + g * 6 + b < 1100) {
            r += (255 - r) * 45 / 100;
            g += (255 - g) * 45 / 100;
            b += (255 - b) * 45 / 100;
        }
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    /** Desenha o símbolo com o canto superior esquerdo em (x, y); {@code scale} pixels de tela por pixel do símbolo. */
    public static void draw(GuiGraphicsExtractor g, RuneElement element, int x, int y, int scale) {
        String[] rows = PATTERNS.getOrDefault(element, FALLBACK);
        int color = color(element);
        for (int pass = 0; pass < 2; pass++) {
            for (int j = 0; j < SIZE; j++) {
                for (int i = 0; i < SIZE; i++) {
                    if (rows[j].charAt(i) == '#') {
                        int px = x + i * scale + (pass == 0 ? 1 : 0);
                        int py = y + j * scale + (pass == 0 ? 1 : 0);
                        g.fill(px, py, px + scale, py + scale, pass == 0 ? 0xC0000000 : color);
                    }
                }
            }
        }
    }

    /** Barra de filtro: botões chanfrados do Minecraft, um por elemento; o ativo fica "apertado" com moldura dourada. */
    public static void drawFilter(GuiGraphicsExtractor g, List<RuneElement> elements, @Nullable RuneElement active, int x, int y, int cell, int mouseX, int mouseY) {
        for (int i = 0; i < elements.size(); i++) {
            RuneElement e = elements.get(i);
            int cx = x + i * cell;
            boolean on = e == active;
            boolean hover = mouseX >= cx && mouseX < cx + cell && mouseY >= y && mouseY < y + cell;
            MysticUi.bevel(g, cx, y, cell - 1, cell - 1, on ? 0xFF5A5A5A : hover ? 0xFF8F8FBF : 0xFF7C7C7C, !on);
            if (on) {
                g.outline(cx, y, cell - 1, cell - 1, MysticUi.GOLD);
            }
            draw(g, e, cx + (cell - 1 - SIZE) / 2 + (on ? 1 : 0), y + (cell - 1 - SIZE) / 2 + (on ? 1 : 0), 1);
        }
    }

    public static @Nullable RuneElement filterAt(List<RuneElement> elements, int x, int y, int cell, double mouseX, double mouseY) {
        if (mouseY < y || mouseY >= y + cell || mouseX < x) {
            return null;
        }
        int i = (int) ((mouseX - x) / cell);
        return i < elements.size() ? elements.get(i) : null;
    }

    /** Elementos presentes numa lista de magias, na ordem do enum. */
    public static List<RuneElement> present(List<com.mistech.mistechmod.spellcraft.Spell> spells) {
        return java.util.Arrays.stream(RuneElement.values())
                .filter(e -> spells.stream().anyMatch(s -> s.element() == e)).toList();
    }
}

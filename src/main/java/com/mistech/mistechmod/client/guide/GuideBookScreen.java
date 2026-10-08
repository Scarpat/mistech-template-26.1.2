package com.mistech.mistechmod.client.guide;

import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix3x2fStack;
import org.lwjgl.glfw.GLFW;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mistech.mistechmod.Mistech;
import com.mojang.blaze3d.platform.cursor.CursorTypes;

import net.minecraft.ChatFormatting;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

/**
 * Uma entrada do Grimório Mistech aberta como livro de duas páginas: texto, receitas de crafting, da Mesa de
 * Escrita, rituais, fornalha, multiblocos e destaques de item. Volta para o mapa da jornada ({@link GuideMapScreen}).
 * Em cima fica o caminho (Grimório › Capítulo › Entrada); embaixo, o objetivo da entrada e para onde ela leva.
 */
public class GuideBookScreen extends Screen {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Mistech.MODID, "textures/gui/guide_book.png");
    private static final int BOOK_W = 292;
    private static final int BOOK_H = 184;
    private static final int LEFT_X = 20;
    private static final int RIGHT_X = 157;
    private static final int TEXT_Y = 17;
    private static final int PW = 115;
    private static final int PH = 137;
    private static final int LINE = 9;

    private static final int TEXT = 0xFF3B2A1A;
    private static final int TITLE = 0xFF5A2E6B;
    private static final int DIM = 0xFF8A7458;
    private static final int HOVER = 0x28602A70;

    // botões (posição relativa ao livro)
    private static final int PREV_X = 22, NEXT_X = 252, NAV_Y = 159, BACK_X = 44;

    private static final int GOLD = 0xFFFFD86A;
    private static final int GREEN = 0xFF7DDB7A;
    private static final int CRUMB = 0xFFE8DCC8;
    private static final int CRUMB_DIM = 0xFFB0A4BC;

    /** Onde o jogador parou dentro da entrada (volta para a mesma página ao reabrir). */
    private static String savedEntryId = null;
    private static int savedSpread = 0;

    private final GuideMapScreen parent;
    private GuideBook book;
    private final int category;
    private final int entry;
    private int spread = 0;
    private final List<Page> pages = new ArrayList<>();
    private int left, top;
    private ItemStack hoveredStack = ItemStack.EMPTY;
    /** Entradas para onde esta leva (as que dependem dela). */
    private final List<GuideBook.Entry> leadsTo = new ArrayList<>();

    public GuideBookScreen(GuideMapScreen parent, int category, int entry) {
        super(Component.translatable("guide.mistech.title"));
        this.parent = parent;
        this.category = category;
        this.entry = entry;
    }

    private GuideBook.Entry current() {
        return book.categories.get(category).entries().get(entry);
    }

    @Override
    protected void init() {
        this.left = (this.width - BOOK_W) / 2;
        this.top = Math.max(16, (this.height - BOOK_H) / 2 - 4);
        if (book == null) {
            book = GuideBook.get();
            GuideBook.Entry e = current();
            spread = e.id().equals(savedEntryId) ? savedSpread : 0;
            GuideMapScreen.openEntry = e.id();
            GuideProgress.markRead(book, e);
            leadsTo.clear();
            for (String next : book.dependents(e.id())) {
                GuideBook.Entry dependent = book.entry(next);
                if (dependent != null) {
                    leadsTo.add(dependent);
                }
            }
            playPageSound();
        }
        rebuild();
    }

    @Override
    public void removed() {
        savedEntryId = current().id();
        savedSpread = spread;
        GuideProgress.save();
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ================================================================= navegação

    private void rebuild() {
        pages.clear();
        GuideBook.Entry e = current();
        if (e.id().equals("welcome")) {
            pages.add(new HomePage());
        }
        Component entryTitle = Component.translatable(e.name());
        boolean first = true;
        for (JsonObject json : e.pages()) {
            Component title = json.has("title") ? Component.translatable(json.get("title").getAsString())
                    : (first ? entryTitle : null);
            addPage(json, title);
            first = false;
        }
        int maxSpread = Math.max(0, (pages.size() - 1) / 2);
        spread = Math.min(spread, maxSpread);
    }

    /** Altura ocupada antes do texto em cada tipo de receita (sem o título). -1 = não mede. */
    private static int recipeHeaderHeight(String type, JsonObject json) {
        return switch (type) {
            case "crafting" -> 64 + (json.has("shapeless") && json.get("shapeless").getAsBoolean() ? 12 : 0);
            case "writing" -> 78;
            case "ritual" -> 77 + (json.has("condition") ? 11 : 0) + (json.has("min_tier") && json.get("min_tier").getAsInt() > 1 ? 11 : 0);
            case "smelting" -> 48;
            case "spotlight" -> 46; // item grande; o título (ou o nome do item) sempre aparece
            default -> -1;
        };
    }

    private void addPage(JsonObject json, Component title) {
        String type = json.has("type") ? json.get("type").getAsString() : "text";
        // texto que não cabe embaixo da receita vai para as páginas seguintes
        int header = recipeHeaderHeight(type, json);
        if (header >= 0 && json.has("text")) {
            int available = PH - header - (title != null || type.equals("spotlight") ? 14 : 0);
            List<FormattedCharSequence> lines = this.font.split(Component.translatable(json.get("text").getAsString()), PW);
            if (lines.size() * LINE > available) {
                JsonObject copy = json.deepCopy();
                copy.remove("text");
                addPage(copy, title);
                int index = 0;
                int capacity = PH / LINE;
                while (index < lines.size()) {
                    int end = Math.min(lines.size(), index + capacity);
                    pages.add(new TextPage(null, lines.subList(index, end)));
                    index = end;
                }
                return;
            }
        }
        switch (type) {
            case "crafting" -> pages.add(new CraftingPage(json, title));
            case "writing" -> pages.add(new WritingPage(json, title));
            case "ritual" -> pages.add(new RitualPage(json, title));
            case "smelting" -> pages.add(new SmeltingPage(json, title));
            case "multiblock" -> pages.add(new MultiblockPage(json, title));
            case "spotlight" -> pages.add(new SpotlightPage(json, title));
            default -> {
                // texto: quebra em várias páginas se não couber
                List<FormattedCharSequence> lines = this.font.split(Component.translatable(json.get("text").getAsString()), PW);
                int index = 0;
                Component pageTitle = title;
                do {
                    int capacity = (PH - (pageTitle != null ? 14 : 0)) / LINE;
                    int end = Math.min(lines.size(), index + capacity);
                    pages.add(new TextPage(pageTitle, lines.subList(index, end)));
                    index = end;
                    pageTitle = null;
                } while (index < lines.size());
            }
        }
    }

    /** Volta para o mapa da jornada. */
    private void back() {
        GuideMapScreen.openEntry = null;
        savedEntryId = null;
        playPageSound();
        this.minecraft.setScreen(parent);
    }

    /** Abre outra entrada (das que esta revela), se já estiver disponível. */
    private void openOther(GuideBook.Entry other) {
        if (GuideProgress.state(book, other) == GuideProgress.State.LOCKED) {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.CHEST_LOCKED, 1.0F));
            return;
        }
        int otherCategory = book.categoryIndex(other.id());
        int otherIndex = book.categories.get(otherCategory).entries().indexOf(other);
        this.minecraft.setScreen(new GuideBookScreen(parent, otherCategory, otherIndex));
    }

    private boolean hasNext() {
        return (spread + 1) * 2 < pages.size();
    }

    private void turn(int delta) {
        int target = spread + delta;
        if (target >= 0 && target * 2 < pages.size() && target != spread) {
            spread = target;
            playPageSound();
        }
    }

    private void playPageSound() {
        this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
    }

    // ================================================================= entrada

    private boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= left + x && mx < left + x + w && my >= top + y && my < top + y + h;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x(), my = event.y();
        if (event.button() == 1) {
            back();
            return true;
        }
        if (event.button() == 0) {
            if (spread > 0 && inside(mx, my, PREV_X, NAV_Y, 18, 11)) {
                turn(-1);
                return true;
            }
            if (hasNext() && inside(mx, my, NEXT_X, NAV_Y, 18, 11)) {
                turn(1);
                return true;
            }
            if (inside(mx, my, BACK_X, NAV_Y - 2, 15, 14)) {
                back();
                return true;
            }
            if (overCrumb(mx, my) >= 0) {
                back();
                return true;
            }
            int lead = leadAt(mx, my);
            if (lead >= 0) {
                openOther(leadsTo.get(lead));
                return true;
            }
            for (int side = 0; side < 2; side++) {
                int index = spread * 2 + side;
                if (index < pages.size()) {
                    int px = left + (side == 0 ? LEFT_X : RIGHT_X);
                    if (pages.get(index).click(px, top + TEXT_Y, mx, my)) {
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (scrollY != 0) {
            turn(scrollY > 0 ? -1 : 1);
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        switch (event.key()) {
            case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_PAGE_UP -> {
                turn(-1);
                return true;
            }
            case GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_KEY_PAGE_DOWN -> {
                turn(1);
                return true;
            }
            case GLFW.GLFW_KEY_BACKSPACE -> {
                back();
                return true;
            }
            default -> {
                return super.keyPressed(event);
            }
        }
    }

    // ================================================================= desenho

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        hoveredStack = ItemStack.EMPTY;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, left, top, 0.0F, 0.0F, BOOK_W, BOOK_H, 512, 256);

        for (int side = 0; side < 2; side++) {
            int index = spread * 2 + side;
            if (index >= pages.size()) {
                continue;
            }
            int px = left + (side == 0 ? LEFT_X : RIGHT_X);
            pages.get(index).render(graphics, px, top + TEXT_Y, mouseX, mouseY);
            {
                String number = String.valueOf(index + 1);
                graphics.text(this.font, number, px + PW / 2 - this.font.width(number) / 2, top + NAV_Y + 2, DIM, false);
            }
        }

        // setas
        if (spread > 0) {
            boolean hover = inside(mouseX, mouseY, PREV_X, NAV_Y, 18, 11);
            sprite(graphics, PREV_X, NAV_Y, hover ? 60 : 40, 192, 18, 11);
        }
        if (hasNext()) {
            boolean hover = inside(mouseX, mouseY, NEXT_X, NAV_Y, 18, 11);
            sprite(graphics, NEXT_X, NAV_Y, hover ? 20 : 0, 192, 18, 11);
        }
        {
            boolean hover = inside(mouseX, mouseY, BACK_X, NAV_Y - 2, 15, 14);
            sprite(graphics, BACK_X, NAV_Y - 2, hover ? 100 : 80, 192, 15, 14);
            if (hover) {
                graphics.setTooltipForNextFrame(this.font, Component.translatable("guide.mistech.ui.back"), mouseX, mouseY);
            }
        }

        breadcrumb(graphics, mouseX, mouseY);
        footer(graphics, mouseX, mouseY);

        if (!hoveredStack.isEmpty()) {
            graphics.setTooltipForNextFrame(this.font, hoveredStack, mouseX, mouseY);
        }
    }

    private void sprite(GuiGraphicsExtractor graphics, int x, int y, int u, int v, int w, int h) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, left + x, top + y, (float) u, (float) v, w, h, 512, 256);
    }

    /** Texto centralizado na página, sem sombra. */
    private void centered(GuiGraphicsExtractor graphics, Component text, int px, int y, int color) {
        graphics.text(this.font, text, px + PW / 2 - this.font.width(text) / 2, y, color, false);
    }

    /** Texto centralizado que diminui sozinho para caber na largura da página. */
    private void fitted(GuiGraphicsExtractor graphics, Component text, int px, int y, int color, float maxScale) {
        int width = this.font.width(text);
        float scale = Math.min(maxScale, (PW - 4) / (float) Math.max(1, width));
        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(px + PW / 2.0F, y);
        pose.scale(scale, scale);
        graphics.text(this.font, text, -width / 2, 0, color, false);
        pose.popMatrix();
    }

    private int titleBlock(GuiGraphicsExtractor graphics, Component title, int px, int py) {
        if (title == null) {
            return py;
        }
        fitted(graphics, title, px, py, TITLE, 1.0F);
        graphics.fill(px + 10, py + 10, px + PW - 10, py + 11, 0x60896A3C);
        return py + 14;
    }

    private int wrapped(GuiGraphicsExtractor graphics, Component text, int px, int y, int color) {
        List<FormattedCharSequence> lines = this.font.split(text, PW);
        for (FormattedCharSequence line : lines) {
            graphics.text(this.font, line, px, y, color, false);
            y += LINE;
        }
        return y;
    }

    /** Desenha um item (com quantidade) e registra tooltip se o mouse estiver em cima. */
    private void item(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y, int mouseX, int mouseY) {
        if (stack.isEmpty()) {
            return;
        }
        graphics.item(stack, x, y);
        graphics.itemDecorations(this.font, stack, x, y);
        if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
            hoveredStack = stack;
        }
    }

    private void slot(GuiGraphicsExtractor graphics, String id, int count, int x, int y, int mouseX, int mouseY) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 120.0F, 192.0F, 18, 18, 512, 256);
        item(graphics, GuideBook.stack(id, count), x + 1, y + 1, mouseX, mouseY);
    }

    private void arrow(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 140.0F, 192.0F, 18, 11, 512, 256);
    }

    private static String str(JsonObject json, String key) {
        return json.has(key) ? json.get(key).getAsString() : "";
    }

    private static int count(JsonObject json) {
        return json.has("count") ? json.get("count").getAsInt() : 1;
    }

    // ================================================================= caminho e rodapé

    private Component[] crumbs() {
        return new Component[] {
                Component.translatable("guide.mistech.map.root"),
                Component.translatable(book.categories.get(category).name()),
                Component.translatable(current().name())};
    }

    private int crumbY() {
        return top - 12;
    }

    /** Índice da parte clicável do caminho sob o mouse (0 = Grimório, 1 = capítulo), ou -1. */
    private int overCrumb(double mx, double my) {
        int y = crumbY();
        if (my < y || my >= y + 9) {
            return -1;
        }
        Component[] parts = crumbs();
        int x = left + 6;
        for (int i = 0; i < 2; i++) {
            int w = this.font.width(parts[i]);
            if (mx >= x && mx < x + w) {
                return i;
            }
            x += w + this.font.width(" › ");
        }
        return -1;
    }

    private void breadcrumb(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        Component[] parts = crumbs();
        int x = left + 6, y = crumbY();
        int hovered = overCrumb(mouseX, mouseY);
        for (int i = 0; i < parts.length; i++) {
            int color = i == parts.length - 1 ? 0xFFD7A8FF : hovered == i ? GOLD : CRUMB_DIM;
            graphics.text(this.font, parts[i], x, y, color, true);
            x += this.font.width(parts[i]);
            if (i < parts.length - 1) {
                graphics.text(this.font, " › ", x, y, CRUMB_DIM, true);
                x += this.font.width(" › ");
            }
        }
        if (hovered >= 0) {
            graphics.requestCursor(CursorTypes.POINTING_HAND);
        }
    }

    private int footerY() {
        return top + BOOK_H + 4;
    }

    private int leadX(int index) {
        return left + BOOK_W - 6 - (leadsTo.size() - index) * 19;
    }

    private int leadAt(double mx, double my) {
        int y = footerY();
        if (my < y || my >= y + 16) {
            return -1;
        }
        for (int i = 0; i < leadsTo.size(); i++) {
            int x = leadX(i);
            if (mx >= x && mx < x + 16) {
                return i;
            }
        }
        return -1;
    }

    /** Objetivo da entrada (esquerda) e as entradas que ela revela (direita). */
    private void footer(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuideBook.Entry e = current();
        int y = footerY();
        GuideProgress.State state = GuideProgress.state(book, e);
        int x = left + 6;
        if (state == GuideProgress.State.DONE) {
            graphics.text(this.font, Component.translatable("guide.mistech.entry.task_done"), x, y + 4, GREEN, true);
        } else if (e.task().read()) {
            graphics.text(this.font, Component.translatable("guide.mistech.map.task_read"), x, y + 4, CRUMB, true);
        } else {
            // "Objetivo: obter" + ícones dos itens (qualquer um serve)
            Component label = Component.translatable("guide.mistech.map.task_item", "");
            graphics.text(this.font, label, x, y + 4, GOLD, true);
            x += this.font.width(label) + 2;
            int shown = 0;
            for (String id : e.task().items()) {
                if (shown++ >= 6) {
                    break;
                }
                graphics.fill(x - 1, y - 1, x + 17, y + 17, 0x60000000);
                item(graphics, GuideBook.stack(id), x, y, mouseX, mouseY);
                x += 19;
            }
        }
        if (leadsTo.isEmpty()) {
            return;
        }
        Component label = Component.translatable("guide.mistech.map.next").append(" »");
        graphics.text(this.font, label, leadX(0) - 4 - this.font.width(label), y + 4, CRUMB_DIM, true);
        int hovered = leadAt(mouseX, mouseY);
        for (int i = 0; i < leadsTo.size(); i++) {
            GuideBook.Entry next = leadsTo.get(i);
            GuideProgress.State st = GuideProgress.state(book, next);
            int lx = leadX(i);
            int border = st == GuideProgress.State.DONE ? 0xFF4FAE5A : st == GuideProgress.State.AVAILABLE ? 0xFFD9A441 : 0xFF4A4552;
            graphics.fill(lx - 1, y - 1, lx + 17, y + 17, hovered == i ? 0xFFFFFFFF : border);
            graphics.fill(lx, y, lx + 16, y + 16, 0xFF1C1528);
            graphics.item(GuideBook.stack(next.icon()), lx, y);
            if (st == GuideProgress.State.LOCKED) {
                graphics.fill(lx, y, lx + 16, y + 16, 0xC0141218);
            }
            if (hovered == i) {
                List<Component> lines = new ArrayList<>();
                if (st == GuideProgress.State.LOCKED) {
                    lines.add(Component.translatable("guide.mistech.map.hidden"));
                    lines.add(Component.translatable("guide.mistech.map.locked").withStyle(ChatFormatting.RED));
                } else {
                    lines.add(Component.translatable(next.name()).withStyle(ChatFormatting.LIGHT_PURPLE));
                    boolean finished = st == GuideProgress.State.DONE;
                    lines.add(Component.translatable(finished ? "guide.mistech.map.done" : "guide.mistech.map.available")
                            .withStyle(finished ? ChatFormatting.GREEN : ChatFormatting.GOLD));
                }
                graphics.setComponentTooltipForNextFrame(this.font, lines, mouseX, mouseY);
                graphics.requestCursor(st == GuideProgress.State.LOCKED ? CursorTypes.NOT_ALLOWED : CursorTypes.POINTING_HAND);
            }
        }
    }

    // ================================================================= páginas

    private interface Page {
        void render(GuiGraphicsExtractor graphics, int px, int py, int mouseX, int mouseY);

        default boolean click(int px, int py, double mouseX, double mouseY) {
            return false;
        }
    }

    /** Capa: título grande, subtítulo e introdução. */
    private class HomePage implements Page {
        @Override
        public void render(GuiGraphicsExtractor graphics, int px, int py, int mouseX, int mouseY) {
            fitted(graphics, Component.translatable(book.title), px, py + 6, TITLE, 1.5F);
            int y = py + 24;
            fitted(graphics, Component.translatable(book.subtitle), px, y, DIM, 1.0F);
            y += 12;
            // ornamento
            graphics.fill(px + 20, y, px + PW - 20, y + 1, 0x80896A3C);
            graphics.fill(px + PW / 2 - 2, y - 2, px + PW / 2 + 3, y + 3, 0xFF8A5AA0);
            y += 8;
            item(graphics, GuideBook.stack("mistech:guide_book"), px + PW / 2 - 8, y, mouseX, mouseY);
            y += 22;
            wrapped(graphics, Component.translatable(book.intro), px, y, TEXT);
        }
    }

    private class TextPage implements Page {
        private final Component title;
        private final List<FormattedCharSequence> lines;

        TextPage(Component title, List<FormattedCharSequence> lines) {
            this.title = title;
            this.lines = lines;
        }

        @Override
        public void render(GuiGraphicsExtractor graphics, int px, int py, int mouseX, int mouseY) {
            int y = titleBlock(graphics, title, px, py);
            for (FormattedCharSequence line : lines) {
                graphics.text(font, line, px, y, TEXT, false);
                y += LINE;
            }
        }
    }

    /** Receita de crafting 3x3 (com ou sem forma). */
    private class CraftingPage implements Page {
        private final JsonObject json;
        private final Component title;

        CraftingPage(JsonObject json, Component title) {
            this.json = json;
            this.title = title;
        }

        @Override
        public void render(GuiGraphicsExtractor graphics, int px, int py, int mouseX, int mouseY) {
            int y = titleBlock(graphics, title, px, py) + 4;
            int gx = px + 4;
            JsonArray grid = json.getAsJsonArray("grid");
            for (int i = 0; i < 9; i++) {
                String id = i < grid.size() ? grid.get(i).getAsString() : "";
                slot(graphics, id, 1, gx + (i % 3) * 18, y + (i / 3) * 18, mouseX, mouseY);
            }
            arrow(graphics, gx + 60, y + 22);
            slot(graphics, str(json, "result"), count(json), gx + 84, y + 18, mouseX, mouseY);
            y += 58;
            if (json.has("shapeless") && json.get("shapeless").getAsBoolean()) {
                graphics.text(font, Component.translatable("guide.mistech.ui.shapeless"), gx, y, DIM, false);
                y += 12;
            }
            if (json.has("text")) {
                wrapped(graphics, Component.translatable(json.get("text").getAsString()), px, y + 2, TEXT);
            }
        }
    }

    /** Receita da Mesa de Escrita: giz + papel à esquerda, ingredientes em cruz. */
    private class WritingPage implements Page {
        private final JsonObject json;
        private final Component title;

        WritingPage(JsonObject json, Component title) {
            this.json = json;
            this.title = title;
        }

        @Override
        public void render(GuiGraphicsExtractor graphics, int px, int py, int mouseX, int mouseY) {
            int y = titleBlock(graphics, title, px, py) + 4;
            int gx = px;
            slot(graphics, "mistech:ash_chalk", 1, gx, y + 9, mouseX, mouseY);
            slot(graphics, "minecraft:paper", 1, gx, y + 27, mouseX, mouseY);
            int cx = gx + 22;
            int[][] cross = {{18, 0}, {0, 18}, {36, 18}, {18, 36}};
            JsonArray ings = json.getAsJsonArray("ingredients");
            for (int i = 0; i < 4; i++) {
                String id = i < ings.size() ? ings.get(i).getAsString() : "";
                slot(graphics, id, 1, cx + cross[i][0], y + cross[i][1], mouseX, mouseY);
            }
            arrow(graphics, gx + 79, y + 22);
            slot(graphics, str(json, "result"), count(json), gx + 97, y + 18, mouseX, mouseY);
            y += 60;
            int chalk = json.has("chalk") ? json.get("chalk").getAsInt() : 0;
            graphics.text(font, Component.translatable("guide.mistech.ui.chalk", chalk), gx, y, DIM, false);
            y += 12;
            if (json.has("text")) {
                wrapped(graphics, Component.translatable(json.get("text").getAsString()), px, y + 2, TEXT);
            }
        }
    }

    /** Ritual do Altar Impuro: catalisador no centro, 4 pedestais em cruz, essência e condição. */
    private class RitualPage implements Page {
        private final JsonObject json;
        private final Component title;

        RitualPage(JsonObject json, Component title) {
            this.json = json;
            this.title = title;
        }

        @Override
        public void render(GuiGraphicsExtractor graphics, int px, int py, int mouseX, int mouseY) {
            int y = titleBlock(graphics, title, px, py) + 4;
            int gx = px;
            int essence = json.has("essence") ? json.get("essence").getAsInt() : 0;
            // rituais do Santuário usam Essência Purificada
            boolean sanctuary = json.has("sanctuary") && json.get("sanctuary").getAsBoolean();
            slot(graphics, sanctuary ? "mistech:purified_essence_bottle" : "mistech:impure_essence_bottle", Math.max(1, essence), gx, y + 18, mouseX, mouseY);
            int cx = gx + 22;
            // 4 pedestais cardeais; nos altares Tier II+ vêm mais 4 nas diagonais
            int[][] cross = {{18, 0}, {0, 18}, {36, 18}, {18, 36}, {0, 0}, {36, 0}, {0, 36}, {36, 36}};
            JsonArray ings = json.has("ingredients") ? json.getAsJsonArray("ingredients") : new JsonArray();
            for (int i = 0; i < Math.max(4, Math.min(8, ings.size())); i++) {
                String id = i < ings.size() ? ings.get(i).getAsString() : "";
                slot(graphics, id, 1, cx + cross[i][0], y + cross[i][1], mouseX, mouseY);
            }
            slot(graphics, str(json, "catalyst"), 1, cx + 18, y + 18, mouseX, mouseY);
            arrow(graphics, gx + 79, y + 22);
            slot(graphics, str(json, "result"), count(json), gx + 97, y + 18, mouseX, mouseY);
            y += 60;
            graphics.text(font, Component.translatable(sanctuary ? "guide.mistech.ui.ritual_pure_essence" : "guide.mistech.ui.ritual_essence", essence), gx, y, DIM, false);
            y += 11;
            if (json.has("min_tier") && json.get("min_tier").getAsInt() > 1) {
                graphics.text(font, Component.translatable("guide.mistech.ui.ritual_tier", json.get("min_tier").getAsInt()), gx, y, DIM, false);
                y += 11;
            }
            if (json.has("condition")) {
                graphics.text(font, Component.translatable("ritual.mistech.condition." + json.get("condition").getAsString()), gx, y, DIM, false);
                y += 11;
            }
            if (json.has("text")) {
                wrapped(graphics, Component.translatable(json.get("text").getAsString()), px, y + 2, TEXT);
            }
        }
    }

    /** Fornalha / defumador / fogueira. */
    private class SmeltingPage implements Page {
        private final JsonObject json;
        private final Component title;

        SmeltingPage(JsonObject json, Component title) {
            this.json = json;
            this.title = title;
        }

        @Override
        public void render(GuiGraphicsExtractor graphics, int px, int py, int mouseX, int mouseY) {
            int y = titleBlock(graphics, title, px, py) + 6;
            int gx = px + 16;
            slot(graphics, str(json, "input"), 1, gx, y, mouseX, mouseY);
            arrow(graphics, gx + 30, y + 4);
            slot(graphics, str(json, "result"), 1, gx + 62, y, mouseX, mouseY);
            // "fogo": fornalha, defumador e fogueira alternando
            String[] heaters = {"minecraft:furnace", "minecraft:smoker", "minecraft:campfire", "mistech:runic_furnace"};
            String heater = heaters[(int) ((net.minecraft.util.Util.getMillis() / 1000L) % heaters.length)];
            item(graphics, GuideBook.stack(heater), gx + 31, y + 18, mouseX, mouseY);
            y += 42;
            if (json.has("text")) {
                wrapped(graphics, Component.translatable(json.get("text").getAsString()), px, y, TEXT);
            }
        }
    }

    /** Desenho de multibloco visto de cima. */
    private class MultiblockPage implements Page {
        private static final int CELL = 14;
        private final JsonObject json;
        private final Component title;

        MultiblockPage(JsonObject json, Component title) {
            this.json = json;
            this.title = title;
        }

        @Override
        public void render(GuiGraphicsExtractor graphics, int px, int py, int mouseX, int mouseY) {
            int y = titleBlock(graphics, title, px, py) + 2;
            JsonArray pattern = json.getAsJsonArray("pattern");
            JsonObject key = json.getAsJsonObject("key");
            int rows = pattern.size();
            int cols = rows == 0 ? 0 : pattern.get(0).getAsString().length();
            int cell = Math.max(1, Math.min(CELL, PW / Math.max(1, cols)));
            float scale = Math.min(0.75F, (cell - 1) / 16.0F);
            int ox = px + (PW - cols * cell) / 2;
            Matrix3x2fStack pose = graphics.pose();
            for (int r = 0; r < rows; r++) {
                String line = pattern.get(r).getAsString();
                for (int c = 0; c < line.length(); c++) {
                    int x = ox + c * cell, cy = y + r * cell;
                    String symbol = String.valueOf(line.charAt(c));
                    if (!key.has(symbol)) {
                        graphics.fill(x + cell / 2 - 1, cy + cell / 2 - 1, x + cell / 2 + 1, cy + cell / 2 + 1, 0x40896A3C);
                        continue;
                    }
                    ItemStack stack = GuideBook.stack(key.get(symbol).getAsString());
                    pose.pushMatrix();
                    pose.translate(x + (cell - 16 * scale) / 2F, cy + (cell - 16 * scale) / 2F);
                    pose.scale(scale, scale);
                    graphics.item(stack, 0, 0);
                    pose.popMatrix();
                    if (mouseX >= x && mouseX < x + cell && mouseY >= cy && mouseY < cy + cell) {
                        hoveredStack = stack;
                    }
                }
            }
            y += rows * cell + 4;
            if (json.has("text")) {
                wrapped(graphics, Component.translatable(json.get("text").getAsString()), px, y, TEXT);
            }
        }
    }

    /** Destaque de um item (ícone grande + texto). */
    private class SpotlightPage implements Page {
        private final JsonObject json;
        private final Component title;

        SpotlightPage(JsonObject json, Component title) {
            this.json = json;
            this.title = title;
        }

        @Override
        public void render(GuiGraphicsExtractor graphics, int px, int py, int mouseX, int mouseY) {
            ItemStack stack = GuideBook.stack(str(json, "item"));
            Component heading = title != null ? title : stack.getHoverName();
            int y = titleBlock(graphics, heading, px, py) + 2;
            int ix = px + PW / 2 - 16;
            graphics.fill(ix - 4, y - 2, ix + 36, y + 38, 0x20896A3C);
            Matrix3x2fStack pose = graphics.pose();
            pose.pushMatrix();
            pose.translate(ix, y + 2);
            pose.scale(2.0F, 2.0F);
            graphics.item(stack, 0, 0);
            pose.popMatrix();
            if (mouseX >= ix && mouseX < ix + 32 && mouseY >= y && mouseY < y + 36) {
                hoveredStack = stack;
            }
            y += 44;
            if (json.has("text")) {
                wrapped(graphics, Component.translatable(json.get("text").getAsString()), px, y, TEXT);
            }
        }
    }
}

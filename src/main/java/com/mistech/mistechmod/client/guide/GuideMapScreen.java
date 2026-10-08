package com.mistech.mistechmod.client.guide;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joml.Matrix3x2fStack;
import org.lwjgl.glfw.GLFW;

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
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;

/**
 * Mapa da jornada do Grimório, no estilo dos livros de missões de modpack: capítulos na lateral e, no meio,
 * as entradas do capítulo como nós ligados pelas dependências. Arrastar move o mapa, a roda dá zoom e clicar
 * num nó abre a entrada no livro.
 */
public class GuideMapScreen extends Screen {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Mistech.MODID, "textures/gui/guide_map.png");
    private static final int TEX_W = 256, TEX_H = 128;
    /** Tamanho de uma célula do mapa, em pixels (zoom 1). */
    private static final int CELL = 34;
    private static final float MIN_ZOOM = 0.5F, MAX_ZOOM = 2.0F;

    // paleta
    private static final int FRAME_OUT = 0xFF0B0810;
    private static final int FRAME_GOLD = 0xFF8A6A3C;
    private static final int FRAME_GOLD_HI = 0xFFC8A060;
    private static final int SIDEBAR = 0xF0181320;
    private static final int HEADER = 0xF01D1727;
    private static final int CANVAS = 0xF0110D17;
    private static final int GRID_DOT = 0x22C8B4FF;
    private static final int TEXT = 0xFFE8DCC8;
    private static final int TEXT_DIM = 0xFF8C8098;
    private static final int TITLE = 0xFFD7A8FF;
    private static final int GOLD = 0xFFFFD86A;
    private static final int GREEN = 0xFF7DDB7A;
    private static final int LINE_DONE = 0xFF4FAE5A;
    private static final int LINE_OPEN = 0xFFC89A40;
    private static final int LINE_LOCKED = 0xFF3A3542;

    private static final int ROW_H = 20;
    private static final int HEADER_H = 30;

    // ---- estado lembrado entre aberturas
    private static int savedChapter = -1;
    /** Organização escolhida (por assunto, progressão ou dimensão) e o capítulo aberto em cada uma. */
    private static String savedViewMode = GuideBook.TYPE_VIEW;
    private static final Map<String, Integer> savedChapters = new HashMap<>();
    private static final Map<String, float[]> savedViews = new HashMap<>();
    /** Entrada que estava aberta quando o livro foi fechado (reabre nela). */
    static String openEntry = null;

    private GuideBook book;
    private int chapter;
    private float panX, panY, zoom = 1.0F;
    private int sidebarScroll = 0;

    // geometria (recalculada no init)
    private int px0, py0, px1, py1, sideW;
    private int cx0, cy0, cx1, cy1;

    // arrasto
    private boolean pressing;
    private boolean dragging;
    private double pressX, pressY;

    public GuideMapScreen() {
        super(Component.translatable("guide.mistech.title"));
    }

    @Override
    protected void init() {
        boolean first = book == null;
        if (first) {
            GuideBook.clearCache();
            book = GuideBook.reload();
            book.setView(savedViewMode);
            savedChapter = savedChapters.getOrDefault(book.view(), -1);
            GuideProgress.prepare();
            chapter = savedChapter >= 0 && savedChapter < book.categories.size()
                    && GuideProgress.chapterOpen(book, book.categories.get(savedChapter)) ? savedChapter : suggestedChapter();
        }
        int margin = 8;
        int maxW = 600, maxH = 360;
        int w = Math.min(maxW, this.width - margin * 2);
        int h = Math.min(maxH, this.height - margin * 2);
        px0 = (this.width - w) / 2;
        py0 = (this.height - h) / 2;
        px1 = px0 + w;
        py1 = py0 + h;
        sideW = w >= 380 ? 122 : 26;
        cx0 = px0 + sideW + 3;
        cy0 = py0 + HEADER_H + 2;
        cx1 = px1 - 2;
        cy1 = py1 - 2;
        if (first) {
            loadView(true);
            playSound(SoundEvents.BOOK_PAGE_TURN);
        }
        clampSidebarScroll();
    }

    @Override
    public void removed() {
        saveView();
        savedChapter = chapter;
        savedChapters.put(book.view(), chapter);
        savedViewMode = book.view();
        GuideProgress.save();
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ================================================================= capítulos e visão

    private GuideBook.Category current() {
        return book.categories.get(chapter);
    }

    /** O capítulo com o próximo passo da jornada (a primeira entrada revelada e ainda não concluída). */
    private int suggestedChapter() {
        GuideBook.Entry next = nextStep();
        int index = next != null ? book.categoryIndex(next.id()) : 0;
        return Math.max(0, Math.min(index, book.categories.size() - 1));
    }

    private GuideBook.Entry nextStep() {
        for (GuideBook.Category category : book.categories) {
            for (GuideBook.Entry entry : category.entries()) {
                if (GuideProgress.state(book, entry) == GuideProgress.State.AVAILABLE) {
                    return entry;
                }
            }
        }
        return null;
    }

    private void selectChapter(int index) {
        if (index == chapter || index < 0 || index >= book.categories.size()) {
            return;
        }
        saveView();
        chapter = index;
        loadView(false);
        playSound(SoundEvents.BOOK_PAGE_TURN);
    }

    private void saveView() {
        if (book != null && !book.categories.isEmpty()) {
            savedViews.put(current().id(), new float[] {panX, panY, zoom});
        }
    }

    private void loadView(boolean focusNext) {
        if (book.categories.isEmpty()) {
            return;
        }
        float[] view = savedViews.get(current().id());
        if (view != null && !focusNext) {
            panX = view[0];
            panY = view[1];
            zoom = view[2];
            return;
        }
        fitChapter();
        if (focusNext) {
            GuideBook.Entry next = nextStep();
            if (next != null && book.categoryIndex(next.id()) == chapter && !visible(next)) {
                centerOn(next);
            }
        }
    }

    /** Ajusta zoom e posição para o capítulo inteiro caber. */
    private void fitChapter() {
        List<GuideBook.Entry> entries = current().entries();
        if (entries.isEmpty()) {
            panX = panY = 0;
            zoom = 1;
            return;
        }
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        for (GuideBook.Entry e : entries) {
            minX = Math.min(minX, e.x());
            minY = Math.min(minY, e.y());
            maxX = Math.max(maxX, e.x());
            maxY = Math.max(maxY, e.y());
        }
        float wPx = (maxX - minX) * CELL + CELL * 2;
        float hPx = (maxY - minY) * CELL + CELL * 2;
        float fit = Math.min((cx1 - cx0) / wPx, (cy1 - cy0) / hPx);
        zoom = Mth.clamp(fit, 0.75F, 1.25F);
        panX = (minX + maxX) / 2 * CELL;
        panY = (minY + maxY) / 2 * CELL;
    }

    private void centerOn(GuideBook.Entry entry) {
        panX = entry.x() * CELL;
        panY = entry.y() * CELL;
    }

    private boolean visible(GuideBook.Entry entry) {
        float sx = toScreenX(entry.x() * CELL), sy = toScreenY(entry.y() * CELL);
        return sx > cx0 + 12 && sx < cx1 - 12 && sy > cy0 + 12 && sy < cy1 - 12;
    }

    private float centerX() {
        return (cx0 + cx1) / 2.0F;
    }

    private float centerY() {
        return (cy0 + cy1) / 2.0F;
    }

    private float toScreenX(float worldX) {
        return centerX() + (worldX - panX) * zoom;
    }

    private float toScreenY(float worldY) {
        return centerY() + (worldY - panY) * zoom;
    }

    private float toWorldX(double screenX) {
        return (float) ((screenX - centerX()) / zoom + panX);
    }

    private float toWorldY(double screenY) {
        return (float) ((screenY - centerY()) / zoom + panY);
    }

    private static int radius(GuideBook.Entry entry) {
        return entry.big() ? 17 : 13;
    }

    private GuideBook.Entry entryAt(double mx, double my) {
        if (!inCanvas(mx, my)) {
            return null;
        }
        float wx = toWorldX(mx), wy = toWorldY(my);
        GuideBook.Entry best = null;
        for (GuideBook.Entry e : current().entries()) {
            float dx = wx - e.x() * CELL, dy = wy - e.y() * CELL;
            int r = radius(e);
            if (dx * dx + dy * dy <= r * r) {
                best = e;
            }
        }
        return best;
    }

    private boolean inCanvas(double mx, double my) {
        return mx >= cx0 && mx < cx1 && my >= cy0 && my < cy1;
    }

    // ================================================================= entrada

    private int chapterRowY(int index) {
        return py0 + 34 + index * ROW_H - sidebarScroll;
    }

    private int chapterAt(double mx, double my) {
        if (mx < px0 + 2 || mx >= px0 + sideW || my < py0 + 32 || my >= py1 - 2) {
            return -1;
        }
        for (int i = 0; i < book.categories.size(); i++) {
            int y = chapterRowY(i);
            if (my >= y && my < y + ROW_H) {
                return i;
            }
        }
        return -1;
    }

    private void clampSidebarScroll() {
        int content = book.categories.size() * ROW_H;
        int view = py1 - 2 - (py0 + 34);
        sidebarScroll = Mth.clamp(sidebarScroll, 0, Math.max(0, content - view));
    }

    // botões do cabeçalho (canto direito)
    private int buttonX(int slot) {
        return px1 - 4 - 18 * (slot + 1);
    }

    private int buttonY() {
        return py0 + 7;
    }

    private boolean overButton(double mx, double my, int slot) {
        int x = buttonX(slot), y = buttonY();
        return mx >= x && mx < x + 16 && my >= y && my < y + 16;
    }

    private boolean overRootCrumb(double mx, double my) {
        Component root = Component.translatable("guide.mistech.map.root");
        int x = cx0 + 6, y = py0 + 6;
        return mx >= x && mx < x + this.font.width(root) && my >= y && my < y + 9;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x(), my = event.y();
        if (event.button() == 0) {
            int row = chapterAt(mx, my);
            if (row >= 0) {
                if (GuideProgress.chapterOpen(book, book.categories.get(row))) {
                    selectChapter(row);
                } else {
                    playSound(SoundEvents.CHEST_LOCKED);
                }
                return true;
            }
            if (overButton(mx, my, 0)) {
                GuideProgress.setFreeMode(!GuideProgress.freeMode());
                playSound(SoundEvents.AMETHYST_BLOCK_CHIME);
                return true;
            }
            if (overButton(mx, my, 2)) {
                cycleView();
                return true;
            }
            if (overButton(mx, my, 1)) {
                GuideBook.Entry next = nextStep();
                if (next != null) {
                    int index = book.categoryIndex(next.id());
                    if (index != chapter) {
                        selectChapter(index);
                    }
                    centerOn(next);
                    playSound(SoundEvents.UI_BUTTON_CLICK.value());
                }
                return true;
            }
            if (overRootCrumb(mx, my)) {
                selectChapter(0);
                return true;
            }
            if (inCanvas(mx, my)) {
                pressing = true;
                dragging = false;
                pressX = mx;
                pressY = my;
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (pressing && event.button() == 0) {
            if (!dragging && Math.abs(event.x() - pressX) + Math.abs(event.y() - pressY) > 3) {
                dragging = true;
            }
            if (dragging) {
                panX -= (float) (dx / zoom);
                panY -= (float) (dy / zoom);
            }
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (pressing && event.button() == 0) {
            pressing = false;
            if (!dragging) {
                GuideBook.Entry entry = entryAt(event.x(), event.y());
                if (entry != null) {
                    openEntry(entry);
                }
            }
            dragging = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    /** Próxima organização do livro (por assunto → progressão → dimensão). */
    private void cycleView() {
        List<String> ids = book.viewIds();
        if (ids.size() < 2) {
            return;
        }
        saveView();
        savedChapters.put(book.view(), chapter);
        String next = ids.get((ids.indexOf(book.view()) + 1) % ids.size());
        book.setView(next);
        savedViewMode = next;
        int remembered = savedChapters.getOrDefault(next, -1);
        chapter = remembered >= 0 && remembered < book.categories.size() && GuideProgress.chapterOpen(book, book.categories.get(remembered))
                ? remembered : suggestedChapter();
        sidebarScroll = 0;
        clampSidebarScroll();
        loadView(remembered < 0);
        playSound(SoundEvents.BOOK_PAGE_TURN);
    }

    private static String viewIcon(String view) {
        return switch (view) {
            case "progress" -> "minecraft:experience_bottle";
            case "dimension" -> "minecraft:filled_map";
            default -> "minecraft:bookshelf";
        };
    }

    private void openEntry(GuideBook.Entry entry) {
        if (GuideProgress.state(book, entry) == GuideProgress.State.LOCKED) {
            playSound(SoundEvents.CHEST_LOCKED);
            return;
        }
        int category = book.categoryIndex(entry.id());
        int index = book.categories.get(category).entries().indexOf(entry);
        saveView();
        this.minecraft.setScreen(new GuideBookScreen(this, category, index));
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (scrollY == 0) {
            return false;
        }
        if (x < px0 + sideW) {
            sidebarScroll -= (int) (scrollY * ROW_H);
            clampSidebarScroll();
            return true;
        }
        if (inCanvas(x, y)) {
            // zoom em volta do mouse
            float wx = toWorldX(x), wy = toWorldY(y);
            zoom = Mth.clamp(zoom * (scrollY > 0 ? 1.15F : 1 / 1.15F), MIN_ZOOM, MAX_ZOOM);
            panX = wx - (float) ((x - centerX()) / zoom);
            panY = wy - (float) ((y - centerY()) / zoom);
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        switch (event.key()) {
            case GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_PAGE_UP -> {
                for (int i = chapter - 1; i >= 0; i--) {
                    if (GuideProgress.chapterOpen(book, book.categories.get(i))) {
                        selectChapter(i);
                        break;
                    }
                }
                return true;
            }
            case GLFW.GLFW_KEY_DOWN, GLFW.GLFW_KEY_PAGE_DOWN -> {
                for (int i = chapter + 1; i < book.categories.size(); i++) {
                    if (GuideProgress.chapterOpen(book, book.categories.get(i))) {
                        selectChapter(i);
                        break;
                    }
                }
                return true;
            }
            case GLFW.GLFW_KEY_HOME -> {
                fitChapter();
                return true;
            }
            default -> {
                return super.keyPressed(event);
            }
        }
    }

    private void playSound(net.minecraft.sounds.SoundEvent sound) {
        this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(sound, 1.0F));
    }

    // ================================================================= desenho

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        if (book.categories.isEmpty()) {
            return;
        }
        frame(graphics);
        sidebar(graphics, mouseX, mouseY);
        header(graphics, mouseX, mouseY);
        canvas(graphics, mouseX, mouseY);
    }

    /** Moldura dourada em volta de tudo, com cantos ornamentados. */
    private void frame(GuiGraphicsExtractor g) {
        g.fill(px0 - 3, py0 - 3, px1 + 3, py1 + 3, FRAME_OUT);
        g.fill(px0 - 2, py0 - 2, px1 + 2, py1 + 2, FRAME_GOLD);
        g.fill(px0 - 1, py0 - 1, px1 + 1, py1 + 1, FRAME_OUT);
        g.fill(px0, py0, px1, py1, SIDEBAR);
        // separadores
        g.fill(px0 + sideW, py0, px0 + sideW + 1, py1, FRAME_GOLD);
        g.fill(px0 + sideW + 1, py0, px0 + sideW + 2, py1, FRAME_OUT);
        g.fill(px0 + sideW + 2, py0, px1, py0 + HEADER_H, HEADER);
        g.fill(px0 + sideW + 2, py0 + HEADER_H, px1, py0 + HEADER_H + 1, FRAME_GOLD);
        g.fill(px0 + sideW + 2, py0 + HEADER_H + 1, px1, py0 + HEADER_H + 2, FRAME_OUT);
        // cantos
        int[][] corners = {{px0 - 3, py0 - 3}, {px1 - 4, py0 - 3}, {px0 - 3, py1 - 4}, {px1 - 4, py1 - 4}};
        for (int[] c : corners) {
            g.fill(c[0], c[1], c[0] + 7, c[1] + 7, FRAME_OUT);
            g.fill(c[0] + 1, c[1] + 1, c[0] + 6, c[1] + 6, FRAME_GOLD_HI);
            g.fill(c[0] + 2, c[1] + 2, c[0] + 5, c[1] + 5, 0xFF5A2E6B);
        }
    }

    private void sidebar(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        boolean compact = sideW < 60;
        // topo: progresso total
        int total = book.size(), done = GuideProgress.doneCount(book);
        if (!compact) {
            Component title = Component.translatable(book.title);
            g.text(this.font, trim(title.getString(), sideW - 10), px0 + 6, py0 + 6, TITLE, false);
            bar(g, px0 + 6, py0 + 18, sideW - 12, done, total);
            String count = done + "/" + total;
            g.text(this.font, count, px0 + sideW - 6 - this.font.width(count), py0 + 22, TEXT_DIM, false);
        } else {
            g.item(GuideBook.stack("mistech:guide_book"), px0 + 5, py0 + 8);
        }
        g.fill(px0 + 4, py0 + 31, px0 + sideW - 4, py0 + 32, 0x408A6A3C);

        g.enableScissor(px0, py0 + 33, px0 + sideW, py1 - 1);
        int hovered = chapterAt(mouseX, mouseY);
        for (int i = 0; i < book.categories.size(); i++) {
            GuideBook.Category c = book.categories.get(i);
            int y = chapterRowY(i);
            if (y + ROW_H < py0 + 33 || y > py1) {
                continue;
            }
            boolean open = GuideProgress.chapterOpen(book, c);
            int cDone = GuideProgress.doneCount(c), cTotal = c.entries().size();
            boolean complete = cTotal > 0 && cDone == cTotal;
            if (i == chapter) {
                g.fill(px0 + 2, y, px0 + sideW - 1, y + ROW_H - 1, 0x40B080FF);
                g.fill(px0 + 2, y, px0 + 4, y + ROW_H - 1, 0xFFB080FF);
            } else if (i == hovered && open) {
                g.fill(px0 + 2, y, px0 + sideW - 1, y + ROW_H - 1, 0x20FFFFFF);
            }
            g.item(GuideBook.stack(c.icon()), px0 + 6, y + 2);
            if (!open) {
                g.fill(px0 + 6, y + 2, px0 + 22, y + 18, 0xC0110D17);
                sprite(g, px0 + 10, y + 5, 18, 64, 10, 11);
            }
            if (!compact) {
                Component name = open ? Component.translatable(c.name()) : Component.translatable("guide.mistech.map.hidden");
                int color = !open ? TEXT_DIM : complete ? GREEN : i == chapter ? 0xFFFFFFFF : TEXT;
                g.text(this.font, trim(name.getString(), sideW - 32), px0 + 26, y + 3, color, false);
                if (open) {
                    // barrinha de progresso do capítulo
                    int bw = sideW - 34;
                    g.fill(px0 + 26, y + 14, px0 + 26 + bw, y + 16, 0xFF2A2433);
                    if (cTotal > 0) {
                        g.fill(px0 + 26, y + 14, px0 + 26 + bw * cDone / cTotal, y + 16, complete ? LINE_DONE : LINE_OPEN);
                    }
                    if (hasNew(c)) {
                        sprite(g, px0 + sideW - 9, y + 3, 12, 64, 5, 9);
                    }
                }
            }
        }
        g.disableScissor();

        if (hovered >= 0) {
            GuideBook.Category c = book.categories.get(hovered);
            List<Component> lines = new ArrayList<>();
            if (GuideProgress.chapterOpen(book, c)) {
                lines.add(Component.translatable(c.name()).withStyle(ChatFormatting.LIGHT_PURPLE));
                lines.add(Component.translatable("guide.mistech.map.chapter_progress", GuideProgress.doneCount(c), c.entries().size())
                        .withStyle(ChatFormatting.GRAY));
            } else {
                lines.add(Component.translatable("guide.mistech.map.chapter_locked").withStyle(ChatFormatting.DARK_GRAY));
                lines.add(Component.translatable("guide.mistech.map.chapter_locked.hint").withStyle(ChatFormatting.GRAY));
            }
            g.setComponentTooltipForNextFrame(this.font, lines, mouseX, mouseY);
            if (GuideProgress.chapterOpen(book, c)) {
                g.requestCursor(CursorTypes.POINTING_HAND);
            }
        }
    }

    /** Capítulo tem entrada revelada que o jogador ainda não leu? */
    private boolean hasNew(GuideBook.Category c) {
        for (GuideBook.Entry e : c.entries()) {
            if (GuideProgress.state(book, e) == GuideProgress.State.AVAILABLE && !GuideProgress.wasRead(e.id())) {
                return true;
            }
        }
        return false;
    }

    private void header(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        GuideBook.Category c = current();
        int x = cx0 + 6, y = py0 + 6;
        Component root = Component.translatable("guide.mistech.map.root");
        boolean rootHover = overRootCrumb(mouseX, mouseY);
        g.text(this.font, root, x, y, rootHover ? GOLD : TEXT_DIM, false);
        x += this.font.width(root);
        g.text(this.font, " › ", x, y, TEXT_DIM, false);
        x += this.font.width(" › ");
        int maxNameW = buttonX(2) - 8 - x - 40;
        g.text(this.font, trim(Component.translatable(c.name()).getString(), maxNameW), x, y, TITLE, false);
        String desc = Component.translatable(c.description()).getString();
        g.text(this.font, trim(desc, buttonX(2) - 8 - (cx0 + 6)), cx0 + 6, y + 12, TEXT_DIM, false);

        // progresso do capítulo
        int cDone = GuideProgress.doneCount(c), cTotal = c.entries().size();
        String count = cDone + "/" + cTotal;
        int countX = buttonX(2) - 8 - this.font.width(count);
        g.text(this.font, count, countX, y, cDone == cTotal ? GREEN : GOLD, false);

        // botões: [organização] [próximo passo] [modo livre]
        if (book.viewIds().size() > 1) {
            button(g, 2, GuideBook.stack(viewIcon(book.view())), mouseX, mouseY);
        }
        button(g, 1, GuideBook.stack("minecraft:compass"), mouseX, mouseY);
        button(g, 0, GuideBook.stack(GuideProgress.freeMode() ? "minecraft:ender_eye" : "minecraft:ender_pearl"), mouseX, mouseY);
        if (overButton(mouseX, mouseY, 0)) {
            g.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.translatable(GuideProgress.freeMode() ? "guide.mistech.map.free_on" : "guide.mistech.map.free_off"),
                    Component.translatable("guide.mistech.map.free_hint").withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
            g.requestCursor(CursorTypes.POINTING_HAND);
        } else if (overButton(mouseX, mouseY, 2) && book.viewIds().size() > 1) {
            g.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.translatable("guide.mistech.map.view", Component.translatable("guide.mistech.map.view." + book.view())
                            .withStyle(ChatFormatting.GOLD)),
                    Component.translatable("guide.mistech.map.view_hint").withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
            g.requestCursor(CursorTypes.POINTING_HAND);
        } else if (overButton(mouseX, mouseY, 1)) {
            GuideBook.Entry next = nextStep();
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("guide.mistech.map.next"));
            lines.add(next != null ? Component.translatable(next.name()).withStyle(ChatFormatting.GOLD)
                    : Component.translatable("guide.mistech.map.all_done").withStyle(ChatFormatting.GREEN));
            g.setComponentTooltipForNextFrame(this.font, lines, mouseX, mouseY);
            g.requestCursor(CursorTypes.POINTING_HAND);
        } else if (rootHover) {
            g.requestCursor(CursorTypes.POINTING_HAND);
        }
    }

    private void button(GuiGraphicsExtractor g, int slot, ItemStack icon, int mouseX, int mouseY) {
        int x = buttonX(slot), y = buttonY();
        boolean hover = overButton(mouseX, mouseY, slot);
        g.fill(x - 1, y - 1, x + 17, y + 17, hover ? FRAME_GOLD_HI : FRAME_GOLD);
        g.fill(x, y, x + 16, y + 16, hover ? 0xFF3A2C52 : 0xFF221B2E);
        g.item(icon, x, y);
    }

    private void canvas(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.enableScissor(cx0, cy0, cx1, cy1);
        g.fill(cx0, cy0, cx1, cy1, CANVAS);
        // pontinhos do papel arcano
        float step = CELL / 2.0F * zoom;
        if (step >= 6) {
            float startX = toScreenX((float) Math.floor(toWorldX(cx0) / (CELL / 2.0F)) * (CELL / 2.0F));
            float startY = toScreenY((float) Math.floor(toWorldY(cy0) / (CELL / 2.0F)) * (CELL / 2.0F));
            for (float sx = startX; sx < cx1; sx += step) {
                for (float sy = startY; sy < cy1; sy += step) {
                    int ix = Math.round(sx), iy = Math.round(sy);
                    g.fill(ix, iy, ix + 1, iy + 1, GRID_DOT);
                }
            }
        }
        // sombra nas bordas
        g.fillGradient(cx0, cy0, cx1, cy0 + 10, 0x80000000, 0x00000000);
        g.fillGradient(cx0, cy1 - 10, cx1, cy1, 0x00000000, 0x80000000);

        GuideBook.Entry hovered = dragging ? null : entryAt(mouseX, mouseY);
        List<GuideBook.Entry> entries = current().entries();
        long millis = Util.getMillis();

        Matrix3x2fStack pose = g.pose();
        pose.pushMatrix();
        pose.translate(centerX(), centerY());
        pose.scale(zoom, zoom);
        pose.translate(-panX, -panY);

        // linhas
        for (GuideBook.Entry e : entries) {
            for (String depId : e.deps()) {
                GuideBook.Entry dep = book.entry(depId);
                if (dep == null || book.categoryIndex(depId) != chapter) {
                    continue;
                }
                boolean depDone = GuideProgress.isDone(depId);
                GuideProgress.State st = GuideProgress.state(book, e);
                int color = depDone && st == GuideProgress.State.DONE ? LINE_DONE
                        : depDone ? LINE_OPEN : LINE_LOCKED;
                boolean highlight = hovered != null && (hovered == e || hovered == dep);
                line(g, dep.x() * CELL, dep.y() * CELL, e.x() * CELL, e.y() * CELL, highlight ? 0xFFFFFFFF : color,
                        depDone && st == GuideProgress.State.AVAILABLE, millis);
            }
        }

        // nós
        for (GuideBook.Entry e : entries) {
            node(g, e, e == hovered, millis);
        }
        pose.popMatrix();

        g.disableScissor();

        if (hovered != null) {
            g.setComponentTooltipForNextFrame(this.font, tooltip(hovered), mouseX, mouseY);
            if (GuideProgress.state(book, hovered) != GuideProgress.State.LOCKED) {
                g.requestCursor(CursorTypes.POINTING_HAND);
            } else {
                g.requestCursor(CursorTypes.NOT_ALLOWED);
            }
        } else if (dragging) {
            g.requestCursor(CursorTypes.RESIZE_ALL);
        }
    }

    /** Linha grossa entre dois pontos; as que levam a uma entrada recém-revelada têm um brilho correndo. */
    private void line(GuiGraphicsExtractor g, float x0, float y0, float x1, float y1, int color, boolean flowing, long millis) {
        float dx = x1 - x0, dy = y1 - y0;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length < 1) {
            return;
        }
        Matrix3x2fStack pose = g.pose();
        pose.pushMatrix();
        pose.translate(x0, y0);
        pose.rotate((float) Math.atan2(dy, dx));
        int len = Math.round(length);
        g.fill(0, -2, len, 2, 0xFF0B0810);
        g.fill(0, -1, len, 1, color);
        if (flowing) {
            int pulse = (int) ((millis / 25) % Math.max(1, len + 12)) - 6;
            g.fill(Math.max(0, pulse - 4), -1, Math.min(len, pulse + 4), 1, 0xFFFFF0B0);
        }
        pose.popMatrix();
    }

    private void node(GuiGraphicsExtractor g, GuideBook.Entry e, boolean hover, long millis) {
        GuideProgress.State st = GuideProgress.state(book, e);
        int size = e.big() ? 34 : 26;
        int v = e.big() ? 26 : 0;
        int u = switch (st) {
            case AVAILABLE -> 0;
            case DONE -> size;
            case LOCKED -> size * 2;
        };
        int x = Math.round(e.x() * CELL) - size / 2;
        int y = Math.round(e.y() * CELL) - size / 2;
        // brilho pulsando nas entradas disponíveis
        if (st == GuideProgress.State.AVAILABLE) {
            float t = (float) (Math.sin(millis / 300.0) * 0.5 + 0.5);
            int alpha = (int) (60 + t * 120);
            g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, size * 3, v, size, size, TEX_W, TEX_H, (alpha << 24) | 0xFFD86A);
        }
        g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, u, v, size, size, TEX_W, TEX_H);
        ItemStack icon = GuideBook.stack(e.icon());
        Matrix3x2fStack pose = g.pose();
        if (e.big()) {
            pose.pushMatrix();
            pose.translate(x + size / 2.0F - 10, y + size / 2.0F - 10);
            pose.scale(1.25F, 1.25F);
            g.item(icon, 0, 0);
            pose.popMatrix();
        } else {
            g.item(icon, x + size / 2 - 8, y + size / 2 - 8);
        }
        if (st == GuideProgress.State.LOCKED) {
            int inset = e.big() ? 6 : 5;
            g.fill(x + inset, y + inset, x + size - inset, y + size - inset, 0xC0141218);
            sprite(g, x + size / 2 - 5, y + size / 2 - 5, 18, 64, 10, 11);
        }
        if (st == GuideProgress.State.DONE) {
            sprite(g, x + size - 10, y + size - 9, 0, 64, 11, 9);
        } else if (st == GuideProgress.State.AVAILABLE && !GuideProgress.wasRead(e.id())) {
            int bob = (int) Math.round(Math.sin(millis / 200.0) * 1.5);
            sprite(g, x + size - 6, y - 3 + bob, 12, 64, 5, 9);
        }
        if (hover) {
            g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, size * 3, v, size, size, TEX_W, TEX_H);
        }
    }

    private List<Component> tooltip(GuideBook.Entry e) {
        GuideProgress.State st = GuideProgress.state(book, e);
        List<Component> lines = new ArrayList<>();
        if (st == GuideProgress.State.LOCKED) {
            lines.add(Component.translatable("guide.mistech.map.hidden").withStyle(ChatFormatting.DARK_GRAY));
            lines.add(Component.translatable("guide.mistech.map.locked").withStyle(ChatFormatting.RED));
        } else {
            lines.add(Component.translatable(e.name()).withStyle(ChatFormatting.LIGHT_PURPLE));
            if (st == GuideProgress.State.DONE) {
                lines.add(Component.translatable("guide.mistech.map.done").withStyle(ChatFormatting.GREEN));
            } else if (!GuideProgress.wasRead(e.id())) {
                lines.add(Component.translatable("guide.mistech.map.new").withStyle(ChatFormatting.GOLD));
            } else {
                lines.add(Component.translatable("guide.mistech.map.available").withStyle(ChatFormatting.GOLD));
            }
            if (st != GuideProgress.State.DONE) {
                lines.addAll(taskLines(e.task()));
            }
        }
        List<GuideBook.Entry> missing = new ArrayList<>();
        for (String dep : e.deps()) {
            GuideBook.Entry d = book.entry(dep);
            if (d != null && !GuideProgress.isDone(dep)) {
                missing.add(d);
            }
        }
        if (!missing.isEmpty() && st != GuideProgress.State.DONE) {
            lines.add(Component.translatable("guide.mistech.map.requires").withStyle(ChatFormatting.GRAY));
            for (GuideBook.Entry d : missing) {
                boolean depVisible = GuideProgress.state(book, d) != GuideProgress.State.LOCKED;
                MutableComponent name = depVisible ? Component.translatable(d.name()) : Component.translatable("guide.mistech.map.hidden");
                MutableComponent line = Component.literal(" ✘ ").withStyle(ChatFormatting.RED).append(name.withStyle(ChatFormatting.WHITE));
                int idx = book.categoryIndex(d.id());
                if (idx != chapter && idx >= 0 && GuideProgress.chapterOpen(book, book.categories.get(idx))) {
                    line.append(Component.literal(" (").withStyle(ChatFormatting.DARK_GRAY))
                            .append(Component.translatable(book.categories.get(idx).name()).withStyle(ChatFormatting.DARK_GRAY))
                            .append(Component.literal(")").withStyle(ChatFormatting.DARK_GRAY));
                }
                lines.add(line);
            }
        }
        return lines;
    }

    /** Linhas descrevendo a tarefa de uma entrada (usadas aqui e no livro). */
    static List<Component> taskLines(GuideBook.Task task) {
        List<Component> lines = new ArrayList<>();
        if (task.read()) {
            lines.add(Component.translatable("guide.mistech.map.task_read").withStyle(ChatFormatting.GRAY));
        } else if (task.items().size() == 1) {
            lines.add(Component.translatable("guide.mistech.map.task_item", GuideBook.stack(task.items().get(0)).getHoverName())
                    .withStyle(ChatFormatting.GRAY));
        } else {
            lines.add(Component.translatable("guide.mistech.map.task_any").withStyle(ChatFormatting.GRAY));
            int shown = 0;
            for (String id : task.items()) {
                if (shown++ >= 6) {
                    lines.add(Component.literal("  …").withStyle(ChatFormatting.DARK_GRAY));
                    break;
                }
                lines.add(Component.literal("  • ").withStyle(ChatFormatting.DARK_GRAY)
                        .append(GuideBook.stack(id).getHoverName().copy().withStyle(ChatFormatting.WHITE)));
            }
        }
        return lines;
    }

    private void bar(GuiGraphicsExtractor g, int x, int y, int w, int value, int max) {
        g.fill(x, y, x + w, y + 3, 0xFF2A2433);
        if (max > 0) {
            g.fill(x, y, x + w * value / max, y + 3, value == max ? LINE_DONE : 0xFFB080FF);
        }
    }

    private void sprite(GuiGraphicsExtractor g, int x, int y, int u, int v, int w, int h) {
        g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, (float) u, (float) v, w, h, TEX_W, TEX_H);
    }

    private String trim(String text, int width) {
        if (width <= 0) {
            return "";
        }
        if (this.font.width(text) <= width) {
            return text;
        }
        return this.font.plainSubstrByWidth(text, Math.max(0, width - this.font.width("…"))) + "…";
    }

    // ================================================================= abrir

    /** Abre o Grimório onde o jogador parou (no mapa, ou na entrada que estava lendo). */
    public static void open() {
        net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
        GuideMapScreen map = new GuideMapScreen();
        GuideProgress.prepare();
        if (openEntry != null) {
            GuideBook book = GuideBook.reload();
            GuideBook.Entry entry = book.entry(openEntry);
            if (entry != null && GuideProgress.state(book, entry) != GuideProgress.State.LOCKED) {
                int category = book.categoryIndex(entry.id());
                int index = book.categories.get(category).entries().indexOf(entry);
                minecraft.setScreen(new GuideBookScreen(map, category, index));
                return;
            }
            openEntry = null;
        }
        minecraft.setScreen(map);
    }
}

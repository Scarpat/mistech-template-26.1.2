package com.mistech.mistechmod.client;

import java.util.List;
import java.util.Random;

import org.joml.Matrix3x2fStack;

import com.mistech.mistechmod.astrology.AstrologyData;
import com.mistech.mistechmod.astrology.AstrologyService;
import com.mistech.mistechmod.astrology.Constellations;
import com.mistech.mistechmod.astrology.Constellations.Constellation;
import com.mistech.mistechmod.network.UnlockStarPayload;
import com.mistech.mistechmod.registry.MistechAttachments;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * Tela da Mesa de Astrologia: um céu noturno que dá para arrastar e dar zoom, com as 12 constelações do zodíaco.
 * Clique numa constelação para focar nela e numa estrela para ver o custo e desbloquear.
 */
public class AstrologyScreen extends Screen {
    private static final int PANEL_W = 156;
    private static final float MAX_ZOOM = 3.2F;
    private static final float FOCUS_ZOOM = 2.1F;
    private static final int BG_STARS = 3200;
    /** Zoom mínimo: não dá para ver o céu inteiro de uma vez — é preciso procurar as constelações. */
    private static final float MIN_ZOOM_LIMIT = 0.3F;

    // cores (ARGB)
    private static final int GOLD = 0xFFF2CE6B;
    private static final int GOLD_DIM = 0xFFB8963F;
    private static final int TEXT = 0xFFE6E9FF;
    private static final int TEXT_DIM = 0xFF8C93BF;
    private static final int GREEN = 0xFF7CFC6A;
    private static final int RED = 0xFFFF6464;

    private final BlockPos tablePos;

    // câmera (coordenadas do céu) e zoom
    private float camX = Constellations.SKY_WIDTH / 2.0F;
    private float camY = Constellations.SKY_HEIGHT / 2.0F;
    private float zoom = 0.9F;
    private float minZoom = 0.5F;
    private boolean focusing;
    private float targetX, targetY, targetZoom;
    private long lastFrame = System.nanoTime();
    private float time;

    // arrastar
    private boolean dragging;
    private boolean dragMoved;
    private double dragDistance;

    // seleção / mouse
    private int selected = -1;
    private int selectedStar = -1;
    private int hoveredConst = -1;
    private int hoveredStar = -1;

    // estrelas de fundo (fixas)
    private final float[] bgX = new float[BG_STARS];
    private final float[] bgY = new float[BG_STARS];
    private final float[] bgDepth = new float[BG_STARS];
    private final float[] bgSize = new float[BG_STARS];
    private final float[] bgPhase = new float[BG_STARS];
    private final int[] bgColor = new int[BG_STARS];

    private Button unlockButton;
    private Button backButton;
    private Button natalButton;

    public AstrologyScreen(BlockPos tablePos) {
        super(Component.translatable("screen.mistech.astrology"));
        this.tablePos = tablePos;
        Random random = new Random(0x5EED_A57L);
        float w = Constellations.SKY_WIDTH, h = Constellations.SKY_HEIGHT;
        for (int i = 0; i < BG_STARS; i++) {
            float x, y;
            if (i % 3 == 0) {
                // Via Láctea: uma faixa diagonal mais densa
                float t = random.nextFloat();
                x = -w * 0.4F + t * w * 1.8F;
                y = h * 1.1F - t * h * 1.2F + (float) random.nextGaussian() * 160.0F;
            } else {
                x = -w * 0.4F + random.nextFloat() * w * 1.8F;
                y = -h * 0.4F + random.nextFloat() * h * 1.8F;
            }
            bgX[i] = x;
            bgY[i] = y;
            bgDepth[i] = 0.45F + random.nextFloat() * 0.45F;
            bgSize[i] = random.nextFloat() < 0.08F ? 2.0F : (random.nextFloat() < 0.4F ? 1.4F : 1.0F);
            bgPhase[i] = random.nextFloat() * Mth.TWO_PI;
            int tone = random.nextInt(10);
            bgColor[i] = tone < 6 ? 0xFFFFFF : (tone < 8 ? 0xBFD4FF : (tone < 9 ? 0xFFE7B0 : 0xFFC0C0));
        }
    }

    @Override
    protected void init() {
        float viewW = this.width - PANEL_W;
        minZoom = Math.max(MIN_ZOOM_LIMIT,
                Math.min(viewW / Constellations.SKY_WIDTH, (float) this.height / Constellations.SKY_HEIGHT) * 0.95F);
        clampCamera();
        int px = this.width - PANEL_W + 8;
        this.unlockButton = this.addRenderableWidget(Button.builder(Component.translatable("screen.mistech.astrology.unlock"), b -> unlock())
                .bounds(px, this.height - 50, PANEL_W - 16, 20).build());
        this.backButton = this.addRenderableWidget(Button.builder(Component.literal("‹"), b -> deselect())
                .bounds(this.width - 26, 6, 20, 20).build());
        this.natalButton = this.addRenderableWidget(Button.builder(Component.translatable("screen.mistech.natal.open"),
                b -> this.minecraft.setScreen(new NatalChartScreen(this.tablePos)))
                .bounds(px, this.height - 28, PANEL_W - 16, 20).build());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        LocalPlayer player = this.minecraft.player;
        if (player == null || player.distanceToSqr(tablePos.getCenter()) > 64.0) {
            this.onClose();
        }
    }

    private AstrologyData data() {
        LocalPlayer player = this.minecraft.player;
        return player == null ? AstrologyData.EMPTY : player.getData(MistechAttachments.ASTROLOGY);
    }

    // ---------------------------------------------------------------- câmera

    private float viewCx() {
        return (this.width - PANEL_W) / 2.0F;
    }

    private float viewCy() {
        return this.height / 2.0F;
    }

    private float toScreenX(float wx) {
        return (wx - camX) * zoom + viewCx();
    }

    private float toScreenY(float wy) {
        return (wy - camY) * zoom + viewCy();
    }

    private void clampCamera() {
        camX = Mth.clamp(camX, 0.0F, Constellations.SKY_WIDTH);
        camY = Mth.clamp(camY, 0.0F, Constellations.SKY_HEIGHT);
        zoom = Mth.clamp(zoom, minZoom, MAX_ZOOM);
    }

    private float starX(Constellation c, int i) {
        return c.cx() + c.stars().get(i).dx();
    }

    private float starY(Constellation c, int i) {
        return c.cy() + c.stars().get(i).dy();
    }

    private void focusOn(int constellation) {
        Constellation c = Constellations.get(constellation);
        float sx = 0, sy = 0;
        for (int i = 0; i < c.stars().size(); i++) {
            sx += starX(c, i);
            sy += starY(c, i);
        }
        targetX = sx / c.stars().size();
        targetY = sy / c.stars().size();
        targetZoom = Math.max(zoom, Math.min(FOCUS_ZOOM, MAX_ZOOM));
        focusing = true;
    }

    private void select(int constellation, int star) {
        if (constellation != selected) {
            focusOn(constellation);
        }
        selected = constellation;
        selectedStar = star;
    }

    private void deselect() {
        if (selectedStar >= 0) {
            selectedStar = -1;
        } else {
            selected = -1;
            focusing = false;
        }
    }

    private void unlock() {
        if (selected >= 0 && selectedStar >= 0) {
            ClientPacketDistributor.sendToServer(new UnlockStarPayload(tablePos, selected, selectedStar));
        }
    }

    private float starRadius(Constellations.Star star) {
        return 2.2F * star.size() * Mth.clamp(zoom, 0.8F, 2.0F);
    }

    /** Atualiza qual constelação/estrela está sob o mouse. */
    private void updateHover(double mouseX, double mouseY) {
        hoveredConst = -1;
        hoveredStar = -1;
        if (mouseX >= this.width - PANEL_W || dragging && dragMoved) {
            return;
        }
        double best = Double.MAX_VALUE;
        for (int ci = 0; ci < Constellations.ALL.size(); ci++) {
            Constellation c = Constellations.get(ci);
            float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
            for (int si = 0; si < c.stars().size(); si++) {
                float x = toScreenX(starX(c, si)), y = toScreenY(starY(c, si));
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
                double dist = Math.hypot(mouseX - x, mouseY - y);
                double reach = Math.max(6.0, starRadius(c.stars().get(si)) + 4.0);
                if (dist <= reach && dist < best) {
                    best = dist;
                    hoveredConst = ci;
                    hoveredStar = si;
                }
            }
            float pad = 14.0F * Mth.clamp(zoom, 0.6F, 1.5F);
            if (hoveredStar < 0 && hoveredConst < 0
                    && mouseX >= minX - pad && mouseX <= maxX + pad && mouseY >= minY - pad && mouseY <= maxY + pad) {
                hoveredConst = ci;
            }
        }
    }

    // ---------------------------------------------------------------- entrada

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        if (event.x() >= this.width - PANEL_W) {
            return clickPanel(event.x(), event.y());
        }
        if (event.button() == 1) {
            deselect();
            return true;
        }
        if (event.button() == 0) {
            dragging = true;
            dragMoved = false;
            dragDistance = 0;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (dragging && event.button() == 0) {
            dragDistance += Math.abs(dx) + Math.abs(dy);
            if (dragDistance > 3.0) {
                dragMoved = true;
                focusing = false;
            }
            if (dragMoved) {
                camX -= (float) (dx / zoom);
                camY -= (float) (dy / zoom);
                clampCamera();
            }
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragging && event.button() == 0) {
            dragging = false;
            if (!dragMoved) {
                updateHover(event.x(), event.y());
                if (hoveredConst >= 0) {
                    select(hoveredConst, hoveredStar);
                }
            }
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (x >= this.width - PANEL_W || scrollY == 0) {
            return false;
        }
        focusing = false;
        float worldX = camX + (float) ((x - viewCx()) / zoom);
        float worldY = camY + (float) ((y - viewCy()) / zoom);
        zoom = Mth.clamp(zoom * (scrollY > 0 ? 1.15F : 1.0F / 1.15F), minZoom, MAX_ZOOM);
        camX = worldX - (float) ((x - viewCx()) / zoom);
        camY = worldY - (float) ((y - viewCy()) / zoom);
        clampCamera();
        return true;
    }

    /** Lista de constelações do painel (quando nada está selecionado). */
    private boolean clickPanel(double x, double y) {
        if (selected >= 0) {
            return true;
        }
        int top = listTop() - 2;
        int row = (int) Math.floor((y - top) / 12.0);
        if (y >= top && row >= 0 && row < Constellations.ALL.size() && data().countIn(row) > 0) {
            select(row, -1);
        }
        return true;
    }

    /** Onde começa a lista do painel (calculado ao desenhar, depende de quantas linhas o texto de ajuda ocupa). */
    private int listTopY = 68;

    private int listTop() {
        return listTopY;
    }

    // ---------------------------------------------------------------- desenho

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        long now = System.nanoTime();
        float dt = Math.min(0.1F, (now - lastFrame) / 1.0E9F);
        lastFrame = now;
        time += dt;
        if (focusing) {
            float k = 1.0F - (float) Math.exp(-dt * 7.0);
            camX += (targetX - camX) * k;
            camY += (targetY - camY) * k;
            zoom += (targetZoom - zoom) * k;
            if (Math.abs(targetX - camX) < 0.5F && Math.abs(targetY - camY) < 0.5F && Math.abs(targetZoom - zoom) < 0.005F) {
                focusing = false;
            }
            clampCamera();
        }
        updateHover(mouseX, mouseY);

        // céu
        graphics.fillGradient(0, 0, this.width, this.height, 0xFF03051A, 0xFF1B0F38);
        extractBackgroundStars(graphics);

        AstrologyData data = data();
        for (int ci = 0; ci < Constellations.ALL.size(); ci++) {
            extractConstellation(graphics, data, ci);
        }

        // cabeçalho
        graphics.text(this.font, this.title, 8, 8, GOLD, true);
        graphics.text(this.font, Component.translatable("screen.mistech.astrology.hint"), 8, 20, TEXT_DIM, true);

        if (hoveredConst >= 0 && hoveredStar >= 0) {
            Constellation c = Constellations.get(hoveredConst);
            graphics.setTooltipForNextFrame(this.font, c.starName(hoveredStar), mouseX, mouseY);
        }
    }

    private void extractBackgroundStars(GuiGraphicsExtractor graphics) {
        float cx = viewCx(), cy = viewCy();
        float midX = Constellations.SKY_WIDTH / 2.0F, midY = Constellations.SKY_HEIGHT / 2.0F;
        float sizeScale = Mth.clamp(zoom, 0.8F, 1.6F);
        int maxX = this.width - PANEL_W;
        for (int i = 0; i < BG_STARS; i++) {
            float d = bgDepth[i];
            // paralaxe: estrelas "mais longe" andam menos
            float x = ((bgX[i] - midX) - (camX - midX) * d) * zoom * d + cx;
            float y = ((bgY[i] - midY) - (camY - midY) * d) * zoom * d + cy;
            if (x < -2 || y < -2 || x > maxX + 2 || y > this.height + 2) {
                continue;
            }
            float twinkle = 0.55F + 0.45F * Mth.sin(time * (1.2F + d * 2.0F) + bgPhase[i]);
            int alpha = (int) (255 * d * twinkle);
            float s = bgSize[i] * sizeScale;
            rect(graphics, x - s / 2, y - s / 2, s, s, (alpha << 24) | bgColor[i]);
        }
    }

    private void extractConstellation(GuiGraphicsExtractor graphics, AstrologyData data, int ci) {
        Constellation c = Constellations.get(ci);
        List<Constellations.Star> stars = c.stars();
        boolean isSelected = ci == selected;
        boolean isHovered = ci == hoveredConst;
        boolean complete = data.countIn(ci) == stars.size();

        // linhas
        for (int si = 0; si < stars.size(); si++) {
            int parent = stars.get(si).parent();
            if (parent < 0) {
                continue;
            }
            boolean lit = data.isUnlocked(ci, si) && data.isUnlocked(ci, parent);
            int color;
            if (lit) {
                color = isSelected || isHovered ? 0xE0F2CE6B : 0xA0D8B060;
            } else {
                color = isSelected || isHovered ? 0x90A0B8FF : 0x506070B0;
            }
            float thick = (lit ? 1.6F : 1.0F) * Mth.clamp(zoom, 0.8F, 1.5F);
            line(graphics, toScreenX(starX(c, parent)), toScreenY(starY(c, parent)),
                    toScreenX(starX(c, si)), toScreenY(starY(c, si)), thick, color);
        }

        for (Constellations.Link link : c.links()) {
            boolean lit = data.isUnlocked(ci, link.a()) && data.isUnlocked(ci, link.b());
            int color = lit ? (isSelected || isHovered ? 0xE0F2CE6B : 0xA0D8B060) : (isSelected || isHovered ? 0x90A0B8FF : 0x506070B0);
            float thick = (lit ? 1.6F : 1.0F) * Mth.clamp(zoom, 0.8F, 1.5F);
            line(graphics, toScreenX(starX(c, link.a())), toScreenY(starY(c, link.a())),
                    toScreenX(starX(c, link.b())), toScreenY(starY(c, link.b())), thick, color);
        }

        // estrelas
        float lowestY = -Float.MAX_VALUE;
        float sumX = 0;
        for (int si = 0; si < stars.size(); si++) {
            Constellations.Star star = stars.get(si);
            float x = toScreenX(starX(c, si)), y = toScreenY(starY(c, si));
            sumX += x;
            lowestY = Math.max(lowestY, y);
            float r = starRadius(star);
            boolean unlocked = data.isUnlocked(ci, si);
            boolean available = data.isAvailable(ci, si);
            boolean starHovered = isHovered && si == hoveredStar;

            if (unlocked) {
                float pulse = 0.8F + 0.2F * Mth.sin(time * 2.0F + si);
                diamond(graphics, x, y, r * 4.2F * pulse, 0x30FFD070);
                diamond(graphics, x, y, r * 2.6F, 0x60FFD890);
                rect(graphics, x - r * 2.2F, y - 0.5F, r * 4.4F, 1.0F, 0x90FFF0C0);
                rect(graphics, x - 0.5F, y - r * 2.2F, 1.0F, r * 4.4F, 0x90FFF0C0);
                diamond(graphics, x, y, r * 1.5F, 0xFFFFF4C8);
            } else if (available) {
                float pulse = 0.5F + 0.5F * Mth.sin(time * 4.0F + si);
                int glowAlpha = (int) (0x20 + 0x50 * pulse);
                diamond(graphics, x, y, r * (3.0F + pulse * 1.5F), (glowAlpha << 24) | 0x9FD8FF);
                diamond(graphics, x, y, r * 1.3F, 0xFFD8F0FF);
            } else {
                diamond(graphics, x, y, r * 1.1F, isSelected || isHovered ? 0xFF7C86A8 : 0xFF4E5670);
            }

            if (starHovered) {
                diamond(graphics, x, y, r * 3.6F, 0x30FFFFFF);
            }
            if (isSelected && si == selectedStar) {
                // anel girando em volta da estrela selecionada
                float ring = r * 2.6F + 3.0F;
                for (int k = 0; k < 4; k++) {
                    float ang = time * 1.5F + k * Mth.HALF_PI;
                    diamond(graphics, x + Mth.cos(ang) * ring, y + Mth.sin(ang) * ring, 2.2F, 0xFFF2CE6B);
                }
            }
        }

        // nome
        boolean discovered = data.countIn(ci) > 0;
        if (!discovered && !isHovered && !isSelected) {
            return;
        }
        int nameColor = complete ? GOLD : (isSelected ? 0xFFFFFFFF : (isHovered ? 0xFFD8DEFF : 0xB08C93BF));
        int textY = (int) (lowestY + 8 + 6 * Mth.clamp(zoom, 0.6F, 2.0F));
        graphics.centeredText(this.font, c.name(), (int) (sumX / stars.size()), textY, nameColor);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        extractPanel(graphics, mouseX, mouseY);
        super.extractRenderState(graphics, mouseX, mouseY, a);
    }

    private void extractPanel(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int x0 = this.width - PANEL_W;
        graphics.fill(x0, 0, this.width, this.height, 0xE00B0D22);
        graphics.fill(x0, 0, x0 + 1, this.height, 0xFF8A6D2E);
        graphics.fill(x0 + 2, 0, x0 + 3, this.height, 0x608A6D2E);
        int x = x0 + 8;
        int w = PANEL_W - 16;
        AstrologyData data = data();
        LocalPlayer player = this.minecraft.player;

        unlockButton.visible = false;
        backButton.visible = selected >= 0;
        natalButton.visible = selected < 0;

        if (selected < 0) {
            // visão geral: lista das constelações
            int total = 0, unlocked = 0;
            for (int ci = 0; ci < Constellations.ALL.size(); ci++) {
                total += Constellations.get(ci).stars().size();
                unlocked += data.countIn(ci);
            }
            graphics.text(this.font, this.title, x, 10, GOLD, true);
            graphics.text(this.font, Component.translatable("screen.mistech.astrology.total", unlocked, total), x, 24, TEXT_DIM, false);
            graphics.fill(x, 38, x + w, 39, 0x608A6D2E);
            Component hint = Component.translatable("screen.mistech.astrology.select");
            graphics.textWithWordWrap(this.font, hint, x, 43, w, TEXT_DIM, false);
            int hintBottom = 43 + this.font.split(hint, w).size() * 9;
            graphics.fill(x, hintBottom + 4, x + w, hintBottom + 5, 0x608A6D2E);
            listTopY = hintBottom + 11;
            int top = listTop();
            for (int ci = 0; ci < Constellations.ALL.size(); ci++) {
                Constellation c = Constellations.get(ci);
                int y = top + ci * 12;
                int count = data.countIn(ci);
                boolean discovered = count > 0;
                boolean hover = discovered && mouseX >= x0 && mouseY >= y - 2 && mouseY < y + 10;
                if (hover) {
                    graphics.fill(x0 + 3, y - 2, this.width, y + 10, 0x30FFFFFF);
                }
                boolean complete = count == c.stars().size();
                graphics.text(this.font, discovered ? c.name() : Component.literal("???"), x, y,
                        complete ? GOLD : (hover ? 0xFFFFFFFF : (discovered ? TEXT : TEXT_DIM)), false);
                String progress = count + "/" + c.stars().size();
                graphics.text(this.font, progress, x + w - this.font.width(progress), y, complete ? GOLD : TEXT_DIM, false);
            }
            // botão do Mapa Astral logo abaixo da lista (ou no rodapé, o que vier primeiro)
            natalButton.setY(Math.min(this.height - 26, top + Constellations.ALL.size() * 12 + 6));
            return;
        }

        Constellation c = Constellations.get(selected);
        Constellations.Perk perk = c.perk();
        graphics.text(this.font, c.name(), x, 10, GOLD, true);
        graphics.text(this.font, Component.translatable("screen.mistech.astrology.progress", data.countIn(selected), c.stars().size()),
                x, 22, TEXT_DIM, false);
        int y = 36;
        graphics.fill(x, y, x + w, y + 1, 0x608A6D2E);
        y += 5;
        Component perStar = Component.translatable("screen.mistech.astrology.per_star",
                Component.translatable("astrology.mistech.perk." + c.id(), perk.format(perk.perStar())));
        graphics.textWithWordWrap(this.font, perStar, x, y, w, TEXT, false);
        y += this.font.split(perStar, w).size() * 9 + 2;
        int pillars = this.minecraft.player.getData(com.mistech.mistechmod.registry.MistechAttachments.ASTRAL_LINK);
        double totalBonus = com.mistech.mistechmod.astrology.AstrologyPerks.totalBonus(data, selected, pillars);
        if (pillars > 0) {
            Component link = Component.translatable("screen.mistech.astrology.astral_link", pillars,
                    String.format(java.util.Locale.ROOT, "%.1f", pillars * com.mistech.mistechmod.astrology.AstrologyPerks.PILLAR_BONUS * 100));
            graphics.textWithWordWrap(this.font, link, x, y, w, 0xFFB98CF0, false);
            y += this.font.split(link, w).size() * 9 + 2;
        }
        Component current = Component.translatable("screen.mistech.astrology.bonus",
                Component.translatable("astrology.mistech.perk." + c.id(), perk.format(totalBonus)));
        graphics.textWithWordWrap(this.font, current, x, y, w, totalBonus > 0 ? GOLD_DIM : TEXT_DIM, false);
        y += this.font.split(current, w).size() * 9 + 6;
        graphics.fill(x, y, x + w, y + 1, 0x608A6D2E);
        y += 6;

        if (selectedStar < 0) {
            graphics.textWithWordWrap(this.font, Component.translatable("screen.mistech.astrology.pick_star"), x, y, w, TEXT_DIM, false);
            return;
        }

        // estrela selecionada
        graphics.text(this.font, c.starName(selectedStar), x, y, 0xFFFFFFFF, true);
        y += 13;
        boolean unlocked = data.isUnlocked(selected, selectedStar);
        boolean available = data.isAvailable(selected, selectedStar);
        Component status = Component.translatable(unlocked ? "screen.mistech.astrology.unlocked"
                : available ? "screen.mistech.astrology.available" : "screen.mistech.astrology.locked");
        graphics.textWithWordWrap(this.font, status, x, y, w, unlocked ? GOLD : (available ? 0xFF9FD8FF : TEXT_DIM), false);
        y += this.font.split(status, w).size() * 9 + 6;

        if (!unlocked) {
            graphics.text(this.font, Component.translatable("screen.mistech.astrology.cost"), x, y, TEXT, false);
            y += 12;
            int xp = com.mistech.mistechmod.astrology.NatalService.xpCost(player, selected, selectedStar);
            boolean creative = player != null && player.hasInfiniteMaterials();
            boolean starFragment = player != null && AstrologyService.hasStarFragment(player);
            boolean collector = !creative && !starFragment && player != null && AstrologyService.collectorPays(player, xp, this.tablePos);
            boolean hasXp = creative || starFragment || collector || player != null && player.experienceLevel >= xp;
            // bolinha verde de XP
            diamond(graphics, x + 4, y + 3.5F, 5.0F, 0xFF7CFC00);
            diamond(graphics, x + 4, y + 3.5F, 2.5F, 0xFFE8FFB0);
            graphics.text(this.font, starFragment ? Component.translatable("screen.mistech.astrology.xp_star_fragment")
                    : collector ? Component.translatable("screen.mistech.astrology.xp_collector", xp, AstrologyService.pointsForLevels(xp))
                    : Component.translatable("screen.mistech.astrology.xp", xp), x + 12, y, hasXp ? GREEN : RED, false);
            y += 14;
            for (Constellations.Cost cost : c.itemCost(selectedStar)) {
                ItemStack stack = cost.stack();
                int have = player == null ? 0 : AstrologyService.countItem(player, cost.item().get());
                graphics.item(stack, x, y - 4);
                String amount = Math.min(have, 999) + "/" + cost.count();
                graphics.text(this.font, amount, x + 20, y, creative || have >= cost.count() ? GREEN : RED, false);
                if (mouseX >= x && mouseX < x + 16 && mouseY >= y - 4 && mouseY < y + 12) {
                    graphics.setTooltipForNextFrame(this.font, stack, mouseX, mouseY);
                }
                y += 18;
            }
            unlockButton.visible = true;
            unlockButton.active = available && player != null && AstrologyService.canAfford(player, c, selectedStar, this.tablePos);
        }
    }

    // ---------------------------------------------------------------- formas

    /** Retângulo com posição/tamanho fracionados (fica suave ao mover/zoom). */
    private static void rect(GuiGraphicsExtractor graphics, float x, float y, float w, float h, int color) {
        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(w, h);
        graphics.fill(0, 0, 1, 1, color);
        pose.popMatrix();
    }

    /** Losango (quadrado girado 45°) centrado em (x, y). */
    private static void diamond(GuiGraphicsExtractor graphics, float x, float y, float size, int color) {
        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.rotate(Mth.PI / 4.0F);
        pose.translate(-size / 2.0F, -size / 2.0F);
        pose.scale(size, size);
        graphics.fill(0, 0, 1, 1, color);
        pose.popMatrix();
    }

    private static void line(GuiGraphicsExtractor graphics, float x0, float y0, float x1, float y1, float thickness, int color) {
        float length = (float) Math.hypot(x1 - x0, y1 - y0);
        if (length < 0.5F) {
            return;
        }
        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(x0, y0);
        pose.rotate((float) Math.atan2(y1 - y0, x1 - x0));
        pose.translate(0.0F, -thickness / 2.0F);
        pose.scale(length, thickness);
        graphics.fill(0, 0, 1, 1, color);
        pose.popMatrix();
    }
}

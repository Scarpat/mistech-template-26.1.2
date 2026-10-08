package com.mistech.mistechmod.client;

import org.joml.Matrix3x2fStack;

import com.mistech.mistechmod.astrology.AstrologyService;
import com.mistech.mistechmod.astrology.Constellations;
import com.mistech.mistechmod.astrology.NatalChart;
import com.mistech.mistechmod.astrology.NatalChart.Part;
import com.mistech.mistechmod.astrology.NatalService;
import com.mistech.mistechmod.network.NatalActionPayload;
import com.mistech.mistechmod.registry.MistechAttachments;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * Mapa Astral (aberto pela Mesa de Astrologia): a roda do zodíaco com o Sol, a Lua e o Ascendente do jogador.
 * Cada parte começa escondida e é revelada em ordem, na hora certa (Sol de dia, Lua à noite, Ascendente no amanhecer).
 * O cliente só conhece o que já foi revelado (o servidor esconde o resto).
 */
public class NatalChartScreen extends Screen {
    private static final int GOLD = 0xFFF2CE6B;
    private static final int GOLD_DIM = 0xFFB8963F;
    private static final int TEXT = 0xFFE6E9FF;
    private static final int TEXT_DIM = 0xFF8C93BF;
    private static final int GREEN = 0xFF7CFC6A;
    private static final int RED = 0xFFFF6464;
    private static final int ROW_H = 46;
    private static final String[] PART_GLYPH = {"☉", "☽", "↑"};

    private final BlockPos tablePos;
    private final Button[] revealButtons = new Button[3];
    private Button copyButton;
    private float time;
    private long lastFrame = System.nanoTime();

    public NatalChartScreen(BlockPos tablePos) {
        super(Component.translatable("screen.mistech.natal"));
        this.tablePos = tablePos;
    }

    private int panelX() {
        return Math.max(this.width / 2 + 10, this.width - 230);
    }

    private int panelW() {
        return this.width - 12 - panelX();
    }

    private int rowsTop() {
        return Math.max(34, this.height / 2 - (ROW_H * 3) / 2 - 10);
    }

    @Override
    protected void init() {
        int px = panelX();
        int pw = panelW();
        for (int i = 0; i < 3; i++) {
            int index = i;
            this.revealButtons[i] = this.addRenderableWidget(Button.builder(Component.translatable("screen.mistech.natal.reveal"),
                    b -> ClientPacketDistributor.sendToServer(new NatalActionPayload(this.tablePos, index)))
                    .bounds(px + pw - 62, rowsTop() + i * ROW_H + 24, 62, 16).build());
        }
        int bottom = rowsTop() + 3 * ROW_H + 6;
        this.copyButton = this.addRenderableWidget(Button.builder(Component.translatable("screen.mistech.natal.copy"),
                b -> ClientPacketDistributor.sendToServer(new NatalActionPayload(this.tablePos, NatalActionPayload.COPY)))
                .bounds(px, bottom, pw / 2 - 2, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("screen.mistech.natal.back"),
                b -> this.minecraft.setScreen(new AstrologyScreen(this.tablePos)))
                .bounds(px + pw / 2 + 2, bottom, pw - pw / 2 - 2, 20).build());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        LocalPlayer player = this.minecraft.player;
        if (player == null || player.distanceToSqr(this.tablePos.getCenter()) > 64.0) {
            this.onClose();
        }
    }

    private NatalChart chart() {
        LocalPlayer player = this.minecraft.player;
        return player == null ? NatalChart.UNROLLED : player.getData(MistechAttachments.NATAL_CHART);
    }

    // ---------------------------------------------------------------- desenho

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        graphics.fillGradient(0, 0, this.width, this.height, 0xFF05061A, 0xFF141A3D);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        long now = System.nanoTime();
        this.time += Math.min(0.1F, (now - this.lastFrame) / 1.0E9F);
        this.lastFrame = now;

        NatalChart chart = chart();
        extractWheel(graphics, chart);
        extractPanel(graphics, chart, mouseX, mouseY);
        super.extractRenderState(graphics, mouseX, mouseY, a);
    }

    private void extractWheel(GuiGraphicsExtractor graphics, NatalChart chart) {
        float cx = panelX() / 2.0F;
        float cy = this.height / 2.0F + 6;
        float r = Math.min(this.height * 0.38F, panelX() * 0.40F);

        graphics.centeredText(this.font, this.title, (int) cx, 10, GOLD);

        // anéis
        circle(graphics, cx, cy, r, 1.2F, 0xFF8A6D2E);
        circle(graphics, cx, cy, r * 0.72F, 1.0F, 0xA08A6D2E);
        circle(graphics, cx, cy, r * 0.30F, 1.0F, 0x608A6D2E);

        // divisões e signos (Áries à esquerda, como no mapa astral, girando no sentido anti-horário)
        for (int sign = 0; sign < 12; sign++) {
            float edge = angle(sign) - Mth.PI / 12.0F;
            line(graphics, cx + Mth.cos(edge) * r * 0.72F, cy + Mth.sin(edge) * r * 0.72F,
                    cx + Mth.cos(edge) * r, cy + Mth.sin(edge) * r, 1.0F, 0x808A6D2E);

            boolean mine = false;
            for (Part part : Part.values()) {
                mine |= chart.isRevealed(part) && chart.sign(part) == sign;
            }
            float mid = angle(sign);
            float gx = cx + Mth.cos(mid) * r * 0.86F;
            float gy = cy + Mth.sin(mid) * r * 0.86F;
            int color = mine ? (0xFF000000 | NatalChart.elementOf(sign).color()) : TEXT_DIM;
            if (mine) {
                float glow = 5.5F + Mth.sin(this.time * 2.0F + sign) * 0.8F;
                diamond(graphics, gx, gy, glow * 2.0F, 0x40FFFFFF);
            }
            String glyph = NatalChart.glyph(sign);
            graphics.text(this.font, glyph, (int) (gx - this.font.width(glyph) / 2.0F), (int) (gy - 4), color, mine);
        }

        // Sol, Lua e Ascendente dentro da roda, apontando para o signo
        for (Part part : Part.values()) {
            int i = part.ordinal();
            if (!chart.isRevealed(part) || chart.sign(part) < 0) {
                continue;
            }
            float ang = angle(chart.sign(part)) + (i - 1) * 0.12F;
            float mx = cx + Mth.cos(ang) * r * (0.58F - i * 0.1F);
            float my = cy + Mth.sin(ang) * r * (0.58F - i * 0.1F);
            line(graphics, cx, cy, mx, my, 1.0F, 0x70F2CE6B);
            graphics.text(this.font, PART_GLYPH[i], (int) (mx - this.font.width(PART_GLYPH[i]) / 2.0F), (int) (my - 4), GOLD, true);
        }

        // centro: "?" pulsando enquanto houver algo escondido
        if (chart.revealed() != 7) {
            int alpha = (int) (120 + 100 * Mth.sin(this.time * 1.6F));
            graphics.centeredText(this.font, Component.literal("?"), (int) cx, (int) cy - 4, (Mth.clamp(alpha, 20, 255) << 24) | 0xB98CF0);
        } else {
            diamond(graphics, cx, cy, 6.0F, GOLD);
        }
    }

    /** Ângulo do meio do signo: Áries começa à esquerda (leste do mapa) e segue no sentido anti-horário. */
    private static float angle(int sign) {
        return Mth.PI - sign * (Mth.TWO_PI / 12.0F) - Mth.PI / 12.0F;
    }

    private void extractPanel(GuiGraphicsExtractor graphics, NatalChart chart, int mouseX, int mouseY) {
        int x = panelX();
        int w = panelW();
        LocalPlayer player = this.minecraft.player;
        boolean creative = player != null && player.hasInfiniteMaterials();
        graphics.fill(x - 6, rowsTop() - 20, x + w + 6, rowsTop() + 3 * ROW_H + 32, 0xC00B0D22);
        graphics.fill(x - 6, rowsTop() - 20, x - 5, rowsTop() + 3 * ROW_H + 32, 0xFF8A6D2E);
        graphics.text(this.font, Component.translatable("screen.mistech.natal.hint"), x, rowsTop() - 14, TEXT_DIM, false);

        for (Part part : Part.values()) {
            int i = part.ordinal();
            int y = rowsTop() + i * ROW_H;
            Button button = this.revealButtons[i];
            button.visible = false;
            graphics.fill(x, y - 3, x + w, y - 2, 0x408A6D2E);

            Component title = Component.literal(PART_GLYPH[i] + " ").append(part.displayName());
            graphics.text(this.font, title, x, y + 2, GOLD, true);
            int sign = chart.sign(part);
            if (chart.isRevealed(part) && sign >= 0) {
                Component name = Component.literal(NatalChart.glyph(sign) + " ").append(NatalChart.signName(sign));
                int color = 0xFF000000 | NatalChart.elementOf(sign).color();
                graphics.text(this.font, name, x + w - this.font.width(name), y + 2, color, true);
                Component element = Component.translatable("screen.mistech.natal.element",
                        Component.translatable(NatalChart.elementOf(sign).translationKey()));
                graphics.text(this.font, element, x, y + 14, TEXT_DIM, false);
                extractBonus(graphics, chart, part, sign, x, y + 26, w);
                continue;
            }

            graphics.text(this.font, "???", x + w - this.font.width("???"), y + 2, TEXT_DIM, false);
            boolean previous = NatalService.previousRevealed(chart, part);
            if (!previous) {
                graphics.text(this.font, Component.translatable("screen.mistech.natal.locked"), x, y + 14, TEXT_DIM, false);
                continue;
            }
            boolean rightTime = player != null && NatalService.rightTime(player.level(), part);
            graphics.text(this.font, Component.translatable("screen.mistech.natal.when." + part.id()), x, y + 14,
                    rightTime ? GREEN : RED, false);

            // custo
            NatalService.RevealCost cost = NatalService.cost(part);
            int cxp = x;
            boolean hasXp = creative || player != null && player.experienceLevel >= cost.xp();
            Component xp = Component.translatable("screen.mistech.astrology.xp", cost.xp());
            graphics.text(this.font, xp, cxp, y + 28, hasXp ? GREEN : RED, false);
            cxp += this.font.width(xp) + 6;
            for (Constellations.Cost item : cost.items()) {
                ItemStack stack = item.stack();
                int have = player == null ? 0 : AstrologyService.countItem(player, item.item().get());
                graphics.item(stack, cxp, y + 24);
                graphics.text(this.font, String.valueOf(item.count()), cxp + 16, y + 28,
                        creative || have >= item.count() ? GREEN : RED, false);
                if (mouseX >= cxp && mouseX < cxp + 16 && mouseY >= y + 24 && mouseY < y + 40) {
                    graphics.setTooltipForNextFrame(this.font, stack, mouseX, mouseY);
                }
                cxp += 16 + this.font.width(String.valueOf(item.count())) + 6;
            }
            button.visible = true;
            button.active = rightTime && player != null && NatalService.canAfford(player, part);
        }

        this.copyButton.active = chart.revealed() != 0;
    }

    private void extractBonus(GuiGraphicsExtractor graphics, NatalChart chart, Part part, int sign, int x, int y, int w) {
        Constellations.Constellation c = Constellations.get(sign);
        double value = NatalService.constellationTotal(sign) * switch (part) {
            case SUN -> NatalService.SUN_SHARE;
            case MOON -> NatalService.MOON_SHARE;
            case RISING -> NatalService.RISING_SHARE;
        };
        double boost = this.minecraft.player == null ? 1.0 : NatalService.gearBoost(this.minecraft.player, part);
        value *= boost;
        Component bonus = Component.translatable("astrology.mistech.perk." + c.id(), c.perk().format(value));
        if (part == Part.MOON) {
            bonus = Component.translatable("screen.mistech.natal.at_night", bonus);
        }
        boolean active = this.minecraft.player != null && NatalService.bonus(this.minecraft.player.level(), chart, part, sign) != 0.0;
        if (boost > 1.0) {
            bonus = Component.empty().append(bonus).append(Component.literal(String.format(java.util.Locale.ROOT, " ✦×%.2f", boost))
                    .withColor(0xFFFFB04A));
        }
        graphics.text(this.font, bonus, x, y, active ? GOLD_DIM : TEXT_DIM, false);
    }

    // ---------------------------------------------------------------- formas

    private static void circle(GuiGraphicsExtractor graphics, float cx, float cy, float r, float thickness, int color) {
        int segments = Math.max(24, (int) (r * 0.8F));
        for (int i = 0; i < segments; i++) {
            float a0 = Mth.TWO_PI * i / segments;
            float a1 = Mth.TWO_PI * (i + 1) / segments;
            line(graphics, cx + Mth.cos(a0) * r, cy + Mth.sin(a0) * r, cx + Mth.cos(a1) * r, cy + Mth.sin(a1) * r, thickness, color);
        }
    }

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

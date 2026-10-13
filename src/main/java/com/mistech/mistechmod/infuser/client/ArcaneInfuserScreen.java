package com.mistech.mistechmod.infuser.client;

import static com.mistech.mistechmod.infuser.ArcaneInfuserMenu.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.client.MysticContainerScreen;
import com.mistech.mistechmod.infuser.ArcaneInfuserBlockEntity;
import com.mistech.mistechmod.infuser.ArcaneInfuserMenu;
import com.mistech.mistechmod.infuser.InfuserRules;
import com.mistech.mistechmod.infuser.InfuserRules.Cost;
import com.mistech.mistechmod.infuser.InfuserRules.Option;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Tela do Infusor Arcano: lista dos encantos que o item aceita (rola com a roda), − / + escolhem o nível e "Infundir"
 * manda o botão (encanto × 8 + nível). O custo aparece embaixo; o que falta fica em vermelho.
 */
public class ArcaneInfuserScreen extends MysticContainerScreen<ArcaneInfuserMenu> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Mistech.MODID, "textures/gui/container/arcane_infuser.png");
    private static final int LIST_X = 60, LIST_Y = 18, LIST_W = 110, ROW_H = 12, ROWS = 5;
    private static final int CTRL_Y = 82, MINUS_X = 60, PLUS_X = 96, BTN_W = 12, INFUSE_X = 112, INFUSE_W = 58, BTN_H = 12;
    private static final int COST_Y = 100;

    private List<Option> options = List.of();
    private ItemStack cachedStack = ItemStack.EMPTY;
    private int cachedTier = -1;
    private int scroll;
    private int selected = -1;
    private int level = 1;

    public ArcaneInfuserScreen(ArcaneInfuserMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, IMAGE_WIDTH, IMAGE_HEIGHT);
        this.inventoryLabelX = INV_X;
        this.inventoryLabelY = INV_Y - 11;
    }

    private void refresh() {
        ItemStack stack = this.menu.target();
        int tier = this.menu.tier();
        if (tier == this.cachedTier && ItemStack.isSameItemSameComponents(stack, this.cachedStack) && stack.getCount() == this.cachedStack.getCount()) {
            return;
        }
        int keepId = this.selectedOption() != null ? this.selectedOption().rawId() : -1;
        this.cachedStack = stack.copy();
        this.cachedTier = tier;
        if (this.minecraft == null || this.minecraft.level == null) {
            this.options = List.of();
            return;
        }
        var registry = this.minecraft.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        this.options = InfuserRules.options(registry, tier, stack);
        this.selected = -1;
        for (int i = 0; i < this.options.size(); i++) {
            if (this.options.get(i).rawId() == keepId) {
                this.selected = i;
            }
        }
        this.scroll = Math.max(0, Math.min(this.scroll, this.options.size() - ROWS));
        this.clampLevel();
    }

    private Option selectedOption() {
        return this.selected >= 0 && this.selected < this.options.size() ? this.options.get(this.selected) : null;
    }

    private void clampLevel() {
        Option option = this.selectedOption();
        if (option == null) {
            return;
        }
        int min = option.current() + 1;
        this.level = Math.max(min, Math.min(option.max(), this.level));
    }

    private boolean canUpgrade(Option option) {
        return option.current() < option.max();
    }

    private int rowAt(double mouseX, double mouseY) {
        double x = mouseX - (this.leftPos + LIST_X), y = mouseY - (this.topPos + LIST_Y);
        if (x < 0 || y < 0 || x >= LIST_W || y >= ROWS * ROW_H) {
            return -1;
        }
        int row = (int) (y / ROW_H) + this.scroll;
        return row < this.options.size() ? row : -1;
    }

    private boolean in(double mouseX, double mouseY, int x, int y, int w, int h) {
        double rx = mouseX - (this.leftPos + x), ry = mouseY - (this.topPos + y);
        return rx >= 0 && ry >= 0 && rx < w && ry < h;
    }

    private static String shortFe(int fe) {
        return fe >= 1000 ? (fe % 1000 == 0 ? fe / 1000 + "k" : String.format("%.1fk", fe / 1000.0)) : Integer.toString(fe);
    }

    /** Linhas do custo, cada uma verde (tem) ou vermelha (falta). */
    private List<Component> costLines(Cost cost) {
        List<Component> lines = new ArrayList<>();
        var player = this.minecraft.player;
        boolean creative = player != null && player.getAbilities().instabuild;
        lines.add(line("gui.mistech.arcane_infuser.cost_xp", creative || (player != null && player.experienceLevel >= cost.xp()), cost.xp()));
        lines.add(line("gui.mistech.arcane_infuser.cost_fe", this.menu.energy() >= cost.energy(), shortFe(cost.energy())));
        lines.add(line("gui.mistech.arcane_infuser.cost_lapis", this.menu.count(InfuserRules.SLOT_LAPIS) >= cost.lapis(), cost.lapis()));
        lines.add(line("gui.mistech.arcane_infuser.cost_essence", this.menu.count(InfuserRules.SLOT_ESSENCE) >= cost.essence(), cost.essence()));
        if (cost.star() > 0) {
            lines.add(line("gui.mistech.arcane_infuser.cost_star", this.menu.count(InfuserRules.SLOT_STAR) >= cost.star(), cost.star()));
            lines.add(line("gui.mistech.arcane_infuser.cost_purified", this.menu.count(InfuserRules.SLOT_PURIFIED) >= cost.purified(), cost.purified()));
        }
        return lines;
    }

    private static Component line(String key, boolean ok, Object value) {
        return Component.translatable(key, value).withStyle(ok ? ChatFormatting.GREEN : ChatFormatting.RED);
    }

    private boolean affordable(Cost cost) {
        for (Component c : this.costLines(cost)) {
            if (c.getStyle().getColor() != null && c.getStyle().getColor().getValue() == ChatFormatting.RED.getColor()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        this.refresh();
        int x = this.leftPos, y = this.topPos;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
        this.extractMysticAmbient(graphics);

        // energia
        int bar = Math.round(BAR_H * Math.min(1.0F, this.menu.energy() / (float) ArcaneInfuserBlockEntity.CAPACITY));
        if (bar > 0) {
            graphics.fill(x + BAR_X, y + BAR_Y + BAR_H - bar, x + BAR_X + BAR_W, y + BAR_Y + BAR_H, 0xFFB06BF0);
            graphics.fill(x + BAR_X, y + BAR_Y + BAR_H - bar, x + BAR_X + 1, y + BAR_Y + BAR_H, 0xFFE0C0FF);
        }

        // lista
        int hovered = this.rowAt(mouseX, mouseY);
        for (int r = 0; r < ROWS; r++) {
            int i = r + this.scroll;
            if (i >= this.options.size()) {
                break;
            }
            int ry = y + LIST_Y + r * ROW_H;
            int color = i == this.selected ? 0x806B3FA0 : i == hovered ? 0x40FFFFFF : 0;
            if (color != 0) {
                graphics.fill(x + LIST_X, ry, x + LIST_X + LIST_W, ry + ROW_H, color);
            }
        }
        if (this.options.size() > ROWS) {
            int barX = x + LIST_X + LIST_W - 2, barTop = y + LIST_Y, barH = ROWS * ROW_H;
            int thumbH = Math.max(6, barH * ROWS / this.options.size());
            int thumbY = barTop + (barH - thumbH) * this.scroll / (this.options.size() - ROWS);
            graphics.fill(barX, barTop, barX + 2, barTop + barH, 0x40000000);
            graphics.fill(barX, thumbY, barX + 2, thumbY + thumbH, 0xFFB78CF0);
        }

        // botões
        Option option = this.selectedOption();
        boolean any = option != null && this.canUpgrade(option);
        button(graphics, x + MINUS_X, y + CTRL_Y, BTN_W, any && this.level > option.current() + 1, this.in(mouseX, mouseY, MINUS_X, CTRL_Y, BTN_W, BTN_H));
        button(graphics, x + PLUS_X, y + CTRL_Y, BTN_W, any && this.level < option.max(), this.in(mouseX, mouseY, PLUS_X, CTRL_Y, BTN_W, BTN_H));
        boolean ok = any && this.affordable(InfuserRules.cost(option.enchantment(), this.level));
        button(graphics, x + INFUSE_X, y + CTRL_Y, INFUSE_W, ok, this.in(mouseX, mouseY, INFUSE_X, CTRL_Y, INFUSE_W, BTN_H));
    }

    private static void button(GuiGraphicsExtractor graphics, int x, int y, int w, boolean active, boolean hover) {
        int face = !active ? 0xFF3A3046 : hover ? 0xFF8A5CC8 : 0xFF6A44A0;
        graphics.fill(x, y, x + w, y + BTN_H, 0xFF120C1A);
        graphics.fill(x + 1, y + 1, x + w - 1, y + BTN_H - 1, face);
        graphics.fill(x + 1, y + 1, x + w - 1, y + 2, active ? 0xFFB79AE8 : 0xFF4A4056);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        if (this.menu.target().isEmpty()) {
            graphics.text(this.font, Component.translatable("gui.mistech.arcane_infuser.put_item"), LIST_X + 3, LIST_Y + 3, 0xFF9A8AB0, false);
        } else if (this.options.isEmpty()) {
            graphics.text(this.font, Component.translatable("gui.mistech.arcane_infuser.nothing"), LIST_X + 3, LIST_Y + 3, 0xFF9A8AB0, false);
        }
        for (int r = 0; r < ROWS; r++) {
            int i = r + this.scroll;
            if (i >= this.options.size()) {
                break;
            }
            Option o = this.options.get(i);
            int ry = LIST_Y + r * ROW_H + 2;
            boolean full = !this.canUpgrade(o);
            Component name = o.enchantment().value().description();
            String lv = o.current() + "/" + o.max();
            int lvW = this.font.width(lv);
            String text = this.font.plainSubstrByWidth(name.getString(), LIST_W - lvW - 10);
            int color = full ? 0xFF7A6E8A : o.max() > o.enchantment().value().getMaxLevel() && o.current() >= o.enchantment().value().getMaxLevel()
                    ? 0xFFF0D070 : 0xFFE6DAF6;
            graphics.text(this.font, Component.literal(text), LIST_X + 3, ry, color, false);
            graphics.text(this.font, Component.literal(lv), LIST_X + LIST_W - 5 - lvW, ry, full ? 0xFF7A6E8A : 0xFFB79AE8, false);
        }

        Option option = this.selectedOption();
        center(graphics, "−", MINUS_X, BTN_W);
        center(graphics, "+", PLUS_X, BTN_W);
        String lvText = option == null ? "-" : roman(this.level);
        boolean over = option != null && this.level > option.enchantment().value().getMaxLevel();
        int lvW = this.font.width(lvText);
        graphics.text(this.font, Component.literal(lvText), MINUS_X + BTN_W + (PLUS_X - MINUS_X - BTN_W - lvW) / 2, CTRL_Y + 2,
                over ? 0xFFF0D070 : 0xFFE6DAF6, false);
        Component infuse = Component.translatable("gui.mistech.arcane_infuser.infuse");
        graphics.text(this.font, infuse, INFUSE_X + (INFUSE_W - this.font.width(infuse)) / 2, CTRL_Y + 2, 0xFFFFFFFF, false);

        if (option != null && this.canUpgrade(option)) {
            Cost cost = InfuserRules.cost(option.enchantment(), this.level);
            List<Component> lines = this.costLines(cost);
            MutableComponent summary = Component.empty();
            for (int i = 0; i < Math.min(2, lines.size()); i++) {
                if (i > 0) {
                    summary.append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY));
                }
                summary.append(lines.get(i));
            }
            graphics.text(this.font, summary, LIST_X, COST_Y, 0xFFFFFFFF, false);
        }
    }

    private void center(GuiGraphicsExtractor graphics, String s, int x, int w) {
        graphics.text(this.font, Component.literal(s), x + (w - this.font.width(s)) / 2, CTRL_Y + 2, 0xFFFFFFFF, false);
    }

    private static String roman(int n) {
        String[] r = {"0", "I", "II", "III", "IV", "V", "VI", "VII"};
        return n >= 0 && n < r.length ? r[n] : Integer.toString(n);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        Option option = this.selectedOption();
        if (this.in(mouseX, mouseY, BAR_X, BAR_Y, BAR_W, BAR_H)) {
            graphics.setTooltipForNextFrame(this.font, List.of(Component.translatable("gui.mistech.energy_tooltip", this.menu.energy(),
                    ArcaneInfuserBlockEntity.CAPACITY)), Optional.empty(), mouseX, mouseY);
        } else if (this.in(mouseX, mouseY, INFUSE_X, CTRL_Y, INFUSE_W, BTN_H) && option != null && this.canUpgrade(option)) {
            List<Component> lines = new ArrayList<>();
            lines.add(net.minecraft.world.item.enchantment.Enchantment.getFullname(option.enchantment(), this.level).copy()
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
            lines.addAll(this.costLines(InfuserRules.cost(option.enchantment(), this.level)));
            graphics.setTooltipForNextFrame(this.font, lines, Optional.empty(), mouseX, mouseY);
        } else {
            int row = this.rowAt(mouseX, mouseY);
            if (row >= 0) {
                Option o = this.options.get(row);
                List<Component> lines = new ArrayList<>();
                lines.add(o.enchantment().value().description());
                lines.add(Component.translatable("gui.mistech.arcane_infuser.levels", o.current(), o.max()).withStyle(ChatFormatting.GRAY));
                if (o.max() > o.enchantment().value().getMaxLevel()) {
                    lines.add(Component.translatable("gui.mistech.arcane_infuser.beyond").withStyle(ChatFormatting.GOLD));
                }
                graphics.setTooltipForNextFrame(this.font, lines, Optional.empty(), mouseX, mouseY);
            } else if (this.in(mouseX, mouseY, TARGET_X - 1, TARGET_Y - 1, 18, 18) && this.menu.target().isEmpty()) {
                graphics.setTooltipForNextFrame(this.font, List.of(Component.translatable("gui.mistech.arcane_infuser.slot_target")), Optional.empty(), mouseX, mouseY);
            } else if (this.in(mouseX, mouseY, LAPIS_X - 1, ROW1_Y - 1, 18, 18) && this.menu.count(InfuserRules.SLOT_LAPIS) == 0) {
                graphics.setTooltipForNextFrame(this.font, List.of(Component.translatable("gui.mistech.arcane_infuser.slot_lapis")), Optional.empty(), mouseX, mouseY);
            } else if (this.in(mouseX, mouseY, ESSENCE_X - 1, ROW1_Y - 1, 18, 18) && this.menu.count(InfuserRules.SLOT_ESSENCE) == 0) {
                graphics.setTooltipForNextFrame(this.font, List.of(Component.translatable("gui.mistech.arcane_infuser.slot_essence")), Optional.empty(), mouseX, mouseY);
            } else if (this.in(mouseX, mouseY, STAR_X - 1, ROW2_Y - 1, 18, 18) && this.menu.count(InfuserRules.SLOT_STAR) == 0) {
                graphics.setTooltipForNextFrame(this.font, List.of(Component.translatable("gui.mistech.arcane_infuser.slot_star")), Optional.empty(), mouseX, mouseY);
            } else if (this.in(mouseX, mouseY, PURIFIED_X - 1, ROW2_Y - 1, 18, 18) && this.menu.count(InfuserRules.SLOT_PURIFIED) == 0) {
                graphics.setTooltipForNextFrame(this.font, List.of(Component.translatable("gui.mistech.arcane_infuser.slot_purified")), Optional.empty(), mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            int row = this.rowAt(event.x(), event.y());
            if (row >= 0) {
                this.selected = row;
                Option o = this.options.get(row);
                this.level = o.current() + 1;
                this.clampLevel();
                this.click();
                return true;
            }
            Option option = this.selectedOption();
            if (option != null && this.canUpgrade(option)) {
                if (this.in(event.x(), event.y(), MINUS_X, CTRL_Y, BTN_W, BTN_H)) {
                    this.level--;
                    this.clampLevel();
                    this.click();
                    return true;
                }
                if (this.in(event.x(), event.y(), PLUS_X, CTRL_Y, BTN_W, BTN_H)) {
                    this.level++;
                    this.clampLevel();
                    this.click();
                    return true;
                }
                if (this.in(event.x(), event.y(), INFUSE_X, CTRL_Y, INFUSE_W, BTN_H)) {
                    if (this.minecraft != null && this.minecraft.gameMode != null) {
                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, option.rawId() * 8 + this.level);
                    }
                    this.click();
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void click() {
        if (this.minecraft != null) {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.options.size() > ROWS && scrollY != 0) {
            this.scroll = Math.max(0, Math.min(this.options.size() - ROWS, this.scroll - (int) Math.signum(scrollY)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
}

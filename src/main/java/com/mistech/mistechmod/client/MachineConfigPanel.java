package com.mistech.mistechmod.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mistech.mistechmod.machine.MachineBlockEntity;
import com.mistech.mistechmod.machine.MachineMenu;
import com.mistech.mistechmod.machine.MachineUpgrade;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Abas do lado direito das telas de máquina: Redstone (clique troca o modo), Lados (painel com as 6 faces e a
 * entrada/saída automática) e Melhorias (clique numa melhoria devolve 1 para o inventário).
 * Tudo vai para o servidor pelos botões de menu ({@link MachineBlockEntity#handleConfigButton}).
 */
public final class MachineConfigPanel {
    public static final int TAB = 22, GAP = 2, PANEL_W = 92;

    private enum Tab { REDSTONE, SIDES, UPGRADES }

    /** Painel aberto (lembrado entre telas). */
    private static Tab open;

    private final MachineMenu menu;
    private final GuiThemes.Theme theme;
    private final ItemStack redstoneIcon = new ItemStack(Items.REDSTONE);
    private final ItemStack torchIcon = new ItemStack(Items.REDSTONE_TORCH);
    private final ItemStack leverIcon = new ItemStack(Items.LEVER);
    private final ItemStack hopperIcon = new ItemStack(Items.HOPPER);
    private final ItemStack upgradeIcon;

    private int left, top;

    public MachineConfigPanel(MachineMenu menu, GuiThemes.Theme theme) {
        this.menu = menu;
        this.theme = theme;
        this.upgradeIcon = new ItemStack(MachineUpgrade.SPEED.item());
    }

    private List<Tab> tabs() {
        List<Tab> tabs = new ArrayList<>(3);
        tabs.add(Tab.REDSTONE);
        if (this.menu.hasItemFaces()) {
            tabs.add(Tab.SIDES);
        }
        if (this.menu.acceptsUpgrade(MachineUpgrade.SPEED) || this.menu.acceptsUpgrade(MachineUpgrade.RANGE)) {
            tabs.add(Tab.UPGRADES);
        }
        return tabs;
    }

    private int tabY(int index) {
        return this.top + 4 + index * (TAB + GAP);
    }

    private int panelX() {
        return this.left + TAB + 1;
    }

    private int panelH() {
        return open == Tab.SIDES ? 112 : 78;
    }

    /** Áreas ocupadas (para não contar como "clique fora" e para o JEI desviar). */
    public List<Rect2i> areas(int screenLeft, int screenTop, int imageWidth) {
        this.left = screenLeft + imageWidth;
        this.top = screenTop;
        List<Rect2i> list = new ArrayList<>();
        List<Tab> tabs = this.tabs();
        list.add(new Rect2i(this.left, this.top + 4, TAB, tabs.size() * (TAB + GAP)));
        if (open != null && tabs.contains(open) && open != Tab.REDSTONE) {
            list.add(new Rect2i(this.panelX(), this.top + 4, PANEL_W, this.panelH()));
        }
        return list;
    }

    public boolean isInside(double mx, double my, int screenLeft, int screenTop, int imageWidth) {
        for (Rect2i r : this.areas(screenLeft, screenTop, imageWidth)) {
            if (r.contains((int) mx, (int) my)) {
                return true;
            }
        }
        return false;
    }

    // ----- desenho -----

    public void draw(GuiGraphicsExtractor g, Font font, int screenLeft, int screenTop, int imageWidth, int mouseX, int mouseY) {
        this.areas(screenLeft, screenTop, imageWidth);
        List<Tab> tabs = this.tabs();
        if (open != null && !tabs.contains(open)) {
            open = null;
        }
        for (int i = 0; i < tabs.size(); i++) {
            Tab tab = tabs.get(i);
            int x = this.left, y = this.tabY(i);
            boolean hover = mouseX >= x && mouseX < x + TAB && mouseY >= y && mouseY < y + TAB;
            boolean active = tab == open;
            this.box(g, x - 1, y, TAB, TAB, hover || active);
            ItemStack icon = switch (tab) {
                case REDSTONE -> switch (this.menu.redstoneMode()) {
                    case IGNORE -> this.redstoneIcon;
                    case HIGH -> this.torchIcon;
                    case LOW -> this.leverIcon;
                };
                case SIDES -> this.hopperIcon;
                case UPGRADES -> this.upgradeIcon;
            };
            g.item(icon, x + 2, y + 3);
            if (tab == Tab.REDSTONE && this.menu.redstoneBlocked()) {
                g.fill(x + 14, y + 2, x + 19, y + 7, 0xFFE04040);
            }
        }
        if (open == Tab.SIDES) {
            this.drawSides(g, font, mouseX, mouseY);
        } else if (open == Tab.UPGRADES) {
            this.drawUpgrades(g, font, mouseX, mouseY);
        }
    }

    private void box(GuiGraphicsExtractor g, int x, int y, int w, int h, boolean highlight) {
        int border = highlight ? (0xFF000000 | (this.theme.glow() & 0xFFFFFF)) : 0xFF000000 | (this.theme.accent() & 0xFFFFFF);
        g.fill(x, y, x + w, y + h, border);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xF0000000 | (this.theme.well() & 0xFFFFFF));
    }

    // ----- lados -----

    /** Posição de cada face relativa à frente da máquina, no desenho em cruz. */
    private record FaceButton(String key, int dx, int dy) {}

    private static final FaceButton[] FACES = {
            new FaceButton("top", 1, 0), new FaceButton("left", 0, 1), new FaceButton("front", 1, 1),
            new FaceButton("right", 2, 1), new FaceButton("back", 3, 1), new FaceButton("bottom", 1, 2)};

    private Direction facing() {
        return this.menu.facing();
    }

    static Direction faceDirection(String key, Direction front) {
        return switch (key) {
            case "top" -> Direction.UP;
            case "bottom" -> Direction.DOWN;
            case "front" -> front;
            case "back" -> front.getOpposite();
            case "left" -> front.getClockWise();
            default -> front.getCounterClockWise();
        };
    }

    private int faceX(FaceButton f) {
        return this.panelX() + 8 + f.dx() * 19;
    }

    private int faceY(FaceButton f) {
        return this.top + 18 + f.dy() * 19;
    }

    private static int modeColor(MachineBlockEntity.SideMode mode) {
        return switch (mode) {
            case NONE -> 0xFF505050;
            case INPUT -> 0xFF3A78D8;
            case OUTPUT -> 0xFFE08A2A;
            case BOTH -> 0xFF4CAF50;
        };
    }

    private void drawSides(GuiGraphicsExtractor g, Font font, int mouseX, int mouseY) {
        int px = this.panelX(), py = this.top + 4;
        this.box(g, px, py, PANEL_W, this.panelH(), true);
        g.text(font, Component.translatable("gui.mistech.config.sides"), px + 6, py + 4, this.theme.label(), false);
        Direction front = this.facing();
        for (FaceButton f : FACES) {
            int x = this.faceX(f), y = this.faceY(f);
            MachineBlockEntity.SideMode mode = this.menu.side(faceDirection(f.key(), front));
            boolean hover = mouseX >= x && mouseX < x + 17 && mouseY >= y && mouseY < y + 17;
            g.fill(x, y, x + 17, y + 17, hover ? 0xFFFFFFFF : 0xFF101010);
            g.fill(x + 1, y + 1, x + 16, y + 16, modeColor(mode));
            String letter = Component.translatable("gui.mistech.config.face_short." + f.key()).getString();
            g.text(font, letter, x + 9 - font.width(letter) / 2, y + 5, 0xFFFFFFFF, true);
        }
        int cy = py + 78;
        this.toggle(g, font, px + 6, cy, this.menu.autoOutput(), Component.translatable("gui.mistech.config.auto_out"), mouseX, mouseY);
        this.toggle(g, font, px + 6, cy + 14, this.menu.autoInput(), Component.translatable("gui.mistech.config.auto_in"), mouseX, mouseY);
    }

    private void toggle(GuiGraphicsExtractor g, Font font, int x, int y, boolean on, Component label, int mouseX, int mouseY) {
        boolean hover = mouseX >= x && mouseX < x + PANEL_W - 12 && mouseY >= y && mouseY < y + 11;
        g.fill(x, y, x + 10, y + 10, hover ? 0xFFFFFFFF : 0xFF101010);
        g.fill(x + 1, y + 1, x + 9, y + 9, on ? 0xFF4CAF50 : 0xFF303030);
        g.text(font, label, x + 13, y + 1, this.theme.text(), false);
    }

    // ----- melhorias -----

    private int upgradeY(int index) {
        return this.top + 20 + index * 19;
    }

    private void drawUpgrades(GuiGraphicsExtractor g, Font font, int mouseX, int mouseY) {
        int px = this.panelX(), py = this.top + 4;
        this.box(g, px, py, PANEL_W, this.panelH(), true);
        g.text(font, Component.translatable("gui.mistech.config.upgrades"), px + 6, py + 4, this.theme.label(), false);
        MachineUpgrade[] types = MachineUpgrade.values();
        for (int i = 0; i < types.length; i++) {
            MachineUpgrade type = types[i];
            int y = this.upgradeY(i);
            boolean accepted = this.menu.acceptsUpgrade(type);
            int count = this.menu.upgrade(type);
            boolean hover = mouseX >= px + 4 && mouseX < px + PANEL_W - 4 && mouseY >= y && mouseY < y + 18;
            if (hover && accepted && count > 0) {
                g.fill(px + 4, y, px + PANEL_W - 4, y + 18, 0x40FFFFFF);
            }
            g.item(new ItemStack(type.item()), px + 6, y + 1);
            String text = accepted ? count + " / " + type.max() : "—";
            g.text(font, text, px + 28, y + 5, accepted ? this.theme.text() : 0xFF707070, false);
            if (!accepted) {
                g.fill(px + 6, y + 1, px + 22, y + 17, 0xA0000000);
            }
        }
    }

    // ----- clique -----

    /** true = o clique foi das abas/painel (já tratado). */
    public boolean click(double mx, double my, int containerId) {
        List<Tab> tabs = this.tabs();
        for (int i = 0; i < tabs.size(); i++) {
            int x = this.left, y = this.tabY(i);
            if (mx >= x && mx < x + TAB && my >= y && my < y + TAB) {
                Tab tab = tabs.get(i);
                if (tab == Tab.REDSTONE) {
                    this.send(containerId, MachineBlockEntity.BUTTON_REDSTONE);
                } else {
                    open = open == tab ? null : tab;
                    this.sound(1.0F);
                }
                return true;
            }
        }
        if (open == Tab.SIDES) {
            Direction front = this.facing();
            for (FaceButton f : FACES) {
                int x = this.faceX(f), y = this.faceY(f);
                if (mx >= x && mx < x + 17 && my >= y && my < y + 17) {
                    this.send(containerId, MachineBlockEntity.BUTTON_SIDE + faceDirection(f.key(), front).get3DDataValue());
                    return true;
                }
            }
            int px = this.panelX() + 6, cy = this.top + 4 + 78;
            if (mx >= px && mx < px + PANEL_W - 12) {
                if (my >= cy && my < cy + 11) {
                    this.send(containerId, MachineBlockEntity.BUTTON_AUTO_OUT);
                    return true;
                }
                if (my >= cy + 14 && my < cy + 25) {
                    this.send(containerId, MachineBlockEntity.BUTTON_AUTO_IN);
                    return true;
                }
            }
        } else if (open == Tab.UPGRADES) {
            int px = this.panelX();
            MachineUpgrade[] types = MachineUpgrade.values();
            for (int i = 0; i < types.length; i++) {
                int y = this.upgradeY(i);
                if (mx >= px + 4 && mx < px + PANEL_W - 4 && my >= y && my < y + 18) {
                    if (this.menu.acceptsUpgrade(types[i]) && this.menu.upgrade(types[i]) > 0) {
                        this.send(containerId, MachineBlockEntity.BUTTON_UPGRADE + i);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    private void send(int containerId, int id) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode != null) {
            mc.gameMode.handleInventoryButtonClick(containerId, id);
        }
        this.sound(1.3F);
    }

    private void sound(float pitch) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, pitch));
    }

    // ----- dicas -----

    public boolean tooltip(GuiGraphicsExtractor g, Font font, int mouseX, int mouseY) {
        List<Component> lines = this.tooltipLines(mouseX, mouseY);
        if (lines.isEmpty()) {
            return false;
        }
        g.setTooltipForNextFrame(font, lines, Optional.empty(), mouseX, mouseY);
        return true;
    }

    private List<Component> tooltipLines(int mx, int my) {
        List<Tab> tabs = this.tabs();
        for (int i = 0; i < tabs.size(); i++) {
            int x = this.left, y = this.tabY(i);
            if (mx >= x && mx < x + TAB && my >= y && my < y + TAB) {
                return switch (tabs.get(i)) {
                    case REDSTONE -> {
                        List<Component> l = new ArrayList<>();
                        l.add(Component.translatable("gui.mistech.config.redstone"));
                        l.add(Component.translatable("gui.mistech.config.redstone." + this.menu.redstoneMode().name().toLowerCase(java.util.Locale.ROOT))
                                .withStyle(ChatFormatting.GRAY));
                        if (this.menu.redstoneBlocked()) {
                            l.add(Component.translatable("info.mistech.redstone_off").withStyle(ChatFormatting.RED));
                        }
                        l.add(Component.translatable("gui.mistech.config.click_cycle").withStyle(ChatFormatting.DARK_GRAY));
                        yield l;
                    }
                    case SIDES -> List.of(Component.translatable("gui.mistech.config.sides"),
                            Component.translatable("gui.mistech.config.sides.help").withStyle(ChatFormatting.GRAY));
                    case UPGRADES -> List.of(Component.translatable("gui.mistech.config.upgrades"),
                            Component.translatable("gui.mistech.config.upgrades.help").withStyle(ChatFormatting.GRAY));
                };
            }
        }
        if (open == Tab.SIDES) {
            Direction front = this.facing();
            for (FaceButton f : FACES) {
                int x = this.faceX(f), y = this.faceY(f);
                if (mx >= x && mx < x + 17 && my >= y && my < y + 17) {
                    MachineBlockEntity.SideMode mode = this.menu.side(faceDirection(f.key(), front));
                    return List.of(Component.translatable("gui.mistech.config.face." + f.key()),
                            Component.translatable("gui.mistech.config.mode." + mode.name().toLowerCase(java.util.Locale.ROOT))
                                    .withColor(modeColor(mode) & 0xFFFFFF),
                            Component.translatable("gui.mistech.config.click_cycle").withStyle(ChatFormatting.DARK_GRAY));
                }
            }
            int px = this.panelX() + 6, cy = this.top + 4 + 78;
            if (mx >= px && mx < px + PANEL_W - 12) {
                if (my >= cy && my < cy + 11) {
                    return List.of(Component.translatable("gui.mistech.config.auto_out.help").withStyle(ChatFormatting.GRAY));
                }
                if (my >= cy + 14 && my < cy + 25) {
                    return List.of(Component.translatable("gui.mistech.config.auto_in.help").withStyle(ChatFormatting.GRAY));
                }
            }
        } else if (open == Tab.UPGRADES) {
            int px = this.panelX();
            MachineUpgrade[] types = MachineUpgrade.values();
            for (int i = 0; i < types.length; i++) {
                int y = this.upgradeY(i);
                if (mx >= px + 4 && mx < px + PANEL_W - 4 && my >= y && my < y + 18) {
                    List<Component> l = new ArrayList<>();
                    l.add(Component.translatable("item.mistech." + types[i].id() + "_upgrade"));
                    l.add(Component.translatable("item.mistech.upgrade." + types[i].id() + ".desc").withStyle(ChatFormatting.GRAY));
                    if (!this.menu.acceptsUpgrade(types[i])) {
                        l.add(Component.translatable("message.mistech.upgrade.not_accepted").withStyle(ChatFormatting.RED));
                    } else if (this.menu.upgrade(types[i]) > 0) {
                        l.add(Component.translatable("gui.mistech.config.upgrades.remove").withStyle(ChatFormatting.DARK_GRAY));
                    }
                    return l;
                }
            }
        }
        return List.of();
    }
}

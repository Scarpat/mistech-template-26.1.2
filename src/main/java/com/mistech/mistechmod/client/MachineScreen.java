package com.mistech.mistechmod.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.machine.MachineLayout;
import com.mistech.mistechmod.machine.MachineMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Tela genérica das máquinas (Triturador, geradores, Colheitadeira, Plantadeira).
 * Desenha fundo, seta de progresso, chama, barra de energia (com dica ao passar o mouse),
 * as linhas de informação do layout e o selo do gnomo quando há Impulso de Gnomo.
 */
public class MachineScreen extends MysticContainerScreen<MachineMenu> {
    private static final int GNOME_X = 152, GNOME_Y = 72;

    private final Identifier texture;
    private final MachineLayout layout;
    private MachineConfigPanel config;

    public MachineScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.layout = menu.layout();
        this.texture = Identifier.fromNamespaceAndPath(Mistech.MODID, "textures/gui/container/" + this.layout.name + ".png");
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
        this.config = new MachineConfigPanel(this.menu, this.theme());
    }

    /** Abas de configuração (redstone, lados, melhorias) à direita da tela e a coluna de engastes à esquerda. */
    public java.util.List<net.minecraft.client.renderer.Rect2i> configAreas() {
        java.util.List<net.minecraft.client.renderer.Rect2i> list = new java.util.ArrayList<>(
                this.config == null ? java.util.List.of() : this.config.areas(this.leftPos, this.topPos, this.imageWidth));
        net.minecraft.client.renderer.Rect2i sockets = this.socketArea();
        if (sockets != null) {
            list.add(sockets);
        }
        return list;
    }

    // ----- engastes (coluna à esquerda) -----

    /** Retângulo da coluna de engastes, ou null se a máquina não tem. */
    private net.minecraft.client.renderer.Rect2i socketArea() {
        int n = this.layout.socketCount();
        if (n == 0) {
            return null;
        }
        int x = this.leftPos + com.mistech.mistechmod.machine.socket.MachineSockets.SLOT_X - 6;
        int y = this.topPos + com.mistech.mistechmod.machine.socket.MachineSockets.SLOT_Y - 13;
        return new net.minecraft.client.renderer.Rect2i(x, y, 26, 18 * n + 18);
    }

    private java.util.List<com.mistech.mistechmod.item.RuneElement> clientSocketed() {
        java.util.List<com.mistech.mistechmod.item.RuneElement> list = new java.util.ArrayList<>();
        for (int i = this.layout.firstSocket; i < this.layout.size(); i++) {
            com.mistech.mistechmod.item.RuneElement e = com.mistech.mistechmod.item.RuneElement.ofRune(this.menu.getSlot(i).getItem());
            if (e != null) {
                list.add(e);
            }
        }
        return list;
    }

    private void extractSocketPanel(GuiGraphicsExtractor graphics) {
        net.minecraft.client.renderer.Rect2i r = this.socketArea();
        if (r == null) {
            return;
        }
        GuiThemes.Theme theme = this.theme();
        int border = 0xFF000000 | (theme.accent() & 0xFFFFFF);
        graphics.fill(r.getX(), r.getY(), r.getX() + r.getWidth() + 2, r.getY() + r.getHeight(), border);
        graphics.fill(r.getX() + 1, r.getY() + 1, r.getX() + r.getWidth() + 2, r.getY() + r.getHeight() - 1, 0xF0000000 | (theme.well() & 0xFFFFFF));
        // gema no topo da coluna
        int gx = r.getX() + r.getWidth() / 2, gy = r.getY() + 6;
        int glow = 0xFF000000 | (theme.glow() & 0xFFFFFF);
        graphics.fill(gx - 1, gy - 3, gx + 1, gy + 3, glow);
        graphics.fill(gx - 3, gy - 1, gx + 3, gy + 1, glow);
        boolean cancelled = com.mistech.mistechmod.machine.socket.MachineSockets.cancelled(this.clientSocketed());
        for (int i = this.layout.firstSocket; i < this.layout.size(); i++) {
            MachineLayout.SlotSpec spec = this.layout.slots.get(i);
            int x = this.leftPos + spec.x(), y = this.topPos + spec.y();
            graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF000000 | (theme.accent() & 0xFFFFFF));
            graphics.fill(x, y, x + 16, y + 16, 0xFF000000 | (theme.well() & 0xFFFFFF));
            ItemStack stack = this.menu.getSlot(i).getItem();
            com.mistech.mistechmod.item.RuneElement e = com.mistech.mistechmod.item.RuneElement.ofRune(stack);
            if (e != null) {
                boolean works = com.mistech.mistechmod.machine.socket.MachineSockets.applies(e, this.layout.sockets)
                        && !(cancelled && (e == com.mistech.mistechmod.item.RuneElement.FIRE || e == com.mistech.mistechmod.item.RuneElement.ICE));
                int c = works ? (0xFF000000 | e.color()) : 0xFF555555;
                graphics.fill(x - 1, y + 17, x + 17, y + 18, c);
            }
        }
    }

    private boolean extractSocketTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (this.layout.socketCount() == 0 || this.hoveredSlot == null || this.hoveredSlot.index < this.layout.firstSocket
                || this.hoveredSlot.index >= this.layout.size()) {
            return false;
        }
        List<Component> lines = new ArrayList<>();
        ItemStack stack = this.hoveredSlot.getItem();
        com.mistech.mistechmod.item.RuneElement e = com.mistech.mistechmod.item.RuneElement.ofRune(stack);
        if (e == null) {
            lines.add(Component.translatable("gui.mistech.socket.empty"));
            lines.add(Component.translatable("gui.mistech.socket.empty.help").withStyle(net.minecraft.ChatFormatting.GRAY));
            lines.add(Component.translatable("gui.mistech.socket.category." + this.layout.sockets.category().id()).withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        } else {
            lines.add(stack.getHoverName().copy().withColor(e.color()));
            boolean cancelled = com.mistech.mistechmod.machine.socket.MachineSockets.cancelled(this.clientSocketed())
                    && (e == com.mistech.mistechmod.item.RuneElement.FIRE || e == com.mistech.mistechmod.item.RuneElement.ICE);
            if (cancelled) {
                lines.add(Component.translatable("gui.mistech.socket.cancelled").withStyle(net.minecraft.ChatFormatting.RED));
            } else if (com.mistech.mistechmod.machine.socket.MachineSockets.applies(e, this.layout.sockets)) {
                lines.add(Component.translatable(com.mistech.mistechmod.machine.socket.MachineSockets.effectKey(e)).withStyle(net.minecraft.ChatFormatting.GREEN));
            } else {
                lines.add(Component.translatable("gui.mistech.socket.no_effect").withStyle(net.minecraft.ChatFormatting.GRAY));
            }
            if (stack.isDamageableItem()) {
                lines.add(Component.translatable("gui.mistech.socket.uses", stack.getMaxDamage() - stack.getDamageValue(), stack.getMaxDamage())
                        .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
            }
        }
        graphics.setTooltipForNextFrame(this.font, lines, Optional.empty(), mouseX, mouseY);
        return true;
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && this.config != null && this.config.click(event.x(), event.y(), this.menu.containerId)) {
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    protected boolean hasClickedOutside(double mx, double my, int xo, int yo) {
        if (this.config != null && this.config.isInside(mx, my, this.leftPos, this.topPos, this.imageWidth)) {
            return false;
        }
        net.minecraft.client.renderer.Rect2i sockets = this.socketArea();
        if (sockets != null && sockets.contains((int) mx, (int) my)) {
            return false;
        }
        return super.hasClickedOutside(mx, my, xo, yo);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = this.leftPos, y = this.topPos;
        graphics.blit(RenderPipelines.GUI_TEXTURED, this.texture, x, y, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
        this.extractMysticAmbient(graphics);
        this.extractSocketPanel(graphics);

        // dicas: ícone apagado nos espaços vazios
        for (var e : this.layout.hints.entrySet()) {
            int i = e.getKey();
            if (i < this.menu.slots.size() && !this.menu.getSlot(i).hasItem()) {
                MachineLayout.SlotSpec spec = this.layout.slots.get(i);
                int sx = x + spec.x(), sy = y + spec.y();
                graphics.item(e.getValue().icon().get(), sx, sy);
                graphics.fill(sx, sy, sx + 16, sy + 16, (this.theme().well() & 0x00FFFFFF) | 0xB0000000);
            }
        }
        if (this.layout.arrowX >= 0) {
            int arrow = Math.round(MachineLayout.ARROW_W * this.menu.progress());
            if (arrow > 0) {
                graphics.blit(RenderPipelines.GUI_TEXTURED, this.texture, x + this.layout.arrowX, y + this.layout.arrowY,
                        176.0F, 0.0F, arrow, MachineLayout.ARROW_H, 256, 256);
            }
        }
        if (this.layout.flameX >= 0) {
            // a chama vai diminuindo conforme o combustível queima (progresso = quanto falta)
            int flame = Math.round(MachineLayout.FLAME_SIZE * this.menu.progress());
            if (flame > 0) {
                int top = MachineLayout.FLAME_SIZE - flame;
                graphics.blit(RenderPipelines.GUI_TEXTURED, this.texture, x + this.layout.flameX, y + this.layout.flameY + top,
                        176.0F, 17.0F + top, MachineLayout.FLAME_SIZE, flame, 256, 256);
            }
        }
        int bar = Math.round(MachineLayout.BAR_H * Math.min(1.0F, this.menu.energy() / (float) this.layout.capacity));
        if (bar > 0) {
            int top = MachineLayout.BAR_H - bar;
            graphics.blit(RenderPipelines.GUI_TEXTURED, this.texture, x + MachineLayout.BAR_X, y + MachineLayout.BAR_Y + top,
                    200.0F, (float) top, MachineLayout.BAR_W, bar, 256, 256);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        List<Component> lines = this.layout.infoLines.apply(this.menu);
        if (this.layout.infoW > 0) {
            this.boxedInfo(graphics, lines);
        } else {
            for (int i = 0; i < lines.size(); i++) {
                graphics.text(this.font, lines.get(i), this.layout.infoX, this.layout.infoY + i * 11, this.textColor(), false);
            }
        }
        if (this.menu.boosted()) {
            // selo do gnomo: chapeuzinho vermelho com um brilho dourado
            int gx = GNOME_X, gy = GNOME_Y;
            graphics.fill(gx + 5, gy, gx + 7, gy + 1, 0xFFB93A3A);
            graphics.fill(gx + 4, gy + 1, gx + 8, gy + 3, 0xFFB93A3A);
            graphics.fill(gx + 3, gy + 3, gx + 9, gy + 5, 0xFFD04848);
            graphics.fill(gx + 2, gy + 5, gx + 10, gy + 6, 0xFF8A2A2A);
            graphics.fill(gx + 4, gy + 6, gx + 8, gy + 8, 0xFFF1D2B5);
            graphics.fill(gx + 4, gy + 8, gx + 8, gy + 10, 0xFFEFEFEF);
            graphics.fill(gx + 11, gy + 1, gx + 12, gy + 2, 0xFFF0C870);
        }
    }

    /** Info dentro da caixa do layout: quebra as linhas e diminui a letra (até 60%) para não invadir espaços e medidores. */
    private void boxedInfo(GuiGraphicsExtractor graphics, List<Component> lines) {
        int w = this.layout.infoW, h = this.layout.infoH > 0 ? this.layout.infoH : 60;
        float scale = 1.0F;
        List<net.minecraft.util.FormattedCharSequence> wrapped = new java.util.ArrayList<>();
        while (true) {
            wrapped.clear();
            for (Component line : lines) {
                wrapped.addAll(this.font.split(line, (int) (w / scale)));
            }
            if (wrapped.size() * 10 * scale <= h || scale <= 0.6F) {
                break;
            }
            scale -= 0.05F;
        }
        org.joml.Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(this.layout.infoX, this.layout.infoY);
        pose.scale(scale, scale);
        int y = 0;
        for (net.minecraft.util.FormattedCharSequence line : wrapped) {
            if ((y + 9) * scale > h + 1) {
                break;
            }
            graphics.text(this.font, line, 0, y, this.textColor(), false);
            y += 10;
        }
        pose.popMatrix();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (this.config != null) {
            this.config.draw(graphics, this.font, this.leftPos, this.topPos, this.imageWidth, mouseX, mouseY);
            if (this.config.tooltip(graphics, this.font, mouseX, mouseY)) {
                return;
            }
        }
        if (this.extractSocketTooltip(graphics, mouseX, mouseY)) {
            return;
        }
        if (this.menu.getCarried().isEmpty()) {
            for (var e : this.layout.hints.entrySet()) {
                int i = e.getKey();
                if (i < this.menu.slots.size() && !this.menu.getSlot(i).hasItem()) {
                    MachineLayout.SlotSpec spec = this.layout.slots.get(i);
                    if (this.isHovering(spec.x(), spec.y(), 16, 16, mouseX, mouseY)) {
                        graphics.setTooltipForNextFrame(this.font, List.of(Component.translatable(e.getValue().key()),
                                Component.translatable(e.getValue().key() + ".more").withStyle(net.minecraft.ChatFormatting.GRAY)),
                                Optional.empty(), mouseX, mouseY);
                        return;
                    }
                }
            }
        }
        if (this.isHovering(MachineLayout.BAR_X, MachineLayout.BAR_Y, MachineLayout.BAR_W, MachineLayout.BAR_H, mouseX, mouseY)) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("gui.mistech.energy_tooltip", this.menu.energy(), this.layout.capacity));
            graphics.setTooltipForNextFrame(this.font, lines, Optional.empty(), mouseX, mouseY);
        } else if (this.menu.boosted() && this.isHovering(GNOME_X, GNOME_Y, 12, 10, mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(this.font, List.of(Component.translatable("gui.mistech.gnome_boost")),
                    Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    protected GuiThemes.Theme theme() {
        return GuiThemes.get(this.layout.name);
    }
}

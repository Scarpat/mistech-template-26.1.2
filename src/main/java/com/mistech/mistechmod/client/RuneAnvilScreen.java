package com.mistech.mistechmod.client;

import java.util.ArrayList;
import java.util.List;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.item.RuneElement;
import com.mistech.mistechmod.item.mithril.GearRunes;
import com.mistech.mistechmod.item.mithril.MithrilGear;
import com.mistech.mistechmod.menu.RuneAnvilMenu;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Tela da Bigorna Rúnica: peça à esquerda, 8 engastes (os que a peça não tem ficam trancados). */
public class RuneAnvilScreen extends MysticContainerScreen<RuneAnvilMenu> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Mistech.MODID, "textures/gui/container/rune_anvil.png");

    public RuneAnvilScreen(RuneAnvilMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = this.leftPos, y = this.topPos;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
        this.extractMysticAmbient(graphics);

        int capacity = this.menu.capacity();
        for (int i = 0; i < GearRunes.MAX_SLOTS; i++) {
            int sx = x + RuneAnvilMenu.RUNE_X0 + (i % 4) * RuneAnvilMenu.RUNE_STEP;
            int sy = y + RuneAnvilMenu.RUNE_Y0 + (i / 4) * RuneAnvilMenu.RUNE_STEP;
            if (i >= capacity) {
                // engaste trancado
                graphics.fill(sx, sy, sx + 16, sy + 16, 0xE0100A18);
                graphics.fill(sx + 5, sy + 7, sx + 11, sy + 12, 0xFF3A2D52);
                graphics.fill(sx + 6, sy + 4, sx + 10, sy + 5, 0xFF3A2D52);
                graphics.fill(sx + 6, sy + 4, sx + 7, sy + 7, 0xFF3A2D52);
                graphics.fill(sx + 9, sy + 4, sx + 10, sy + 7, 0xFF3A2D52);
            } else {
                ItemStack rune = this.menu.getSlot(1 + i).getItem();
                RuneElement element = RuneElement.ofRune(rune);
                if (element != null) {
                    // brilho da cor do elemento atrás da runa
                    int glow = 0x60000000 | (element.color() & 0xFFFFFF);
                    graphics.fill(sx, sy, sx + 16, sy + 16, glow);
                }
            }
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        ItemStack gear = this.menu.gear();
        int cx = RuneAnvilMenu.GEAR_X + 8;
        if (gear.isEmpty()) {
            Component hint = Component.translatable("screen.mistech.rune_anvil.insert");
            graphics.text(this.font, hint, cx - this.font.width(hint) / 2, 60, this.textColor(), false);
            return;
        }
        int capacity = this.menu.capacity();
        int used = 0;
        for (int i = 0; i < capacity; i++) {
            if (!this.menu.getSlot(1 + i).getItem().isEmpty()) {
                used++;
            }
        }
        Component count = Component.translatable("screen.mistech.rune_anvil.count", used, capacity);
        graphics.text(this.font, count, cx - this.font.width(count) / 2, 60, this.labelColor(), false);
    }

    /** Runas mostram o que fazem engastadas. */
    @Override
    protected List<Component> getTooltipFromContainerItem(ItemStack stack) {
        List<Component> lines = new ArrayList<>(super.getTooltipFromContainerItem(stack));
        RuneElement element = RuneElement.ofRune(stack);
        if (element != null) {
            lines.add(Component.translatable("screen.mistech.rune_anvil.socket").withStyle(ChatFormatting.LIGHT_PURPLE));
            lines.add(Component.translatable("rune_socket.mistech." + element.id()).withColor(element.color()));
            if (GearRunes.isSealed(stack)) {
                lines.add(Component.translatable("screen.mistech.rune_anvil.sealed").withStyle(ChatFormatting.GOLD));
            }
        } else if (MithrilGear.isMithril(stack) && GearRunes.slots(stack) == 0) {
            lines.add(Component.translatable("screen.mistech.rune_anvil.no_sockets").withStyle(ChatFormatting.RED));
        }
        return lines;
    }

    @Override
    protected GuiThemes.Theme theme() {
        return GuiThemes.get("rune_anvil");
    }
}

package com.mistech.mistechmod.spellcraft.conjurer.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mistech.mistechmod.machine.MachineLayout;
import com.mistech.mistechmod.machine.MachineMenu;
import com.mistech.mistechmod.purity.client.FluidMachineScreen;
import com.mistech.mistechmod.spellcraft.Spell;
import com.mistech.mistechmod.spellcraft.conjurer.RunicConjurerBlockEntity;
import com.mistech.mistechmod.spellcraft.conjurer.SpellCrystalItem;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Tela do Conjurador Rúnico: a das máquinas com tanques (Impura à esquerda, Purificada à direita) + botão de modo
 * (automático / pulso de redstone), o estado atual e a dica da magia na seta de recarga.
 */
public class ConjurerScreen extends FluidMachineScreen {
    private static final int MODE_X = 69, MODE_Y = 54, MODE_SIZE = 16;
    private final ItemStack autoIcon = new ItemStack(Items.CLOCK);
    private final ItemStack pulseIcon = new ItemStack(Items.REDSTONE_TORCH);

    public ConjurerScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    private int mode() {
        return this.menu.info(2) & 3;
    }

    private int status() {
        return (this.menu.info(2) >> 2) & 15;
    }

    private boolean overMode(double mouseX, double mouseY) {
        return this.isHovering(MODE_X, MODE_Y, MODE_SIZE, MODE_SIZE, mouseX, mouseY);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        int x = MODE_X, y = MODE_Y;
        boolean hover = this.overMode(mouseX, mouseY);
        graphics.fill(x - 1, y - 1, x + MODE_SIZE + 1, y + MODE_SIZE + 1, hover ? this.labelColor() : (this.theme().accent() & 0x00FFFFFF) | 0x90000000);
        graphics.fill(x, y, x + MODE_SIZE, y + MODE_SIZE, this.theme().well());
        graphics.item(this.mode() == RunicConjurerBlockEntity.MODE_PULSE ? this.pulseIcon : this.autoIcon, x, y);
        int st = Math.min(this.status(), RunicConjurerBlockEntity.STATUS_KEYS.length - 1);
        Component text = Component.translatable("gui.mistech.runic_conjurer.status." + RunicConjurerBlockEntity.STATUS_KEYS[st]);
        int color = st == RunicConjurerBlockEntity.ST_NO_ENERGY || st == RunicConjurerBlockEntity.ST_NO_ESSENCE
                || st == RunicConjurerBlockEntity.ST_INCOMPATIBLE ? 0xFFE06060 : this.textColor();
        String line = this.font.plainSubstrByWidth(text.getString(), 100);
        graphics.text(this.font, line, this.imageWidth - 8 - this.font.width(line), this.inventoryLabelY, color, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && this.overMode(event.x(), event.y()) && this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, RunicConjurerBlockEntity.BUTTON_MODE);
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (this.overMode(mouseX, mouseY)) {
            boolean pulse = this.mode() == RunicConjurerBlockEntity.MODE_PULSE;
            graphics.setTooltipForNextFrame(this.font, List.of(
                    Component.translatable(pulse ? "gui.mistech.runic_conjurer.mode.pulse" : "gui.mistech.runic_conjurer.mode.auto"),
                    Component.translatable(pulse ? "gui.mistech.runic_conjurer.mode.pulse.help" : "gui.mistech.runic_conjurer.mode.auto.help")
                            .withStyle(ChatFormatting.GRAY),
                    Component.translatable("gui.mistech.runic_conjurer.mode.click").withStyle(ChatFormatting.DARK_GRAY)),
                    Optional.empty(), mouseX, mouseY);
        } else if (this.isHovering(this.menu.layout().arrowX, this.menu.layout().arrowY, MachineLayout.ARROW_W, MachineLayout.ARROW_H, mouseX, mouseY)) {
            List<Component> lines = new ArrayList<>();
            Spell spell = SpellCrystalItem.spell(this.menu.getSlot(RunicConjurerBlockEntity.SLOT_CRYSTAL).getItem());
            if (spell == null) {
                lines.add(Component.translatable("gui.mistech.runic_conjurer.status.no_crystal"));
            } else {
                lines.add(Component.translatable(spell.nameKey()).withColor(spell.element().color()));
                float p = this.menu.progress();
                lines.add(Component.translatable("gui.mistech.runic_conjurer.recharge", Math.round(p * 100)).withStyle(ChatFormatting.GRAY));
            }
            graphics.setTooltipForNextFrame(this.font, lines, Optional.empty(), mouseX, mouseY);
        }
    }
}

package com.mistech.mistechmod.extraplanar.client;

import com.mistech.mistechmod.extraplanar.ExtraplanarData;
import com.mistech.mistechmod.extraplanar.ExtraplanarNetwork;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** Tela para digitar o código (senha) que liga Barris/Tanques Extraplanares entre si. */
public class ExtraplanarCodeScreen extends Screen {
    private static final int PANEL_W = 220;
    private static final int PANEL_H = 92;
    private static final int GOLD = 0xFF7FD8FF;
    private static final int TEXT_DIM = 0xFF9C8DB8;

    private final BlockPos pos;
    private final String current;
    private EditBox codeBox;

    public ExtraplanarCodeScreen(BlockPos pos, String current) {
        super(Component.translatable("screen.mistech.extraplanar.title"));
        this.pos = pos;
        this.current = current;
    }

    private int panelX() {
        return (this.width - PANEL_W) / 2;
    }

    private int panelY() {
        return (this.height - PANEL_H) / 2;
    }

    @Override
    protected void init() {
        int x = this.panelX();
        int y = this.panelY();
        this.codeBox = new EditBox(this.font, x + 10, y + 32, PANEL_W - 20, 16, Component.translatable("screen.mistech.extraplanar.code"));
        this.codeBox.setMaxLength(ExtraplanarData.MAX_CODE_LENGTH);
        this.codeBox.setValue(this.current);
        this.codeBox.setHint(Component.translatable("screen.mistech.extraplanar.hint"));
        this.addRenderableWidget(this.codeBox);
        this.setInitialFocus(this.codeBox);
        this.addRenderableWidget(Button.builder(Component.translatable("screen.mistech.extraplanar.confirm"), b -> this.confirm())
                .bounds(x + 10, y + 62, 100, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("screen.mistech.extraplanar.clear"), b -> this.clear())
                .bounds(x + PANEL_W - 110, y + 62, 100, 20).build());
    }

    private void confirm() {
        ClientPacketDistributor.sendToServer(new ExtraplanarNetwork.SetCodePayload(this.pos, this.codeBox.getValue().strip()));
        this.onClose();
    }

    private void clear() {
        ClientPacketDistributor.sendToServer(new ExtraplanarNetwork.SetCodePayload(this.pos, ""));
        this.onClose();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER || event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER) {
            this.confirm();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        graphics.fillGradient(0, 0, this.width, this.height, 0xC0040A14, 0xE0081A2A);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        int x = this.panelX();
        int y = this.panelY();
        graphics.fill(x - 3, y - 3, x + PANEL_W + 3, y + PANEL_H + 3, 0xFF0A1422);
        graphics.fill(x - 2, y - 2, x + PANEL_W + 2, y + PANEL_H + 2, 0xFF2E7A8A);
        graphics.fillGradient(x, y, x + PANEL_W, y + PANEL_H, 0xFF12263A, 0xFF0A1220);
        graphics.centeredText(this.font, this.title, x + PANEL_W / 2, y + 8, GOLD);
        graphics.fill(x + 10, y + 20, x + PANEL_W - 10, y + 21, 0x802E7A8A);
        graphics.centeredText(this.font, Component.translatable("screen.mistech.extraplanar.info"), x + PANEL_W / 2, y + 52 - 1 + 0, TEXT_DIM);
        super.extractRenderState(graphics, mouseX, mouseY, a);
    }
}

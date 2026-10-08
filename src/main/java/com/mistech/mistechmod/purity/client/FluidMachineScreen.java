package com.mistech.mistechmod.purity.client;

import java.util.List;
import java.util.Optional;

import com.mistech.mistechmod.client.MachineScreen;
import com.mistech.mistechmod.machine.MachineMenu;
import com.mistech.mistechmod.purity.PurityLayouts;
import com.mistech.mistechmod.purity.block.DistillerBlockEntity;
import com.mistech.mistechmod.purity.block.PureGeneratorBlockEntity;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Tela do Destilador e do Gerador Purificado: a tela genérica das máquinas + medidores de fluido
 * (roxo = Essência Impura, dourado = Essência Purificada) com dica ao passar o mouse.
 */
public class FluidMachineScreen extends MachineScreen {
    private static final int IMPURE = 0xFF5B2A86, IMPURE_TOP = 0xFF8D4FC4;
    private static final int PURE = 0xFFE8C766, PURE_TOP = 0xFFFFF2B8;

    private static final int LAVA = 0xFFD8501A, LAVA_TOP = 0xFFFFB347;
    private static final int XP = 0xFF4FA82A, XP_TOP = 0xFFC8FF5A;

    private record Gauge(int x, int info, int capacity, int color, int top, String name, String unit) {
        Gauge(int x, int info, int capacity, int color, int top, String name) {
            this(x, info, capacity, color, top, name, "gui.mistech.fluid_amount");
        }
    }

    private final List<Gauge> gauges;

    public FluidMachineScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        int gx = com.mistech.mistechmod.power.PowerLayouts.GAUGE_X;
        String name = menu.layout().name;
        if (name.equals("impure_reactor")) {
            this.gauges = List.of(new Gauge(gx, 1, com.mistech.mistechmod.power.ImpureReactorBlockEntity.TANK, IMPURE, IMPURE_TOP, "block.mistech.impure_essence"));
        } else if (name.equals("igneous_boiler")) {
            this.gauges = List.of(new Gauge(gx, 1, com.mistech.mistechmod.power.IgneousBoilerBlockEntity.TANK, LAVA, LAVA_TOP, "block.minecraft.lava"));
        } else if (name.equals("experience_generator")) {
            this.gauges = List.of(new Gauge(gx, 1, com.mistech.mistechmod.power.ExperienceGeneratorBlockEntity.TANK, XP, XP_TOP,
                    "gui.mistech.experience_generator.stored", "gui.mistech.experience_amount"));
        } else if (name.equals("distiller")) {
            this.gauges = List.of(
                    new Gauge(PurityLayouts.DISTILLER_IMPURE_X, 0, DistillerBlockEntity.TANK, IMPURE, IMPURE_TOP, "block.mistech.impure_essence"),
                    new Gauge(PurityLayouts.DISTILLER_PURE_X, 1, DistillerBlockEntity.TANK, PURE, PURE_TOP, "block.mistech.purified_essence"));
        } else {
            this.gauges = List.of(new Gauge(PurityLayouts.GENERATOR_TANK_X, 1, PureGeneratorBlockEntity.TANK, PURE, PURE_TOP, "block.mistech.purified_essence"));
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        for (Gauge g : this.gauges) {
            int amount = this.menu.info(g.info());
            int h = Math.round(PurityLayouts.GAUGE_H * Math.min(1.0F, amount / (float) g.capacity()));
            int x = this.leftPos + g.x(), bottom = this.topPos + PurityLayouts.GAUGE_Y + PurityLayouts.GAUGE_H;
            if (h > 0) {
                graphics.fill(x, bottom - h, x + PurityLayouts.GAUGE_W, bottom, g.color());
                graphics.fill(x, bottom - h, x + PurityLayouts.GAUGE_W, bottom - h + 1, g.top());
            }
            // marcas de 1.000 mB
            for (int mb = 1000; mb < g.capacity(); mb += 1000) {
                int my = bottom - Math.round(PurityLayouts.GAUGE_H * mb / (float) g.capacity());
                graphics.fill(x, my, x + 4, my + 1, 0x80FFFFFF);
            }
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        for (Gauge g : this.gauges) {
            if (this.isHovering(g.x(), PurityLayouts.GAUGE_Y, PurityLayouts.GAUGE_W, PurityLayouts.GAUGE_H, mouseX, mouseY)) {
                graphics.setTooltipForNextFrame(this.font, List.of(Component.translatable(g.name()),
                        Component.translatable(g.unit(), this.menu.info(g.info()), g.capacity())), Optional.empty(), mouseX, mouseY);
            }
        }
    }
}

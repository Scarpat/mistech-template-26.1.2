package com.mistech.mistechmod.power;

import java.util.ArrayList;
import java.util.List;

import com.mistech.mistechmod.celestial.CelestialContent;
import com.mistech.mistechmod.machine.MachineLayout;
import com.mistech.mistechmod.registry.MistechMenus;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

import static com.mistech.mistechmod.machine.MachineLayout.Kind.*;

/** Telas dos geradores avançados. Os medidores (x = {@link #GAUGE_X}) são desenhados pela FluidMachineScreen. */
public final class PowerLayouts {
    private PowerLayouts() {}

    public static final int GAUGE_X = 124;

    /** Reator Impuro: combustível + 2 saídas (Borra e recipientes). info0 FE/t, info1 tanque, info2 situação, info3 % da próxima Borra. */
    public static final MachineLayout IMPURE_REACTOR = MachineLayout.builder("impure_reactor", MistechMenus.IMPURE_REACTOR::get)
            .slot(30, 50, FUEL)
            .grid(58, 50, 2, 1, OUTPUT)
            .capacity(ImpureReactorBlockEntity.CAPACITY)
            .fuel(ImpureReactorBlockEntity::isFuel)
            .flame(31, 32)
            .info(56, 20, menu -> {
                List<Component> lines = new ArrayList<>();
                int status = menu.info(2);
                if (menu.info(0) > 0) {
                    lines.add(Component.translatable("gui.mistech.generating", menu.info(0)));
                } else {
                    lines.add(Component.translatable("gui.mistech.impure_reactor.status." + status)
                            .withStyle(status == ImpureReactorBlockEntity.STATUS_RESIDUE ? ChatFormatting.RED : ChatFormatting.GRAY));
                }
                lines.add(Component.translatable("gui.mistech.impure_reactor.residue", menu.info(3)));
                return lines;
            })
            .build();

    /**
     * Observatório Estelar: lente (Fragmento Estelar). info0 FE/t; info1 = fase (bits 0-2), céu (3-4), Mesa (5), altitude (6), sem lente (7).
     */
    public static final MachineLayout STELLAR_OBSERVATORY = MachineLayout.builder("stellar_observatory", MistechMenus.STELLAR_OBSERVATORY::get)
            .slot(128, 20, RUNE)
            .capacity(StellarObservatoryBlockEntity.CAPACITY)
            .rune(s -> s.is(CelestialContent.STAR_FRAGMENT.get()))
            .info(12, 22, menu -> {
                int info = menu.info(1);
                List<Component> lines = new ArrayList<>();
                if ((info & 128) != 0) {
                    lines.add(Component.translatable("gui.mistech.observatory.no_lens").withStyle(ChatFormatting.RED));
                } else {
                    lines.add(menu.info(0) > 0 ? Component.translatable("gui.mistech.generating", menu.info(0))
                            : Component.translatable("gui.mistech.idle"));
                }
                lines.add(Component.translatable("gui.mistech.astral.moon", Component.translatable("gui.mistech.moon." + (info & 7))));
                int sky = (info >> 3) & 3;
                lines.add(Component.translatable(sky == 1 ? "gui.mistech.observatory.day"
                        : sky == 3 ? "gui.mistech.observatory.rain" : "gui.mistech.astral.sky." + sky));
                if ((info & 32) != 0) {
                    lines.add(Component.translatable("gui.mistech.astral.table_bonus"));
                }
                if ((info & 64) != 0) {
                    lines.add(Component.translatable("gui.mistech.observatory.altitude"));
                }
                return lines;
            })
            .build();

    /** Caldeira Ígnea: balde de lava, saída (baldes/frascos vazios) e catalisador. info0 FE/t, info1 lava, info2 catalisador (ticks). */
    public static final MachineLayout IGNEOUS_BOILER = MachineLayout.builder("igneous_boiler", MistechMenus.IGNEOUS_BOILER::get)
            .slot(30, 50, FUEL)
            .slot(58, 50, OUTPUT)
            .slot(94, 50, FUEL)
            .capacity(IgneousBoilerBlockEntity.CAPACITY)
            .fuel(s -> s.is(Items.LAVA_BUCKET) || IgneousBoilerBlockEntity.catalystTicks(s) > 0)
            .flame(95, 32)
            .info(30, 20, menu -> List.of(menu.info(0) > 0
                    ? Component.translatable("gui.mistech.generating", menu.info(0))
                    : Component.translatable("gui.mistech.idle")))
            .build();

    /** Gerador de Experiência: frascos/itens encantados e saída dos itens limpos. info0 FE/t, info1 pontos. */
    public static final MachineLayout EXPERIENCE_GENERATOR = MachineLayout.builder("experience_generator", MistechMenus.EXPERIENCE_GENERATOR::get)
            .slot(30, 50, FUEL)
            .slot(58, 50, OUTPUT)
            .capacity(ExperienceGeneratorBlockEntity.CAPACITY)
            .fuel(s -> ExperienceGeneratorBlockEntity.itemPoints(s) > 0)
            .info(30, 20, menu -> List.of(
                    menu.info(0) > 0 ? Component.translatable("gui.mistech.generating", menu.info(0)) : Component.translatable("gui.mistech.idle"),
                    Component.translatable("gui.mistech.experience_generator.hint").withStyle(ChatFormatting.DARK_GRAY)))
            .build();
}

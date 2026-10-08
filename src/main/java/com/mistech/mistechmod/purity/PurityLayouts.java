package com.mistech.mistechmod.purity;

import java.util.List;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.machine.MachineLayout;
import com.mistech.mistechmod.registry.MistechMenus;

import net.minecraft.network.chat.Component;

import static com.mistech.mistechmod.machine.MachineLayout.Kind.*;

/** Telas das máquinas da Magia Purificada (mesmo sistema das máquinas genéricas). */
public final class PurityLayouts {
    private PurityLayouts() {}

    /** Medidores de fluido desenhados pela {@link com.mistech.mistechmod.purity.client.FluidMachineScreen}. */
    public static final int GAUGE_Y = 17, GAUGE_W = 14, GAUGE_H = 52;
    public static final int DISTILLER_IMPURE_X = 18, DISTILLER_PURE_X = 124, GENERATOR_TANK_X = 124;

    /**
     * Destilador: entrada (frasco/balde de Essência Impura), filtro de mitril, 2 saídas (Borra + recipientes vazios).
     * info0 = Essência Impura (mB), info1 = Essência Purificada (mB).
     */
    public static final MachineLayout DISTILLER = MachineLayout.builder("distiller", MistechMenus.DISTILLER::get)
            .slot(40, 25, INPUT)
            .slot(40, 53, RUNE)
            .grid(98, 26, 1, 2, OUTPUT)
            .capacity(20_000)
            .input(s -> s.is(Mistech.IMPURE_ESSENCE_BOTTLE.get()) || s.is(Mistech.IMPURE_ESSENCE_BUCKET.get()))
            .rune(s -> s.is(PurityContent.MITHRIL_FILTER.get()))
            .arrow(66, 34)
            .build();

    /** Gerador Purificado: combustível (frasco/balde de Essência Purificada) e saída dos recipientes. info0 = FE/t, info1 = tanque (mB). */
    public static final MachineLayout PURE_GENERATOR = MachineLayout.builder("pure_generator", MistechMenus.PURE_GENERATOR::get)
            .slot(30, 50, FUEL)
            .slot(58, 50, OUTPUT)
            .capacity(50_000)
            .fuel(s -> s.is(PurityContent.PURIFIED_ESSENCE_BOTTLE.get()) || s.is(PurityContent.PURIFIED_ESSENCE_BUCKET.get()))
            .flame(31, 32)
            .info(56, 24, menu -> List.of(menu.info(0) > 0
                    ? Component.translatable("gui.mistech.fe_rate", menu.info(0))
                    : Component.translatable("gui.mistech.idle")))
            .build();
}

package com.mistech.mistechmod.beyond;

import java.util.List;

import com.mistech.mistechmod.machine.MachineLayout;
import com.mistech.mistechmod.registry.MistechMenus;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

import static com.mistech.mistechmod.machine.MachineLayout.Kind.*;

/** Layouts das máquinas do Nether (mesma tela do Gerador de Cinzas). */
public final class BeyondLayouts {
    private BeyondLayouts() {}

    /** Gerador de Blaze: 1 espaço de combustível (Vara ou Pó de Blaze). info0 = FE/t gerando agora. */
    public static final MachineLayout BLAZE_GENERATOR = MachineLayout.builder("blaze_generator", MistechMenus.BLAZE_GENERATOR::get)
            .slot(30, 50, FUEL)
            .capacity(BlazeGeneratorBlockEntity.CAPACITY)
            .fuel(s -> s.is(Items.BLAZE_ROD) || s.is(Items.BLAZE_POWDER))
            .flame(31, 32)
            .info(56, 32, menu -> List.of(menu.info(0) > 0
                    ? Component.translatable("gui.mistech.generating", menu.info(0))
                    : Component.translatable("gui.mistech.idle")))
            .build();
}

package com.mistech.mistechmod.multiblock;

import com.mistech.mistechmod.Mistech;

import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Agachado + botão direito com a Pedra Guia numa âncora de multibloco: só projeta o holograma (no cliente) e
 * não monta nada. Cancela nos dois lados para nenhum outro uso da Pedra Guia rodar.
 */
@EventBusSubscriber(modid = Mistech.MODID)
public final class GuideStoneEvents {
    private GuideStoneEvents() {}

    public static boolean isProjectionClick(PlayerInteractEvent.RightClickBlock event) {
        return event.getEntity().isShiftKeyDown() && event.getItemStack().is(Mistech.GUIDE_STONE.get())
                && MultiblockPlans.isAnchor(event.getLevel().getBlockState(event.getPos()));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (isProjectionClick(event)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }
}

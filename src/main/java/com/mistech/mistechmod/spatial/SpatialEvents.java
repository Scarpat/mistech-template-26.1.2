package com.mistech.mistechmod.spatial;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.registry.SpatialContent;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Shift + botão direito com corante numa Âncora Espacial: muda a cor dela (agachado o bloco não recebe o clique sozinho). */
@EventBusSubscriber(modid = Mistech.MODID)
public final class SpatialEvents {
    private SpatialEvents() {}

    @SubscribeEvent
    static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getEntity().isShiftKeyDown() || !event.getItemStack().has(DataComponents.DYE)) {
            return;
        }
        BlockState state = event.getLevel().getBlockState(event.getPos());
        if (!state.is(SpatialContent.SPATIAL_ANCHOR.get())) {
            return;
        }
        if (SpatialAnchorBlock.dye(event.getLevel(), event.getPos(), state, event.getEntity(), event.getItemStack())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }
}

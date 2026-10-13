package com.mistech.mistechmod.lore.client;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.lore.CucaCauldronBlockEntity;
import com.mistech.mistechmod.lore.LoreContent;

import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

/** Cor do caldo no Caldeirão da Cuca (mistura das poções que estão dentro). */
@EventBusSubscriber(modid = Mistech.MODID, value = Dist.CLIENT)
public final class LoreClient {
    private LoreClient() {}

    @SubscribeEvent
    static void onBlockTints(RegisterColorHandlersEvent.BlockTintSources event) {
        event.register(java.util.List.of(new BlockTintSource() {
            @Override
            public int color(BlockState state) {
                return 0xFF3A8A3A;
            }

            @Override
            public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
                return level.getBlockEntity(pos) instanceof CucaCauldronBlockEntity cauldron ? 0xFF000000 | cauldron.color() : this.color(state);
            }
        }), LoreContent.CUCA_CAULDRON.get());
    }
}

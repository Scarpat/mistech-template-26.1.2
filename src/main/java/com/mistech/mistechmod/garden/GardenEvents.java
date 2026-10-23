package com.mistech.mistechmod.garden;

import com.mistech.mistechmod.Mistech;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

/** Sementes de Mandrágora: colher uma plantação madura À NOITE tem {@link #SEED_CHANCE} de chance de trazer uma. */
@EventBusSubscriber(modid = Mistech.MODID)
public final class GardenEvents {
    public static final float SEED_CHANCE = 0.04F;

    private GardenEvents() {}

    @SubscribeEvent
    static void onBlockDrops(BlockDropsEvent event) {
        BlockState state = event.getState();
        ServerLevel level = event.getLevel();
        if (!(event.getBreaker() instanceof Player) || !(state.getBlock() instanceof CropBlock crop) || crop instanceof MandrakeCropBlock
                || !crop.isMaxAge(state) || !level.isDarkOutside() || level.getRandom().nextFloat() >= SEED_CHANCE) {
            return;
        }
        BlockPos pos = event.getPos();
        event.getDrops().add(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5,
                new ItemStack(GardenContent.MANDRAKE_SEEDS.get())));
    }
}

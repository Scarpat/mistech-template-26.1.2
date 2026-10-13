package com.mistech.mistechmod.utility.client;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.utility.RunicElevatorBlock;
import com.mistech.mistechmod.utility.UtilityContent;
import com.mistech.mistechmod.utility.UtilityNetwork;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** Cliente dos utilitários: pulo/agachar no Elevador Rúnico e cor da runa. */
@EventBusSubscriber(modid = Mistech.MODID, value = Dist.CLIENT)
public final class UtilityClient {
    private static boolean jumpWas;
    private static boolean sneakWas;

    private UtilityClient() {}

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }
        boolean jump = player.input.keyPresses.jump();
        boolean sneak = player.input.keyPresses.shift();
        if (minecraft.screen == null) {
            BlockPos below = BlockPos.containing(player.getX(), player.getY() - 0.5, player.getZ());
            boolean onElevator = player.level().getBlockState(below).getBlock() instanceof RunicElevatorBlock;
            if (onElevator && jump && !jumpWas) {
                ClientPacketDistributor.sendToServer(new UtilityNetwork.ElevatorPayload(true));
            } else if (onElevator && sneak && !sneakWas && player.onGround()) {
                ClientPacketDistributor.sendToServer(new UtilityNetwork.ElevatorPayload(false));
            }
        }
        jumpWas = jump;
        sneakWas = sneak;
    }

    @SubscribeEvent
    static void onBlockTints(RegisterColorHandlersEvent.BlockTintSources event) {
        event.register(java.util.List.of(new net.minecraft.client.color.block.BlockTintSource() {
            @Override
            public int color(net.minecraft.world.level.block.state.BlockState state) {
                return RunicElevatorBlock.tint(state);
            }

            @Override
            public java.util.Set<net.minecraft.world.level.block.state.properties.Property<?>> relevantProperties() {
                return java.util.Set.of(RunicElevatorBlock.COLOR);
            }
        }), UtilityContent.RUNIC_ELEVATOR.get());
        event.register(java.util.List.of(new net.minecraft.client.color.block.BlockTintSource() {
            @Override
            public int color(net.minecraft.world.level.block.state.BlockState state) {
                return com.mistech.mistechmod.utility.RunicHookahBlock.runeTint(state);
            }

            @Override
            public java.util.Set<net.minecraft.world.level.block.state.properties.Property<?>> relevantProperties() {
                return java.util.Set.of(com.mistech.mistechmod.utility.RunicHookahBlock.RUNE);
            }
        }, new net.minecraft.client.color.block.BlockTintSource() {
            @Override
            public int color(net.minecraft.world.level.block.state.BlockState state) {
                return 0xFF3F76E4; // água da base
            }
        }), UtilityContent.RUNIC_HOOKAH.get());
    }
}

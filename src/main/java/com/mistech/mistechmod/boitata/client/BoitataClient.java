package com.mistech.mistechmod.boitata.client;

import org.lwjgl.glfw.GLFW;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.MistechClient;
import com.mistech.mistechmod.boitata.BoitataGear;
import com.mistech.mistechmod.boitata.FireBreath;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** Tecla do Sopro de Fogo (R), visão dentro da lava com o capacete e nado rápido na lava com o conjunto. */
@EventBusSubscriber(modid = Mistech.MODID, value = Dist.CLIENT)
public final class BoitataClient {
    public static final KeyMapping FIRE_BREATH = new KeyMapping("key.mistech.fire_breath",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, MistechClient.KEY_CATEGORY);

    private static boolean breathHeld;

    private BoitataClient() {}

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        // segurar = jato contínuo montado; soltar avisa o servidor
        while (FIRE_BREATH.consumeClick()) {
            // só esvazia a fila; o estado vem de isDown()
        }
        boolean down = player != null && minecraft.screen == null && FIRE_BREATH.isDown();
        if (down != breathHeld) {
            breathHeld = down;
            if (player != null) {
                ClientPacketDistributor.sendToServer(new FireBreath.BreathPayload(down));
            }
        }
        if (player != null && player.isInLava() && !player.isPassenger() && BoitataGear.fullSet(player)) {
            // Forma de Serpente: nada na lava quase como na água
            Vec2 move = player.input.getMoveVector();
            if (move.lengthSquared() > 0.0F) {
                Vec3 look = player.getLookAngle();
                Vec3 side = new Vec3(-look.z, 0.0, look.x).normalize();
                Vec3 push = look.scale(move.y * 0.06).add(side.scale(-move.x * 0.04));
                player.setDeltaMovement(player.getDeltaMovement().add(push));
            }
            if (player.input.keyPresses.jump()) {
                player.setDeltaMovement(player.getDeltaMovement().add(0.0, 0.03, 0.0));
            }
        }
    }

    @SubscribeEvent
    static void onFog(ViewportEvent.RenderFog event) {
        if (event.getType() != FogType.LAVA) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && BoitataGear.wearing(player, EquipmentSlot.HEAD)) {
            event.setNearPlaneDistance(0.0F);
            event.setFarPlaneDistance(24.0F);
        }
    }
}

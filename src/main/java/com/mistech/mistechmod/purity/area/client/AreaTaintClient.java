package com.mistech.mistechmod.purity.area.client;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.purity.area.AreaTaintPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Cliente da Mácula do lugar: guarda o que o servidor manda a cada 5 s e desenha os sinais.
 * Maculada (40+): cinzas no ar. Corrompida (60+): névoa roxa que fica mais fechada até a faixa Profana.
 */
@EventBusSubscriber(modid = Mistech.MODID, value = Dist.CLIENT)
public final class AreaTaintClient {
    private static float targetHere, targetAround;
    private static float here, around;

    private AreaTaintClient() {}

    public static void handle(AreaTaintPayload payload) {
        targetHere = payload.here() / 10.0F;
        targetAround = payload.around() / 10.0F;
    }

    /** Mácula do chunk em que o jogador está (suavizada). */
    public static float here() {
        return here;
    }

    /** 0 a 1: quanto a névoa roxa pesa (começa na Corrompida). */
    private static float fog() {
        return Math.max(0.0F, Math.min(1.0F, (around - 55.0F) / 45.0F));
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            here = around = targetHere = targetAround = 0;
            return;
        }
        here += (targetHere - here) * 0.05F;
        around += (targetAround - around) * 0.05F;
        if (minecraft.isPaused() || here < 40.0F) {
            return;
        }
        RandomSource random = player.getRandom();
        int count = here >= 80 ? 3 : here >= 60 ? 2 : 1;
        for (int i = 0; i < count; i++) {
            if (random.nextInt(3) != 0) {
                continue;
            }
            double x = player.getX() + (random.nextDouble() - 0.5) * 16.0;
            double y = player.getY() + random.nextDouble() * 6.0;
            double z = player.getZ() + (random.nextDouble() - 0.5) * 16.0;
            minecraft.level.addParticle(here >= 60 && random.nextInt(3) == 0 ? ParticleTypes.WITCH : ParticleTypes.ASH, x, y, z, 0.0, 0.0, 0.0);
        }
    }

    @SubscribeEvent
    static void onFogColor(ViewportEvent.ComputeFogColor event) {
        float f = fog() * 0.55F;
        if (f <= 0) {
            return;
        }
        event.setRed(event.getRed() + (0.30F - event.getRed()) * f);
        event.setGreen(event.getGreen() + (0.12F - event.getGreen()) * f);
        event.setBlue(event.getBlue() + (0.40F - event.getBlue()) * f);
    }

    @SubscribeEvent
    static void onFog(ViewportEvent.RenderFog event) {
        float f = fog();
        if (f <= 0 || event.getType() != FogType.NONE) {
            return;
        }
        event.scaleFarPlaneDistance(1.0F - 0.6F * f);
        event.scaleNearPlaneDistance(1.0F - 0.8F * f);
    }
}

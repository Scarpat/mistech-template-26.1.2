package com.mistech.mistechmod.celestial;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.mistech.mistechmod.Mistech;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

/**
 * Lado do cliente dos Eventos Celestes: guarda quais estão acesos (pacote do servidor), sobe/desce a intensidade
 * devagar e pendura as camadas de {@link CelestialAttributes} no {@link ClientLevel} do overworld (céu, névoa, luz).
 */
@EventBusSubscriber(modid = Mistech.MODID, value = Dist.CLIENT)
public final class CelestialClient {
    private static final float[] INTENSITY = new float[CelestialEvent.values().length];
    private static int mask;
    private static boolean warned;

    private CelestialClient() {}

    public static void handle(CelestialSyncPayload payload) {
        mask = payload.mask();
    }

    public static float intensity(CelestialEvent event) {
        return INTENSITY[event.ordinal()];
    }

    public static boolean isOn(CelestialEvent event) {
        return (mask & event.bit()) != 0;
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        ClientLevel level = Minecraft.getInstance().level;
        boolean overworld = level != null && level.dimension() == Level.OVERWORLD;
        for (CelestialEvent e : CelestialEvent.values()) {
            float target = overworld && (mask & e.bit()) != 0 ? 1.0F : 0.0F;
            float cur = INTENSITY[e.ordinal()];
            INTENSITY[e.ordinal()] = cur < target ? Math.min(target, cur + Celestial.FADE) : Math.max(target, cur - Celestial.FADE);
        }
    }

    /** O ClientLevel monta o sistema de atributos no construtor; aqui ele é refeito com as camadas do mod. */
    @SubscribeEvent
    static void onLevelLoad(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ClientLevel level) || level.dimension() != Level.OVERWORLD) {
            return;
        }
        java.util.Arrays.fill(INTENSITY, 0.0F);
        try {
            Method layers = ClientLevel.class.getDeclaredMethod("addEnvironmentAttributeLayers", EnvironmentAttributeSystem.Builder.class);
            layers.setAccessible(true);
            EnvironmentAttributeSystem.Builder builder = (EnvironmentAttributeSystem.Builder) layers.invoke(level, EnvironmentAttributeSystem.builder());
            CelestialAttributes.addLayers(builder, CelestialClient::intensity);
            Field field = ClientLevel.class.getDeclaredField("environmentAttributes");
            field.setAccessible(true);
            field.set(level, builder.build());
        } catch (ReflectiveOperationException | RuntimeException e) {
            if (!warned) {
                warned = true;
                Mistech.LOGGER.warn("Mistech: não deu para pendurar as cores dos Eventos Celestes no céu do cliente", e);
            }
        }
    }
}

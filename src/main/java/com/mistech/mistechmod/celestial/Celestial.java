package com.mistech.mistechmod.celestial;

import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.Mistech;

import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Eventos Celestes: janelas no relógio do overworld, velocidade do relógio (noite/dia mais longos),
 * intensidade de cada evento (para as camadas de {@link CelestialAttributes}) e sincronização com os clientes.
 * Os efeitos de jogabilidade ficam em {@link CelestialEffects}.
 */
@EventBusSubscriber(modid = Mistech.MODID)
public final class Celestial {
    public static final long DAY = 24000L;
    /** Faixa da noite em que o relógio anda mais devagar. */
    public static final int NIGHT_START = 13000;
    public static final int NIGHT_END = 23000;
    /** Velocidades do relógio. */
    public static final float LONG_RATE = 0.5F;
    public static final float ETERNAL_RATE = 0.2F;
    /** Quanto a intensidade anda por tick (0→1 em ~2,5 s). */
    public static final float FADE = 0.02F;

    private static final float[] INTENSITY = new float[CelestialEvent.values().length];
    private static int sentMask = -1;
    private static int currentMask;

    private Celestial() {}

    // ----- relógio -----

    public static Optional<Holder<WorldClock>> clock(MinecraftServer server) {
        return server.overworld().dimensionType().defaultClock();
    }

    /** Ticks totais do relógio do overworld (dias × 24000 + hora). */
    public static long now(MinecraftServer server) {
        return server.overworld().getDefaultClockTime();
    }

    public static int timeOfDay(long clockTicks) {
        return (int) Math.floorMod(clockTicks, DAY);
    }

    public static boolean isNightTime(long clockTicks) {
        int t = timeOfDay(clockTicks);
        return t >= NIGHT_START && t < NIGHT_END;
    }

    /** Esta noite (ou a próxima, se for dia): do anoitecer até o fim da madrugada. */
    public static CelestialData.Window tonight(long t) {
        int tod = timeOfDay(t);
        long base = t - tod;
        if (tod >= 23500) {
            base += DAY;
        }
        long start = Math.max(t, base + 12500);
        return new CelestialData.Window(start, base + 23500);
    }

    /** Hoje (ou amanhã, se já for fim de tarde/noite): do amanhecer até o pôr do sol. */
    public static CelestialData.Window today(long t) {
        int tod = timeOfDay(t);
        long base = t - tod;
        if (tod >= 12000) {
            base += DAY;
        }
        long start = Math.max(t, base + 500);
        return new CelestialData.Window(start, base + 12500);
    }

    /** De agora até o fim da {@code n}-ésima noite. */
    public static CelestialData.Window nights(long t, int n) {
        int tod = timeOfDay(t);
        long base = tod < 23500 ? t - tod : t - tod + DAY;
        return new CelestialData.Window(t, base + DAY * (n - 1) + 23500);
    }

    /** De agora até o fim do {@code n}-ésimo dia. */
    public static CelestialData.Window days(long t, int n) {
        int tod = timeOfDay(t);
        long base = tod < 12500 ? t - tod : t - tod + DAY;
        return new CelestialData.Window(t, base + DAY * (n - 1) + 12500);
    }

    // ----- API -----

    public static void start(MinecraftServer server, CelestialEvent event, CelestialData.Window window) {
        CelestialData data = CelestialData.get(server);
        switch (event) {
            case BLOOD_MOON -> data.windows.remove(CelestialEvent.SILVER_MOON);
            case SILVER_MOON -> {
                data.windows.remove(CelestialEvent.BLOOD_MOON);
                data.windows.remove(CelestialEvent.ETERNAL_NIGHT);
            }
            default -> {
            }
        }
        data.windows.put(event, window);
        data.setDirty();
        update(server, true);
    }

    public static void stop(MinecraftServer server, CelestialEvent event) {
        CelestialData data = CelestialData.get(server);
        if (data.windows.remove(event) != null) {
            if (event == CelestialEvent.BLOOD_MOON) {
                data.windows.remove(CelestialEvent.ETERNAL_NIGHT);
            }
            data.setDirty();
            update(server, true);
        }
    }

    public static void clear(MinecraftServer server) {
        CelestialData data = CelestialData.get(server);
        data.windows.clear();
        data.setDirty();
        update(server, true);
    }

    /** O evento está dentro da janela agora (mesmo que ainda não seja a hora dele aparecer)? */
    public static boolean isScheduled(MinecraftServer server, CelestialEvent event) {
        CelestialData.Window w = CelestialData.get(server).windows.get(event);
        return w != null && w.end() > now(server);
    }

    /** O evento está acontecendo agora (na hora certa)? Só vale para o overworld. */
    public static boolean isActive(@Nullable Level level, CelestialEvent event) {
        return level instanceof ServerLevel server && server.dimension() == Level.OVERWORLD && (currentMask & event.bit()) != 0;
    }

    /** Intensidade (0–1) no servidor. */
    public static float intensity(CelestialEvent event) {
        return INTENSITY[event.ordinal()];
    }

    public static List<CelestialEvent> scheduled(MinecraftServer server) {
        long t = now(server);
        return CelestialData.get(server).windows.entrySet().stream().filter(e -> e.getValue().end() > t).map(java.util.Map.Entry::getKey).toList();
    }

    /** Janela de um evento (para mensagens e comandos). */
    public static CelestialData.@Nullable Window window(MinecraftServer server, CelestialEvent event) {
        return CelestialData.get(server).windows.get(event);
    }

    // ----- tick -----

    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.overworld() == null) {
            return;
        }
        update(server, false);
        CelestialEffects.tick(server);
    }

    private static boolean phaseOk(CelestialEvent event, ServerLevel overworld, int tod) {
        boolean night = tod >= 12000 && tod < 23800;
        return switch (event) {
            case BLOOD_MOON, SILVER_MOON, STARFALL, AURORA, ETERNAL_NIGHT, LONG_NIGHT -> night;
            case ECLIPSE -> tod < 12500 || tod >= 23500;
            case SOLSTICE -> tod < 12500 || tod >= 23500;
            case BLESSED_RAIN -> overworld.isRaining();
        };
    }

    private static void update(MinecraftServer server, boolean force) {
        ServerLevel overworld = server.overworld();
        CelestialData data = CelestialData.get(server);
        long t = now(server);
        int tod = timeOfDay(t);

        // janelas vencidas saem; Noite Eterna só existe junto com a Lua de Sangue
        if (data.windows.values().removeIf(w -> w.end() <= t)) {
            data.setDirty();
        }
        if (data.windows.containsKey(CelestialEvent.ETERNAL_NIGHT) && !data.windows.containsKey(CelestialEvent.BLOOD_MOON)) {
            data.windows.remove(CelestialEvent.ETERNAL_NIGHT);
            data.setDirty();
        }

        int mask = 0;
        for (var entry : data.windows.entrySet()) {
            if (entry.getValue().contains(t) && phaseOk(entry.getKey(), overworld, tod)) {
                mask |= entry.getKey().bit();
            }
        }
        currentMask = mask;
        for (CelestialEvent e : CelestialEvent.values()) {
            float target = (mask & e.bit()) != 0 ? 1.0F : 0.0F;
            float cur = INTENSITY[e.ordinal()];
            INTENSITY[e.ordinal()] = cur < target ? Math.min(target, cur + FADE) : Math.max(target, cur - FADE);
        }
        if (force || mask != sentMask) {
            sentMask = mask;
            PacketDistributor.sendToAllPlayers(new CelestialSyncPayload(mask));
        }
        applyRate(server, data, t);
    }

    /** Noite Longa / Solstício / Noite Eterna mexem na velocidade do relógio (o NeoForge sincroniza com o cliente). */
    private static void applyRate(MinecraftServer server, CelestialData data, long t) {
        Optional<Holder<WorldClock>> clock = clock(server);
        if (clock.isEmpty()) {
            return;
        }
        boolean night = isNightTime(t);
        float factor = 1.0F;
        if (night && (currentMask & CelestialEvent.ETERNAL_NIGHT.bit()) != 0) {
            factor = ETERNAL_RATE;
        } else if (night && data.windows.containsKey(CelestialEvent.LONG_NIGHT) && data.windows.get(CelestialEvent.LONG_NIGHT).contains(t)) {
            factor = LONG_RATE;
        } else if (timeOfDay(t) < 12000 && data.windows.containsKey(CelestialEvent.SOLSTICE) && data.windows.get(CelestialEvent.SOLSTICE).contains(t)) {
            factor = LONG_RATE;
        }
        var manager = server.clockManager();
        float current = manager.getRate(clock.get());
        if (factor != 1.0F) {
            if (data.baseRate < 0) {
                data.baseRate = current;
                data.setDirty();
            }
            float want = data.baseRate * factor;
            if (Math.abs(current - want) > 1.0E-4F) {
                manager.setRate(clock.get(), want);
            }
        } else if (data.baseRate >= 0) {
            if (Math.abs(current - data.baseRate) > 1.0E-4F) {
                manager.setRate(clock.get(), data.baseRate);
            }
            data.baseRate = -1.0F;
            data.setDirty();
        }
    }

    // ----- camadas do servidor + sincronização -----

    /** No servidor, o overworld ganha as camadas dos Eventos Celestes (jogabilidade: sono, luz, monstros queimarem). */
    @SubscribeEvent
    static void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level && level.dimension() == Level.OVERWORLD) {
            EnvironmentAttributeSystem.Builder builder = EnvironmentAttributeSystem.builder().addDefaultLayers(level);
            level.setEnvironmentAttributes(CelestialAttributes.addLayers(builder, Celestial::intensity).build());
            java.util.Arrays.fill(INTENSITY, 0.0F);
            sentMask = -1;
        }
    }

    @SubscribeEvent
    static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PacketDistributor.sendToPlayer(player, new CelestialSyncPayload(currentMask));
        }
    }

    @SubscribeEvent
    static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PacketDistributor.sendToPlayer(player, new CelestialSyncPayload(currentMask));
        }
    }

    // ----- bênção (Exorcismo) -----

    public static final String BLESSED_TAG = "mistech_blessed_until";

    public static void bless(Player player, long ticks) {
        player.getPersistentData().putLong(BLESSED_TAG, player.level().getGameTime() + ticks);
    }

    /** Abençoado pelo Exorcismo: nem Mal Olhado nem Pisadeira chegam perto. */
    public static boolean isBlessed(Player player) {
        return player.getPersistentData().getLongOr(BLESSED_TAG, 0L) > player.level().getGameTime();
    }

    /** Sem Pisadeira: abençoado, ou na manhã depois de uma Lua de Prata. */
    public static boolean shieldsFromPisadeira(Player player) {
        if (isBlessed(player)) {
            return true;
        }
        MinecraftServer server = player.level().getServer();
        return server != null && player.level().dimension() == Level.OVERWORLD && isScheduled(server, CelestialEvent.SILVER_MOON);
    }
}

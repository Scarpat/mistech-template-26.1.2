package com.mistech.mistechmod.celestial;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.mistech.mistechmod.Mistech;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Estado dos Eventos Celestes (salvo no overworld).
 * <ul>
 * <li>{@code windows}: evento → janela [início, fim) em ticks do relógio do mundo (o mesmo que conta os dias).</li>
 * <li>{@code baseRate}: velocidade do relógio antes de um evento mexer nela (-1 = nada mexido).</li>
 * <li>{@code lures}: Chamarizes do Ermo ativos (até um gameTime).</li>
 * <li>{@code vigils}: Vigílias (chunks forçados até um gameTime).</li>
 * </ul>
 */
public class CelestialData extends SavedData {
    public record Window(long start, long end) {
        public static final Codec<Window> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.LONG.fieldOf("start").forGetter(Window::start),
                Codec.LONG.fieldOf("end").forGetter(Window::end)
        ).apply(i, Window::new));

        public boolean contains(long t) {
            return t >= this.start && t < this.end;
        }
    }

    /** Área com efeito até {@code until} (gameTime). */
    public record Zone(String dimension, BlockPos pos, long until) {
        public static final Codec<Zone> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("dimension").forGetter(Zone::dimension),
                BlockPos.CODEC.fieldOf("pos").forGetter(Zone::pos),
                Codec.LONG.fieldOf("until").forGetter(Zone::until)
        ).apply(i, Zone::new));
    }

    private record Packed(Map<CelestialEvent, Window> windows, float baseRate, List<Zone> lures, List<Zone> vigils) {}

    private static final Codec<Packed> PACKED = RecordCodecBuilder.create(i -> i.group(
            Codec.unboundedMap(CelestialEvent.CODEC, Window.CODEC).optionalFieldOf("windows", Map.of()).forGetter(Packed::windows),
            Codec.FLOAT.optionalFieldOf("base_rate", -1.0F).forGetter(Packed::baseRate),
            Zone.CODEC.listOf().optionalFieldOf("lures", List.of()).forGetter(Packed::lures),
            Zone.CODEC.listOf().optionalFieldOf("vigils", List.of()).forGetter(Packed::vigils)
    ).apply(i, Packed::new));

    public static final Codec<CelestialData> CODEC = PACKED.xmap(CelestialData::new, CelestialData::pack);

    public static final SavedDataType<CelestialData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(Mistech.MODID, "celestial_events"), CelestialData::new, CODEC);

    final Map<CelestialEvent, Window> windows = new EnumMap<>(CelestialEvent.class);
    float baseRate = -1.0F;
    final List<Zone> lures = new ArrayList<>();
    final List<Zone> vigils = new ArrayList<>();

    public CelestialData() {}

    private CelestialData(Packed packed) {
        this.windows.putAll(packed.windows());
        this.baseRate = packed.baseRate();
        this.lures.addAll(packed.lures());
        this.vigils.addAll(packed.vigils());
    }

    private Packed pack() {
        return new Packed(Map.copyOf(this.windows), this.baseRate, List.copyOf(this.lures), List.copyOf(this.vigils));
    }

    public static CelestialData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }
}

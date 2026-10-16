package com.mistech.mistechmod.realm;

import java.util.List;
import java.util.stream.Stream;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

/**
 * Biomas do Sonho (Dimensão dos Gnomos) — ver {@link DreamLayout}.
 * <ul>
 * <li>{@code rings}: 4 listas de biomas, uma por anel (Sonho Comum, Pesadelo, Sonho Profundo, Sono REM), na ordem de
 * {@link DreamLayout#subBiome}.</li>
 * <li>{@code edge}: a Borda do Sonho (últimos 1000 blocos antes de acordar).</li>
 * <li>{@code depths}: Profundezas dos Gnomos, embaixo de tudo — abaixo de {@code depths_y} nos anéis 1-2 e abaixo de
 * {@code island_depths_y} onde o chão desce (anéis 3+).</li>
 * </ul>
 * Os sub-biomas usam os ruídos de temperatura e vegetação do noise router ({@code mistech:dream_region_a/b}).
 */
public class GnomeRealmBiomeSource extends BiomeSource {
    public static final MapCodec<GnomeRealmBiomeSource> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Biome.CODEC.listOf().listOf().fieldOf("rings").forGetter(s -> s.rings),
            Biome.CODEC.fieldOf("depths").forGetter(s -> s.depths),
            Biome.CODEC.fieldOf("edge").forGetter(s -> s.edge),
            Codec.INT.fieldOf("depths_y").orElse(48).forGetter(s -> s.depthsY),
            Codec.INT.fieldOf("island_depths_y").orElse(12).forGetter(s -> s.islandDepthsY)
    ).apply(i, GnomeRealmBiomeSource::new));

    private final List<List<Holder<Biome>>> rings;
    private final Holder<Biome> depths;
    private final Holder<Biome> edge;
    private final int depthsY;
    private final int islandDepthsY;

    public GnomeRealmBiomeSource(List<List<Holder<Biome>>> rings, Holder<Biome> depths, Holder<Biome> edge, int depthsY, int islandDepthsY) {
        if (rings.size() != 4 || rings.stream().anyMatch(List::isEmpty)) {
            throw new IllegalArgumentException("gnome_realm: 'rings' precisa de 4 listas de biomas não vazias");
        }
        this.rings = rings;
        this.depths = depths;
        this.edge = edge;
        this.depthsY = depthsY;
        this.islandDepthsY = islandDepthsY;
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return Stream.concat(this.rings.stream().flatMap(List::stream), Stream.of(this.depths, this.edge)).distinct();
    }

    @Override
    public Holder<Biome> getNoiseBiome(int quartX, int quartY, int quartZ, Climate.Sampler sampler) {
        int x = QuartPos.toBlock(quartX), y = QuartPos.toBlock(quartY), z = QuartPos.toBlock(quartZ);
        int floor = DreamLayout.islandWeight(x, z) > 0.5 ? this.islandDepthsY : this.depthsY;
        if (y < floor) {
            return this.depths;
        }
        int ring = DreamLayout.ring(x, z);
        if (ring >= DreamLayout.RING_EDGE) {
            return this.edge;
        }
        Climate.TargetPoint point = sampler.sample(quartX, quartY, quartZ);
        double t = Climate.unquantizeCoord(point.temperature());
        double v = Climate.unquantizeCoord(point.humidity());
        List<Holder<Biome>> list = this.rings.get(ring - 1);
        int index = DreamLayout.subBiome(ring, x, z, t, v);
        return list.get(Math.min(index, list.size() - 1));
    }

    /** No Sonho Comum (anel 1)? */
    public static boolean isCommonDream(int x, int z) {
        return DreamLayout.ring(x, z) == DreamLayout.RING_COMMON;
    }
}

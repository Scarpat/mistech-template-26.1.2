package com.mistech.mistechmod.realm;

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
 * Biomas da Dimensão dos Gnomos pela distância do centro (0,0):
 * até {@code radius} blocos = Prado dos Gnomos (verde); depois = Ermo Turvo (cinza).
 * Abaixo de {@code depthsY} = Profundezas dos Gnomos (cavernas cheias de minério), em qualquer lugar.
 * A borda do prado ondula um pouco para não ficar um círculo perfeito.
 */
public class GnomeRealmBiomeSource extends BiomeSource {
    public static final MapCodec<GnomeRealmBiomeSource> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Biome.CODEC.fieldOf("meadow").forGetter(s -> s.meadow),
            Biome.CODEC.fieldOf("wastes").forGetter(s -> s.wastes),
            Biome.CODEC.fieldOf("depths").forGetter(s -> s.depths),
            Codec.intRange(16, 4096).fieldOf("radius").orElse(112).forGetter(s -> s.radius),
            Codec.INT.fieldOf("depths_y").orElse(48).forGetter(s -> s.depthsY)
    ).apply(i, GnomeRealmBiomeSource::new));

    private final Holder<Biome> meadow;
    private final Holder<Biome> wastes;
    private final Holder<Biome> depths;
    private final int radius;
    private final int depthsY;

    public GnomeRealmBiomeSource(Holder<Biome> meadow, Holder<Biome> wastes, Holder<Biome> depths, int radius, int depthsY) {
        this.meadow = meadow;
        this.wastes = wastes;
        this.depths = depths;
        this.radius = radius;
        this.depthsY = depthsY;
    }

    public int radius() {
        return this.radius;
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return Stream.of(this.meadow, this.wastes, this.depths);
    }

    @Override
    public Holder<Biome> getNoiseBiome(int quartX, int quartY, int quartZ, Climate.Sampler sampler) {
        int x = QuartPos.toBlock(quartX), y = QuartPos.toBlock(quartY), z = QuartPos.toBlock(quartZ);
        if (y < this.depthsY) {
            return this.depths;
        }
        return isMeadow(x, z, this.radius) ? this.meadow : this.wastes;
    }

    /** Dentro do prado verde? (mesma conta usada pelo Mal Olhado). */
    public static boolean isMeadow(int x, int z, int radius) {
        double angle = Math.atan2(z, x);
        double wobble = Math.sin(angle * 5.0) * 10.0 + Math.sin(angle * 3.0 + 1.3) * 6.0;
        return x * (double) x + z * (double) z < (radius + wobble) * (radius + wobble);
    }
}

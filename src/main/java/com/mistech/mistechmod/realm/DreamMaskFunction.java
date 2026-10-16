package com.mistech.mistechmod.realm;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * Density function {@code mistech:dream_mask}: pesos 0..1 vindos da geometria do Sonho ({@link DreamLayout}).
 * <ul>
 * <li>{@code "kind": "islands"} — 0 no relevo normal (anéis 1-2), 1 onde o chão vira ilhas flutuantes (anéis 3+).</li>
 * <li>{@code "kind": "ring", "ring": N, "fade": F} — 1 dentro do anel N, com borda suave de F blocos.</li>
 * </ul>
 */
public record DreamMaskFunction(String kind, int ring, double fade) implements DensityFunction.SimpleFunction {
    public static final MapCodec<DreamMaskFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("kind").forGetter(DreamMaskFunction::kind),
            Codec.INT.optionalFieldOf("ring", 0).forGetter(DreamMaskFunction::ring),
            Codec.DOUBLE.optionalFieldOf("fade", 32.0).forGetter(DreamMaskFunction::fade)
    ).apply(i, DreamMaskFunction::new));
    public static final KeyDispatchDataCodec<DreamMaskFunction> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    @Override
    public double compute(DensityFunction.FunctionContext context) {
        if ("islands".equals(this.kind)) {
            return DreamLayout.islandWeight(context.blockX(), context.blockZ());
        }
        return DreamLayout.ringMask(context.blockX(), context.blockZ(), this.ring, this.fade);
    }

    @Override
    public double minValue() {
        return 0.0;
    }

    @Override
    public double maxValue() {
        return 1.0;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }
}

package com.mistech.mistechmod.realm;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * Density function {@code mistech:dream_pillow}: peso 0..1 das Colinas de Travesseiro (Sonho Comum), usando o mesmo
 * {@link DreamLayout#commonSector} do BiomeSource. {@code input} = o ruído de região (temperatura do router).
 */
public record DreamPillowFunction(DensityFunction input) implements DensityFunction {
    public static final MapCodec<DreamPillowFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("input").forGetter(DreamPillowFunction::input)
    ).apply(i, DreamPillowFunction::new));
    public static final KeyDispatchDataCodec<DreamPillowFunction> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    @Override
    public double compute(DensityFunction.FunctionContext context) {
        int x = context.blockX(), z = context.blockZ();
        double ring = DreamLayout.ringMask(x, z, DreamLayout.RING_COMMON, 32.0);
        if (ring <= 0.0) {
            return 0.0;
        }
        double fromCenter = Mth.clamp((DreamLayout.distance(x, z) - DreamLayout.CENTER) / 30.0, 0.0, 1.0);
        if (fromCenter <= 0.0) {
            return 0.0;
        }
        double sector = DreamLayout.commonSector(x, z, this.input.compute(context));
        return Mth.clamp((-sector - 0.6) * 4.0, 0.0, 1.0) * ring * fromCenter;
    }

    @Override
    public void fillArray(double[] array, DensityFunction.ContextProvider provider) {
        provider.fillAllDirectly(array, this);
    }

    @Override
    public DensityFunction mapAll(DensityFunction.Visitor visitor) {
        return visitor.apply(new DreamPillowFunction(this.input.mapAll(visitor)));
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

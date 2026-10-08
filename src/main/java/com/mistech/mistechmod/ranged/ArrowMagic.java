package com.mistech.mistechmod.ranged;

import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.item.RuneElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Magia guardada numa flecha (attachment {@code mistech:arrow_magic} na entidade): força de cada elemento e se o tiro foi
 * agachado (a Runa do Espaço só teleporta o atirador nesse caso, quando vem do arco).
 * <ul>
 * <li>Flecha Rúnica: o elemento dela com força {@link #RUNE_ARROW_POWER}.</li>
 * <li>Arcos de mitril e acima: cada runa engastada vale 1 (selada 1,5).</li>
 * <li>Arco Turvo: o elemento de maior afinidade do atirador, força {@link #AFFINITY_POWER}.</li>
 * </ul>
 * Efeitos em {@link RangedEvents}.
 */
public record ArrowMagic(Map<String, Double> powers, boolean sneaking) {
    public static final double RUNE_ARROW_POWER = 3.0;
    public static final double AFFINITY_POWER = 0.5;

    public static final ArrowMagic EMPTY = new ArrowMagic(Map.of(), false);

    public static final MapCodec<ArrowMagic> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.unboundedMap(Codec.STRING, Codec.DOUBLE).optionalFieldOf("powers", Map.of()).forGetter(ArrowMagic::powers),
            Codec.BOOL.optionalFieldOf("sneaking", false).forGetter(ArrowMagic::sneaking)
    ).apply(i, ArrowMagic::new));

    public boolean isEmpty() {
        return this.powers.isEmpty();
    }

    public double power(RuneElement element) {
        return this.powers.getOrDefault(element.id(), 0.0);
    }

    public ArrowMagic with(RuneElement element, double power) {
        if (power <= 0.0) {
            return this;
        }
        Map<String, Double> map = new HashMap<>(this.powers);
        map.merge(element.id(), power, Double::sum);
        return new ArrowMagic(Map.copyOf(map), this.sneaking);
    }

    public ArrowMagic withAll(Map<RuneElement, Double> tally) {
        ArrowMagic out = this;
        for (Map.Entry<RuneElement, Double> e : tally.entrySet()) {
            out = out.with(e.getKey(), e.getValue());
        }
        return out;
    }

    public ArrowMagic sneaking(boolean value) {
        return new ArrowMagic(this.powers, value);
    }

    /** Elemento mais forte (cor do rastro). */
    public @Nullable RuneElement strongest() {
        RuneElement best = null;
        double max = 0.0;
        for (Map.Entry<String, Double> e : this.powers.entrySet()) {
            RuneElement element = RuneElement.byId(e.getKey());
            if (element != null && e.getValue() > max) {
                max = e.getValue();
                best = element;
            }
        }
        return best;
    }
}

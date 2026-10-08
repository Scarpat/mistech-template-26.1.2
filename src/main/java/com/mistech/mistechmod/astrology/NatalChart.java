package com.mistech.mistechmod.astrology;

import com.mistech.mistechmod.item.RuneElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.RandomSource;

/**
 * Mapa astral de um jogador: signo solar, lunar e ascendente (índices em {@link Constellations#ALL}, 0 = Áries).
 * Sorteado escondido na primeira entrada no mundo; cada parte é revelada na Mesa de Astrologia.
 * {@code revealed} é uma máscara de bits (1 = Sol, 2 = Lua, 4 = Ascendente). Imutável.
 *
 * O {@link #SYNC_CODEC} só manda ao cliente o que já foi revelado (o resto vai como -1),
 * então o jogador não consegue espiar o próprio mapa.
 */
public record NatalChart(int sun, int moon, int rising, int revealed) {
    public static final NatalChart UNROLLED = new NatalChart(-1, -1, -1, 0);

    /** As três partes do mapa, na ordem em que costumam ser reveladas. */
    public enum Part {
        SUN("sun", 1), MOON("moon", 2), RISING("rising", 4);

        private final String id;
        private final int bit;

        Part(String id, int bit) {
            this.id = id;
            this.bit = bit;
        }

        public String id() {
            return this.id;
        }

        public int bit() {
            return this.bit;
        }

        public Component displayName() {
            return Component.translatable("natal.mistech.part." + this.id);
        }

        public static Part byIndex(int index) {
            Part[] values = values();
            return values[Math.floorMod(index, values.length)];
        }
    }

    public static final MapCodec<NatalChart> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.fieldOf("sun").forGetter(NatalChart::sun),
            Codec.INT.fieldOf("moon").forGetter(NatalChart::moon),
            Codec.INT.fieldOf("rising").forGetter(NatalChart::rising),
            Codec.INT.optionalFieldOf("revealed", 0).forGetter(NatalChart::revealed)
    ).apply(i, NatalChart::new));

    /** Para a rede: esconde o que ainda não foi revelado. */
    public static final StreamCodec<ByteBuf, NatalChart> SYNC_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, c -> c.isRevealed(Part.SUN) ? c.sun : -1,
            ByteBufCodecs.VAR_INT, c -> c.isRevealed(Part.MOON) ? c.moon : -1,
            ByteBufCodecs.VAR_INT, c -> c.isRevealed(Part.RISING) ? c.rising : -1,
            ByteBufCodecs.VAR_INT, NatalChart::revealed,
            NatalChart::new);

    public static NatalChart roll(RandomSource random) {
        int signs = Constellations.ALL.size();
        return new NatalChart(random.nextInt(signs), random.nextInt(signs), random.nextInt(signs), 0);
    }

    public boolean isRolled() {
        return this.sun >= 0 && this.moon >= 0 && this.rising >= 0 || this.revealed != 0;
    }

    public boolean isRevealed(Part part) {
        return (this.revealed & part.bit()) != 0;
    }

    public NatalChart reveal(Part part) {
        return new NatalChart(this.sun, this.moon, this.rising, this.revealed | part.bit());
    }

    /** Signo daquela parte (-1 = desconhecido no cliente). */
    public int sign(Part part) {
        return switch (part) {
            case SUN -> this.sun;
            case MOON -> this.moon;
            case RISING -> this.rising;
        };
    }

    // ----- signos -----

    /** Elemento do signo: Fogo (Áries, Leão, Sagitário), Terra, Ar e Água, na ordem do zodíaco. */
    public static RuneElement elementOf(int sign) {
        return switch (Math.floorMod(sign, 4)) {
            case 0 -> RuneElement.FIRE;
            case 1 -> RuneElement.EARTH;
            case 2 -> RuneElement.AIR;
            default -> RuneElement.WATER;
        };
    }

    /** Símbolo do signo (♈ … ♓). */
    public static String glyph(int sign) {
        return sign < 0 ? "?" : String.valueOf((char) (0x2648 + sign));
    }

    public static Component signName(int sign) {
        return sign < 0 ? Component.literal("???") : Constellations.get(sign).name();
    }
}

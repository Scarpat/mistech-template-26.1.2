package com.mistech.mistechmod.archive.craft;

import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.UUIDUtil;

/** Um trabalho da fila do Núcleo: fazer {@code remaining} vezes o padrão. */
public final class CraftJob {
    public static final Codec<CraftJob> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(j -> j.id),
            Pattern.CODEC.fieldOf("pattern").forGetter(j -> j.pattern),
            Codec.INT.fieldOf("remaining").forGetter(j -> j.remaining)).apply(i, CraftJob::new));

    public final UUID id;
    public final Pattern pattern;
    public int remaining;
    /** Quantos estão com golens agora (não é salvo). */
    public transient int claimed;

    public CraftJob(UUID id, Pattern pattern, int remaining) {
        this.id = id;
        this.pattern = pattern;
        this.remaining = remaining;
    }

    public static CraftJob of(Pattern pattern, int times) {
        return new CraftJob(UUID.randomUUID(), pattern, times);
    }

    public int free() {
        return this.remaining - this.claimed;
    }
}

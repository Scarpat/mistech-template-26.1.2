package com.mistech.mistechmod.purity.area;

import java.util.HashMap;
import java.util.Map;

import com.mistech.mistechmod.affinity.Affinity;
import com.mistech.mistechmod.item.RuneElement;
import com.mistech.mistechmod.purity.PurityContent;
import com.mistech.mistechmod.purity.Taint;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Mácula do lugar (Fase 4): um valor de 0 a 100 em cada chunk, com as mesmas faixas da Mácula do jogador.
 * Máquinas impuras, rituais e o Conjurador com magia sombria soltam; o Filtro de Arruda, o Gerador Purificado,
 * a magia Purificar e o ritual Consagrar Solo limpam. Os chunks com Mácula ficam num conjunto "sujo" por mundo
 * e só esses são processados (a cada minuto: espalhar, sumir e efeitos), ver {@link AreaTaintEvents}.
 */
public final class AreaTaint {
    /** Acima disto o chunk passa parte do excesso para os vizinhos. */
    public static final float SPREAD_ABOVE = 50.0F;
    public static final float SPREAD_FRACTION = 0.10F;
    /** Abaixo disto a Mácula some sozinha com sol. */
    public static final float FADE_BELOW = 20.0F;
    public static final float FADE_PER_MINUTE = 0.5F;

    /** Chunks com Mácula em cada mundo (só os carregados). */
    private static final Map<ResourceKey<Level>, LongOpenHashSet> DIRTY = new HashMap<>();

    private AreaTaint() {}

    // ----- leitura -----

    public static float get(LevelChunk chunk) {
        return chunk.getData(PurityContent.AREA_TAINT);
    }

    /** Mácula do chunk da posição (0 no cliente, onde o valor não é enviado; use {@code AreaTaintClient}). */
    public static float get(Level level, BlockPos pos) {
        if (level.isClientSide()) {
            return 0.0F;
        }
        LevelChunk chunk = loaded(level, pos.getX() >> 4, pos.getZ() >> 4);
        return chunk == null ? 0.0F : get(chunk);
    }

    public static Taint.Stage stage(Level level, BlockPos pos) {
        return Taint.Stage.of(get(level, pos));
    }

    /** Chunk já carregado (não carrega nada). */
    public static LevelChunk loaded(Level level, int chunkX, int chunkZ) {
        if (level instanceof ServerLevel server) {
            return server.getChunkSource().getChunkNow(chunkX, chunkZ);
        }
        return null;
    }

    // ----- escrita -----

    public static void set(ServerLevel level, LevelChunk chunk, float value) {
        float clamped = Math.max(0.0F, Math.min(Taint.MAX, value));
        if (clamped < 0.01F) {
            clamped = 0.0F;
        }
        if (clamped == get(chunk)) {
            return;
        }
        chunk.setData(PurityContent.AREA_TAINT, clamped);
        chunk.markUnsaved();
        LongOpenHashSet set = DIRTY.computeIfAbsent(level.dimension(), k -> new LongOpenHashSet());
        if (clamped > 0) {
            set.add(chunk.getPos().pack());
        } else {
            set.remove(chunk.getPos().pack());
        }
    }

    /** Soma (ou tira, se negativo) Mácula no chunk da posição. */
    public static void add(ServerLevel level, BlockPos pos, float amount) {
        if (amount == 0 || !com.mistech.mistechmod.Config.areaTaint()) {
            return;
        }
        LevelChunk chunk = loaded(level, pos.getX() >> 4, pos.getZ() >> 4);
        if (chunk != null) {
            set(level, chunk, get(chunk) + amount);
        }
    }

    /** Tira Mácula do chunk da posição e dos chunks num raio (em chunks). @return quanto tirou no total */
    public static float clean(ServerLevel level, BlockPos pos, int chunkRadius, float amount) {
        float removed = 0;
        int cx = pos.getX() >> 4, cz = pos.getZ() >> 4;
        for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
            for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                LevelChunk chunk = loaded(level, cx + dx, cz + dz);
                if (chunk == null) {
                    continue;
                }
                float before = get(chunk);
                if (before > 0) {
                    set(level, chunk, before - amount);
                    removed += before - get(chunk);
                }
            }
        }
        return removed;
    }

    /** Maior Mácula num raio (em chunks): {chunkX, chunkZ, valor×100}. */
    public static int[] worstAround(ServerLevel level, BlockPos pos, int chunkRadius) {
        int cx = pos.getX() >> 4, cz = pos.getZ() >> 4;
        int[] best = {cx, cz, 0};
        for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
            for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                LevelChunk chunk = loaded(level, cx + dx, cz + dz);
                if (chunk != null) {
                    int value = Math.round(get(chunk) * 100);
                    if (value > best[2]) {
                        best = new int[] {cx + dx, cz + dz, value};
                    }
                }
            }
        }
        return best;
    }

    // ----- conjunto de chunks sujos -----

    static LongOpenHashSet dirty(ServerLevel level) {
        return DIRTY.computeIfAbsent(level.dimension(), k -> new LongOpenHashSet());
    }

    static void forget(ResourceKey<Level> dimension) {
        DIRTY.remove(dimension);
    }

    static void forget(ServerLevel level, ChunkPos pos) {
        LongOpenHashSet set = DIRTY.get(level.dimension());
        if (set != null) {
            set.remove(pos.pack());
        }
    }

    // ----- efeitos que outros sistemas consultam -----

    /** Faixa usada pelos Presságios: a pior entre a Mácula do jogador e a do lugar. */
    public static Taint.Stage omenStage(Player player) {
        Taint.Stage own = Taint.stage(player);
        Taint.Stage place = stage(player.level(), player.blockPosition());
        return place.ordinal() > own.ordinal() ? place : own;
    }

    /** Magia "impura": tem Escuridão entre os elementos básicos (Escuridão, Vazio, Sangue...). */
    public static boolean isImpure(RuneElement element) {
        return element == RuneElement.DARKNESS || element == RuneElement.VOID || Affinity.basics(element).contains(RuneElement.DARKNESS);
    }

    /** Multiplicador do poder de magia: impuras +20% num lugar Profano. */
    public static double spellPower(Player player, RuneElement element) {
        if (player.level().isClientSide() || !isImpure(element)) {
            return 1.0;
        }
        return stage(player.level(), player.blockPosition()) == Taint.Stage.PROFANE ? 1.2 : 1.0;
    }

    /** Multiplicador de velocidade das máquinas: −20% num lugar Profano. */
    public static double machineSpeed(Level level, BlockPos pos) {
        return stage(level, pos) == Taint.Stage.PROFANE ? 0.8 : 1.0;
    }

    /** Mácula pessoal por minuto de quem fica no chunk, pela faixa do lugar. */
    public static float personalPerMinute(Taint.Stage stage) {
        return switch (stage) {
            case PURE -> 0.0F;
            case STAINED -> 0.1F;
            case TAINTED -> 0.3F;
            case CORRUPTED -> 0.6F;
            case PROFANE -> 1.0F;
        };
    }
}

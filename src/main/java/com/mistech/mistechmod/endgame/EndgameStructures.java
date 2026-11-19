package com.mistech.mistechmod.endgame;

import java.util.ArrayList;
import java.util.List;

import com.mistech.mistechmod.registry.AltarContent;
import com.mistech.mistechmod.registry.DreamRealmContent;
import com.mistech.mistechmod.registry.OreContent;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * As estruturas da Fase 9 (só leem blocos: valem no cliente para o holograma da Pedra Guia).
 * <p>
 * <b>Forja Estelar</b> (coração no meio, y = 0): chão 5×5 de blocos de Oricalco ou Adamantina em y = −1; Obeliscos de Mitril nos
 * 4 cantos (±2, 0, ±2); Braseiros de Oricalco no meio das 4 bordas; vidro em cima do coração, e céu aberto acima do vidro.
 * <p>
 * <b>Reator Onírico</b> (núcleo no meio): cubo 3×3×3 em volta — Pedra Estelar nos 8 cantos, Lã de Nuvem no resto, e a face
 * da frente (para onde o núcleo olha) aberta no meio.
 */
public final class EndgameStructures {
    private EndgameStructures() {}

    public enum Kind {
        FORGE_FLOOR, OBELISK, BRAZIER, GLASS, STAR_STONE, CLOUD_WOOL
    }

    public record Part(BlockPos pos, Kind kind) {}

    public static boolean matches(Level level, Part part) {
        BlockState s = level.getBlockState(part.pos());
        return switch (part.kind()) {
            case FORGE_FLOOR -> s.is(OreContent.ORICHALCUM_BLOCK.get()) || s.is(OreContent.ADAMANTINE_BLOCK.get());
            case OBELISK -> s.is(AltarContent.MITHRIL_OBELISK.get());
            case BRAZIER -> s.is(AltarContent.ORICHALCUM_BRAZIER.get());
            case GLASS -> s.is(Blocks.GLASS) || s.is(net.minecraft.tags.BlockTags.IMPERMEABLE);
            case STAR_STONE -> s.is(DreamRealmContent.STAR_STONE.get());
            case CLOUD_WOOL -> s.is(DreamRealmContent.CLOUD_WOOL.get());
        };
    }

    public static List<Part> forge(BlockPos heart) {
        List<Part> parts = new ArrayList<>();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                parts.add(new Part(heart.offset(dx, -1, dz), Kind.FORGE_FLOOR));
                if (Math.abs(dx) == 2 && Math.abs(dz) == 2) {
                    parts.add(new Part(heart.offset(dx, 0, dz), Kind.OBELISK));
                } else if (Math.abs(dx) + Math.abs(dz) == 2 && (dx == 0 || dz == 0)) {
                    parts.add(new Part(heart.offset(dx, 0, dz), Kind.BRAZIER));
                }
            }
        }
        parts.add(new Part(heart.above(), Kind.GLASS));
        return parts;
    }

    public static List<Part> reactor(BlockPos core, Direction front) {
        List<Part> parts = new ArrayList<>();
        BlockPos opening = core.relative(front);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    BlockPos p = core.offset(dx, dy, dz);
                    if (p.equals(opening)) {
                        continue;
                    }
                    boolean corner = dx != 0 && dy != 0 && dz != 0;
                    parts.add(new Part(p, corner ? Kind.STAR_STONE : Kind.CLOUD_WOOL));
                }
            }
        }
        return parts;
    }

    /** Quantas peças faltam (0 = montada). */
    public static int missing(Level level, List<Part> parts) {
        int n = 0;
        for (Part p : parts) {
            if (!matches(level, p)) {
                n++;
            }
        }
        return n;
    }
}

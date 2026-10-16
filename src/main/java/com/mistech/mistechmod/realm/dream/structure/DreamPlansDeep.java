package com.mistech.mistechmod.realm.dream.structure;

import java.util.Random;

import com.mistech.mistechmod.realm.goblin.Blueprint;
import com.mistech.mistechmod.registry.DreamDeepContent;
import com.mistech.mistechmod.registry.DreamRealmContent;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Estruturas dos anéis 3 e 4 do Sonho.
 * <ul>
 * <li><b>Templo Fractal</b> (Sonho Profundo): uma Esponja de Menger de 27 blocos pousada na ilha — ametista, púrpura e madeira fractal,
 * com as arestas dos cubos de 9 em lanternas-do-mar. Uma rampa sobe até o túnel do lado oeste; no meio oco, um piso de ametista,
 * o baú, cristais e Mariposas-Vitral.</li>
 * <li><b>Biblioteca das Memórias</b> (Sono REM): torre de três andares empilhados tortos (cada andar deslocado para um lado),
 * paredes de estante, andaimes para subir, velas e lampiões de alma; Memórias pelos andares e um Vigia no térreo. No último andar,
 * a sala de ponta-cabeça e o baú com livros de lembranças.</li>
 * </ul>
 */
final class DreamPlansDeep {
    private DreamPlansDeep() {}

    // ================================================================== Templo Fractal

    static final int SPONGE = 27;

    /** Esponja de Menger: some onde, em alguma escala, dois dos três "dígitos" (base 3) são o do meio. */
    static boolean menger(int x, int y, int z) {
        for (int s = 1; s < SPONGE; s *= 3) {
            int a = (x / s) % 3 == 1 ? 1 : 0, b = (y / s) % 3 == 1 ? 1 : 0, c = (z / s) % 3 == 1 ? 1 : 0;
            if (a + b + c >= 2) {
                return false;
            }
        }
        return true;
    }

    static void fractalTemple(Blueprint bp, Random rnd, BlockPos center) {
        int x0 = center.getX() - SPONGE / 2, y0 = center.getY(), z0 = center.getZ() - SPONGE / 2;
        BlockState[] cells = {Blocks.AMETHYST_BLOCK.defaultBlockState(), Blocks.PURPUR_BLOCK.defaultBlockState(),
                DreamRealmContent.FRACTAL_PLANKS.get().defaultBlockState()};
        BlockState edge = Blocks.SEA_LANTERN.defaultBlockState();
        bp.shape(x0, y0, z0, x0 + SPONGE - 1, y0 + SPONGE - 1, z0 + SPONGE - 1, (x, y, z) -> menger(x - x0, y - y0, z - z0), (x, y, z) -> {
            int lx = x - x0, ly = y - y0, lz = z - z0;
            int border = (lx % 9 == 0 || lx % 9 == 8 ? 1 : 0) + (ly % 9 == 0 || ly % 9 == 8 ? 1 : 0) + (lz % 9 == 0 || lz % 9 == 8 ? 1 : 0);
            if (border >= 2) {
                return edge;
            }
            return cells[(lx / 3 + ly / 3 + lz / 3) % 3];
        });
        // pés: as colunas dos cantos descem até achar chão (no máximo 40)
        for (int cx : new int[] {x0, x0 + SPONGE - 3}) {
            for (int cz : new int[] {z0, z0 + SPONGE - 3}) {
                pillarDown(bp, cx, y0 - 1, cz, 3, Blocks.PURPUR_PILLAR.defaultBlockState());
            }
        }
        // piso de ametista no meio oco (fecha o túnel de baixo)
        int c = SPONGE / 2;
        bp.fill(x0 + 9, y0 + 8, z0 + 9, x0 + 17, y0 + 8, z0 + 17, Blocks.AMETHYST_BLOCK.defaultBlockState());
        bp.chest(x0 + c, y0 + 9, z0 + c, Direction.WEST, Blueprint.loot("chests/fractal_temple"));
        for (int i = 0; i < 10; i++) {
            int x = x0 + 9 + rnd.nextInt(9), z = z0 + 9 + rnd.nextInt(9);
            if (Math.abs(x - x0 - c) + Math.abs(z - z0 - c) > 1) {
                bp.block(x, y0 + 9, z, (rnd.nextBoolean() ? Blocks.AMETHYST_CLUSTER : Blocks.LARGE_AMETHYST_BUD).defaultBlockState()
                        .setValue(AmethystClusterBlock.FACING, Direction.UP));
            }
        }
        for (int[] e : new int[][] {{9, 9}, {17, 9}, {9, 17}, {17, 17}}) {
            bp.block(x0 + e[0], y0 + 9, z0 + e[1], Blocks.END_ROD.defaultBlockState());
        }
        for (int i = 0; i < 3; i++) {
            bp.mob(x0 + c + 0.5 + rnd.nextInt(5) - 2, y0 + 12, z0 + c + 0.5 + rnd.nextInt(5) - 2, DreamDeepContent.PRISM_MOTH, null);
        }
        // rampa do lado oeste até o túnel do meio (o chão do túnel fica em y0 + 9)
        int rz = z0 + c;
        for (int k = 0; k < 9; k++) {
            int x = x0 - 9 + k, y = y0 + k + 1;
            BlockState step = Blocks.PURPUR_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST);
            bp.fill(x, y, rz - 1, x, y, rz + 1, step);
            pillarDown(bp, x, y - 1, rz - 1, 1, Blocks.PURPUR_BLOCK.defaultBlockState());
            pillarDown(bp, x, y - 1, rz + 1, 1, Blocks.PURPUR_BLOCK.defaultBlockState());
        }
        bp.fill(x0, y0 + 9, rz - 1, x0 + 8, y0 + 9, rz + 1, Blocks.PURPUR_SLAB.defaultBlockState());
    }

    /** Coluna {@code w}×{@code w} que desce de {@code y} até o chão (para em sólido; no máximo 40 blocos). */
    static void pillarDown(Blueprint bp, int x, int y, int z, int w, BlockState state) {
        bp.custom(new BoundingBox(x, y - 40, z, x + w - 1, y, z + w - 1), ctx -> {
            for (int dx = 0; dx < w; dx++) {
                for (int dz = 0; dz < w; dz++) {
                    if (!ctx.inside(x + dx, y, z + dz)) {
                        continue;
                    }
                    for (int yy = y; yy > y - 40; yy--) {
                        BlockState here = ctx.get(x + dx, yy, z + dz);
                        if (!here.isAir() && !here.canBeReplaced()) {
                            break;
                        }
                        ctx.set(x + dx, yy, z + dz, state);
                    }
                }
            }
        });
    }

    // ================================================================== Biblioteca das Memórias

    static final int HALF = 7;
    static final int STORY = 7;
    static final int[][] SHIFT = {{0, 0}, {3, -2}, {-1, 3}};

    static void memoryLibrary(Blueprint bp, Random rnd, BlockPos center) {
        int y0 = center.getY();
        BlockState wall = Blocks.DARK_OAK_PLANKS.defaultBlockState();
        BlockState log = Blocks.DARK_OAK_LOG.defaultBlockState();
        BlockState floor = Blocks.DARK_OAK_PLANKS.defaultBlockState();
        BlockState shelf = Blocks.BOOKSHELF.defaultBlockState();
        // fundação sob o térreo
        for (int x = -HALF; x <= HALF; x += 3) {
            for (int z = -HALF; z <= HALF; z += 3) {
                pillarDown(bp, center.getX() + x, y0 - 1, center.getZ() + z, 1, Blocks.DEEPSLATE_BRICKS.defaultBlockState());
            }
        }
        for (int k = 0; k < 3; k++) {
            int cx = center.getX() + SHIFT[k][0], cz = center.getZ() + SHIFT[k][1], fy = y0 + k * STORY;
            int ax = cx - HALF, bx = cx + HALF, az = cz - HALF, bz = cz + HALF;
            // piso, paredes, quinas de tronco
            bp.fill(ax, fy, az, bx, fy, bz, floor);
            bp.air(ax + 1, fy + 1, az + 1, bx - 1, fy + STORY - 1, bz - 1);
            bp.fill(ax, fy + 1, az, bx, fy + STORY - 1, az, wall);
            bp.fill(ax, fy + 1, bz, bx, fy + STORY - 1, bz, wall);
            bp.fill(ax, fy + 1, az, ax, fy + STORY - 1, bz, wall);
            bp.fill(bx, fy + 1, az, bx, fy + STORY - 1, bz, wall);
            for (int[] q : new int[][] {{ax, az}, {bx, az}, {ax, bz}, {bx, bz}}) {
                bp.fill(q[0], fy, q[1], q[0], fy + STORY, q[1], log);
            }
            // estantes por dentro das paredes (com vãos para janelas)
            bp.fill(ax + 1, fy + 1, az + 1, bx - 1, fy + 4, az + 1, shelf);
            bp.fill(ax + 1, fy + 1, bz - 1, bx - 1, fy + 4, bz - 1, shelf);
            bp.fill(ax + 1, fy + 1, az + 1, ax + 1, fy + 4, bz - 1, shelf);
            bp.fill(bx - 1, fy + 1, az + 1, bx - 1, fy + 4, bz - 1, shelf);
            for (int w = -4; w <= 4; w += 4) {
                bp.fill(cx + w, fy + 2, az, cx + w, fy + 3, az + 1, Blocks.TINTED_GLASS.defaultBlockState());
                bp.fill(cx + w, fy + 2, bz - 1, cx + w, fy + 3, bz, Blocks.TINTED_GLASS.defaultBlockState());
                bp.fill(ax, fy + 2, cz + w, ax + 1, fy + 3, cz + w, Blocks.TINTED_GLASS.defaultBlockState());
                bp.fill(bx - 1, fy + 2, cz + w, bx, fy + 3, cz + w, Blocks.TINTED_GLASS.defaultBlockState());
            }
            // tapete, estantes soltas, atris e velas
            bp.fill(cx - 2, fy + 1, cz - 4, cx + 2, fy + 1, cz + 4, Blocks.RED_CARPET.defaultBlockState());
            for (int i = 0; i < 3; i++) {
                int sx = cx - 4 + i * 4;
                bp.fill(sx, fy + 1, cz + 2, sx, fy + 3, cz + 3, shelf);
            }
            bp.block(cx - 3, fy + 1, cz - 3, Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.FACING, Direction.SOUTH));
            bp.block(cx + 3, fy + 1, cz - 3, Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.FACING, Direction.SOUTH));
            for (int[] p : new int[][] {{cx - 5, cz - 5}, {cx + 5, cz - 5}, {cx - 5, cz + 5}, {cx + 5, cz + 5}}) {
                bp.block(p[0], fy + 1, p[1], Blocks.WHITE_CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 1 + rnd.nextInt(4))
                        .setValue(CandleBlock.LIT, true));
            }
            bp.block(cx, fy + STORY - 1, cz, Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        }
        // andaimes entre os andares (depois de todos os pisos, para o furo no teto ficar): no meio do caminho entre os centros
        for (int k = 0; k < 2; k++) {
            int cx = center.getX() + SHIFT[k][0], cz = center.getZ() + SHIFT[k][1], fy = y0 + k * STORY;
            int nx = center.getX() + SHIFT[k + 1][0], nz = center.getZ() + SHIFT[k + 1][1];
            int sx = (cx + nx) / 2 - 1, sz = (cz + nz) / 2 - 1;
            bp.air(sx - 1, fy + 1, sz - 1, sx + 1, fy + 3, sz + 1);
            bp.air(sx - 1, fy + STORY + 1, sz - 1, sx + 1, fy + STORY + 2, sz + 1);
            bp.fill(sx, fy + 1, sz, sx, fy + STORY, sz, Blocks.SCAFFOLDING.defaultBlockState());
        }
        // porta do térreo (sul)
        bp.air(center.getX(), y0 + 1, center.getZ() + HALF - 1, center.getX(), y0 + 2, center.getZ() + HALF);
        // último andar: a sala de ponta-cabeça (os móveis no teto) e o baú
        int tx = center.getX() + SHIFT[2][0], tz = center.getZ() + SHIFT[2][1], ty = y0 + 2 * STORY;
        bp.chest(tx, ty + 1, tz, Direction.SOUTH, Blueprint.loot("chests/memory_library"));
        int ceil = ty + STORY - 1;
        bp.block(tx - 2, ceil, tz + 1, Blocks.DARK_OAK_FENCE.defaultBlockState());
        bp.block(tx - 2, ceil - 1, tz + 1, Blocks.DARK_OAK_PRESSURE_PLATE.defaultBlockState());
        bp.block(tx - 1, ceil, tz + 1, Blocks.DARK_OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST).setValue(StairBlock.HALF, Half.TOP));
        bp.block(tx + 2, ceil, tz + 1, Blocks.BOOKSHELF.defaultBlockState());
        bp.block(tx + 2, ceil - 1, tz + 1, Blocks.BOOKSHELF.defaultBlockState());
        bp.block(tx + 1, ceil, tz - 2, Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.FACING, Direction.NORTH));
        // telhado torto (pirâmide de ardósia deslocada)
        for (int k = 0; k <= HALF; k++) {
            bp.fill(tx - HALF - 1 + k + 1, ty + STORY + k, tz - HALF - 1 + k, tx + HALF + 1 - k + 1, ty + STORY + k, tz + HALF + 1 - k,
                    Blocks.DEEPSLATE_TILES.defaultBlockState());
        }
        // moradores
        for (int k = 0; k < 3; k++) {
            int cx = center.getX() + SHIFT[k][0], cz = center.getZ() + SHIFT[k][1];
            bp.mob(cx + 0.5 + rnd.nextInt(5) - 2, y0 + k * STORY + 1.5, cz - 1.5, DreamDeepContent.MEMORY, null);
        }
        bp.mob(center.getX() + 0.5, y0 + 4.0, center.getZ() + 0.5, DreamDeepContent.REM_WATCHER, null);
    }
}

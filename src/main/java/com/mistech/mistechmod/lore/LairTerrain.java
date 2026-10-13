package com.mistech.mistechmod.lore;

import com.mistech.mistechmod.realm.goblin.Blueprint;
import com.mistech.mistechmod.realm.goblin.Blueprint.Palette;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/** Ajudas de terreno para as arenas do folclore. */
final class LairTerrain {
    private LairTerrain() {}

    static BlockState st(net.minecraft.world.level.block.Block b) {
        return b.defaultBlockState();
    }

    /**
     * Aplaina um círculo de raio {@code flat} na altura {@code top} (bloco do chão) com {@code ground}, e faz uma rampa
     * suave de {@code blend} blocos até o terreno natural. Corta o que estiver acima (até 40 blocos).
     */
    static void flatten(Blueprint bp, int cx, int cz, int top, int flat, int blend, Palette ground) {
        int r = flat + blend;
        BoundingBox box = new BoundingBox(cx - r, top - 30, cz - r, cx + r, top + 45, cz + r);
        bp.custom(box, ctx -> {
            int ax = Math.max(box.minX(), ctx.chunk.minX()), bx = Math.min(box.maxX(), ctx.chunk.maxX());
            int az = Math.max(box.minZ(), ctx.chunk.minZ()), bz = Math.min(box.maxZ(), ctx.chunk.maxZ());
            for (int x = ax; x <= bx; x++) {
                for (int z = az; z <= bz; z++) {
                    double d = Math.hypot(x - cx, z - cz);
                    if (d > r) {
                        continue;
                    }
                    int natural = Blueprint.surface(ctx, x, z);
                    boolean inner = d <= flat;
                    int target;
                    if (inner) {
                        target = top;
                    } else {
                        double s = (d - flat) / blend;
                        s = s * s * (3 - 2 * s);
                        target = (int) Math.round(top + (natural - top) * s);
                    }
                    if (natural > target) {
                        for (int y = target + 1; y <= Math.min(natural, target + 40); y++) {
                            ctx.set(x, y, z, Blocks.AIR.defaultBlockState());
                        }
                    } else {
                        for (int y = natural + 1; y < target; y++) {
                            ctx.set(x, y, z, y < target - 3 ? Blocks.STONE.defaultBlockState() : Blocks.DIRT.defaultBlockState());
                        }
                    }
                    if (inner) {
                        ctx.set(x, target, z, ground.pick(x, target, z));
                        ctx.set(x, target - 1, z, Blocks.DIRT.defaultBlockState());
                    } else if (natural != target) {
                        ctx.set(x, target, z, Blocks.GRASS_BLOCK.defaultBlockState());
                    }
                }
            }
        });
    }
}

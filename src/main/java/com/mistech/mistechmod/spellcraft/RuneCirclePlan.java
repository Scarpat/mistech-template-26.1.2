package com.mistech.mistechmod.spellcraft;

import static com.mistech.mistechmod.lore.LairTerrain.st;

import java.util.Random;

import com.mistech.mistechmod.realm.goblin.Blueprint;
import com.mistech.mistechmod.realm.goblin.Blueprint.Palette;

import net.minecraft.world.level.block.Blocks;

/** Círculo de Pedras Rúnico: oito menires de pedra (4–6 de altura) em círculo de raio 7, anel de musgo e, no centro, o Altar Rúnico. */
public final class RuneCirclePlan {
    private final Blueprint bp = new Blueprint();

    public RuneCirclePlan(long seed, int cx, int f, int cz) {
        Random rnd = new Random(seed);
        Palette ground = Palette.mix(110, st(Blocks.MOSS_BLOCK), 40, st(Blocks.GRASS_BLOCK), 40, st(Blocks.COARSE_DIRT), 20);
        com.mistech.mistechmod.lore.LairTerrain.flatten(this.bp, cx, cz, f, 9, 6, ground);
        Palette stone = Palette.mix(111, st(Blocks.STONE_BRICKS), 40, st(Blocks.MOSSY_STONE_BRICKS), 35, st(Blocks.CRACKED_STONE_BRICKS), 25);
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * 2 * i / 8;
            int x = cx + (int) Math.round(Math.cos(a) * 7), z = cz + (int) Math.round(Math.sin(a) * 7);
            int h = 4 + rnd.nextInt(3);
            for (int y = 1; y <= h; y++) {
                this.bp.block(x, f + y, z, stone.pick(x, f + y, z));
            }
            this.bp.block(x, f + h + 1, z, st(Blocks.CHISELED_STONE_BRICKS));
            this.bp.block(x, f + h + 2, z, st(Blocks.SOUL_LANTERN));
            this.bp.block(x, f + 1, z, st(Blocks.CHISELED_STONE_BRICKS));
        }
        // anel de pedra lisa e centro
        this.bp.shape(cx - 4, f, cz - 4, cx + 4, f, cz + 4, (x, y, z) -> {
            double d = Math.hypot(x - cx, z - cz);
            return d >= 2.5 && d <= 4.2;
        }, Palette.mix(112, st(Blocks.POLISHED_ANDESITE), 60, st(Blocks.MOSSY_COBBLESTONE), 40));
        this.bp.fill(cx - 1, f, cz - 1, cx + 1, f, cz + 1, Palette.of(st(Blocks.CHISELED_STONE_BRICKS)));
        this.bp.block(cx, f + 1, cz, SpellcraftContent.RUNE_ALTAR.get().defaultBlockState());
        for (int i = 0; i < 14; i++) {
            double a = rnd.nextDouble() * Math.PI * 2, r = 2 + rnd.nextDouble() * 6;
            this.bp.block(cx + (int) Math.round(Math.cos(a) * r), f + 1, cz + (int) Math.round(Math.sin(a) * r),
                    st(i % 2 == 0 ? Blocks.MOSS_CARPET : Blocks.SHORT_GRASS));
        }
    }

    public Blueprint blueprint() {
        return this.bp;
    }
}

package com.mistech.mistechmod.spellcraft;

import static com.mistech.mistechmod.lore.LairTerrain.st;

import java.util.Random;

import com.mistech.mistechmod.realm.goblin.Blueprint;
import com.mistech.mistechmod.realm.goblin.Blueprint.Palette;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Árvore-Mãe: carvalho gigante (tronco de raio 2,5, 28 de altura) com copa de azaleia florida, galhos, raízes de mangue
 * e uma câmara oca no tronco com o baú de Vida ({@code chests/mother_tree}), porta ao sul.
 */
public final class MotherTreePlan {
    private final Blueprint bp = new Blueprint();
    private final Random rnd;
    private final int cx, f, cz;
    private static final BlockState LEAF_A = Blocks.AZALEA_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
    private static final BlockState LEAF_B = Blocks.FLOWERING_AZALEA_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
    private static final BlockState LEAF_C = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);

    public MotherTreePlan(long seed, int cx, int y0, int cz) {
        this.rnd = new Random(seed);
        this.cx = cx;
        this.f = y0;
        this.cz = cz;
        com.mistech.mistechmod.lore.LairTerrain.flatten(this.bp, cx, cz, this.f, 7, 7, Palette.mix(100, st(Blocks.GRASS_BLOCK), 70, st(Blocks.MOSS_BLOCK), 20, st(Blocks.PODZOL), 10));
        this.trunk();
        this.canopy();
        this.chamber();
        this.roots();
    }

    public Blueprint blueprint() {
        return this.bp;
    }

    private void trunk() {
        Palette bark = Palette.mix(101, Blocks.OAK_WOOD.defaultBlockState(), 85, Blocks.MOSS_BLOCK.defaultBlockState(), 10, Blocks.MANGROVE_ROOTS.defaultBlockState(), 5);
        this.bp.shape(this.cx - 3, this.f + 1, this.cz - 3, this.cx + 3, this.f + 28, this.cz + 3,
                (x, y, z) -> Math.hypot(x - this.cx, z - this.cz) <= (y - this.f < 4 ? 3.2 : 2.6), bark);
        // galhos horizontais
        BlockState ew = Blocks.OAK_WOOD.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
        BlockState ns = Blocks.OAK_WOOD.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);
        for (int h : new int[] {19, 23}) {
            for (int i = 3; i <= 9; i++) {
                this.bp.block(this.cx + i, this.f + h + i / 4, this.cz, ew);
                this.bp.block(this.cx - i, this.f + h + i / 4, this.cz, ew);
                this.bp.block(this.cx, this.f + h + i / 4, this.cz + i, ns);
                this.bp.block(this.cx, this.f + h + i / 4, this.cz - i, ns);
            }
        }
    }

    private void canopy() {
        final int top = this.f + 27;
        this.bp.shape(this.cx - 13, top - 8, this.cz - 13, this.cx + 13, top + 8, this.cz + 13, (x, y, z) -> {
            double dx = (x - this.cx) / 12.5, dy = (y - top) / 8.0, dz = (z - this.cz) / 12.5;
            if (dx * dx + dy * dy + dz * dz > 1.0) {
                return false;
            }
            return Math.floorMod(Blueprint.hash(x, y, z, 102), 100L) < 88; // copa irregular
        }, (x, y, z) -> {
            int r = (int) Math.floorMod(Blueprint.hash(x, y, z, 103), 10L);
            return r < 5 ? LEAF_A : r < 7 ? LEAF_B : LEAF_C;
        });
        // o tronco não pode ser coberto pela copa
        this.bp.shape(this.cx - 3, top - 8, this.cz - 3, this.cx + 3, top + 1, this.cz + 3,
                (x, y, z) -> Math.hypot(x - this.cx, z - this.cz) <= 2.6, Palette.of(Blocks.OAK_WOOD.defaultBlockState()));
        // raízes aéreas pendendo da copa
        for (int i = 0; i < 40; i++) {
            double a = this.rnd.nextDouble() * Math.PI * 2, r = 4 + this.rnd.nextDouble() * 8;
            int x = this.cx + (int) Math.round(Math.cos(a) * r), z = this.cz + (int) Math.round(Math.sin(a) * r);
            int y = top - 5 - this.rnd.nextInt(3);
            this.bp.block(x, y, z, st(Blocks.HANGING_ROOTS));
        }
    }

    private void chamber() {
        this.bp.air(this.cx - 1, this.f + 1, this.cz - 1, this.cx + 1, this.f + 4, this.cz + 1);
        this.bp.air(this.cx, this.f + 1, this.cz + 2, this.cx, this.f + 2, this.cz + 3);
        this.bp.fill(this.cx - 1, this.f, this.cz - 1, this.cx + 1, this.f, this.cz + 1, Palette.of(st(Blocks.MOSS_BLOCK)));
        this.bp.chest(this.cx, this.f + 1, this.cz - 1, Direction.SOUTH, Blueprint.loot("chests/mother_tree"));
        this.bp.block(this.cx - 1, this.f + 1, this.cz, st(Blocks.FLOWERING_AZALEA));
        this.bp.block(this.cx + 1, this.f + 1, this.cz, st(Blocks.SHROOMLIGHT));
        this.bp.block(this.cx, this.f + 4, this.cz, st(Blocks.GLOW_LICHEN));
    }

    private void roots() {
        for (int i = 0; i < 9; i++) {
            double a = Math.PI * 2 * i / 9 + this.rnd.nextDouble() * 0.3;
            for (int r = 3; r <= 6; r++) {
                int x = this.cx + (int) Math.round(Math.cos(a) * r), z = this.cz + (int) Math.round(Math.sin(a) * r);
                if (a > 1.2 && a < 1.9 && r < 5) {
                    continue; // não tampa a porta
                }
                this.bp.block(x, this.f + 1, z, st(r < 5 ? Blocks.MANGROVE_ROOTS : Blocks.MOSS_CARPET));
            }
        }
        // flores em volta
        for (int i = 0; i < 24; i++) {
            double a = this.rnd.nextDouble() * Math.PI * 2, r = 5 + this.rnd.nextDouble() * 5;
            int x = this.cx + (int) Math.round(Math.cos(a) * r), z = this.cz + (int) Math.round(Math.sin(a) * r);
            this.bp.block(x, this.f + 1, z, st(i % 3 == 0 ? Blocks.AZURE_BLUET : i % 3 == 1 ? Blocks.LILY_OF_THE_VALLEY : Blocks.OXEYE_DAISY));
        }
    }
}

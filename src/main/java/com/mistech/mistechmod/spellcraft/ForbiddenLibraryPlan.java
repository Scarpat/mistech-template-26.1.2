package com.mistech.mistechmod.spellcraft;

import static com.mistech.mistechmod.lore.LairTerrain.st;

import java.util.Random;

import com.mistech.mistechmod.realm.goblin.Blueprint;
import com.mistech.mistechmod.realm.goblin.Blueprint.Palette;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;

/**
 * Biblioteca Proibida: sala enterrada (17x13, 14 blocos abaixo da superfície) de estantes e teias, guardada por geradores
 * de esqueletos. Entrada: um poço de pedra na superfície com escada de mão. Baú do centro = Tomo Proibido
 * ({@code chests/forbidden_library}); dois baús menores nas pontas.
 */
public final class ForbiddenLibraryPlan {
    private final Blueprint bp = new Blueprint();
    private final Random rnd;
    private final int cx, f, cz, y0;
    private final Palette wall = Palette.mix(90, st(Blocks.DEEPSLATE_BRICKS), 55, st(Blocks.CRACKED_DEEPSLATE_BRICKS), 25, st(Blocks.POLISHED_BLACKSTONE_BRICKS), 20);

    public ForbiddenLibraryPlan(long seed, int cx, int surface, int cz) {
        this.rnd = new Random(seed);
        this.cx = cx;
        this.f = surface;
        this.cz = cz;
        this.y0 = surface - 14;
        this.room();
        this.shaft();
        this.contents();
    }

    public Blueprint blueprint() {
        return this.bp;
    }

    private void room() {
        // casca e interior
        this.bp.fill(this.cx - 9, this.y0, this.cz - 7, this.cx + 9, this.y0 + 6, this.cz + 7, this.wall);
        this.bp.air(this.cx - 8, this.y0 + 1, this.cz - 6, this.cx + 8, this.y0 + 5, this.cz + 6);
        this.bp.fill(this.cx - 8, this.y0, this.cz - 6, this.cx + 8, this.y0, this.cz + 6,
                Palette.mix(91, st(Blocks.DARK_OAK_PLANKS), 60, st(Blocks.POLISHED_DEEPSLATE), 40));
        // tapete central
        this.bp.fill(this.cx - 1, this.y0, this.cz - 5, this.cx + 1, this.y0, this.cz + 5, Palette.of(st(Blocks.PURPLE_CARPET)));
        this.bp.fill(this.cx - 1, this.y0 + 1, this.cz - 5, this.cx + 1, this.y0 + 1, this.cz + 5, Palette.of(st(Blocks.AIR)));
        // colunas
        for (int x : new int[] {-5, 5}) {
            for (int z : new int[] {-3, 3}) {
                for (int y = 1; y <= 5; y++) {
                    this.bp.block(this.cx + x, this.y0 + y, this.cz + z, st(Blocks.DEEPSLATE_BRICK_WALL));
                }
                this.bp.block(this.cx + x, this.y0 + 4, this.cz + z, st(Blocks.SOUL_LANTERN));
            }
        }
    }

    private void shaft() {
        int sz = this.cz + 9;
        // poço: paredes de pedra de 3x3 do fundo à superfície, com abertura de 1x1
        this.bp.fill(this.cx - 1, this.y0 + 1, sz - 1, this.cx + 1, this.f + 2, sz + 1, this.wall);
        this.bp.air(this.cx, this.y0 + 1, sz, this.cx, this.f + 3, sz);
        // corredor entre o poço e a sala
        this.bp.fill(this.cx - 1, this.y0, this.cz + 7, this.cx + 1, this.y0 + 3, sz, this.wall);
        this.bp.air(this.cx, this.y0 + 1, this.cz + 7, this.cx, this.y0 + 2, sz);
        // porta da sala (parede sul)
        this.bp.air(this.cx, this.y0 + 1, this.cz + 7, this.cx, this.y0 + 2, this.cz + 7);
        this.bp.air(this.cx, this.y0 + 1, this.cz + 8, this.cx, this.y0 + 2, this.cz + 8);
        // marca na superfície: murinho baixo em volta da boca do poço
        this.bp.fill(this.cx - 1, this.f + 1, sz - 1, this.cx + 1, this.f + 1, sz + 1, Palette.of(st(Blocks.MOSSY_STONE_BRICK_WALL)));
        this.bp.air(this.cx, this.f + 1, sz, this.cx, this.f + 3, sz);
        // a escada é posta por último para o murinho não apagar os degraus do topo
        for (int y = this.y0 + 1; y <= this.f + 2; y++) {
            this.bp.block(this.cx, y, sz, st(Blocks.LADDER).setValue(LadderBlock.FACING, Direction.NORTH));
        }
    }

    private void contents() {
        // estantes nas paredes norte, leste e oeste (duas alturas)
        for (int x = -8; x <= 8; x++) {
            if (Math.abs(x) <= 1 && false) {
                continue;
            }
            this.bp.block(this.cx + x, this.y0 + 1, this.cz - 6, st(Blocks.BOOKSHELF));
            this.bp.block(this.cx + x, this.y0 + 2, this.cz - 6, st(Blocks.BOOKSHELF));
            if (this.rnd.nextInt(3) > 0) {
                this.bp.block(this.cx + x, this.y0 + 3, this.cz - 6, st(Blocks.BOOKSHELF));
            }
        }
        for (int z = -5; z <= 5; z++) {
            for (int side : new int[] {-8, 8}) {
                this.bp.block(this.cx + side, this.y0 + 1, this.cz + z, st(Blocks.BOOKSHELF));
                if (this.rnd.nextBoolean()) {
                    this.bp.block(this.cx + side, this.y0 + 2, this.cz + z, st(Blocks.BOOKSHELF));
                }
            }
        }
        // fileiras de estantes soltas
        for (int z : new int[] {-1, 2}) {
            for (int x : new int[] {-6, -5, -4, 4, 5, 6}) {
                this.bp.block(this.cx + x, this.y0 + 1, this.cz + z, st(Blocks.BOOKSHELF));
                this.bp.block(this.cx + x, this.y0 + 2, this.cz + z, st(Blocks.BOOKSHELF));
            }
        }
        // teias
        for (int i = 0; i < 18; i++) {
            this.bp.block(this.cx + this.rnd.nextInt(15) - 7, this.y0 + 1 + this.rnd.nextInt(5), this.cz + this.rnd.nextInt(11) - 5, st(Blocks.COBWEB));
        }
        // altar do Tomo no fundo
        this.bp.fill(this.cx - 1, this.y0 + 1, this.cz - 5, this.cx + 1, this.y0 + 1, this.cz - 4, Palette.of(st(Blocks.CHISELED_DEEPSLATE)));
        this.bp.chest(this.cx, this.y0 + 2, this.cz - 5, Direction.SOUTH, Blueprint.loot("chests/forbidden_library"));
        this.bp.block(this.cx - 1, this.y0 + 2, this.cz - 5, st(Blocks.SOUL_LANTERN));
        this.bp.block(this.cx + 1, this.y0 + 2, this.cz - 5, st(Blocks.SOUL_LANTERN));
        // baús menores
        this.bp.chest(this.cx - 7, this.y0 + 1, this.cz + 4, Direction.EAST, Blueprint.loot("chests/forbidden_library_minor"));
        this.bp.chest(this.cx + 7, this.y0 + 1, this.cz + 4, Direction.WEST, Blueprint.loot("chests/forbidden_library_minor"));
        // guardiões
        this.bp.spawner(this.cx - 4, this.y0 + 1, this.cz - 3, () -> EntityType.SKELETON);
        this.bp.spawner(this.cx + 4, this.y0 + 1, this.cz - 3, () -> EntityType.SKELETON);
    }
}

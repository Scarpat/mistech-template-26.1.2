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
 * Torre do Arquimago Caído: torre de pedra em ruínas (raio 4, 15 de altura) com três andares ligados por escada de mão.
 * Térreo vazio com teias; 2º andar com um gerador de Evocadores; topo com estantes, mesa de encantamento e o baú
 * com o Cajado do Arquimago (loot {@code chests/archmage_tower}). A parte de cima está meio desabada.
 */
public final class ArchmageTowerPlan {
    private final Blueprint bp = new Blueprint();
    private final Random rnd;
    private final int cx, f, cz;
    private final Palette wall = Palette.mix(71, st(Blocks.STONE_BRICKS), 45, st(Blocks.MOSSY_STONE_BRICKS), 25, st(Blocks.CRACKED_STONE_BRICKS), 20,
            st(Blocks.DEEPSLATE_BRICKS), 10);
    private final Palette floor = Palette.mix(72, st(Blocks.SPRUCE_PLANKS), 60, st(Blocks.DARK_OAK_PLANKS), 40);

    public ArchmageTowerPlan(long seed, int cx, int y0, int cz) {
        this.rnd = new Random(seed);
        this.cx = cx;
        this.f = y0;
        this.cz = cz;
        com.mistech.mistechmod.lore.LairTerrain.flatten(this.bp, cx, cz, this.f, 8, 6, Palette.mix(70, st(Blocks.COBBLESTONE), 30, st(Blocks.MOSSY_COBBLESTONE), 30,
                st(Blocks.GRAVEL), 20, st(Blocks.COARSE_DIRT), 20));
        this.shell();
        this.floors();
        this.ladder();
        this.contents();
    }

    public Blueprint blueprint() {
        return this.bp;
    }

    private boolean ring(int x, int z, double r) {
        double d = Math.hypot(x - this.cx, z - this.cz);
        return d <= r && d > r - 1.0;
    }

    private void shell() {
        final int height = 15;
        // base e paredes (anel de raio 4)
        this.bp.shape(this.cx - 5, this.f, this.cz - 5, this.cx + 5, this.f, this.cz + 5,
                (x, y, z) -> Math.hypot(x - this.cx, z - this.cz) <= 4.6, this.wall);
        this.bp.shape(this.cx - 5, this.f + 1, this.cz - 5, this.cx + 5, this.f + height, this.cz + 5, (x, y, z) -> {
            if (!this.ring(x, z, 4.6)) {
                return false;
            }
            // topo desabado: quanto mais alto, mais buracos (estável por posição)
            int top = y - this.f;
            if (top > 10) {
                long h = Blueprint.hash(x, y, z, 73);
                return Math.floorMod(h, 100L) < 100 - (top - 10) * 18;
            }
            return true;
        }, this.wall);
        // interior limpo
        this.bp.shape(this.cx - 4, this.f + 1, this.cz - 4, this.cx + 4, this.f + height, this.cz + 4,
                (x, y, z) -> Math.hypot(x - this.cx, z - this.cz) < 3.6, Palette.of(st(Blocks.AIR)));
        // porta ao sul (2 de altura) e janelas
        this.bp.air(this.cx, this.f + 1, this.cz + 4, this.cx, this.f + 2, this.cz + 4);
        this.bp.air(this.cx, this.f + 1, this.cz + 3, this.cx, this.f + 2, this.cz + 3);
        for (int y : new int[] {this.f + 4, this.f + 9}) {
            this.bp.block(this.cx - 4, y, this.cz, st(Blocks.IRON_BARS));
            this.bp.block(this.cx + 4, y, this.cz, st(Blocks.IRON_BARS));
            this.bp.block(this.cx, y, this.cz - 4, st(Blocks.IRON_BARS));
        }
        // contrafortes
        for (int i = 0; i < 4; i++) {
            int x = this.cx + (i % 2 == 0 ? -5 : 5), z = this.cz + (i < 2 ? -3 : 3);
            for (int y = 1; y <= 3 + this.rnd.nextInt(3); y++) {
                this.bp.block(x, this.f + y, z, st(Blocks.COBBLESTONE_WALL));
            }
        }
    }

    private void floors() {
        for (int level : new int[] {5, 10}) {
            this.bp.shape(this.cx - 4, this.f + level, this.cz - 4, this.cx + 4, this.f + level, this.cz + 4, (x, y, z) -> {
                if (Math.hypot(x - this.cx, z - this.cz) >= 3.6) {
                    return false;
                }
                return !(x == this.cx && z == this.cz - 3); // buraco da escada
            }, this.floor);
        }
    }

    private void ladder() {
        for (int y = 1; y <= 11; y++) {
            this.bp.block(this.cx, this.f + y, this.cz - 4, st(Blocks.STONE_BRICKS)); // apoio (o topo desabado não pode soltar a escada)
            this.bp.block(this.cx, this.f + y, this.cz - 3, st(Blocks.LADDER).setValue(LadderBlock.FACING, Direction.SOUTH));
        }
    }

    private void contents() {
        // térreo: teias
        for (int i = 0; i < 8; i++) {
            this.bp.block(this.cx + this.rnd.nextInt(5) - 2, this.f + 1 + this.rnd.nextInt(3), this.cz + this.rnd.nextInt(5) - 2, st(Blocks.COBWEB));
        }
        this.bp.block(this.cx + 2, this.f + 1, this.cz + 1, st(Blocks.BARREL));
        // 2º andar: gerador de evocadores, estantes e uma mesa de pesquisa
        this.bp.spawner(this.cx, this.f + 6, this.cz + 1, () -> EntityType.EVOKER);
        this.bp.block(this.cx - 2, this.f + 6, this.cz, st(Blocks.LECTERN));
        this.bp.block(this.cx + 2, this.f + 6, this.cz + 2, st(Blocks.BOOKSHELF));
        this.bp.block(this.cx + 2, this.f + 7, this.cz + 2, st(Blocks.BOOKSHELF));
        this.bp.block(this.cx - 1, this.f + 6, this.cz + 3, st(Blocks.CANDLE));
        // topo: estantes, mesa de encantamento e o baú
        for (int i = -2; i <= 2; i++) {
            this.bp.block(this.cx + i, this.f + 11, this.cz + 3, st(Blocks.BOOKSHELF));
            if (i != 0) {
                this.bp.block(this.cx + i, this.f + 12, this.cz + 3, st(Blocks.BOOKSHELF));
            }
        }
        this.bp.block(this.cx - 2, this.f + 11, this.cz, st(Blocks.ENCHANTING_TABLE));
        this.bp.block(this.cx + 2, this.f + 11, this.cz - 1, st(Blocks.CANDLE));
        this.bp.chest(this.cx + 2, this.f + 11, this.cz, Direction.WEST, Blueprint.loot("chests/archmage_tower"));
        this.bp.block(this.cx, this.f + 11, this.cz - 1, st(Blocks.AMETHYST_CLUSTER));
    }
}

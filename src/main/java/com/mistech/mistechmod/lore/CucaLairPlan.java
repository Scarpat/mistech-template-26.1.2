package com.mistech.mistechmod.lore;

import static com.mistech.mistechmod.lore.LairTerrain.st;

import java.util.Random;

import com.mistech.mistechmod.realm.goblin.Blueprint;
import com.mistech.mistechmod.realm.goblin.Blueprint.Palette;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Toca da Cuca (arena da Cuca), na superfície da Mata Ancestral.
 *
 * <pre>
 * Clareira de lama batida (raio 15) cercada por 10 totens de caveira; lagoa verde com aguapés e taboas;
 * no meio, a cabana torta de barro e madeira escura (9x9), telhado de musgo em degraus e chaminé com fumaça
 * (dá para achar de longe). Dentro: caldeirão sobre fogueira, bancadas de poção, prateleiras com vasos,
 * baú (loot {@code chests/cuca_lair}), teias e lanternas de alma. Porta ao sul.
 * Embaixo do chão: o Coração da Toca — quem chega a ~11 blocos acorda a Cuca, que sai na porta da cabana.
 * </pre>
 */
public final class CucaLairPlan {
    static final int FLAT = 15, BLEND = 8;

    private final Blueprint bp = new Blueprint();
    private final Random rnd;
    private final int cx, f, cz;

    private final Palette ground = Palette.mix(31, st(Blocks.PACKED_MUD), 60, st(Blocks.MUD_BRICKS), 10, st(Blocks.GRAVEL), 15,
            st(Blocks.TUFF), 8, st(Blocks.DIRT_PATH), 7);
    private final Palette wall = Palette.mix(32, st(Blocks.MUD_BRICKS), 45, st(Blocks.PACKED_MUD), 20, st(Blocks.DARK_OAK_PLANKS), 22,
            st(Blocks.MOSSY_COBBLESTONE), 13);
    private final Palette roof = Palette.mix(33, st(Blocks.DARK_OAK_PLANKS), 35, st(Blocks.MOSS_BLOCK), 40, st(Blocks.MUD_BRICKS), 12,
            st(Blocks.MANGROVE_ROOTS), 13);
    private final Palette floor = Palette.mix(34, st(Blocks.DARK_OAK_PLANKS), 55, st(Blocks.MUD_BRICKS), 30, st(Blocks.PACKED_MUD), 15);
    private final Palette chimney = Palette.mix(35, st(Blocks.COBBLESTONE), 50, st(Blocks.MOSSY_COBBLESTONE), 50);
    private final BlockState post = Blocks.MANGROVE_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);

    public CucaLairPlan(long seed, int cx, int y0, int cz) {
        this.rnd = new Random(seed);
        this.cx = cx;
        this.f = y0;
        this.cz = cz;
        LairTerrain.flatten(this.bp, cx, cz, this.f, FLAT, BLEND, this.ground);
        this.pond();
        this.totems();
        this.hut();
        this.interior();
        this.core();
    }

    public Blueprint blueprint() {
        return this.bp;
    }

    // ------------------------------------------------------------------ lagoa
    private void pond() {
        double a = this.rnd.nextDouble() * Math.PI * 2;
        int px = this.cx + (int) Math.round(Math.cos(a) * 10), pz = this.cz + (int) Math.round(Math.sin(a) * 10);
        if (pz > this.cz + 4 && Math.abs(px - this.cx) < 5) {
            pz = this.cz - 10; // não tampa a porta
        }
        final int ox = px, oz = pz;
        int r = 4;
        this.bp.shape(ox - r - 1, this.f - 2, oz - r - 1, ox + r + 1, this.f, oz + r + 1,
                (x, y, z) -> Math.hypot(x - ox, z - oz) <= r + 1.2, Palette.of(st(Blocks.MUD)));
        this.bp.shape(ox - r, this.f - 1, oz - r, ox + r, this.f, oz + r,
                (x, y, z) -> Math.hypot(x - ox, z - oz) <= r - (y < this.f ? 1 : 0) + 0.3, Palette.of(st(Blocks.WATER)));
        for (int i = 0; i < 4; i++) {
            int lx = ox + this.rnd.nextInt(5) - 2, lz = oz + this.rnd.nextInt(5) - 2;
            this.bp.block(lx, this.f + 1, lz, st(Blocks.LILY_PAD));
        }
        for (int i = 0; i < 6; i++) { // taboas (cana) na beira de lama
            double b = this.rnd.nextDouble() * Math.PI * 2;
            int sx = ox + (int) Math.round(Math.cos(b) * (r + 1)), sz = oz + (int) Math.round(Math.sin(b) * (r + 1));
            int hgt = 1 + this.rnd.nextInt(2);
            for (int y = 1; y <= hgt; y++) {
                this.bp.block(sx, this.f + y, sz, st(Blocks.SUGAR_CANE));
            }
        }
    }

    // ------------------------------------------------------------------ totens de caveira
    private void totems() {
        int n = 10;
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2 * i / n + 0.15;
            int x = this.cx + (int) Math.round(Math.cos(a) * 13), z = this.cz + (int) Math.round(Math.sin(a) * 13);
            int h = 2 + (i % 3 == 0 ? 1 : 0);
            for (int y = 1; y <= h; y++) {
                this.bp.block(x, this.f + y, z, this.post);
            }
            if (i % 2 == 0) {
                // caveira olhando para o centro (rotação 0 = sul, 4 = oeste, 8 = norte, 12 = leste)
                double toCenter = Math.atan2(this.cx - x, this.cz - z); // 0 = +z (sul)
                int rot = Math.floorMod((int) Math.round(-toCenter / (Math.PI * 2) * 16), 16);
                this.bp.block(x, this.f + h + 1, z, st(Blocks.SKELETON_SKULL).setValue(SkullBlock.ROTATION, rot));
            } else {
                this.bp.block(x, this.f + h + 1, z, st(Blocks.SOUL_LANTERN));
            }
            if (this.rnd.nextInt(3) == 0) {
                this.bp.block(x + 1, this.f + 1, z, st(Blocks.BONE_BLOCK));
            }
        }
    }

    // ------------------------------------------------------------------ cabana
    private void hut() {
        int x0 = this.cx - 4, x1 = this.cx + 4, z0 = this.cz - 4, z1 = this.cz + 4;
        // piso
        this.bp.fill(x0, this.f, z0, x1, this.f, z1, this.floor);
        // paredes
        this.bp.shape(x0, this.f + 1, z0, x1, this.f + 4, z1, (x, y, z) -> x == x0 || x == x1 || z == z0 || z == z1, this.wall);
        this.bp.air(x0 + 1, this.f + 1, z0 + 1, x1 - 1, this.f + 4, z1 - 1);
        // esteios de mangue nos cantos (passam do telhado, tortos)
        int[][] corners = {{x0, z0}, {x1, z0}, {x0, z1}, {x1, z1}};
        for (int[] c : corners) {
            for (int y = 0; y <= 5; y++) {
                this.bp.block(c[0], this.f + y, c[1], this.post);
            }
        }
        // janelas de raiz de mangue (leste e oeste) e porta ao sul
        for (int y = 2; y <= 3; y++) {
            this.bp.block(x0, this.f + y, this.cz, st(Blocks.MANGROVE_ROOTS));
            this.bp.block(x1, this.f + y, this.cz, st(Blocks.MANGROVE_ROOTS));
            this.bp.block(this.cx - 2, this.f + y, z0, st(Blocks.MANGROVE_ROOTS));
        }
        this.bp.air(this.cx, this.f + 1, z1, this.cx, this.f + 2, z1);
        // telhado em degraus (5 camadas), meio torto para um lado
        int lean = this.rnd.nextBoolean() ? 1 : -1;
        for (int k = 0; k < 5; k++) {
            int s = 5 - k;
            int y = this.f + 5 + k;
            int ox = k >= 3 ? lean : 0;
            int fk = k;
            int sx0 = this.cx - s + ox, sx1 = this.cx + s + ox, sz0 = this.cz - s, sz1 = this.cz + s;
            this.bp.shape(sx0, y, sz0, sx1, y, sz1, (x, yy, z) -> fk == 0 || fk == 4 || x == sx0 || x == sx1 || z == sz0 || z == sz1, this.roof);
        }
        // raízes penduradas na beira do telhado
        for (int x = this.cx - 5; x <= this.cx + 5; x++) {
            for (int z : new int[] {this.cz - 5, this.cz + 5}) {
                if (Math.floorMod((long) Blueprint.hash(x, 0, z, 36), 3L) == 0) {
                    this.bp.block(x, this.f + 4, z, st(Blocks.HANGING_ROOTS));
                }
            }
        }
        // chaminé com fumaça alta (fogueira de sinal em cima de feno)
        int chx = this.cx + 2, chz = this.cz - 2;
        this.bp.fill(chx, this.f + 5, chz, chx, this.f + 10, chz, this.chimney);
        this.bp.block(chx, this.f + 11, chz, st(Blocks.HAY_BLOCK));
        this.bp.block(chx, this.f + 12, chz, st(Blocks.CAMPFIRE).setValue(CampfireBlock.SIGNAL_FIRE, true));
        // caminho de ossos até a porta
        for (int z = z1 + 1; z <= z1 + 6; z++) {
            if (z % 2 == 0) {
                this.bp.block(this.cx, this.f, z, st(Blocks.BONE_BLOCK));
            }
        }
        this.bp.block(this.cx - 1, this.f + 1, z1 + 1, this.post);
        this.bp.block(this.cx - 1, this.f + 2, z1 + 1, st(Blocks.SKELETON_SKULL).setValue(SkullBlock.ROTATION, 0));
        this.bp.block(this.cx + 1, this.f + 1, z1 + 1, this.post);
        this.bp.block(this.cx + 1, this.f + 2, z1 + 1, st(Blocks.SOUL_LANTERN));
    }

    private void interior() {
        int y = this.f + 1;
        // caldeirão sobre a fogueira, no meio
        this.bp.block(this.cx, this.f, this.cz, st(Blocks.CAMPFIRE));
        this.bp.block(this.cx, y, this.cz, st(Blocks.WATER_CAULDRON).setValue(LayeredCauldronBlock.LEVEL, 3));
        // parede norte: estante, poções, barril, baú, barril, poções, estante
        int zn = this.cz - 3;
        BlockState[] row = {st(Blocks.BOOKSHELF), st(Blocks.BREWING_STAND), st(Blocks.BARREL), null, st(Blocks.BARREL),
            st(Blocks.BREWING_STAND), st(Blocks.BOOKSHELF)};
        for (int i = 0; i < row.length; i++) {
            if (row[i] != null) {
                this.bp.block(this.cx - 3 + i, y, zn, row[i]);
            }
        }
        this.bp.chest(this.cx, y, zn, Direction.SOUTH, Blueprint.loot("chests/cuca_lair"));
        BlockState[] pots = {st(Blocks.POTTED_DEAD_BUSH), st(Blocks.POTTED_BROWN_MUSHROOM), st(Blocks.POTTED_RED_MUSHROOM),
            st(Blocks.POTTED_CRIMSON_FUNGUS), st(Blocks.POTTED_WITHER_ROSE)};
        for (int i : new int[] {0, 2, 4, 6}) {
            this.bp.block(this.cx - 3 + i, y + 1, zn, pots[this.rnd.nextInt(pots.length)]);
        }
        // parede oeste: estantes e crânio
        this.bp.block(this.cx - 3, y, this.cz + 2, st(Blocks.BOOKSHELF));
        this.bp.block(this.cx - 3, y + 1, this.cz + 2, st(Blocks.BOOKSHELF));
        this.bp.block(this.cx - 3, y + 2, this.cz + 2, st(Blocks.SKELETON_SKULL).setValue(SkullBlock.ROTATION, 12));
        this.bp.block(this.cx - 3, y, this.cz - 1, st(Blocks.COMPOSTER));
        // parede leste: mesa de poções improvisada
        this.bp.block(this.cx + 3, y, this.cz + 2, st(Blocks.CRAFTING_TABLE));
        this.bp.block(this.cx + 3, y + 1, this.cz + 2, st(Blocks.POTTED_WITHER_ROSE));
        this.bp.block(this.cx + 3, y, this.cz - 1, st(Blocks.HAY_BLOCK));
        // tapete roxo na entrada
        this.bp.fill(this.cx - 1, y, this.cz + 2, this.cx + 1, y, this.cz + 3, st(Blocks.PURPLE_CARPET));
        // teias nos cantos do teto e lanternas de alma penduradas
        int[][] corners = {{-3, -3}, {3, -3}, {-3, 3}, {3, 3}};
        for (int[] c : corners) {
            this.bp.block(this.cx + c[0], this.f + 4, this.cz + c[1], st(Blocks.COBWEB));
        }
        this.bp.block(this.cx - 2, this.f + 4, this.cz + 1, st(Blocks.SOUL_LANTERN).setValue(LanternBlock.HANGING, true));
        this.bp.block(this.cx + 2, this.f + 4, this.cz + 1, st(Blocks.SOUL_LANTERN).setValue(LanternBlock.HANGING, true));
    }

    // ------------------------------------------------------------------ coração da toca
    private void core() {
        int x = this.cx, y = this.f - 3, z = this.cz;
        this.bp.custom(new BoundingBox(x, y, z, x, y, z), ctx -> {
            if (!ctx.inside(x, y, z)) {
                return;
            }
            ctx.set(x, y, z, LoreContent.LAIR_CORE.get().defaultBlockState());
            if (ctx.level.getBlockEntity(new net.minecraft.core.BlockPos(x, y, z)) instanceof LairCoreBlockEntity be) {
                be.setup("cuca", 0, 4, 7); // nasce na frente da porta
            }
        });
    }
}

package com.mistech.mistechmod.lore;

import static com.mistech.mistechmod.lore.LairTerrain.st;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.mistech.mistechmod.realm.goblin.Blueprint;
import com.mistech.mistechmod.realm.goblin.Blueprint.Palette;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Caverna do Mapinguari (arena do Mapinguari) embaixo da Mata Ancestral.
 *
 * <pre>
 * Superfície: chão pisoteado (raio 8) com troncos caídos, tocos arranhados de garra, ossos e caveiras,
 *             e a boca do túnel.
 * Túnel:      desce em rampa (~31 blocos) até o salão.
 * Salão:      elipsoide de 31x16x31, chão de musgo/terra/cascalho, raízes e estalactites no teto, shroomlights,
 *             pilares; no fundo, o NINHO de folhas e ossos com o baú (loot {@code chests/mapinguari_cave}).
 * Paredes:    casca de pedra que tampa cavernas e água em volta (não alaga).
 * Embaixo do salão: o Coração da Toca — quem entra no salão acorda o Mapinguari, que levanta do ninho.
 * </pre>
 */
public final class MapinguariCavePlan {
    static final int STEPS = 31;
    static final int RX = 15, RY = 8, RZ = 15;

    private final Blueprint bp = new Blueprint();
    private final Random rnd;
    private final int cx, f, cz, ux, uz;
    private final int cf;      // chão do salão
    private final int ex, ez;  // centro do salão
    private final int nx, nz;  // centro do ninho

    private final Palette rock = Palette.mix(41, st(Blocks.STONE), 40, st(Blocks.TUFF), 20, st(Blocks.ANDESITE), 15,
            st(Blocks.MOSSY_COBBLESTONE), 15, st(Blocks.COBBLESTONE), 10);
    private final Palette trampled = Palette.mix(42, st(Blocks.PACKED_MUD), 50, st(Blocks.GRAVEL), 25, st(Blocks.TUFF), 10,
            st(Blocks.MUD_BRICKS), 10, st(Blocks.BONE_BLOCK), 5);
    private final Palette caveFloor = Palette.mix(43, st(Blocks.MOSS_BLOCK), 30, st(Blocks.COARSE_DIRT), 20, st(Blocks.ROOTED_DIRT), 10,
            st(Blocks.GRAVEL), 15, st(Blocks.TUFF), 13, st(Blocks.PACKED_MUD), 12);
    private final BlockState leaves = Blocks.JUNGLE_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);

    public MapinguariCavePlan(long seed, int cx, int y0, int cz) {
        this.rnd = new Random(seed);
        this.cx = cx;
        this.f = y0;
        this.cz = cz;
        int dir = this.rnd.nextInt(4);
        this.ux = dir == 0 ? 1 : dir == 1 ? -1 : 0;
        this.uz = dir == 2 ? 1 : dir == 3 ? -1 : 0;
        this.cf = Math.max(y0 - 26, -40);
        int dist = STEPS + 12;
        this.ex = cx + this.ux * dist;
        this.ez = cz + this.uz * dist;
        this.nx = this.ex + this.ux * 9;
        this.nz = this.ez + this.uz * 9;

        LairTerrain.flatten(this.bp, cx, cz, this.f, 8, 5, this.trampled);
        this.surfaceDecor();
        // 1ª passada: casca de pedra (só onde é ar ou líquido, abaixo da superfície); 2ª passada: escava
        List<double[]> tunnel = this.tunnelPath();
        for (double[] p : tunnel) {
            this.shell(p[0], p[1], p[2], 2.6 + 1.6, 1.0, this.f - 3);
        }
        this.shell(this.ex, this.cf + 6, this.ez, RX + 2.5, (double) (RY + 2) / (RX + 2.5), Integer.MAX_VALUE);
        for (double[] p : tunnel) {
            this.carveSphere(p[0], p[1], p[2], 2.6);
        }
        this.chamber();
        this.pillars();
        this.nest();
        this.core();
    }

    public Blueprint blueprint() {
        return this.bp;
    }

    // ------------------------------------------------------------------ superfície
    private void surfaceDecor() {
        BlockState log = st(Blocks.JUNGLE_LOG);
        // troncos caídos
        for (int i = 0; i < 2; i++) {
            double a = this.rnd.nextDouble() * Math.PI * 2;
            int x = this.cx + (int) Math.round(Math.cos(a) * 7), z = this.cz + (int) Math.round(Math.sin(a) * 7);
            boolean alongX = this.rnd.nextBoolean();
            BlockState s = log.setValue(RotatedPillarBlock.AXIS, alongX ? Direction.Axis.X : Direction.Axis.Z);
            int len = 5 + this.rnd.nextInt(3);
            for (int k = 0; k < len; k++) {
                int bx = x + (alongX ? k - len / 2 : 0), bz = z + (alongX ? 0 : k - len / 2);
                if (Math.hypot(bx - this.cx, bz - this.cz) > 4) {
                    this.bp.block(bx, this.f + 1, bz, s);
                }
            }
        }
        // tocos arranhados (descascados pela garra)
        BlockState stripped = st(Blocks.STRIPPED_JUNGLE_LOG);
        for (int i = 0; i < 4; i++) {
            double a = this.rnd.nextDouble() * Math.PI * 2;
            int x = this.cx + (int) Math.round(Math.cos(a) * (5 + this.rnd.nextInt(3)));
            int z = this.cz + (int) Math.round(Math.sin(a) * (5 + this.rnd.nextInt(3)));
            int h = 2 + this.rnd.nextInt(3);
            for (int y = 1; y <= h; y++) {
                this.bp.block(x, this.f + y, z, stripped);
            }
        }
        // ossos e caveiras na boca do túnel
        for (int i = 0; i < 6; i++) {
            double a = this.rnd.nextDouble() * Math.PI * 2;
            int x = this.cx + (int) Math.round(Math.cos(a) * (3.5 + this.rnd.nextDouble() * 3));
            int z = this.cz + (int) Math.round(Math.sin(a) * (3.5 + this.rnd.nextDouble() * 3));
            this.bp.block(x, this.f + 1, z, i < 4 ? st(Blocks.BONE_BLOCK).setValue(RotatedPillarBlock.AXIS,
                    this.rnd.nextBoolean() ? Direction.Axis.X : Direction.Axis.Z)
                    : st(Blocks.SKELETON_SKULL).setValue(SkullBlock.ROTATION, this.rnd.nextInt(16)));
        }
    }

    // ------------------------------------------------------------------ túnel
    private List<double[]> tunnelPath() {
        List<double[]> out = new ArrayList<>();
        double y0 = this.f + 1.5, y1 = this.cf + 2.5;
        for (int s = 0; s <= STEPS + 2; s++) {
            double t = Math.min(1.0, s / (double) STEPS);
            double y = y0 + (y1 - y0) * t;
            double wobble = Math.sin(s * 0.35) * 1.5;
            out.add(new double[] {this.cx + this.ux * s + this.uz * wobble, y, this.cz + this.uz * s + this.ux * wobble});
        }
        return out;
    }

    /** Casca: troca ar/líquido por pedra numa esfera (achatada por {@code squash} em y), só até {@code maxY}. */
    private void shell(double x0, double y0, double z0, double r, double squash, int maxY) {
        int ir = (int) Math.ceil(r);
        int iry = (int) Math.ceil(r * squash);
        int bx = (int) Math.floor(x0), by = (int) Math.floor(y0), bz = (int) Math.floor(z0);
        BoundingBox box = new BoundingBox(bx - ir, by - iry, bz - ir, bx + ir, Math.min(by + iry, maxY), bz + ir);
        if (box.minY() > box.maxY()) {
            return;
        }
        this.bp.custom(box, ctx -> {
            for (int x = Math.max(box.minX(), ctx.chunk.minX()); x <= Math.min(box.maxX(), ctx.chunk.maxX()); x++) {
                for (int z = Math.max(box.minZ(), ctx.chunk.minZ()); z <= Math.min(box.maxZ(), ctx.chunk.maxZ()); z++) {
                    for (int y = box.minY(); y <= box.maxY(); y++) {
                        double dx = x + 0.5 - x0, dy = (y + 0.5 - y0) / squash, dz = z + 0.5 - z0;
                        if (dx * dx + dy * dy + dz * dz > r * r) {
                            continue;
                        }
                        BlockState cur = ctx.get(x, y, z);
                        if (cur.isAir() || !cur.getFluidState().isEmpty()) {
                            ctx.set(x, y, z, this.rock.pick(x, y, z));
                        }
                    }
                }
            }
        });
    }

    private void carveSphere(double x0, double y0, double z0, double r) {
        int ir = (int) Math.ceil(r);
        int bx = (int) Math.floor(x0), by = (int) Math.floor(y0), bz = (int) Math.floor(z0);
        this.bp.shape(bx - ir, by - ir, bz - ir, bx + ir, by + ir, bz + ir, (x, y, z) -> {
            double dx = x + 0.5 - x0, dy = y + 0.5 - y0, dz = z + 0.5 - z0;
            return dx * dx + dy * dy + dz * dz <= r * r;
        }, (x, y, z) -> y > this.f ? Blocks.AIR.defaultBlockState() : Blocks.CAVE_AIR.defaultBlockState());
    }

    // ------------------------------------------------------------------ salão
    private double ell(int x, int z) {
        double dx = (x + 0.5 - this.ex) / RX, dz = (z + 0.5 - this.ez) / RZ;
        return dx * dx + dz * dz;
    }

    private void chamber() {
        int cy = this.cf + 6;
        BoundingBox box = new BoundingBox(this.ex - RX, this.cf - 1, this.ez - RZ, this.ex + RX, cy + RY + 1, this.ez + RZ);
        this.bp.custom(box, ctx -> {
            for (int x = Math.max(box.minX(), ctx.chunk.minX()); x <= Math.min(box.maxX(), ctx.chunk.maxX()); x++) {
                for (int z = Math.max(box.minZ(), ctx.chunk.minZ()); z <= Math.min(box.maxZ(), ctx.chunk.maxZ()); z++) {
                    double e = this.ell(x, z);
                    if (e > 1.0) {
                        continue;
                    }
                    int top = (int) Math.floor(cy + RY * Math.sqrt(1.0 - e));
                    int bottom = this.cf + 1;
                    if (top < bottom + 1) {
                        continue;
                    }
                    for (int y = bottom; y <= top; y++) {
                        ctx.set(x, y, z, Blocks.CAVE_AIR.defaultBlockState());
                    }
                    ctx.set(x, this.cf, z, this.caveFloor.pick(x, this.cf, z));
                    ctx.set(x, this.cf - 1, z, this.rock.pick(x, this.cf - 1, z));
                    long h = Blueprint.hash(x, 7, z, 44);
                    int roll = (int) Math.floorMod(h, 100L);
                    // teto: raízes, estalactites, shroomlight
                    if (roll < 12) {
                        ctx.set(x, top, z, st(Blocks.HANGING_ROOTS));
                    } else if (roll < 16 && top - 1 > bottom + 3) {
                        ctx.set(x, top, z, st(Blocks.POINTED_DRIPSTONE).setValue(PointedDripstoneBlock.TIP_DIRECTION, Direction.DOWN)
                                .setValue(PointedDripstoneBlock.THICKNESS, DripstoneThickness.BASE));
                        ctx.set(x, top - 1, z, st(Blocks.POINTED_DRIPSTONE).setValue(PointedDripstoneBlock.TIP_DIRECTION, Direction.DOWN)
                                .setValue(PointedDripstoneBlock.THICKNESS, DripstoneThickness.TIP));
                    } else if (roll < 20) {
                        ctx.set(x, top + 1, z, st(Blocks.SHROOMLIGHT));
                    }
                    // chão: musgo, samambaia, ossos, teias (longe do ninho e do meio)
                    double dn = Math.hypot(x - this.nx, z - this.nz);
                    if (dn < 6 || e > 0.92) {
                        continue;
                    }
                    int floorRoll = (int) Math.floorMod(Blueprint.hash(x, 9, z, 45), 1000L);
                    BlockState deco = null;
                    if (floorRoll < 140) {
                        deco = st(Blocks.MOSS_CARPET);
                    } else if (floorRoll < 190) {
                        deco = st(Blocks.FERN);
                    } else if (floorRoll < 205) {
                        deco = st(Blocks.BONE_BLOCK);
                    } else if (floorRoll < 212) {
                        deco = st(Blocks.SKELETON_SKULL).setValue(SkullBlock.ROTATION, floorRoll % 16);
                    } else if (floorRoll < 222) {
                        deco = st(Blocks.COBWEB);
                    } else if (floorRoll < 232) {
                        deco = st(Blocks.POINTED_DRIPSTONE);
                    }
                    if (deco != null && (deco.is(Blocks.FERN) ? ctx.get(x, this.cf, z).is(net.minecraft.tags.BlockTags.DIRT) : true)) {
                        ctx.set(x, this.cf + 1, z, deco);
                    }
                }
            }
        });
    }

    private void pillars() {
        for (int i = 0; i < 3; i++) {
            double a = this.rnd.nextDouble() * Math.PI * 2;
            double d = 5 + this.rnd.nextDouble() * 5;
            int px = this.ex + (int) Math.round(Math.cos(a) * d), pz = this.ez + (int) Math.round(Math.sin(a) * d);
            if (Math.hypot(px - this.nx, pz - this.nz) < 7) {
                continue;
            }
            double r = 1.3 + this.rnd.nextDouble() * 0.6;
            this.bp.shape(px - 2, this.cf + 1, pz - 2, px + 2, this.cf + 6 + RY + 1, pz + 2, (x, y, z) -> {
                double w = r + (y < this.cf + 3 ? 0.6 : 0) + (y > this.cf + 11 ? 0.5 : 0);
                return Math.hypot(x + 0.5 - (px + 0.5), z + 0.5 - (pz + 0.5)) <= w && this.ell(x, z) < 0.9;
            }, this.rock);
        }
    }

    // ------------------------------------------------------------------ ninho
    private void nest() {
        int y = this.cf + 1;
        Palette rim = Palette.mix(46, this.leaves, 60, st(Blocks.BONE_BLOCK), 15, st(Blocks.HAY_BLOCK), 10, st(Blocks.ROOTED_DIRT), 15);
        this.bp.shape(this.nx - 5, y, this.nz - 5, this.nx + 5, y + 1, this.nz + 5, (x, yy, z) -> {
            double d = Math.hypot(x - this.nx, z - this.nz);
            return yy == y ? d <= 5.0 && d > 3.2 : d <= 4.6 && d > 3.6;
        }, rim);
        this.bp.shape(this.nx - 3, y - 1, this.nz - 3, this.nx + 3, y - 1, this.nz + 3,
                (x, yy, z) -> Math.hypot(x - this.nx, z - this.nz) <= 3.2, Palette.mix(47, st(Blocks.ROOTED_DIRT), 50, st(Blocks.MOSS_BLOCK), 50));
        this.bp.shape(this.nx - 3, y, this.nz - 3, this.nx + 3, y, this.nz + 3,
                (x, yy, z) -> Math.hypot(x - this.nx, z - this.nz) <= 3.2 && Math.floorMod(Blueprint.hash(x, yy, z, 48), 3L) == 0,
                Palette.mix(49, st(Blocks.MOSS_CARPET), 70, st(Blocks.BONE_BLOCK), 30));
        // caveiras na borda e o baú meio enterrado no fundo do ninho
        for (int i = 0; i < 3; i++) {
            double a = this.rnd.nextDouble() * Math.PI * 2;
            this.bp.block(this.nx + (int) Math.round(Math.cos(a) * 4), y + 2, this.nz + (int) Math.round(Math.sin(a) * 4),
                    st(Blocks.SKELETON_SKULL).setValue(SkullBlock.ROTATION, this.rnd.nextInt(16)));
        }
        int bx = this.nx + this.ux * 3, bz = this.nz + this.uz * 3;
        Direction facing = this.ux > 0 ? Direction.WEST : this.ux < 0 ? Direction.EAST : this.uz > 0 ? Direction.NORTH : Direction.SOUTH;
        this.bp.chest(bx, y, bz, facing, Blueprint.loot("chests/mapinguari_cave"));
    }

    // ------------------------------------------------------------------ coração da toca
    private void core() {
        int x = this.ex, y = this.cf - 3, z = this.ez;
        int dx = this.nx - x, dz = this.nz - z;
        this.bp.custom(new BoundingBox(x, y, z, x, y, z), ctx -> {
            if (!ctx.inside(x, y, z)) {
                return;
            }
            ctx.set(x, y, z, LoreContent.LAIR_CORE.get().defaultBlockState());
            if (ctx.level.getBlockEntity(new BlockPos(x, y, z)) instanceof LairCoreBlockEntity be) {
                be.setup("mapinguari", dx, 4, dz); // levanta do ninho
            }
        });
    }
}

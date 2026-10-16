package com.mistech.mistechmod.realm.dream.structure;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.mistech.mistechmod.realm.goblin.Blueprint;
import com.mistech.mistechmod.registry.DreamMobs;
import com.mistech.mistechmod.registry.DreamRealmContent;
import com.mistech.mistechmod.registry.DreamStructures;
import com.mistech.mistechmod.registry.GnomeContent;
import com.mistech.mistechmod.registry.RealmContent;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.RandomizableContainer;

/**
 * Plantas das estruturas do Sonho (coordenadas do mundo, aplicadas chunk a chunk pelo {@link Blueprint}).
 * <ul>
 * <li><b>Vilarejo Gnomo</b>: clareira com fogueira, 4-6 casas-cogumelo (tronco de cogumelo, chapéu Onírico rosa/ciano, porta para a
 * praça, janelas, mesa e barril), caminhos, cercado com Ovelhas-de-Contar, gnomos e um Gnomo Mercador.</li>
 * <li><b>Vila Gnoma em Ruínas</b>: a mesma planta, quebrada, com chapéus apodrecidos, teias e Gnomos Corrompidos.</li>
 * <li><b>Casa Errada</b>: casa comprida por fora; por dentro três quartos iguais em fila — passar para o terceiro devolve você para o
 * segundo (3 vezes). No fim, o quarto com as camas, um baú... e o Bicho-Papão.</li>
 * <li><b>Escadaria Sem Fim</b>: torre com escada em caracol de 3 voltas; ao começar a terceira, você volta uma volta (3 vezes).
 * Lá em cima, um baú.</li>
 * </ul>
 */
public final class DreamPlans {
    private DreamPlans() {}

    public static Blueprint build(String kind, long seed, BlockPos center) {
        Blueprint bp = new Blueprint();
        Random rnd = new Random(seed);
        switch (kind) {
            case "ruined_hamlet" -> hamlet(bp, rnd, center, true);
            case "wrong_house" -> wrongHouse(bp, rnd, center);
            case "endless_stair" -> endlessStair(bp, rnd, center);
            case "fractal_temple" -> DreamPlansDeep.fractalTemple(bp, rnd, center);
            case "memory_library" -> DreamPlansDeep.memoryLibrary(bp, rnd, center);
            default -> hamlet(bp, rnd, center, false);
        }
        return bp;
    }

    // ================================================================== comum

    private static BlockState air() {
        return Blocks.AIR.defaultBlockState();
    }

    /** Aplaina um círculo em {@code top} (bloco do chão) com rampa suave até o terreno natural. */
    static void flatten(Blueprint bp, int cx, int cz, int top, int radius, int blend, Blueprint.Palette ground) {
        int r = radius + blend;
        BoundingBox box = new BoundingBox(cx - r, top - 30, cz - r, cx + r, top + 40, cz + r);
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
                    boolean inside = d <= radius;
                    int target = top;
                    if (!inside) {
                        double s = (d - radius) / blend;
                        s = s * s * (3 - 2 * s);
                        target = (int) Math.round(top + (natural - top) * s);
                    }
                    if (natural > target) {
                        for (int y = target + 1; y <= Math.min(natural, target + 40); y++) {
                            ctx.set(x, y, z, air());
                        }
                    } else {
                        for (int y = Math.max(natural + 1, target - 30); y < target; y++) {
                            ctx.set(x, y, z, y < target - 3 ? Blocks.STONE.defaultBlockState() : Blocks.DIRT.defaultBlockState());
                        }
                    }
                    if (inside || natural != target) {
                        ctx.set(x, target, z, ground.pick(x, target, z));
                    }
                }
            }
        });
    }

    static void barrel(Blueprint bp, int x, int y, int z, Direction facing, String loot) {
        bp.custom(new BoundingBox(x, y, z, x, y, z), ctx -> {
            if (!ctx.inside(x, y, z)) {
                return;
            }
            ctx.set(x, y, z, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, facing));
            RandomizableContainer.setBlockEntityLootTable(ctx.level, ctx.random, new BlockPos(x, y, z), Blueprint.loot(loot));
        });
    }

    static void door(Blueprint bp, int x, int y, int z, Block block, Direction facing) {
        BlockState d = block.defaultBlockState().setValue(DoorBlock.FACING, facing);
        bp.block(x, y, z, d.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        bp.block(x, y + 1, z, d.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
    }

    /** Âncora Onírica escondida em {@code (x,y,z)}, vigiando a caixa relativa e com o destino relativo. */
    static void anchor(Blueprint bp, int x, int y, int z, int mode, int[] zone, int[] offset, int loops, String message) {
        bp.custom(new BoundingBox(x, y, z, x, y, z), ctx -> {
            if (!ctx.inside(x, y, z)) {
                return;
            }
            ctx.set(x, y, z, DreamStructures.ANCHOR.get().defaultBlockState());
            if (ctx.level.getBlockEntity(new BlockPos(x, y, z)) instanceof DreamAnchorBlockEntity be) {
                be.configure(mode, zone[0], zone[1], zone[2], zone[3], zone[4], zone[5], offset[0], offset[1], offset[2], loops, message);
            }
        });
    }

    // ================================================================== Vilarejo Gnomo (e as ruínas)

    private record House(int x, int z, Direction door, int r) {}

    private static void hamlet(Blueprint bp, Random rnd, BlockPos center, boolean ruined) {
        int cx = center.getX(), cz = center.getZ(), y0 = center.getY();
        int top = y0 - 1;
        long salt = rnd.nextLong();
        Blueprint.Palette ground = ruined
                ? Blueprint.Palette.mix(salt, RealmContent.WITHERED_GRASS.get().defaultBlockState(), 6, Blocks.COARSE_DIRT.defaultBlockState(), 2,
                        Blocks.PODZOL.defaultBlockState(), 1)
                : Blueprint.Palette.of(Blocks.GRASS_BLOCK.defaultBlockState());
        flatten(bp, cx, cz, top, 21, 7, ground);

        // praça e fogueira
        Blueprint.Palette plaza = ruined
                ? Blueprint.Palette.mix(salt + 1, Blocks.COBBLESTONE.defaultBlockState(), 3, Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 3,
                        Blocks.GRAVEL.defaultBlockState(), 2)
                : Blueprint.Palette.mix(salt + 1, Blocks.STONE_BRICKS.defaultBlockState(), 4, Blocks.MOSSY_STONE_BRICKS.defaultBlockState(), 2,
                        Blocks.DIRT_PATH.defaultBlockState(), 2);
        bp.shape(cx - 4, top, cz - 4, cx + 4, top, cz + 4, (x, y, z) -> Math.hypot(x - cx, z - cz) <= 4.2, plaza);
        bp.block(cx, y0, cz, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, !ruined));
        if (!ruined) {
            for (Direction d : Direction.Plane.HORIZONTAL) {
                int px = cx + d.getStepX() * 4 + d.getClockWise().getStepX() * 2, pz = cz + d.getStepZ() * 4 + d.getClockWise().getStepZ() * 2;
                bp.fill(px, y0, pz, px, y0 + 1, pz, Blocks.SPRUCE_FENCE.defaultBlockState());
                bp.block(px, y0 + 2, pz, Blocks.LANTERN.defaultBlockState());
            }
        }

        // casas em volta
        int n = 4 + rnd.nextInt(3);
        double a0 = rnd.nextDouble() * Math.PI * 2;
        List<House> houses = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            double a = a0 + i * Math.PI * 2 / n + (rnd.nextDouble() - 0.5) * 0.3;
            int dist = 12 + rnd.nextInt(3);
            int hx = cx + (int) Math.round(Math.cos(a) * dist), hz = cz + (int) Math.round(Math.sin(a) * dist);
            int ddx = cx - hx, ddz = cz - hz;
            Direction door = Math.abs(ddx) > Math.abs(ddz) ? (ddx > 0 ? Direction.EAST : Direction.WEST) : (ddz > 0 ? Direction.SOUTH : Direction.NORTH);
            houses.add(new House(hx, hz, door, 3));
        }
        // caminhos (antes das casas, para a casa ficar por cima)
        Blueprint.Palette path = ruined ? Blueprint.Palette.mix(salt + 2, Blocks.COARSE_DIRT.defaultBlockState(), 2, Blocks.GRAVEL.defaultBlockState(), 1)
                : Blueprint.Palette.of(Blocks.DIRT_PATH.defaultBlockState());
        for (House h : houses) {
            int dx = h.x + h.door.getStepX() * (h.r + 1), dz = h.z + h.door.getStepZ() * (h.r + 1);
            line(bp, dx, dz, cx, cz, top, path);
        }
        Block cap = rnd.nextBoolean() ? DreamRealmContent.PINK_DREAM_CAP.get() : DreamRealmContent.CYAN_DREAM_CAP.get();
        for (House h : houses) {
            Block thisCap = ruined ? Blocks.BROWN_MUSHROOM_BLOCK : (rnd.nextFloat() < 0.5F ? cap
                    : (cap == DreamRealmContent.PINK_DREAM_CAP.get() ? DreamRealmContent.CYAN_DREAM_CAP.get() : DreamRealmContent.PINK_DREAM_CAP.get()));
            mushroomHouse(bp, rnd, h, y0, thisCap, ruined, salt);
        }

        if (ruined) {
            for (int i = 0; i < 4; i++) {
                double a = rnd.nextDouble() * Math.PI * 2;
                bp.mob(cx + Math.cos(a) * 5, y0, cz + Math.sin(a) * 5, DreamMobs.CORRUPTED_GNOME, null);
            }
            // teias e Hera-Insone espalhadas
            for (int i = 0; i < 14; i++) {
                int x = cx + rnd.nextInt(33) - 16, z = cz + rnd.nextInt(33) - 16;
                if (clearOfHouses(houses, x, z, 6) && Math.hypot(x - cx, z - cz) > 5) {
                    bp.block(x, y0, z, rnd.nextBoolean() ? DreamRealmContent.SLEEPLESS_IVY.get().defaultBlockState() : Blocks.COBWEB.defaultBlockState());
                }
            }
            return;
        }

        // cercado das Ovelhas-de-Contar, entre duas casas
        // procura um vão entre as casas (meio-caminho angular), do mais perto para o mais longe da praça
        int px = Integer.MIN_VALUE, pz = 0;
        search:
        for (int dist = 16; dist <= 19; dist++) {
            for (int k = 0; k < n; k++) {
                double pa = a0 + (k + 0.5) * Math.PI * 2 / n;
                int tx = cx + (int) Math.round(Math.cos(pa) * dist), tz = cz + (int) Math.round(Math.sin(pa) * dist);
                if (clearOfHouses(houses, tx, tz, 7)) {
                    px = tx;
                    pz = tz;
                    break search;
                }
            }
        }
        if (px != Integer.MIN_VALUE) {
            BlockState fence = Blocks.OAK_FENCE.defaultBlockState();
            for (int i = -2; i <= 2; i++) {
                bp.block(px + i, y0, pz - 2, fence);
                bp.block(px + i, y0, pz + 2, fence);
                bp.block(px - 2, y0, pz + i, fence);
                bp.block(px + 2, y0, pz + i, fence);
            }
            int gx = cx - px, gz = cz - pz;
            Direction gate = Math.abs(gx) > Math.abs(gz) ? (gx > 0 ? Direction.EAST : Direction.WEST) : (gz > 0 ? Direction.SOUTH : Direction.NORTH);
            bp.block(px + gate.getStepX() * 2, y0, pz + gate.getStepZ() * 2,
                    Blocks.OAK_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.FACING, gate));
            bp.block(px, y0, pz, Blocks.HAY_BLOCK.defaultBlockState());
            bp.mob(px - 1 + 0.5, y0, pz - 1 + 0.5, DreamMobs.COUNTING_SHEEP, null);
            bp.mob(px + 1 + 0.5, y0, pz + 1 + 0.5, DreamMobs.COUNTING_SHEEP, null);
        }
        // flores
        for (int i = 0; i < 18; i++) {
            int x = cx + rnd.nextInt(37) - 18, z = cz + rnd.nextInt(37) - 18;
            if (clearOfHouses(houses, x, z, 6) && Math.hypot(x - cx, z - cz) > 5 && Math.hypot(x - px, z - pz) > 3.5 && Math.hypot(x - cx, z - cz) < 19) {
                Block flower = switch (rnd.nextInt(3)) {
                    case 0 -> DreamRealmContent.DREAM_POPPY.get();
                    case 1 -> DreamRealmContent.DREAMFLOWER.get();
                    default -> DreamRealmContent.PUFFBALL.get();
                };
                bp.custom(new BoundingBox(x, y0, z, x, y0, z), ctx -> {
                    if (ctx.inside(x, y0, z) && ctx.get(x, y0, z).isAir() && ctx.get(x, y0 - 1, z).is(Blocks.GRASS_BLOCK)) {
                        ctx.set(x, y0, z, flower.defaultBlockState());
                    }
                });
            }
        }
        // moradores
        for (int i = 0; i < 3; i++) {
            double a = rnd.nextDouble() * Math.PI * 2;
            bp.mob(cx + Math.cos(a) * 6, y0, cz + Math.sin(a) * 6, GnomeContent.GNOME, null);
        }
        bp.mob(cx + 2.5, y0, cz + 0.5, DreamMobs.GNOME_MERCHANT, null);
    }

    private static boolean clearOfHouses(List<House> houses, int x, int z, double margin) {
        for (House h : houses) {
            if (Math.hypot(x - h.x, z - h.z) < margin) {
                return false;
            }
        }
        return true;
    }

    /** Linha de caminho (1 de largura + vizinhos de vez em quando) de (x0,z0) até (x1,z1). */
    private static void line(Blueprint bp, int x0, int z0, int x1, int z1, int y, Blueprint.Palette palette) {
        int steps = Math.max(Math.abs(x1 - x0), Math.abs(z1 - z0));
        for (int i = 0; i <= steps; i++) {
            int x = x0 + Math.round((x1 - x0) * i / (float) Math.max(1, steps));
            int z = z0 + Math.round((z1 - z0) * i / (float) Math.max(1, steps));
            bp.block(x, y, z, palette.pick(x, y, z));
            bp.block(x + (i % 2), y, z + ((i + 1) % 2), palette.pick(x + 1, y, z));
        }
    }

    private static void mushroomHouse(Blueprint bp, Random rnd, House h, int y0, Block cap, boolean ruined, long salt) {
        int hx = h.x, hz = h.z, r = h.r;
        int rc = r + 2;
        BlockState stem = Blocks.MUSHROOM_STEM.defaultBlockState();
        Blueprint.Palette wall = ruined ? Blueprint.Palette.mix(salt + hx * 31L + hz, stem, 6, air(), 3, Blocks.COBWEB.defaultBlockState(), 1)
                : Blueprint.Palette.of(stem);
        Blueprint.Palette roof = ruined ? Blueprint.Palette.mix(salt + hx * 17L + hz * 3L, cap.defaultBlockState(), 5, air(), 4)
                : Blueprint.Palette.of(cap.defaultBlockState());
        // chão e interior
        bp.shape(hx - r, y0 - 1, hz - r, hx + r, y0 - 1, hz + r, (x, y, z) -> Math.hypot(x - hx, z - hz) < r + 0.5,
                Blueprint.Palette.of(Blocks.SPRUCE_PLANKS.defaultBlockState()));
        bp.shape(hx - rc, y0, hz - rc, hx + rc, y0 + 9, hz + rc, (x, y, z) -> Math.hypot(x - hx, z - hz) < r - 0.4 && y <= y0 + 4,
                Blueprint.Palette.of(air()));
        // paredes
        bp.shape(hx - r, y0, hz - r, hx + r, y0 + 3, hz + r, (x, y, z) -> {
            double d = Math.hypot(x - hx, z - hz);
            return d >= r - 0.4 && d < r + 0.5;
        }, wall);
        // chapéu: cúpula oca + aba
        bp.shape(hx - rc, y0 + 4, hz - rc, hx + rc, y0 + 8, hz + rc, (x, y, z) -> {
            double e = ((x - hx) * (x - hx) + (z - hz) * (z - hz)) / (double) (rc * rc) + Math.pow((y - y0 - 4) / 4.2, 2);
            return e <= 1.0 && e > 0.5;
        }, roof);
        bp.shape(hx - rc, y0 + 4, hz - rc, hx + rc, y0 + 4, hz + rc, (x, y, z) -> {
            double d = Math.hypot(x - hx, z - hz);
            return d <= rc + 0.3 && d >= r - 0.5;
        }, roof);
        // porta e janelas
        Direction door = h.door;
        int dx = hx + door.getStepX() * r, dz = hz + door.getStepZ() * r;
        if (ruined) {
            bp.fill(dx, y0, dz, dx, y0 + 1, dz, air());
        } else {
            door(bp, dx, y0, dz, Blocks.SPRUCE_DOOR, door);
        }
        for (Direction side : new Direction[] {door.getClockWise(), door.getCounterClockWise()}) {
            int wx = hx + side.getStepX() * r, wz = hz + side.getStepZ() * r;
            bp.block(wx, y0 + 1, wz, ruined ? air() : Blocks.GLASS_PANE.defaultBlockState());
            bp.block(wx, y0 + 2, wz, ruined ? air() : Blocks.GLASS_PANE.defaultBlockState());
        }
        // móveis no fundo
        Direction back = door.getOpposite();
        int bx = hx + back.getStepX() * (r - 1), bz = hz + back.getStepZ() * (r - 1);
        Direction s = door.getClockWise();
        if (!ruined) {
            bp.block(bx + s.getStepX(), y0, bz + s.getStepZ(), Blocks.CRAFTING_TABLE.defaultBlockState());
            bp.block(bx + s.getStepX(), y0 + 1, bz + s.getStepZ(), Blocks.LANTERN.defaultBlockState());
        } else {
            bp.block(bx + s.getStepX(), y0, bz + s.getStepZ(), Blocks.COBWEB.defaultBlockState());
        }
        barrel(bp, bx - s.getStepX(), y0, bz - s.getStepZ(), Direction.UP, ruined ? "chests/ruined_hamlet" : "chests/gnome_hamlet");
        if (!ruined && rnd.nextBoolean()) {
            bp.block(bx, y0, bz, Blocks.POTTED_RED_MUSHROOM.defaultBlockState());
        }
    }

    // ================================================================== Casa Errada

    private static void wrongHouse(Blueprint bp, Random rnd, BlockPos center) {
        int y0 = center.getY();
        int top = y0 - 1;
        long salt = rnd.nextLong();
        int x0 = center.getX() - 15, z0 = center.getZ() - 4;
        int len = 30, wid = 8; // paredes em x0..x0+30 e z0..z0+8
        flatten(bp, center.getX(), center.getZ(), top, 19, 6,
                Blueprint.Palette.mix(salt, RealmContent.WITHERED_GRASS.get().defaultBlockState(), 5, Blocks.COARSE_DIRT.defaultBlockState(), 1));
        BlockState planks = Blocks.OAK_PLANKS.defaultBlockState();
        BlockState log = Blocks.DARK_OAK_LOG.defaultBlockState();
        Blueprint.Palette walls = Blueprint.Palette.mix(salt + 1, planks, 8, Blocks.STRIPPED_OAK_LOG.defaultBlockState(), 1);
        // fundação, chão, paredes, teto
        bp.fill(x0, top - 2, z0, x0 + len, top, z0 + wid, Blocks.STONE_BRICKS.defaultBlockState());
        bp.fill(x0 + 1, top, z0 + 1, x0 + len - 1, top, z0 + wid - 1, Blocks.SPRUCE_PLANKS.defaultBlockState());
        bp.fill(x0, y0, z0, x0 + len, y0 + 4, z0 + wid, walls);
        bp.air(x0 + 1, y0, z0 + 1, x0 + len - 1, y0 + 3, z0 + wid - 1);
        bp.fill(x0 + 1, y0 + 4, z0 + 1, x0 + len - 1, y0 + 4, z0 + wid - 1, Blocks.SPRUCE_PLANKS.defaultBlockState());
        for (int lx : new int[] {0, 6, 11, 16, 21, 30}) {
            bp.fill(x0 + lx, y0, z0, x0 + lx, y0 + 4, z0, log);
            bp.fill(x0 + lx, y0, z0 + wid, x0 + lx, y0 + 4, z0 + wid, log);
        }
        // divisórias com vão de porta no meio
        int mid = z0 + 4;
        for (int lx : new int[] {6, 11, 16, 21}) {
            bp.fill(x0 + lx, y0, z0 + 1, x0 + lx, y0 + 3, z0 + wid - 1, planks);
            bp.fill(x0 + lx, y0, mid, x0 + lx, y0 + 1, mid, air());
        }
        // telhado de duas águas ao longo de x
        for (int k = 0; k <= 4; k++) {
            int y = y0 + 5 + k;
            int za = z0 - 1 + k, zb = z0 + wid + 1 - k;
            if (za > zb) {
                break;
            }
            BlockState north = Blocks.DARK_OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH);
            BlockState south = Blocks.DARK_OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);
            bp.fill(x0 - 1, y, za, x0 + len + 1, y, za, north);
            bp.fill(x0 - 1, y, zb, x0 + len + 1, y, zb, south);
            if (zb - za > 1) {
                bp.fill(x0, y, za + 1, x0 + len, y, zb - 1, Blocks.DARK_OAK_PLANKS.defaultBlockState());
            }
        }
        // porta da frente (oeste) e caminho
        door(bp, x0, y0, mid, Blocks.DARK_OAK_DOOR, Direction.WEST);
        bp.fill(x0 - 6, top, mid, x0 - 1, top, mid, Blocks.GRAVEL.defaultBlockState());
        bp.fill(x0 - 3, y0, mid + 2, x0 - 3, y0 + 2, mid + 2, Blocks.DARK_OAK_FENCE.defaultBlockState());
        bp.block(x0 - 3, y0 + 3, mid + 2, Blocks.REDSTONE_LAMP.defaultBlockState());

        // hall de entrada (x 1..5)
        bp.block(x0 + 3, y0 + 3, mid, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        bp.block(x0 + 1, y0, z0 + 1, Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST));
        bp.block(x0 + 5, y0, z0 + wid - 1, Blocks.BOOKSHELF.defaultBlockState());
        bp.block(x0 + 5, y0 + 1, z0 + wid - 1, Blocks.BOOKSHELF.defaultBlockState());

        // três quartos idênticos (x 7..10, 12..15, 17..20)
        for (int b : new int[] {7, 12, 17}) {
            int bx = x0 + b;
            // janela na parede norte
            bp.fill(bx + 1, y0 + 1, z0, bx + 2, y0 + 2, z0, Blocks.GLASS_PANE.defaultBlockState());
            // mesa e cadeira
            bp.block(bx + 1, y0, z0 + 6, Blocks.OAK_FENCE.defaultBlockState());
            bp.block(bx + 1, y0 + 1, z0 + 6, Blocks.OAK_PRESSURE_PLATE.defaultBlockState());
            bp.block(bx + 2, y0, z0 + 6, Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST));
            // a mesma cadeira e mesa, de cabeça para baixo no teto
            bp.block(bx + 1, y0 + 3, z0 + 2, Blocks.OAK_FENCE.defaultBlockState());
            bp.block(bx + 2, y0 + 3, z0 + 2, Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST).setValue(StairBlock.HALF, Half.TOP));
            // porta que dá na parede
            door(bp, bx + 2, y0, z0 + wid - 1, Blocks.OAK_DOOR, Direction.NORTH);
            // luz
            bp.block(bx + 1, y0 + 3, mid, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
            bp.block(bx, y0, z0 + 1, Blocks.FLOWER_POT.defaultBlockState());
        }
        // o laço: passar pela porta do 3º quarto (x 16) devolve para a porta do 2º (x 11)
        anchor(bp, x0 + 16, y0 + 4, mid, DreamAnchorBlockEntity.LOOP, new int[] {0, -4, -1, 0, -2, 1}, new int[] {-5, 0, 0}, 3,
                "message.mistech.wrong_house.loop");

        // quarto (x 22..29): escuro, duas camas, baú
        bp.fill(x0 + 22, top, z0 + 1, x0 + 29, top, z0 + wid - 1, Blocks.SPRUCE_PLANKS.defaultBlockState());
        bp.fill(x0 + 22, y0 - 1 + 1, z0 + 1, x0 + 29, y0, z0 + wid - 1, air());
        bp.fill(x0 + 23, y0, z0 + 3, x0 + 28, y0, z0 + 5, Blocks.BLACK_CARPET.defaultBlockState());
        for (int bz : new int[] {z0 + 2, z0 + 6}) {
            BlockState bed = Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.EAST);
            bp.block(x0 + 27, y0, bz, bed.setValue(BedBlock.PART, BedPart.FOOT));
            bp.block(x0 + 28, y0, bz, bed.setValue(BedBlock.PART, BedPart.HEAD));
        }
        bp.chest(x0 + 29, y0, mid, Direction.WEST, Blueprint.loot("chests/wrong_house"));
        bp.block(x0 + 29, y0, z0 + 1, Blocks.BARREL.defaultBlockState());
        bp.block(x0 + 29, y0, z0 + wid - 1, Blocks.BARREL.defaultBlockState());
        // o Bicho-Papão sai de baixo das camas quando você chega perto delas
        anchor(bp, x0 + 26, top - 1, mid, DreamAnchorBlockEntity.BOGEYMAN, new int[] {-1, 1, -3, 3, 3, 3}, new int[] {1, 2, 0}, 2,
                "message.mistech.wrong_house.bogeyman");
    }

    // ================================================================== Escadaria Sem Fim

    public static final int LAP = 16;
    public static final int LAPS = 3;

    /** Índice do degrau (0..15) pela posição em volta do centro. */
    static int stepIndex(int dx, int dz) {
        double t = (Math.atan2(dz, dx) + Math.PI) / (2 * Math.PI);
        t -= Math.floor(t);
        return Math.min(LAP - 1, (int) Math.floor(t * LAP));
    }

    private static void endlessStair(Blueprint bp, Random rnd, BlockPos center) {
        int cx = center.getX(), cz = center.getZ(), y0 = center.getY();
        long salt = rnd.nextLong();
        flatten(bp, cx, cz, y0 - 1, 8, 5,
                Blueprint.Palette.mix(salt, RealmContent.WITHERED_GRASS.get().defaultBlockState(), 4, Blocks.GRAVEL.defaultBlockState(), 1));
        int topY = y0 + LAP * LAPS; // chão do topo em topY - 1
        Blueprint.Palette bricks = Blueprint.Palette.mix(salt + 1, Blocks.DEEPSLATE_BRICKS.defaultBlockState(), 8,
                Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState(), 2, Blocks.DEEPSLATE_TILES.defaultBlockState(), 2);
        // fundação, paredes (9×9) e miolo (3×3)
        bp.fill(cx - 5, y0 - 6, cz - 5, cx + 5, y0 - 1, cz + 5, Blocks.COBBLED_DEEPSLATE.defaultBlockState());
        bp.fill(cx - 4, y0 - 1, cz - 4, cx + 4, y0 - 1, cz + 4, Blocks.POLISHED_DEEPSLATE.defaultBlockState());
        bp.shape(cx - 4, y0, cz - 4, cx + 4, topY + 5, cz + 4,
                (x, y, z) -> Math.max(Math.abs(x - cx), Math.abs(z - cz)) == 4, bricks);
        bp.air(cx - 3, y0, cz - 3, cx + 3, topY + 4, cz + 3);
        bp.fill(cx - 1, y0, cz - 1, cx + 1, topY - 2, cz + 1, Blocks.POLISHED_DEEPSLATE.defaultBlockState());
        // escada em caracol (anéis 2 e 3), 16 degraus por volta, voltas idênticas
        for (int lap = 0; lap < LAPS; lap++) {
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    int ring = Math.max(Math.abs(dx), Math.abs(dz));
                    if (ring < 2) {
                        continue;
                    }
                    int idx = stepIndex(dx, dz);
                    int y = y0 + lap * LAP + idx;
                    // sobe no sentido em que o ângulo cresce: tangente (-dz, dx)
                    int tx = -dz, tz = dx;
                    Direction facing = Math.abs(tx) >= Math.abs(tz) ? (tx > 0 ? Direction.EAST : Direction.WEST) : (tz > 0 ? Direction.SOUTH : Direction.NORTH);
                    bp.block(cx + dx, y, cz + dz, Blocks.POLISHED_DEEPSLATE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, facing));
                    if (y - 1 >= y0) {
                        bp.block(cx + dx, y - 1, cz + dz, Blocks.POLISHED_DEEPSLATE.defaultBlockState());
                    }
                }
            }
            // janelas e luzes, iguais em toda volta
            int wy = y0 + lap * LAP + 5;
            for (Direction d : Direction.Plane.HORIZONTAL) {
                bp.fill(cx + d.getStepX() * 4, wy, cz + d.getStepZ() * 4, cx + d.getStepX() * 4, wy + 1, cz + d.getStepZ() * 4,
                        Blocks.IRON_BARS.defaultBlockState());
                bp.block(cx + d.getStepX() * 2, y0 + lap * LAP + 9, cz + d.getStepZ() * 2, Blocks.SOUL_LANTERN.defaultBlockState());
            }
        }
        // entrada (oeste), chegando no primeiro degrau
        bp.fill(cx - 4, y0, cz, cx - 4, y0 + 1, cz, air());
        bp.fill(cx - 4, y0, cz + 1, cx - 4, y0 + 1, cz + 1, air());
        // topo: chão por cima das 12 primeiras posições da última volta (as 4 últimas ficam abertas para a escada chegar)
        bp.shape(cx - 3, topY - 1, cz - 3, cx + 3, topY - 1, cz + 3, (x, y, z) -> {
            int dx = x - cx, dz = z - cz;
            return Math.max(Math.abs(dx), Math.abs(dz)) < 2 || stepIndex(dx, dz) < 12;
        }, Blueprint.Palette.of(Blocks.DEEPSLATE_TILES.defaultBlockState()));
        bp.chest(cx, topY, cz, Direction.EAST, Blueprint.loot("chests/endless_stair"));
        bp.block(cx + 1, topY, cz + 1, Blocks.SOUL_LANTERN.defaultBlockState());
        bp.block(cx - 1, topY, cz + 1, DreamRealmContent.EYE_FLOWER.get().defaultBlockState());
        bp.block(cx - 1, topY - 1, cz + 1, Blocks.MOSS_BLOCK.defaultBlockState());
        // telhado em pirâmide
        for (int k = 0; k <= 4; k++) {
            bp.fill(cx - 5 + k, topY + 5 + k, cz - 5 + k, cx + 5 - k, topY + 5 + k, cz + 5 - k, Blocks.DEEPSLATE_TILES.defaultBlockState());
        }
        bp.block(cx, topY + 10, cz, Blocks.SOUL_LANTERN.defaultBlockState());
        // o laço: ao começar a 3ª volta, volta uma volta inteira para baixo (3 vezes)
        int ay = y0 + LAP * (LAPS - 1) + 1;
        anchor(bp, cx, ay, cz, DreamAnchorBlockEntity.LOOP, new int[] {-3, -1, -2, -2, 0, 0}, new int[] {0, -LAP, 0}, 3,
                "message.mistech.endless_stair.loop");
    }
}

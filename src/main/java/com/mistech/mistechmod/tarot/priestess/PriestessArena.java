package com.mistech.mistechmod.tarot.priestess;

import com.mistech.mistechmod.tarot.ArenaLayout;
import com.mistech.mistechmod.tarot.ArenaTools;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.Vec3;

/**
 * Arena da Sacerdotisa: o Templo do Véu.
 * <ul>
 * <li>Disco de raio {@link #RADIUS}, chão em xadrez preto e branco (os dois pilares), anel de prismarinho e uma lua
 * crescente de lanternas do mar no centro (onde o jogador é jogado no modo Boaz).</li>
 * <li>Ao norte, o <b>trono</b> num estrado, entre os pilares <b>Boaz</b> (preto, à esquerda de quem olha) e
 * <b>Jachin</b> (branco, à direita), com o véu de vidro azul e romãs atrás. O topo do pilar do modo ativo acende.</li>
 * <li>Mureta em volta com colunas alternadas e lanternas.</li>
 * </ul>
 */
public class PriestessArena implements ArenaLayout {
    public static final PriestessArena INSTANCE = new PriestessArena();

    public static final int RADIUS = 21;
    /** Onde fica o trono (relativo ao centro). */
    public static final int THRONE_Z = -16;
    /** Altura do estrado (o chão do estrado fica em y + DAIS). */
    public static final int DAIS = 1;
    public static final int PILLAR_X = 4;
    public static final int PILLAR_HEIGHT = 8;

    @Override
    public double leashRadius() {
        return RADIUS + 3.0;
    }

    @Override
    public int clearRadius() {
        return ArenaTools.MAX_CLEAR_RADIUS;
    }

    /** Assento do trono: posição da entidade sentada (os quadris apoiados no degrau da escada de quartzo). */
    public static Vec3 throneSeat(BlockPos center) {
        return new Vec3(center.getX() + 0.5, center.getY() + DAIS + 1 + 0.5 - 0.84, center.getZ() + THRONE_Z + 0.7);
    }

    /** Topo (o bloco que acende) dos pilares. */
    public static BlockPos pillarTop(BlockPos center, boolean boaz) {
        return center.offset(boaz ? -PILLAR_X : PILLAR_X, DAIS + PILLAR_HEIGHT, THRONE_Z);
    }

    @Override
    public Vec3 arrival(BlockPos center, int index) {
        double x = (index % 2 == 0 ? 1 : -1) * ((index + 1) / 2) * 2.0;
        return new Vec3(center.getX() + 0.5 + x, center.getY() + 1.0, center.getZ() + 0.5 + 13.0);
    }

    @Override
    public Vec3 bossSpawn(BlockPos center) {
        return throneSeat(center);
    }

    // ----------------------------------------------------------------- construção

    @Override
    public void build(ServerLevel level, BlockPos center) {
        ArenaTools.clear(level, center, this);
        int flags = Block.UPDATE_CLIENTS;
        BlockState black = Blocks.POLISHED_BLACKSTONE.defaultBlockState();
        BlockState white = Blocks.CALCITE.defaultBlockState();
        for (int dx = -RADIUS - 1; dx <= RADIUS + 1; dx++) {
            for (int dz = -RADIUS - 1; dz <= RADIUS + 1; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > RADIUS + 0.5) {
                    continue;
                }
                BlockPos pos = center.offset(dx, 0, dz);
                BlockState floor;
                if (inCrescent(dx, dz)) {
                    floor = Blocks.SEA_LANTERN.defaultBlockState();
                } else if (d < 4.5) {
                    floor = Blocks.DARK_PRISMARINE.defaultBlockState();
                } else if (d >= 8.5 && d < 9.5 || d >= 16.5 && d < 17.3) {
                    floor = Blocks.PRISMARINE_BRICKS.defaultBlockState();
                } else {
                    floor = ((dx + dz) & 1) == 0 ? black : white;
                }
                level.setBlock(pos, floor, flags);
                level.setBlock(pos.below(), Blocks.DEEPSLATE_TILES.defaultBlockState(), flags);
                level.setBlock(pos.below(2), Blocks.DEEPSLATE.defaultBlockState(), flags);
                // mureta
                if (d >= RADIUS - 0.5) {
                    double seg = Math.atan2(dz, dx) * 8.0 / Math.PI; // 16 colunas em volta
                    long nearest = Math.round(seg);
                    boolean column = Math.abs(seg - nearest) < 0.12;
                    BlockState wall = Math.floorMod(nearest, 2) == 0
                            ? Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState() : Blocks.QUARTZ_BRICKS.defaultBlockState();
                    level.setBlock(pos.above(), wall, flags);
                    if (column) {
                        level.setBlock(pos.above(2), wall, flags);
                        level.setBlock(pos.above(3), wall.is(Blocks.QUARTZ_BRICKS) ? Blocks.LANTERN.defaultBlockState()
                                : Blocks.SOUL_LANTERN.defaultBlockState(), flags);
                    }
                }
            }
        }
        buildDais(level, center, flags);
        buildPillar(level, center, true, flags);
        buildPillar(level, center, false, flags);
        buildThrone(level, center, flags);
        buildVeil(level, center, flags);
        setMode(level, center, true);
    }

    /** Lua crescente de lanternas do mar no centro. */
    private static boolean inCrescent(int dx, int dz) {
        double outer = dx * dx + dz * dz;
        double sx = dx - 1.4;
        double inner = sx * sx + dz * dz;
        return outer <= 3.2 * 3.2 && inner > 2.6 * 2.6;
    }

    private static void buildDais(ServerLevel level, BlockPos center, int flags) {
        // estrado: 11 de largura, de z = -19 até z = -13, com degraus na frente
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -19; dz <= -13; dz++) {
                level.setBlock(center.offset(dx, DAIS, dz), Blocks.DARK_PRISMARINE.defaultBlockState(), flags);
            }
            BlockState step = Blocks.PRISMARINE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);
            level.setBlock(center.offset(dx, DAIS, -12), step, flags);
        }
        // tapete azul até o trono
        for (int dz = -15; dz <= -13; dz++) {
            level.setBlock(center.offset(0, DAIS + 1, dz), Blocks.BLUE_CARPET.defaultBlockState(), flags);
        }
    }

    private static void buildPillar(ServerLevel level, BlockPos center, boolean boaz, int flags) {
        BlockPos base = center.offset(boaz ? -PILLAR_X : PILLAR_X, DAIS + 1, THRONE_Z);
        BlockState body = boaz ? Blocks.POLISHED_BASALT.defaultBlockState() : Blocks.QUARTZ_PILLAR.defaultBlockState();
        BlockState foot = boaz ? Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState() : Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState();
        level.setBlock(base, foot, flags);
        for (int dy = 1; dy < PILLAR_HEIGHT - 1; dy++) {
            level.setBlock(base.above(dy), body, flags);
        }
        // capitel com "romãs" (vermelho) dos dois lados
        BlockPos capital = base.above(PILLAR_HEIGHT - 1);
        level.setBlock(capital, foot, flags);
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            level.setBlock(capital.relative(dir), Blocks.RED_GLAZED_TERRACOTTA.defaultBlockState(), flags);
        }
    }

    private static void buildThrone(ServerLevel level, BlockPos center, int flags) {
        BlockPos seat = center.offset(0, DAIS + 1, THRONE_Z);
        level.setBlock(seat, Blocks.QUARTZ_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH), flags);
        // braços: preto (Boaz) e branco (Jachin)
        level.setBlock(seat.west(), Blocks.POLISHED_BLACKSTONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP), flags);
        level.setBlock(seat.east(), Blocks.QUARTZ_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP), flags);
        // encosto alto com a lua no alto
        for (int dy = 0; dy <= 3; dy++) {
            level.setBlock(seat.north().above(dy), Blocks.QUARTZ_PILLAR.defaultBlockState(), flags);
        }
        level.setBlock(seat.north().above(4), Blocks.PEARLESCENT_FROGLIGHT.defaultBlockState(), flags);
        level.setBlock(seat.north().above(4).west(), Blocks.QUARTZ_STAIRS.defaultBlockState()
                .setValue(StairBlock.FACING, Direction.EAST).setValue(StairBlock.HALF, net.minecraft.world.level.block.state.properties.Half.TOP), flags);
        level.setBlock(seat.north().above(4).east(), Blocks.QUARTZ_STAIRS.defaultBlockState()
                .setValue(StairBlock.FACING, Direction.WEST).setValue(StairBlock.HALF, net.minecraft.world.level.block.state.properties.Half.TOP), flags);
        // a lua crescente aos pés
        level.setBlock(seat.south(), Blocks.LIGHT_BLUE_CARPET.defaultBlockState(), flags);
    }

    /** O véu entre os pilares: vidro azul com romãs (vidro vermelho) e palmas (verde). */
    private static void buildVeil(ServerLevel level, BlockPos center, int flags) {
        for (int dx = -PILLAR_X; dx <= PILLAR_X; dx++) {
            for (int dy = 1; dy <= PILLAR_HEIGHT - 1; dy++) {
                BlockPos pos = center.offset(dx, DAIS + dy, THRONE_Z - 2);
                int pattern = Math.floorMod(dx * 3 + dy * 5, 7);
                BlockState glass = pattern == 0 ? Blocks.RED_STAINED_GLASS.defaultBlockState()
                        : pattern == 3 && dy > 2 ? Blocks.GREEN_STAINED_GLASS.defaultBlockState()
                        : dy == PILLAR_HEIGHT - 1 ? Blocks.PURPLE_STAINED_GLASS.defaultBlockState()
                        : Blocks.BLUE_STAINED_GLASS.defaultBlockState();
                level.setBlock(pos, glass, flags);
            }
        }
    }

    /** Acende o pilar do modo ativo (Boaz = preto / Jachin = branco) e apaga o outro. */
    public static void setMode(ServerLevel level, BlockPos center, boolean boaz) {
        level.setBlock(pillarTop(center, true), boaz ? Blocks.CRYING_OBSIDIAN.defaultBlockState()
                : Blocks.POLISHED_BLACKSTONE.defaultBlockState(), Block.UPDATE_CLIENTS);
        level.setBlock(pillarTop(center, false), boaz ? Blocks.QUARTZ_BLOCK.defaultBlockState()
                : Blocks.SEA_LANTERN.defaultBlockState(), Block.UPDATE_CLIENTS);
    }
}

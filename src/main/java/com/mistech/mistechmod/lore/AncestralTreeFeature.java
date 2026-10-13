package com.mistech.mistechmod.lore;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Árvore Ancestral da Mata Ancestral: tronco grosso (≈3×3 ou 4×4, redondo) de 22 a 40 blocos, sapopemas (raízes em
 * contraforte) descendo para o chão, galhos com copas, copa larga no topo, cipós no tronco e musgo em volta.
 */
public class AncestralTreeFeature extends Feature<NoneFeatureConfiguration> {
    private static final BlockState WOOD = Blocks.JUNGLE_WOOD.defaultBlockState();
    private static final BlockState LEAVES = Blocks.JUNGLE_LEAVES.defaultBlockState().setValue(LeavesBlock.DISTANCE, 1);
    private static final BlockState FLOWER_LEAVES = Blocks.FLOWERING_AZALEA_LEAVES.defaultBlockState().setValue(LeavesBlock.DISTANCE, 1);

    public AncestralTreeFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    /** As arenas do folclore têm chão de barro batido (packed mud), que não existe na mata: não nasce árvore perto. */
    private static boolean nearLair(WorldGenLevel level, BlockPos origin) {
        int hits = 0;
        for (int dx = -12; dx <= 12; dx += 4) {
            for (int dz = -12; dz <= 12; dz += 4) {
                int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG, origin.getX() + dx, origin.getZ() + dz) - 1;
                var s = level.getBlockState(new BlockPos(origin.getX() + dx, y, origin.getZ() + dz));
                if (s.is(Blocks.PACKED_MUD) || s.is(Blocks.MUD_BRICKS)) {
                    if (++hits >= 2) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();
        if (!level.getBlockState(origin.below()).is(BlockTags.DIRT) || nearLair(level, origin)) {
            return false;
        }
        for (int y = 1; y < 8; y++) {
            if (!replaceable(level.getBlockState(origin.above(y)))) {
                return false;
            }
        }
        boolean big = random.nextFloat() < 0.45F;
        double radius = big ? 2.2 : 1.6;
        int height = (big ? 30 : 22) + random.nextInt(10);

        // tronco (com base mais larga), afundando 2 blocos no chão
        for (int y = -2; y < height; y++) {
            double r = y < 4 ? radius + (4 - y) * 0.18 : radius * (1.0 - 0.3 * y / height);
            int ir = Mth.ceil(r);
            for (int dx = -ir; dx <= ir; dx++) {
                for (int dz = -ir; dz <= ir; dz++) {
                    if (dx * dx + dz * dz <= r * r + 0.3) {
                        this.wood(level, origin.offset(dx, y, dz), y < 1);
                    }
                }
            }
        }
        // sapopemas: lâminas de raiz que descem do tronco até o chão
        int roots = 4 + random.nextInt(3);
        double start = random.nextDouble() * Math.PI * 2;
        for (int i = 0; i < roots; i++) {
            double a = start + i * Math.PI * 2 / roots + (random.nextDouble() - 0.5) * 0.5;
            int len = 3 + random.nextInt(big ? 5 : 3);
            for (int s = 0; s <= len; s++) {
                int x = (int) Math.round(Math.cos(a) * (radius + s));
                int z = (int) Math.round(Math.sin(a) * (radius + s));
                int top = Math.max(0, (big ? 5 : 3) - s);
                for (int y = -2; y <= top; y++) {
                    this.wood(level, origin.offset(x, y, z), y < 1);
                }
            }
        }
        // galhos com copas
        int branches = 4 + random.nextInt(3);
        for (int i = 0; i < branches; i++) {
            double a = random.nextDouble() * Math.PI * 2;
            int y0 = (int) (height * 0.55) + random.nextInt(Math.max(1, (int) (height * 0.35)));
            int len = 3 + random.nextInt(big ? 5 : 4); // alcance máx. ~13 blocos (escrita fica nos chunks vizinhos)
            BlockPos end = origin;
            for (int s = 0; s <= len; s++) {
                end = origin.offset((int) Math.round(Math.cos(a) * (radius + s)), y0 + s / 2, (int) Math.round(Math.sin(a) * (radius + s)));
                this.branch(level, end, Math.abs(Math.cos(a)) > Math.abs(Math.sin(a)) ? Direction.Axis.X : Direction.Axis.Z);
            }
            this.leafBlob(level, random, end.above(), 3 + random.nextInt(2), 0.55);
        }
        // copa do topo
        this.leafBlob(level, random, origin.above(height), big ? 6 : 5, 0.5);
        this.leafBlob(level, random, origin.above(height - 3), big ? 5 : 4, 0.6);
        // cipós no tronco
        int ir = Mth.ceil(radius) + 1;
        for (int k = 0; k < 14; k++) {
            int dx = random.nextInt(ir * 2 + 1) - ir;
            int dz = random.nextInt(ir * 2 + 1) - ir;
            int y = 4 + random.nextInt(Math.max(1, height - 6));
            BlockPos pos = origin.offset(dx, y, dz);
            if (!level.getBlockState(pos).isAir()) {
                continue;
            }
            for (Direction d : Direction.Plane.HORIZONTAL) {
                if (level.getBlockState(pos.relative(d)).is(Blocks.JUNGLE_WOOD)) {
                    int hang = 3 + random.nextInt(8);
                    for (int h = 0; h < hang && level.getBlockState(pos.below(h)).isAir()
                            && level.getBlockState(pos.below(h).relative(d)).is(Blocks.JUNGLE_WOOD); h++) {
                        level.setBlock(pos.below(h), Blocks.VINE.defaultBlockState().setValue(VineBlock.getPropertyForFace(d), true), 2);
                    }
                    break;
                }
            }
        }
        // musgo e folhas secas em volta da base
        int moss = (int) (radius + 6);
        for (int dx = -moss; dx <= moss; dx++) {
            for (int dz = -moss; dz <= moss; dz++) {
                if (random.nextFloat() > 0.35F || dx * dx + dz * dz > moss * moss) {
                    continue;
                }
                BlockPos top = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG, origin.offset(dx, 0, dz));
                if (Math.abs(top.getY() - origin.getY()) > 3 || !level.getBlockState(top).isAir()) {
                    continue;
                }
                if (level.getBlockState(top.below()).is(Blocks.GRASS_BLOCK)) {
                    level.setBlock(top.below(), random.nextBoolean() ? Blocks.MOSS_BLOCK.defaultBlockState() : Blocks.PODZOL.defaultBlockState(), 2);
                    if (random.nextFloat() < 0.4F) {
                        level.setBlock(top, Blocks.MOSS_CARPET.defaultBlockState(), 2);
                    }
                }
            }
        }
        return true;
    }

    private static boolean replaceable(BlockState state) {
        return state.isAir() || state.is(BlockTags.LEAVES) || state.canBeReplaced() || state.is(BlockTags.REPLACEABLE_BY_TREES)
                || state.is(Blocks.VINE);
    }

    private void wood(WorldGenLevel level, BlockPos pos, boolean underground) {
        BlockState state = level.getBlockState(pos);
        if (replaceable(state) || state.is(BlockTags.LOGS) || underground && (state.is(BlockTags.DIRT) || state.is(BlockTags.BASE_STONE_OVERWORLD))) {
            level.setBlock(pos, WOOD, 2);
        }
    }

    private void branch(WorldGenLevel level, BlockPos pos, Direction.Axis axis) {
        if (replaceable(level.getBlockState(pos))) {
            level.setBlock(pos, Blocks.JUNGLE_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis), 2);
        }
    }

    /** Copa achatada (elipsoide) de folhas, com um pouco de azaleia florida. */
    private void leafBlob(WorldGenLevel level, RandomSource random, BlockPos center, int r, double flat) {
        int ry = Math.max(2, (int) Math.round(r * flat));
        for (int dy = -ry; dy <= ry; dy++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    double d = (dx * dx + dz * dz) / (double) (r * r) + (dy * dy) / (double) (ry * ry);
                    if (d > 1.0 || d > 0.75 && random.nextFloat() < 0.35F) {
                        continue;
                    }
                    BlockPos pos = center.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(pos);
                    if (state.isAir() || state.canBeReplaced() || state.is(Blocks.VINE)) {
                        level.setBlock(pos, random.nextFloat() < 0.06F ? FLOWER_LEAVES : LEAVES, 2);
                    }
                }
            }
        }
    }
}

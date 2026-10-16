package com.mistech.mistechmod.realm.dream;

import com.mistech.mistechmod.registry.DreamRealmContent;
import com.mistech.mistechmod.registry.MurkyWood;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Features próprias do Sonho (todas sem configuração). */
public final class DreamFeatures {

    private DreamFeatures() {}

    static boolean soil(BlockState state) {
        return state.is(BlockTags.SUPPORTS_VEGETATION) || state.is(BlockTags.DIRT);
    }

    static boolean free(WorldGenLevel level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        return s.isAir() || s.canBeReplaced() || s.is(BlockTags.LEAVES);
    }

    static void set(WorldGenLevel level, BlockPos pos, BlockState state) {
        if (free(level, pos)) {
            level.setBlock(pos, state, Block.UPDATE_CLIENTS);
        }
    }

    /** Altura do topo da ilha (ou chão) na coluna da origem. */
    static BlockPos surface(WorldGenLevel level, BlockPos origin) {
        return new BlockPos(origin.getX(), level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, origin.getX(), origin.getZ()), origin.getZ());
    }

    // ------------------------------------------------------------------ Árvore-Mão (Floresta das Mãos)

    /** Tronco turvo torto que termina numa mão aberta: 5 dedos de madeira subindo para fora. */
    public static final class HandTree extends Feature<NoneFeatureConfiguration> {
        public HandTree() {
            super(NoneFeatureConfiguration.CODEC);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
            WorldGenLevel level = context.level();
            RandomSource random = context.random();
            BlockPos base = surface(level, context.origin());
            if (!soil(level.getBlockState(base.below()))) {
                return false;
            }
            BlockState log = MurkyWood.LOG.get().defaultBlockState();
            BlockState wood = MurkyWood.WOOD.get().defaultBlockState();
            int height = 6 + random.nextInt(5);
            double leanX = (random.nextDouble() - 0.5) * 0.5, leanZ = (random.nextDouble() - 0.5) * 0.5;
            BlockPos top = base;
            for (int y = 0; y < height; y++) {
                top = base.offset(Mth.floor(leanX * y), y, Mth.floor(leanZ * y));
                set(level, top, log);
            }
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (Math.abs(dx) + Math.abs(dz) <= 1) {
                        set(level, top.offset(dx, 1, dz), wood);
                    }
                }
            }
            double start = random.nextDouble() * Math.PI * 2.0;
            for (int f = 0; f < 5; f++) {
                double angle = start + f * (Math.PI * 2.0 / 5.0) + (random.nextDouble() - 0.5) * 0.4;
                boolean thumb = f == 0;
                int length = thumb ? 3 + random.nextInt(2) : 4 + random.nextInt(3);
                double out = thumb ? 1.0 : 0.65, up = thumb ? 0.45 : 1.0;
                BlockPos from = top.above(thumb ? 0 : 1);
                for (int i = 1; i <= length; i++) {
                    double curl = i > length - 2 ? -0.35 : 0.0; // a ponta dobra para dentro
                    double r = i * out + curl * (i - length + 2);
                    BlockPos p = from.offset((int) Math.round(Math.cos(angle) * r), (int) Math.round(i * up), (int) Math.round(Math.sin(angle) * r));
                    set(level, p, wood);
                }
            }
            return true;
        }
    }

    // ------------------------------------------------------------------ embaixo das ilhas

    /** Acha o fundo de uma ilha na coluna (bloco sólido, não folha, com ar embaixo, acima do chão de baixo, até y 200). */
    static BlockPos islandBottom(WorldGenLevel level, int x, int z) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(x, 200, z);
        for (int y = 200; y > FLOOR_SCAN; y--) {
            p.setY(y);
            BlockState s = level.getBlockState(p);
            if (!s.isAir() && s.isSolid() && !s.is(BlockTags.LEAVES) && level.getBlockState(p.below()).isAir()) {
                return p.immutable();
            }
        }
        return null;
    }

    /** Cerejeira pendurada embaixo da ilha: tronco descendo e copa no fim. */
    public static final class InvertedTree extends Feature<NoneFeatureConfiguration> {
        public InvertedTree() {
            super(NoneFeatureConfiguration.CODEC);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
            WorldGenLevel level = context.level();
            RandomSource random = context.random();
            BlockPos top = islandBottom(level, context.origin().getX(), context.origin().getZ());
            if (top == null) {
                return false;
            }
            BlockState log = Blocks.CHERRY_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
            BlockState leaves = Blocks.CHERRY_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
            int length = 4 + random.nextInt(5);
            BlockPos end = top;
            for (int i = 1; i <= length; i++) {
                BlockPos p = top.below(i);
                if (!free(level, p)) {
                    break;
                }
                level.setBlock(p, log, Block.UPDATE_CLIENTS);
                end = p;
            }
            if (end == top) {
                return false;
            }
            int r = 2 + random.nextInt(2);
            for (int dx = -r; dx <= r; dx++) {
                for (int dy = -r; dy <= 1; dy++) {
                    for (int dz = -r; dz <= r; dz++) {
                        double d = dx * dx + dy * dy * 1.6 + dz * dz;
                        if (d <= r * r + 0.5 && random.nextFloat() > 0.08F) {
                            BlockPos p = end.offset(dx, dy, dz);
                            if (level.getBlockState(p).isAir()) {
                                level.setBlock(p, leaves, Block.UPDATE_CLIENTS);
                            }
                        }
                    }
                }
            }
            return true;
        }
    }

    /** Vinhas Espirais penduradas embaixo das ilhas (3 a 14 blocos). */
    public static final class HangingVines extends Feature<NoneFeatureConfiguration> {
        public HangingVines() {
            super(NoneFeatureConfiguration.CODEC);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
            WorldGenLevel level = context.level();
            BlockPos top = islandBottom(level, context.origin().getX(), context.origin().getZ());
            if (top == null) {
                return false;
            }
            BlockState vine = DreamRealmContent.SPIRAL_VINE.get().defaultBlockState();
            int length = 3 + context.random().nextInt(12);
            for (int i = 1; i <= length; i++) {
                BlockPos p = top.below(i);
                if (!level.getBlockState(p).isAir()) {
                    break;
                }
                level.setBlock(p, vine, Block.UPDATE_CLIENTS);
            }
            return true;
        }
    }

    // ------------------------------------------------------------------ Nuvem Saltitante

    /** Acha o chão de baixo (embaixo das ilhas): primeiro bloco sólido descendo de y {@link #FLOOR_SCAN}; null se cair dentro de ilha. */
    static BlockPos floor(WorldGenLevel level, int x, int z) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(x, FLOOR_SCAN, z);
        if (!level.getBlockState(p).isAir()) {
            return null;
        }
        for (int y = FLOOR_SCAN - 1; y > 20; y--) {
            p.setY(y);
            BlockState s = level.getBlockState(p);
            if (s.isSolid() && !s.is(BlockTags.LEAVES)) {
                return p.immutable();
            }
        }
        return null;
    }

    /** Altura de onde começa a busca do chão (as ilhas ficam acima disso). */
    public static final int FLOOR_SCAN = 92;

    /** Mancha de Nuvem Saltitante (losango de 13 blocos) no chão de baixo, para pular até as ilhas. */
    public static final class BouncyPad extends Feature<NoneFeatureConfiguration> {
        public BouncyPad() {
            super(NoneFeatureConfiguration.CODEC);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
            WorldGenLevel level = context.level();
            BlockPos o = context.origin();
            BlockPos center = floor(level, o.getX(), o.getZ());
            if (center == null || !soil(level.getBlockState(center))) {
                return false;
            }
            BlockState bouncy = DreamRealmContent.BOUNCY_CLOUD.get().defaultBlockState();
            boolean any = false;
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (Math.abs(dx) + Math.abs(dz) > 2) {
                        continue;
                    }
                    BlockPos p = center.offset(dx, 0, dz);
                    {
                        BlockState s = level.getBlockState(p);
                        if (s.isSolid() && level.getBlockState(p.above()).canBeReplaced()) {
                            level.setBlock(p, bouncy, Block.UPDATE_CLIENTS);
                            if (!level.getBlockState(p.above()).isAir()) {
                                level.setBlock(p.above(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                            }
                            any = true;
                        }
                    }
                }
            }
            return any;
        }
    }

    // ------------------------------------------------------------------ Poste apagado (Bairro Errado)

    public static final class LampPost extends Feature<NoneFeatureConfiguration> {
        public LampPost() {
            super(NoneFeatureConfiguration.CODEC);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
            WorldGenLevel level = context.level();
            BlockPos base = surface(level, context.origin());
            if (!level.getBlockState(base.below()).isSolid()) {
                return false;
            }
            int h = 3 + context.random().nextInt(2);
            for (int y = 0; y < h; y++) {
                set(level, base.above(y), Blocks.DARK_OAK_FENCE.defaultBlockState());
            }
            set(level, base.above(h), Blocks.REDSTONE_LAMP.defaultBlockState());
            return true;
        }
    }
}

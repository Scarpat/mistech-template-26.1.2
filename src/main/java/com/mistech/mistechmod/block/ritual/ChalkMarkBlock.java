package com.mistech.mistechmod.block.ritual;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Marca de Giz: símbolo desenhado no chão com o Giz de Cinzas (botão direito no topo de um bloco, gasta 1 de durabilidade).
 * Fino, sem colisão; some se o bloco de baixo sair. Não dropa nada.
 */
public class ChalkMarkBlock extends Block {
    public static final MapCodec<ChalkMarkBlock> CODEC = simpleCodec(ChalkMarkBlock::new);
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 0.25, 16);

    public ChalkMarkBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        return direction == Direction.DOWN && !this.canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(40) == 0) {
            level.addParticle(new DustParticleOptions(0xD8D0E8, 0.6F), pos.getX() + random.nextDouble(), pos.getY() + 0.05,
                    pos.getZ() + random.nextDouble(), 0.0, 0.02, 0.0);
        }
    }
}

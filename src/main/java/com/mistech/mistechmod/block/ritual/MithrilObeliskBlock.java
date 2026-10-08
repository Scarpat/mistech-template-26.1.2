package com.mistech.mistechmod.block.ritual;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Obelisco de Mitril: peça do círculo Tier II/III (diagonais a 4 do altar). Só decoração + faíscas. */
public class MithrilObeliskBlock extends Block {
    public static final MapCodec<MithrilObeliskBlock> CODEC = simpleCodec(MithrilObeliskBlock::new);

    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(2, 0, 2, 14, 3, 14),
            Block.box(4, 3, 4, 12, 13, 12),
            Block.box(5, 13, 5, 11, 16, 11));

    public MithrilObeliskBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<MithrilObeliskBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(6) == 0) {
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.4, pos.getY() + 1.05,
                    pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.4, 0.0, 0.02, 0.0);
        }
    }
}

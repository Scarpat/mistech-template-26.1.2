package com.mistech.mistechmod.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Cogumelo Amarelado: nasce perto da Árvore Turva quando ela cresce (10%).
 * Planta pequena (como os cogumelos do jogo), mas não precisa de escuro e não se espalha.
 */
public class YellowMushroomBlock extends VegetationBlock {
    public static final MapCodec<YellowMushroomBlock> CODEC = simpleCodec(YellowMushroomBlock::new);
    private static final VoxelShape SHAPE = Block.column(6.0, 0.0, 6.0);

    public YellowMushroomBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<YellowMushroomBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** De vez em quando solta um brilho amarelo. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(12) == 0) {
            level.addParticle(ParticleTypes.WAX_ON, pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 0.35,
                    pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0.0, 0.02, 0.0);
        }
    }
}

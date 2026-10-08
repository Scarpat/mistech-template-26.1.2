package com.mistech.mistechmod.block;

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
import net.minecraft.world.phys.shapes.VoxelShape;

/** Raio Engarrafado: pote de vidro com um raio preso dentro. Brilha e solta faíscas. */
public class BottledLightningBlock extends Block {
    public static final MapCodec<BottledLightningBlock> CODEC = simpleCodec(BottledLightningBlock::new);

    public BottledLightningBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<BottledLightningBlock> codec() {
        return CODEC;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(2) == 0) {
            level.addParticle(ParticleTypes.ELECTRIC_SPARK,
                    pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 0.1 + random.nextDouble() * 0.55,
                    pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0.0, 0.0, 0.0);
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return GlassJarBlock.SHAPE;
    }
}

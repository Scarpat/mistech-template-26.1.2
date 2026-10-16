package com.mistech.mistechmod.realm.dream;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/** Planta simples do Sonho (modelo em cruz), com partícula opcional de vez em quando. */
public class DreamPlantBlock extends VegetationBlock {
    public static final MapCodec<DreamPlantBlock> CODEC = simpleCodec(p -> new DreamPlantBlock(p, 12.0, null, 0));
    private final VoxelShape shape;
    private final @Nullable ParticleOptions particle;
    private final int particleChance;

    public DreamPlantBlock(BlockBehaviour.Properties properties, double height, @Nullable ParticleOptions particle, int particleChance) {
        super(properties);
        this.shape = Block.column(12.0, 0.0, height);
        this.particle = particle;
        this.particleChance = particleChance;
    }

    @Override
    protected MapCodec<? extends VegetationBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.shape;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (this.particle != null && random.nextInt(this.particleChance) == 0) {
            level.addParticle(this.particle, pos.getX() + 0.25 + random.nextDouble() * 0.5, pos.getY() + 0.4 + random.nextDouble() * 0.4,
                    pos.getZ() + 0.25 + random.nextDouble() * 0.5, 0.0, 0.015, 0.0);
        }
    }
}

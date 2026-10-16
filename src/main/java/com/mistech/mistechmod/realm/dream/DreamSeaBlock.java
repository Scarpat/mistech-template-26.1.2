package com.mistech.mistechmod.realm.dream;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Nuvem Densa (Mar de Nuvens) e Névoa Estelar (Sono REM): dá para andar por cima, agachar afunda (como andaime),
 * dentro dela dá para subir pulando (tag {@code minecraft:climbable}) e cair nela nunca machuca.
 * Translúcida (modelo com {@code force_translucent}): só desenha as faces viradas para fora do "mar", deixa a luz passar.
 */
public class DreamSeaBlock extends Block {
    public static final MapCodec<DreamSeaBlock> CODEC = simpleCodec(DreamSeaBlock::new);

    public DreamSeaBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return context.isAbove(Shapes.block(), pos, true) && !context.isDescending() ? Shapes.block() : Shapes.empty();
    }

    @Override
    protected boolean skipRendering(BlockState state, BlockState adjacent, Direction direction) {
        return adjacent.is(this) || super.skipRendering(state, adjacent, direction);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }

    @Override
    protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(80) == 0 && level.getBlockState(pos.above()).isAir()) {
            level.addParticle(ParticleTypes.CLOUD, pos.getX() + random.nextDouble(), pos.getY() + 1.05, pos.getZ() + random.nextDouble(),
                    (random.nextDouble() - 0.5) * 0.02, 0.01, (random.nextDouble() - 0.5) * 0.02);
        }
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
        entity.causeFallDamage(fallDistance, 0.0F, level.damageSources().fall());
    }
}

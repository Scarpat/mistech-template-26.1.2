package com.mistech.mistechmod.sky;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Bloco de Nuvem (semissólido): só é sólido para quem está <b>em cima</b> e não está agachado (mesma regra do Andaime).
 * Dá para pular de baixo para cima através dele, e agachar faz cair através. Amortece {@link #FALL_ABSORB} da queda.
 * Esta é a Nuvem Estável (permanente); a nuvem da runa é {@link TemporaryCloudBlock}.
 */
public class CloudBlock extends HalfTransparentBlock {
    public static final MapCodec<CloudBlock> CODEC = simpleCodec(CloudBlock::new);
    /** Fração do dano de queda que a nuvem absorve. */
    public static final float FALL_ABSORB = 0.8F;

    public CloudBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends CloudBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return context.isAbove(Shapes.block(), pos, true) && !context.isDescending() ? Shapes.block() : Shapes.empty();
    }

    @Override
    protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }

    @Override
    protected boolean skipRendering(BlockState state, BlockState adjacent, Direction direction) {
        return adjacent.getBlock() instanceof CloudBlock || super.skipRendering(state, adjacent, direction);
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
        entity.causeFallDamage(fallDistance, 1.0F - FALL_ABSORB, level.damageSources().fall());
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(12) == 0) {
            level.addParticle(ParticleTypes.CLOUD, pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(),
                    pos.getZ() + random.nextDouble(), 0.0, 0.005, 0.0);
        }
    }
}

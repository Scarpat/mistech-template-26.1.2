package com.mistech.mistechmod.machine.forge;

import com.mistech.mistechmod.machine.MachineBlock;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** Núcleo da Forja Trituradora (o resto do cubo são blocos normais; ver {@link CrusherForgeStructure}). */
public class CrusherForgeBlock extends MachineBlock {
    public static final MapCodec<CrusherForgeBlock> CODEC = simpleCodec(CrusherForgeBlock::new);

    public CrusherForgeBlock(BlockBehaviour.Properties properties) {
        super(properties, ForgeContent.CRUSHER_FORGE_BE);
        this.registerDefaultState(this.defaultBlockState().setValue(CrusherForgeStructure.FORMED, false));
    }

    @Override
    protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(CrusherForgeStructure.FORMED);
    }

    /** Núcleo quebrado: as peças voltam a aparecer. */
    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        for (CrusherForgeStructure.Part part : CrusherForgeStructure.parts(pos, state.getValue(FACING))) {
            BlockState s = level.getBlockState(part.pos());
            if (s.hasProperty(CrusherForgeStructure.FORMED) && s.getValue(CrusherForgeStructure.FORMED)) {
                level.setBlock(part.pos(), s.setValue(CrusherForgeStructure.FORMED, false), net.minecraft.world.level.block.Block.UPDATE_ALL);
            }
        }
    }

    @Override
    public MapCodec<CrusherForgeBlock> codec() {
        return CODEC;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(LIT) && state.getValue(CrusherForgeStructure.FORMED)) {
            // migalhas caindo entre as mós e um pouco de fumaça subindo da fenda da forja
            var front = state.getValue(FACING);
            var back = front.getOpposite();
            var right = CrusherForgeStructure.right(front);
            double cx = pos.getX() + 0.5 + back.getStepX() * 0.5 + right.getStepX() * 0.5;
            double cz = pos.getZ() + 0.5 + back.getStepZ() * 0.5 + right.getStepZ() * 0.5;
            double along = (random.nextDouble() - 0.5) * 1.2;
            double px = cx + back.getStepX() * along, pz = cz + back.getStepZ() * along;
            level.addParticle(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.FALLING_DUST,
                    net.minecraft.world.level.block.Blocks.GRAVEL.defaultBlockState()), px, pos.getY() + 1.45, pz, 0.0, 0.0, 0.0);
            if (random.nextInt(3) == 0) {
                level.addParticle(ParticleTypes.CRIT, px, pos.getY() + 1.55, pz, (random.nextDouble() - 0.5) * 0.2, 0.15, (random.nextDouble() - 0.5) * 0.2);
            }
            if (random.nextInt(4) == 0) {
                level.addParticle(ParticleTypes.SMOKE, px, pos.getY() + 1.05, pz, 0.0, 0.03, 0.0);
            }
        }
        if (state.getValue(LIT) && random.nextInt(3) == 0) {
            var front = state.getValue(FACING);
            double x = pos.getX() + 0.5 + front.getStepX() * 0.52, z = pos.getZ() + 0.5 + front.getStepZ() * 0.52;
            if (state.getValue(CrusherForgeStructure.FORMED)) {
                // boca da forja montada: no meio da frente do cubo, um pouco para dentro
                var right = CrusherForgeStructure.right(front);
                x = pos.getX() + 0.5 + right.getStepX() * (0.5 + (random.nextDouble() - 0.5) * 0.8) + front.getStepX() * 0.3;
                z = pos.getZ() + 0.5 + right.getStepZ() * (0.5 + (random.nextDouble() - 0.5) * 0.8) + front.getStepZ() * 0.3;
            }
            level.addParticle(ParticleTypes.SMOKE, x, pos.getY() + 0.4, z, 0.0, 0.02, 0.0);
            level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, x, pos.getY() + 0.3, z, 0.0, 0.0, 0.0);
        }
    }
}

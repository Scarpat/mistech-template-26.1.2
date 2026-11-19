package com.mistech.mistechmod.spellcraft.conjurer;

import com.mistech.mistechmod.machine.arcane.ArcaneMachineBlock;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Conjurador Rúnico: pedestal de pedra-negra com anel de mitril; o cristal gira em cima (renderer).
 * Comparador: com Olhar Vigilante, quantas criaturas hostis foram vistas (até 15); senão, o normal das máquinas.
 */
public class RunicConjurerBlock extends ArcaneMachineBlock {
    public static final MapCodec<RunicConjurerBlock> CODEC = simpleCodec(RunicConjurerBlock::new);

    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(1.0, 0.0, 1.0, 15.0, 4.0, 15.0),
            Block.box(3.0, 4.0, 3.0, 13.0, 10.0, 13.0),
            Block.box(2.0, 10.0, 2.0, 14.0, 13.0, 14.0));

    public RunicConjurerBlock(BlockBehaviour.Properties properties) {
        super(properties, ConjurerContent.RUNIC_CONJURER_BE);
    }

    protected RunicConjurerBlock(BlockBehaviour.Properties properties,
            java.util.function.Supplier<? extends net.minecraft.world.level.block.entity.BlockEntityType<? extends com.mistech.mistechmod.machine.MachineBlockEntity>> type) {
        super(properties, type);
    }

    @Override
    public MapCodec<? extends RunicConjurerBlock> codec() {
        return CODEC;
    }

    @Override
    protected ParticleOptions particle() {
        return ParticleTypes.ENCHANT;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        if (level.getBlockEntity(pos) instanceof RunicConjurerBlockEntity be && be.alarm() > 0) {
            return be.alarm();
        }
        return super.getAnalogOutputSignal(state, level, pos, direction);
    }
}

package com.mistech.mistechmod.block;

import com.mistech.mistechmod.machine.forge.CrusherForgeStructure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Bloco de Magia Impura. Também é a peça de "revestimento" da Forja Trituradora: com a forja montada
 * ({@link CrusherForgeStructure#FORMED}) fica invisível, não esconde as faces vizinhas e o clique abre a Forja.
 */
public class ImpureMagicBlock extends Block {
    public ImpureMagicBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(CrusherForgeStructure.FORMED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CrusherForgeStructure.FORMED);
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state) {
        return state.getValue(CrusherForgeStructure.FORMED) ? Shapes.empty() : super.getOcclusionShape(state);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return state.getValue(CrusherForgeStructure.FORMED) || super.propagatesSkylightDown(state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (state.getValue(CrusherForgeStructure.FORMED) && CrusherForgeStructure.openCore(level, pos, player)) {
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
